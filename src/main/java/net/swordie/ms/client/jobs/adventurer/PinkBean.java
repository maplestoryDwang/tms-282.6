package net.swordie.ms.client.jobs.adventurer;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Summoned;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.EnumMap;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/**
 * Created on 12/14/2017.
 */
public class PinkBean extends Job {

    public static final int PINK_WARRIOR = 131001018;

    public static final int CHILL_OUT_ZZZ = 131001306; //Buff
    public static final int CHILL_OUT_TONGUE_OUT = 131001106; //Buff
    public static final int CHILL_OUT_MYSTERIOUS_COCKTAIL = 131001406; //Buff
    public static final int CHILL_OUT_NOM_NOM_MEAT = 131001206; //Buff
    public static final int CHILL_OUT_HEADSET = 131001506; //Buff

    public static final int INSTANT_GARDEN_POSIE = 131001107; //Area of Effect
    public static final int INSTANT_GARDEN_BREEZY = 131001207; //Area of Effect
    public static final int INSTANT_GARDEN_PRETTY = 131001307; //Summon

    public static final int MATRYOSHKA = 131001023; //   Pink Bean's Matryoshka
    public static final int GO_MINI_BEANS = 131001015; //   ON/OFF buff
    public static final int MINI_BEANS = 131002015; //Summon Info
    public static final int EVERYBODY_HAPPY = 131001009; //Buff
    public static final int LETS_ROLL = 131001004;
    public static final int BLAZING_YOYO = 131001010;
    public static final int BLAZING_YOYO_2 = 131001011;
    public static final int PINK_SHADOW = 131001017;
    public static final int PINK_SHADOW_1 = 131002017;
    public static final int PINK_SHADOW_2 = 131003017;
    private final int MAX_YOYO_STACK = 8;
    private int yoyo;

    public PinkBean(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isPinkBean(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
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
            if (skillID != MINI_BEANS) {
                summonGoMiniBeans(attackInfo);
            }
            if (skillID == BLAZING_YOYO || skillID == BLAZING_YOYO_2) {
                costYoYo();
            }
        }
    }

    private void costYoYo() {
        boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        if (tms282) yoyo = Math.max(0, yoyo - 1);
        Option o = new Option();
        o.nOption = tms282 ? yoyo : (yoyo - 1);
        chr.getTemporaryStatManager().sendStat(PinkbeanYoYoStack, o);
    }

    public void incrementYoYoStack(int amount) {
        yoyo += amount;
        yoyo = Math.min(MAX_YOYO_STACK, yoyo);
        Option o = new Option();
        o.nOption = yoyo;
        chr.getTemporaryStatManager().sendStat(PinkbeanYoYoStack, o);
    }

