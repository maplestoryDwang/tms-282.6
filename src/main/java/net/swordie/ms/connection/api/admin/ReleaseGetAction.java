package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.release.ClientManifestMeta;
import net.swordie.ms.connection.api.launcher.release.DownloadSlots;
import net.swordie.ms.connection.api.launcher.release.ReleaseFileEditor;
import net.swordie.ms.connection.api.launcher.release.ReleaseSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code release.get} (發佈): data/launcher/release.json as written (text, SHA-256 for the save's conflict check, the
 * fields, the first problem), what the launchers get right now ({@code live}: launcherVersion's values and whether the
 * download is offered, with the reason when not), the files of data/launcher/releases, the release-sign.mjs outputs
 * that can be imported (dist-release), manifestMeta's values, the downloads running now and the launcher versions seen
 * since the server started (X-Cat-Launcher; helps to decide when everybody has the new launcher).
 */
final class ReleaseGetAction implements AdminAction {
    @Override
    public String name() {
        return "release.get";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        ReleaseFileEditor editor = ReleaseAdmin.editor(ctx);
        ReleaseFileEditor.Current c = editor.read();
        ReleaseSnapshot live = ReleaseAdmin.release(ctx).snapshot();
        Map<String, Object> liveMap = new LinkedHashMap<>();
        liveMap.put("status", live.status());
        liveMap.put("problem", live.problem());
        liveMap.put("info", ReleaseAdmin.info(live.info()));
        liveMap.put("download", ReleaseAdmin.download(live.download()));
        liveMap.put("downloadState", live.downloadState());
        String registered = c.info() != null && c.info().download() != null ? c.info().download().file()
                : live.info() != null && live.info().download() != null ? live.info().download().file() : null;
        List<Map<String, Object>> candidates = new ArrayList<>();
        editor.candidates().forEach(x -> candidates.add(x.toMap()));
        String[] why = new String[1];
        Optional<ClientManifestMeta.Meta> m = ClientManifestMeta.standard().read(why);
        Map<String, Object> manifest = new LinkedHashMap<>();
        if (m.isPresent()) {
            manifest.put("ok", true);
            manifest.put("referenceSha256", m.get().referenceSha256());
            manifest.put("exeSha256", m.get().exeSha256());
            manifest.put("count", m.get().count());
            manifest.put("reference", m.get().reference().toString());
            manifest.put("clientExe", m.get().clientExe().toString());
        } else {
            manifest.put("ok", false);
            manifest.put("problem", why[0]);
            manifest.put("clientExe", ClientManifestMeta.configuredClientExe().toString());
        }
        DownloadSlots slots = DownloadSlots.shared();
        Map<String, Object> downloads = new LinkedHashMap<>();
        downloads.put("active", slots.list());
        downloads.put("started", slots.started());
        downloads.put("refused", slots.refused());
        downloads.put("perIp", slots.perIp());
        downloads.put("total", slots.total());
        return LauncherResult.ok(AdminCodes.OK)
                .with("file", editor.file().toAbsolutePath().normalize().toString())
                .with("releasesDir", editor.releases().toAbsolutePath().normalize().toString())
                .with("distDir", editor.dist().toString())
                .with("exists", c.exists())
                .with("sha256", c.sha256())
                .with("text", c.text())
                .with("problem", c.problem())
                .with("fields", fields(c))
                .with("live", liveMap)
                .with("releases", editor.releasesFolder(registered))
                .with("candidates", candidates)
                .with("manifest", manifest)
                .with("downloads", downloads)
                .with("launcherVersions", ctx.env().traffic().byVersion())
                .with("allowTestKey", ReleaseAdmin.allowTestKey())
                .with("reloadSec", 5);
    }

    /** The editable values as written (also when the file breaks a rule, so the console can fix it). */
    private static Map<String, Object> fields(ReleaseFileEditor.Current c) {
        Map<String, Object> f = new LinkedHashMap<>();
        if (c.root() == null) {
            return f;
        }
        f.put("latest", c.root().path("latest").isTextual() ? c.root().path("latest").textValue() : "");
        f.put("minSupported", c.root().path("minSupported").isTextual() ? c.root().path("minSupported").textValue() : "");
        List<String> notes = new ArrayList<>();
        c.root().path("notes").forEach(n -> notes.add(n.isTextual() ? n.textValue() : n.toString()));
        f.put("notes", notes);
        f.put("publishedAt", c.root().path("publishedAt").isTextual() ? c.root().path("publishedAt").textValue() : "");
        f.put("download", c.root().has("download") ? c.root().get("download") : null);
        return f;
    }
}
