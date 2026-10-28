package net.swordie.ms.connection.packet;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * In-field client reports of TMS 282.3 that need no reply: the sender builds the packet and sends it without taking
 * the exclusive-request lock (no 5E04DA0(ctx, 1) after 346A070), so the client never waits for an answer. Each
 * constant names the static sender and the body layout; {@link #describe} decodes one packet strictly (wrong opcode,
 * short body or trailing bytes are {@link IllegalArgumentException}s) for the server log / GUI trace.
 * Evidence: packet-audit/unknown-field-report-cp-2823.md (senders disassembled 2026-09-23, samples from
 * formal-channel-1790176449461-3 and channel-1790158495223-2).
 */
public enum Tms282FieldReport {
    /** 579CF70 (vtable 87CDC78/87D6B48) and 58644D0: empty notice, 265 USER_UNK_REQUEST (0x107, shift +2). */
    CP0109(0x0109, "notice (empty body; 265 USER_UNK_REQUEST)"),
    // CP0120 (5EE28D0) is the title activation (i32 item, i32 slot), routed by Tms282TitleHandlers since 2026-09-28.
    /** 5906420 from CUserLocal update (5774E00), every 600000 ms: 4-byte buffer (own character id), own name. */
    CP024E(0x024E, "10-minute self report", Field.u32("characterId"), Field.str("name")),
    /** 5DC7F00 (field init), sent back to back with CP035E: empty body. */
    CP0326(0x0326, "field-init notice A (empty body)"),
    /** 5DC7F00 (field init), right after CP0326: empty body. */
    CP035E(0x035E, "field-init notice B (empty body)"),
    /** 5E72390 when the value of 54315B0 changes: u32 value (3AC9C50), u32 n, n x u32. */
    CP0427(0x0427, "value-changed report", Field.u32("value"), Field.u32List("entries")),
    /** 593AE70 (vtable 87CDD78/87D6C48), throttled 3000 ms: u8 flag (1 then 0 observed). */
    CP047C(0x047C, "flag report (3 s throttle)", Field.u8("flag")),
    /** 199B470 (field init, 5DC7F00) and 199B6E0 (UI): u32 value (DE5740). */
    CP04DB(0x04DB, "field-init value report", Field.u32("value")),
    /** 5E84480 (context init 57647F0): u8 1 when the stored "date" record is past 250402125959000, else 0. */
    CP07B1(0x07B1, "event date flag", Field.u8("expired")),
    /** Window focus toggle (infield-capture-1790158495223.md): u8 1/0. */
    CP00D9(0x00D9, "focus toggle", Field.u8("active"));

    private final int opcode;
    private final String meaning;
    private final List<Field> fields;

    Tms282FieldReport(int opcode, String meaning, Field... fields) {
        this.opcode = opcode;
        this.meaning = meaning;
        this.fields = List.of(fields);
    }

    public int opcode() {
        return opcode;
    }

    public String meaning() {
        return meaning;
    }

    /** One log line: opcode, meaning and every decoded field. */
    public String describe(byte[] packet, Charset charset) {
        Objects.requireNonNull(charset, "charset");
        Tms282FieldRequestReader r = Tms282FieldRequestReader.open(packet, opcode, name());
        List<String> parts = new ArrayList<>(fields.size());
        for (Field f : fields) {
            parts.add(f.name + "=" + f.kind.read(r, charset));
        }
        r.end();
        return String.format("CP%04X %s%s", opcode, meaning, parts.isEmpty() ? "" : " " + String.join(" ", parts));
    }

    /** The report for {@code opcode}, or null. */
    public static Tms282FieldReport of(int opcode) {
        for (Tms282FieldReport r : values()) {
            if (r.opcode == opcode) {
                return r;
            }
        }
        return null;
    }

    private record Field(String name, Kind kind) {
        static Field u8(String name) {
            return new Field(name, Kind.U8);
        }

        static Field u32(String name) {
            return new Field(name, Kind.U32);
        }

        static Field str(String name) {
            return new Field(name, Kind.STR);
        }

        static Field u32List(String name) {
            return new Field(name, Kind.U32_LIST);
        }
    }

    private enum Kind {
        U8 {
            @Override
            String read(Tms282FieldRequestReader r, Charset charset) {
                return Integer.toString(r.u8());
            }
        },
        U32 {
            @Override
            String read(Tms282FieldRequestReader r, Charset charset) {
                return Integer.toUnsignedString(r.i32());
            }
        },
        STR {
            @Override
            String read(Tms282FieldRequestReader r, Charset charset) {
                return "\"" + r.str(charset) + "\"";
            }
        },
        /** u32 count, then that many u32; the count is bounded by the bytes left. */
        U32_LIST {
            @Override
            String read(Tms282FieldRequestReader r, Charset charset) {
                long n = Integer.toUnsignedLong(r.i32());
                if (n * 4 > r.remaining()) {
                    throw new IllegalArgumentException("list of " + n + " u32 does not fit in " + r.remaining() + " bytes");
                }
                List<String> v = new ArrayList<>();
                for (long i = 0; i < n; i++) {
                    v.add(Integer.toUnsignedString(r.i32()));
                }
                return v.toString();
            }
        };

        abstract String read(Tms282FieldRequestReader r, Charset charset);
    }

    /** Hex of a packet for rejected-report log lines. */
    public static String hex(byte[] packet) {
        return HexFormat.of().formatHex(packet, 0, Math.min(packet.length, 48));
    }
}
