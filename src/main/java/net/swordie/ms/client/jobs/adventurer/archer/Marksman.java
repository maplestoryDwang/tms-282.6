package net.swordie.ms.client.jobs.adventurer.archer;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
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
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.EnumMap;
import java.util.List;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Marksman extends Archer {

    public static final int SOUL_ARROW = 3200014;
    public static final int NET_TOSS = 3201008;
    public static final int PIECING_ARROW = 3201011;

    public static final int FROSTPREY = 3211005;
    public static final int PAIN_KILLER = 3211011;
    public static final int MARKSMANSHIP = 3210015;
    public static final int MORTAL_BLOW = 3210001;
    public static final int AGGRESSIVE_RESISTANCE = 3210013;
    public static final int EVASION_BOOST = 3210007;
    public static final int EMPOWERED_PIECING_ARROW = 3211017;
    public static final int BOLT_BURST = 3211018;
    public static final int BLINK_SHOT = 3211019;
    public static final int BLINK_SHOT_TP = 3211020;

    public static final int SHARP_EYES = 3221002;
    public static final int SHARP_EYES_IED_H = 3220044;
    public static final int SHARP_EYES_CR_H = 3220045;
    public static final int SNIPE = 3221007;
    public static final int GREATER_EMPOWERED_PIECING_ARROW = 3220021;
    public static final int MARKED_TARGET = 3221022;
    public static final int EMPOWERED_SNIPE = 3221025;
    public static final int EMPOWERED_SNIPE_BONUS = 3221026;
    public static final int PIECING_ARROW_II = 3221027;
    public static final int EMPOWERED_PIECING_ARROW_II = 3221023;
    public static final int ARROW_ILLUSION = 3221014;
    public static final int HEROS_WILL = 3221008;

    public static final int HIGH_SPEED_SHOT = 3221052;
    public static final int EPIC_ADVENTURE = 3221053;
    public static final int BULLSEYE_SHOT = 3221054;

    //Final Attack
    public static final int FINAL_ATTACK_CROSSBOW = 3200001;

    // V Skills
    public static final int PERFECT_SHOT = 400031006;
    public static final int PERFECT_SHOT_HIT = 400031010;
    public static final int SPLIT_SHOT = 400031015;
    public static final int SPLIT_SHOT_FINAL_ATTACK = 400031016;
    public static final int REPEATING_CROSSBOW_CARTRIDGE = 400031055;
    public static final int FULL_BURST_SHOT = 400031056;
    public static final int SURGE_BOLT = 400031025;

    // HEXA Skills
    public static final int HEXA_FINAL_ATTACK = 3240014;
    public static final int HEXA_FROSTPREY = 3241012;
    public static final int HEXA_PIECING_ARROW = 3241005;
    public static final int HEXA_EMPOWERED_PIECING_ARROW = 3241006;
    public static final int HEXA_ULTIMATE_PIECING_ARROW = 3241007;
    public static final int HEXA_SNIPE = 3241000;
    public static final int HEXA_EMPOWERED_SNIPE = 3241001;
    public static final int ULTIMATE_SNIPE = 3241003;
    public static final int HEXA_BOLT_BURST = 3241013;
    public static final int HEXA_HIGH_SPEED_SHOT = 3241015;

    public Marksman(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isMarksman(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        MobTemporaryStat mts = mob.getTemporaryStat();
        Option o1 = new Option();
        if (chr.hasSkill(MORTAL_BLOW)) {
            incrementMortalBlow(mob);
        }
        if (mob.isAdvMarked() && skillID != Job.GUIDED_ARROW) {
            setEnhanceSniping(mob, false);
        } else {
            setEnhanceSniping(mob, true);
        }
        switch (skillID) {
            case NET_TOSS: {
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (mob.isBoss()) {
                        o1.nOption = si.getValue(x, slv);
                        o1.tOption = si.getValue(time, slv) / 2;
                    } else {
                        o1.nOption = si.getValue(y, slv);
                        o1.tOption = si.getValue(time, slv);
                    }
                    o1.rOption = skillID;
                    mts.addStatOptions(mob, MobStat.Speed, o1);
                }
                break;
            }
            case ARROW_ILLUSION: {
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(subTime, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            }
            case FROSTPREY:
            case HEXA_FROSTPREY:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = 3;
                        mts.addStatOptions(mob, MobStat.Freeze, o1);
                    }
                }
                break;
        }
        if (!JobConstants.isPhantom(chr.getJob())) {
            super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
        }
    }

    private void setEnhanceSniping(Mob mob, boolean advMarked) {
        mob.setAdvMarked(advMarked);
        if (!advMarked) {
            chr.write(UserLocal.attackAdvMark(EMPOWERED_SNIPE_BONUS, EMPOWERED_SNIPE, mob.getObjectId()));
        }
        chr.write(UserLocal.setMobAdvMark(EMPOWERED_SNIPE, advMarked, mob.getObjectId()));
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
        if (hasHitMobs) {
            if (chr.hasSkill(MARKSMANSHIP)) {
                handleMarksmanship();
            }
            if (chr.hasSkill(AGGRESSIVE_RESISTANCE)) {
                giveAggressiveResistanceBuff(attackInfo);
            }
        }
        Option o1 = new Option();
        Option o2 = new Option();
        switch (skillID) {
            case PIECING_ARROW:
            case PIECING_ARROW_II:
            case HEXA_PIECING_ARROW:
                var val = tsm.hasStat(EnhancePiercing) ? tsm.getOption(EnhancePiercing).nOption + 1 : 1;
                o1.nOption = Math.max(val, 0);
                tsm.sendStat(EnhancePiercing, o1);
                break;
            case EMPOWERED_PIECING_ARROW:
            case EMPOWERED_PIECING_ARROW_II:
                o1.nOption = 0;
                tsm.sendStat(EnhancePiercing, o1);
                break;
            case HEXA_EMPOWERED_PIECING_ARROW:
                o1 = tsm.getOption(EnhancePiercing);
                o1.nOption = o1.nOption == 1 ? 2 : 1;
                tsm.sendStat(EnhancePiercing, o1);
                if (o1.nOption == 1) {
                    o2.nOption = 1;
                    tsm.sendStat(UltimatePiercing, o2);
                }
                break;
            case HEXA_ULTIMATE_PIECING_ARROW:
                o1.nOption = 0;
                tsm.sendStat(EnhancePiercing, o1);
                tsm.removeStat(UltimatePiercing);
                break;
            case SNIPE:
            case HEXA_SNIPE:
                val = tsm.hasStat(EnhanceSniping) ? tsm.getOption(EnhanceSniping).nOption + 1 : 1;
                o1.nOption = Math.max(val, 0);
                tsm.sendStat(EnhanceSniping, o1);
                break;
            case HEXA_EMPOWERED_SNIPE:
                o1 = tsm.getOption(EnhanceSniping);
                o1.nOption = o1.nOption == 1 ? 2 : 1;
                tsm.sendStat(EnhanceSniping, o1);
                if (o1.nOption == 1) {
                    o2.nOption = 1;
                    tsm.sendStat(UltimateSniping, o2);
                }
                break;
            case EMPOWERED_SNIPE:
                o1.nOption = 0;
                tsm.sendStat(EnhanceSniping, o1);
                break;
            case ULTIMATE_SNIPE:
                o1.nOption = 0;
                tsm.sendStat(EnhanceSniping, o1);
                tsm.removeStat(UltimateSniping);
                break;
            case PERFECT_SHOT_HIT:
                o1 = tsm.getOption(CursorSniping);
                int curCount = o1.nOption - 1;
                if (curCount <= 0) {
                    tsm.removeStat(CursorSniping);
                } else {
                    o1.nOption = curCount;
                    tsm.updateStat(CursorSniping, o1);
                }
                break;
            case FULL_BURST_SHOT:
                decreaseRepeatingCrossbowCartridge();
                break;
        }
    }

    private void handleMarksmanship() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        Skill skill = chr.getSkill(MARKSMANSHIP);
        if (skill != null) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (Util.succeedProp(5) && !tsm.hasStatBySkillId(MARKSMANSHIP)) {
                o.nOption = si.getValue(ignoreMobpdpR, chr.getSkillLevel(MARKSMANSHIP));
                o.rOption = MARKSMANSHIP;
                o.tOption = 5;
                tsm.sendStat(IgnoreMobpdpR, o);
            }
        }
    }

    private void giveAggressiveResistanceBuff(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(AGGRESSIVE_RESISTANCE);
        SkillInfo si = SkillData.getSkillInfoById(AGGRESSIVE_RESISTANCE);
        int slv = skill.getCurrentLevel();
        Option o;
        if (!tsm.hasStat(PowerTransferGauge)) {
            o = new Option();
            o.nOption = 0;
            o.rOption = AGGRESSIVE_RESISTANCE;
        } else {
            o = tsm.getOption(PowerTransferGauge);
            o.nOption = (int) Math.min((int) attackInfo.totalDamageDealt * (si.getValue(y, slv) / 100D) + o.nOption,
                    chr.getStat(Stat.mhp) / (si.getValue(z, slv) / 100D));
        }
        o.tOption = si.getValue(time, slv);
        tsm.sendStat(PowerTransferGauge, o);
        var effect = Effect.skillSpecial(AGGRESSIVE_RESISTANCE);
        chr.write(UserPacket.effect(effect));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
    }

    private void incrementMortalBlow(Mob mob) {
        int amount = 1;
        if (chr.hasSkill(MORTAL_BLOW)) {
            SkillInfo si = SkillData.getSkillInfoById(MORTAL_BLOW);
            int slv = chr.getSkillLevel(MORTAL_BLOW);
            if (Util.succeedProp(si.getValue(x, slv))) {
                chr.getField().broadcast(MobPool.specialEffectBySkill(mob, MORTAL_BLOW, chr.getId(), amount));
                chr.heal((int) (chr.getMaxHP() * 0.1D));
                chr.healMP((int) (chr.getMaxMP() * 0.1D));
            }
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == FINAL_ATTACK_CROSSBOW) {
            if (chr.hasSkill(HEXA_FINAL_ATTACK)) return HEXA_FINAL_ATTACK;
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
            case FROSTPREY:
            case HEXA_FROSTPREY:
                Summon summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                Field field = chr.getField();
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setAssistType(AssistType.Attack);
                field.spawnSummon(summon);
                break;
            case BLINK_SHOT:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                field.spawnSummon(summon);
                chr.getField().broadcast(Summoned.assistAttackRequest(summon, 0));
                break;
            case BLINK_SHOT_TP:
                chr.addSkillCooldown(BLINK_SHOT_TP, 2000);
                break;
            case PAIN_KILLER:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieNotDamaged, o1);
                break;
            case SHARP_EYES:
                int cr = si.getValue(x, slv);
                int crDmg = si.getValue(y, slv);
                cr += SkillData.getSkillInfoById(SHARP_EYES_CR_H).getValue(x, chr.getSkillLevel(SHARP_EYES_CR_H));
                o1.nOption = (cr << 8) + crDmg;
                if (net.swordie.ms.connection.protocol.Protocol.active().version() == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    o1.xOption = cr;
                    o1.yOption = crDmg;
                }
                o1.nValue = si.getValue(y, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                //mOption is for the hyper passive
                if (chr.hasSkill(SHARP_EYES_IED_H)) {
                    o1.mOption = si.getValue(ignoreMobpdpR, slv);
                }
                tsm.sendStat(SharpEyes, o1);
                break;
            case ARROW_ILLUSION:
                Position position = new Position(chr.isLeft() ? chr.getPosition().getX() - 250 : chr.getPosition().getX() + 250, chr.getPosition().getY());
                if (chr.getField().findFootHoldBelow(position) != null) {
                    summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setMaxHP(si.getValue(x, slv));
                    summon.setCurFoothold((short) (int) java.util.Optional.ofNullable(chr.getField().findFootHoldBelow(position)).map(below -> below.getId()).orElse(0) /* in the air: no foothold (was an NPE) */);
                    summon.setPosition(position);
                    summon.setMaxHP(si.getValue(x, slv));
                    summon.setHp(summon.getMaxHP());
                    chr.getField().spawnSummon(summon);
                } else {
                    chr.chatMessage("請換個位置再使用這個技能。");
                }
                break;
            case EPIC_ADVENTURE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case BULLSEYE_SHOT:
                o1.nOption = si.getValue(x, slv) << 8 | si.getValue(y, slv) & 0xFF;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(BullsEye, o1);
                o2.nValue = si.getValue(indieDamR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o2);
                o3.nValue = si.getValue(indieIgnoreMobpdpR, slv);
                o3.nReason = skillID;
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieIgnoreMobpdpR, o3);
                tsm.sendStat(newStats);
                break;
            case SPLIT_SHOT:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(SplitArrow, o1);
                break;
            case PERFECT_SHOT:
                o1.nOption = si.getValue(q, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(CursorSniping, o1);
                break;
            case REPEATING_CROSSBOW_CARTRIDGE:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(RepeatinCartrige, o1);
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
        }
    }

    private void decreaseRepeatingCrossbowCartridge() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = REPEATING_CROSSBOW_CARTRIDGE;
        if (!tsm.hasStat(RepeatinCartrige) || !chr.hasSkill(skillID)) {
            return;
        }
        var o = tsm.getOption(RepeatinCartrige);
        o.nOption -= 1;
        if (o.nOption <= 0) {
            tsm.removeStatsBySkill(skillID);
        } else {
            tsm.updateStat(RepeatinCartrige, o);
        }
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_BOLT_BURST -> {
                int skillID = BOLT_BURST;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_HIGH_SPEED_SHOT -> {
                int skillID = HIGH_SPEED_SHOT;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }

    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        if (cts.equals(CursorSniping)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o1 = new Option();
            o1.nOption = 1;
            o1.rOption = PERFECT_SHOT_HIT;
            o1.tOption = 2;
            tsm.sendStat(IndieNotDamaged, o1);
        }
        super.handleRemoveCTS(cts, options);
    }
}
