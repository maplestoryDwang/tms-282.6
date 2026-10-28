package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * CP00C5, sender 3CB3890 (send 3CB38C6): u8 flag (0 on every known caller), i32 accountId (app+23AC, the
 * user ID LP0000 wrote), i32 characterId, u8 burnType (BurningCheckDlg 492D870), i32 job, i32 trailer (-1).
 * 20 bytes including the opcode. Evidence: burning-display-chars-2823.md 1.1, login-cp-layouts-2823.md CP00C5.
 * The trailer is kept, not checked: only the layout is verified, not that every caller sends -1.
 */
public record Tms282BurningCharacterRequest(int flag, int accountId, int characterId, int burnType, int job,
                                            int trailer) {
    public static final int OPCODE = 0x00C5;

    /** Accepts the full plaintext packet, including opcode. */
    public static Tms282BurningCharacterRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Not a CP00C5 burning-character request");
            }
            var request = new Tms282BurningCharacterRequest(Byte.toUnsignedInt(in.get()), in.getInt(), in.getInt(),
                    Byte.toUnsignedInt(in.get()), in.getInt(), in.getInt());
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("Unexpected bytes after CP00C5");
            }
            return request;
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP00C5", e);
        }
    }
}
