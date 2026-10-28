package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Tms282InventoryPackets;
import net.swordie.ms.connection.packet.Tms282ItemEncoder;
import net.swordie.ms.enums.InvType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What a 265 handler did to every inventory of a character, as TMS 282.3 LP008F operations (memento: {@link #of}
 * before the call, {@link #changes} after). For the enhancement flows (scrolls, star force, cubes, flames, symbols,
 * hammers), which change an equip in place and answer with 265 {@code Item.updateToChar} - an INVENTORY_OPERATION
 * Add over the same slot that the fail-closed guard drops.
 * <ul>
 *     <li>CONSUME / INSTALL / ETC / CASH: {@link Tms282InventoryDiff} (Add, UpdateQuantity, Move, Remove);</li>
 *     <li>EQUIP / DECORATION / EQUIPPED: by item identity and the item's 282 wire form - a new item is an Add, a
 *     gone item a Remove, a moved item a Move, and an item whose encoding changed (stats, potential, stars, flame,
 *     symbol level...) an Add over its own slot, which is what 265 {@code updateToChar} sends. Worn items use the
 *     wire positions of 265 {@code WvsContext.inventoryOperation}: bag type EQUIP (DECORATION for a cash item) and
 *     the negative body slot.</li>
 * </ul>
 * Removes come first, then moves, then adds, as 265 sends them in these flows (a boomed equip, then the refresh).
 */
public final class Tms282InventoryWatch {
    private static final List<InvType> BUNDLES = List.of(InvType.CONSUME, InvType.INSTALL, InvType.ETC, InvType.CASH);
    private static final List<InvType> EQUIPS = List.of(InvType.EQUIP, InvType.DECORATION, InvType.EQUIPPED);

    private final Char chr;
    private final List<Tms282InventoryDiff> bundles = new ArrayList<>();
    private final Map<Item, EquipState> equips;

    /** One equip-like item as it was: where (265 inventory and bag index) and its 282 bytes. */
    private record EquipState(InvType inventory, int bagIndex, byte[] wire) {
    }

    private Tms282InventoryWatch(Char chr) {
        this.chr = chr;
        for (InvType type : BUNDLES) {
            bundles.add(Tms282InventoryDiff.of(type, chr.getInventoryByType(type)));
        }
        this.equips = snapshotEquips(chr);
    }

    /** Remembers every inventory of {@code chr} as it is now. */
    public static Tms282InventoryWatch of(Char chr) {
        return new Tms282InventoryWatch(Objects.requireNonNull(chr, "chr"));
    }

    /** The LP008F operations that turn the remembered inventories into the current ones. */
    public List<Tms282InventoryPackets.Operation> changes() {
        List<Tms282InventoryPackets.Operation> removes = new ArrayList<>();
        List<Tms282InventoryPackets.Operation> moves = new ArrayList<>();
        List<Tms282InventoryPackets.Operation> adds = new ArrayList<>();
        for (Tms282InventoryDiff diff : bundles) {
            for (Tms282InventoryPackets.Operation op : diff.changes()) {
                (op instanceof Tms282InventoryPackets.Remove ? removes
                        : op instanceof Tms282InventoryPackets.Add ? adds : moves).add(op);
            }
        }
        Map<Item, EquipState> after = snapshotEquips(chr);
        for (Map.Entry<Item, EquipState> e : equips.entrySet()) {
            if (!after.containsKey(e.getKey())) {
                removes.add(new Tms282InventoryPackets.Remove(wireType(e.getKey(), e.getValue()), wirePos(e.getValue())));
            }
        }
        for (Map.Entry<Item, EquipState> e : after.entrySet()) {
            Item item = e.getKey();
            EquipState now = e.getValue();
            EquipState was = equips.get(item);
            if (was == null) {
                adds.add(new Tms282InventoryPackets.Add(wireType(item, now), wirePos(now), item));
                continue;
            }
            if (was.inventory() != now.inventory() || was.bagIndex() != now.bagIndex()) {
                moves.add(new Tms282InventoryPackets.Move(wireType(item, was), wirePos(was), wirePos(now)));
            }
            if (!Arrays.equals(was.wire(), now.wire())) {
                adds.add(new Tms282InventoryPackets.Add(wireType(item, now), wirePos(now), item));
            }
        }
        List<Tms282InventoryPackets.Operation> ops = new ArrayList<>(removes);
        ops.addAll(moves);
        ops.addAll(adds);
        return ops;
    }

    /** The 282 wire form of one item (the fingerprint of an equip). */
    static byte[] wire(Item item) {
        OutPacket out = new OutPacket();
        Tms282ItemEncoder.encode(out, item, StandardCharsets.UTF_8);
        return out.getData();
    }

    private static Map<Item, EquipState> snapshotEquips(Char chr) {
        Map<Item, EquipState> m = new IdentityHashMap<>();
        for (InvType type : EQUIPS) {
            Inventory inventory = chr.getInventoryByType(type);
            for (Item item : new ArrayList<>(inventory.getItems())) {
                if (item != null) {
                    m.put(item, new EquipState(type, item.getBagIndex(), wire(item)));
                }
            }
        }
        return m;
    }

    /** 265 inventoryOperation: a worn item travels as EQUIP, or DECORATION when it is a cash item. */
    private static InvType wireType(Item item, EquipState state) {
        if (state.inventory() != InvType.EQUIPPED) {
            return state.inventory();
        }
        return item.isCash() ? InvType.DECORATION : InvType.EQUIP;
    }

    private static short wirePos(EquipState state) {
        int pos = Math.abs(state.bagIndex());
        return (short) (state.inventory() == InvType.EQUIPPED ? -pos : pos);
    }
}
