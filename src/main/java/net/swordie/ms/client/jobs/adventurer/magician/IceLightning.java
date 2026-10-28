package net.swordie.ms.client.jobs.adventurer.magician;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SecondAtom;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.AffectedAreaSpeacial;
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
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class IceLightning extends Magician {

    public static final int MP_EATER = 2200000;
    public static final int CHILLING_STEP = 2201009;
    public static final int COLD_BEAM = 2201008;
    public static final int FREEZING_CRUSH = 2200011;
    public static final int FROST_CLUTCH = 2220015;

    public static final int MAGIC_BOOSTER = 2201010;
    public static final int MEDITATION = 2201001;

    public static final int ICE_STRIKE = 2211002;
    public static final int GLACIER_CHAIN = 2211010;
    public static final int THUNDER_SPHERE = 2211011;
    public static final int THUNDER_SPHERE_STAY = 2211015;
    public static final int TELEPORT_MASTERY = 2211007;
    public static final int TELEPORT_BOOST = 2211017;
    public static final int ELEMENTAL_DECREASE = 2211008;
    public static final int ELEMENTAL_ADAPTATION = 2211012;
    public static final int CHAIN_LIGHTNING = 2221006;
    public static final int FREEZING_BREATH = 2221011;
    public static final int BLIZZARD = 2221007;
    public static final int BLIZZARD_FA = 2220014;
    public static final int FROZEN_ORB = 2221012;
    public static final int INFINITY = 2221004;
    public static final int ELQUINES = 2221005;
    public static final int ARCANE_AIM = 2220010;
    public static final int HEROS_WILL = 2221008;
    public static final int LIGHTNING_ORB = 2221052;
    public static final int EPIC_ADVENTURE = 2221053;
    public static final int ABSOLUTE_ZERO_AURA = 2221054;

    // V Skills
    public static final int ICE_AGE = 400021002;
    public static final int ICE_AGE_TILE = 400020002;
    public static final int BOLT_BARRAGE = 400021030;
    public static final int BOLT_BARRAGE_TILE = 400021040;
    public static final int BOLT_BARRAGE_TILE_2 = 400021031;
    public static final int SPIRIT_OF_SNOW = 400021067;
    public static final int JUPITER_THUNDER = 400021094;
    public static final int JUPITER_THUNDER_2 = 400021112;

    // HEXA Skills
    public static final int HEXA_BLIZZARD = 2241003;
    public static final int HEXA_BLIZZARD_FA = 2241004;
    public static final int HEXA_CHAIN_LIGHTNING = 2241000;
    public static final int HEXA_CHAIN_LIGHTNING_TILE = 2241001;
    public static final int HEXA_FROZEN_ORB = 2241002;
    public static final int HEXA_LIGHTNING_ORB = 2241005;
    public static final int HEXA_LIGHTNING_ORB_2 = 2241010;
    public static final int CRYO_SHOCK = 2240006;
    public static final int HEXA_THUNDER_SPHERE = 2241007;
    public static final int HEXA_THUNDER_SPHERE_STAY = 2241008;
    public static final int HEXA_ELQUINES = 2241009;
    public static final int FROZEN_LIGHTING = 2241503;
    public static final int FROZEN_LIGHTING_BUFF = 2241504;
    public static final int PARABOLIC_BOLT = 2241505;
    public static final int PARABOLIC_BOLT_SA = 2241506;

    private int infinityStack = 0;
    private int ascentCount = 0;
    private ScheduledFuture<?> infinityTimer;
    private Tms282IceAura iceAura;

    public IceLightning(Char chr) {
        super(chr);
        if (tms282()) iceAura = chr.getJobHandler() instanceof IceLightning old && old.iceAura != null
                ? old.iceAura : new Tms282IceAura(chr);
    }

    private static boolean tms282() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isIceLightning(id);
    }

    private void infinity() {
        if (!chr.hasSkill(getInfinitySkill())) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Skill skill = chr.getSkill(getInfinitySkill());
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        infinityStack++;
        if (tsm.hasStat(Infinity)) {
            o1.nValue = infinityStack * si.getValue(damage, slv);
            o1.nReason = getInfinitySkill() + 100; //To make the buff icon hidden
            tsm.sendStat(IndieMADR, o1);
            if (chr.getHP() > 0) {
                chr.heal((int) (chr.getMaxHP() / ((double) 100 / si.getValue(y, slv))));
                chr.healMP((int) (chr.getMaxMP() / ((double) 100 / si.getValue(y, slv))));
            }
            infinityTimer = chr.getTimer().addEvent(this::infinity, 4, TimeUnit.SECONDS);
        } else {
            tsm.removeStatsBySkill(getInfinitySkill() + 100);
            infinityStack = 0;
        }
    }

    private int getInfinitySkill() {
        int skill = 0;
        if (chr.hasSkill(INFINITY)) {
            skill = INFINITY;
        }
        return skill;
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        mpEaterEffect(mob, skillID);
        applyFreezingCrushOnMob(mob, skillID);
        switch (skillID) {
            case FREEZING_BREATH:
                o2.nOption = 1;
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                o2.cOption = chr.getId();
                map.put(MobStat.Freeze, o2);
                o3.nOption = si.getValue(x, slv);
                o3.rOption = skillID;
                o3.tOption = 5;
                map.put(MobStat.PDR, o3);
                o4.nOption = si.getValue(y, slv);
                o4.rOption = skillID;
                o4.tOption = 5;
                map.put(MobStat.MDR, o4);
                mts.addStatOptions(mob, map);
                break;
            case COLD_BEAM:
            case ICE_STRIKE:
            case GLACIER_CHAIN:
                if (!mts.hasCurrentMobStat(MobStat.Freeze)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 5;
                        o1.rOption = skillID;
                        o1.tOption = 3;
                        o1.cOption = chr.getId();
                        mts.addStatOptions(mob, MobStat.Freeze, o1);
                    }
                }
                break;
            case TELEPORT_MASTERY:
            case CHAIN_LIGHTNING:
            case HEXA_CHAIN_LIGHTNING:
                if (!mts.hasCurrentMobStat(MobStat.Stun)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        o1.cOption = chr.getId();
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
        }
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
            incrementArcaneAim();
            handleIceAge(attackInfo, skillID, slv);
        }
        Option o1 = new Option();
        switch (skillID) {
            case FREEZING_BREATH:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = 5;
                tsm.sendStat(NotDamaged, o1);
                break;
            case FROZEN_ORB:
            case HEXA_FROZEN_ORB:
                if (!chr.hasSkillOnCooldown(FROZEN_ORB)) {
                    chr.addSkillCooldown(FROZEN_ORB, 5000);
                }
                break;
            case HEXA_CHAIN_LIGHTNING:
                setHexaChainLightning(attackInfo);
                break;
            case FROZEN_LIGHTING:
                if (!tsm.hasStat(SixthFrozenLightning)) {
                    o1.nOption = 1;
                    o1.rOption = FROZEN_LIGHTING_BUFF;
                    o1.tOption = 30;
                    tsm.sendStat(SixthFrozenLightning, o1);
                } else {
                    // todo?
                }
                break;
        }
    }

    private void setHexaChainLightning(AttackInfo attackInfo) {
        if (!chr.hasSkill(HEXA_CHAIN_LIGHTNING)) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(HEXA_CHAIN_LIGHTNING_TILE);
        int slv = attackInfo.slv;
        int prop = si.getValue(SkillStat.prop, slv);
        if (Util.succeedProp(prop) && !chr.hasSkillOnCooldown(HEXA_CHAIN_LIGHTNING_TILE)) {
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                AffectedArea aa = AffectedArea.getPassiveAA(chr, HEXA_CHAIN_LIGHTNING_TILE, slv);
                aa.setPosition(mob.getPosition());
                aa.setRect(aa.getRectAround(si.getFirstRect()));
                aa.setDelay((short) 3);
                chr.getField().spawnAffectedArea(aa);
            }
            chr.addSkillCooldown(HEXA_CHAIN_LIGHTNING_TILE, 7000);
        }
    }

    private void incrementArcaneAim() {
        Skill skill = chr.getSkill(ARCANE_AIM);
        if (skill == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int arcaneAimProp = si.getValue(prop, slv);
        if (!Util.succeedProp(arcaneAimProp)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        int amount = 1;
        if (tsm.hasStat(ArcaneAim)) {
            amount = tsm.getOption(ArcaneAim).nOption;
            if (amount < si.getValue(y, slv)) {
                amount++;
            }
        }
        o1.nOption = amount;
        o1.rOption = skill.getSkillId();
        o1.tOption = 5; // No Time Variable
        tsm.sendStat(ArcaneAim, o1);
    }

    private void applyFreezingCrushOnMob(Mob mob, int skillID) {
        if (!chr.hasSkill(FREEZING_CRUSH)) {
            return;
        }
        int counter = 1;
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        MobTemporaryStat mts = mob.getTemporaryStat();
        Option o1 = new Option();
        Option o2 = new Option();
        if (si != null && (si.getElemAttr().contains("i"))) { // Ice Skills
            if (tms282()) {
                int maximum = 5;
                int increment = skillID == SPIRIT_OF_SNOW ? 3 : 1;
                Option fury = chr.getTemporaryStatManager().getOption(GlacialFury);
                if (fury.nOption == 1 && fury.rOption == ABSOLUTE_ZERO_AURA) {
                    SkillInfo furyInfo = SkillData.getSkillInfoById(ABSOLUTE_ZERO_AURA);
                    int furyLevel = chr.getSkillLevel(ABSOLUTE_ZERO_AURA);
                    maximum = furyInfo.getValue(x, furyLevel);
                    increment = furyInfo.getValue(y, furyLevel);
                }
                int oldStacks = mts.hasCurrentMobStat(MobStat.ArcMage2Stack)
                        ? mts.getCurrentOptionsByMobStat(MobStat.ArcMage2Stack).nOption : 0;
                counter = Math.min(maximum, oldStacks + increment);
            } else if (mts.hasCurrentMobStat(MobStat.ArcMage2Stack)) {
                counter = mts.getCurrentOptionsByMobStat(MobStat.ArcMage2Stack).nOption;
                if (counter < 5) {
                    counter += skillID == SPIRIT_OF_SNOW ? 3 : 1;
                    counter = Math.min(counter, 5);
                }
            }
        } else {
            if (mts.hasCurrentMobStat(MobStat.ArcMage2Stack)) {
                counter = mts.getCurrentOptionsByMobStat(MobStat.ArcMage2Stack).nOption;
            }
            if (counter >= 5 && chr.hasSkill(CRYO_SHOCK)) {
                mts.removeMobStat(mob, MobStat.Speed);
                mts.removeMobStat(mob, MobStat.ArcMage2Stack);
                chr.write(UserLocal.userBonusAttackRequest(CRYO_SHOCK, mob.getObjectId()));
                mob.getField().broadcast(MobPool.specialEffectBySkill(mob, CRYO_SHOCK, chr.getId(), 0));
                mob.getField().broadcast(MobPool.specialEffectBySkill(mob, skillID, chr.getId(), 0));
                return;
            }
            counter -= 1;
            if (counter <= 0) {
                mts.removeMobStat(mob, MobStat.Speed);
                mts.removeMobStat(mob, MobStat.ArcMage2Stack);
                mob.getField().broadcast(MobPool.specialEffectBySkill(mob, skillID, chr.getId(), 0));
                return;
            }
        }
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        o1.nOption = counter;
        o1.rOption = COLD_BEAM;
        o1.tOption = 20;
        map.put(MobStat.ArcMage2Stack, o1);
        o2.nOption = -15 * (tms282() ? Math.min(counter, 5) : counter);
        o2.rOption = COLD_BEAM;
        o2.tOption = 20;
        o2.mOption = counter;
        map.put(MobStat.Speed, o2);
        mts.addStatOptions(mob, map);
        mob.getField().broadcast(MobPool.specialEffectBySkill(mob, skillID, chr.getId(), 0));
    }

    private void handleIceAge(AttackInfo attackInfo, int skillID, int slv) {
        if (skillID == ICE_AGE) {
            SkillInfo si = SkillData.getSkillInfoById(ICE_AGE_TILE);
            AffectedArea aa = AffectedArea.getAffectedArea(chr, skillID, slv);
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                final Position pos = mob.getPosition();
                int x = pos.getX();
                Foothold fh = chr.getField().findFootHoldBelow(pos);
                if (fh != null) {
                    aa.setSkillID(ICE_AGE_TILE);
                    aa.setMobOrigin((byte) 0);
                    aa.setPosition(new Position(x, fh.getYFromX(x) + 50));
                    aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                    aa.setDelay((short) 4);
                    aa.setDuration(15000);
                    chr.getField().spawnAffectedArea(aa);
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
        Field field = chr.getField();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        switch (skillID) {
            case TELEPORT:
                if (chr.hasSkill(IceLightning.CHILLING_STEP)) {
                    createChillStepAA();
                }
                break;
            case MEDITATION:
                o1.nValue = si.getValue(indieMad, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieMAD, o1);
                break;
            case ELEMENTAL_DECREASE:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ElementalReset, o1);
                break;
            case ELEMENTAL_ADAPTATION:
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(AntiMagicShell, o1);
                break;
            case TELEPORT_MASTERY:
                if (tsm.hasStat(TeleportMasteryOn)) {
                    tsm.removeStat(TeleportMasteryOn);
                    tsm.removeStat(TeleportMasteryRange);
                } else {
                    o1.nOption = si.getValue(y, slv);
                    o1.rOption = skillID;
                    tsm.sendStat(TeleportMasteryOn, o1);
                }
                break;
            case TELEPORT_BOOST:
                if (tsm.hasStat(TeleportMasteryRange)) {
                    tsm.removeStat(TeleportMasteryRange);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(TeleportMasteryRange, o1);
                }
                break;
            case INFINITY:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Infinity, o1);
                o2.nOption = si.getValue(prop, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Stance, o2);
                tsm.sendStat(newStats);

                this.infinityStack = 0;
                if (this.infinityTimer != null) {
                    this.infinityTimer.cancel(false);
                }
                infinity();
                break;
            case ELQUINES:
            case HEXA_ELQUINES:
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Walk);
                field.spawnSummon(summon);
                field.broadcast(Summoned.attackActive(summon));
                break;
            case THUNDER_SPHERE:
            case HEXA_THUNDER_SPHERE:
                tsm.removeStatsBySkill(chr.hasSkill(HEXA_THUNDER_SPHERE) ? HEXA_THUNDER_SPHERE_STAY : THUNDER_SPHERE_STAY);
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                field = c.getChr().getField();
                summon.setFlyMob(true);
                field.spawnSummon(summon);
                field.broadcast(Summoned.attackActive(summon));
                break;
            case THUNDER_SPHERE_STAY:
            case HEXA_THUNDER_SPHERE_STAY:
                tsm.removeStatsBySkill(chr.hasSkill(HEXA_THUNDER_SPHERE) ? HEXA_THUNDER_SPHERE : THUNDER_SPHERE);
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                field = c.getChr().getField();
                summon.setFlyMob(true);
                field.spawnSummon(summon);
                field.broadcast(Summoned.attackActive(summon));
                break;
            case CHILLING_STEP:
                if (tms282()) { iceAura.toggle(); break; }
                if (tsm.hasStat(ChillingStep)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.rOption = skillID;
                    tsm.sendStat(ChillingStep, o1);
                }
                break;
            case EPIC_ADVENTURE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case ABSOLUTE_ZERO_AURA:
                if (tms282()) {
                    // 282 Glacial Fury: native5A18B10 requires n=1, then looks up x=max/y=increment by reason.
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    tsm.sendStat(GlacialFury, o1);
                    break;
                }
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.xOption = chr.getId();
                    o1.yOption = 0; // isPartyMember
                    newStats.put(IceAura, o1);
                    o2.nValue = si.getValue(x, slv);
                    o2.nReason = skillID;
                    newStats.put(IndieStance, o2);
                    o3.nOption = si.getValue(y, slv);
                    o3.rOption = skillID;
                    newStats.put(DamageReduce, o3);
                    o4.nValue = si.getValue(v, slv);
                    o4.nReason = skillID;
                    newStats.put(IndieAsrR, o4);
                    newStats.put(IndieTerR, o4.deepCopy());
                    tsm.sendStat(newStats);
                }
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case SPIRIT_OF_SNOW:
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Stop);
                chr.getField().spawnSummon(summon);
                break;
            case BOLT_BARRAGE:
                Position chrPos = inPacket.decodePosition();
                boolean isLeft = inPacket.decodeByte() != 0;
                Position[] positions = new Position[inPacket.decodeInt()]; // amount of Positions
                for (int i = 0; i < positions.length; i++) {
                    positions[i] = inPacket.decodePositionInt();
                }
                setBoltBarrage(si, skillID, slv, chrPos, isLeft, positions);
                chr.write(UserPacket.effect(Effect.skillUse(skillID, chr.getLevel(), (byte) slv)));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(skillID, chr.getLevel(), (byte) slv)), chr);
                break;
            case PARABOLIC_BOLT_SA: {
                slv = chr.getSkillLevel(PARABOLIC_BOLT);
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    createParabolicBolts(si, slv);
                    break;
                }
                int bulletCount = si.getValue(SkillStat.bulletCount, slv);
                List<SecondAtom> secondAtoms = new LinkedList<>();
                final var sai = si.getSecondAtomInfos().get(0);
                final var pos = chr.getPosition();
                int key = 0;
                final long start = System.currentTimeMillis();
                final var mobs = chr.getField().getMobsFiltered();
                for (int i = 0; i < bulletCount; i++) {
                    var mob = Util.getRandomFromCollection(mobs);
                    SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, key,
                            si.getSkillId(), pos, start);
                    fa.setSpecialAtom(true);
                    secondAtoms.add(fa);
                    key++;
                }
                chr.createSecondAtom(secondAtoms);
                break;
            }
        }
    }

    private void createParabolicBolts(SkillInfo info, int level) {
        if (level <= 0 || level > info.getMaxLevel() || chr.getField() == null || info.getFirstRect() == null) return;
        var atom = info.getSecondAtomInfos().get(0);
        if (atom == null) return;
        var rect = chr.getPosition().getRectAround(info.getFirstRect());
        // Original282: eight pursuing bolts per living boss in range, up to four bosses (v).
        // Deterministic server target ordering; do not replace a missing target with object ID zero.
        var targets = chr.getField().getMobsInRect(rect).stream().filter(Mob::isBoss)
                .sorted(Comparator.comparingInt(Mob::getObjectId)).limit(info.getValue(v, level)).toList();
        List<SecondAtom> bolts = new ArrayList<>();
        long now = System.currentTimeMillis();
        int key = 0;
        for (Mob target : targets) {
            for (int i = 0; i < info.getValue(bulletCount, level); i++) {
                var bolt = new SecondAtom(atom, chr.getNewSecondAtomKey(), chr.getId(), target.getObjectId(),
                        key++, info.getSkillId(), chr.getPosition(), now);
                bolt.setSpecialAtom(true);
                bolts.add(bolt);
            }
        }
        if (!bolts.isEmpty()) chr.createSecondAtom(bolts);
    }

    private void setBoltBarrage(SkillInfo si, int skillID, int slv, Position chrPos, boolean isLeft, Position[] positions) {
        AffectedArea aa = AffectedArea.getPassiveAA(chr, BOLT_BARRAGE_TILE_2, slv);
        aa.setPosition(chrPos);
        aa.setFlip(isLeft);
        aa.setRect(new Rect(0, 0, 0, 0));
        aa.setDelay((short) 0);
        aa.setDuration(3090);
        Set<AffectedAreaSpeacial> areaSpeacialSet = new HashSet<>();
        SkillInfo boltBarrageTile = SkillData.getSkillInfoById(BOLT_BARRAGE_TILE_2);
        for (Position position : positions) {
            AffectedAreaSpeacial affectedAreaSpeacial = new AffectedAreaSpeacial();
            affectedAreaSpeacial.setOriginalSkillID(skillID);
            affectedAreaSpeacial.setValue(1);
            affectedAreaSpeacial.setPosition(position);
            affectedAreaSpeacial.setRect(affectedAreaSpeacial.getRectAround(boltBarrageTile.getFirstRect()));
            affectedAreaSpeacial.setY(si.getValue(y, slv));
            affectedAreaSpeacial.setU(si.getValue(u, slv));
            areaSpeacialSet.add(affectedAreaSpeacial);
        }
        aa.setAffectedAreaSpeacials(areaSpeacialSet);
        chr.getField().spawnAffectedArea(aa);
    }

    public void handleRemoveJupiterThunder(int shocksRemaining) {
        var finalCdR = shocksRemaining * (3.4 * 1000); // giá trị t trong JUPITER THUNDER
        chr.reduceSkillCoolTime(JUPITER_THUNDER, (int) finalCdR);
    }

    private void mpEaterEffect(Mob mob, int skillID) {
        Skill skill = chr.getSkill(MP_EATER);
        if (skill == null || skillID == 0) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int mpStolen = si.getValue(x, slv);
        if (mob.getMp() >= 0) {
            long mobMaxMP = mob.getMaxMp();
            int mpAbsorbed = (int) (mobMaxMP * ((double) mpStolen / 100));
            mob.healMP(-(Math.min(mpAbsorbed, 500)));
            chr.healMP(Math.min(mpAbsorbed, 500));
            chr.write(UserPacket.effect(Effect.skillUse(MP_EATER, chr.getLevel(), (byte) slv)));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(MP_EATER, chr.getLevel(), (byte) slv)), chr);
        }
    }

    public void doIceAura() {
        if (tms282()) return; // 282 aura work is server-owned in update(), never the unvalidated legacy request.
        if (chr.hasSkillOnCooldown(-ABSOLUTE_ZERO_AURA) || !chr.getTemporaryStatManager().hasStat(IceAura)) {
            return;
        }

        var si = SkillData.getSkillInfoById(ABSOLUTE_ZERO_AURA);
        var slv = chr.getSkillLevel(ABSOLUTE_ZERO_AURA);
        Rect rect = chr.getRectAround(si.getFirstRect());
        for (Mob mob : chr.getField().getMobsInRect(rect).stream().limit(15).collect(Collectors.toList())) {
            applyIceAuraOnMob(mob, si, slv);
        }
        chr.addSkillCooldown(-ABSOLUTE_ZERO_AURA, 3000);
    }

    public void applyIceAuraOnMob(Mob mob, SkillInfo si, int slv) {
        Option o = new Option();
        var mts = mob.getTemporaryStat();

        if (!mob.isBoss()) {
            o.nOption = 1;
            o.rOption = ICE_STRIKE; // for Blue effect
            o.tOption = si.getValue(time, slv);
            mts.addStatOptions(mob, MobStat.Freeze, o);
        }
    }

    private void createChillStepAA() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo chillingStepInfo = SkillData.getSkillInfoById(CHILLING_STEP);
        int slv = chr.getSkill(CHILLING_STEP).getCurrentLevel();
        if (tsm.hasStat(ChillingStep) && Util.succeedProp(chillingStepInfo.getValue(prop, slv))) {
            for (int i = 0; i < 168; i += 56) {
                AffectedArea aa = AffectedArea.getPassiveAA(chr, CHILLING_STEP, slv);
                aa.setMobOrigin((byte) 0);
                int x = chr.isLeft() ? chr.getPosition().getX() - i : chr.getPosition().getX() + i;
                int y = chr.getPosition().getY();
                aa.setPosition(new Position(x, y));
                if (aa.getPosition() != null && chr.getField().findFootHoldBelow(aa.getPosition()) != null) {
                    aa.setRect(aa.getPosition().getRectAround(chillingStepInfo.getRects().get(0)));
                    aa.setCurFoothold((short) chr.getField().findFootHoldBelow(aa.getPosition()).getId());
                    aa.setDelay((short) 4);
                    aa.setSkillID(CHILLING_STEP);
                    aa.setRemoveSkill(false);
                    chr.getField().spawnAffectedArea(aa);
                } else {
                    chr.chatMessage("請換個位置再使用這個技能。");
                }
            }
        }
    }

    @Override
    public void handleMobDebuffSkill(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(ELEMENTAL_ADAPTATION) && tsm.getOptByCTSAndSkill(AntiMagicShell, ELEMENTAL_ADAPTATION) != null) {
            if (tsm.getOption(AntiMagicShell).bOption == 0) {
                Skill skill = chr.getSkill(ELEMENTAL_ADAPTATION);
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                int slv = skill.getCurrentLevel();

                tsm.removeStatsBySkill(skill.getSkillId());
                tsm.removeAllDebuffs();

                Option o = new Option();
                o.nOption = 1;
                o.rOption = skill.getSkillId();
                o.tOption = si.getValue(time, slv);
                o.bOption = 1;
                tsm.sendStat(AntiMagicShell, o);
            } else {
                tsm.removeAllDebuffs();
            }
        }
        super.handleMobDebuffSkill(chr);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (tms282() && iceAura != null) iceAura.stop();
        if (infinityTimer != null) {
            infinityTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void update(long now) {
        super.update(now);
        if (tms282() && iceAura != null) iceAura.update(now);
    }

    public void handleCancelKeyDownSkill(Char chr, int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (skillID) {
            case FREEZING_BREATH:
                tsm.removeStatsBySkill(skillID);
                break;
            default:
                super.handleCancelKeyDownSkill(chr, skillID);
        }
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_BLIZZARD -> {
                int skillID = BLIZZARD;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_LIGHTNING_ORB_2 -> {
                int skillID = LIGHTNING_ORB;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
