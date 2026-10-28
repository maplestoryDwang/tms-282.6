package net.swordie.ms.connection.api.launcher.release;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * The admin console's editor of {@code data/launcher/release.json} and {@code data/launcher/releases\} (「發佈」 tab).
 * <ul>
 *     <li>{@link #save}: latest / minSupported / notes / publishedAt, validated with exactly the server's rules
 *     ({@link ReleaseInfo#parse}) plus the editor's stricter ones (at most 10 notes of at most 100 characters, no empty
 *     line - what is saved is what the launchers show); the {@code download} entry and other keys stay as they are.
 *     Raising minSupported needs {@code confirmRaise} (the friends' launchers below it stop logging in).</li>
 *     <li>{@link #importRelease}: {@code <dist-release>\<version>\release-entry.json} + its exe (release-sign.mjs's
 *     output): the entry checked with the section 7 rules, the exe's size and SHA-256 compared, the exe copied into
 *     the releases folder (temporary file + SHA-256 check + atomic move), then {@code download} = the entry's five
 *     values, {@code latest} raised to its version, {@code publishedAt} = now. Entries signed with a test key
 *     ({@code "test": true}) are refused unless {@code allowTest} (test servers only).</li>
 * </ul>
 * Every write: only when the file is still the one the console read ({@code baseSha256}), the previous file kept as
 * {@code release.json.bak}, written through a unique temporary file and an atomic move, under one lock per file shared
 * by every editor of this process. The server sees the change within {@value ReleaseFileSource#CHECK_MS} ms. The editor
 * never creates, reads or stores a signing key: the signature comes from release-entry.json as it is.
 */
public final class ReleaseFileEditor {
    public static final String DIST_KEY = "launcher.distReleaseDir";
    public static final String DEFAULT_DIST = "D:\\TMS\\launcher\\cat-launcher\\dist-release";
    /** Before any release.json existed the launchers 1.1.0+ saw no minimum: raising above 1.0.0 needs a confirmation. */
    static final String BASELINE_MIN = "1.0.0";
    static final int ENTRY_MAX_BYTES = 4096;
    static final int LIST_MAX = 100;
    private static final ObjectMapper READ = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private static final ObjectMapper WRITE = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();

    private final Path file;
    private final Path releases;
    private final Path dist;
    private final ZoneId zone;
    private final LongSupplier clock;

    public ReleaseFileEditor(Path file, Path releases, Path dist, ZoneId zone, LongSupplier clock) {
        this.file = Objects.requireNonNull(file, "file");
        this.releases = Objects.requireNonNull(releases, "releases");
        this.dist = Objects.requireNonNull(dist, "dist");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** {@code root}/data/launcher/release.json and releases, the configured dist-release folder. */
    public static ReleaseFileEditor under(Path root, Path dist) {
        return new ReleaseFileEditor(root.resolve(ReleaseFileSource.DEFAULT_FILE), root.resolve(ReleaseFileSource.DEFAULT_DIR),
                dist, ZoneId.systemDefault(), System::currentTimeMillis);
    }

    /** {@value #DIST_KEY} (-D first, then the given server.properties value), default {@value #DEFAULT_DIST}. */
    public static Path configuredDist(String fromConfig) {
        String raw = System.getProperty(DIST_KEY);
        if (raw == null || raw.isBlank()) {
            raw = fromConfig;
        }
        return Path.of(raw == null || raw.isBlank() ? DEFAULT_DIST : raw.trim()).toAbsolutePath().normalize();
    }

    public Path file() {
        return file;
    }

    public Path releases() {
        return releases;
    }

    public Path dist() {
        return dist;
    }

    // ------------------------------------------------------------------ read

    /**
     * The file as it is: {@code root} null when it is not a JSON object (then {@code problem} says why); {@code info}
     * null when it does not pass the server's rules ({@code problem} = the first offending field).
     */
    public record Current(boolean exists, String sha256, String text, ObjectNode root, ReleaseInfo info, String problem) {
    }

    public Current read() throws IOException {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (NoSuchFileException e) {
            return new Current(false, "", "", null, null, null);
        }
        String sha = sha256(bytes);
        String text = bytes.length > ReleaseRules.FILE_MAX_BYTES ? "" : new String(bytes, StandardCharsets.UTF_8);
        if (bytes.length > ReleaseRules.FILE_MAX_BYTES) {
            return new Current(true, sha, text, null, null, "size");
        }
        JsonNode root;
        try {
            root = READ.readTree(bytes);
        } catch (IOException e) {
            return new Current(true, sha, text, null, null, "json");
        }
        if (!(root instanceof ObjectNode o)) {
            return new Current(true, sha, text, null, null, "root");
        }
        try {
            return new Current(true, sha, text, o, ReleaseInfo.parse(o), null);
        } catch (ReleaseInfo.InvalidRelease e) {
            return new Current(true, sha, text, o, null, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ edit

    /**
     * A checked edit: {@code problem} null = valid. {@code raise}: minSupported goes up from {@code fromMin} (the
     * current valid file's, or {@value #BASELINE_MIN}).
     */
    public record Checked(String problem, byte[] document, ReleaseInfo info, boolean raise, String fromMin) {
        public boolean valid() {
            return problem == null;
        }
    }

    /** Outcome of a write: {@code code} ok / conflict / rejected / confirmRequired. */
    public record Saved(String code, String problem, String sha256, Path backup, ReleaseInfo info, String fromMin, Map<String, Object> extra) {
    }

    /** Builds and validates the file an edit would write; nothing is written. */
    public Checked check(JsonNode edit) throws IOException {
        return check(edit, read());
    }

    private Checked check(JsonNode edit, Current current) throws IOException {
        if (edit == null || !edit.isObject()) {
            return new Checked("edit", null, null, false, null);
        }
        if (current.exists() && current.root() == null) {
            return new Checked("current." + current.problem(), null, null, false, null);
        }
        ObjectNode doc = current.root() == null ? WRITE.createObjectNode() : current.root().deepCopy();
        JsonNode latest = edit.get("latest");
        JsonNode min = edit.get("minSupported");
        if (latest == null || !latest.isTextual() || !ReleaseRules.isVersion(latest.textValue().strip())) {
            return new Checked("latest", null, null, false, null);
        }
        if (min == null || !min.isTextual() || !ReleaseRules.isVersion(min.textValue().strip())) {
            return new Checked("minSupported", null, null, false, null);
        }
        doc.put("latest", latest.textValue().strip());
        doc.put("minSupported", min.textValue().strip());
        JsonNode notes = edit.get("notes");
        ArrayNode out = WRITE.createArrayNode();
        if (notes != null && !notes.isNull()) {
            if (!notes.isArray()) {
                return new Checked("notes", null, null, false, null);
            }
            for (int i = 0; i < notes.size(); i++) {
                JsonNode n = notes.get(i);
                if (!n.isTextual()) {
                    return new Checked("notes[" + i + "]", null, null, false, null);
                }
                String c = ReleaseRules.cleanNote(n.textValue());
                if (c == null) {
                    return new Checked("notes[" + i + "].empty", null, null, false, null);
                }
                String strip = n.textValue().strip();
                if (strip.codePointCount(0, strip.length()) > ReleaseRules.NOTE_MAX) {
                    return new Checked("notes[" + i + "].length", null, null, false, null);
                }
                out.add(c);
            }
            if (out.size() > ReleaseRules.NOTES_MAX) {
                return new Checked("notes.size", null, null, false, null);
            }
        }
        doc.set("notes", out);
        JsonNode published = edit.get("publishedAt");
        if (published == null || published.isNull() || (published.isTextual() && published.textValue().isBlank())) {
            doc.remove("publishedAt");
        } else if (!published.isTextual() || ReleaseInfo.publishedAt(published) == null) {
            return new Checked("publishedAt", null, null, false, null);
        } else {
            doc.put("publishedAt", published.textValue());
        }
        return finish(doc, current);
    }

    private Checked finish(ObjectNode doc, Current current) throws IOException {
        byte[] bytes = (WRITE.writeValueAsString(order(doc)) + "\n").getBytes(StandardCharsets.UTF_8);
        if (bytes.length > ReleaseRules.FILE_MAX_BYTES) {
            return new Checked("size", null, null, false, null);
        }
        ReleaseInfo info;
        try {
            info = ReleaseInfo.parse(bytes);
        } catch (ReleaseInfo.InvalidRelease e) {
            return new Checked(e.getMessage(), null, null, false, null);
        }
        String from = current.info() != null ? current.info().minSupported() : BASELINE_MIN;
        boolean raise = ReleaseRules.compare(info.minSupported(), from) > 0;
        return new Checked(null, bytes, info, raise, from);
    }

    /** Known keys first (latest, minSupported, notes, publishedAt, download), then the rest as they were. */
    private static ObjectNode order(ObjectNode doc) {
        ObjectNode o = WRITE.createObjectNode();
        for (String k : List.of("latest", "minSupported", "notes", "publishedAt", "download")) {
            if (doc.has(k)) {
                o.set(k, doc.get(k));
            }
        }
        doc.fields().forEachRemaining(e -> {
            if (!o.has(e.getKey())) {
                o.set(e.getKey(), e.getValue());
            }
        });
        return o;
    }

    public Saved save(JsonNode edit, String baseSha256, boolean confirmRaise) throws IOException {
        synchronized (lock(file)) {
            Current current = read();
            if (!current.sha256().equalsIgnoreCase(baseSha256 == null ? "" : baseSha256)) {
                return new Saved("conflict", null, current.sha256(), null, null, null, Map.of());
            }
            Checked c = check(edit, current);
            if (!c.valid()) {
                return new Saved("rejected", c.problem(), null, null, null, null, Map.of());
            }
            if (c.raise() && !confirmRaise) {
                return new Saved("confirmRequired", "minSupported.raise", current.sha256(), null, c.info(), c.fromMin(), Map.of());
            }
            Path backup = write(c.document(), current.exists());
            return new Saved("ok", null, sha256(c.document()), backup, c.info(), c.fromMin(), Map.of("raised", c.raise()));
        }
    }

    // ------------------------------------------------------------------ import

    /** A release-sign.mjs output folder found under the dist-release folder. */
    public record Candidate(String version, String file, long size, boolean exePresent, boolean test, String keyId, String problem) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("version", version);
            m.put("file", file);
            m.put("size", size);
            m.put("exePresent", exePresent);
            m.put("test", test);
            m.put("keyId", keyId);
            m.put("problem", problem);
            return m;
        }
    }

    /** {@code <dist>\<x.y.z>\} folders, newest version first (no hashing: only the entry and the exe's size). */
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

    private Candidate candidate(String v, Path folder) {
        Entry e = entry(folder, v);
        if (e.problem() != null) {
            return new Candidate(v, e.download() == null ? "" : e.download().file(), 0, false, e.test(), e.keyId(), e.problem());
        }
        Path exe = folder.resolve(e.download().file());
        long size = -1;
        try {
            BasicFileAttributes a = Files.readAttributes(exe, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            size = a.isRegularFile() ? a.size() : -1;
        } catch (IOException ignored) {
            // missing
        }
        String problem = size < 0 ? "exe.missing" : size != e.download().size() ? "exe.size" : null;
        return new Candidate(v, e.download().file(), e.download().size(), size >= 0, e.test(), e.keyId(), problem);
    }

    private record Entry(ReleaseDownload download, boolean test, String keyId, String problem) {
    }

    private static Entry entry(Path folder, String version) {
        Path f = folder.resolve("release-entry.json");
        byte[] bytes;
        try {
            if (!Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS) || Files.size(f) > ENTRY_MAX_BYTES) {
                return new Entry(null, false, null, Files.exists(f, LinkOption.NOFOLLOW_LINKS) ? "entry.size" : "entry.missing");
            }
            bytes = Files.readAllBytes(f);
        } catch (IOException e) {
            return new Entry(null, false, null, "entry.read");
        }
        JsonNode root;
        try {
            root = READ.readTree(bytes);
        } catch (IOException e) {
            return new Entry(null, false, null, "entry.json");
        }
        if (root == null || !root.isObject()) {
            return new Entry(null, false, null, "entry.json");
        }
        JsonNode t = root.get("test");
        boolean test = t != null && !(t.isBoolean() && !t.booleanValue());
        JsonNode k = root.get("keyId");
        String keyId = k != null && k.isTextual() && k.textValue().matches("[0-9a-f]{16}") ? k.textValue() : null;
        ReleaseDownload d;
        try {
            d = ReleaseInfo.download(root);
        } catch (ReleaseInfo.InvalidRelease e) {
            return new Entry(null, test, keyId, "entry." + e.getMessage().replaceFirst("^download\\.", ""));
        }
        if (!d.version().equals(version)) {
            return new Entry(d, test, keyId, "entry.version.folder");
        }
        return new Entry(d, test, keyId, null);
    }

    /**
     * Imports {@code <dist>\<version>\}: see the class comment. {@code code} ok / conflict / rejected; the extra map
     * has {@code file}, {@code copied} (false when the same exe was already in the releases folder), {@code keyId},
     * {@code test}.
     */
    public Saved importRelease(String version, String baseSha256, boolean allowTest) throws IOException {
        if (!ReleaseRules.isVersion(version)) {
            return new Saved("rejected", "version", null, null, null, null, Map.of());
        }
        Path folder = dist.resolve(version);
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) {
            return new Saved("rejected", "dist.missing", null, null, null, null, Map.of());
        }
        synchronized (lock(file)) {
            Current current = read();
            if (!current.sha256().equalsIgnoreCase(baseSha256 == null ? "" : baseSha256)) {
                return new Saved("conflict", null, current.sha256(), null, null, null, Map.of());
            }
            if (current.exists() && current.root() == null) {
                return new Saved("rejected", "current." + current.problem(), null, null, null, null, Map.of());
            }
            Entry e = entry(folder, version);
            if (e.problem() != null) {
                return new Saved("rejected", e.problem(), null, null, null, null, Map.of());
            }
            if (e.test() && !allowTest) {
                return new Saved("rejected", "entry.testKey", null, null, null, null, Map.of());
            }
            ReleaseDownload d = e.download();
            String latest = current.root() != null && current.root().path("latest").isTextual()
                    && ReleaseRules.isVersion(current.root().path("latest").textValue()) ? current.root().path("latest").textValue() : null;
            if (latest != null && ReleaseRules.compare(d.version(), latest) < 0) {
                return new Saved("rejected", "version.older", null, null, null, null, Map.of("latest", latest));
            }
            Path exe = folder.resolve(d.file());
            String problem = checkExe(exe, d);
            if (problem != null) {
                return new Saved("rejected", problem, null, null, null, null, Map.of());
            }
            boolean copied;
            Path target = releases.resolve(d.file());
            synchronized (lock(target)) {
                Files.createDirectories(releases.toAbsolutePath());
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    if (checkExe(target, d) != null) {
                        return new Saved("rejected", "releases.exists", null, null, null, null, Map.of("file", d.file()));
                    }
                    copied = false;
                } else {
                    copy(exe, target, d);
                    copied = true;
                }
            }
            ObjectNode doc = current.root() == null ? WRITE.createObjectNode() : current.root().deepCopy();
            if (!doc.has("minSupported")) {
                doc.put("minSupported", BASELINE_MIN);
            }
            if (!doc.has("notes")) {
                doc.putArray("notes");
            }
            doc.put("latest", latest == null || ReleaseRules.compare(d.version(), latest) > 0 ? d.version() : latest);
            doc.put("publishedAt", OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(clock.getAsLong()), zone)
                    .truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            ObjectNode dn = doc.putObject("download");
            d.toMap().forEach((k, v) -> {
                if (v instanceof Long l) {
                    dn.put(k, l);
                } else {
                    dn.put(k, (String) v);
                }
            });
            Checked c = finish(doc, current);
            if (!c.valid()) {
                return new Saved("rejected", c.problem(), null, null, null, null, Map.of());
            }
            Path backup = write(c.document(), current.exists());
            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("file", d.file());
            extra.put("copied", copied);
            extra.put("keyId", e.keyId());
            extra.put("test", e.test());
            return new Saved("ok", null, sha256(c.document()), backup, c.info(), c.fromMin(), extra);
        }
    }

    /** null when {@code exe} is a regular file (no link) with the entry's size and SHA-256, else the problem. */
    static String checkExe(Path exe, ReleaseDownload d) {
        try {
            BasicFileAttributes a = Files.readAttributes(exe, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!a.isRegularFile()) {
                return "exe.missing";
            }
            if (a.size() != d.size()) {
                return "exe.size";
            }
            return FileHashCache.hash(exe).toLowerCase(Locale.ROOT).equals(d.sha256()) ? null : "exe.sha256";
        } catch (NoSuchFileException e) {
            return "exe.missing";
        } catch (IOException e) {
            return "exe.read";
        }
    }

    /** Copies through a temporary file in the releases folder, checks its SHA-256, then moves it in place. */
    private static void copy(Path exe, Path target, ReleaseDownload d) throws IOException {
        Path abs = target.toAbsolutePath();
        Path tmp = Files.createTempFile(abs.getParent(), d.file() + ".", ".tmp");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(exe, LinkOption.NOFOLLOW_LINKS);
                 var out = Files.newOutputStream(tmp)) {
                byte[] buf = new byte[1 << 20];
                int n;
                while ((n = in.read(buf)) > 0) {
                    md.update(buf, 0, n);
                    out.write(buf, 0, n);
                }
            }
            if (!HexFormat.of().formatHex(md.digest()).equals(d.sha256()) || Files.size(tmp) != d.size()) {
                throw new IOException("copy does not match the entry");
            }
            Files.move(tmp, abs, StandardCopyOption.ATOMIC_MOVE);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ------------------------------------------------------------------ listing

    /** The releases folder: name, size, modified (epoch ms), registered (= release.json's download.file). */
    public List<Map<String, Object>> releasesFolder(String registered) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!Files.isDirectory(releases, LinkOption.NOFOLLOW_LINKS)) {
            return out;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(releases)) {
            for (Path p : ds) {
                if (out.size() >= LIST_MAX) {
                    break;
                }
                BasicFileAttributes a = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                if (!a.isRegularFile()) {
                    continue;
                }
                String name = p.getFileName().toString();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", name);
                m.put("size", a.size());
                m.put("modified", a.lastModifiedTime().toMillis());
                m.put("registered", name.equals(registered));
                m.put("validName", ReleaseRules.isSafeFileName(name));
                out.add(m);
            }
        } catch (IOException e) {
            return out;
        }
        out.sort((x, y) -> ((String) x.get("name")).compareTo((String) y.get("name")));
        return out;
    }

    // ------------------------------------------------------------------ writing

    private static Object lock(Path p) {
        return LOCKS.computeIfAbsent(p.toAbsolutePath().normalize(), k -> new Object());
    }

    /** Keeps release.json.bak (when the file existed), writes a unique temporary file, moves it over the file. */
    private Path write(byte[] document, boolean existed) throws IOException {
        Path abs = file.toAbsolutePath();
        Files.createDirectories(abs.getParent());
        Path backup = null;
        if (existed) {
            byte[] before = Files.readAllBytes(abs);
            backup = abs.resolveSibling(abs.getFileName() + ".bak");
            Files.write(backup, before);
        }
        Path tmp = Files.createTempFile(abs.getParent(), abs.getFileName() + ".", ".tmp");
        try {
            Files.write(tmp, document);
            replace(tmp, abs);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return backup;
    }

    /** Atomic replace, retried for about a second while Windows refuses it (the server's reader, a scanner). */
    static void replace(Path tmp, Path target) throws IOException {
        FileSystemException last = null;
        for (int i = 0; i < 25; i++) {
            try {
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (AtomicMoveNotSupportedException e) {
                throw e;
            } catch (FileSystemException e) {
                last = e;
                try {
                    Thread.sleep(40);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last != null ? last : new IOException("cannot replace " + target);
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
