package net.swordie.ms.connection.api.launcher.outfit;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SessionReadAction;
import net.swordie.ms.world.outfit.Outfit;
import net.swordie.ms.world.outfit.OutfitStore;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code outfits} (launcher 1.3, session, read) {sort: "new" | "top" | "mine", page 0..200}: one page
 * ({@link OutfitStore#PAGE}) of the shared outfits ({@code @outfit} in the game) - id, name, by (character name),
 * likes, gender, pieces (how many visible equips), mine, createdAt - and {@code more} (a full page came back).
 * The picture of each is a separate call ({@code outfitImage}).
 */
public final class OutfitsAction extends SessionReadAction {

    @Override
    public String name() {
        return "outfits";
    }

    @Override
    protected LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId) throws Exception {
        String sort = request != null && request.path("sort").isTextual() ? request.path("sort").textValue() : "new";
        if (!sort.equals("new") && !sort.equals("top") && !sort.equals("mine")) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "sort");
        }
        JsonNode p = request == null ? null : request.get("page");
        int page = 0;
        if (p != null && !p.isNull()) {
            if (!p.isIntegralNumber() || p.intValue() < 0 || p.intValue() > 200) {
                return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "page");
            }
            page = p.intValue();
        }
        List<Outfit> rows = OutfitStore.standard().list(sort.equals("top"), sort.equals("mine") ? userId : 0, page);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Outfit o : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.id());
            m.put("name", o.name());
            m.put("by", o.charName());
            m.put("likes", o.likes());
            m.put("gender", o.gender());
            m.put("pieces", net.swordie.ms.world.outfit.OutfitShare.fashion(o.items()).size());
            m.put("mine", o.userId() == userId);
            m.put("createdAt", o.createdAt());
            out.add(m);
        }
        return LauncherResult.ok(LauncherCodes.OK).with("outfits", out).with("sort", sort).with("page", page)
                .with("more", rows.size() == OutfitStore.PAGE);
    }
}
