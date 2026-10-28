package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;

/**
 * CP0117 USER_CHAT (265 InHeader 0x115; senders 372B430, 372C260, 3837CA0). Evidence: packet-audit/ingame-cp-map-2823.tsv
 * row 0x0117 (encode sequence {@code 4 S 1}, [verified]) and the live capture channel-1790159554177
 * (recv-1908..1910-0117.bin):
 * <pre>
 * 17 01 | ae d6 dd 02 | 01 00 67 | 03        u32 tick, Str "g", u8 3
 * 17 01 | de e2 dd 02 | 05 00 75 68 69 6a 6e | 03
 * </pre>
 * There is no extra leading byte: after the 2-byte opcode come exactly the tick, the string and one byte.
 * The trailing byte was 3 in every sample; 265 ChatHandler ignores it, and 265 UserPacket.chat sends 3 as its
 * "onlyBalloon" byte (0 nothing, 1 chat log, 2 balloon, 3 both), so it is presumed to be that display mode and is
 * echoed back in LP03D5 ({@link Tms282ChatPackets}) [presumed meaning, verified width].
 * <p>
 * The parser is strict: a string that is not valid in the caller's charset, a truncated packet or trailing bytes are
 * rejected.
 */
public record Tms282ChatRequest(int tick, String text, int displayMode) {
    public static final int OPCODE = 0x0117;
    /** Value seen in every captured CP0117 (all three samples). */
    public static final int DISPLAY_BOTH = 3;

    public Tms282ChatRequest {
        Objects.requireNonNull(text, "text");
        Tms282LoginPacketSupport.checkByte(displayMode, "Chat display mode");
    }

    /** Accepts the full plaintext packet, including the opcode. */
    public static Tms282ChatRequest parse(byte[] packet, Charset charset) {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(charset, "charset");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong CP0117 opcode");
            }
            int tick = in.getInt();
            byte[] raw = new byte[Short.toUnsignedInt(in.getShort())];
            in.get(raw);
            String text;
            try {
                CharBuffer chars = charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(raw));
                text = chars.toString();
            } catch (CharacterCodingException e) {
                throw new IllegalArgumentException("CP0117 text is not valid " + charset.name(), e);
            }
            int displayMode = Byte.toUnsignedInt(in.get());
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("CP0117 has " + in.remaining() + " trailing bytes");
            }
            return new Tms282ChatRequest(tick, text, displayMode);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP0117", e);
        }
    }
}
