package net.swordie.ms.enums;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.ToBaseStat;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 5/4/2018.
 */
public enum BaseStat {
    unk,
    str, strR, strLv,
    dex, dexR, dexLv,
    inte, intR, intLv,
    luk, lukR, lukLv,
    mdf, // MP for non-MP jobs,   Time Force, Fury, Mana
    pad, padR, padLv,
    mad, madR, madLv,
    pdd, pddR,
    mdd, mddR,
    mhp, mhpR, mhpLv,
    mmp, mmpR, mmpLv,
    cr, // Crit rate
    addCrOnBoss, // Additional Crit rate on bosses
    crDmg, // Crit damage
    fd, // Final damage %
    damR, // Damage %
    bd, // Boss damage
    nbd, // Non-Boss damage %
    ied, // Ignore enemy defense
    asr, // All status resistance
    ter, // Status time minus
    acc, accR,
    eva, evaR,
    jump,
    speed,
    expR,
    dropR,
    mesoR,
    booster,
    stance,
    mastery,
    damageOver, // max damage
    allStat, allStatR,
    hpRecovery,
    mpRecovery,
    incAllSkill,
    summonTimeR, // Summon Duration multiplier
    buffTimeR, // Buff Duration multiplier
    runeBuffTimerR, // Buff Duration multiplier specifically for Rune's EXP buff
    noCoolProp, // chance to skip cooltimes
    reduceCooltimeR, // %
    recoveryUp,
    mpconReduce,
    reduceCooltime,
    costhpR,
    costmpR,
    hpDrain,
    mpDrain,
    dmgReduce,
    magicGuard, // in %  of HP goes to MP instead.
    invincibleAfterRevive, // in seconds
    shopDiscountR, // % discount on shop items
    pqShopDiscountR, // % discount in pq Shop
    arc,
    ;

    public static BaseStat getFromStat(Stat s) {
        return switch (s) {
            case str -> str;
            case dex -> dex;
            case inte -> inte;
            case luk -> luk;
            case mhp -> mhp;
            case mmp -> mmp;
            default -> unk;
        };
    }

