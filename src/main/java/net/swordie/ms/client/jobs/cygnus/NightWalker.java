package net.swordie.ms.client.jobs.cygnus;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.enums.LeaveType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class NightWalker extends Noblesse {

    public static final int ELEMENTAL_HARMONY_LUK = 10000249;

    public static final int SHADOW_JUMP = 14001026;
    public static final int SHADOW_MOMENTUM = 14110032;

    public static final int LUCKY_SEVEN = 14001020;
    public static final int DARK_ELEMENTAL = 14001021; //Buff (Mark of Darkness)
    public static final int HASTE = 14001022; //Buff
    public static final int DARK_SIGHT = 14001023; //Buff

    public static final int THROWING_BOOSTER = 14101022; //Buff

    public static final int POISON_BOMB = 14111006;
    public static final int DARK_SERVANT = 14111024; //Buff
    public static final int SPIRIT_PROJECTION = 14110031; //Passive
    public static final int DARKNESS_ASCENDING = 14110030; //Special Buff
    public static final int SHADOW_SPARK = 14111023;

    public static final int DARK_OMEN = 14121003; //Summon
    public static final int SHADOW_STITCH = 14121004; //Special Attack (Bind Debuff)
    public static final int CALL_OF_CYGNUS = 14121000; //Buff
    public static final int VITALITY_SIPHON = 14120009;

    public static final int GLORY_OF_THE_GUARDIANS = 14121053;
    public static final int SHADOW_ILLUSION = 14121054;
    public static final int SHADOW_ILLUSION_1 = 14121055;
    public static final int SHADOW_ILLUSION_2 = 14121056;
    public static final int DOMINION = 14121052;

    //Bats
    public static final int SHADOW_BAT = 14001027; //Buff (Shadow Bats) (ON/OFF)
    public static final int SHADOW_BAT_DOMINION = 14000027; // Summon

    public static final int SHADOW_BAT_ATOM = 14000028;
    public static final int SHADOW_BAT_FROM_MOB_ATOM = 14000029;

    public static final int SHADOW_BAT_SUMMON_II = 14110033;
    public static final int SHADOW_BAT_FROM_MOB_ATOM_II = 14110034;
    public static final int SHADOW_BAT_EXTRA = 14110035;

    public static final int BAT_AFFINITY = 14100027; //Summon Upgrade
    public static final int BAT_AFFINITY_II = 14110029; //Summon Upgrade
    public static final int BAT_AFFINITY_III = 14120008; //Summon Upgrade

    public static final int RAVENOUS_BAT = 14121016; //Buff (Ravenous Bats) (ON/OFF)
    public static final int RAVENOUS_BAT_SUMMON = 14120017; //Summon
    public static final int RAVENOUS_BAT_ATOM = 14120018;
    public static final int RAVENOUS_BAT_SUMMON2 = 14120019; //Summon
    public static final int RAVENOUS_BAT_FROM_MOB_ATOM = 14120020;

    //Dark Elemental
    public static final int ADAPTIVE_DARKNESS = 14100026;
    public static final int ADAPTIVE_DARKNESS_II = 14110028;
    public static final int ADAPTIVE_DARKNESS_III = 14120007;

    //Attacks
    public static final int QUINTUPLE_STAR = 14121001;
    public static final int QUINTUPLE_STAR_FINISHER = 14121002;

    public static final int QUAD_STAR = 14111020;
    public static final int QUAD_STAR_FINISHER = 14111021;

    public static final int TRIPLE_THROW = 14101020;
    public static final int TRIPLE_THROW_FINISHER = 14101021;

    // V Skill
    public static final int SHADOW_SPEAR = 400041008;
    public static final int SHADOW_SPEAR_AA_SMALL = 400040008;
    public static final int SHADOW_SPEAR_AA_LARGE = 400041019;
    public static final int GREATER_DARK_SERVANT = 400041028;
    public static final int SHADOW_BITE = 400041037;
    public static final int RAPID_THROW = 400041059;

    // HEXA Skills
    public static final int HEXA_SHADOW_BAT = 14141004; //Buff (Shadow Bats) (ON/OFF)
    public static final int HEXA_SHADOW_BAT_SUMMON = 14141005; // Summon
    public static final int HEXA_SHADOW_BAT_ATOM = 14141006;
    public static final int HEXA_SHADOW_BAT_FROM_MOB_ATOM = 14141007;
    public static final int HEXA_SHADOW_BAT_SUMMON_II = 14141008; // Summon
    public static final int HEXA_RAVENOUS_BAT = 14141012; //Buff (Ravenous Bats) (ON/OFF)
    public static final int HEXA_RAVENOUS_BAT_SUMMON = 14141013; //Summon
    public static final int HEXA_RAVENOUS_BAT_SUMMON2 = 14141015; //Summon

    // HEXA Boosts
    public static final int HEXA_GREATER_DARK_SERVANT = 500061004;
    public static final int HEXA_SHADOW_BITE = 500061035; // TODO

    private final int[] addedSkills = new int[]{
            ELEMENTAL_HARMONY_LUK
    };

    private final int[] darkEleSkills = new int[]{
            DARK_ELEMENTAL,
            ADAPTIVE_DARKNESS,
            ADAPTIVE_DARKNESS_II,
            ADAPTIVE_DARKNESS_III,};

    private final int[] batSkills = new int[]{
            SHADOW_BAT,
            BAT_AFFINITY,
            BAT_AFFINITY_II,
            BAT_AFFINITY_III,};

    private final List<Summon> bats = new ArrayList<>();
    private Summon darkServant;
    private final List<Summon> darkOmenBats = new ArrayList<>();
    private final List<Summon> dominionBats = new ArrayList<>();
    private int darkOmenAttackCount = 0;
    private int darkSightCDCount = 0;
    private static final Rect SHADOW_SPEAR_REMOVE_RECT = new Rect(-500, -500, 500, 500);

    public NightWalker(Char chr) {
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

    public static void reviveByDarknessAscending(@NotNull Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        chr.heal(chr.getMaxHP(), true);
        tsm.removeStatsBySkill(DARKNESS_ASCENDING);
        var eff = Effect.skillPreLoopEnd(DARKNESS_ASCENDING, 1000);
        chr.write(UserPacket.effect(eff));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), eff), chr);
    }

    public static int getDarkOmenBatCount(@NotNull Char chr) {
        if (chr.hasSkill(DARK_OMEN)) {
            SkillInfo si = SkillData.getSkillInfoById(DARK_OMEN);
            return si.getValue(z, chr.getSkillLevel(DARK_OMEN));
        }
        return 0;
    }

    public static boolean isDarkAtackSkill(int skillId) {
        return skillId == TRIPLE_THROW_FINISHER
                || skillId == QUAD_STAR_FINISHER
                || skillId == 14111022
                || skillId == SHADOW_SPARK
                || skillId == QUINTUPLE_STAR_FINISHER
                || skillId == SHADOW_BAT_DOMINION
                || skillId == SHADOW_BAT_ATOM
                || skillId == SHADOW_BAT_FROM_MOB_ATOM
                || skillId == SHADOW_BAT;
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isNightWalker(id);
    }

    private void applyDarkServant() {
        Field field = c.getChr().getField();
        Skill skill = chr.getSkill(DARK_SERVANT);
        int slv = skill.getCurrentLevel();
        darkServant = Summon.getSummonByAndSetStat(c.getChr(), skill.getSkillId(), slv);
        darkServant.setAvatarLook(chr.getAvatarData().getAvatarLook());
        darkServant.setMoveAbility(MoveAbility.WalkClone);
        darkServant.setActionDelay(400);
        darkServant.setMovementDelay(30);
        darkServant.setAssistType(AssistType.None);
        darkServant.setFlip(true);
        field.spawnSummon(darkServant);
    }

    private void applyShadowIllusion() {
        Field field = chr.getField();
        for (int i = 0; i < 3; i++) {
            Summon shadowIllusion = Summon.getSummonByAndSetStat(chr, SHADOW_ILLUSION + i, (byte) 1);
            shadowIllusion.setFlyMob(false);
            shadowIllusion.setAvatarLook(chr.getAvatarData().getAvatarLook());
            shadowIllusion.setMoveAbility(MoveAbility.WalkClone);
            shadowIllusion.setAssistType(AssistType.AttackManual);
            shadowIllusion.setAttackActive(true);
            shadowIllusion.setActionDelay(400 + i * 100);
            shadowIllusion.setMovementDelay(30 + i * 100);
            field.broadcast(Summoned.attackActive(shadowIllusion));
            field.spawnSummon(shadowIllusion);
        }
    }

    private int getGreaterDarkServantID() {
        return chr.hasSkill(HEXA_GREATER_DARK_SERVANT) ? HEXA_GREATER_DARK_SERVANT : GREATER_DARK_SERVANT;
    }

    private Summon getGreaterDarkServant() {
        return chr.getField().getSummonBySkillId(chr, getGreaterDarkServantID());
    }

    public void swapWithServant() {
        Summon summon = getGreaterDarkServant();
        if (summon != null && summon.getCount() > 0) {
            summon.setCount(summon.getCount() - 1);
            chr.getField().broadcast(Summoned.upgradeStage(summon, 2));
            chr.getField().broadcast(Summoned.effect(summon, 2));
            chr.write(UserLocal.greaterDarkServantSwapResult(summon.getPosition()));
            summon.setPosition(chr.getPosition());
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillSpecial(getGreaterDarkServantID())), chr);
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        // Handling Dark Elemental
        if (tsm.hasStat(ElementDarkness)) {
            applyDarkElementalOnMob(mob, slv, damage);
        }
        // Reduce Dark Omen Cooltime
        if (si.getElemAttr().equalsIgnoreCase("d") && skillID != DARK_OMEN && skillID != SHADOW_BAT_ATOM && chr.hasSkillOnCooldown(DARK_OMEN)) {
            reduceDarkOmenCooltime();
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        super.handleAttack(c, attackInfo, si, now);
        int skillID = attackInfo.skillId;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (hasHitMobs) {
            if (skillID == DARK_OMEN) {
                if (tsm.hasStatBySkillId(SHADOW_BAT)) {
                    int mobsHit = attackInfo.mobAttackInfo.size();
                    darkOmenAttackCount += mobsHit;
                    int attForBats = si.getValue(x, slv);
                    int batsToSummon = darkOmenAttackCount / 3;
                    darkOmenAttackCount = darkOmenAttackCount % 3;
                    for (int i = 0; i < batsToSummon; i++) {
                        if (darkOmenBats.size() < getDarkOmenBatCount(chr)) {
                            summonBatByDarkOmen();
                        } else {
                            break;
                        }
                    }
                }
            }
            if (tsm.hasStatBySkillId(SHADOW_SPEAR) && si.getElemAttr().equalsIgnoreCase("d")
                    && skillID != DARK_OMEN && skillID != SHADOW_SPEAR_AA_SMALL && skillID != SHADOW_SPEAR_AA_LARGE) {
                placeShadowSpearAA(attackInfo);
            }

            //handle Vitality Siphon
            if (chr.hasSkill(VITALITY_SIPHON)) {
                incrementSiphonVitality(tsm);
            }
            // Handling Shadow Bats
            if (skillID != SHADOW_BAT_ATOM && skillID != SHADOW_BAT_FROM_MOB_ATOM) {
                shadowBats(attackInfo);
            }
        }
        Option o1 = new Option();
        switch (skillID) {
            case SHADOW_STITCH:
                int size = attackInfo.mobAttackInfo.size();
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = (Math.min(si.getValue(time, slv) + size, 25));
                for (int i = 0; i < size; i++) {
                    Life life = chr.getField().getLifeByObjectID(attackInfo.mobAttackInfo.get(i).mobId);
                    if (life instanceof Mob mob) {
                        if (mob.getHp() <= 0 || mob.isBoss()) {
                            continue;
                        }
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                            mts.addStatOptions(mob, MobStat.Freeze, o1.deepCopy());
                        }
                    }
                }
                break;
            case DOMINION:
                Option o2 = new Option();
                Option o3 = new Option();
                Option o4 = new Option();
                Option o5 = new Option();
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Dominion, o1);
                o2.nValue = si.getValue(indieCr, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieCrR, o2);
                o3.nValue = si.getValue(indieStance, slv);
                o3.nReason = skillID;
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieStance, o3);
                o4.nValue = si.getValue(indieDamR, slv);
                o4.nReason = skillID;
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o4);
                o5.nOption = 1;
                o5.rOption = skillID;
                o5.tOption = si.getValue(time, slv);
                newStats.put(NotDamaged, o5);
                tsm.sendStat(newStats);
                break;
        }
    }

    // handling Shadow Bats
    private void shadowBats(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(NightWalkerBat)) {
            return;
        }
        boolean hasElementDarkness = false;
        final var mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
        for (Mob mob : mobs) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            if (mts == null) {
                return;
            }
            // Dark Elemental : Hit enemies with Throwing Stars Skill for a 20% chance to create a Mark of Darkness that stacks 2 times
            hasElementDarkness = mts.hasCurrentMobStat(MobStat.ElementDarkness) & Util.succeedProp(20);
            if (hasElementDarkness) {
                break;
            }
        }
        final int prop = getBatAttackProp(hasElementDarkness);
        List<Summon> toRemove = new ArrayList<>();
        collectBatsToRemove(bats, prop, toRemove);
        collectBatsToRemove(darkOmenBats, prop, toRemove);
        collectBatsToRemove(dominionBats, prop, toRemove);
        for (Summon bat : toRemove) {
            final var pos = bat.getPosition();
            chr.getField().broadcast(Summoned.doSkill(bat, (byte) 0, 0, null));
            chr.getField().removeSummon(bat.getSkillID(), chr.getId());
            createShadowBatForceAtom(mobs, pos);
        }
        bats.removeAll(toRemove);
        darkOmenBats.removeAll(toRemove);
        dominionBats.removeAll(toRemove);
        if (bats.size() < getMaxBats()) {
            summonBatAndRegister();
        }
    }

    private void collectBatsToRemove(List<Summon> source, int prop, List<Summon> out) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Summon bat : source) {
            if (bat == null) {
                continue;
            }
            if (Util.succeedProp(prop)) {
                out.add(bat);
            }
        }
    }

    private void createShadowBatForceAtom(List<Mob> mobs, Position batPosition) {
        Field field = chr.getField();
        final var mob = Util.getRandomFromCollection(mobs);
        if (mob == null) {
            return;
        }
        final var pos = mob.getPosition();
        final var rect = pos.getRectAround(new Rect(-200, -200, 200, 200));
        final var now = Util.getCurrentTime();
        final var toMobs = field.getMobsInRect(rect);
        if (toMobs.isEmpty()) {
            return;
        }
        Mob toMob = Util.getRandomFromCollection(toMobs);
        if (toMob != null && toMob.getHp() > 0) {
            ForceAtomEnum fae = getCurrentShadowBatSkill().getSkillId() == BAT_AFFINITY_III ? ForceAtomEnum.NIGHT_WALKER_BAT_4 : ForceAtomEnum.NIGHT_WALKER_BAT;
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), 1, 5,
                    (int) Util.getAngleOfTwoPositions(chr.getPosition(), toMob.getPosition()), 500, now, 0, 0,
                    new Position(batPosition.getX() - chr.getPosition().getX(), batPosition.getY() - chr.getPosition().getY()));
            ForceAtom fa = new ForceAtom(false, 0, chr.getId(), fae,
                    true, mob.getObjectId(), SHADOW_BAT_ATOM, fai, chr.getRectAround(new Rect(-20, -20, 20, 20)), 0, 0,
                    new Position(), 0, new Position(), 0);
            fa.setMaxRecreationCount(getShadowBatRecreationCount() + 1);
            chr.createForceAtom(fa);
        }
    }

    private Skill getCurrentShadowBatSkill() {
        Skill skill = null;
        for (int batSkill : batSkills) {
            if (chr.hasSkill(batSkill)) {
                skill = chr.getSkill(batSkill);
            }
        }
        return skill;
    }

    private int getMaxBats() {
        int maxBats = 0;
        for (int batSkill : batSkills) {
            Skill skill = chr.getSkill(batSkill);
            if (skill == null) {
                continue;
            }
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            maxBats += Math.min(si.getValue(y, slv), 2);
        }
        return maxBats;
    }

    private int getBatAttackProp(boolean isDouble) {
        int batAttackProp = 0;
        for (int batSkill : batSkills) {
            Skill skill = chr.getSkill(batSkill);
            if (skill == null) {
                continue;
            }
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            batAttackProp += si.getValue(prop, slv);
        }
        return isDouble ? batAttackProp * 2 : batAttackProp;
    }

    private int getShadowBatRecreationCount() {
        return Arrays.stream(batSkills)
                .filter(skill -> chr.hasSkill(skill))
                .map(skillInt -> SkillData.getSkillInfoById(skillInt).getValue(mobCount, chr.getSkillLevel(skillInt)))
                .sum();
    }

    private Skill getBatSkill() {
        Skill skill = null;
        for (int batSkill : batSkills) {
            if (chr.hasSkill(batSkill)) {
                skill = chr.getSkill(batSkill);
            }
        }
        return skill;
    }

    private void summonBatAndRegister() {
        Summon summon = Summon.getSummonBy(chr, getBatSkill().getSkillId(), (byte) getBatSkill().getCurrentLevel());
        summon.setFlyMob(true);
        summon.setMoveAction((byte) 5);
        summon.setMoveAbility(MoveAbility.Fly);
        summon.setAssistType(AssistType.None);
        summon.setAttackActive(false);
        summon.setSummonTerm(60000);
        chr.getField().spawnAddSummon(summon);
        bats.add(summon);
    }

    private void summonBatByDarkOmen() {
        Summon summon = Summon.getSummonBy(chr, getBatSkill().getSkillId(), (byte) getBatSkill().getCurrentLevel());
        summon.setFlyMob(true);
        summon.setMoveAbility(MoveAbility.Fly);
        summon.setAttackActive(false);
        chr.getField().spawnAddSummon(summon);
        chr.getField().broadcast(Summoned.doSkill(summon, (byte) 0, 0, null));
        darkOmenBats.add(summon);
    }

    private void summonBatByDominion() {
        Summon summon = Summon.getSummonBy(chr, SHADOW_BAT_DOMINION, chr.getSkillLevel(SHADOW_BAT));
        summon.setFlyMob(true);
        summon.setMoveAbility(MoveAbility.Fly);
        summon.setAttackActive(false);
        chr.getField().spawnAddSummon(summon);
        chr.getField().broadcast(Summoned.doSkill(summon, (byte) 0, 0, null));
        dominionBats.add(summon);
    }

    private void applyDarkElementalOnMob(Mob mob, int slv, long damage) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(ElementDarkness)) {
            if (Util.succeedProp(getDarkEleProp())) {
                Option o1 = new Option();
                int amount = 1;
                MobTemporaryStat mts = mob.getTemporaryStat();
                if (mts.hasCurrentMobStat(MobStat.ElementDarkness)) {
                    amount = mts.getCurrentOptionsByMobStat(MobStat.ElementDarkness).nOption;
                    if (amount < getMaxDarkEleStack()) {
                        amount++;
                    }
                }
                // if dominion is active, instantly gain max stack
                if (tsm.getOptByCTSAndSkill(Stance, DOMINION) != null) {
                    amount = getMaxDarkEleStack();
                }

                o1.nOption = amount;
                o1.rOption = DARK_ELEMENTAL;
                o1.tOption = 30;
                o1.xOption = amount * 7;
                mts.addStatOptions(mob, MobStat.ElementDarkness, o1);
                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, DARK_ELEMENTAL, slv, damage);
                mts.createAndAddBurnedInfo(mob, bi, DARK_ELEMENTAL);
            }
        }
    }

    private int getMaxDarkEleStack() {
        int maxStack = 0;
        for (int darkEleSkill : darkEleSkills) {
            if (chr.hasSkill(darkEleSkill)) {
                Skill skill = chr.getSkill(darkEleSkill);
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                int slv = skill.getCurrentLevel();
                maxStack += si.getValue(x, slv);
            }
        }
        return maxStack;
    }

    private Skill getDarkElementalSkill() {
        Skill skill = null;
        for (int darkEleSkill : darkEleSkills) {
            if (chr.hasSkill(darkEleSkill)) {
                skill = chr.getSkill(darkEleSkill);
            }
        }
        return skill;
    }

    private int getDarkEleProp() {
        int proc = 0;
        for (int darkEleSkill : darkEleSkills) {
            if (chr.hasSkill(darkEleSkill)) {
                Skill skill = chr.getSkill(darkEleSkill);
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                int slv = skill.getCurrentLevel();
                proc += si.getValue(prop, slv);
            }
        }
        return proc;
    }

    private void incrementSiphonVitality(TemporaryStatManager tsm) {
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        SkillInfo si = SkillData.getSkillInfoById(VITALITY_SIPHON);
        Skill skill = chr.getSkill(VITALITY_SIPHON);
        int slv = skill.getCurrentLevel();
        int amount = 1;
        if (tsm.hasStat(ElementDarkness)) {

            if (tsm.hasStat(SiphonVitality)) {
                amount = tsm.getOption(SiphonVitality).nOption;
                if (amount < getMaxSiphon()) {
                    amount++;
                }
            }

            // if dominion is active, instantly gain max stack
            if (tsm.hasStatBySkillId(DOMINION)) {
                amount = getMaxSiphon();
            }
            o1.nOption = amount;
            o1.rOption = VITALITY_SIPHON;
            o1.tOption = si.getValue(time, slv);
            tsm.sendStat(SiphonVitality, o1);
            if (amount == getMaxSiphon()) {
                SkillInfo si2 = SkillData.getSkillInfoById(14120011);
                SkillInfo si3 = SkillData.getSkillInfoById(VITALITY_SIPHON);
                o2.nOption = si2.getValue(x, slv) + (chr.hasSkill(14120049) ? si3.getValue(x, chr.getSkillLevel(14120049)) : 0);
                o2.rOption = VITALITY_SIPHON;
                o2.tOption = si.getValue(time, slv);
                tsm.sendStat(SiphonVitalityBarrier, o2, true);
                if (chr.hasSkill(14120050)) {
                    // Vitality Siphon - Reinforce
                    SkillInfo siphonInfo4 = SkillData.getSkillInfoById(14120050);
                    o3.nValue = siphonInfo4.getValue(x, chr.getSkillLevel(14120050));
                    o3.nReason = VITALITY_SIPHON;
                    o3.tTerm = si.getValue(time, slv);
                    tsm.sendStat(IndiePAD, o3, true);
                }
            }
        }
    }

    private int getMaxSiphon() {
        Skill skill = null;
        if (chr.hasSkill(VITALITY_SIPHON)) {
            skill = chr.getSkill(VITALITY_SIPHON);
        }
        return skill == null ? 0 : SkillData.getSkillInfoById(skill.getSkillId()).getValue(x, skill.getCurrentLevel());
    }

    private void reduceDarkOmenCooltime() {
        if (chr.hasSkill(DARK_OMEN)) {
            SkillInfo si = SkillData.getSkillInfoById(DARK_OMEN);
            int slv = chr.getSkillLevel(DARK_OMEN);
            if (si.getElemAttr().equalsIgnoreCase("d")) {
                chr.reduceSkillCoolTime(DARK_OMEN, si.getValue(y, slv));
            }
        }
    }

    private void placeShadowSpearAA(AttackInfo attackInfo) {
        Skill skill = chr.getSkill(SHADOW_SPEAR);
        if (skill != null) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            Rect rect = new Rect(
                    new Position(
                            -20,
                            -20),
                    new Position(
                            20,
                            20)
            );
            AffectedArea aa = AffectedArea.getPassiveAA(chr, SHADOW_SPEAR_AA_SMALL, slv);
            int randInt = new Random().nextInt(140) - 70;
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                final Position pos = mob.getPosition();
                Position randPos = new Position(pos.getX() + randInt, pos.getY() - 20);
               if (canSpawnGiantShadowSpear(pos) && !chr.hasSkillOnCooldown(SHADOW_SPEAR_AA_LARGE)) {
                    randPos = new Position(pos.getX() + randInt, pos.getY());
                    removeShadowSpearAfterGiant(pos);
                    chr.write(UserLocal.giantShadowSpearAttack(randPos));
                    chr.addSkillCooldown(SHADOW_SPEAR_AA_LARGE, si.getValue(w, slv) * 1000);
                } else {
                    if (chr.getField().findFootHoldBelow(pos) != null) {
                        aa.setPosition(randPos);
                        aa.setFh(chr.getField().findFootHoldBelow(pos).getId());
                        aa.setRect(aa.getPosition().getRectAround(rect));
                        aa.setTemplateId(SHADOW_SPEAR_AA_SMALL);
                        aa.setNoFoothold(true);
                        aa.setDuration(si.getValue(subTime, slv));
                        chr.getField().spawnAffectedArea(aa);
                    }
                }
            }
        }
    }

    private boolean canSpawnGiantShadowSpear(Position position) {
        return chr.getField().getAffectAreasInRect(position.getRectAround(new Rect(-500, -500, 500, 500))).stream()
                .filter(aa -> aa.getSkillID() == SHADOW_SPEAR_AA_SMALL && aa.getCharID() == chr.getId())
                .count() > 4;
    }

    private void removeShadowSpearAfterGiant(Position position) {
        final var field = chr.getField();
        final int cid = chr.getId();
        final var rect = position.getRectAround(SHADOW_SPEAR_REMOVE_RECT);
        for (AffectedArea aa : field.getAffectAreasInRect(rect)) {
            if (aa.getSkillID() == SHADOW_SPEAR_AA_SMALL && aa.getCharID() == cid) {
                field.removeLife(aa);
            }
        }
        setGiantShadowSpearAA(position);
    }

    private void setGiantShadowSpearAA(Position position) {
        SkillInfo si = SkillData.getSkillInfoById(SHADOW_SPEAR);
        AffectedArea aa = AffectedArea.getPassiveAA(chr, SHADOW_SPEAR_AA_LARGE, 1);
        aa.setPosition(position);
        aa.setRect(position.getRectAround(new Rect(-100, -50, 100, 50)));
        aa.setDuration(si.getValue(w, 1));
        chr.getField().spawnAffectedAreaAndRemoveOld(aa);
    }

    private AffectedArea getGiantShadowSpearAA() {
        return chr.getField().getAffectedAreas().stream().filter(aa -> aa.getSkillID() == SHADOW_SPEAR_AA_LARGE && aa.getCharID() == chr.getId()).findFirst().orElse(null);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Summon summon;
        Field field = chr.getField();
        switch (skillID) {
            case DARK_ELEMENTAL:
                if (tsm.hasStat(ElementDarkness)) {
                    tsm.removeStat(ElementDarkness);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(ElementDarkness, o1);
                }
                break;
            case DARK_SIGHT:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(DarkSight, o1);
                this.darkSightCDCount = 0;
                break;
            case DARK_SERVANT:
                applyDarkServant();
                break;
            case DARKNESS_ASCENDING:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ReviveOnce, o1);
                break;
            case SHADOW_BAT:
            case HEXA_SHADOW_BAT:
            case RAVENOUS_BAT:
            case HEXA_RAVENOUS_BAT:
                if (tsm.hasStat(NightWalkerBat)) {
                    tsm.removeStat(NightWalkerBat);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(NightWalkerBat, o1);
                }
                break;
            case GLORY_OF_THE_GUARDIANS:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case SHADOW_ILLUSION:
                if (chr.getField().getSummons().stream()
                        .anyMatch(l -> l.getOwnerId() == chr.getId() &&
                                l.getSkillID() == DARK_SERVANT)
                ) {
                    tsm.removeStatsBySkill(DARK_SERVANT);
                    c.getChr().getField().broadcast(Summoned.removed(darkServant, LeaveType.ANIMATION));
                }
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ShadowIllusion, o1);
                for (int i = skillID; i < skillID + 3; i++) {
                    summon = Summon.getSummonBy(chr, i, slv);
                    summon.setAvatarLook(chr.getAvatarData().getAvatarLook());
                    summon.setMoveAbility(MoveAbility.WalkClone);
                    summon.setAssistType(AssistType.None);
                    summon.setActionDelay((i - (skillID - 1)) * 400);
                    summon.setMovementDelay((i - (skillID - 1)) * 30);
                    summon.setFlip(true);
                    field.spawnSummon(summon);
                }
                if (chr.hasSkill(DARK_SERVANT)) {
                    chr.getTimer().addEvent(this::applyDarkServant, si.getValue(time, slv) * 1001L, TimeUnit.MILLISECONDS);
                }
                return;
            case DARK_OMEN:
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case SHADOW_SPEAR:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.xOption = 1;
                tsm.sendStat(ShadowSpear, o1);
                break;
            case SHADOW_BAT_DOMINION:
                if (dominionBats.size() < 3) {
                    summonBatByDominion();
                }
                break;
            case GREATER_DARK_SERVANT:
            case HEXA_GREATER_DARK_SERVANT:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(false);
                summon.setAvatarLook(chr.getAvatarData().getAvatarLook());
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setCount(si.getValue(w, slv));
                field.spawnSummon(summon);
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (hitInfo.hpDamage > 0 && tsm.hasStatBySkillId(DARK_SIGHT)) {
            // TODO     Check if hp% attack
            incrementDarkSightCooltime();
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    private void incrementDarkSightCooltime() {
        Skill skill = chr.getSkill(DARK_SIGHT);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int maxStack = si.getValue(y, slv);
        if (darkSightCDCount < maxStack) {
            darkSightCDCount++;
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        if (cts == ShadowIllusion) {
            applyDarkServant();
        } else if (cts == DarkSight) {
            Skill skill = chr.getSkill(DARK_SIGHT);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            chr.addSkillCooldown(skill.getSkillId(), si.getValue(cooltime, slv) * darkSightCDCount * 1000);
        }
    }
}
