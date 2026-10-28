package net.swordie.ms.client.jobs.cygnus;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
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

public class Mihile extends Job {

    public static final int ROYAL_GUARD_STACK_BUFF = 51001005;
    public static final int ROYAL_GUARD = 51001006; //Special Buff/Attack
    public static final int ROYAL_GUARD_2 = 51001007;
    public static final int ROYAL_GUARD_3 = 51001008;
    public static final int ROYAL_GUARD_4 = 51001009;
    public static final int ROYAL_GUARD_5 = 51001010;

    public static final int SWORD_BOOSTER = 51101003; //Buff
    public static final int RALLY = 51101004; //Buff

    public static final int SELF_RECOVERY = 51110000;

    public static final int ENDURING_SPIRIT = 51111004; //Buff
    public static final int SOUL_LINK = 51111008; //Special Buff (ON/OFF)
    public static final int MAGIC_CRASH = 51111005; //Special Skill (Debuff Mobs)
    public static final int ADVANCED_ROYAL_GUARD = 51110009; //Upgrade on Royal Guard

    public static final int ROILING_SOUL = 51121006; //Buff (ON/OFF)
    public static final int FOUR_POINT_ASSAULT = 51121007; //Special Attack (Accuracy Debuff)
    public static final int RADIANT_CROSS = 51121009; //Special Attack (Accuracy Debuff)    Creates an Area of Effect
    public static final int RADIANT_CROSS_AA = 51120057; //Area of Effect,  After Radiant Cross
    public static final int CALL_OF_CYGNUS_MIHILE = 51121005; //Buff
    public static final int SOUL_ASYLUM = 51120003;
    public static final int ENDURING_SPIRIT_STEEL_SKIN = 51120044;
    public static final int ENDURING_SPIRIT_PREPARATION = 51120045;
    public static final int ENDURING_SPIRIT_PERSIST = 51120043;
    public static final int FOUR_POINT_ASSAULT_OPPORTUNITY = 51120050;

    //Final Attack
    public static final int FINAL_ATTACK_MIHILE = 51100002;
    public static final int ADVANCED_FINAL_ATTACK_MIHILE = 51120002;

    public static final int CHARGING_LIGHT = 51121052;
    public static final int QUEEN_OF_TOMORROW = 51121053;
    public static final int SACRED_CUBE = 51121054;

    // V Skills
    public static final int SHIELD_OF_LIGHT = 400011011;
    public static final int SWORD_OF_LIGHT = 400011032;
    public static final int SWORD_OF_LIGHT_1 = 400011033;
    public static final int SWORD_OF_LIGHT_2 = 400011034;
    public static final int SWORD_OF_LIGHT_3 = 400011035;
    public static final int SWORD_OF_LIGHT_4 = 400011036;
    public static final int SWORD_OF_LIGHT_5 = 400011037;
    public static final int SWORD_OF_LIGHT_PASSIVE = 400011067;
    public static final int RADIANT_SOUL = 400011083;
    public static final int LIGHT_OF_COURAGE_BUFF = 400011127; // buff
    public static final int LIGHT_OF_COURAGE_SI = 400011128; // Buff Info

    // HEXA skills
    public static final int KNIGHTS_IMMORTAL = 51141503;
    public static final int KNIGHTS_IMMORTAL_EXTRA = 51141504;
    public static final int HEXA_FINAL_ATTACK_MIHILE = 51140018;

    // HEXA boosts
    public static final int HEXA_SHIELD_OF_LIGHT = 500061008;
    public static final int HEXA_LIGHT_OF_COURAGE_BUFF = 500061010; // buff
    public static final int HEXA_LIGHT_OF_COURAGE_SI = 500061011; // Buff In

    private long lastSelfRecovery = 0L;
    private ScheduledFuture<?> soulLinkBuffsTimer;
    private ScheduledFuture<?> soulLinkHPRegenTimer;
    private ScheduledFuture<?> shielfOfLightTimer;

    private final int[] addedSkills = new int[]{};

    public Mihile(Char chr) {
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
        return id == JobConstants.JobEnum.NAMELESS_WARDEN.getJobId() || JobConstants.isMihile(id);
    }