    public static Map<BaseStat, Integer> getFromCTS(Char chr, CharacterTemporaryStat ctsArg, Option o) {
        Map<BaseStat, Integer> stats = new HashMap<>();
        switch (ctsArg) {
            case IndiePAD:
                stats.put(pad, o.nValue);
                break;
            case EPAD:
            case PAD:
            case SelfHyperBodyIncPAD:
                stats.put(pad, o.nOption);
                break;
            case IndieMAD:
                stats.put(mad, o.nValue);
                break;
            case UsefulAdvancedBless:
                // Equipment Decent Blessing uses skill 8005; regular and V blessings already supply attack CTS.
                stats.put(unk, o.nOption);
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                        && o.nOption == 1 && o.rOption % 10000 == net.swordie.ms.client.jobs.Job.DECENT_ADVANCED_BLESSING) {
                    var skill = net.swordie.ms.loaders.SkillData.getSkillInfoById(net.swordie.ms.client.jobs.Job.DECENT_ADVANCED_BLESSING);
                    if (skill != null) {
                        stats.put(pad, skill.getValue(net.swordie.ms.client.character.skills.SkillStat.x, o.nOption));
                        stats.put(mad, skill.getValue(net.swordie.ms.client.character.skills.SkillStat.y, o.nOption));
                    }
                }
                break;
            case MAD:
            case EMAD:
                stats.put(mad, o.nOption);
                break;
            case IndiePDD:
            case FoxBless:
                stats.put(pdd, o.nValue);
                break;
            case PDD:
            case EPDD:
                stats.put(pdd, o.nOption);
                break;
            case IndiePADR:
                stats.put(padR, o.nValue);
                break;
            case IndieMADR:
                stats.put(madR, o.nValue);
                break;
            case IndiePDDR:
                stats.put(pddR, o.nValue);
                break;
            case IndieMHP:
                stats.put(mhp, o.nValue);
                break;
            case IndieMHPR:
                stats.put(mhpR, o.nValue);
                break;
            case MaxHP:
            case IncMaxHP:
            case SelfHyperBodyMaxHP:
                stats.put(mhpR, o.nOption);
                break;
            case IndieMMP:
                stats.put(mmp, o.nValue);
                break;
            case MaxMP:
            case IncMaxMP:
            case SelfHyperBodyMaxMP:
                stats.put(mmpR, o.nOption);
                break;
            case IndieMMPR:
                stats.put(mmpR, o.nValue);
                break;
            case IndieACC:
                stats.put(acc, o.nValue);
                break;
            case ACC:
                stats.put(acc, o.nOption);
                break;
            case ACCR:
                stats.put(accR, o.nOption);
                break;
            case IndieEVA:
                stats.put(eva, o.nValue);
                break;
            case EVA:
                stats.put(eva, o.nOption);
                break;
            case IndieEVAR:
                stats.put(evaR, o.nValue);
                break;
            case EVAR:
            case RWMovingEvar:
                stats.put(evaR, o.nOption);
                break;
            case Speed:
                stats.put(speed, o.nOption);
                break;
            case IndieSpeed:
                stats.put(speed, o.nValue);
                break;
            case IndieJump:
                stats.put(jump, o.nValue);
                break;
            case Jump:
                stats.put(jump, o.nOption);
                break;
            case IndieAllStat:
                stats.put(str, o.nValue);
                stats.put(dex, o.nValue);
                stats.put(inte, o.nValue);
                stats.put(luk, o.nValue);
                break;
            case IndieDodgeCriticalTime:
            case IndieCrR:
                stats.put(cr, o.nValue);
                break;
            case Enrage:
                stats.put(fd, o.xOption); // fd
                break;
            case EnrageCrDamMin:
            case SoulGazeCriDamR:
            case BullsEye:
                stats.put(crDmg, o.nOption);
                break;
            case IndieCD:
                stats.put(crDmg, o.nValue);
                break;
            case IndieEXP:
            case IndieRelaxEXP:
                stats.put(expR, o.nValue);
                break;
            case HolySymbol:
            case ExpBuffRate:
            case CarnivalExp:
            case PlusExpRate:
                stats.put(expR, o.nOption);
                break;
            case IndieBooster:
                stats.put(booster, o.nValue);
                break;
            case Booster:
            case PartyBooster:
            case BladeStanceBooster:
                stats.put(booster, o.nOption);
                break;
            case STR:
            case ZeroAuraStr:
                stats.put(str, o.nOption);
                break;
            case IndieSTR:
                stats.put(str, o.nValue);
                break;
            case IndieDEX:
                stats.put(dex, o.nValue);
                break;
            case IndieINT:
                stats.put(inte, o.nValue);
                break;
            case IndieLUK:
                stats.put(luk, o.nValue);
                break;
            case DamR:
            case IndieDamR:
            case BeastForm:
                stats.put(damR, o.nOption);
                break;
            case IndieMDF:
                stats.put(mdf, o.nValue);
                break;
            case IndiePMdR:
                stats.put(fd, o.nValue);
                break;
            case Arcana:
                // Native 282 computes stage * skill.z from Arcana itself; never also send IndiePMdR.
                stats.put(fd, net.swordie.ms.client.jobs.adventurer.magician.Magician.isHexaArcana(o.rOption)
                        ? (o.nOption == 2 ? o.zOption : o.wOption) : o.nOption * o.zOption);
                break;
            case IndieAsrR:
                stats.put(asr, o.nValue);
                break;
            case AsrR:
            case AsrRByItem:
            case IncAsrR:
                stats.put(asr, o.nOption);
                break;
            case IndieTerR:
                stats.put(ter, o.nValue);
                break;
            case TerR:
            case IncTerR:
                stats.put(ter, o.nOption);
                break;
            case IndieBDR:
                stats.put(bd, o.nValue);
                break;
            case BdR:
            case Concentration:
                stats.put(bd, o.nOption);
                break;
            case IndieStance:
                stats.put(stance, o.nValue);
                break;
            case IndieIgnoreMobpdpR:
                stats.put(ied, o.nValue);
                break;
            case IgnoreTargetDEF:
            case IgnoreMobpdpR:
                stats.put(ied, o.nOption);
                break;
            case MesoUp:
            case MesoUpByItem:
                stats.put(mesoR, o.nOption);
                break;
            case IndieStatR:
            case IndieStatRBasic:
            case BasicStatUp:
                stats.put(strR, o.nValue);
                stats.put(dexR, o.nValue);
                stats.put(intR, o.nValue);
                stats.put(lukR, o.nValue);
                break;
            case Stance:
                stats.put(stance, o.nOption);
                break;
            case SharpEyes:
                // Combination of Cr, CrDmg, Ied
                stats.put(cr, o.xOption);
                stats.put(crDmg, o.yOption);
                stats.put(ied, o.mOption);
                break;
            case CriticalGrowing:
            case EnrageCr:
            case CriticalBuff:
            case ItemCritical:
                stats.put(cr, o.nOption);
                break;
            case DropRate:
            case ItemUpByItem:
                stats.put(dropR, o.nOption);
                break;
            case EventRate:
                // TODO
                break;
            case EMHP:
            case BeastFormMaxHP:
                stats.put(mhp, o.nOption);
                break;
            case EMMP:
                stats.put(mmp, o.nOption);
                break;
            case IndieScriptBuff:
                stats.put(buffTimeR, o.nValue);
                break;
            case ComboCounter:
                ToBaseStat.comboCounter(chr, o, stats);
                break;
            case BowMasterConcentration:
                ToBaseStat.focusedFury(chr, o, stats);
                break;
            case IndieArc:
                stats.put(arc, o.nValue);
                break;
            default:
                stats.put(unk, o.nOption);
        }
        return stats;
    }

    public boolean isRateVar() {
        return switch (this) {
            case strR, dexR, intR, lukR, padR, madR, pddR, mddR, mhpR, mmpR, accR, evaR, expR, dropR, mesoR, allStatR -> true;
            default -> false;
        };
    }

    public BaseStat getRateVar() {
        return switch (this) {
            case str -> strR;
            case dex -> dexR;
            case inte -> intR;
            case luk -> lukR;
            case pad -> padR;
            case mad -> madR;
            case pdd -> pddR;
            case mdd -> mddR;
            case mhp -> mhpR;
            case mmp -> mmpR;
            case acc -> accR;
            case eva -> evaR;
            case allStat -> allStatR;
            default -> null;
        };
    }

    public boolean isLevelVar() {
        return switch (this) {
            case strLv, dexLv, intLv, lukLv, padLv, madLv, mhpLv, mmpLv -> true;
            default -> false;
        };
    }

    public BaseStat getLevelVar() {
        return switch (this) {
            case str -> strLv;
            case dex -> dexLv;
            case inte -> intLv;
            case luk -> lukLv;
            case pad -> padLv;
            case mad -> madLv;
            case mhp -> mhpLv;
            case mmp -> mmpLv;
            default -> null;
        };
    }

    public Stat toStat() {
        return switch (this) {
            case str -> Stat.str;
            case dex -> Stat.dex;
            case inte -> Stat.inte;
            case luk -> Stat.luk;
            case mhp -> Stat.mhp;
            case mmp -> Stat.mmp;
            default -> null;
        };
    }

    public String getBaseStatName() {
        return switch (this) {
            case str -> "Strength (STR)";
            case dex -> "Dexterity (DEX)";
            case inte -> "Intelligence (INT)";
            case luk -> "Luck (LUK)";
            case strR -> "Strength Rate (STR %)";
            case dexR -> "Dexterity Rate (DEX %)";
            case intR -> "Intelligence Rate (INT %)";
            case lukR -> "Luck Rate (LUK %)";
            case mhp -> "Maximum HP";
            case mmp -> "Maximum MP";
            case mhpR -> "Maximum HP Rate (%)";
            case mmpR -> "Maximum MP Rate (%)";
            case pad -> "Weapon Attack (PAD)";
            case mad -> "Magic Attack (MAD)";
            case pdd -> "Physical Defense (PDD)";
            case mdd -> "Magical Defense (MDD)";
            case padR -> "Weapon Attack Rate (PAD %)";
            case madR -> "Magic Attack Rate (MAD %)";
            case pddR -> "Physical Defense Rate (PDD %)";
            case mddR -> "Magical Defense Rate (MDD %)";
            case cr -> "Critical Rate (%)";
            case crDmg -> "Crititical Damage (Min CD)";
            case damR -> "Final Damage (FD)";
            case bd -> "Boss Damage (BD)";
            case ied -> "Ignore Enemy Defense (IED)";
            case asr -> "Abnormal Status Resistance (ASR)";
            case ter -> "Elemental Resistance (TER)";
            case acc -> "Accuracy (ACC)";
            case eva -> "Dodge Rate (EVA)";
            case jump -> "Movement Jump (Jump)";
            case speed -> "Movement Speed (Speed)";
            case expR -> "EXP Rate";
            case dropR -> "DROP Rate";
            case mesoR -> "MESO Rate";
            case booster -> "Attack Speed (Booster)";
            case stance -> "Knockback Resistance (Stance)";
            case mastery -> "Mastery";
            case allStat -> "All Stats (%)";
            case hpRecovery -> "HP Recovery (%)";
            case mpRecovery -> "MP Recovery (%)";
            case incAllSkill -> "All Skills";
            //case buffTimeR -> "Maximum MP";
            //case recoveryUp -> "Maximum MP";
            case mpconReduce -> "MP Consuming Reduce (%)";
            case reduceCooltime -> "Skill Cooldown Reduce";
            default -> null;
        };
    }
}