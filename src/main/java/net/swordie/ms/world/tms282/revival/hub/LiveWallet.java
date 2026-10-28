package net.swordie.ms.world.tms282.revival.hub;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.lifecycle.OnlineCharacterSaver;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.tms282.activity.ActivityRewards;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/** {@link HubExchange.Wallet} of a live character: EconomyLocks.packet + account monitor, as ActivityRewards.grant. */
public final class LiveWallet implements HubExchange.Wallet {
    private final Char chr;

    public LiveWallet(Char chr) {
        this.chr = chr;
    }

    @Override
    public HubExchange.Result run(Supplier<HubExchange.Result> body) {
        HubExchange.Result[] out = {HubExchange.Result.FAILED};
        try {
            EconomyLocks.packet(chr, () -> {
                if (!ActivityRewards.isOnline(chr)) {
                    out[0] = HubExchange.Result.OFFLINE;
                    return null;
                }
                synchronized (chr.getAccount()) {
                    Tms282VMatrixRuntime.requireEconomicAccess(chr);
                    out[0] = body.get();
                }
                return null;
            });
        } catch (RuntimeException e) {
            System.out.println("[Revival] exchange failed character=" + chr.getId() + " " + e.getClass().getSimpleName());
            out[0] = HubExchange.Result.FAILED;
        }
        return out[0];
    }

    private Inventory inventoryOf(int itemId) {
        Item proto = ItemData.getItemDeepCopy(itemId);
        return proto == null ? null : chr.getInventoryByType(proto.getInvType());
    }

    @Override
    public int count(int itemId) {
        Inventory inv = inventoryOf(itemId);
        if (inv == null) return 0;
        long n = inv.getItems().stream().filter(i -> i.getItemId() == itemId).mapToLong(Item::getQuantity).sum();
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    @Override
    public boolean grantable(int itemId) {
        return Tms282ItemAvailability.isGrantable(itemId) && ItemData.getItemDeepCopy(itemId) != null;
    }

    private List<Item> prepare(int itemId, int quantity) {
        List<Item> out = new ArrayList<>();
        Item template = ItemData.getItemDeepCopy(itemId);
        if (template == null) return out;
        if (ItemConstants.isEquip(itemId)) {
            for (int i = 0; i < quantity; i++) {
                Item e = template.deepCopy();
                e.setQuantity(1);
                e.setDateExpire(FileTime.MAX_TIME());
                out.add(e);
            }
        } else {
            template.setQuantity(quantity);
            template.setDateExpire(FileTime.MAX_TIME());
            out.add(template);
        }
        return out;
    }

    @Override
    public boolean canHold(int itemId, int quantity) {
        List<Item> items = prepare(itemId, quantity);
        return !items.isEmpty() && chr.canHold(items);
    }

    @Override
    public boolean take(int itemId, int quantity) {
        Inventory inv = inventoryOf(itemId);
        if (inv == null || count(itemId) < quantity) return false;
        List<Item> stacks = new ArrayList<>(inv.getItems().stream().filter(i -> i.getItemId() == itemId).toList());
        stacks.sort(Comparator.comparingInt(Item::getQuantity));
        int left = quantity;
        for (Item stack : stacks) {
            if (left <= 0) break;
            int take = Math.min(left, stack.getQuantity());
            chr.consumeItem(stack, take);
            left -= take;
        }
        return left == 0;
    }

    @Override
    public void give(int itemId, int quantity) {
        for (Item item : prepare(itemId, quantity)) chr.addItemToInventory(item);
    }

    @Override
    public boolean commit() {
        return OnlineCharacterSaver.save(chr) == OnlineCharacterSaver.Result.SAVED;
    }
}
