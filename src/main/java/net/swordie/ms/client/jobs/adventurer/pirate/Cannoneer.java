package net.swordie.ms.client.jobs.adventurer.pirate;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Cannoneer extends Job {

    public static final int MAPLE_RETURN = 1281;
    public static final int DASH = 5001005; //Buff

    public static final int BLAST_BACK = 5011002; //Special Attack
    public static final int MONKEY_COMPANION = 5011007;
    public static final int MONKEY_MAGIC = 5301003; //Buff

    public static final int MONKEY_WAVE = 5311002; //Special Attack
    public static final int BARREL_ROULETTE = 5311004; //Buff
    public static final int MONKEY_FURY = 5311010;
    public static final int MONKEY_MORTAR = 5311013;
    public static final int MONKEY_MORTAR_MANUAL = 5311015;
    public static final int LUCK_OF_THE_DIE = 5311005; //Buff

    public static final int LUCK_OF_THE_DIE_DD = 5320007;
    public static final int ANCHOR_AWEIGH = 5321003; //Summon
    public static final int MONKEY_MALITIA = 5321004; //Summon
    public static final int NAUTILUS_STRIKE = 5321001; //Special Attack / Buff
    public static final int PIRATE_SPIRIT = 5321010; //Buff
    public static final int MAPLE_WARRIOR = 5321005; //Buff
    public static final int HEROS_WILL = 5321006;
    public static final int MEGA_MONKEY_MAGIC = 5320008;
    public static final int EPIC_ADVENTURER = 5321053;
    public static final int BUCKSHOT = 5321054;
    public static final int ROLLING_RAINBOW = 5321052;

    // V skills
    public static final int PIRATES_BANNER = 400001017;

    public static final int SPECIAL_MONKEY_SIDEKICK_3 = 400051053;
    public static final int SPECIAL_MONKEY_SIDEKICK_2 = 400051052;
    public static final int SPECIAL_MONKEY_SIDEKICK = 400051038;
    public static final int BIG_HUGE_GIGANTIC_ROCKET = 400051008;
    public static final int NUCLEAR_OPTION_TILE = 400051026;
    public static final int NUCLEAR_OPTION_EXPLOSION = 400051025;
    public static final int NUCLEAR_OPTION = 400051024;
    public static final int POOLMAKER = 400051074;
    public static final int POOLMAKER_ATTACK = 400051075;
    public static final int POOLMAKER_AA = 400051076;
    public static final int POOLMAKER_CHEST_BUFF = 400051077;

    // HEXA Skills
    public static final int HEXA_MONKEY_MORTAR = 5341002;
    public static final int HEXA_ANCHOR_AWEIGH = 5341005; //Summon
    public static final int HEXA_NAUTILUS_STRIKE = 5341006; //Special Attack / Buff
    public static final int HEXA_MONKEY_MALITIA = 5341009; //Summon
    public static final int HEXA_MONKEY_FURY = 5341012; //Summon
    public static final int HEXA_ROLLING_RAINBOW = 5341014;
    public static final int HEXA_ROLLING_RAINBOW_SUMMON = 5341007;

    // HEXA Boosts
    public static final int HEXA_POOLMAKER = 500061029;
    public static final int HEXA_POOLMAKER_ATTACK = 500061030;
    public static final int HEXA_POOLMAKER_AA = 500061031;
    public static final int HEXA_POOLMAKER_CHEST_BUFF = 500061032;

    private int poolMakerRemainCount = 0;
    private long poolMakerStartTime = 0L;
    private int poolMakerCoolTime = 0;

    private static final List<Integer> specialMonkeySideKickIds = new ArrayList<Integer>() {{
        add(SPECIAL_MONKEY_SIDEKICK);
        add(SPECIAL_MONKEY_SIDEKICK_2);
        add(SPECIAL_MONKEY_SIDEKICK_3);
    }};

    private final int[] addedSkills = new int[]{
            MAPLE_RETURN,};

    private ScheduledFuture<?> rollingRainbowTimer;
    private boolean monkeyVisible;

    public boolean isMonkeyVisible() {
        return tms282() && monkeyVisible;
    }

    private static boolean tms282() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    public Cannoneer(Char chr) {
        super(chr);
        // Char.setJob constructs the replacement before assigning it; keep this character's visible companion.
        if (tms282() && isHandlerOfJob(chr.getJob()) && chr.getJobHandler() instanceof Cannoneer previous
                && previous.chr == chr) {
            monkeyVisible = previous.monkeyVisible;
        }
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
            // 282's visible fixed-level monkey skill is absent from older character records.
            // This constructor is shared by first-login loading and Char.setJob advancement.
            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && !chr.hasSkill(5011007)) {
                Skill monkey = SkillData.getSkillDeepCopyById(5011007);
                if (monkey != null && monkey.getCurrentLevel() == 1) {
                    chr.addSkill(monkey);
                }
            }
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isCannoneer(id);
    }

    public void giveBarrelRouletteBuff(int roulette) {   //TODO
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        Skill skill = chr.getSkill(BARREL_ROULETTE);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        chr.write(UserPacket.effect(Effect.skillAffectedSelect(BARREL_ROULETTE, slv, roulette, false)));
        switch (roulette) {
            case 1: // Extra Attack (Final Attack)
                //Handled, See Final Attack Handler
                break;
            case 2: // Max CritDmg
                o.nValue = si.getValue(s, slv);
                o.nReason = skill.getSkillId();
                o.tTerm = si.getValue(time, slv);
                tsm.sendStat(EnrageCrDamMin, o);
                break;
            case 3: // Slow Debuff
                //Handled, See Attack Handler
                break;
            case 4: // DoT
                //Handled, See Attack Handler
                break;
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        // 282 reworked 5311004 into Slumber: an on/off, on-hit slow, including bosses.
        // Native 59B2248 tests the same Roulette CTS getter used by bit445 (12C5620).
        if (tms282() && chr.getTemporaryStatManager().hasStat(Roulette)
                && chr.getTemporaryStatManager().getOption(Roulette).rOption == BARREL_ROULETTE) {
            int sleepLevel = chr.getSkillLevel(BARREL_ROULETTE);
            SkillInfo sleep = SkillData.getSkillInfoById(BARREL_ROULETTE);
            if (sleepLevel > 0 && sleep != null && Util.succeedProp(sleep.getValue(w, sleepLevel))) {
                Option slow = new Option();
                slow.nOption = sleep.getValue(u, sleepLevel);
                slow.rOption = BARREL_ROULETTE;
                slow.tOption = sleep.getValue(v, sleepLevel);
                mts.addStatOptions(mob, MobStat.Speed, slow);
            }
        }
        switch (skillID) {
            case BLAST_BACK:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(z, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Speed, o1);
                }
                break;
            case MONKEY_WAVE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case MONKEY_FURY:
            case HEXA_MONKEY_FURY:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nValue = 1;
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(y, slv);
                    mts.addStatOptions(mob, MobStat.IndieTotalDam, o1);
                    BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
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
            // Monkey Wave Ignore KeyDown Time
            if (chr.hasSkill(MONKEY_WAVE)) {
                Skill mwskill = chr.getSkill(MONKEY_WAVE);
                SkillInfo mwsi = SkillData.getSkillInfoById(MONKEY_WAVE);
                byte mwslv = (byte) mwskill.getCurrentLevel();
                if (Util.succeedProp(mwsi.getValue(w, mwslv)) && !(tsm.getOption(KeyDownTimeIgnore).nOption > 0) && skillID != 5310008) {
                    Option o1 = new Option();
                    o1.nOption = 1;
                    o1.rOption = 5310008;
                    o1.tOption = 15; // doesn't have an assigned skillStat
                    tsm.sendStat(KeyDownTimeIgnore, o1);
                }
            }
            if (skillID == POOLMAKER_ATTACK || skillID == HEXA_POOLMAKER_ATTACK) {
                handlePoolMaker(attackInfo, now);
            }
         }
        switch (skillID) {
            case MONKEY_WAVE:
                if (tsm.hasStat(KeyDownTimeIgnore) && tsm.getOption(KeyDownTimeIgnore).nOption > 0) {
                    tsm.removeStatsBySkill(5310008);
                    tsm.removeStat(KeyDownTimeIgnore);
                }
                break;
        }
    }

    private void handlePoolMaker(AttackInfo attackInfo, long now) {
        int skillID = attackInfo.skillId;
        int originalSkillID = skillID - 1;
        int aaSkillID = skillID + 1;
        SkillInfo aaSi = SkillData.getSkillInfoById(aaSkillID);
        if (chr.getField().getAffectedAreasBySkillID(chr.getId(), aaSkillID).size() < 2) {
            AffectedArea aa = AffectedArea.getAffectedArea(chr, aaSkillID, chr.getSkillLevel(originalSkillID));
            var mob = Util.getRandomFromCollection(chr.getField().getMobs(attackInfo.mobAttackInfo));
            aa.setPosition(mob != null ? mob.getPosition() : chr.getPosition());
            aa.setRect(aa.getPosition().getRectAround(aaSi.getRects().getFirst()));
            chr.getField().spawnAffectedArea(aa);
        }
        this.poolMakerRemainCount -= 1;
        long elapsed = now - poolMakerStartTime;
        int remain = (int) (poolMakerCoolTime - elapsed);
        if (poolMakerRemainCount <= 0 || remain <= 0) {
            attackInfo.poolmakerEnabled = false;
            chr.write(UserLocal.poolMakerRequest(0, 0, 0));
        } else {
            attackInfo.poolmakerEnabled = true;
            chr.write(UserLocal.poolMakerRequest(originalSkillID, poolMakerRemainCount, remain));
        }
    }

    public void increaseMonkeyMortar() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(MONKEY_MORTAR)) {
            Option o = tsm.getOption(CannonShooter_MiniCannonBall);
            o.nOption = Math.min(5, o.nOption + 1);
            o.rOption = chr.hasSkill(HEXA_MONKEY_MORTAR) ? HEXA_MONKEY_MORTAR : MONKEY_MORTAR;
            tsm.sendStat(CannonShooter_MiniCannonBall, o);
            chr.write(WvsContext.updateSkillStackRequestResult(-1, (byte) 1));
        }
    }

    public void increaseRollingRainbow() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(HEXA_ROLLING_RAINBOW)) {
            Option o = tsm.getOption(CannonShooter_RollingCannon);
            o.nOption = Math.min(2, o.nOption + 1);
            o.rOption = HEXA_ROLLING_RAINBOW;
            tsm.sendStat(CannonShooter_RollingCannon, o);
            if (this.rollingRainbowTimer != null) {
                this.rollingRainbowTimer.cancel(false);
            }
            this.rollingRainbowTimer = chr.getTimer().addEvent(() -> chr.write(WvsContext.updateSkillStackRequestResult(HEXA_ROLLING_RAINBOW, (byte) 1)), 60000);
        }
    }

    private void applyBarrelRouletteDebuffOnMob(Mob mob, long damage) {
        if (chr.hasSkill(BARREL_ROULETTE)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o = new Option();
            Skill skill = chr.getSkill(BARREL_ROULETTE);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            if (tsm.hasStat(Roulette)) {
                if (Util.succeedProp(si.getValue(w, slv))) {
                    BurnedInfo bi = BurnedInfo.createBurnInfo(chr, BARREL_ROULETTE, slv, damage);
                    MobTemporaryStat mts = mob.getTemporaryStat();
                    if (tsm.hasStat(Roulette) && tsm.getOption(Roulette).nOption == 4) {  //DoT Debuff
                        mts.createAndAddBurnedInfo(mob, bi, BARREL_ROULETTE);
                    } else if (tsm.hasStat(Roulette) && tsm.getOption(Roulette).nOption == 3) {  //Slow Debuff
                        int slowProc = si.getValue(w, slv);
                        if (Util.succeedProp(slowProc)) {
                            o.nOption = -20;
                            o.rOption = skill.getSkillId();
                            o.tOption = si.getValue(v, slv);
                            mts.addStatOptions(mob, MobStat.Speed, o);
                        }
                    }
                }
            }
        }
    }

    public void incVSkillStackBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 1;
        count = tsm.getOption(CannonShooter_BFCannonBall).nOption;
        if (count < 2) {
            count++;
        }
        updateVSkillStackBuff(chr, count);
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
        AffectedArea aa;
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        Option o6 = new Option();
        Option o7 = new Option();
        switch (skillID) {
            case MONKEY_COMPANION:
                if (tms282()) {
                    monkeyVisible = !monkeyVisible;
                    chr.getField().broadcast(Tms282CannoneerPackets.monkey(chr.getId(), monkeyVisible));
                }
                break;
            case PIRATES_BANNER:
                aa = AffectedArea.getAffectedArea(chr, skillID, slv);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                aa.setMobOrigin((byte) 0);
                aa.setOption(1);
                chr.getField().spawnAffectedArea(aa);
                break;
            case DASH:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Speed, o1);
                o2.nOption = si.getValue(y, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Jump, o2);
                tsm.sendStat(newStats);
                break;
            case MONKEY_MORTAR:
            case HEXA_MONKEY_MORTAR:
                o1 = tsm.getOption(CannonShooter_MiniCannonBall);
                o1.nOption = Math.max(0, o1.nOption - 1);
                o1.rOption = skillID;
                tsm.sendStat(CannonShooter_MiniCannonBall, o1);
                super.handleExtraSkill(si, skillID, null);
                break;
            case LUCK_OF_THE_DIE: {
                int upbound = 6;
                int diceThrow1 = new Random().nextInt(upbound) + 1;
                if (Util.succeedProp(40)) {
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
            case LUCK_OF_THE_DIE_DD: {
                chr.removeBaseStatByOption();
                boolean isCharged = tsm.getViperEnergyCharge() > 0;
                int upbound = 6;
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
                chr.reduceSkillCoolTime(HEXA_NAUTILUS_STRIKE, (long) (chr.getRemainingCoolTime(HEXA_NAUTILUS_STRIKE) * 0.5F));
                break;
            }
            case MONKEY_MAGIC:
            case MEGA_MONKEY_MAGIC:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieAcc, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieACC, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieAllStat, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieAllStat, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieEva, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieEVA, o3);
                o4.nReason = skillID;
                o4.nValue = si.getValue(indieJump, slv);
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieJump, o4);
                o5.nReason = skillID;
                o5.nValue = si.getValue(indieMhp, slv);
                o5.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHP, o5);
                o6.nReason = skillID;
                o6.nValue = si.getValue(indieMmp, slv);
                o6.tTerm = si.getValue(time, slv);
                newStats.put(IndieMMP, o6);
                o7.nReason = skillID;
                o7.nValue = si.getValue(indieSpeed, slv);
                o7.tTerm = si.getValue(time, slv);
                newStats.put(IndieSpeed, o7);
                tsm.sendStat(newStats);
                break;
            case BARREL_ROULETTE:
                if (tms282()) {
                    if (tsm.hasStat(Roulette)) {
                        tsm.removeStat(Roulette);
                    } else {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        tsm.sendStat(Roulette, o1);
                    }
                } else {
                    handleBarrelRoulette(skillID);
                }
                break;
            case PIRATE_SPIRIT:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Stance, o1);
                break;
            case EPIC_ADVENTURER:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case BUCKSHOT:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(BuckShot, o1);
                break;
            case ROLLING_RAINBOW: //Stationary, Attacks
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 0);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case HEXA_ROLLING_RAINBOW_SUMMON: //Stationary, Attacks
                o1 = tsm.getOption(CannonShooter_RollingCannon);
                o1.nOption = Math.max(0, o1.nOption - 1);
                o1.rOption = HEXA_ROLLING_RAINBOW;
                tsm.sendStat(CannonShooter_RollingCannon, o1);
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 0);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case MONKEY_MALITIA: //Stationary, Attacks
                int[] summons = new int[]{
                        5320011,
                        5321004
                };
                for (int summonZ : summons) {
                    summon = Summon.getSummonByAndSetStat(chr, summonZ, slv);
                    field = chr.getField();
                    summon.setFlyMob(false);
                    summon.setMoveAction((byte) 0);
                    summon.setMoveAbility(MoveAbility.Stop);
                    field.spawnSummon(summon);
                }
                break;
            case HEXA_MONKEY_MALITIA: //Stationary, Attacks
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                field = chr.getField();
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 0);
                summon.setMoveAbility(MoveAbility.Stop);
                field.spawnSummon(summon);
                break;
            case ANCHOR_AWEIGH: //Stationary, Pulls mobs
            case HEXA_ANCHOR_AWEIGH: //Stationary, Pulls mobs
                Position position = new Position(chr.isLeft() ? chr.getPosition().getX() - 250 : chr.getPosition().getX() + 250, chr.getPosition().getY());
                if (chr.getField().findFootHoldBelow(position) != null) {
                    summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                    field = chr.getField();
                    summon.setFlyMob(false);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setCurFoothold((short) (int) java.util.Optional.ofNullable(chr.getField().findFootHoldBelow(position)).map(below -> below.getId()).orElse(0) /* in the air: no foothold (was an NPE) */);
                    summon.setPosition(position);
                    summon.setSummonTerm(20);
                    field.spawnSummon(summon);
                } else {
                    chr.chatMessage("請換個位置再使用這個技能。");
                }
                break;
            case MAPLE_RETURN:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case SPECIAL_MONKEY_SIDEKICK:
                for (int summonSkillId : specialMonkeySideKickIds) {
                    summon = Summon.getSummonByAndSetStat(chr, summonSkillId, slv);
                    summon.setMoveAction((byte) 4);
                    summon.setMoveAbility(MoveAbility.SmartFollow);
                    summon.setAssistType(AssistType.ExplosionAttack);
                    summon.setLinkedSummonSkillIds(specialMonkeySideKickIds);
                    field = chr.getField();
                    field.spawnSummon(summon);
                    field.broadcast(Summoned.attackActive(summon));
                }
                break;
            case NUCLEAR_OPTION_EXPLOSION:
                field = chr.getField();
                tsm.removeStatsBySkill(NUCLEAR_OPTION);
                position = inPacket.decodePositionInt();
                aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setPosition(position);
                aa.setRect(aa.getPosition().getRectAround(si.getFirstRect()));
                field.spawnAffectedArea(aa);

                aa = AffectedArea.getPassiveAA(chr, NUCLEAR_OPTION_TILE, slv);
                aa.setPosition(position);
                aa.setRect(aa.getPosition().getRectAround(SkillData.getSkillInfoById(NUCLEAR_OPTION_EXPLOSION).getFirstRect()));
                field.spawnAffectedArea(aa);
                break;
            case POOLMAKER:
            case HEXA_POOLMAKER:
                this.poolMakerRemainCount = si.getValue(w, slv);
                this.poolMakerStartTime = System.currentTimeMillis();
                this.poolMakerCoolTime = si.getValue(cooltime, slv) * 1000;
                chr.write(UserLocal.poolMakerRequest(skillID, si.getValue(w, slv), poolMakerCoolTime));
                break;
        }
    }

    private void handleBarrelRoulette(int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkillLevel(skillID);
        Option o1 = new Option();
        int roulette = new Random().nextInt(4) + 1;
        o1.nOption = roulette;
        o1.rOption = skillID;
        o1.tOption = si.getValue(time, slv);
        tsm.sendStat(Roulette, o1);
        giveBarrelRouletteBuff(roulette);
        chr.reduceSkillCoolTime(NAUTILUS_STRIKE, (long) (chr.getRemainingCoolTime(NAUTILUS_STRIKE) * 0.5F));
        chr.reduceSkillCoolTime(HEXA_NAUTILUS_STRIKE, (long) (chr.getRemainingCoolTime(HEXA_NAUTILUS_STRIKE) * 0.5F));
        chr.write(WvsContext.updateSkillStackRequestResult(skillID, (byte) 1));
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleInitAfterField() {
        super.handleInitAfterField();
        if (tms282() && monkeyVisible && chr.getField() != null) {
            chr.getField().broadcast(Tms282CannoneerPackets.monkey(chr.getId(), true));
        }
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        // hacks to bypass the quest glitch (accept but no packet)
        var sm = chr.getScriptManager();
        if (level == 60 || level == 100) {
            final short jobID = chr.getJob();
            if (!JobConstants.canJobAdvance(jobID)) {
                return;
            }
            final short next = JobConstants.nextJob(jobID);
            sm.setJob(next);
            sm.completeQuestNoRewards(level == 60 ? 1444 : 1458);
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv(next, 3);
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (this.rollingRainbowTimer != null) {
            this.rollingRainbowTimer.cancel(false);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleShootObject(Char chr, ShootObjectSkillInfo sosi) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (sosi.getSkillId()) {
            case BIG_HUGE_GIGANTIC_ROCKET:
                updateVSkillStackBuff(chr, Math.max(tsm.getOption(CannonShooter_BFCannonBall).nOption - 1, 0));
                break;
        }
        super.handleShootObject(chr, sosi);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);
        cs.setLevel(10);
        cs.setJob((short) 501);
        cs.setStr(45);
        cs.setDex(4);
        cs.setInt(4);
        cs.setLuk(4);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            cs.setMp(500);
            cs.setMaxMp(500);
        }
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        if (chr.getLevel() < 30) {
            ScriptManagerImpl sm = chr.getScriptManager();
            sm.setJob((short) 530);
            sm.levelUntil(30);
            sm.completeQuestNoRewards(1427);
            sm.completeQuestNoRewards(1428);
            sm.addSPJobAdv((short) 501, 5);
            sm.addSPJobAdv((short) 530, 3);
            sm.giveAndEquip(1532004);
            sm.giveAndEquip(1352920);
            sm.warp(FieldConstants.HOME_MAP);
        }
        super.handleInitAfterMigrate(chr);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HEXA_NAUTILUS_STRIKE -> {
                int skillID = NAUTILUS_STRIKE;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_POOLMAKER -> {
                int skillID = POOLMAKER;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
