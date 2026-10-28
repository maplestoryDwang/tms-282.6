package net.swordie.ms.client.jobs.resistance.demon;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class DemonAvenger extends Job {

    public static final int BLOOD_PACT = 30010242;
    public static final int EXCEED = 30010230;
    public static final int HYPER_POTION_MASTERY = 30010231;
    public static final int STAR_FORCE_CONVERSION = 30010232;

    public static final int EXCEED_DOUBLE_SLASH_1 = 31011000; //Special Attack
    public static final int EXCEED_DOUBLE_SLASH_2 = 31011004; //Special Attack
    public static final int EXCEED_DOUBLE_SLASH_3 = 31011005; //Special Attack
    public static final int EXCEED_DOUBLE_SLASH_4 = 31011006; //Special Attack
    public static final int EXCEED_DOUBLE_SLASH_PURPLE = 31011007; //Special Attack
    public static final int OVERLOAD_RELEASE = 31011001; // Special Buff        //TODO TempStat: ExceedOverload
    public static final int LIFE_SAP = 31010002; //Passive Life Drain

    public static final int EXCEED_DEMON_STRIKE_1 = 31201000; //Special Attack
    public static final int EXCEED_DEMON_STRIKE_2 = 31201007; //Special Attack
    public static final int EXCEED_DEMON_STRIKE_3 = 31201008; //Special Attack
    public static final int EXCEED_DEMON_STRIKE_4 = 31201009; //Special Attack
    public static final int EXCEED_DEMON_STRIKE_PURPLE = 31201010; //Special Attack
    public static final int BATTLE_PACT_DA = 31201002; //Buff
    public static final int BAT_SWARM = 31201001;

    public static final int EXCEED_LUNAR_SLASH_1 = 31211000; //Special Attack
    public static final int EXCEED_LUNAR_SLASH_2 = 31211007; //Special Attack
    public static final int EXCEED_LUNAR_SLASH_3 = 31211008; //Special Attack
    public static final int EXCEED_LUNAR_SLASH_4 = 31211009; //Special Attack
    public static final int EXCEED_LUNAR_SLASH_PURPLE = 31211010; //Special Attack
    public static final int VITALITY_VEIL = 31211001;
    public static final int SHIELD_CHARGE_RUSH = 31211002;
    public static final int SHIELD_CHARGE = 31211011; //Special Attack (Stun Debuff)
    public static final int DIABOLIC_RECOVERY = 31211004; //Buff
    public static final int WARD_EVIL = 31211003; //Buff
    public static final int ADVANCED_LIFE_SAP = 31210006; //Passive Life Drain
    public static final int PAIN_DAMPENER = 31210005;

    public static final int EXCEED_EXECUTION_1 = 31221000; //Special Attack
    public static final int EXCEED_EXECUTION_2 = 31221009; //Special Attack
    public static final int EXCEED_EXECUTION_3 = 31221010; //Special Attack
    public static final int EXCEED_EXECUTION_4 = 31221011; //Special Attack
    public static final int EXCEED_EXECUTION_PURPLE = 31221012; //Special Attack//TODO (EXCEED System)
    public static final int NETHER_SHIELD = 31221001; //Special Attack
    public static final int NETHER_SHIELD_ATOM = 31221014; //atom
    public static final int NETHER_SLICE = 31221002; // Special Attack (DefDown Debuff)
    public static final int BLOOD_PRISON = 31221003; // Special Attack (Stun Debuff)
    public static final int MAPLE_WARRIOR_DA = 31221008; //Buff
    public static final int INFERNAL_EXCEED = 31220007;

    public static final int DEMONIC_FORTITUDE_DA = 31221053;
    public static final int FORBIDDEN_CONTRACT = 31221054;
    public static final int THOUSAND_SWORDS = 31221052;

    // V Skills
    public static final int DEMONIC_FRENZY = 400011010;
    public static final int DEMONIC_FRENZY_AA = 400010010;
    public static final int DEMONIC_BLAST_HOLDDOWN = 400011038;
    public static final int DEMONIC_BLAST_ATTACK_1 = 400011062;
    public static final int DEMONIC_BLAST_ATTACK_2 = 400011063;
    public static final int DEMONIC_BLAST_ATTACK_3 = 400011064;
    public static final int DIMENSIONAL_SWORD_SUMMON = 400011090;
    public static final int DIMENSIONAL_SWORD_ATTACK = 400011102;
    public static final int REVENANT = 400011112;
    public static final int THORN_OF_FURY_1 = 400011113;
    public static final int THORN_OF_FURY_2 = 400011114;
    public static final int THORN_OF_FURY_3 = 400011115;
    public static final int REVENANT_END = 400011129;

    // HEXA Skills
    public static final int HEXA_INFERNAL_EXCEED = 31240013;
    public static final int HEXA_REVENANT = 500061054;
    public static final int HEXA_THORN_OF_FURY_1 = 500061055;
    public static final int HEXA_THORN_OF_FURY_2 = 500061056;
    public static final int HEXA_THORN_OF_FURY_3 = 500061057;
    public static final int HEXA_REVENANT_END = 500061058;

    private final int[] addedSkills = new int[]{
            EXCEED,
            BLOOD_PACT,
            HYPER_POTION_MASTERY,
            STAR_FORCE_CONVERSION,
            // demon-0930: Demon beginner skills (see DemonSlayer.addedSkills)
            Demon.DARK_WINDS,
            Demon.DEMONIC_BLOOD,
            DemonSlayer.DEMON_WINGS,
    };

    private int lastExceedSkill;
    private ScheduledFuture<?> diabolicRecoveryTimer;
    private Tms282DemonicBlast demonicBlast;

    public DemonAvenger(Char chr) {
        super(chr);
        if (Tms282DemonicBlast.active()) demonicBlast = chr.getJobHandler() instanceof DemonAvenger old && old.demonicBlast != null
                ? old.demonicBlast : new Tms282DemonicBlast(chr);
        if (chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
            for (int id : addedSkills) {
                if (!chr.hasSkill(id)) {
                    Skill skill = SkillData.getSkillDeepCopyById(id);
                    if (skill != null) {
                        skill.setCurrentLevel(skill.getMasterLevel());
                        chr.addSkill(skill);
                        // An online job change (first job in the newbie flow) must tell the client, or the Demon jump
                        // key stays dead until the next login.
                        if (chr.getClient() != null && chr.getField() != null
                                && chr.getClient().getCurrentState() == net.swordie.ms.client.Client.IN_FIELD) {
                            chr.write(net.swordie.ms.connection.packet.WvsContext.changeSkillRecordResult(skill));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isDemonAvenger(id);
    }

    public void diabolicRecoveryHPRecovery() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(DiabolikRecovery)) {
            Skill skill = chr.getSkill(DIABOLIC_RECOVERY);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int recovery = si.getValue(x, slv);
            int duration = si.getValue(w, slv);
            chr.heal((int) (chr.getMaxHP() / ((double) 100 / recovery)));
            diabolicRecoveryTimer = chr.getTimer().addEvent(this::diabolicRecoveryHPRecovery, duration, TimeUnit.SECONDS);
        }
    }

    public void drainHPByDemonicBlast() {
        if (Tms282DemonicBlast.active()) return;
        Skill skill = chr.getSkill(DEMONIC_BLAST_HOLDDOWN);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int hpDrained = (int) ((double) (chr.getHP() * si.getValue(x, slv)) / 100D);
        chr.heal(-hpDrained, true);
    }

    public void giveDemonFrenzy() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = DEMONIC_FRENZY;
        if (!chr.hasSkill(skillID) || tsm.getOptByCTSAndSkill(CharacterTemporaryStat.Frenzy, skillID) == null) {
            return;
        }
        Skill skill = chr.getSkill(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        if (chr.getCurrentHPPerc() >= si.getValue(q2, slv)) { // As it's random
            if (Util.succeedProp(55)) {
                spillBlood();
                chr.heal(-si.getValue(u, slv), true);

                Option o1 = new Option();
                Option o2 = new Option();
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                Option prevOpt = tsm.getOption(CharacterTemporaryStat.Frenzy);
                int indiePmdR = Math.round((float) (100 - chr.getHPPerc()) / si.getValue(u, slv)) * si.getValue(x, slv);
                if (indiePmdR <= 0) {
                    indiePmdR = 1;
                }
                o1.nOption = 1;
                o1.rOption = prevOpt.rOption;
                newStats.put(Frenzy, o1);
                o2.nValue = indiePmdR; // Dmg Reduction%
                o2.nReason = prevOpt.rOption;
                newStats.put(IndieDamR, o2);
                tsm.sendStat(newStats);
            }
        } else {
            tsm.removeStatsBySkill(skillID);
        }
    }

    private void spillBlood() {
        Position chrPosition = chr.getPosition();
        Position aaPosition = new Position(chrPosition.getX(), chrPosition.getY() + 30);
        Foothold fh = chr.getField().findFootHoldBelow(chrPosition);
        if (fh != null) {
            int skillID = DEMONIC_FRENZY;
            int aaskillID = DEMONIC_FRENZY_AA;
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            SkillInfo demonicFrenzyTile = SkillData.getSkillInfoById(aaskillID);
            int slv = chr.getSkillLevel(skillID);
            AffectedArea aa = AffectedArea.getPassiveAA(chr, aaskillID, slv);
            aa.setMobOrigin((byte) 0);
            aa.setPosition(aaPosition);
            aa.setDuration(si.getValue(s2, slv) * 1000);
            aa.setCurFoothold((short) fh.getId());
            aa.setRect(aa.getPosition().getRectAround(demonicFrenzyTile.getRects().get(0)));
            chr.getField().spawnAffectedArea(aa);
        } else {
            chr.chatMessage("請換個位置再使用這個技能。");
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        switch (skillID) {
            case BLOOD_PRISON:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case SHIELD_CHARGE:
                if (!mts.hasCurrentMobStatBySkillId(SkillConstants.getActualSkillIDfromSkillID(skillID))) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = SkillConstants.getActualSkillIDfromSkillID(skillID);
                        o1.tOption = 5;
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case NETHER_SLICE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = 30;
                    map.put(MobStat.PDR, o1);
                    map.put(MobStat.MDR, o1);
                    mts.addStatOptions(mob, map);
                }
                break;
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        if (Tms282DemonicBlast.active()) {
            if (attackInfo.skillId == Tms282DemonicBlast.SKILL) {
                if (demonicBlast.begin(attackInfo, now)) super.handleAttack(c, attackInfo, attackInfo.skillInfo, now);
                return;
            }
            if (Tms282DemonicBlast.removedStage(attackInfo.skillId)) return;
        }
        Char chr = c.getChr();
        super.handleAttack(c, attackInfo, si, now);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        //DA HP Cost System
        if (skillID != NETHER_SHIELD_ATOM) {
            applyHpCost(SkillConstants.getActualSkillIDfromSkillID(attackInfo.skillId));
        }
        if (hasHitMobs) {
            //Life Sap & Advanced Life Sap
            if (attackInfo.skillId != DEMONIC_FRENZY && attackInfo.skillId != DEMONIC_FRENZY_AA) {
                lifeSapHealing();
            }
        }

        int hpRecovered = 5;
        switch (attackInfo.skillId) {
            case THOUSAND_SWORDS:
                for (int i = 0; i < 4; i++) {
                    incrementOverloadCount(skillID, tsm);
                }
                break;
            case VITALITY_VEIL:
                int amounthealed = si.getValue(y, slv);
                int healamount = (int) ((chr.getMaxHP()) / ((double) 100 / amounthealed));
                chr.heal(healamount);
                break;
            case EXCEED_DOUBLE_SLASH_1:
            case EXCEED_DOUBLE_SLASH_2:
            case EXCEED_DOUBLE_SLASH_3:
            case EXCEED_DOUBLE_SLASH_4:
            case EXCEED_DOUBLE_SLASH_PURPLE:

            case EXCEED_DEMON_STRIKE_1:
            case EXCEED_DEMON_STRIKE_2:
            case EXCEED_DEMON_STRIKE_3:
            case EXCEED_DEMON_STRIKE_4:
            case EXCEED_DEMON_STRIKE_PURPLE:

            case EXCEED_LUNAR_SLASH_1:
            case EXCEED_LUNAR_SLASH_2:
            case EXCEED_LUNAR_SLASH_3:
            case EXCEED_LUNAR_SLASH_4:
            case EXCEED_LUNAR_SLASH_PURPLE:

            case EXCEED_EXECUTION_1:
            case EXCEED_EXECUTION_2:
            case EXCEED_EXECUTION_3:
            case EXCEED_EXECUTION_4:
            case EXCEED_EXECUTION_PURPLE:
                giveExceedOverload(SkillConstants.getActualSkillIDfromSkillID(attackInfo.skillId));
                incrementOverloadCount(SkillConstants.getActualSkillIDfromSkillID(attackInfo.skillId), tsm);
                break;
            case DEMONIC_BLAST_ATTACK_3:
                hpRecovered += 8;
            case DEMONIC_BLAST_ATTACK_2:
                hpRecovered += 7;
            case DEMONIC_BLAST_ATTACK_1:
                if (tsm.hasStatBySkillId(DEMONIC_FRENZY)) {
                    hpRecovered = 100 * hpRecovered;
                }
                chr.heal((int) ((double) (chr.getMaxHP() * hpRecovered) / 100D));
                tsm.removeStatsBySkill(DEMONIC_BLAST_HOLDDOWN);
                break;
        }
    }

    public void sendHpUpdate() {
        // Used for client side damage calculation for DAs
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = 3; // Hp -> damage conversion
        o.mOption = chr.getTotalStat(BaseStat.mhp);
        tsm.sendStat(LifeTidal, o);
    }

    public void changeDimensionalSword() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        if (chr.hasSkill(DIMENSIONAL_SWORD_SUMMON) && tsm.hasStat(CharacterTemporaryStat.IndiePMdR) && chr.getField().getSummons().stream().anyMatch(s -> s.getOwnerId() == chr.getId() && s.getSkillID() == DIMENSIONAL_SWORD_SUMMON)) {
            long remainingTime = tsm.getRemainingTime(CharacterTemporaryStat.IndiePMdR, DIMENSIONAL_SWORD_SUMMON);
            chr.getField().removeSummon(DIMENSIONAL_SWORD_SUMMON, chr.getId());
            tsm.removeStatsBySkill(DIMENSIONAL_SWORD_SUMMON);
            o1.nOption = 1;
            o1.rOption = DIMENSIONAL_SWORD_ATTACK;
            o1.tOption = (int) ((remainingTime) / 5);
            o1.setInMillis(true);
            tsm.sendStat(DevilishPower, o1);
        }
    }

    private void createNetherShieldForceAtom() {
        Field field = chr.getField();
        SkillInfo si = SkillData.getSkillInfoById(NETHER_SHIELD);
        int slv = chr.getSkillLevel(NETHER_SHIELD);
        Rect rect = chr.getPosition().getRectAround(si.getRects().getFirst());
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        List<Mob> mobs = field.getMobsInRect(rect);
        if (mobs.size() <= 0) {
            return;
        }
        if (mobs.size() == 1) {
            mobs.add(mobs.get(0)); // Ensure there's 2 shields created
        }
        ForceAtomEnum fae = ForceAtomEnum.NETHER_SHIELD;
        List<ForceAtomInfo> faiList = new ArrayList<>();
        List<Integer> targetList = new ArrayList<>();
        int max = si.getValue(bulletCount, slv);
        int i = 0;
        for (Mob mob : mobs) {
            if (i >= max) {
                break;
            }
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), Util.getRandom(20, 26), Util.getRandom(35, 45), 0, 480 + Util.getRandom(100), Util.getCurrentTime(), 0, 0, new Position(0, -100));
            faiList.add(fai);
            targetList.add(mob.getObjectId());
            i++;
        }
        ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                true, targetList, NETHER_SHIELD_ATOM, faiList, new Rect(), 0, 300,
                new Position(), 0, new Position(), 0);
        fa.setMaxRecreationCount(si.getValue(z, slv) + SkillData.getSkillInfoById(31220050).getValue(z, slv));
        chr.createForceAtom(fa);
    }

    private void recreateNetherShieldForceAtom(Mob mob) {
        SkillInfo si = SkillData.getSkillInfoById(NETHER_SHIELD);
        int slv = chr.getSkillLevel(NETHER_SHIELD);
        int anglenum = new Random().nextInt(360);
        int TW1prop = 80;
        if (Util.succeedProp(TW1prop)) {
            int mobID = mob.getObjectId();
            int inc = ForceAtomEnum.NETHER_SHIELD_RECREATION.getInc();
            int type = ForceAtomEnum.NETHER_SHIELD_RECREATION.getForceAtomType();
            ForceAtomEnum fae = ForceAtomEnum.NETHER_SHIELD_RECREATION;
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(1, inc, 35, 4,
                    anglenum, 0, Util.getCurrentTime(), 1, 0,
                    mob.getPosition());
            ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                    true, mobID, NETHER_SHIELD_ATOM, forceAtomInfo, new Rect(), 0, 300,
                    mob.getPosition(), NETHER_SHIELD_ATOM, mob.getPosition(), 0);
            fa.setMaxRecreationCount(si.getValue(z, slv));
            chr.createForceAtom(fa);
        }
    }

    public void giveExceedOverload(int skillid) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = skillid;
        o.rOption = skillid;
        o.tOption = 8;
        tsm.sendStat(ExceedOverload, o);
    }

    public void incrementOverloadCount(int skillid, TemporaryStatManager tsm) {
        Option o = new Option();
        int amount = 1;
        if (tsm.hasStat(OverloadCount)) {
            amount = tsm.getOption(OverloadCount).nOption;
            if (amount < getMaxExceed()) {
                if (skillid != lastExceedSkill && lastExceedSkill != 0) {
                    amount++;
                }
                amount++;
            }
        }
        amount = Math.min(amount, getMaxExceed());
        lastExceedSkill = skillid;
        o.nOption = amount;
        o.rOption = EXCEED;
        tsm.sendStat(OverloadCount, o);
    }

    private void resetExceed(TemporaryStatManager tsm) {
        tsm.removeStatsBySkill(EXCEED);
    }

    private int getMaxExceed() {
        int num = 20;
        if (chr.hasSkill(31220044)) { //Hyper Skill Boost [ Reduce Overload ]
            num = 18;
        }
        return num;
    }

    public void lifeSapHealing() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(LIFE_SAP)) {
            Skill skill = chr.getSkill(LIFE_SAP);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            int amounthealed = si.getValue(x, slv);
            if (chr.hasSkill(ADVANCED_LIFE_SAP)) {
                amounthealed = SkillData.getSkillInfoById(ADVANCED_LIFE_SAP).getValue(x, chr.getSkill(ADVANCED_LIFE_SAP).getCurrentLevel());
            }
            if (chr.hasSkill(PAIN_DAMPENER)) {
                amounthealed -= SkillData.getSkillInfoById(PAIN_DAMPENER).getValue(x, chr.getSkill(PAIN_DAMPENER).getCurrentLevel());
            }
            int exceedamount = tsm.getOption(OverloadCount).nOption;
            int exceedpenalty = (int) Math.floor(exceedamount / 5);
            amounthealed -= exceedpenalty;
            if (Util.succeedProp(proc)) {
                int healamount = (int) ((chr.getMaxHP()) / ((double) 100 / amounthealed));
                chr.heal(healamount);
            }
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == INFERNAL_EXCEED) {
            if (chr.hasSkill(HEXA_INFERNAL_EXCEED)) return HEXA_INFERNAL_EXCEED;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        if (Tms282DemonicBlast.active() && (skillUseInfo.skillID == Tms282DemonicBlast.SKILL
                || Tms282DemonicBlast.removedStage(skillUseInfo.skillID))) return;
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();
        Summon summon;
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (skillID) {
            case NETHER_SHIELD:
                createNetherShieldForceAtom();
                break;
            case OVERLOAD_RELEASE:
                int overloadCount = tsm.getOption(OverloadCount).nOption;
                double overloadRate = (double) overloadCount / getMaxExceed();

                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Exceed, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePMdR, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePMdR, o2);

                tsm.sendStat(newStats);

                resetExceed(tsm);
                chr.heal((int) (overloadRate * chr.getMaxHP()));
                break;
            case BATTLE_PACT_DA:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Booster, o1);
                break;
            case WARD_EVIL:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(DamageReduce, o1);
                o2.nOption = si.getValue(z, slv);
                o2.nReason = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(AsrR, o2);
                o3.nOption = si.getValue(z, slv);
                o3.nReason = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(TerR, o3);
                tsm.sendStat(newStats);
                break;
            case DIABOLIC_RECOVERY: // x = HP restored at interval
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieMhpR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHPR, o1);
                o2.nOption = 1;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(DiabolikRecovery, o2);
                tsm.sendStat(newStats);
                if (diabolicRecoveryTimer != null) {
                    diabolicRecoveryTimer.cancel(false);
                }
                diabolicRecoveryHPRecovery();
                break;
            case MAPLE_WARRIOR_DA:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(IncMaxHP, o1);
                break;
            case DEMONIC_FORTITUDE_DA:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case FORBIDDEN_CONTRACT:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                //HP consumption from Skills = 0;
                break;
            case DEMONIC_FRENZY:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(Frenzy, o1);
                }
                break;
            case DEMONIC_BLAST_HOLDDOWN:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                o1.xOption = si.getValue(x, slv); // hp% drain
                newStats.put(IndieKeyDownTime, o1);
                o2.nReason = skillID;
                o2.nValue = -si.getValue(indieCr, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieAllHitDamR, o2);
                tsm.sendStat(newStats);
                break;
            case DIMENSIONAL_SWORD_SUMMON:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 0);
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setPosition(chr.getPosition());
                chr.getField().spawnSummon(summon);
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePMdR, o1);
                break;
            case REVENANT:
            case HEXA_REVENANT:
                o1.nOption = 1; // Stored Fury
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.zOption = si.getValue(z, slv);
                tsm.sendStat(RevenantGauge, o1);
                break;
        }
    }

    public void reduceFuryRevenant() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = chr.hasSkill(HEXA_REVENANT) ? HEXA_REVENANT : REVENANT;
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkillLevel(skillID);
        if (!tsm.hasStat(RevenantGauge)) {
            return;
        }
        Option o = tsm.getOption(RevenantGauge);
        o.zOption = Math.max(0, (int) Math.ceil(o.zOption * si.getValue(q2, slv) / 100.0));
        tsm.updateStat(RevenantGauge, o);
        chr.write(WvsContext.updateSkillStackRequestResult(REVENANT, (byte) o.nOption));
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        if (hitInfo.hpDamage > 0) {
            int skillID = DEMONIC_FRENZY;
            if (chr.hasSkill(skillID)) {
                Skill skill = chr.getSkill(skillID);
                int slv = skill.getCurrentLevel();
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                hitInfo.hpDamage = (int) (((double) (100 - si.getValue(s, slv)) / 100) * hitInfo.hpDamage);
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
    }

    @Override
    public void handleAddCTS(CharacterTemporaryStat cts, List<Option> options) {
        if (cts != LifeTidal) {
            sendHpUpdate();
        }
        super.handleAddCTS(cts, options);
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        sendHpUpdate();
        if (cts == RevenantGauge) {
            Option o1 = new Option();
            o1.nOption = 1;
            o1.rOption = chr.hasSkill(HEXA_REVENANT) ? HEXA_REVENANT_END : REVENANT_END;
            o1.tOption = 40;
            chr.getTemporaryStatManager().sendStat(DeathDance, o1);
        }
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (diabolicRecoveryTimer != null) {
            diabolicRecoveryTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    public void demonicBlastKeydownCost() {
        if (Tms282DemonicBlast.active()) return;
        var tsm = chr.getTemporaryStatManager();
        var opt = tsm.getOptByCTSAndSkill(IndieKeyDownTime, DEMONIC_BLAST_HOLDDOWN);
        if (opt != null) {
            int hpDrained = chr.getHPPerc(opt.xOption);
            chr.heal(-hpDrained, false);
        }
    }

    public void onVerifiedDemonicBlastHit(AttackInfo attack, Mob target, boolean killed) {
        if (demonicBlast != null) demonicBlast.onDamageApplied(attack, target, killed);
    }
}