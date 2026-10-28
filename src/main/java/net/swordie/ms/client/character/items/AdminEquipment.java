package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.enums.EnchantStat;
import net.swordie.ms.enums.EquipBaseStat;
import net.swordie.ms.enums.FlameStat;
import net.swordie.ms.enums.ItemGrade;
import net.swordie.ms.enums.SpellTraceScrollType;
import net.swordie.ms.loaders.ItemData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** GM test gear: fresh copies only, existing upgrade rules and compatible data-backed potentials. */
public final class AdminEquipment {
    private AdminEquipment() {}

    public static String group(short job) {
        if (JobConstants.isXenon(job)) return "pirate";
        if (JobConstants.isMageEquipJob(job)) return "mage";
        if (JobConstants.isArcherEquipJob(job) || JobConstants.isKain(job)) return "archer";
        if (JobConstants.isThiefEquipJob(job)) return "thief";
        if (JobConstants.isPirateEquipJob(job)) return "pirate";
        if (JobConstants.isWarriorEquipJob(job)) return "warrior";
        return null;
    }

    public static BaseStat mainStat(short job) {
        if (JobConstants.isDemonAvenger(job)) return BaseStat.mhpR;
        if (JobConstants.isXenon(job)) return BaseStat.allStatR;
        String group = group(job);
        if (group == null) throw new IllegalArgumentException("無法判斷目前職業的裝備系別。");
        return switch (group) {
            case "mage" -> BaseStat.intR;
            case "archer" -> BaseStat.dexR;
            case "thief" -> BaseStat.lukR;
            case "pirate" -> JobConstants.isCorsair(job) || JobConstants.isMechanic(job)
                    || JobConstants.isAngelicBuster(job) || JobConstants.isJett(job) || JobConstants.isMoXuan(job)
                    ? BaseStat.dexR : BaseStat.strR;
            default -> BaseStat.strR;
        };
    }

    /** Validate the entire set and aggregate space before preparing anything for delivery. No inventory writes. */
    public static List<Equip> prepare(Char chr, int[] itemIds) {
        checkGm(chr);
        if (itemIds == null || itemIds.length == 0 || itemIds.length > 64)
            throw new IllegalArgumentException("裝備套組件數不正確，已取消發放。");
        requireSpace(chr, itemIds.length);
        BaseStat stat = mainStat(chr.getJob());
        List<Equip> result = new ArrayList<>();
        for (int id : itemIds) {
            // 282: removed from the client / no server data -> refuse the whole set (plan B1, no partial grant)
            net.swordie.ms.world.tms282.items.Tms282ItemAvailability.requireGrantable(id);
            Equip template = ItemData.getEquipById(id);
            if (template == null || template.isCash()) throw new IllegalArgumentException("裝備資料不存在，或不屬於戰鬥裝備：" + id);
            if (template.getLevelUpType() != 0)
                throw new IllegalArgumentException("此裝備的成長資料尚未確認，已取消發放：" + id);
            Equip equip = ItemData.getEquipDeepCopyFromID(id, false);
            if (equip == null || equip.getId() != 0) throw new IllegalArgumentException("無法建立全新裝備，已取消發放：" + id);
            maximizeScrolls(chr, equip, stat);
            if (ItemConstants.isUpgradable(id) && !ItemConstants.isDemonShield(id) && !ItemConstants.isSoulShield(id)
                    && !equip.getHyperUpgradeStats().isEmpty())
                equip.setChuc((short) GameConstants.getMaxStars(equip), true);
            maximizeFlame(equip, stat);
            if (ItemConstants.canEquipHavePotential(equip)) {
                boolean attackEquip = ItemConstants.isWeapon(id) || ItemConstants.isSecondary(id)
                        || ItemConstants.isShield(id) || ItemConstants.isEmblem(id);
                BaseStat potential = attackEquip ? stat == BaseStat.intR ? BaseStat.madR : BaseStat.padR : stat;
                fillPotentials(equip, potential, false);
                fillPotentials(equip, potential, true);
            }
            result.add(equip);
        }
        return List.copyOf(result);
    }

