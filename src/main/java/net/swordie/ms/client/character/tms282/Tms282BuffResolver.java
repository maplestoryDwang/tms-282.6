package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;

import java.util.List;
import java.util.Objects;

/**
 * Strategy: which CharacterTemporaryStats a skill use sets, with 265 {@link Option} values (times already in
 * milliseconds). 265 decides this in hand-written per-job code ({@code Job.handleSkill}), which needs a full field
 * and client; {@link SkillStatBuffResolver} derives it from the skill's data instead. A per-skill override (a port
 * of one job handler's case) can be layered on top by another implementation.
 */
public interface Tms282BuffResolver {

    /** Empty when the skill sets no temporary stat (an attack skill, or a buff this resolver cannot express). */
    List<ResolvedStat> resolve(SkillInfo info, int skillId, int slv, long nowMillis);

    /** One stat to set; {@code source} names the skill data entry it came from, for logs. */
    record ResolvedStat(CharacterTemporaryStat stat, Option option, String source) {
        public ResolvedStat {
            Objects.requireNonNull(stat, "stat");
            Objects.requireNonNull(option, "option");
            Objects.requireNonNull(source, "source");
        }

        /** Remaining time in milliseconds when set (indie: tTerm, standard: tOption). */
        public long durationMillis() {
            return stat.isIndie() ? option.tTerm : option.tOption;
        }
    }
}
