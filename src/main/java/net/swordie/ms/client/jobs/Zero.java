package net.swordie.ms.client.jobs;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.SPSet;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.commands.AdminCommands;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.QuestData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/**
 * Created on 12/14/2017.
 */
public class Zero extends Job {

    public static final int DUAL_COMBAT = 100001270;
    public static final int DUAL_COMBAT_2 = 100000282;
    public static final int TEMPLE_RECALL = 100001262;
    public static final int RESOLUTION_TIME = 100000279;
    public static final int BURST_JUMP = 100001265;
    public static final int BURST_STEP = 100001266;
    public static final int BURST_LEAP = 100001269;

    public static final int RHINNES_BLESSING_BOOST = 100000280;

    public static final int DIVINE_FORCE = 100001263; //Aura (Unlimited Duration)
    public static final int DIVINE_SPEED = 100001264; //Aura (Unlimited Duration)
    public static final int RHINNES_PROTECTION = 100001268; //Buff

    public static final int TIME_HOLDING = 100001274;
    public static final int TIME_HOLDING_2 = 100001281;
    public static final int TIME_DISTORTION = 100001261;
    public static final int REWIND = 100001272;
    public static final int FOCUSED_TIME = 100001005;
    public static final int DOUBLE_TIME = 100000267;
    public static final int DOUBLE_TIME_ALPHA = 100000276;
    public static final int DOUBLE_TIME_BETA = 100000277;
    public static final int SHADOW_RAIN = 100001283;

    public static final int AIR_RIOT = 101000101; //Special Attack (Stun Debuff)
    public static final int THROWING_WEAPON = 101100100; //Special Attack (Throw Sword)
    public static final int ADVANCED_THROWING_WEAPON = 101100101; //Special Attack (Throw Sword)
    public static final int THROWING_WEAPON_VI = 101141022; // 6th job (TMS 282), summon like 101100101

    public static final int TIME_GENERATOR = 101110205;

    public static final int STORM_BREAK = 101120202;
    public static final int STORM_BREAK_INIT = 101120203;
    public static final int ADV_EARTH_BREAK = 101120104;
    public static final int ADV_STORM_BREAK = 101120204;
    public static final int ADV_EARTH_BREAK_SHOCK_INIT = 101120105; //Attack to initialise the Shockwave
    public static final int ADV_STORM_BREAK_SHOCK_INIT = 101120205; //Attack to initialise the Shockwave
    public static final int ADV_EARTH_BREAK_SHOCKWAVE = 101120106; //Tile Skill
    public static final int ADV_STORM_BREAK_SHOCKWAVE = 101120206; //Tile Skill
    public static final int DIVINE_LEER = 101120207;
    public static final int CRITICAL_BIND = 101120110;
    public static final int IMMUNE_BARRIER = 101120109;
    public static final int ARMOR_SPLIT = 101110103;
    public static final int ADVANCED_WHEEL_WIND = 101110102;
    public static final int ADVANCED_SPIN_CUTTER = 101100201;
    public static final int GRAND_ROLLING_CROSS = 101110200;
    public static final int ADVANCED_ROLLING_ASSAULT = 101110203;

    // V Skills
    public static final int CHRONO_BREAK = 400011015;
    public static final int CHRONO_BREAK_DEBUFF = 400011024;
    public static final int TWIN_BLADES_OF_TIME_START = 400011039;
    public static final int SHADOW_FLASH_ALPHA_TILE = 400011098;
    public static final int SHADOW_FLASH_ALPHA_ATTACK = 400011099;
    public static final int SHADOW_FLASH_BETA_TILE = 400011100;
    public static final int SHADOW_FLASH_BETA_ATTACK = 400011101;

    public static final int EGO_WEAPON_ALPHA = 400011134; // CTS 648 (sniffed in v220.3)
    public static final int EGO_WEAPON_BETA = 400011135;

    public static final int TRANSCENDENT_RHINNE_PRAYER = 400001045;
    public static final int TRANSCENDENT_RHINNE_PRAYER_ATTACK = 400001056;


    private final int[] addedSkills = new int[]{
            DUAL_COMBAT,
            DUAL_COMBAT_2,
            TEMPLE_RECALL,
            RESOLUTION_TIME,
            BURST_STEP,
            BURST_JUMP,
            BURST_LEAP,
            DIVINE_FORCE,
            DIVINE_SPEED,
            RHINNES_PROTECTION,
            DOUBLE_TIME,
    };

