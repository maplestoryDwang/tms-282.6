package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.QuickMoveType;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * TMS 282.3/282.4 quick move (小地圖旁的「快速移動」, UI {@code UI/UIWindow2.img/EasyMove}), 2026-10-05.
 * <p>
 * List entry (decoder 967070, called from CField 37806B0 for LP0331 and LP0296):
 * {@code i32 code, i32 levelMin, str msg, i32 reqQuest, str script, str title, i32 npc, i32, str, str desc, str,
 * buf8 start FILETIME, buf8 end FILETIME}. The layout is the 265 one, but two things differ:
 * <ul>
 *     <li><b>code</b>: the UI list builder 4E548C0 keeps an entry only when {@code 0 <= code <= buttons-1}
 *     (1380510(0, [ui+0x32C]-1, code)) and the layout has a control with id {@code 2000 + code} (3C09680). The
 *     constructor 4E53120 counts {@code button:0..} of EasyMove: 282 has 13 ({@code button:0}..{@code button:12},
 *     WZ id 0..12). The 265 codes (QuickMoveType 100..110) are all out of range, so every entry was dropped and the
 *     window opened empty (2026-10-05 live report 「點擊快速移動，沒有東西」, session
 *     formal-channel-1791168552687-3133).</li>
 *     <li><b>strings</b>: 282 DecodeStr bytes are the client codepage (MS950, {@code tms282.nameCharset}); the 265
 *     body is UTF-8.</li>
 * </ul>
 * The button art (normal/0 canvases) fixes what each code means; the mapping below follows the labels drawn on them.
 * Click: 4E54FF8 runs the same check as the disabled state (967310: 3 outside start..end, 1 level, 2 quest) and, when
 * it passes, sends CP025C {@code i32 code} (4E55032..4E5505C) and closes the window.
 */
public final class Tms282QuickMove {
    public static final int SET_QUICK_MOVE_INFO = 0x0331;
    /** CWvsContext 5DFA4BA -> 4E52A00: u8 refresh, optional list (37806B0), then open the window. */
    public static final int USER_OPEN_QUICK_MOVE = 0x0296;
    /** EasyMove {@code button:0}..{@code button:12}. */
    public static final int BUTTONS = 13;

    private static final Map<QuickMoveType, Integer> TO_282;
    private static final QuickMoveType[] TO_265 = new QuickMoveType[BUTTONS];

    static {
        Map<QuickMoveType, Integer> m = new EnumMap<>(QuickMoveType.class);
        m.put(QuickMoveType.MonsterPark, 0);        // 怪物公園
        m.put(QuickMoveType.Ardentmill, 1);         // 梅斯特鎮
        m.put(QuickMoveType.LegionCoinShop, 2);     // 聯盟硬幣商店
        m.put(QuickMoveType.CrystalShop, 3);        // 結晶販售 & 靈藥購買
        m.put(QuickMoveType.AuctionHouse, 4);       // 楓之谷拍賣
        m.put(QuickMoveType.MesoMarket, 5);         // 楓幣商城
        m.put(QuickMoveType.StorageRoom, 6);        // 倉庫
        m.put(QuickMoveType.DimensionalPortal, 7);  // 次元之鏡
        m.put(QuickMoveType.SymbolExpressPass, 8);  // 符文快速通關
        m.put(QuickMoveType.MapleAdministrator, 11);// 楓之谷營運人員
        // 9 (button:8 art again), 10 自定義插圖 and 12 傭兵掃蕩系統 have no 265 content; CubeShop has no 282 button.
        TO_282 = Collections.unmodifiableMap(m);
        m.forEach((type, button) -> TO_265[button] = type);
    }

    private Tms282QuickMove() {
    }

    /** The 282 button of a 265 code, empty when 282 has no button for it. */
    public static OptionalInt to282(int code265) {
        QuickMoveType type = QuickMoveType.getValByNum(code265);
        Integer button = type == null ? null : TO_282.get(type);
        return button == null ? OptionalInt.empty() : OptionalInt.of(button);
    }

    /** The 265 type of a 282 button, empty when the button has no 265 content. */
    public static Optional<QuickMoveType> to265(int button) {
        return button < 0 || button >= BUTTONS ? Optional.empty() : Optional.ofNullable(TO_265[button]);
    }

    /** 265 {@code [u16 version, list]} (FieldPacket.setQuickMoveInfo) -> LP0331. */
    public static Optional<OutPacket> setQuickMoveInfo(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(SET_QUICK_MOVE_INFO);
        out.encodeShort(body.u16());
        list(body, out, charset);
        body.end();
        return Optional.of(out);
    }

    /** 265 {@code [u8 refresh, (u16 version, list) if refresh]} (WvsContext.userOpenQuickMove) -> LP0296. */
    public static Optional<OutPacket> openQuickMove(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(USER_OPEN_QUICK_MOVE);
        int refresh = body.u8();
        out.encodeByte(refresh);
        if (refresh != 0) {
            out.encodeShort(body.u16());
            list(body, out, charset);
        }
        body.end();
        return Optional.of(out);
    }

    /** u8 count + entries; entries without a 282 button are left out (the client would skip them anyway). */
    private static void list(Tms282LegacyPackets.Body body, OutPacket out, Charset charset) {
        int count = body.u8();
        List<Runnable> kept = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int code = body.i32();
            int levelMin = body.i32();
            String msg = body.str();
            int reqQuest = body.i32();
            String script = body.str();
            String title = body.str();
            int npc = body.i32();
            int unknown = body.i32();
            String s1 = body.str();
            String desc = body.str();
            String s2 = body.str();
            byte[] start = body.bytes(8);
            byte[] end = body.bytes(8);
            OptionalInt button = to282(code);
            if (button.isEmpty()) {
                continue;
            }
            kept.add(() -> {
                out.encodeInt(button.getAsInt());
                out.encodeInt(levelMin);
                Tms282LoginPacketSupport.str(out, msg, charset);
                out.encodeInt(reqQuest);
                Tms282LoginPacketSupport.str(out, script, charset);
                Tms282LoginPacketSupport.str(out, title, charset);
                out.encodeInt(npc);
                out.encodeInt(unknown);
                Tms282LoginPacketSupport.str(out, s1, charset);
                Tms282LoginPacketSupport.str(out, desc, charset);
                Tms282LoginPacketSupport.str(out, s2, charset);
                out.encodeArr(start);
                out.encodeArr(end);
            });
        }
        out.encodeByte(kept.size());
        kept.forEach(Runnable::run);
    }

    /** CP025C USER_QUICK_MOVE_SCRIPT {@code i32 button} (sender 4E55032: COutPacket(0x25C), Encode4 entry code). */
    public record Script(int button, QuickMoveType type) {
        public static final int OPCODE = 0x025C;

        public static Script parse(byte[] packet) {
            Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP025C");
            int button = in.i32();
            in.end();
            QuickMoveType type = to265(button).orElseThrow(
                    () -> new IllegalArgumentException("quick move button " + button + " has no 265 content"));
            return new Script(button, type);
        }

        /** UserHandler.handleUserQuickMoveScript: i32 QuickMoveType value. */
        public byte[] toGms265Body() {
            return new Gms265BodyWriter().i32(type.getVal()).bytes();
        }
    }
}
