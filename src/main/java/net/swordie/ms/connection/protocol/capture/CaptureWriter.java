package net.swordie.ms.connection.protocol.capture;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The one background thread that writes every capture file and trace line, so a socket's event loop only copies
 * bytes and enqueues. One thread keeps each socket's files in order. The queue is bounded
 * ({@value #QUEUE_CAPACITY} packets): when the disk cannot keep up, further packets are skipped and counted rather
 * than slowing the game or growing the heap; nothing here ever blocks or throws into a socket.
 */
public final class CaptureWriter implements Executor {
    public static final int QUEUE_CAPACITY = 65_536;
    private static final CaptureWriter SHARED = new CaptureWriter("packet-capture-writer");

    private final ThreadPoolExecutor executor;
    private final AtomicLong skipped = new AtomicLong();

    public CaptureWriter(String threadName) {
        executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(QUEUE_CAPACITY), r -> {
            Thread thread = new Thread(r, threadName);
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy());
    }

    /** Process-wide writer used by the acceptors. */
    public static CaptureWriter shared() {
        return SHARED;
    }

    /** Runs the writes on the calling thread (checks that read the files right after a packet). */
    public static Executor direct() {
        return Runnable::run;
    }

    @Override
    public void execute(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            long n = skipped.incrementAndGet();
            if (n == 1 || n % 1000 == 0) {
                System.out.println("[Capture] writer queue full, " + n + " packet(s) not captured so far");
            }
        }
    }

    /** Packets not captured because the queue was full. */
    public long skipped() {
        return skipped.get();
    }

    /** Waits until everything queued before this call is written; false on timeout (checks and shutdown). */
    public boolean awaitIdle(Duration timeout) throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        try {
            executor.execute(done::countDown);
        } catch (RejectedExecutionException e) {
            return false;
        }
        return done.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
}