    private final int[] skillsNeedMastery = new int[]{
            AIR_RIOT,
            ADVANCED_THROWING_WEAPON,
            ADV_EARTH_BREAK,
            ADVANCED_WHEEL_WIND,
            ADVANCED_SPIN_CUTTER,
            ADV_STORM_BREAK,
            GRAND_ROLLING_CROSS,
            ADVANCED_ROLLING_ASSAULT

    };
    private int doubleTimePrevSkill = 0;
    private final int regenTFRate = chr.hasSkill(TIME_GENERATOR) ? 5 + (5 * (10 + chr.getSkillLevel(TIME_GENERATOR)) / 100) : 5;
    private ScheduledFuture<?> regenTimeForceTimer;

    public Zero(Char chr) {
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
            if (chr.getZeroInfo() == null) {
                chr.initZeroInfo();
                ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(() -> chr.healMP(regenTFRate)
                        , 1000, 1000, false);
                this.regenTimeForceTimer = sf;
                GlobalTimerManager.addCharTimer(chr.getId(), sf);
            }

            if (!chr.hasQuestCompleted(QuestConstants.ZERO_WEAPON_WINDOW_QUEST)) {
                //40900: Quest: Shadowvale, Unlock Story UI
                //40905: Quest: V: Lapis Lazuli, Unlock Zero Weapon UI
                chr.completeQuest(QuestConstants.ZERO_WEAPON_WINDOW_QUEST); //enables weapon button
            }
            if (chr.getLevel() >= 200) {
                List<Skill> updateSkills = maxBaseSkillsAt200(chr, chr::addSkill);
                if (updateSkills.size() > 0) {
                    chr.write(WvsContext.changeSkillRecordResult(updateSkills, true, false, false));
                }
            }
        }
    }

    /** 282's existing prologue skip policy, without ordinary job-advance AP/SP or character recreation.
     * The caller owns the save gate and must save before writing the durable newbie receipt/rewards. */
    public boolean completePrologue() {
        if (!net.swordie.ms.client.character.tms282.Tms282SkillLearning.enabled()
                || (chr.getJob() != 10000 && chr.getJob() != 10112) || chr.getLevel() < 100
                || chr.getAvatarData().getZeroAvatarLook() == null
                || chr.getEquippedInventory().getItems().stream().noneMatch(i -> i.getItemId() / 10000 == 156)
                || chr.getEquippedInventory().getItems().stream().noneMatch(i -> i.getItemId() / 10000 == 157)) return false;

        // Load everything before mutating. Keep learned skills and the creation-time Zero SP sets intact.
        List<Skill> missing = new ArrayList<>();
        for (int id : addedSkills) {
            // Divine Speed (100001264) was removed in 282; the legacy constructor also skips absent data.
            if (!net.swordie.ms.loaders.Tms282SkillRequirements.hasOriginalSkill(id)) continue;
            if (chr.hasSkill(id)) continue;
            Skill skill = SkillData.getSkillDeepCopyById(id);
            if (skill == null) return false;
            skill.setCurrentLevel(skill.getMasterLevel());
            missing.add(skill);
        }
        for (short root : new short[]{10100, 10110, 10111, 10112}) {
            List<Skill> book = SkillData.getSkillsByJob(root);
            if (book.isEmpty()) return false;
            for (Skill skill : book) if (chr.getSkill(skill.getSkillId()) == null) missing.add(skill);
        }
        chr.getAvatarData().getCharacterStat().setJob(10112);
        chr.getAvatarData().getAvatarLook().setJob(10112);
        chr.getAvatarData().getZeroAvatarLook().setJob(10112);
        for (Skill skill : missing) {
            skill.setCharId(chr.getId());
            net.swordie.ms.client.character.tms282.Tms282SkillLearning.putWithStats(chr, skill);
        }
        if (chr.getZeroInfo() == null) chr.initZeroInfo();
        // Same weapon-UI bootstrap as the Zero constructor. No unproved story completion or Lv200 rewards.
        if (!chr.hasQuestCompleted(QuestConstants.ZERO_WEAPON_WINDOW_QUEST))
            chr.completeQuest(QuestConstants.ZERO_WEAPON_WINDOW_QUEST, true);
        chr.sendStatsPacket(Map.of(Stat.job, (short) 10112));
        if (!missing.isEmpty()) chr.write(WvsContext.changeSkillRecordResult(missing, true, false, false));
        return true;
    }

    public static int getAlphaOrBetaSkill(int skillID) {
        switch (skillID) {
            case 101001200: //Moon Strike
            case 101000200: //Piercing Thrust
            case 101000201: //Shadow Strike
            case 101000202: //Shadow Strike

            case 101101200: //Flash Assault
            case 101100200: //Spin Cutter
            case 101100201: //Adv Spin Cutter
            case 101100202: //Adv Blade Ring

            case 101110200: //Grand Rolling Cross
            case 101110201: //Grand Rolling Cross
            case 101111200: //Rolling Cross
            case 101110202: //Rolling Assault
            case 101110203: //Advanced Rolling Assault
            case 101110204: //Advanced Rolling Assault

            case 101120200: //Wind Cutter
            case 101120201: //Wind Striker
            case 101120202: //Storm Break
            case 101120203: //Storm Break
            case 101120204: //Advanced Storm Break
            case 101120205: //Severe Storm Break (Tile)
            case 101120206: //Severe Storm Break
            case 101121101: //Hurricane Wind
            case 101121200: //Wind Cutter:

            // 6th job (TMS 282 Skill.wz 10114: weapon 57 = Alpha long sword; invisible follow-ups by their parent)
            case 101141006: case 101141007: //Wind Cutter VI
            case 101141008: //Wind Striker VI
            case 101141009: case 101141010: case 101141011: //Storm Break VI
            case 101141017: case 101141018: //Grand Rolling Cross VI
            case 101141019: case 101141020: //Rolling Assault VI
            case 101141023: //Flash Assault VI
            case 101141024: case 101141025: //Spin Cutter VI
            case 101141029: //Moon Strike VI
            case 101141030: //Piercing Thrust VI
            case 101141031: case 101141032: //Shadow Strike VI
                return 1; //Alpha skills

            case 101001100: //Rising Slash
            case 101000100: //Air Raid
            case 101000101: //Air Riot
            case 101000102: //Air Riot

            case 101101100: //Flash Cut
            case 101100100: //Throwing Weapon
            case 101100101: //Adv. Throwing Weapon

            case 101111100: //Spin Driver
            case 101110101: //Wheel Wind
            case 101110102: //Adv Wheel Wind
            case 101110104: //Adv Blade Tempest

            case 101121100: //Giga Crash
            case 101120100: //Falling Star
            case 101120101: //Falling Star
            case 101120102: //Earth Break
            case 101120103: //Groundbreaker
            case 101120104: //Adv Earth Break
            case 101120105: //Mega Groundbreaker (Tile)

            // 6th job (TMS 282 Skill.wz 10114: weapon 56 = Beta heavy sword)
            case 101141000: //Giga Crash VI
            case 101141001: case 101141002: //Falling Star VI
            case 101141003: case 101141004: case 101141005: //Earth Break VI
            case 101141014: //Spin Driver VI
            case 101141015: case 101141016: //Wheel Wind VI
            case 101141021: //Flash Cut VI
            case 101141022: //Throwing Weapon VI
            case 101141026: //Rising Slash VI
            case 101141027: case 101141028: //Air Raid VI
                return 2; //Beta skills

        }
        return skillID; // no original skill linked with this one
    }

    public static void reviveByRewind(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        chr.heal(chr.getMaxHP(), true);
        tsm.removeStatsBySkill(REWIND);
        chr.chatMessage("透過時間倒轉復活了！");
        chr.write(UserPacket.effect(Effect.skillSpecial(REWIND)));
        chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillSpecial(REWIND)), chr);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isZero(id);
    }

    private boolean isBeta() {
        return chr.getZeroInfo().isZeroBetaState();
    }

    /** The existing Zero milestone policy; TMS must also meet the original skill's reqLev. */
    private static final int[][] LEVEL_SKILLS = {
            {109, RHINNES_BLESSING_BOOST, 1, 5}, {109, TIME_DISTORTION, 1, 1},
            {119, TIME_HOLDING, 1, 1}, {129, RHINNES_BLESSING_BOOST, 2, 5},
            {139, RHINNES_BLESSING_BOOST, 3, 5}, {139, REWIND, 1, 1},
            {149, RHINNES_BLESSING_BOOST, 4, 5}, {159, RHINNES_BLESSING_BOOST, 5, 5},
            {159, SHADOW_RAIN, 1, 1}, {200, FOCUSED_TIME, 1, 1}
    };

    public static List<Skill> skillsAtLevel(int level) {
        return levelSkills(level, false);
    }

    /** Free milestone skills may be repaired for an older saved character without replaying its SP. */
    public static List<Skill> availableLevelSkills(int level) {
        return levelSkills(level, true);
    }

    private static List<Skill> levelSkills(int level, boolean includeEarlier) {
        List<Skill> skills = new ArrayList<>();
        for (int[] grant : LEVEL_SKILLS) {
            if (level < grant[0]) continue;
            SkillInfo info = SkillData.getSkillInfoById(grant[1]);
            if (info == null) continue;
            int required = net.swordie.ms.client.character.tms282.Tms282SkillLearning.enabled()
                    ? Math.max(grant[0], info.getReqLevel()) : grant[0];
            if (includeEarlier ? level < required : level != required) continue;
            Skill skill = SkillData.getSkillDeepCopyById(grant[1]);
            skill.setCurrentLevel(grant[2]);
            skill.setMasterLevel(grant[3]);
            skills.add(skill);
        }
        return skills;
    }

    /** Each existing Zero SP set receives this amount, independently of ordinary job-stage SP. */
    public static int spPerSetAtLevel(int level) {
        return level >= 200 ? 0 : level > 100 && (level % 10) % 3 == 0 ? 6 : 3;
    }

    /** The existing login policy, also used at the actual TMS level transition instead of waiting for relogin. */
    public static List<Skill> maxBaseSkillsAt200(Char chr, java.util.function.Consumer<Skill> apply) {
        if (chr.getLevel() < 200 || !JobConstants.isZero(chr.getJob())) return List.of();
        List<Skill> changed = new ArrayList<>();
        for (Skill skill : List.copyOf(chr.getSkills())) {
            if ((skill.getRootId() == 10100 || skill.getRootId() == 10110 || skill.getRootId() == 10111 || skill.getRootId() == 10112)
                    && skill.getCurrentLevel() < skill.getMaxLevel()) {
                Skill maxed = skill; // GMS keeps its original in-place mutation before Char.addSkill.
                if (net.swordie.ms.client.character.tms282.Tms282SkillLearning.enabled()) {
                    // TMS replaces a copy so the old passive cache is removed using its old level.
                    maxed = new Skill();
                    maxed.setId(skill.getId()); maxed.setCharId(chr.getId()); maxed.setSkillId(skill.getSkillId());
                    maxed.setRootId(skill.getRootId()); maxed.setMaxLevel(skill.getMaxLevel());
                }
                maxed.setCurrentLevel(skill.getMaxLevel()); maxed.setMasterLevel(skill.getMaxLevel());
                apply.accept(maxed);
                changed.add(maxed);
            }
        }
        // Zero has seven saved sets; all become unusable after its base skills are maxed.
        for (SPSet set : chr.getAvatarData().getCharacterStat().getExtendSP().getSpSet())
            if (set != null && set.getSp() > 0) set.setSp(0);
        return changed;
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        chr.addStat(Stat.mhp, 500);
        chr.addStat(Stat.ap, 5);
        chr.addHonorExp(700 + ((chr.getLevel() - 50) / 10) * 100);

        for (Skill skill : skillsAtLevel(level))
            chr.addSkill(skill.getSkillId(), skill.getCurrentLevel(), skill.getMasterLevel());
        if (level == 109) {
            if (!chr.hasQuestCompleted(QuestConstants.ZERO_WEAPON_WINDOW_QUEST)) {
                chr.completeQuest(QuestConstants.ZERO_WEAPON_WINDOW_QUEST); //enables weapon button
            }
        }
        if (level == 200) completeLevel200Milestone();

        int sp = spPerSetAtLevel(level);
        if (sp == 6) {
            if (level >= 110) {
                Quest q = QuestData.createQuestFromId(QuestConstants.ZERO_SET_QUEST, chr.getId());
                q.setQrValue(String.valueOf(0));
                chr.addQuest(q);
            }
        }
        if (level < 200) {
            ExtendSP esp = chr.getAvatarData().getCharacterStat().getExtendSP();
            for (SPSet spSet : esp.getSpSet()) {
                spSet.addSp(sp);
            }
        }
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.mhp, chr.getStat(Stat.mhp));
        stats.put(Stat.mmp, chr.getStat(Stat.mmp));
        stats.put(Stat.ap, (short) chr.getStat(Stat.ap));
        stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
        chr.sendStatsPacket(stats);
    }

    /** Reusable after TMS numeric level-up; the durable quest gate makes repeat/failed callbacks safe. */
    public void completeLevel200Milestone() {
        if (net.swordie.ms.client.character.QuestShortcut.enabled()) {
            synchronized (net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
                synchronized (chr) { synchronized (net.swordie.ms.client.character.QuestShortcut.monitor(chr)) {
                    completeFifthJobOnLevel200();
                } }
            }
        } else completeFifthJobOnLevel200();
    }

    /** The native200-level path must earn and persist the same1465 transition as the real quest script. */
    private void completeFifthJobOnLevel200() {
        boolean tms = net.swordie.ms.client.character.QuestShortcut.enabled();
        if (tms) {
            if (!JobConstants.isZero(chr.getJob()) || chr.getLevel() < 200 || chr.hasQuestCompleted(1465)
                    || !chr.hasQuestCompleted(1461)) return;
            if (!chr.hasQuestInProgress(1465)) {
                if (!chr.canStartQuest(1465)) return;
                var started = net.swordie.ms.client.character.quest.Tms282QuestService.markStarted(chr, 1465);
                if (!started.created() && started.status() != net.swordie.ms.enums.QuestStatus.Started) return;
            }
        }
        chr.completeQuest(1465); // TMS: ready Started record, strict commit before any reward; failure throws.
        if (tms && !chr.hasQuestCompleted(1465)) return;
        ScriptManagerImpl sm = chr.getScriptManager();
        chr.sendRewardToChar(2435770, 1, 0, "恭喜神之子達到 200 級！", 30);
        sm.playSound("Sound/SoundEff.img/5thJob");
        sm.showFieldEffect("Effect/5skill.img/screen");
        sm.avatarOriented("Effect/5skill.img/character_delayed");
        AdminCommands.MaxSkills.execute(chr, null); // existing200-level skill completion
    }

    @Override
    public void setCharCreationStats(Char chr) {
        AvatarLook alphaLook = chr.getAvatarData().getAvatarLook();
        chr.getAvatarData().setZeroAvatarLook(alphaLook.deepCopy());
        AvatarLook betaLook = chr.getAvatarData().getZeroAvatarLook();

        //alphaLook.getHairEquips().remove(alphaLook.getHairEquips().indexOf(1562000)); -> Delete this so Alpha dont have Beta Weapon ID.
        betaLook.getHairEquips().remove((Integer) 1572000);

        betaLook.setWeaponId(1562000);
        betaLook.setGender(1);
        betaLook.setSkin(chr.getAvatarData().getAvatarLook().getSkin());
        betaLook.setFace(21290);
        betaLook.setHair(37623);
        betaLook.setZeroBetaLook(true);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setLevel(100);
        cs.setStr(518);
        cs.setHp(5000);
        cs.setMaxHp(5000);
        cs.setMp(100);
        cs.setMaxMp(100);
        cs.setJob(10000);
        cs.setPosMap(JobConstants.ZERO_CREATION_MAP);
        ExtendSP esp = chr.getAvatarData().getCharacterStat().getExtendSP();
        for (SPSet spSet : esp.getSpSet()) {
            spSet.addSp(6);
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        if (getAlphaOrBetaSkill(skillID) == 1) {
            applyDivineLeerOnMob(mob, skillID, damage);
        }
        if (getAlphaOrBetaSkill(skillID) == 2) {
            applyCriticalBindOnMob(mob, skillID);
            applyArmorSplitOnMob(mob);
        }
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case AIR_RIOT:
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o1);
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
        if (chr.hasSkill(TIME_GENERATOR)) {
            SkillInfo tg = SkillData.getSkillInfoById(TIME_GENERATOR);
            byte skilllv = (byte) chr.getSkill(TIME_GENERATOR).getCurrentLevel();
            if (Util.succeedProp(tg.getValue(y, skilllv))) {
                chr.healMP(tg.getValue(z, skilllv));
                chr.write(UserLocal.zeroCombatRecovery(skilllv, tg.getValue(z, skilllv)));
            }
        }
        if (getAlphaOrBetaSkill(skillID) == 1) {
            if (hasHitMobs) {
                incrementDoubleTimeAlpha(skillID);
            }
        }
        if (getAlphaOrBetaSkill(skillID) == 2) {
            if (hasHitMobs) {
                incrementDoubleTimeBeta(skillID);
            }
        }
        switch (skillID) {
            case ADV_EARTH_BREAK_SHOCK_INIT:
                slv = (byte) chr.getSkill(ADV_EARTH_BREAK).getCurrentLevel();
                SkillInfo fci = SkillData.getSkillInfoById(ADV_EARTH_BREAK);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, ADV_EARTH_BREAK, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setSkillID(ADV_EARTH_BREAK);
                aa.setRect(aa.getPosition().getRectAround(fci.getRects().get(0)));
                aa.setDuration(fci.getValue(v, slv) * 1000);
                chr.getField().spawnAffectedArea(aa);
                break;
            case CHRONO_BREAK:
                Option o1 = new Option();
                Option o2 = new Option();
                Option o3 = new Option();
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                o1.nReason = skillID;
                o1.nValue = 2000 + 20 * slv;
                o1.tTerm = 30 + (slv / 2);
                newStats.put(IndieCooltimeReduce, o1); //Indie
                o2.nReason = skillID;
                o2.nValue = -2;
                o2.tTerm = 30 + (slv / 2);
                newStats.put(IndieBooster, o2); //Indie
                o3.nReason = skillID;
                o3.nValue = 15 + slv / 2;
                o3.tTerm = 30 + (slv / 2);
                newStats.put(IndiePMdR, o3); //Indie
                tsm.sendStat(newStats);
                break;
            case SHADOW_FLASH_ALPHA_ATTACK:
                chr.getField().removeAffectedArea(SHADOW_FLASH_ALPHA_TILE, chr.getId());
                break;
            case SHADOW_FLASH_BETA_ATTACK:
                chr.getField().removeAffectedArea(SHADOW_FLASH_BETA_TILE, chr.getId());
                break;
            case SHADOW_FLASH_ALPHA_TILE:
            case SHADOW_FLASH_BETA_TILE:
                aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                aa.setDelay((short) 5);
                chr.getField().spawnAffectedAreaAndRemoveOld(aa);
                break;

        }
    }

    private void applyDivineLeerOnMob(Mob mob, int skillID, long damage) {
        Skill skill = chr.getSkill(DIVINE_LEER);
        if (skill == null) {
            return;
        }
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(DIVINE_LEER);
        if (Util.succeedProp(si.getValue(prop, slv))) {
            BurnedInfo bi = BurnedInfo.createBurnInfo(chr, skillID, slv, damage);
            mob.getTemporaryStat().createAndAddBurnedInfo(mob, bi, skillID);
        }
    }

    private void applyCriticalBindOnMob(Mob mob, int skillID) {
        Skill skill = chr.getSkill(CRITICAL_BIND);
        if (skill == null) {
            return;
        }
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(CRITICAL_BIND);
        if (Util.succeedProp(si.getValue(prop, slv))) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
            Option o = new Option();
            Option o1 = new Option();
            o.nOption = 1;
            o.rOption = CRITICAL_BIND;
            o.tOption = 4;
            map.put(MobStat.Freeze, o);
            o1.nOption = si.getValue(SkillStat.x, slv);
            o1.rOption = skillID;
            o1.tOption = 4;//   si.getValue(time, slv);
            map.put(MobStat.TotalDamParty, o1);
            mts.addStatOptions(mob, map);
        }
    }

    private void applyArmorSplitOnMob(Mob mob) {
        Skill skill = chr.getSkill(ARMOR_SPLIT);
        if (skill == null) {
            return;
        }
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(ARMOR_SPLIT);
        int amount = 1;
        if (Util.succeedProp(si.getValue(prop, slv))) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            Option o = new Option();
            if (mts.hasCurrentMobStat(MobStat.MultiPMDR)) {
                amount = mts.getCurrentOptionsByMobStat(MobStat.MultiPMDR).cOption;
                if (amount < si.getValue(x, slv)) {
                    amount++;
                }
            }
            o.nOption = si.getValue(y, slv) * amount;
            o.rOption = ARMOR_SPLIT;
            o.tOption = si.getValue(time, slv);
            o.cOption = amount;
            mts.addStatOptions(mob, MobStat.MultiPMDR, o);
        }
    }

    private void incrementDoubleTimeAlpha(int skillID) {
        if (chr.hasSkill(DOUBLE_TIME)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o = new Option();
            Option o1 = new Option();
            SkillInfo si = SkillData.getSkillInfoById(DOUBLE_TIME_ALPHA);
            int amount = 1;
            if (tsm.hasStat(TimeFastABuff)) {
                if (doubleTimePrevSkill == skillID) {
                    return;
                }
                amount = tsm.getOption(TimeFastABuff).nOption;
                if (amount < 10) {
                    amount++;
                }
            }
            doubleTimePrevSkill = skillID;
            o.nOption = amount;
            o.rOption = DOUBLE_TIME_ALPHA;
            o.tOption = tsm.hasStatBySkillId(TRANSCENDENT_RHINNE_PRAYER) ? 0 : 20;
            tsm.sendStat(TimeFastABuff, o);
        }
    }

    private void incrementDoubleTimeBeta(int skillID) {
        if (chr.hasSkill(DOUBLE_TIME)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            Option o = new Option();
            Option o1 = new Option();
            SkillInfo si = SkillData.getSkillInfoById(DOUBLE_TIME_BETA);
            int amount = 1;
            if (tsm.hasStat(TimeFastBBuff)) {
                if (doubleTimePrevSkill == skillID) {
                    return;
                }
                amount = tsm.getOption(TimeFastBBuff).nOption;
                if (amount < 10) {
                    amount++;
                }
            }
            doubleTimePrevSkill = skillID;
            o.nOption = amount;
            o.rOption = DOUBLE_TIME_BETA;
            o.tOption = tsm.hasStatBySkillId(TRANSCENDENT_RHINNE_PRAYER) ? 0 : 20;
            tsm.sendStat(TimeFastBBuff, o);
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
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        Option o6 = new Option();
        switch (skillID) {
            case DIVINE_FORCE:
                if (tsm.hasStatBySkillId(DIVINE_FORCE) || tsm.hasStatBySkillId(DIVINE_SPEED)) {
                    tsm.removeStatsBySkill(DIVINE_FORCE);
                    tsm.removeStatsBySkill(DIVINE_SPEED);
                }
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieAsrR, slv);
                newStats.put(IndieAsrR, o1); //Indie
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieMad, slv);
                newStats.put(IndieMAD, o2); //Indie
                o3.nReason = skillID;
                o3.nValue = si.getValue(indiePad, slv);
                newStats.put(IndiePAD, o3); //Indie
                o4.nReason = skillID;
                o4.nValue = si.getValue(indiePdd, slv);
                newStats.put(IndiePDD, o4); //Indie
                o5.nReason = skillID;
                o5.nValue = si.getValue(indieTerR, slv);
                newStats.put(IndieTerR, o5); //Indie
                o6.nOption = 1;
                o6.rOption = skillID;
                newStats.put(ZeroAuraStr, o6);
                tsm.sendStat(newStats);
                break;
            case DIVINE_SPEED:
                if (tsm.hasStatBySkillId(DIVINE_FORCE) || tsm.hasStatBySkillId(DIVINE_SPEED)) {
                    tsm.removeStatsBySkill(DIVINE_FORCE);
                    tsm.removeStatsBySkill(DIVINE_SPEED);
                }
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieAcc, slv);
                newStats.put(IndieACC, o1); //Indie
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieBooster, slv);
                newStats.put(IndieBooster, o2); //Indie
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieEva, slv);
                newStats.put(IndieEVA, o3); //Indie
                o4.nReason = skillID;
                o4.nValue = si.getValue(indieJump, slv);
                newStats.put(IndieJump, o4); //Indie
                o4.nReason = skillID;
                o4.nValue = si.getValue(indieSpeed, slv);
                newStats.put(IndieSpeed, o4); //Indie
                tsm.sendStat(newStats);
                break;
            case TIME_HOLDING:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(NotDamaged, o1);

                if (chr.getStat(Stat.level) >= 200) {
                    o2.nOption = si.getValue(y, slv);
                    o2.rOption = TIME_HOLDING_2;
                    o2.tOption = si.getValue(x, slv);
                    newStats.put(DamR, o2);
                }

                tsm.sendStat(newStats);

                for (int skillId : chr.getSkillCoolTimes().keySet()) {
                    chr.resetSkillCoolTime(skillId);
                }
                break;
            case REWIND:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ReviveOnce, o1);
                break;
            case CHRONO_BREAK:
                o1.nOption = 1;
                o1.rOption = CHRONO_BREAK_DEBUFF;
                o1.tOption = 1;
                for (Mob mob : chr.getField().getMobs()) {
                    if (mob.getHp() <= 0) {
                        continue;
                    }
                    MobTemporaryStat mts = mob.getTemporaryStat();
                    if (!mts.hasCurrentMobStatBySkillId(CHRONO_BREAK_DEBUFF)) {
                        mts.addStatOptions(mob, MobStat.AddDamSkill, o1.deepCopy());
                    }
                }
                break;
            case FOCUSED_TIME:
                o1.nReason = skillID;
                o1.nValue = 4;
                o1.tTerm = 2400;
                newStats.put(IndiePADR, o1); //Indie
                o2.nReason = skillID;
                o2.nValue = 4;
                o2.tTerm = 2400;
                newStats.put(IndieMADR, o2); //Indie
                tsm.sendStat(newStats);
                break;
            case THROWING_WEAPON:
            case ADVANCED_THROWING_WEAPON:
            case THROWING_WEAPON_VI: // 282 Skill.wz 10114: summon node + info type 33, like 101100101
                Summon summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.FixVMove);
                chr.getField().spawnSummon(summon);
                break;
            case TEMPLE_RECALL:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case TIME_DISTORTION:
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                aa.setDelay((short) 5);
                chr.getField().spawnAffectedArea(aa);
                break;
            case TRANSCENDENT_RHINNE_PRAYER:
                for (Integer sid : chr.getSkillCoolTimes().keySet()) {
                    if (sid != skillID
                            && !JobConstants.isZero(chr.getJob())
                            && SkillConstants.isHyperSkill(chr.getJob(), sid)) {
                        chr.removeSkillCoolTime(sid);
                    }
                }
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(FifthGoddessBless, o1);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                o2.nValue = si.getValue(indiePad, slv);
                newStats.put(IndiePAD, o2);
                tsm.sendStat(newStats);
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill immuneBarrier = chr.getSkill(IMMUNE_BARRIER);
        if (immuneBarrier == null) {
            return;
        }
        byte slv = (byte) immuneBarrier.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(IMMUNE_BARRIER);
        if (Util.succeedProp(si.getValue(prop, slv))) {
            Option o = new Option(IMMUNE_BARRIER, slv);
            int max = (int) (chr.getStat(Stat.mhp) * (si.getValue(x, slv) / 100D));
            o.nOption = max;
            o.xOption = max;
            tsm.sendStat(ImmuneBarrier, o);
        }
        if (tsm.hasStat(ImmuneBarrier)) {
            Option o = tsm.getOption(ImmuneBarrier);
            int maxSoakDamage = o.nOption;
            int newDamage = Math.max(hitInfo.hpDamage - maxSoakDamage, 0);
            o.nOption = maxSoakDamage - (hitInfo.hpDamage - newDamage); // update soak value
            hitInfo.hpDamage = newDamage;
            o.tOption = tsm.hasStatBySkillId(TRANSCENDENT_RHINNE_PRAYER) ? 0 : si.getValue(time, slv); //added duration
            tsm.sendStat(ImmuneBarrier, o);
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (regenTimeForceTimer != null) {
            regenTimeForceTimer.cancel(true);
            regenTimeForceTimer = null;
        }
        super.handleCancelTimer(chr);
    }
}

