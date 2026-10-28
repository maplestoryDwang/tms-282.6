package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.client.User;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.DevAccounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

/**
 * {@code register}: account + second password (pin) + recovery code in one transaction, as LocalAccountCli did since
 * 09-22 ({@code registered} + userId + recoveryCode). Never issues a session.
 */
public final class RegisterAction extends CredentialAction {

    @Override
    public String name() {
        return "register";
    }

    @Override
    public boolean requiresSession() {
        return false;
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String rateBucket() {
        return LauncherRateLimits.REGISTER;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    /** New offline accounts are not cached by the server, so the CLI may still write them. */
    @Override
    public LauncherTransport transport() {
        return LauncherTransport.ANY;
    }

    @Override
    protected LauncherResult run(LauncherContext ctx, JsonNode request, AccountCredentials credentials) throws Exception {
        try (Connection con = DatabaseManager.getConnection()) {
            if (!schemaReady(con)) {
                return LauncherResult.fail(LauncherCodes.SETUP_REQUIRED);
            }
            String pin = LauncherJson.text(request, "pin");
            if (!pin.matches("[0-9]{6,16}") || pin.equals(credentials.password())) {
                return LauncherResult.fail(LauncherCodes.INVALID_PIN);
            }
            if (DevAccounts.isForbiddenID(credentials.username().toLowerCase(Locale.ROOT))) {
                return LauncherResult.fail(LauncherCodes.NAME_UNAVAILABLE);
            }
            return register(con, credentials, pin);
        }
    }

    private static LauncherResult register(Connection con, AccountCredentials credentials, String pin) throws Exception {
        User user = new User(credentials.username(), AccountSecrets.hash(credentials.password()));
        user.setPic(AccountSecrets.hash(pin));
        user.setEmail("");
        String code = AccountSecrets.newRecoveryCode();
        con.setAutoCommit(false);
        try {
            user.insertUserToSQL(con);
            if (user.getId() <= 0) {
                throw new SQLException("Insert failed");
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO user_recovery_codes(user_id, code_hash) VALUES (?, ?)")) {
                ps.setInt(1, user.getId());
                ps.setBytes(2, AccountSecrets.digest(code));
                ps.executeUpdate();
            }
            con.commit();
            return LauncherResult.ok(LauncherCodes.REGISTERED).with("userId", user.getId())
                    .with("recoveryCode", AccountSecrets.displayRecovery(code));
        } catch (SQLException e) {
            con.rollback();
            if (e.getErrorCode() == 1062) {
                return LauncherResult.fail(LauncherCodes.NAME_UNAVAILABLE);
            }
            throw e;
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }
}
