package net.swordie.ms.client.jobs.Jianghu;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.ExtraTMSSystem;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;

import java.util.EnumMap;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Lynn extends Job {

    public static final int FOCUS_HEAL = 172001001; // IndieLoopEffect (charID) + IndieDotHealHP (349)
    public static final int FOCUS_HEAL_2 = 172001003;
    public static final int PAN = 172000011; // Spirit Guide Unity: Pan | IndiePMdR (5-10-15)
    public static final int PENNI = 172100011; // Spirit Guide Unity: Penni | IndiePMdR (5-10-15)

    public static final int PECK = 172101003;
    public static final int PECK_III = 172111003;
    public static final int PECK_IV = 172121003;

    public static final int STRIKE_1 = 172001000;
    public static final int STRIKE_2 = 172101000;
    public static final int STRIKE_3 = 172111000;
    public static final int STRIKE_4 = 172121000;

    public static final int SWEEP = 172101001; // VSkillStackBuff
    public static final int PAEON = 172110011; // Spirit Guide Unity: Paeon | IndiePMdR (3-6-9)
    public static final int EARTH_PULVERIZATION = 172111001;
    public static final int PURIFY = 172111008; // IndieLoopEffect(charID) + IndiePeriodicalSkillActivation(6)
    public static final int PREDATOR_BLOW = 172121006;
    public static final int NATURE_PROVIDENCE = 172111009; // IndieIgnoreMobpdpR(30) + IndieItemDropRate(10)
    public static final int MOTHER_NATURE_TOUCH = 172121008; // IndieLoopEffect(charID) + IndieDotHealHP(222)

    // V Skills
    public static final int BEAST_RAGE = 400021134;
    public static final int BEAK_STRIKE = 400021137;
    public static final int FOCUS_AWAKEN = 400021138;
    public static final int NATURE_GRACE = 400021139;


    public int mode = 0;

    private final int[] addedSkills = new int[]{
    };

    public Lynn(Char chr) {
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
        return JobConstants.isLynn(id);
    }


    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {

        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        super.handleAttack(c, attackInfo, si, now);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        Option o1 = new Option();
        switch (skillID) {
            case SWEEP:
                updateVSkillStackBuff(chr, Math.max(tsm.getOption(CannonShooter_BFCannonBall).nOption - 1, 0));
                break;
            case EARTH_PULVERIZATION:
                chr.extraTMSSystem.magicNumber = 112;
                if (hasHitMobs) {
                    var mob = Util.getRandomFromCollection(chr.getField().getMobs(attackInfo.mobAttackInfo));
                    if (mob != null) {
                        chr.write(ExtraTMSSystem.earthPulverization(false, chr, mob.getObjectId()));
                        //chr.write(ExtraTMSSystem.earthPulverization(true, chr, 0));
                        break;
                    }
                }
                chr.write(ExtraTMSSystem.earthPulverization(false, chr, 0));
                //chr.write(ExtraTMSSystem.earthPulverization(true, chr, 0));
                break;
        }
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        OutPacket outPacket;
        switch (skillID) {
            case FOCUS_HEAL:
                if (chr.hasSkill(172120001)) {
                    var boostSI = SkillData.getSkillInfoById(172120001);
                    var boostSLV = chr.getSkillLevel(172120001);
                    o1.nValue = boostSI.getValue(pad, boostSLV);
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndiePAD, o1);
                    o2.nValue = boostSI.getValue(mad, boostSLV);
                    o2.nReason = skillID;
                    o2.tTerm = si.getValue(time, slv);
                    newStats.put(IndieMAD, o2);
                }
                o3.nValue = chr.getId();
                o3.nReason = skillID;
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieLoopEffect, o3);
                o4.nValue = chr.getMaxHP() * si.getValue(s2, slv);
                o4.nReason = skillID;
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieDotHealHP, o4);
                tsm.sendStat(newStats);
                chr.write(ExtraTMSSystem.focusHeal1(chr));
                chr.write(ExtraTMSSystem.focusHeal2(chr));
                var effect = Effect.skillSpecialAffected(skillID, chr.getLevel());
                chr.write(UserPacket.effect(effect));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
                break;
            case PURIFY:
                o1.nValue = chr.getId();
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieLoopEffect, o1);
                o2.nValue = 10;
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePeriodicalSkillActivation, o2);
                tsm.sendStat(newStats);
                break;
            case 172111006:
                chr.extraTMSSystem.magicNumber = 112;
                outPacket = new OutPacket(OutHeader.LYNN_RESULT);
                outPacket.encodeInt(chr.getId());
                outPacket.encodeByte(7);
                outPacket.encodeInt(112);
                outPacket.encodeInt(ExtraTMSSystem.unkNum2);
                outPacket.encodeByte(1);
                outPacket.encodeShort(21);
                outPacket.encodeShort(125);
                outPacket.encodeShort(27);
                outPacket.encodeInt(skillID);
                outPacket.encodeByte(0);
                outPacket.encodeByte(0);
                outPacket.encodeByte(0);
                chr.write(outPacket);
                break;
            case PECK:
            case PECK_III:
            case PECK_IV:
                if (mode == 0) {
                    mode = skillID;
                } else {
                    mode = 0;
                }
                chr.extraTMSSystem.magicNumber = 112;
                chr.write(ExtraTMSSystem.peck(chr, mode));
                break;
        }
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HENESYS_ID);
        cs.setJob(JobConstants.JobEnum.LYNN_1.getJobId());
        cs.setLevel(10);
        cs.setStr(4);
        cs.setDex(45);
        cs.setInt(4);
        cs.setLuk(4);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            cs.setMp(500);
            cs.setMaxMp(500);
        }
        cs.getExtendSP().addSpToJobLevel(1, 5);
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        super.handleInitAfterMigrate(chr);
        if (chr.getLevel() < 30) {
            ScriptManagerImpl sm = chr.getScriptManager();
            for (int qid = 66908; qid <= 66920; qid++) {
                sm.completeQuestNoRewards(qid);
            }
            boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
            if (tms282) sm.setJob(JobConstants.JobEnum.LYNN_2.getJobId());
            sm.levelUntil(30);
            if (!tms282) sm.setJob(JobConstants.JobEnum.LYNN_2.getJobId());
            sm.addSPJobAdv(JobConstants.JobEnum.LYNN_1.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.LYNN_2.getJobId(), 3);
            sm.giveAndEquip(1252002);
            sm.giveAndEquip(1352811);
            sm.warp(FieldConstants.HOME_MAP);
        }
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        var sm = chr.getScriptManager();
        if (level == 60) {
            final var jobID = chr.getJob();
            sm.setJob((short) (jobID + 1));
            for (int qid = 66921; qid <= 66925; qid++) {
                sm.completeQuestNoRewards(qid);
            }
            sm.completeQuestNoRewards(66941);
            sm.completeQuestNoRewards(66954);
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv((short) (jobID + 1), 3);
        } else if (level == 100) {
            final var jobID = chr.getJob();
            sm.setJob((short) (jobID + 1));
            for (int qid = 66926; qid <= 66934; qid++) {
                sm.completeQuestNoRewards(qid);
            }
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv((short) (jobID + 1), 3);
        }else if (level == 200) {
            sm.completeQuestNoRewards(66935);
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
    }

    public void incVSkillStackBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 1;
        count = tsm.getOption(CannonShooter_BFCannonBall).nOption;
        var maxCharges = SkillData.getSkillInfoById(SWEEP).getValue(y, chr.getSkillLevel(SWEEP));
        if (count < maxCharges) {
            count++;
            updateVSkillStackBuff(chr, count);
        }
    }
}
