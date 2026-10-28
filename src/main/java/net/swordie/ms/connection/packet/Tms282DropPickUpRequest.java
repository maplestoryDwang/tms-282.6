package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

/**
 * DROP_PICK_UP_REQUEST of TMS 282.3 [opcode unknown: no static send site, presumed 0x054A inside the client VM;
 * ingame-cp-map-2823.md 4.5 and 7]. Read with the 265 {@code DropHandler.handleDropPickUpRequest} layout until a
 * capture shows the 282 one:
 * <pre>
 * u8  fieldKey
 * u32 tick
 * u32 0
 * u16 x, u16 y        where the character stood
 * u32 dropObjectId
 * u32 clientCrc
 * ...                 265: "foreground info", ignored
 * </pre>
 * Where 282 may differ (check against the first capture): an extra u32 somewhere in the prefix (282 added one before
 * the skill id in attacks and skill use, and after the damage in USER_HIT), the position as two i32 (282 uses i32
 * positions in some newer requests), the crc moved or removed, the u32 0 turned into a pet/pickup kind, and a tail
 * appended by 346A070 (0x54A is not in its list, so no tail is expected). {@link #parse} checks only that the prefix
 * is present; the handler additionally requires the drop id to name a drop of the character's field.
 */
public record Tms282DropPickUpRequest(int opcode, byte fieldKey, int tick, int zero, short x, short y, int dropId,
                                      int crc, byte[] rest) {
    /** Bytes of the 265 prefix after the opcode. */
    public static final int PREFIX = 1 + 4 + 4 + 2 + 2 + 4 + 4;

    public Tms282DropPickUpRequest {
        rest = Objects.requireNonNull(rest, "rest").clone();
    }

    @Override
    public byte[] rest() {
        return rest.clone();
    }

    /** Accepts the full plaintext packet, including the opcode, which must be {@code opcode}. */
    public static Tms282DropPickUpRequest parse(byte[] packet, int opcode) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            int op = Short.toUnsignedInt(in.getShort());
            if (op != opcode) {
                throw new IllegalArgumentException(String.format("Wrong pick-up opcode %04X, configured %04X", op, opcode));
            }
            byte fieldKey = in.get();
            int tick = in.getInt();
            int zero = in.getInt();
            short x = in.getShort();
            short y = in.getShort();
            int dropId = in.getInt();
            int crc = in.getInt();
            byte[] rest = Arrays.copyOfRange(packet, in.position(), packet.length);
            return new Tms282DropPickUpRequest(opcode, fieldKey, tick, zero, x, y, dropId, crc, rest);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException(String.format("Truncated CP%04X (265 pick-up prefix is %d bytes)", opcode, PREFIX), e);
        }
    }

    @Override
    public String toString() {
        return String.format("CP%04X[drop=%d at %d,%d tick=%d zero=%d rest=%dB]", opcode, dropId, x, y, tick, zero, rest.length);
    }
}
