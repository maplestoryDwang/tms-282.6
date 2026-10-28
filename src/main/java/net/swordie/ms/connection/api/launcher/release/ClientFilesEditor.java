package net.swordie.ms.connection.api.launcher.release;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

/**
 * The admin console's import of client files (「發佈」 → 「客戶端檔案」, cat-launcher 1.2.3): takes
 * {@code <dist>\<version>\} as release-sign.mjs client-sign wrote it (client-files.json + files\) and makes it the
 * published set:
 * <ol>
 *     <li>the list must pass {@link ClientFileSet#parse} and name its folder's version; a test-key list only on a test
 *     server; the version must not be older than the published one (no downgrade);</li>
 *     <li>every file of the list is copied from {@code files\<path>} (regular file, no link, inside the folder) into a
 *     staging folder under {@code data\launcher\client-files\}, hashed while copying, and must have the listed size and
 *     SHA-256;</li>
 *     <li>the list is written next to the files, the staging folder is renamed to {@code <version>} (refused when that
 *     folder exists with a different list), then {@code active.json} is replaced atomically.</li>
 * </ol>
 * The import runs on its own thread (a set can be gigabytes; the console polls {@code clientfiles.get}); one at a time.
 * Nothing is signed or verified with a key here: the server holds none. {@link #disable} stops publishing (the
 * folders stay).
 */
public final class ClientFilesEditor {
    public static final String DIST_KEY = "launcher.distClientDir";
    public static final String DEFAULT_DIST = "D:\\TMS\\launcher\\cat-launcher\\dist-release\\client";
    static final int LIST_MAX = 100;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicReference<Job> JOB = new AtomicReference<>(Job.idle());

    /** The running (or last) import: state idle / running / done / failed. */
    public record Job(String state, String version, int done, int total, long bytes, String problem, long at) {
        static Job idle() {
            return new Job("idle", null, 0, 0, 0, null, 0);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("state", state);
            m.put("version", version);
            m.put("done", done);
            m.put("total", total);
            m.put("bytes", bytes);
            m.put("problem", problem);
            m.put("at", at);
            return m;
        }
    }

    private final Path dir;
    private final Path dist;
    private final LongSupplier clock;

