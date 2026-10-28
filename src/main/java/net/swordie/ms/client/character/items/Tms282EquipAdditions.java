package net.swordie.ms.client.character.items;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

/**
 * TMS 282 equip {@code info/addition} (equips README WI-6), exported from the 282 Character.wz overlay by
 * research/tms2823/item-usability-20260928/equips/export_additions.py into
 * data/protocol/tms282.3/equip-additions.tsv.
 * <ul>
 * <li>{@code skill}: an item skill (9000xxxx) granted through the equipment-skill path while the condition holds.</li>
 * <li>{@code mobdie}: HP / MP recovery when the wearer kills a mob ({@code Job.handleMobKilled}).</li>
 * <li>{@code critical}, {@code mobcategory}: damage the client computes itself; kept as data only.</li>
 * <li>{@code hpmpchange}: periodic HP change (1 item); not applied (no periodic equip hook).</li>
 * </ul>
 * Conditions: {@code lv} (character level, also written {@code level} in the data), {@code job} (one of the ids).
 * {@code craft} is not modelled (unknown meaning in 282) and is treated as not met, so those rows never apply.
 */
public final class Tms282EquipAdditions {
    public record Addition(int itemId, String block, String condition, Map<String, Integer> values) {
        /** Whether the wearer meets the condition. */
        public boolean applies(int charLevel, int job) {
            if (condition.isEmpty()) {
                return true;
            }
            for (String part : condition.split(";")) {
                String[] kv = part.split("=", 2);
                switch (kv[0]) {
                    case "lv" -> {
                        if (charLevel < Integer.parseInt(kv[1])) return false;
                    }
                    case "job" -> {
                        boolean any = false;
                        for (String id : kv[1].split("\\|")) any |= Integer.parseInt(id) == job;
                        if (!any) return false;
                    }
                    default -> {
                        return false; // craft and anything unknown: not met
                    }
                }
            }
            return true;
        }

        public int value(String key) {
            return values.getOrDefault(key, 0);
        }
    }

    private Tms282EquipAdditions() {
    }

    private static final class Holder {
        static final Map<Integer, List<Addition>> BY_ITEM = load(Path.of(ServerConstants.DIR,
                "data/protocol/tms282.3/equip-additions.tsv"));
    }

    static Map<Integer, List<Addition>> load(Path path) {
        Map<Integer, List<Addition>> result = new HashMap<>();
        try {
            for (String row : Files.readAllLines(path)) {
                if (row.isBlank() || row.startsWith("item_id\t")) continue;
                String[] cells = row.split("\t", -1);
                if (cells.length != 4) throw new IllegalArgumentException("Invalid equip addition row: " + row);
                Map<String, Integer> values = new HashMap<>();
                for (String kv : cells[3].split(",")) {
                    if (kv.isEmpty()) continue;
                    String[] pair = kv.split("=", 2);
                    values.put(pair[0], Integer.parseInt(pair[1]));
                }
                result.computeIfAbsent(Integer.parseInt(cells[0]), ignored -> new ArrayList<>())
                        .add(new Addition(Integer.parseInt(cells[0]), cells[1], cells[2], Map.copyOf(values)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read 282 equip additions " + path, e);
        }
        result.replaceAll((item, list) -> List.copyOf(list));
        return Map.copyOf(result);
    }

    public static List<Addition> of(int itemId) {
        return Holder.BY_ITEM.getOrDefault(itemId, List.of());
    }

    /** Every addition skill id of an item regardless of condition (to revoke stale grants). */
    public static Set<Integer> skillIds(int itemId) {
        Set<Integer> result = new HashSet<>();
        for (Addition a : of(itemId)) {
            if (a.block().equals("skill") && a.value("id") > 0) result.add(a.value("id"));
        }
        return result;
    }

    /** Addition skill levels of the worn items whose condition holds; duplicate sources give the highest level. */
    public static Map<Integer, Integer> skillLevels(Iterable<? extends Item> worn, int charLevel, int job) {
        Map<Integer, Integer> result = new HashMap<>();
        for (Item item : worn) {
            if (!(item instanceof Equip)) continue;
            for (Addition a : of(item.getItemId())) {
                if (a.block().equals("skill") && a.value("id") > 0 && a.value("level") > 0 && a.applies(charLevel, job)) {
                    result.merge(a.value("id"), a.value("level"), Math::max);
                }
            }
        }
        return result;
    }

    /** HP / MP to restore for one kill: {hp, mp}; {@code roll} returns 0..99. */
    public static int[] mobDieRecovery(Iterable<? extends Item> worn, int charLevel, int job, int maxHp, int maxMp,
                                       IntSupplier roll) {
        int hp = 0, mp = 0;
        for (Item item : worn) {
            if (!(item instanceof Equip)) continue;
            for (Addition a : of(item.getItemId())) {
                if (!a.block().equals("mobdie") || !a.applies(charLevel, job)) continue;
                hp += a.value("hpIncOnMobDie");
                mp += a.value("mpIncOnMobDie");
                if (a.value("hpIncRatioOnMobDie") > 0 && roll.getAsInt() < a.value("hpRatioProp")) {
                    hp += (int) ((long) maxHp * a.value("hpIncRatioOnMobDie") / 100);
                }
                if (a.value("mpIncRatioOnMobDie") > 0 && roll.getAsInt() < a.value("mpRatioProp")) {
                    mp += (int) ((long) maxMp * a.value("mpIncRatioOnMobDie") / 100);
                }
            }
        }
        return new int[]{hp, mp};
    }

    /** {@code Job.handleMobKilled} (282 only): the wearer's mobdie recovery. */
    public static void onMobKilled(Char chr) {
        if (chr == null || chr.getEquippedInventory() == null || chr.getHP() <= 0) return;
        int[] heal = mobDieRecovery(chr.getEquippedInventory().getItems(), chr.getLevel(), chr.getJob(),
                chr.getMaxHP(), chr.getMaxMP(), () -> ThreadLocalRandom.current().nextInt(100));
        if (heal[0] > 0) chr.heal(heal[0]);
        if (heal[1] > 0) chr.healMP(heal[1]);
    }
}
