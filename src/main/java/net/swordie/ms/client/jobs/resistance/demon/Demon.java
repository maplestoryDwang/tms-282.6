package net.swordie.ms.client.jobs.resistance.demon;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Demon extends Job {

    //Demon Skills
    public static final int DARK_WINDS = 30010110;
    public static final int DEMONIC_BLOOD = 30010185;
    public static final int SECRET_ASSEMBLY = 30001281;

    // V Skills
    public static final int DEFENDER_OF_THE_DEMON = 400001013;
    public static final int DEFENDER_OF_THE_DEMON_MASTEMA_MARK = 400001016;

    private final int[] addedSkills = new int[]{
            SECRET_ASSEMBLY,
            DARK_WINDS,
            DEMONIC_BLOOD,
    };

    private long mastemaMarkTime;

    public Demon(Char chr) {
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
        return JobConstants.isDemon(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        super.handleAttack(c, attackInfo, si, now);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        Field field = chr.getField();
        Summon summon;
        switch (skillID) {
            case DEFENDER_OF_THE_DEMON:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 0);
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setPosition(chr.getPosition());
                field.spawnSummon(summon);
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        if (tsm.hasStatBySkillId(DEFENDER_OF_THE_DEMON_MASTEMA_MARK)) {
            if (hitInfo.hpDamage > 0) {
                Skill skill = chr.getSkill(DEFENDER_OF_THE_DEMON);
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                int slv = skill.getCurrentLevel();
                int dmgIntakeReduction = si.getValue(s, slv);
                int count = tsm.getOption(MastemaGuard).xOption;

                hitInfo.hpDamage -= (hitInfo.hpDamage * dmgIntakeReduction) / 100;

                if (count > 0) {
                    count--;
                    o1.setInMillis(true);
                    o1.nOption = 1;
                    o1.rOption = DEFENDER_OF_THE_DEMON_MASTEMA_MARK;
                    o1.tOption = (int) (mastemaMarkTime - System.currentTimeMillis());
                    o1.xOption = count;
                    tsm.sendStat(MastemaGuard, o1);
                } else {
                    tsm.removeStatsBySkill(DEFENDER_OF_THE_DEMON_MASTEMA_MARK);
                }
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    public void giveMastemasMark() {
        if (!chr.hasSkill(DEFENDER_OF_THE_DEMON)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        Skill skill = chr.getSkill(DEFENDER_OF_THE_DEMON);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        o.nOption = 1;
        o.rOption = DEFENDER_OF_THE_DEMON_MASTEMA_MARK;
        o.tOption = si.getValue(q, slv);
        o.xOption = 2;
        tsm.sendStat(MastemaGuard, o);
        mastemaMarkTime = System.currentTimeMillis() + (1000L * si.getValue(q, slv));
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
    }

    @Override
    public void handleAddCTS(CharacterTemporaryStat cts, List<Option> options) {
        super.handleAddCTS(cts, options);
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        super.handleRemoveCTS(cts, options);
    }

    // Character creation related methods ---------------------------------------------------------------------------------------------

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.START_MAP);
        chr.setStat(Stat.mp, 30);
        chr.setStat(Stat.mmp, 10);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (JobConstants.isDemonSlayer(chr.getJob())) {
            if (chr.getJob() == JobConstants.JobEnum.DEMON_SLAYER1.getJobId()) {
                if (chr.getLevel() < 30) {
                    sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                    return;
                }
                if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                    if (sm.getEmptyInventorySlots(1) < 2) {
                        sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                        return;
                    }
                    sm.jobAdvance(JobConstants.JobEnum.DEMON_SLAYER2.getJobId());
                    sm.giveItem(1142342);
                    sm.giveAndEquip(1099002);
                    sm.completeQuestNoRewards(23210);
                    sm.completeQuestNoRewards(23211);
                    sm.completeQuestNoRewards(23212);
                }
            } else if (chr.getJob() == JobConstants.JobEnum.DEMON_SLAYER2.getJobId()) {
                if (chr.getLevel() < 60) {
                    sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                    return;
                }
                if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                    if (sm.getEmptyInventorySlots(1) < 2) {
                        sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                        return;
                    }
                    sm.jobAdvance(JobConstants.JobEnum.DEMON_SLAYER3.getJobId());
                    sm.giveItem(1142343);
                    sm.giveAndEquip(1099003);
                    sm.completeQuestNoRewards(23213);
                    sm.completeQuestNoRewards(23214);
                    chr.addQRValue(23206, "1");
                }
            } else if (chr.getJob() == JobConstants.JobEnum.DEMON_SLAYER3.getJobId()) {
                if (chr.getLevel() < 100) {
                    sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                    return;
                }
                if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                    if (sm.getEmptyInventorySlots(1) < 2) {
                        sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                        return;
                    }
                    sm.jobAdvance(JobConstants.JobEnum.DEMON_SLAYER4.getJobId());
                    sm.giveItem(1142344);
                    sm.giveAndEquip(1099004);
                    sm.completeQuestNoRewards(23215);
                }
            } else {
                sm.sendSayOkay("#e目前的狀態無法轉職。");
            }
        } else if (JobConstants.isDemonAvenger(chr.getJob())) {
            if (chr.getJob() == JobConstants.JobEnum.DEMON_AVENGER1.getJobId()) {
                if (chr.getLevel() < 30) {
                    sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                    return;
                }
                if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                    if (sm.getEmptyInventorySlots(1) < 2) {
                        sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                        return;
                    }
                    sm.jobAdvance(JobConstants.JobEnum.DEMON_AVENGER2.getJobId());
                    sm.giveItem(1142554);
                    sm.giveAndEquip(1099007);
                    sm.completeQuestNoRewards(23210);
                    sm.completeQuestNoRewards(23211);
                    sm.completeQuestNoRewards(23212);
                }
            } else if (chr.getJob() == JobConstants.JobEnum.DEMON_AVENGER2.getJobId()) {
                if (chr.getLevel() < 60) {
                    sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                    return;
                }
                if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                    if (sm.getEmptyInventorySlots(1) < 2) {
                        sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                        return;
                    }
                    sm.jobAdvance(JobConstants.JobEnum.DEMON_AVENGER3.getJobId());
                    sm.giveItem(1142555);
                    sm.giveAndEquip(1099008);
                    sm.completeQuestNoRewards(23213);
                    sm.completeQuestNoRewards(23218);
                    chr.addQRValue(23206, "1");
                }
            } else if (chr.getJob() == JobConstants.JobEnum.DEMON_AVENGER3.getJobId()) {
                if (chr.getLevel() < 100) {
                    sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                    return;
                }
                if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                    if (sm.getEmptyInventorySlots(1) < 2) {
                        sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                        return;
                    }
                    sm.jobAdvance(JobConstants.JobEnum.DEMON_AVENGER4.getJobId());
                    sm.giveItem(1142556);
                    sm.giveAndEquip(1099009);
                    sm.completeQuestNoRewards(23221);
                }
            } else {
                sm.sendSayOkay("#e目前的狀態無法轉職。");
            }
        }
    }
}
