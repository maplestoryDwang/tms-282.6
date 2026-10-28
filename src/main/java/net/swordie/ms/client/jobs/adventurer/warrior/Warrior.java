package net.swordie.ms.client.jobs.adventurer.warrior;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.adventurer.Beginner;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.LeaveType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Warrior extends Beginner {

    public static final int MAPLE_RETURN = 1281;

    // V skills
    public static final int BLITZ_SHIELD_BUFF = 400001010;
    public static final int BLITZ_SHIELD_ATTACK = 400001011;

    private final int[] addedSkills = new int[]{
            MAPLE_RETURN,};

    private boolean removeBlitzNotByHit;

    public Warrior(Char chr) {
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
        return JobConstants.isAdventurerWarrior(id);
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
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        switch (skillID) {
            case MAPLE_RETURN:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case BLITZ_SHIELD_ATTACK:
                removeBlitzNotByHit = true;
                tsm.removeStatsBySkill(BLITZ_SHIELD_BUFF);
                break;
            case BLITZ_SHIELD_BUFF:
                o1.nOption = 1500;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(FifthAdvWarriorShield, o1);
                removeBlitzNotByHit = true;
                break;
        }
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(FifthAdvWarriorShield)) {
            int blockedHP = tsm.getOptByCTSAndSkill(FifthAdvWarriorShield, BLITZ_SHIELD_BUFF).nOption;
            if (hitInfo.hpDamage >= chr.getHP()) {
                hitInfo.hpDamage = chr.getHP() - blockedHP;
                removeBlitzNotByHit = false;
                tsm.removeStatsBySkill(BLITZ_SHIELD_BUFF);
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
            sm.completeQuestNoRewards(level == 60 ? 1430 : 1450);
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv(next, 3);
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        if (cts == FifthAdvWarriorShield && removeBlitzNotByHit) {
            chr.write(UserLocal.userBonusAttackRequest(BLITZ_SHIELD_ATTACK));
        }
        super.handleRemoveCTS(cts, options);
    }
}
