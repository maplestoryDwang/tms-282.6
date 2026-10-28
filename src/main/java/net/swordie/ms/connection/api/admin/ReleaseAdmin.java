package net.swordie.ms.connection.api.admin;

import net.swordie.ms.connection.api.launcher.LocalDatabaseGuard;
import net.swordie.ms.connection.api.launcher.release.LauncherRelease;
import net.swordie.ms.connection.api.launcher.release.ReleaseDownload;
import net.swordie.ms.connection.api.launcher.release.ReleaseFileEditor;
import net.swordie.ms.connection.api.launcher.release.ReleaseInfo;
import net.swordie.ms.console.config.ServerRuntimeConfig;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared pieces of the 「發佈」 actions (release.get / validate / save / import). */
final class ReleaseAdmin {
    private ReleaseAdmin() {
    }

    static ReleaseFileEditor editor(AdminContext ctx) {
        return ReleaseFileEditor.under(ctx.env().root(), ReleaseFileEditor.configuredDist(ServerRuntimeConfig.stringValue(ReleaseFileEditor.DIST_KEY)));
    }

    /** What the server serves: the process-wide instance when the admin root is the working directory (always on a server). */
    static LauncherRelease release(AdminContext ctx) {
        Path root = ctx.env().root().toAbsolutePath().normalize();
        if (root.equals(Path.of("").toAbsolutePath().normalize())) {
            return LauncherRelease.standard();
        }
        return LauncherRelease.under(root, ctx.env().clock());
    }

    /** Test-key entries are accepted only by a test server (-Dtms.launcher.testDb=true). */
    static boolean allowTestKey() {
        return Boolean.getBoolean(LocalDatabaseGuard.TEST_DB_PROPERTY);
    }

    static Map<String, Object> info(ReleaseInfo i) {
        if (i == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("latest", i.latest());
        m.put("minSupported", i.minSupported());
        m.put("notes", i.notes());
        m.put("publishedAt", i.publishedAt());
        m.put("download", download(i.download()));
        return m;
    }

    static Map<String, Object> download(ReleaseDownload d) {
        return d == null ? null : d.toMap();
    }
}
