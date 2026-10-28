package net.swordie.ms.connection.api.launcher.outfit;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherAction;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherRateLimits;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LauncherSession;
import net.swordie.ms.connection.api.launcher.LauncherTransport;
import net.swordie.ms.world.outfit.Outfit;
import net.swordie.ms.world.outfit.OutfitStore;

/**
 * {@code outfitLike} (launcher 1.3, session) {id}: one like per account and outfit, the same table the in-game
 * {@code @outfit} menu uses. Answers {@code ok} with {@code added} (false = this account had liked it already) and
 * the outfit's {@code likes}; {@code notFound} when it is gone. Bucket {@link LauncherRateLimits#GIFT}; only while
 * the server runs (it owns the outfit tables).
 */
public final class OutfitLikeAction implements LauncherAction {

    @Override
    public String name() {
        return "outfitLike";
    }

    @Override
    public boolean requiresSession() {
        return true;
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String rateBucket() {
        return LauncherRateLimits.GIFT;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    @Override
    public LauncherTransport transport() {
        return LauncherTransport.SERVER_ONLY;
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception {
        LauncherSession session = ctx.session();
        if (session == null) {
            return LauncherResult.fail(LauncherCodes.NEED_SESSION);
        }
        JsonNode id = request == null ? null : request.get("id");
        if (id == null || !id.isIntegralNumber() || !id.canConvertToInt() || id.intValue() <= 0) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "id");
        }
        OutfitStore store = OutfitStore.standard();
        if (store.get(id.intValue()) == null) {
            return LauncherResult.fail("notFound");
        }
        boolean added = store.like(id.intValue(), session.userId());
        Outfit now = store.get(id.intValue());
        return LauncherResult.ok(LauncherCodes.OK).with("id", id.intValue()).with("added", added).with("likes", now == null ? 0 : now.likes());
    }
}
