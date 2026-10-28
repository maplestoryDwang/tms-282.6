package net.swordie.ms.client.jobs.adventurer.thief;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.adventurer.Beginner;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Thief extends Beginner {
    public static final int MAPLE_RETURN = 1281;

    public static final int DARK_SIGHT = 4001003; //Buff
    public static final int LUCKY_SEVEN = 4001344;

    // V Skill
    public static final int SHADOW_WALKER = 400001023;

    private final int[] addedSkills = new int[]{
            MAPLE_RETURN
    };

    private int darkSightCount = 0;

    public Thief(Char chr) {
        super(chr);
        if (chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
            for (int id : addedSkills) {
                if (!chr.hasSkill(id)) {
                    Skill skill = SkillData.getSkillDeepCopyById(id);
                    if (skill != null) {
                        skill.setCurrentLevel(skill.getMasterLevel());
                        chr.addSkill(skill);
                    }
                }
            }
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isAdventurerThief(id);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleAttack(c, attackInfo, si, now);
        }
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        if (hasHitMobs) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (tsm.hasStat(DarkSight)) {
                tsm.removeStat(DarkSight);
            }
        }
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleSkill(c, inPacket, skillUseInfo);
        }
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        switch (skillID) {
            case DARK_SIGHT:
                darkSightCount = 0;
            case SHADOW_WALKER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(DarkSight, o1);
                break;
            case MAPLE_RETURN:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(DARK_SIGHT)) {
            if (hitInfo.hpDamage <= 0 && tsm.getOptByCTSAndSkill(DarkSight, DARK_SIGHT) != null) {
                darkSightCount++;
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        // hacks to bypass the quest glitch (accept but no packet)
        var sm = chr.getScriptManager();
        if (level == 60 || level == 100) {
            final short jobID = chr.getJob();
            if (!JobConstants.canJobAdvance(jobID)) {
                return;
            }
            final short next = JobConstants.nextJob(jobID);
            sm.setJob(next);
            sm.completeQuestNoRewards(level == 60 ? 1441 : 1456);
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv(next, 3);
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        if (cts == DarkSight) {
            for (Option removeOpt : options) {
                if (removeOpt == null) {
                    continue;
                }
                if (removeOpt.rOption == DARK_SIGHT) {
                    SkillInfo si = SkillData.getSkillInfoById(DARK_SIGHT);
                    int slv = chr.getSkillLevel(DARK_SIGHT);
                    int totalCooltime = (si.getValue(cooltime, slv) * 1000) * darkSightCount;
                    chr.addSkillCooldown(DARK_SIGHT, totalCooltime);
                }
            }
        }
        super.handleRemoveCTS(cts, options);
    }
}
