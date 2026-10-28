package net.swordie.ms.client.character.items;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.constants.SkillConstants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/** Exact deterministic EquipmentSkill rows exported from original 282 Character.wz.
 * This is an equipment-level mapping, not evidence that the skill effect is implemented. */
public final class Tms282EquipmentSkillData {
    private Tms282EquipmentSkillData() { }
    private static final class Holder {
        static final Map<Integer, Map<Integer, Map<Integer, Integer>>> ITEMS = load();
        static final Set<Integer> SKILLS = skillIds();
        private static Set<Integer> skillIds() {
            Set<Integer> result = new HashSet<>();
            ITEMS.values().forEach(levels -> levels.values().forEach(skills -> result.addAll(skills.keySet())));
            return Set.copyOf(result);
        }
    }

    private static Map<Integer, Map<Integer, Map<Integer, Integer>>> load() {
        Map<Integer, Map<Integer, Map<Integer, Integer>>> result = new HashMap<>();
        Path path = Path.of(ServerConstants.DIR, "data/protocol/tms282.3/equipment-skills.tsv");
        try {
            for (String row : Files.readAllLines(path)) {
                if (row.startsWith("item_id\t") || row.isBlank()) continue;
                String[] cells = row.split("\t", -1);
                if (cells.length != 4) throw new IllegalArgumentException("Invalid equipment skill row: " + row);
                int item = Integer.parseInt(cells[0]), level = Integer.parseInt(cells[1]);
                int skill = Integer.parseInt(cells[2]), slv = Integer.parseInt(cells[3]);
                if (item <= 0 || level <= 0 || skill <= 0 || slv <= 0)
                    throw new IllegalArgumentException("Nonpositive equipment skill row: " + row);
                var skills = result.computeIfAbsent(item, ignored -> new HashMap<>())
                        .computeIfAbsent(level, ignored -> new HashMap<>());
                if (skills.putIfAbsent(skill, slv) != null)
                    throw new IllegalArgumentException("Duplicate equipment skill row: " + row);
            }
            result.values().forEach(levels -> levels.replaceAll((level, skills) -> Map.copyOf(skills)));
            result.replaceAll((item, levels) -> Map.copyOf(levels));
            if (result.isEmpty()) throw new IllegalArgumentException("Empty equipment skill data");
            return Map.copyOf(result);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read original 282 equipment skill levels", e);
        }
    }

    /** Null means this export has no coverage: caller keeps its existing template path.
     * Empty means a KNOWN item has an unsupported level: do not invent a skill upgrade.
     * Legacy unlevelled fixed-level items use their first level, matching the template loader. */
    public static Map<Integer, Integer> levels(int itemId, int itemLevel) {
        var levels = Holder.ITEMS.get(itemId);
        return levels == null ? null : levels.getOrDefault(itemLevel == 0 ? 1 : itemLevel, Map.of());
    }

    public static boolean hasSource(int skillId) { return Holder.SKILLS.contains(skillId); }

    /** A grant on an item does not by itself prove exclusive ownership. Keep the existing
     * item-only classification; unclassified cosmetic/event skills need separate source review. */
    public static boolean requiresEquippedSource(int skillId) {
        return hasSource(skillId) && (SkillConstants.isSkillFromItem(skillId)
                || skillId == Tms282EquipmentEffects.ASCETIC || skillId == Tms282EquipmentEffects.LUCID_NIGHTMARE);
    }

    /** Includes every level so changing/removing an invalid old level can still revoke stale grants. */
    public static Set<Integer> skillIds(int itemId) {
        Set<Integer> result = new HashSet<>();
        var levels = Holder.ITEMS.get(itemId);
        if (levels != null) levels.values().forEach(skills -> result.addAll(skills.keySet()));
        return Set.copyOf(result);
    }
}
