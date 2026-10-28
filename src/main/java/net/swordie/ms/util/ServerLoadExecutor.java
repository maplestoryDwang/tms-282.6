package net.swordie.ms.util;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServerLoadExecutor {
    private static final int MIN_THREADS = 2;
    /** 並行讀 WZ/DB 時同時駐留的任務上限，避免磁盤爭用拖慢總耗時。 */
    public static final int PARALLEL_IO_CAP = 6;
    private static final int STEADY_WAIT_SECONDS = 30;
    private static final AtomicInteger THREAD_ID = new AtomicInteger(1);
    private static volatile ExecutorService startupPool;

    private ServerLoadExecutor() {
    }

    public static void ensureStartupPool() {
        pool();
    }

    public static void runAll(String phase, LoadTask... tasks) {
        runAll(phase, PARALLEL_IO_CAP, tasks);
    }

    public static void runAll(String phase, int maxConcurrent, LoadTask... tasks) {
        if (tasks == null || tasks.length == 0) {
            return;
        }
        if (tasks.length == 1) {
            runSequential(phase, tasks[0]);
            return;
        }
        CountDownLatch latch = new CountDownLatch(tasks.length);
        CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();
        Semaphore concurrency = maxConcurrent > 0
                ? new Semaphore(Math.min(maxConcurrent, tasks.length))
                : null;
        long start = System.currentTimeMillis();
        if (concurrency != null) {
            System.out.printf("[Startup] %s: parallel loading %d tasks (%d threads, max %d concurrent).%n",
                    phase, tasks.length, startupThreads(), concurrency.availablePermits());
        } else {
            System.out.printf("[Startup] %s: parallel loading %d tasks with %d threads.%n",
                    phase, tasks.length, startupThreads());
        }
        for (LoadTask task : tasks) {
            pool().execute(() -> {
                if (concurrency != null) {
                    try {
                        concurrency.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        failures.add(e);
                        latch.countDown();
                        return;
                    }
                }
                long taskStart = System.currentTimeMillis();
                try {
                    task.run();
                    System.out.printf("[Startup] %s loaded in %d ms.%n", task.name(), System.currentTimeMillis() - taskStart);
                } catch (Throwable t) {
                    failures.add(t);
                    System.err.printf("[Startup] %s failed: %s%n", task.name(), t.getMessage());
                    t.printStackTrace();
                } finally {
                    if (concurrency != null) {
                        concurrency.release();
                    }
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(phase + " interrupted", e);
        }
        if (!failures.isEmpty()) {
            IllegalStateException error = new IllegalStateException(phase + " failed: " + failures.size() + " task(s)");
            for (Throwable failure : failures) {
                error.addSuppressed(failure);
            }
            throw error;
        }
        System.out.printf("[Startup] %s finished in %d ms.%n", phase, System.currentTimeMillis() - start);
    }

    /** 重 IO / 有依賴的任務在主線程順序執行，避免與並行階段搶磁盤。 */
    public static void runSequential(String phase, LoadTask... tasks) {
        if (tasks == null || tasks.length == 0) {
            return;
        }
        long phaseStart = System.currentTimeMillis();
        for (LoadTask task : tasks) {
            long taskStart = System.currentTimeMillis();
            try {
                task.run();
                System.out.printf("[Startup] %s loaded in %d ms.%n", task.name(), System.currentTimeMillis() - taskStart);
            } catch (Throwable t) {
                throw new IllegalStateException(phase + " failed: " + task.name(), t);
            }
        }
        if (tasks.length > 1) {
            System.out.printf("[Startup] %s finished in %d ms.%n", phase, System.currentTimeMillis() - phaseStart);
        }
    }

    public static void shutdownStartupPool() {
        ExecutorService pool = startupPool;
        startupPool = null;
        if (pool == null) {
            return;
        }
        pool.shutdown();
        try {
            if (!pool.awaitTermination(STEADY_WAIT_SECONDS, TimeUnit.SECONDS)) {
                List<Runnable> pending = pool.shutdownNow();
                if (!pending.isEmpty()) {
                    System.out.printf("[Startup] discarded %d pending startup task(s).%n", pending.size());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }

    public static int startupThreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores < 10) {
            return Math.max(MIN_THREADS, cores);
        }
        return Math.max(MIN_THREADS, (int) (cores * 0.8d));
    }

    private static ExecutorService pool() {
        if (startupPool == null) {
            synchronized (ServerLoadExecutor.class) {
                if (startupPool == null) {
                    startupPool = Executors.newFixedThreadPool(startupThreads(), new StartupThreadFactory());
                }
            }
        }
        return startupPool;
    }

    public record LoadTask(String name, ThrowingRunnable action) {
        public void run() throws Exception {
            action.run();
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class StartupThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "GMS265-StartupLoad-" + THREAD_ID.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}