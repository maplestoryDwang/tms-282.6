package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.util.regex.Pattern;

/**
 * P7 base of changePassword / changePin (template method): the session's own account only, the current password
 * checked again with BCrypt (also on the CLI, whose session came from the same password), the write through
 * {@link AccountSecretWriter}. SERVER_WHEN_RUNNING: while the game server runs only its process may write, because
 * it holds the account in memory.
 * <p>
 * Request: {@code currentPassword} (the CLI may send it as {@code password}, which its session check already
 * uses), plus the action's new value. A malformed field answers {@code invalidInput}; a wrong current password
 * {@code wrongCurrentPassword} (charged to the AUTH bucket and to the login back-off of the account and source IP,
 * like a wrong login: after 5 the action answers {@code rateLimited} + retryAfterSec).
 */
abstract class AccountWriteAction implements LauncherAction {
    static final Pattern PASSWORD = Pattern.compile("[A-Za-z0-9]{8,32}");
    static final Pattern PIN = Pattern.compile("[0-9]{6,16}");

    @Override
    public final boolean requiresSession() {
        return true;
    }

    @Override
    public final boolean mutating() {
        return true;
    }

    @Override
    public final String rateBucket() {
        return LauncherRateLimits.AUTH;
    }

    @Override
    public final boolean remoteAllowed() {
        return true;
    }

    @Override
    public final LauncherTransport transport() {
        return LauncherTransport.SERVER_WHEN_RUNNING;
    }

    @Override
    public final LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception {
        LauncherSession session = ctx.session();
        if (session == null) {
            return LauncherResult.fail(LauncherCodes.NEED_SESSION);
        }
        String current = LauncherJson.text(request, "currentPassword");
        if (current.isEmpty() && !ctx.channel().http()) {
            current = LauncherJson.text(request, "password");
        }
        if (!PASSWORD.matcher(current).matches()) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "currentPassword");
        }
        String problem = validate(request, current);
        if (problem != null) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", problem);
        }
        if (!LocalDatabaseGuard.accepted()) {
            return LauncherResult.fail(LauncherCodes.LOCAL_ONLY);
        }
        AccountSecretWriter.Row row;
        try (Connection con = DatabaseManager.getConnection()) {
            row = AccountSecretWriter.read(con, session.userId());
        }
        if (row == null) {
            AccountSecrets.spendVerifyTime(current);
            return LauncherResult.fail(LauncherCodes.SESSION_EXPIRED);
        }
        // the login back-off of this account + source IP: a stolen session cannot be used to guess the password
        LoginBackoff backoff = ctx.services().loginBackoff();
        long wait = backoff.retryAfterMs(row.name(), ctx.sourceIp());
        if (wait > 0) {
            return LauncherResult.fail(LauncherCodes.RATE_LIMITED).with("retryAfterSec", Math.max(1, (wait + 999) / 1000));
        }
        if (!AccountSecrets.verifyPassword(current, row.password())) {
            backoff.failure(row.name(), ctx.sourceIp());
            return LauncherResult.fail(LauncherCodes.WRONG_CURRENT_PASSWORD);
        }
        backoff.success(row.name(), ctx.sourceIp());
        return write(ctx, request, current, row);
    }

    /** Null when the action's own fields are fine, otherwise the name of the bad one. */
    protected abstract String validate(JsonNode request, String currentPassword);

    /** The current password was verified against {@code row}. */
    protected abstract LauncherResult write(LauncherContext ctx, JsonNode request, String currentPassword,
                                            AccountSecretWriter.Row row) throws Exception;

    static LauncherResult conflict(AccountSecretWriter.Outcome o) {
        return o == AccountSecretWriter.Outcome.GONE ? LauncherResult.fail(LauncherCodes.SESSION_EXPIRED)
                // password or pin changed between the check and the write (another changePassword / reset): retry
                : LauncherResult.fail(LauncherCodes.WRONG_CURRENT_PASSWORD);
    }
}
