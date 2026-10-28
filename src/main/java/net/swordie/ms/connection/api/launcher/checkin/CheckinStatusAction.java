package net.swordie.ms.connection.api.launcher.checkin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.client.daily.DailyStreakBonus;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;

import java.sql.Connection;

/**
 * {@code checkinStatus} (session, read) {world}: the streak bonus of the caller's account in that world
 * ({@link DailyStreakBonus#status}) - streak (consecutive daily-gift claims this month), claimedToday, given (bonus
 * thresholds already given this month) and the bonuses. No account in the world: streak 0 and nothing given.
 */
public final class CheckinStatusAction extends SessionReadAction {

    @Override
    public String name() {
        return "checkinStatus";
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        JsonNode w = request == null ? null : request.get("world");
        if (w == null || !w.isIntegralNumber() || w.intValue() < 0 || w.intValue() > 255) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "world");
        }
        Integer accountId = CheckinAction.accountId(con, userId, w.intValue());
        DailyStreakBonus.Status st = accountId == null
                ? new DailyStreakBonus.Status(0, false, java.util.List.of(), DailyStreakBonus.bonuses())
                : DailyStreakBonus.status(con, accountId);
        return LauncherResult.ok(LauncherCodes.OK).with("world", w.intValue()).with("checkin", st.toMap());
    }
}
