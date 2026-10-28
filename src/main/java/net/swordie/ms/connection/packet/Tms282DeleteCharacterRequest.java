package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * CP0091 (int characterId, str spw, plaintext), CP0092 (int characterId, no SPW) and
 * CP0090 (str spw, int characterId; legacy order with no UI caller, 3C9CFC0).
 * Evidence: research/tms2823/packet-audit/login-cp-layouts-2823.md section 1.
 */
public record Tms282DeleteCharacterRequest(int opcode, int characterId, String spw) {

    public static Tms282DeleteCharacterRequest parse(byte[] packet, Charset charset) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            int opcode = Short.toUnsignedInt(in.getShort());
            int characterId;
            String spw = "";
            if (opcode == 0x0090) {
                spw = readString(in, charset, opcode);
                characterId = in.getInt();
            } else if (opcode == 0x0091) {
                characterId = in.getInt();
                spw = readString(in, charset, opcode);
            } else if (opcode == 0x0092) {
                characterId = in.getInt();
            } else {
                throw new IllegalArgumentException("Not a delete-character request");
            }
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("Unexpected bytes after delete-character request");
            }
            return new Tms282DeleteCharacterRequest(opcode, characterId, spw);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated delete-character request", e);
        }
    }

    /** The SPW is plaintext; logs and exception messages only show whether one was sent. */
    @Override
    public String toString() {
        return String.format("Tms282DeleteCharacterRequest[opcode=0x%04X, characterId=%d, spw=%s]",
                opcode, characterId, spw == null || spw.isEmpty() ? "<none>" : "<redacted>");
    }

    private static String readString(ByteBuffer in, Charset charset, int opcode) {
        int length = Short.toUnsignedInt(in.getShort());
        if (length > in.remaining()) {
            throw new IllegalArgumentException(String.format("CP%04X SPW length exceeds packet", opcode));
        }
        byte[] bytes = new byte[length];
        in.get(bytes);
        return new String(bytes, charset);
    }
}
