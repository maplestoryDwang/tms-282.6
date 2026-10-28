package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * {@code changePin} (P7, session) {currentPassword, newPin}: BCrypt check of the current password; the new second
 * password ([0-9]{6,16}) must not equal the login password ({@code sameAsPin}); written to the row and every
 * in-memory copy ({@link AccountSecretWriter}), so the next character select asks for the new one. Sessions stay.
 * Answers {@code pinChanged}.
 */
public final class ChangePinAction extends AccountWriteAction {

    @Override
    public String name() {
        return "changePin";
    }

    @Override
    protected String validate(JsonNode request, String currentPassword) {
        return PIN.matcher(LauncherJson.text(request, "newPin")).matches() ? null : "newPin";
    }

    @Override
    protected LauncherResult write(LauncherContext ctx, JsonNode request, String currentPassword, AccountSecretWriter.Row row)
            throws Exception {
        String pin = LauncherJson.text(request, "newPin");
        if (pin.equals(currentPassword)) {
            return LauncherResult.fail(LauncherCodes.SAME_AS_PIN);
        }
        AccountSecretWriter.Outcome o = AccountSecretWriter.write(ctx.services().onlineAccounts(), row,
                AccountSecretWriter.Field.PIN, AccountSecrets.hash(pin));
        if (o != AccountSecretWriter.Outcome.WRITTEN) {
            return conflict(o);
        }
        System.out.printf("[HttpAuthApi] changePin: account %d%n", row.id());
        return LauncherResult.ok(LauncherCodes.PIN_CHANGED);
    }
}
