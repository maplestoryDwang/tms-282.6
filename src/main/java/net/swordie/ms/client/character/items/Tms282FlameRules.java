package net.swordie.ms.client.character.items;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.enums.FlameType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.IntUnaryOperator;

/** TMS 282 rebirth flames. The kind comes from the original 282 {@code createType} (6 powerful, 7 eternal/black,
 * 12 abyssal), not from the item name (282 names are Chinese). Line/tier weights are imported from the 279
 * package ex-upgrade.toml. Boss-reward equipment always gets 4 lines and tiers shifted up by 2. */
public final class Tms282FlameRules {
    public static final int BOSS_TIER_SHIFT = 2;
    public static final int BOSS_LINES = 4;

    /** 暗黑 / 深淵 flames show the before/after choice window; all others apply directly. */
    private static final Set<Integer> BLACK = Set.of(2048753, 2048755, 2048766, 2048767, 2048768, 2048799);
    private static final Set<Integer> ABYSSAL = Set.of(2048794, 2048796);

    private Tms282FlameRules() { }

    public record Rule(FlameType baseKind, Weights lines, Weights tiers) { }

    /** Cumulative integer weights; {@code pick(r)} maps r in [0, total) to a key. */
    public record Weights(TreeMap<Integer, Integer> cumulative, int total) {
        public int pick(int r) {
            if (r < 0 || r >= total) throw new IllegalArgumentException("roll out of range");
            return cumulative.higherEntry(r).getValue();
        }

        static Weights parse(String spec, int minKey, int maxKey) {
            TreeMap<Integer, Integer> cumulative = new TreeMap<>();
            int total = 0;
            for (String part : spec.split(",")) {
                String[] kv = part.split(":");
                if (kv.length != 2) throw new IllegalArgumentException("Bad weight " + part);
                int key = Integer.parseInt(kv[0].trim()), weight = Integer.parseInt(kv[1].trim());
                if (key < minKey || key > maxKey || weight < 0) throw new IllegalArgumentException("Bad weight " + part);
                if (weight == 0) continue;
                total += weight;
                cumulative.put(total, key);
            }
            if (total <= 0) throw new IllegalArgumentException("Empty weights " + spec);
            return new Weights(cumulative, total);
        }
    }

    private static final class Holder { static final Map<Integer, Rule> RULES = load(); }

    static Map<Integer, Rule> parse(Iterable<String> rows) {
        Map<Integer, Rule> result = new HashMap<>();
        for (String row : rows) {
            if (row.isBlank() || row.startsWith("create_type\t")) continue;
            String[] c = row.split("\t", -1);
            if (c.length != 5) throw new IllegalArgumentException("Malformed flame rule: " + row);
            int createType = Integer.parseInt(c[0]);
            Rule rule = new Rule(FlameType.valueOf(switch (c[1]) {
                case "POWERFUL" -> "Powerful";
                case "ETERNAL" -> "Eternal";
                case "ABYSSAL" -> "Abyssal";
                default -> throw new IllegalArgumentException("Unknown flame kind " + c[1]);
            }), Weights.parse(c[2], 1, 4), Weights.parse(c[3], 1, 7 - BOSS_TIER_SHIFT));
            if (result.putIfAbsent(createType, rule) != null) throw new IllegalArgumentException("Duplicate createType " + createType);
        }
        if (result.isEmpty()) throw new IllegalArgumentException("No flame rules");
        return Map.copyOf(result);
    }

    private static Map<Integer, Rule> load() {
        try {
            return parse(Files.readAllLines(Path.of(ServerConstants.DIR, "data/protocol/tms282.3/flame-rules.tsv")));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read flame rules", e);
        }
    }

    /** Null for createTypes without a rule (22 覺醒暗黑, …): those stay unsupported. 覺醒 20 / 噴出岩 21 have rows; their
     * tiers come from data/resources/tms282/enhance/flame-items.tsv via ExUpgradeRules. Row 8 is RED, row 0
     * ({@link Tms282EnhanceItems#NO_CREATE_TYPE}) the 282 exNew flames without a createType (週年武器專用). */
    public static Rule rule(int createType) {
        return Holder.RULES.get(createType);
    }

    /** The legacy flame type that decides direct-apply vs choice window. */
    public static FlameType kind(int itemId, Rule rule) {
        if (BLACK.contains(itemId)) return FlameType.Black;
        if (ABYSSAL.contains(itemId)) return FlameType.Abyssal;
        return rule.baseKind() == FlameType.Abyssal ? FlameType.Abyssal : rule.baseKind();
    }

    /** Rolls onto {@code equip}; {@code random.applyAsInt(n)} must return a uniform value in [0, n). */
    public static short apply(Equip equip, Rule rule, IntUnaryOperator random) {
        boolean boss = equip.isBossReward();
        int lines = boss ? BOSS_LINES : rule.lines().pick(random.applyAsInt(rule.lines().total()));
        return equip.flameTms282(lines, () -> rule.tiers().pick(random.applyAsInt(rule.tiers().total()))
                + (boss ? BOSS_TIER_SHIFT : 0));
    }
}
