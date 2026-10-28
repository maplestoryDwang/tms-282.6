package net.swordie.ms.connection.api.launcher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Bearer sessions of {@code /api/v2/launcher}, in server memory only (a restart logs every launcher out; it then
 * receives {@code sessionExpired} and logs in again).
 * <ul>
 *     <li>Token: 32 {@link SecureRandom} bytes as unpadded base64url (43 characters). Only its SHA-256 is stored.</li>
 *     <li>Expiry: 30 minutes without use, or 12 hours after login even when used.</li>
 *     <li>At most 3 per account: a 4th login drops the least recently used one.</li>
 *     <li>Bound to the source IP it was issued to.</li>
 *     <li>{@link #revokeAll(int)} on a successful reset or when the account is found banned.</li>
 * </ul>
 * Traffic is a handful of friends, so one lock over a plain map keeps every rule simple and atomic.
 */
public final class LauncherSessions {
    public static final long IDLE_MS = TimeUnit.MINUTES.toMillis(30);
    public static final long MAX_AGE_MS = TimeUnit.HOURS.toMillis(12);
    public static final int MAX_PER_ACCOUNT = 3;
    public static final int TOKEN_BYTES = 32;
    private static final long SWEEP_EVERY_MS = TimeUnit.MINUTES.toMillis(1);
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum Status {
        VALID,
        /** No token, or not a well-formed one. */
        MISSING,
        /** Unknown (server restarted, revoked) or past the idle / age limit. */
        EXPIRED,
        /** A live token presented from a different source IP. */
        WRONG_SOURCE
    }

    public record Lookup(Status status, LauncherSession session) {
    }

    private final LongSupplier clock;
    private final Map<String, LauncherSession> byHash = new HashMap<>();
    private long lastSweep;

    public LauncherSessions(LongSupplier clock) {
        this.clock = clock;
    }

    /** A new token for {@code userId} bound to {@code sourceIp}; the caller hands it to the launcher and forgets it. */
    public synchronized String issue(int userId, String sourceIp) {
        long now = clock.getAsLong();
        sweepLocked(now);
        List<LauncherSession> mine = new ArrayList<>();
        for (LauncherSession s : byHash.values()) {
            if (s.userId() == userId) {
                mine.add(s);
            }
        }
        mine.sort(Comparator.comparingLong(LauncherSession::lastSeen));
        for (int i = 0; mine.size() - i >= MAX_PER_ACCOUNT; i++) {
            byHash.remove(mine.get(i).tokenHash());
        }
        byte[] raw = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        String hash = hash(token);
        byHash.put(hash, new LauncherSession(hash, userId, sourceIp, now));
        return token;
    }

    /** Resolves a presented token; a valid one is refreshed (idle timer restarts). */
    public synchronized Lookup lookup(String token, String sourceIp) {
        if (!wellFormed(token)) {
            return new Lookup(Status.MISSING, null);
        }
        long now = clock.getAsLong();
        sweepIfDue(now);
        LauncherSession s = byHash.get(hash(token));
        if (s == null) {
            return new Lookup(Status.EXPIRED, null);
        }
        if (expired(s, now)) {
            byHash.remove(s.tokenHash());
            return new Lookup(Status.EXPIRED, null);
        }
        if (sourceIp == null || !s.sourceIp().equals(sourceIp)) {
            return new Lookup(Status.WRONG_SOURCE, null);
        }
        s.touch(now);
        return new Lookup(Status.VALID, s);
    }

    /** session.logout: drops exactly this session. */
    public synchronized boolean revoke(LauncherSession session) {
        return session != null && byHash.remove(session.tokenHash(), session);
    }

    /** Drops every session of the account; returns how many. */
    public synchronized int revokeAll(int userId) {
        int n = 0;
        for (Iterator<LauncherSession> it = byHash.values().iterator(); it.hasNext(); ) {
            if (it.next().userId() == userId) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    /**
     * Drops every session of the account except {@code keep} (changePassword: the session that changed it stays);
     * returns how many. {@code keep} null or transient (CLI) drops them all.
     */
    public synchronized int revokeOthers(int userId, LauncherSession keep) {
        int n = 0;
        for (Iterator<LauncherSession> it = byHash.values().iterator(); it.hasNext(); ) {
            LauncherSession s = it.next();
            if (s.userId() == userId && s != keep) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    /** Live sessions of one account (monitoring). */
    public synchronized int countFor(int userId) {
        sweepLocked(clock.getAsLong());
        int n = 0;
        for (LauncherSession s : byHash.values()) {
            if (s.userId() == userId) {
                n++;
            }
        }
        return n;
    }

    public synchronized int size() {
        return byHash.size();
    }

    /** Removes expired sessions now; returns how many. */
    public synchronized int sweep() {
        return sweepLocked(clock.getAsLong());
    }

    public static boolean wellFormed(String token) {
        return token != null && TOKEN.matcher(token).matches();
    }

    private void sweepIfDue(long now) {
        if (now - lastSweep >= SWEEP_EVERY_MS || now < lastSweep) {
            sweepLocked(now);
        }
    }

    private int sweepLocked(long now) {
        lastSweep = now;
        int n = 0;
        for (Iterator<LauncherSession> it = byHash.values().iterator(); it.hasNext(); ) {
            if (expired(it.next(), now)) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    private static boolean expired(LauncherSession s, long now) {
        return now - s.lastSeen() >= IDLE_MS || now - s.createdAt() >= MAX_AGE_MS;
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized String toString() {
        return "LauncherSessions[size=" + byHash.size() + "]";
    }
}
