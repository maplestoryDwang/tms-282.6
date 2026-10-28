package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.daily.DailyRewardTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code daily.rewards} (簽到): the 28 rewards of data/protocol/tms282.3/daily-gift.tsv as the launcher shows them
 * (day, item id, count, granted expiry, item name). Read-only; the table is edited by hand, not from the console.
 */
final class DailyRewardsAdminAction implements AdminAction {
    @Override
    public String name() {
        return "daily.rewards";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) {
        DailyRewardTable table = ctx.env().services().rewards();
        List<Map<String, Object>> rewards = new ArrayList<>();
        try {
            for (DailyRewardTable.Reward r : table.rewards()) {
                rewards.add(r.toMap());
            }
            return LauncherResult.ok(AdminCodes.OK).with("rewards", rewards).with("version", table.version())
                    .with("problem", table.problem())
                    .with("file", ctx.env().resolve("data", "protocol", "tms282.3", "daily-gift.tsv").toString());
        } catch (RuntimeException e) {
            return LauncherResult.fail(AdminCodes.UNAVAILABLE).with("problem", e.getMessage());
        }
    }
}
