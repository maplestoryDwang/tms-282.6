package net.swordie.ms.connection.api.launcher;

/**
 * One launcher login held by {@link LauncherSessions}. Keeps the SHA-256 of the bearer token, never the token.
 * {@link #lastSeen} is guarded by the owning {@link LauncherSessions}. A {@link #transientFor transient} session
 * (CLI: credentials checked for this one request) has no token and is never stored.
 */
public final class LauncherSession {
    private final String tokenHash;
    private final int userId;
    private final String sourceIp;
    private final long createdAt;
    private long lastSeen;

    LauncherSession(String tokenHash, int userId, String sourceIp, long createdAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.sourceIp = sourceIp;
        this.createdAt = createdAt;
        this.lastSeen = createdAt;
    }

    /**
     * The caller of one CLI request whose username + password were just verified ({@link SessionSource#CREDENTIALS}):
     * no token, not held by any {@link LauncherSessions}, gone with the request.
     */
    static LauncherSession transientFor(int userId, String sourceIp, long now) {
        return new LauncherSession(null, userId, sourceIp, now);
    }

    /** Created by {@link #transientFor} (no bearer token behind it). */
    public boolean transientSession() {
        return tokenHash == null;
    }

    String tokenHash() {
        return tokenHash;
    }

    public int userId() {
        return userId;
    }

    /** The source IP the session is bound to. */
    public String sourceIp() {
        return sourceIp;
    }

    public long createdAt() {
        return createdAt;
    }

    long lastSeen() {
        return lastSeen;
    }

    void touch(long now) {
        if (now > lastSeen) {
            lastSeen = now;
        }
    }

    @Override
    public String toString() {
        return "LauncherSession[userId=" + userId + ", source=" + sourceIp + ", createdAt=" + createdAt
                + (tokenHash == null ? ", transient" : "") + "]";
    }
}
