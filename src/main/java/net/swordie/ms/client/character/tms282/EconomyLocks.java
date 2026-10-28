package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.console.config.ServerRuntimeConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * The one lock order of the 282 server (multiplayer-0930): {@link Tms282VMatrixRuntime#ECONOMY_MONITOR} first, then
 * the Char monitor; two characters in ascending character id. Tms282Routes wraps every channel packet in
 * ECONOMY_MONITOR -> actor, and Char.addItemToInventory / addMoney / consumeItem take the same pair, so a
 * thread that holds a Char monitor first (script virtual threads, timers) and then reaches one of them deadlocks
 * with that character's next packet - and, the monitor being global, freezes every player (BossRun 2026-09-29,
 * research/tms2823/pickup-stuck-0929). Both monitors are re-entrant, so this is safe on packet threads too.
 * <p>
 * <b>Save gate (step 6, 2026-10-04).</b> A character save runs about 450 SQL statements (about 1 s). Under
 * ECONOMY_MONITOR that stalled every channel. Now a save started by a thread that holds neither ECONOMY_MONITOR nor
 * the character (periodic save, logout save) takes only
 * <pre>  save gate(id) -> Char monitor</pre>
 * and never ECONOMY_MONITOR. The full order is therefore
 * <pre>  save gate(id) -> ECONOMY_MONITOR -> Char (lower id first) -> User / Account</pre>
 * <ul>
 *     <li>The gate is a per-character-id fair {@link ReentrantLock}. It is only ever taken by a thread that holds no
 *     ECONOMY_MONITOR and no Char monitor ({@link #mayTakeGate}), so it is always the outermost lock: a gate can
 *     never be part of a cycle with the monitors.</li>
 *     <li>The channel packet wrapper takes gate(actor) before ECONOMY_MONITOR -> actor ({@link #packet}): while a
 *     character is being saved, its own packets wait on the gate - holding nothing - instead of waiting on the Char
 *     monitor while holding the global ECONOMY_MONITOR. Other characters' packets run on.</li>
 *     <li>The save holds the Char monitor for the whole write, so every change made under ECONOMY_MONITOR -> chr
 *     (the inventory / meso primitives, trades, parcels, cube / guild journals) waits for it: the saved snapshot of
 *     one character is consistent exactly as before. Code holding ECONOMY_MONITOR that reaches a character being
 *     saved waits for that one save (rare: trade partner, guild kick).</li>
 *     <li>The save path never takes ECONOMY_MONITOR (Char.saveCharacterState, User / Account saves: checked from the
 *     bytecode by tools/Tms282SaveLockCheck). Saves started inside ECONOMY_MONITOR (cash shop entry, guild create /
 *     join, coupon expansion) keep the old ECONOMY_MONITOR -> chr path; so does a thread that already holds the Char
 *     monitor without the gate (an old inverted path: unchanged behaviour).</li>
 *     <li>Loading a character (Tms282FieldEntry: journal recovery + loadCharacterData) and deleting one take the
 *     gate of its id first, so they wait for a logout save of the same character that is still running.</li>
 * </ul>
 * {@code tms282.save.outsideEconomy=false} (server.properties or -D) turns the save back to ECONOMY_MONITOR -> chr
 * (the packet gate stays: on its own it changes nothing).
 */
public final class EconomyLocks {
    public static final String SAVE_OUTSIDE_ECONOMY = "tms282.save.outsideEconomy";
    private static final ConcurrentHashMap<Integer, ReentrantLock> GATES = new ConcurrentHashMap<>();

    private EconomyLocks() {
    }

    /** A body that may throw (packet handlers, SQL). */
    @FunctionalInterface
    public interface Body<T, E extends Exception> {
        T run() throws E;
    }

    /** ECONOMY_MONITOR -> chr, then {@code action}. */
    public static void run(Char chr, Runnable action) {
        synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            synchronized (chr) {
                action.run();
            }
        }
    }

    /** ECONOMY_MONITOR -> chr, then {@code action}; returns its result. */
    public static <T> T call(Char chr, Supplier<T> action) {
        synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            synchronized (chr) {
                return action.get();
            }
        }
    }

    /** ECONOMY_MONITOR -> the lower id -> the higher id, then {@code action}. */
    public static void run(Char a, Char b, Runnable action) {
        if (a == null || b == null || a == b) {
            run(a != null ? a : b, action);
            return;
        }
        Char first = a.getId() <= b.getId() ? a : b;
        Char second = first == a ? b : a;
        synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            synchronized (first) {
                synchronized (second) {
                    action.run();
                }
            }
        }
    }

    /** ECONOMY_MONITOR -> the lower id -> the higher id, then {@code action}; returns its result. */
    public static <T> T call(Char a, Char b, Supplier<T> action) {
        if (a == null || b == null || a == b) {
            return call(a != null ? a : b, action);
        }
        Char first = a.getId() <= b.getId() ? a : b;
        Char second = first == a ? b : a;
        synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) {
            synchronized (first) {
                synchronized (second) {
                    return action.get();
                }
            }
        }
    }

    /**
     * True when the current thread holds a Char monitor without ECONOMY_MONITOR (an inverted order); used by checks
     * and to log a warning. Holding the character's save gate makes the Char monitor alone legal (the save path).
     */
    public static boolean inverted(Char chr) {
        return chr != null && Thread.holdsLock(chr) && !Thread.holdsLock(Tms282VMatrixRuntime.ECONOMY_MONITOR)
                && !gateHeld(chr.getId());
    }

    // ------------------------------------------------------------------ save gate

    /** Saves skip ECONOMY_MONITOR (default true; {@code tms282.save.outsideEconomy=false} restores the old path). */
    public static boolean saveOutsideEconomy() {
        String v = System.getProperty(SAVE_OUTSIDE_ECONOMY);
        if (v == null) {
            v = ServerRuntimeConfig.stringValue(SAVE_OUTSIDE_ECONOMY);
        }
        return v == null || v.isBlank() || !v.trim().equalsIgnoreCase("false");
    }

    static ReentrantLock gate(int charId) {
        return GATES.computeIfAbsent(charId, k -> new ReentrantLock(true));
    }

    /** The current thread holds the save gate of {@code charId}. */
    public static boolean gateHeld(int charId) {
        ReentrantLock g = GATES.get(charId);
        return g != null && g.isHeldByCurrentThread();
    }

    /** A thread may take a gate only while it holds no ECONOMY_MONITOR and not {@code chr}'s monitor (outermost). */
    static boolean mayTakeGate(Char chr) {
        return !Thread.holdsLock(Tms282VMatrixRuntime.ECONOMY_MONITOR) && (chr == null || !Thread.holdsLock(chr));
    }

    /**
     * Runs {@code body} holding the save gate of {@code charId} when the current thread may take it (holds no
     * ECONOMY_MONITOR); otherwise runs it as it is. For loading and deleting a character.
     */
    public static <T, E extends Exception> T withGate(int charId, Body<T, E> body) throws E {
        if (charId <= 0 || Thread.holdsLock(Tms282VMatrixRuntime.ECONOMY_MONITOR)) {
            return body.run();
        }
        ReentrantLock g = gate(charId);
        g.lock();
        try {
            return body.run();
        } finally {
            g.unlock();
        }
    }

    /**
     * The channel packet wrapper: save gate(actor) (when the thread may take it) -> ECONOMY_MONITOR -> actor, then
     * {@code body}.
     */
    public static <E extends Exception> void packet(Char actor, Body<Void, E> body) throws E {
        ReentrantLock g = actor.getId() > 0 && mayTakeGate(actor) ? gate(actor.getId()) : null;
        if (g != null) {
            g.lock();
        }
        try {
            synchronized (Tms282VMatrixRuntime.ECONOMY_MONITOR) {
                synchronized (actor) {
                    body.run();
                }
            }
        } finally {
            if (g != null) {
                g.unlock();
            }
        }
    }

    /**
     * A character save without ECONOMY_MONITOR: save gate(chr) -> chr, then {@code body}. Returns false - and runs
     * nothing - when that is not allowed here: the switch is off, the thread holds ECONOMY_MONITOR, or it holds the
     * Char monitor without the gate (the caller then uses ECONOMY_MONITOR -> chr as before).
     */
    public static <E extends Exception> boolean saveWithoutEconomy(Char chr, Body<Void, E> body) throws E {
        if (chr == null || chr.getId() <= 0 || !saveOutsideEconomy()
                || Thread.holdsLock(Tms282VMatrixRuntime.ECONOMY_MONITOR)) {
            return false;
        }
        int id = chr.getId();
        if (gateHeld(id)) {
            synchronized (chr) {
                body.run();
            }
            return true;
        }
        if (Thread.holdsLock(chr)) {
            return false;
        }
        ReentrantLock g = gate(id);
        g.lock();
        try {
            synchronized (chr) {
                body.run();
            }
        } finally {
            g.unlock();
        }
        return true;
    }

    /** Gates created so far (monitoring / tests). */
    public static int gateCount() {
        return GATES.size();
    }
}
