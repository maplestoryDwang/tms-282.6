package net.swordie.ms.connection.api.launcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Keyed token buckets: each key starts with {@code capacity} tokens and regains one every {@code refillMs}.
 * A bucket that has refilled to capacity carries no information, so {@link #sweep()} drops it; sweeps also run
 * lazily at most once a minute from {@link #tryAcquire}, which keeps idle source IPs from piling up.
 */
public final class TokenBucketLimiter {
    private static final long SWEEP_EVERY_MS = TimeUnit.MINUTES.toMillis(1);

    public record Policy(int capacity, long refillMs) {
        public Policy {
            if (capacity < 1 || refillMs < 1) {
                throw new IllegalArgumentException("capacity and refillMs must be positive");
            }
        }
    }

    private final Policy policy;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicLong refused = new AtomicLong();
    private volatile long lastSweep;

    public TokenBucketLimiter(Policy policy, LongSupplier clock) {
        this.policy = policy;
        this.clock = clock;
        this.lastSweep = clock.getAsLong();
    }

    public Policy policy() {
        return policy;
    }

    /** Takes one token for {@code key}; false (and counted) when the bucket is empty. */
    public boolean tryAcquire(String key) {
        long now = clock.getAsLong();
        sweepIfDue(now);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(policy, now));
        boolean ok;
        synchronized (bucket) {
            ok = bucket.take(now, policy);
        }
        if (!ok) {
            refused.incrementAndGet();
        }
        return ok;
    }

    /** Milliseconds until {@code key} has a token again (0 = available now). */
    public long retryAfterMs(String key) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            return 0;
        }
        synchronized (bucket) {
            return bucket.waitMs(clock.getAsLong(), policy);
        }
    }

    /** Drops every bucket that has refilled to capacity; returns how many. */
    public int sweep() {
        long now = clock.getAsLong();
        lastSweep = now;
        int[] removed = {0};
        buckets.forEach((key, bucket) -> {
            boolean full;
            synchronized (bucket) {
                full = bucket.full(now, policy);
            }
            if (full && buckets.remove(key, bucket)) {
                removed[0]++;
            }
        });
        return removed[0];
    }

    public int size() {
        return buckets.size();
    }

    /** Refusals since start (monitoring). */
    public long refused() {
        return refused.get();
    }

    public void clear() {
        buckets.clear();
    }

    private void sweepIfDue(long now) {
        long last = lastSweep;
        if (now - last >= SWEEP_EVERY_MS || now < last) {
            sweep();
        }
    }

    /** Credit in milliseconds: one token = {@code refillMs} of credit, so refills are exact integer arithmetic. */
    private static final class Bucket {
        private long credit;
        private long updated;

        Bucket(Policy policy, long now) {
            this.credit = policy.capacity() * policy.refillMs();
            this.updated = now;
        }

        private void refill(long now, Policy policy) {
            if (now > updated) {
                long max = policy.capacity() * policy.refillMs();
                credit = Math.min(max, credit + Math.min(now - updated, max));
                updated = now;
            }
        }

        boolean take(long now, Policy policy) {
            refill(now, policy);
            if (credit >= policy.refillMs()) {
                credit -= policy.refillMs();
                return true;
            }
            return false;
        }

        long waitMs(long now, Policy policy) {
            refill(now, policy);
            return credit >= policy.refillMs() ? 0 : policy.refillMs() - credit;
        }

        boolean full(long now, Policy policy) {
            refill(now, policy);
            return credit >= policy.capacity() * policy.refillMs();
        }
    }
}
