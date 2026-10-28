package net.swordie.ms.connection.protocol;

import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * TMS 282.3 opcode tables: generation from the research status table and loading into an {@link OpcodeTable}.
 * <p>
 * Source: research/tms2823/packet-audit/opcode-status-282.tsv. A row is kept when its status is
 * LIVE_VERIFIED, CAPTURED or IMPLEMENTED_UNTESTED, or when it is UNVERIFIED, has a 265 name and its in-game
 * map evidence level is 證實 ({@code "in-game map (...) 證實:"}). Output: {@code inbound.tsv} (CP) and
 * {@code outbound.tsv} (LP) under {@code data/protocol/tms282.3/}; {@code tools/GenerateTms282OpcodeTables.java}
 * writes them and OpcodeTableCheck proves they are current.
 * <p>
 * Format rules (fail-closed, first match wins):
 * <ol>
 *     <li>status row (live/captured/implemented): {@link WireFormat#PROFILE_CODEC}; these login and channel
 *     packets are owned by the 282 codecs, a 265 body is not proven for any of them;</li>
 *     <li>evidence says the read sequence only partly matches (序列部分) or 282 reads more ({@code 多 u}):
 *     {@link WireFormat#PROFILE_CODEC};</li>
 *     <li>evidence says the whole sequence matches (序列一致（exact）/(full,...)): {@link WireFormat#SAME_AS_265};</li>
 *     <li>anything else (prefix-only match, prose): {@link WireFormat#UNKNOWN}.</li>
 * </ol>
 */
public final class Tms282OpcodeTables {
    public static final String INBOUND_FILE = "inbound.tsv";
    public static final String OUTBOUND_FILE = "outbound.tsv";
    public static final String COLUMNS = "opcode\tname\topcode_265\tstatus\tformat\trule";

    private static final Pattern CONFIRMED = Pattern.compile("in-game map \\([^)]*\\) 證實:");
    private static final Pattern PARTIAL = Pattern.compile("序列部分|多 ?u(8|16|32|64)");
    private static final Pattern WHOLE = Pattern.compile("序列一致(（exact）|\\(full)");

    private Tms282OpcodeTables() {
    }

    /** Traffic direction of a status row: CP = client packet (inbound), LP = server packet (outbound). */
    public enum Direction {
        CP(INBOUND_FILE),
        LP(OUTBOUND_FILE);

        private final String file;

        Direction(String file) {
            this.file = file;
        }

        public String file() {
            return file;
        }
    }

    /**
     * One generated row.
     *
     * @param name  265 enum name as written in the status table, or "-" when the opcode has none
     * @param op265 265 opcode as written in the status table (hex or decimal), or "-"
     * @param rule  which format rule decided {@code format}
     */
    public record Row(Direction direction, int op, String name, String op265, OpcodeEvidence evidence,
                      WireFormat format, String rule) {
        String toTsv() {
            return String.format("0x%04X\t%s\t%s\t%s\t%s\t%s", op, name, op265, evidence, format, rule);
        }
    }

    /** Applies the selection and format rules to the lines of opcode-status-282.tsv (header included). */
    public static List<Row> generate(List<String> statusLines) {
        List<Row> rows = new ArrayList<>();
        boolean header = true;
        for (String line : statusLines) {
            if (header) {
                header = false;
                if (!line.startsWith("direction\topcode_282\tname_265\topcode_265\tstatus\tevidence")) {
                    throw new IllegalArgumentException("Unexpected status table header: " + line);
                }
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            List<String> f = splitTsv(line);
            if (f.size() < 6) {
                throw new IllegalArgumentException("Short status row: " + line);
            }
            classify(Direction.valueOf(f.get(0)), Integer.decode(f.get(1)), f.get(2), f.get(3), f.get(4), f.get(5))
                    .ifPresent(rows::add);
        }
        return rows;
    }

    static Optional<Row> classify(Direction dir, int op, String name, String op265, String status, String evidence) {
        switch (status) {
            case "LIVE_VERIFIED", "CAPTURED", "IMPLEMENTED_UNTESTED" -> {
                return Optional.of(new Row(dir, op, name, op265, OpcodeEvidence.valueOf(status),
                        WireFormat.PROFILE_CODEC, "status"));
            }
            case "UNVERIFIED" -> {
                if (name.equals("-") || !CONFIRMED.matcher(evidence).find()) {
                    return Optional.empty();
                }
                WireFormat format;
                String rule;
                if (PARTIAL.matcher(evidence).find()) {
                    format = WireFormat.PROFILE_CODEC;
                    rule = "seq-partial";
                } else if (WHOLE.matcher(evidence).find()) {
                    format = WireFormat.SAME_AS_265;
                    rule = "seq-whole";
                } else {
                    format = WireFormat.UNKNOWN;
                    rule = evidence.contains("序列一致") ? "seq-prefix" : "unclassified";
                }
                return Optional.of(new Row(dir, op, name, op265, OpcodeEvidence.STATIC_CONFIRMED, format, rule));
            }
            default -> throw new IllegalArgumentException("Unknown status " + status + " for " + dir + " " + op);
        }
    }

    /** TSV text of one direction, as written to {@link Direction#file()}. */
    public static String render(List<Row> rows, Direction direction, String source) {
        StringBuilder out = new StringBuilder();
        out.append("# Generated by tools/GenerateTms282OpcodeTables.java from ").append(source).append(". Do not edit.\n");
        out.append("# Rules: net.swordie.ms.connection.protocol.Tms282OpcodeTables. format: SAME_AS_265 | PROFILE_CODEC | UNKNOWN.\n");
        out.append(COLUMNS).append('\n');
        for (Row row : rows) {
            if (row.direction() == direction) {
                out.append(row.toTsv()).append('\n');
            }
        }
        return out.toString();
    }

    /** Reads a generated file back into rows. */
    public static List<Row> read(Path file, Direction direction) throws IOException {
        List<Row> rows = new ArrayList<>();
        boolean columns = false;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!columns) {
                if (!line.equals(COLUMNS)) {
                    throw new IOException(file + ": unexpected columns " + line);
                }
                columns = true;
                continue;
            }
            String[] f = line.split("\t", -1);
            if (f.length != 6) {
                throw new IOException(file + ": bad row " + line);
            }
            rows.add(new Row(direction, Integer.decode(f[0]), f[1], f[2], OpcodeEvidence.valueOf(f[3]),
                    WireFormat.valueOf(f[4]), f[5]));
        }
        return rows;
    }

    /**
     * Loads {@code dir/inbound.tsv} and {@code dir/outbound.tsv}. A row binds to the InHeader/OutHeader constant
     * named like its label: the 265 name, or {@code CPxxxx}/{@code LPxxxx} for an unnamed row (the 282-only
     * constants, value UnkValue). A label without a constant stays unbound (header null).
     */
    public static OpcodeTable load(Path dir, Predicate<InboundOpcode> inboundFilter,
                                   Predicate<OutboundOpcode> outboundFilter) throws IOException {
        return builder(dir, inboundFilter, outboundFilter).build();
    }

    /** As {@link #load}, but returns the builder so a profile can add the opcodes its overlay declares. */
    public static OpcodeTable.Builder builder(Path dir, Predicate<InboundOpcode> inboundFilter,
                                              Predicate<OutboundOpcode> outboundFilter) throws IOException {
        return builder(dir, inboundFilter, outboundFilter, op -> null);
    }

    /**
     * As {@link #builder(Path, Predicate, Predicate)}; an inbound row whose label has no constant binds to
     * {@code inboundFallback.apply(op)} (a profile overlay's header for that opcode; null leaves it unbound).
     */
    public static OpcodeTable.Builder builder(Path dir, Predicate<InboundOpcode> inboundFilter,
                                              Predicate<OutboundOpcode> outboundFilter,
                                              java.util.function.IntFunction<InHeader> inboundFallback) throws IOException {
        OpcodeTable.Builder b = OpcodeTable.builder(ProtocolVersion.TMS_282_3);
        for (Row r : read(dir.resolve(INBOUND_FILE), Direction.CP)) {
            String label = label(r);
            InHeader header = inHeader(label);
            if (header == null) {
                header = inboundFallback.apply(r.op());
            }
            InboundOpcode e = new InboundOpcode(r.op(), label, header, r.format(), r.evidence());
            if (inboundFilter.test(e)) {
                b.inbound(e);
            }
        }
        for (Row r : read(dir.resolve(OUTBOUND_FILE), Direction.LP)) {
            String label = label(r);
            OutboundOpcode e = new OutboundOpcode(r.op(), label, outHeader(label), r.format(), r.evidence());
            if (outboundFilter.test(e)) {
                b.outbound(e);
            }
        }
        return b;
    }

    /** {@code CPxxxx}/{@code LPxxxx}: the name of an unnamed row and of its 282-only header constant. */
    public static String label(Direction direction, int op) {
        return String.format(Locale.ROOT, "%s%04X", direction, op);
    }

    private static String label(Row r) {
        return r.name().equals("-") ? label(r.direction(), r.op()) : r.name();
    }

    static InHeader inHeader(String name) {
        try {
            return InHeader.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static OutHeader outHeader(String name) {
        try {
            return OutHeader.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Splits one TSV line with CSV-style quoting ("..." with "" escapes), as the status table uses. */
    static List<String> splitTsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"' && cur.isEmpty()) {
                quoted = true;
            } else if (ch == '\t') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        fields.add(cur.toString());
        return fields;
    }
}
