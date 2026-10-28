package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.daily.DailyRewardTable;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import java.util.ArrayList;

/** Host-only reward editing, audited by the existing admin API. */
final class DailySaveAdminAction implements AdminAction {
    public String name() { return "daily.save"; }
    public boolean mutating() { return true; }
    public String auditSummary(JsonNode request) { return "daily rewards rows=" + request.path("rewards").size(); }
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        JsonNode input = request.get("rewards");
        if (input == null || !input.isArray() || input.size() != 28) return LauncherResult.fail(AdminCodes.REJECTED).with("problem", "必須提供 28 天獎勵。");
        var rows = new ArrayList<DailyRewardTable.Reward>();
        for (int i = 0; i < 28; i++) {
            JsonNode n = input.get(i);
            for (String key : new String[]{"day", "itemId", "count", "periodDays"}) {
                if (!n.path(key).isIntegralNumber() || !n.path(key).canConvertToInt()) return LauncherResult.fail(AdminCodes.REJECTED).with("problem", "第 " + (i+1) + " 天：欄位必須是整數。");
            }
            int item = n.path("itemId").asInt();
            if (n.path("day").asInt() != i + 1 || !Tms282ItemAvailability.isGrantable(item))
                return LauncherResult.fail(AdminCodes.REJECTED).with("problem", "第 " + (i+1) + " 天：道具不存在或無法發放。");
            String name = StringData.getItemStringById(item);
            if (name == null || name.isBlank()) return LauncherResult.fail(AdminCodes.REJECTED).with("problem", "找不到道具名稱：" + item);
            int count = n.path("count").asInt();
            if (item / 1_000_000 == 1 && count != 1) return LauncherResult.fail(AdminCodes.REJECTED).with("problem", "裝備獎勵每次只能發 1 件。");
            rows.add(new DailyRewardTable.Reward(i + 1, item, count, n.path("periodDays").asInt(), name));
        }
        DailyRewardTable table = ctx.env().services().rewards();
        try {
            String code = table.save(rows, AdminParams.text(request, "baseVersion"));
            if (code.equals("conflict")) return LauncherResult.fail(AdminCodes.CONFLICT);
        } catch (IllegalArgumentException e) {
            return LauncherResult.fail(AdminCodes.REJECTED).with("problem", e.getMessage());
        }
        int refreshed = net.swordie.ms.client.daily.DailyGift.refreshOnline();
        // 2026-10-08: the launcher asks for the icons of the new rewards - extract the missing ones now, off this thread
        net.swordie.ms.connection.api.launcher.daily.ItemIconService.standard()
                .ensureAsync(net.swordie.ms.connection.api.launcher.daily.ItemIconAction.allowed(table));
        return LauncherResult.ok(AdminCodes.OK).with("version", table.version()).with("refreshed", refreshed);
    }
}
