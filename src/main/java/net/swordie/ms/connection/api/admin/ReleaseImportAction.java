package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.release.ReleaseFileEditor;

/**
 * {@code release.import} {version, baseSha256}: takes {@code <dist-release>\<version>\} as release-sign.mjs wrote it
 * (release-entry.json + exe): checks the entry and the exe (size, SHA-256), copies the exe into
 * data/launcher/releases, then writes release.json's {@code download} (the entry's five values unchanged),
 * {@code latest} (raised to that version) and {@code publishedAt}; minSupported is never changed here. A test-key entry
 * is refused unless this is a test server. The server holds no key and does not sign anything.
 */
final class ReleaseImportAction implements AdminAction {
    @Override
    public String name() {
        return "release.import";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        return "version=" + AdminParams.text(request, "version");
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        ReleaseFileEditor.Saved s = ReleaseAdmin.editor(ctx).importRelease(AdminParams.text(request, "version"),
                AdminParams.text(request, "baseSha256"), ReleaseAdmin.allowTestKey());
        return switch (s.code()) {
            case "ok" -> {
                ReleaseAdmin.release(ctx).source().invalidate();
                LauncherResult r = LauncherResult.ok(AdminCodes.OK).with("sha256", s.sha256())
                        .with("backup", s.backup() == null ? null : s.backup().toString()).with("info", ReleaseAdmin.info(s.info()));
                s.extra().forEach(r::with);
                yield r.with("reloadSec", 5);
            }
            case "conflict" -> LauncherResult.fail(AdminCodes.CONFLICT).with("sha256", s.sha256());
            default -> {
                LauncherResult r = LauncherResult.fail(AdminCodes.REJECTED).with("problem", s.problem());
                s.extra().forEach(r::with);
                yield r;
            }
        };
    }
}
