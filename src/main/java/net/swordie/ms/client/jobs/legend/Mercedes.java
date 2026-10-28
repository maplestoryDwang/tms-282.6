package net.swordie.ms.client.jobs.legend;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Summoned;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.InvType.EQUIPPED;

public class Mercedes extends Job {
    //Link Skill = return skill

    public static final int ELVEN_GRACE = 20020112;
    public static final int UPDRAFT = 20020111;
    public static final int ELVEN_HEALING = 20020109;

    public static final int DUAL_BOWGUN_BOOSTER = 23101002; //Buff

    public static final int STUNNING_STRIKES = 23111000; //Special Attack
    public static final int UNICORN_SPIKE = 23111002; //Special Attack
    public static final int IGNIS_ROAR = 23110004; //Passive
    public static final int WATER_SHIELD = 23111005; //Buff
    public static final int ELEMENTAL_KNIGHTS_BLUE = 23111008; //Summon
    public static final int ELEMENTAL_KNIGHTS_RED = 23111009; //Summon
    public static final int ELEMENTAL_KNIGHTS_PURPLE = 23111010; //Summon

    public static final int ROLLING_MOONSAULT = 23121011;

    public static final int SPIKES_ROYALE = 23121002;  //Special Attack
    public static final int STAGGERING_STRIKES = 23120013; //Special Attack
    public static final int ANCIENT_WARDING = 23121004; //Buff
    public static final int MAPLE_WARRIOR_MERC = 23121005; //Buff
    public static final int LIGHTNING_EDGE = 23121003; //Debuff mobs
    public static final int HEROS_WILL_MERC = 23121008;
    public static final int SPIRIT_NIMBLE_FLIGHT = 23121014; // Spirit Nimble Flight
    public static final int SPIRIT_NIMBLE_FLIGHT_2 = 23121015; // Spirit Nimble Flight

    public static final int HEROIC_MEMORIES_MERC = 23121053;
    public static final int ELVISH_BLESSING = 23121054;
    public static final int WRATH_OF_ENLIL = 23121052;

    //Final Attack
    public static final int FINAL_ATTACK_DBG = 23100006;
    public static final int ADVANCED_FINAL_ATTACK = 23120012;

    // V Skills
    public static final int SPIRIT_OF_ELLUEL = 400031007;
    public static final int SPIRIT_OF_ELLUEL_1 = 400031008;
    public static final int SPIRIT_OF_ELLUEL_2 = 400031009;
    public static final int SYLVIDIAS_FLIGHT = 400031017;
    public static final int BREATH_OF_IRKALLA = 400031011;
    public static final int IRKILLAS_WRATH = 400031024;
    public static final int ROYAL_KNIGHTS = 400031044;
    public static final int ROYAL_KNIGHTS_SA = 400031045;

    // HEXA Skills
    public static final int HEXA_SPIRIT_OF_ELLUEL = 500061046;
    public static final int HEXA_SPIRIT_OF_ELLUEL_1 = 500061047;
    public static final int HEXA_SPIRIT_OF_ELLUEL_2 = 500061048;
    public static final int HEXA_FINAL_ATTACK_DBG = 23140017;

    private final int[] summonAttacks = new int[]{
            ELEMENTAL_KNIGHTS_BLUE,
            ELEMENTAL_KNIGHTS_RED,
            ELEMENTAL_KNIGHTS_PURPLE,};

    private final int[] addedSkills = new int[]{
            ELVEN_GRACE,
            UPDRAFT,
            ELVEN_HEALING,};

    private int eleKnightSummonID = 1;
    private int lastAttackSkill = 0;
    private long lastElvenHealing = 0L;
    private List<Summon> summonList = new ArrayList<>();

    public Mercedes(Char chr) {
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
        return JobConstants.isMercedes(id);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(JobConstants.MERCEDES_CREATION_MAP);
        cs.setLevel(10);
        cs.setStr(4);
        cs.setDex(58);
        cs.setMaxHp(300);
        cs.setMaxMp(200);
    }

    @Override
    public void addItemToNewCharacter(Char chr) {
        super.addItemToNewCharacter(chr);
        chr.getAvatarData().getAvatarLook().setDrawElfEar(true);
        chr.getAvatarData().getAvatarLook().updateAvatarLookToSQL();
    }

