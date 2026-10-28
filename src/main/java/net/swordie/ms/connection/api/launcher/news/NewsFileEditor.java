package net.swordie.ms.connection.api.launcher.news;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * The admin console's editor of {@code data/launcher/news.json}: reads the file as written (items in file order, the
 * {@code _...} comment keys), validates a new item list with exactly the rules of {@link NewsFileSource#parse}, and
 * writes it - only when it validates, only when the file is still the one the console read ({@code baseSha256}),
 * after copying the current file to {@code news.json.bak}, through a temporary file (a unique name in the same folder)
 * and an atomic move. Reading, the hash check and the write run under one lock per file shared by every editor of
 * that path, so two saves with the same {@code baseSha256} cannot both succeed (the second answers conflict). The running
 * server then reloads it by itself within {@value NewsFileSource#CHECK_MS} ms (modification time / size).
 */
public final class NewsFileEditor {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    /** Written order of an item's keys; other keys are kept after them (and then rejected by the validation). */
    private static final List<String> KEY_ORDER = List.of("id", "kind", "title", "body", "startAt", "endAt", "pinned");

    /** One lock per file (absolute, normalized path), shared by every editor instance of this process. */
    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();

    private final Path file;
    private final ZoneId zone;
    private final LongSupplier clock;

    public NewsFileEditor(Path file, ZoneId zone, LongSupplier clock) {
        this.file = Objects.requireNonNull(file, "file");
        this.zone = Objects.requireNonNull(zone, "zone");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** {@code root}/data/launcher/news.json in the server's time zone. */
    public static NewsFileEditor under(Path root) {
        return new NewsFileEditor(root.resolve(NewsFileSource.DEFAULT_FILE), ZoneId.systemDefault(), System::currentTimeMillis);
    }

    public Path file() {
        return file;
    }

    /** The file as it is now. {@code items} null when it is not a readable news.json (then {@code problem} says why). */
    public record Current(boolean exists, String sha256, ArrayNode items, String problem, List<Map<String, Object>> preview) {
    }

    /** A checked item list: {@code problem} null = valid; {@code preview} in launcher order with visibility. */
    public record Checked(String problem, byte[] document, List<Map<String, Object>> preview) {
        public boolean valid() {
            return problem == null;
        }
    }

    /** Outcome of {@link #save}: {@code code} ok / conflict / rejected. */
    public record Saved(String code, String problem, String sha256, Path backup, List<Map<String, Object>> preview) {
    }

    public synchronized Current read() throws IOException {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file);
        } catch (NoSuchFileException e) {
            return new Current(false, "", JSON.createArrayNode(), null, List.of());
        }
        String sha = sha256(bytes);
        JsonNode root;
        try {
            root = JSON.readTree(bytes);
        } catch (IOException e) {
            return new Current(true, sha, null, "json", List.of());
        }
        if (root == null || !root.isObject() || !(root.get("items") instanceof ArrayNode items)) {
            return new Current(true, sha, null, "items", List.of());
        }
        String problem = null;
        List<Map<String, Object>> preview = List.of();
        try {
            preview = preview(NewsFileSource.parse(bytes, zone));
        } catch (NewsFileSource.InvalidNews e) {
            problem = e.getMessage();
        }
        return new Current(true, sha, items, problem, preview);
    }

    /** Builds the file a save would write (keeping the current file's {@code _} keys) and validates it. */
    public synchronized Checked check(JsonNode items) throws IOException {
        if (items == null || !items.isArray()) {
            return new Checked("items", null, List.of());
        }
        byte[] doc = document(items);
        if (doc.length > NewsFileSource.MAX_BYTES) {
            return new Checked("size", null, List.of());
        }
        try {
            return new Checked(null, doc, preview(NewsFileSource.parse(doc, zone)));
        } catch (NewsFileSource.InvalidNews e) {
            return new Checked(e.getMessage(), null, List.of());
        }
    }

    /** Validates and writes {@code items} if the file still has {@code baseSha256} ("" = the file did not exist). */
    private Object lock() {
        return LOCKS.computeIfAbsent(file.toAbsolutePath().normalize(), k -> new Object());
    }

    public Saved save(JsonNode items, String baseSha256) throws IOException {
        synchronized (lock()) {
            return saveLocked(items, baseSha256);
        }
    }

    private Saved saveLocked(JsonNode items, String baseSha256) throws IOException {
        Checked c = check(items);
        if (!c.valid()) {
            return new Saved("rejected", c.problem(), null, null, List.of());
        }
        byte[] before = null;
        try {
            before = Files.readAllBytes(file);
        } catch (NoSuchFileException e) {
            // a new file
        }
        String now = before == null ? "" : sha256(before);
        if (!now.equalsIgnoreCase(baseSha256 == null ? "" : baseSha256)) {
            return new Saved("conflict", null, now, null, List.of());
        }
        Path abs = file.toAbsolutePath();
        Files.createDirectories(abs.getParent());
        Path backup = null;
        if (before != null) {
            backup = abs.resolveSibling(abs.getFileName() + ".bak");
            Files.write(backup, before);
        }
        Path tmp = Files.createTempFile(abs.getParent(), abs.getFileName() + ".", ".tmp");
        try {
            Files.write(tmp, c.document());
            replace(tmp, abs);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return new Saved("ok", null, sha256(c.document()), backup, c.preview());
    }

    /** The current root's {@code _} keys (if it is a JSON object), then {@code items} with the keys in wire order. */
    byte[] document(JsonNode items) throws IOException {
        ObjectNode root = JSON.createObjectNode();
        try {
            JsonNode old = JSON.readTree(Files.readAllBytes(file));
            if (old != null && old.isObject()) {
                for (Iterator<Map.Entry<String, JsonNode>> it = old.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> e = it.next();
                    if (e.getKey().startsWith("_")) {
                        root.set(e.getKey(), e.getValue());
                    }
                }
            }
        } catch (IOException e) {
            // no file or not JSON: nothing to keep
        }
        ArrayNode out = root.putArray("items");
        for (JsonNode item : items) {
            if (!item.isObject()) {
                out.add(item);
                continue;
            }
            ObjectNode o = JSON.createObjectNode();
            for (String k : KEY_ORDER) {
                if (item.has(k)) {
                    o.set(k, item.get(k));
                }
            }
            for (Iterator<String> it = item.fieldNames(); it.hasNext(); ) {
                String k = it.next();
                if (!KEY_ORDER.contains(k)) {
                    o.set(k, item.get(k));
                }
            }
            out.add(o);
        }
        return (JSON.writeValueAsString(root) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    /** Launcher order (pinned, maintenance, newest start, id) with state shown / scheduled / ended at the clock's now. */
    List<Map<String, Object>> preview(List<NewsItem> items) {
        long now = clock.getAsLong();
        List<NewsItem> sorted = new ArrayList<>(items);
        sorted.sort(LauncherNews.ORDER);
        List<Map<String, Object>> out = new ArrayList<>();
        for (NewsItem i : sorted) {
            Map<String, Object> m = new LinkedHashMap<>(i.toMap());
            m.put("state", i.visibleAt(now) ? "shown" : i.startAt() != null && now < i.startAt() ? "scheduled" : "ended");
            out.add(m);
        }
        return out;
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Moves {@code tmp} over {@code target} (atomically where possible). Windows refuses the replace while another
     * process or thread has {@code target} open without delete sharing (the server's own hot reload, a virus scanner,
     * an editor): retried for about a second before giving up.
     */
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
}
