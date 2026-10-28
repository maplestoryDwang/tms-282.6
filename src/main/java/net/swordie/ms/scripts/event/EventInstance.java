package net.swordie.ms.scripts.event;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;

import javax.script.Invocable;
import javax.script.ScriptException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntFunction;

/**
 * One running MetaMS event ({@code data/scripts/event/<name>.js}). This object is the script's global
 * {@code event}; its public methods are the MetaMS event API.
 *
 * <p>Callbacks the script may define: {@code init(attachment)}, {@code timerExpired(key)}, {@code mobDied(mob)},
 * {@code mobHit(player, mob, damage)}, {@code playerChangedMap(player, destination)},
 * {@code playerDisconnected(player)}, {@code partyMemberDischarged(party, player)}, {@code deinit()}.
 * All calls into one instance are serialized by a reentrant lock (Nashorn engines are not thread-safe).</p>
 */
public final class EventInstance {

    private final EventManager manager;
    private final int id;
    private final String name;
    private final int channelId;
    private final long createdAt = System.currentTimeMillis();
    private final ReentrantLock lock = new ReentrantLock();
    private final ArrayDeque<Runnable> callbacks = new ArrayDeque<>();
    private Thread callbackOwner;
    private boolean callbackScheduled;
    private final Map<String, Object> variables = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    private final Map<Integer, Field> ownedFields = new ConcurrentHashMap<>();
    private final Map<Integer, Char> members = new ConcurrentHashMap<>();
    private final Set<Integer> registeredUsers = ConcurrentHashMap.newKeySet();
    private final IntFunction<Field> channelFields;
    private final IntFunction<Field> fieldFactory;
    private final Char owner;
    private volatile Invocable script;
    private volatile EventFieldInstance fieldInstance;
    private volatile boolean destroyed;
    private volatile boolean practiceMode;
    private volatile int exitFieldId;
    private volatile long lastActivity = System.currentTimeMillis();

    EventInstance(EventManager manager, int id, String name, int channelId, Char owner,
                  IntFunction<Field> channelFields, IntFunction<Field> fieldFactory, int exitFieldId) {
        this.manager = manager;
        this.id = id;
        this.name = name;
        this.channelId = channelId;
        this.owner = owner;
        this.channelFields = channelFields;
        this.fieldFactory = fieldFactory;
        this.exitFieldId = exitFieldId;
    }

    void attachScript(Invocable script) {
        this.script = script;
    }

    EventManager manager() {
        return manager;
    }

    Char owner() {
        return owner;
    }

    // ---- identity --------------------------------------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** MetaMS instance name (event name + '_' + id); scripts strip the trailing underscore part. */
    public String getInstanceName() {
        return name + "_";
    }

