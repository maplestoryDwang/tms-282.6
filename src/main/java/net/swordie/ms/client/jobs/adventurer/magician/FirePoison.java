package net.swordie.ms.client.jobs.adventurer.magician;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class FirePoison extends Magician {

    public static final int MP_EATER = 2100000;
    public static final int POISON_BREATH = 2101005;
    public static final int MAGIC_BOOSTER = 2101008;
    public static final int MEDITATION = 2101001;
    public static final int IGNITE = 2101010;
    public static final int IGNITE_AA = 2100010;

    public static final int BURNING_MAGIC = 2110000;
    public static final int POISON_MIST = 2111003;
    public static final int TELEPORT_MASTERY = 2111007;
    public static final int TELEPORT_BOOST = 2111016;
    public static final int ELEMENTAL_DECREASE = 2111008;
    public static final int ELEMENTAL_ADAPTATION = 2111011;
    public static final int CREEPING_TOXIN = 2111013;
    public static final int CREEPING_TOXIN_EXTRA = 2111014;

    public static final int PARALYZE = 2121006;
    public static final int MIST_ERUPTION = 2121003;
    public static final int HEXA_MIST_ERUPTION = 2141005;
    public static final int FLAME_HAZE = 2121011;
    public static final int INFINITY = 2121004;
    public static final int IFRIT = 2121005;
    public static final int ELEMENTAL_DRAIN = 2100009;
    public static final int FERVENT_DRAIN = 2120014;
    public static final int METEOR_SHOWER = 2121007;
    public static final int METEOR_SHOWER_FA = 2120013;
    public static final int ARCANE_AIM = 2120010;
    public static final int HEROS_WILL = 2121008;
    public static final int POISON_MIST_AFTERMATH = 2120044;
    public static final int POISON_MIST_CRIPPLE = 2120045;
    public static final int PARALYZE_CRIPPLE = 2120047;
    public static final int EPIC_ADVENTURE = 2121053;
    public static final int INFERNO_AURA = 2121054;
    public static final int HEXA_INFERNO_AURA = 2141006;
    public static final int MEGIDDO_FLAME = 2121052;

    // V Skills
    public static final int DOT_PUNISHER = 400021001;
    public static final int POISON_NOVA = 400021028;
    public static final int ELEMENTAL_FURY = 400021066;
    public static final int POISON_CHAIN_1 = 400021101;
    public static final int POISON_CHAIN_2 = 400021102;
    public static final int POISON_CHAIN_3 = 400021103;

    // HEXA Skills
    public static final int HEXA_METEOR_SHOWER = 2141012;
    public static final int HEXA_METEOR_SHOWER_FA = 2140013;
    public static final int HEXA_FLAME_SWEEP = 2141000;
    public static final int HEXA_FLAME_SWEEP_GIANT = 2141001;
    public static final int HEXA_FLAME_HAZE = 2141003;
    public static final int HEXA_POISON_MIST = 2141004;
    public static final int HEXA_IGNITE = 2141007;
    public static final int HEXA_IGNITE_AA = 2140008;
    public static final int HEXA_IFRIT = 2141009;
    public static final int HEXA_CREEPING_TOXIN = 2141014;
    public static final int HEXA_CREEPING_TOXIN_EXTRA = 2141015;
    public static final int HEXA_MEGIDDO_FLAME_MANUAL = 2141010;
    public static final int HEXA_MEGIDDO_FLAME_AUTO = 2141011;
    public static final int HEXA_MEGIDDO_FLAME_EXPLOSION = 2141016;
    public static final int INFERNAL_VENOM_ICON = 2141501;

    private int megiddoFlameCount = 0;
    private boolean megiddoArmedForInstantEx = false;
    private int infinityStack = 0;
    private ScheduledFuture<?> infinityTimer;
    private List<Integer> explodeShootObjList;

    public FirePoison(Char chr) {
        super(chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isFirePoison(id);
    }

    public List<Integer> getExplodeShootObjList() {
        return explodeShootObjList;
    }

    public void setExplodeShootObjList(List<Integer> explodeShootObjList) {
        this.explodeShootObjList = explodeShootObjList;
    }

    private void infinity() {
        int skillID = INFINITY;
        if (!chr.hasSkill(skillID)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Skill skill = chr.getSkill(skillID);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        infinityStack++;
        if (tsm.hasStat(Infinity)) {
            o1.nValue = infinityStack * si.getValue(damage, slv);
            o1.nReason = skillID + 100; //To make the buff icon hidden
            tsm.sendStat(IndieMADR, o1);
            if (chr.getHP() > 0) {
                chr.heal((int) (chr.getMaxHP() / ((double) 100 / si.getValue(y, slv))));
                chr.healMP((int) (chr.getMaxMP() / ((double) 100 / si.getValue(y, slv))));
            }
            infinityTimer = chr.getTimer().addEvent(this::infinity, 4, TimeUnit.SECONDS);
        } else {
            tsm.removeStatsBySkill(skillID + 100);
            infinityStack = 0;
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        mpEaterEffect(mob, skillID);
        BurnedInfo bi;
        long damageDot = !chr.getDamageCalc().getDamages().isEmpty() ? Util.getRandomFromCollection(chr.getDamageCalc().getDamages()) : 1;
        switch (skillID) {
            case POISON_BREATH:
            case IFRIT:
            case HEXA_IFRIT:
            case MEGIDDO_FLAME:
            case HEXA_MEGIDDO_FLAME_MANUAL:
            case HEXA_MEGIDDO_FLAME_AUTO:
            case INFERNO_AURA:
            case HEXA_INFERNO_AURA:
            case DOT_PUNISHER:
            case HEXA_FLAME_SWEEP:
                bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damageDot);
                mts.createAndAddBurnedInfo(mob, bi, skillID);
                break;
            case POISON_CHAIN_1:
                bi = BurnedInfo.createBurnInfo(chr, POISON_CHAIN_2, slv, damageDot);
                mts.createAndAddBurnedInfo(mob, bi, POISON_CHAIN_2);
                break;
            case TELEPORT_MASTERY:
                bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damageDot);
                if (!mob.isBoss()) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o1);
                }
                mts.createAndAddBurnedInfo(mob, bi, skillID);
                break;
            case FLAME_HAZE:
            case HEXA_FLAME_HAZE:
                int poisonSkillID = skillID == HEXA_FLAME_HAZE ? HEXA_POISON_MIST : POISON_MIST;
                SkillInfo pmSi = SkillData.getSkillInfoById(poisonSkillID);
                int pmSlv = chr.getSkillLevel(poisonSkillID);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, poisonSkillID, pmSlv > 0 ? pmSlv : 1);
                aa.setPosition(chr.getPosition());
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o2.nOption = si.getValue(x, slv); // already negative in si
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damageDot);
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    if (!mob.isBoss()) {
                        map.put(MobStat.DodgeBodyAttack, o1); //Untouchable (physical dmg) Mob Stat
                    }
                    map.put(MobStat.Speed, o2);
                    mts.addStatOptions(mob, map);
                    mts.createAndAddBurnedInfo(mob, bi, skillID);
                }
                aa.setPosition(mob.getPosition());
                aa.setRect(aa.getPosition().getRectAround(pmSi.getFirstRect()));
                chr.getField().spawnAffectedArea(aa);
                break;
            case PARALYZE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damageDot);
                    if (!mob.isBoss()) {
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                    mts.createAndAddBurnedInfo(mob, bi, skillID);
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
            updateElementDrain();
            incrementArcaneAim();
            applyIgniteOnMob(attackInfo, tsm, skillID);
        }
        Option o1 = new Option();
        switch (skillID) {
            case POISON_MIST:
            case HEXA_POISON_MIST:
                AffectedArea aa = AffectedArea.getAffectedArea(chr, attackInfo);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getFirstRect()));
                aa.setDelay((short) 9);
                aa.setDamage(!chr.getDamageCalc().getDamages().isEmpty() ? Util.getRandomFromCollection(chr.getDamageCalc().getDamages()) : 1);
                chr.getField().spawnAffectedArea(aa);
                break;
            case HEXA_FLAME_SWEEP:
                o1 = tsm.getOption(FlameSweep);
                var val = o1.nOption + 1;
                if (val > 2) {
                    val = 0;
                    chr.write(UserLocal.userBonusAttackRequest(HEXA_FLAME_SWEEP_GIANT));
                }
                o1.nOption = val;
                o1.rOption = skillID;
                tsm.sendStat(FlameSweep, o1);
                break;
            case INFERNAL_VENOM_ICON:
                if (!tsm.hasStat(SixthDotBasedBuff)) {
                    o1.nOption = 10;
                    o1.rOption = INFERNAL_VENOM_ICON;
                    o1.tOption = 20;
                    tsm.sendStat(SixthDotBasedBuff, o1);
                }
                break;
        }
    }

    public void spawnCreepingToxinAreas(InPacket inPacket) {
        int skillID = chr.hasSkill(HEXA_CREEPING_TOXIN) ? HEXA_CREEPING_TOXIN : CREEPING_TOXIN;
        if (chr.hasSkill(skillID)) {
            int slv = chr.getSkillLevel(skillID);
            if (chr.getTemporaryStatManager().hasStatBySkillId(skillID)) {
                SkillInfo si = SkillData.getSkillInfoById(skillID);
                byte action = inPacket.decodeByte();
                int size = inPacket.decodeInt();
                for (int i = 0; i < size; i++) {
                    int x = inPacket.decodeInt();
                    int y = inPacket.decodeInt();
                    AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                    aa.setPosition(new Position(x, y));
                    aa.setRect(aa.getPosition().getRectAround(si.getFirstRect()));
                    aa.setDamage(!chr.getDamageCalc().getDamages().isEmpty() ? Util.getRandomFromCollection(chr.getDamageCalc().getDamages()) : 1);
                    aa.setHitMob(true);
                    chr.getField().spawnAffectedArea(aa);
                }
            }
        }
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_MEGIDDO_FLAME_MANUAL -> {
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }

    private void createMegiddoFlameSecondAtom(SkillInfo si) {
        List<SecondAtom> secondAtoms = new LinkedList<>();
        final var mobs = chr.getField().getMobsFiltered();
        if (mobs.isEmpty()) {
            return;
        }
        int key = 0;
        final long start = System.currentTimeMillis();
        for (var entry : si.getSecondAtomInfos().int2ObjectEntrySet()) {
            int index = entry.getIntKey();
            var sai = entry.getValue();
            if (index == 3) {
                continue;
            }
            var mob = Util.getRandomFromCollection(mobs);
            final var pos = chr.getPosition().add(sai.getPos());
            SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, key, si.getSkillId(), pos, start);
            fa.setFirstAngleStart(1);
            fa.setSpecialAtom(true);
            fa.setUnk(1);
            secondAtoms.add(fa);
            key++;
        }
        this.megiddoFlameCount = 0;
        chr.createSecondAtom(secondAtoms);
    }

    @Override
    public boolean handleSecondAtomRemoveRequest(int objectId) {
        SecondAtom sa = chr.getSecondAtomById(objectId);
        if (sa == null) {
            return true;
        }
        int skillId = sa.getSkillId();
        int targetId = sa.getTargetID();
        if (skillId == MEGIDDO_FLAME || skillId == HEXA_MEGIDDO_FLAME_MANUAL || skillId == HEXA_MEGIDDO_FLAME_AUTO) {
            if (skillId == HEXA_MEGIDDO_FLAME_AUTO) {
                chr.write(UserLocal.userBonusAttackRequest(HEXA_MEGIDDO_FLAME_EXPLOSION, targetId));
                megiddoArmedForInstantEx = false;
                return true;
            }
            if (this.megiddoFlameCount >= 11) {
                if (chr.hasSkill(HEXA_MEGIDDO_FLAME_MANUAL)) {
                    chr.write(UserLocal.userBonusAttackRequest(HEXA_MEGIDDO_FLAME_EXPLOSION, targetId));
                    chr.setSkillCooldown(MEGIDDO_FLAME, 1);
                    megiddoArmedForInstantEx = false;
                }
                return true;
            }
            if (!megiddoArmedForInstantEx && skillId != MEGIDDO_FLAME) {
                return true;
            }
            List<SecondAtom> secondAtoms = new LinkedList<>();
            SkillInfo si = SkillData.getSkillInfoById(skillId);
            final long start = System.currentTimeMillis();
            final var sai = si.getSecondAtomInfos().get(3);
            int key = 0;
            final var mob = chr.getField().getMobByObjectId(targetId);
            final var pos = mob != null ? mob.getPosition().add(sai.getPos()) : chr.getPosition().add(sai.getPos());
            final var customs = new Int2IntOpenHashMap();
            customs.put(0, 1);
            for (int i = 0; i < 2; i++) {
                SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), targetId,
                        key, si.getSkillId(), pos, start);
                fa.setFirstAngleStart(1);
                fa.setCustoms(customs);
                fa.setUnk(1);
                secondAtoms.add(fa);
                this.megiddoFlameCount += 1;
                key++;
            }
            chr.createSecondAtom(secondAtoms);
            return true;
        }
        return super.handleSecondAtomRemoveRequest(objectId);
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
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
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

    private void applyIgniteOnMob(AttackInfo attackInfo, TemporaryStatManager tsm, int skillID) {
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        if (si == null || !si.getElemAttr().contains("f")
                || skillID == IGNITE || skillID == IGNITE_AA
                || skillID == HEXA_IGNITE || skillID == HEXA_IGNITE_AA
                || skillID == INFERNO_AURA || skillID == HEXA_INFERNO_AURA) {
            return;
        }
        if (tsm.hasStat(WizardIgnite)) {
            int igniteSkillID = chr.hasSkill(HEXA_IGNITE) ? HEXA_IGNITE : IGNITE;
            int igniteAASkillID = chr.hasSkill(HEXA_IGNITE) ? IGNITE_AA : IGNITE_AA; // TODO: HEXA_IGNITE_AA
            SkillInfo igniteInfo = SkillData.getSkillInfoById(igniteAASkillID);
            Skill skill = chr.getSkill(igniteSkillID);
            int slv = skill.getCurrentLevel();
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                if (Util.succeedProp(igniteInfo.getValue(prop, slv))) {
                    AffectedArea aa = AffectedArea.getPassiveAA(chr, igniteAASkillID, slv);
                    aa.setPosition(mob.getPosition());
                    aa.setRect(aa.getRectAround(igniteInfo.getFirstRect()));
                    aa.setDelay((short) 3);
                    chr.getField().spawnAffectedArea(aa);
                }
            }
        }
    }

    public void updateElementDrain() {
        if (!chr.hasSkill(ELEMENTAL_DRAIN) || chr.hasSkillOnCooldown(ELEMENTAL_DRAIN)) {
            return;
        }
        int stack = (int) chr.getField().getMobs().stream().filter(mob -> mob.getTemporaryStat().hasBurnFromOwner(chr.getId())).count();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (stack == 0) {
            tsm.removeStatsBySkill(ELEMENTAL_DRAIN);
            return;
        }
        Option o1 = new Option();
        o1.nOption = (Math.min(stack, tsm.hasStat(SixthDotBasedBuff) ? 10 : 5));
        o1.rOption = ELEMENTAL_DRAIN;
        tsm.sendStat(DotBasedBuff, o1);
        chr.addSkillCooldown(ELEMENTAL_DRAIN, 500);
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
        switch (skillID) {
            case MEDITATION:
                o1.nValue = si.getValue(indieMad, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieMAD, o1);
                break;
            case IGNITE:
            case HEXA_IGNITE:
                if (tsm.hasStat(WizardIgnite)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(WizardIgnite, o1);
                }
                break;
            case ELEMENTAL_DECREASE:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ElementalReset, o1);
                break;
            case ELEMENTAL_ADAPTATION:
                o1.nOption = 6;
                o1.rOption = skillID;
                // no bOption for FP's AntiMagicShell
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
            case CREEPING_TOXIN:
            case HEXA_CREEPING_TOXIN:
                Summon summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                Field field = c.getChr().getField();
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.None);
                field.spawnSummon(summon);
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
            case IFRIT:
            case HEXA_IFRIT:
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                field = c.getChr().getField();
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Walk);
                field.spawnSummon(summon);
                field.broadcast(Summoned.attackActive(summon));
                break;
            case MIST_ERUPTION:
            case HEXA_MIST_ERUPTION:
                if (getExplodeShootObjList() != null && !getExplodeShootObjList().isEmpty()) {
                    chr.write(UserPacket.shootObjectExplodeResult(chr.getId(), getExplodeShootObjList()));
                    getExplodeShootObjList().clear();
                }
                for (AffectedArea poisonMist : chr.getField().getAffectedAreas()) {
                    if (poisonMist.getSkillID() == POISON_MIST && poisonMist.getCharID() == chr.getId()) {
                        poisonMist.setLinkingSkillID(skillID);
                        chr.getField().removeLife(poisonMist);
                    }
                }
                break;
            case EPIC_ADVENTURE:
                o1.nValue = si.getValue(indieDamR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case INFERNO_AURA:
            case HEXA_INFERNO_AURA:
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(FireAura, o1);
                break;
            case DOT_PUNISHER:
                createDoTPunisherForceAtom();
                break;
            case MEGIDDO_FLAME:
                createMegiddoFlameSecondAtom(si);
                break;
            case HEXA_MEGIDDO_FLAME_MANUAL:
                megiddoFlameCount = 0;
                megiddoArmedForInstantEx = true;
                createMegiddoFlameSecondAtom(si);
                break;
            case HEXA_MEGIDDO_FLAME_AUTO:
                if (megiddoArmedForInstantEx) {
                    megiddoArmedForInstantEx = false;
                    chr.setSkillCooldown(MEGIDDO_FLAME, 1);
                    megiddoFlameCount = 11;
                    break;
                }
                chr.setSkillCooldown(MEGIDDO_FLAME, 1);
                createMegiddoFlameSecondAtom(si);
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
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

    @Override
    public void handleMobDebuffSkill(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(ELEMENTAL_ADAPTATION) && tsm.getOptByCTSAndSkill(AntiMagicShell, ELEMENTAL_ADAPTATION) != null) {
            deductEleAdaptationFP();
            tsm.removeAllDebuffs();
        }
        super.handleMobDebuffSkill(chr);
    }

    // Elemental Adaptation - FP
    private void deductEleAdaptationFP() {
        if (!chr.hasSkill(ELEMENTAL_ADAPTATION)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        Skill skill = chr.getSkill(ELEMENTAL_ADAPTATION);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int proc = si.getValue(prop, slv);

        int stack = tsm.getOption(AntiMagicShell).nOption;
        if (stack > 0) {
            if (Util.succeedProp(proc)) {
                stack--;

                o.nOption = stack;
                o.rOption = ELEMENTAL_ADAPTATION;
                tsm.sendStat(AntiMagicShell, o);
            } else {
                tsm.removeStatsBySkill(ELEMENTAL_ADAPTATION);
            }
        } else {
            tsm.removeStatsBySkill(ELEMENTAL_ADAPTATION);
        }
    }

    private void createDoTPunisherForceAtom() {
        Field field = chr.getField();
        if (!chr.hasSkill(DOT_PUNISHER)) {
            return;
        }
        Skill skill = chr.getSkill(DOT_PUNISHER);
        SkillInfo si = SkillData.getSkillInfoById(DOT_PUNISHER);
        int slv = skill.getCurrentLevel();
        int forceAtomCount = si.getValue(x, slv);
        ForceAtomEnum fae = ForceAtomEnum.DOT_PUNISHER;
        List<Integer> targetList = new ArrayList<>();
        List<ForceAtomInfo> faiList = new ArrayList<>();
        int doTMobs = (int) field.getMobs().stream().filter(mob -> mob.getTemporaryStat().hasBurnFromOwner(chr.getId())).count();
        for (int i = 0; i < forceAtomCount + doTMobs; i++) {
            int angle = (360 / forceAtomCount) * i;
            int circleRadii = 150;
            int vTranslation = (int) (Math.sin(angle) * circleRadii);
            int hTranslation = (int) (Math.cos(angle) * circleRadii);

            Mob mob = Util.getRandomFromCollection(field.getMobs());
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 43, 6,
                    angle, 2000 + (i * 20), Util.getCurrentTime(), 1, 0,
                    new Position(chr.getPosition().getX() + hTranslation, chr.getPosition().getY() + vTranslation - 350));
            targetList.add(mob != null ? mob.getObjectId() : 0);
            faiList.add(forceAtomInfo);
        }
        ForceAtom fa = new ForceAtom(chr.getId(), fae, targetList, DOT_PUNISHER, faiList);
        fa.setRect(chr.getPosition().getRectAround(si.getFirstRect()));
        chr.createForceAtom(fa);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (infinityTimer != null) {
            infinityTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    public void handleShootObject(Char chr, ShootObjectSkillInfo sosi) {
        var skillId = sosi.getSkillId();
        var slv = sosi.getSlv();
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        switch (skillId) {
            case POISON_NOVA:
                chr.addSkillCooldown(skillId, (int) (si.getValue(cooltime, slv) * 1000L));
                break;
        }
        super.handleShootObject(chr, sosi);
    }
}
