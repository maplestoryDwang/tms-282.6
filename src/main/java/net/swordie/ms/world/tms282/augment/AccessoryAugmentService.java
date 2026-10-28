package net.swordie.ms.world.tms282.augment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.tms282.urus.InventoryCost;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Special-skill ring grinding with 生命研磨石 (2539001), data from MetaMS 273 {@code AccessoryAugment.json}
 * ({@code data/resources/tms282/augment/accessory-augment.json}). The player picks a ring (worn or in the equip tab)
 * whose item id is a rule's {@code targetItemId} at {@code baseLevel}, then how many stones to spend; stones and
 * mesos are always consumed, and on success the same ring object is raised to {@code nextLevel} in place, so its
 * potential, flames, stars and ownership flags are preserved. Worn rings get their equipment skills refreshed.
 */
public final class AccessoryAugmentService {
    public enum Status { SUCCESS, FAILED, NO_STONE, NO_MESO, INVALID }

    public record Result(Status status, String message) {
    }

    private static volatile AccessoryAugmentService instance;

    private final List<AccessoryAugmentRule> rules;

    private AccessoryAugmentService(List<AccessoryAugmentRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static AccessoryAugmentService get() {
        AccessoryAugmentService s = instance;
        if (s == null) {
            synchronized (AccessoryAugmentService.class) {
                if (instance == null) instance = load();
                s = instance;
            }
        }
        return s;
    }

    public static synchronized void reload() {
        instance = load();
    }

    private static AccessoryAugmentService load() {
        Path path = Path.of(ServerConstants.RESOURCES_DIR, "tms282", "augment", "accessory-augment.json");
        try {
            List<AccessoryAugmentRule> rules = new ArrayList<>();
            for (JsonNode n : new ObjectMapper().readTree(path.toFile())) {
                rules.add(new AccessoryAugmentRule(n.path("reqItemId").asInt(), n.path("reqCount").asInt(1),
                        n.path("prob").asInt(), n.path("mesos").asLong(), n.path("targetItemId").asInt(),
                        n.path("baseLevel").asInt(), n.path("nextLevel").asInt()));
            }
            return new AccessoryAugmentService(rules);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
    }

    public boolean isStone(int itemId) {
        return rules.stream().anyMatch(r -> r.reqItemId() == itemId);
    }

    public static int levelOf(Equip equip) {
        return Math.max(1, equip.getItemLevel());
    }

    /** Stone options for a ring, ordered by stone count. */
    public List<AccessoryAugmentRule> options(Equip equip, int stoneId) {
        if (equip == null) return List.of();
        int level = levelOf(equip);
        return rules.stream()
                .filter(r -> r.reqItemId() == stoneId && r.targetItemId() == equip.getItemId() && r.baseLevel() == level)
                .sorted(Comparator.comparingInt(AccessoryAugmentRule::reqCount))
                .toList();
    }

    /** Rings of the character (worn first, then equip tab) that can be ground with {@code stoneId}. */
    public List<Equip> candidates(Char chr, int stoneId) {
        List<Equip> result = new ArrayList<>();
        for (InvType type : new InvType[]{InvType.EQUIPPED, InvType.EQUIP}) {
            for (Item item : chr.getInventoryByType(type).getItems()) {
                if (item instanceof Equip equip && !options(equip, stoneId).isEmpty()) result.add(equip);
            }
        }
        return result;
    }

    public boolean isWorn(Char chr, Equip equip) {
        return chr.getEquippedInventory().getItems().stream().anyMatch(i -> i == equip);
    }

    private static boolean owns(Char chr, Equip equip) {
        return chr.getEquippedInventory().getItems().stream().anyMatch(i -> i == equip)
                || chr.getEquipInventory().getItems().stream().anyMatch(i -> i == equip);
    }

    public String candidateMenu(Char chr, List<Equip> candidates) {
        StringBuilder sb = new StringBuilder("請選擇要研磨的戒指：\r\n#b");
        for (int i = 0; i < candidates.size(); i++) {
            Equip e = candidates.get(i);
            sb.append("#L").append(i).append("##v").append(e.getItemId()).append("# #t").append(e.getItemId())
                    .append("# Lv.").append(levelOf(e)).append(isWorn(chr, e) ? " (穿戴中)" : "").append("#l\r\n");
        }
        return sb.toString();
    }

    public String optionMenu(Equip equip, List<AccessoryAugmentRule> options) {
        StringBuilder sb = new StringBuilder("#v").append(equip.getItemId()).append("# #t").append(equip.getItemId())
                .append("# Lv.").append(levelOf(equip)).append("\r\n請選擇要使用的研磨石數量：\r\n#b");
        for (int i = 0; i < options.size(); i++) {
            AccessoryAugmentRule r = options.get(i);
            sb.append("#L").append(i).append("#研磨石 x").append(r.reqCount()).append("，成功率 ").append(r.prob())
                    .append("%，楓幣 ").append(String.format("%,d", r.mesos())).append("，成功後 Lv.").append(r.nextLevel())
                    .append("#l\r\n");
        }
        return sb.toString();
    }

    /** Validates and performs one grind attempt. */
    public Result augment(Char chr, Equip equip, AccessoryAugmentRule rule) {
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { // multiplayer-0930 lock order
            if (equip == null || rule == null || !owns(chr, equip) || !options(equip, rule.reqItemId()).contains(rule)) {
                return new Result(Status.INVALID, "這個道具無法研磨，請重新確認。");
            }
            if (InventoryCost.count(chr, rule.reqItemId()) < rule.reqCount()) {
                return new Result(Status.NO_STONE, "#t" + rule.reqItemId() + "# 數量不足，需要 " + rule.reqCount() + " 個。");
            }
            if (chr.getMoney() < rule.mesos()) {
                return new Result(Status.NO_MESO, String.format("楓幣不足，需要 %,d 楓幣。", rule.mesos()));
            }
            InventoryCost.consume(chr, rule.reqItemId(), rule.reqCount());
            chr.deductMoney(rule.mesos());
            String name = StringData.getItemStringById(equip.getItemId());
            if (!Util.succeedProp(rule.prob())) {
                log(chr, equip, rule, false);
                return new Result(Status.FAILED, "研磨失敗了……#t" + equip.getItemId() + "# 維持 Lv." + rule.baseLevel() + "。");
            }
            Equip before = equip.deepCopy();
            equip.setItemLevel((short) rule.nextLevel());
            equip.updateToChar(chr);
            chr.refreshEquipmentSkillsAfterChange(before, equip);
            log(chr, equip, rule, true);
            return new Result(Status.SUCCESS, "研磨成功！" + (name == null ? "戒指" : name) + " 已成長至 Lv." + rule.nextLevel() + "。");
        } }
    }

    private static void log(Char chr, Equip equip, AccessoryAugmentRule rule, boolean success) {
        DataPrinter.send(DataPrinter.ITEM, String.format("飾品研磨：%s(%d) 戒指 %d(sn %d) Lv.%d->%d 石 %d x%d 楓幣 %d %s。",
                chr.getName(), chr.getId(), equip.getItemId(), equip.getId(), rule.baseLevel(), rule.nextLevel(),
                rule.reqItemId(), rule.reqCount(), rule.mesos(), success ? "成功" : "失敗"), true);
    }
}
