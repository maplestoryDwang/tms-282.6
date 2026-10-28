package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.SpecStat;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.loaders.ItemData;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class ItemBuffs {

    public static void giveItemBuffsFromItemID(Char chr, TemporaryStatManager tsm, int itemID) {
        Map<SpecStat, Integer> specStats = ItemData.getItemInfoByID(itemID).getSpecStats();
        long time = specStats.getOrDefault(SpecStat.time, 0) / 1000;

        boolean specialBuffEffect =
                itemID != GameConstants.BLUE_EXP_ORB_ID
                        && itemID != GameConstants.PURPLE_EXP_ORB_ID
                        && itemID != GameConstants.RED_EXP_ORB_ID
                        && itemID != GameConstants.YELLOW_EXP_ORB_ID;

        for (Map.Entry<SpecStat, Integer> entry : specStats.entrySet()) {
            SpecStat specStat = entry.getKey();
            int value = entry.getValue();
            Option o = new Option(-itemID, time);
            o.nOption = value;
            o.nValue = value;
            switch (specStat) {
                case hp:
                    chr.heal(value);
                    specialBuffEffect = false;
                    break;
                case hpR:
                    chr.heal((int) ((value / 100D) * chr.getMaxHP()));
                    specialBuffEffect = false;
                    break;
                case mp:
                    if (!JobConstants.isNoManaJob(chr.getJob())) {
                        chr.healMP(value);
                    }
                    specialBuffEffect = false;
                    break;
                case mpR:
                    if (!JobConstants.isNoManaJob(chr.getJob())) {
                        chr.healMP((int) ((value / 100D) * chr.getMaxMP()));
                    }
                    specialBuffEffect = false;
                    break;
                case eva:
                    tsm.sendStat(EVA, o);
                    break;
                case speed:
                    tsm.sendStat(Speed, o);
                    break;
                case pad:
                    tsm.sendStat(PAD, o);
                    break;
                case mad:
                    tsm.sendStat(MAD, o);
                    break;
                case pdd:
                    tsm.sendStat(PDD, o);
                    break;
                case acc:
                    tsm.sendStat(ACC, o);
                    break;
                case jump:
                    tsm.sendStat(Jump, o);
                    break;
                case indieAllStat:
                    tsm.sendStat(IndieAllStat, o);
                    break;
                case indieSpeed:
                    tsm.sendStat(IndieSpeed, o);
                    break;
                case indieJump:
                    tsm.sendStat(IndieJump, o);
                    break;
                case indieSTR:
                    tsm.sendStat(IndieSTR, o);
                    break;
                case indieDEX:
                    tsm.sendStat(IndieDEX, o);
                    break;
                case indieINT:
                    tsm.sendStat(IndieINT, o);
                    break;
                case indieLUK:
                    tsm.sendStat(IndieLUK, o);
                    break;
                case indiePad:
                    tsm.sendStat(IndiePAD, o);
                    break;
                case indiePdd:
                    tsm.sendStat(IndiePDD, o);
                    break;
                case indieMad:
                    tsm.sendStat(IndieMAD, o);
                    break;
                case indieBDR:
                    tsm.sendStat(IndieBDR, o);
                    break;
                case indieIgnoreMobpdpR:
                    tsm.sendStat(IndieIgnoreMobpdpR, o);
                    break;
                case indieStatR:
                    tsm.sendStat(IndieStatR, o);
                    break;
                case imhp:
                case indieMhp:
                    tsm.sendStat(IndieMHP, o);
                    break;
                case immp:
                case indieMmp:
                    tsm.sendStat(IndieMMP, o);
                    break;
                case indieBooster:
                    tsm.sendStat(IndieBooster, o);
                    break;
                case indieAcc:
                    tsm.sendStat(IndieACC, o);
                    break;
                case indieEva:
                    tsm.sendStat(IndieEVA, o);
                    break;
                case indieAllSkill:
                    tsm.sendStat(CombatOrders, o);
                    break;
                case indieMhpR:
                    tsm.sendStat(IndieMHPR, o);
                    break;
                case indieMmpR:
                    tsm.sendStat(IndieMMPR, o);
                    break;
                case indieStance:
                    tsm.sendStat(IndieStance, o);
                    break;
                case indieForceSpeed:
                    tsm.sendStat(IndieForceSpeed, o);
                    break;
                case indieForceJump:
                    tsm.sendStat(IndieForceJump, o);
                    break;
                case indieQrPointTerm:
                    tsm.sendStat(IndieQrPointTerm, o);
                    break;
                case indieWaterSmashBuff:
                    tsm.sendStat(IndieCooltimeReduce, o);
                    break;
                case indieExp:
                    tsm.sendStat(IndieEXP, o);
                    break;
                case padRate:
                    tsm.sendStat(IndiePADR, o);
                    break;
                case madRate:
                    tsm.sendStat(IndieMADR, o);
                    break;
                case pddRate:
                    tsm.sendStat(IndiePDDR, o);
                    break;
                case accRate:
                    tsm.sendStat(ACCR, o);
                    break;
                case evaRate:
                    tsm.sendStat(EVAR, o);
                    break;
                case mhpR:
                case mhpRRate:
                    tsm.sendStat(IndieMHPR, o);
                    break;
                case mmpR:
                case mmpRRate:
                    tsm.sendStat(IndieMHPR, o);
                    break;
                case booster:
                    tsm.sendStat(Booster, o);
                    break;
                case expinc:
                case expBuff:
                    tsm.sendStat(ExpBuffRate, o);
                    break;
                case str:
                    tsm.sendStat(CharacterTemporaryStat.STR, o);
                    break;
                case dex:
                    tsm.sendStat(DEX, o);
                    break;
                case inte:
                    tsm.sendStat(INT, o);
                    break;
                case luk:
                    tsm.sendStat(LUK, o);
                    break;
                case asrR:
                    tsm.sendStat(AsrRByItem, o);
                    break;
                case bdR:
                    tsm.sendStat(BdR, o);
                    break;
                case prob:
                    EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                    newStats.put(ItemUpByItem, o);
                    newStats.put(MesoUpByItem, o.deepCopy());
                    tsm.sendStat(newStats);
                    break;
                case inflation:
                    tsm.sendStat(Inflation, o);
                    break;
                case morph:
                    tsm.sendStat(Morph, o);
                    break;
                case repeatEffect:
                    tsm.sendStat(RepeatEffect, o);
                    break;
                case itemInvincible:
                    tsm.sendStat(Invincible, o);
                    break;
                case charismaEXP:
                    chr.addTraitExp(Stat.charismaEXP, value);
                    break;
                case insightEXP:
                    chr.addTraitExp(Stat.insightEXP, value);
                    break;
                case willEXP:
                    chr.addTraitExp(Stat.willEXP, value);
                    break;
                case craftEXP:
                    chr.addTraitExp(Stat.craftEXP, value);
                    break;
                case senseEXP:
                    chr.addTraitExp(Stat.senseEXP, value);
                    break;
                case charmEXP:
                    chr.addTraitExp(Stat.charmEXP, value);
                    break;
                case berserk:
                case incFixedDamageR:
                    tsm.sendStat(IndiePMdR, o);
                    break;
                case dropPer:
                    tsm.sendStat(ItemUpByItem, o);
                    break;
                case mesoAmountRate:
                    if (itemID == 2023664 || itemID == 2023665 || itemID == 2023666) {
                        tsm.sendStat(MesoAmountRate, o);
                    } else {
                        tsm.sendStat(MesoUpByItem, o);
                    }
                    break;
            }
        }
        if (specialBuffEffect) {
            chr.write(UserPacket.effect(Effect.avatarOriented("Effect/BasicEff.img/BonusItemEat")));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.avatarOriented("Effect/BasicEff.img/BonusItemEat")), chr);
        }
    }
}
