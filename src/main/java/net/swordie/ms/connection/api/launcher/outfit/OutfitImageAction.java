package net.swordie.ms.connection.api.launcher.outfit;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;
import net.swordie.ms.connection.api.launcher.account.AvatarRenderService;
import net.swordie.ms.world.outfit.Outfit;
import net.swordie.ms.world.outfit.OutfitStore;

import java.sql.Connection;
import java.util.Base64;

/**
 * {@code outfitImage} (launcher 1.3, session, read) {id}: the paper doll of a shared outfit as a base64 PNG
 * ({@link AvatarRenderService}; the same cache as {@code avatar}). {@code notFound} when the outfit is gone,
 * {@code unavailable} when this host cannot draw it.
 */
public final class OutfitImageAction extends SessionReadAction {

    @Override
    public String name() {
        return "outfitImage";
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        JsonNode id = request == null ? null : request.get("id");
        if (id == null || !id.isIntegralNumber() || !id.canConvertToInt() || id.intValue() <= 0) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "id");
        }
        Outfit o = OutfitStore.standard().get(id.intValue());
        if (o == null) {
            return LauncherResult.fail("notFound");
        }
        byte[] png = AvatarRenderService.standard().png(new AvatarRenderService.Look(o.skin(), o.face(), o.hair(), o.elfEar(),
                net.swordie.ms.world.outfit.OutfitShare.renderItems(o)));
        if (png == null) {
            return LauncherResult.fail(LauncherCodes.UNAVAILABLE);
        }
        return LauncherResult.ok(LauncherCodes.OK).with("id", o.id()).with("png", Base64.getEncoder().encodeToString(png));
    }
}
