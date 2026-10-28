package net.swordie.ms.connection.packet.tms282report;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Report layouts of the in-field TMS 282.3 packets that the client sends on timers or once at field entry and never
 * waits for (packet-audit/unknown-periodic-cp-2823.md; senders in infield-capture-1790158495223.md §2.5-2.12,
 * samples from sessions/formal-channel-1790176449461-3 and channel-1790158495223-2). One {@link ClientReportParser}
 * per opcode, registered into {@link ClientReportParsers#standard}; every list count is bounded by the bytes left,
 * so a bad count fails the parser (hex fallback) instead of allocating.
 * <ul>
 *     <li>CP021F 5E80820, every 10 s: {@code u32 n1, n1 x (u32 skillId, u32 hash), u32 n2, n2 x (u32 skillId,
 *     u32 count, u32 hash)} - skill-data hash poll, 10 entries per batch;</li>
 *     <li>CP02B0 VM F947C0: {@code u32 subtype} + subtype body; captured 1 (u32 n, n x (id, count, hash)),
 *     2 (u32 n, n x (id, hash)), 3 (u32 n, n x (u32 id, u32 k, k x u32)), 0x2B (4 x u32), 0x33 (u32 vendorId,
 *     u32 productId, u32, u32, str device path: a HID device), 0x6D (empty); others as hex;</li>
 *     <li>CP0496 (8 senders, field entry): {@code u32 table, u8 n, n x (u32 key, u32 value)} - settings table;</li>
 *     <li>CP04B5 60031B0, every 30 s: {@code u8 first, u32 n1, n1 x (u32 key, u32 m, m x (u32 k, u32 v)), u32 n2,
 *     n2 x (u32 k, u32 v), u32 tail} (the tail is appended by 346A070);</li>
 *     <li>CP077D 336F020: {@code u32 reqId, u32 type, payload} - a request that waits for an ACK (LP opcode not
 *     found: the socket dispatcher is virtualized) and is resent with the same reqId every 60 s;</li>
 *     <li>CP0782 GameUser one-way RPC: {@code u32 interface, u16 method}; captured method0x52 is PartyParadise_UpdateCache;</li>
 *     <li>CP0788 VM, once at field entry: 18 bytes, fields unknown (kept as u32 + hex).</li>
 * </ul>
 */
public final class Tms282PeriodicReportParsers {
    /** Entries shown per list; the count is always shown. */
    static final int SHOWN = 4;

    private Tms282PeriodicReportParsers() {
    }

    /** Calls {@code register} once per opcode (CP021F, CP02B0, CP0496, CP04B5, CP077D, CP0782, CP0788). */
    public static void registerAll(BiConsumer<Integer, ClientReportParser> register) {
        Objects.requireNonNull(register, "register");
        // Native3FA9C22..3FA9D96: diagnostics after rectangle intersection, not another attack.
        register.accept(0x02B3, LayoutReportParser.named("命中範圍診斷 02B3").step((in, out) -> {
            out.add("技能", in.i32());
            out.add("怪物資料", in.i32());
            for (String rectangle : List.of("相交範圍", "範圍A", "範圍B")) {
                // Coordinates are signed; do not wrap negatives into huge unsigned positions.
                out.add(rectangle, "(" + in.i32() + "," + in.i32() + "," + in.i32() + "," + in.i32() + ")");
            }
        }).build());
        // Native5899E93: argument code, client clock, two flags. Business meaning remains unproven.
        register.accept(0x0306, LayoutReportParser.named("客戶端狀態回報 0306")
                .i32("代碼").i32("客戶端時間").u8("旗標1").u8("旗標2").build());
        register.accept(0x021F, LayoutReportParser.named("技能雜湊輪詢 021F（10 秒）").step((in, out) -> {
            list(in, out, "技能雜湊", 8, r -> r.i32() + ":" + hex32(r.i32()));
            list(in, out, "技能計數雜湊", 12, r -> r.i32() + "x" + r.i32() + ":" + hex32(r.i32()));
        }).build());
        register.accept(0x02B0, LayoutReportParser.named("多工回報 02B0").step(Tms282PeriodicReportParsers::multiReport).build());
        register.accept(0x0496, LayoutReportParser.named("設定表 0496（進圖）").step((in, out) -> {
            int table = in.i32();
            out.add("表", table);
            // Native29E68D0 emits only i32(3); it has no count byte or entries.
            if (table == 3) return;
            int n = in.u8();
            entries(in, out, "設定", n, 8, r -> r.i32() + "=" + r.i32());
        }).build());
        register.accept(0x04B5, LayoutReportParser.named("計數回報 04B5（30 秒）").step((in, out) -> {
            out.add("首次", in.u8());
            list(in, out, "分組計數", 8, r -> {
                int key = r.i32();
                List<String> inner = new ArrayList<>();
                int m = count(r, r.i32(), 8);
                for (int i = 0; i < m; i++) {
                    inner.add(r.i32() + "=" + r.i32());
                }
                return key + inner.toString();
            });
            list(in, out, "計數", 8, r -> r.i32() + "=" + r.i32());
            out.add("尾端", in.i32());
        }).build());
        register.accept(0x077D, LayoutReportParser.named("可靠請求 077D（等待 ACK）").i32("reqId").i32("類型")
                .rest("內容").build());
        register.accept(0x0782, LayoutReportParser.named("GameUser RPC 0782").step((in, out) -> {
            out.add("介面", hex32(in.i32()));
            out.add("方法", in.u16());
        }).build());
        register.accept(0x0788, LayoutReportParser.named("進圖回報 0788").step((in, out) -> {
            out.add("值", in.i32());
            out.hex("其餘", in.rest());
        }).build());
    }

    private static void multiReport(ReportReader in, ReportFields out) {
        int subtype = in.i32();
        out.add("子型別", subtype);
        switch (subtype) {
            case 1 -> list(in, out, "技能計數雜湊", 12, r -> r.i32() + "x" + r.i32() + ":" + hex32(r.i32()));
            case 2 -> list(in, out, "技能雜湊", 8, r -> r.i32() + ":" + hex32(r.i32()));
            case 3 -> list(in, out, "雜湊組", 8, r -> {
                int id = r.i32();
                int k = count(r, r.i32(), 4);
                List<String> hashes = new ArrayList<>();
                for (int i = 0; i < k; i++) {
                    hashes.add(hex32(r.i32()));
                }
                return id + hashes.toString();
            });
            case 0x2B -> {
                out.add("值1", in.i32());
                out.add("值2", in.i32());
                out.add("值3", in.i32());
                out.add("值4", in.i32());
            }
            case 0x33 -> {
                out.add("VID", String.format("%04X", in.i32()));
                out.add("PID", String.format("%04X", in.i32()));
                out.add("值1", in.i32());
                out.add("值2", in.i32());
                out.add("裝置路徑", in.str());
            }
            case 0x6D -> {
            }
            default -> out.hex("內容", in.rest());
        }
    }

    /** {@code u32 n} then n entries of at least {@code minSize} bytes, shown as a count plus the first entries. */
    private static void list(ReportReader in, ReportFields out, String name, int minSize, Function<ReportReader, String> entry) {
        entries(in, out, name, in.i32(), minSize, entry);
    }

    private static void entries(ReportReader in, ReportFields out, String name, int rawCount, int minSize,
                                Function<ReportReader, String> entry) {
        int n = count(in, rawCount, minSize);
        List<String> shown = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String e = entry.apply(in);
            if (i < SHOWN) {
                shown.add(e);
            }
        }
        out.add(name, n + " 筆" + (n == 0 ? "" : " " + String.join(" ", shown) + (n > SHOWN ? " …" : "")));
    }

    /** A list count that fits in the bytes left (each entry at least {@code minSize} bytes). */
    private static int count(ReportReader in, int raw, int minSize) {
        long n = Integer.toUnsignedLong(raw);
        if (n * minSize > in.remaining()) {
            throw new IllegalArgumentException("count " + n + " does not fit in " + in.remaining() + " bytes");
        }
        return (int) n;
    }

    private static String hex32(int v) {
        return String.format("%08x", v);
    }
}
