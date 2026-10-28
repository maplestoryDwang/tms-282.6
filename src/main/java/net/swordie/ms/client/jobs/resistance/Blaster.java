package net.swordie.ms.client.jobs.resistance;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.world.field.Field;

import java.util.EnumMap;
import java.util.concurrent.ScheduledFuture;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Blaster extends Citizen {

    public static final int SECRET_ASSEMBLY = 30001281;

    public static final int HAMMER_SMASH = 37111000;
    public static final int HAMMER_SMASH_CHARGE = 37110001;
    public static final int ARM_CANNON_BOOST = 37101003;
    public static final int MAPLE_WARRIOR_BLASTER = 37121006;
    public static final int HEROS_WILL_BLASTER = 37121007;

    public static final int FOR_LIBERTY_BLASTER = 37121053;
    public static final int CANNON_OVERDRIVE = 37121054;
    public static final int HYPER_MAGNUM_PUNCH = 37121052;

    public static final int DETONATE = 37001004;
    public static final int DETONATE_UP = 37000005;

    //Revolving Cannon
    public static final int MAGNUM_PUNCH = 37001000;
    public static final int REVOLVING_CANON_MASTERY = 37000007;
    public static final int REVOLVING_CANNON_RELOAD = 37000010;
    public static final int REVOLVING_CANNON = 37001001;
    public static final int REVOLVING_CANNON_2 = 37100008;
    public static final int REVOLVING_CANNON_3 = 37000009;

    public static final int REVOLVING_CANNON_PLUS = 37100007;
    public static final int REVOLVING_CANNON_PLUS_II = 37110007;
    public static final int REVOLVING_CANNON_PLUS_III = 37120008;

    public static final int BUNKER_BUSTER_EXPLOSION = 37000008;
    public static final int BUNKER_BUSTER_EXPLOSION_1 = 37001002;
    public static final int BUNKER_BUSTER_EXPLOSION_2 = 37000011;
    public static final int BUNKER_BUSTER_EXPLOSION_3 = 37000012;
    public static final int BUNKER_BUSTER_EXPLOSION_4 = 37000013;

    //Blast Shield
    public static final int BLAST_SHIELD = 37000006;
    public static final int SHIELD_TRAINING = 37110008;
    public static final int SHIELD_TRAINING_II = 37120009;
    public static final int VITALITY_SHIELD = 37121005;

    //Combo Training
    public static final int COMBO_TRAINING = 37110009;
    public static final int COMBO_TRAINING_II = 37120012;

    // Charged Skills
    public static final int CHARGE_MASTERY = 37100006;
    public static final int ADVANCED_CHARGE_MASTERY = 37120011;
    public static final int BOBBING_CHARGED = 37100002;
    public static final int WEAVING_CHARGED = 37110004;

    public static final int ROCKET_RUSH = 37111005;
    public static final int MAGNUM_LAUNCH = 37110006;

    public static final int BALLISTIC_HURRICANE = 37121003;
    public static final int REVOLVING_BLAST = 37121004;
    public static final int BALLISTIC_HURRICANE_1 = 37120024;
    public static final int WEAVING = 37111003;
    public static final int BOBBING = 37101001;

    // V Skills
    public static final int ROCKET_PUNCH = 400011017;
    public static final int ROCKET_PUNCH_EXTRA_SKILL = 400011019;
    public static final int GATLING_PUNCH = 400011028;
    public static final int BULLET_BLAST = 400011091;
    public static final int BULLET_BLAST_2 = 400011103;
    public static final int AFTERIMAGE_SHOCK_1 = 400011116; // Buff
    public static final int AFTERIMAGE_SHOCK_2 = 400011117; // Bonus Attack

    // HEXA Skills
    public static final int HEXA_REVOLVING_CANON_MASTERY = 37141008;

    private final int[] addedSkills = new int[]{
            SECRET_ASSEMBLY,};

    private int gauge = 0;
    private int ammo = 0;
    private int lastAttack = 0;

    private ScheduledFuture<?> reloadCylinder;

    public Blaster(Char chr) {
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
        return JobConstants.isBlaster(id);
    }

    public void resetBlastShield() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.removeStat(RWBarrier);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(TempSecondaryStat) && skillID != ROCKET_PUNCH_EXTRA_SKILL) {
            chr.write(UserLocal.userBonusAttackRequest(ROCKET_PUNCH_EXTRA_SKILL));
        }
        Option o1 = new Option();
        Option o2 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();

        switch (skillID) {
            case HAMMER_SMASH:
                if (!mts.hasCurrentMobStatBySkillId(HAMMER_SMASH_CHARGE)) {
                    si = SkillData.getSkillInfoById(HAMMER_SMASH_CHARGE);
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = HAMMER_SMASH_CHARGE;
                    o1.tOption = 10;
                    mts.addStatOptions(mob, MobStat.RWLiftPress, o1);
                }
                break;
            case REVOLVING_BLAST:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = 10;
                        if (isTms282()) o1.cOption = chr.getId(); // existing Freeze caster field / bind ownership
                        mts.addStatOptions(mob, MobStat.Freeze, o1);
                        if (!tsm.hasStatBySkillId(skillID)) {
                            o2.nOption = 1;
                            o2.rOption = skillID;
                            o2.tOption = 10;
                            tsm.sendStat(NotDamaged, o2);
                        }
                        if (!isTms282()) chr.getField().broadcast(UserPacket.RWZeroBunkerMobBind(chr.getId(), mob, true));
                    }
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
        switch (attackInfo.skillId) {
            case DETONATE:
                removeAmmo();
                break;
            case BOBBING_CHARGED:
            case WEAVING_CHARGED:
                if (chr.hasSkill(CHARGE_MASTERY) && getAmmo() > 0 && getAmmo() < getMaxAmmo()) {
                    addAmmo(chr.hasSkill(ADVANCED_CHARGE_MASTERY) ? 2 : 1);
                }
                int realSkillId = skillID == BOBBING_CHARGED ? BOBBING : WEAVING;
                si = SkillData.getSkillInfoById(realSkillId);
                o1.nOption = si.getValue(w, slv);
                o1.rOption = realSkillId;
                o1.tOption = si.getValue(subTime, slv);
                o1.setInMillis(true);
                tsm.sendStat(RWMovingEvar, o1);
                break;
            case HAMMER_SMASH_CHARGE:
                if (chr.hasSkill(CHARGE_MASTERY) && getAmmo() > 0 && getAmmo() < getMaxAmmo()) {
                    addAmmo(chr.hasSkill(ADVANCED_CHARGE_MASTERY) ? 2 : 1);
                }
                si = SkillData.getSkillInfoById(HAMMER_SMASH);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, HAMMER_SMASH, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                aa.setDelay((short) 5);
                chr.getField().spawnAffectedArea(aa);
                break;
            case BUNKER_BUSTER_EXPLOSION:
                gaugeChange(0);
                break;
            case BUNKER_BUSTER_EXPLOSION_1:
            case BUNKER_BUSTER_EXPLOSION_2:
            case BUNKER_BUSTER_EXPLOSION_3:
            case BUNKER_BUSTER_EXPLOSION_4:
                if (tsm.hasStat(RWOverHeat)) {
                    return;
                }
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = (tsm.hasStat(RWMaximizeCannon) ? 1 : si.getValue(time, 1));
                tsm.sendStat(RWOverHeat, o1);
                removeGauge(getMaxAmmo());
                break;
            case HYPER_MAGNUM_PUNCH:
                o1.nOption = 5;
                o1.rOption = skillID;
                o1.tOption = 10;
                tsm.sendStat(RWMagnumBlow, o1);
                break;
            case MAGNUM_PUNCH:
            case REVOLVING_CANON_MASTERY: {
                if (hasHitMobs) {
                    if (getGauge() < getMaxAmmo() && !tsm.hasStat(RWOverHeat)) {
                        addGauge();
                    }
                }
                lastAttack = skillID;
                chr.getField().broadcast(UserRemote.requestRWMultiChargeCancel(chr, skillID), chr);
                break;
            }
            case REVOLVING_CANNON_3:
            case REVOLVING_CANNON_2:
            case REVOLVING_CANNON: {
                if (hasHitMobs) {
                    removeAmmo();
                    if (getGauge() < getMaxAmmo() && !tsm.hasStat(RWOverHeat)) {
                        addGauge();
                    }
                }
                lastAttack = skillID;
                chr.getField().broadcast(UserRemote.requestRWMultiChargeCancel(chr, skillID), chr);
                break;
            }
        }
        if (getAmmo() == 0) {
            o1.nOption = 1;
            o1.rOption = Blaster.REVOLVING_CANNON_RELOAD;
            o1.bOption = getMaxAmmo();
            o1.cOption = getGauge();
            tsm.sendStat(RWCylinder, o1);
            if (reloadCylinder == null || reloadCylinder.isDone()) {
                int time = tsm.hasStat(RWMaximizeCannon) ? 500 : 1500;
                reloadCylinder = chr.getTimer().addEvent(this::reloadCylinder, time);
            }
        }
        incrementComboTraining(skillID, tsm);
    }

    public int getAmmo() {
        return ammo;
    }

    public void setAmmo(int ammo) {
        this.ammo = ammo;
    }

    public void addAmmo() {
        addAmmo(1);
    }

    public void addAmmo(int amount) {
        ammoChange(amount);
    }

    public void removeAmmo() {
        removeAmmo(1);
    }

    public void removeAmmo(int amount) {
        ammoChange(-amount);
    }

    public void ammoChange(int amount) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        if (getAmmo() > 0) {
            setAmmo(Math.min(getAmmo() + amount, getMaxAmmo()));
            o.nOption = 1;
            o.bOption = getAmmo();
            o.cOption = getGauge();
            tsm.sendStat(RWCylinder, o);
        }
    }

    public int getGauge() {
        return gauge;
    }

    public void setGauge(int gauge) {
        this.gauge = gauge;
    }

    public void addGauge() {
        addGauge(1);
    }

    public void addGauge(int amount) {
        gaugeChange(amount);
    }

    public void removeGauge() {
        removeGauge(1);
    }

    public void removeGauge(int amount) {
        gaugeChange(-amount);
    }

    public void gaugeChange(int amount) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        setGauge(getGauge() + amount);
        if (getGauge() <= getMaxAmmo() || (amount < 0 && getGauge() + amount > 0)) {
            setGauge(getGauge() + amount);
            o.nOption = 1;
            o.bOption = getAmmo();
            o.cOption = getGauge();
            tsm.sendStat(RWCylinder, o);
        }
    }

    public int getMaxAmmo() {
        int maxAmmo = 3;
        if (chr.hasSkill(REVOLVING_CANNON_PLUS)) {
            maxAmmo = 4;
        }
        if (chr.hasSkill(REVOLVING_CANNON_PLUS_II)) {
            maxAmmo = 5;
        }
        if (chr.hasSkill(REVOLVING_CANNON_PLUS_III)) {
            maxAmmo = 6;
        }
        return maxAmmo;
    }

    public void reloadCylinder() {
        setAmmo(getMaxAmmo());
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = 1;
        o.bOption = getMaxAmmo(); //ammo
        o.cOption = getGauge(); //gauge
        tsm.sendStat(RWCylinder, o);
    }

    private void incrementComboTraining(int skillId, TemporaryStatManager tsm) {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            return; // 282: 組合訓練 37110009 / 37120012 are plain passives (psd, no stacks); RWCombination has no 282 stat
        }
        if (chr.hasSkill(COMBO_TRAINING)) {
            SkillInfo chargeInfo = SkillData.getSkillInfoById(COMBO_TRAINING);
            int amount = 1;
            if (tsm.hasStat(RWCombination)) {
                amount = tsm.getOption(RWCombination).nOption;
                if (lastAttack == skillId) {
                    return;
                }
                if (amount < chargeInfo.getValue(z, 1)) {
                    amount++;
                }
            }
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            Option o4 = new Option();
            lastAttack = skillId;
            o1.nOption = amount;
            o1.rOption = chr.hasSkill(COMBO_TRAINING_II) ? COMBO_TRAINING_II : COMBO_TRAINING;
            o1.tOption = 10;
            newStats.put(RWCombination, o1);
            if (amount >= chargeInfo.getValue(w, 1)) { //if combo higher than w value give attack speed, for both passives
                o2.nValue = -1;
                o2.rOption = COMBO_TRAINING;
                o2.tTerm = 10;
                newStats.put(IndieBooster, o2);
            }
            chargeInfo = SkillData.getSkillInfoById(COMBO_TRAINING_II);
            o3.nOption = chr.hasSkill(COMBO_TRAINING_II) ? (3 + (chr.getSkillLevel(COMBO_TRAINING_II) / 10)) * amount : chr.getSkillLevel(COMBO_TRAINING) / 3 * amount; //diff calculation depends if player has combo training 2
            o3.rOption = chargeInfo.getCurrentLevel();
            o3.tOption = 10;
            newStats.put(DamR, o3);

            if (chr.hasSkill(COMBO_TRAINING_II)) {
                o4.nOption = chr.getSkillLevel(COMBO_TRAINING_II) / 10 * amount;
                o4.rOption = chargeInfo.getCurrentLevel();
                o4.tOption = 10;
                newStats.put(CriticalBuff, o4);
            }
            tsm.sendStat(newStats);
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == REVOLVING_CANON_MASTERY) {
            if (chr.hasSkill(HEXA_REVOLVING_CANON_MASTERY)) return HEXA_REVOLVING_CANON_MASTERY;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        Option o1 = new Option();
        switch (skillID) {
            case SECRET_ASSEMBLY:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case REVOLVING_CANNON_RELOAD:
                reloadCylinder();
                break;
            case VITALITY_SHIELD:
                if (!tsm.hasStat(RWBarrier)) {
                    return;
                }
                int healAmount = (int) (0.5 * chr.getMaxHP() + tsm.getOption(RWBarrier).nOption);
                chr.heal(healAmount);
                resetBlastShield();
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(RWBarrierHeal, o1);
                break;
            case HEROS_WILL_BLASTER:
                tsm.removeAllDebuffs();
                break;
            case ARM_CANNON_BOOST:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Booster, o1);
                break;
            case FOR_LIBERTY_BLASTER:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case CANNON_OVERDRIVE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(RWMaximizeCannon, o1);
                break;
            case ROCKET_PUNCH:
                if (tsm.hasStat(TempSecondaryStat)) {
                    tsm.removeStatsBySkill(skillID);
                }
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.xOption = ROCKET_PUNCH_EXTRA_SKILL;
                tsm.sendStat(TempSecondaryStat, o1);
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(BLAST_SHIELD) && hitInfo.hpDamage > 0 && !tsm.hasStat(RWBarrier)) {
            Skill shieldSkill = getBlastShieldSkill();
            SkillInfo shieldInfo = SkillData.getSkillInfoById(shieldSkill.getSkillId());
            int amount = Math.min(hitInfo.hpDamage * shieldInfo.getValue(x, shieldSkill.getCurrentLevel()) / 100 + 1, chr.getMaxHP());
            putOnShield(amount);
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    public Skill getBlastShieldSkill() {
        Skill skill = null;
        if (chr.hasSkill(BLAST_SHIELD)) {
            skill = chr.getSkill(BLAST_SHIELD);
        }
        if (chr.hasSkill(SHIELD_TRAINING)) {
            skill = chr.getSkill(SHIELD_TRAINING);
        }
        if (chr.hasSkill(SHIELD_TRAINING_II)) {
            skill = chr.getSkill(SHIELD_TRAINING_II);
        }
        return skill;
    }

    public void decreaseShield() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(RWBarrier)) { //function stops on vitality shield or shield value <=0
            return;
        }
        Skill shieldSkill = getBlastShieldSkill();
        SkillInfo si = SkillData.getSkillInfoById(shieldSkill.getSkillId());
        int oldShield = tsm.getOption(RWBarrier).nOption;
        int newShield = (oldShield * si.getValue(y, shieldSkill.getCurrentLevel()) / 100) - si.getValue(z, shieldSkill.getCurrentLevel());
        if (newShield <= 0) {
            tsm.removeStat(RWBarrier);
        } else {
            putOnShield(newShield);
        }
    }

    public void putOnShield(int amount) {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        o1.nReason = BLAST_SHIELD;
        o1.nValue = 100;
        newStats.put(IndieStance, o1);
        o2.nOption = amount;
        o2.rOption = BLAST_SHIELD;
        newStats.put(RWBarrier, o2);
        tsm.sendStat(newStats);
        chr.getTimer().addEvent(this::decreaseShield, 3000);
    }

    public void releaseRevolvingBlast() {
        boolean tms282 = isTms282();
        if (tms282) chr.getTemporaryStatManager().removeStatsBySkill(REVOLVING_BLAST);
        for (Mob mob : chr.getField().getMobs()) {
            if (mob == null || mob.getHp() <= 0) {
                continue;
            }
            MobTemporaryStat mts = mob.getTemporaryStat();
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option freeze = mts.getCurrentOptionsByMobStat(MobStat.Freeze);
            if (tms282 ? freeze != null && freeze.rOption == REVOLVING_BLAST && freeze.cOption == chr.getId()
                    : mts.hasCurrentMobStatBySkillId(REVOLVING_BLAST)) {
                if (!tms282) tsm.removeStatsBySkill(REVOLVING_BLAST);
                mts.removeMobStat(mob, MobStat.Freeze);
                // 282 clears its local actor before this callback. Mob/CTS reset have verified native codecs;
                // the old RW_ZERO_BUNKER_MOB_BIND has no proven 282 mapping or rendering contract yet.
                if (!tms282) chr.getField().broadcast(UserPacket.RWZeroBunkerMobBind(chr.getId(), mob, false));
            }
        }
    }

    private static boolean isTms282() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    @Override
    public void handleKeyDownSkill(Char chr, SkillInfo si, InPacket inPacket) {
        super.handleKeyDownSkill(chr, si, inPacket);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillId = si.getSkillId();
        Option o1 = new Option();
        switch (skillId) {
            case BULLET_BLAST:
                o1.nOption = 1;
                o1.rOption = BULLET_BLAST;
                o1.tOption = 5;
                tsm.sendStat(NotDamaged, o1);
                break;
        }
    }

    @Override
    public void handleCancelKeyDownSkill(Char chr, int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (skillID) {
            case BULLET_BLAST:
                tsm.removeStatsBySkill(skillID);
                break;
            default:
                super.handleCancelKeyDownSkill(chr, skillID);
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (reloadCylinder != null) {
            reloadCylinder.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.BLASTER_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.BLASTER_2.getJobId());
                sm.giveItem(1142243);
                sm.completeQuestNoRewards(23161);
                sm.completeQuestNoRewards(23162);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.BLASTER_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.BLASTER_3.getJobId());
                sm.giveItem(1142244);
                sm.completeQuestNoRewards(23163);
                sm.completeQuestNoRewards(23164);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.BLASTER_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.BLASTER_4.getJobId());
                sm.giveItem(1142245);
                sm.completeQuestNoRewards(23165);
                sm.completeQuestNoRewards(23166);
                sm.completeQuestNoRewards(23167);
                sm.completeQuestNoRewards(23168);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
