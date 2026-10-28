package net.swordie.ms.connection.api.launcher.release;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * The host's {@code data/launcher/release.json} (relative to the working directory), hot reloaded like news.json: each
 * request checks the file's modification time and size at most every {@value #CHECK_MS} ms and re-reads it when they
 * changed. A wrong file keeps the previous valid version (one log line naming the first offending field, never the
 * content); no file = nothing to serve ({@code missing}; launcherVersion answers unavailable).
 * <p>
 * The {@code download} of the served version is offered only when {@code data/launcher/releases/<file>} is a regular
 * file (no link, really inside the releases folder) with the declared size and SHA-256. The SHA-256 is computed when
 * the file (or the entry) changes and cached by size + modification time ({@link FileHashCache}); a mismatch leaves
 * {@code download} out (one log line) while latest / minSupported / notes are still served.
 */
public final class ReleaseFileSource {
    public static final Path DEFAULT_FILE = Path.of("data", "launcher", "release.json");
    public static final Path DEFAULT_DIR = Path.of("data", "launcher", "releases");
    static final long CHECK_MS = 5_000;

    private final Path file;
    private final Path dir;
    private final FileHashCache hashes;
    private long lastCheck = Long.MIN_VALUE;
    private String lastKey;
    private ReleaseInfo valid;
    private String status = ReleaseSnapshot.MISSING;
    private String problem;
    private String exeKey;
    private ReleaseDownload checkedEntry;
    private String downloadState = ReleaseSnapshot.DOWNLOAD_NONE;
    private Path exe;

    public ReleaseFileSource(Path file, Path dir, FileHashCache hashes) {
        this.file = Objects.requireNonNull(file, "file");
        this.dir = Objects.requireNonNull(dir, "dir");
        this.hashes = Objects.requireNonNull(hashes, "hashes");
    }

    public Path file() {
        return file;
    }

    public Path dir() {
        return dir;
    }

    /** The state at {@code now} (re-checked at most every {@value #CHECK_MS} ms). */
    public synchronized ReleaseSnapshot snapshot(long now) {
        if (lastCheck == Long.MIN_VALUE || now - lastCheck >= CHECK_MS || now < lastCheck) {
            lastCheck = now;
            refreshFile();
            refreshDownload();
        }
        boolean ok = ReleaseSnapshot.DOWNLOAD_OK.equals(downloadState);
        return new ReleaseSnapshot(status, problem, valid, ok && valid != null ? valid.download() : null, downloadState,
                ok ? exe : null);
    }

    /** Forces the next {@link #snapshot} to look at the files again (the admin console after a save). */
    public synchronized void invalidate() {
        lastCheck = Long.MIN_VALUE;
    }

    private void refreshFile() {
        String key = FileHashCache.key(file);
        if (key == null) {
            key = "missing";
        }
        if (key.equals(lastKey)) {
            return;
        }
        lastKey = key;
        if (key.equals("missing")) {
            if (!ReleaseSnapshot.MISSING.equals(status)) {
                System.out.println("[LauncherRelease] " + file + " is gone: launcherVersion answers unavailable");
            }
            valid = null;
            status = ReleaseSnapshot.MISSING;
            problem = null;
            return;
        }
        try {
            byte[] bytes = Files.size(file) > ReleaseRules.FILE_MAX_BYTES ? null : Files.readAllBytes(file);
            valid = ReleaseInfo.parse(bytes);
            status = ReleaseSnapshot.OK;
            problem = null;
            System.out.println("[LauncherRelease] " + file + " loaded: latest " + valid.latest() + ", minSupported "
                    + valid.minSupported() + ", " + valid.notes().size() + " note(s), download "
                    + (valid.download() == null ? "none" : valid.download().file()));
        } catch (NoSuchFileException e) {
            valid = null;
            status = ReleaseSnapshot.MISSING;
            problem = null;
        } catch (ReleaseInfo.InvalidRelease e) {
            invalid(e.getMessage());
        } catch (IOException e) {
            invalid("read");
        }
    }

    private void invalid(String where) {
        status = ReleaseSnapshot.INVALID;
        problem = where;
        System.out.println("[LauncherRelease] " + file + " is invalid (" + where + "): "
                + (valid == null ? "no valid version yet, launcherVersion answers unavailable" : "keeping the previous valid version (latest " + valid.latest() + ")"));
    }

    private void refreshDownload() {
        ReleaseDownload d = valid == null ? null : valid.download();
        if (d == null) {
            checkedEntry = null;
            exeKey = null;
            exe = null;
            downloadState = ReleaseSnapshot.DOWNLOAD_NONE;
            return;
        }
        Path candidate = dir.resolve(d.file());
        String key = FileHashCache.key(candidate);
        if (d.equals(checkedEntry) && Objects.equals(key, exeKey)) {
            return;
        }
        checkedEntry = d;
        exeKey = key;
        String before = downloadState;
        downloadState = verify(candidate, d, key);
        exe = ReleaseSnapshot.DOWNLOAD_OK.equals(downloadState) ? candidate.toAbsolutePath().normalize() : null;
        if (!downloadState.equals(before) || !ReleaseSnapshot.DOWNLOAD_OK.equals(downloadState)) {
            System.out.println("[LauncherRelease] download " + d.file() + " " + d.version() + ": "
                    + (ReleaseSnapshot.DOWNLOAD_OK.equals(downloadState) ? "offered (" + d.size() + " bytes, SHA-256 checked)"
                    : downloadState + " - launcherVersion leaves download out"));
        }
    }

    private String verify(Path candidate, ReleaseDownload d, String key) {
        if (key == null) {
            return ReleaseSnapshot.DOWNLOAD_FILE_MISSING;
        }
        try {
            Path realDir = dir.toRealPath();
            Path real = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realDir.equals(real.getParent()) || !Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
                return ReleaseSnapshot.DOWNLOAD_FILE_MISSING;
            }
            if (Files.size(real) != d.size()) {
                return ReleaseSnapshot.DOWNLOAD_SIZE_MISMATCH;
            }
            String sha = hashes.sha256(real).toLowerCase(Locale.ROOT);
            return sha.equals(d.sha256()) ? ReleaseSnapshot.DOWNLOAD_OK : ReleaseSnapshot.DOWNLOAD_HASH_MISMATCH;
        } catch (NoSuchFileException e) {
            return ReleaseSnapshot.DOWNLOAD_FILE_MISSING;
        } catch (IOException | RuntimeException e) {
            return ReleaseSnapshot.DOWNLOAD_UNREADABLE;
        }
    }
}
