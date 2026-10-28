package net.swordie.ms.lifecycle;

/**
 * Startup of the step-2 lifecycle pieces (health audit 2026-10-01), called by {@code Server.init}:
 * the JVM shutdown hook first (right after the timers exist), the periodic save and the stop-file watcher once the
 * network is up. Each piece starts once; a failure is logged and never stops the server from starting.
 */
public final class ServerLifecycle {
    private ServerLifecycle() {
    }

    public static void installShutdownHook() {
        try {
            ServerShutdown.installHook();
        } catch (Throwable t) {
            System.out.println("[Shutdown] could not install the shutdown hook: " + t);
        }
    }

    public static void startAfterNetwork() {
        try {
            net.swordie.ms.connection.netty.GameSourceGate.standard().allowlist(); // logs the game-port allowlist once
        } catch (Throwable t) {
            System.out.println("[NetAllow] cannot read net.allow: " + t);
        }
        try {
            PeriodicSaveService.start();
        } catch (Throwable t) {
            System.out.println("[PeriodicSave] could not start: " + t);
        }
        try {
            StopFileWatcher.start();
        } catch (Throwable t) {
            System.out.println("[Shutdown] could not start the stop-file watcher: " + t);
        }
    }
}