    public ClientFilesEditor(Path dir, Path dist, LongSupplier clock) {
        this.dir = Objects.requireNonNull(dir, "dir");
        this.dist = Objects.requireNonNull(dist, "dist");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static ClientFilesEditor under(Path root, Path dist) {
        return new ClientFilesEditor(root.resolve(LauncherClientFiles.DEFAULT_DIR), dist, System::currentTimeMillis);
    }

    /** {@value #DIST_KEY} (-D first, then the given server.properties value), default {@value #DEFAULT_DIST}. */
    public static Path configuredDist(String fromConfig) {
        String raw = System.getProperty(DIST_KEY);
        if (raw == null || raw.isBlank()) {
            raw = fromConfig;
        }
        return Path.of(raw == null || raw.isBlank() ? DEFAULT_DIST : raw.trim()).toAbsolutePath().normalize();
    }

    public Path dir() {
        return dir;
    }

    public Path dist() {
        return dist;
    }

    public static Job job() {
        return JOB.get();
    }

    /** Tests: forget the last job. */
    public static void resetJob() {
        if (!"running".equals(JOB.get().state())) {
            JOB.set(Job.idle());
        }
    }

    // ------------------------------------------------------------------ candidates and folders

    /** A client-sign output folder under the dist folder. */
    public record Candidate(String version, int count, long totalSize, boolean test, String keyId, String problem) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("version", version);
            m.put("count", count);
            m.put("totalSize", totalSize);
            m.put("test", test);
            m.put("keyId", keyId);
            m.put("problem", problem);
            return m;
        }
    }

    /** {@code <dist>\<x.y.z>\} folders, newest first (the list and the files' sizes only; no hashing). */
    public List<Candidate> candidates() {
        List<Candidate> out = new ArrayList<>();
        if (!Files.isDirectory(dist, LinkOption.NOFOLLOW_LINKS)) {
            return out;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dist)) {
            for (Path p : ds) {
                String v = p.getFileName().toString();
                if (!ReleaseRules.isVersion(v) || !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS) || out.size() >= LIST_MAX) {
                    continue;
                }
                out.add(candidate(v, p));
            }
        } catch (IOException e) {
            return out;
        }
        out.sort((a, b) -> ReleaseRules.compare(b.version(), a.version()));
        return out;
    }

    private static Candidate candidate(String v, Path folder) {
        ClientFileSet set;
        try {
            set = readList(folder.resolve("client-files.json"));
        } catch (ClientFileSet.Invalid e) {
            return new Candidate(v, 0, 0, false, null, "list." + e.getMessage());
        }
        if (!set.version().equals(v)) {
            return new Candidate(v, set.files().size(), set.totalSize(), set.test(), set.keyId(), "list.version.folder");
        }
        for (ClientFileSet.Entry e : set.files()) {
            Path f = ClientFileRules.resolveInside(folder.resolve("files"), e.path());
            String k = f == null ? null : FileHashCache.key(f);
            if (k == null) {
                return new Candidate(v, set.files().size(), set.totalSize(), set.test(), set.keyId(), "file.missing");
            }
            if (!k.startsWith(e.size() + ":")) {
                return new Candidate(v, set.files().size(), set.totalSize(), set.test(), set.keyId(), "file.size");
            }
        }
        return new Candidate(v, set.files().size(), set.totalSize(), set.test(), set.keyId(), null);
    }

    static ClientFileSet readList(Path f) throws ClientFileSet.Invalid {
        try {
            if (!Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS)) {
                throw new ClientFileSet.Invalid("missing");
            }
            if (Files.size(f) > ClientFileRules.LIST_MAX_BYTES) {
                throw new ClientFileSet.Invalid("size");
            }
            return ClientFileSet.parse(Files.readAllBytes(f));
        } catch (IOException e) {
            throw new ClientFileSet.Invalid("read");
        }
    }

    /** The versions imported into data\launcher\client-files\ (name, count, totalSize, problem). */
    public List<Map<String, Object>> imported() {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return out;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                String v = p.getFileName().toString();
                if (!ReleaseRules.isVersion(v) || !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS) || out.size() >= LIST_MAX) {
                    continue;
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("version", v);
                try {
                    ClientFileSet s = readList(p.resolve("client-files.json"));
                    m.put("count", s.files().size());
                    m.put("totalSize", s.totalSize());
                    m.put("test", s.test());
                    m.put("problem", null);
                } catch (ClientFileSet.Invalid e) {
                    m.put("count", 0);
                    m.put("totalSize", 0L);
                    m.put("test", false);
                    m.put("problem", "list." + e.getMessage());
                }
                out.add(m);
            }
        } catch (IOException e) {
            return out;
        }
        out.sort((a, b) -> ReleaseRules.compare((String) b.get("version"), (String) a.get("version")));
        return out;
    }

    public String activeVersion() {
        return LauncherClientFiles.readActive(dir.resolve("active.json"));
    }

    // ------------------------------------------------------------------ import

    /** Outcome of starting an import (or of the synchronous part of it). */
    public record Started(String code, String problem, Map<String, Object> extra) {
    }

    /**
     * Checks {@code <dist>\<version>\} and starts the copy on its own thread: {@code code} started / rejected / busy.
     * {@code onDone} runs on that thread after a successful import (the console's caller invalidates the live state).
     */
    public Started startImport(String version, boolean allowTest, Runnable onDone) {
        Prepared p;
        try {
            p = prepare(version, allowTest);
        } catch (Rejected e) {
            return new Started("rejected", e.getMessage(), e.extra);
        }
        Job running = new Job("running", version, 0, p.set.files().size(), 0, null, clock.getAsLong());
        Job before = JOB.get();
        if ("running".equals(before.state()) || !JOB.compareAndSet(before, running)) {
            return new Started("busy", "import.running", Map.of("version", String.valueOf(JOB.get().version())));
        }
        Thread t = new Thread(() -> {
            try {
                long bytes = copy(p);
                JOB.set(new Job("done", version, p.set.files().size(), p.set.files().size(), bytes, null, clock.getAsLong()));
                System.out.println("[LauncherClientFiles] imported client files " + version + " (" + p.set.files().size() + " file(s), " + bytes + " bytes)");
                if (onDone != null) {
                    onDone.run();
                }
            } catch (Rejected e) {
                JOB.set(new Job("failed", version, JOB.get().done(), p.set.files().size(), 0, e.getMessage(), clock.getAsLong()));
                System.out.println("[LauncherClientFiles] import of " + version + " failed: " + e.getMessage());
            } catch (IOException | RuntimeException e) {
                JOB.set(new Job("failed", version, JOB.get().done(), p.set.files().size(), 0, "io", clock.getAsLong()));
                System.out.println("[LauncherClientFiles] import of " + version + " failed: " + e.getClass().getSimpleName());
            }
        }, "ClientFiles-import");
        t.setDaemon(true);
        t.start();
        return new Started("started", null, Map.of("version", version, "count", p.set.files().size(), "totalSize", p.set.totalSize()));
    }

    /** Runs the whole import on the caller's thread (checks). */
    public Started importNow(String version, boolean allowTest) {
        try {
            Prepared p = prepare(version, allowTest);
            long bytes = copy(p);
            return new Started("ok", null, Map.of("version", version, "count", p.set.files().size(), "bytes", bytes));
        } catch (Rejected e) {
            return new Started("rejected", e.getMessage(), e.extra);
        } catch (IOException e) {
            return new Started("rejected", "io", Map.of());
        }
    }

    private static final class Rejected extends Exception {
        private static final long serialVersionUID = 1L;
        final transient Map<String, Object> extra;

        Rejected(String problem) {
            this(problem, Map.of());
        }

        Rejected(String problem, Map<String, Object> extra) {
            super(problem);
            this.extra = extra;
        }
    }

    private record Prepared(String version, Path source, ClientFileSet set, byte[] listBytes) {
    }

    private Prepared prepare(String version, boolean allowTest) throws Rejected {
        if (!ReleaseRules.isVersion(version)) {
            throw new Rejected("version");
        }
        Path folder = dist.resolve(version);
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
            throw new Rejected("dist.missing");
        }
        Path listFile = folder.resolve("client-files.json");
        ClientFileSet set;
        byte[] bytes;
        try {
            set = readList(listFile);
            bytes = Files.readAllBytes(listFile);
        } catch (ClientFileSet.Invalid e) {
            throw new Rejected("list." + e.getMessage());
        } catch (IOException e) {
            throw new Rejected("list.read");
        }
        if (!set.version().equals(version)) {
            throw new Rejected("list.version.folder");
        }
        if (set.test() && !allowTest) {
            throw new Rejected("list.testKey");
        }
        String active = activeVersion();
        if (active != null && ReleaseRules.compare(version, active) < 0) {
            throw new Rejected("version.older", Map.of("active", active));
        }
        Path target = dir.resolve(version);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            try {
                byte[] existing = Files.readAllBytes(target.resolve("client-files.json"));
                if (!java.util.Arrays.equals(existing, bytes)) {
                    throw new Rejected("target.exists");
                }
            } catch (IOException e) {
                throw new Rejected("target.exists");
            }
        }
        return new Prepared(version, folder, set, bytes);
    }

    private long copy(Prepared p) throws Rejected, IOException {
        Files.createDirectories(dir.toAbsolutePath());
        Path target = dir.resolve(p.version());
        long bytes = 0;
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            Path staging = Files.createTempDirectory(dir.toAbsolutePath(), "." + p.version() + ".staging-");
            try {
                Path files = staging.resolve("files");
                int done = 0;
                for (ClientFileSet.Entry e : p.set().files()) {
                    Path src = ClientFileRules.resolveInside(p.source().resolve("files"), e.path());
                    Path dst = ClientFileRules.resolveInside(files, e.path());
                    if (src == null || dst == null) {
                        throw new Rejected("file.path");
                    }
                    copyChecked(src, dst, e);
                    bytes += e.size();
                    done++;
                    Job j = JOB.get();
                    if ("running".equals(j.state()) && p.version().equals(j.version())) {
                        JOB.set(new Job("running", j.version(), done, j.total(), bytes, null, j.at()));
                    }
                }
                Files.write(staging.resolve("client-files.json"), p.listBytes());
                try {
                    Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(staging, target);
                }
            } finally {
                deleteTree(staging);
            }
        } else {
            // the same list already imported: check that its files are still complete (sizes), then just re-activate
            for (ClientFileSet.Entry e : p.set().files()) {
                Path f = ClientFileRules.resolveInside(target.resolve("files"), e.path());
                String k = f == null ? null : FileHashCache.key(f);
                if (k == null || !k.startsWith(e.size() + ":")) {
                    throw new Rejected("target.incomplete");
                }
                bytes += e.size();
            }
        }
        writeActive(p.version());
        return bytes;
    }

    /** Copies src to dst (new file), hashing while copying; the size and SHA-256 must be the entry's. */
    static void copyChecked(Path src, Path dst, ClientFileSet.Entry e) throws Rejected, IOException {
        BasicFileAttributes a;
        try {
            a = Files.readAttributes(src, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException ex) {
            throw new Rejected("file.missing", Map.of("path", e.path()));
        }
        if (!a.isRegularFile() || a.isSymbolicLink()) {
            throw new Rejected("file.missing", Map.of("path", e.path()));
        }
        if (a.size() != e.size()) {
            throw new Rejected("file.size", Map.of("path", e.path()));
        }
        Files.createDirectories(dst.getParent());
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
        long n = 0;
        try (InputStream in = Files.newInputStream(src, LinkOption.NOFOLLOW_LINKS); OutputStream out = Files.newOutputStream(dst, java.nio.file.StandardOpenOption.CREATE_NEW)) {
            byte[] buf = new byte[1 << 20];
            int r;
            while ((r = in.read(buf)) > 0) {
                md.update(buf, 0, r);
                out.write(buf, 0, r);
                n += r;
            }
        }
        if (n != e.size() || !HexFormat.of().formatHex(md.digest()).equals(e.sha256())) {
            throw new Rejected("file.sha256", Map.of("path", e.path()));
        }
    }

    /** Stops publishing (active.json = {"version":null}); the imported folders stay. */
    public void disable() throws IOException {
        writeActive(null);
    }

    private void writeActive(String version) throws IOException {
        Files.createDirectories(dir.toAbsolutePath());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", version);
        m.put("changedAt", Instant.ofEpochMilli(clock.getAsLong()).toString());
        Path active = dir.resolve("active.json").toAbsolutePath();
        Path tmp = Files.createTempFile(active.getParent(), "active.json.", ".tmp");
        try {
            Files.write(tmp, (JSON.writeValueAsString(m) + "\n").getBytes(StandardCharsets.UTF_8));
            ReleaseFileEditor.replace(tmp, active);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    static void deleteTree(Path p) {
        if (p == null || !Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> s = Files.walk(p)) {
            s.sorted(Comparator.reverseOrder()).forEach(x -> {
                try {
                    Files.deleteIfExists(x);
                } catch (IOException ignored) {
                    // left behind
                }
            });
        } catch (IOException ignored) {
            // left behind
        }
    }
}
