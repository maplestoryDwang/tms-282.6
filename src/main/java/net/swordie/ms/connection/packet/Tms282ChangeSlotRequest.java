package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * CP0141 USER_CHANGE_SLOT_POSITION_REQUEST (265 InHeader 0x13C): move, swap, equip, unequip and drop (newPos 0).
 * Sender 5E06FB0, packet built at 5E075B4 [verified: static sender; opcode presumed from the +5 shift of 12C..14B]:
 * <pre>
 * u32  tick      5B4FC50()
 * u8   invType   1 equip (body slots are negative positions), 2 consume, 3 install, 4 etc, 5 cash, 6 decoration
 * u16  from
 * u16  to        0 = drop
 * u32  count     265: u16
 * Buf8 option    the sender's 6th argument (8 bytes); the request lock (5E04DA0(ctx, 1)) is set only when its
 *                dword +0 and byte +5 are 0, i.e. for plain moves
 * </pre>
 * 346A070 appends nothing for 0x141. 265 reads {@code tick, u8, i16, i16, i16 count, i32, i32} and ignores the two
 * ints: {@link #toGms265Body()} narrows the count (a larger count than a stack can hold is capped at
 * {@link Short#MAX_VALUE}, which 265 treats as "the whole stack") and passes the 8 option bytes as those ints.
 */
public record Tms282ChangeSlotRequest(int tick, int invType, short from, short to, int count, byte[] option) {
    public static final int OPCODE = 0x0141;
    public static final int LENGTH = 2 + 4 + 1 + 2 + 2 + 4 + 8;
    /** InvType.EQUIP (worn items move to / from it with negative positions). */
    static final int EQUIP_INV_TYPE = 1;

    public Tms282ChangeSlotRequest {
        Tms282LoginPacketSupport.checkByte(invType, "CP0141 invType");
        if (option == null || option.length != 8) {
            throw new IllegalArgumentException("CP0141 option is 8 bytes");
        }
        option = option.clone();
    }

    /** Accepts the full plaintext packet, including the opcode. Exactly {@value #LENGTH} bytes. */
    public static Tms282ChangeSlotRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0141");
        Tms282ChangeSlotRequest request = new Tms282ChangeSlotRequest(in.i32(), in.u8(), in.i16(), in.i16(), in.i32(),
                in.bytes(8));
        in.end();
        return request;
    }

    public boolean isDrop() {
        return to == 0;
    }

    @Override
    public byte[] option() {
        return option.clone();
    }

    /** The count as 265 reads it (u16 in a signed short): negative counts become 0, large ones the maximum. */
    public short gms265Count() {
        return (short) Math.max(0, Math.min(count, Short.MAX_VALUE));
    }

    /** {@link #from} as the 265 server stores it (a worn android / heart slot of the equip inventory, {@link Tms282BodySlots}). */
    public short serverFrom() {
        return invType == EQUIP_INV_TYPE ? Tms282BodySlots.toServer(from) : from;
    }

    /** {@link #to} as the 265 server stores it. */
    public short serverTo() {
        return invType == EQUIP_INV_TYPE ? Tms282BodySlots.toServer(to) : to;
    }

    /**
     * The 265 body (no opcode): {@code i32 tick, u8 invType, i16 from, i16 to, i16 count, 8 option bytes}, with the
     * positions as the server stores them ({@link #serverFrom()}, {@link #serverTo()}).
     */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(4 + 1 + 2 + 2 + 2 + 8).order(ByteOrder.LITTLE_ENDIAN).putInt(tick).put((byte) invType)
                .putShort(serverFrom()).putShort(serverTo()).putShort(gms265Count()).put(option).array();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tms282ChangeSlotRequest r && r.tick == tick && r.invType == invType && r.from == from
                && r.to == to && r.count == count && Arrays.equals(r.option, option);
    }

    @Override
    public int hashCode() {
        return 31 * java.util.Objects.hash(tick, invType, from, to, count) + Arrays.hashCode(option);
    }

    @Override
    public String toString() {
        return String.format("CP0141 change slot inv=%d %d->%d count=%d option=%s", invType, from, to, count,
                HexFormat.of().formatHex(option));
    }
}
