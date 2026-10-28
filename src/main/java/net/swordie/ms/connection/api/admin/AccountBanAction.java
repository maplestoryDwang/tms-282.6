package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.world.tms282.audit.BanService;
import net.swordie.ms.world.tms282.audit.BanType;

import java.sql.Connection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * {@code account.ban} {userId, type, hours, reason} (封鎖): the in-game {@code !tempban} path from the server process -
 * {@link BanService#tempBan} (cache at once, account_block row on the audit thread; hours 0 = permanent). A type that
 * blocks login also disconnects the account's clients (Client.close, which saves) and drops its launcher sessions,
 * like {@code !tempban} kicks the character.
 */
final class AccountBanAction implements AdminAction {
    static final int MAX_REASON = 100;
    static final long MAX_HOURS = 24L * 3650;

    @Override
    public String name() {
        return "account.ban";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        return "userId=" + AdminParams.number(request, "userId", 1, Integer.MAX_VALUE) + " type=" + AdminParams.text(request, "type")
                + " hours=" + AdminParams.number(request, "hours", 0, MAX_HOURS) + " reason=" + AdminParams.text(request, "reason");
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        Long id = AdminParams.number(request, "userId", 1, Integer.MAX_VALUE);
        if (id == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "userId");
        }
        BanType type = BanType.parse(AdminParams.text(request, "type"));
        if (type == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "type");
        }
        Long hours = AdminParams.number(request, "hours", 0, MAX_HOURS);
        if (hours == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "hours");
        }
        String reason = AdminParams.text(request, "reason").strip();
        if (reason.isEmpty() || reason.length() > MAX_REASON || reason.chars().anyMatch(Character::isISOControl)) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "reason");
        }
        Optional<AccountLookup.UserRow> user;
        try (Connection con = DatabaseManager.getConnection()) {
            user = AccountLookup.byId(con, id.intValue(), "id");
        }
        if (user.isEmpty()) {
            return LauncherResult.fail(AdminCodes.NOT_FOUND);
        }
        Boolean written;
        try {
            written = BanService.tempBan(user.get().id(), type, hours, reason + " (by admin console)").get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            written = null;
        }
        int disconnected = 0;
        int revoked = 0;
        if (type.blocksLogin()) {
            revoked = ctx.env().services().sessions().revokeAll(user.get().id());
            disconnected = ctx.env().server().disconnectUser(user.get().id());
        }
        return LauncherResult.ok(AdminCodes.OK).with("userId", user.get().id()).with("name", user.get().name())
                .with("type", type.name()).with("hours", hours)
                .with("databaseWritten", written).with("disconnected", disconnected).with("revokedSessions", revoked);
    }
}