    /** Repeat the GM/aggregate space check immediately before handing new copies to the existing inventory API. */
    public static void grant(Char chr, List<Equip> equips) {
        checkGm(chr);
        synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) { synchronized (chr) { synchronized (chr.getEquipInventory()) { // multiplayer-0930 lock order
            requireSpace(chr, equips.size());
            for (Equip equip : equips)
                if (equip == null || equip.getId() != 0) throw new IllegalArgumentException("只允許發放全新準備的裝備，不會更動已有裝備。");
            for (Equip equip : equips) chr.addItemToInventory(equip);
        } } }
    }

    /**
     * GM tool (裝備系統/特殊潛能.py): legendary main + bonus potentials picked from the item's legal loaded option pool
     * for the character's main stat. Refuses equips that cannot carry potentials (androids, badges, cash...) and
     * equips that already have one. Mutates the given inventory equip; the caller refreshes the slot.
     */
    public static void applyLegendaryPotentials(Char chr, Equip equip) {
        checkGm(chr);
        if (equip == null) throw new IllegalArgumentException("找不到裝備。");
        net.swordie.ms.world.tms282.items.Tms282ItemAvailability.requireGrantable(equip.getItemId());
        if (ItemConstants.isAndroid(equip.getItemId()) || ItemConstants.isMechanicalHeart(equip.getItemId())
                || ItemConstants.isBadge(equip.getItemId()) || !ItemConstants.canEquipHavePotential(equip))
            throw new IllegalArgumentException("此裝備不能擁有潛能（機器人、心臟、胸章、現金裝備等），未做任何變更。");
        if (equip.hasPotential())
            throw new IllegalArgumentException("此裝備已經有潛能，未做任何變更。");
        BaseStat stat = mainStat(chr.getJob());
        int id = equip.getItemId();
        boolean attackEquip = ItemConstants.isWeapon(id) || ItemConstants.isSecondary(id)
                || ItemConstants.isShield(id) || ItemConstants.isEmblem(id);
        BaseStat potential = attackEquip ? stat == BaseStat.intR ? BaseStat.madR : BaseStat.padR : stat;
        int[] base = equip.getOptionBase().clone(), bonus = equip.getOptionBonus().clone();
        try {
            fillPotentials(equip, potential, false);
            fillPotentials(equip, potential, true);
        } catch (IllegalArgumentException e) {
            for (int i = 0; i < 3; i++) { equip.setOption(i, base[i], false); equip.setOption(i, bonus[i], true); }
            throw e;
        }
    }

    private static void checkGm(Char chr) {
        if (chr == null || !chr.isGM()) throw new IllegalArgumentException("此功能僅限管理員使用。");
    }
    private static void requireSpace(Char chr, int amount) {
        if (chr.getEquipInventory() == null || chr.getEquipInventory().getEmptySlots() < amount)
            throw new IllegalArgumentException("裝備欄須預留 " + amount + " 格，請整理後再試。");
    }

    private static void fillPotentials(Equip equip, BaseStat stat, boolean bonus) {
        // Only a temporary grade seed; each final ID is selected from the item's legal loaded option pool.
        equip.setOption(0, bonus ? 42001 : 40001, bonus);
        for (int line = 0; line < 3; line++) {
            ItemOption best = ItemConstants.getOptionsByEquip(equip, bonus, line,
                            ItemConstants.SYSTEM_DEFAULT_CUBE_INDICATOR, 2).stream()
                    .filter(o -> o.hasMatchingGrade(ItemGrade.Legendary.getVal()) && o.isBonus() == bonus)
                    .filter(o -> optionValue(o, equip, stat) > 0)
                    .max(Comparator.comparingDouble((ItemOption o) -> optionValue(o, equip, stat))
                            .thenComparingInt(o -> -o.getId()))
                    .orElseThrow(() -> new IllegalArgumentException("此部位沒有相容的職業向傳說潛能，已取消發放：" + equip.getItemId()));
            equip.setOption(line, best.getId(), bonus);
        }
    }

    private static double optionValue(ItemOption option, Equip equip, BaseStat stat) {
        var values = option.getStatValuesByLevel(option.getOptionLevel(equip.getrLevel() + equip.getiIncReq()));
        if (stat == BaseStat.allStatR && values.getOrDefault(stat, 0D) == 0)
            return Math.min(Math.min(values.getOrDefault(BaseStat.strR, 0D), values.getOrDefault(BaseStat.dexR, 0D)),
                    Math.min(values.getOrDefault(BaseStat.intR, 0D), values.getOrDefault(BaseStat.lukR, 0D)));
        return values.getOrDefault(stat, 0D);
    }

    private static void maximizeScrolls(Char chr, Equip equip, BaseStat main) {
        if (equip.getTuc() <= 0) return;
        ScrollUpgradeInfo best = ItemConstants.getScrollUpgradeInfosByEquip(chr, equip, false).stream()
                .filter(s -> s.getType() == SpellTraceScrollType.Normal)
                .max(Comparator.comparingDouble(s -> scrollScore(s.getStats(), main)))
                .orElseThrow(() -> new IllegalArgumentException("此裝備沒有可用的合法卷軸強化：" + equip.getItemId()));
        if (scrollScore(best.getStats(), main) <= 0) throw new IllegalArgumentException("此裝備沒有符合職業屬性的卷軸強化：" + equip.getItemId());
        if (ItemConstants.canEquipGoldHammer(equip)) {
            int extra = equip.getIUCMax() - equip.getIuc();
            equip.addStat(EquipBaseStat.iuc, extra);
            equip.addStat(EquipBaseStat.tuc, extra);
        }
        int slots = equip.getTuc();
        // Apply only the values of an existing legal spell-trace choice, with every attempt successful for GM gear.
        for (var entry : best.getStats().entrySet()) equip.addStat(entry.getKey().getEquipBaseStat(), entry.getValue() * slots);
        equip.addStat(EquipBaseStat.cuc, slots);
        equip.setTuc((short) 0);
    }

    private static double scrollScore(Map<EnchantStat, Integer> stats, BaseStat main) {
        double attack = stats.getOrDefault(main == BaseStat.intR ? EnchantStat.MAD : EnchantStat.PAD, 0);
        double value = switch (main) {
            case strR -> stats.getOrDefault(EnchantStat.STR, 0);
            case dexR -> stats.getOrDefault(EnchantStat.DEX, 0);
            case intR -> stats.getOrDefault(EnchantStat.INT, 0);
            case lukR -> stats.getOrDefault(EnchantStat.LUK, 0);
            case mhpR -> stats.getOrDefault(EnchantStat.MHP, 0) / 10D + stats.getOrDefault(EnchantStat.STR, 0) / 100D;
            case allStatR -> stats.getOrDefault(EnchantStat.STR, 0) + stats.getOrDefault(EnchantStat.DEX, 0) + stats.getOrDefault(EnchantStat.LUK, 0);
            default -> 0;
        };
        return attack * 10_000 + value;
    }

    private static void maximizeFlame(Equip equip, BaseStat main) {
        if (!ItemConstants.canEquipHaveFlame(equip)) return;
        FlameStat[] lines;
        if (ItemConstants.isWeapon(equip.getItemId())) {
            lines = new FlameStat[]{main == BaseStat.intR ? FlameStat.MagicAttack : FlameStat.Attack,
                    FlameStat.BossDamage, FlameStat.Damage, main == BaseStat.mhpR ? FlameStat.MaxHP : FlameStat.AllStats};
        } else {
            lines = switch (main) {
                case strR -> new FlameStat[]{FlameStat.STR, FlameStat.AllStats, FlameStat.STRDEX, FlameStat.STRLUK};
                case dexR -> new FlameStat[]{FlameStat.DEX, FlameStat.AllStats, FlameStat.STRDEX, FlameStat.DEXLUK};
                case intR -> new FlameStat[]{FlameStat.INT, FlameStat.AllStats, FlameStat.STRINT, FlameStat.INTLUK};
                case lukR -> new FlameStat[]{FlameStat.LUK, FlameStat.AllStats, FlameStat.STRLUK, FlameStat.DEXLUK};
                case mhpR -> new FlameStat[]{FlameStat.MaxHP, FlameStat.Attack, FlameStat.STR, FlameStat.AllStats};
                case allStatR -> new FlameStat[]{FlameStat.AllStats, FlameStat.STRDEX, FlameStat.STRLUK, FlameStat.DEXLUK};
                default -> throw new IllegalArgumentException("無法判斷此職業適用的火花屬性。");
            };
        }
        int tier = equip.isBossReward() ? 7 : 5, count = equip.isBossReward() ? 4 : 3;
        long encoded = 0, multiplier = 1;
        for (int i = 0; i < count; i++) {
            encoded += (lines[i].getExGrade() * 10L + tier) * multiplier;
            multiplier *= 1000;
        }
        equip.flame(encoded);
    }
}
