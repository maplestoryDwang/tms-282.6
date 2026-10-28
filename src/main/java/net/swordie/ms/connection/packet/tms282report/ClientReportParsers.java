package net.swordie.ms.connection.packet.tms282report;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable registry: opcode → {@link ClientReportParser} (one Strategy per layout), plus the one entry
 * point that turns a plaintext packet into a {@link Tms282ClientReport}. Unregistered opcodes and parsers
 * that throw fall back to {@link HexReportParser}; {@link #parse} never throws for a well-formed argument,
 * so a malformed report cannot end the client connection. {@link SensitiveClientOpcodes} can never be
 * registered.
 * <p>
 * Layout sources: packet-audit/login-cp-layouts-2823.md (static send sites) and the captures in
 * research/tms2823/sessions (observed). "Captured" parsers are checked by tools/Tms282ClientReportCheck.
 */
public final class ClientReportParsers {
    public static final String TRAILING_FIELD = "尾端未解析";
    private final Map<Integer, ClientReportParser> parsers;
    private final Charset charset;

    private ClientReportParsers(Map<Integer, ClientReportParser> parsers, Charset charset) {
        this.parsers = Map.copyOf(parsers);
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    /** All known report layouts. {@code charset} decodes DecodeStr fields (the probe's NAME_CHARSET). */
    public static ClientReportParsers standard(Charset charset) {
        Map<Integer, ClientReportParser> p = new HashMap<>();
        // Captured (layout observed in sessions/*/recv-*-<op>.bin).
        register(p, 0x0070, LayoutReportParser.named("客戶端啟動").u8("類型").i32("版本").rest("其餘").build());
        register(p, 0x0072, LayoutReportParser.named("版本字串").step((in, out) -> {
            int flag = in.u8();
            out.add("旗標", flag);
            if (flag != 0) {
                out.add("種類", in.u8());
                out.add("內容", in.str());
            }
        }).build());
        register(p, 0x007B, LayoutReportParser.named("載入時間").u8("旗標1").u8("旗標2")
                .i32("時間1").i32("時間2").i32("時間3").i32("時間4").i32("時間5").build());
        register(p, 0x0095, new ELogReportParser("ELog 文字"));
        register(p, 0x0096, new ELogReportParser("ELog 文字（完整）"));
        register(p, 0x0098, LayoutReportParser.named("整數回報 0098").i32("值").build());
        register(p, 0x00A3, LayoutReportParser.named("機器 ID").str("機器ID").i32("值1").u8("值2").u8("值3")
                .rotatedStr("硬體雜湊", 2).build());
        register(p, 0x00A4, LayoutReportParser.named("混淆資料").sizedBlob("資料").build());
        register(p, 0x00A5, new MachineInfoReportParser());
        register(p, 0x00AE, new EmbeddedStringReportParser("斷線前回報"));
        register(p, 0x00B5, LayoutReportParser.named("帳號名稱（登入前）").str("帳號").build());
        register(p, 0x00C1, LayoutReportParser.named("啟動回報 00C1").i32("類型").rest("附加").build());
        register(p, 0x00DD, LayoutReportParser.named("回報 00DD").u8("值").build());
        // Login: a single int (e.g. 0x6D); in field (channel-1790158495223): subtype 1/2 followed by a list.
        // CP02B0 by subtype and the periodic in-field reports (CP021F 0496 04B5 077D 0782 0788): unknown-periodic-cp-2823.md.
        Tms282PeriodicReportParsers.registerAll((op, parser) -> register(p, op, parser));
        register(p, 0x0307, new EmbeddedStringReportParser("硬體資訊（GPU）"));
        register(p, 0x0309, new EmbeddedStringReportParser("硬體資訊（CPU／GPU）"));
        register(p, 0x0374, LayoutReportParser.named("回報 0374").i32("值1").i32("值2").build());
        // Static layouts only (login-cp-layouts-2823.md); not captured yet.
        // CP0073, CP00A2 and CP00DC are sensitive (SensitiveClientOpcodes) and deliberately have no parser.
        register(p, 0x009C, LayoutReportParser.named("安全確認 4").i32("類型").u8("結果").build());
        register(p, 0x00A1, LayoutReportParser.named("安全回答 2B").i32("類型").u8("+4C4").u8("+4D8").i32("值").build());
        register(p, 0x00BB, LayoutReportParser.named("同意旗標").u8("同意").build());
        register(p, 0x00C5, LayoutReportParser.named("角色選擇回報").u8("a2").i32("ctx+23AC").i32("a4")
                .u8("a6").i32("a5").i32("固定 -1").build());
        register(p, 0x00DA, LayoutReportParser.named("安全回報 00DA").i32("類型").rest("附加").build());
        register(p, 0x00DB, LayoutReportParser.named("對話框結果 00DB").i32("類型").build());
        register(p, 0x00E9, LayoutReportParser.named("對話框結果 00E9").u8("是").build());
        register(p, 0x0617, LayoutReportParser.named("遊戲內回報 0617").u8("值").build());
        register(p, 0x07C6, LayoutReportParser.named("登入失敗回報").u8("值1").i32("值2").i32("值3").build());
        // Static layouts from unknown-misc-cp-2823.md / unknown-lp-2823.md; not captured yet.
        register(p, 0x0094, new ELogReportParser("錯誤記錄 msmig.ini", ELogReportParser.LengthPrefix.U16));
        register(p, 0x0097, new ELogReportParser("錯誤記錄 msexcr.ini"));
        register(p, 0x00CC, LayoutReportParser.named("登入測謊回答").step((in, out) -> {
            int kind = in.u8(); // 0 answer (2C8F6E0/24D737C), 1 new picture (2C8F7BE), 2 give up (2C8F9FD/24D741A)
            out.add("種類", kind);
            if (kind == 0) {
                out.add("答案", in.str());
            }
        }).build());
        LayoutReportParser empty = LayoutReportParser.named("UI 確認（無內容）").build();
        for (int op : new int[]{0x0081, 0x0099, 0x009F, 0x00A0, 0x00D0}) {
            register(p, op, empty);
        }
        return new ClientReportParsers(p, charset);
    }

    private static void register(Map<Integer, ClientReportParser> parsers, int opcode, ClientReportParser parser) {
        if (SensitiveClientOpcodes.contains(opcode)) {
            throw new IllegalStateException(String.format("CP%04X is sensitive and cannot have a report parser", opcode));
        }
        if (parsers.putIfAbsent(opcode, parser) != null) {
            throw new IllegalStateException(String.format("Duplicate report parser for %04X", opcode));
        }
    }

    /**
     * New registry with one more parser (same checks as the standard ones: no sensitive opcode, no duplicate).
     * This registry is unchanged.
     */
    public ClientReportParsers with(int opcode, ClientReportParser parser) {
        Map<Integer, ClientReportParser> p = new HashMap<>(parsers);
        register(p, opcode, Objects.requireNonNull(parser, "parser"));
        return new ClientReportParsers(p, charset);
    }

    /** Registered parser, or {@link HexReportParser#INSTANCE}. */
    public ClientReportParser forOpcode(int opcode) {
        return parsers.getOrDefault(opcode, HexReportParser.INSTANCE);
    }

    public boolean isRegistered(int opcode) {
        return parsers.containsKey(opcode);
    }

    /**
     * Strategy dispatch: looks up the opcode's {@link ClientReportParser} and runs it; bytes it leaves unread
     * become a {@link #TRAILING_FIELD}. If the parser throws, {@link HexReportParser} (the fallback strategy)
     * reports the body instead, keeping the secret ranges the failed parser had already marked. Hex fields
     * and the stored raw bytes are taken from the masked packet, so a secret never reaches them.
     * Empty for packets shorter than an opcode and for {@link SensitiveClientOpcodes}.
     */
    public Optional<Tms282ClientReport> parse(byte[] packet, Tms282ClientReport.Source source, Instant time) {
        if (packet == null || packet.length < 2) {
            return Optional.empty();
        }
        int opcode = Byte.toUnsignedInt(packet[0]) | Byte.toUnsignedInt(packet[1]) << 8;
        if (SensitiveClientOpcodes.contains(opcode)) {
            return Optional.empty();
        }
        ClientReportParser parser = forOpcode(opcode);
        ReportFields fields = new ReportFields();
        ReportReader in = new ReportReader(packet, 2, charset);
        try {
            parser.parse(in, fields);
            if (in.remaining() > 0) {
                int from = in.position();
                fields.hex(TRAILING_FIELD, Arrays.copyOfRange(masked(packet, fields), from, packet.length));
            }
        } catch (RuntimeException e) {
            ReportFields partial = fields;
            fields = partial.withoutFields();
            HexReportParser.failed(parser, e).parse(new ReportReader(masked(packet, partial), 2, charset), fields);
        }
        return Optional.of(Tms282ClientReport.redacted(time, source, opcode, parser.name(), fields.build(), packet,
                fields.secrets()));
    }

    private static byte[] masked(byte[] packet, ReportFields fields) {
        return Tms282ClientReport.Redaction.apply(packet, fields.secrets());
    }
}
