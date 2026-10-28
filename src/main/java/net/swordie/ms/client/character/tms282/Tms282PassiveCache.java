package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.constants.SkillConstants;

/**
 * Updates the base stat cache after a caller mutates a raw skill record without updating its passive values.
 * The CP0196 service already uses {@link Tms282SkillLearning#putWithStats}; calling this helper after that path
 * would apply the passive twice. Kept for callers/checks that directly replace or edit records through putSkill.
 */
public final class Tms282PassiveCache {
    private Tms282PassiveCache() {
    }

    /** {@code skillId} went from {@code oldLevel} to its current level on {@code chr}. */
    public static void refresh(Char chr, int skillId, int oldLevel) {
        Skill now = chr.getSkill(skillId);
        if (now == null || now.getCurrentLevel() == oldLevel || !SkillConstants.isPassiveSkill(skillId)) {
            return;
        }
        if (oldLevel > 0) {
            Skill old = new Skill();
            old.setSkillId(skillId); // removeFromBaseStatCache reads only the id and the level
            old.setCurrentLevel(oldLevel);
            chr.removeFromBaseStatCache(old);
        }
        chr.addToBaseStatCache(now);
    }
}
