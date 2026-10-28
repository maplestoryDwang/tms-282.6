package net.swordie.ms.world.tms282.revival;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Switches and periods of the revived legacy events (data/tms282-revival/revival-events.json, built-in default
 * {@link RevivalDefaults#JSON}). Immutable; {@link RevivalConfigSource} swaps whole instances.
 *
 * <pre>{"version": "...", "events": {"&lt;key&gt;": {"enabled": true, "start": "2026-10-05", "end": "2026-12-31"}}}</pre>
 * start / end are inclusive ISO dates in {@code ServerConfig.ZONE}; either may be omitted (open-ended).
 */
public final class RevivalConfig {
    public static final class InvalidConfig extends RuntimeException {
        public InvalidConfig(String message) {
            super(message);
        }
    }

    public record Period(boolean enabled, LocalDate start, LocalDate end) {
        public boolean openOn(LocalDate day) {
            return enabled && (start == null || !day.isBefore(start)) && (end == null || !day.isAfter(end));
        }
    }

    private static final Set<String> EVENT_FIELDS = Set.of("enabled", "start", "end", "note");
    private final String version;
    private final Map<String, Period> events;

    private RevivalConfig(String version, Map<String, Period> events) {
        this.version = version;
        this.events = Collections.unmodifiableMap(events);
    }

    public static RevivalConfig defaults() {
        return parse(RevivalDefaults.JSON.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static RevivalConfig parse(byte[] json) {
        JsonNode root;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            root = mapper.readTree(json);
        } catch (Exception e) {
            throw new InvalidConfig("not valid JSON: " + e.getClass().getSimpleName());
        }
        if (root == null || !root.isObject()) throw new InvalidConfig("root must be an object");
        String version = root.path("version").asText("");
        if (version.isEmpty() || version.length() > 40) throw new InvalidConfig("version missing");
        JsonNode list = root.get("events");
        if (list == null || !list.isObject()) throw new InvalidConfig("events must be an object");
        Map<String, Period> events = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = list.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = e.getKey();
            if (!key.matches("[a-z][a-z0-9_]{0,23}")) throw new InvalidConfig("bad event key " + key);
            JsonNode v = e.getValue();
            if (!v.isObject()) throw new InvalidConfig(key + " must be an object");
            for (Iterator<String> f = v.fieldNames(); f.hasNext(); ) {
                String name = f.next();
                if (!EVENT_FIELDS.contains(name)) throw new InvalidConfig(key + ": unknown field " + name);
            }
            if (!v.path("enabled").isBoolean()) throw new InvalidConfig(key + ": enabled must be true/false");
            LocalDate start = date(key, v.get("start")), end = date(key, v.get("end"));
            if (start != null && end != null && end.isBefore(start)) throw new InvalidConfig(key + ": end before start");
            events.put(key, new Period(v.get("enabled").booleanValue(), start, end));
        }
        return new RevivalConfig(version, events);
    }

    private static LocalDate date(String key, JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (!n.isTextual()) throw new InvalidConfig(key + ": dates are \"yyyy-MM-dd\" strings");
        try {
            return LocalDate.parse(n.asText());
        } catch (DateTimeParseException e) {
            throw new InvalidConfig(key + ": bad date " + n.asText());
        }
    }

    public String version() {
        return version;
    }

    /** A key the file does not list is closed. */
    public Period period(String key) {
        return events.getOrDefault(key, new Period(false, null, null));
    }

    public Set<String> keys() {
        return events.keySet();
    }
}
