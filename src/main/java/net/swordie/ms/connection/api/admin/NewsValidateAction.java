package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.news.NewsFileEditor;

/**
 * {@code news.validate} {items}: checks an edited item list with the server's own news.json rules without writing
 * anything; answers the first problem (a field path such as {@code items[2].kind}) or the preview in launcher order.
 */
final class NewsValidateAction implements AdminAction {
    @Override
    public String name() {
        return "news.validate";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        NewsFileEditor.Checked c = NewsFileEditor.under(ctx.env().root()).check(request.get("items"));
        if (!c.valid()) {
            return LauncherResult.fail(AdminCodes.REJECTED).with("problem", c.problem());
        }
        return LauncherResult.ok(AdminCodes.OK).with("preview", c.preview()).with("bytes", c.document().length);
    }
}