    private void summonGoMiniBeans(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();
        int currentCount = field.getSummonsByChar(chr).stream().filter(s -> s.getSkillID() == MINI_BEANS).toList().size();
        if (tsm.hasStat(PinkbeanYoYoAddDamR)) {
            SkillInfo miniBeanInfo = SkillData.getSkillInfoById(GO_MINI_BEANS);
            byte slv = (byte) chr.getSkillLevel(GO_MINI_BEANS);
            int minibeanproc = miniBeanInfo.getValue(z, slv);
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Life life = chr.getField().getLifeByObjectID(mai.mobId);
                if (life instanceof Mob mob) {
                    if (mob == null || mob.getHp() <= 0) {
                        continue;
                    }
                    if (Util.succeedProp(minibeanproc) && currentCount < 3) {
                        Summon summon = Summon.getSummonBy(c.getChr(), MINI_BEANS, slv);
                        summon.setFlyMob(true);
                        summon.setPosition(mob.getPosition());
                        summon.setMoveAbility(MoveAbility.FlyRandom);
                        field.spawnAddSummon(summon);
                    }
                }
            }
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
        Summon summon;
        Field field;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        switch (skillID) {
            case CHILL_OUT_ZZZ:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indiePadR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePADR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieExp, slv);
                o2.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieEXP, o2);
                break;
            case CHILL_OUT_MYSTERIOUS_COCKTAIL:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieAsrR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieAsrR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieExp, slv);
                o2.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieEXP, o2);
                break;
            case CHILL_OUT_NOM_NOM_MEAT:    //Regen 1%MaxHP per second
                o1.nOption = si.getValue(dotHealHPPerSecondR, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.xOption = chr.getMaxHP();
                tsm.sendStat(DotHealHPPerSecond, o1);  //DoTHealHPPerSecond  Rate?
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieExp, slv);
                o2.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieEXP, o2);
                break;
            case CHILL_OUT_HEADSET:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieAsrR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieAsrR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieExp, slv);
                o2.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieEXP, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indiePadR, slv);
                o3.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePADR, o3);
                break;
            case INSTANT_GARDEN_PRETTY: //Summon
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                field = c.getChr().getField();
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                summon.setAttackActive(false);
                field.spawnSummon(summon);
                break;
            case GO_MINI_BEANS: //  ON/OFF Buff
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(PinkbeanYoYoAddDamR, o1);
                break;
            case MATRYOSHKA:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(NotDamaged, o1);
                o2.nOption = 1;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                tsm.sendStat(PinkbeanMatryoshka, o2);
                break;
            case EVERYBODY_HAPPY:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(y, slv);
                tsm.sendStat(NotDamaged, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieExp, slv);
                o2.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieEXP, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieSpeed, slv);
                o3.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieSpeed, o3);
                o4.nReason = skillID;
                o4.nValue = si.getValue(indiePadR, slv);
                o4.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePADR, o4);
                o5.nReason = skillID;
                o5.nValue = si.getValue(indieMadR, slv);
                o5.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieMADR, o5);
                break;
            case PINK_SHADOW:
                field = chr.getField();
                Position pos;

                Summon pinkShadow = Summon.getSummonByAndSetStat(chr, PINK_SHADOW, (byte) 1);
                pinkShadow.setFlyMob(false);
                pinkShadow.setAvatarLook(chr.getAvatarData().getAvatarLook());
                pinkShadow.setMoveAbility(MoveAbility.WalkClone);
                pinkShadow.setAssistType(AssistType.AttackManual);
                pinkShadow.setAttackActive(true);
                field.broadcast(Summoned.attackActive(pinkShadow));
                field.spawnSummon(pinkShadow);

                Summon pinkShadow2 = Summon.getSummonByAndSetStat(chr, PINK_SHADOW_1, (byte) 1);
                pinkShadow2.setFlyMob(false);
                pinkShadow2.setAvatarLook(chr.getAvatarData().getAvatarLook());
                pinkShadow2.setMoveAbility(MoveAbility.WalkClone);
                pinkShadow2.setAssistType(AssistType.AttackManual);
                pinkShadow2.setAttackActive(true);
                pinkShadow2.setActionDelay(800);
                pinkShadow2.setMovementDelay(60);
                field.broadcast(Summoned.attackActive(pinkShadow2));
                field.spawnSummon(pinkShadow2);

                Summon pinkShadow3 = Summon.getSummonByAndSetStat(chr, PINK_SHADOW_2, (byte) 1);
                pinkShadow3.setFlyMob(false);
                pinkShadow3.setAvatarLook(chr.getAvatarData().getAvatarLook());
                pinkShadow3.setMoveAbility(MoveAbility.WalkClone);
                pinkShadow3.setAssistType(AssistType.AttackManual);
                pinkShadow3.setAttackActive(true);
                pinkShadow2.setActionDelay(1200);
                pinkShadow2.setMovementDelay(90);
                field.broadcast(Summoned.attackActive(pinkShadow3));
                field.spawnSummon(pinkShadow3);
                break;
            case CHILL_OUT_TONGUE_OUT:
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieExp, slv);
                o2.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieEXP, o2);
                if (inPacket.getUnreadAmount() > 0) {
                    Rect rect = new Rect(inPacket.decodeShort(), inPacket.decodeShort(), inPacket.decodeShort(), inPacket.decodeShort());
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    o2.nOption = -si.getValue(z, slv);
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(subTime, slv);
                    for (Life life : chr.getField().getLifesInRect(rect)) {
                        if (life instanceof Mob mob && mob.getHp() > 0) {
                            MobTemporaryStat mts = mob.getTemporaryStat();
                            if (Util.succeedProp(si.getValue(prop, slv))) {
                                EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
                                map.put(MobStat.PDR, o1.deepCopy());
                                map.put(MobStat.MDR, o1.deepCopy());
                                map.put(MobStat.Darkness, o2.deepCopy());
                                mts.addStatOptions(mob, map);
                            }
                        }
                    }
                }
                break;
            case INSTANT_GARDEN_BREEZY:
                SkillInfo isb = SkillData.getSkillInfoById(INSTANT_GARDEN_BREEZY);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, INSTANT_GARDEN_BREEZY, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setFlip(!chr.isLeft());
                aa.setRect(aa.getPosition().getRectAround(isb.getRects().get(0)));
                aa.setDelay((short) 10);
                chr.getField().spawnAffectedArea(aa);
                break;
            case INSTANT_GARDEN_POSIE:
                SkillInfo isp = SkillData.getSkillInfoById(INSTANT_GARDEN_POSIE);
                AffectedArea aa2 = AffectedArea.getPassiveAA(chr, INSTANT_GARDEN_POSIE, slv);
                aa2.setMobOrigin((byte) 0);
                aa2.setPosition(chr.getPosition());
                aa2.setFlip(!chr.isLeft());
                aa2.setRect(aa2.getPosition().getRectAround(isp.getRects().get(0)));
                aa2.setDelay((short) 12);
                chr.getField().spawnAffectedArea(aa2);
                break;
            case PINK_WARRIOR:
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                o1.nValue = si.getValue(y, slv);
                tsm.sendStat(IndieAllStat, o1);
                break;
        }
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        super.handleHit(c, inPacket, hitInfo);
    }
}
