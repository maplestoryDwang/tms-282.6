package net.swordie.ms.connection.api.launcher.daily;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.client.daily.DailyStreakBonus;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.ReadAction;

import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@code itemIcon} (public, read) {itemId}: the icon of a sign-in reward as a base64 PNG ({@link ItemIconService}), so
 * the launcher shows the right picture for whatever the host puts on the 28-day board or in the streak bonuses without
 * a new launcher build. Only the items of the current board ({@link DailyRewardTable}) and of the streak bonuses
 * ({@link DailyStreakBonus#bonuses()}) are served: any other id is {@code notFound} and no work is done for it.
 * <p>
 * {@code notFound}: not a reward item, or the client files have no icon for it. {@code unavailable}: this host cannot
 * extract icons (or is busy doing so) - the launcher keeps its generic icon and may ask again later. Icons are extracted
 * when the board or the bonuses change; a request that still misses extracts every missing reward icon in one run
 * (server process only - the CLI answers from what is stored).
 */
public final class ItemIconAction extends ReadAction {
    public static final String NOT_FOUND = "notFound";

    @Override
    public String name() {
        return "itemIcon";
    }

    /** The items whose icon may be asked for: the 28 rewards and every streak bonus item. */
    public static Set<Integer> allowed(DailyRewardTable table) {
        Set<Integer> ids = new LinkedHashSet<>();
        try {
            for (DailyRewardTable.Reward r : table.snapshot().rewards()) {
                ids.add(r.itemId());
            }
        } catch (RuntimeException ignored) {
            // an unreadable board serves no board icons
        }
        try {
            for (DailyStreakBonus.Bonus b : DailyStreakBonus.bonuses()) {
                for (DailyStreakBonus.Item i : b.items()) {
                    ids.add(i.itemId());
                }
            }
        } catch (RuntimeException ignored) {
            // bonuses() keeps its last good list; anything else serves no bonus icons
        }
        return ids;
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) {
        JsonNode id = request == null ? null : request.get("itemId");
        if (id == null || !id.isIntegralNumber() || !id.canConvertToInt() || !ItemIconService.validId(id.intValue())) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "itemId");
        }
        int itemId = id.intValue();
        Set<Integer> allowed = allowed(ctx.services().rewards());
        if (!allowed.contains(itemId)) {
            return LauncherResult.fail(NOT_FOUND);
        }
        ItemIconService icons = ItemIconService.standard();
        byte[] png = icons.png(itemId, ctx.services().memory().available(), allowed);
        if (png == null) {
            return LauncherResult.fail(icons.knownMissing(itemId) ? NOT_FOUND : LauncherCodes.UNAVAILABLE);
        }
        return LauncherResult.ok(LauncherCodes.OK).with("itemId", itemId).with("png", Base64.getEncoder().encodeToString(png));
    }
}
