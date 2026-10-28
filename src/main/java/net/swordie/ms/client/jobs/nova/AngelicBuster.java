package net.swordie.ms.client.jobs.nova;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
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
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
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
public class AngelicBuster extends Job {

    //AB Beginner Skills
    public static final int DRESS_UP = 60011222;
    public static final int SOUL_BUSTER = 60011216;
    public static final int HYPER_COORDINATE = 60011221;
    public static final int GRAPPLING_HEART = 60011218;
    public static final int DAY_DREAMER = 60011220;
    public static final int TRUE_HEART_INHERITANCE = 60010217;

    public static final int AB_NORMAL_ATTACK = 60011216;

    //1st Job
    public static final int STAR_BUBBLE = 65001100;
    public static final int MELODY_CROSS = 65001002; //Buff

    //2nd job
    public static final int LOVELY_STING = 65101100;
    public static final int LOVELY_STING_EXPLOSION = 65101006;
    public static final int PINK_PUMMEL = 65101001;
    public static final int POWER_TRANSFER = 65101002; //Buff

    //3rd Job
    public static final int SOUL_SEEKER = 65111100;
    public static final int SOUL_SEEKER_ATOM = 65111007;
    public static final int SHINING_STAR_BURST = 65111101;
    public static final int HEAVENLY_CRASH = 65111002;
    public static final int IRON_BLOSSOM = 65111004; //Buff

    //4th Job
    public static final int CELESTIAL_ROAR = 65121100;
    public static final int TRINITY = 65121101;
    public static final int TRINITY_2 = 65121007;
    public static final int TRINITY_3 = 65121008;
    //65121101 - Trinity -combo count-
    public static final int FINALE_RIBBON = 65121002;
    public static final int STAR_GAZER = 65121004; //Buff
    public static final int NOVA_WARRIOR_AB = 65121009; //Buff
    public static final int SOUL_SEEKER_EXPERT = 65121011; //ON/OFF Buff
    public static final int NOVA_TEMPERANCE_AB = 65121010;
    public static final int SOUL_RESONANCE = 65121003;

    public static final int SUPREME_SUPERNOVA = 65121052;

    //Hypers
    public static final int PRETTY_EXALTATION = 65121054;
    public static final int FINAL_CONTRACT = 65121053;

    //Affinity Heart Passives
    public static final int AFFINITY_HEART_I = 65000003;
    public static final int AFFINITY_HEART_II = 65100005;
    public static final int AFFINITY_HEART_III = 65110006;
    public static final int AFFINITY_HEART_IV = 65120006;

    // V skills
    public static final int SPARKLE_BURST = 400051011;
    public static final int SUPER_STAR_SPOTLIGHT = 400051018;
    public static final int SUPER_STAR_SPOTLIGHT_2 = 400051027; // number value
    public static final int MIGHTY_MASCOT = 400051046;
    public static final int TRINITY_FUSION = 400051072;

    private static final short INSTANT_RECHARGE_LEVEL = 140;

    private final int[] addedSkills = new int[]{
            DRESS_UP,
            SOUL_BUSTER,
            HYPER_COORDINATE,
            GRAPPLING_HEART,
            DAY_DREAMER,
            TRUE_HEART_INHERITANCE,
    };

    private int affinityHeartIIcounter = 0;
    private int affinityHeartIIIcounter = 0;

    public enum MightMascotSkillTypes {
        TwinkleStar(10),
        MagicalBalloon(11),
        ShinyBubbleBreath(12),
        ;

        int val;

        MightMascotSkillTypes(int val) {
            this.val = val;
        }

        public int getVal() {
            return val;
        }
    }

