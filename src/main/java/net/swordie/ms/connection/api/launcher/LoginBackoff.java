package net.swordie.ms.connection.api.launcher;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Failed launcher logins per (account, source IP). The {@link #LOCK_AFTER_FAILURES}th failure in a row locks the
 * pair for 30 s and every further failure doubles the lock up to 15 minutes.
 * A correct password clears the pair, a successful reset every pair of the account; a pair idle for 30 minutes is
 * forgotten. Other accounts from the same IP and the same account from other IPs are unaffected (a friend cannot
 * lock someone else out). Callers pass account names already restricted to [A-Za-z0-9] (users.name compares
 * accent- and width-insensitively, so any other spelling would get a key of its own).
 */
public final class LoginBackoff {
    public static final int LOCK_AFTER_FAILURES = 5;
    public static final long BASE_LOCK_MS = TimeUnit.SECONDS.toMillis(30);
    public static final long MAX_LOCK_MS = TimeUnit.MINUTES.toMillis(15);
    public static final long FORGET_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long SWEEP_EVERY_MS = TimeUnit.MINUTES.toMillis(1);

    private final boolean enabled;
    private final LongSupplier clock;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private volatile long lastSweep;

    public LoginBackoff(LongSupplier clock) {
        this(true, clock);
    }

    private LoginBackoff(boolean enabled, LongSupplier clock) {
        this.enabled = enabled;
        this.clock = clock;
        this.lastSweep = clock.getAsLong();
    }

    /** CLI: nothing survives the JVM anyway. */
    public static LoginBackoff disabled() {
        return new LoginBackoff(false, System::currentTimeMillis);
    }

    /** Milliseconds the pair must still wait (0 = may try now). */
    public long retryAfterMs(String account, String sourceIp) {
        if (!enabled) {
            return 0;
        }
        Entry e = entries.get(key(account, sourceIp));
        if (e == null) {
            return 0;
        }
        synchronized (e) {
            return Math.max(0, e.lockedUntil - clock.getAsLong());
        }
    }

    public void failure(String account, String sourceIp) {
        if (!enabled) {
            return;
        }
        long now = clock.getAsLong();
        sweepIfDue(now);
        Entry e = entries.computeIfAbsent(key(account, sourceIp), k -> new Entry());
        synchronized (e) {
            e.failures++;
            e.lastFailure = now;
            if (e.failures >= LOCK_AFTER_FAILURES) {
                int doublings = Math.min(e.failures - LOCK_AFTER_FAILURES, 10);
                e.lockedUntil = now + Math.min(MAX_LOCK_MS, BASE_LOCK_MS << doublings);
            }
        }
    }

    public void success(String account, String sourceIp) {
        if (enabled) {
            entries.remove(key(account, sourceIp));
        }
    }

    /** Forgets the account's pairs from every source IP (a successful reset); returns how many. */
    public int clearAccount(String account) {
        if (!enabled) {
            return 0;
        }
        String prefix = key(account, "");
        int[] removed = {0};
        entries.keySet().removeIf(k -> {
            boolean match = k.startsWith(prefix);
            if (match) {
                removed[0]++;
            }
            return match;
        });
        return removed[0];
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    /** Forgets pairs that are unlocked and idle for {@link #FORGET_MS}; returns how many. */
    public int sweep() {
        long now = clock.getAsLong();
        lastSweep = now;
        int[] removed = {0};
        entries.forEach((key, e) -> {
            boolean stale;
            synchronized (e) {
                stale = now >= e.lockedUntil && now - e.lastFailure >= FORGET_MS;
            }
            if (stale && entries.remove(key, e)) {
                removed[0]++;
            }
        });
        return removed[0];
    }

    private void sweepIfDue(long now) {
        long last = lastSweep;
        if (now - last >= SWEEP_EVERY_MS || now < last) {
            sweep();
        }
    }

    private static String key(String account, String sourceIp) {
        return (account == null ? "" : account.toLowerCase(Locale.ROOT)) + '|' + (sourceIp == null ? "" : sourceIp);
    }

    private static final class Entry {
        int failures;
        long lastFailure;
        long lockedUntil;
    }

    /** One remembered (account, source IP) pair as the admin console shows it. */
    public record Pair(String sourceIp, int failures, long lockedForMs, long lastFailure) {
    }

    /**
     * The account's remembered pairs from every source IP (admin console, read-only), most recently failed first;
     * {@code lockedForMs} 0 = not locked right now (failures still counted).
     */
    public java.util.List<Pair> pairsOf(String account) {
        java.util.List<Pair> out = new java.util.ArrayList<>();
        if (!enabled) {
            return out;
        }
        String prefix = key(account, "");
        long now = clock.getAsLong();
        entries.forEach((k, e) -> {
            if (k.startsWith(prefix)) {
                synchronized (e) {
                    out.add(new Pair(k.substring(prefix.length()), e.failures, Math.max(0, e.lockedUntil - now), e.lastFailure));
                }
            }
        });
        out.sort(java.util.Comparator.comparingLong(Pair::lastFailure).reversed());
        return out;
    }

    /** Pairs locked right now over every account (admin console). */
    public int lockedCount() {
        long now = clock.getAsLong();
        int[] n = {0};
        entries.forEach((k, e) -> {
            synchronized (e) {
                if (e.lockedUntil > now) {
                    n[0]++;
                }
            }
        });
        return n[0];
    }
}
