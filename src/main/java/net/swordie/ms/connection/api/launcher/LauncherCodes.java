package net.swordie.ms.connection.api.launcher;

import java.util.Set;

/**
 * The {@code code} values of launcher results. The first group is what LocalAccountCli, cat-launcher 1.0.0 and
 * TmsAccountLauncher already understand: never rename them.
 */
public final class LauncherCodes {
    // --- existing (CLI / /api/v2/account since 09-22) ---
    public static final String INVALID_INPUT = "invalidInput";
    public static final String UNAVAILABLE = "unavailable";
    public static final String LOCAL_ONLY = "localOnly";
    public static final String SETUP_REQUIRED = "setupRequired";
    public static final String WRONG_CREDENTIALS = "wrongCredentials";
    public static final String BANNED = "banned";
    public static final String LOGGED_IN = "loggedIn";
    public static final String REGISTERED = "registered";
    public static final String INVALID_PIN = "invalidPin";
    public static final String NAME_UNAVAILABLE = "nameUnavailable";
    public static final String INVALID_RECOVERY = "invalidRecovery";
    public static final String ALREADY_ONLINE = "alreadyOnline";
    public static final String SAME_AS_PIN = "sameAsPin";
    public static final String RESET = "reset";
    /** /api/v2/account rate limit; cat-launcher 1.0.0 maps it to rateLimited. */
    public static final String BUSY = "busy";
    /** The codes above: all a {@link LauncherChannel#legacyCodes()} channel answers for login / register / reset. */
    public static final Set<String> EXISTING = Set.of(INVALID_INPUT, UNAVAILABLE, LOCAL_ONLY, SETUP_REQUIRED,
            WRONG_CREDENTIALS, BANNED, LOGGED_IN, REGISTERED, INVALID_PIN, NAME_UNAVAILABLE, INVALID_RECOVERY,
            ALREADY_ONLINE, SAME_AS_PIN, RESET, BUSY);

    // --- P5a ---
    /**
     * login.maintenance refused the account (CP0074 sends login.maintenance.code). Only /api/v2/launcher answers it;
     * legacy channels answer {@code banned} with {@code reason: maintenance}.
     */
    public static final String MAINTENANCE = "maintenance";
    /** CLI: a SERVER_WHEN_RUNNING action (reset) while 127.0.0.1:8484 is listening. */
    public static final String SERVER_RUNNING = "serverRunning";
    /** CLI: a SERVER_ONLY action. */
    public static final String SERVER_ONLY = "serverOnly";
    /** HTTP: an action with remoteAllowed=false from a non-loopback source. */
    public static final String REMOTE_FORBIDDEN = "remoteForbidden";
    /** /api/v2/launcher rate limit or login back-off. */
    public static final String RATE_LIMITED = "rateLimited";
    /** No (well-formed) bearer token, or a token presented from another source IP. */
    public static final String NEED_SESSION = "needSession";
    /** Unknown, idle (30 min) or aged-out (12 h) token: log in again. */
    public static final String SESSION_EXPIRED = "sessionExpired";
    public static final String LOGGED_OUT = "loggedOut";
    /** HTTP protocol refusals (non-200 status). */
    public static final String FORBIDDEN = "forbidden";
    public static final String LAUNCHER_REQUIRED = "launcherRequired";
    public static final String UNSUPPORTED_MEDIA_TYPE = "unsupportedMediaType";

    // --- P5b ---
    /** A read-only view answered (status, me, characters, dashboard). */
    public static final String OK = "ok";

    // --- P7 ---
    /** changePassword done (other sessions of the account revoked). */
    public static final String PASSWORD_CHANGED = "passwordChanged";
    /** changePin done. */
    public static final String PIN_CHANGED = "pinChanged";
    /** changePassword / changePin: the current password does not match. */
    public static final String WRONG_CURRENT_PASSWORD = "wrongCurrentPassword";
    /** giftRedeem done (items queued for the character's next field entry). */
    public static final String REDEEMED = "redeemed";
    /** giftRedeem: every refusal about the code itself (unknown, disabled, not started, expired, used up, world, level). */
    public static final String INVALID_OR_EXPIRED = "invalidOrExpired";
    /** giftRedeem: this account already used the code as often as it may. */
    public static final String ALREADY_REDEEMED = "alreadyRedeemed";
    /** giftRedeem: characterId is not a (non-deleting) character of the session's account. */
    public static final String NO_RECEIVER = "noReceiver";

    private LauncherCodes() {
    }
}
