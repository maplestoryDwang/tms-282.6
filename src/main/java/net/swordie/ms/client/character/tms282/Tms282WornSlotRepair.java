package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;

/**
 * TMS 282 worn-slot repair (research/tms2823/item-usability-20260928/equips/README.md, WI-1).
 * <p>
 * The 282 client wears medal / shoulder / pocket / badge / emblem / 2nd pendant in 21 / 28 / 31 / 34 / 35 / 36. The
 * 265 login migration in {@code Char.initEquips} (21->49, 28->54, 31->65, ...) therefore must not run under 282; this
 * class moves items that an earlier login (or a server-side 265 placement) put in the 265 slots back. Only items with
 * the matching prefix move, and only into an empty target slot; everything else is left alone. Idempotent.
 * The 2nd / 3rd pet's pet equip (265 PetAcc2 / PetAcc3 130 / 138) moves to the 282 slots 124 / 126 (WI-3).
 */
public final class Tms282WornSlotRepair {
    /** {from slot, to slot, item prefix (itemId / 10000)}; checked in this order. */
    static final int[][] MOVES = {
            {49, 21, 114},  // medal: 265 Medal 49 -> 282 21
            {51, 28, 115},  // shoulder: 265 Shoulder 51 -> 282 28
            {54, 28, 115},  // shoulder moved to MechanicalHeart 54 by the old 28 -> 54 migration
            {52, 31, 116},  // pocket: 265 PocketItem 52 -> 282 31
            {65, 31, 116},  // pocket moved to ExtendedPendant 65 by the old 31 -> 65 migration
            {56, 34, 118},  // badge: 265 Badge 56 -> 282 34
            {61, 35, 119},  // emblem 1190xxx / 1191xxx: 265 Emblem 61 -> 282 35
            {65, 36, 112},  // 2nd pendant 1122xxx: 265 ExtendedPendant 65 -> 282 36
            {130, 124, 180}, // 2nd pet's pet equip: 265 PetAcc2 130 -> 282 124 (cash 24; README WI-3)
            {138, 126, 180}, // 3rd pet's pet equip: 265 PetAcc3 138 -> 282 126 (cash 26)
    };

    private Tms282WornSlotRepair() {
    }

    /** @return the number of items moved */
    public static int repair(Char chr) {
        Inventory worn = chr == null ? null : chr.getEquippedInventory();
        if (worn == null) {
            return 0;
        }
        int moved = 0;
        for (int[] m : MOVES) {
            // scan instead of getItemBySlot: the old 28 -> 54 move could leave a shoulder and a heart both in 54
            Item item = worn.getItems().stream()
                    .filter(i -> i.getBagIndex() == m[0] && matches(i.getItemId(), m[2]))
                    .findFirst().orElse(null);
            if (item == null || worn.getItemBySlot(m[1]) != null) {
                continue;
            }
            item.setBagIndex(m[1]);
            moved++;
            System.out.printf("[TMS282] worn slot repair character=%d item=%d %d -> %d%n", chr.getId(), item.getItemId(), m[0], m[1]);
        }
        return moved;
    }

    static boolean matches(int itemId, int prefix) {
        if (prefix == 119) {
            return itemId / 10000 == 119 && (itemId / 1000 == 1190 || itemId / 1000 == 1191); // emblems 1190xxx / 1191xxx
        }
        if (prefix == 180) {
            return itemId / 10000 == 180 && !net.swordie.ms.connection.packet.Tms282BodySlots.isPetNameTag(itemId);
        }
        return itemId / 10000 == prefix;
    }
}
