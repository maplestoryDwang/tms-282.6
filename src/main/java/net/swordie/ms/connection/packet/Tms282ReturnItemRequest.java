package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HexFormat;

/**
 * The two "move me somewhere" consumable requests:
 * <ul>
 *     <li>CP015F USER_PORTAL_SCROLL_USE_REQUEST (return / town scrolls) [verified: sender 5EE0110, sequence
 *     {@code i32 tick, u16 slot, i32 itemId}, identical to 265];</li>
 *     <li>CP015E USER_MAP_TRANSFER_ITEM_USE_REQUEST [presumed: sender 5E1FEF0 writes {@code u16 slot, i32 itemId},
 *     an optional block (5E33DC0) and {@code i32 tick} (5B4FC50)]; 265 has no handler, the server treats it like a
 *     scroll with a fixed destination ({@code ItemInfo.moveTo}). The body after itemId is kept raw.</li>
 * </ul>
 */
public record Tms282ReturnItemRequest(int opcode, int tick, short slot, int itemId, byte[] tail) {
    public static final int PORTAL_SCROLL = 0x015F;
    public static final int MAP_TRANSFER_ITEM = 0x015E;

    public Tms282ReturnItemRequest {
        tail = tail.clone();
    }

    @Override
    public byte[] tail() {
        return tail.clone();
    }

    /** CP015F: exactly {@code i32 tick, u16 slot, i32 itemId}. */
    public static Tms282ReturnItemRequest parsePortalScroll(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, PORTAL_SCROLL, "CP015F");
        Tms282ReturnItemRequest r = new Tms282ReturnItemRequest(PORTAL_SCROLL, in.i32(), in.i16(), in.i32(), new byte[0]);
        in.end();
        return r;
    }

    /** CP015E: {@code u16 slot, i32 itemId}, then the rest raw (its last 4 bytes, when present, are the tick). */
    public static Tms282ReturnItemRequest parseMapTransferItem(byte[] packet) {
        Tms282FieldRequestReader in = Tms282FieldRequestReader.open(packet, MAP_TRANSFER_ITEM, "CP015E");
        short slot = in.i16();
        int itemId = in.i32();
        byte[] rest = in.rest();
        int tick = rest.length >= 4 ? ByteBuffer.wrap(rest, rest.length - 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() : 0;
        return new Tms282ReturnItemRequest(MAP_TRANSFER_ITEM, tick, slot, itemId, rest);
    }

    /** The 265 USER_PORTAL_SCROLL_USE_REQUEST body: {@code i32 tick, i16 slot, i32 itemId}. */
    public byte[] toGms265PortalScrollBody() {
        return ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN).putInt(tick).putShort(slot).putInt(itemId).array();
    }

    @Override
    public String toString() {
        return String.format("CP%04X %s slot=%d item=%d%s", opcode, opcode == PORTAL_SCROLL ? "portal scroll" : "map transfer item",
                slot, itemId, tail.length == 0 ? "" : " tail=" + HexFormat.of().formatHex(tail));
    }
}
