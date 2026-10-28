package net.swordie.ms.client.jobs.adventurer;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.InvType.EQUIPPED;

/**
 * Created on 12/14/2017.
 */
public class Kinesis extends Job {

    public static final int RETURN_KINESIS = 140001290;
    public static final int PSYCHIC_ATTACK = 140001289;

    public static final int PSYCHIC_FORCE = 142001000;
    public static final int KINETIC_CRASH = 142001001;
    public static final int MENTAL_SHIELD = 142001007;
    public static final int ESP_BOOSTER = 142001003;
    public static final int ULTIMATE_METAL_PRESS = 142001002;
    public static final int PSYCHIC_BLAST_FWD = 142100000;
    public static final int PSYCHIC_BLAST_DOWN = 142100001;
    public static final int PSYCHIC_ASSAULT_FWD = 142110000;
    public static final int PSYCHIC_ASSAULT_DOWN = 142110001;
    public static final int PSYCHIC_DRAIN = 142101009; // TODO, AffectedArea?
    public static final int PSYCHIC_ARMOR = 142101004;
    public static final int KINETIC_PILEDRIVER = 142101002;
    public static final int ULTIMATE_DEEP_IMPACT = 142101003;
    public static final int PSYCHIC_BULWARK = 142110009;
    public static final int PSYCHIC_GRAB = 142111002;
    public static final int MIND_TREMOR = 142111006;
    public static final int KINETIC_JAUNT = 142111010;
    public static final int ULTIMATE_TRAINWRECK = 142111007;
    public static final int KINETIC_COMBO = 142110011;
    public static final int MIND_BREAK = 142121004;
    public static final int PSYCHIC_CLUTCH = 142120000;
    public static final int ULTIMATE_PSYCHIC_SHOT = 142120002;
    public static final int MIND_QUAKE = 142120003;
    public static final int ULTIMATE_BPM = 142120002;
    public static final int ULTIMATE_BPM_ATTACK = 142121005;
    public static final int CLEAR_MIND = 142121007;
    /** 282: 精神淨化 is 142121005 (142121007 is a 265-only id); 142001003 is 心靈漫步 (movement), not ESP Booster. */
    public static final int CLEAR_MIND_282 = 142121005;
    /** 282: 142121004 is 異界祝禱 (psd passive, basicStatUp u(x/2), no time), not the 265 attack 破魂. */
    public static final int OTHERWORLD_BLESSING_282 = MIND_BREAK;
    /** 282-only: 心靈遊動 (subTime/u/v/w, cooltime 2 s, no stat key) and 永恆壞滅 (ndTime 10000, x 180, cooltime 120). */
    public static final int PSYCHIC_GLIDE_282 = 142111005;
    public static final int ETERNAL_DESTRUCTION_282 = 142121041;

