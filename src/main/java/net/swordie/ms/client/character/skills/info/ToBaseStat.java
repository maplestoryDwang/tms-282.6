package net.swordie.ms.client.character.skills.info;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.jobs.adventurer.archer.BowMaster;
import net.swordie.ms.client.jobs.adventurer.warrior.Hero;
import net.swordie.ms.enums.BaseStat;
import net.swordie.ms.loaders.SkillData;

import java.util.Map;

public class ToBaseStat {

    public static Map<BaseStat, Integer> comboCounter(Char chr, Option o, Map<BaseStat, Integer> stats) {
        int orbAmount = o.nOption - 1;
        int totalFdBonus = 0;
        if (chr.getJobHandler() instanceof Hero hero) {
            Skill skill = hero.getComboAttackSkill();
            switch (skill.getSkillId()) {
                case Hero.ADVANCED_COMBO:
                    totalFdBonus = chr.getSkillStatValue(SkillStat.v, Hero.ADVANCED_COMBO);
                    break;
                case Hero.COMBO_SYNERGY:
                    totalFdBonus = chr.getSkillStatValue(SkillStat.indieDamR, Hero.COMBO_SYNERGY);
                    break;
                case Hero.COMBO_ATTACK:
                    stats.put(BaseStat.pad, orbAmount * chr.getSkillStatValue(SkillStat.y, Hero.COMBO_ATTACK));
                    break;
            }
            if (chr.hasSkill(Hero.ADVANCED_COMBO_REINFORCE)) {
                totalFdBonus += chr.getSkillStatValue(SkillStat.damR, Hero.ADVANCED_COMBO_REINFORCE);
            }
        }

        stats.put(BaseStat.fd, orbAmount * totalFdBonus);
        return stats;
    }

    public static Map<BaseStat, Integer> focusedFury(Char chr, Option o, Map<BaseStat, Integer> stats) {
        Skill skill = chr.getSkill(BowMaster.FOCUSED_FURY);
        if (skill != null) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();

            stats.put(BaseStat.asr, (si.getValue(SkillStat.x, slv) * o.nOption));
            return stats;
        }
        return stats;
    }
}

