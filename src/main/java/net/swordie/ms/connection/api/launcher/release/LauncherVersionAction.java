package net.swordie.ms.connection.api.launcher.release;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.ReadAction;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@code launcherVersion} (public, P8 / P8b): the launcher release of data/launcher/release.json -
 * {@code latest}, {@code minSupported}, {@code notes}, {@code publishedAt} (when it parses) and, when the host put the
 * exe in data/launcher/releases (same size and SHA-256), {@code download} with the five values of release-entry.json
 * unchanged; 1.2.3: when client files are published, {@code clientFiles} {version, count, totalSize, listSha256} (fixes10: left out when the list
 * needs path rules 2 and the request has no {@code "pathRules":2}). No valid release.json ever read (or the file is gone) -> {@code unavailable}. No paths, no account data.
 */
public final class LauncherVersionAction extends ReadAction {
    private final Supplier<LauncherRelease> release;
    private final Supplier<LauncherClientFiles> clientFiles;

    public LauncherVersionAction() {
        this(LauncherRelease::standard, LauncherClientFiles::standard);
    }

    /** Tests of the release part: no client files. */
    public LauncherVersionAction(Supplier<LauncherRelease> release) {
        this(release, LauncherClientFiles::none);
    }

    public LauncherVersionAction(Supplier<LauncherRelease> release, Supplier<LauncherClientFiles> clientFiles) {
        this.release = Objects.requireNonNull(release, "release");
        this.clientFiles = Objects.requireNonNull(clientFiles, "clientFiles");
    }

    @Override
    public String name() {
        return "launcherVersion";
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) {
        ReleaseSnapshot s = release.get().snapshot();
        ReleaseInfo i = s.info();
        if (i == null) {
            return LauncherResult.fail(LauncherCodes.UNAVAILABLE);
        }
        LauncherResult r = LauncherResult.ok(LauncherCodes.OK).with("latest", i.latest()).with("minSupported", i.minSupported())
                .with("notes", i.notes());
        if (i.publishedAt() != null) {
            r.with("publishedAt", i.publishedAt());
        }
        if (s.download() != null) {
            r.with("download", s.download().toMap());
        }
        // 1.2.3: the published client files, as a small summary (the list and signature: the clientFiles action)
        // fixes10: a list that needs path rules 2 is summarised only for launchers that send "pathRules":2
        ClientFileSet cf = clientFiles.get().snapshot().set();
        if (ClientFileRules.offerable(cf, ClientFileRules.declaredPathRules(request))) {
            r.with("clientFiles", cf.summary());
        }
        return r;
    }
}
