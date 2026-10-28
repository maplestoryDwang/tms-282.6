package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.enums.InvType;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What a 265 inventory change did, as TMS 282.3 LP008F operations (memento: {@link #of} before, {@link #changes}
 * after). The 265 code that changes an inventory ({@code Char.addDrop}, {@code addItemToInventory}) writes its own
 * 265 INVENTORY_OPERATION, which the fail-closed guard drops; comparing the inventory before and after gives the
 * 282 packet without touching that code.
 * <ul>
 *     <li>an item object not there before -> Add (type 0) at its bag index;</li>
 *     <li>the same item object with another quantity -> UpdateQuantity (type 1, i32);</li>
 *     <li>the same object at another bag index -> Move (type 2);</li>
 *     <li>an item object gone -> Remove (type 3) at its old bag index.</li>
 * </ul>
 */
public final class Tms282InventoryDiff {
    private final InvType type;
    private final Inventory inventory;
    private final Map<Item, Slot> before;

    private record Slot(int bagIndex, int quantity) {
    }

    private Tms282InventoryDiff(InvType type, Inventory inventory) {
        this.type = type;
        this.inventory = inventory;
        this.before = snapshot(inventory);
    }

    /** Remembers {@code inventory} (of type {@code type}) as it is now. */
    public static Tms282InventoryDiff of(InvType type, Inventory inventory) {
        return new Tms282InventoryDiff(Objects.requireNonNull(type, "type"), Objects.requireNonNull(inventory, "inventory"));
    }

    /** The operations that turn the remembered inventory into the current one. */
    public List<Tms282InventoryPackets.Operation> changes() {
        Map<Item, Slot> after = snapshot(inventory);
        List<Tms282InventoryPackets.Operation> ops = new ArrayList<>();
        for (Map.Entry<Item, Slot> e : before.entrySet()) {
            if (!after.containsKey(e.getKey())) {
                ops.add(new Tms282InventoryPackets.Remove(type, (short) e.getValue().bagIndex()));
            }
        }
        for (Map.Entry<Item, Slot> e : after.entrySet()) {
            Slot now = e.getValue();
            Slot was = before.get(e.getKey());
            if (was == null) {
                ops.add(new Tms282InventoryPackets.Add(type, (short) now.bagIndex(), e.getKey()));
                continue;
            }
            if (was.bagIndex() != now.bagIndex()) {
                ops.add(new Tms282InventoryPackets.Move(type, (short) was.bagIndex(), (short) now.bagIndex()));
            }
            if (was.quantity() != now.quantity()) {
                ops.add(new Tms282InventoryPackets.UpdateQuantity(type, (short) now.bagIndex(), now.quantity()));
            }
        }
        return ops;
    }

    private static Map<Item, Slot> snapshot(Inventory inventory) {
        Map<Item, Slot> m = new IdentityHashMap<>();
        for (Item item : new ArrayList<>(inventory.getItems())) {
            if (item != null) {
                m.put(item, new Slot(item.getBagIndex(), item.getQuantity()));
            }
        }
        return m;
    }
}
