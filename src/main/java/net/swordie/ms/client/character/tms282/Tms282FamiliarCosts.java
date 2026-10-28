package net.swordie.ms.client.character.tms282;

import net.swordie.ms.loaders.enhance.EnhanceTables;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Familiar fees from data/resources/tms282/enhance/familiar-costs.tsv (TMS 279 familiar.yml); the built-in
 * defaults are the same 279 values. No 282 service performs these operations yet (Tms282FamiliarService only edits
 * favorite/lock/name), so nothing charges them: seal, appearance change/restore, upgrade, disassemble and the link
 * unlock/apply/activate RPCs still need handlers that call these getters. */
public record Tms282FamiliarCosts(int sealCost, int appearanceChangeFee, int appearanceRestoreFee,
                                  int[] upgradeEssenceNormal, int[] upgradeEssenceSpecial,
                                  int[] disassembleEssenceNormal, int[] disassembleEssenceSpecial,
                                  int[] linkUnlockFee, int[] linkApplyFee, int[] linkActivateFee,
                                  int linkActivateMaxHours) {
    public static final Tms282FamiliarCosts DEFAULTS = new Tms282FamiliarCosts(200, 2500, 1200,
            new int[]{120, 300, 1100, 3200}, new int[]{0, 0, 1100, 3200},
            new int[]{3, 10, 30, 100, 400}, new int[]{0, 0, 1000, 3000, 5000},
            new int[]{3000, 5000}, new int[]{1000, 1000, 1000, 0}, new int[]{2000, 12000, 48000, 0}, 720);

    private static volatile Tms282FamiliarCosts current = load();

    public static Tms282FamiliarCosts get() {
        return current;
    }

    public static synchronized void reload() {
        current = load();
    }

    /** Essence to upgrade from {@code level} (0-based, 4 steps); 0 out of range. */
    public int upgradeEssence(int level, boolean special) {
        return at(special ? upgradeEssenceSpecial : upgradeEssenceNormal, level);
    }

    /** Essence returned when disassembling a card of {@code grade} (0-based, 5 grades); 0 out of range. */
    public int disassembleEssence(int grade, boolean special) {
        return at(special ? disassembleEssenceSpecial : disassembleEssenceNormal, grade);
    }

    public int linkUnlockFee(int slot) { return at(linkUnlockFee, slot); }
    public int linkApplyFee(int index) { return at(linkApplyFee, index); }
    public int linkActivateFee(int index) { return at(linkActivateFee, index); }

    private static int at(int[] values, int index) {
        return index < 0 || index >= values.length ? 0 : values[index];
    }

    private static Tms282FamiliarCosts load() {
        var path = EnhanceTables.dir().resolve("familiar-costs.tsv");
        if (!EnhanceTables.enabled() || !Files.isRegularFile(path)) return DEFAULTS;
        Map<String, String> v = new HashMap<>();
        try {
            boolean header = true;
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                if (header) { header = false; continue; }
                String[] c = line.split("\t", -1);
                if (c.length >= 2) v.put(c[0].trim(), c[1].trim());
            }
            var d = DEFAULTS;
            return new Tms282FamiliarCosts(num(v, "seal_cost", d.sealCost), num(v, "appearance_change_fee", d.appearanceChangeFee),
                    num(v, "appearance_restore_fee", d.appearanceRestoreFee),
                    list(v, "upgrade_essence_normal", d.upgradeEssenceNormal, 4), list(v, "upgrade_essence_special", d.upgradeEssenceSpecial, 4),
                    list(v, "disassemble_essence_normal", d.disassembleEssenceNormal, 5),
                    list(v, "disassemble_essence_special", d.disassembleEssenceSpecial, 5),
                    list(v, "link_unlock_fee", d.linkUnlockFee, -1), list(v, "link_apply_fee", d.linkApplyFee, -1),
                    list(v, "link_activate_fee", d.linkActivateFee, -1), num(v, "link_activate_max_hours", d.linkActivateMaxHours));
        } catch (Exception e) {
            System.err.println("[enhance] familiar-costs.tsv ignored: " + e);
            return DEFAULTS;
        }
    }

    private static int num(Map<String, String> v, String key, int def) {
        String s = v.get(key);
        if (s == null) return def;
        int n = Integer.parseInt(s);
        if (n < 0) throw new IllegalArgumentException(key + " negative");
        return n;
    }

    /** 279 requires fixed list lengths (4 upgrade steps, 5 grades); {@code length} -1 accepts any. */
    private static int[] list(Map<String, String> v, String key, int[] def, int length) {
        String s = v.get(key);
        if (s == null) return def;
        int[] out = Arrays.stream(s.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
        if (length >= 0 && out.length != length) throw new IllegalArgumentException(key + " needs " + length + " values");
        if (Arrays.stream(out).anyMatch(n -> n < 0)) throw new IllegalArgumentException(key + " negative");
        return out;
    }
}
