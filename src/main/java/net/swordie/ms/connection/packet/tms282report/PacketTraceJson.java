package net.swordie.ms.connection.packet.tms282report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.HexFormat;

/**
 * One-line JSON form of a {@link Tms282PacketTrace} (the record of {@code logs/tms282-packets.jsonl}):
 * <pre>{"time":"…Z","dir":"RECV","socket":"LOGIN","session":"1790…-3-123","seq":12,"opcode":165,
 * "opcodeHex":"00A5","name":"…","length":64,"hex":"a500…","redacted":false}</pre>
 * {@code hex} holds the kept bytes (masked, at most {@link Tms282PacketTrace#MAX_DATA_BYTES}) and {@code length}
 * the original packet length. {@code opcodeHex} and {@code redacted} are for people reading the file; reading
 * derives them again and applies every {@link Tms282PacketTrace} check (sensitive body zeroed, data starts with
 * the opcode).
 */
public final class PacketTraceJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** {@link JsonlCodec} for {@link JsonlFileSink} / {@link JsonlFileSource}. */
    public static final JsonlCodec<Tms282PacketTrace> CODEC = JsonlCodec.of(PacketTraceJson::toJson, PacketTraceJson::fromJson);

    private PacketTraceJson() {
    }

    public static String toJson(Tms282PacketTrace trace) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("time", trace.time().toString());
        node.put("dir", trace.direction().name());
        node.put("socket", trace.socket().name());
        node.put("session", trace.session());
        node.put("seq", trace.sequence());
        node.put("opcode", trace.opcode());
        node.put("opcodeHex", String.format("%04X", trace.opcode()));
        node.put("name", trace.name());
        node.put("length", trace.length());
        node.put("hex", trace.hex());
        node.put("redacted", trace.redacted());
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize packet trace", e);
        }
    }

    /** Throws {@link IllegalArgumentException} for malformed lines and for anything the record refuses. */
    public static Tms282PacketTrace fromJson(String line) {
        JsonNode node;
        try {
            node = MAPPER.readTree(line);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed packet trace line", e);
        }
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Packet trace line is not a JSON object");
        }
        try {
            return new Tms282PacketTrace(Instant.parse(required(node, "time").asText()),
                    Tms282PacketTrace.Direction.valueOf(required(node, "dir").asText()),
                    Tms282PacketTrace.SocketKind.valueOf(required(node, "socket").asText()),
                    node.path("session").asText(""), required(node, "seq").asInt(),
                    required(node, "opcode").asInt(), required(node, "name").asText(),
                    required(node, "length").asInt(), HexFormat.of().parseHex(required(node, "hex").asText()));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid packet trace line: " + e.getMessage(), e);
        }
    }

    private static JsonNode required(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing " + name);
        }
        return value;
    }
}
