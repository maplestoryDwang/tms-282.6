package net.swordie.ms.lifecycle;

import net.swordie.ms.Server;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongPredicate;

/**
 * The one graceful shutdown of the server (health audit 2026-10-01, step 2): stop the periodic save, refuse new game
 * connections ({@link #inProgress()}), save and disconnect every online client ({@code Server.shutdownOnlineClients},
 * the GUI's path), then wait - bounded - until every queued character save ({@link PendingSaves}) has run.
 * <p>
 * Reached from the JVM shutdown hook ({@link #installHook()}: System.exit, Ctrl+C, a console close) and from the
 * stop-file watcher ({@link StopFileWatcher}, which exits the JVM afterwards). Runs once; a second caller (the hook
 * after the watcher's System.exit) waits for the first to finish, at most {@link #SECOND_CALLER_WAIT_MS}.
 * {@code Stop-Process} / TerminateProcess skip every hook - the deploy script uses the stop file instead.
 */
public final class ServerShutdown {
    /** Upper bound for the queued saves after the clients were disconnected. */
    public static final long SAVE_WAIT_MS = 60_000;
    public static final long SECOND_CALLER_WAIT_MS = 120_000;

    private static final ServerShutdown STANDARD = new ServerShutdown(
            () -> Server.get().shutdownOnlineClients(), PendingSaves::awaitIdle, PendingSaves::summary);

    private final Runnable disconnectAll;
    private final LongPredicate awaitSaves;
    private final java.util.function.Supplier<String> saveSummary;
    private final AtomicBoolean started = new AtomicBoolean();
    private final CountDownLatch finished = new CountDownLatch(1);
    private volatile boolean savesFinished;
    private static final AtomicBoolean HOOK = new AtomicBoolean();

    ServerShutdown(Runnable disconnectAll, LongPredicate awaitSaves, java.util.function.Supplier<String> saveSummary) {
        this.disconnectAll = Objects.requireNonNull(disconnectAll, "disconnectAll");
        this.awaitSaves = Objects.requireNonNull(awaitSaves, "awaitSaves");
        this.saveSummary = Objects.requireNonNull(saveSummary, "saveSummary");
    }

    public static ServerShutdown get() {
        return STANDARD;
    }

    /** True from the moment a graceful shutdown started: the game ports refuse new connections. */
    public static boolean inProgress() {
        return STANDARD.started.get();
    }

    /** Registers the JVM shutdown hook once. */
    public static void installHook() {
        if (!HOOK.compareAndSet(false, true)) {
            return;
        }
        Thread hook = new Thread(() -> STANDARD.run("JVM shutdown hook"), "GracefulShutdownHook");
        Runtime.getRuntime().addShutdownHook(hook);
        System.out.println("[Shutdown] JVM shutdown hook installed: online characters are saved before the JVM exits"
                + " (not on Stop-Process / TerminateProcess)");
    }

    /**
     * Saves and disconnects everyone; returns true when every queued save finished in time. Never throws.
     */
    public boolean run(String reason) {
        if (!started.compareAndSet(false, true)) {
            try {
                finished.await(SECOND_CALLER_WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return savesFinished;
        }
        long t0 = System.nanoTime();
        try {
            System.out.println("[Shutdown] graceful shutdown started (" + reason + ")");
            PeriodicSaveService periodic = PeriodicSaveService.running();
            if (periodic != null) {
                periodic.stop();
            }
            try {
                disconnectAll.run();
            } catch (Throwable t) {
                System.out.println("[Shutdown] saving / disconnecting the online clients failed: " + t);
            }
            savesFinished = awaitSaves.test(SAVE_WAIT_MS);
            long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t0);
            if (savesFinished) {
                System.out.printf("[Shutdown] complete in %d ms: every character save finished (%s)%n", ms, saveSummary.get());
            } else {
                System.out.printf("[Shutdown] gave up after %d ms: character saves still pending (%s)%n", ms, saveSummary.get());
            }
        } catch (Throwable t) {
            System.out.println("[Shutdown] failed: " + t);
        } finally {
            finished.countDown();
        }
        return savesFinished;
    }

    public boolean started() {
        return started.get();
    }
}
