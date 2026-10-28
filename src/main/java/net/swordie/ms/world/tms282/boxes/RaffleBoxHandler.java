package net.swordie.ms.world.tms282.boxes;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.InvType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Raffle boxes and incubators (5060xxx, 5222xxx, 5533xxx, 5680946 ...), data from {@code raffle/raffle.tsv}
 * (TMS279 raffle.csv + MetaMS 273 Incubator) and optional per-box rules from {@code raffle/box-rules.tsv}.
 * The 282 incubator result packet (INCUBATOR_RESULT) has no 282 opcode mapping, so results are chat messages.
 */
public final class RaffleBoxHandler implements BoxUseHandler {
    public record Reward(int itemId, int amount, int periodDays, boolean notice) {
    }

    public record Rule(int[] keyItems, int bonusItem, int bonusQty, boolean consumeBox, boolean genderFilter) {
        static final Rule DEFAULT = new Rule(new int[0], 0, 0, true, true);
    }

    private final Map<Integer, WeightedPool<Reward>> pools;
    private final Map<Integer, Rule> rules;

    private RaffleBoxHandler(Map<Integer, WeightedPool<Reward>> pools, Map<Integer, Rule> rules) {
        this.pools = Map.copyOf(pools);
        this.rules = Map.copyOf(rules);
    }

    public static RaffleBoxHandler load() {
        Path dir = BoxSupport.dataDir().resolve("raffle");
        Map<Integer, WeightedPool.Builder<Reward>> builders = new HashMap<>();
        for (String[] r : BoxSupport.readTsv(dir.resolve("raffle.tsv"), 6)) {
            int box = Integer.parseInt(r[0]);
            builders.computeIfAbsent(box, k -> WeightedPool.builder()).add(
                    new Reward(Integer.parseInt(r[1]), BoxSupport.intOr(r[2], 1), BoxSupport.intOr(r[4], 0),
                            "1".equals(r[5])),
                    Integer.parseInt(r[3]));
        }
        Map<Integer, WeightedPool<Reward>> pools = new HashMap<>();
        builders.forEach((k, b) -> pools.put(k, b.build()));
        Map<Integer, Rule> rules = new HashMap<>();
        Path rulesFile = dir.resolve("box-rules.tsv");
        if (Files.exists(rulesFile)) {
            for (String[] r : BoxSupport.readTsv(rulesFile, 6)) {
                List<Integer> keys = new ArrayList<>();
                for (String k : r[1].split("\\|")) {
                    if (!k.isBlank()) {
                        keys.add(Integer.parseInt(k.trim()));
                    }
                }
                rules.put(Integer.parseInt(r[0]), new Rule(keys.stream().mapToInt(Integer::intValue).toArray(),
                        BoxSupport.intOr(r[2], 0), BoxSupport.intOr(r[3], 0),
                        !"0".equals(r[4]), !"0".equals(r[5])));
            }
        }
        return new RaffleBoxHandler(pools, rules);
    }

    @Override
    public String name() {
        return "raffle(" + pools.size() + " boxes)";
    }

    @Override
    public boolean handles(int itemId) {
        return pools.containsKey(itemId);
    }

    public Map<Integer, WeightedPool<Reward>> pools() {
        return pools;
    }

    @Override
    public void use(Char chr, Item box) {
        int boxId = box.getItemId();
        Rule rule = rules.getOrDefault(boxId, Rule.DEFAULT);
        Item key = null;
        if (rule.keyItems().length > 0) {
            for (int keyId : rule.keyItems()) {
                Item k = findItem(chr, keyId);
                if (k != null && k.getQuantity() > 0) {
                    key = k;
                    break;
                }
            }
            if (key == null) {
                chr.chatMessage(ChatType.SystemNotice, String.format("需要 %s 才能使用 %s。",
                        BoxSupport.itemName(rule.keyItems()[0]), BoxSupport.itemName(boxId)));
                return;
            }
        }
        Reward pick = pools.get(boxId).pick(r -> BoxSupport.available(r.itemId())
                && (!rule.genderFilter() || BoxSupport.genderAllows(chr, r.itemId())));
        if (pick == null) {
            chr.chatMessage(ChatType.SystemNotice, "獎勵資料尚未就緒，未消耗此道具。");
            return;
        }
        Item reward = BoxSupport.createItem(pick.itemId(), pick.amount(), pick.periodDays());
        if (reward == null) {
            chr.chatMessage(ChatType.SystemNotice, "獎勵資料尚未就緒，未消耗此道具。");
            return;
        }
        List<Item> gains = new ArrayList<>();
        gains.add(reward);
        if (rule.bonusItem() > 0 && rule.bonusQty() > 0 && BoxSupport.available(rule.bonusItem())) {
            Item bonus = BoxSupport.createItem(rule.bonusItem(), rule.bonusQty(), 0);
            if (bonus != null) {
                gains.add(bonus);
            }
        }
        if (!chr.canHold(gains)) {
            chr.chatMessage(ChatType.SystemNotice, "請先保留足夠的背包空間，未消耗此道具。");
            return;
        }
        if (key != null) {
            chr.consumeItem(key, 1);
        }
        if (rule.consumeBox()) {
            chr.consumeItem(box, 1);
        }
        for (Item gain : gains) {
            chr.addItemToInventory(gain);
            chr.chatMessage(ChatType.GameDesc, String.format("從 %s 獲得了 %s。", BoxSupport.itemName(boxId), BoxSupport.describe(gain)));
        }
        if (pick.notice()) {
            BoxSupport.announce(chr, boxId, reward);
        }
    }

    private static Item findItem(Char chr, int itemId) {
        for (InvType t : new InvType[]{InvType.ETC, InvType.CONSUME, InvType.CASH}) {
            var inv = chr.getInventoryByType(t);
            Item it = inv == null ? null : inv.getItemByItemID(itemId);
            if (it != null) {
                return it;
            }
        }
        return null;
    }
}
