package net.swordie.ms.client.jobs.adventurer.pirate;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.GuidedBullet;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Corsair extends Pirate {

    public static final int SCURVY_SUMMONS = 5201012; //Summon
    public static final int SCURVY_SUMMONS_ATOM = 5201017; //Atom
    public static final int INFINITY_BLAST = 5200016; //Passive
    public static final int ALL_ABOARD = 5210015; //Summon
    public static final int ALL_ABOARD_SHOOT = 5211020; //Summon

    public static final int ROLL_OF_THE_DICE = 5211007; //Buff
    public static final int SIEGE_BOMBER = 5211014; //Summon

    public static final int QUICKDRAW = 5220055; //Passive
    public static final int PARROTARGETTING = 5221015; //Special Attack
    public static final int NAUTILUS_STRIKE = 5221013; //Special Attack
    public static final int MAPLE_WARRIOR = 5221000; //Buff
    public static final int JOLLY_ROGER = 5221018; //Buff
    public static final int PIRATE_REVENGE = 5220012;
    public static final int ROLL_OF_THE_DICE_DD = 5220014;
    public static final int ROLL_OF_THE_DICE_ADDITION = 5220044;
    public static final int ROLL_OF_THE_DICE_SAVING_GRACE = 5220043;
    public static final int ROLL_OF_THE_DICE_ENHANCE = 5220045;
    public static final int HEROS_WILL = 5221010;
    public static final int BRAIN_SCRAMBLER = 5221016;
    public static final int MAJESTIC_PRESENCE = 5220020;
    public static final int AHOY_MATEYS = 5220019;
    public static final int FIRING_ORDERS = 5221029;
    public static final int BROADSIDE = 5221022;
    public static final int BROADSIDE_SUMMON = 5221027;
    public static final int EPIC_ADVENTURER = 5221053;
    public static final int WHALERS_POTION = 5221054;

    // V skills
    public static final int BULLET_BARRAGE = 400051006;
    public static final int TARGET_LOCK = 400051021;
    public static final int NAUTILUS_ASSAULT = 400051040;
    public static final int NAUTILUS_ASSAULT_2 = 400051049;
    public static final int NAUTILUS_ASSAULT_3 = 400051050;
    public static final int DEATH_TRIGGER = 400051073;
    public static final int DEATH_TRIGGER_2 = 400051081;

    // HEXA Skills
    public static final int HEXA_RAPID_FIRE = 5241000;
    public static final int HEXA_RAPID_FIRE_SHOOTOUT_MODE = 5241001;
    public static final int HEXA_BROADSIDE = 5241002;
    public static final int HEXA_BROADSIDE_SUMMON = 5241003;
    public static final int HEXA_BRAIN_SCRAMBLER = 5241005;
    public static final int CONDEMNATION = 5241006;
    public static final int HEXA_FIRING_ORDERS = 5241009;
    public static final int HEXA_SCURVY_SUMMONS = 5241010;
    public static final int HEXA_SCURVY_SUMMONS_ATOM = 5241011;
    public static final int HEXA_ALL_ABOARD_SHOOT = 5241013; //Summon
    public static final int HEXA_SIEGE_BOMBER = 5241015;
    public static final int HEXA_NAUTILUS_STRIKE = 5241018; //Special Attack

    public Corsair(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isCorsair(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
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
        if (hasHitMobs) {
            activateQuickdraw(attackInfo);
        }
        Option o1 = new Option();
        switch (skillID) {
            case TARGET_LOCK:
                chr.addSkillCooldown(skillID, (int) (si.getValue(cooltime, slv) * 1000L));
                break;
            case NAUTILUS_ASSAULT:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv); // <string name="time" value="3000" />
                o1.setInMillis(true);
                tsm.sendStat(IndieNotDamaged, o1);
                break;
            case PARROTARGETTING:
                if (hasHitMobs) {
                    GuidedBullet guidedBullet = (GuidedBullet) tsm.getTSBByTSIndex(TSIndex.GuidedBullet);
                    List<Mob> mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
                    boolean tms282 = net.swordie.ms.connection.packet.Tms282RemoteBuffs.active();
                    if (tms282 && mobs.isEmpty()) break; // A miss/stale target keeps the existing mark.
                    Mob target = Util.getRandomFromCollection(mobs);
                    int previous = tsm.hasStat(GuidedBullet) ? tsm.getOption(GuidedBullet).xOption : 0;
                    guidedBullet.setNOption(1);
                    guidedBullet.setROption(skillID);
                    guidedBullet.setMobID(target.getObjectId());
                    if (tms282) {
                        guidedBullet.setXOption(target.getObjectId());
                        guidedBullet.setYOption(previous);
                    }
                    tsm.sendStat(GuidedBullet, tms282 ? guidedBullet.getOption().deepCopy() : guidedBullet.getOption());
                }
                break;
            case HEXA_RAPID_FIRE:
                o1 = tsm.getOption(RapidFire);
                o1.nOption = Math.min(o1.nOption + 1, si.getValue(z, slv) * si.getValue(y, slv));
                if (o1.nOption >= 50) {
                    o1.xOption = 1;
                }
                tsm.sendStat(RapidFire, o1);
                break;
            case HEXA_RAPID_FIRE_SHOOTOUT_MODE:
                o1 = tsm.getOption(RapidFire);
                o1.nOption = Math.max(o1.nOption - 2, 0);
                if (o1.nOption <= 0) {
                    o1.xOption = 0;
                }
                tsm.sendStat(RapidFire, o1);
                break;
            case HEXA_BRAIN_SCRAMBLER:
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) break;
                o1 = tsm.getOption(EnhanceHeadShot);
                o1.nOption = Math.max(o1.nOption + 1, si.getValue(w, slv));
                tsm.sendStat(EnhanceHeadShot, o1);
                break;
        }
    }

    /** TMS282 String: two successful headshot uses, not attempted casts or individual damage lines.
     * AttackHandler calls once after the first mob actually loses HP to an accepted attack. */
    public void onVerifiedHeadshotHit(int skillId) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 || skillId != HEXA_BRAIN_SCRAMBLER
                || !JobConstants.isCorsair(chr.getJob())) return;
        int level = chr.getSkillLevel(skillId);
        var info = SkillData.getSkillInfoById(skillId);
        if (info == null || level <= 0 || level > info.getMaxLevel()) return;
        var tsm = chr.getTemporaryStatManager();
        var counter = tsm.getOption(EnhanceHeadShot);
        counter.nOption = Math.min(counter.nOption + 1, info.getValue(w, level));
        tsm.sendStat(EnhanceHeadShot, counter);
    }

    private void activateQuickdraw(AttackInfo attackInfo) {
        int skillID = QUICKDRAW;
        if (!chr.hasSkill(skillID)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkillLevel(skillID);
        if (si.getSkillList1().contains(attackInfo.skillId)) {
            if (tsm.hasStat(QuickDraw)) {
                tsm.removeStat(QuickDraw);
            } else {
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    Option o = new Option();
                    o.nOption = 1;
                    o.rOption = skillID;
                    tsm.sendStat(QuickDraw, o);
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
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Summon summon;
        Field field;
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        Option o6 = new Option();
        switch (skillID) {
            case FIRING_ORDERS:
            case HEXA_FIRING_ORDERS:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 4);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                summon.setAttackActive(true);
                field.spawnSummon(summon);
                break;
            case ROLL_OF_THE_DICE: {
                int upbound = 6;
                if (chr.hasSkill(ROLL_OF_THE_DICE_DD) && chr.hasSkill(ROLL_OF_THE_DICE_ADDITION)) {
                    upbound = 7;
                }
                int diceThrow1 = new Random().nextInt(upbound) + 1;

                if (chr.hasSkill(ROLL_OF_THE_DICE_ENHANCE) && Util.succeedProp(40)) {
                    diceThrow1 = new Random().nextInt(4) + 4;
                }

                chr.write(UserPacket.effect(Effect.skillAffectedSelect(skillID, slv, diceThrow1, false)));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffectedSelect(skillID, slv, diceThrow1, false)), chr);

                if (diceThrow1 < 2) {
                    chr.reduceSkillCoolTime(skillID, (1000L * si.getValue(cooltime, slv)) / 2);
                    return;
                }

                o1.nOption = diceThrow1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);

                tsm.throwDice(diceThrow1);
                tsm.sendStat(Dice, o1);
                break;
            }
            case ROLL_OF_THE_DICE_DD: {
                chr.removeBaseStatByOption();
                int upbound = 6;
                if (chr.hasSkill(ROLL_OF_THE_DICE_DD) && chr.hasSkill(5220044)) {
                    upbound = 7;
                }

                int random = new Random().nextInt(upbound) + 1;
                int randomDD = new Random().nextInt(upbound) + 1;
                Option o = tsm.getOption(LoadedDice);
                if (o != null && tsm.hasStat(LoadedDice)) {
                    //Background
                    Effect eff1 = Effect.avatarOriented("Skill/40005.img/skill/400051000/affected1");
                    chr.write(UserPacket.effect(eff1));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), eff1), chr);
                    //Top Dice
                    Effect eff2 = Effect.avatarOriented("Skill/40005.img/skill/400051000/affected/" + o.nOption);
                    chr.write(UserPacket.effect(eff2));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), eff2), chr);
                    Effect eff3 = Effect.avatarOriented("Skill/40005.img/skill/400051000/specialAffected0/" + random);
                    chr.write(UserPacket.effect(eff3));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), eff3), chr);
                    Effect eff4 = Effect.avatarOriented("Skill/40005.img/skill/400051000/specialAffected/" + randomDD);
                    chr.write(UserPacket.effect(eff4));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), eff4), chr);
                } else {
                    chr.write(UserPacket.effect(Effect.skillAffectedSelect(skillID, slv, random, false)));
                    chr.write(UserPacket.effect(Effect.skillAffectedSelect(skillID, slv, randomDD, true)));
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffectedSelect(skillID, slv, random, false)), chr);
                    chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffectedSelect(skillID, slv, randomDD, true)), chr);
                }
                if (random < 2 && randomDD < 2) {
                    return;
                }
                chr.addBaseStatByDiceNumber(random, si, slv);
                chr.addBaseStatByDiceNumber(randomDD, si, slv);
                if (o != null) {
                    chr.addBaseStatByDiceNumber(o.nOption, si, slv);
                }
                o1.nOption = (random * 10) + randomDD; // if rolled: 3 and 5, the DoubleDown nOption = 35
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.throwDice(random, randomDD);

                tsm.sendStat(Dice, o1);
                chr.reduceSkillCoolTime(NAUTILUS_STRIKE, (long) (chr.getRemainingCoolTime(NAUTILUS_STRIKE) * 0.5F));
                break;
            }
            case JOLLY_ROGER:
                o1.nValue = si.getValue(indieEva, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieEVA, o1);
                o2.nValue = si.getValue(indieAsrR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieAsrR, o2);
                o3.nValue = si.getValue(indieTerR, slv);
                o3.nReason = skillID;
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieTerR, o3);
                o4.nValue = si.getValue(indiePadR, slv);
                o4.nReason = skillID;
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndiePADR, o4);
                o5.nOption = 0;
                o5.rOption = skillID;
                o5.tOption = si.getValue(time, slv);
                newStats.put(DamR, o5);
                o6.nOption = 0;
                o6.rOption = skillID;
                o6.tOption = si.getValue(time, slv);
                newStats.put(TerR, o6);
                tsm.sendStat(newStats);
                break;
            case EPIC_ADVENTURER:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case WHALERS_POTION:
                o1.nOption = si.getValue(w, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(IgnoreMobDamR, o1);
                o2.nOption = si.getValue(w, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(UnwearyingRun, o2);
                tsm.sendStat(newStats);
                break;
            case BROADSIDE:
                field = chr.getField();
                if (field.getSummonsBySkillId(chr, BROADSIDE_SUMMON).size() < 2) {
                    summon = Summon.getSummonByAndSetStat(chr, BROADSIDE_SUMMON, slv);
                    summon.setFlyMob(false);
                    summon.setMoveAction((byte) 5);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setAssistType(AssistType.CreateB2BodyRequests);
                    summon.setAttackActive(true);
                    field.spawnAddSummon(summon);
                }
                break;
            case HEXA_BROADSIDE:
                field = chr.getField();
                if (field.getSummonsBySkillId(chr, HEXA_BROADSIDE_SUMMON).size() < 2) {
                    summon = Summon.getSummonByAndSetStat(chr, HEXA_BROADSIDE_SUMMON, slv);
                    summon.setFlyMob(false);
                    summon.setMoveAction((byte) 5);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setAssistType(AssistType.CreateB2BodyRequests);
                    summon.setAttackActive(true);
                    field.spawnAddSummon(summon);
                }
                break;
            case SIEGE_BOMBER: //Stationary, Attacks
            case HEXA_SIEGE_BOMBER: //Stationary, Attacks
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 4);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case SCURVY_SUMMONS: //Moves, Attacks
            case HEXA_SCURVY_SUMMONS: //Moves, Attacks
                handleScurvySummons(si, slv);
                break;
            case BULLET_BARRAGE:
                o1.nOption = si.getValue(y, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(BulletParty, o1);
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case CONDEMNATION:
                o1 = tsm.getOption(EnhanceHeadShot);
                o1.nOption = 0;
                tsm.sendStat(EnhanceHeadShot, o1);
                break;
        }
    }

    private void handleScurvySummons(SkillInfo si, int slv) {
        final long now = System.currentTimeMillis();
        int[] summons = si.getSkillId() ==  HEXA_SCURVY_SUMMONS ? new int[]{5241012, si.getSkillId()} : new int[]{5211019, ALL_ABOARD};
        for (int i : summons) {
            Option o = new Option();
            o.nOption = i;
            o.rOption = AHOY_MATEYS;
            o.tOption = si.getValue(time, slv);
            chr.getTemporaryStatManager().sendStat(SpiritLink, o);
            Summon summon = Summon.getSummonByAndSetStatWithTime(chr, i, slv, now, si.getValue(time, slv));
            summon.setFlyMob(false);
            summon.setMoveAction((byte) 4);
            summon.setMoveAbility(MoveAbility.WalkSmart);
            summon.setAssistType(i == HEXA_SCURVY_SUMMONS || i == ALL_ABOARD ? AssistType.SequenceAttack : AssistType.CreateB2BodyRequests);
            summon.setAttackActive(true);
            chr.getField().spawnSummon(summon);
        }
    }

    public void handleScurvySecondAtoms() {
        if (!chr.getTemporaryStatManager().hasStat(SpiritLink)) {
            return;
        }
        int skillID = chr.hasSkill(HEXA_SCURVY_SUMMONS) ? HEXA_SCURVY_SUMMONS : SCURVY_SUMMONS;
        int atomSkillID = skillID == HEXA_SCURVY_SUMMONS ? HEXA_SCURVY_SUMMONS_ATOM : SCURVY_SUMMONS_ATOM;
        int summonID = skillID == HEXA_SCURVY_SUMMONS ? HEXA_SCURVY_SUMMONS : ALL_ABOARD;
        SkillInfo si = SkillData.getSkillInfoById(atomSkillID);
        int slv = chr.getSkillLevel(skillID);
        Rect rect = chr.getRectAround(si.getFirstRect());
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        int bulletCount = si.getValue(SkillStat.bulletCount, slv);
        final var mob = Util.getRandomFromCollection(chr.getField().getMobsInRect(rect));
        List<SecondAtom> secondAtoms = new LinkedList<>();
        final long start = System.currentTimeMillis();
        for (int key = 0; key < bulletCount; key++) {
            var sai = si.getSecondAtomInfos().get(key);
            var pos = chr.getField().getSummonBySkillId(chr, summonID).getPosition().add(sai.getPos());
            SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(),
                    mob != null ? mob.getObjectId() : 0, key, si.getSkillId(), pos, start);
            secondAtoms.add(fa);
        }
        chr.createSecondAtom(secondAtoms);
        chr.write(UserPacket.effect(Effect.skillAffected(summonID, slv, 0)));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffected(summonID, slv, 0)), chr);
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        int skillID = PIRATE_REVENGE;
        if (chr.hasSkill(skillID) && !chr.hasSkillOnCooldown(skillID)) {
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = chr.getSkillLevel(skillID);
            int prop = si.getValue(SkillStat.prop, slv);
            if (Util.succeedProp(prop)) {
                Option o = new Option();
                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                o.nValue = si.getValue(indieDamR, slv);
                o.nReason = skillID;
                o.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o);
                chr.addSkillCooldown(skillID, 500);
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case CONDEMNATION -> {
                return 1;
            }
            case HEXA_BROADSIDE -> {
                int skillID = BROADSIDE;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_NAUTILUS_STRIKE -> {
                int skillID = NAUTILUS_STRIKE;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case 5241017 -> {
                int skillID = 5221052;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case NAUTILUS_ASSAULT_3 -> {
                int skillID = NAUTILUS_ASSAULT;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_SCURVY_SUMMONS -> {
                int skillID = SCURVY_SUMMONS;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        // hacks to bypass the quest glitch (accept but no packet)
        if (level == 30) {
            chr.completeQuest(1424);
        } else if (level == 60) {
            chr.completeQuest(1444);
        } else if (level == 100) {
            chr.completeQuest(1458);
        }
    }
}
