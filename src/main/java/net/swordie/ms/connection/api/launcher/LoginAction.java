package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code login}: checks the account like the game login (CP0074) does, in the same order - IP ban, password,
 * users.banExpireDate / account_block ban, login.maintenance ({@link AccountAuthenticator}) - and answers
 * {@code loggedIn} + userId. On {@code /api/v2/launcher} it also issues a bearer session ({@code token}); the CLI and
 * {@code /api/v2/account} never do.
 * <p>
 * CP0074 answers every refusal but maintenance with the same LP0000 code 4; the launcher tells them apart
 * ({@code banned} only after a correct password, as before). Failed passwords feed {@link LoginBackoff}. A maintenance
 * refusal is {@link LauncherChannel#maintenanceRefusal} (legacy channels keep the 09-22 code {@code banned}). An
 * unknown account costs the same BCrypt check as a wrong password ({@link AccountSecrets#verifyPassword}).
 */
public final class LoginAction extends CredentialAction {

    @Override
    public String name() {
        return "login";
    }

    @Override
    public boolean requiresSession() {
        return false;
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public String rateBucket() {
        return LauncherRateLimits.AUTH;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    @Override
    public LauncherTransport transport() {
        return LauncherTransport.ANY;
    }

    @Override
    protected LauncherResult run(LauncherContext ctx, JsonNode request, AccountCredentials credentials) throws Exception {
        AccountAuthenticator.Outcome outcome = AccountAuthenticator.authenticate(ctx, credentials);
        if (!outcome.accepted()) {
            return outcome.refusal();
        }
        LauncherServices services = ctx.services();
        LauncherResult result = LauncherResult.ok(LauncherCodes.LOGGED_IN).with("userId", outcome.userId());
        if (ctx.channel().issuesSessions()) {
            result.with("token", services.sessions().issue(outcome.userId(), ctx.sourceIp()))
                    .with("sessionIdleSec", LauncherSessions.IDLE_MS / 1000)
                    .with("sessionMaxSec", LauncherSessions.MAX_AGE_MS / 1000);
        }
        return result;
    }

    /**
     * login.maintenance.message as a result field: at most {@value #MAX_MESSAGE} characters (CLI stdout < 8 KB). Also
     * used by the {@code status} view.
     */
    public static final int MAX_MESSAGE = 200;

    public static String clip(String message) {
        if (message == null) {
            return "";
        }
        return message.length() <= MAX_MESSAGE ? message : message.substring(0, MAX_MESSAGE);
    }
}
