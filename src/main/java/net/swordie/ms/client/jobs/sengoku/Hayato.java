package net.swordie.ms.client.jobs.sengoku;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Util;

import java.util.Arrays;
import java.util.EnumMap;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Hayato extends Job {

    //Blade Energy
    public static final int QUICK_DRAW = 40011288;
    public static final int NORMAL_STANCE_BONUS = 40011291;
    public static final int QUICK_DRAW_STANCE_BONUS = 40011292;
    public static final int SUMMER_RAIN = 40011289;
    public static final int HITOKIRI_HUNDRED_STRIKE = 40011290;
    public static final int MASTER_OF_BLADES = 40010000;
    public static final int SHIMADA_HEART = 40010067;

    public static final int BATTOUJUTSU_ADVANCE = 41001010; //not sure what this skill does

    public static final int KATANA_BOOSTER = 41101005; //Buff
    public static final int MILITARY_MIGHT = 41101003; //Buff

    public static final int TORNADO_BLADE = 41111005;
    public static final int WILLOW_DODGE = 41110006;
    public static final int MERCILESS_BLADE = 41110007;
    public static final int WARRIOR_HEART = 41110009;

    public static final int IRON_SKIN = 41121003; //Buff
    public static final int AKATSUKI_HERO_HAYATO = 41121005; //Buff
    public static final int TORNADO_BLADE_IV = 41121017; //Attack (Stun Debuff)
    public static final int HITOKIRI_STRIKE = 41121002;
    public static final int HITOKIRI_STRIKE_ANGLE = 41121022;
    public static final int HITOKIRI_STRIKE_PRESIST = 41120049;
    public static final int EYE_FOR_AN_EYE = 41121015; //  ON/OFF
    public static final int JINSOKU = 41120006;
    public static final int BLOODLETTER = 41120007;
    public static final int SUDDEN_STRIKE = 41121018;
    public static final int AKATSUKI_BLOSSOMS = 41121004;
    public static final int AKATSUKI_TRACE = 41120013;

    public static final int GOD_OF_BLADES = 41121054;
    public static final int PRINCESS_VOW_HAYATO = 41121053;
    public static final int FALCONS_HONOR = 41121052;

    //BattouJutsu Linked Skills
    public static final int SURGING_BLADE_BATTOUJUTSU = 41001014;
    public static final int SHOURYUUSEN_BATTOUJUTSU = 41001015;
    public static final int RISING_SLASH_BATTOUJUTSU = 41101014;
    public static final int FALCON_DIVE_BATTOUJUTSU = 41101015;
    public static final int DANKUUSEN_BATTOUJUTSU = 41111018;
    public static final int SWEEPING_SWORD_BATTOUJUTSU = 41111017;
    public static final int TORNADO_BLADE_BATTOUJUTSU = 41121020;
    public static final int SUDDEN_STRIKE_BATTOUJUTSU = 41121021;

    // V skills
    public static final int BATTOUJUTSU_ZANKOU = 400011026;
    public static final int IAIJUTSU_PHANTOM_BLADE = 400011029;
    public static final int BATTOUJUTSU_ULTIMATE_WILL = 400011104;
    public static final int INSTANT_SLICE = 400011138;

    // HEXA skills
    public static final int HEXA_AKATSUKI_TRACE = 41140006;
    public static final int HEXA_HITOKIRI_STRIKE = 41141007;
    public static final int HEXA_HITOKIRI_STRIKE_ANGLE = 41141008;

    private final int[] addedSkills = new int[]{
            QUICK_DRAW,
            SUMMER_RAIN,
            MASTER_OF_BLADES,
            SHIMADA_HEART,
    };

    private int swordEnergy;

    public void setSwordEnergy(int swordEnergy) {
        this.swordEnergy = swordEnergy;
        chr.write(UserLocal.setSwordEnergy(swordEnergy));
    }

    public int getSwordEnergy() {
        return swordEnergy;
    }

    public void incSwordEnergy(int inc) {
        setSwordEnergy(getSwordEnergy() + inc > 1000 ? 1000 : (Math.max(getSwordEnergy() + inc, 0)));
    }

    public void incSwordEnergyFromGodOfBladesSummon(int swordEnergy) {
        int endingSwordEnergy = Math.min(swordEnergy, 800);
        incSwordEnergy(endingSwordEnergy);
        chr.write(UserPacket.effect(Effect.effectFromWZ("Skill/40001.img/skill/400011104/special0", !chr.isLeft(), 0, 0, 0)));
    }

    public Hayato(Char chr) {
        super(chr);
        if (chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
            for (int id : addedSkills) {
                if (!chr.hasSkill(id)) {
                    Skill skill = SkillData.getSkillDeepCopyById(id);
                    assert skill != null;
                    skill.setCurrentLevel(skill.getMasterLevel());
                    chr.addSkill(skill);
                }
            }
            chr.getTimer().addEvent(() -> setSwordEnergy(0), 2000);
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isHayato(id);
    }

    private boolean isInQuickDrawStance() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.hasStat(BladeStanceMode) && tsm.getOption(BladeStanceMode).nOption == 1;
    }

    public void toggleQuickDraw(TemporaryStatManager tsm) {
        Option o = new Option();
        if (isInQuickDrawStance()) {
            if (tsm.hasStatBySkillId(QUICK_DRAW_STANCE_BONUS)) {
                tsm.removeStatsBySkill(QUICK_DRAW_STANCE_BONUS);
            }
            if (tsm.hasStatBySkillId(QUICK_DRAW)) {
                tsm.removeStatsBySkill(QUICK_DRAW);
            }
            chr.getTimer().addEvent(this::normalStanceBonus, 1000);
        } else {
            if (tsm.hasStatBySkillId(NORMAL_STANCE_BONUS)) {
                tsm.removeStatsBySkill(NORMAL_STANCE_BONUS);
            }
            o.nOption = 1; // true
            o.rOption = QUICK_DRAW;
            tsm.sendStat(BladeStanceMode, o);
            chr.getTimer().addEvent(this::quickDrawStanceBonus, 1000);
        }
    }

    public void setHayatoStanceBonus() {
        if (chr.hasSkill(QUICK_DRAW)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (tsm.hasStat(BladeStancePower)) {
                if (isInQuickDrawStance()) {
                    if (tsm.getOptByCTSAndSkill(BladeStancePower, QUICK_DRAW_STANCE_BONUS) == null) {
                        tsm.removeStat(BladeStancePower);
                        Option o = new Option();
                        o.nOption = 1;
                        o.rOption = QUICK_DRAW_STANCE_BONUS;
                        tsm.sendStat(BladeStancePower, o);
                    }
                } else {
                    if (tsm.getOptByCTSAndSkill(BladeStancePower, NORMAL_STANCE_BONUS) == null) {
                        tsm.removeStat(BladeStancePower);
                        Option o = new Option();
                        o.nOption = 1;
                        o.rOption = NORMAL_STANCE_BONUS;
                        tsm.sendStat(BladeStancePower, o);
                    }
                }
            } else {
                Option o = new Option();
                o.nOption = 1;
                o.rOption = isInQuickDrawStance() ? QUICK_DRAW_STANCE_BONUS : NORMAL_STANCE_BONUS;
                tsm.sendStat(BladeStancePower, o);
            }
        }
    }

    private void quickDrawStanceBonus() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        int hayatoBDR = 1;
        int hayatoBooster = -1;
        boolean setStat = false;
        if (getSwordEnergy() >= 200) {
            hayatoBDR += 1;
        }
        if (getSwordEnergy() >= 400) {
            hayatoBDR += 1;
        }
        if (getSwordEnergy() >= 700) {
            hayatoBDR += 1;
        }
        if (getSwordEnergy() == 1000) {
            hayatoBDR += 1;
        }
        //BossDmg
        if (!tsm.hasStat(IndieBDR)
                || (tsm.hasStat(IndieBDR) && hayatoBDR > tsm.getTotalNOptionOfStat(IndieBDR))) {
            o1.nValue = hayatoBDR;
            o1.nReason = QUICK_DRAW_STANCE_BONUS;
            newStats.put(IndieBDR, o1);
            setStat = true;
        }
        // Stance
        if (!tsm.hasStat(Stance)) {
            o2.nOption = 100;
            o2.rOption = QUICK_DRAW_STANCE_BONUS;
            newStats.put(Stance, o2);
            setStat = true;
        }
        //Booster
        if (!tsm.hasStat(IndieBooster)) {
            o3.nValue = hayatoBooster;
            o3.nReason = QUICK_DRAW_STANCE_BONUS;
            newStats.put(IndieBooster, o3);
            setStat = true;
        }
        if (setStat) {
            tsm.sendStat(newStats, true);
        }
    }

    private void normalStanceBonus() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        int hayatoPAD = 1;
        int hayatoMHPR = 1;
        int hayatoIgnoreMobpdpR = 1;
        boolean setStat = false;
        if (getSwordEnergy() >= 200) {
            hayatoPAD += 1;
            hayatoMHPR += 1;
            hayatoIgnoreMobpdpR += 1;
        }
        if (getSwordEnergy() >= 400) {
            hayatoPAD += 1;
            hayatoMHPR += 1;
            hayatoIgnoreMobpdpR += 1;
        }
        if (getSwordEnergy() >= 700) {
            hayatoPAD += 1;
            hayatoMHPR += 1;
            hayatoIgnoreMobpdpR += 1;
        }
        if (getSwordEnergy() == 1000) {
            hayatoPAD += 1;
            hayatoMHPR += 1;
            hayatoIgnoreMobpdpR += 1;
        }
        // PAD
        if (!tsm.hasStat(IndiePADR)
                || (tsm.hasStat(IndiePADR) && hayatoPAD > tsm.getTotalNOptionOfStat(IndiePADR))) {
            o1.nOption = hayatoPAD;
            o1.rOption = NORMAL_STANCE_BONUS;
            newStats.put(IndiePADR, o1);
            setStat = true;
        }
        // Max HP & Max MP
        if (!tsm.hasStat(IndieMHPR)
                || (tsm.hasStat(IndieMHPR) && hayatoMHPR > tsm.getTotalNOptionOfStat(IndieMHPR))) {
            o2.nOption = hayatoMHPR;
            o2.rOption = NORMAL_STANCE_BONUS;
            newStats.put(IndieMHPR, o2);
            newStats.put(IndieMMPR, o2.deepCopy());
            setStat = true;
        }
        // Ignore DEF
        if (!tsm.hasStat(IndieIgnoreMobpdpR)
                || (tsm.hasStat(IndieIgnoreMobpdpR) && hayatoIgnoreMobpdpR > tsm.getTotalNOptionOfStat(IndieIgnoreMobpdpR))) {
            o3.nOption = hayatoIgnoreMobpdpR;
            o3.rOption = NORMAL_STANCE_BONUS;
            newStats.put(IndieIgnoreMobpdpR, o3);
            setStat = true;
        }
        // Stance
        if (tsm.getOptByCTSAndSkill(Stance, NORMAL_STANCE_BONUS) == null) {
            o4.nOption = 100;
            o4.rOption = NORMAL_STANCE_BONUS;
            newStats.put(Stance, o4);
            setStat = true;
        }
        if (setStat) {
            tsm.sendStat(newStats, true);
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();

        if (isInQuickDrawStance()) {
            quickDrawStunBonus(mob);
        }
        applyDOTOnMob(mob, damage);
        switch (skillID) {
            case TORNADO_BLADE_IV:
                //case TORNADO_BLADE_BATTOUJUTSU:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(subProp, slv))) {
                        if (!mob.isBoss()) {
                            o1.nOption = 1;
                            o1.rOption = skillID;
                            o1.tOption = si.getValue(time, slv);
                            mts.addStatOptions(mob, MobStat.Stun, o1);
                        }
                    }
                }
                break;
            case SUDDEN_STRIKE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(y, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.TotalDamParty, o1);
                }
                break;
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        super.handleAttack(c, attackInfo, si, now);
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        if (hasHitMobs) {
            if (isInQuickDrawStance()) {
                quickDrawStanceBonus();
            } else {
                normalStanceBonus();
            }
            warriorHeartHealHP();
        }
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (attackInfo.skillId) {
            case SUMMER_RAIN:
            case HITOKIRI_HUNDRED_STRIKE:
                if ((tsm.getOptByCTSAndSkill(IndieDamR, SUMMER_RAIN) == null) || (tsm.getOptByCTSAndSkill(IndieDamR, HITOKIRI_HUNDRED_STRIKE) == null)) {
                    o1.nReason = skillID;
                    o1.nValue = 15;
                    o1.tTerm = 120;
                    tsm.sendStat(IndieDamR, o1); //Indie
                    setSwordEnergy(0);
                }
                break;
            case HITOKIRI_STRIKE:
                if (!tsm.hasStatBySkillId(skillID)) {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(prop, slv);
                    o1.tTerm = si.getValue(time, slv) + (chr.hasSkill(HITOKIRI_STRIKE_PRESIST) ? 60 : 0); // Hitokiri Strike - Persist : Increases the buff duration of Hitokiri Strike.
                    newStats.put(IndieCrR, o1);
                    o2.nReason = skillID;
                    o2.nValue = 1;
                    o2.tTerm = 3;
                    newStats.put(IndieNotDamaged, o2);
                    tsm.sendStat(newStats);
                }
                break;
            case HEXA_HITOKIRI_STRIKE:
                if (!tsm.hasStatBySkillId(skillID)) {
                    o1.nReason = skillID;
                    o1.nValue = si.getValue(prop, slv);
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieCrR, o1);
                    o2.nReason = skillID;
                    o2.nValue = 1;
                    o2.tTerm = 3;
                    newStats.put(IndieNotDamaged, o2);
                    o3.nReason = skillID;
                    o3.nValue = si.getValue(x, slv);
                    o3.tTerm = si.getValue(time, slv);
                    newStats.put(IndieCD, o3);
                    tsm.sendStat(newStats);
                }
                break;
            case IAIJUTSU_PHANTOM_BLADE:
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break; // 282 direct attack, no 265 energy/stack.
                if (getSwordEnergy() < 200) {
                    chr.chatMessage("使用該技能至少需要 200 點劍氣。");
                } else {
                    incSwordEnergy(-200);
                    o1.nOption = (tsm.hasStat(SummonProp) ? tsm.getOption(SummonProp).nOption : 0) + 1;
                    o1.rOption = skillID;
                    o1.tOption = 15;
                    if (o1.nOption > 5) {
                        o1.nOption = 5;
                    }
                    newStats.put(SummonProp, o1);
                    o2.nValue = 20 * o1.nOption;
                    o2.nReason = skillID;
                    o2.tTerm = 15;
                    newStats.put(IndiePMdR, o2);
                    tsm.sendStat(newStats);
                }
                break;
            case BATTOUJUTSU_ULTIMATE_WILL:
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break; // 282 charged rush, no summon counter.
                Summon gobSummon = chr.getField().getSummonBySkillId(chr, BATTOUJUTSU_ULTIMATE_WILL);
                if (gobSummon == null) {
                    return;
                }
                for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                    Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                    if (mob == null || mob.getHp() <= 0) {
                        continue;
                    }
                    long totaldmg = Arrays.stream(mai.damages).sum();
                    if (totaldmg >= mob.getHp()) {
                        gobSummon.setCount(Math.min(gobSummon.getCount() + 8, 800));
                        chr.write(MobPool.specialEffectBySkill(mob, attackInfo.skillId, chr.getId(), (short) 1000));
                    }
                }
                chr.getField().broadcast(Summoned.effect(gobSummon, 3));
                gobSummon.setCount(Math.min(gobSummon.getCount() + 10, 800));
                break;
            case FALCONS_HONOR:
            case BATTOUJUTSU_ZANKOU:
                if (skillID == BATTOUJUTSU_ZANKOU && net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break; // 282 timed buff; no 265 sword-energy packet.
                if (getSwordEnergy() + si.getValue(x, slv) > 1000) {
                    setSwordEnergy(1000);
                } else {
                    incSwordEnergy(si.getValue(x, slv));
                }
                break;
        }
        setHayatoStanceBonus();
    }

    public void applyDOTOnMob(Mob mob, long damage) {
        if (chr.hasSkill(BLOODLETTER)) {
            Skill skill = chr.getSkill(BLOODLETTER);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            if (Util.succeedProp(proc)) {
                MobTemporaryStat mts = mob.getTemporaryStat();
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skill.getSkillId(), slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skill.getSkillId());
            }
        } else if (chr.hasSkill(MERCILESS_BLADE)) {
            Skill skill = chr.getSkill(MERCILESS_BLADE);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            if (Util.succeedProp(proc)) {
                MobTemporaryStat mts = mob.getTemporaryStat();
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skill.getSkillId(), slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, skill.getSkillId());
            }
        }
    }

    public void warriorHeartHealHP() { //TODO on Crit hit,  proc% to gainHP%
        if (chr.hasSkill(WARRIOR_HEART)) {
            Skill skill = chr.getSkill(WARRIOR_HEART);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            int healrate = si.getValue(x, slv);
            int healhp = (int) ((chr.getMaxHP() / ((double) 100 / healrate)));

            //Get chance to heal on  #Crit hits
            if (Util.succeedProp(proc)) {
                chr.heal(healhp);
            }
        }
    }

    public void quickDrawStunBonus(Mob mob) {
        Option o = new Option();
        int stunProc = 30;
        if (getSwordEnergy() >= 200) {
            stunProc += 5;
        }
        if (getSwordEnergy() >= 400) {
            stunProc += 5;
        }
        if (getSwordEnergy() >= 700) {
            stunProc += 5;
        }
        if (getSwordEnergy() == 1000) {
            stunProc += 5;
        }
        MobTemporaryStat mts = mob.getTemporaryStat();
        if (!mts.hasCurrentMobStatBySkillId(QUICK_DRAW)) {
            if (Util.succeedProp(stunProc) && !mob.isBoss()) {
                o.nOption = 1;
                o.rOption = QUICK_DRAW;
                o.tOption = 3;
                mts.addStatOptions(mob, MobStat.Stun, o);
            }
        }
    }

    public void incrementSwordEnergy() {
        //reward BladeEnergy
        if (isInQuickDrawStance()) {
            //Reward 2 Blade Energy
            if (getSwordEnergy() + 2 > 1000) {
                setSwordEnergy(1000);
            } else {
                incSwordEnergy(2);
            }
        } else {
            //Reward 5 Blade Energy
            if (getSwordEnergy() + 5 > 1000) {
                setSwordEnergy(1000);
            } else {
                incSwordEnergy(5);
            }
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
        Option o5 = new Option();
        switch (skillID) {
            case AKATSUKI_BLOSSOMS:
                tsm.removeAllDebuffs();
                break;
            case QUICK_DRAW:
                int cost = si.getValue(z, slv);
                if (!isInQuickDrawStance()) {
                    if (getSwordEnergy() < cost) {
                        chr.chatMessage("切換到快速拔刀姿態需要 " + cost + " 點劍氣。");
                    } else {
                        incSwordEnergy(-cost);
                        toggleQuickDraw(tsm);
                    }
                }
                return;
            case BATTOUJUTSU_ADVANCE:
                o1.nOption = 2;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(SkillDeployment, o1);
                o2.nOption = 4;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(DamR, o2);
                tsm.sendStat(newStats);
                break;
            case KATANA_BOOSTER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Booster, o1);
                break;
            case MILITARY_MIGHT:
                o1.nReason = skillID;
                o1.nValue = si.getValue(x, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHPR, o1); //Indie
                o2.nReason = skillID;
                o2.nValue = si.getValue(y, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieMMPR, o2); //Indie
                o3.nOption = si.getValue(speed, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(Speed, o3);
                o4.nOption = si.getValue(jump, slv);
                o4.rOption = skillID;
                o4.tOption = si.getValue(time, slv);
                newStats.put(Jump, o4);
                o5.nOption = si.getValue(padX, slv);
                o5.rOption = skillID;
                o5.tOption = si.getValue(time, slv);
                newStats.put(PAD, o5);
                tsm.sendStat(newStats);
                break;
            case EYE_FOR_AN_EYE:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(KenjiCounter, o1);
                }
                break;
            case PRINCESS_VOW_HAYATO:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv); // 60
                tsm.sendStat(IndieDamR, o1);
                break;
            case GOD_OF_BLADES:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indiePad, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o1); //Indie
                o2.nOption = si.getValue(x, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(AsrR, o2);
                newStats.put(TerR, o2.deepCopy());
                tsm.sendStat(newStats);
                break;
            case IAIJUTSU_PHANTOM_BLADE:
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break; // No summon node/time in 282 data.
                Summon summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setAssistType(AssistType.AttackCounter);
                summon.setAvatarLook(chr.getAvatarData().getAvatarLook());
                chr.getField().spawnSummon(summon);
                break;
            case BATTOUJUTSU_ULTIMATE_WILL:
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break; // 282 rush/charge-stack mechanics need native evidence.
                if (getSwordEnergy() < si.getValue(s, slv)) {
                    chr.chatMessage("該技能的劍氣不足。");
                    return;
                }
                incSwordEnergy(-si.getValue(s, slv));

                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Stop);
                chr.getField().spawnSummon(summon);
                break;
        }
        setHayatoStanceBonus();
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        //Dodge
        if (hitInfo.hpDamage == 0 && hitInfo.mpDamage == 0) {
            jinsoku();
            incrementWillowDodge();
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    public void incrementWillowDodge() {
        if (!chr.hasSkill(WILLOW_DODGE)) {
            return;
        }
        int slv = chr.getSkillLevel(WILLOW_DODGE);
        SkillInfo si = SkillData.getSkillInfoById(WILLOW_DODGE);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        if (tsm.hasStat(EvasionMaster)) {
            int currentNOption = tsm.getOption(EvasionMaster).nOption;
            if (currentNOption / si.getValue(damR, slv) < si.getValue(x, slv)) {
                o1.nOption = tsm.getOption(EvasionMaster).nOption + si.getValue(damR, slv);
                o1.rOption = WILLOW_DODGE;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(EvasionMaster, o1);
            }
        } else {
            o1.nOption = si.getValue(damR, slv);
            o1.rOption = WILLOW_DODGE;
            o1.tOption = si.getValue(time, slv);
            tsm.sendStat(EvasionMaster, o1);
        }
    }

    public void jinsoku() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        if (chr.hasSkill(JINSOKU)) {
            Skill skill = chr.getSkill(JINSOKU);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(t, slv);
            if (Util.succeedProp(proc)) {
                o.nOption = si.getValue(y, slv);
                o.rOption = skill.getSkillId();
                o.tOption = si.getValue(time, slv);
                tsm.sendStat(DamageReduce, o);
            }
        }
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(JobConstants.HAYATO_CREATION_MAP);
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == AKATSUKI_TRACE) {
            if (chr.hasSkill(HEXA_AKATSUKI_TRACE)) return HEXA_AKATSUKI_TRACE;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        if (level == 30) {
            chr.setJob(4110);
            chr.setStatAndSendPacket(Stat.job, 4110);
            chr.addSpToSpecificJob((short) 4100, 1);
        } else if (level == 60) {
            chr.setJob(4111);
            chr.completeQuest(57163);
            chr.setStatAndSendPacket(Stat.job, 4111);
            chr.addSpToSpecificJob((short) 4110, 1);
            chr.addSpToSpecificJob((short) 4111, 3);
        } else if (level == 100) {
            chr.setJob(4112);
            chr.completeQuest(57164);
            chr.setStatAndSendPacket(Stat.job, 4112);
            chr.addSpToSpecificJob((short) 4111, 2);
            chr.addSpToSpecificJob((short) 4112, 3);
        }
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case TORNADO_BLADE, TORNADO_BLADE_BATTOUJUTSU -> { // Tornado Blade
                chr.setSkillCooldown(TORNADO_BLADE_IV, chr.getSkillLevel(TORNADO_BLADE_IV));
                return 1;
            }
            case HITOKIRI_HUNDRED_STRIKE -> {
                chr.setSkillCooldown(SUMMER_RAIN, chr.getSkillLevel(SUMMER_RAIN));
                return 1;
            }
            case HITOKIRI_STRIKE_ANGLE -> {
                chr.setSkillCooldown(Hayato.HITOKIRI_STRIKE_ANGLE, chr.getSkillLevel(HITOKIRI_STRIKE));
                return 1;
            }
            case HEXA_HITOKIRI_STRIKE -> {
                chr.setSkillCooldown(HITOKIRI_STRIKE, chr.getSkillLevel(HEXA_HITOKIRI_STRIKE));
                return 1;
            }
            case HEXA_HITOKIRI_STRIKE_ANGLE -> {
                chr.setSkillCooldown(HITOKIRI_STRIKE_ANGLE, chr.getSkillLevel(HEXA_HITOKIRI_STRIKE));
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
