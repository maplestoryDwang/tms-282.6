package net.swordie.ms.connection.api.launcher.release;

import java.nio.file.Path;

/**
 * What the server serves from release.json right now.
 *
 * @param status        {@code ok}, {@code missing} (no release.json: launcherVersion answers unavailable) or
 *                      {@code invalid} (the current file is wrong; {@code info} is the previous valid version, or null)
 * @param problem       the first offending field of an invalid file, else null
 * @param info          the release served to launchers, or null when no valid version was ever read
 * @param download      {@code info.download()} when its file exists in the releases folder with the declared size and
 *                      SHA-256 (served as launcherVersion's {@code download} and by the download endpoint), else null
 * @param downloadState {@code none} (no download in the file), {@code ok}, {@code fileMissing}, {@code sizeMismatch},
 *                      {@code hashMismatch} or {@code unreadable}
 * @param file          the verified exe (only with {@code download})
 */
public record ReleaseSnapshot(String status, String problem, ReleaseInfo info, ReleaseDownload download, String downloadState,
                              Path file) {
    public static final String OK = "ok";
    public static final String MISSING = "missing";
    public static final String INVALID = "invalid";

    public static final String DOWNLOAD_NONE = "none";
    public static final String DOWNLOAD_OK = "ok";
    public static final String DOWNLOAD_FILE_MISSING = "fileMissing";
    public static final String DOWNLOAD_SIZE_MISMATCH = "sizeMismatch";
    public static final String DOWNLOAD_HASH_MISMATCH = "hashMismatch";
    public static final String DOWNLOAD_UNREADABLE = "unreadable";
}
