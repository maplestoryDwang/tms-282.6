package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.InventoryOperation;

import java.util.Optional;

/**
 * Codec family of the 265 INVENTORY_OPERATION builders in WvsContext (inventoryOperation, addItemToInventory,
 * removeAndAddItem). GMS 265 registers none, so every builder keeps its own bytes. A profile's encoder returns its
 * packet, or empty to keep the 265 packet (which its outbound guard may then drop).
 */
public interface InventoryOperationEncoder {

    /** One operation of {@code type} on {@code item} (WvsContext.inventoryOperation). */
    Optional<OutPacket> operation(boolean exclRequest, boolean notRemoveAddInfo, InventoryOperation type, short oldPos,
                                  short newPos, Item item);

    /** {@code item} added at its bag index, exclRequest set (WvsContext.addItemToInventory). */
    Optional<OutPacket> add(Item item);

    /** {@code item} removed from and re-added at its bag index (WvsContext.removeAndAddItem). */
    Optional<OutPacket> removeAndAdd(Item item);

    /** The active profile's encoder, if it has one. */
    static Optional<InventoryOperationEncoder> active() {
        return ProfileEncoders.find(InventoryOperationEncoder.class);
    }
}
