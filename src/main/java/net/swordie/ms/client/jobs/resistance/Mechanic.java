package net.swordie.ms.client.jobs.resistance;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.ForceAtomInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.Mechanic;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.ForceAtomEnum.*;

/**
 * Created on 12/14/2017.
 */
public class Mechanic extends Citizen {

    public static final int MECH_VEHICLE = 1932016;

    public static final int SECRET_ASSEMBLY = 30001281;
    public static final int MECHANIC_DASH = 30001068;
    public static final int HIDDEN_PEACE = 30000227;

    public static final int HUMANOID_MECH = 35001002; //Mech Suit
    public static final int TANK_MECH = 35111003; //Tank Mech Suit

    public static final int MECHANIC_RAGE = 35101006; //Buff
    public static final int PERFECT_ARMOR = 35101007; //Buff (ON/OFF)
    public static final int OPEN_PORTAL_GX9 = 35101005; //Special Skill
    public static final int ROBO_LAUNCHER_RM7 = 35101012; //Summon
    public static final int HOMING_BEACON = 35101002;

    public static final int ROCK_N_SHOCK = 35111002; //Special Summon
    public static final int ROLL_OF_THE_DICE = 35111013; //Special Buff
    public static final int SUPPORT_UNIT_HEX = 35111008; //Summon
    public static final int ADV_HOMING_BEACON = 35110017;

    public static final int ROBOT_MASTERY = 35120001;
    public static final int BOTS_N_TOTS = 35121009; //Special Summon
    public static final int BOTS_N_TOTS_SUB_SUMMON = 35121011; // Summon that spawn from the main BotsNtots
    public static final int MAPLE_WARRIOR_MECH = 35121007; //Buff
    public static final int ENHANCED_SUPPORT_UNIT = 35120002;
    public static final int HEROS_WILL_MECH = 35121008;
    public static final int HOMING_BEACON_RESEARCH = 35120017;
    public static final int ROLL_OF_THE_DICE_DD = 35120014; //Special Buff
    public static final int GIANT_ROBOT_SG_88 = 35121003;

    public static final int FOR_LIBERTY_MECH = 35121053;
    public static final int FULL_SPREAD = 35121055;
    public static final int DISTORTION_BOMB = 35121052;

    // V Skills
    public static final int MULTIPURPOSE_BOT_MFL = 400051009;
    public static final int MOBILE_MISSILE_BATTERY = 400051017;
    public static final int FULL_METAL_BARRAGE = 400051041;
    public static final int MECHA_CARRIER = 400051068; // Summon
    public static final int MECHA_CARRIER_2 = 400051069; // SecondAtom

    private final int[] addedSkills = new int[]{
            SECRET_ASSEMBLY,
            MECHANIC_DASH,
            HIDDEN_PEACE,};

    private final int[] homingBeacon = new int[]{
            HOMING_BEACON,
            ADV_HOMING_BEACON,
            HOMING_BEACON_RESEARCH,};

    private ScheduledFuture<?> supportUnitTimer;
    private byte gateId = 0;

    public Mechanic(Char chr) {
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
        return JobConstants.isMechanic(id);
    }

    public void healFromSupportUnit(Summon summon) {
        Char owner = summon.getChr();
        if (owner == null) {
            return;
        }
        if (owner.hasSkill(ENHANCED_SUPPORT_UNIT) || owner.hasSkill(SUPPORT_UNIT_HEX)) {
            SkillInfo si = SkillData.getSkillInfoById(SUPPORT_UNIT_HEX);
            byte slv = (byte) owner.getSkill(SUPPORT_UNIT_HEX).getCurrentLevel();
            int healrate = si.getValue(hp, slv);
            chr.heal((int) (chr.getMaxHP() * ((double) healrate / 100)));
        }
    }

    public void spawnBotsNTotsSubSummons(Summon summon) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Position position = summon.getPosition();

