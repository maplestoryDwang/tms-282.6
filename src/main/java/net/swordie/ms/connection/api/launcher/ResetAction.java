package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@code reset}: new password with the recovery code, rotating the code (LocalAccountCli since 09-22). Offline
 * accounts only ({@code alreadyOnline} otherwise): users.clientstate = 0 and no client of this server process holding
 * the User ({@link OnlineAccounts}; clientstate stays 0 at character select, where the cached row would be saved
 * back over the new password on disconnect). Success revokes every launcher session of the account and clears its
 * login back-off, the launcher's and the game login's (CP0074): the recovery code proves ownership, so the new
 * password works at once.
 */
public final class ResetAction extends CredentialAction {

    @Override
    public String name() {
        return "reset";
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
        return LauncherRateLimits.AUTH;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    /**
     * Only the server process can see whether it holds the account in memory, so while 127.0.0.1:8484 listens the
     * CLI answers {@code serverRunning} and the launcher goes through 127.0.0.1:8600.
     */
    @Override
    public LauncherTransport transport() {
        return LauncherTransport.SERVER_WHEN_RUNNING;
    }

    @Override
    protected LauncherResult run(LauncherContext ctx, JsonNode request, AccountCredentials credentials) throws Exception {
        LauncherServices services = ctx.services();
        try (Connection con = DatabaseManager.getConnection()) {
            if (!schemaReady(con)) {
                return LauncherResult.fail(LauncherCodes.SETUP_REQUIRED);
            }
            String recovery = AccountSecrets.normalizeRecovery(LauncherJson.text(request, "recoveryCode"));
            if (!AccountSecrets.wellFormedRecovery(recovery)) {
                return LauncherResult.fail(LauncherCodes.INVALID_RECOVERY);
            }
            LauncherResult result = reset(con, credentials, recovery, services.onlineAccounts());
            if (result.ok()) {
                services.sessions().revokeAll((Integer) result.get("userId"));
                services.loginBackoff().clearAccount(credentials.username());
                // the game login (CP0074) keeps its own back-off: the reset unlocks it as well
                net.swordie.ms.handlers.login.tms282.Tms282LoginThrottle.standard().clearAccount(credentials.username());
            }
            return result;
        }
    }

    private static LauncherResult reset(Connection con, AccountCredentials credentials, String recovery,
                                        OnlineAccounts online) throws Exception {
        con.setAutoCommit(false);
        try {
            int userId;
            byte[] stored;
            String pin;
            int clientState;
            // Lock both rows before checking the code. Concurrent reuse sees the rotated hash.
            try (PreparedStatement ps = con.prepareStatement("SELECT u.id, u.pic, u.clientstate, r.code_hash "
                    + "FROM users u JOIN user_recovery_codes r ON r.user_id = u.id WHERE u.name = ? FOR UPDATE")) {
                ps.setString(1, credentials.username());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return LauncherResult.fail(LauncherCodes.INVALID_RECOVERY);
                    }
                    userId = rs.getInt("id");
                    stored = rs.getBytes("code_hash");
                    pin = rs.getString("pic");
                    clientState = rs.getInt("clientstate");
                }
            }
            if (!MessageDigest.isEqual(stored, AccountSecrets.digest(recovery))) {
                con.rollback();
                return LauncherResult.fail(LauncherCodes.INVALID_RECOVERY);
            }
            if (clientState != 0 || online.online(userId)) {
                con.rollback();
                return LauncherResult.fail(LauncherCodes.ALREADY_ONLINE);
            }
            if (AccountSecrets.matches(credentials.password(), pin)) {
                con.rollback();
                return LauncherResult.fail(LauncherCodes.SAME_AS_PIN);
            }
            String code = AccountSecrets.newRecoveryCode();
            String hash = AccountSecrets.hash(credentials.password());
            try (PreparedStatement ps = con.prepareStatement("UPDATE users SET password = ? WHERE id = ?")) {
                ps.setString(1, hash);
                ps.setInt(2, userId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Account changed");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE user_recovery_codes SET code_hash = ?, created_at = CURRENT_TIMESTAMP WHERE user_id = ?")) {
                ps.setBytes(1, AccountSecrets.digest(code));
                ps.setInt(2, userId);
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Recovery changed");
                }
            }
            con.commit();
            try {
                online.passwordChanged(userId, hash);
            } catch (RuntimeException e) {
                // Committed: the reset stands; only the racing in-memory copy (if any) keeps the old hash.
                System.out.printf("[HttpAuthApi] reset: in-memory copy not updated (%s)%n", e.getClass().getSimpleName());
            }
            return LauncherResult.ok(LauncherCodes.RESET).with("userId", userId)
                    .with("recoveryCode", AccountSecrets.displayRecovery(code));
        } catch (Exception e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(true);
        }
    }
}
