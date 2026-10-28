package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.release.ClientFileSet;
import net.swordie.ms.connection.api.launcher.release.ClientFilesEditor;
import net.swordie.ms.connection.api.launcher.release.DownloadSlots;
import net.swordie.ms.connection.api.launcher.release.LauncherClientFiles;
import net.swordie.ms.console.config.ServerRuntimeConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 「發佈」 → 「客戶端檔案」 admin actions (cat-launcher 1.2.3): {@code clientfiles.get} (what is published now, the
 * imported versions, the client-sign outputs that can be imported, the running import, the transfers),
 * {@code clientfiles.import} {version} (starts the copy on its own thread; the console polls get) and
 * {@code clientfiles.disable} (stop publishing). The server holds no key and signs nothing.
 */
final class ClientFilesAdmin {
    private ClientFilesAdmin() {
    }

    static ClientFilesEditor editor(AdminContext ctx) {
        return ClientFilesEditor.under(ctx.env().root(), ClientFilesEditor.configuredDist(ServerRuntimeConfig.stringValue(ClientFilesEditor.DIST_KEY)));
    }

    /** What launchers get: the process-wide instance when the admin root is the working directory (always on a server). */
    static LauncherClientFiles live(AdminContext ctx) {
        Path root = ctx.env().root().toAbsolutePath().normalize();
        if (root.equals(Path.of("").toAbsolutePath().normalize())) {
            return LauncherClientFiles.standard();
        }
        return LauncherClientFiles.under(root, ctx.env().clock());
    }

    static final class Get implements AdminAction {
        @Override
        public String name() {
            return "clientfiles.get";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            ClientFilesEditor ed = editor(ctx);
            LauncherClientFiles.Snapshot s = live(ctx).snapshot();
            Map<String, Object> live = new LinkedHashMap<>();
            live.put("state", s.state());
            live.put("problem", s.problem());
            live.put("activeVersion", s.activeVersion());
            ClientFileSet set = s.set();
            if (set != null) {
                live.put("summary", set.summary());
                live.put("files", set.fileMaps());
                live.put("test", set.test());
                live.put("keyId", set.keyId());
            }
            List<Map<String, Object>> candidates = new ArrayList<>();
            ed.candidates().forEach(c -> candidates.add(c.toMap()));
            DownloadSlots slots = DownloadSlots.client();
            Map<String, Object> downloads = new LinkedHashMap<>();
            downloads.put("active", slots.list());
            downloads.put("started", slots.started());
            downloads.put("refused", slots.refused());
            return LauncherResult.ok(AdminCodes.OK)
                    .with("dir", ed.dir().toAbsolutePath().normalize().toString())
                    .with("distDir", ed.dist().toString())
                    .with("live", live)
                    .with("imported", ed.imported())
                    .with("candidates", candidates)
                    .with("job", ClientFilesEditor.job().toMap())
                    .with("downloads", downloads)
                    .with("allowTestKey", ReleaseAdmin.allowTestKey());
        }
    }

    static final class Import implements AdminAction {
        @Override
        public String name() {
            return "clientfiles.import";
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
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            LauncherClientFiles live = live(ctx);
            ClientFilesEditor.Started r = editor(ctx).startImport(AdminParams.text(request, "version"), ReleaseAdmin.allowTestKey(), live::invalidate);
            return switch (r.code()) {
                case "started" -> {
                    LauncherResult ok = LauncherResult.ok(AdminCodes.OK).with("started", true);
                    r.extra().forEach(ok::with);
                    yield ok;
                }
                case "busy" -> LauncherResult.fail(AdminCodes.CONFLICT).with("problem", r.problem());
                default -> {
                    LauncherResult f = LauncherResult.fail(AdminCodes.REJECTED).with("problem", r.problem());
                    r.extra().forEach(f::with);
                    yield f;
                }
            };
        }
    }

    static final class Disable implements AdminAction {
        @Override
        public String name() {
            return "clientfiles.disable";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
            editor(ctx).disable();
            live(ctx).invalidate();
            return LauncherResult.ok(AdminCodes.OK).with("reloadSec", 5);
        }
    }
}