        if ((tsm.getOptByCTSAndSkill(IndieEmpty, BOTS_N_TOTS) != null) && chr.hasSkill(BOTS_N_TOTS)) {
            if (chr.getField().findFootHoldBelow(position) != null) {
                Skill skill = chr.getSkill(BOTS_N_TOTS);
                int slv = skill.getCurrentLevel();
                Summon subSummon = Summon.getSummonByAndSetStat(chr, BOTS_N_TOTS_SUB_SUMMON, slv);
                subSummon.setCurFoothold((short) chr.getField().findFootHoldBelow(position).getId());
                subSummon.setPosition(position);
                subSummon.setAttackActive(false);
                subSummon.setMoveAbility(MoveAbility.WalkRandom);
                chr.getField().spawnAddSummon(subSummon);
            } else {
                chr.chatMessage("請換個位置再使用這個技能。");
            }
        }
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
        int slv = attackInfo.slv;
        int skillID = attackInfo.skillId;
        if (si == null) {
            return;
        }
        switch (skillID) {
            case DISTORTION_BOMB:
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setPosition(chr.getPosition().deepCopy());
                Rect rect = aa.getPosition().getRectAround(si.getRects().getFirst());
                aa.setRect(rect);
                aa.setFlip(!chr.isLeft());
                chr.getField().spawnAffectedAreaAndRemoveOld(aa);
                break;
        }
    }

    private void createMicroMissileForceAtoms(int skillId, int slv) {
        if (!chr.hasSkill(skillId)) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        Field field = chr.getField();
        ForceAtomEnum fae = INVISIBLE_ATOM;
        List<Integer> targetList = new ArrayList<>();
        List<ForceAtomInfo> faiList = new ArrayList<>();
        int totalMobCount = si.getValue(mobCount, slv);
        for (int i = 0; i < totalMobCount; i++) {
            Mob mob = Util.getRandomFromCollection(field.getMobs());
            if (mob == null || mob.getHp() <= 0) {
                targetList.add(0);
                continue;
            }
            targetList.add(Util.getRandomFromCollection(field.getMobs()).getObjectId());
        }
        int totalMissileCount = si.getValue(bulletCount, slv);
        for (int i = 0; i < totalMissileCount; i++) {
            int fImpact = new Random().nextInt(36) + 15;
            int sImpact = new Random().nextInt(3) + 5;
            int delay = new Random().nextInt(500) + 2000;
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, sImpact, new Random().nextInt(360), delay, Util.getCurrentTime(), 0, 0, new Position());
            faiList.add(fai);
        }
        ForceAtom fa = new ForceAtom(chr.getId(), fae, targetList, skillId, faiList);
        chr.createForceAtom(fa);
    }

    private void createHumanoidMechRocketForceAtom() { // Humanoid Rockets are spread around
        Field field = chr.getField();
        SkillInfo si = SkillData.getSkillInfoById((chr.hasSkill(ADV_HOMING_BEACON) ? ADV_HOMING_BEACON : HOMING_BEACON));
        int slv = getHomingBeaconSkill().getCurrentLevel();
        Rect rect = chr.getPosition().getRectAround(si.getRects().get(0));
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        List<Mob> mobs = field.getMobsInRect(rect);
        if (mobs.size() <= 0) {
            return;
        }
        ForceAtomEnum fae = getHomingBeaconForceAtomEnum();
        for (int i = 0; i < getHomingBeaconBulletCount(); i++) {
            Mob mob = Util.getRandomFromCollection(mobs);
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 30, 25,
                    0, 200 + (i * 2), Util.getCurrentTime(), 1, 0,
                    new Position());
            chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                    true, mob.getObjectId(), HOMING_BEACON, forceAtomInfo, rect, 90, 30,
                    mob.getPosition(), 0, mob.getPosition(), 0));
        }

    }

    private void createTankMechRocketForceAtom() { // Tank Rockets are focused on 1 enemy
        Field field = chr.getField();
        SkillInfo si = SkillData.getSkillInfoById((chr.hasSkill(ADV_HOMING_BEACON) ? ADV_HOMING_BEACON : HOMING_BEACON));
        Rect rect = chr.getPosition().getRectAround(si.getRects().getFirst());
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        Mob mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
        if (mob == null) {
            return;
        }
        ForceAtomEnum fae = getHomingBeaconForceAtomEnum();
        for (int i = 0; i < getHomingBeaconBulletCount(); i++) {
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 30, 25,
                    0, 200, Util.getCurrentTime(), 1, 0,
                    new Position());
            chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                    true, mob.getObjectId(), HOMING_BEACON, forceAtomInfo, rect, 90, 30,
                    mob.getPosition(), 0, mob.getPosition(), 0));
        }
    }

    private ForceAtomEnum getHomingBeaconForceAtomEnum() {
        int skillId = getHomingBeaconSkill().getSkillId();
        if (skillId == ADV_HOMING_BEACON) {
            return MECH_MEGA_ROCKET_1;
        } else if (skillId == HOMING_BEACON_RESEARCH) {
            return MECH_MEGA_ROCKET_2;
        }
        return MECH_ROCKET;
    }

    private Skill getHomingBeaconSkill() {
        Skill skill = null;
        for (int skillId : homingBeacon) {
            if (chr.hasSkill(skillId)) {
                skill = chr.getSkill(skillId);
            }
        }

        return skill;
    }

    private int getHomingBeaconBulletCount() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int forceAtomCount = 0;
        for (int skillId : homingBeacon) {
            if (chr.hasSkill(skillId)) {
                Skill skill = chr.getSkill(skillId);
                SkillInfo si = SkillData.getSkillInfoById(skillId);
                int slv = skill.getCurrentLevel();
                forceAtomCount += si.getValue(bulletCount, slv);
            }
        }
        if (tsm.getOptByCTSAndSkill(BombTime, FULL_SPREAD) != null) {
            forceAtomCount += chr.hasSkill(FULL_SPREAD) ? SkillData.getSkillInfoById(FULL_SPREAD).getValue(x, chr.getSkill(FULL_SPREAD).getCurrentLevel()) : 0;
        }
        return forceAtomCount;
    }

    private void applySupportUnitDebuffOnMob(int skillId) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(SUPPORT_UNIT_HEX) || tsm.getOptByCTSAndSkill(IndieEmpty, skillId) == null) {
            return;
        }
        Skill skill = chr.getSkill(skillId);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        SkillInfo suhInfo = SkillData.getSkillInfoById(SUPPORT_UNIT_HEX);
        int slv = skill.getCurrentLevel();

        Option o = new Option();
        Field field = chr.getField();

        o.nOption = -suhInfo.getValue(w, chr.getSkill(SUPPORT_UNIT_HEX).getCurrentLevel()); // enhancement doesn't contain the debuff info
        o.rOption = skill.getSkillId();
        o.tOption = 6;
        for (Mob mob : field.getMobs()) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
            map.put(MobStat.PDR, o);
            map.put(MobStat.MDR, o.deepCopy());
            mts.addStatOptions(mob, map);
        }

        supportUnitTimer = chr.getTimer().addEvent(() -> applySupportUnitDebuffOnMob(skillId), si.getValue(x, slv), TimeUnit.SECONDS);
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
        TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        Summon summon;
        Field field = chr.getField();
        boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        switch (skillID) {
            case SECRET_ASSEMBLY:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case OPEN_PORTAL_GX9:
                int duration = si.getValue(time, slv);
                if (chr.hasSkill(ROBOT_MASTERY)) {
                    SkillInfo robotMastery = SkillData.getSkillInfoById(ROBOT_MASTERY);
                    int percent = robotMastery.getValue(x, chr.getSkillLevel(ROBOT_MASTERY));
                    duration *= 1 + (tms282 ? percent / 100D : percent / 100);
                }
                OpenGate openGate = new OpenGate(chr, chr.getPosition(), chr.getParty(), gateId, duration);
                if (gateId == 0) {
                    gateId = 1;
                } else if (gateId == 1) {
                    gateId = 0;
                }
                openGate.spawnOpenGate(field);
                break;
            case HOMING_BEACON: //4
            case ADV_HOMING_BEACON: // 4thJob upgrade +5 -> 9
                if (tsm.hasStat(Mechanic) && tsm.getOption(Mechanic).nOption <= 0) {
                    createHumanoidMechRocketForceAtom();
                } else if (tsm.hasStat(Mechanic) && tsm.getOption(Mechanic).nOption == 1) {
                    createTankMechRocketForceAtom();
                }
                break;
            case HEROS_WILL_MECH:
                tsm.removeAllDebuffs();
                break;
            case ROCK_N_SHOCK:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                if (chr.hasSkill(ROBOT_MASTERY)) {
                    SkillInfo robotMastery = SkillData.getSkillInfoById(ROBOT_MASTERY);
                    int percent = robotMastery.getValue(x, chr.getSkillLevel(ROBOT_MASTERY));
                    // getSummonTerm is milliseconds; setSummonTerm accepts seconds.
                    summon.setSummonTerm(tms282 ? (int) ((long) si.getValue(time, slv) * (100 + percent) / 100)
                            : (int) (summon.getSummonTerm() * (1 + percent / 100)));
                }
                field.spawnAddSummon(summon);
                List<Summon> rockNshockLifes = field.getSummons().stream().filter(s -> s.getSkillID() == ROCK_N_SHOCK && s.getOwnerId() == chr.getId()).collect(Collectors.toList());
                if (!tms282) {
                    field.spawnAddSummon(summon);
                }
                if (!tms282 || rockNshockLifes.size() >= 3) {
                    field.broadcast(UserPacket.teslaTriangle(rockNshockLifes, chr.getId()));
                }
                break;
            case GIANT_ROBOT_SG_88:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.Attack);
                summon.setMoveAction((byte) 1);
                summon.setSummonTerm(5);
                summon.setLeaveType(LeaveType.BEHIND);
                tsm.removeStatsBySkill(skillID);
                field.spawnAddSummon(summon);
                break;
            case HUMANOID_MECH:
                o1.nOption = si.getCurrentLevel();
                o1.rOption = skillID;
                tsm.sendStat(Mechanic, o1);

                o2.nOption = si.getValue(epad, slv);
                o2.rOption = skillID;
                newStats.put(PAD, o2);
                o3.nOption = si.getValue(emmp, slv);
                o3.rOption = skillID;
                newStats.put(EMMP, o3);
                o4.nOption = si.getValue(emhp, slv);
                o4.rOption = skillID;
                newStats.put(EMHP, o4);
                o5.nOption = si.getValue(indieSpeed, slv);
                o5.rOption = skillID;
                newStats.put(IndieSpeed, o5);

                tsm.sendStat(newStats);

                tsb.setNOption(MECH_VEHICLE);
                tsb.setROption(skillID + 100);
                tsm.sendStat(RideVehicle, tsb.getOption());
                break;
            case TANK_MECH:
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(Mechanic, o1);

                o2.nValue = si.getValue(cr, slv);
                o2.nReason = skillID;
                newStats.put(IndieCrR, o2);

                tsb.setNOption(MECH_VEHICLE);
                tsb.setROption(skillID + 100);
                newStats.put(RideVehicle, tsb.getOption());

                tsm.sendStat(newStats);
                break;
            case MECHANIC_RAGE:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Booster, o1);
                break;
            case PERFECT_ARMOR:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    tsm.sendStat(PowerGuard, o1);
                }
                break;
            case ROLL_OF_THE_DICE: {
                int random = new Random().nextInt(6) + 1;

                Effect eff = Effect.avatarOriented("Skill/" + (skillID / 10000) + ".img/skill/" + skillID + "/affected/" + random);
                chr.write(UserPacket.effect(eff));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), eff), chr);

                if (random < 2) {
                    return;
                }

                o1.nOption = random;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);

                tsm.throwDice(random);
                tsm.sendStat(Dice, o1);
                break;
            }
            case ROLL_OF_THE_DICE_DD: {
                int random = new Random().nextInt(6) + 1;
                int randomDD = new Random().nextInt(6) + 1;

                Effect eff = Effect.avatarOriented("Skill/" + (skillID / 10000) + ".img/skill/" + skillID + "/affected/" + random);
                Effect effSpecial = Effect.avatarOriented("Skill/" + (skillID / 10000) + ".img/skill/" + skillID + "/specialAffected/" + randomDD);
                chr.write(UserPacket.effect(eff));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), eff), chr);
                chr.write(UserPacket.effect(effSpecial));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), effSpecial), chr);

                if (random < 2 && randomDD < 2) {
                    return;
                }

                o1.nOption = (random * 10) + randomDD; // if rolled: 5 and 7, the DoubleDown nOption = 57
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);

                tsm.throwDice(random, randomDD);
                tsm.sendStat(Dice, o1);
                break;
            }
            case ENHANCED_SUPPORT_UNIT:
                o2.nReason = skillID;
                o2.nValue = si.getValue(z, slv);
                o2.tTerm = 80;
                tsm.sendStat(IndieDamR, o2);
                // Fallthrough intended
            case SUPPORT_UNIT_HEX:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                summon.setAttackActive(false);
                field.spawnSummon(summon);

                if (supportUnitTimer != null) {
                    supportUnitTimer.cancel(false);
                }
                applySupportUnitDebuffOnMob(skillID);
                break;
            case ROBO_LAUNCHER_RM7:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case BOTS_N_TOTS:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.Summon);
                summon.setAttackActive(false);
                field.spawnSummon(summon);
                break;
            case FOR_LIBERTY_MECH:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case FULL_SPREAD:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(BombTime, o1);
                break;
            case MULTIPURPOSE_BOT_MFL:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(CreateEventMeso, o1);

                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setAssistType(AssistType.MultiSkills);
                summon.setAttackActive(true);
                chr.getField().spawnSummon(summon);
                break;
            case MOBILE_MISSILE_BATTERY:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                field.spawnSummon(summon);
                createMicroMissileForceAtoms(skillID, slv);
                break;
            case FULL_METAL_BARRAGE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(NotDamaged, o1);
                o2.nOption = 100;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(IgnoreAllCounter, o2);
                tsm.sendStat(newStats);
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (supportUnitTimer != null) {
            supportUnitTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.MECHANIC_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MECHANIC_2.getJobId());
                sm.giveItem(1142243);
                sm.completeQuestNoRewards(23022);
                sm.completeQuestNoRewards(23025);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.MECHANIC_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MECHANIC_3.getJobId());
                sm.giveItem(1142244);
                sm.completeQuestNoRewards(23032);
                sm.completeQuestNoRewards(23035);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.MECHANIC_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MECHANIC_4.getJobId());
                sm.giveItem(1142245);
                sm.completeQuestNoRewards(23042);
                sm.completeQuestNoRewards(23045);
                sm.completeQuestNoRewards(23048);
                sm.completeQuestNoRewards(23051);
                sm.completeQuestNoRewards(23054);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
