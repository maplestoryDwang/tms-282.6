package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.world.tms282.audit.BanService;

import java.sql.Connection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@code account.unban} {userId} (解封): the in-game {@code !untempban} path - {@link BanService#liftBans} ends every
 * active account_block of the account (cache at once, end_time = now on the audit thread). The legacy
 * users.banExpireDate is shown by account.find but not changed here.
 */
final class AccountUnbanAction implements AdminAction {
    @Override
    public String name() {
        return "account.unban";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        return "userId=" + AdminParams.number(request, "userId", 1, Integer.MAX_VALUE);
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        Long id = AdminParams.number(request, "userId", 1, Integer.MAX_VALUE);
        if (id == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "userId");
        }
        Optional<AccountLookup.UserRow> user;
        try (Connection con = DatabaseManager.getConnection()) {
            user = AccountLookup.byId(con, id.intValue(), "id");
        }
        if (user.isEmpty()) {
            return LauncherResult.fail(AdminCodes.NOT_FOUND);
        }
        BanService.activeBan(user.get().id()); // the cache must be loaded before it is changed
        Integer lifted;
        try {
            lifted = BanService.liftBans(user.get().id(), null).get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            lifted = null;
        }
        return LauncherResult.ok(AdminCodes.OK).with("userId", user.get().id()).with("name", user.get().name())
                .with("lifted", lifted);
    }
}
