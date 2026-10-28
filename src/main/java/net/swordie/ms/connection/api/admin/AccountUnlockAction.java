package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.handlers.login.tms282.Tms282LoginThrottle;

import java.sql.Connection;
import java.util.Optional;

/**
 * {@code account.unlock} {userId} (解除鎖定): forgets the account's failed-password pairs from every source IP in both
 * back-offs (launcher 8600 and game login CP0074) - the same thing a successful launcher password reset does - and
 * the account's failed gift-code guesses (P7).
 */
final class AccountUnlockAction implements AdminAction {
    @Override
    public String name() {
        return "account.unlock";
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
        String name = user.get().name();
        int launcher = ctx.env().services().loginBackoff().clearAccount(name);
        int game = Tms282LoginThrottle.standard().clearAccount(name);
        // P7: the per-account limit of failed gift codes too
        net.swordie.ms.connection.api.launcher.gift.GiftCodeService.standard().clearFailures(user.get().id());
        return LauncherResult.ok(AdminCodes.OK).with("userId", user.get().id()).with("name", name)
                .with("launcherCleared", launcher).with("gameCleared", game);
    }
}
