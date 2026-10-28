package net.swordie.ms.connection.api.launcher.release;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * The launcher release the server publishes (P8 / P8b): release.json ({@link ReleaseFileSource}) and the exe the
 * download endpoint may serve. {@link #standard()} is the working directory's {@code data/launcher}, shared by the
 * process (launcherVersion, the download endpoint and the admin console's 「發佈」 tab see the same state).
 */
public final class LauncherRelease {
    private static final LauncherRelease STANDARD = new LauncherRelease(
            new ReleaseFileSource(ReleaseFileSource.DEFAULT_FILE, ReleaseFileSource.DEFAULT_DIR, FileHashCache.standard()),
            System::currentTimeMillis);

    /** An exe the download endpoint may send: the registered file, verified (size and SHA-256). */
    public record Served(Path path, String file, long size, String version) {
    }

    private final ReleaseFileSource source;
    private final LongSupplier clock;

    public LauncherRelease(ReleaseFileSource source, LongSupplier clock) {
        this.source = Objects.requireNonNull(source, "source");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static LauncherRelease standard() {
        return STANDARD;
    }

    /** {@code root}/data/launcher (tests, a second server root). */
    public static LauncherRelease under(Path root, LongSupplier clock) {
        return new LauncherRelease(new ReleaseFileSource(root.resolve(ReleaseFileSource.DEFAULT_FILE),
                root.resolve(ReleaseFileSource.DEFAULT_DIR), FileHashCache.standard()), clock);
    }

    public ReleaseSnapshot snapshot() {
        return source.snapshot(clock.getAsLong());
    }

    public ReleaseFileSource source() {
        return source;
    }

    /**
     * The file for {@code GET /api/v2/launcher/download/<name>}: only the exact registered name (case-sensitive) of a
     * verified download; anything else is empty (the endpoint answers 404 without saying why).
     */
    public Optional<Served> served(String name) {
        if (!ReleaseRules.isSafeFileName(name)) {
            return Optional.empty();
        }
        ReleaseSnapshot s = snapshot();
        ReleaseDownload d = s.download();
        if (d == null || s.file() == null || !d.file().equals(name)) {
            return Optional.empty();
        }
        return Optional.of(new Served(s.file(), d.file(), d.size(), d.version()));
    }
}
