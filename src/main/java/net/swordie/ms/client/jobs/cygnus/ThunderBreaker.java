package net.swordie.ms.client.jobs.cygnus;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.PartyBooster;
import net.swordie.ms.client.character.skills.ShootObjectSkillInfo;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class ThunderBreaker extends Noblesse {

    public static final int ELEMENTAL_HARMONY_STR = 10000246;

    public static final int LIGHTNING_ELEMENTAL = 15001022; //Buff (Charge) //Stackable Charge
    public static final int LIGHTNING_ELEMENTAL_ADJUSTED_BUFF = 15001026;
    public static final int ELECTRIFIED = 15000023;
    public static final int FLASH = 15001021;

    public static final int KNUCKLE_BOOSTER = 15101022; //Buff
    public static final int LIGHTNING_BOOST = 15100025;

    public static final int GALE = 15111022; //Special Attack (Charge)
    public static final int LINK_MASTERY = 15110025; //Special Passive
    public static final int LIGHTNING_LORD = 15110026;
    public static final int THUNDER = 15111021;

    public static final int ARC_CHARGER = 15121004; //Buff
    public static final int SPEED_INFUSION = 15121005; //Buff
    public static final int CALL_OF_CYGNUS_TB = 15121000; //Buff
    public static final int TYPHOON = 15120003;
    public static final int THUNDER_GOD = 15120008;

    public static final int GLORY_OF_THE_GUARDIANS_TB = 15121053;
    public static final int PRIMAL_BOLT = 15121054;

    // V Skill
    public static final int LIGHTNING_CASCADE = 400051007;
    public static final int LIGHTNING_CASCADE_ATTACK = 400051013;
    public static final int SHARK_TORPEDO = 400051016;
    public static final int TRIDENT_STRIKE = 400051044;
    public static final int TRIDENT_STRIKE_AFTER_HITS = 400051045;
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_SKILL = 400051058;
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_THUNDERBOLT_1 = 400051065; // UserBonusAttackRequest
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_FINISHER = 400051066;
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_THUNDERBOLT_2 = 400051067; // UserBonusAttackRequest
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_1 = 400051059;
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_2 = 400051060;
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_3 = 400051061;
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_4 = 400051062;
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_5 = 400051063;
    public static final int LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_6 = 400051064;

    // HEXA Boosts
    public static final int HEXA_LIGHTNING_CASCADE = 500061036;
    public static final int HEXA_LIGHTNING_CASCADE_ATTACK = 500061037;

    private final int[] addedSkills = new int[]{
            ELEMENTAL_HARMONY_STR
    };

    private final int[] lightningBuffs = new int[]{
            LIGHTNING_ELEMENTAL,
            ELECTRIFIED,
            LIGHTNING_BOOST,
            LIGHTNING_LORD,
            THUNDER_GOD,
    };

    private int lastAttackSkill = 0;
    private byte arcChargeCDCount;

    public ThunderBreaker(Char chr) {
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
        return JobConstants.isThunderBreaker(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
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
        int chargeProp = getChargeProp();
        if (tsm.hasStat(CygnusElementSkill)
                && hasHitMobs
                && Util.succeedProp(chargeProp)
                && skillID != SHARK_TORPEDO
                && skillID != GALE
                && skillID != TYPHOON) {
            incrementLightningElemental(tsm);
        }
        linkedSkillLogic(skillID);
        if (!chr.hasSkillOnCooldown(LIGHTNING_CASCADE_ATTACK) && chr.hasSkill(LIGHTNING_CASCADE) && hasHitMobs) {
            c.write(UserLocal.lightitngCascadeEffect(skillID, LIGHTNING_CASCADE, chr.getSkillLevel(LIGHTNING_CASCADE)));
        }
        if (chr.hasSkill(LINK_MASTERY)) {
            if (hasHitMobs) {
                giveLinkMasteryBuff(skillID, tsm);
            }
        }
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        switch (skillID) {
            case THUNDER:
                o1.nOption = 100;
                o1.rOption = skillID;
                o1.tOption = 479;
                o1.setInMillis(true);
                tsm.sendStat(Stance, o1);
                break;
            case GALE:
            case TYPHOON:
                int chargeStack = tsm.getOption(IgnoreTargetDEF).mOption;
                int chargeNOption = tsm.getOption(IgnoreTargetDEF).nOption;
                if (tsm.getOptByCTSAndSkill(IndieDamR, GALE) == null || tsm.getOptByCTSAndSkill(IndieDamR, TYPHOON) == null) {
                    EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                    o1.nValue = chargeStack * si.getValue(y, slv);
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(time, slv);
                    newStats.put(IndieDamR, o1); //Indie
                    o2.nOption = chargeNOption;
                    o2.rOption = LIGHTNING_ELEMENTAL_ADJUSTED_BUFF;
                    o2.tOption = 10;
                    o2.xOption = chargeStack;
                    newStats.put(StrikerElectricUsed, o1);
                    if (skillID == TYPHOON) {
                        o3.nOption = 100;
                        o3.rOption = skillID;
                        o3.tOption = 779;
                        o3.setInMillis(true);
                        newStats.put(Stance, o3);
                    }
                    tsm.sendStat(newStats);
                    tsm.removeStat(IgnoreTargetDEF);
                }
                break;
            case LIGHTNING_CASCADE:
            case LIGHTNING_CASCADE_ATTACK:
                si = SkillData.getSkillInfoById(LIGHTNING_CASCADE);
                slv = chr.getSkillLevel(LIGHTNING_CASCADE);
                int cooldown = si.getValue(y, slv);
                chr.addSkillCooldown(LIGHTNING_CASCADE_ATTACK, cooldown * 1000);
                break;
            case LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_1:
            case LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_2:
            case LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_3:
            case LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_4:
            case LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_5:
            case LIGHTNING_SPEAR_MULTISTRIKE_ATTACK_6:
            case LIGHTNING_SPEAR_MULTISTRIKE_FINISHER:
                si = SkillData.getSkillInfoById(LIGHTNING_SPEAR_MULTISTRIKE_SKILL);
                slv = chr.getSkillLevel(si.getSkillId());

                if (!tsm.hasStat(StrikerThunderChain)) {
                    o1.nOption = 1;
                    o1.rOption = si.getSkillId();
                    o1.tOption = si.getValue(time, slv);
                    tsm.sendStat(StrikerThunderChain, o1);
                    chr.setSkillCooldown(si.getSkillId(), slv);
                } else {
                    var opt = tsm.getOption(StrikerThunderChain);
                    opt.nOption++;

                    if (opt.nOption < 12) {
                        tsm.updateStat(StrikerThunderChain, opt);
                    } else {
                        tsm.removeStat(StrikerThunderChain);
                    }
                }

                if (hasHitMobs) {
                    var thunderBoltSkillId = attackInfo.skillId == LIGHTNING_SPEAR_MULTISTRIKE_FINISHER ? LIGHTNING_SPEAR_MULTISTRIKE_THUNDERBOLT_2 : LIGHTNING_SPEAR_MULTISTRIKE_THUNDERBOLT_1;
                    var thunderBoltSI = SkillData.getSkillInfoById(thunderBoltSkillId);
                    var rect = thunderBoltSI.getFirstRect();
                    List<Integer> mobList = new ArrayList<>();
                    for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                        Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                        if (mob == null || mob.getHp() <= 0) {
                            continue;
                        }
                        mobList.add(mob.getObjectId());
                    }
                    rect = chr.getRectAround(rect);
                    chr.write(UserLocal.userBonusAttackRequest(thunderBoltSkillId, mobList,
                            thunderBoltSI.getValue(mobCount, slv),
                            rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom(),
                            (byte) 1));
                }
                break;
        }
    }

    private void linkedSkillLogic(int skillId) {
        if (lastAttackSkill == skillId) {
            return;
        }

        SkillInfo si = SkillData.getSkillInfoById(lastAttackSkill);
        lastAttackSkill = skillId;
        if (si == null || si.getAddAttackSkills().stream().noneMatch(aas -> aas == skillId)) {
            return;
        }

        if (chr.hasSkill(TRIDENT_STRIKE)) {
            incrementTridentStrikeCounter();
        }
    }

    private void incrementTridentStrikeCounter() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(TRIDENT_STRIKE);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int counter = 1;
        if (tsm.hasStat(StrikerComboStack)) {
            counter = tsm.getOption(StrikerComboStack).nOption;
            if (counter < si.getValue(x, slv)) {
                counter++;
            } else {
                counter = 0;
            }
        }
        giveTridentStrikeCTS(counter);
    }

    private void giveTridentStrikeCTS(int counter) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(TRIDENT_STRIKE);
        Option o = new Option();
        if (counter > 0) {
            o.nOption = counter;
            o.rOption = TRIDENT_STRIKE;
            tsm.sendStat(StrikerComboStack, o);
        } else {
            for (Map.Entry<Integer, ExtraSkillInfo> map : si.getExtraSkillInfo().entrySet()) {
                var entry = map.getValue();
                chr.getTimer().addEvent(() -> chr.write(UserLocal.userBonusAttackRequest(entry.getSkillId())), entry.getDelay());
            }
            tsm.removeStatsBySkill(TRIDENT_STRIKE);
        }
    }

    private void giveLinkMasteryBuff(int skillId, TemporaryStatManager tsm) {
        if (lastAttackSkill != skillId) {
            lastAttackSkill = skillId;
            Option o = new Option();
            o.nOption = 3; // <string name="u" value="3" />
            o.rOption = LINK_MASTERY;
            o.tOption = 10;
            tsm.sendStat(StrikerChainReaction, o);
        }
    }

    private void incrementLightningElemental(TemporaryStatManager tsm) {
        int amount = 1;
        if (tsm.hasStat(IgnoreTargetDEF)) {
            amount = tsm.getOption(IgnoreTargetDEF).mOption;
            if (amount < getMaxCharge()) {
                amount++;
            }
        }
        updateLightningElemental(tsm, amount);
    }

    private void updateLightningElemental(TemporaryStatManager tsm, int amount) {
        Option o1 = new Option();
        Option o2 = new Option();
        int skillID = LIGHTNING_ELEMENTAL;
        Skill skill = chr.getSkill(skillID);
        SkillInfo leInfo = SkillData.getSkillInfoById(skillID);
        SkillInfo pbInfo = SkillData.getSkillInfoById(PRIMAL_BOLT);
        int slv = skill.getCurrentLevel();
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        int xVal = (tsm.hasStat(StrikerHyperElectric) ? (pbInfo.getValue(x, slv)) : (leInfo.getValue(x, slv)));
        xVal -= 1;
        var inc = xVal * amount;
        o1.nOption = inc;
        o1.mOption = amount;
        o1.rOption = skillID;
        o1.tOption = 30;
        newStats.put(IgnoreTargetDEF, o1);
        o2.nOption = inc;
        o2.xOption = amount;
        newStats.put(StrikerElectricStack, o1);
        if (tsm.hasStat(StrikerElectricUsed)) {
            Option o3 = tsm.getOption(StrikerElectricUsed);
            o3.nOption = Math.max(o3.nOption - xVal, 0);
            if (o3.nOption <= 0) {
                tsm.removeStat(StrikerElectricUsed);
            } else {
                o3.rOption = LIGHTNING_ELEMENTAL_ADJUSTED_BUFF;
                o3.tOption = 10;
                o3.xOption = amount;
                newStats.put(StrikerElectricUsed, o1);
            }
        }
        tsm.sendStat(newStats);
        reduceArcChargerCoolTime();
    }

    private void reduceArcChargerCoolTime() {
        Skill skill = chr.getSkill(ARC_CHARGER);
        if (skill == null || arcChargeCDCount >= 5) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        arcChargeCDCount++;
        chr.reduceSkillCoolTime(ARC_CHARGER, (si.getValue(y, slv) * 1000L));
    }

    private Skill getLightningChargeSkill() {
        Skill skill = null;
        for (int lightningSkill : lightningBuffs) {
            if (chr.hasSkill(lightningSkill)) {
                skill = chr.getSkill(lightningSkill);
            }
        }
        return skill;
    }

    private int getChargeProp() {
        Skill skill = getLightningChargeSkill();
        if (skill != null) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            return si.getValue(prop, slv);
        }
        return 0;
    }

    private int getMaxCharge() {
        int num = 0;
        for (int skill : lightningBuffs) {
            if (chr.hasSkill(skill)) {
                num++;
            }
        }
        return num;
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
        switch (skillID) {
            case LIGHTNING_ELEMENTAL:
                if (tsm.hasStat(CygnusElementSkill)) {
                    tsm.removeStat(CygnusElementSkill);
                } else {
                    o1.nOption = slv;
                    o1.rOption = skillID;
                    tsm.sendStat(CygnusElementSkill, o1);
                }
                break;
            case ARC_CHARGER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ShadowPartner, o1);
                arcChargeCDCount = 0;
                break;
            case SPEED_INFUSION:
                PartyBooster tsb = (PartyBooster) tsm.getTSBByTSIndex(TSIndex.PartyBooster);
                tsb.setNOption(si.getValue(x, slv));
                tsb.setROption(skillID);
                tsb.setCurrentTime(Util.getCurrentTime());
                tsb.setStartTime(System.currentTimeMillis());
                tsb.setExpireTerm(si.getValue(time, slv));
                tsm.sendStat(PartyBooster, tsb.getOption());
                break;
            case LINK_MASTERY:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(DamR, o1);
                break;
            case GLORY_OF_THE_GUARDIANS_TB:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case PRIMAL_BOLT:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(StrikerHyperElectric, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieDamR, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o2);
                tsm.sendStat(newStats);
                chr.resetSkillCoolTime(TYPHOON);
                chr.resetSkillCoolTime(GALE);
                break;
            case LIGHTNING_CASCADE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(LightningUnion, o1);
                break;
        }
    }

    @Override
    public int alterCooldownSkill(int skillId) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (skillId) {
            case GALE:
            case TYPHOON:
                if (tsm.hasStat(StrikerHyperElectric)) {
                    return 0;
                }
        }
        return -1;
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        super.handleHit(c, inPacket, hitInfo);
    }

    public void handleShootObject(Char chr, ShootObjectSkillInfo sosi) {
        var skillId = sosi.getSkillId();
        var slv = sosi.getSlv();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        switch (skillId) {
            case SHARK_TORPEDO:
                int amount = tsm.getOption(IgnoreTargetDEF).mOption;
                if (amount < 2) {
                    chr.chatMessage("雷電充能不足，無法使用這個技能。");
                    return;
                }
                updateLightningElemental(tsm, amount - si.getValue(x, slv));
                break;
        }
        super.handleShootObject(chr, sosi);
    }
}
