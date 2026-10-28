package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.enums.InvType;

import java.util.List;
import java.util.Objects;

/**
 * What a 265 {@code InventoryHandler.handleUserChangeSlotPositionRequest} did, as the TMS 282.3 LP008F operation
 * 265 would have written (memento: {@link #before} the call, {@link #operations} after). 265 answers a move, swap,
 * equip or unequip with one {@code Move(from, to)} and a drop with {@code Remove(from)} (whole stack) or
 * {@code UpdateQuantity(from)} (part of it), all in the request's bag type with the wire positions (body slots
 * negative); its own INVENTORY_OPERATION is dropped by the fail-closed guard. A refused request changes nothing and
 * gives no operation (the caller unlocks the client).
 * <p>
 * Unlike {@link Tms282InventoryDiff} this follows the one item across two 265 inventories (EQUIP/DECORATION and
 * EQUIPPED, whose bag indexes are stored positive), so an equip is a single Move and not a Remove plus an Add.
 */
public final class Tms282SlotChange {
    private final Char chr;
    private final InvType wireType;
    private final short from;
    private final short to;
    private final Item item;
    private final int quantity;

    private Tms282SlotChange(Char chr, InvType wireType, short from, short to) {
        this.chr = chr;
        this.wireType = wireType;
        this.from = from;
        this.to = to;
        this.item = wireType == null ? null : inventoryAt(chr, wireType, from).getItemBySlot(from);
        this.quantity = item == null ? 0 : item.getQuantity();
    }

    /**
     * Remembers the item the request moves ({@code invType} as sent: 1..6; {@code from}/{@code to} wire positions).
     * An unknown bag type or an empty slot remembers nothing, and {@link #operations} is then empty.
     */
    public static Tms282SlotChange before(Char chr, int invType, short from, short to) {
        Objects.requireNonNull(chr, "chr");
        InvType type = InvType.getInvTypeByVal(invType);
        if (type == InvType.EQUIPPED) {
            type = null; // never on the wire: body slots are EQUIP/DECORATION with a negative position
        }
        return new Tms282SlotChange(chr, type, from, to);
    }

    /** The item the request names, or null. */
    public Item item() {
        return item;
    }

    /** The LP008F operations for what happened since {@link #before}; empty when 265 refused the request. */
    public List<Tms282InventoryPackets.Operation> operations() {
        if (item == null) {
            return List.of();
        }
        if (to == 0) {
            if (inventoryAt(chr, wireType, from).getItemBySlot(from) != item) {
                return List.of(new Tms282InventoryPackets.Remove(wireType, from));
            }
            if (item.getQuantity() != quantity) {
                return List.of(new Tms282InventoryPackets.UpdateQuantity(wireType, from, item.getQuantity()));
            }
            return List.of();
        }
        if (inventoryAt(chr, wireType, to).getItemBySlot(to) == item) {
            return List.of(new Tms282InventoryPackets.Move(wireType, from, to));
        }
        return List.of();
    }

    /** True when the request put an item on or took one off a body slot (the look may have changed). */
    public boolean touchesBody() {
        return from < 0 || to < 0;
    }

    private static Inventory inventoryAt(Char chr, InvType wireType, short pos) {
        return chr.getInventoryByType(pos < 0 ? InvType.EQUIPPED : wireType);
    }
}
