package net.swordie.ms.lifecycle;

import java.util.concurrent.TimeUnit;

/**
 * Character saves that were queued but have not finished yet (logout saves handed to the server timer by
 * {@code Client.finalizeSession}, periodic saves). Shutdown waits on {@link #awaitIdle} so the JVM does not exit
 * with a save still in the queue - the GUI used to {@code System.exit} right after {@code shutdownOnlineClients}
 * returned, while the saves were still waiting in the server timer.
 * <p>
 * A task that is dropped without running (Timer.restart uses shutdownNow) never decrements; {@link #awaitIdle} is
 * always bounded, so that costs at most the timeout.
 */
public final class PendingSaves {
    private static final Object LOCK = new Object();
    private static int pending;
    private static long started;
    private static long finished;
    private static long failed;

    private PendingSaves() {
    }

    /**
     * Counts {@code task} as pending from now until it has run. Pass the returned runnable to the executor; when the
     * executor refuses it, call {@link #abandon()} (or run it inline).
     */
    public static Runnable track(Runnable task) {
        synchronized (LOCK) {
            pending++;
            started++;
        }
        return () -> {
            boolean ok = false;
            try {
                task.run();
                ok = true;
            } finally {
                synchronized (LOCK) {
                    pending--;
                    finished++;
                    if (!ok) {
                        failed++;
                    }
                    LOCK.notifyAll();
                }
            }
        };
    }

    /** The executor refused a tracked task: it will never run. */
    public static void abandon() {
        synchronized (LOCK) {
            pending--;
            LOCK.notifyAll();
        }
    }

    public static int pending() {
        synchronized (LOCK) {
            return pending;
        }
    }

    /** started / finished / failed since the JVM started (diagnostics). */
    public static String summary() {
        synchronized (LOCK) {
            return "pending=" + pending + " started=" + started + " finished=" + finished + " failed=" + failed;
        }
    }

    /** Waits until nothing is pending or {@code timeoutMs} passed; true when idle. */
    public static boolean awaitIdle(long timeoutMs) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(0, timeoutMs));
        synchronized (LOCK) {
            while (pending > 0) {
                long left = deadline - System.nanoTime();
                if (left <= 0) {
                    return false;
                }
                try {
                    TimeUnit.NANOSECONDS.timedWait(LOCK, left);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return pending <= 0;
                }
            }
            return true;
        }
    }
}
