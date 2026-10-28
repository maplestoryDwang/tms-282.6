package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

import java.nio.charset.Charset;
import java.util.Objects;

import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.checkByte;
import static net.swordie.ms.connection.packet.Tms282LoginPacketSupport.str;

/**
 * TMS 282.3 user chat. LP03D5 USER_CHAT is the first case of CUserPool::OnUserCommonPacket (jump table at 5AA7C6B,
 * op - 0x3D5): the dispatcher reads {@code i32 characterId} (Decode4 at 5AA7C0A), looks the user up (5AA1AF0) and calls
 * the chat handler 545C2E0 on it. The client shows a line, including the local user's own, only when this packet
 * arrives; CP0117 ({@link Tms282ChatRequest}) alone shows nothing.
 * <p>
 * Read order [verified: setfield_trace.py --entry 545C2E0, research/tms2823/setfield-trace/expected/infield-chat.tsv;
 * the dispatcher's charId read by static disassembly]:
 * <pre>
 * i32  characterId                                    5AA7C0A (dispatcher)
 * u8   chat type (265 ChatUserType: 0 user, 1 admin)  545C305
 * Str  text                                           545C319
 * chat info 1345CF0:
 *   Str name                                          1345D13
 *   134E4A0: Str text, Buf4 accountId, Buf4 characterId, Buf1 worldId, Buf4 characterId, Buf4 0, Str "",
 *            Buf4 0, Str "", Buf4 0                   134E4C3..134E61F (265 Char.encodeChatInfo after the name)
 *   Buf1 0                                            134E647 (tail jump; NEW in 282, 265 writes nothing)
 * u8   display mode (265 "onlyBalloon", 3 = both)     545C389
 * u8   0                                              545C395
 * u8   worldId                                        545C3A3
 * u8   n, n x i32 (std::set, 56AF80)                  56AFE6; this builder sends n = 0
 * </pre>
 * 265 UserPacket.chat writes the same fields but four trailing bytes (3, 0, world, 0) and no chat-info tail byte, so
 * its packet ends one byte early for the 282 client. The meanings of the chat type, the display mode, the second
 * trailing byte and the set are taken from 265 [presumed]; widths and order are [verified].
 */
public final class Tms282ChatPackets {
    public static final int USER_CHAT = 0x03D5;
    public static final int CHAT_TYPE_USER = 0;
    public static final int CHAT_TYPE_ADMIN = 1;

    private Tms282ChatPackets() {
    }

    /** The speaking character, as 265 Char.encodeChatInfo reads it from Char/Account. */
    public record ChatSender(int characterId, int accountId, String name, int worldId) {
        public ChatSender {
            Objects.requireNonNull(name, "name");
            checkByte(worldId, "World ID");
        }
    }

    /**
     * LP03D5. {@code displayMode} is normally the byte the client sent in CP0117 ({@link Tms282ChatRequest#displayMode()}).
     * Strings are written in {@code charset} (the same charset as character names).
     */
    public static OutPacket userChat(ChatSender sender, int chatType, String text, int displayMode, Charset charset) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(charset, "charset");
        checkByte(chatType, "Chat type");
        checkByte(displayMode, "Chat display mode");
        OutPacket out = Tms282LoginPacketSupport.packet(USER_CHAT);
        // encodeInt(long) throughout: OutPacket.encodeInt(int) silently drops the value -88888.
        out.encodeInt((long) sender.characterId());
        out.encodeByte(chatType);
        str(out, text, charset);
        // chat info (1345CF0 -> 134E4A0)
        str(out, sender.name(), charset);
        str(out, text, charset);
        out.encodeInt((long) sender.accountId());
        out.encodeInt((long) sender.characterId());
        out.encodeByte(sender.worldId());
        out.encodeInt((long) sender.characterId());
        out.encodeInt(0L);
        str(out, "", charset);
        out.encodeInt(0L);
        str(out, "", charset);
        out.encodeInt(0L);
        out.encodeByte(0); // 282 chat-info tail byte
        out.encodeByte(displayMode);
        out.encodeByte(0);
        out.encodeByte(sender.worldId());
        out.encodeByte(0); // empty i32 set (56AF80)
        return out;
    }
}