    public int getChannelId() {
        return channelId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public boolean isPracticeMode() {
        return practiceMode;
    }

    public void setPracticeMode(boolean practiceMode) {
        this.practiceMode = practiceMode;
    }

    public int getExitMap() {
        return exitFieldId;
    }

    /** Map members are sent to when the event is destroyed while they are still inside an event map. */
    public void setExitMap(int fieldId) {
        this.exitFieldId = fieldId;
        EventFieldInstance fi = fieldInstance;
        if (fi != null) {
            fi.setForcedReturn(fieldId); // death / warpInstanceOut(-1) destination
        }
    }

    // ---- variables -------------------------------------------------------------------------------------------------

    public void setVariable(String key, Object value) {
        if (key == null) {
            return;
        }
        if (value == null) {
            variables.remove(key);
        } else {
            variables.put(key, value);
        }
    }

    public Object getVariable(String key) {
        return key == null ? null : variables.get(key);
    }

    public Object removeVariable(String key) {
        return key == null ? null : variables.remove(key);
    }

    public boolean hasVariable(String key) {
        return key != null && variables.containsKey(key);
    }

    /** {@code event.clearVariables()} (GM event scripts). */
    public void clearVariables() {
        variables.clear();
    }

    public Set<String> getVariableKeys() {
        synchronized (variables) {
            return new LinkedHashSet<>(variables.keySet());
        }
    }

    // ---- timers ----------------------------------------------------------------------------------------------------

    /** Calls {@code timerExpired(key)} after {@code delayMillis}; restarting a running key replaces it. */
    public void startTimer(String key, double delayMillis) {
        if (destroyed || key == null) {
            return;
        }
        final Object[] self = new Object[1];
        Runnable task = () -> {
            ScheduledFuture<?> mine;
            synchronized (self) {
                mine = (ScheduledFuture<?>) self[0];
            }
            if (mine != null) {
                timers.remove(key, mine);
            } else {
                timers.remove(key);
            }
            if (!destroyed) {
                manager.dispatchCallback(this, null, () -> invoke("timerExpired", key));
            }
        };
        ScheduledFuture<?> future;
        synchronized (self) {
            future = manager.scheduler().schedule(task, (long) Math.max(0, delayMillis));
            self[0] = future;
        }
        ScheduledFuture<?> previous = timers.put(key, future);
        if (previous != null && previous != future) {
            previous.cancel(false);
        }
    }

    public void stopTimer(String key) {
        ScheduledFuture<?> f = key == null ? null : timers.remove(key);
        if (f != null) {
            f.cancel(false);
        }
    }

    public boolean isTimerRunning(String key) {
        ScheduledFuture<?> f = key == null ? null : timers.get(key);
        return f != null && !f.isDone();
    }

    public long getTimeLeft(String key) {
        ScheduledFuture<?> f = key == null ? null : timers.get(key);
        return f == null ? 0 : Math.max(0, f.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    /** Alias used by the GM event scripts ({@code event.stopTimers()}). */
    public void stopTimers() {
        stopAllTimers();
    }

    public void stopAllTimers() {
        for (String key : new ArrayList<>(timers.keySet())) {
            stopTimer(key);
        }
    }

    // ---- maps ------------------------------------------------------------------------------------------------------

    /** Creates this event's own copies of the given maps (members bound to the event warp into these copies). */
    public void initMap(Object... fieldIds) {
        if (fieldIds == null) {
            return;
        }
        for (Object o : fieldIds) {
            for (int fid : EventManager.toIntArray(o)) {
                ensureOwned(fid);
            }
        }
    }

    /** Event-owned copy of {@code fieldId} when it was initialised, otherwise the public channel map. */
    public EventMap getMap(int fieldId) {
        Field owned = ownedFields.get(fieldId);
        if (owned != null) {
            return new EventMap(this, owned, true);
        }
        Field pub = channelField(fieldId);
        return pub == null ? null : new EventMap(this, pub, false);
    }

    /** Like {@link #getMap(int)} but always an event-owned copy. */
    public EventMap getInstanceMap(int fieldId) {
        Field f = ensureOwned(fieldId);
        return f == null ? null : new EventMap(this, f, true);
    }

    public boolean isEventMap(int fieldId) {
        return ownedFields.containsKey(fieldId);
    }

    public List<Integer> getMapIds() {
        return new ArrayList<>(ownedFields.keySet());
    }

    private Field ensureOwned(int fieldId) {
        Field f = ownedFields.get(fieldId);
        if (f != null) {
            return f;
        }
        synchronized (ownedFields) {
            f = ownedFields.get(fieldId);
            if (f == null) {
                if (!net.swordie.ms.connection.protocol.codec.FieldAvailability.active().isAvailable(fieldId)) {
                    manager.log(this, "initMap: map " + fieldId + " is unavailable in the active client");
                    return null;
                }
                f = fieldFactory.apply(fieldId);
                if (f == null) {
                    manager.log(this, "initMap: map " + fieldId + " is not in the server data");
                    return null;
                }
                ownedFields.put(fieldId, f);
                manager.registerOwnedField(this, f);
            }
        }
        EventFieldInstance inst = fieldInstance();
        for (Char chr : members.values()) {
            if (chr.getInstance() != inst) {
                chr.setInstance(inst);
            }
        }
        return f;
    }

    Field ownedField(int fieldId) {
        return ownedFields.get(fieldId);
    }

    Map<Integer, Field> ownedFields() {
        return ownedFields;
    }

    boolean hasOwnedFields() {
        return !ownedFields.isEmpty();
    }

    boolean owns(Field field) {
        return field != null && ownedFields.get(field.getId()) == field;
    }

    Field channelField(int fieldId) {
        return channelFields == null ? null : channelFields.apply(fieldId);
    }

    EventFieldInstance fieldInstance() {
        EventFieldInstance fi = fieldInstance;
        if (fi == null) {
            synchronized (this) {
                if (fieldInstance == null) {
                    fieldInstance = new EventFieldInstance(this, owner);
                    fieldInstance.setForcedReturn(exitFieldId);
                    manager.registerFieldInstance(fieldInstance);
                }
                fi = fieldInstance;
            }
        }
        return fi;
    }

    /** Event-level mob spawn on an event-owned (or public) map; the event gets its {@code mobDied}. */
    public EventMob spawnMob(int fieldId, int mobId, int x, int y) {
        EventMap map = getMap(fieldId);
        return map == null ? null : map.spawnMob(mobId, x, y);
    }

    /** Removes a mob this event spawned (no {@code mobDied}); false when it is already gone. */
    public boolean removeMob(EventMob mob) {
        if (mob == null) {
            return false;
        }
        Mob m = mob.getMob();
        manager.ignoreMob(m);
        Field f = m.getField();
        if (f == null || f.getLifeByObjectID(m.getObjectId()) != m) {
            return false;
        }
        m.removeWithAnimation();
        return true;
    }

    /** True while the mob this event spawned is still alive on its map. */
    public boolean isMobAlive(EventMob mob) {
        if (mob == null) {
            return false;
        }
        Mob m = mob.getMob();
        Field f = m.getField();
        return f != null && f.getLifeByObjectID(m.getObjectId()) == m && m.getHp() > 0;
    }

    /** Runs content script {@code path} for {@code player} on its own thread (NPC dialogs, reward scripts). */
    public void runScript(Object player, String path) {
        if (player != null && path != null) {
            manager.host().runScriptAsync(player, path);
        }
    }

    void watchMob(Mob mob) {
        manager.watchMob(mob, this);
    }

    void ignoreMob(Mob mob) {
        manager.ignoreMob(mob);
    }

    // ---- members ---------------------------------------------------------------------------------------------------

    void addMember(Char chr) {
        members.put(chr.getId(), chr);
        if (hasOwnedFields()) {
            EventFieldInstance inst = fieldInstance();
            inst.getChars().put(chr.getId(), chr);
            if (chr.getInstance() != inst) {
                chr.setInstance(inst);
            }
        }
        touch();
    }

    void removeMember(Char chr) {
        members.remove(chr.getId());
        EventFieldInstance inst = fieldInstance;
        if (inst != null) {
            inst.getChars().remove(chr.getId());
            if (chr.getInstance() == inst) {
                chr.setInstance(null);
            }
        }
        touch();
    }

    /** Script objects of the characters currently bound to this event. */
    public List<Object> getMembers() {
        List<Object> out = new ArrayList<>();
        for (Char chr : members.values()) {
            out.add(manager.host().player(chr));
        }
        return out;
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isMember(int charId) {
        return members.containsKey(charId);
    }

    Collection<Char> memberChars() {
        return new ArrayList<>(members.values());
    }

    public void registerUser(int charId) {
        registeredUsers.add(charId);
    }

    public boolean isRegisteredUser(int charId) {
        return registeredUsers.contains(charId);
    }

    public List<Integer> getRegisteredUsers() {
        return new ArrayList<>(registeredUsers);
    }

    /** MetaMS boss-clear bookkeeping: logs each registered online user's clear through the player API. */
    public void updateAllUserBossClear(Object bossId, boolean clear) {
        if (!clear || practiceMode) {
            return;
        }
        for (Char chr : members.values()) {
            if (registeredUsers.contains(chr.getId())) {
                manager.log(this, "boss clear " + bossId + " for " + chr.getId());
            }
        }
    }

    /** Chat line to every member. */
    public void broadcastMessage(String msg) {
        for (Char chr : members.values()) {
            chr.chatMessage(msg);
        }
    }

    public void broadcastMessage(int type, String msg) {
        broadcastMessage(msg);
    }

    // ---- lifecycle -------------------------------------------------------------------------------------------------

    /** Ends the event: {@code deinit()}, timers, members (warped out of event maps), map copies. */
    public void destroyEvent() {
        if (destroyed) return;
        manager.dispatchCallback(this, null, this::destroyEventNow);
    }

    private void destroyEventNow() {
        List<Char> leaving;
        lock.lock();
        try {
            if (destroyed) {
                return;
            }
            leaving = new ArrayList<>(members.values());
            destroyed = true;
            invokeUnchecked("deinit");
        } finally {
            lock.unlock();
        }
        stopAllTimers();
        for (Char chr : leaving) {
            boolean inside = owns(chr.getField());
            manager.unbind(chr, this);
            if (inside && chr.isOnline() && exitFieldId > 0) {
                try {
                    chr.warp(exitFieldId, 0);
                } catch (RuntimeException e) {
                    manager.log(this, "exit warp of " + chr.getId() + " failed: " + e);
                }
            }
        }
        for (Field f : ownedFields.values()) {
            try {
                for (Mob mob : new ArrayList<>(f.getMobs())) {
                    manager.ignoreMob(mob);
                }
                f.clear();
            } catch (RuntimeException ignored) {
                // map copy teardown is best-effort
            }
        }
        manager.remove(this);
    }

    /** Alias used by a few MetaMS scripts. */
    public void dispose() {
        destroyEvent();
    }

    void touch() {
        lastActivity = System.currentTimeMillis();
    }

    long lastActivity() {
        return lastActivity;
    }

    // ---- script calls ----------------------------------------------------------------------------------------------

    /** A packet holding an economy/character lock must never wait for this event's script lock. */
    void dispatchCallback(Runnable callback, boolean defer) {
        boolean direct = false, nested = false, schedule = false;
        synchronized (callbacks) {
            if (!defer && callbackOwner == Thread.currentThread() && callbacks.isEmpty()) {
                direct = nested = true;
            } else if (!defer && callbackOwner == null && !callbackScheduled && callbacks.isEmpty()) {
                callbackOwner = Thread.currentThread();
                direct = true;
            } else {
                callbacks.add(callback);
                if (callbackOwner == null && !callbackScheduled) callbackScheduled = schedule = true;
            }
        }
        if (direct) {
            try {
                callback.run();
            } finally {
                if (!nested) {
                    synchronized (callbacks) {
                        callbackOwner = null;
                        if (!callbacks.isEmpty() && !callbackScheduled) callbackScheduled = schedule = true;
                    }
                }
            }
        }
        if (schedule) manager.scheduler().schedule(this::drainCallbacks, 0);
    }

    private void drainCallbacks() {
        synchronized (callbacks) {
            callbackScheduled = false;
            callbackOwner = Thread.currentThread();
        }
        while (true) {
            Runnable callback;
            synchronized (callbacks) {
                callback = callbacks.poll();
                if (callback == null) {
                    callbackOwner = null;
                    return;
                }
            }
            try {
                callback.run();
            } catch (RuntimeException | LinkageError e) {
                manager.log(this, "queued callback failed: " + e);
            }
        }
    }

    /** Calls {@code fn} if the script defines it. Script errors are logged, never thrown to the game thread. */
    public Object invoke(String fn, Object... args) {
        if (destroyed && !"deinit".equals(fn)) {
            return null;
        }
        return invokeUnchecked(fn, args);
    }

    private Object invokeUnchecked(String fn, Object... args) {
        Invocable inv = script;
        if (inv == null) {
            return null;
        }
        lock.lock();
        EventManager.enterCallback();
        try {
            touch();
            return inv.invokeFunction(fn, args);
        } catch (NoSuchMethodException missing) {
            return null;
        } catch (ScriptException | RuntimeException e) {
            manager.log(this, fn + " failed: " + e);
            if (manager.isVerbose()) {
                e.printStackTrace();
            }
            return null;
        } finally {
            EventManager.exitCallback();
            lock.unlock();
        }
    }

    /** Like {@link #invoke} but reports a missing function or a script error to the caller ({@code init}). */
    Object invokeChecked(String fn, Object... args) throws ScriptException, NoSuchMethodException {
        Invocable inv = script;
        if (inv == null) {
            throw new NoSuchMethodException(fn);
        }
        lock.lock();
        EventManager.enterCallback();
        try {
            touch();
            return inv.invokeFunction(fn, args);
        } finally {
            EventManager.exitCallback();
            lock.unlock();
        }
    }

    EventFieldInstance fieldInstanceIfCreated() {
        return fieldInstance;
    }

    /** True when the script defines function {@code fn}. */
    public boolean hasFunction(String fn) {
        Invocable inv = script;
        if (inv instanceof javax.script.ScriptEngine se) {
            return se.get(fn) instanceof org.openjdk.nashorn.api.scripting.ScriptObjectMirror m && m.isFunction();
        }
        return false;
    }

    @Override
    public String toString() {
        return "EventInstance[" + name + "#" + id + ",ch" + channelId + ",members=" + members.keySet()
                + ",maps=" + ownedFields.keySet() + (destroyed ? ",destroyed" : "") + "]";
    }
}