    private void increaseRoyalGuardStackBuffDuration() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(SWORD_OF_LIGHT);
        int slv = chr.getSkillLevel(SWORD_OF_LIGHT);
        if (tsm.hasStat(RoyalGuardState)) {
            Option prevOption = tsm.getOption(RoyalGuardState);
            Option o1 = new Option();

            o1.nOption = prevOption.nOption;
            o1.xOption = prevOption.xOption;
            o1.bOption = prevOption.bOption;
            o1.rOption = prevOption.rOption;
            o1.tOption = (int) tsm.getRemainingTime(RoyalGuardState, o1.rOption) + (si.getValue(q, slv) * 1000);
            if (o1.tOption > 12000) {
                o1.tOption = 12000;
            }
            o1.setInMillis(true);
            tsm.removeStatsBySkill(ROYAL_GUARD_STACK_BUFF);

            tsm.sendStat(RoyalGuardState, o1);
        }
    }

    private void doRoyalGuard() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();

        o1.nOption = 1;
        o1.rOption = ROYAL_GUARD;
        o1.tOption = calcRoyalGuardTime();
        o1.setInMillis(true);
        tsm.sendStat(BodyRectGuardPrepare, o1);
    }

    private int calcRoyalGuardTime() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int royalGuardTime = 1450;
        int curStack = (tsm.hasStat(RoyalGuardState) ? tsm.getOption(RoyalGuardState).xOption : 0);
        royalGuardTime -= (curStack * 200);
        if (tsm.hasStatBySkillId(SACRED_CUBE)) {
            royalGuardTime += 500;
        }
        return royalGuardTime;
    }

    private void successfulGuard(HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.removeStat(BodyRectGuardPrepare);
        giveRoyalGuardBuff();
        giveSoulAsylumBuff();
        giveRoyalGuardStackedBuff();
        doRoyalGuardAttack();
        if (chr.hasSkillOnCooldown(SWORD_OF_LIGHT) && chr.hasSkillOnCooldown(SWORD_OF_LIGHT) && !chr.hasSkillOnCooldown(SWORD_OF_LIGHT_PASSIVE)) {
            chr.write(UserLocal.userBonusAttackRequest(SWORD_OF_LIGHT_PASSIVE, hitInfo.mobID));
        }
    }

    private void giveRoyalGuardBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = 1;
        o.rOption = ROYAL_GUARD;
        o.tOption = 4;
        tsm.sendStat(NotDamaged, o);
    }

    private void giveSoulAsylumBuff() {
        if (chr.hasSkill(SOUL_ASYLUM)) {
            Skill skill = chr.getSkill(SOUL_ASYLUM);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();

            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o = new Option();
            o.nOption = si.getValue(y, slv);
            o.rOption = skill.getSkillId();
            o.tOption = si.getValue(time, slv);
            tsm.sendStat(MichaelFixDamDecTime, o);
        }
    }

    private void giveRoyalGuardStackedBuff() { //TempStat  Shield Attack is Effect
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        int amount = 1;
        if (tsm.hasStat(RoyalGuardState)) {
            amount = tsm.getOption(RoyalGuardState).xOption;
            if (amount < royalGuardMaxCounter()) {
                amount++;
            }
        }
        o1.nOption = amount;
        o1.xOption = amount;
        o1.bOption = 4;
        o1.rOption = ROYAL_GUARD_STACK_BUFF;
        o1.tOption = 12;
        o1.startTime = System.currentTimeMillis();
        newStats.put(RoyalGuardState, o1);
        o2.nOption = getRoyalGuardAttPower();
        o2.rOption = ROYAL_GUARD_STACK_BUFF;
        o2.tOption = 12;
        newStats.put(PAD, o2);
        tsm.sendStat(newStats);
    }

    private int royalGuardMaxCounter() {
        int num = 3;
        if (chr.hasSkill(ROYAL_GUARD)) {
            num = 3;
        }
        if (chr.hasSkill(ADVANCED_ROYAL_GUARD)) {
            num = 5;
        }
        return num;
    }

    private int getRoyalGuardAttPower() {
        int pad = 0;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.getOption(RoyalGuardState).xOption == 1) {
            pad = 10;
        }
        if (tsm.getOption(RoyalGuardState).xOption == 2) {
            pad = 15;
        }
        if (tsm.getOption(RoyalGuardState).xOption == 3) {
            pad = 20;
        }
        if (tsm.getOption(RoyalGuardState).xOption == 4) {
            pad = 30;
        }
        if (tsm.getOption(RoyalGuardState).xOption == 5) {
            pad = 45;
        }
        return pad;
    }

    private int getRoyalGuardAttPower(Char chr) {
        int pad = 0;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(ROYAL_GUARD);
        byte slv = (byte) si.getCurrentLevel();
        if (tsm.getOption(RoyalGuardState).xOption == 1) {
            pad = 10;
        }
        if (tsm.getOption(RoyalGuardState).xOption == 2) {
            pad = 15;
        }
        if (tsm.getOption(RoyalGuardState).xOption == 3) {
            pad = 20;
        }
        if (tsm.getOption(RoyalGuardState).xOption == 4) {
            pad = 25;
        }
        if (tsm.getOption(RoyalGuardState).xOption == 5) {
            pad = 35;
        }
        return pad;
    }

    private void giveSoulLinkBuffs() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr != null && chr.hasSkill(SOUL_LINK) && tsm.hasStat(MichaelSoulLink)) {
            Skill skill = chr.getSkill(SOUL_LINK);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            Rect rect = chr.getPosition().getRectAround(si.getRects().get(0));
            if (!chr.isLeft()) {
                rect = rect.moveRight();
            }
            Party party = chr.getParty();
            int partySize = 0;

            // Party Member's buffs
            if (party != null) {

                for (Char partyChr : party.getPartyMembersInSameField(chr)) {
                    if (partyChr != null) {
                        EnumMap<CharacterTemporaryStat, Option> partyNewStats = new EnumMap<>(CharacterTemporaryStat.class);
                        TemporaryStatManager partyTSM = partyChr.getTemporaryStatManager();
                        int partyChrX = partyChr.getPosition().getX();
                        int partyChrY = partyChr.getPosition().getY();

                        if (JobConstants.isMihile(partyChr.getJob())) {   // Mihile doesn't get the PartyMember's Buffs
                            continue;
                        }

                        if (partyChrX >= rect.getLeft() && partyChrY >= rect.getTop() // if  Party Members in Range
                                && partyChrX <= rect.getRight() && partyChrY <= rect.getBottom()) {

                            Option o1 = new Option();
                            Option o2 = new Option();
                            Option o3 = new Option();
                            Option o4 = new Option();

                            o1.nOption = 1;
                            o1.rOption = skill.getSkillId();
                            o1.tOption = 2;
                            o1.cOption = chr.getId(); // Owner of Soul Link (Mihile's chr Id)
                            partyNewStats.put(MichaelSoulLink, o1);

                            if (chr.getSkill(ROYAL_GUARD) != null && tsm.hasStatBySkillId(ROYAL_GUARD) && !partyTSM.hasStatBySkillId(ROYAL_GUARD)) {
                                o2.nReason = ROYAL_GUARD;
                                o2.nValue = (int) (getRoyalGuardAttPower(chr) * ((double) si.getValue(x, slv) / 100));
                                o2.tTerm = 12;
                                partyNewStats.put(IndiePAD, o2);
                                partyNewStats.put(IndieMAD, o2);
                            }
                            if (chr.getSkill(ENDURING_SPIRIT) != null && tsm.hasStatBySkillId(ENDURING_SPIRIT) && !partyTSM.hasStatBySkillId(ENDURING_SPIRIT)) {
                                Skill enduringSpirit = chr.getSkill(ENDURING_SPIRIT);
                                SkillInfo esInfo = SkillData.getSkillInfoById(enduringSpirit.getSkillId());
                                byte esLevel = (byte) enduringSpirit.getCurrentLevel();

                                // Enduring Spirit - DEF
                                o3.nReason = ENDURING_SPIRIT;
                                o3.nValue = (int) (esInfo.getValue(x, esLevel) * ((double) si.getValue(w, slv) / 100));
                                o3.tTerm = esInfo.getValue(time, esLevel);
                                partyNewStats.put(IndiePDDR, o3);

                                // Enduring Spirit - AsrR
                                o4.nReason = ENDURING_SPIRIT;
                                o4.nValue = (int) (esInfo.getValue(y, esLevel) * ((double) si.getValue(y, slv) / 100));
                                o4.tTerm = esInfo.getValue(time, esLevel);
                                partyNewStats.put(IndieAsrR, o4);
                            }
                            partyTSM.sendStat(partyNewStats);
                            partySize++;

                        } else {    // if  Party Members outside Range
                            partyTSM.removeStatsBySkill(SOUL_LINK);
                            partyTSM.removeStatsBySkill(ROYAL_GUARD);
                            partyTSM.removeStatsBySkill(ENDURING_SPIRIT);
                        }
                    }
                }
            }

            // Mihile's Buffs
            Option o5 = new Option();
            o5.nReason = SOUL_LINK + 100; // for invisible Icon
            o5.nValue = si.getValue(indieDamR, slv) * partySize;
            tsm.sendStat(IndieDamR, o5);

            this.soulLinkBuffsTimer = chr.getTimer().addEvent(this::giveSoulLinkBuffs, 1, TimeUnit.SECONDS);
        }
        tsm.removeStatsBySkill(SOUL_LINK + 100);
    }

    public void soulLinkHPRegen() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr != null && chr.hasSkill(SOUL_LINK) && tsm.hasStat(MichaelSoulLink)) {
            Skill skill = chr.getSkill(SOUL_LINK);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            int delay = si.getValue(dot, slv);
            chr.heal((int) (chr.getMaxHP() / ((double) 100 / si.getValue(s, slv))));
            this.soulLinkHPRegenTimer = chr.getTimer().addEvent(this::soulLinkHPRegen, delay, TimeUnit.SECONDS);
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        Option o2 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case FOUR_POINT_ASSAULT:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv) + (chr.hasSkill(FOUR_POINT_ASSAULT_OPPORTUNITY) ? 20 : 0))) {
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.ACC, o1);
                    }
                }
                break;
            case RADIANT_CROSS:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(y, slv))) {
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.ACC, o1);
                    }
                }
                break;
            case RADIANT_CROSS_AA:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = mob.isBoss() ? (si.getValue(time, slv) / 2) : si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.ACC, o1);
                }
                break;
            case CHARGING_LIGHT:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 10; // Because WzFile doesn't have a variable for it.
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.TotalDamParty, o1);
                }
                break;
            case SWORD_OF_LIGHT_1:
            case SWORD_OF_LIGHT_2:
            case SWORD_OF_LIGHT_3:
            case SWORD_OF_LIGHT_4:
            case SWORD_OF_LIGHT_5:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    si = SkillData.getSkillInfoById(SWORD_OF_LIGHT);
                    slv = chr.getSkillLevel(SWORD_OF_LIGHT);
                    int duration = si.getValue(time, slv);
                    o1.nOption = -30;
                    o1.rOption = skillID;
                    o1.tOption = duration;
                    o2.nOption = -30;
                    o2.rOption = skillID;
                    o2.tOption = duration / 2;
                    mts.addStatOptions(mob, MobStat.ACC, mob.isBoss() ? o2 : o1);
                }
                break;
            case SWORD_OF_LIGHT_PASSIVE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = -si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv) / 2;
                    o2.nOption = -si.getValue(x, slv);
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.ACC, mob.isBoss() ? o1 : o2);
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
        int skillID = attackInfo.skillId;
        int slv = attackInfo.slv;
        if (chr.hasSkill(SELF_RECOVERY)) {
            SkillInfo siSR = SkillData.getSkillInfoById(SELF_RECOVERY);
            int slvSR = chr.getSkillLevel(SELF_RECOVERY);
            if (lastSelfRecovery <= System.currentTimeMillis() - siSR.getValue(hcHp, slvSR)) {
                chr.heal(siSR.getValue(hp, slvSR));
                chr.healMP(siSR.getValue(mp, slvSR));
                lastSelfRecovery = System.currentTimeMillis();
            }
        }
        switch (skillID) {
            case RADIANT_CROSS:
                if (chr.hasSkill(RADIANT_CROSS_AA)) {
                    Field field = chr.getField();
                    AffectedArea aa = AffectedArea.getAffectedArea(chr, attackInfo);
                    aa.setMobOrigin((byte) 0);
                    aa.setSkillID(RADIANT_CROSS_AA);
                    aa.setPosition(chr.getPosition());
                    Rect rect = aa.getPosition().getRectAround(si.getRects().get(0));
                    if (!chr.isLeft()) {
                        rect = rect.horizontalFlipAround(chr.getPosition().getX());
                    }
                    aa.setRect(rect);
                    aa.setFlip(!attackInfo.left);
                    aa.setDelay((short) 7); //spawn delay
                    field.spawnAffectedAreaAndRemoveOld(aa);
                }
                break;
            case SWORD_OF_LIGHT_1:
            case SWORD_OF_LIGHT_2:
            case SWORD_OF_LIGHT_3:
            case SWORD_OF_LIGHT_4:
            case SWORD_OF_LIGHT_5:
                increaseRoyalGuardStackBuffDuration();
                chr.addSkillCooldown(SWORD_OF_LIGHT, 5000);
                chr.addSkillCooldown(SWORD_OF_LIGHT_PASSIVE, 5000);
                break;
            case SWORD_OF_LIGHT_PASSIVE:
                chr.addSkillCooldown(SWORD_OF_LIGHT_PASSIVE, 5000);
                break;
        }
    }

    private void doRoyalGuardAttack() {
        c.write(UserLocal.royalGuardAttack(true));
    }

    public int getShieldOfLightID() {
        return chr.hasSkill(HEXA_SHIELD_OF_LIGHT) ? HEXA_SHIELD_OF_LIGHT : SHIELD_OF_LIGHT;
    }

    public void giveShieldOfLightBuff() {
        int skillID = getShieldOfLightID();
        Skill skill = chr.getSkill(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = skill.getCurrentLevel();

        Rect rect = chr.getRectAround(si.getFirstRect());

        Set<Char> chrList = chr.getParty().getPartyMembersInSameField(chr);
        for (Char pChr : chrList) {
            TemporaryStatManager pTSM = pChr.getTemporaryStatManager();
            Option o = new Option();

            if (!rect.hasPositionInside(pChr.getPosition())) {
                pTSM.removeStatsBySkill(skillID);
                continue;
            }
            o.nOption = si.getValue(x, slv); // dmg reduction
            o.rOption = skillID;
            o.tOption = 2;
            o.xOption = chr.getId(); // Mihile Chr Id
            o.startTime = System.currentTimeMillis();
            pTSM.sendStat(Michael_RhoAias, o);
        }

    }

    public void hitShieldOfLight() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option prevOption = tsm.getOption(Michael_RhoAias);
        Option o1 = new Option();
        o1.nOption = prevOption.nOption; // dmg reduction
        o1.rOption = prevOption.rOption;
        o1.tOption = (int) tsm.getRemainingTime(Michael_RhoAias);
        o1.xOption = chr.getId(); // Mihile Chr Id
        o1.cOption = ++prevOption.cOption; // total Hits absorbed
        o1.bOption = --prevOption.bOption; // Shield Hits Left
        o1.wOption = prevOption.wOption; // Shield Stage
        if (o1.bOption <= 0) {
            o1.wOption = ++prevOption.wOption; // Shield Stage
            o1.bOption = getShieldOfLightHitCount();
        }
        if (o1.wOption > 3) {
            tsm.removeStat(Michael_RhoAias);
            return;
        }
        o1.setInMillis(true);
        tsm.sendStat(Michael_RhoAias, o1);
    }

    private int getShieldOfLightHitCount() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int curStage = tsm.getOption(Michael_RhoAias).wOption;
        int skillID = getShieldOfLightID();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkillLevel(skillID);
        switch (curStage) {
            case 1:
                return si.getValue(y, slv);
            case 2:
                return si.getValue(w, slv);
            case 3:
                return si.getValue(z, slv);
            default:
                return 0;
        }
    }

    private void giveEndShieldBuff(int totalHitsAbsorbed) {
        if (!chr.hasSkill(SHIELD_OF_LIGHT)) {
            return;
        }
        int skillID = getShieldOfLightID();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkillLevel(skillID);
        int fdGiven = si.getValue(w2, slv) + totalHitsAbsorbed;

        Option o = new Option();
        o.nValue = fdGiven;
        o.nReason = skillID;
        o.tTerm = si.getValue(q2, slv);
        tsm.sendStat(IndiePMdR, o);
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == FINAL_ATTACK_MIHILE) {
            if (chr.hasSkill(HEXA_FINAL_ATTACK_MIHILE)) return HEXA_FINAL_ATTACK_MIHILE;
            else if (chr.hasSkill(ADVANCED_FINAL_ATTACK_MIHILE)) return ADVANCED_FINAL_ATTACK_MIHILE;
        }
        return super.getFinalAttackSkill(faSkill);
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
        switch (skillID) {
            case ROYAL_GUARD:   //BuffStat 'ShieldAttack'  has something to do with this skill
            case ROYAL_GUARD_2:
            case ROYAL_GUARD_3:
            case ROYAL_GUARD_4:
            case ROYAL_GUARD_5:
                doRoyalGuard();
                chr.addSkillCooldown(ROYAL_GUARD_STACK_BUFF, 6000);
                break;
            case RALLY:
                o1.nValue = si.getValue(indiePad, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePAD, o1);
                break;
            case ENDURING_SPIRIT:
                o1.nValue = si.getValue(x, slv) + (chr.hasSkill(ENDURING_SPIRIT_STEEL_SKIN) ? 20 : 0);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv) + (chr.hasSkill(ENDURING_SPIRIT_PERSIST) ? 20 : 0);
                newStats.put(IndiePDDR, o1);
                o2.nValue = si.getValue(y, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv) + (chr.hasSkill(ENDURING_SPIRIT_PERSIST) ? 20 : 0);
                newStats.put(IndieAsrR, o2);
                o3.nValue = si.getValue(z, slv) + (chr.hasSkill(ENDURING_SPIRIT_PREPARATION) ? 10 : 0);
                o3.nReason = skillID;
                o3.tTerm = si.getValue(time, slv) + (chr.hasSkill(ENDURING_SPIRIT_PERSIST) ? 20 : 0);
                newStats.put(IndieTerR, o3);
                tsm.sendStat(newStats);
                break;
            case SOUL_LINK: // (ON/OFF)
                if (tsm.hasStatBySkillId(SOUL_LINK)) {
                    tsm.removeStatsBySkill(SOUL_LINK);
                } else {
                    o1.nOption = 1;
                    o1.rOption = SOUL_LINK;
                    o1.cOption = chr.getId();
                    tsm.sendStat(MichaelSoulLink, o1);
                }
                if (this.soulLinkBuffsTimer != null) {
                    this.soulLinkBuffsTimer.cancel(false);
                }
                giveSoulLinkBuffs();

                if (this.soulLinkHPRegenTimer != null) {
                    this.soulLinkHPRegenTimer.cancel(false);
                }
                soulLinkHPRegen();
                break;
            case ROILING_SOUL:
                int fd = si.getValue(x, slv);
                int mobsHit = si.getValue(mobCount, slv);
                o1.nOption = (fd * 100) + mobsHit;
                o1.rOption = skillID;
                newStats.put(Enrage, o1);
                o2.nOption = si.getValue(x, slv);
                o2.rOption = skillID;
                newStats.put(DamR, o2);
                tsm.sendStat(newStats);
                break;
            case QUEEN_OF_TOMORROW:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case SACRED_CUBE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieMhpR, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHPR, o2);
                o3.nOption = si.getValue(x, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(DamageReduce, o3);
                o4.nOption = 1;
                o4.rOption = skillID;
                o4.tOption = si.getValue(time, slv);
                newStats.put(DamAbsorbShield, o4);
                tsm.sendStat(newStats);
                break;
            case RADIANT_SOUL:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(MichaelSwordOfLight, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieCr, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieCrR, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indiePadR, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndiePADR, o3);
                o4.nReason = skillID;
                o4.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieIgnoreMobpdpR, o4);
                tsm.sendStat(newStats);
                break;
            case MAGIC_CRASH:
                var rect = chr.getRectAround(new Rect(-500, -250, 500, 250));
                if (!chr.isLeft()) {
                    rect = rect.moveRight();
                }
                int count = 0;
                final List<Mob> mobs = chr.getField().getMobsInRect(rect);
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.cOption = chr.getId();
                for (Mob mob : mobs) {
                    if (mob != null && mob.getHp() > 0) {
                        if (count >= 10) {
                            break;
                        }
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                            mts.removeBuffs(mob);
                            mts.addStatOptions(mob, MobStat.MagicCrash, o1.deepCopy());
                            count += 1;
                        }
                    }
                }
                break;
            case SHIELD_OF_LIGHT:
            case HEXA_SHIELD_OF_LIGHT:
                if (tsm.hasStat(Michael_RhoAias)) {
                    tsm.removeStat(Michael_RhoAias);
                    return;
                }
                o1.nOption = si.getValue(x, slv); // dmg reduction
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.xOption = chr.getId(); // Mihile Chr Id
                o1.cOption = 0; // total Hits absorbed
                o1.bOption = 16; // Shield Hits Left
                o1.wOption = 1; // Shield Stage
                tsm.sendStat(Michael_RhoAias, o1);
                if (this.shielfOfLightTimer != null) {
                    this.shielfOfLightTimer.cancel(false);
                }
                ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(this::giveShieldOfLightBuff, 500, 500, false);
                this.shielfOfLightTimer = sf;
                GlobalTimerManager.addCharTimer(chr.getId(), sf);
                break;
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        switch (cts) {
            case Michael_RhoAias:
                for (Option removeOpt : options) {
                    if (removeOpt == null) {
                        continue;
                    }
                    if (removeOpt.xOption == chr.getId()) {
                        chr.getTimer().addEvent(() -> giveEndShieldBuff(removeOpt.cOption), 40, TimeUnit.MILLISECONDS);
                    }
                }
                break;
        }
        super.handleRemoveCTS(cts, options);
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(BodyRectGuardPrepare)) {
            successfulGuard(hitInfo);
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    // Character creation related methods ---------------------------------------------------------------------------------------------
    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        chr.getAvatarData().getCharacterStat().setPosMap(JobConstants.MIHILE_CREATION_MAP);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (this.soulLinkBuffsTimer != null) {
            this.soulLinkBuffsTimer.cancel(true);
        }
        if (this.soulLinkHPRegenTimer != null) {
            this.soulLinkHPRegenTimer.cancel(true);
        }
        if (this.shielfOfLightTimer != null) {
            this.shielfOfLightTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        ExtendSP esp = chr.getAvatarData().getCharacterStat().getExtendSP();
        if (level == 10) {
            esp.setSpToJobLevel(1, 1);
        } else if (level == 60) {
            esp.setSpToJobLevel(3, 5);
        } else if (level >= 100 && !chr.hasSkill(51121005)) {
            chr.getScriptManager().giveSkill(51121005, 0, 30);
        }
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.MIHILE1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 3) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MIHILE2.getJobId());
                sm.giveItem(1302038);
                sm.giveItem(1142400);
                sm.giveAndEquip(1098001);
                sm.completeQuestNoRewards(20806);
                sm.completeQuestNoRewards(20807);
                sm.completeQuestNoRewards(20808);
                sm.completeQuestNoRewards(20809);
                sm.completeQuestNoRewards(20810);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.MIHILE2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 2) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MIHILE3.getJobId());
                sm.giveItem(1142401);
                sm.giveAndEquip(1098002);
                sm.completeQuestNoRewards(20320);
                sm.completeQuestNoRewards(20321);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.MIHILE3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 2) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MIHILE4.getJobId());
                sm.giveItem(1142402);
                sm.giveAndEquip(1098003);
                sm.completeQuestNoRewards(20411);
                sm.completeQuestNoRewards(20412);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
