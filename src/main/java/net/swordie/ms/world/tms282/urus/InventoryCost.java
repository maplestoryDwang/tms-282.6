package net.swordie.ms.world.tms282.urus;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.loaders.ItemData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Counts and removes stackable items across every stack of the inventory tab (Char.consumeItem only takes one stack). */
public final class InventoryCost {
    private InventoryCost() {
    }

    public static int count(Char chr, int itemId) {
        Item proto = ItemData.getItemDeepCopy(itemId);
        if (proto == null) return 0;
        return chr.getInventoryByType(proto.getInvType()).getItems().stream()
                .filter(i -> i.getItemId() == itemId).mapToInt(Item::getQuantity).sum();
    }

    public static boolean has(Char chr, Map<Integer, Integer> cost, int multiplier) {
        for (Map.Entry<Integer, Integer> e : cost.entrySet()) {
            if (count(chr, e.getKey()) < (long) e.getValue() * multiplier) return false;
        }
        return true;
    }

    /** Removes {@code quantity} of {@code itemId}; caller must have checked {@link #count}. */
    public static void consume(Char chr, int itemId, int quantity) {
        Item proto = ItemData.getItemDeepCopy(itemId);
        if (proto == null || quantity <= 0) return;
        List<Item> stacks = new ArrayList<>(chr.getInventoryByType(proto.getInvType()).getItems());
        int left = quantity;
        for (Item stack : stacks) {
            if (left <= 0) break;
            if (stack.getItemId() != itemId) continue;
            int take = Math.min(left, stack.getQuantity());
            chr.consumeItem(stack, take);
            left -= take;
        }
    }
}
