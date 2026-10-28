package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.loaders.SkillData;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TMS 282 ring skill buffs from skill data (equips README WI-7).
 * <ul>
 * <li>Tower of Oz rings 80001455..80001479: a CP04B4 use goes through {@code Tms282SkillService}, whose
 * {@code SkillStatBuffResolver} already sets every {@code indie*} stat of the skill (LP009E): Restraint 80001455 /
 * 80001479 PAD% / MAD%, 80001457 / 80001458 boss damage, 80001459 ignore defence, 80001460 max HP%, 80001474
 * booster, 80001476 ignore defence + defence -50%. {@link #buffs} is the same derivation (for checks). Rings whose
 * effect is only {@code x} / {@code y} / {@code prop} (crit / stance / level jump / cleanse ...) have no generic
 * mapping and stay unimplemented.</li>
 * <li>Continuous ring: CP03F8 (after the 120-second preparation of 80003341) grants 80003342 (boss damage, PAD% /
 * MAD%, 30 s) at the source ring's level; see {@link #continuous}. Sent through the TemporaryStatManager, which 282
 * writes as LP009E ({@code Tms282ManagedBuffs}).</li>
 * </ul>
 */
public final class Tms282OzRingEffects {
    public static final int OZ_FIRST = 80001455;
    public static final int OZ_LAST = 80001479;

    /** skill stat -> the indie temporary stat it is applied as, in application order. */
    static final Map<SkillStat, CharacterTemporaryStat> INDIE = new LinkedHashMap<>();

    static {
        INDIE.put(SkillStat.indiePadR, CharacterTemporaryStat.IndiePADR);
        INDIE.put(SkillStat.indieMadR, CharacterTemporaryStat.IndieMADR);
        INDIE.put(SkillStat.indieBDR, CharacterTemporaryStat.IndieBDR);
        INDIE.put(SkillStat.indieIgnoreMobpdpR, CharacterTemporaryStat.IndieIgnoreMobpdpR);
        INDIE.put(SkillStat.indieMhpR, CharacterTemporaryStat.IndieMHPR);
        INDIE.put(SkillStat.indieBooster, CharacterTemporaryStat.IndieBooster);
        INDIE.put(SkillStat.indiePddR, CharacterTemporaryStat.IndiePDDR);
    }

    /** Minimum time between two continuous-ring procs of one character (80003341 time = 120 s, minus jitter). */
    static final long CONTINUOUS_PREPARE_MS = 118_000L;
    private static final Map<Integer, Long> LAST_CONTINUOUS = new ConcurrentHashMap<>();

    private Tms282OzRingEffects() {
    }

    public static boolean isOzRing(int skillId) {
        return skillId >= OZ_FIRST && skillId <= OZ_LAST;
    }

    /** The indie buffs of {@code skillId} at {@code slv} from skill data; empty when it defines none (or no time). */
    public static EnumMap<CharacterTemporaryStat, Option> buffs(int skillId, int slv) {
        EnumMap<CharacterTemporaryStat, Option> stats = new EnumMap<>(CharacterTemporaryStat.class);
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (si == null || slv <= 0) {
            return stats;
        }
        int time = si.getValue(SkillStat.time, slv);
        if (time <= 0) {
            return stats;
        }
        for (Map.Entry<SkillStat, CharacterTemporaryStat> e : INDIE.entrySet()) {
            int value = si.getValue(e.getKey(), slv);
            if (value != 0) {
                Option o = new Option();
                o.nReason = skillId;
                o.nValue = value;
                o.tTerm = time;
                stats.put(e.getValue(), o);
            }
        }
        return stats;
    }

    /**
     * CP03F8: the client reports that {@code triggeringSkill} consumed the prepared continuous ring. Grants
     * 80003342 at the level of the character's 80003341 record (the equipped ring's level) when the reported level
     * matches, the triggering skill is one of the character's skills, and the preparation time has passed.
     * @return whether the buff was applied
     */
    public static boolean continuous(Char chr, int triggeringSkill, int sourceLevel, long nowMs) {
        if (chr == null) {
            return false;
        }
        Skill source = chr.getSkill(Tms282EquipmentEffects.CONTINUOUS_READY);
        Skill trigger = chr.getSkill(triggeringSkill);
        if (source == null || source.getCurrentLevel() <= 0 || source.getCurrentLevel() != sourceLevel
                || trigger == null || trigger.getCurrentLevel() <= 0) {
            return false;
        }
        Long last = LAST_CONTINUOUS.get(chr.getId());
        if (last != null && nowMs - last < CONTINUOUS_PREPARE_MS) {
            return false;
        }
        var stats = buffs(Tms282EquipmentEffects.CONTINUOUS_EFFECT, sourceLevel);
        if (stats.isEmpty()) {
            return false;
        }
        LAST_CONTINUOUS.put(chr.getId(), nowMs);
        chr.getTemporaryStatManager().sendStat(stats);
        return true;
    }
}
