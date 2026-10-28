package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP0190 USER_ABILITY_UP_REQUEST (265 InHeader 0x189): one AP into one stat. Sender 5EDF600 (packet built at
 * 5EDF7A7) [verified: static sender; opcode presumed from the +7 shift shared with CP0191]:
 * <pre>
 * u32  tick   5B4FC50()
 * Buf8 stat   the sender's 2nd argument, a u64 stat flag (the LP009D mask bit of the stat: STR 0x40, DEX 0x80,
 *             INT 0x100, LUK 0x200, MaxHP 0x800, MaxMP 0x2000)
 * </pre>
 * The client asks a yes/no question first (5DFC300 / 1302640 0x1080) and sets the request lock after sending
 * (5E04DA0(ctx, 1)). 265 reads {@code tick, i16 stat}; the flags are the same values as the 265 {@code Stat} enum,
 * so {@link #toGms265Body()} narrows the u64 when it fits a short and the 265 handler does the rest.
 */
public record Tms282AbilityUpRequest(int tick, long stat) {
    public static final int OPCODE = 0x0190;
    public static final int LENGTH = 2 + 4 + 8;

    /** Accepts the full plaintext packet, including the opcode. Exactly {@value #LENGTH} bytes. */
    public static Tms282AbilityUpRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0190");
        int tick = in.i32();
        long stat = ByteBuffer.wrap(in.bytes(8)).order(ByteOrder.LITTLE_ENDIAN).getLong();
        in.end();
        return new Tms282AbilityUpRequest(tick, stat);
    }

    /** True when the flag fits the 265 {@code i16} (every AP stat does). */
    public boolean fitsGms265() {
        return stat > 0 && stat <= Short.MAX_VALUE;
    }

    /** The 265 body (no opcode): {@code i32 tick, i16 stat}. */
    public byte[] toGms265Body() {
        if (!fitsGms265()) {
            throw new IllegalStateException(String.format("CP0190 stat flag 0x%X does not fit the 265 short", stat));
        }
        return ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN).putInt(tick).putShort((short) stat).array();
    }

    @Override
    public String toString() {
        return String.format("CP0190 ability up stat=0x%X", stat);
    }
}
