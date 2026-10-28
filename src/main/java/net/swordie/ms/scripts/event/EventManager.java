package net.swordie.ms.scripts.event;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.world.field.Field;

import javax.script.Invocable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * Registry and dispatcher of MetaMS script events ({@code ctx.makeEvent / ctx.getEvent / stage.setEvent}).
 *
 * <p>Game hooks call {@link #onMobRemoved}, {@link #onPlayerChangedMap}, {@link #onDisconnect},
 * {@link #onMobHit} and {@link #onPartyMemberLeft}; the matching script callbacks run on the event's engine.</p>
 */
public final class EventManager {

    private static final EventManager INSTANCE = new EventManager();
    private static final ThreadLocal<int[]> CALLBACK_DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    /** Instanced events with no member for this long are destroyed. */
    private static final long IDLE_INSTANCED_MS = 2 * 60 * 1000L;
    /** Hard lifetime cap for instanced events (MetaMS scripts run their own ≤ 1 h kick timers). */
    private static final long MAX_INSTANCED_MS = 6 * 60 * 60 * 1000L;
    private static final long REAPER_PERIOD_MS = 60 * 1000L;
    /** Event map copies are ticked here (drops, queued field tasks, mob updates) like Swordie instance fields. */
    private static final long TICK_PERIOD_MS = 500L;
    private static final Set<String> UNFINISHED = Set.of("event_bingo", "event_rps", "mg_rainbow_rush",
            "mg_colorful_mail", "mg_seesaw", "mg_wagultower");

    private final Map<Integer, EventInstance> instances = new ConcurrentHashMap<>();
    private final Map<Integer, EventInstance> bound = new ConcurrentHashMap<>();
    private final Map<Integer, Field> lastField = new ConcurrentHashMap<>();
    private final Map<Field, EventInstance> ownedFields = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<Mob, EventInstance> watchedMobs = Collections.synchronizedMap(new WeakHashMap<>());
    private final Set<Mob> handledMobs = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private final AtomicInteger ids = new AtomicInteger(1);
    private final AtomicBoolean reaperStarted = new AtomicBoolean();

    private volatile EventScriptHost host;
    private volatile EventScheduler scheduler = EventScheduler.server();
    private volatile Path scriptDir = Path.of(ServerConstants.SCRIPT_DIR, "event");
    private volatile IntFunction<Field> fieldFactory = FieldData::getFieldCopyById;
    private volatile boolean registerWithServer = true;
    private volatile boolean verbose = Boolean.getBoolean("events.verbose");

    private EventManager() {
    }

    public static EventManager get() {
        return INSTANCE;
    }

    // ---- configuration (ScriptManagerImpl installs the host; checks inject the rest) ------------------------------

    public void setHost(EventScriptHost host) {
        this.host = host;
    }

    EventScriptHost host() {
        EventScriptHost h = host;
        if (h == null) {
            throw new IllegalStateException("event script host not installed");
        }
        return h;
    }

    public void setScheduler(EventScheduler scheduler) {
        this.scheduler = scheduler == null ? EventScheduler.server() : scheduler;
    }

    EventScheduler scheduler() {
        return scheduler;
    }

    public void setScriptDir(Path dir) {
        this.scriptDir = dir;
    }

    public Path getScriptDir() {
        return scriptDir;
    }

    public void setFieldFactory(IntFunction<Field> factory) {
        this.fieldFactory = factory == null ? FieldData::getFieldCopyById : factory;
    }

    /** Kept for tools: event map copies are never added to the server's instance list. */
    public void setRegisterWithServer(boolean register) {
        this.registerWithServer = register;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    boolean isVerbose() {
        return verbose;
    }

    public boolean hasScript(String name) {
        return scriptFile(name) != null;
    }

    Path scriptFile(String name) {
        if (name == null || name.isBlank() || name.contains("..") || name.contains("/") || name.contains("\\")) {
            return null;
        }
        Path p = scriptDir.resolve(name + ".js");
        return Files.isRegularFile(p) ? p : null;
    }

    // ---- creation / lookup -----------------------------------------------------------------------------------------

    /**
     * {@code ctx.makeEvent(name, onlyOne, attachment)}: loads {@code event/<name>.js}, runs {@code init(attachment)}.
     *
     * @return the running instance, or null when the script is missing, {@code onlyOne} and one already runs on
     * the channel, or {@code init} failed (the caller scripts then show their own error text).
     */
    public EventInstance makeEvent(String name, boolean onlyOne, Object attachment, int channelId, Char owner,
                                   IntFunction<Field> channelFields, int exitFieldId) {
        if (name != null && UNFINISHED.contains(name)) {
            log(null, "makeEvent(" + name + "): gameplay is not implemented; admission refused");
            return null;
        }
        Path file = scriptFile(name);
        if (file == null) {
            log(null, "makeEvent(" + name + "): no script data/scripts/event/" + name + ".js");
            return null;
        }
        EventInstance ev;
        synchronized (this) {
            if (onlyOne && find(name, channelId, false) != null) {
                return null;
            }
            ev = new EventInstance(this, ids.getAndIncrement(), name, channelId, owner, channelFields,
                    fieldFactory, exitFieldId);
            instances.put(ev.getId(), ev);
        }
        try {
            Invocable inv = host().load(ev, file);
            ev.attachScript(inv);
        } catch (Exception | LinkageError e) {
            log(ev, "load failed: " + e);
            if (verbose) {
                e.printStackTrace();
            }
            ev.destroyEvent();
            return null;
        }
        startReaper();
        Object att = host().importValue(attachment);
        boolean ok = initEvent(ev, att);
        if (!ok || ev.isDestroyed()) {
            if (!ev.isDestroyed()) {
                ev.destroyEvent();
            }
            return ok ? ev : null;
        }
        log(ev, "started");
        return ev;
    }

    private boolean initEvent(EventInstance ev, Object attachment) {
        try {
            ev.invokeChecked("init", attachment);
            return true;
        } catch (NoSuchMethodException missing) {
            return true;
        } catch (Exception e) {
            log(ev, "init failed: " + e);
            if (verbose) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /** {@code ctx.getEvent(name)}: the running instance on the channel, else on any channel. */
    public EventInstance find(String name, int channelId) {
        return find(name, channelId, true);
    }

    private EventInstance find(String name, int channelId, boolean anyChannel) {
        if (name == null) {
            return null;
        }
        EventInstance fallback = null;
        for (EventInstance ev : instances.values()) {
            if (ev.isDestroyed() || !name.equals(ev.getName())) {
                continue;
            }
            if (ev.getChannelId() == channelId) {
                return ev;
            }
            if (fallback == null) {
                fallback = ev;
            }
        }
        return anyChannel ? fallback : null;
    }

    /** Every running instance of {@code name} on {@code channelId}. */
    public List<EventInstance> findAll(String name, int channelId) {
        List<EventInstance> out = new ArrayList<>();
        for (EventInstance ev : getRunning()) {
            if (ev.getName().equals(name) && ev.getChannelId() == channelId) {
                out.add(ev);
            }
        }
        return out;
    }

    public List<EventInstance> getRunning() {
        List<EventInstance> out = new ArrayList<>();
        for (EventInstance ev : instances.values()) {
            if (!ev.isDestroyed()) {
                out.add(ev);
            }
        }
        out.sort(Comparator.comparingInt(EventInstance::getId));
        return out;
    }

    void remove(EventInstance ev) {
        instances.remove(ev.getId());
        synchronized (ownedFields) {
            ownedFields.values().removeIf(e -> e == ev);
        }
        synchronized (watchedMobs) {
            watchedMobs.values().removeIf(e -> e == ev);
        }
        log(ev, "destroyed");
    }

    void registerFieldInstance(EventFieldInstance fi) {
        // Deliberately not added to Server.getInstances() (see scheduleTick).
    }

    void registerOwnedField(EventInstance ev, Field f) {
        ownedFields.put(f, ev);
    }

    /** Destroys every running event (server shutdown / GM). */
    public void destroyAll() {
        for (EventInstance ev : getRunning()) {
            ev.destroyEvent();
        }
    }

    // ---- member binding ({@code stage.setEvent}) --------------------------------------------------------------------

    public EventInstance getEvent(Char chr) {
        return chr == null ? null : bound.get(chr.getId());
    }

    /** {@code player.setEvent(event)}; {@code event == null} unbinds. */
    public void setEvent(Char chr, EventInstance ev) {
        if (chr == null) {
            return;
        }
        if (ev == null) {
            EventInstance cur = bound.get(chr.getId());
            if (cur != null) {
                unbind(chr, cur);
            }
            return;
        }
        if (ev.isDestroyed()) {
            return;
        }
        EventInstance previous = bound.put(chr.getId(), ev);
        if (previous != null && previous != ev) {
            previous.removeMember(chr);
        }
        if (chr.getField() != null) {
            lastField.put(chr.getId(), chr.getField());
        } else {
            lastField.remove(chr.getId());
        }
        ev.addMember(chr);
    }

    void unbind(Char chr, EventInstance ev) {
        bound.remove(chr.getId(), ev);
        lastField.remove(chr.getId());
        ev.removeMember(chr);
    }

    // ---- game hooks --------------------------------------------------------------------------------------------------

    /**
     * Mob removed from a map (Swordie notifies every observer; this dedupes). Only real kills (HP ≤ 0) reach
     * {@code mobDied}; scripted removals ({@code killAllMob}, map reset) do not.
     */
    public void onMobRemoved(Mob mob) {
        if (mob == null || instances.isEmpty()) {
            return;
        }
        EventInstance ev = watchedMobs.remove(mob);
        if (ev == null && mob.getField() != null) {
            ev = ownedFields.get(mob.getField());
        }
        if (ev == null || ev.isDestroyed() || mob.getHp() > 0) {
            return;
        }
        if (!handledMobs.add(mob)) {
            return;
        }
        EventInstance target = ev;
        dispatchCallback(target, null, () -> target.invoke("mobDied", new EventMob(mob)));
    }

    /** Damage dealt to a mob of an event map or a mob the event spawned. */
    public void onMobHit(Char chr, Mob mob, long damage) {
        if (mob == null || instances.isEmpty()) {
            return;
        }
        EventInstance ev = watchedMobs.get(mob);
        if (ev == null && mob.getField() != null) {
            ev = ownedFields.get(mob.getField());
        }
        if (ev == null || ev.isDestroyed()) {
            return;
        }
        EventInstance target = ev;
        dispatchCallback(target, chr, () -> target.invoke("mobHit", chr == null ? null : host().player(chr), new EventMob(mob), damage));
    }

    /** A bound character arrived on {@code to}; idempotent per (character, field object). */
    public void onPlayerChangedMap(Char chr, Field to) {
        if (chr == null || to == null) {
            return;
        }
        EventInstance ev = bound.get(chr.getId());
        if (ev == null || ev.isDestroyed()) {
            return;
        }
        Field previous = lastField.put(chr.getId(), to);
        if (previous == to) {
            return;
        }
        EventMap destination = new EventMap(ev, to, ev.owns(to));
        dispatchCallback(ev, chr, () -> ev.invoke("playerChangedMap", host().player(chr), destination));
    }

    /**
     * Fallback when the Char.warp hook is not installed: called before a warp, checks shortly after that the
     * character really arrived on {@code destination} and then reports the map change.
     */
    public void onWarpRequested(Char chr, Field destination) {
        if (chr == null || destination == null || !bound.containsKey(chr.getId())) {
            return;
        }
        scheduler.schedule(() -> {
            if (chr.getField() == destination) {
                onPlayerChangedMap(chr, destination);
            }
        }, 300);
    }

    /** Logout of a bound character. */
    public void onDisconnect(Char chr) {
        if (chr == null) {
            return;
        }
        EventInstance ev = bound.get(chr.getId());
        if (ev == null) {
            return;
        }
        dispatchCallback(ev, chr, () -> {
            try {
                if (!ev.isDestroyed()) {
                    ev.invoke("playerDisconnected", host().player(chr));
                }
            } finally {
                if (bound.get(chr.getId()) == ev) {
                    unbind(chr, ev);
                }
            }
        });
    }

    /** A bound character left (or was expelled from) the party. */
    public void onPartyMemberLeft(Char chr) {
        EventInstance ev = chr == null ? null : bound.get(chr.getId());
        if (ev == null || ev.isDestroyed()) {
            return;
        }
        Object party = host().partyDeparture(chr);
        dispatchCallback(ev, chr, () -> ev.invoke("partyMemberDischarged", party, host().player(chr)));
    }

    void dispatchCallback(EventInstance event, Char actor, Runnable callback) {
        boolean defer = Thread.holdsLock(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR)
                || holdsCharacterLock(actor) || holdsCharacterLock(event.owner());
        if (!defer) {
            for (Char member : event.memberChars()) {
                if (holdsCharacterLock(member)) { defer = true; break; }
            }
        }
        event.dispatchCallback(callback, defer);
    }

    private static boolean holdsCharacterLock(Char actor) {
        return actor != null && (Thread.holdsLock(actor)
                || net.swordie.ms.client.character.tms282.EconomyLocks.gateHeld(actor.getId()));
    }

    void watchMob(Mob mob, EventInstance ev) {
        watchedMobs.put(mob, ev);
    }

    void ignoreMob(Mob mob) {
        watchedMobs.remove(mob);
        handledMobs.add(mob);
    }

    // ---- callback context --------------------------------------------------------------------------------------------

    static void enterCallback() {
        CALLBACK_DEPTH.get()[0]++;
    }

    static void exitCallback() {
        CALLBACK_DEPTH.get()[0]--;
    }

    /** True while the current thread runs an event callback (member warps must then be immediate). */
    public static boolean inEventCallback() {
        return CALLBACK_DEPTH.get()[0] > 0;
    }

    // ---- housekeeping ------------------------------------------------------------------------------------------------

    private void startReaper() {
        if (reaperStarted.compareAndSet(false, true)) {
            scheduleReaper();
            scheduleTick();
        }
    }

    /**
     * Updates the event map copies. They are not added to {@code Server.getInstances()} (a plain list the server
     * tick iterates without locking), so the event manager ticks them itself.
     */
    private void scheduleTick() {
        scheduler.schedule(() -> {
            try {
                tick(System.currentTimeMillis());
            } finally {
                scheduleTick();
            }
        }, TICK_PERIOD_MS);
    }

    void tick(long now) {
        for (EventInstance ev : instances.values()) {
            if (ev.isDestroyed()) {
                continue;
            }
            for (Field f : ev.ownedFields().values()) {
                try {
                    f.update(now);
                } catch (RuntimeException e) {
                    if (verbose) {
                        log(ev, "field " + f.getId() + " update failed: " + e);
                    }
                }
            }
        }
    }

    private void scheduleReaper() {
        scheduler.schedule(() -> {
            try {
                reap(System.currentTimeMillis());
            } finally {
                scheduleReaper();
            }
        }, REAPER_PERIOD_MS);
    }

    /** Destroys instanced events that lost all members or outlived {@link #MAX_INSTANCED_MS}. */
    public int reap(long now) {
        int n = 0;
        for (EventInstance ev : getRunning()) {
            if (!ev.hasOwnedFields()) {
                continue; // world/global events (bingo, world boss, ...) live until their script ends them
            }
            for (Char chr : ev.memberChars()) {
                if (chr.getClient() == null && !chr.isOnline()) {
                    onDisconnect(chr);
                }
            }
            boolean idle = ev.getMemberCount() == 0 && now - ev.lastActivity() > IDLE_INSTANCED_MS;
            boolean expired = now - ev.getCreatedAt() > MAX_INSTANCED_MS;
            if (idle || expired) {
                log(ev, idle ? "idle, destroying" : "lifetime exceeded, destroying");
                ev.destroyEvent();
                n++;
            }
        }
        return n;
    }

    void log(EventInstance ev, String msg) {
        System.out.println("[Event] " + (ev == null ? "" : ev.getName() + "#" + ev.getId() + " ") + msg);
    }

    // ---- helpers -----------------------------------------------------------------------------------------------------

    /** Flattens ints / numbers / Java arrays / iterables / script arrays into field ids. */
    static int[] toIntArray(Object o) {
        List<Integer> out = new ArrayList<>();
        collectInts(o, out);
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = out.get(i);
        }
        return r;
    }

    private static void collectInts(Object o, List<Integer> out) {
        if (o == null) {
            return;
        }
        if (o instanceof Number n) {
            out.add(n.intValue());
        } else if (o instanceof CharSequence s) {
            try {
                out.add(Integer.parseInt(s.toString().trim()));
            } catch (NumberFormatException ignored) {
                // not a map id
            }
        } else if (o instanceof int[] arr) {
            for (int v : arr) {
                out.add(v);
            }
        } else if (o instanceof Object[] arr) {
            for (Object v : arr) {
                collectInts(v, out);
            }
        } else if (o instanceof Map<?, ?> m) { // script arrays
            for (Object v : m.values()) {
                collectInts(v, out);
            }
        } else if (o instanceof Iterable<?> it) {
            for (Object v : it) {
                collectInts(v, out);
            }
        }
    }
}
