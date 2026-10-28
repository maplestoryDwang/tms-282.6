package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * 265 packets of the inventory-arrange and death flows, copied under their 282 opcodes (bodies equal):
 * <ul>
 *     <li>GATHER_ITEM_RESULT -> LP00C5 and SORT_ITEM_RESULT -> LP00C6 {@code u8 1, u8 invType}: CWvsContext handlers
 *     5F29A00 / 5F29A30 are identical (Decode1, Decode1, nothing else) [layout 證實]; 282 LP00C7 reads I I B, so the
 *     two adjacent B B handlers are the 265 pair 198/199 shifted by -1 [opcode order 推定].</li>
 *     <li>REMOTE_SHOW_UPGRADE_TOMB_EFFECT -> LP048B {@code i32 charId, i32 itemId, i32 x, i32 y}: the remote dispatcher
 *     reads the character id, handler 5ADA930 reads three Decode4 [證實, anchor of the remote map].</li>
 * </ul>
 * The unlock of these flows is carried by the LP008F that {@code Tms282InventoryArrangeHandlers} sends.
 */
public final class Tms282InventoryArrangeTranslators implements Tms282LegacyPackets.Contributor {
    public static final Tms282InventoryArrangeTranslators INSTANCE = new Tms282InventoryArrangeTranslators();

    public static final int GATHER_ITEM_RESULT = 0x00C5;
    public static final int SORT_ITEM_RESULT = 0x00C6;
    public static final int REMOTE_SHOW_UPGRADE_TOMB_EFFECT = 0x048B;
    public static final int INVENTORY_GROW = 0x009A;

    private Tms282InventoryArrangeTranslators() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.GATHER_ITEM_RESULT, p -> copy(p, GATHER_ITEM_RESULT, 2))
                .on(OutHeader.SORT_ITEM_RESULT, p -> copy(p, SORT_ITEM_RESULT, 2))
                .on(OutHeader.INVENTORY_GROW, Tms282InventoryArrangeTranslators::translateGrow)
                .on(OutHeader.REMOTE_SHOW_UPGRADE_TOMB_EFFECT, p -> copy(p, REMOTE_SHOW_UPGRADE_TOMB_EFFECT, 16));
    }

    private static Optional<OutPacket> translateGrow(OutPacket legacy) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != 2) return Optional.empty();
        int type = Byte.toUnsignedInt(body[0]), slots = Byte.toUnsignedInt(body[1]);
        if (type < 1 || type > 6 || slots == 0) return Optional.empty();
        return Optional.of(grow(type, slots));
    }

    /** Native5EEA090 reads two unsigned bytes and resizes the slot array to slots+1.
     * Zero is not a 256-slot sentinel. Full CharacterData's wider slot count is a different format.
     */
    public static OutPacket grow(int inventoryType, int slots) {
        if (inventoryType < 1 || inventoryType > 6 || slots < 1 || slots > 255)
            throw new IllegalArgumentException("LP009A requires inventory type1..6 and slots1..255");
        OutPacket packet = Tms282LoginPacketSupport.packet(INVENTORY_GROW);
        packet.encodeByte(inventoryType);
        packet.encodeByte(slots);
        return packet;
    }

    /** The 265 body unchanged under {@code op}; a body of another length is declined. */
    static Optional<OutPacket> copy(OutPacket legacy, int op, int length) {
        byte[] body = Tms282LegacyPackets.Body.of(legacy).rest();
        if (body.length != length) {
            return Optional.empty();
        }
        OutPacket out = Tms282LoginPacketSupport.packet(op);
        out.encodeArr(body);
        return Optional.of(out);
    }
}
