package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Data-driven {@link Tms282BuffResolver}: a skill is a buff when its 265 skill data has {@code time > 0} and no
 * {@code damage}; each of its stat entries that names a temporary stat becomes one CTS, the way 265 job handlers
 * write them (e.g. Warrior Rage: {@code o.nReason = skillId; o.nValue = si.getValue(indiePad, slv);
 * o.tTerm = si.getValue(time, slv); IndiePAD}).
 * <ul>
 *     <li>every {@code indie*} SkillStat -> the indie CTS of the same name (case-insensitive, e.g. indiePad ->
 *     IndiePAD): {@code nReason = skillId, nValue = value, tTerm = time};</li>
 *     <li>{@link #STANDARD} SkillStats -> the standard CTS of the same name (pad -> PAD, speed -> Speed, ...):
 *     {@code nOption = value, rOption = skillId, tOption = time}.</li>
 * </ul>
 * Anything else (x/y/prop and job-specific stats) is ignored: those need the job handler's own meaning. Times are
 * {@code time} seconds x 1000 (265 {@code Option.setTimeToMillis}). Zero values are not set.
 */
public final class SkillStatBuffResolver implements Tms282BuffResolver {
    /** Standard stats whose SkillStat and CTS share a name and whose value is the CTS nOption. */
    public static final Set<String> STANDARD = Set.of("pad", "mad", "pdd", "mdd", "acc", "eva", "speed", "jump",
            "epad", "emad", "epdd", "emdd", "emhp", "emmp", "asrR", "terR");

    private final Map<SkillStat, CharacterTemporaryStat> mapping;

    public SkillStatBuffResolver() {
        Map<String, CharacterTemporaryStat> byName = new java.util.HashMap<>();
        for (CharacterTemporaryStat cts : CharacterTemporaryStat.values()) {
            byName.put(cts.name().toLowerCase(Locale.ROOT), cts);
        }
        Map<SkillStat, CharacterTemporaryStat> map = new EnumMap<>(SkillStat.class);
        for (SkillStat stat : SkillStat.values()) {
            String name = stat.name();
            boolean indie = name.startsWith("indie");
            if (!indie && !STANDARD.contains(name)) {
                continue;
            }
            CharacterTemporaryStat cts = byName.get(name.toLowerCase(Locale.ROOT));
            if (cts != null && cts.isIndie() == indie) {
                map.put(stat, cts);
            }
        }
        this.mapping = Collections.unmodifiableMap(map);
    }

    /** SkillStat -> CTS pairs this resolver uses. */
    public Map<SkillStat, CharacterTemporaryStat> mapping() {
        return mapping;
    }

    @Override
    public List<ResolvedStat> resolve(SkillInfo info, int skillId, int slv, long nowMillis) {
        if (info == null || slv <= 0) {
            return List.of();
        }
        if (Tms282DefensiveSkills.SKILLS.contains(skillId)) {
            return Tms282DefensiveSkills.resolve(info, skillId, slv, nowMillis);
        }
        if (net.swordie.ms.connection.packet.Tms282RemoteBuffs.active()
                && net.swordie.ms.client.jobs.adventurer.magician.Magician.isHexaArcana(skillId)) {
            return List.of(); // The job/TSM owns both Arcana stages and their changing IndieMADR value.
        }
        if (skillId == 121002002) { // Beast Breathing V: permanent ON/OFF, not a timed generic stat buff.
            Option option = new Option();
            option.nOption = slv;
            option.rOption = skillId;
            option.startTime = nowMillis;
            option.isInMillis = true;
            return List.of(new ResolvedStat(CharacterTemporaryStat.TanjiroBeastBreathingType5, option, "Beast Breathing ON"));
        }
        if (Tms282LucidFlight.isFlightSkill(skillId)) { // lucidfly-0930: NewFlying toggle, no time in the WZ entry
            return List.of(Tms282LucidFlight.flight(skillId, slv, nowMillis));
        }
        Map<SkillStat, String> stats = info.getSkillStatInfo();
        int seconds = stats.containsKey(SkillStat.time) ? info.getValue(SkillStat.time, slv) : 0;
        if (seconds <= 0 || (stats.containsKey(SkillStat.damage) && info.getValue(SkillStat.damage, slv) > 0)) {
            return List.of();
        }
        int millis = (int) Math.min(Integer.MAX_VALUE, seconds * 1000L);
        List<ResolvedStat> out = new ArrayList<>();
        for (SkillStat stat : stats.keySet()) {
            CharacterTemporaryStat cts = mapping.get(stat);
            if (cts == null) {
                continue;
            }
            int value = info.getValue(stat, slv);
            if (value == 0) {
                continue;
            }
            Option o = new Option();
            o.startTime = nowMillis;
            o.tStart = (int) nowMillis;
            o.slv = slv;
            if (cts.isIndie()) {
                o.nReason = skillId;
                o.nValue = value;
                o.tTerm = millis;
            } else {
                o.nOption = value;
                o.rOption = skillId;
                o.tOption = millis;
            }
            o.isInMillis = true;
            out.add(new ResolvedStat(cts, o, stat.name() + "=" + value));
        }
        out.sort((a, b) -> Integer.compare(a.stat().ordinal(), b.stat().ordinal()));
        return List.copyOf(out);
    }
}
