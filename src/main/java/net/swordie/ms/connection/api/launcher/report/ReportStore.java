package net.swordie.ms.connection.api.launcher.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Problem reports sent from the launcher (1.2.8): one JSON file each under {@code logs/launcher-reports/}
 * ({@code <yyyyMMdd-HHmmss>-u<userId>-<4 hex>.json}), newest {@link #KEEP} kept. A report holds what the player typed,
 * the launcher's own state (version, mode, last game exit, launcher log tail - never a password: the launcher log has
 * none) and what the server knew at that moment (the account's online characters and their recent skill problems).
 */
public final class ReportStore {
    public static final int KEEP = 300;
    public static final int MAX_TEXT = 500;
    public static final int MAX_LOG = 32 * 1024;
    public static final int MAX_CLIENT = 8 * 1024;
    private static final Pattern ID = Pattern.compile("\\d{8}-\\d{6}-u\\d{1,10}-[0-9a-f]{4}");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ReportStore STANDARD = new ReportStore(Path.of("logs", "launcher-reports"));

    private final Path dir;

    public ReportStore(Path dir) {
        this.dir = dir;
    }

    public static ReportStore standard() {
        return STANDARD;
    }

    /** Stores a report and returns its id. */
    public synchronized String save(long now, int userId, Map<String, Object> report) throws IOException {
        Files.createDirectories(dir);
        String id = STAMP.format(Instant.ofEpochMilli(now)) + "-u" + userId + "-"
                + String.format("%04x", ThreadLocalRandom.current().nextInt(0x10000));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        out.put("time", now);
        out.put("user", userId);
        out.putAll(report);
        Path tmp = dir.resolve(id + ".tmp");
        Files.write(tmp, JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(out));
        Files.move(tmp, dir.resolve(id + ".json"));
        prune();
        return id;
    }

    private void prune() throws IOException {
        List<Path> files = files();
        for (int i = KEEP; i < files.size(); i++) {
            Files.deleteIfExists(files.get(i));
        }
    }

    /** Report files, newest first (the name starts with the time). */
    private List<Path> files() throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> out = new ArrayList<>(s.filter(p -> {
                String n = p.getFileName().toString();
                return n.endsWith(".json") && ID.matcher(n.substring(0, n.length() - 5)).matches();
            }).toList());
            out.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
            return out;
        }
    }

    /** The newest {@code limit} reports as summaries: id, time, user, username, text, mode, version, issues (a count). */
    public synchronized List<Map<String, Object>> list(int limit) throws IOException {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Path p : files()) {
            if (out.size() >= limit) {
                break;
            }
            try {
                JsonNode n = JSON.readTree(Files.readAllBytes(p));
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", n.path("id").asText());
                m.put("time", n.path("time").asLong());
                m.put("user", n.path("user").asInt());
                m.put("username", n.path("username").asText(""));
                m.put("text", n.path("text").asText(""));
                m.put("mode", n.path("client").path("mode").asText(""));
                m.put("version", n.path("client").path("version").asText(""));
                m.put("exit", n.path("client").path("lastExit").path("reason").asText(""));
                m.put("issues", n.path("issues").size());
                out.add(m);
            } catch (IOException | RuntimeException e) {
                // an unreadable file is skipped
            }
        }
        return out;
    }

    /** One report as stored, or null when the id is unknown / malformed. */
    public synchronized JsonNode read(String id) throws IOException {
        if (id == null || !ID.matcher(id).matches()) {
            return null;
        }
        Path p = dir.resolve(id + ".json");
        return Files.isRegularFile(p) ? JSON.readTree(new String(Files.readAllBytes(p), StandardCharsets.UTF_8)) : null;
    }
}
