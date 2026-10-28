package net.swordie.ms.client.jobs.adventurer.thief;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.ExtraSkill;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
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

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class DualBlade extends Job {

    public static final int RETURN = 1282;

    public static final int DARK_SIGHT = 4001003; //Buff

    public static final int SELF_HASTE = 4301003; //Buff

    public static final int KATARA_BOOSTER = 4311009; //Buff

    public static final int FLASHBANG = 4321002; //Special Attack

    public static final int CHAINS_OF_HELL = 4331006; //Special Attack (Stun Debuff)
    public static final int MIRROR_IMAGE = 4331002; //Buff
    public static final int SHADOW_MELD = 4330009;
    public static final int VENOM = 4320005;
    public static final int LIFE_DRAIN = 4330007;

    public static final int FINAL_CUT = 4341002; //Special Attack
    public static final int SUDDEN_RAID = 4341011; //Special Attack
    public static final int MAPLE_WARRIOR_DB = 4341000; //Buff
    public static final int MIRRORED_TARGET = 4341006; //Summon
    public static final int TOXIC_VENOM = 4340012;
    public static final int HEROS_WILL = 4341008;
    public static final int EPIC_ADVENTURE = 4341053;
    public static final int BLADE_CLONE = 4341054;
    public static final int ASURAS_ANGER = 4341052;

    // V Skills
    public static final int SHADOW_WALKER = 400001023;
    public static final int BLADE_TEMPEST = 400041006;
    final Tms282BladeTempest bladeTempest = new Tms282BladeTempest();
    public static final int BLADES_OF_DESTINY = 400041021;
    public static final int BLADE_TORNADO = 400041042;
    public static final int BLADE_TORNADO_SHOOT_OBJECT = 400041043;
    public static final int HAUNTED_EDGE = 400041075;
    public static final int HAUNTED_EDGE_ASURA = 400041076;
    public static final int HAUNTED_EDGE_ASURA_WIND = 400041077;
    public static final int HAUNTED_EDGE_YAKSA = 400041078;

    // HEXA Skills
    public static final int KARMA_BLADE = 4361500;
    public static final int KARMA_BLADE_BUFF = 4361501;
    public static final int KARMA_BLADE_EXTRA = 4361502;
    public static final int KARMA_BLADE_AFTER_ATTACK = 4361503;
    public static final int HEXA_ASURAS_ANGER = 4361001;
    public static final int HEXA_BLADE_CLONE = 4361002;
    public static final int HEXA_BLADE_FURY = 4361003;
    public static final int FURY_JET = 4361004;
    public static final int HEXA_SUDDEN_RAID = 4361005;
    public static final int MORTALITY = 4360006;

    public static long lastShadowMeld = Long.MIN_VALUE;
    private int darkSightCount = 0;

    private final int[] addedSkills = new int[]{
            RETURN
    };

    public DualBlade(Char chr) {
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
        return JobConstants.isDualBlade(id);
    }

    public void giveShadowMeld() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(SHADOW_MELD)) {
            if (tsm.getOptByCTSAndSkill(IndiePAD, SHADOW_MELD) == null) {
                Skill skill = chr.getSkill(SHADOW_MELD);
                int slv = skill.getCurrentLevel();
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                if (lastShadowMeld + 5000 < System.currentTimeMillis()) {
                    Option o1 = new Option();
                    o1.nValue = si.getValue(indiePad, slv);
                    o1.nReason = skill.getSkillId();
                    o1.tTerm = si.getValue(time, slv);
                    tsm.sendStat(IndiePAD, o1); //Indie
                    lastShadowMeld = System.currentTimeMillis();
                }
            }
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        if (skillID == 400041018 || skillID == 4120019 || skillID == 4100012) {
            //This skill not active type like create atom, dot damage,...
            return;
        }
        applyPassiveDoTSkillsOnMob(mob, damage);
        Option o1 = new Option();
        Option o2 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        BurnedInfo bi;
        switch (skillID) {
            case SUDDEN_RAID:
            case HEXA_SUDDEN_RAID:
                chr.reduceSkillCoolTime(FINAL_CUT, (long) (chr.getRemainingCoolTime(FINAL_CUT) * 0.2F));
                bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skillID);
                break;
            case FLASHBANG:
                if (Util.succeedProp(si.getValue(prop, slv)) && !mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nValue = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    map.put(MobStat.IndieTotalDam, o1);
                    o2.nOption = 1;
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(time, slv);
                    map.put(MobStat.Blind, o2);
                    mts.addStatOptions(mob, map);
                }
                break;
            case CHAINS_OF_HELL:
                if (Util.succeedProp(si.getValue(prop, slv)) && !mts.hasCurrentMobStatBySkillId(skillID) && !mob.isBoss()) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o1);
                }
                break;
        }
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
        }
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
        if (skillID == 400041018 || skillID == 4120019 || skillID == 4100012) {
            //This skill not active type like create atom, dot damage,...
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (hasHitMobs) {
            if (tsm.hasStat(DarkSight)) {
                tsm.removeStat(DarkSight);
            }
            if (tsm.hasStat(KarmaBlade) && SkillData.getSkillInfoById(KARMA_BLADE_BUFF).getSkillList1().contains(skillID)) {
                Option opt = tsm.getOption(KarmaBlade);
                opt.nOption = Math.max(opt.nOption - 1, 0);
                if (opt.nOption == 0) {
                    tsm.removeStat(KarmaBlade);
                    ExtraSkill extraSkill = new ExtraSkill(KARMA_BLADE_EXTRA, chr.getPosition());
                    extraSkill.Value = 1;
                    chr.write(UserLocal.registerExtraSkill(KARMA_BLADE_EXTRA, Collections.singletonList(extraSkill)));
                } else {
                    tsm.updateStat(KarmaBlade, opt);
                    chr.write(UserLocal.userBonusAttackRequest(KARMA_BLADE_BUFF));
                }
            }
            if (chr.hasSkill(MORTALITY) && SkillData.getSkillInfoById(MORTALITY).getSkillList1().contains(skillID)) {
                Option opt = tsm.getOption(Mortality);
                opt.nOption = Math.min(opt.nOption + 1, 15);
                opt.rOption = MORTALITY;
                if (opt.nOption >= 15) {
                    opt.nOption = 0;
                    chr.write(UserLocal.userBonusAttackRequest(MORTALITY));
                }
                tsm.sendStat(Mortality, opt);
            }
            recoverHPByLifeDrain();
        }
        Option o1 = new Option();
        switch (skillID) {
            case CHAINS_OF_HELL:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = 1500;
                o1.setInMillis(true);
                tsm.sendStat(IndieNotDamaged, o1);
                break;
            case BLADES_OF_DESTINY:
                o1.nValue = si.getValue(ignoreMobpdpR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(cooltime, slv);
                tsm.sendStat(IndieIgnoreMobpdpR, o1);
                break;
            case KARMA_BLADE_AFTER_ATTACK:
                if (!tsm.hasStat(KarmaBlade)) {
                    o1.nOption = 25;
                    o1.rOption = KARMA_BLADE_BUFF;
                    o1.tOption = 20;
                    tsm.sendStat(KarmaBlade, o1);
                }
                break;
        }
    }

    private void applyPassiveDoTSkillsOnMob(Mob mob, long damage) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        if (chr.hasSkill(TOXIC_VENOM)) {
            Skill skill = chr.getSkill(TOXIC_VENOM);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            if (Util.succeedProp(proc)) {
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, TOXIC_VENOM, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, TOXIC_VENOM);
            }
        } else if (chr.hasSkill(VENOM)) {
            Skill skill = chr.getSkill(VENOM);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            if (Util.succeedProp(proc)) {
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, VENOM, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, VENOM);
            }
        }
    }

    private void recoverHPByLifeDrain() {
        if (chr.hasSkill(LIFE_DRAIN)) {
            Skill skill = chr.getSkill(LIFE_DRAIN);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            int amounthealed = si.getValue(x, slv);
            if (Util.succeedProp(proc)) {
                int healamount = (int) ((chr.getMaxHP()) / ((double) 100 / amounthealed));
                chr.heal(healamount);
            }
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (faSkill == BLADE_CLONE && tsm.hasStatBySkillId(BLADE_CLONE)) {
            if (chr.hasSkill(HEXA_BLADE_CLONE)) return HEXA_BLADE_CLONE;
        }
        return super.getFinalAttackSkill(faSkill);
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
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        switch (skillID) {
            case RETURN:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case DARK_SIGHT:
                darkSightCount = 0;
                // Fall through intended
            case SHADOW_WALKER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(DarkSight, o1);
                break;
            case SELF_HASTE:
                o1.nOption = si.getValue(indieSpeed, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(IndieSpeed, o1);
                o2.nOption = si.getValue(indieJump, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(IndieJump, o2);
                tsm.sendStat(newStats);
                break;
            case MIRROR_IMAGE:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ShadowPartner, o1);
                break;
            case MIRRORED_TARGET:
                if (tsm.getOptByCTSAndSkill(ShadowPartner, MIRROR_IMAGE) != null) {
                    Summon summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                    Field field = c.getChr().getField();
                    summon.setFlyMob(false);
                    summon.setMoveAction((byte) 0);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setAssistType(AssistType.None);
                    summon.setAttackActive(false);
                    summon.setAvatarLook(chr.getAvatarData().getAvatarLook());
                    summon.setMaxHP(si.getValue(x, slv));
                    summon.setHp(summon.getMaxHP());
                    field.spawnSummon(summon);

                    tsm.removeStatsBySkill(MIRROR_IMAGE);
                }
                break;
            case EPIC_ADVENTURE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case BLADE_CLONE:
            case HEXA_BLADE_CLONE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(WindBreakerFinal, o1);
                o2.nValue = si.getValue(indieDamR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o2);
                tsm.sendStat(newStats);
                break;
            case ASURAS_ANGER:
            case HEXA_ASURAS_ANGER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Asura, o1);
                o2.nValue = 50; // s5
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamReduceR, o2);
                tsm.sendStat(newStats);
                o3.nValue = 1;
                o3.nReason = skillID;
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieCheckTimeByClient, o3);
                tsm.sendStat(newStats);
                o4.nValue = 1;
                o4.nReason = skillID;
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieAntiMagicShell, o4);
                tsm.sendStat(newStats);
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (hitInfo.hpDamage <= 0) {
            giveShadowMeld();
        }
        if (chr.hasSkill(DARK_SIGHT)) {
            if (hitInfo.hpDamage <= 0 && tsm.getOptByCTSAndSkill(DarkSight, DARK_SIGHT) != null) {
                darkSightCount++;
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);
        cs.setJob(JobConstants.JobEnum.THIEF.getJobId());
        cs.setLevel(10);
        cs.setStr(4);
        cs.setDex(4);
        cs.setInt(4);
        cs.setLuk(45);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            cs.setMp(500);
            cs.setMaxMp(500);
        }
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        super.handleInitAfterMigrate(chr);
        if (chr.getLevel() < 30) {
            ScriptManagerImpl sm = chr.getScriptManager();
            for (int qid = 2604; qid <= 2617; qid++) {
                sm.completeQuestNoRewards(qid);
            }
            boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
            // Select the tutorial's target before leveling so automatic advancement cannot grant it twice.
            if (tms282) sm.setJob(JobConstants.JobEnum.BLADE_RECRUIT.getJobId());
            sm.levelUntil(20);
            if (!tms282) sm.setJob(JobConstants.JobEnum.BLADE_RECRUIT.getJobId());
            sm.addSPJobAdv(JobConstants.JobEnum.THIEF.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_RECRUIT.getJobId(), 3);
            sm.completeQuestNoRewards(2622);
            sm.completeQuestNoRewards(2623);
            sm.setJob(JobConstants.JobEnum.BLADE_ACOLYTE.getJobId());
            sm.levelUntil(30);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_RECRUIT.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_ACOLYTE.getJobId(), 3);
            sm.completeQuestNoRewards(2637);
            sm.completeQuestNoRewards(2638);
            sm.giveAndEquip(1332009);
            sm.giveAndEquip(1342001);
            sm.warp(FieldConstants.HOME_MAP);
        }
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        ScriptManagerImpl sm = chr.getScriptManager();
        if (level >= 45 && level < 60 && chr.getJob() == JobConstants.JobEnum.BLADE_ACOLYTE.getJobId()) {
            sm.setJob(JobConstants.JobEnum.BLADE_SPECIALIST.getJobId());
            sm.completeQuestNoRewards(2640);
            sm.completeQuestNoRewards(2641);
            sm.completeQuestNoRewards(2642);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_ACOLYTE.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_SPECIALIST.getJobId(), 3);
        } else if (level >= 60 && level < 100 && chr.getJob() == JobConstants.JobEnum.BLADE_SPECIALIST.getJobId()) {
            sm.setJob(JobConstants.JobEnum.BLADE_LORD.getJobId());
            sm.giveAndEquip(1142109);
            sm.completeQuestNoRewards(1441);
            sm.completeQuestNoRewards(1443);
            sm.completeQuestNoRewards(2643);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_SPECIALIST.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_LORD.getJobId(), 3);
        } else if (level >= 100 && level < 140 && chr.getJob() == JobConstants.JobEnum.BLADE_LORD.getJobId()) {
            sm.setJob(JobConstants.JobEnum.BLADE_MASTER.getJobId());
            sm.giveAndEquip(1142110);
            sm.completeQuestNoRewards(1456);
            sm.completeQuestNoRewards(1457);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_LORD.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.BLADE_MASTER.getJobId(), 5);
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

    @Override
    public void handleKeyDownSkill(Char chr, SkillInfo si, InPacket inPacket) {
        int skillId = si.getSkillId();
        if (Tms282BladeTempest.handles(skillId)) return;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (skillId) {
            case FINAL_CUT:
                o1.nValue = 1;
                o1.nReason = skillId;
                o1.tTerm = 3;
                newStats.put(IndieNotDamaged, o1);
                o2.nValue = 1;
                o2.nReason = skillId;
                o2.tTerm = 3;
                newStats.put(IndieIgnorePCounter, o2);
                tsm.sendStat(newStats);
                break;
            case BLADE_TEMPEST:
                o1.nValue = 50; // s5
                o1.nReason = skillId;
                o1.tTerm = 3;
                newStats.put(IndieDamReduceR, o1);
                o2.nValue = 1;
                o2.nReason = skillId;
                o2.tTerm = 3;
                newStats.put(IndieAntiMagicShell, o2);
                tsm.sendStat(newStats);
                o3.nOption = 200;
                o3.rOption = skillId;
                tsm.sendStat(KeyDownMoving, o3);
                break;
        }
        super.handleKeyDownSkill(chr, si, inPacket);
    }

    @Override
    public void handleCancelKeyDownSkill(Char chr, int skillID) {
        if (Tms282BladeTempest.stop(chr, skillID)) return;
        switch (skillID) {
            case FINAL_CUT:
                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                SkillInfo si = SkillData.getSkillInfoById(skillID);
                int slv = chr.getSkillLevel(skillID);
                Option o1 = new Option();
                o1.nOption = si.getValue(y, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(FinalCut, o1);
                chr.heal(-(chr.getMaxHP() * si.getValue(x, slv)) / 100);
                break;
        }
        super.handleCancelKeyDownSkill(chr, skillID);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        Tms282BladeTempest.reset(chr);
        super.handleCancelTimer(chr);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case FURY_JET -> {
                chr.addSkillCooldown(skillId, 6000);
                return 1;
            }
            case HEXA_SUDDEN_RAID -> {
                int skillID = SUDDEN_RAID;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_ASURAS_ANGER -> {
                int skillID = ASURAS_ANGER;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
