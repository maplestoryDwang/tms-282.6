package net.swordie.ms.client.jobs.cygnus;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.character.tms282.Tms282BuffBook;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.enums.LeaveType;
import net.swordie.ms.enums.MoveAbility;
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

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class BlazeWizard extends Noblesse {

    public static final int ELEMENTAL_HARMONY_INT = 10000248;

    public static final int FIRE_REPULSION = 12000024;

    // Flame Elements
    public static final int FLAME_ELEMENT = 12000022;
    public static final int GREATER_FLAME_ELEMENT = 12100026;
    public static final int GRAND_FLAME_ELEMENT = 12110024;
    public static final int FINAL_FLAME_ELEMENT = 12120007;

    public static final int ORBITAL_FLAME = 12001020;
    public static final int GREATER_ORBITAL_FLAME = 12100020;
    public static final int GRAND_ORBITAL_FLAME = 12110020;
    public static final int FINAL_ORBITAL_FLAME = 12120006;

    public static final int ORBITAL_FLAME_ATOM = 12000026;
    public static final int GREATER_ORBITAL_FLAME_ATOM = 12100028;
    public static final int GRAND_ORBITAL_FLAME_ATOM = 12110028;
    public static final int FINAL_ORBITAL_FLAME_ATOM = 12120010;

    public static final int FIREWALK_HORIZONTAL = 12001028;
    public static final int FIREWALK_VERTICAL = 12001027;

    public static final int ORBITAL_EXPLOSION = 12101024; //Buff
    public static final int ORBITAL_EXPLOSION_BONUS = 12101030; //Bonus Attack
    public static final int FLASHFIRE = 12101025; //Special Skill
    public static final int WORD_OF_FIRE = 12101023; //Buff
    public static final int CONTROLLED_BURN = 12101022; //Special Skill

    public static final int CINDER_MAELSTROM = 12111022; //Special Skill
    public static final int PHOENIX_RUN = 12111023; //Special Buff
    public static final int PHOENIX_RUN_EFFECTS = 12111029;

    public static final int BLAZING_EXTINCTION = 12121001;
    public static final int BLAZING_EXTINCTION_SUMMON = 12121025;
    public static final int TOWERING_INFERNO = 12121002;
    public static final int TOWERING_INFERNO_CD = 12120023;
    public static final int BURNING_CONDUIT = 12121005;
    public static final int BURNING_CONDUIT_BUFF = 12121016;
    public static final int FIRES_OF_CREATION = 12121004; //only used for visual cooldown
    public static final int FIRES_OF_CREATION_FOX = 12120014; //Buff
    public static final int FIRES_OF_CREATION_LION = 12120013; //Buff
    public static final int FLAME_BARRIER = 12121003; //Buff
    public static final int CALL_OF_CYGNUS = 12121000; //Buff
    public static final int ORBITAL_FLAME_RANGE = 12121043; // Buff - toggle

    public static final int CATACLYSM = 12121052;
    public static final int GLORY_OF_THE_GUARDIANS = 12121053;
    public static final int PHOENIX_DRIVE = 12121054;

    // V Skills
    public static final int SAVAGE_FLAME = 400021042;
    public static final int SAVAGE_FLAME_LION = 400021043;
    public static final int SAVAGE_FLAME_FOX = 400021044;
    public static final int SAVAGE_FLAME_FOX_ATOM = 400021045;
    public static final int INFERNO_SPHERE = 400021072;
    public static final int SALAMANDER_MISCHIEF = 400021092;
    public static final int SALAMANDER_MISCHIEF_END_BUFF = 400021093;
    public static final int ORBITAL_INFERNO = 400021004;

    // HEXA Skills
    public static final int HEXA_ORBITAL_FLAME = 12141000;
    public static final int HEXA_ORBITAL_FLAME_EXTRA = 12141006;
    public static final int HEXA_BLAZING_EXTINCTION = 12141007;
    public static final int HEXA_BLAZING_EXTINCTION_SUMMON = 12141009;
    public static final int HEXA_PHOENIX_DRIVE = 12141010; //Special Skill
    public static final int HEXA_ORBITAL_EXPLOSION = 12141015; //Buff
    public static final int HEXA_ORBITAL_EXPLOSION_BONUS = 12141016; //Bonus Attack
    public static final int HEXA_TOWERING_INFERNO_ = 12141017;
    public static final int HEXA_TOWERING_INFERNO = 12141018;
    public static final int ETERNITY = 12141500;
    public static final int ETERNITY_ATTACK = 12141501;

    public static boolean used;
    private int flameCharge = 0;
    private ScheduledFuture<?> eternity;
    private Option infernoCast;
    private Field infernoField;
    private long infernoUntil;
    private int infernoLevel, infernoAttacks;
    private ScheduledFuture<?> infernoExpiry;
    private Tms282BuffBook infernoBook;

    private final int[] addedSkills = new int[]{
            ELEMENTAL_HARMONY_INT
    };

    private Summon summonFox;
    private Summon summonLion;

    public BlazeWizard(Char chr) {
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

    public static void reviveByPhoenixRun(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(PHOENIX_RUN);
        int slv = skill.getCurrentLevel();

        chr.heal(chr.getMaxHP() / 2, true); // 50%
        tsm.removeStatsBySkill(PHOENIX_RUN);

        chr.chatMessage("你已被本鳳凰技能復活。");

        Position position = chr.getPosition();
        chr.write(FieldPacket.teleport(new Position(position.getX() + (chr.isLeft() ? +350 : -350), position.getY()), chr));

        // Hit effect
        chr.write(UserPacket.effect(Effect.skillUse(PHOENIX_RUN_EFFECTS, chr.getLevel(), slv)));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(PHOENIX_RUN_EFFECTS, chr.getLevel(), slv)), chr);

        // Backstep effect
        chr.write(UserPacket.effect(Effect.skillAffected(PHOENIX_RUN_EFFECTS, chr.getLevel(), slv)));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffected(PHOENIX_RUN_EFFECTS, chr.getLevel(), slv)), chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isBlazeWizard(id);
    }

    public static boolean isPhoenixDrive(int skillId) {
        return skillId == PHOENIX_DRIVE || skillId == HEXA_PHOENIX_DRIVE;
    }

    /** CP04B9 owns this cast. Keep the server's existing release-time cooldown policy. */
    public synchronized boolean prepareInferno(Tms282BuffBook book, long now) {
        if (!Tms282RemoteBuffs.active()) return false;
        Objects.requireNonNull(book);
        if (infernoCast != null) {
            if (validInferno(infernoCast, now)) return false;
            endInferno(infernoCast);
        }
        int level = chr.getSkillLevel(INFERNO_SPHERE);
        SkillInfo info = SkillData.getSkillInfoById(INFERNO_SPHERE);
        var field = chr.getField();
        if (level <= 0 || info == null || field == null || chr.getHP() <= 0 || !isHandlerOfJob(chr.getJob())
                || (field.getFieldLimit() & (net.swordie.ms.enums.FieldOption.SkillLimit.getVal()
                    | net.swordie.ms.enums.FieldOption.MoveSkillOnly.getVal())) != 0
                || chr.hasSkillOnCooldown(INFERNO_SPHERE) || book.coolingDown(INFERNO_SPHERE, now)
                || info.getValue(updatableTime, level) <= 0) return false;
        if (!chr.applyMpCon(info, level, true)) return false;
        Option marker = new Option(); marker.nValue = 1; marker.nReason = chr.getJob();
        // The native process deadline uses updatableTime (6000ms); time=5 is the client hold description.
        marker.tTerm = info.getValue(updatableTime, level); marker.setInMillis(true);
        infernoCast = marker; infernoField = field; infernoLevel = level; infernoBook = book;
        infernoUntil = now + marker.tTerm;
        infernoAttacks = 40; // Current WZ common/s6, constant at every level; absent from the legacy SkillStat enum.
        // IndieKeyDownTime has no verified282 bit. This token remains server-owned; no invented CTS is emitted.
        marker.startTime = now;
        Option reduction = new Option(); reduction.nReason = INFERNO_SPHERE;
        reduction.nValue = 50; // Current WZ common/s5; native indie52 multiplies incoming damage by0.5.
        reduction.tTerm = info.getValue(time, level) * 1000; reduction.setInMillis(true);
        chr.getTemporaryStatManager().sendStat(IndieDamReduceR, reduction);
        infernoExpiry = chr.getTimer().addEvent(() -> endInferno(marker), marker.tTerm);
        field.broadcast(UserRemote.skillPrepare(chr, INFERNO_SPHERE, level), chr);
        return true;
    }

    public synchronized Option claimInferno(AttackInfo attack, long now) {
        if (!validInferno(infernoCast, now)) { endInferno(infernoCast); return null; }
        SkillInfo info = SkillData.getSkillInfoById(INFERNO_SPHERE);
        int hits = info.getValue(attackCount, infernoLevel), mobs = info.getValue(mobCount, infernoLevel);
        if (attack.skillId != INFERNO_SPHERE || attack.inHeader != net.swordie.ms.handlers.header.InHeader.USER_MAGIC_ATTACK
                || infernoAttacks <= 0 || Byte.toUnsignedInt(attack.hits) > hits || attack.mobCount > mobs
                || attack.mobAttackInfo.size() > mobs
                || attack.mobAttackInfo.stream().anyMatch(m -> m.damages == null || m.damages.length > hits)) return null;
        infernoAttacks--;
        return infernoCast;
    }

    public synchronized boolean validInferno(Option marker, long now) {
        return marker != null && marker == infernoCast && chr.getHP() > 0 && chr.getField() == infernoField
                && chr.getSkillLevel(INFERNO_SPHERE) > 0 && isHandlerOfJob(chr.getJob())
                && now >= marker.startTime && now < infernoUntil;
    }

    public synchronized void endInferno(Option marker) {
        if (marker == null || marker != infernoCast) return;
        infernoCast = null;
        if (infernoExpiry != null) { infernoExpiry.cancel(false); infernoExpiry = null; }
        chr.getTemporaryStatManager().removeStatsBySkill(INFERNO_SPHERE);
        // Same policy as Job.handleCancelKeyDownSkill: one cooldown at stop, including forced cleanup.
        chr.setSkillCooldown(INFERNO_SPHERE, infernoLevel);
        infernoBook.startCooldown(INFERNO_SPHERE, chr.getSkillCoolTimes().getOrDefault(INFERNO_SPHERE, 0L));
        infernoBook = null;
        if (chr.getField() != null) chr.getField().broadcast(UserRemote.skillCancel(chr.getId(), INFERNO_SPHERE), chr);
        infernoField = null;
    }

    public static void stopInferno(Char chr) {
        if (Tms282RemoteBuffs.active() && chr != null && chr.getJobHandler() instanceof BlazeWizard blaze) {
            synchronized (blaze) { blaze.endInferno(blaze.infernoCast); }
        }
    }

    /** Current Phoenix attacks continue the already-paid CTS443 cast; they are not fresh skill uses. */
    public boolean canAttackWithPhoenix(AttackInfo attack, Option cast, long now) {
        Option active = chr.getTemporaryStatManager().getOptByCTSAndSkill(DevilishPower, attack.skillId);
        int level = chr.getSkillLevel(attack.skillId);
        SkillInfo info = SkillData.getSkillInfoById(attack.skillId);
        if (!isPhoenixDrive(attack.skillId) || attack.inHeader != net.swordie.ms.handlers.header.InHeader.USER_MELEE_ATTACK
                || level <= 0 || info == null || chr.getHP() <= 0 || !isHandlerOfJob(chr.getJob())
                || active == null || active != cast || active.rOption != attack.skillId || active.nOption == 0
                || now < active.startTime
                || now >= active.startTime + active.tOption * (active.isInMillis ? 1L : 1000L)) return false;
        int hits = info.getValue(attackCount, level), mobs = info.getValue(mobCount, level);
        return Byte.toUnsignedInt(attack.hits) <= hits && attack.mobCount <= mobs
                && attack.mobAttackInfo.size() <= mobs
                && attack.mobAttackInfo.stream().allMatch(m -> m.damages != null && m.damages.length <= hits);
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        for (Option removeOpt : options) {
            if (removeOpt == null) {
                continue;
            }
            int skillId = cts.isIndie() ? removeOpt.nReason : removeOpt.rOption;
            switch (skillId) {
                case FIRES_OF_CREATION_FOX:
                case FIRES_OF_CREATION_LION:
                    removeFiresOfCreationSummon(c, skillId);
                    break;
            }
        }
        super.handleRemoveCTS(cts, options);
    }

    private void removeFiresOfCreationSummon(Client c, int skillID) {
        Summon summon = skillID == FIRES_OF_CREATION_FOX ? summonFox : summonLion;

        if (summon != null) {
            Field field = chr.getField();
            field.broadcast(Summoned.removed(summon, LeaveType.ANIMATION));
        }
    }

    private void summonFlameElement() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = getFlameElement();
        Option o1 = new Option();
        Skill skill = chr.getSkill(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        if (si != null) {
            int slv = skill.getCurrentLevel();
            Field field = chr.getField();
            o1.nOption = si.getValue(x, slv);
            o1.rOption = skillID;
            o1.tOption = si.getValue(time, slv);
            tsm.sendStat(ElementFlameBuff, o1);
            if (field.getSummonBySkillId(chr, skillID) == null) {
                Summon summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setAssistType(AssistType.None);
                field.spawnSummon(summon);
                chr.write(UserLocal.flameWizardElementFlameSummon());
            }
        }
    }

    private int getFlameElement() {
        int skill = 0;
        if (chr.hasSkill(FLAME_ELEMENT)) {
            skill = FLAME_ELEMENT;
        }
        if (chr.hasSkill(GREATER_FLAME_ELEMENT)) {
            skill = GREATER_FLAME_ELEMENT;
        }
        if (chr.hasSkill(GRAND_FLAME_ELEMENT)) {
            skill = GRAND_FLAME_ELEMENT;
        }
        if (chr.hasSkill(FINAL_FLAME_ELEMENT)) {
            skill = FINAL_FLAME_ELEMENT;
        }
        return skill;
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        MobTemporaryStat mts = mob.getTemporaryStat();
        Option o1 = new Option();
        switch (skillID) {
            case CINDER_MAELSTROM:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Freeze, o1);
                }
                break;
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        super.handleAttack(c, attackInfo, si, now);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        int slv = attackInfo.slv;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        if (hasHitMobs) {
            handleOrbitalExplosion(attackInfo);
            if (skillID == SALAMANDER_MISCHIEF) {
                increaseSalamanderHitCount();
            }
            if (attackInfo.skillId != SALAMANDER_MISCHIEF && chr.getField().getSummonBySkillId(chr, SALAMANDER_MISCHIEF) != null) {
                var targetId = Util.getRandomFromCollection(attackInfo.mobAttackInfo).mobId;
                createSalamanderSecondAtom(targetId);
            }
            if (SkillConstants.isHexaOrbitalFlame(skillID) && !chr.hasSkillOnCooldown(HEXA_ORBITAL_FLAME_EXTRA)) {
                var o = tsm.getOption(FlameOrbMultiAttack);
                o.nOption += 1;
                if (o.nOption >= 3) {
                    o.nOption = 0;
                    ExtraSkill extraSkill = new ExtraSkill(HEXA_ORBITAL_FLAME_EXTRA, chr.getPosition());
                    extraSkill.FaceLeft = chr.isLeft() ? 1 : 0;
                    extraSkill.Value = 1;
                    chr.write(UserLocal.registerExtraSkill(HEXA_ORBITAL_FLAME, Collections.singletonList(extraSkill)));
                    chr.addSkillCooldown(HEXA_ORBITAL_FLAME_EXTRA, 15000);
                }
                tsm.sendStat(FlameOrbMultiAttack, o);
            }
        }
        switch (skillID) {
            case ORBITAL_FLAME_ATOM:
            case GREATER_ORBITAL_FLAME_ATOM:
            case GRAND_ORBITAL_FLAME_ATOM:
            case FINAL_ORBITAL_FLAME_ATOM:
                summonFlameElement();
                break;
            case FIRES_OF_CREATION_LION:
                chr.setSkillCooldown(SAVAGE_FLAME, chr.getSkillLevel(SAVAGE_FLAME));
                updateFlameCharge(0);
                chr.dispose();
                break;
            case INFERNO_SPHERE:
                if (!Tms282RemoteBuffs.active()) decreasePhoenixFeather();
                break;
        }
    }

    private void handleOrbitalExplosion(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(ORBITAL_EXPLOSION_BONUS);
        if (tsm.hasStat(OrbitalExplosion) && si.getSkillList1().contains(attackInfo.skillId)) {
            Option o = tsm.getOption(OrbitalExplosion);
            o.xOption += 1;
            if (o.xOption >= 10) {
                List<Mob> mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
                int bonusID = chr.hasSkill(HEXA_ORBITAL_EXPLOSION) ? HEXA_ORBITAL_EXPLOSION_BONUS : ORBITAL_EXPLOSION_BONUS;
                chr.write(UserLocal.userBonusAttackRequest(bonusID, mobs));
                o.xOption = 0;
            }
            tsm.sendStat(OrbitalExplosion, o);
            updateFlameCharge(flameCharge + 1);
        }
    }

    private void updateFlameCharge(int stack) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(SAVAGE_FLAME);
        int slv = chr.getSkillLevel(SAVAGE_FLAME);
        if (!chr.hasSkill(SAVAGE_FLAME)) {
            return;
        }
        int maxFlameCharge = si.getValue(y, slv);
        Option o = new Option();

        o.nOption = tsm.hasStat(FlameDischarge) ? 1 : 0;
        o.rOption = chr.hasSkill(HEXA_ORBITAL_EXPLOSION) ? HEXA_ORBITAL_EXPLOSION : ORBITAL_EXPLOSION;
        o.xOption = stack < 0 ? 0 : Math.min(stack, maxFlameCharge);
        o.setInMillis(true);
        tsm.sendStat(FlameDischarge, o);
        flameCharge = o.xOption;
    }

    public void increasePhoenixFeather() {
        if (Tms282RemoteBuffs.active()) return; // Current WZ has no feather resource or corresponding CTS.
        var tsm = chr.getTemporaryStatManager();
        int skillID = INFERNO_SPHERE;
        int slv = chr.getSkillLevel(skillID);
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        var opt = tsm.getOption(FlameWizardInfiniteFlame);
        opt.nOption = 1;
        opt.rOption = chr.getJob();
        opt.xOption++;
        opt.xOption = Math.max(0, Math.min(opt.xOption, si.getValue(x, slv)));
        tsm.sendStat(FlameWizardInfiniteFlame, opt);
        chr.write(WvsContext.updateSkillStackRequestResult(skillID, (byte) 1));
    }

    private void decreasePhoenixFeather() {
        var tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(FlameWizardInfiniteFlame)) {
            return;
        }
        var opt = tsm.getOption(FlameWizardInfiniteFlame);
        opt.nOption = 1;
        opt.rOption = chr.getJob();
        opt.xOption -= 1;
        tsm.sendStat(FlameWizardInfiniteFlame, opt);
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
        Summon summon;
        Option o1 = new Option();
        Option o2 = new Option();
        switch (skillID) {
            case WORD_OF_FIRE:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Booster, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieMad, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieMAD, o2);
                tsm.sendStat(newStats);
                break;
            case FLAME_BARRIER:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(AntiMagicShell, o1);
                break;
            case ORBITAL_EXPLOSION:
            case HEXA_ORBITAL_EXPLOSION:
                if (tsm.hasStat(OrbitalExplosion)) {
                    tsm.removeStat(OrbitalExplosion);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    tsm.sendStat(OrbitalExplosion, o1);
                }
                break;
            case PHOENIX_DRIVE:
            case HEXA_PHOENIX_DRIVE:
                if (Tms282RemoteBuffs.active()) {
                    // Native5A13B53 reads CTS443 n/r into the sustained-attack slot; DragonSlave no longer exists.
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    tsm.sendStat(DevilishPower, o1);
                    break;
                }
                o1.nOption = si.getValue(v, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(DragonSlave, o1);
                break;
            case FIRES_OF_CREATION_FOX:
            case FIRES_OF_CREATION_LION:
                if (chr.hasSkillOnCooldown(FIRES_OF_CREATION_FOX) || chr.hasSkillOnCooldown(FIRES_OF_CREATION_LION)) {
                    break;
                }

                var focSi = SkillData.getSkillInfoById(FIRES_OF_CREATION);

                tsm.removeStatsBySkill(skillID);
                Field field = chr.getField();

                if (summonFox != null) {
                    field.broadcast(Summoned.removed(summonFox, LeaveType.ANIMATION));
                }

                if (summonLion != null) {
                    field.broadcast(Summoned.removed(summonLion, LeaveType.ANIMATION));
                }

                chr.setSkillCooldown(FIRES_OF_CREATION, slv); // to display cooldown in quickslot
                chr.setSkillCooldown(FIRES_OF_CREATION_FOX, slv);
                chr.setSkillCooldown(FIRES_OF_CREATION_LION, slv);

                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setMoveAction((byte) 4);
                summon.setFlyMob(skillID == FIRES_OF_CREATION_FOX);
                summon.setMoveAbility(MoveAbility.WalkSmart);
                summon.setAssistType(AssistType.AttackCounter);
                // i have to specify the summon term as the _FOX/LION skills have the time set to 0, making the summon last forever!
                summon.setSummonTerm(focSi.getValue(time, slv));
                field.spawnSummon(summon);

                if (skillID == FIRES_OF_CREATION_FOX) {
                    summonFox = summon;
                } else {
                    summonLion = summon;
                }
                o1.nOption = focSi.getValue(y, slv);
                o1.rOption = skillID;
                o1.tOption = focSi.getValue(time, slv);
                tsm.sendStat(IgnoreTargetDEF, o1);
                break;
            case PHOENIX_RUN:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(FlareTrick, o1);
                break;
            case GLORY_OF_THE_GUARDIANS:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case ORBITAL_FLAME_RANGE:
                if (tsm.hasStat(AddRangeOnOff)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = si.getValue(range, slv);
                    o1.rOption = skillID;
                    tsm.sendStat(AddRangeOnOff, o1);
                }
                break;
            case FLASHFIRE:
                Position flamepos = inPacket.decodePosition();
                inPacket.skipInt();
                inPacket.skipByte();
                boolean isReplaceOrCreate = inPacket.decodeByte() == 0;
                if (isReplaceOrCreate) {
                    c.write(WvsContext.flameWizardFlareBlink(chr, flamepos, false));
                } else {
                    chr.write(FieldPacket.teleport(flamepos, chr));
                }
                break;
            case CONTROLLED_BURN:
                int healmp = si.getValue(x, slv);
                int healpercent = (chr.getMaxMP() * healmp) / 100;
                chr.healMP(healpercent);
                break;
            case BURNING_CONDUIT:
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                aa.setDelay((short) 15);
                chr.getField().spawnAffectedArea(aa);
                break;
            case FIREWALK_HORIZONTAL:
            case FIREWALK_VERTICAL:
                chr.getField().broadcast(WvsContext.flameWizardFlameWalkEffect(chr));
                break;
            case CATACLYSM:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = si.getValue(y, slv);
                o1.setInMillis(true);
                tsm.sendStat(IndieNotDamaged, o1);
                break;
            case SALAMANDER_MISCHIEF:
                createSalamanderSummon(slv);
                break;
            case SAVAGE_FLAME_FOX:
                doFoxSavageFlameAttack();
                break;
            case BLAZING_EXTINCTION_SUMMON:
            case HEXA_BLAZING_EXTINCTION_SUMMON:
                slv = skillID == HEXA_BLAZING_EXTINCTION_SUMMON ? HEXA_BLAZING_EXTINCTION : BLAZING_EXTINCTION;
                summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setMoveAction((byte) 4);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.Attack);
                chr.getField().spawnSummon(summon);
                break;
            case ETERNITY:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(SixthPhoenix, o1);
                if (eternity != null) {
                    eternity.cancel(false);
                }
                eternity = chr.getTimer().addEvent(this::doEternity, 1000);
                break;
        }
    }

    private void doEternity() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(SixthPhoenix)) {
            chr.write(UserLocal.userBonusAttackRequest(ETERNITY_ATTACK));
            eternity = chr.getTimer().addEvent(this::doEternity, 3000);
        }
    }

    private void increaseSalamanderHitCount() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        if (tsm.hasStat(FWSalamanderMischiefAttack)) {
            var si = SkillData.getSkillInfoById(SALAMANDER_MISCHIEF);
            var slv = chr.getSkillLevel(SALAMANDER_MISCHIEF);
            var maxCount = si.getValue(z, slv);
            var o = tsm.getOption(FWSalamanderMischiefAttack);

            o.nOption += 1;
            o.nOption = Math.min(maxCount, o.nOption);
            tsm.updateStat(FWSalamanderMischiefAttack, o);
        } else {
            Option o = new Option();
            o.nOption = 1;
            o.rOption = SALAMANDER_MISCHIEF;
            o.tOption = 60000;
            o.setInMillis(true);
            tsm.sendStat(FWSalamanderMischiefAttack, o);
        }
    }

    private void createSalamanderSummon(int slv) {
        Summon summon = Summon.getSummonBy(chr, SALAMANDER_MISCHIEF, slv);
        chr.getField().spawnSummon(summon);
    }

    private void createSalamanderSecondAtom(int targetId) {
        var skillId = SALAMANDER_MISCHIEF;
        if (!chr.hasSkill(skillId)) {
            return;
        }

        Summon summon = chr.getField().getSummonBySkillId(chr, skillId);

        if (summon == null || summon.isHide()) {
            return;
        }

        var si = SkillData.getSkillInfoById(skillId);
        var sai = si.getSecondAtomInfos().get(0);

        var sa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), targetId, 0,
                si.getSkillId(), chr.getPosition(), System.currentTimeMillis());

        chr.createSecondAtom(sa);

        summon.setHide(true);
        chr.getField().broadcast(Summoned.doSkill(summon, (byte) 0, 0, null));
    }

    public void doFoxSavageFlameAttack() {
        createSavageFlameForceAtoms();
        chr.setSkillCooldown(SAVAGE_FLAME, chr.getSkillLevel(SAVAGE_FLAME));
        updateFlameCharge(0);
    }

    private void createSavageFlameForceAtoms() {
        SkillInfo si = SkillData.getSkillInfoById(SAVAGE_FLAME);
        Field field = chr.getField();
        int slv = chr.getSkillLevel(SAVAGE_FLAME);
        int flameChargeOverReq = flameCharge - 6;
        Rect rect = chr.getPosition().getRectAround(SkillData.getSkillInfoById(SAVAGE_FLAME_FOX).getFirstRect());
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        if (flameChargeOverReq < 0) {
            return;
        }
        int atomsCreated = 8;
        int maxRecreations = 1 + (flameChargeOverReq * 2);
        ForceAtomEnum fae = ForceAtomEnum.FLAME_DISCHARGE;
        for (int i = 0; i < atomsCreated; i++) {
            List<Integer> targetList = new ArrayList<>();
            List<ForceAtomInfo> faiList = new ArrayList<>();
            Mob mob;
            if (!field.getMobsInRect(rect).isEmpty()) {
                mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
            } else {
                mob = null;
            }
            if (mob != null) {
                targetList.add(mob.getObjectId());
            } else {
                targetList.add(0);
            }
            int fImpact = new Random().nextInt(4) + 10;
            int sImpact = new Random().nextInt(2) + 4;
            int angle = new Random().nextInt(20) + ((chr.isLeft() ? -260 : 260) - 10);
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, sImpact,
                    angle, 750, Util.getCurrentTime(), 0, 0,
                    new Position());
            faiList.add(fai);
            ForceAtom fa = new ForceAtom(false, chr.getId(), chr.getId(), fae,
                    true, targetList, SAVAGE_FLAME_FOX_ATOM, faiList, new Rect(), 0, 0,
                    new Position(), 0, new Position(), 0);
            fa.setMaxRecreationCount(maxRecreations);
            chr.createForceAtom(fa);
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        if (chr.hasSkill(FIRE_REPULSION)) {
            if (chr.getMP() > 0) {
                Skill skill = chr.getSkill(FIRE_REPULSION);
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                int dmgPerc = si.getValue(x, skill.getCurrentLevel());
                int dmg = hitInfo.hpDamage;
                int mpDmg = (int) (dmg * (dmgPerc / 100D));
                mpDmg = chr.getMP() - mpDmg < 0 ? chr.getMP() : mpDmg;
                hitInfo.hpDamage = dmg - mpDmg;
                hitInfo.mpDamage = mpDmg;
            }
        }

        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        stopInferno(chr);
        super.handleCancelTimer(chr);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case FIRES_OF_CREATION_LION,
                 FIRES_OF_CREATION_FOX -> {
                chr.setSkillCooldown(FIRES_OF_CREATION, chr.getSkillLevel(FIRES_OF_CREATION));
                return 1;
            }
            case HEXA_PHOENIX_DRIVE -> {
                int skillID = PHOENIX_DRIVE;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_BLAZING_EXTINCTION-> {
                int skillID = BLAZING_EXTINCTION;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_TOWERING_INFERNO_ -> {
                int skillID = TOWERING_INFERNO;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_TOWERING_INFERNO -> {
                int skillID = TOWERING_INFERNO_CD;
                int slv = chr.getSkillLevel(TOWERING_INFERNO);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }

    @Override
    public boolean handleSecondAtomRemoveRequest(int objectId) {
        Summon summon = chr.getField().getSummonBySkillId(chr, SALAMANDER_MISCHIEF);
        if (summon != null) {
            summon.setHide(false);
            chr.getField().broadcast(Summoned.doSkill(summon, (byte) 0, 0, null));
        }

        return super.handleSecondAtomRemoveRequest(objectId);
    }
}
