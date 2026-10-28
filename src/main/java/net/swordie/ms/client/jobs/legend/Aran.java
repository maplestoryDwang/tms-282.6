package net.swordie.ms.client.jobs.legend;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Aran extends Job {

    public static final int COMBO_ABILITY = 21100019;
    public static final int COMBAT_STEP = 20001295;
    public static final int REGAINED_MEMORY = 20000194;
    public static final int RETURN_TO_RIEN = 20001296;

    public static final int POLEARM_BOOSTER = 21001003; //Buff

    public static final int SNOW_CHARGE = 21101006; //Buff
    public static final int DRAIN = 21101005; //Special Skill (HP Recovery) (ON/OFF)

    public static final int MAHA_BLESSING = 21111012; //Buff
    public static final int ADRENALINE_RUSH = 21110016; //at 1000 combo activated
    public static final int AERO_SWING = 21110026; //Passive that activates when Combo'ing in Air TODO

    public static final int MAPLE_WARRIOR_ARAN = 21121000; //Buff
    public static final int HEROS_WILL_ARAN = 21121008;

    public static final int HEROIC_MEMORIES_ARAN = 21121053;
    public static final int ADRENALINE_BURST = 21121058;
    public static final int MAHAS_DOMAIN = 21121068; //AoE Effect
    public static final int MAHAS_DOMAIN_SKILL_USE = 21121057; //AoE Effect

    //Final Attack
    public static final int FINAL_ATTACK = 21100010;
    public static final int ADVANCED_FINAL_ATTACK = 21120012;

    //Attacking Skills:
    public static final int SMASH_WAVE = 21001009;
    public static final int SMASH_WAVE_COMBO = 21000004;

    public static final int SMASH_SWING_1 = 21001010;
    public static final int SMASH_SWING_2 = 21000006;
    public static final int SMASH_SWING_3 = 21000007;
    public static final int SMASH_SWING_2_FINAL_BLOW = 21120025;

    public static final int FINAL_CHARGE = 21101011;
    public static final int FINAL_CHARGE_COMBO = 21100002; //Special Attack (Stun Debuff) (Special Skill from Key-Command)

    //public static final int FINAL_TOSS = 21100015;
    public static final int FINAL_TOSS = 21101016;
    public static final int FINAL_TOSS_COMBO = 21100012;

    public static final int ROLLING_SPIN = 21101017;
    public static final int ROLLING_SPIN_COMBO = 21100013; //Special Attack (Stun Debuff) (Special Skill from Key-Command)

    public static final int GATHERING_HOOK = 21111019;
    public static final int GATHERING_HOOK_COMBO = 21110018;

    public static final int FINAL_BLOW = 21111021;
    public static final int FINAL_BLOW_COMBO = 21110020; //Special Attack (Stun Debuff) (Special Skill from Key-Command)
    public static final int FINAL_BLOW_SMASH_SWING_COMBO = 21110028; //Special Attack (Stun Debuff) (Special Skill from Key-Command)
    public static final int FINAL_BLOW_ADRENALINE_SHOCKWAVE = 21110027; //Shockwave after final blow when in Adrenaline Rush

    public static final int JUDGEMENT_DRAW = 21111017;
    public static final int JUDGEMENT_DRAW_COMBO_DOWN = 21110011; //Special Attack (Freeze Debuff) (Special Skill from Key-Command)
    public static final int JUDGEMENT_DRAW_COMBO_LEFT = 21110024; //Special Attack (Freeze Debuff) (Special Skill from Key-Command)
    public static final int JUDGEMENT_DRAW_COMBO_RIGHT = 21110025; //Special Attack (Freeze Debuff) (Special Skill from Key-Command)

    public static final int BEYOND_BLADE_1 = 21120022;
    public static final int BEYOND_BLADE_2 = 21121016;
    public static final int BEYOND_BLADE_3 = 21121017;

    //Finisher
    public static final int FINISHER_HUNTER_PREY = 21120019;
    public static final int FINISHER_STORM_OF_FEAR = 21120023;

    // Passive Hyper
    public static final int SURGING_ADRENALINE = 21120064;

    // V skill
    public static final int MAHAS_FURY_BUFF = 400011016;
    public static final int MAHAS_FURY_ATTACK = 400011020;
    public static final int MAHAS_CARNAGE = 400011031;
    public static final int MAHAS_CARNAGE_COMBO = 400010030;
    public static final int FENRIR_CRASH = 400010070;
    public static final int FENRIR_CRASH_ADRENALINE = 400010071;
    public static final int BLIZZARD_TEMPEST = 400011121;
    public static final int BLIZZARD_TEMPEST_DIRE_WOLF_ATTACK = 400011122;
    public static final int BLIZZARD_TEMPEST_MOB = 400011123;

    // HEXA skills
    public static final int HEXA_FINAL_ATTACK = 21140012;

    private final int[] addedSkills = new int[]{
            REGAINED_MEMORY,
            RETURN_TO_RIEN,};

    private int combo;
    private final Tms282AranPeriodicCombo periodicCombo=new Tms282AranPeriodicCombo(chr,this);

    public Tms282AranPeriodicCombo getPeriodicCombo() { return periodicCombo; }
    private long lastDrain = 0L;
    private ScheduledFuture<?> mahasFuryTimer;
    private long lastDireWolfCurse = Long.MIN_VALUE;
    private static final int direWolfCurseInterval = 2000;

    public Aran(Char chr) {
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

    public static int getOriginalSkillByID(int skillID) {
        switch (skillID) {
            case SMASH_WAVE_COMBO:
                return SMASH_WAVE;
            case FINAL_BLOW_COMBO:
            case FINAL_BLOW_SMASH_SWING_COMBO:
                return FINAL_BLOW;
        }
        return skillID; // no original skill linked with this one
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isAran(id);
    }

    private void giveAdrenalinRushBuff(TemporaryStatManager tsm) {
        Skill skill = chr.getSkill(ADRENALINE_RUSH);
        if (skill == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(ADRENALINE_RUSH);
        int slv = skill.getCurrentLevel();
        if (chr.hasSkill(ADRENALINE_RUSH)) {
            Option o = new Option();
            o.nOption = 1;
            o.rOption = ADRENALINE_RUSH;
            o.tOption = si.getValue(time, slv) + (chr.hasSkill(SURGING_ADRENALINE) && chr.getLevel() >= 180 ? 5000 : 0);
            o.cOption = 1;
            tsm.sendStat(AdrenalinBoost, o);
            chr.getTimer().addEvent(() -> setCombo(500), si.getValue(time, slv), TimeUnit.SECONDS);
        }
    }

    private void setComboCountAfterAdrenaline() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = 500;
        o.rOption = COMBO_ABILITY;
        tsm.sendStat(ComboAbilityBuff, o);
        setCombo(500);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        snowCharge(mob);
        switch (skillID) {
            case FINAL_CHARGE_COMBO: {
                //TODO  Leaves an ice trail behind that freezes enemies
                int hcProp = 5; //hcProp is defined yet still gives NPEs
                int hcTime = 10; //hcTime is defined yet still gives NPEs
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        if (Util.succeedProp(hcProp)) {
                            o1.nOption = 1;
                            o1.rOption = getOriginalSkillByID(skillID);
                            o1.tOption = hcTime;
                            mts.addStatOptions(mob, MobStat.Freeze, o1);
                        }
                    }
                }
                break;
            }
            case ROLLING_SPIN_COMBO: {
                int prop = 30; //Prop value never given, so I decided upon 30%.
                int time = 3; //Time value never given, so I decided upon 3 seconds.
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        if (Util.succeedProp(prop)) {
                            o1.nOption = 1;
                            o1.rOption = getOriginalSkillByID(skillID);
                            o1.tOption = time;
                            mts.addStatOptions(mob, MobStat.Stun, o1);
                        }
                    }
                }
                break;
            }
            case FINAL_BLOW_COMBO: {
                int prop = 10; //Prop value never given, so I decided upon 10%.
                int hcTime = 1; //Time value never given, so I decided upon 1 seconds.
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(prop)) {
                        o1.nOption = 1;
                        o1.rOption = ROLLING_SPIN_COMBO;
                        o1.tOption = hcTime;
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            }
            case FINAL_BLOW_SMASH_SWING_COMBO: {
                int prop = 30; //Prop value never given, so I decided upon 30%.
                int time = 3; //Time value never given, so I decided upon 3 seconds.
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(prop)) {
                        o1.nOption = 1;
                        o1.rOption = FINAL_CHARGE_COMBO; // Final Blow doens't have a mob stat effect
                        o1.tOption = time;
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            }
            case JUDGEMENT_DRAW_COMBO_DOWN:
            case JUDGEMENT_DRAW_COMBO_LEFT:
            case JUDGEMENT_DRAW_COMBO_RIGHT:
                si = SkillData.getSkillInfoById(JUDGEMENT_DRAW);
                slv = chr.getSkillLevel(JUDGEMENT_DRAW);
                if (Util.succeedProp(si.getValue(hcProp, slv))) {
                    if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                        o1.nOption = 1;
                        o1.rOption = getOriginalSkillByID(skillID);
                        o1.tOption = 10;
                        mts.addStatOptions(mob, MobStat.Freeze, o1);
                    }
                } else {
                    BurnedInfo bi = BurnedInfo.createBurnInfo(chr, JUDGEMENT_DRAW, slv, damage);
                    mts.createAndAddBurnedInfo(mob, bi, JUDGEMENT_DRAW);
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
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        int slv = attackInfo.slv;
        Option o1 = new Option();
        if (hasHitMobs) {
            if (!Tms282AranPeriodicCombo.activeProfile() && chr.hasSkill(ADRENALINE_RUSH) && getCombo() > 999 && !tsm.hasStat(AdrenalinBoost)) {
                giveAdrenalinRushBuff(tsm);
            }
            if (now - lastDrain >= 500L) {
                aranDrain();
                lastDrain = now;
            }
            mahaFuryAttack(tsm);
            if (tsm.hasStat(AranComboTempestAura)) {
                chr.write(UserLocal.userBonusAttackRequest(BLIZZARD_TEMPEST_DIRE_WOLF_ATTACK));
            }
        }
        doSwingStudiesAddAttack(tsm);
        switch (attackInfo.skillId) {
            case FINISHER_HUNTER_PREY:
                int t = si.getValue(subTime, slv);
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = t;
                tsm.sendStat(AranBoostEndHunt, o1);
                break;
            case SMASH_WAVE_COMBO:
                Skill skill = chr.getSkill(SMASH_WAVE);
                si = SkillData.getSkillInfoById(skill.getSkillId());
                slv = skill.getCurrentLevel();
                skillID = skill.getSkillId();

                int swCDInSec = si.getValue(SkillStat.cooltime, slv);
                int swCDInMillis = swCDInSec > 0 ? swCDInSec * 1000 : si.getValue(SkillStat.cooltimeMS, slv);

                chr.addSkillCoolTime(SMASH_WAVE, System.currentTimeMillis() + swCDInMillis);
                chr.write(UserLocal.skillCooltimeSetM(skillID, swCDInMillis));
                break;
            case GATHERING_HOOK_COMBO:
                skill = chr.getSkill(GATHERING_HOOK);
                si = SkillData.getSkillInfoById(skill.getSkillId());
                slv = skill.getCurrentLevel();
                skillID = skill.getSkillId();

                int ghCDInSec = si.getValue(SkillStat.cooltime, slv);
                int ghCDInMillis = ghCDInSec > 0 ? ghCDInSec * 1000 : si.getValue(SkillStat.cooltimeMS, slv);

                chr.addSkillCoolTime(GATHERING_HOOK, System.currentTimeMillis() + ghCDInMillis);
                chr.write(UserLocal.skillCooltimeSetM(skillID, ghCDInMillis));
                break;
            case MAHAS_DOMAIN_SKILL_USE:
                SkillInfo mdi = SkillData.getSkillInfoById(MAHAS_DOMAIN);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, MAHAS_DOMAIN, slv);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(mdi.getFirstRect()));
                chr.getField().spawnAffectedArea(aa);
                aa.activateTimer(1000, 1000);
                chr.setSkillCooldown(skillID, slv);
                break;
            case MAHAS_CARNAGE_COMBO:
                si = SkillData.getSkillInfoById(MAHAS_CARNAGE);
                slv = chr.getSkillLevel(MAHAS_CARNAGE);
                int cd = si.getValue(cooltime, slv);
                if (tsm.hasStat(EunwolSoulSeperateNotTime)) {
                    cd = (int) ((cd * si.getValue(x, slv)) / 100D);
                }
                chr.addSkillCoolTime(MAHAS_CARNAGE, Util.getCurrentTimeLong() + cd * 1000L);
                break;
        }
    }

    private void doSwingStudiesAddAttack(TemporaryStatManager tsm) {
        Option o = new Option();
        if (chr.hasSkill(21100015)) {
            o.nOption = 1;
            o.rOption = 21100015;
            o.tOption = 5;
            tsm.sendStat(NextAttackEnhance, o);
        }
    }

    public int getCombo() {
        return combo;
    }

    public void setCombo(int combo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Client comboClient=Tms282AranPeriodicCombo.activeProfile()?chr.getClient():c;
        if (combo < 0) {
            this.combo = 0;
            tsm.removeStatsBySkill(COMBO_ABILITY);
            comboClient.write(WvsContext.modComboResponse(0));
        } else {
            Option o = new Option();
            this.combo = Math.min(combo, 10000);
            o.nOption = getCombo() <= 0 ? 1 : getCombo();
            o.rOption = COMBO_ABILITY;
            tsm.sendStat(ComboAbilityBuff, o);
            comboClient.write(WvsContext.modComboResponse(getCombo()));
        }
    }

    public void aranDrain() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(AranDrain)) {
            Skill skill = chr.getSkill(DRAIN);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int healrate = si.getValue(x, slv);
            chr.heal((int) (chr.getMaxHP() / ((double) 100 / healrate)));
        }
    }

    public void applyDireWolfCurse() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(AranComboTempestAura) || lastDireWolfCurse + direWolfCurseInterval >Util.getCurrentTimeLong()) {
            return;
        }

        var si = SkillData.getSkillInfoById(BLIZZARD_TEMPEST_MOB);
        List<Mob> mobList = chr.getField().getMobsInRect(chr.getRectAround(si.getFirstRect())).stream().limit(10).collect(Collectors.toList());
        Map<Mob, Integer> mobCursedMap = new HashMap<>();
        for (var mob : mobList) {
            if (mob == null) {
                continue;
            }

            var count = incDireWolfCurse(mob);
            mobCursedMap.put(mob, count);
        }

        lastDireWolfCurse = Util.getCurrentTimeLong();
        chr.write(WvsContext.aranDireWolfCurse(mobCursedMap));
    }

    private int incDireWolfCurse(Mob mob) {
        var rOpt = JobConstants.JobEnum.ARAN4.getJobId();

        MobTemporaryStat mts = mob.getTemporaryStat();
        Option o = new Option();
        var count = 1;

        var prevOpt = mts.getCurrentOptionsByMobStat(MobStat.ACC);
        if (prevOpt != null) {
            count = prevOpt.xOption;
            if (count < 6) {
                count++;
            }
        }

        o.tOption = 10;
        o.rOption = rOpt;
        o.xOption = count;
        mts.addStatOptions(mob, MobStat.ACC, o);

        return count;
    }

    public void snowCharge(Mob mob) {
        if (!chr.hasSkill(SNOW_CHARGE)) {
            return;
        }
        Skill skill = chr.getSkill(SNOW_CHARGE);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        byte slv = (byte) chr.getSkill(skill.getSkillId()).getCurrentLevel();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        if (tsm.getOptByCTSAndSkill(WeaponCharge, SNOW_CHARGE) != null) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            if (!mts.hasCurrentMobStatBySkillId(skill.getSkillId())) {
                o1.nOption = (mob.isBoss() ? -(si.getValue(q, slv) / 2) : -si.getValue(q, slv));
                o1.rOption = skill.getSkillId();
                o1.tOption = si.getValue(y, slv);
                o1.mOption = 1;
                mts.addStatOptions(mob, MobStat.Speed, o1);
            }
        }
    }

    public void mahaFuryAttack(TemporaryStatManager tsm) {
        if (tsm.hasStat(EunwolSoulSeperateNotTime) && chr.checkAndSetSkillCooltime(MAHAS_FURY_BUFF, true)) {
            chr.getField().broadcast(UserLocal.userBonusAttackRequest(MAHAS_FURY_ATTACK));
            chr.addSkillCooldown(MAHAS_FURY_BUFF, 3000); // every 3 sec
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == FINAL_ATTACK) {
            if (chr.hasSkill(HEXA_FINAL_ATTACK)) return HEXA_FINAL_ATTACK;
            if (chr.hasSkill(ADVANCED_FINAL_ATTACK)) return ADVANCED_FINAL_ATTACK;
        }
        return super.getFinalAttackSkill(faSkill);
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
            case DRAIN:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    tsm.sendStat(AranDrain, o1);
                }
                break;
            case SNOW_CHARGE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(WeaponCharge, o1);
                break;
            case MAHA_BLESSING:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieMad, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieMAD, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePad, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o2);
                tsm.sendStat(newStats);
                break;
            case HEROIC_MEMORIES_ARAN:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case MAHAS_FURY_BUFF:
                if (chr.getField().getAffectedAreas().stream()
                        .anyMatch(aa -> aa.getSkillID() == MAHAS_DOMAIN
                                && aa.getCharID() == chr.getId())) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    newStats.put(EunwolSoulSeperateNotTime, o1);
                    o2.nReason = skillID;
                    o2.nValue = si.getValue(indiePadR, slv);
                    o2.tTerm = si.getValue(time, slv);
                    newStats.put(IndiePADR, o2);
                    tsm.sendStat(newStats);
                    int addedCombo = si.getValue(z, slv);
                    setCombo(Math.min(getCombo() + addedCombo, 999));
                    AffectedArea mahasDomain = chr.getField().getAffectedAreas().stream()
                            .filter(aa -> aa.getSkillID() == MAHAS_DOMAIN
                                    && aa.getCharID() == chr.getId())
                            .findFirst().orElse(null);
                    if (mahasDomain != null) {
                        chr.getField().removeLife(mahasDomain.getObjectId(), true);
                    }
                    if (this.mahasFuryTimer != null) {
                        this.mahasFuryTimer.cancel(false);
                    }
                    ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(this::doMahasFuryAttack, 0, si.getValue(x, slv), TimeUnit.SECONDS, false);
                    this.mahasFuryTimer = sf;
                    GlobalTimerManager.addCharTimer(chr.getId(), sf);
                }
                break;
            case ADRENALINE_BURST:
                giveAdrenalinRushBuff(tsm);
                break;
            case RETURN_TO_RIEN:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case HEROS_WILL_ARAN:
                tsm.removeAllDebuffs();
                break;
        }
    }

    private void doMahasFuryAttack() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(MAHAS_FURY_BUFF) || !tsm.hasStat(EunwolSoulSeperateNotTime)) {
            if (this.mahasFuryTimer != null) {
                this.mahasFuryTimer.cancel(false);
            }
            return;
        }
        chr.write(UserLocal.userBonusAttackRequest(MAHAS_FURY_ATTACK));
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(JobConstants.ARAN_CREATION_MAP);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        if (level >= 100 && !chr.hasSkill(21121000)) {
            chr.getScriptManager().giveSkill(21121000, 0, 10);
        }
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.ARAN1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ARAN2.getJobId());
                sm.giveItem(1142130);
                sm.completeQuestNoRewards(21200);
                sm.completeQuestNoRewards(21201);
                sm.completeQuestNoRewards(21202);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.ARAN2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ARAN3.getJobId());
                sm.giveItem(1142131);
                sm.completeQuestNoRewards(21300);
                sm.completeQuestNoRewards(21301);
                sm.completeQuestNoRewards(21302);
                sm.completeQuestNoRewards(21303);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.ARAN3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ARAN4.getJobId());
                sm.giveItem(1142132);
                sm.completeQuestNoRewards(21400);
                sm.completeQuestNoRewards(21401);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        if (cts == AdrenalinBoost) {
            setComboCountAfterAdrenaline();
        }
        if (cts == AranComboTempestAura) {
            chr.write(WvsContext.aranDireWolfCurse(new HashMap<>()));
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (Tms282AranPeriodicCombo.activeProfile()) periodicCombo.clear();
        super.handleCancelTimer(chr);
    }
}
