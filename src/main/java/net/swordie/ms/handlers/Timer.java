package net.swordie.ms.handlers;

import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.BroadcastMsg;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.util.DataPrinter;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Timer {

    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private String name = "";
    private static final int OVERLOAD_THRESHOLD = 1000;
    private ScheduledThreadPoolExecutor scheduler;
    private static ScheduledExecutorService monitorScheduler;
    private final List<RepeatingTask> repeatingTasks = new CopyOnWriteArrayList<>();
    private volatile long lastExecutionTime = 0;
    private ScheduledFuture<?> monitorTask;
    private final AtomicInteger monitorCounter = new AtomicInteger(0);
    private final Object monitorLock = new Object();

    public Timer(String name) {
        long startNow = System.currentTimeMillis();
        this.name = name;
        initScheduler();
        initMonitor();
        startUnifiedMonitor(3, TimeUnit.HOURS);
        System.out.printf("[Timer] Loaded Timer " + name + " in %d ms%n", System.currentTimeMillis() - startNow);
    }

    private void initScheduler() {
        this.scheduler = new CathingScheduledThreadPoolExecutor(THREAD_POOL_SIZE);
        this.scheduler.allowCoreThreadTimeOut(true);
        this.scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    private void initMonitor() {
        synchronized (monitorLock) {
            if (monitorScheduler == null) {
                monitorScheduler = Executors.newSingleThreadScheduledExecutor(
                        Thread.ofPlatform().name(name + "-Monitor-Thread").factory()
                );
            }
        }
    }

    public void startUnifiedMonitor(long period, TimeUnit timeUnit) {
        if (monitorTask != null) {
            monitorTask.cancel(true); // Hủy tác vụ giám sát cũ nếu có
        }
        monitorTask = monitorScheduler.scheduleAtFixedRate(() -> {
            boolean isStuck = false;
            boolean isOverloaded = false;
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - lastExecutionTime;
            long monitorPeriod = timeUnit.toMillis(period);
            if (lastExecutionTime > 0 && elapsed > monitorPeriod * 2) {
                isStuck = true;
            }
            if (scheduler != null && scheduler.getQueue().size() > OVERLOAD_THRESHOLD) {
                isOverloaded = true;
            }
            if (isStuck || isOverloaded) {
                monitorCounter.incrementAndGet();
                if (monitorCounter.get() >= 3) {
                    String reason = isStuck ? "線程卡住" : "隊列過載";
                    System.err.println("[Timer] 檢測到 timer '" + name + "' 因 " + reason + " 卡住，正在重啓...");
                    this.restart();
                    monitorCounter.set(0);
                }
            } else {
                monitorCounter.set(0);
            }
        }, 0, period, timeUnit);
    }

    public synchronized void restart() {
        try {
            if (this.scheduler != null) {
                this.scheduler.shutdownNow();
                this.scheduler = null;
            }
            initScheduler();
            for (RepeatingTask task : repeatingTasks) {
                scheduler.scheduleAtFixedRate(task.task, task.initialDelay, task.delay, task.timeUnit);
            }
            for (Char chr : Server.get().getChars().values()) {
                chr.chatPopup("服務器數據已刷新。如遇異常，請重新登錄。");
            }
            System.err.println("[Timer] timer '" + name + "' 已成功重啓。");
        } catch (Exception e) {
            System.err.println("[Timer] timer '" + name + "' 重啓失敗。");
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e); // 記錄詳細異常
        }
    }

    public String getName() {
        return name;
    }

    private Runnable wrapTask(Runnable originalTask) {
        return () -> {
            this.lastExecutionTime = System.currentTimeMillis();
            originalTask.run();
        };
    }

    public void execute(Runnable runnable) {
        if (scheduler == null) {
            return;
        }
        scheduler.execute(wrapTask(runnable));
    }

    public ScheduledFuture<?> addEvent(Runnable runnable, long delay) {
        if (scheduler == null) {
            return null;
        }
        return scheduler.schedule(wrapTask(runnable), delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> addEvent(Runnable runnable, long delay, TimeUnit timeUnit) {
        if (scheduler == null) {
            return null;
        }
        return scheduler.schedule(wrapTask(runnable), delay, timeUnit);
    }

    public ScheduledFuture<?> addFixedRateEvent(Runnable runnable, long initialDelay, long delay, boolean isSave) {
        if (scheduler == null) {
            return null;
        }
        if (isSave) {
            repeatingTasks.add(new RepeatingTask(runnable, initialDelay, delay, TimeUnit.MILLISECONDS));
        }
        return scheduler.scheduleAtFixedRate(wrapTask(runnable), initialDelay, delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> addFixedRateEvent(Runnable runnable, long initialDelay, long delay, TimeUnit timeUnit, boolean isSave) {
        if (scheduler == null) {
            return null;
        }
        if (isSave) {
            repeatingTasks.add(new RepeatingTask(runnable, initialDelay, delay, timeUnit));
        }
        return scheduler.scheduleAtFixedRate(wrapTask(runnable), initialDelay, delay, timeUnit);
    }

    public ScheduledFuture<?> addFixedRateEvent(Runnable runnable, long initialDelay, long delay, int executes) {
        if (scheduler == null) {
            return null;
        }
        ScheduledFuture<?> sf = scheduler.scheduleAtFixedRate(wrapTask(runnable), initialDelay, delay, TimeUnit.MILLISECONDS);
        addEvent(() -> sf.cancel(true), 10 + initialDelay + delay * executes);
        return sf;
    }

    class RepeatingTask {
        public final Runnable task;
        public final long initialDelay;
        public final long delay;
        public final TimeUnit timeUnit;

        public RepeatingTask(Runnable task, long initialDelay, long delay, TimeUnit timeUnit) {
            this.task = task;
            this.initialDelay = initialDelay;
            this.delay = delay;
            this.timeUnit = timeUnit;
        }
    }
}
