package net.swordie.ms.loaders.enhance;

import net.swordie.ms.enums.ItemGrade;
import net.swordie.ms.loaders.enhance.EnhanceTables.Weighted;

import java.util.*;
import java.util.function.Supplier;

/** Per-cube potential rates imported from MetaMS 273 (Common/Cube/&lt;id&gt;/{Grade,OptionLevel,Rare..Legendary}.json)
 * with TMS 279 cube-rate.toml forced option weights as override. Cubes without rows keep the fork's own rates in
 * ItemConstants / Equip.
 * <ul>
 *   <li>cube-grade.tsv: tier-up per 10000 for grade 1 rare, 2 epic, 3 unique.</li>
 *   <li>cube-option-level.tsv: per 10000 chance that line 1..3 keeps the item grade (else one grade lower).</li>
 *   <li>cube-options.tsv: option weights per grade and 273 equipment category. The category is chosen by overlap
 *       with the options the fork already considers legal for the item, so the item-type filters stay authoritative.</li>
 *   <li>cube-option-override.tsv: 279 forced weights per grade.</li>
 *   <li>cube-settings.tsv: {@code tier_up} / {@code option_level} (default false) switch the first two tables on.
 *       Both are off: the 273 tier-up rates are a private-server tweak (e.g. 97.7% rare-&gt;epic) and its
 *       OptionLevel values mirror those tier-up numbers, so this fork's own tier-up and prime-line logic stays.</li>
 * </ul>
 * Some 282 cube services pass a probability-policy id instead of the item id; they set the real cube with
 * {@link #withCube}. */
public final class CubeRateTable {
    private record Data(Map<Integer, int[]> tierUp, Map<Integer, int[][]> optionLevel,
                        Map<Long, List<Weighted>> categories, Map<Long, Weighted> overrides) { }

    private static final ThreadLocal<Integer> CONTEXT = new ThreadLocal<>();
    private static volatile Data data = load();

    private CubeRateTable() { }

    public static synchronized void reload() {
        data = load();
    }

    /** Runs {@code body} with {@code cubeId} as the cube whose table rows apply. */
    public static <T> T withCube(int cubeId, Supplier<T> body) {
        Integer previous = CONTEXT.get();
        CONTEXT.set(cubeId);
        try {
            return body.get();
        } finally {
            if (previous == null) CONTEXT.remove(); else CONTEXT.set(previous);
        }
    }

    /** The cube id whose rows apply: the {@link #withCube} context, else {@code cubeId}. */
    public static int effectiveCube(int cubeId) {
        Integer context = CONTEXT.get();
        return context != null ? context : cubeId;
    }

    public static boolean hasCube(int cubeId) {
        Data d = data;
        return d.tierUp.containsKey(cubeId) || d.optionLevel.containsKey(cubeId)
                || d.overrides.keySet().stream().anyMatch(k -> (int) (k >> 8) == cubeId)
                || d.categories.keySet().stream().anyMatch(k -> (int) (k >> 8) == cubeId);
    }

    /** 1 rare .. 4 legendary for any visible/hidden/bonus-hidden grade, 0 otherwise. */
    public static int gradeIndex(ItemGrade grade) {
        if (grade == null) return 0;
        return switch (grade) {
            case Rare, HiddenRare, RareBonusHidden -> 1;
            case Epic, HiddenEpic, EpicBonusHidden -> 2;
            case Unique, HiddenUnique, UniqueBonusHidden -> 3;
            case Legendary, HiddenLegendary, LegendaryBonusHidden -> 4;
            default -> 0;
        };
    }

    /** Tier-up chance per 10000 from {@code grade} (1..3), or -1 when the cube/grade has no row. */
    public static int tierUp(int cubeId, int grade) {
        int[] rates = data.tierUp.get(cubeId);
        return rates == null || grade < 1 || grade > 3 ? -1 : rates[grade - 1];
    }

    /** Per 10000 chance that {@code line} (1..3) keeps the item grade, or -1 when not configured. */
    public static int sameGradeChance(int cubeId, int grade, int line) {
        int[][] levels = data.optionLevel.get(cubeId);
        if (levels == null || grade < 1 || grade > 4 || line < 1 || line > 3) return -1;
        return levels[grade - 1][line - 1];
    }