    private static boolean tms282() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }
    public static final int PRESIDENTS_ORDERS = 142121016;
    public static final int PSYCHIC_CHARGER = 142121008;
    public static final int MENTAL_TEMPEST = 142121030;
    public static final int MENTAL_SHOCK = 142121031;
    public static final int MENTAL_OVERDRIVE = 142121032;

    // V skills
    public static final int PSYCHIC_TORNADO = 400021008;
    public static final int PSYCHIC_TORNADO_1 = 400021009;
    public static final int PSYCHIC_TORNADO_2 = 400021010;
    public static final int PSYCHIC_TORNADO_3 = 400021011;
    public static final int MIND_OVER_MATTER = 400021048;
    public static final int ULTIMATE_MIND_OVER_MATTER_EXPLODE = 400021053;
    public static final int ULTIMATE_PSYCHIC_SHOCKWAVE = 400021074;
    public static final int ULTIMATE_PSYCHIC_SHOCKWAVE_BLACKHOLE = 400021075;
    public static final int ULTIMATE_PSYCHIC_SHOCKWAVE_SHOOT = 400021076;
    public static final int LAW_OF_GRAVITY = 400021096; // Used as rOpt for MobStat | Used as rOpt for Buff
    public static final int LAW_OF_GRAVITY_2 = 400021097;
    public static final int LAW_OF_GRAVITY_3 = 400021098;
    public static final int LAW_OF_GRAVITY_4 = 400021104; // AA

    private final int[] addedSkills = new int[]{
    };

    private final int[] nonOrbSkills = new int[]{
            ULTIMATE_METAL_PRESS,
            ULTIMATE_BPM,
            ULTIMATE_DEEP_IMPACT,
            ULTIMATE_PSYCHIC_SHOT,
            ULTIMATE_TRAINWRECK,
            PSYCHIC_FORCE,
            PSYCHIC_DRAIN,
            MENTAL_TEMPEST,
            KINETIC_COMBO,};

    private static final int MAX_PP = 30;

    public Kinesis(Char chr) {
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
        return JobConstants.isKinesis(id);
    }

    public int getPP() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(KinesisPsychicPoint)) {
            return tsm.getOption(KinesisPsychicPoint).nOption;
        }
        return 0;
    }

    public int getMaxPP() {
        return MAX_PP;
    }

    public void addPP(int amount) {
        int pp = Math.min(getPP() + amount, MAX_PP);
        sendPPPacket(pp);
    }

    public void substractPP(int amount) {
        int pp = Math.max(getPP() - amount, 0);
        sendPPPacket(pp);
    }

    private void sendPPPacket(int pp) {
        Option o = new Option();
        o.nOption = pp;
        o.rOption = JobConstants.JobEnum.KINESIS_4.getJobId();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.sendStat(KinesisPsychicPoint, o);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case PSYCHIC_FORCE:
            case PSYCHIC_BLAST_FWD:
            case PSYCHIC_BLAST_DOWN:
            case PSYCHIC_ASSAULT_FWD:
            case PSYCHIC_ASSAULT_DOWN:
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skillID);
                break;
            case MENTAL_SHOCK:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o.nOption = 1;
                    o.rOption = skillID;
                    o.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o);
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
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        // 282 Kinesis attacks cost MP (no PP any more). Char.applyMpCon still lets every 265 Kinesis attack through
        // (!isUserSkillUseRequest), so the server never lowered MP and the next STAT_CHANGED gave it back (audit 09-29).
        // Charge it here with the same rules the attack path applies to other jobs.
        if (tms282() && si != null && skillID != 0
                && !net.swordie.ms.constants.SkillConstants.isShootObj(si)
                && !net.swordie.ms.constants.SkillConstants.isNoMPConsumeSkill(skillID)
                && !net.swordie.ms.constants.SixthJobSkillConstants.isRecognizedOriginSkill(skillID, si)
                && !net.swordie.ms.constants.SixthJobSkillConstants.isRecognizedAscentSkill(skillID, si)) {
            chr.applyMpCon(si, slv, true);
        }
        if (tms282() && skillID != 0) {
            // 釋放: the 終極技 cooldown the attack path just stored becomes unleashCooltime
            net.swordie.ms.client.character.tms282.Tms282KinesisUnleash.afterAttack(chr, skillID, System.currentTimeMillis());
        }
        // 282 has no 心魂之球 (265 142110011 stays in old characters' records; its force atom has no 282 codec)
        if (hasHitMobs && chr.hasSkill(KINETIC_COMBO) && !tms282()) {
            createKineticOrbForceAtom(skillID, slv, attackInfo);
        }
        if (hasHitMobs && skillID != ULTIMATE_BPM &&
                skillID != ULTIMATE_METAL_PRESS &&
                skillID != ULTIMATE_TRAINWRECK &&
                skillID != MIND_OVER_MATTER &&
                skillID != ULTIMATE_PSYCHIC_SHOT &&
                skillID != PSYCHIC_TORNADO_1 &&
                skillID != PSYCHIC_TORNADO_2 &&
                skillID != PSYCHIC_TORNADO_3 &&
                skillID != PSYCHIC_TORNADO
        ) {
            kinesisPPAttack(skillID, slv, si, attackInfo);
        }
        Option o = new Option();
        switch (skillID) {
            case MIND_BREAK:
                if (tms282()) {
                    break; // 282 異界祝禱 is a passive, never an attack: no 265 破魂 IndiePMdR stack
                }
                int count = 0;
                for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                    Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                    if (mob == null || mob.getHp() <= 0) {
                        continue;
                    }
                    if (mob.isBoss()) {
                        count += si.getValue(x, slv);
                    } else {
                        count++;
                    }
                }
                count = Math.min(count, si.getValue(w, slv));
                if (count <= 0) {
                    return;
                }
                o.nValue = count * si.getValue(indiePMdR, slv);
                o.nReason = skillID;
                o.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePMdR, o);
                break;
        }
    }

    private void kinesisPPAttack(int skillID, int slv, SkillInfo si, AttackInfo ai) {
        if (ai != null) {
            if (ai.skillId == ULTIMATE_PSYCHIC_SHOCKWAVE_BLACKHOLE) {
                if (ai.mobCount > 0) {
                    addPP(1);
                }
                return;
            }
            if (ai.skillId == ULTIMATE_PSYCHIC_SHOCKWAVE_SHOOT) {
                boolean HitBoss = false;
                for (int i = 0; i < ai.mobAttackInfo.size(); ++i) {
                    Mob mob = (Mob) chr.getField().getLifeByObjectID(ai.mobAttackInfo.get(i).mobId);
                    if (mob == null || mob.getHp() <= 0) {
                        continue;
                    }
                    HitBoss |= mob.isBoss();
                }
                if (HitBoss) {
                    addPP(1);
                }
                return;
            }
            if (ai.skillId == ULTIMATE_PSYCHIC_SHOCKWAVE) {
                int ppCons = si.getValue(ppCon, slv);
                substractPP(ppCons);
                return;
            }
        }
        if (si == null) {
            if (skillID == 0) {
                addPP(1);
            }
            return;
        }
        int ppRec = si.getValue(ppRecovery, slv);
        addPP(ppRec);
        int ppCons = si.getValue(ppCon, slv);
        if (chr.getTemporaryStatManager().hasStat(KinesisPsychicOver)) {
            ppCons = ppCons / 2;
        }
        if (skillID == ULTIMATE_BPM) {
            ppCons = si.getValue(w, slv); // why nexon..
        }
        if (skillID == KINETIC_JAUNT) {
            ppCons = si.getValue(x, slv); // why nexon..
        }
        substractPP(ppCons);
    }

    private void createKineticOrbForceAtom(int skillID, int slv, AttackInfo attackInfo) {
        if (Collections.singletonList(nonOrbSkills).contains(skillID) || Collections.singletonList(nonOrbSkills).contains(skillID)) {
            return;
        }
        Field field = chr.getField();
        SkillInfo si = SkillData.getSkillInfoById(KINETIC_COMBO);
        int proc = si.getValue(prop, chr.getSkillLevel(KINETIC_COMBO));
        ForceAtomEnum fae = ForceAtomEnum.KINESIS_ORB_REAL;
        Rect rect = chr.getRectAround(new Rect(-500, -300, 200, 100));
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        List<Integer> targetList = new ArrayList<>();
        List<ForceAtomInfo> faiList = new ArrayList<>();

        if (Util.succeedProp(proc) && field.getMobsInRect(rect).size() > 0) {
            Mob mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
            targetList.add(mob.getObjectId());

            int ranStuff = new Random().nextInt(3);
            int fImpact = new Random().nextInt(31) + 20;
            int sImpact = new Random().nextInt(25) + 10;
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc() + ranStuff, fImpact, sImpact,
                    new Random().nextInt(360), new Random().nextInt(400) + 400, Util.getCurrentTime(), 0, 0,
                    new Position());
            faiList.add(fai);
        }

        if (faiList.size() > 0 && targetList.size() > 0) {
            ForceAtom fa = new ForceAtom(chr.getId(), fae, targetList, KINETIC_COMBO, faiList);
            chr.createForceAtom(fa);
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
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        if (tms282()) {
            // Kinesis audit 2026-09-29 (research/tms2823/kinesis-audit-0929): same ids, 282 meanings
            if (skillID == ESP_BOOSTER) {
                return; // 心靈漫步: client-side movement; the 265 IndieBooster had value 0 / time 0 here
            }
            if (skillID == CLEAR_MIND_282) {
                tsm.removeAllDebuffs();
                return;
            }
            if (net.swordie.ms.client.character.tms282.Tms282JobFixKinesis.handled282(chr, skillID, si, slv)) {
                return; // 異界祝禱 / 心靈遊動 / 永恆壞滅: see Tms282JobFixKinesis (job-fixes-0929)
            }
            if (skillID == net.swordie.ms.client.character.tms282.Tms282KinesisUnleash.UNLEASH) {
                net.swordie.ms.client.character.tms282.Tms282KinesisUnleash.started(chr,
                        si.getValue(net.swordie.ms.client.character.skills.SkillStat.time, slv) * 1000,
                        System.currentTimeMillis());
            }
        }
        switch (skillID) {
            case ESP_BOOSTER:
                o1.nValue = si.getValue(indieBooster, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieBooster, o1);
                break;
            case MENTAL_SHIELD:
                if (tsm.hasStat(KinesisPsychicEnergeShield)) {
                    tsm.removeStatsBySkill(skillID);
                    return;
                } else {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    tsm.sendStat(KinesisPsychicEnergeShield, o1);
                    chr.getField().broadcast(UserRemote.psychicEnergyShieldEffect(chr, true), chr);
                }
                break;
            case PSYCHIC_ARMOR:
            case PSYCHIC_BULWARK:
                int psyArmorSLV = chr.getSkillLevel(PSYCHIC_ARMOR);
                int t = SkillData.getSkillInfoById(PSYCHIC_ARMOR).getValue(time, psyArmorSLV);
                int e = SkillData.getSkillInfoById(PSYCHIC_ARMOR).getValue(er, psyArmorSLV);
                o1.nValue = si.getValue(indiePdd, slv);
                o1.nReason = skillID;
                o1.tTerm = t;
                newStats.put(IndiePDD, o1);
                o2.nValue = e;
                o2.nReason = skillID;
                o2.tTerm = t;
                newStats.put(IndieEVAR, o2);
                o3.nOption = si.getValue(stanceProp, slv);
                o3.rOption = skillID;
                o3.tOption = t;
                newStats.put(IndieStance, o3);
                tsm.sendStat(newStats);
                break;
            case KINETIC_JAUNT:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(NewFlying, o1); //38s
                break;
            case PSYCHIC_TORNADO:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Kinesis_DustTornado, o1);
                break;
            case PSYCHIC_CHARGER:
                int add = (MAX_PP - getPP()) / 2;
                addPP(add);
                break;
            case RETURN_KINESIS:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case CLEAR_MIND:
                tsm.removeAllDebuffs();
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStatBySkillId(MENTAL_SHIELD)) {
            hitInfo.hpDamage = (int) (hitInfo.hpDamage * (tsm.getOption(KinesisPsychicEnergeShield).nOption / 100D));
            substractPP(1);
        }
        if (getPP() <= 0) {
            tsm.removeStatsBySkill(MENTAL_SHIELD);
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(JobConstants.KINESIS_CREATION_MAP);
        cs.setLevel(10);
        cs.setInt(45);
        cs.setHp(574);
        cs.setMaxHp(574);
        cs.setMp(0);
        cs.setMaxMp(30);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.KINESIS_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.KINESIS_2.getJobId());
                sm.giveItem(1142864);
                sm.completeQuestNoRewards(22770);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.KINESIS_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.KINESIS_3.getJobId());
                sm.giveItem(1142865);
                sm.completeQuestNoRewards(22800);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.KINESIS_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.KINESIS_4.getJobId());
                sm.giveItem(1142866);
                sm.completeQuestNoRewards(22850);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
