package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.AccountType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * A username + password checked like the game login (CP0074, {@code Tms282AuthHandlers.authenticate}) does, in the same
 * order: IP ban, password, users.banExpireDate / account_block ban, login.maintenance. Shared by {@link LoginAction}
 * and the CLI's per-request session ({@link SessionSource#CREDENTIALS}), so both refuse exactly alike.
 * <p>
 * CP0074 answers every refusal but maintenance with the same LP0000 code 4; the launcher tells them apart
 * ({@code banned} only after a correct password). Failed passwords feed {@link LoginBackoff}. A maintenance refusal is
 * {@link LauncherChannel#maintenanceRefusal} (legacy channels keep the 09-22 code {@code banned}). An unknown account
 * costs the same BCrypt check as a wrong password ({@link AccountSecrets#verifyPassword}). Never logs.
 */
final class AccountAuthenticator {

    /** {@link #accepted()}: {@link #userId()} is the verified users.id; otherwise {@link #refusal()} is the answer. */
    record Outcome(int userId, LauncherResult refusal) {
        boolean accepted() {
            return refusal == null;
        }

        static Outcome accept(int userId) {
            return new Outcome(userId, null);
        }

        static Outcome refuse(LauncherResult refusal) {
            return new Outcome(0, refusal);
        }
    }

    private record Row(int id, String hash, Timestamp banExpire, AccountType accountType) {
    }

    private AccountAuthenticator() {
    }

    static Outcome authenticate(LauncherContext ctx, AccountCredentials credentials) throws Exception {
        LauncherServices services = ctx.services();
        LoginBackoff backoff = services.loginBackoff();
        long wait = backoff.retryAfterMs(credentials.username(), ctx.sourceIp());
        if (wait > 0) {
            return Outcome.refuse(LauncherResult.fail(ctx.channel().rateLimitedCode())
                    .with("retryAfterSec", LauncherPolicies.seconds(wait)));
        }
        try (Connection con = DatabaseManager.getConnection()) {
            LoginGate gate = services.loginGate();
            // After getConnection: BanService loads its cache only once the pool is up.
            if (gate.ipBanned(ctx.sourceIp())) {
                return Outcome.refuse(LauncherResult.fail(LauncherCodes.BANNED));
            }
            Row row = find(con, credentials.username());
            if (!AccountSecrets.verifyPassword(credentials.password(), row == null ? null : row.hash())) {
                backoff.failure(credentials.username(), ctx.sourceIp());
                return Outcome.refuse(LauncherResult.fail(LauncherCodes.WRONG_CREDENTIALS));
            }
            backoff.success(credentials.username(), ctx.sourceIp());
            if ((row.banExpire() != null && row.banExpire().getTime() > System.currentTimeMillis())
                    || gate.accountBanned(row.id())) {
                services.sessions().revokeAll(row.id());
                return Outcome.refuse(LauncherResult.fail(LauncherCodes.BANNED));
            }
            if (gate.refusedByMaintenance(row.accountType())) {
                return Outcome.refuse(ctx.channel().maintenanceRefusal(LoginAction.clip(gate.maintenanceMessage())));
            }
            return Outcome.accept(row.id());
        }
    }

    /** The single row of {@code name}; null when absent or (defensively) duplicated. */
    private static Row find(Connection con, String name) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("SELECT id, password, banExpireDate, accounttype FROM users WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Row row = new Row(rs.getInt("id"), rs.getString("password"), rs.getTimestamp("banExpireDate"),
                        AccountType.getByVal(rs.getInt("accounttype")));
                return rs.next() ? null : row;
            }
        }
    }
}
