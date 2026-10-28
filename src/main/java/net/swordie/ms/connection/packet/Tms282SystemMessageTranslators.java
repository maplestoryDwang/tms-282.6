package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Set;

/**
 * {@link Tms282LegacyPackets.Contributor} for the small 265 system packets the fail-closed guard dropped in the first
 * formal field session (formal-channel-1790176449461-3, 2026-09-23); each 282 reader was traced with the fixtures
 * of {@code tools/Tms282LegacyPacketsCheck} (research/tms2823/sysmsg-trace).
 * <ul>
 *     <li>CHAT_MSG -> LP04F0 {@code u16 type, str}: CUserLocal case 5788B4C reads Decode2, DecodeStr and calls the
 *     chat-log function 348CB20(str, type) (2285 callers). Opcode by the +56 shift between SET_PARTNER 0x4E7 and
 *     REWARD_MOB_LIST_RESULT 0x508 plus the read shape [推定]; layout traced CLEAN [證實]. The 265 ChatType value is
 *     passed through (282 colour table not checked).</li>
 *     <li>EXCL_REQUEST is not translated here: {@link Tms282UnlockScope} handles it request-scoped (2026-09-24). Original note: 265 flows send it next to replies that the 282 handlers already
 *     build with exclRequest set (Tms282ItemUseCheck / Tms282DropCheck see an extra LP009D otherwise), and the three
 *     live drops came from the entry script at field entry, when nothing was locked. Every 282 route unlocks itself.</li>
 *     <li>SET_IN_GAME_DIRECTION_MODE -> LP04E2 (5795B80, 證實): lock 0 {@code u8 0, u8} as 265; lock 1
 *     {@code u8 1, u8, u8, u8, u8, str}: 282 reads a fourth flag and a string after the three 265 flags, sent as
 *     0 and "" (layout 證實; the three flags kept in 265 order: 推定).</li>
 *     <li>MESSAGE -> LP00AE, only the types whose live 265 bodies the 282 reader 5ED27F0 consumed exactly: 1 quest
 *     record, 13 quest record ex, 14 world share record, 31 NX record, 44 achievement init with one i32 only (type numbers equal, read
 *     shape equal; meaning of 13/14/31/44 in 282: 推定). Other types are declined (own 282 codecs or unported).</li>
 *     <li>PROGRESS_MESSAGE_FONT (273) -> LP0112 and SCRIPT_PROGRESS_MESSAGE (263) -> LP0108 (2026-09-30, shop-overlap-0929
 *     TAB-DATE.md): CWvsContext::OnPacket 5DF5B20 jump table (op - 0x8F, 5DFB140) sends 0x112 to 5DF6E2F
 *     {@code i32 fontNameType, i32 fontSize, i32 fontColorType, i32 fadeOutDelay, u8, str} and 0x108 to 5DF697B
 *     {@code str} (both then draw on CWvsContext+0x32B0), the 265 bodies; only the string is re-encoded (265 UTF-8 ->
 *     282 charset). Before this the guard dropped them (「你獲得了 N 點武陵道場點數」, BossHelper kill counts...).
 *     {@code -Dtms282.progressMessage.enabled=false} leaves them to the guard.</li>
 * </ul>
 */
public final class Tms282SystemMessageTranslators implements Tms282LegacyPackets.Contributor {
    public static final int CHAT_MSG = 0x04F0;
    public static final int SET_IN_GAME_DIRECTION_MODE = 0x04E2;
    public static final int MESSAGE = 0x00AE;
    public static final int SCRIPT_PROGRESS_MESSAGE = 0x0108;
    public static final int PROGRESS_MESSAGE_FONT = 0x0112;
    /** Before dropfix-0930 (kept for {@code -Dtms282.message.dropfix.enabled=false}). */
    static final Set<Integer> MESSAGE_TYPES_OLD = Set.of(1, 13, 14, 31, 35, 39, 44); // 35 -> 282 type36 combo/multikill
    /**
     * dropfix-0930 (research/tms2823/dropfix-0930/message): every 265 type the server sends has the same number and body
     * in the 282.4 reader 5ED27F0 (jump table 5ED34D8), each traced CLEAN: 0 drop pick-up 5EEE7E0, 4 SP 5EF8B70,
     * 5/7/9 i32, 6 meso 5EF91B0 (i64, i32), 8 5EF9C60, 11 system text (chat log 348CB20), 44 achievement entries
     * 5F7FA70 (the len-37..52 drops, ~1000 per session), 45 achievement complete 5F7FD20.
     */
    public static final Set<Integer> MESSAGE_TYPES = messageDropfixEnabled()
            ? Set.of(0, 1, 4, 5, 6, 7, 8, 9, 11, 13, 14, 31, 35, 39, 44, 45) : MESSAGE_TYPES_OLD;

