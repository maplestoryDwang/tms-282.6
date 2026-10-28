package net.swordie.ms.handlers.login.tms282;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.protocol.capture.PacketCapture;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Optional local observer summaries. Never queries DB, changes gameplay, or reads paths from JSON. */
public final class Tms282ClientDiagnostics {
    public static final String PROPERTY = "tms282.clientDiagnostics.summary";
    static final int MAX_BYTES = 16 * 1024;
    static final long LOG_BYTES = 5L * 1024 * 1024;
    private static Tms282ClientDiagnostics standard;
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(6).maxStringLength(256).maxNumberLength(20).build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build());
    private final Path summary, log;
    private final long logLimit;
    private final Set<Client> clients = new HashSet<>();
    private final LinkedHashMap<String, State> states = new LinkedHashMap<>();
    private final LinkedHashMap<Client, Recent> recent = new LinkedHashMap<>();
    private record Recent(Instant seen, ObjectNode binding) { }
    private record Resolution(ArrayNode bindings, boolean ambiguous) { }
    private String lastError;
    private static final class State {
        ObjectNode summary;
        ArrayNode bindings;
        String emitted;
    }

    Tms282ClientDiagnostics(Path summary, Path log, long logLimit) {
        this.summary = summary.toAbsolutePath().normalize();
        this.log = log.toAbsolutePath().normalize();
        this.logLimit = logLimit;
    }

    public static synchronized void registerClient(Client client) {
        String configured = System.getProperty(PROPERTY, "").trim();
        if (configured.isEmpty()) return; // No file access or executor when disabled (the default).
        try {
            if (standard == null) {
                standard = new Tms282ClientDiagnostics(Path.of(configured), Path.of("logs/client-diagnostics.jsonl"), LOG_BYTES);
                var timer = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "Tms282ClientDiagnostics"); t.setDaemon(true); return t;
                });
                timer.scheduleWithFixedDelay(standard::pollSafely, 0, 5, TimeUnit.SECONDS);
            }
            standard.add(client);
        } catch (RuntimeException e) {
            System.out.println("[ClientDiagnostics] initialization unavailable: " + e.getClass().getSimpleName());
        }
    }

    public static synchronized void removeClient(Client client) {
        if (standard != null) {
            try { standard.remove(client); }
            catch (RuntimeException e) { standard.forget(client); } // Diagnostics must not block timer/logout cleanup.
        }
    }

    public static synchronized void snapshotClient(Client client, Char character) {
        if (standard != null) {
            try { standard.snapshot(client, character, Instant.now()); }
            catch (RuntimeException ignored) { /* Optional diagnostics cannot affect entering a field. */ }
        }
    }

    synchronized void add(Client client) { clients.add(client); snapshot(client, null, Instant.now()); }
    private synchronized void forget(Client client) { clients.remove(client); }
    synchronized void remove(Client client) {
        snapshot(client, null, Instant.now());
        // Preserve the last observed identity before channel logout clears its character. No file I/O here.
        for (State state : states.values()) {
            if (!state.summary.path("exited").asBoolean() && matches(state.summary, client)) {
                ArrayNode updated = bindings(state.summary);
                // Logout can already have cleared Client.chr. Never replace a known identity with null.
                if (state.bindings != null) for (JsonNode b : updated) for (JsonNode old : state.bindings) {
                    if (b.path("session").equals(old.path("session"))) {
                        for (String key : List.of("userId", "characterId", "mapId"))
                            if (b.path(key).isNull()) ((ObjectNode) b).set(key, old.get(key));
                    }
                }
                if (!updated.isEmpty()) state.bindings = updated;
            }
        }
        clients.remove(client);
    }

    private synchronized void snapshot(Client client, Char character, Instant now) {
        ObjectNode b = binding(client, character);
        if (b != null) {
            Recent old = recent.get(client);
            if (old != null) for (String key : List.of("userId", "characterId", "mapId"))
                if (b.path(key).isNull()) b.set(key, old.binding.get(key));
            recent.remove(client); recent.put(client, new Recent(now, b));
        }
        pruneRecent(now);
    }

    private void pruneRecent(Instant now) {
        recent.entrySet().removeIf(e -> e.getValue().seen.isBefore(now.minusSeconds(120)));
        while (recent.size() > 128) recent.remove(recent.keySet().iterator().next());
    }

    private void pollSafely() {
        try { poll(Instant.now()); lastError = null; }
        catch (IOException | RuntimeException e) {
            String error = e.getClass().getSimpleName();
            if (!error.equals(lastError)) System.out.println("[ClientDiagnostics] summary ignored: " + error);
            lastError = error;
        }
    }

    synchronized void poll(Instant now) throws IOException {
        if (!Files.exists(summary)) return;
        byte[] bytes;
        try (var input = Files.newInputStream(summary)) { bytes = input.readNBytes(MAX_BYTES + 1); }
        require(bytes.length > 0 && bytes.length <= MAX_BYTES);
        JsonNode parsed;
        try (var parser = JSON.createParser(bytes)) {
            parsed = JSON.readTree(parser); require(parser.nextToken() == null);
        }
        ObjectNode input = validate(parsed, now);
        String run = input.path("runId").asText();
        State previous = states.get(run);
        if (previous != null) {
            require(previous.summary.get("pid").equals(input.get("pid"))
                    && previous.summary.get("creationTime").equals(input.get("creationTime")));
            if (Instant.parse(input.path("updatedUtc").asText()).isBefore(Instant.parse(previous.summary.path("updatedUtc").asText()))) return;
            require(input.path("tickets").asLong() >= previous.summary.path("tickets").asLong()
                    && input.path("recoveredCount").asLong() >= previous.summary.path("recoveredCount").asLong()
                    && input.path("dropped").asLong() >= previous.summary.path("dropped").asLong());
            if (previous.summary.path("exited").asBoolean() && !input.path("exited").asBoolean()) return;
        }
        State next = new State(); next.summary = input;
        for (Client client : clients) snapshot(client, null, now);
        // Correlate even a fast exit before the first poll, using captured endpoints, never names/timing.
        // Multiple retained sessions for one recycled endpoint are ambiguous and deliberately unmatched.
        Resolution resolution = recentBindings(input, now);
        ArrayNode proven = resolution.bindings;
        boolean active = !input.path("exited").asBoolean() && !bindings(input).isEmpty();
        next.bindings = !proven.isEmpty() ? proven : !resolution.ambiguous && previous != null && previous.bindings != null
                ? previous.bindings : JSON.createArrayNode();
        ObjectNode event = input.deepCopy();
        event.remove("updatedUtc"); event.remove("connections");
        event.set("bindings", next.bindings);
        event.put("ambiguousEndpoints", resolution.ambiguous);
        event.put("bindingSource", next.bindings.isEmpty() ? "unmatched" : active ? "endpoint" : "cached-endpoint");
        String fingerprint = JSON.writeValueAsString(event);
        next.emitted = fingerprint;
        if (previous == null || !fingerprint.equals(previous.emitted)) {
            event.put("observedUtc", now.toString()); event.put("updatedUtc", input.path("updatedUtc").asText());
            byte[] line = (JSON.writeValueAsString(event) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            require(line.length <= MAX_BYTES);
            append(line);
            long fault = input.path("lastFault").path("sequence").asLong();
            if ((fault > 0 && (previous == null || fault > previous.summary.path("lastFault").path("sequence").asLong()))
                    || (input.path("exited").asBoolean() && (previous == null || !previous.summary.path("exited").asBoolean()))) {
                System.out.printf("[ClientDiagnostics] run=%s pid=%d fault=%d redirects=%d exited=%s bindings=%d%n",
                        run, input.path("pid").asLong(), fault, input.path("recoveredCount").asLong(),
                        input.path("exited").asBoolean(), next.bindings.size());
            }
        }
        states.put(run, next);
        while (states.size() > 128) states.remove(states.keySet().iterator().next());
    }

    private ArrayNode bindings(ObjectNode input) {
        ArrayNode found = JSON.createArrayNode();
        clients.stream().sorted(Comparator.comparing(c -> c.getCh().id().asLongText())).forEach(client -> {
            if (!matches(input, client)) return;
            ObjectNode b = binding(client, null); if (b != null) found.add(b);
        });
        return found;
    }

    private ObjectNode binding(Client client, Char character) {
        if (!(client.getCh().remoteAddress() instanceof InetSocketAddress remote)
                || !(client.getCh().localAddress() instanceof InetSocketAddress local) || remote.getAddress() == null) return null;
        var capture = PacketCapture.of(client.getCh());
        if (capture == null) return null;
        String session = capture.session();
        if (session.length() > 160 || !session.matches("[A-Za-z0-9_.-]+")) return null;
        ObjectNode b = JSON.createObjectNode(); b.put("session", session);
        b.put("localAddress", remote.getAddress().getHostAddress());
        b.put("localPort", remote.getPort()); b.put("remotePort", local.getPort());
        var user = client.getUser(); var chr = character != null ? character : client.getChr();
        if (user == null) b.putNull("userId"); else b.put("userId", user.getId());
        if (chr == null) { b.putNull("characterId"); b.putNull("mapId"); }
        else { b.put("characterId", chr.getId()); b.put("mapId", chr.getFieldID()); }
        return b;
    }

    private Resolution recentBindings(ObjectNode input, Instant now) {
        pruneRecent(now); ArrayNode found = JSON.createArrayNode();
        boolean ambiguous = false;
        for (JsonNode endpoint : input.path("connections")) {
            Map<String, ObjectNode> candidates = new TreeMap<>();
            for (Recent r : recent.values()) {
                ObjectNode b = r.binding;
                if (b.path("localAddress").equals(endpoint.path("localAddress"))
                        && b.path("localPort").equals(endpoint.path("localPort"))
                        && b.path("remotePort").equals(endpoint.path("remotePort"))) candidates.put(b.path("session").asText(), b);
            }
            if (candidates.size() == 1) found.add(candidates.values().iterator().next());
            if (candidates.size() > 1) ambiguous = true;
        }
        return new Resolution(found, ambiguous);
    }

    private boolean matches(ObjectNode input, Client client) {
        if (!(client.getCh().remoteAddress() instanceof InetSocketAddress remote)
                || !(client.getCh().localAddress() instanceof InetSocketAddress local) || remote.getAddress() == null) return false;
        for (JsonNode c : input.path("connections")) {
            if (remote.getPort() == c.path("localPort").asInt() && local.getPort() == c.path("remotePort").asInt()
                    && remote.getAddress().getHostAddress().equals(c.path("localAddress").asText())) return true;
        }
        return false;
    }

    private static ObjectNode validate(JsonNode n, Instant now) {
        require(n != null && n.isObject());
        require(number(n, "schema", 1, 1) == 1);
        String run = text(n, "runId"); require(run.matches("[A-Za-z0-9_.-]{1,160}"));
        number(n, "pid", 1, 0xffffffffL);
        require(text(n, "creationTime").matches("[0-9]{1,20}"));
        require(n.path("clientExeSha256").isNull()
                || text(n, "clientExeSha256").matches("[0-9a-fA-F]{64}"));
        Instant stamp = Instant.parse(text(n, "updatedUtc"));
        require(!stamp.isAfter(now.plusSeconds(10)) && !stamp.isBefore(now.minusSeconds(30)));
        require(n.path("exited").isBoolean());
        if (n.path("exited").asBoolean()) number(n, "exitCode", 0, 0xffffffffL); else require(n.path("exitCode").isNull());
        number(n, "compatible", 0, 1); number(n, "ready", 0, 1);
        long tickets = number(n, "tickets", 0, Long.MAX_VALUE);
        require(number(n, "recoveredCount", 0, Long.MAX_VALUE) <= tickets);
        require(number(n, "dropped", 0, Long.MAX_VALUE) <= tickets);
        JsonNode fault = n.path("lastFault"); require(fault.isNull() || fault.isObject());
        if (!fault.isNull()) {
            require(number(fault, "sequence", 1, Long.MAX_VALUE) <= tickets);
            require(text(fault, "code").matches("0x[0-9A-Fa-f]{8}"));
            require(fault.path("rva").isNull() || text(fault, "rva").matches("0x[0-9A-Fa-f]{1,16}"));
            if (!fault.path("skillId").isNull()) number(fault, "skillId", 0, Integer.MAX_VALUE);
            require(fault.path("recovered").isBoolean());
        }
        JsonNode connections = n.path("connections"); require(connections.isArray() && connections.size() <= 16);
        Set<String> endpoints = new HashSet<>();
        for (JsonNode c : connections) {
            String address = text(c, "localAddress");
            String[] octets = address.split("\\.", -1); require(octets.length == 4);
            for (String part : octets) require(part.matches("0|[1-9][0-9]{0,2}") && Integer.parseInt(part) <= 255);
            long port = number(c, "localPort", 1, 65535), server = number(c, "remotePort", 1, 65535);
            require(server == 8484 || (server >= 8585 && server <= 8588));
            require(endpoints.add(address + ":" + port + ":" + server));
        }
        // Whitelist fields: do not forward arbitrary registers, filenames, paths or extra content to logs.
        ObjectNode result = JSON.createObjectNode();
        for (String key : List.of("schema", "runId", "pid", "creationTime", "clientExeSha256", "updatedUtc", "exited", "exitCode", "compatible", "ready", "tickets", "recoveredCount", "dropped")) result.set(key, n.get(key));
        if (fault.isNull()) result.putNull("lastFault");
        else { ObjectNode f = result.putObject("lastFault"); for (String key : List.of("sequence", "code", "rva", "skillId", "recovered")) f.set(key, fault.get(key)); }
        ArrayNode list = result.putArray("connections");
        for (JsonNode c : connections) { ObjectNode a = list.addObject(); for (String key : List.of("localAddress", "localPort", "remotePort")) a.set(key, c.get(key)); }
        return result;
    }

    private static String text(JsonNode n, String key) { require(n.path(key).isTextual()); return n.path(key).textValue(); }
    private static long number(JsonNode n, String key, long min, long max) {
        JsonNode v = n.path(key); require(v.isIntegralNumber() && v.canConvertToLong());
        long value = v.longValue(); require(value >= min && value <= max); return value;
    }
    private static void require(boolean condition) { if (!condition) throw new IllegalArgumentException("Invalid diagnostic summary"); }

    private void append(byte[] line) throws IOException {
        Files.createDirectories(log.getParent());
        if (Files.exists(log) && Files.size(log) + line.length > logLimit) {
            Files.deleteIfExists(archive(3));
            for (int i = 2; i >= 1; i--) if (Files.exists(archive(i))) Files.move(archive(i), archive(i + 1), StandardCopyOption.REPLACE_EXISTING);
            Files.move(log, archive(1), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.write(log, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    private Path archive(int index) { return log.resolveSibling(log.getFileName() + "." + index); }
}
