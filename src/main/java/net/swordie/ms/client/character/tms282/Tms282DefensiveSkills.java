package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** New 282 defensive actions. WZ describes invulnerability, without an indieNotDamaged stat key.
 * Native 564F5BC tests indie bit50 for reason5311016; its WZ time2500 is milliseconds (String h: t=2.5s).
 * The other three skills use time in seconds. The buff book owns cancellation/expiry and mirrors it to the TSM. */
public final class Tms282DefensiveSkills {
    public static final int DEFENSIVE_OVERDRIVE = 37121025;
    public static final Set<Integer> SKILLS = Set.of(5311016, 3111020, 31211012, DEFENSIVE_OVERDRIVE);

    private Tms282DefensiveSkills() { }

    public static List<Tms282BuffResolver.ResolvedStat> resolve(SkillInfo info, int skillId, int slv, long now) {
        if (!SKILLS.contains(skillId) || info == null || slv <= 0
                || Protocol.active().version() != ProtocolVersion.TMS_282_3) return List.of();
        long duration = (long)info.getValue(SkillStat.time, slv) * (skillId == 5311016 ? 1 : 1000);
        if (duration <= 0) return List.of();
        Option o = new Option();
        o.nReason = skillId; o.nValue = 1; o.slv = slv; o.startTime = now; o.tStart = (int)now;
        o.tTerm = (int)Math.min(Integer.MAX_VALUE,duration); o.isInMillis = true;
        return List.of(new Tms282BuffResolver.ResolvedStat(CharacterTemporaryStat.IndieNotDamaged,o,"282 defensive action"));
    }

    public static boolean active(Map<CharacterTemporaryStat,List<Option>> stats, int skillId, long now) {
        return SKILLS.contains(skillId) && stats.getOrDefault(CharacterTemporaryStat.IndieNotDamaged,List.of()).stream()
                .anyMatch(o -> o.nReason == skillId && o.nValue > 0 && o.tTerm > 0
                        && now >= o.startTime && now - o.startTime < (o.isInMillis ? o.tTerm : o.tTerm * 1000L));
    }

    /** Only these accepted, unexpired server-owned buffs protect HP; never broadens all legacy invulnerability. */
    public static boolean blocksDamage(Char chr, long now) {
        if (chr == null || chr.getHP() <= 0 || Protocol.active().version() != ProtocolVersion.TMS_282_3) return false;
        var stats = chr.getTemporaryStatManager().getCurrentStats();
        return Tms282DragonMaster.blocksDamage(chr,now) || SKILLS.stream().anyMatch(id -> active(stats,id,now));
    }
}
