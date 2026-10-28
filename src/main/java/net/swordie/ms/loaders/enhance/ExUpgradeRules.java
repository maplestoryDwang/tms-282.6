package net.swordie.ms.loaders.enhance;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Tms282FlameRules;
import net.swordie.ms.loaders.enhance.EnhanceTables.Weighted;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/** Settings layered over {@link Tms282FlameRules} (createType rules in data/protocol/tms282.3/flame-rules.tsv):
 * <ul>
 *   <li>ex-upgrade-settings.tsv {@code disable_boss_reward} (TMS 279 ex-upgrade.toml): boss-reward equipment rolls
 *       like normal equipment instead of 4 lines / tiers +2.</li>
 *   <li>flame-items.tsv: absolute tier weights (273 ExGradeItem) keyed by flame item id, or by {@code ct<createType>}
 *       for every flame of that createType (item rows win). Shipped: ct20 覺醒 and ct21 噴出岩 use the strong 273
 *       tiers; the per-item rows are commented out.</li>
 * </ul>
 * With neither set, {@link #apply} is exactly {@link Tms282FlameRules#apply}. The 279 type 0 group (drop-time
 * flames) is recorded in the settings file only: the drop path (ItemData) does not roll flame lines. */
public final class ExUpgradeRules {
    public static final int MAX_TIER = 7;

    private record Data(boolean disableBossReward, Map<Integer, Weighted> itemTiers, Map<Integer, Weighted> typeTiers) { }

    private static volatile Data data = load();

    private ExUpgradeRules() { }

    public static synchronized void reload() {
        data = load();
    }

    public static boolean disableBossReward() {
        return data.disableBossReward;
    }

    /** Absolute tier weights for one flame item, or null. */
    public static Weighted itemTiers(int flameItemId) {
        return data.itemTiers.get(flameItemId);
    }

    /** Rolls {@code rule} (plus the settings above) onto {@code equip}; {@code random} returns uniform [0, n). */
    public static short apply(Equip equip, int flameItemId, Tms282FlameRules.Rule rule, IntUnaryOperator random) {
        return apply(equip, flameItemId, -1, rule, random);
    }

    public static short apply(Equip equip, int flameItemId, int createType, Tms282FlameRules.Rule rule, IntUnaryOperator random) {
        Data d = data;
        Weighted perItem = d.itemTiers.get(flameItemId);
        if (perItem == null) perItem = d.typeTiers.get(createType);
        boolean boss = equip.isBossReward() && !d.disableBossReward;
        if (perItem == null && boss == equip.isBossReward()) {
            return Tms282FlameRules.apply(equip, rule, random);
        }
        int lines = boss ? Tms282FlameRules.BOSS_LINES : rule.lines().pick(random.applyAsInt(rule.lines().total()));
        int shift = boss ? Tms282FlameRules.BOSS_TIER_SHIFT : 0;
        Weighted tiers = perItem;
        return equip.flameTms282(lines, () -> {
            int tier = tiers != null
                    ? tiers.keys()[indexOf(tiers, random.applyAsInt(tiers.total()))]
                    : rule.tiers().pick(random.applyAsInt(rule.tiers().total()));
            return Math.min(MAX_TIER, tier + shift);
        });
    }

    private static int indexOf(Weighted w, int roll) {
        for (int i = 0; i < w.cumulative().length; i++) if (roll < w.cumulative()[i]) return i;
        throw new IllegalArgumentException("roll out of range");
    }

    private static Data load() {
        boolean disableBoss = false;
        List<String[]> rows = EnhanceTables.rows("ex-upgrade-settings.tsv");
        if (rows != null) for (String[] c : rows) {
            if (c.length >= 2 && "disable_boss_reward".equals(c[0])) disableBoss = Boolean.parseBoolean(c[1].trim());
        }
        Map<Integer, Weighted> items = new HashMap<>(), types = new HashMap<>();
        rows = EnhanceTables.rows("flame-items.tsv");
        if (rows != null) for (String[] c : rows) {
            try {
                Weighted w = Weighted.parse(c[1]);
                for (int tier : w.keys()) if (tier < 1 || tier > MAX_TIER) throw new IllegalArgumentException("tier " + tier);
                String key = c[0].trim();
                if (key.startsWith("ct")) types.put(Integer.parseInt(key.substring(2)), w);
                else items.put(Integer.parseInt(key), w);
            } catch (RuntimeException e) {
                EnhanceTables.warn("flame-items.tsv", String.join("|", c), e);
            }
        }
        return new Data(disableBoss, Map.copyOf(items), Map.copyOf(types));
    }
}
