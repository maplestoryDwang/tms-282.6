package net.swordie.ms.connection.api.launcher;

/**
 * How a request reached the {@link LauncherActionRegistry}; each channel carries its own policy switches so the
 * pipeline stays one list of {@link LauncherPolicy} objects.
 */
public enum LauncherChannel {
    /**
     * LocalAccountCli: one JSON request per JVM on stdin; no bearer sessions, no rate limits, transport checks apply.
     * Session actions authenticate the request's own username + password ({@link SessionSource#CREDENTIALS}). Its
     * readers include TmsAccountLauncher and cat-launcher 1.0.0, so account outcomes keep the 09-22 codes.
     */
    CLI(false, false, false, LauncherCodes.BUSY, true, SessionSource.CREDENTIALS),
    /**
     * POST /api/v2/account (cat-launcher 1.0.0): never issues tokens; the per-action buckets apply too (else they
     * could be bypassed here), refused with {@code busy}, which 1.0.0 shows as rateLimited; 09-22 codes only.
     */
    ACCOUNT_V2(true, false, true, LauncherCodes.BUSY, true, SessionSource.NONE),
    /** POST /api/v2/launcher: bearer sessions, per-action rate buckets, P5a codes. */
    LAUNCHER_V2(true, true, true, LauncherCodes.RATE_LIMITED, false, SessionSource.BEARER);

    private final boolean http;
    private final boolean issuesSessions;
    private final boolean limitsActions;
    private final String rateLimitedCode;
    private final boolean legacyCodes;
    private final SessionSource sessionSource;

    LauncherChannel(boolean http, boolean issuesSessions, boolean limitsActions, String rateLimitedCode,
                    boolean legacyCodes, SessionSource sessionSource) {
        this.http = http;
        this.issuesSessions = issuesSessions;
        this.limitsActions = limitsActions;
        this.rateLimitedCode = rateLimitedCode;
        this.legacyCodes = legacyCodes;
        this.sessionSource = sessionSource;
    }

    /** Served by the game server process (8600). */
    public boolean http() {
        return http;
    }

    /** login returns a bearer token (and session-only actions take it). */
    public boolean issuesSessions() {
        return issuesSessions;
    }

    /** How a {@code requiresSession} action finds its caller on this channel. */
    public SessionSource sessionSource() {
        return sessionSource;
    }

    /** Session actions carry their own username + password (CLI: no session outlives one request). */
    public boolean authenticatesCredentials() {
        return sessionSource == SessionSource.CREDENTIALS;
    }

    /** Each action's {@link LauncherAction#rateBucket()} is enforced per source IP. */
    public boolean limitsActions() {
        return limitsActions;
    }

    /** The code a rate limit / login back-off refusal uses on this channel ({@code busy} for 1.0.0 clients). */
    public String rateLimitedCode() {
        return rateLimitedCode;
    }

    /**
     * Account outcomes use only {@link LauncherCodes#EXISTING} (readers that fall back to "service unavailable,
     * check MySQL" for anything else). Transport refusals of new actions (serverOnly / serverRunning) are P5a codes
     * on every channel.
     */
    public boolean legacyCodes() {
        return legacyCodes;
    }

    /**
     * login.maintenance refused the account. {@code maintenance} + message on {@code /api/v2/launcher}; elsewhere the
     * 09-22 code {@code banned} ("cannot log in, contact the host") with {@code reason: maintenance} and the message
     * as added fields (CP0074 refuses the same login in game with the message).
     */
    public LauncherResult maintenanceRefusal(String message) {
        if (legacyCodes) {
            return LauncherResult.fail(LauncherCodes.BANNED).with("reason", LauncherCodes.MAINTENANCE).with("message", message);
        }
        return LauncherResult.fail(LauncherCodes.MAINTENANCE).with("message", message);
    }
}