    public AngelicBuster(Char chr) {
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
        return JobConstants.isAngelicBuster(id);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        chr.setStat(Stat.level, 10);
        chr.setStat(Stat.dex, 49);
        chr.setStat(Stat.mp, 0);
        chr.setStat(Stat.mmp, 0);
        chr.getAvatarData().getCharacterStat().setPosMap(JobConstants.ANGELIC_BUSTER_CREATION_MAP);
        chr.getAvatarData().getCharacterStat().setJob(JobConstants.JobEnum.ANGELIC_BUSTER1.getJobId());
        chr.setSpToCurrentJob(5);
    }


    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case LOVELY_STING:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.wOption = chr.getId();
                    mts.addStatOptions(mob, MobStat.Explosion, o1);
                }
                break;
            case FINALE_RIBBON:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.TotalDamParty, o1);
                    //TODO Check if this is the Correct MobStat
                }
                break;
            case CELESTIAL_ROAR:    //Stun Debuff
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        if (Util.succeedProp(si.getValue(prop, slv))) {
                            o1.nOption = 1;
                            o1.rOption = skillID;
                            o1.tOption = si.getValue(time, slv);
                            mts.addStatOptions(mob, MobStat.Stun, o1);
                        }
                    }
                }
                break;
            case LOVELY_STING_EXPLOSION:
                if (mts.hasCurrentMobStat(MobStat.Explosion)) {
                    mts.removeMobStat(mob, MobStat.Explosion);
                }
                break;
            case HEAVENLY_CRASH:
                if (chr.hasSkill(SHINING_STAR_BURST)) {
                    if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                        o1.nOption = 100;
                        o1.rOption = SHINING_STAR_BURST;
                        o1.tOption = 5;
                        mts.addStatOptions(mob, MobStat.AddDamSkill, o1);
                    }
                }
                break;
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        super.handleAttack(c, attackInfo, si, now);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();

        if (hasHitMobs) {
            //Recharging System
            if (Util.succeedProp(getRechargeProc(attackInfo))) {
                rechargeABSkills();
                affinityHeartIIIcounter = 0;
            } else {
                //Affinity Heart IV passive
                if (chr.hasSkill(AFFINITY_HEART_IV) && Util.succeedProp(getRechargeProc(attackInfo))) {
                    Skill ah4Skill = chr.getSkill(AFFINITY_HEART_IV);
                    byte ah4LV = (byte) ah4Skill.getCurrentLevel();
                    SkillInfo ah4SI = SkillData.getSkillInfoById(ah4Skill.getSkillId());
                    if (Util.succeedProp(ah4SI.getValue(x, ah4LV))) {
                        rechargeABSkills();
                        affinityHeartIIIcounter = 0;
                        affinityHeartIV(tsm, ah4LV);
                    }
                } else {
                    //Affinity Heart III passive
                    if (!chr.hasSkill(AFFINITY_HEART_III)) {
                        return;
                    }
                    affinityHeartIIIcounter++;
                    if (affinityHeartIIIcounter > 2) {
                        rechargeABSkills();
                    }
                }
            }
            affinityHeartII(attackInfo);
            //Soul Seeker Expert
            if (attackInfo.skillId != SOUL_SEEKER_ATOM) {
                soulSeekerExpert(attackInfo.skillId, attackInfo.slv, attackInfo);
            }
        }
        Option o1 = new Option();
        switch (attackInfo.skillId) {
            case AB_NORMAL_ATTACK:
                soulSeekerExpert(AB_NORMAL_ATTACK, chr.getSkillLevel(AB_NORMAL_ATTACK), attackInfo);
                break;
            case TRINITY:
            case TRINITY_2:
            case TRINITY_3:
                trinityBuff(tsm);
                break;
            case HEAVENLY_CRASH:
                if (chr.hasSkill(SHINING_STAR_BURST)) {
                    o1.nOption = 1;
                    o1.rOption = SHINING_STAR_BURST;
                    o1.tOption = 5;
                    tsm.sendStat(NextAttackEnhance, o1);
                }
                break;
        }
    }

    private void soulSeekerExpert(int skillID, int slv, AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(AngelicBursterSoulSeeker)) {
            SkillInfo si = SkillData.getSkillInfoById(SOUL_SEEKER_EXPERT);
            int anglenum;
            if (new Random().nextBoolean()) {
                anglenum = 50;
            } else {
                anglenum = 130;
            }
            int TW1prop = si.getValue(prop, slv);
            if (skillID == CELESTIAL_ROAR) {
                TW1prop += si.getValue(z, slv);
            }
            if (tsm.getOptByCTSAndSkill(IndieIgnoreMobpdpR, PRETTY_EXALTATION) != null) {
                TW1prop += 15;
            }
            ForceAtomEnum fae = ForceAtomEnum.AB_ORB;
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 20, 40,
                    anglenum, 0, Util.getCurrentTime(), 1, 0,
                    new Position(5, 0)); //Slightly behind the player
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                if (Util.succeedProp(TW1prop)) {
                    final Position pos = mob.getPosition();
                    int mobID = mai.mobId;
                    chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                            true, mobID, SOUL_SEEKER_ATOM, forceAtomInfo, new Rect(), 0, 300,
                            pos, SOUL_SEEKER_ATOM, pos, 0));
                }
            }
        }
    }

    public void giveSpotlightBuff(boolean giveBuff, int stack) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(SUPER_STAR_SPOTLIGHT)
                || !tsm.hasStat(CharacterTemporaryStat.FifthSpotLight)
                || stack > 3
                || stack < 0) {
            return;
        }
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Skill skill = chr.getSkill(SUPER_STAR_SPOTLIGHT);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        if (giveBuff) {
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            Option o4 = new Option();
            Option o5 = new Option();

            o1.nValue = stack * si.getValue(w, slv);
            o1.nReason = SUPER_STAR_SPOTLIGHT_2;
            newStats.put(IndieAsrR, o1);
            o2.nValue = stack * si.getValue(v, slv);
            o2.nReason = SUPER_STAR_SPOTLIGHT_2;
            newStats.put(IndieCrR, o2);
            o3.nValue = stack * si.getValue(q, slv);
            o3.nReason = SUPER_STAR_SPOTLIGHT_2;
            newStats.put(IndieStance, o3);
            o4.nValue = stack * si.getValue(s, slv);
            o4.nReason = SUPER_STAR_SPOTLIGHT_2;
            newStats.put(IndiePMdR, o4);
            o5.nOption = stack;
            o5.rOption = SUPER_STAR_SPOTLIGHT_2;
            o5.xOption = stack;
            newStats.put(TempSecondaryStat, o5);
            tsm.sendStat(newStats);
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (cts == CharacterTemporaryStat.FifthSpotLight) {
            tsm.removeStatsBySkill(SUPER_STAR_SPOTLIGHT_2);
        }
        super.handleRemoveCTS(cts, options);
    }

    private void createSoulSeekerForceAtom() {
        Field field = chr.getField();
        SkillInfo si = SkillData.getSkillInfoById(SOUL_SEEKER);
        int slv = chr.getSkillLevel(SOUL_SEEKER);
        Rect rect = chr.getPosition().getRectAround(si.getRects().get(0));
        if (!chr.isLeft()) {
            rect = rect.moveRight();
        }
        List<Mob> lifes = field.getMobsInRect(rect);
        if (lifes.size() <= 0) {
            return;
        }
        var mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
        if (mob == null) {
            return;
        }
        int fImpact = new Random().nextInt(4) + 29;
        int angle = new Random().nextInt(10);
        int mobID = mob.getObjectId();
        ForceAtomEnum fae = ForceAtomEnum.AB_ORB;
        ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, 40,
                angle, 250, Util.getCurrentTime(), 0, 0,
                new Position());
        ForceAtom fa = new ForceAtom(false, chr.getId(), chr.getId(), fae,
                true, mobID, SOUL_SEEKER_ATOM, fai, new Rect(), 0, 0,
                new Position(), SOUL_SEEKER_ATOM, new Position(), 0);
        fa.setMaxRecreationCount(si.getValue(z, slv));
        fa.setRecreationChance(si.getValue(s, slv));
        chr.createForceAtom(fa);
    }

    private int getRechargeProc(AttackInfo attackInfo) {
        Skill skill = chr.getSkill(SkillConstants.getActualSkillIDfromSkillID(attackInfo.skillId));
        if (skill == null) {
            return 0;
        }
        int slv = skill.getCurrentLevel();
        SkillInfo rechargeInfo = SkillData.getSkillInfoById(skill.getSkillId());
        int rechargeproc = rechargeInfo.getValue(onActive, slv);
        if (rechargeproc == 0) {
            return rechargeproc;
        }
        if (chr.hasSkill(AFFINITY_HEART_I)) {
            SkillInfo ah1 = SkillData.getSkillInfoById(AFFINITY_HEART_I);
            int extraRecharge = ah1.getValue(x, slv);
            rechargeproc += (extraRecharge - 10);
        }

        return rechargeproc;
    }

    private void affinityHeartII(AttackInfo attackInfo) {
        if (!chr.hasSkill(AFFINITY_HEART_II)) {
            return;
        }
        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
            Life life = chr.getField().getLifeByObjectID(mai.mobId);
            if (life instanceof Mob mob) {
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                if (affinityHeartIIcounter >= 10) {
                    rechargeABSkills();
                    affinityHeartIIcounter = 0;
                    affinityHeartIIIcounter = 0;
                } else {
                    long totaldmg = Arrays.stream(mai.damages).sum();
                    if (totaldmg > mob.getHp()) {
                        affinityHeartIIcounter++;
                    }
                }
            }
        }
    }

    private void affinityHeartIV(TemporaryStatManager tsm, int slv) {
        if (!chr.hasSkill(AFFINITY_HEART_IV)) {
            return;
        }
        if (tsm.getOptByCTSAndSkill(IndieDamR, AFFINITY_HEART_IV) == null) {
            Skill skill = chr.getSkill(AFFINITY_HEART_IV);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            Option o = new Option();
            o.nValue = si.getValue(y, slv);
            o.nReason = skill.getSkillId();
            o.tTerm = 5;
            tsm.sendStat(IndieDamR, o);
        }
    }

    private void trinityBuff(TemporaryStatManager tsm) {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        int amount = 1;
        if (tsm.hasStat(Trinity)) {
            amount = tsm.getOption(Trinity).mOption;
            if (amount < 3) {
                amount++;
            }
        }
        o1.nOption = amount * 10;
        o1.rOption = TRINITY;
        o1.tOption = 7;
        o1.mOption = amount;
        newStats.put(Trinity, o1);
        o2.nValue = (10 * amount);
        o2.nReason = TRINITY;
        o2.tTerm = 7;
        newStats.put(IndieDamR, o2);
        newStats.put(IndieIgnoreMobpdpR, o2);
        tsm.sendStat(newStats);
    }

    public void rechargeABSkills() {
        Effect effect = Effect.createABRechargeEffect();
        chr.write(UserLocal.resetStateForOffSkill());
        chr.write(UserPacket.effect(effect));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
        doMightyMascotAttack();
    }

    private Summon getMightyMascot() {
        return chr.getField().getSummonBySkillId(chr, MIGHTY_MASCOT);
    }

    private void doMightyMascotAttack() {
        SkillInfo si = SkillData.getSkillInfoById(MIGHTY_MASCOT);
        int slv = chr.getSkillLevel(MIGHTY_MASCOT);
        Summon mightyMascot = getMightyMascot();
        if (mightyMascot == null) {
            return;
        }
        if (new Random().nextBoolean()) {
            chr.getField().broadcast(Summoned.assistAttackRequest(mightyMascot, MightMascotSkillTypes.MagicalBalloon.getVal()));
        } else {
            chr.getField().broadcast(Summoned.assistAttackRequest(mightyMascot, MightMascotSkillTypes.TwinkleStar.getVal()));
        }
        int maxStack = si.getValue(s, slv);
        if (mightyMascot.getCount() < maxStack) {
            mightyMascot.setCount(mightyMascot.getCount() + 1);
            increaseMightyMascotSummonTerm();
        }
    }

    private void increaseMightyMascotSummonTerm() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        Option prevOpt = tsm.getOptByCTSAndSkill(IndieEmpty, MIGHTY_MASCOT);
        SkillInfo si = SkillData.getSkillInfoById(MIGHTY_MASCOT);
        int slv = chr.getSkillLevel(MIGHTY_MASCOT);
        int termInc = si.getValue(q, slv);
        o.nValue = prevOpt.nValue;
        o.nReason = prevOpt.nReason;
        o.tTerm = (int) tsm.getRemainingTime(IndieEmpty, MIGHTY_MASCOT) + termInc;
        o.summon = prevOpt.summon;
        o.setInMillis(true);
        tsm.sendStat(IndieEmpty, o);
    }

    public void doBubbleBreath() {
        Summon mightyMascot = getMightyMascot();
        if (mightyMascot == null || mightyMascot.isJaguarActive()) {
            return;
        }
        chr.getField().broadcast(Summoned.assistSpecialAttackRequest(mightyMascot, MightMascotSkillTypes.ShinyBubbleBreath.getVal()));
        mightyMascot.setCount(0);
        mightyMascot.setJaguarActive(true); // placeholder for  Usage Check.
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (skillID) {
            case MELODY_CROSS:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieBooster, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieBooster, o1);
                o2.nOption = si.getValue(mhpX, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(EMHP, o2);
                tsm.sendStat(newStats);
                break;
            case POWER_TRANSFER:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(PowerTransferGauge, o1);
                break;
            case IRON_BLOSSOM:
                o1.nOption = si.getValue(prop, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Stance, o1);
                break;
            case STAR_GAZER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(SharpEyes, o1); //Changed IncCriticalDamMax to SharpEyes
                break;
            case SOUL_SEEKER_EXPERT:
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(AngelicBursterSoulSeeker, o1);
                break;
            case PRETTY_EXALTATION:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                }
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieIgnoreMobpdpR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieBDR, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieBDR, o2);
                tsm.sendStat(newStats);
                break;
            case FINAL_CONTRACT:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(CriticalBuff, o1);
                o2.nOption = si.getValue(asrR, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(AsrR, o2);
                newStats.put(TerR, o2.deepCopy());
                o3.nOption = si.getValue(indieStance, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(Stance, o3);
                tsm.sendStat(newStats);
                break;
            case SPARKLE_BURST:
                o1.nOption = 2;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(EnergyBust, o1);
                Summon summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                summon.setPosition(new Position(chr.getPosition().getX(), chr.getPosition().getY() - 100));
                chr.getField().spawnSummon(summon);
                break;
            case SOUL_SEEKER:
                createSoulSeekerForceAtom();
                createSoulSeekerForceAtom();
                break;
            case DAY_DREAMER:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case NOVA_TEMPERANCE_AB:
                tsm.removeAllDebuffs();
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.ANGELIC_BUSTER1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 2) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ANGELIC_BUSTER2.getJobId());
                sm.giveItem(1142496);
                sm.completeQuestNoRewards(25825);
                sm.giveAndEquip(1352602);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.ANGELIC_BUSTER2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 2) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ANGELIC_BUSTER3.getJobId());
                sm.giveItem(1142497);
                sm.completeQuestNoRewards(25826);
                sm.giveAndEquip(1352603);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.ANGELIC_BUSTER3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 2) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ANGELIC_BUSTER4.getJobId());
                sm.giveItem(1142498);
                sm.completeQuestNoRewards(25827);
                sm.giveAndEquip(1352604);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }

    @Override
    public void handleForceAtomCollision(int faKey, int skillId, int mobObjId, Position position, InPacket inPacket) {
        switch (skillId) {
            case SPARKLE_BURST:
                incrementSparkleBurstEnergy();
                break;
        }

        super.handleForceAtomCollision(faKey, skillId, mobObjId, position, inPacket);
    }

    private void incrementSparkleBurstEnergy() {
        incrementSparkleBurstEnergy(1);
    }

    private void incrementSparkleBurstEnergy(int amount) {
        Summon summon = getSparkleBurstSummon();
        if (!chr.hasSkill(SPARKLE_BURST) || summon == null) {
            return;
        }

        summon.incCount(amount);

        if (summon.getCount() == 50 || summon.getCount() == 120) {
            incrementSparkleBurstState();
        }
    }

    private void incrementSparkleBurstState() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Summon summon = getSparkleBurstSummon();
        if (!chr.hasSkill(SPARKLE_BURST) || summon == null || summon.getState() > 2) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o2 = new Option();
        Option o1 = new Option();

        if (summon.getState() == 0) {
            chr.getField().broadcast(Summoned.doSkill(summon, (byte) 16, SPARKLE_BURST, null));
            summon.incState();
        } else if (summon.getState() == 1) {
            chr.getField().broadcast(Summoned.doSkill(summon, (byte) 17, SPARKLE_BURST, null));
            summon.incState();
        }

        if (tsm.getOptByCTSAndSkill(NotDamaged, SPARKLE_BURST) != null) {
            o1.nOption = 1;
            o1.rOption = SPARKLE_BURST;
            o1.tOption = (int) (tsm.getRemainingTime(NotDamaged, SPARKLE_BURST) + 2000);
            o1.setInMillis(true);
            newStats.put(NotDamaged, o1);
        }

        o2.nOption = summon.getState() + 1;
        o2.rOption = SPARKLE_BURST;
        o2.tOption = (int) tsm.getRemainingTime(IndiePMdR, SPARKLE_BURST);
        o2.setInMillis(true);
        newStats.put(EnergyBust, o2);

        tsm.sendStat(newStats);
    }

    private Summon getSparkleBurstSummon() {
        return chr.getField().getSummons().stream().filter(s -> s.getOwnerId() == chr.getId() && s.getSkillID() == SPARKLE_BURST).findAny().orElse(null);
    }
}
