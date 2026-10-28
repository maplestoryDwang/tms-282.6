package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * {@link Tms282LegacyPackets.Contributor} for the 265 whisper / group chat packets (FieldPacket.whisper,
 * FieldPacket.groupMessage), traced with the fixtures of {@code tools/Tms282SocialCheck} (research/tms2823/social-trace).
 * <ul>
 *     <li>WHISPER (265 0x02E3) -> LP0305, CField handler 3756A00 (Decode1 type, jump table 3758DD8 for type-9):
 *     type 18 (a received whisper, 3756A7F): 265 {@code i32 0, Str from, i32 id, u8 channel, u8, i32 world, Str msg,
 *     chat info, i32 item, [item], u8} -> 282 reads the chat info with the 282 tail byte (1345CF0, as LP03D5), Buf4
 *     item count (134A856) and the 265 last u8 as the std::set count (56AFE6); an item-linked whisper is declined;
 *     type 9 (target not found / /find, 3757DC6): Str, u8, i32 as 265.</li>
 *     <li>GROUP_MESSAGE (265 0x02E1) -> LP0302, CField handler 3754810: 265 {@code u8 type, i32 account, i32 id,
 *     Str name, Str msg, chat info}; 282 reads an i32 between the message and the chat info (sent as 0), the chat info
 *     tail byte and a std::set count u8 (56AFE6, sent 0). The client drops buddy (0) / guild (2) / alliance (3) lines
 *     when its chat options +0x170 / +0x174 / +0x178 are off, and shows a line only when the chat filter object
 *     9BB29D0 exists (the trace sets both). An item-linked group message is declined.</li>
 * </ul>
 * Opcodes: 265 TRANSFER_PVP_REQ_IGNORED 0x2DF = 282 0x300 (證實); 282 0x302 and 0x305 are the only CField handlers
 * of 0x301..0x306 whose reads start with the 265 GROUP_MESSAGE / WHISPER bodies (ingame-lp-map-2823 read sequences),
 * so GROUP_MESSAGE 0x2E1 -> 0x302 and WHISPER 0x2E3 -> 0x305 [推定 opcode, layout traced CLEAN].
 */
public final class Tms282ChatGroupTranslators implements Tms282LegacyPackets.Contributor {
    public static final int GROUP_MESSAGE = 0x0302;
    public static final int WHISPER = 0x0305;

    public static final Tms282ChatGroupTranslators INSTANCE = new Tms282ChatGroupTranslators();

    private Tms282ChatGroupTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        Charset charset = builder.charset();
        builder.on(OutHeader.WHISPER, p -> whisper(p, charset))
                .on(OutHeader.GROUP_MESSAGE, p -> groupMessage(p, charset));
    }

    static Optional<OutPacket> whisper(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        int type = b.u8();
        OutPacket out = Tms282LoginPacketSupport.packet(WHISPER);
        out.encodeByte(type);
        switch (type) {
            case 18 -> {
                out.encodeInt(b.i32());
                str(out, b.str(), cs);
                out.encodeInt(b.i32());
                out.encodeByte(b.u8());
                out.encodeByte(b.u8());
                out.encodeInt(b.i32());
                str(out, b.str(), cs);
                chatInfo(b, out, cs);
                int item = b.i32();
                if (item != 0) {
                    return Optional.empty(); // item-linked whisper: 282 item block not traced
                }
                out.encodeInt(item);
                out.encodeByte(b.u8());
            }
            case 9 -> {
                str(out, b.str(), cs);
                out.encodeByte(b.u8());
                out.encodeInt(b.i32());
            }
            default -> {
                return Optional.empty();
            }
        }
        b.end();
        return Optional.of(out);
    }

    static Optional<OutPacket> groupMessage(OutPacket legacy, Charset cs) {
        Tms282LegacyPackets.Body b = Tms282LegacyPackets.Body.of(legacy);
        OutPacket out = Tms282LoginPacketSupport.packet(GROUP_MESSAGE);
        out.encodeByte(b.u8());
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        str(out, b.str(), cs);
        str(out, b.str(), cs);
        out.encodeInt(0);
        chatInfo(b, out, cs);
        if (b.remaining() != 0) {
            return Optional.empty(); // item-linked group message: 282 item block not traced
        }
        out.encodeByte(0); // u8 n + n x i32 (std::set, 56AFE6), as LP03D5
        return Optional.of(out);
    }

    /**
     * 265 Char.encodeChatInfo (Str name, Str msg, i32 account, i32 id, u8 world, i32 id, i32 0, Str "", i32 0, Str "",
     * i32 0) -> 282 1345CF0: the same fields and a trailing u8 0 (Tms282ChatPackets).
     */
    static void chatInfo(Tms282LegacyPackets.Body b, OutPacket out, Charset cs) {
        str(out, b.str(), cs);
        str(out, b.str(), cs);
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        out.encodeByte(b.u8());
        out.encodeInt(b.i32());
        out.encodeInt(b.i32());
        str(out, b.str(), cs);
        out.encodeInt(b.i32());
        str(out, b.str(), cs);
        out.encodeInt(b.i32());
        out.encodeByte(0);
    }

    private static void str(OutPacket out, String s, Charset cs) {
        Tms282LoginPacketSupport.str(out, s, cs);
    }
}
