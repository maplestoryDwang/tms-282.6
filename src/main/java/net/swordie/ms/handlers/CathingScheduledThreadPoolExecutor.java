package net.swordie.ms.handlers;

import net.swordie.ms.util.DataPrinter;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class CathingScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private static final ThreadFactory platformThreadFactory = Thread.ofPlatform()
            .name("Timer-Platform-Thread-", 0)
            .factory();

    public CathingScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, platformThreadFactory);
    }

    @Override
    public void execute(Runnable command) {
        super.execute(wrapRunnable(command));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(wrapRunnable(command), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
    }

    private Runnable wrapRunnable(Runnable command) {
        return new LogOnExceptionRunnable(command);
    }

    private class LogOnExceptionRunnable implements Runnable {
        private final Runnable runnable;

        public LogOnExceptionRunnable(Runnable runnable) {
            super();
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Exception e) {
                DataPrinter.send("ExceptionCaught/EventManager.txt", e);
            }
        }
    }
}
