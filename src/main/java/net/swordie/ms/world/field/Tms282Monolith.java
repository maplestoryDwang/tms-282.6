package net.swordie.ms.world.field;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.Inventory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** 輪迴碑石 1202193: the totem's star count (chuc) selects a spawn tier, rates imported from the 279 package
 * monolith.toml. Rates are bonuses: respawn interval / (1 + dec), mob capacity * (1 + inc). */
public final class Tms282Monolith {
    public static final int TOTEM_ID = 1202193;
    public static final int MAX_TIER = 30;

    private Tms282Monolith() { }

    public record Rates(double decMobIntervalR, double incSpawnMobR) {
        public double respawnDivisor() { return 1 + decMobIntervalR; }
        public double capacityMultiplier() { return 1 + incSpawnMobR; }
    }

    private static final class Holder { static final Map<Integer, Rates> TIERS = load(); }

    static Map<Integer, Rates> parse(Iterable<String> rows) {
        Map<Integer, Rates> result = new HashMap<>();
        for (String row : rows) {
            if (row.isBlank() || row.startsWith("tier\t")) continue;
            String[] c = row.split("\t", -1);
            if (c.length != 3) throw new IllegalArgumentException("Malformed monolith tier row: " + row);
            int tier = Integer.parseInt(c[0]);
            double dec = Double.parseDouble(c[1]), inc = Double.parseDouble(c[2]);
            if (tier < 1 || tier > MAX_TIER || dec < 0 || dec > 1 || inc < 0 || inc > 1
                    || result.putIfAbsent(tier, new Rates(dec, inc)) != null)
                throw new IllegalArgumentException("Invalid/duplicate monolith tier " + tier);
        }
        for (int tier = 1; tier <= MAX_TIER; tier++) {
            if (!result.containsKey(tier)) throw new IllegalArgumentException("Missing monolith tier " + tier);
        }
        return Map.copyOf(result);
    }

    private static Map<Integer, Rates> load() {
        try {
            return parse(Files.readAllLines(Path.of(ServerConstants.DIR, "data/protocol/tms282.3/monolith-tiers.tsv")));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read monolith tiers", e);
        }
    }

    /** Star count clamped to 1..30; an unstarred totem is tier 1. */
    public static int tierForChuc(int chuc) {
        return Math.max(1, Math.min(MAX_TIER, chuc));
    }

    /** Highest tier among the character's WORN 1202193 copies (totem slots); 0 when none. In 282 the skill itself
     * requires the equipped source (Tms282SkillService), so a better-starred copy in a bag must not raise the tier. */
    public static int tierOf(Char chr) {
        int best = 0;
        Inventory worn = chr.getEquippedInventory();
        if (worn == null) return 0;
        for (Item item : worn.getItems()) {
            if (item.getItemId() == TOTEM_ID && item instanceof Equip equip) {
                best = Math.max(best, tierForChuc(equip.getChuc()));
            }
        }
        return best;
    }

    /** Highest tier among the 輪迴碑石 summons on the field, ignoring {@code excluded} (a summon being removed). */
    public static int activeTier(Field field, net.swordie.ms.life.Summon excluded) {
        int best = 0;
        for (net.swordie.ms.life.Summon s : field.getSummons()) {
            if (s != excluded && s.getSkillID() == net.swordie.ms.client.jobs.Job.MONOLITH && s.getChr() != null) {
                best = Math.max(best, tierOf(s.getChr()));
            }
        }
        return best;
    }

    public static Rates rates(int tier) {
        return tier <= 0 ? null : Holder.TIERS.get(tierForChuc(tier));
    }
}
