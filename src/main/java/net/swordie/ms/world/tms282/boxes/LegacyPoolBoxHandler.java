package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.constants.ItemConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The hardcoded 265 pools of {@code ItemHandler.handleUserLotteryItemUseRequest} (USER_LOTTERY_ITEM_USE, which the
 * 282 client never sends for these ids: they come as CP0148): sealed boxes 2028154-56/61-65, Alicia ring boxes
 * 2028263-72 and guild reward boxes 2028338/42-46, with the same uniform draw and quantities as the 265 code. Only
 * reached for ids without a WZ {@code reward/} node (the WZ handler is registered first); rewards that no longer
 * exist in 282 are left out of the draw.
 */
public final class LegacyPoolBoxHandler implements BoxUseHandler {
    private final Map<Integer, Integer[]> pools = new HashMap<>();

    private LegacyPoolBoxHandler() {
        pools.put(2028154, ItemConstants.sealedBox_Hat);
        pools.put(2028155, ItemConstants.sealedBox_Overall_Top_Bottom);
        pools.put(2028156, ItemConstants.sealedBox_Shoes_Gloves);
        pools.put(2028161, ItemConstants.sealedBox_Weapon);
        pools.put(2028162, ItemConstants.sealedBox_Hat_Chaos);
        pools.put(2028163, ItemConstants.sealedBox_Overall_Top_Bottom_Chaos);
        pools.put(2028164, ItemConstants.sealedBox_Shoes_Gloves_Chaos);
        pools.put(2028165, ItemConstants.sealedBox_Weapon_Chaos);
        for (int id = 2028263; id <= 2028272; id++) {
            pools.put(id, id >= 2028268 ? ItemConstants.AliciaRingBox_6thRank : ItemConstants.AliciaRingBox_2ndRank);
        }
        pools.put(2028338, ItemConstants.guildReward_TierS);
        pools.put(2028342, ItemConstants.guildReward_TierA);
        pools.put(2028343, ItemConstants.guildReward_TierB);
        pools.put(2028344, ItemConstants.guildReward_TierC);
        pools.put(2028345, ItemConstants.guildReward_TierD_F);
        pools.put(2028346, ItemConstants.guildReward_TierD_F);
    }

    public static LegacyPoolBoxHandler load() {
        return new LegacyPoolBoxHandler();
    }

    @Override
    public String name() {
        return "legacy-265-pools(" + pools.size() + " boxes)";
    }

    public Map<Integer, Integer[]> pools() {
        return Map.copyOf(pools);
    }

    @Override
    public boolean handles(int itemId) {
        return pools.containsKey(itemId) && !BoxSupport.hasItemScript(itemId);
    }

    @Override
    public void use(Char chr, Item box) {
        int boxId = box.getItemId();
        List<Integer> ok = new ArrayList<>();
        for (Integer id : pools.get(boxId)) {
            if (id != null && id > 1 && BoxSupport.available(id)) {
                ok.add(id);
            }
        }
        if (ok.isEmpty()) {
            BoxSupport.refuseNoData(chr);
            return;
        }
        int rewardId = ok.get(ThreadLocalRandom.current().nextInt(ok.size()));
        Item reward = BoxSupport.createItem(rewardId, quantity(boxId, rewardId), 0);
        if (reward == null) {
            BoxSupport.refuseNoData(chr);
            return;
        }
        BoxSupport.giveAndConsume(chr, box, List.of(reward), 0);
    }

    /** The 265 quantities (handleUserLotteryItemUseRequest). */
    static int quantity(int boxId, int rewardId) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        if (boxId >= 2028263 && boxId <= 2028272) {
            return rewardId == 4001832 ? r.nextInt(100, 501) : (rewardId == 2432468 || rewardId == 2432503) ? r.nextInt(1, 6) : 1;
        }
        return switch (boxId) {
            case 2028338 -> r.nextInt(1, 3);
            case 2028342, 2028346 -> r.nextInt(1, 4);
            case 2028345 -> r.nextInt(5, 8);
            default -> 1;
        };
    }
}
