package net.swordie.ms.connection.api.launcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;

/**
 * Named per-source-IP token buckets.
 * <ul>
 *     <li>{@link #IP}: the flood guard of every account endpoint ({@code HttpAuthApi.allow()}): 20 tokens, one per
 *     3 s - the original "20 per IP per minute" of {@code /api/v2/account}, now as a token bucket.</li>
 *     <li>{@link #AUTH}: login / reset on {@code /api/v2/launcher} and {@code /api/v2/account}, and the legacy
 *     {@code /api/login}: 10, one per 6 s.</li>
 *     <li>{@link #REGISTER}: account creation on {@code /api/v2/launcher} and {@code /api/v2/account}: 5, one per
 *     minute.</li>
 *     <li>{@link #SESSION}: cheap session calls (logout): 30, one per 2 s.</li>
 *     <li>{@link #READ}: read-only views (status, me, characters, dashboard, dailyRewards, dailyStatus, news): 30, one
 *     per 2 s.</li>
 *     <li>{@link #NAME}: checkName (P7, public): 10, one per 6 s - at most 10 per IP per minute after a burst.</li>
 *     <li>{@link #GIFT}: giftRedeem (P7): 10, one per 6 s; GiftCodeService also limits failed guesses per account.</li>
 * </ul>
 * The {@code unlimited()} instance (CLI: one request per JVM) accepts everything.
 */
public final class LauncherRateLimits {
    public static final String IP = "ip";
    public static final String AUTH = "auth";
    public static final String REGISTER = "register";
    public static final String SESSION = "session";
    public static final String READ = "read";
    public static final String NAME = "name";
    public static final String GIFT = "gift";
    /**
     * 1.2.3: client file downloads ({@code GET /api/v2/launcher/client/...}): 60, one per second. Separate from {@link #IP}
     * (shared with login) so a friend fetching many game files is not locked out of logging in, and vice versa.
     */
    public static final String CLIENT_FILE = "clientFile";
    /** Bucket names an action may declare. */
    public static final Set<String> BUCKETS = Set.of(IP, AUTH, REGISTER, SESSION, READ, NAME, GIFT, CLIENT_FILE);

    private final Map<String, TokenBucketLimiter> limiters;

    private LauncherRateLimits(Map<String, TokenBucketLimiter> limiters) {
        this.limiters = limiters;
    }

    public static LauncherRateLimits standard(LongSupplier clock) {
        Map<String, TokenBucketLimiter> m = new LinkedHashMap<>();
        m.put(IP, new TokenBucketLimiter(new TokenBucketLimiter.Policy(20, 3_000), clock));
        m.put(AUTH, new TokenBucketLimiter(new TokenBucketLimiter.Policy(10, 6_000), clock));
        m.put(REGISTER, new TokenBucketLimiter(new TokenBucketLimiter.Policy(5, 60_000), clock));
        m.put(SESSION, new TokenBucketLimiter(new TokenBucketLimiter.Policy(30, 2_000), clock));
        m.put(READ, new TokenBucketLimiter(new TokenBucketLimiter.Policy(30, 2_000), clock));
        m.put(NAME, new TokenBucketLimiter(new TokenBucketLimiter.Policy(10, 6_000), clock));
        m.put(GIFT, new TokenBucketLimiter(new TokenBucketLimiter.Policy(10, 6_000), clock));
        m.put(CLIENT_FILE, new TokenBucketLimiter(new TokenBucketLimiter.Policy(60, 1_000), clock));
        return new LauncherRateLimits(Map.copyOf(m));
    }

    public static LauncherRateLimits unlimited() {
        return new LauncherRateLimits(Map.of());
    }

    public boolean unlimitedInstance() {
        return limiters.isEmpty();
    }

    /** One token of {@code bucket} for {@code sourceIp}. */
    public boolean tryAcquire(String bucket, String sourceIp) {
        if (limiters.isEmpty()) {
            return true;
        }
        TokenBucketLimiter limiter = limiters.get(bucket);
        if (limiter == null) {
            throw new IllegalArgumentException("unknown rate bucket " + bucket);
        }
        return limiter.tryAcquire(sourceIp == null ? "" : sourceIp);
    }

    public long retryAfterMs(String bucket, String sourceIp) {
        TokenBucketLimiter limiter = limiters.get(bucket);
        return limiter == null ? 0 : limiter.retryAfterMs(sourceIp == null ? "" : sourceIp);
    }

    /** The limiter of one bucket (tests / monitoring), or null on the unlimited instance. */
    public TokenBucketLimiter limiter(String bucket) {
        return limiters.get(bucket);
    }

    /** Refusals over all buckets since start. */
    public long refused() {
        long n = 0;
        for (TokenBucketLimiter l : limiters.values()) {
            n += l.refused();
        }
        return n;
    }

    public int sweep() {
        int n = 0;
        for (TokenBucketLimiter l : limiters.values()) {
            n += l.sweep();
        }
        return n;
    }

    /** Forgets every bucket (console "reset limits"; tests between sections). */
    public void clear() {
        for (TokenBucketLimiter l : limiters.values()) {
            l.clear();
        }
    }
}
