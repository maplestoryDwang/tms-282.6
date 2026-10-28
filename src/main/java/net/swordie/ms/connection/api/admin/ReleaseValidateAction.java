package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.release.ReleaseFileEditor;

/**
 * {@code release.validate} {latest, minSupported, notes, publishedAt}: checks an edit with the server's release.json
 * rules (and the editor's: at most 10 notes of at most 100 characters) without writing; says whether minSupported
 * would go up ({@code raise}, from {@code fromMin}) so the console can warn before saving.
 */
final class ReleaseValidateAction implements AdminAction {
    @Override
    public String name() {
        return "release.validate";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        ReleaseFileEditor.Checked c = ReleaseAdmin.editor(ctx).check(request);
        if (!c.valid()) {
            return LauncherResult.fail(AdminCodes.REJECTED).with("problem", c.problem());
        }
        return LauncherResult.ok(AdminCodes.OK).with("info", ReleaseAdmin.info(c.info())).with("raise", c.raise())
                .with("fromMin", c.fromMin()).with("bytes", c.document().length);
    }
}
