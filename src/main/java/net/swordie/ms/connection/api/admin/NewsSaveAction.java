package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.news.NewsFileEditor;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code news.save} {items, baseSha256}: validates and writes data/launcher/news.json (previous file kept as
 * news.json.bak); refused when the file changed since the console read it. The launchers see the new notices after
 * the server's hot reload (at most 5 s).
 */
final class NewsSaveAction implements AdminAction {
    @Override
    public String name() {
        return "news.save";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        JsonNode items = request.get("items");
        List<String> ids = new ArrayList<>();
        if (items != null && items.isArray()) {
            for (JsonNode i : items) {
                ids.add(i.path("id").asText("?"));
            }
        }
        return "items=" + ids.size() + " ids=" + ids;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        NewsFileEditor editor = NewsFileEditor.under(ctx.env().root());
        NewsFileEditor.Saved s = editor.save(request.get("items"), AdminParams.text(request, "baseSha256"));
        return switch (s.code()) {
            case "ok" -> LauncherResult.ok(AdminCodes.OK).with("sha256", s.sha256())
                    .with("backup", s.backup() == null ? null : s.backup().toString())
                    .with("preview", s.preview()).with("reloadSec", 5);
            case "conflict" -> LauncherResult.fail(AdminCodes.CONFLICT).with("sha256", s.sha256());
            default -> LauncherResult.fail(AdminCodes.REJECTED).with("problem", s.problem());
        };
    }
}
