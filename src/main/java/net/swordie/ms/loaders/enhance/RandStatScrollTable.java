package net.swordie.ms.loaders.enhance;

import net.swordie.ms.enums.EquipBaseStat;
import net.swordie.ms.enums.ScrollStat;
import net.swordie.ms.loaders.enhance.EnhanceTables.Weighted;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Weighted stat deltas for random-stat scrolls (rand-stat-scroll.tsv from MetaMS 273 RandStatScroll.json).
 * Used by the chaos-scroll roll in Equip.applyScroll; stats without a row keep the old uniform roll.
 * The table also lists non-chaos random-range scrolls (e.g. 2048830, 2612082); those are not wired yet. */
public final class RandStatScrollTable {
    private static volatile Map<Integer, Map<EquipBaseStat, Weighted>> data = load();

    private RandStatScrollTable() { }

    public static synchronized void reload() {
        data = load();
    }

    /** Stat -> weighted delta for {@code scrollId}, or null when the scroll has no row. */
    public static Map<EquipBaseStat, Weighted> forScroll(int scrollId) {
        return data.get(scrollId);
    }

    private static Map<Integer, Map<EquipBaseStat, Weighted>> load() {
        Map<Integer, Map<EquipBaseStat, Weighted>> out = new HashMap<>();
        List<String[]> rows = EnhanceTables.rows("rand-stat-scroll.tsv");
        if (rows != null) for (String[] c : rows) {
            try {
                int scroll = Integer.parseInt(c[0]);
                EquipBaseStat stat = ScrollStat.valueOf(c[1]).getEquipStat();
                if (stat == null) throw new IllegalArgumentException("no equip stat for " + c[1]);
                out.computeIfAbsent(scroll, k -> new EnumMap<>(EquipBaseStat.class)).put(stat, Weighted.parse(c[2]));
            } catch (RuntimeException e) {
                EnhanceTables.warn("rand-stat-scroll.tsv", String.join("|", c), e);
            }
        }
        out.replaceAll((k, v) -> Map.copyOf(v));
        return Map.copyOf(out);
    }
}
