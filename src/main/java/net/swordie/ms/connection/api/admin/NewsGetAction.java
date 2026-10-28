package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.news.NewsFileEditor;
import net.swordie.ms.connection.api.launcher.news.NewsSnapshot;

/**
 * {@code news.get} (公告): data/launcher/news.json as written (items in file order, SHA-256 for the save's conflict
 * check, the first validation problem if any), its preview in launcher order, and what the launchers are served right
 * now ({@code live}: the server's news including generated events / maintenance / game.slide, and the file status the
 * server sees).
 */
final class NewsGetAction implements AdminAction {
    @Override
    public String name() {
        return "news.get";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        NewsFileEditor editor = NewsFileEditor.under(ctx.env().root());
        NewsFileEditor.Current c = editor.read();
        NewsSnapshot live = ctx.env().services().news().snapshot();
        return LauncherResult.ok(AdminCodes.OK)
                .with("file", editor.file().toAbsolutePath().normalize().toString())
                .with("exists", c.exists())
                .with("sha256", c.sha256())
                .with("items", c.items())
                .with("problem", c.problem())
                .with("preview", c.preview())
                .with("live", live.itemMaps())
                .with("liveStatus", live.fileStatus())
                .with("liveVersion", live.version())
                .with("reloadSec", 5);
    }
}
