package net.swordie.ms.connection.api.launcher.gift;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;

import java.sql.Connection;

/**
 * {@code giftHistory} (P7, session, read) {limit 1..20, default 3}: the account's latest redemptions - redeemedAt,
 * title, characterId, characterName, items, delivered. Never a code. No tables yet: an empty list.
 */
public final class GiftHistoryAction extends SessionReadAction {

    @Override
    public String name() {
        return "giftHistory";
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        int limit = 3;
        JsonNode l = request == null ? null : request.get("limit");
        if (l != null && !l.isNull()) {
            if (!l.isIntegralNumber() || l.intValue() < 1 || l.intValue() > 20) {
                return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "limit");
            }
            limit = l.intValue();
        }
        return LauncherResult.ok(LauncherCodes.OK).with("history", GiftCodeService.standard().history(con, userId, limit));
    }
}
