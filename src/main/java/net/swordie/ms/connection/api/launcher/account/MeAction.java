package net.swordie.ms.connection.api.launcher.account;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;

import java.sql.Connection;
import java.util.Optional;

/**
 * {@code me} (session): the caller's own account - username, createdAt, vipGrade, characterSlots, online
 * ({@link AccountView}); never email, pic or password. The account row being gone answers {@code sessionExpired}.
 */
public final class MeAction extends SessionReadAction {
    private final AccountReader reader = new AccountReader();

    @Override
    public String name() {
        return "me";
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        Optional<AccountView> me = reader.read(con, userId, ctx.services());
        if (me.isEmpty()) {
            return LauncherResult.fail(LauncherCodes.SESSION_EXPIRED);
        }
        LauncherResult result = LauncherResult.ok(LauncherCodes.OK);
        me.get().toMap().forEach(result::with);
        return result;
    }
}
