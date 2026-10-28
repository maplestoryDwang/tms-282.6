package net.swordie.ms.world.tms282.bossrun;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player damage snapshots of boss mobs, grouped by scope ("i&lt;instanceId&gt;" or "f&lt;field identity&gt;").
 * Mob keeps damage in {@code Mob#getDamageDone()} but clears it before death observers run (see
 * {@code UrusRewardService}), so the value kept for each (mob, character) is the largest one seen while the mob lived.
 */
public final class BossRunDamage {
    private final Map<String, Map<Integer, Map<Integer, Long>>> byScope = new ConcurrentHashMap<>();

    /** Merges one reading of a mob's damage map (character id -> damage). */
    public void record(String scope, int mobObjectId, Map<Integer, Long> damageByChar) {
        if (scope == null || damageByChar == null || damageByChar.isEmpty()) return;
        Map<Integer, Long> mob = byScope.computeIfAbsent(scope, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(mobObjectId, k -> new ConcurrentHashMap<>());
        damageByChar.forEach((id, dmg) -> {
            if (id != null && dmg != null && dmg > 0) mob.merge(id, dmg, Math::max);
        });
    }

    /** Sum over every mob of the given scopes, per character. */
    public Map<Integer, Long> totals(Collection<String> scopes) {
        Map<Integer, Long> out = new HashMap<>();
        for (String s : scopes) {
            Map<Integer, Map<Integer, Long>> mobs = byScope.get(s);
            if (mobs == null) continue;
            for (Map<Integer, Long> m : mobs.values()) m.forEach((id, dmg) -> out.merge(id, dmg, Long::sum));
        }
        return out;
    }

    /**
     * Damage share in permille for each member (order kept). Damage by characters outside {@code memberIds} still
     * counts in the total. All zero when no damage was seen.
     */
    public static Map<Integer, Integer> sharesPermille(Map<Integer, Long> totals, List<Integer> memberIds) {
        long total = 0;
        for (long v : totals.values()) total += v;
        Map<Integer, Integer> out = new LinkedHashMap<>();
        for (int id : memberIds) {
            long mine = totals.getOrDefault(id, 0L);
            out.put(id, total <= 0 ? 0 : (int) Math.round(mine * 1000.0 / total));
        }
        return out;
    }

    public void forget(Collection<String> scopes) {
        for (String s : scopes) byScope.remove(s);
    }

    /** Drops instance scopes ("i...") whose instance is gone; field scopes are dropped by {@link #forget}. */
    public void retainInstanceScopes(java.util.Set<String> live) {
        byScope.keySet().removeIf(k -> k.startsWith("i") && !live.contains(k));
    }

    public int scopeCount() {
        return byScope.size();
    }
}
