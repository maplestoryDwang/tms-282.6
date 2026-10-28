package net.swordie.ms.connection.api.launcher.news;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The host's hand-written notices: {@code data/launcher/news.json} (relative to the working directory), hot reloaded -
 * each request checks the file's modification time and size at most every {@value #CHECK_MS} ms and re-reads it when
 * they changed.
 * <pre>
 * {"items":[{"id":"oct-update","kind":"up","title":"10 月更新","body":"…",
 *            "startAt":"2026-10-01T00:00","endAt":"2026-10-31","pinned":false}]}
 * </pre>
 * Rules (any violation makes the whole file invalid: the previous valid version stays, {@link #status()} =
 * {@code invalid}, one log line names the first offending field - never the content):
 * <ul>
 *     <li>a UTF-8 JSON object (BOM allowed) of at most {@value #MAX_BYTES} bytes; top-level keys {@code items} and
 *     {@code _...} (comments); at most {@value #MAX_ITEMS} items; no duplicate keys;</li>
 *     <li>{@code id} {@code [A-Za-z0-9_-]{1,40}}, unique, not one of the generated ids ({@code maintenance},
 *     {@code slide}, {@code event-<n>});</li>
 *     <li>{@code kind} {@code ev} / {@code up} / {@code mt}; {@code title} 1-{@value NewsItem#MAX_TITLE} characters;
 *     {@code body} 0-{@value NewsItem#MAX_BODY} (optional);</li>
 *     <li>{@code startAt} / {@code endAt}: {@code yyyy-MM-dd} or {@code yyyy-MM-ddTHH:mm} in the server's time zone,
 *     or null / absent; a date-only {@code endAt} lasts to the end of that day; {@code endAt >= startAt};</li>
 *     <li>{@code pinned} boolean (default false); no other keys (a misspelt key is an error, not silently
 *     ignored).</li>
 * </ul>
 * No file = no notices ({@code missing}; the previous version is not kept). Only the items visible now are returned.
 */
public final class NewsFileSource implements NewsSource {
    public static final Path DEFAULT_FILE = Path.of("data", "launcher", "news.json");
    static final long CHECK_MS = 5_000;
    static final int MAX_BYTES = 256 * 1024;
    static final int MAX_ITEMS = 50;
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,40}");
    private static final Pattern GENERATED_ID = Pattern.compile("maintenance|slide|event-\\d+");
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DATE_TIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}");
    private static final Set<String> ITEM_KEYS = Set.of("id", "kind", "title", "body", "startAt", "endAt", "pinned");
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    public static final String OK = "ok";
    public static final String MISSING = "missing";
    public static final String INVALID = "invalid";

    /** news.json could not be accepted; the message is a field path such as {@code items[2].kind}. */
    static final class InvalidNews extends Exception {
        private static final long serialVersionUID = 1L;

        InvalidNews(String where) {
            super(where);
        }
    }

    private final Path file;
    private final ZoneId zone;
    private long lastCheck = Long.MIN_VALUE;
    private String lastKey;
    private List<NewsItem> valid = List.of();
    private String status = MISSING;
    private String lastProblem;

    public NewsFileSource(Path file) {
        this(file, ZoneId.systemDefault());
    }

    public NewsFileSource(Path file, ZoneId zone) {
        this.file = Objects.requireNonNull(file, "file");
        this.zone = Objects.requireNonNull(zone, "zone");
    }

    @Override
    public String name() {
        return "news.json";
    }

    @Override
    public synchronized List<NewsItem> items(long now) {
        refresh(now);
        List<NewsItem> out = new ArrayList<>();
        for (NewsItem item : valid) {
            if (item.visibleAt(now)) {
                out.add(item);
            }
        }
        return out;
    }

    /** {@code ok}, {@code missing} or {@code invalid} (the previous valid items are still served). */
    public synchronized String status() {
        return status;
    }

    /** The field path that made the current file invalid, or null. */
    public synchronized String problem() {
        return status.equals(INVALID) ? lastProblem : null;
    }

    private void refresh(long now) {
        if (lastCheck != Long.MIN_VALUE && now - lastCheck < CHECK_MS && now >= lastCheck) {
            return;
        }
        lastCheck = now;
        String key;
        try {
            BasicFileAttributes a = Files.readAttributes(file, BasicFileAttributes.class);
            key = a.isRegularFile() ? a.lastModifiedTime().toMillis() + ":" + a.size() : "missing";
        } catch (IOException e) {
            key = "missing";
        }
        if (key.equals(lastKey)) {
            return;
        }
        lastKey = key;
        if (key.equals("missing")) {
            valid = List.of();
            status = MISSING;
            return;
        }
        try {
            byte[] bytes = Files.size(file) > MAX_BYTES ? null : Files.readAllBytes(file);
            if (bytes == null || bytes.length > MAX_BYTES) {
                throw new InvalidNews("size");
            }
            valid = parse(bytes, zone);
            status = OK;
            lastProblem = null;
        } catch (NoSuchFileException e) {
            valid = List.of();
            status = MISSING;
        } catch (InvalidNews e) {
            invalid(e.getMessage());
        } catch (IOException e) {
            invalid("read");
        }
    }

    private void invalid(String where) {
        status = INVALID;
        lastProblem = where;
        System.out.println("[LauncherNews] " + file + " is invalid (" + where + "): keeping the previous valid version ("
                + valid.size() + " items)");
    }

    /** Parses and validates a whole news.json; every item is checked even when it is not visible now. */
    static List<NewsItem> parse(byte[] bytes, ZoneId zone) throws InvalidNews {
        JsonNode root;
        try {
            root = JSON.readTree(bytes);
        } catch (IOException e) {
            throw new InvalidNews("json");
        }
        if (root == null || !root.isObject()) {
            throw new InvalidNews("root");
        }
        for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
            String k = it.next();
            if (!k.equals("items") && !k.startsWith("_")) {
                throw new InvalidNews("root." + safe(k));
            }
        }
        JsonNode items = root.get("items");
        if (items == null || !items.isArray()) {
            throw new InvalidNews("items");
        }
        if (items.size() > MAX_ITEMS) {
            throw new InvalidNews("items.size");
        }
        List<NewsItem> out = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            String at = "items[" + i + "]";
            JsonNode n = items.get(i);
            if (!n.isObject()) {
                throw new InvalidNews(at);
            }
            for (Iterator<String> it = n.fieldNames(); it.hasNext(); ) {
                String k = it.next();
                if (!ITEM_KEYS.contains(k)) {
                    throw new InvalidNews(at + "." + safe(k));
                }
            }
            String id = text(n, "id", at, false);
            if (!ID.matcher(id).matches() || GENERATED_ID.matcher(id).matches()) {
                throw new InvalidNews(at + ".id");
            }
            if (!ids.add(id)) {
                throw new InvalidNews(at + ".id.duplicate");
            }
            String kind = text(n, "kind", at, false);
            if (!NewsItem.KINDS.contains(kind)) {
                throw new InvalidNews(at + ".kind");
            }
            String title = NewsItem.clean(text(n, "title", at, false)).strip();
            if (title.isEmpty() || NewsItem.length(title) > NewsItem.MAX_TITLE) {
                throw new InvalidNews(at + ".title");
            }
            String body = NewsItem.clean(text(n, "body", at, true));
            if (NewsItem.length(body) > NewsItem.MAX_BODY) {
                throw new InvalidNews(at + ".body");
            }
            Long start = time(n.get("startAt"), zone, false, at + ".startAt");
            Long end = time(n.get("endAt"), zone, true, at + ".endAt");
            if (start != null && end != null && end < start) {
                throw new InvalidNews(at + ".endAt");
            }
            JsonNode pinned = n.get("pinned");
            if (pinned != null && !pinned.isNull() && !pinned.isBoolean()) {
                throw new InvalidNews(at + ".pinned");
            }
            out.add(new NewsItem(id, kind, title, body, start, end, pinned != null && pinned.asBoolean(false)));
        }
        return List.copyOf(out);
    }

    private static String text(JsonNode n, String key, String at, boolean optional) throws InvalidNews {
        JsonNode v = n.get(key);
        if (v == null || v.isNull()) {
            if (optional) {
                return "";
            }
            throw new InvalidNews(at + "." + key);
        }
        if (!v.isTextual()) {
            throw new InvalidNews(at + "." + key);
        }
        return v.textValue();
    }

    /**
     * {@code yyyy-MM-dd} (start of the day; for an end, the last millisecond of that day) or {@code yyyy-MM-ddTHH:mm}
     * in {@code zone}; null / absent = no limit.
     */
    static Long time(JsonNode v, ZoneId zone, boolean end, String at) throws InvalidNews {
        if (v == null || v.isNull()) {
            return null;
        }
        if (!v.isTextual()) {
            throw new InvalidNews(at);
        }
        String s = v.textValue();
        try {
            if (DATE.matcher(s).matches()) {
                LocalDate d = LocalDate.parse(s);
                return end ? d.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
                        : d.atStartOfDay(zone).toInstant().toEpochMilli();
            }
            if (DATE_TIME.matcher(s).matches()) {
                return LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli();
            }
        } catch (DateTimeParseException e) {
            throw new InvalidNews(at);
        }
        throw new InvalidNews(at);
    }

    /** A key as it may appear in a log line: at most 20 plain characters. */
    private static String safe(String key) {
        String s = key.replaceAll("[^A-Za-z0-9_-]", "?");
        return s.length() > 20 ? s.substring(0, 20) + "..." : s;
    }
}
