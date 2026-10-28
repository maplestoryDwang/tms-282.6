package net.swordie.ms.scripts.event;

import net.swordie.ms.Server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Where event timers run. Production uses the server event timer; offline checks inject a manual clock.
 */
public interface EventScheduler {

    ScheduledFuture<?> schedule(Runnable task, long delayMillis);

    /** Server event timer, or a private daemon scheduler when the server timer is not running (tools). */
    static EventScheduler server() {
        return new EventScheduler() {
            private volatile ScheduledExecutorService fallback;

            @Override
            public ScheduledFuture<?> schedule(Runnable task, long delayMillis) {
                long delay = Math.max(0, delayMillis);
                try {
                    Server server = Server.get();
                    if (server != null && server.getEventTimer() != null) {
                        ScheduledFuture<?> f = server.getEventTimer().addEvent(task, delay, TimeUnit.MILLISECONDS);
                        if (f != null) {
                            return f;
                        }
                    }
                } catch (RuntimeException | LinkageError ignored) {
                    // tools without a started server
                }
                return fallback().schedule(task, delay, TimeUnit.MILLISECONDS);
            }

            private ScheduledExecutorService fallback() {
                ScheduledExecutorService s = fallback;
                if (s == null) {
                    synchronized (this) {
                        if (fallback == null) {
                            fallback = Executors.newSingleThreadScheduledExecutor(r -> {
                                Thread t = new Thread(r, "event-timer");
                                t.setDaemon(true);
                                return t;
                            });
                        }
                        s = fallback;
                    }
                }
                return s;
            }
        };
    }
}