    /** Weights over the legal options for this cube/line grade, or null to keep the built-in weights. */
    public static Weighted optionWeights(int cubeId, int grade, Collection<Integer> legal) {
        if (legal.isEmpty()) return null;
        Set<Integer> legalSet = legal instanceof Set<Integer> s ? s : new HashSet<>(legal);
        long key = key(cubeId, grade);
        Data d = data;
        Weighted override = d.overrides.get(key);
        if (override != null) {
            Weighted restricted = restrict(override, legalSet);
            if (restricted != null) return restricted;
        }
        List<Weighted> categories = d.categories.get(key);
        if (categories == null) return null;
        Weighted best = null;
        int bestOverlap = 0, bestMissing = Integer.MAX_VALUE;
        for (Weighted category : categories) {
            int overlap = 0;
            for (int option : category.keys()) if (legalSet.contains(option)) overlap++;
            int missing = category.keys().length - overlap;
            if (overlap > bestOverlap || overlap == bestOverlap && missing < bestMissing) {
                best = category; bestOverlap = overlap; bestMissing = missing;
            }
        }
        // Require that most of the chosen 273 category is legal here; otherwise the data does not describe this item.
        if (best == null || bestOverlap * 2 < best.keys().length) return null;
        return restrict(best, legalSet);
    }

    private static Weighted restrict(Weighted weights, Set<Integer> legal) {
        StringBuilder spec = new StringBuilder();
        for (int option : weights.keys()) {
            if (!legal.contains(option)) continue;
            if (!spec.isEmpty()) spec.append(',');
            spec.append(option).append(':').append(weights.weightOf(option));
        }
        return spec.isEmpty() ? null : Weighted.parse(spec.toString());
    }

    private static long key(int cubeId, int grade) {
        return ((long) cubeId << 8) | grade;
    }

    private static boolean setting(List<String[]> settings, String key) {
        if (settings != null) for (String[] c : settings) {
            if (c.length >= 2 && key.equals(c[0].trim())) return Boolean.parseBoolean(c[1].trim());
        }
        return false;
    }

    private static Data load() {
        List<String[]> settings = EnhanceTables.rows("cube-settings.tsv");
        Map<Integer, int[]> tierUp = new HashMap<>();
        List<String[]> rows = setting(settings, "tier_up") ? EnhanceTables.rows("cube-grade.tsv") : null;
        if (rows != null) for (String[] c : rows) {
            try {
                int cube = Integer.parseInt(c[0]), grade = Integer.parseInt(c[1]), rate = Integer.parseInt(c[2]);
                if (grade < 1 || grade > 3 || rate < 0 || rate > 10_000) throw new IllegalArgumentException("range");
                tierUp.computeIfAbsent(cube, k -> new int[]{-1, -1, -1})[grade - 1] = rate;
            } catch (RuntimeException e) {
                EnhanceTables.warn("cube-grade.tsv", String.join("|", c), e);
            }
        }
        // A cube listed with some grades but not others: missing grades mean "no tier-up" in the 273 files.
        tierUp.values().forEach(r -> { for (int i = 0; i < r.length; i++) if (r[i] < 0) r[i] = 0; });

        Map<Integer, int[][]> optionLevel = new HashMap<>();
        rows = setting(settings, "option_level") ? EnhanceTables.rows("cube-option-level.tsv") : null;
        if (rows != null) for (String[] c : rows) {
            try {
                int cube = Integer.parseInt(c[0]), grade = Integer.parseInt(c[1]), line = Integer.parseInt(c[2]),
                        chance = Integer.parseInt(c[3]);
                if (grade < 1 || grade > 4 || line < 1 || line > 3 || chance < 0 || chance > 10_000)
                    throw new IllegalArgumentException("range");
                optionLevel.computeIfAbsent(cube, k -> {
                    int[][] levels = new int[4][3];
                    for (int[] l : levels) Arrays.fill(l, 10_000);
                    return levels;
                })[grade - 1][line - 1] = chance;
            } catch (RuntimeException e) {
                EnhanceTables.warn("cube-option-level.tsv", String.join("|", c), e);
            }
        }

        Map<Long, List<Weighted>> categories = new HashMap<>();
        rows = EnhanceTables.rows("cube-options.tsv");
        if (rows != null) for (String[] c : rows) {
            try {
                int cube = Integer.parseInt(c[0]), grade = Integer.parseInt(c[1]);
                if (grade < 1 || grade > 4) throw new IllegalArgumentException("grade");
                categories.computeIfAbsent(key(cube, grade), k -> new ArrayList<>()).add(Weighted.parse(c[3]));
            } catch (RuntimeException e) {
                EnhanceTables.warn("cube-options.tsv", c[0] + "|" + (c.length > 1 ? c[1] : ""), e);
            }
        }

        Map<Long, Weighted> overrides = new HashMap<>();
        rows = EnhanceTables.rows("cube-option-override.tsv");
        if (rows != null) for (String[] c : rows) {
            try {
                int cube = Integer.parseInt(c[0]), grade = Integer.parseInt(c[1]);
                if (grade < 1 || grade > 4) throw new IllegalArgumentException("grade");
                overrides.put(key(cube, grade), Weighted.parse(c[2]));
            } catch (RuntimeException e) {
                EnhanceTables.warn("cube-option-override.tsv", String.join("|", c), e);
            }
        }
        return new Data(Map.copyOf(tierUp), Map.copyOf(optionLevel), Map.copyOf(categories), Map.copyOf(overrides));
    }
}
