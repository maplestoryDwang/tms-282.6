package net.swordie.ms.connection.packet.tms282report;

import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * One parsed TMS 282.3 client report packet: a packet the client sends without expecting a reply
 * (version string, ELog text, machine information, hardware, loading time and similar).
 * <p>
 * Immutable value. {@code raw} is the plaintext packet including its two-byte opcode, which must equal
 * {@code opcode}. At most {@link #MAX_RAW_BYTES} bytes are kept; {@code rawLength} is the original packet
 * length and {@link #truncated()} tells whether the tail was dropped. Parser-marked secret fields
 * ({@link ReportFields#secret}) are zeroed in {@code raw} by {@link #redacted}, so the GUI and the JSONL
 * file only ever see the masked bytes.
 * <p>
 * A report can never be built for a {@link SensitiveClientOpcodes sensitive opcode} (credentials or
 * secondary password), so no sink or reader can ever persist one.
 */
public record Tms282ClientReport(Instant time, Source source, int opcode, String name,
                                 List<Field> fields, byte[] raw, int rawLength) {
    /** Most raw bytes a report keeps (and the JSONL file stores); longer packets are truncated. */
    public static final int MAX_RAW_BYTES = 64 * 1024;
    private static final int SUMMARY_MAX_CHARS = 160;

    /** Where the report came from. {@code account} is empty before the session has authenticated. */
    public record Source(String remoteAddress, String account) {
        public static final Source UNKNOWN = new Source("", "");

        public Source {
            remoteAddress = remoteAddress == null ? "" : remoteAddress;
            account = account == null ? "" : account;
        }

        public boolean hasAccount() {
            return !account.isEmpty();
        }

        /** "address" or "address (account)", for one-line display. */
        public String display() {
            return hasAccount() ? remoteAddress + " (" + account + ")" : remoteAddress;
        }
    }

    /** One decoded field in wire order. {@code value} is already rendered for display. */
    public record Field(String name, String value) {
        public Field {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(value, "value");
        }
    }

    /**
     * A secret byte range in packet coordinates (opcode = offset 0). The opcode itself can never be secret.
     */
    public record Redaction(int offset, int length) {
        public Redaction {
            if (offset < 2 || length < 0) {
                throw new IllegalArgumentException("Secret range must start after the opcode and not be negative");
            }
        }

        /** Copy of {@code packet} with every range zeroed; ranges past the end are clipped. */
        static byte[] apply(byte[] packet, List<Redaction> secrets) {
            byte[] masked = packet.clone();
            for (Redaction secret : secrets) {
                int from = Math.min(secret.offset(), masked.length);
                int to = (int) Math.min((long) secret.offset() + secret.length(), masked.length);
                Arrays.fill(masked, from, to, (byte) 0);
            }
            return masked;
        }
    }

    public Tms282ClientReport {
        Objects.requireNonNull(time, "time");
        source = source == null ? Source.UNKNOWN : source;
        Objects.requireNonNull(name, "name");
        if (opcode < 0 || opcode > 0xFFFF) {
            throw new IllegalArgumentException("Opcode outside u16 range");
        }
        if (SensitiveClientOpcodes.contains(opcode)) {
            throw new IllegalArgumentException(String.format("CP%04X carries credentials and is never reported", opcode));
        }
        fields = List.copyOf(fields);
        Objects.requireNonNull(raw, "raw");
        if (raw.length < 2 || (Byte.toUnsignedInt(raw[0]) | Byte.toUnsignedInt(raw[1]) << 8) != opcode) {
            throw new IllegalArgumentException(String.format("Raw bytes do not start with opcode %04X", opcode));
        }
        raw = raw.length > MAX_RAW_BYTES ? Arrays.copyOf(raw, MAX_RAW_BYTES) : raw.clone();
        if (rawLength < raw.length || rawLength > raw.length && raw.length != MAX_RAW_BYTES) {
            throw new IllegalArgumentException("Raw length " + rawLength + " does not match " + raw.length + " kept bytes");
        }
    }

    /** Report of a complete packet: {@code rawLength} is {@code raw.length}; bytes past {@link #MAX_RAW_BYTES} are dropped. */
    public Tms282ClientReport(Instant time, Source source, int opcode, String name, List<Field> fields, byte[] raw) {
        this(time, source, opcode, name, fields, raw, Objects.requireNonNull(raw, "raw").length);
    }

    /** Report whose raw bytes have every {@code secrets} range zeroed before they are kept. */
    public static Tms282ClientReport redacted(Instant time, Source source, int opcode, String name, List<Field> fields,
                                              byte[] packet, List<Redaction> secrets) {
        Objects.requireNonNull(packet, "packet");
        return new Tms282ClientReport(time, source, opcode, name, fields,
                Redaction.apply(packet, List.copyOf(secrets)), packet.length);
    }

    /** True when the packet was longer than {@link #MAX_RAW_BYTES} and only its head is kept. */
    public boolean truncated() {
        return rawLength > raw.length;
    }

    @Override
    public byte[] raw() {
        return raw.clone();
    }

    public String opcodeHex() {
        return String.format("%04X", opcode);
    }

    public String rawHex() {
        return HexFormat.of().formatHex(raw);
    }

    /** First fields as "name=value", truncated for a table cell. */
    public String summary() {
        String text = fields.stream().map(f -> f.name() + "=" + f.value()).collect(Collectors.joining("  "));
        return text.length() <= SUMMARY_MAX_CHARS ? text : text.substring(0, SUMMARY_MAX_CHARS - 1) + "…";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tms282ClientReport other && time.equals(other.time) && source.equals(other.source)
                && opcode == other.opcode && name.equals(other.name) && fields.equals(other.fields)
                && Arrays.equals(raw, other.raw) && rawLength == other.rawLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, source, opcode, name, fields, rawLength) * 31 + Arrays.hashCode(raw);
    }

    @Override
    public String toString() {
        return "Tms282ClientReport[" + time + " " + source.display() + " CP" + opcodeHex() + " " + name + " " + summary() + "]";
    }
}