    static boolean messageDropfixEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.message.dropfix.enabled", "true"))
                && !"false".equalsIgnoreCase(System.getProperty("tms282.dropfix.enabled", "true"));
    }

    public static final Tms282SystemMessageTranslators INSTANCE = new Tms282SystemMessageTranslators();

    private Tms282SystemMessageTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset charset = builder.charset();
        builder.on(OutHeader.CHAT_MSG, p -> chatMsg(p, charset))
                .on(OutHeader.SESSION_VALUE, p -> sessionValue(p, charset))
                .on(OutHeader.SERVER_ON_CALC_REQUEST_RESULT, p -> fixedBody(p, 0x0B48, 1))
                .on(OutHeader.KILL_DAMAGE_INFO, p -> fixedBody(p, 0x0B46, 16))
                .on(OutHeader.ENEMINES_DEFEATED_UPDATE, p -> fixedBody(p, 0x0B4B, 0))
                .on(OutHeader.SKILL_DAMAGE_LOG, p -> fixedBody(p, 0x0B4A, 4))
                .on(OutHeader.SET_IN_GAME_DIRECTION_MODE, p -> inGameDirectionMode(p, charset))
                .on(OutHeader.MESSAGE, p -> message(p, charset));
        if (progressMessageEnabled()) {
            builder.on(OutHeader.PROGRESS_MESSAGE_FONT, p -> progressMessageFont(p, charset))
                    .on(OutHeader.SCRIPT_PROGRESS_MESSAGE, p -> scriptProgressMessage(p, charset));
        }
    }

    static boolean progressMessageEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.progressMessage.enabled", "true"));
    }

    /** 265 WvsContext.progressMessageFont -> LP0112 (5DF6E2F): four i32, u8, str; the string re-encoded. */
    static Optional<OutPacket> progressMessageFont(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int fontNameType = body.i32();
        int fontSize = body.i32();
        int fontColorType = body.i32();
        int fadeOutDelay = body.i32();
        int flag = body.u8();
        String msg = body.str();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(PROGRESS_MESSAGE_FONT);
        out.encodeInt(fontNameType);
        out.encodeInt(fontSize);
        out.encodeInt(fontColorType);
        out.encodeInt(fadeOutDelay);
        out.encodeByte(flag);
        Tms282LoginPacketSupport.str(out, msg, charset);
        return Optional.of(out);
    }

    /** 265 WvsContext.scriptProgressMessage -> LP0108 (5DF697B): str, re-encoded. */
    static Optional<OutPacket> scriptProgressMessage(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        String msg = body.str();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(SCRIPT_PROGRESS_MESSAGE);
        Tms282LoginPacketSupport.str(out, msg, charset);
        return Optional.of(out);
    }

    /** BattleRecordMan::OnPacket 1FD9600 dispatches B44..B4B; exact widths only. */
    private static Optional<OutPacket> fixedBody(OutPacket legacy, int opcode, int width) {
        byte[] bytes = Tms282LegacyPackets.Body.of(legacy).rest();
        if (bytes.length != width) return Optional.empty();
        OutPacket out = Tms282LoginPacketSupport.packet(opcode);
        out.encodeArr(bytes);
        return Optional.of(out);
    }

    private static Optional<OutPacket> sessionValue(OutPacket legacy, Charset charset) {
        var body = Tms282LegacyPackets.Body.of(legacy);
        String key=body.str(), value=body.str();
        body.end();
        if (!key.equals("kill_count")) return Optional.empty(); // Only this captured session key has been verified.
        OutPacket out=Tms282LoginPacketSupport.packet(0x0100);
        Tms282LoginPacketSupport.str(out,key,charset);
        Tms282LoginPacketSupport.str(out,value,charset);
        return Optional.of(out);
    }

    static Optional<OutPacket> chatMsg(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.u16();
        String msg = body.str();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(CHAT_MSG);
        out.encodeShort(type);
        Tms282LoginPacketSupport.str(out, msg, charset);
        return Optional.of(out);
    }

    static Optional<OutPacket> inGameDirectionMode(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int lock = body.u8();
        int blackFrame = body.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(SET_IN_GAME_DIRECTION_MODE);
        out.encodeByte(lock);
        out.encodeByte(blackFrame);
        if (lock != 0) {
            out.encodeByte(body.u8());
            out.encodeByte(body.u8());
            out.encodeByte(0);
            Tms282LoginPacketSupport.str(out, "", charset);
        }
        body.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> message(OutPacket legacy, Charset charset) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int type = body.u8();
        if (!MESSAGE_TYPES.contains(type)) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(MESSAGE);
        out.encodeByte(type == 35 ? 36 : type);
        switch (type) {
            case 35 -> { // 5EFA120: subtype 1 = combo (4*i32), subtype 0 = multikill (i64,3*i32).
                int subtype=body.u8();
                if (subtype != 0 && subtype != 1) return Optional.empty();
                out.encodeByte(subtype);
                if (subtype==0) out.encodeLong(body.i64());
                for (int i=0; i<(subtype==0 ? 3 : 4); i++) out.encodeInt((long)body.i32());
            }
            case 1 -> {
                out.encodeInt((long) body.i32()); // encodeInt(int) drops -88888
                int state = body.u8();
                out.encodeByte(state);
                if (state == 1) {
                    Tms282LoginPacketSupport.str(out, body.str(), charset);
                } else {
                    out.encodeArr(body.rest());
                }
            }
            case 13, 14, 31, 39 -> { // 39 COLLECTION_RECORD (monster collection): 5ED32A1 i32, 5ED32AF str, traced CLEAN
                out.encodeInt((long) body.i32()); // encodeInt(int) drops -88888
                Tms282LoginPacketSupport.str(out, body.str(), charset);
            }
            case 0 -> { // DROP_PICKUP 5EEE7E0: i32, u8, u8 kind; 0 item {i32 id, i32 qty, u8}, 1 meso {u8, i32, u16, i32}, 0xFF none
                if (Tms282UnlockScope.inRoute()) {
                    // 282 routes build their own pick-up line (Tms282CombatHandlers.pickUp -> LP00AE type 0); the
                    // 265 copy from Char.addDrop would show it twice. Script threads / timers are not in a route.
                    return Optional.empty();
                }
                out.encodeInt((long) body.i32());
                out.encodeByte(body.u8());
                int kind = body.u8();
                out.encodeByte(kind);
                if (kind == 0) {
                    out.encodeInt((long) body.i32());
                    out.encodeInt((long) body.i32());
                    out.encodeByte(body.u8());
                } else if (kind == 1) {
                    out.encodeByte(body.u8());
                    out.encodeInt((long) body.i32());
                    out.encodeShort(body.u16());
                    out.encodeInt((long) body.i32());
                } else if (kind != 0xFF) {
                    return Optional.empty();
                }
            }
            case 4 -> { // INC_SP 5EF8B70: u16 job, u8 amount
                out.encodeShort(body.u16());
                out.encodeByte(body.u8());
            }
            case 5, 7, 9 -> out.encodeInt((long) body.i32()); // INC_POP, INC_GP, GIVE_BUFF
            case 6 -> { // INC_MONEY 5EF91B0: i64, i32 (StringPool 0x537F %lld); type 24 adds an untraced str
                out.encodeLong(body.i64());
                int t = body.i32();
                if (t == 24) {
                    return Optional.empty();
                }
                out.encodeInt((long) t);
            }
            case 8 -> { // INC_COMMITMENT 5EF9C60: 3 x i32
                for (int i = 0; i < 3; i++) {
                    out.encodeInt((long) body.i32());
                }
            }
            case 11 -> Tms282LoginPacketSupport.str(out, body.str(), charset); // SYSTEM_MESSAGE -> chat log
            case 44 -> { // ACHIEVEMENT 5F7FA70 -> 8F89A0: i32 n, n x {i32, u8, u8, FT, i32 type, type 42 ? str : FT}
                int n = body.i32();
                if (n < 0 || n > 4096) {
                    return Optional.empty();
                }
                out.encodeInt((long) n);
                for (int i = 0; i < n; i++) {
                    out.encodeInt((long) body.i32());
                    out.encodeByte(body.u8());
                    out.encodeByte(body.u8());
                    out.encodeLong(body.i64());
                    int t = body.i32();
                    out.encodeInt((long) t);
                    if (t == 42) {
                        Tms282LoginPacketSupport.str(out, body.str(), charset);
                    } else {
                        out.encodeLong(body.i64());
                    }
                }
            }
            case 45 -> { // ACHIEVEMENT_DATA 5F7FD20: i32 n, n x i32
                int n = body.i32();
                if (n < 0 || n > 4096) {
                    return Optional.empty();
                }
                out.encodeInt((long) n);
                for (int i = 0; i < n; i++) {
                    out.encodeInt((long) body.i32());
                }
            }
            default -> {
                return Optional.empty();
            }
        }
        body.end();
        return Optional.of(out);
    }
}
