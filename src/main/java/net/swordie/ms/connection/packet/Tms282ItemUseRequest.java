package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CP0148 USER_STAT_CHANGE_ITEM_USE_REQUEST (265 InHeader 0x143, potions and other stat-change consumables)
 * [verified: both static senders]. Senders 5E0B1A0 (packet built at 5E0D300) and 5E0D410 (5E0D93A):
 * <pre>
 * u32 tick      5B4FC50()
 * u16 slot      CONSUME bag position
 * u32 itemId
 * u32 count     282 only: 5E0D93A writes 1, 5E0D300 writes r13d [presumed: a use count]
 * </pre>
 * Both senders set the client's request lock (5E04DA0(ctx, 1)); the reply must carry exclRequest. 346A070 does not
 * append a tail u32 for 0x148 (ingame-cp-map-2823.md 1.3). 265 reads {@code tick, i16 slot, i32 itemId}: the 282
 * body is the 265 body plus the count, so {@link #toGms265Body()} simply leaves the count out.
 */
public record Tms282ItemUseRequest(int tick, short slot, int itemId, int count) {
    public static final int OPCODE = 0x0148;
    public static final int LENGTH = 2 + 4 + 2 + 4 + 4;

    /** Accepts the full plaintext packet, including the opcode. Exactly {@value #LENGTH} bytes. */
    public static Tms282ItemUseRequest parse(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, OPCODE, "CP0148");
        Tms282ItemUseRequest request = new Tms282ItemUseRequest(in.i32(), in.i16(), in.i32(), in.i32());
        in.end();
        return request;
    }

    /** The 265 USER_STAT_CHANGE_ITEM_USE_REQUEST body (no opcode): {@code i32 tick, i16 slot, i32 itemId}. */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN).putInt(tick).putShort(slot).putInt(itemId).array();
    }

    @Override
    public String toString() {
        return String.format("CP0148 item use slot=%d item=%d count=%d", slot, itemId, count);
    }
}
