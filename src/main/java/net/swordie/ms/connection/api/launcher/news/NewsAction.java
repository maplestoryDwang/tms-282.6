package net.swordie.ms.connection.api.launcher.news;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.ReadAction;

/**
 * {@code news} (public): the notices shown now ({@link LauncherNews}: news.json, running events, maintenance,
 * game.slide), their {@code version} and news.json's {@code fileStatus}. Plain text; no database.
 */
public final class NewsAction extends ReadAction {

    @Override
    public String name() {
        return "news";
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) {
        NewsSnapshot s = ctx.services().news().snapshot();
        return LauncherResult.ok(LauncherCodes.OK).with("version", s.version()).with("items", s.itemMaps())
                .with("fileStatus", s.fileStatus());
    }
}
