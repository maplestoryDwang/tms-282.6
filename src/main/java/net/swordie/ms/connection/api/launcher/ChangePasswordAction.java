package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code changePassword} (P7, session) {currentPassword, newPassword}: BCrypt check of the current password; the new
 * one ([A-Za-z0-9]{8,32}, not the current one) must not equal the second password ({@code sameAsPin}); written to
 * the row and every in-memory copy of the account ({@link AccountSecretWriter}). Success revokes every other launcher
 * session of the account (the caller's stays) and clears the login back-offs of the account (launcher and CP0074).
 * Answers {@code passwordChanged} + {@code revokedSessions}.
 */
public final class ChangePasswordAction extends AccountWriteAction {

    @Override
    public String name() {
        return "changePassword";
    }

    @Override
    protected String validate(JsonNode request, String currentPassword) {
        String next = LauncherJson.text(request, "newPassword");
        if (!PASSWORD.matcher(next).matches() || next.equals(currentPassword)) {
            return "newPassword";
        }
        return null;
    }

    @Override
    protected LauncherResult write(LauncherContext ctx, JsonNode request, String currentPassword, AccountSecretWriter.Row row)
            throws Exception {
        String next = LauncherJson.text(request, "newPassword");
        if (AccountSecrets.matches(next, row.pin())) {
            return LauncherResult.fail(LauncherCodes.SAME_AS_PIN);
        }
        String hash = AccountSecrets.hash(next);
        AccountSecretWriter.Outcome o = AccountSecretWriter.write(ctx.services().onlineAccounts(), row,
                AccountSecretWriter.Field.PASSWORD, hash);
        if (o != AccountSecretWriter.Outcome.WRITTEN) {
            return conflict(o);
        }
        LauncherSession keep = ctx.session().transientSession() ? null : ctx.session();
        int revoked = ctx.services().sessions().revokeOthers(row.id(), keep);
        ctx.services().loginBackoff().clearAccount(row.name());
        net.swordie.ms.handlers.login.tms282.Tms282LoginThrottle.standard().clearAccount(row.name());
        System.out.printf("[HttpAuthApi] changePassword: account %d, %d other session(s) revoked%n", row.id(), revoked);
        return LauncherResult.ok(LauncherCodes.PASSWORD_CHANGED).with("revokedSessions", revoked);
    }
}
