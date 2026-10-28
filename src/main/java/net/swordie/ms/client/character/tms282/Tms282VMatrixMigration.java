package net.swordie.ms.client.character.tms282;

import java.util.*;

/** Non-destructive initial import; no265 inventory conversion and no inferred point grants. */
public final class Tms282VMatrixMigration {
    private Tms282VMatrixMigration() { }
    public record Result(Tms282VMatrixState state, Map<Integer, String> pendingCores) { }

    public static Result fromEffectiveSkills(Tms282VMatrixData data, int job, long validatedVPoints,
                                            Collection<Integer> ownedLegacyCoreIds, Map<Integer, Integer> effectiveSkills) {
        return fromEffectiveSkills(data, job, validatedVPoints, ownedLegacyCoreIds, effectiveSkills, Set.of());
    }

    public static Result fromEffectiveSkills(Tms282VMatrixData data, int job, long validatedVPoints,
                                            Collection<Integer> ownedLegacyCoreIds, Map<Integer, Integer> effectiveSkills,
                                            Set<Integer> activeLegacyBoostSkillIds) {
        if (validatedVPoints < 0) throw new IllegalArgumentException("Invalid existing vPoint balance");
        Map<Integer, Tms282VMatrixState.Core> migrated = new TreeMap<>(); Map<Integer, String> pending = new TreeMap<>();
        for (int id : new TreeSet<>(ownedLegacyCoreIds)) {
            var definition = data.cores().get(id);
            if (definition == null || definition.type() != 0 || !data.legacyCompatible(id) || !data.allowed(job, id)) {
                pending.put(id, "沒有可直接對應的 282 技能核心"); continue;
            }
            Set<Integer> levels = new HashSet<>();
            for (int skill : definition.skills()) levels.add(effectiveSkills.getOrDefault(skill, 0));
            if (levels.size() != 1 || levels.contains(0)) { pending.put(id, "未啟用或連結技能等級不一致"); continue; }
            int level = levels.iterator().next();
            if (level < 1 || level > definition.maxLevel()) { pending.put(id, "等級超出 282 核心上限"); continue; }
            // Imported levels are protected against reset refunds: old inventory has NOT been converted to V points.
            migrated.put(id, new Tms282VMatrixState.Core(id, level, false, Tms282VMatrixState.DEFAULT_EXPIRY, level));
        }
        //282 regrouped boost core IDs. Only preserve a group when EVERY connected skill has active265 backing,
        //the same already-effective level and fits the282 cap. No inactive cores, inventory XP or slot value is converted.
        for (var definition : data.cores().values()) {
            if (definition.type() != 1 || !data.allowed(job, definition.id())
                    || definition.skills().isEmpty() || !activeLegacyBoostSkillIds.containsAll(definition.skills())) continue;
            Set<Integer> levels = new HashSet<>();
            for (int skill : definition.skills()) levels.add(effectiveSkills.getOrDefault(skill, 0));
            if (levels.size() != 1) continue;
            int level = levels.iterator().next();
            if (level <= 0 || level > definition.maxLevel()) continue;
            migrated.put(definition.id(), new Tms282VMatrixState.Core(definition.id(), level, false,
                    Tms282VMatrixState.DEFAULT_EXPIRY, level));
        }
        return new Result(new Tms282VMatrixState(validatedVPoints, 0, !pending.isEmpty(), migrated, Set.of()), Map.copyOf(pending));
    }
}
