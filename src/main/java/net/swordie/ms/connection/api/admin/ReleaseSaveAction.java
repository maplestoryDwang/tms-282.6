package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.release.ReleaseFileEditor;

/**
 * {@code release.save} {latest, minSupported, notes, publishedAt, baseSha256, confirmRaise}: writes release.json
 * (download and other keys unchanged; previous file kept as release.json.bak); refused when the file changed since the
 * console read it ({@code conflict}) or when minSupported goes up without {@code confirmRaise: true}
 * ({@code confirmRequired}, with {@code fromMin} / {@code minSupported}: the console asks whether every friend already
 * has the new launcher). The launchers see the change within 5 s.
 */
final class ReleaseSaveAction implements AdminAction {
    @Override
    public String name() {
        return "release.save";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        return "latest=" + AdminParams.text(request, "latest") + " minSupported=" + AdminParams.text(request, "minSupported")
                + " notes=" + (request.path("notes").isArray() ? request.path("notes").size() : 0)
                + " confirmRaise=" + request.path("confirmRaise").asBoolean(false);
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        ReleaseFileEditor.Saved s = ReleaseAdmin.editor(ctx).save(request, AdminParams.text(request, "baseSha256"),
                request.path("confirmRaise").asBoolean(false));
        return switch (s.code()) {
            case "ok" -> {
                ReleaseAdmin.release(ctx).source().invalidate();
                yield LauncherResult.ok(AdminCodes.OK).with("sha256", s.sha256())
                        .with("backup", s.backup() == null ? null : s.backup().toString())
                        .with("info", ReleaseAdmin.info(s.info())).with("raised", s.extra().get("raised")).with("reloadSec", 5);
            }
            case "conflict" -> LauncherResult.fail(AdminCodes.CONFLICT).with("sha256", s.sha256());
            case "confirmRequired" -> LauncherResult.fail(AdminCodes.CONFIRM_REQUIRED).with("problem", s.problem())
                    .with("fromMin", s.fromMin()).with("minSupported", s.info().minSupported());
            default -> LauncherResult.fail(AdminCodes.REJECTED).with("problem", s.problem());
        };
    }
}
