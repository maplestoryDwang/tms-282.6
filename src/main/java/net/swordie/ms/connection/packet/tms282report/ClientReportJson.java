package net.swordie.ms.connection.packet.tms282report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * One-line JSON form of a {@link Tms282ClientReport} (the JSONL record shared by
 * {@link JsonlClientReportSink} and {@link JsonlClientReportSource}):
 * <pre>{"time":"…Z","remote":"/127.0.0.1:50000","account":"","opcode":165,"opcodeHex":"00A5",
 * "name":"…","fields":[{"name":"…","value":"…"}],"raw":"a500…","rawLength":64}</pre>
 * {@code raw} holds the kept (masked, at most {@link Tms282ClientReport#MAX_RAW_BYTES}) bytes and
 * {@code rawLength} the original packet length; a line without {@code rawLength} reads as untruncated.
 * Reading applies every {@link Tms282ClientReport} check again (sensitive opcode, raw starts with the opcode).
 * Jackson escapes line breaks inside strings, so each record stays on one line.
 */
public final class ClientReportJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** {@link JsonlCodec} for {@link JsonlFileSink} / {@link JsonlFileSource}. */
    public static final JsonlCodec<Tms282ClientReport> CODEC =
            JsonlCodec.of(ClientReportJson::toJson, ClientReportJson::fromJson);

    private ClientReportJson() {
    }

    public static String toJson(Tms282ClientReport report) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("time", report.time().toString());
        node.put("remote", report.source().remoteAddress());
        node.put("account", report.source().account());
        node.put("opcode", report.opcode());
        node.put("opcodeHex", report.opcodeHex());
        node.put("name", report.name());
        ArrayNode fields = node.putArray("fields");
        for (Tms282ClientReport.Field field : report.fields()) {
            fields.addObject().put("name", field.name()).put("value", field.value());
        }
        node.put("raw", report.rawHex());
        node.put("rawLength", report.rawLength());
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize client report", e);
        }
    }

    /** Throws {@link IllegalArgumentException} for malformed lines, sensitive opcodes and raw/opcode mismatches. */
    public static Tms282ClientReport fromJson(String line) {
        JsonNode node;
        try {
            node = MAPPER.readTree(line);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed client report line", e);
        }
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException("Client report line is not a JSON object");
        }
        List<Tms282ClientReport.Field> fields = new ArrayList<>();
        for (JsonNode field : required(node, "fields")) {
            fields.add(new Tms282ClientReport.Field(required(field, "name").asText(), required(field, "value").asText()));
        }
        try {
            byte[] raw = HexFormat.of().parseHex(required(node, "raw").asText());
            return new Tms282ClientReport(Instant.parse(required(node, "time").asText()),
                    new Tms282ClientReport.Source(node.path("remote").asText(""), node.path("account").asText("")),
                    required(node, "opcode").asInt(), required(node, "name").asText(), fields,
                    raw, node.path("rawLength").asInt(raw.length));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid client report line: " + e.getMessage(), e);
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
