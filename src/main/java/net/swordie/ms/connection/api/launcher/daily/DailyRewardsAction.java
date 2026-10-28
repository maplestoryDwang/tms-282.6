package net.swordie.ms.connection.api.launcher.daily;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.ReadAction;
import net.swordie.ms.constants.DailyGiftConstants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code dailyRewards} (public): the 28 daily gift rewards ({@link DailyRewardTable}: day, itemId, count, periodDays,
 * name), the table's {@code version} (the launcher caches until it changes) and the claim requirement the game
 * advertises ({@link DailyGiftConstants#reqLevel()}: 33 on 265, 10 on 282; 300 monsters - shown, not enforced by the server). No database. In the server process the
 * table is compared once with the game's.
 */
public final class DailyRewardsAction extends ReadAction {

    @Override
    public String name() {
        return "dailyRewards";
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) {
        DailyRewardTable table = ctx.services().rewards();
        if (ctx.services().memory().available()) {
            table.checkAgainstGameOnce(DailyGiftConstants::rewards);
        }
        var catalog = table.snapshot();
        List<Map<String, Object>> rewards = new ArrayList<>();
        for (DailyRewardTable.Reward r : catalog.rewards()) {
            rewards.add(r.toMap());
        }
        Map<String, Object> requirement = new LinkedHashMap<>();
        requirement.put("level", DailyGiftConstants.reqLevel());
        requirement.put("monsters", DailyGiftConstants.REQ_MONSTER);
        return LauncherResult.ok(LauncherCodes.OK).with("version", catalog.version()).with("rewards", rewards)
                .with("requirement", requirement);
    }
}
