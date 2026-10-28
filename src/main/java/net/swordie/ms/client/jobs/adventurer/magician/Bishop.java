package net.swordie.ms.client.jobs.adventurer.magician;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SecondAtom;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.TownPortal;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.social.Party.Party;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.FieldData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Bishop extends Magician {

    public static final int MP_EATER = 2300000;
    public static final int HEAL = 2301002;
    public static final int MAGIC_BOOSTER = 2301008;
    public static final int BLESSED_ENSEMBLE = 2300009;
    public static final int BLESS = 2301004;

    public static final int DISPEL = 2311001;
    public static final int MYSTIC_DOOR = 2311002;
    public static final int HOLY_SYMBOL = 2311003;
    public static final int SHINING_RAY = 2311004;
    public static final int HOLY_MAGIC_SHELL = 2311009;
    public static final int HOLY_MAGIC_SHELL_COOLTIME = 2310013;
    public static final int TELEPORT_MASTERY = 2311007;
    public static final int ANGELIC_WRATH = 2301010;
    public static final int HOLY_FOUNTAIN = 2311011;
    public static final int DIVINE_PROTECTION = 2311012;
    public static final int FOUNTAIN_OF_VENGEANCE = 2311014;
    public static final int TRIUMPH_FEATHER = 2311015;
    public static final int TRIUMPH_FEATHER_ATOM = 2311017;
    public static final int TELEPORT_BOOST = 2311016;

    public static final int HOLY_WATER = 2321015;
    public static final int BLOOD_OF_THE_DIVINE = 2321016;
    public static final int ADV_BLESSING = 2321005;
    public static final int BAHAMUT = 2321003;
    public static final int INFINITY = 2321004;
    public static final int BLESSED_HARMONY = 2320013;
    public static final int GENESIS = 2321008;
    public static final int BIG_BANG = 2321001;
    public static final int ARCANE_AIM = 2320011;
    public static final int ANGEL_RAY = 2321007;
    public static final int RESURRECTION = 2321006;
    public static final int HEROS_WILL = 2321009;
    public static final int EPIC_ADVENTURE = 2321053;
    public static final int RIGHTEOUSLY_INDIGNANT = 2321054;
    public static final int HEAVENS_DOOR = 2321052;

    // V Skills
    public static final int BENEDICTION = 400021003;
    public static final int ANGEL_OF_BALANCE_AVENGE = 400021033;
    public static final int ANGEL_OF_BALANCE_BENEVOLENCE = 400021032;
    public static final int PEACEMAKER_TRAVEL = 400021070;
    public static final int PEACEMAKER_EXPLOSION = 400021077;
    public static final int DIVINE_PUNIHSMENT = 400021086;

    // HEXA Skills
    public static final int HOLY_ADVENT = 2341500;
    public static final int HOLY_ADVENT_M = 2341501;
    public static final int HOLY_ADVENT_L = 2341502;
    public static final int HOLY_ADVENT_R = 2341503;
    public static final int HOLY_ADVENT_AFTER = 2341504;
    public static final int COMMANDMENT_OF_HEAVEN = 2341507;
    public static final int HEXA_ANGEL_RAY = 2341000;
    public static final int HEXA_ANGEL_RAY_BONUS = 2341001;
    public static final int HEXA_BIG_BANG = 2341002;
    public static final int HEXA_BIG_BANG_EX = 2341003;
    public static final int HEXA_TRIUMPH_FEATHER = 2341004;
    public static final int HEXA_TRIUMPH_FEATHER_ATOM = 2341005;
    public static final int HEXA_FOUNTAIN_OF_VENGEANCE = 2341006;
    public static final int HEXA_ANGELIC_WRATH = 2341007;
    public static final int HEXA_ANGELIC_WRATH_SUMMON = 2341008;
    public static final int HEXA_GENESIS = 2341009;
    public static final int HEXA_GENESIS_FA = 2341010;
    public static final int HEXA_HEAVENS_DOOR = 2341011;
    public static final int HEXA_BAHAMUT = 2341013;

    // HEXA Boosts
    public static final int HEXA_BENEDICTION = 500061002;

    private int infinityStack = 0;
    private ScheduledFuture<?> infinityTimer;
    public int hmshits = 0;
    public int angelRayHits = 0;
    public int hexaAngelRayHits = 0;

    public Bishop(Char chr) {
        super(chr);
    }

    public static int getHolyMagicShellMaxGuards(Char chr) {
        int num = 9;
        if (chr.hasSkill(2320043)) { //Extra 2 Guards  Hyper Skill
            num = 11;
        }
        return num;
    }

    public static void reviveByHeavensDoor(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        chr.healHPMP();
        int skillID = HEAVENS_DOOR;
        if (tsm.hasStatBySkillId(HEXA_HEAVENS_DOOR)) {
            skillID = HEXA_HEAVENS_DOOR;
            tsm.removeStatsBySkill(HEXA_HEAVENS_DOOR);
        } else {
            tsm.removeStatsBySkill(skillID);
        }
        chr.chatMessage("你被 " + StringData.getSkillStringById(skillID).getName() + " 技能復活。");
        var lotusFlower = chr.getField().getSummonBySkillId(chr, LOTUS_FLOWER);
        if (lotusFlower != null) {
            chr.getField().removeSummon(LOTUS_FLOWER, chr.getId());
            // If you receive a Heaven's Door buff while the lotus is summoned, the lotus will disappear.
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isAdventurerMage(id);
    }

    private void changeBlessedCount() {
        if (getBlessedSkill() == null) {
            return;
        }
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = getBlessedSkill();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        Option o1 = new Option();
        Option o2 = new Option();
        o1.nOption = 6; // Skill-log
        o1.rOption = skill.getSkillId();
        newStats.put(BlessEnsenble, o1);
        o2.nValue = si.getValue(x, slv) * 6; // Skill-log
        o2.nReason = skill.getSkillId();
        newStats.put(IndieDamR, o2);
        tsm.sendStat(newStats);
    }

    private Skill getBlessedSkill() {
        Skill skill = null;
        if (chr.hasSkill(BLESSED_ENSEMBLE)) {
            skill = chr.getSkill(BLESSED_ENSEMBLE);
        }
        if (chr.hasSkill(BLESSED_HARMONY)) {
            skill = chr.getSkill(BLESSED_HARMONY);
        }
        return skill;
    }

    private int changeBishopHealingBuffs(int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(skillID);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int rate = 0;
        int maxHP = chr.getMaxHP();
        int healrate = 0;
        switch (skillID) {
            case HEAL:
                rate = si.getValue(hp, slv);
                healrate = (int) (maxHP / ((double) 100 / rate));
                break;
            case HOLY_MAGIC_SHELL:
                rate = si.getValue(z, slv);
                healrate = (int) (maxHP / ((double) 100 / rate));
                break;
            case ANGEL_RAY:
            case HEXA_ANGEL_RAY:
                rate = si.getValue(hp, slv);
                healrate = (int) (maxHP / ((double) 100 / rate));
                break;
        }
        if (tsm.hasStat(VengeanceOfAngel)) {
            SkillInfo hsi = SkillData.getSkillInfoById(RIGHTEOUSLY_INDIGNANT);
            healrate = (int) (healrate / ((double) 100 / (hsi.getValue(hp, 1))));
        }
        return healrate;
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
        mpEaterEffect(mob, skillID);
        switch (skillID) {
            case SHINING_RAY:
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
            case BIG_BANG:
                o1.nOption = -si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.cOption = 2;
                map.put(MobStat.PDR, o1);
                map.put(MobStat.MDR, o1.deepCopy());
                mts.addStatOptions(mob, map);
                break;
            case BAHAMUT:
            case HEXA_BAHAMUT:
            case ANGEL_OF_BALANCE_AVENGE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (skillID == ANGEL_OF_BALANCE_AVENGE) {
                        si = SkillData.getSkillInfoById(ANGEL_OF_BALANCE_BENEVOLENCE);
                        slv = chr.getSkillLevel(ANGEL_OF_BALANCE_BENEVOLENCE);
                    }
                    o1.nOption = 25;
                    o1.rOption = skillID;
                    o1.tOption = 10;
                    mts.addStatOptions(mob, MobStat.BahamutLightElemAddDam, o1);
                }
                break;
            case TELEPORT_MASTERY:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    mts.addStatOptions(mob, MobStat.BahamutLightElemAddDam, o1);
                }
                break;
            case HEAL:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.TotalDamParty, o1);
                }
                break;
            case ANGELIC_WRATH:
            case HEXA_ANGELIC_WRATH_SUMMON:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.IndiePDR, o1);
                }
                break;
            case HEXA_ANGELIC_WRATH:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.IndiePDR, o1);
                    Field field = c.getChr().getField();
                    Summon summon = Summon.getSummonByAndSetStat(c.getChr(), HEXA_ANGELIC_WRATH_SUMMON, slv);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setAssistType(AssistType.ExplosionAttack);
                    summon.setPosition(mob.getPosition());
                    summon.setCurFoothold(field.findFootHoldBelow(summon.getPosition()) != null
                            ? (short) field.findFootHoldBelow(summon.getPosition()).getId() : 0);
                    field.spawnAddSummon(summon);
                    field.broadcast(Summoned.attackActive(summon));
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
            handleTriumphFeather(attackInfo, now);
        }
        Option o = new Option();
        switch (skillID) {
            case HEAVENS_DOOR:
            case HEXA_HEAVENS_DOOR:
                Party party = chr.getParty();
                if (party != null) {
                    for (Char partyChr : party.getPartyMembersInSameFieldWithChr(chr)) {
                        if (partyChr == null) {
                            continue;
                        }
                        TemporaryStatManager partyTSM = partyChr.getTemporaryStatManager();
                        o.nOption = 1;
                        o.rOption = skillID;
                        partyTSM.sendStat(ReviveOnce, o);
                        if (partyChr != chr) {
                            chr.getField().broadcast(UserRemote.effect(partyChr.getId(), Effect.skillAffected(skillID, slv, 0)), partyChr);
                            partyChr.write(UserPacket.effect(Effect.skillAffected(skillID, slv, 0)));
                        }
                    }
                } else {
                    o.nOption = 1;
                    o.rOption = skillID;
                    tsm.sendStat(ReviveOnce, o);
                }
                break;
            case ANGEL_RAY:
            case HEXA_ANGEL_RAY:
                chr.heal(changeBishopHealingBuffs(ANGEL_RAY));
                angelRayHits += attackInfo.mobAttackInfo.size();
                hexaAngelRayHits += attackInfo.mobAttackInfo.size();
                if (chr.hasSkill(HOLY_WATER) && angelRayHits >= 7) {
                    int value = tsm.hasStat(HolyWater) ? (int) (tsm.getTotalNOptionOfStat(HolyWater) + 1) : 1;
                    o.nOption = Math.min(value, 5);
                    o.rOption = HOLY_WATER;
                    tsm.sendStat(HolyWater, o);
                    angelRayHits = 0;
                }
                int value = tsm.hasStat(SixthAngelsRay) ? (int) (tsm.getTotalNOptionOfStat(SixthAngelsRay) + 1) : 1;
                if (value >= 12 && skillID == HEXA_ANGEL_RAY) {
                    List<Integer> mobList = new ArrayList<>();
                    for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                        Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                        if (mob == null || mob.getHp() <= 0) {
                            continue;
                        }
                        mobList.add(mob.getObjectId());
                        mob.getField().broadcast(MobPool.specialEffectBySkill(mob, HEXA_ANGEL_RAY_BONUS, chr.getId(), 0));
                    }
                    chr.write(UserLocal.userBonusAttackRequest(HEXA_ANGEL_RAY_BONUS, mobList));
                    value = 0;
                }
                o.nOption = Math.min(value, 12);
                o.rOption = HEXA_ANGEL_RAY;
                tsm.sendStat(SixthAngelsRay, o);
                break;
            case GENESIS:
            case HEXA_GENESIS:
                o.nOption = 1;
                o.rOption = chr.hasSkill(HEXA_BIG_BANG) ? HEXA_BIG_BANG : BIG_BANG;
                o.tOption = si.getValue(cooltime, slv);
                tsm.sendStat(KeyDownTimeIgnore, o);
                break;
            case HOLY_ADVENT_AFTER:
                if (!tsm.hasStat(HolyAdvent)) {
                    o.nOption = 1;
                    o.rOption = HOLY_ADVENT;
                    o.tOption = 60;
                    tsm.sendStat(HolyAdvent, o);
                    setHolyAdvent();
                    chr.write(UserLocal.holyAdventResult());
                }
                break;
        }
    }

    private void setHolyAdvent() {
        Field field = chr.getField();
        int slv = chr.getSkillLevel(HOLY_ADVENT);

        Position mid = chr.getPosition();
        Position left = new Position(mid.getX() - 400, mid.getY());
        Position right = new Position(mid.getX() + 400, mid.getY());

        Summon m = Summon.getSummonByAndSetStat(chr, HOLY_ADVENT_M, slv);
        m.setMoveAbility(MoveAbility.Stop);
        m.setFlyMob(true);
        m.setPosition(mid);
        m.setCurFoothold(field.findFootHoldBelow(m.getPosition()) != null
                ? (short) field.findFootHoldBelow(m.getPosition()).getId() : 0);
        field.spawnSummon(m);
        field.broadcast(Summoned.attackActive(m));

        Summon l = Summon.getSummonByAndSetStat(chr, HOLY_ADVENT_L, slv);
        l.setMoveAbility(MoveAbility.Stop);
        l.setFlyMob(true);
        l.setPosition(left);
        l.setCurFoothold(field.findFootHoldBelow(l.getPosition()) != null
                ? (short) field.findFootHoldBelow(l.getPosition()).getId() : 0);
        field.spawnSummon(l);
        field.broadcast(Summoned.attackActive(l));

        Summon r = Summon.getSummonByAndSetStat(chr, HOLY_ADVENT_R, slv);
        r.setMoveAbility(MoveAbility.Stop);
        r.setFlyMob(true);
        r.setPosition(right);
        r.setCurFoothold(field.findFootHoldBelow(r.getPosition()) != null
                ? (short) field.findFootHoldBelow(r.getPosition()).getId() : 0);
        field.spawnSummon(r);
        field.broadcast(Summoned.attackActive(r));
    }

    private void handleTriumphFeather(AttackInfo attackInfo, long now) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = chr.hasSkill(HEXA_TRIUMPH_FEATHER) ? HEXA_TRIUMPH_FEATHER_ATOM : TRIUMPH_FEATHER_ATOM;
        if (tsm.hasStat(TriumphFeather) && !chr.hasSkillOnCooldown(skillID)) {
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            List<SecondAtom> secondAtoms = new LinkedList<>();
            final var mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
            if (mobs.isEmpty()) {
                return;
            }
            int key = 0;
            for (var entry : si.getSecondAtomInfos().int2ObjectEntrySet()) {
                var mob = Util.getRandomFromCollection(mobs);
                var sai = entry.getValue();
                final int randX = chr.getPosition().getX() + Util.getRandom(-200, 200);
                final int randY = chr.getPosition().getY() - Util.getRandom(100, 200);
                final var pos = new Position(randX, randY);
                SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, key,
                        si.getSkillId(), pos, now);
                secondAtoms.add(fa);
                key++;
            }
            chr.createSecondAtom(secondAtoms);
            chr.addSkillCooldown(skillID, 4000);
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
        int amount = 1;
        if (tsm.hasStat(ArcaneAim)) {
            amount = tsm.getOption(ArcaneAim).nOption;
            if (amount < si.getValue(y, slv)) {
                amount++;
            }
        }
        Option o1 = new Option();
        o1.nOption = amount;
        o1.rOption = skill.getSkillId();
        o1.tOption = 5; // No Time Variable
        tsm.sendStat(ArcaneAim, o1);
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
        changeBlessedCount();
        AffectedArea aa;
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        switch (skillID) {
            case DIVINE_PROTECTION:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.bOption = 1;
                tsm.sendStat(AntiMagicShell, o1);
                break;
            case TELEPORT_MASTERY:
                if (tsm.hasStat(TeleportMasteryOn)) {
                    tsm.removeStat(TeleportMasteryOn);
                    tsm.removeStat(TeleportMasteryRange);
                } else {
                    o1.nOption = si.getValue(x, slv);
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
            case HEAL: {
                chr.heal(changeBishopHealingBuffs(HEAL));
                Rect rect = chr.getPosition().getRectAround(si.getRects().get(0));
                if (inPacket.getUnreadAmount() >= 8) {
                    rect = new Rect(inPacket.decodeShort(), inPacket.decodeShort(),
                            inPacket.decodeShort(), inPacket.decodeShort());
                }
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                for (Life life : chr.getField().getLifesInRect(rect)) {
                    if (life instanceof Mob mob && ((Mob) life).getHp() > 0) {
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                            mts.addStatOptions(mob, MobStat.TotalDamParty, o1.deepCopy());
                        }
                    }
                }
                break;
            }
            case BLESS:
                o1.nOption = si.getValue(u, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(PDD, o1);
                o2.nOption = si.getValue(v, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(EVA, o2);
                newStats.put(ACC, o2.deepCopy());
                o3.nOption = si.getValue(x, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(PAD, o3);
                newStats.put(MAD, o3.deepCopy());
                tsm.sendStat(newStats);
                break;
            case ADV_BLESSING:
                o1.nOption = si.getValue(u, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(PDD, o1);

                o2.nOption = si.getValue(v, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(EVA, o2);
                newStats.put(ACC, o2.deepCopy());

                o3.nOption = si.getValue(x, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(PAD, o3);
                newStats.put(MAD, o3.deepCopy());

                o4.nValue = si.getValue(indieMhp, slv);
                o4.nReason = skillID;
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHP, o4);
                newStats.put(IndieMMP, o4.deepCopy());

                o5.nOption = 1;
                o5.rOption = skillID;
                o5.tOption = si.getValue(time, slv);
                o5.xOption = si.getValue(mpConReduce, slv);
                newStats.put(UsefulAdvancedBless, o5);
                tsm.sendStat(newStats);
                break;
            case HOLY_WATER:
                if (tsm.hasStat(HolyWater)) {
                    int inte = chr.getStat(Stat.inte);
                    int stack = inte >= 2500 ? inte / 2500 : 0;
                    int nCount = inPacket.decodeShort();
                    for (int i = 0; i < Math.min(nCount, 5); i++) {
                        aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                        aa.setMobOrigin((byte) 0);
                        aa.setPosition(inPacket.decodePositionInt());
                        aa.setRect(aa.getPosition().getRectAround(si.getRects().getFirst()));
                        aa.setDelay((short) 4);
                        aa.setDuration(5000 + 5000 * stack);
                        chr.getField().spawnAffectedArea(aa);
                    }
                    o1.nOption = Math.max(0, (int) (tsm.getTotalNOptionOfStat(HolyWater) - nCount));
                    o1.rOption = skillID;
                    tsm.sendStat(HolyWater, o1);
                }
                break;
            case BLOOD_OF_THE_DIVINE:
                if (!chr.hasSkillOnCooldown(skillID)) {
                    chr.addSkillCooldown(skillID, 120000);
                }
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = 10;
                newStats.put(HolyBlood, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieMad, slv);
                o2.tTerm = 10;
                newStats.put(IndiePMdR, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieBooster, slv);
                o3.tTerm = 10;
                newStats.put(IndieHitDamR, o3);
                tsm.sendStat(newStats);
                break;
            case HOLY_SYMBOL:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(HolySymbol, o1);
                break;
            case TRIUMPH_FEATHER:
            case HEXA_TRIUMPH_FEATHER:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(TriumphFeather, o1);
                break;
            case FOUNTAIN_OF_VENGEANCE:
            case HEXA_FOUNTAIN_OF_VENGEANCE:
                Summon summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                Field field = c.getChr().getField();
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                field.broadcast(Summoned.attackActive(summon));
                break;
            case HOLY_MAGIC_SHELL:
                int bonusFromHyper = chr.getSkillLevel(2320044) == 1 ? 5 : 0;
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv) + bonusFromHyper;
                tsm.sendStat(HolyMagicShell, o1);
                this.hmshits = 0;
                chr.heal(changeBishopHealingBuffs(HOLY_MAGIC_SHELL));
                break;
            case RESURRECTION: {
                Party party = chr.getParty();
                if (party != null) {
                    field = chr.getField();
                    Rect rect = chr.getPosition().getRectAround(si.getRects().get(0));
                    if (!chr.isLeft()) {
                        rect = rect.moveRight();
                    }
                    List<Char> eligblePartyCharList = field.getPartyCharSameFieldInRect(chr, rect).stream().
                            filter(pmChr -> pmChr.getId() != chr.getId() && pmChr.getHP() <= 0).toList();
                    for (Char partyChr : eligblePartyCharList) {
                        if (partyChr != null) {
                            partyChr.healHPMP();
                            partyChr.write(UserPacket.effect(Effect.skillAffected(skillID, (byte) 1, 0)));
                        }
                    }
                }
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(NotDamaged, o1);
                break;
            }
            case BAHAMUT:
            case HEXA_BAHAMUT:
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                field = c.getChr().getField();
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Walk);
                field.spawnSummon(summon);
                field.broadcast(Summoned.attackActive(summon));
                break;
            case EPIC_ADVENTURE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case RIGHTEOUSLY_INDIGNANT:
                if (tsm.hasStat(VengeanceOfAngel)) {
                    tsm.removeStat(VengeanceOfAngel);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(VengeanceOfAngel, o1);
                }
                break;
            case BENEDICTION:
            case HEXA_BENEDICTION:
                tsm.removeAllDebuffs();
                int duration = si.getValue(time, slv);
                int totalInt = chr.getTotalStat(BaseStat.inte);
                if (totalInt >= 1500) {
                    o1.nValue = Math.min((totalInt / 1500), 75);
                    o1.nReason = skillID;
                    o1.tTerm = duration;
                    newStats.put(IndiePMdR, o1);
                }
                if (totalInt >= 10000) {
                    o2.nValue = Math.min((totalInt / 10000), 3);
                    o2.nReason = skillID;
                    o2.tTerm = duration;
                    newStats.put(IndieBooster, o2);
                }
                o3.nOption = 1;
                o3.rOption = skillID;
                o3.tOption = duration;
                newStats.put(BishopPray, o3);
                tsm.sendStat(newStats);
                break;
            case HOLY_FOUNTAIN:
                aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                aa.setDelay((short) 4);
                chr.getField().spawnAffectedArea(aa);
                break;
            case MYSTIC_DOOR:
                Field townField = FieldData.getFieldById(chr.getField().getReturnMap());
                int x = townField.getPortalByName("tp").getX();
                int y = townField.getPortalByName("tp").getY();
                Position townPosition = new Position(x, y); // Grabs the Portal Co-ordinates for the TownPortalPoint
                duration = si.getValue(time, slv);
                if (chr.getTownPortal() != null) {
                    TownPortal townPortal = chr.getTownPortal();
                    townPortal.despawnTownPortal();
                }
                TownPortal townPortal = new TownPortal(chr, townPosition, chr.getPosition(), chr.getField().getReturnMap(), chr.getFieldID(), skillID, duration);
                townPortal.spawnTownPortal();
                chr.dispose();
                break;
            case DISPEL:
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case ANGEL_OF_BALANCE_BENEVOLENCE:
            case ANGEL_OF_BALANCE_AVENGE:
                summon = Summon.getSummonBy(chr, ANGEL_OF_BALANCE_BENEVOLENCE, slv);
                summon.setSkillID(skillID);
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setAssistType(skillID == ANGEL_OF_BALANCE_AVENGE ? AssistType.Attack : AssistType.Heal);
                chr.getField().spawnSummon(summon);
                break;
        }
    }

    public void increaseDivinePunishment() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 1;
        if (tsm.hasStat(CannonShooter_BFCannonBall)) {
            count = tsm.getOption(CannonShooter_BFCannonBall).nOption;
            if (count < 5) {
                count++;
            }
        }
        updateVSkillStackBuff(chr, count);
    }

    private void updateDivinePunishment(int count) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option(DIVINE_PUNIHSMENT);
        o.nOption = Math.min(Math.max(0, count), 8);
        o.rOption = DIVINE_PUNIHSMENT;
        tsm.sendStat(CannonShooter_BFCannonBall, o);
        chr.write(WvsContext.updateSkillStackRequestResult(DIVINE_PUNIHSMENT, (byte) 1));
    }

    @Override
    public void handleKeyDownSkillCost(int skillId) {
        super.handleKeyDownSkillCost(skillId);
        switch (skillId) {
            case DIVINE_PUNIHSMENT:
                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                Option o = tsm.getOptByCTSAndSkill(CannonShooter_BFCannonBall, DIVINE_PUNIHSMENT);
                if (o == null) {
                    return;
                }
                updateDivinePunishment(o.nOption - 1); // Cost of keydown
                break;
        }
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

    public void giveAngelOfBalanceSummonBuff() {
        if (chr.hasSkill(ANGEL_OF_BALANCE_BENEVOLENCE)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o = new Option();
            Skill skill = chr.getSkill(ANGEL_OF_BALANCE_BENEVOLENCE);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            chr.heal((si.getValue(y, slv) * chr.getMaxHP()) / 100); // heal 15% HP
            int buffnValue = si.getValue(w, slv) + (chr.getStat(Stat.inte) / si.getValue(attackCount, slv));
            o.nReason = ANGEL_OF_BALANCE_AVENGE;
            o.nValue = Math.min(buffnValue, 100);
            o.tTerm = si.getValue(subTime, slv);
            tsm.sendStat(IndieDamR, o);
        }
    }

    public void givePeacemakerBuffs(int skillId, Rect rect, int contactCount) {
        if (!chr.hasSkill(PEACEMAKER_TRAVEL)) {
            return;
        }
        Field field = chr.getField();
        Skill skill = chr.getSkill(PEACEMAKER_TRAVEL);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        List<Char> contactList = new ArrayList<>();
        int slv = skill.getCurrentLevel();
        int maxContactCount = si.getValue(w, slv);

        if (chr.getParty() != null) {
            contactList = field.getCharsInRect(rect).stream().filter(c -> c.getPartyID() == chr.getPartyID() && c.getFieldID() == chr.getFieldID()).collect(Collectors.toList());
        } else {
            contactList.add(chr);
        }
        switch (skillId) {
            case PEACEMAKER_TRAVEL:
                if (contactCount < maxContactCount) {
                    for (Char partyChr : contactList) {
                        partyChr.heal((int) ((double) partyChr.getMaxHP() * si.getValue(hp, slv) / 100D));
                        partyChr.write(UserPacket.effect(Effect.skillAffected(skillId, slv, 0)));
                        partyChr.getField().broadcast(UserRemote.effect(partyChr.getId(), Effect.skillAffected(skillId, slv, 0)));
                    }
                }
                break;
            case PEACEMAKER_EXPLOSION:
                int remainingContactCount = maxContactCount - contactCount;
                for (Char partyChr : contactList) {
                    TemporaryStatManager partyTSM = partyChr.getTemporaryStatManager();
                    Option o = new Option();
                    o.nReason = skillId;
                    o.nValue = si.getValue(q2, slv) + (remainingContactCount * si.getValue(w2, slv));
                    o.tTerm = si.getValue(time, slv);
                    partyTSM.sendStat(IndieDamR, o);
                }
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        // Bishop - Holy Magic Shell
        if (tsm.hasStat(CharacterTemporaryStat.HolyMagicShell)) {
            if (this.hmshits < Bishop.getHolyMagicShellMaxGuards(chr)) {
                this.hmshits++;
            } else {
                this.hmshits = 0;
                tsm.removeStatsBySkill(HOLY_MAGIC_SHELL);
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleMobDebuffSkill(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(DIVINE_PROTECTION) && tsm.getOptByCTSAndSkill(AntiMagicShell, DIVINE_PROTECTION) != null) {
            tsm.removeStatsBySkill(DIVINE_PROTECTION);
            tsm.removeAllDebuffs();
        }
        super.handleMobDebuffSkill(chr);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (infinityTimer != null) {
            infinityTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        changeBlessedCount();
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_BIG_BANG_EX -> {
                chr.addSkillCooldown(skillId, 6000);
                return 1;
            }
            case HEXA_GENESIS -> {
                int skillID = GENESIS;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }

    public void giveBenedictionBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(BENEDICTION) || !tsm.hasStat(BishopPray) || !JobConstants.isBishop(chr.getJob())) {
            return;
        }
        int skillID = chr.hasSkill(HEXA_BENEDICTION) ? HEXA_BENEDICTION : BENEDICTION;
        Skill skill = chr.getSkill(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = skill.getCurrentLevel();
        Rect rect = chr.getRectAround(si.getFirstRect());

        int userINT = chr.getTotalBasicStats().getOrDefault(BaseStat.inte, 0);
        int bonusDamage = userINT / si.getValue(q2, slv);
        int bonusDamageCap = si.getValue(w, slv);
        int bonusRecovery = userINT / si.getValue(y, slv);
        int bonusRecoveryCap = si.getValue(z, slv);
        int bonusAttSpeed = userINT / si.getValue(u, slv);
        int bonusAttSpeedCap = 3;

        List<Char> chrList = new ArrayList<>();
        if (chr.getParty() == null) {
            chrList.add(chr);
        } else {
            chrList.add(chr);
            chrList.addAll(chr.getParty().getPartyMembersInSameField(chr).stream().filter(c -> rect.hasPositionInside(c.getPosition())).toList());
        }
        Option o1 = new Option();
        Option o2 = new Option();

        o1.nReason = skill.getSkillId();
        o1.nValue = si.getValue(q, slv) + (Math.min(bonusDamage, bonusDamageCap));
        o1.tTerm = 3;

        o2.nReason = skill.getSkillId();
        o2.nValue = -(Math.min(bonusAttSpeed, bonusAttSpeedCap));
        o2.tTerm = 3;

        for (Char pChr : chrList) {
            EnumMap<CharacterTemporaryStat, Option> pNewStats = new EnumMap<>(CharacterTemporaryStat.class);
            TemporaryStatManager pTSM = pChr.getTemporaryStatManager();

            pNewStats.put(IndiePMdR, o1);
            pNewStats.put(IndieBooster, o2);

            tsm.sendStat(pNewStats);

            int HPRecovery = (int) (((si.getValue(x, slv) + (Math.min(bonusRecovery, bonusRecoveryCap))) * pChr.getMaxHP()) / 100D);
            int MPRecovery = (int) (((si.getValue(x, slv) + (Math.min(bonusRecovery, bonusRecoveryCap))) * pChr.getMaxMP()) / 100D);
            pChr.heal(HPRecovery);
            pChr.healMP(MPRecovery);
            pTSM.removeAllDebuffs();
            if (pChr != chr) {
                pChr.write(UserPacket.effect(Effect.skillAffected(skillID, slv, 0)));
                pChr.getField().broadcast(UserRemote.effect(pChr.getId(), Effect.skillAffected(skillID, slv, 0)));
            }
        }
    }
}
