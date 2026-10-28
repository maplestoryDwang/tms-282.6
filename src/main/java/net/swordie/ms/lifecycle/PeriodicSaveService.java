package net.swordie.ms.lifecycle;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.util.HotProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Periodic save of every online character (health audit 2026-10-01: EXP, mesos and quest progress were only written
 * at logout, cash-shop entry or channel change, so a crash, a forced stop or a Windows restart lost a whole session).
 * <p>
 * Every character gets its own due time, a random point in {@code [save.periodic.minSec, save.periodic.maxSec]}
 * (default 300..600 s, i.e. 5..10 minutes) after it was first seen online and after each save, so saves are spread
 * out instead of all landing on one tick; at most {@code save.periodic.maxPerTick} (default 1) character is saved
 * per {@value #TICK_MS} ms tick on this service's own thread (never on a Netty I/O thread). A failed save is logged in
 * one line and retried at the next due time. {@code save.periodic.enabled=false} pauses it (hot reloaded, as are the
 * other keys).
 */
public final class PeriodicSaveService {
    public static final long TICK_MS = 5_000;
    public static final String ENABLED = "save.periodic.enabled";
    public static final String MIN_SEC = "save.periodic.minSec";
    public static final String MAX_SEC = "save.periodic.maxSec";
    public static final String MAX_PER_TICK = "save.periodic.maxPerTick";

    private static volatile PeriodicSaveService running;

    private final Supplier<Collection<Char>> online;
    private final Function<Char, OnlineCharacterSaver.Result> saver;
    private final HotProperties settings;
    private final LongSupplier clock;
    private final Map<Integer, Long> due = new HashMap<>();
    private final Map<Integer, Char> seen = new HashMap<>();
    private volatile ScheduledExecutorService executor;
    private volatile boolean stopped;
    private long saved;
    private long failed;

    public PeriodicSaveService(Supplier<Collection<Char>> online, Function<Char, OnlineCharacterSaver.Result> saver,
                               HotProperties settings, LongSupplier clock) {
        this.online = Objects.requireNonNull(online, "online");
        this.saver = Objects.requireNonNull(saver, "saver");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The server's service: online characters of Server.get(), real saves, config/server.properties. */
    public static synchronized PeriodicSaveService start() {
        if (running != null) {
            return running;
        }
        PeriodicSaveService service = new PeriodicSaveService(() -> Server.get().getChars().values(),
                OnlineCharacterSaver::save, HotProperties.server(), System::currentTimeMillis);
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PeriodicSave");
            t.setDaemon(true);
            return t;
        });
        service.executor = ex;
        ex.scheduleWithFixedDelay(service::tickSafely, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
        running = service;
        int[] range = service.range();
        System.out.printf("[PeriodicSave] started: every online character is saved every %d..%d s (%s=%s, at most %d per %d ms)%n",
                range[0], range[1], ENABLED, service.enabled(), service.maxPerTick(), TICK_MS);
        // step 6: which lock a save takes (EconomyLocks): the per-character save gate, or the old global monitor
        System.out.println(net.swordie.ms.client.character.tms282.EconomyLocks.saveOutsideEconomy()
                ? "[PeriodicSave] save lock: save gate -> Char (ECONOMY_MONITOR not held; tms282.save.outsideEconomy=true)"
                : "[PeriodicSave] save lock: ECONOMY_MONITOR -> Char (tms282.save.outsideEconomy=false)");
        return service;
    }

    public static PeriodicSaveService running() {
        return running;
    }

    /** Stops scheduling; a save already running finishes (it is tracked by {@link PendingSaves}). */
    public void stop() {
        stopped = true;
        ScheduledExecutorService ex = executor;
        if (ex != null) {
            ex.shutdown();
        }
    }

    public boolean enabled() {
        return settings.bool(ENABLED, true);
    }

    /** [min, max] seconds. */
    int[] range() {
        int min = settings.integer(MIN_SEC, 300, 10, 86_400);
        int max = settings.integer(MAX_SEC, 600, 10, 86_400);
        return new int[]{min, Math.max(min, max)};
    }

    int maxPerTick() {
        return settings.integer(MAX_PER_TICK, 1, 1, 100);
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Throwable t) {
            System.out.println("[PeriodicSave] tick failed: " + t);
        }
    }

    /** One scheduling round; returns how many characters were saved. Package-private for checks. */
    synchronized int tick() {
        if (stopped) {
            return 0;
        }
        long now = clock.getAsLong();
        Set<Integer> present = new HashSet<>();
        List<Char> dueNow = new ArrayList<>();
        for (Char chr : List.copyOf(online.get())) {
            if (chr == null || chr.isBot()) {
                continue;
            }
            int id = chr.getId();
            present.add(id);
            if (seen.get(id) != chr) {
                // first seen (or a new login of the same id): first save one interval from now
                seen.put(id, chr);
                due.put(id, now + nextDelayMs());
                continue;
            }
            if (due.getOrDefault(id, Long.MAX_VALUE) <= now) {
                dueNow.add(chr);
            }
        }
        due.keySet().retainAll(present);
        seen.keySet().retainAll(present);
        if (!enabled() || dueNow.isEmpty()) {
            return 0;
        }
        dueNow.sort((a, b) -> Long.compare(due.get(a.getId()), due.get(b.getId())));
        int limit = maxPerTick();
        int done = 0;
        for (Char chr : dueNow) {
            if (done >= limit || stopped) {
                break;
            }
            OnlineCharacterSaver.Result[] result = {OnlineCharacterSaver.Result.FAILED};
            long started = System.nanoTime();
            PendingSaves.track(() -> result[0] = saver.apply(chr)).run();
            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            switch (result[0]) {
                case SAVED -> {
                    saved++;
                    done++;
                    SaveHistory.standard().record(chr.getId(), chr.getName(), ms, true, clock.getAsLong());
                    System.out.printf("[PeriodicSave] saved character %d (%s) in %d ms%n", chr.getId(), chr.getName(), ms);
                    due.put(chr.getId(), clock.getAsLong() + nextDelayMs());
                }
                case FAILED -> {
                    failed++;
                    done++;
                    SaveHistory.standard().record(chr.getId(), chr.getName(), ms, false, clock.getAsLong());
                    due.put(chr.getId(), clock.getAsLong() + nextDelayMs());
                }
                case NOT_IN_FIELD -> due.put(chr.getId(), clock.getAsLong() + 30_000); // cash shop / changing channel: soon again
                case OFFLINE -> {
                    due.remove(chr.getId());
                    seen.remove(chr.getId());
                }
            }
        }
        return done;
    }

    private long nextDelayMs() {
        int[] r = range();
        long min = r[0] * 1000L;
        long max = r[1] * 1000L;
        return max <= min ? min : ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    /** Due time of a character (checks), or null. */
    synchronized Long dueOf(int characterId) {
        return due.get(characterId);
    }

    /** Periodic saves that succeeded / failed since start (admin console). */
    public synchronized long savedCount() {
        return saved;
    }

    public synchronized long failedCount() {
        return failed;
    }

    /** Characters currently scheduled (admin console). */
    public synchronized int trackedCount() {
        return due.size();
    }

    /** The configured [min, max] seconds between two saves of one character (admin console). */
    public int[] intervalSec() {
        return range();
    }

    public synchronized String summary() {
        return "tracked=" + due.size() + " saved=" + saved + " failed=" + failed;
    }
}
