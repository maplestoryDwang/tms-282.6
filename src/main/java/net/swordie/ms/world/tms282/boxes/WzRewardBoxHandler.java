package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.ItemRewardInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * W1 (research/tms2823/item-usability-20260928/boxes/README.md): consume boxes whose client data has a
 * {@code reward/} node (ItemData reads item/count/prob/period/meso into {@link ItemInfo#getItemRewardInfos()}),
 * e.g. the 2028xxx boxes the 282 client sends as CP0148. One draw weighted by {@code prob} among the entries whose
 * item still exists in 282 (or that give meso); {@code period} is in minutes as in 265
 * {@code ItemInfo.getRandomReward}. Bag space / meso cap are checked before the box is consumed.
 * <p>
 * Not handled here: cash items (5xxxxxx keep the 553 / raffle paths), items with spec stats (potions that also list
 * a reward keep their effect), items with a written item script (the script wins). The WZ {@code worldMsg} flag is
 * not read by ItemData, so no world notice is sent.
 */
public final class WzRewardBoxHandler implements BoxUseHandler {
    private WzRewardBoxHandler() {
    }

    public static WzRewardBoxHandler load() {
        return new WzRewardBoxHandler();
    }

    @Override
    public String name() {
        return "wz-reward";
    }

    /** The drawable reward/ entries of {@code itemId} (positive finite prob, an item or meso), or empty. */
    static List<ItemRewardInfo> entries(int itemId) {
        if (itemId <= 0 || itemId / 1_000_000 == 5) {
            return List.of();
        }
        ItemInfo ii = ItemData.getItemInfoByID(itemId);
        if (ii == null || ii.getItemRewardInfos() == null || ii.getItemRewardInfos().isEmpty()
                || (ii.getSpecStats() != null && !ii.getSpecStats().isEmpty())) {
            return List.of();
        }
        List<ItemRewardInfo> out = new ArrayList<>();
        for (ItemRewardInfo r : ii.getItemRewardInfos()) {
            if (r.getProb() > 0 && Double.isFinite(r.getProb()) && (r.getItemID() > 0 || r.getMeso() > 0)) {
                out.add(r);
            }
        }
        // stable order (the set is a HashSet) so a draw does not depend on iteration order between runs
        out.sort((a, b) -> a.getItemID() != b.getItemID() ? Integer.compare(a.getItemID(), b.getItemID())
                : Integer.compare(a.getMeso(), b.getMeso()));
        return out;
    }

    @Override
    public boolean handles(int itemId) {
        return !entries(itemId).isEmpty() && !BoxSupport.hasItemScript(itemId);
    }

    @Override
    public void use(Char chr, Item box) {
        ItemRewardInfo pick = draw(entries(box.getItemId()));
        if (pick == null) {
            BoxSupport.refuseNoData(chr);
            return;
        }
        if (pick.getItemID() <= 0) {
            BoxSupport.giveAndConsume(chr, box, List.of(), pick.getMeso());
            return;
        }
        Item reward = BoxSupport.createItemMinutes(pick.getItemID(), Math.max(1, pick.getCount()), pick.getPeriod());
        if (reward == null) {
            BoxSupport.refuseNoData(chr);
            return;
        }
        BoxSupport.giveAndConsume(chr, box, List.of(reward), pick.getMeso() > 0 ? pick.getMeso() : 0);
    }

    /** Weighted by prob among the entries that can be given (meso, or an item that exists in 282). */
    static ItemRewardInfo draw(List<ItemRewardInfo> entries) {
        List<ItemRewardInfo> ok = new ArrayList<>();
        double total = 0;
        for (ItemRewardInfo r : entries) {
            if (r.getItemID() > 0 ? BoxSupport.available(r.getItemID()) : r.getMeso() > 0) {
                ok.add(r);
                total += r.getProb();
            }
        }
        if (ok.isEmpty() || !(total > 0)) {
            return null;
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        for (ItemRewardInfo r : ok) {
            roll -= r.getProb();
            if (roll < 0) {
                return r;
            }
        }
        return ok.get(ok.size() - 1);
    }
}
