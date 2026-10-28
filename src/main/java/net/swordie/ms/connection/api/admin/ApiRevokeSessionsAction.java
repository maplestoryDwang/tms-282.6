package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.util.Optional;

/**
 * {@code api.revokeSessions} {query}: drops every launcher session (bearer token) of one account - the launchers
 * then get {@code sessionExpired} and log in again. Game connections are not touched.
 */
final class ApiRevokeSessionsAction implements AdminAction {
    @Override
    public String name() {
        return "api.revokeSessions";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        return "query=" + AdminParams.text(request, "query");
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        String query = AccountLookup.clean(AdminParams.text(request, "query"));
        if (query == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "query");
        }
        Optional<AccountLookup.UserRow> user;
        try (Connection con = DatabaseManager.getConnection()) {
            user = AccountLookup.resolve(con, query);
        }
        if (user.isEmpty()) {
            return LauncherResult.fail(AdminCodes.NOT_FOUND);
        }
        int n = ctx.env().services().sessions().revokeAll(user.get().id());
        return LauncherResult.ok(AdminCodes.OK).with("userId", user.get().id()).with("name", user.get().name()).with("revoked", n);
    }
}
