package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * CP0196 USER_SKILL_UP_REQUEST (265 InHeader 0x18E) [verified, static sender; ingame-cp-map-2823.md 3.3]:
 * {@code u32 tick, u32 skillId, u32 amount}; wire amount 0 means one point, positive values are bulk amounts.
 * (282 UI 4F53E50: mode 0 sends the initial zero at 4F5428D; 5EDFB30 writes it unchanged.)
 * The client does not append a trailing u32 to this CP. Same fields as
 * 265 {@code UserStatHandler.handleUserSkillUpRequest} reads. The parser is strict (exactly 14 bytes).
 */
public record Tms282SkillUpRequest(int tick, int skillId, int amount) {
    public static final int OPCODE = 0x0196;
    public static final int LENGTH = 2 + 4 + 4 + 4;

    /** Accepts the full plaintext packet, including the opcode. */
    public static Tms282SkillUpRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong CP0196 opcode");
            }
            int tick = in.getInt();
            int skillId = in.getInt();
            int amount = in.getInt();
            var request = new Tms282SkillUpRequest(tick, skillId, amount == 0 ? 1 : amount);
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("CP0196 has " + in.remaining() + " trailing bytes");
            }
            return request;
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP0196", e);
        }
    }
}