    private void summonEleKnights() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        List<Integer> set = new ArrayList<>();
        set.add(ELEMENTAL_KNIGHTS_BLUE);
        set.add(ELEMENTAL_KNIGHTS_RED);
        set.add(ELEMENTAL_KNIGHTS_PURPLE);
        if (eleKnightSummonID != 0) {
            set.removeIf(x -> x == eleKnightSummonID);
        }
        int random = Util.getRandomFromCollection(set);
        eleKnightSummonID = random;
        Summon summon = Summon.getSummonByAndSetStat(chr, random, (byte) 1);
        Field field = chr.getField();
        summon.setMoveAbility(MoveAbility.FlyRandom);
        summon.setSummonTerm(0);
        summon.setAssistType(AssistType.TeleportToMobs);
        summon.setAttackActive(true);
        summon.setBeforeFirstAttack(true);
        summonList.add(summon);
        if (summonList.size() > 2) {
            Summon s = summonList.get(0);
            if (s != null) {
                c.write(Summoned.removed(s, LeaveType.ANIMATION));
                tsm.removeStatsBySkill(s.getSkillID());
                summonList.removeIf(x -> x.getSkillID() == s.getSkillID() &&
                        (x.getOwnerId() == chr.getId()));
            }
        }
        field.spawnSummon(summon);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);

        if (!chr.hasSkillOnCooldown(BREATH_OF_IRKALLA)
                && (tsm.getOptByCTSAndSkill(IndieEmpty, SPIRIT_OF_ELLUEL) != null
                || tsm.getOptByCTSAndSkill(IndieEmpty, HEXA_SPIRIT_OF_ELLUEL) != null)
                && Util.succeedProp(50)) {
            chr.write(UserLocal.userBonusAttackRequest(BREATH_OF_IRKALLA));
        }
        switch (skillID) {
            case STUNNING_STRIKES:
            case STAGGERING_STRIKES:
                si = SkillData.getSkillInfoById(skillID);
                int procc = si.getValue(prop, slv);
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(procc) && !mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case UNICORN_SPIKE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = si.getValue(x, slv);
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.TotalDamParty, o1);
                    }
                }
                break;
            case SPIKES_ROYALE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = -si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    map.put(MobStat.PDR, o1);
                    map.put(MobStat.MDR, o1.deepCopy());
                    mts.addStatOptions(mob, map);
                }
                break;
            case LIGHTNING_EDGE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.AddDamSkill, o1);
                }
                break;
            case ELEMENTAL_KNIGHTS_BLUE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = 3 + (slv / 5);
                        mts.addStatOptions(mob, MobStat.Freeze, o1);
                    }
                }
                break;
            case ELEMENTAL_KNIGHTS_RED:
                Skill skill = SkillData.getSkillDeepCopyById(ELEMENTAL_KNIGHTS_RED);
                if (skill != null) {
                    skill.setCurrentLevel(chr.getSkillLevel(ELEMENTAL_KNIGHTS_BLUE));
                    BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skill.getSkillId(), skill.getCurrentLevel(), damage);
                    mts.createAndAddBurnedInfo(mob, bi, skill.getSkillId());
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
        if (chr.hasSkill(ELVEN_HEALING)) {
            if (now - lastElvenHealing >= 4000) {
                if (chr.getMaxHP() > chr.getHP()) {
                    chr.heal((int) (5 * chr.getMaxHP() / 100.0D));
                }
                if (chr.getMaxMP() > chr.getMP()) {
                    chr.healMP((int) (5 * chr.getMaxMP() / 100.0D));
                }
                lastElvenHealing = now;
            }
        }
        if (hasHitMobs) {
            incrementIgnisRoarStackCount(tsm, attackInfo);
        }
        switch (attackInfo.skillId) {
            case BREATH_OF_IRKALLA:
                chr.addSkillCoolTime(BREATH_OF_IRKALLA, Util.getCurrentTimeLong() + 10000);
                break;
        }
    }

    private void incrementIgnisRoarStackCount(TemporaryStatManager tsm, AttackInfo attackInfo) {
        if (Collections.singletonList(summonAttacks).contains(attackInfo.skillId) || attackInfo.skillId == lastAttackSkill) {
            return;
        }
        Option o1 = new Option();
        Option o2 = new Option();
        Skill skill = chr.getSkill(IGNIS_ROAR);
        if (skill == null) {
            return;
        }
        SkillInfo ignisRoarInfo = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int amount = 1;
        SkillInfo si = SkillData.getSkillInfoById(lastAttackSkill);
        lastAttackSkill = attackInfo.skillId;
        if (si == null) {
            return;
        }
        boolean isMatch = false;
        for (int skillID : si.getAddAttackSkills()) {
            if (skillID == attackInfo.skillId) {
                isMatch = true;
                break;
            }
        }
        if (!isMatch) {
            return;
        }
        if (attackInfo.skillId != SPIKES_ROYALE) {
            chr.reduceSkillCoolTime(SPIKES_ROYALE, 1000);
        }
        if (attackInfo.skillId != UNICORN_SPIKE) {
            chr.reduceSkillCoolTime(UNICORN_SPIKE, 1000);
        }
        if (attackInfo.skillId != WRATH_OF_ENLIL) {
            chr.reduceSkillCoolTime(WRATH_OF_ENLIL, 1000);
        }
        if (tsm.hasStat(RpSiksin)) {
            if (tsm.hasStat(AddAttackCount)) {
                amount = tsm.getOption(AddAttackCount).nOption;
                if (amount < ignisRoarInfo.getValue(y, slv)) {
                    amount++;
                }
            }
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            o1.nOption = (amount * ignisRoarInfo.getValue(x, slv));
            o1.rOption = IGNIS_ROAR;
            o1.tOption = ignisRoarInfo.getValue(subTime, slv);
            newStats.put(DamR, o1);
            o2.nOption = amount;
            o2.rOption = IGNIS_ROAR;
            o2.tOption = ignisRoarInfo.getValue(subTime, slv);
            newStats.put(AddAttackCount, o2);
            tsm.sendStat(newStats);
        }
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (faSkill == FINAL_ATTACK_DBG) {
            if (chr.hasSkill(HEXA_FINAL_ATTACK_DBG)) return HEXA_FINAL_ATTACK_DBG;
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
        Option o3 = new Option();
        switch (skillID) {
            case WATER_SHIELD:
                o1.nOption = si.getValue(asrR, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(AsrR, o1);
                o2.nOption = si.getValue(terR, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(TerR, o2);
                o3.nOption = si.getValue(x, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(DamAbsorbShield, o3);   //IgnoreMobDamR
                tsm.sendStat(newStats);
                break;
            case ANCIENT_WARDING:
                o1.nOption = si.getValue(emhp, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(EMHP, o1);
                o2.nValue = si.getValue(indiePadR, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePADR, o2);
                tsm.sendStat(newStats);
                break;
            case HEROIC_MEMORIES_MERC:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case ELVISH_BLESSING:
                o1.nValue = si.getValue(indiePad, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o1);
                o2.nOption = si.getValue(x, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Stance, o2);
                tsm.sendStat(newStats);
                break;
            case ELEMENTAL_KNIGHTS_BLUE:
                summonEleKnights();
                break;
            case HEROS_WILL_MERC:
                tsm.removeAllDebuffs();
                break;
            case SPIRIT_OF_ELLUEL:
            case HEXA_SPIRIT_OF_ELLUEL:
                for (int i = skillID; i < skillID + 3; i++) {
                    Summon summon = Summon.getSummonByAndSetStat(chr, i, slv);
                    summon.setFlyMob(false);
                    summon.setAvatarLook(chr.getAvatarData().getAvatarLook());
                    summon.setMoveAbility(MoveAbility.WalkClone);
                    summon.setActionDelay((i - (skillID - 1)) * 400);
                    summon.setMovementDelay((i - (skillID - 1)) * 30);
                    chr.getField().spawnSummon(summon);
                }
                break;
            case SYLVIDIAS_FLIGHT:
                TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);
                if (tsm.hasStat(RideVehicle)) {
                    tsm.removeStat(RideVehicle);
                }
                tsb.setNOption(si.getVehicleId());
                tsb.setROption(skillID);
                tsb.setExpireTerm(si.getValue(time, slv));
                tsm.sendStat(RideVehicle, tsb.getOption());
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieStance, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieStance, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePadR, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePADR, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieDamReduceR, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamReduceR, o3);
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
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.MERCEDES1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MERCEDES2.getJobId());
                sm.giveItem(1142337);
                sm.completeQuestNoRewards(24010);
                sm.completeQuestNoRewards(24011);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.MERCEDES2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MERCEDES3.getJobId());
                sm.giveItem(1142338);
                sm.completeQuestNoRewards(24012);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.MERCEDES3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MERCEDES4.getJobId());
                sm.giveItem(1142339);
                sm.completeQuestNoRewards(24013);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case SPIRIT_NIMBLE_FLIGHT_2 -> {
                int skillID = SPIRIT_NIMBLE_FLIGHT;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
