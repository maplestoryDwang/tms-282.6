package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * The inventory "arrange" buttons of the 282 item window:
 * <ul>
 *     <li>CP013F USER_GATHER_ITEM_REQUEST [verified: sender 5E06A40 writes {@code i32 tick, u8 invType}, as 265];</li>
 *     <li>CP0140 USER_SORT_ITEM_REQUEST [verified: sender 5E06B60 writes {@code i32 tick, u8 invType} and a 4-byte
 *     buffer from its third argument (the sort mode); 265 reads only tick and invType].</li>
 * </ul>
 */
public record Tms282InventoryArrangeRequest(int opcode, int tick, int invType, int mode) {
    public static final int GATHER = 0x013F;
    public static final int SORT = 0x0140;

    /** CP013F: exactly {@code i32 tick, u8 invType}. */
    public static Tms282InventoryArrangeRequest parseGather(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, GATHER, "CP013F");
        Tms282InventoryArrangeRequest r = new Tms282InventoryArrangeRequest(GATHER, in.i32(), in.u8(), 0);
        in.end();
        return r;
    }

    /** CP0140: exactly {@code i32 tick, u8 invType, i32 mode}. */
    public static Tms282InventoryArrangeRequest parseSort(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, SORT, "CP0140");
        Tms282InventoryArrangeRequest r = new Tms282InventoryArrangeRequest(SORT, in.i32(), in.u8(), in.i32());
        in.end();
        return r;
    }

    public boolean isSort() {
        return opcode == SORT;
    }

    /** The 265 USER_GATHER_ITEM_REQUEST / USER_SORT_ITEM_REQUEST body: {@code i32 tick, u8 invType}. */
    public byte[] toGms265Body() {
        return ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN).putInt(tick).put((byte) invType).array();
    }

    @Override
    public String toString() {
        return String.format("CP%04X %s invType=%d%s", opcode, isSort() ? "sort" : "gather", invType,
                isSort() ? " mode=" + mode : "");
    }
}
