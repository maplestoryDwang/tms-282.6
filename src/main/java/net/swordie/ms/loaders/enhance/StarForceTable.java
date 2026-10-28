package net.swordie.ms.loaders.enhance;

import java.util.ArrayList;
import java.util.List;

/** Star force rates from starforce.tsv (TMS 279 hyper_upgrade.toml), level caps from starforce-caps.tsv and the
 * chance-time setting from starforce-settings.tsv. Stars/brackets without a row keep GameConstants' built-ins.
 * <p>
 * Not consumed yet (ItemHandler is outside this change): {@link Row#down()} (a failure drops a star),
 * {@link Row#pointCost()}/{@link Row#pointProtectCost()} (maple point star force). */
public final class StarForceTable {
    public static final int NORMAL_STARS = 30, SUPERIOR_STARS = 15;

    /** success/destroy per 10000 of the whole attempt; costs -1 = not set (use the formula). */
    public record Row(int success, int destroy, boolean down, long mesoCost, long pointCost, long pointProtectCost) { }

    private record Data(Row[] normal, Row[] superior, int bonusTime, int[][] capsNormal, int[][] capsSuperior) { }

    private static volatile Data data = load();

    private StarForceTable() { }

    public static synchronized void reload() {
        data = load();
    }

    /** Row for an attempt from {@code star}, or null when the table has none. */
    public static Row row(int star, boolean superior) {
        Row[] rows = superior ? data.superior : data.normal;
        return star < 0 || star >= rows.length ? null : rows[star];
    }

    /** 279 {@code bonus_time}: the Nth try after N-1 consecutive drops always succeeds; {@code def} if unset. */
    public static int chanceTimeDropStreak(int def) {
        return data.bonusTime > 0 ? data.bonusTime - 1 : def;
    }

    /** {max_req_level, max_stars} brackets (max_stars -1 = item star limit), or null when not configured. */
    public static int[][] caps(boolean superior) {
        return superior ? data.capsSuperior : data.capsNormal;
    }

    private static Data load() {
        Row[] normal = new Row[NORMAL_STARS], superior = new Row[SUPERIOR_STARS];
        List<String[]> rows = EnhanceTables.rows("starforce.tsv");
        if (rows != null) for (String[] c : rows) {
            try {
                boolean sup = switch (c[0]) {
                    case "NORMAL" -> false;
                    case "SUPERIOR" -> true;
                    default -> throw new IllegalArgumentException("kind " + c[0]);
                };
                int star = Integer.parseInt(c[1]), success = Integer.parseInt(c[2]), destroy = Integer.parseInt(c[3]);
                if (success < 0 || destroy < 0 || success + destroy > 10_000) throw new IllegalArgumentException("rates");
                Row[] target = sup ? superior : normal;
                if (star < 0 || star >= target.length) throw new IllegalArgumentException("star " + star);
                target[star] = new Row(success, destroy, "1".equals(c[4]), Long.parseLong(c[5]),
                        Long.parseLong(c[6]), Long.parseLong(c[7]));
            } catch (RuntimeException e) {
                EnhanceTables.warn("starforce.tsv", String.join("|", c), e);
            }
        }
        int bonusTime = 0;
        List<String[]> settings = EnhanceTables.rows("starforce-settings.tsv");
        if (settings != null) for (String[] c : settings) {
            try {
                if ("bonus_time".equals(c[0])) bonusTime = Integer.parseInt(c[1].trim());
            } catch (RuntimeException e) {
                EnhanceTables.warn("starforce-settings.tsv", String.join("|", c), e);
            }
        }
        List<int[]> capsN = new ArrayList<>(), capsS = new ArrayList<>();
        List<String[]> caps = EnhanceTables.rows("starforce-caps.tsv");
        if (caps != null) for (String[] c : caps) {
            try {
                int[] bracket = {Integer.parseInt(c[1]), Integer.parseInt(c[2])};
                switch (c[0]) {
                    case "NORMAL" -> capsN.add(bracket);
                    case "SUPERIOR" -> capsS.add(bracket);
                    default -> throw new IllegalArgumentException("kind " + c[0]);
                }
            } catch (RuntimeException e) {
                EnhanceTables.warn("starforce-caps.tsv", String.join("|", c), e);
            }
        }
        return new Data(normal, superior, bonusTime, sorted(capsN), sorted(capsS));
    }

    private static int[][] sorted(List<int[]> brackets) {
        if (brackets.isEmpty()) return null;
        brackets.sort((a, b) -> Integer.compare(a[0], b[0]));
        if (brackets.getLast()[0] != Integer.MAX_VALUE) {
            System.err.println("[enhance] starforce-caps.tsv: no open-ended bracket (2147483647); using built-in caps");
            return null;
        }
        return brackets.toArray(int[][]::new);
    }
}
