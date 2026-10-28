package net.swordie.ms.connection.api.launcher.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * The client files the server publishes (cat-launcher 1.2.3, docs/launcher-api-update.md sections 12 to 17), from the
 * working directory's {@code data/launcher/client-files/}:
 * <pre>
 * active.json                      {"version":"1.0.0"}   (written by the console's import; {"version":null} = not published)
 * &lt;version&gt;/client-files.json     the signed list as release-sign.mjs client-sign wrote it
 * &lt;version&gt;/files/&lt;path&gt;          the files
 * </pre>
 * Hot reloaded like release.json: at most every {@value #CHECK_MS} ms the modification time and size of active.json and
 * the list are compared and the folder is checked again when they changed. The set is offered only when the list
 * passes {@link ClientFileSet#parse}, its version is the folder's, and every file is a regular file (no link, really
 * inside {@code files/}) of the declared size - otherwise nothing is offered (one log line naming the reason, never a
 * path). The SHA-256 of each file was compared when the console imported it (and the launcher checks it again after
 * the download, signed); at load only the size is compared, so a 4 GB set does not stall the first request.
 */
public final class LauncherClientFiles {
    public static final Path DEFAULT_DIR = Path.of("data", "launcher", "client-files");
    static final long CHECK_MS = 5_000;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final LauncherClientFiles STANDARD = new LauncherClientFiles(DEFAULT_DIR, System::currentTimeMillis);
    private static final LauncherClientFiles NONE = new LauncherClientFiles(null, System::currentTimeMillis);

    public static final String NONE_STATE = "none";
    public static final String OK = "ok";
    public static final String INVALID = "invalid";
    public static final String FILE_MISSING = "fileMissing";
    public static final String SIZE_MISMATCH = "sizeMismatch";

    /** What is offered now: {@code set} non-null only when {@code state} is ok. */
    public record Snapshot(String state, String problem, String activeVersion, ClientFileSet set, Path folder) {
    }

    private final Path dir;
    private final LongSupplier clock;
    private long lastCheck = Long.MIN_VALUE;
    private String lastKey;
    private Snapshot current = new Snapshot(NONE_STATE, null, null, null, null);

    public LauncherClientFiles(Path dir, LongSupplier clock) {
        this.dir = dir;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static LauncherClientFiles standard() {
        return STANDARD;
    }

    /** Never offers anything (tests of other parts, a server without the folder). */
    public static LauncherClientFiles none() {
        return NONE;
    }

    public static LauncherClientFiles under(Path root, LongSupplier clock) {
        return new LauncherClientFiles(root.resolve(DEFAULT_DIR), clock);
    }

    public Path dir() {
        return dir;
    }

    public synchronized void invalidate() {
        lastCheck = Long.MIN_VALUE;
        lastKey = null;
    }

    public synchronized Snapshot snapshot() {
        if (dir == null) {
            return current;
        }
        long now = clock.getAsLong();
        if (lastCheck == Long.MIN_VALUE || now - lastCheck >= CHECK_MS || now < lastCheck) {
            lastCheck = now;
            refresh();
        }
        return current;
    }

    /** The file for GET /api/v2/launcher/client/&lt;version&gt;/&lt;path&gt;: only an exact path of the offered version. */
    public Optional<Served> served(String version, String path) {
        if (version == null || !ClientFileRules.isSafePath(path)) {
            return Optional.empty();
        }
        Snapshot s = snapshot();
        if (s.set() == null || !s.set().version().equals(version)) {
            return Optional.empty();
        }
        ClientFileSet.Entry e = s.set().find(path);
        if (e == null) {
            return Optional.empty();
        }
        Path file = ClientFileRules.resolveInside(s.folder().resolve("files"), e.path());
        return file == null ? Optional.empty() : Optional.of(new Served(file, e.path(), e.size(), version));
    }

    public record Served(Path path, String rel, long size, String version) {
    }

    private void refresh() {
        Path active = dir.resolve("active.json");
        String version = readActive(active);
        String key = FileHashCache.key(active) + "|" + version;
        Path folder = version == null ? null : dir.resolve(version);
        Path list = folder == null ? null : folder.resolve("client-files.json");
        if (list != null) {
            key += "|" + FileHashCache.key(list);
        }
        // nothing changed: an offered set is kept while its files are still there; a set refused for a missing or
        // wrong-sized file is checked again every time (a few stats), so putting the file back re-offers it
        boolean settled = current.set() != null ? filesStillThere(current) : NONE_STATE.equals(current.state()) || INVALID.equals(current.state());
        if (key.equals(lastKey) && settled) {
            return;
        }
        lastKey = key;
        Snapshot before = current;
        current = load(version, folder, list);
        if (!Objects.equals(before.state(), current.state()) || !Objects.equals(before.activeVersion(), current.activeVersion())) {
            System.out.println("[LauncherClientFiles] " + (current.set() != null
                    ? "offering client files " + current.set().version() + " (" + current.set().files().size() + " file(s), " + current.set().totalSize() + " bytes)"
                    : current.state().equals(NONE_STATE) ? "no client files published" : "client files " + version + " not offered: " + current.state()
                    + (current.problem() == null ? "" : " (" + current.problem() + ")")));
        }
    }

    /** Cheap re-check of the offered files (sizes only), so a removed file stops being offered within CHECK_MS. */
    private static boolean filesStillThere(Snapshot s) {
        for (ClientFileSet.Entry e : s.set().files()) {
            Path f = ClientFileRules.resolveInside(s.folder().resolve("files"), e.path());
            String k = f == null ? null : FileHashCache.key(f);
            if (k == null || !k.startsWith(e.size() + ":")) {
                return false;
            }
        }
        return true;
    }

    static String readActive(Path active) {
        try {
            if (Files.size(active) > 4096) {
                return null;
            }
            JsonNode j = JSON.readTree(Files.readAllBytes(active));
            JsonNode v = j == null ? null : j.get("version");
            return v != null && v.isTextual() && ReleaseRules.isVersion(v.textValue()) ? v.textValue() : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static Snapshot load(String version, Path folder, Path list) {
        if (version == null) {
            return new Snapshot(NONE_STATE, null, null, null, null);
        }
        ClientFileSet set;
        try {
            if (!Files.isRegularFile(list, LinkOption.NOFOLLOW_LINKS)) {
                return new Snapshot(INVALID, "list.missing", version, null, null);
            }
            set = ClientFileSet.parse(Files.readAllBytes(list));
        } catch (ClientFileSet.Invalid e) {
            return new Snapshot(INVALID, e.getMessage(), version, null, null);
        } catch (NoSuchFileException e) {
            return new Snapshot(INVALID, "list.missing", version, null, null);
        } catch (IOException e) {
            return new Snapshot(INVALID, "list.read", version, null, null);
        }
        if (!set.version().equals(version)) {
            return new Snapshot(INVALID, "version.folder", version, null, null);
        }
        Path files = folder.resolve("files");
        Path realFiles;
        try {
            realFiles = files.toRealPath();
        } catch (IOException e) {
            return new Snapshot(FILE_MISSING, "files", version, null, null);
        }
        for (ClientFileSet.Entry e : set.files()) {
            Path f = ClientFileRules.resolveInside(files, e.path());
            try {
                BasicFileAttributes a = f == null ? null : Files.readAttributes(f, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (a == null || !a.isRegularFile() || a.isSymbolicLink() || !f.toRealPath(LinkOption.NOFOLLOW_LINKS).startsWith(realFiles)) {
                    return new Snapshot(FILE_MISSING, e.path(), version, null, null);
                }
                if (a.size() != e.size()) {
                    return new Snapshot(SIZE_MISMATCH, e.path(), version, null, null);
                }
            } catch (IOException ex) {
                return new Snapshot(FILE_MISSING, e.path(), version, null, null);
            }
        }
        return new Snapshot(OK, null, version, set, folder.toAbsolutePath().normalize());
    }
}
