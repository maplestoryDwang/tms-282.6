package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Base of the username + password actions (login, register, reset). Keeps LocalAccountCli's order of checks so the
 * CLI answers exactly as before: malformed credentials ({@code invalidInput}) before the database guard
 * ({@code localOnly}), both before any connection.
 */
abstract class CredentialAction implements LauncherAction {

    @Override
    public final LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception {
        AccountCredentials credentials = AccountCredentials.parse(request);
        if (credentials == null) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT);
        }
        if (!LocalDatabaseGuard.accepted()) {
            return LauncherResult.fail(LauncherCodes.LOCAL_ONLY);
        }
        return run(ctx, request, credentials);
    }

    protected abstract LauncherResult run(LauncherContext ctx, JsonNode request, AccountCredentials credentials)
            throws Exception;

    /** users and user_recovery_codes are InnoDB and users.name has a whole-column unique index. */
    static boolean schemaReady(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('users', 'user_recovery_codes') AND ENGINE = 'InnoDB'");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next() || rs.getInt(1) != 2) {
                return false;
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND NON_UNIQUE = 0 GROUP BY INDEX_NAME HAVING COUNT(*) = 1 AND MAX(COLUMN_NAME) = 'name' AND MAX(SUB_PART) IS NULL) indexes_by_name");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }
}
