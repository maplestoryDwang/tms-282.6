package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.daily.DailyStatusReader;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code daily.status} {query} (簽到): for each world account of one account, this month's daily gift state - the
 * launcher's {@link DailyStatusReader} (claimed count, today's claim, next reward, the claim dates of this month from
 * dailygift_log; the game's live copy while the account is online). SELECT only.
 */
final class DailyStatusAdminAction implements AdminAction {
    private final DailyStatusReader reader = new DailyStatusReader();

    @Override
    public String name() {
        return "daily.status";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        String query = AccountLookup.clean(AdminParams.text(request, "query"));
        if (query == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "query");
        }
        try (Connection con = DatabaseManager.getConnection()) {
            Optional<AccountLookup.UserRow> user = AccountLookup.resolve(con, query);
            if (user.isEmpty()) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND);
            }
            List<Integer> worlds = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement("SELECT DISTINCT worldid FROM accounts WHERE userid = ? ORDER BY worldid")) {
                ps.setInt(1, user.get().id());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        worlds.add(rs.getInt(1));
                    }
                }
            }
            List<Map<String, Object>> out = new ArrayList<>();
            for (int w : worlds) {
                out.add(reader.read(con, user.get().id(), w, ctx.env().services()).toMap());
            }
            return LauncherResult.ok(AdminCodes.OK).with("account", user.get().toMap()).with("worlds", out);
        }
    }
}
