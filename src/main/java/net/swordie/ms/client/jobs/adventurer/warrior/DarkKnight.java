package net.swordie.ms.client.jobs.adventurer.warrior;

import net.swordie.ms.connection.protocol.codec.MobPacketEncoder;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.SecondAtom;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.MobAttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.LeaveType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class DarkKnight extends Warrior {

    public static final int FINAL_ATTACK = 1300002;

    public static final int SPEAR_SWEEP = 1301012;
    public static final int WEAPON_BOOSTER = 1301004;
    public static final int IRON_WILL = 1301006;
    public static final int HYPER_BODY = 1301007;
    public static final int EVIL_EYE = 1301013;
    public static final int EVIL_EYE_OF_DOMINATION = 1310013;
    public static final int EVIL_EYE_SHOCK = 1301014;
    public static final int CROSS_SURGE = 1311015;
    public static final int HEX_OF_THE_EVIL_EYE = 1310016;
    public static final int LORD_OF_DARKNESS = 1310009;
    public static final int MAPLE_WARRIOR_DARK_KNIGHT = 1321000;
    public static final int REVENGE_OF_THE_EVIL_EYE = 1320011;

    public static final int FINAL_PACT_INFO = 1320016;
    public static final int LESSER_PACT = 1320021;
    public static final int MODERATE_PACT = 1320022;
    public static final int GREATER_PACT = 1320023;
    public static final int ACCEPT_FINAL_PACT = 1321020;

    public static final int MAGIC_CRASH = 1321014;
    public static final int DARK_RESONANCE = 1321015; //Resets summon
    public static final int HEROS_WILL = 1321010;
    public static final int DARK_IMPALE = 1321012;
    public static final int GUNGNIR_DESCENT = 1321013;

    //Hyper Skills
    public static final int NIGHTSHADE_EXPLOSION = 1321052;
    public static final int EPIC_ADVENTURE = 1321053; //Lv200
    public static final int DARK_THIRST = 1321054; //Lv150

    // V Skills
    public static final int RADIANT_EVIL = 400011054;
    public static final int CALAMITOUS_CYCLONE = 400011068;
    public static final int CALAMITOUS_CYCLONE_FINAL_ATTACK = 400011069;
    public static final int DARKNESS_AURA = 400011047; // Uses SecondAtom
    public static final int DARKNESS_AURA_2 = 400011085;
    public static final int SPEAR_OF_DARKNESS = 400011004;

    // HEXA Skills
    public static final int HEXA_GUNGNIR_DESCENT = 1341000;
    public static final int HEXA_DARK_IMPALE = 1341001;
    public static final int DARK_BIDENT = 1341002;
    public static final int HEXA_NIGHTSHADE_EXPLOSION = 1341003;
    public static final int HEXA_REVENGE_OF_THE_EVIL_EYE = 1340004;
    public static final int HEXA_ENHANCED_REVENGE_OF_THE_EVIL_EYE = 1341005;
    public static final int HEXA_EVIL_EYE_SHOCK = 1341008;
    public static final int HEXA_EVIL_EYE_SHOCK_SHOOT = 1341009;
    public static final int HEXA_FINAL_ATTACK = 1340007;

    private Summon evilEye;
    private long revengeEvilEye = Long.MIN_VALUE;
    private long finishFinalPact;
    private long lastDarknessAura;

    public DarkKnight(Char chr) {
        super(chr);
    }

    @Override
    public void update(long now) {
        super.update(now);
        increaseDarknessAuraProtectiveShield(now);
    }

    public void reviveByFinalPact(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(FINAL_PACT_INFO)) return;

        Skill skill = chr.getSkill(FINAL_PACT_INFO);
        int slv = skill.getCurrentLevel();

        if (!isFinalPactAvailable(chr)) return;

        chr.heal(chr.getMaxHP(), true);
        chr.healMP(chr.getMaxMP());

        int modeSkillID = getFinalPactMode(chr);
        SkillInfo modeSi = SkillData.getSkillInfoById(modeSkillID);

        int invincibleSec = modeSi.getValue(u, slv);          // hoặc time tùy data bạn
        int missionSec    = modeSi.getValue(time, slv);       // 10/20/40 theo mô tả
        int cdSec         = modeSi.getValue(cooltime, slv);   // 173/253/893 theo mô tả
        int killsNeeded   = modeSi.getValue(z, slv);          // 24/44/52 theo mô tả

        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);

        Option o1 = new Option(); // Reincarnation
        o1.nOption = 1;
        o1.rOption = modeSkillID;
        o1.tOption = invincibleSec;
        o1.zOption = killsNeeded;
        newStats.put(Reincarnation, o1);

        Option o2 = new Option(); // NotDamaged (invincible)
        o2.nOption = 1;
        o2.rOption = modeSkillID;
        o2.tOption = invincibleSec;
        newStats.put(NotDamaged, o2);

        Option o3 = new Option(); // ReincarnationMission (window + killcount)
        o3.nOption = 1;
        o3.rOption = modeSkillID;     // IMPORTANT: lưu modeSkillID để lấy y/z/time đúng
        o3.tOption = missionSec;
        o3.zOption = killsNeeded;
        newStats.put(ReincarnationMission, o3);

        tsm.sendStat(newStats);

        chr.write(UserPacket.effect(Effect.showFinalPactEffect(FINAL_PACT_INFO, (byte) 1, 0, true)));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.showFinalPactEffect(FINAL_PACT_INFO, (byte) 1, 0, true)), chr);

        // IMPORTANT: set cooldown thật trên skill
        chr.setSkillCooldown(FINAL_PACT_INFO, cdSec);

        finishFinalPact = System.currentTimeMillis() + (missionSec * 1000L);

        chr.write(UserLocal.skillCooltimeSetM(GUNGNIR_DESCENT, 0));
    }

    public static int getFinalPactMode(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(ReincarnationOnOff)) {
            int mode = tsm.getOption(ReincarnationOnOff).nOption;
            switch (mode) {
                case 2:
                    return MODERATE_PACT;
                case 3:
                    return GREATER_PACT;
            }
        }
        return LESSER_PACT;
    }

    public void setFinalPactMode(int mode) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = ACCEPT_FINAL_PACT;
        Option o = new Option();
        o.nOption = mode;
        o.rOption = skillID;
        tsm.sendStat(ReincarnationOnOff, o);
        chr.setSkillCooldown(skillID, chr.getSkillLevel(skillID));
    }

    public static boolean isFinalPactAvailable(Char chr) {
        return chr.getRemainingCoolTime(FINAL_PACT_INFO) <= 0;
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isDarkKnight(id);
    }

    public void spawnEvilEye(int skillID, int slv) {
        Field field = chr.getField();
        evilEye = Summon.getSummonByAndSetStat(chr, skillID, slv);
        evilEye.setFlyMob(true);
        evilEye.setMoveAbility(MoveAbility.Fly);
        evilEye.setAssistType(AssistType.Heal);
        field.spawnSummon(evilEye);
    }

    public void removeEvilEye() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.removeStatsBySkill(EVIL_EYE);
        tsm.removeStat(Beholder);
        chr.getField().broadcast(Summoned.removed(evilEye, LeaveType.ANIMATION));
    }

    public void healByEvilEye() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(EVIL_EYE) && tsm.hasStatBySkillId(EVIL_EYE)) {
            Skill skill = chr.getSkill(EVIL_EYE);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            chr.heal(si.getValue(hp, slv));
        }
    }

    public void giveHexOfTheEvilEyeBuffs() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.getOptByCTSAndSkill(EPDD, HEX_OF_THE_EVIL_EYE) == null) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            Option o4 = new Option();
            Skill skill = chr.getSkill(HEX_OF_THE_EVIL_EYE);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());

            o1.nOption = si.getValue(epad, slv);
            o1.rOption = skill.getSkillId();
            o1.tOption = si.getValue(time, slv);
            newStats.put(EPAD, o1);

            o2.nOption = si.getValue(epdd, slv);
            o2.rOption = skill.getSkillId();
            o2.tOption = si.getValue(time, slv);
            newStats.put(EPDD, o2);

            o3.nReason = skill.getSkillId();
            o3.nValue = si.getValue(indieCr, slv);
            o3.tTerm = si.getValue(time, slv);
            newStats.put(IndieCrR, o3);

            o4.nOption = si.getValue(acc, slv);
            o4.rOption = skill.getSkillId();
            o4.tOption = si.getValue(time, slv);
            newStats.put(ACC, o4);
            newStats.put(EVA, o4.deepCopy());
            tsm.sendStat(newStats);
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case SPEAR_SWEEP:
                if (!mob.isBoss()) {
                    if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = Util.getRandom(1, 2);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case EVIL_EYE:
                if (tsm.getOption(Beholder).ssOption > 0) {
                    Skill skill = chr.getSkill(EVIL_EYE_SHOCK);
                    si = SkillData.getSkillInfoById(skill.getSkillId());
                    slv = skill.getCurrentLevel();

                    if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                        if (Util.succeedProp(si.getValue(prop, slv)) && !mob.isBoss()) {
                            o1.nOption = 1;
                            o1.rOption = skill.getSkillId();
                            o1.tOption = si.getValue(time, slv);
                            mts.addStatOptions(mob, MobStat.Stun, o1);
                        }
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
            //Lord of Darkness
            lordOfDarkness();

            //Final Pact
            killCountFinalPactOnMob(attackInfo);

            //Dark Thirst
            darkThirst(tsm);
        }

        Option o1 = new Option();
        switch (skillID) {
            case FINAL_ATTACK:
                for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                    Life life = chr.getField().getLifeByObjectID(mai.mobId);
                    if (life instanceof Mob mob) {
                        if (mob == null || mob.getHp() <= 0) {
                            continue;
                        }
                        long dmg = 0;
                        for (int i = 0; i < mai.damages.length; i++) {
                            dmg += mai.damages[i];
                        }
                        c.write(MobPacketEncoder.active().damaged(mob.getObjectId(), dmg, mob.getTemplateId(), (byte) 1, (int) mob.getHp(), mob.getMaxHp()));
                    }
                }
                break;
            case EVIL_EYE:
                chr.addSkillCooldown(EVIL_EYE_SHOCK, 12000);
                chr.addSkillCooldown(RADIANT_EVIL, 20000);
                break;
            case CALAMITOUS_CYCLONE_FINAL_ATTACK:
                if (hasHitMobs) {
                    chr.heal(chr.getHPPerc(10));
                }
                break;
            case DARKNESS_AURA:
                darknessAuraDrainLifeForce(attackInfo, now);
                break;
            case SPEAR_OF_DARKNESS:
                o1.nValue = si.getValue(w, slv);
                o1.nReason = SPEAR_OF_DARKNESS;
                o1.tTerm = 2;
                tsm.sendStat(IndieAllHitDamR, o1);
                break;
        }
    }

    public void increaseDarknessAuraLifeForce() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(DarknessAura)) {
            return;
        }
        Option o = tsm.getOption(DarknessAura);
        o.xOption += 1;
        var capLifeForce = chr.getSkillStatValue(s, DARKNESS_AURA);
        o.xOption = Math.max(0, Math.min(capLifeForce, o.xOption)); // cap to 15 Life Force
        tsm.updateStat(DarknessAura, o);
    }

    public void darknessAuraDrainLifeForce(AttackInfo attackInfo, long now) {
        var skillId = DARKNESS_AURA;
        if (!chr.hasSkill(skillId)) {
            return;
        }
        var slv = chr.getSkillLevel(skillId);
        var si = SkillData.getSkillInfoById(skillId);
        if (si == null || si.getSecondAtomInfos().size() <= 0) {
            return;
        }
        List<SecondAtom> secondAtoms = new LinkedList<>();
        final var mobs = chr.getField().getMobs(attackInfo.mobAttackInfo);
        if (mobs.isEmpty()) {
            return;
        }
        var sai = si.getSecondAtomInfos().get(0);
        if (sai == null) {
            return;
        }
        var mob = Util.getRandomFromCollection(mobs);
        for (int key = 0; key < si.getValue(s2, slv); key++) {
            final int randX = chr.getPosition().getX() + Util.getRandom(-200, 200);
            final int randY = chr.getPosition().getY() - Util.getRandom(100, 200);
            final var pos = new Position(randX, randY);
            SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mob != null ? mob.getObjectId() : 0, key, si.getSkillId(), pos, now);
            secondAtoms.add(fa);
        }
        chr.createSecondAtom(secondAtoms);

        // Benefits of Darkness Aura Attack
        var healed = chr.getHPPerc(chr.getSkillStatValue(x, skillId));
        var diff = chr.getMaxHP() - chr.getHP();
        if (healed > diff) {
            var overMaxHp = healed - diff;
            var multi = chr.getSkillStatValue(v, DARKNESS_AURA);
            var protectiveShield = (int) ((multi * overMaxHp) / 100D);
            increaseDarknessAuraProtectiveShield(protectiveShield);
        }
        chr.heal(healed);
        increaseDarknessAuraLifeForce();
    }

    public void increaseDarknessAuraProtectiveShield(long now) {
        if (!chr.hasSkill(DARKNESS_AURA)) {
            return;
        }
        var tsm = chr.getTemporaryStatManager();
        if (tsm.hasStatBySkillId(HYPER_BODY) && (lastDarknessAura == 0 || now - lastDarknessAura >= 1000L)) {
            var max = chr.getHPPerc(chr.getSkillStatValue(y, DARKNESS_AURA));
            increaseDarknessAuraProtectiveShield(max);
            lastDarknessAura = now;
        }
    }

    private void increaseDarknessAuraProtectiveShield(int shield) {
        var tsm = chr.getTemporaryStatManager();
        var newShield = shield;
        if (tsm.hasStat(SiphonVitalityBarrier)) {
            newShield += tsm.getOption(SiphonVitalityBarrier).nOption;
        }

        var opt = new Option();
        opt.nOption = Math.max(0, Math.min(newShield, chr.getHPPerc(chr.getSkillStatValue(y, DARKNESS_AURA))));
        opt.tOption = 40;
        tsm.sendStat(SiphonVitalityBarrier, opt);
    }


    private void darkThirst(TemporaryStatManager tsm) {
        if (tsm.getOptByCTSAndSkill(IndiePAD, DARK_THIRST) != null) {
            Skill skill = chr.getSkill(DARK_THIRST);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int heal = si.getValue(x, slv);
            chr.heal((int) (chr.getMaxHP() / ((double) 100 / heal)));
        }
    }

    public void lordOfDarkness() {
        if (chr.hasSkill(LORD_OF_DARKNESS)) {
            Skill skill = chr.getSkill(LORD_OF_DARKNESS);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            if (Util.succeedProp(proc)) {
                int heal = si.getValue(x, slv);
                chr.heal((int) (chr.getMaxHP() / ((double) 100 / heal)));
            }
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == FINAL_ATTACK) {
            if (chr.hasSkill(HEXA_FINAL_ATTACK)) return HEXA_FINAL_ATTACK;
        }
        return super.getFinalAttackSkill(faSkill);
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
        Option o3 = new Option();
        switch (skillID) {
            case IRON_WILL:
                o1.nOption = si.getValue(pdd, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(PDD, o1);
                break;
            case HYPER_BODY:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(MaxHP, o1);
                o2.nOption = si.getValue(y, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(MaxMP, o2);
                tsm.sendStat(newStats);
                break;
            case CROSS_SURGE:
                int totalHP = c.getChr().getMaxHP();
                int currentHP = c.getChr().getHP();
                o1.nOption = (int) ((si.getValue(x, slv) * ((double) currentHP) / totalHP));
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(DamR, o1);
                o2.nOption = (int) Math.min((0.08 * totalHP - currentHP), si.getValue(z, slv));
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(DamageReduce, o2);
                tsm.sendStat(newStats);
                break;
            case EVIL_EYE:
                spawnEvilEye(skillID, slv);
                if (chr.hasSkill(EVIL_EYE_OF_DOMINATION)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.sOption = EVIL_EYE_OF_DOMINATION;
                    tsm.sendStat(Beholder, o1);
                }
                break;
            case DARK_RESONANCE:
                chr.heal((chr.getMaxHP() * si.getValue(y, slv)) / 100);
                o1.nReason = skillID;
                o1.nValue = si.getValue(v, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o2);
                o2.nReason = skillID;
                o2.nValue = si.getValue(x, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieIgnoreMobpdpR, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieBDR, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieBDR, o3);
                break;
            case EPIC_ADVENTURE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case DARK_THIRST:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indiePad, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePAD, o1);
                break;
            case MAGIC_CRASH: {
                var rect = chr.getRectAround(new Rect(-500, -250, 500, 250));
                if (!chr.isLeft()) {
                    rect = rect.moveRight();
                }
                int count = 0;
                final List<Mob> mobs = chr.getField().getMobsInRect(rect);
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.cOption = chr.getId();
                for (Mob mob : mobs) {
                    if (mob != null && mob.getHp() > 0) {
                        if (count >= 10) {
                            break;
                        }
                        MobTemporaryStat mts = mob.getTemporaryStat();
                        if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                            mts.removeBuffs(mob);
                            mts.addStatOptions(mob, MobStat.MagicCrash, o1.deepCopy());
                            count += 1;
                        }
                    }
                }
                break;
            }
            case HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case DARKNESS_AURA:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.xOption = 0; // life force
                tsm.sendStat(DarknessAura, o1);
                if (tsm.hasStatBySkillId(IRON_WILL)) {
                    o2.nOption = -si.getValue(w, slv);
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(time, slv);
                    tsm.sendStat(IndieAllHitDamR, o2);
                }
                break;
        }
    }

    @Override
    public int alterCooldownSkill(int skillId) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (skillId == GUNGNIR_DESCENT) {
            if (tsm.hasStatBySkillId(DARK_RESONANCE)) {
                return 0;
            }
        }
        return super.alterCooldownSkill(skillId);
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(REVENGE_OF_THE_EVIL_EYE)) {
            Skill skill = chr.getSkill(REVENGE_OF_THE_EVIL_EYE);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int proc = si.getValue(prop, slv);
            int cd = 1000 * si.getValue(cooltime, slv);
            int heal = si.getValue(x, slv);
            if (tsm.getOptByCTSAndSkill(PDD, EVIL_EYE) != null) {
                if (cd + revengeEvilEye < System.currentTimeMillis()) {
                    if (Util.succeedProp(proc)) {
                        c.write(Summoned.beholderRevengeAttack(evilEye, hitInfo.mobID));
                        chr.heal((int) (chr.getMaxHP() / ((double) 100 / heal)));
                        revengeEvilEye = System.currentTimeMillis();
                    }
                }
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    private void finalPactPassive(int skillID) {
        Skill skill = chr.getSkill(FINAL_PACT_INFO);
        if (skill == null) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(FINAL_PACT_INFO);
        int slv = chr.getSkillLevel(FINAL_PACT_INFO);
        if (chr.getHP() >= (chr.getMaxHP() * si.getValue(x, slv) / 100.0D) && !tsm.hasStatBySkillId(skillID)) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            Option o4 = new Option();

            o1.nValue = si.getValue(damage, slv);
            o1.nReason = skillID;
            newStats.put(IndieDamR, o1);

            o2.nOption = si.getValue(psdSpeed, slv);
            o2.rOption = skillID;
            newStats.put(Speed, o2);

            o3.nValue = si.getValue(cr, slv);
            o3.nReason = skillID;
            newStats.put(IndieCrR, o3);

            o4.nValue = si.getValue(criticaldamage, slv);
            o4.nReason = skillID;
            newStats.put(IndieCD, o4);

            tsm.sendStat(newStats);
        } else if (chr.getHP() < (chr.getMaxHP() * si.getValue(x, slv) / 100.0D) && tsm.hasStatBySkillId(skillID)) {
            tsm.removeStatsBySkill(skillID);
        }
    }

    private void lowerFinalPactKillCount() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(FINAL_PACT_INFO);
        if (skill == null || !tsm.hasStat(ReincarnationMission)) return;

        Option cur = tsm.getOption(ReincarnationMission);
        int modeSkillID = cur.rOption;     // bây giờ là modeSkillID
        int killCount = cur.zOption;
        int slv = chr.getSkillLevel(FINAL_PACT_INFO);

        int duration = (int) (finishFinalPact - System.currentTimeMillis());
        if (duration <= 0) {
            tsm.removeStat(Reincarnation);
            tsm.removeStat(ReincarnationMission);
            return;
        }

        if (killCount > 0) {
            killCount--;

            if (killCount == 0) {
                SkillInfo modeSi = SkillData.getSkillInfoById(modeSkillID);
                long reduceMs = modeSi.getValue(y, slv) * 1000L;  // 70/80/300
                chr.reduceSkillCoolTime(FINAL_PACT_INFO, reduceMs);
                tsm.removeStat(Reincarnation);
                tsm.removeStat(ReincarnationMission);
                return;
            }

            Option o = new Option();
            o.setInMillis(true);
            o.nOption = 1;
            o.rOption = modeSkillID;
            o.tOption = duration;
            o.zOption = killCount;
            tsm.sendStat(ReincarnationMission, o);
        }
    }

    private void killCountFinalPactOnMob(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(ReincarnationMission)) {
            return;
        }
        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
            Life life = chr.getField().getLifeByObjectID(mai.mobId);
            if (life instanceof Mob mob) {
                if (mob != null) {
                    if (mob.isBoss()) {
                        lowerFinalPactKillCount();
                    } else {
                        long totaldmg = Arrays.stream(mai.damages).sum();

                        if (totaldmg >= mob.getHp()) {
                            lowerFinalPactKillCount();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        // hacks to bypass the quest glitch (accept but no packet)
        ExtendSP esp = chr.getAvatarData().getCharacterStat().getExtendSP();
        if (level == 30) {
            chr.completeQuest(1410);
        } else if (level == 60) {
            esp.setSpToJobLevel(3, 5);
            chr.completeQuest(1430);
        } else if (level == 100) {
            chr.completeQuest(1450);
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (cts == Reincarnation) {
            for (Option removeOpt : options) {
                if (removeOpt == null) {
                    continue;
                }
                if (removeOpt.xOption > 0) {
                    chr.die();
                }
            }
        } else if (cts == IndieEmpty) {
            if (tsm.hasStat(Beholder) && !options.isEmpty()) {
                for (Option removeOpt : options) {
                    if (removeOpt == null) {
                        continue;
                    }
                    if (removeOpt.summon != null && removeOpt.summon.getSkillID() == EVIL_EYE) {
                        tsm.removeStat(Beholder);
                        break;
                    }
                }
            }
        } else if (cts == DarknessAura) {
            chr.write(UserLocal.userBonusAttackRequest(DARKNESS_AURA_2));
            tsm.removeStat(SiphonVitalityBarrier);
        }
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case DARK_BIDENT -> {
                chr.addSkillCooldown(skillId, 6000);
                return 1;
            }
            case HEXA_NIGHTSHADE_EXPLOSION -> {
                int skillID = NIGHTSHADE_EXPLOSION;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_EVIL_EYE_SHOCK_SHOOT -> {
                int skillID = EVIL_EYE_SHOCK;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}