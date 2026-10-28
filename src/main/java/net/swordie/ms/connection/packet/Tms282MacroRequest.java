package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Macro;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.BufferUnderflowException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.List;

/** CP01B0: native 3D85FE0 -> 903830 -> 9035B0. No transport trailer.
 * At most five entries: Str name (12 encoded bytes), u8 muted, three i32 skill IDs. */
public final class Tms282MacroRequest {
    public static final int OPCODE = 0x01B0;
    private Tms282MacroRequest() { }

    public static List<Macro> parse(byte[] packet, Charset charset) {
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) throw new IllegalArgumentException("wrong opcode");
            int count = Byte.toUnsignedInt(in.get());
            if (count > Tms282KeyPackets.MAX_MACROS) throw new IllegalArgumentException("more than five macros");
            List<Macro> macros = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                int length = Short.toUnsignedInt(in.getShort());
                if (length > 12) throw new IllegalArgumentException("macro name exceeds native 12-byte buffer");
                byte[] name = new byte[length]; in.get(name);
                for (byte b : name) if (b == 0) throw new IllegalArgumentException("embedded name terminator");
                Macro macro = new Macro();
                macro.setName(charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(name)).toString());
                int muted = Byte.toUnsignedInt(in.get());
                if (muted > 1) throw new IllegalArgumentException("invalid muted flag");
                macro.setMuted(muted == 1);
                for (int slot = 0; slot < 3; slot++) {
                    int skill = in.getInt();
                    if (skill < 0) throw new IllegalArgumentException("negative skill ID");
                    macro.setSkillAtPos(slot, skill);
                }
                macros.add(macro);
            }
            if (in.hasRemaining()) throw new IllegalArgumentException("trailing macro bytes");
            return macros;
        } catch (BufferUnderflowException | CharacterCodingException e) {
            throw new IllegalArgumentException("truncated or invalid macro data", e);
        }
    }
}
