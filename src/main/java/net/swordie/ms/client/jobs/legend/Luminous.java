package net.swordie.ms.client.jobs.legend;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.LarknessManager;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.enums.Stat;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;

import java.util.EnumMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Luminous extends Job {

    public static final int SUNFIRE = 20040216;
    public static final int ECLIPSE = 20040217;
    public static final int EQUILIBRIUM_DARK = 20040219;
    public static final int EQUILIBRIUM_LIGHT = 20040220;
    public static final int INNER_LIGHT = 20040221;
    public static final int FLASH_BLINK = 20041222;
    public static final int CHANGE_LIGHT_DARK = 20041239;
    public static final int MAGIC_BOOSTER = 27101004; //Buff
    public static final int PRESSURE_VOID = 27101202;

    public static final int STANDARD_MAGIC_GUARD = 27000003;
    public static final int FLASH_SHOWER = 27001100;
    public static final int ABYSSALL_DROP = 27001201;
    public static final int LIGHT_AFFINITY = 27000106;
    public static final int DARK_AFFINITY = 27000207;


    public static final int SHADOW_SHELL = 27111004; //Buff
    public static final int RAY_OF_REDEMPTION = 27111101; // Attack + heals party members
    public static final int DUSK_GUARD = 27111005; //Buff
    public static final int PHOTIC_MEDITATION = 27111006; //Buff
    public static final int LUNAR_TIDE = 27110007;
    public static final int DEATH_SCYTHE = 27111303;

    public static final int DARK_CRESCENDO = 27121005; //Buff
    public static final int ARCANE_PITCH = 27121006; //Buff
    public static final int MAPLE_WARRIOR_LUMI = 27121009; //Buff
    public static final int ENDER = 27121303;
    public static final int DARKNESS_MASTERY = 27120008;
    public static final int HEROS_WILL_LUMI = 27121010;
    public static final int MORNING_STAR = 27121201;

    public static final int EQUALIZE = 27121054;
    public static final int HEROIC_MEMORIES_LUMI = 27121053;
    public static final int ARMAGEDDON = 27121052; //Stun debuff

    // V skills
    public static final int GATE_OF_LIGHT = 400021005;
    public static final int AETHER_CONDUIT_L = 400021041;
    public static final int AETHER_CONDUIT_D = 400021049;
    public static final int AETHER_CONDUIT_EQ = 400021050;
    public static final int BAPTISM_OF_LIGHT_AND_DARKNESS = 400021071;
    public static final int LIBERATION_ORB = 400021105;
    public static final int LIBERATION_ORB_IMBALANCE_ATOM = 400021106; // Active Imbalance ForceAtom
    public static final int LIBERATION_ORB_LIGHT_ZONE = 400021107; // Passive Light
    public static final int LIBERATION_ORB_DARK_BULLET = 400021108; // Passive Dark
    public static final int LIBERATION_ORB_IMBALANCE_ATTACK = 400021109;
    public static final int LIBERATION_ORB_BALANCE_ATTACK = 400021110;

    private final int[] addedSkills = new int[]{
            EQUILIBRIUM_DARK,
            EQUILIBRIUM_LIGHT,
            CHANGE_LIGHT_DARK,
            SUNFIRE,
            ECLIPSE,
            INNER_LIGHT,
            FLASH_BLINK
    };

    private long darkCrescendoTimer;
    private ScheduledFuture<?> equilibriumTimer;

    public Luminous(Char chr) {
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
            if (chr.getTemporaryStatManager().getLarknessManager() == null) {
                chr.getTemporaryStatManager().setLarknessManager(new LarknessManager(chr));
            }
        }
    }

   /*public static void changeBlackBlessingCount(Char chr, boolean increment) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        int amount = 1;
        if (tsm.hasStat(BlessOfDarkness)) {
            amount = tsm.getOption(BlessOfDarkness).nOption;

            if (increment) {
                if (amount < 2) {
                    amount += 2;
                }
            } else {
                if (amount > 0) {
                    amount--;
                }
            }
        }

        int orbmad = switch (amount) {
            case 1 -> 15;
            case 2 -> 24;
            case 3 -> 30;
            default -> 0;
        };

        if (amount > 0) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            o1.nOption = amount;
            o1.rOption = BLACK_BLESSING;
            newStats.put(BlessOfDarkness, o1);
            o2.nOption = orbmad;
            o2.rOption = BLACK_BLESSING;
            newStats.put(MAD, o2);
            tsm.sendStat(newStats);
        }
    }*/

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isLuminous(id);
    }

    private void changeLarknessState(int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        LarknessManager lm = tsm.getLarknessManager();
        Option o = new Option();
        if (SkillConstants.isLarknessLightSkill(skillID)) {
            lm.addGauge(200, false);
            if (lm.getGauge() == 10000) {
                o.nOption = 1;
                o.rOption = EQUILIBRIUM_LIGHT;
                tsm.sendStat(Larkness, o);
                equilibriumTimer = chr.getTimer().addEvent(this::changeMode, getMoreEquilibriumTime(), TimeUnit.SECONDS);
                chr.resetSkillCoolTime(ENDER);
                chr.resetSkillCoolTime(DEATH_SCYTHE);
            }
        } else if (SkillConstants.isLarknessDarkSkill(skillID)) {
            lm.addGauge(200, true);
            if (lm.getGauge() == 0) {
                o.nOption = 1;
                o.rOption = EQUILIBRIUM_DARK;
                tsm.sendStat(Larkness, o);
                equilibriumTimer = chr.getTimer().addEvent(this::changeMode, getMoreEquilibriumTime(), TimeUnit.SECONDS);
                chr.resetSkillCoolTime(ENDER);
                chr.resetSkillCoolTime(DEATH_SCYTHE);
            }
        }
    }

    private void giveLunarTideBuff() {
    }

    public void changeMode() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        LarknessManager lm = tsm.getLarknessManager();
        lm.setGauge(5000);
        lm.changeMode();
        Option o = new Option();
        o.nOption = 1;
        o.rOption = lm.isDark() ? ECLIPSE : SUNFIRE;
        tsm.sendStat(Larkness, o);
    }

    public int getMoreEquilibriumTime() {
        int eqTime = 10;
        SkillInfo eqi = SkillData.getSkillInfoById(DARKNESS_MASTERY);
        if (chr.hasSkill(DARKNESS_MASTERY)) {
            eqTime += eqi.getValue(time, eqi.getCurrentLevel());
            eqTime += 5;
        }
        return eqTime;
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case ARMAGEDDON:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o1);
                }
                break;
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
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();

        if (chr.getJob() != 2004) { // Beginner Luminous
            changeLarknessState(skillID);
        }
        int crescendoProp = getCrescendoProp();
        if (hasHitMobs) {
            // Eclipse/Sunfire Mode
            LarknessManager lm = tsm.getLarknessManager();
            if (!tsm.hasStat(Larkness)) {
                o1.nOption = 1;
                o1.rOption = lm.isDark() ? ECLIPSE : SUNFIRE;
                newStats.put(Larkness, o1);
            }
            // Dark Crescendo
            if (tsm.hasStat(StackBuff)) {
                if (Util.succeedProp(crescendoProp)) {
                    // increment Dark Crescendo
                    SkillInfo crescendoInfo = SkillData.getSkillInfoById(DARK_CRESCENDO);
                    Skill skill = chr.getSkill(DARK_CRESCENDO);
                    slv = skill.getCurrentLevel();
                    int amount = 1;
                    if (tsm.hasStat(StackBuff)) {
                        amount = tsm.getOption(StackBuff).mOption;
                        if (amount < getMaxDarkCrescendoStack()) {
                            amount++;
                        }
                    }
                    o2.setInMillis(true);
                    o2.nOption = (amount * crescendoInfo.getValue(damR, slv));
                    o2.rOption = DARK_CRESCENDO;
                    o2.tOption = (int) (darkCrescendoTimer - System.currentTimeMillis());
                    o2.mOption = amount;
                    newStats.put(StackBuff, o2);
                }
            }
            // Lunar Tide
            if (chr.hasSkill(LUNAR_TIDE)) {
                // giveLunarTideBuff();
                SkillInfo lti = SkillData.getSkillInfoById(LUNAR_TIDE);
                Skill skill = chr.getSkill(LUNAR_TIDE);
                slv = skill.getCurrentLevel();
                int maxMP = c.getChr().getStat(Stat.mmp);
                int curMP = c.getChr().getStat(Stat.mp);
                int maxHP = c.getChr().getStat(Stat.mhp);
                int curHP = c.getChr().getStat(Stat.hp);
                double ratioHP = ((double) curHP / maxHP);
                double ratioMP = ((double) curMP) / maxMP;
                if (ratioHP > ratioMP) {
                    o3.nOption = 2;
                    o3.rOption = LUNAR_TIDE;
                    newStats.put(LifeTidal, o3);
                    //only gives 10% for w/e reason but the SkillStat is correct
                    o4.nOption = lti.getValue(prop, slv);
                    o4.rOption = LUNAR_TIDE;
                    newStats.put(CriticalBuff, o4);
                } else {
                    o3.nOption = 1;
                    o3.rOption = LUNAR_TIDE;
                    newStats.put(LifeTidal, o3);
                    o4.nOption = lti.getValue(x, slv);
                    o4.rOption = LUNAR_TIDE;
                    newStats.put(DamR, o4);
                }
            }
            tsm.sendStat(newStats);
            newStats.clear();
        }
        switch (skillID) {
            case RAY_OF_REDEMPTION:
                chr.heal(chr.getMaxHP()); // 800% Recovery
                break;
            case PRESSURE_VOID:
                if (!tsm.hasStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(x, slv);
                    o1.rOption = skillID;
                    tsm.sendStat(KeyDownAreaMoving, o1);
                }
                break;
            case ENDER:
                if (chr.hasSkill(BAPTISM_OF_LIGHT_AND_DARKNESS)) {
                    incrementSwordsOfConsciousness();
                }
                break;
        }
    }

    private void incrementSwordsOfConsciousness() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int curStack = tsm.hasStat(SwordBaptism) ? tsm.getOption(SwordBaptism).nOption : 0;
        changeSwordsOfConsciousness(curStack + 1);
    }

    private void changeSwordsOfConsciousness(int swords) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = BAPTISM_OF_LIGHT_AND_DARKNESS;
        Option o = new Option();
        if (swords <= 12) {
            o.nOption = swords;
            o.rOption = skillID;
            tsm.sendStat(SwordBaptism, o);
        } else {
            tsm.removeStatsBySkill(skillID);
            chr.resetSkillCoolTime(skillID);
        }
    }

    private int getCrescendoProp() {
        Skill skill = null;
        if (chr.hasSkill(DARK_CRESCENDO)) {
            skill = chr.getSkill(DARK_CRESCENDO);
        }
        return skill == null ? 0 : SkillData.getSkillInfoById(DARK_CRESCENDO).getValue(prop, skill.getCurrentLevel());
    }

    private int getMaxDarkCrescendoStack() {
        Skill skill = null;
        if (chr.hasSkill(DARK_CRESCENDO)) {
            skill = chr.getSkill(DARK_CRESCENDO);
        }
        return skill == null ? 0 : SkillData.getSkillInfoById(skill.getSkillId()).getValue(x, skill.getCurrentLevel());
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
        LarknessManager lm = tsm.getLarknessManager();
        Option o1 = new Option();
        switch (skillID) {
            case SHADOW_SHELL:
                o1.nOption = 3;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                // no bOption for Luminous' AntiMagicShell
                tsm.sendStat(AntiMagicShell, o1);
                break;
            case DUSK_GUARD:
                o1.nValue = si.getValue(indiePdd, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePDD, o1);
                break;
            case PHOTIC_MEDITATION:
                o1.nOption = si.getValue(emad, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(EMAD, o1);
                break;
            case DARK_CRESCENDO:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.mOption = 1;
                tsm.sendStat(StackBuff, o1);
                this.darkCrescendoTimer = System.currentTimeMillis() + (si.getValue(time, slv) * 1000L);
                break;
            case ARCANE_PITCH:
                o1.nOption = si.getValue(y, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ElementalReset, o1);
                break;
            case EQUALIZE:
                if (lm.isDark()) {
                    o1.nOption = 1;
                    o1.rOption = EQUILIBRIUM_DARK;
                    lm.setGauge(0);
                    tsm.sendStat(Larkness, o1);
                } else {
                    o1.nOption = 1;
                    o1.rOption = EQUILIBRIUM_LIGHT;
                    lm.setGauge(10000);
                    tsm.sendStat(Larkness, o1);
                }
                this.equilibriumTimer = chr.getTimer().addEvent(this::changeMode, getMoreEquilibriumTime(), TimeUnit.SECONDS);
                chr.resetSkillCoolTime(ENDER);
                chr.resetSkillCoolTime(DEATH_SCYTHE);
                break;
            case HEROIC_MEMORIES_LUMI:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case GATE_OF_LIGHT:
                Summon summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                summon.setFlyMob(false);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.Attack);
                summon.setAttackActive(true);
                chr.getField().spawnSummon(summon);
                if (lm.isDark()) {
                    o1.nOption = 1;
                    o1.rOption = SUNFIRE;
                    tsm.sendStat(Larkness, o1);
                } else {
                    o1.nOption = 1;
                    o1.rOption = ECLIPSE;
                    tsm.sendStat(Larkness, o1);
                }
                lm.changeMode();
                lm.setFeathers(0);
                break;
            case AETHER_CONDUIT_L:
            case AETHER_CONDUIT_D:
            case AETHER_CONDUIT_EQ:
                slv = chr.getSkillLevel(AETHER_CONDUIT_L);
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setRect(aa.getPosition().getRectAround(si.getFirstRect()));
                chr.getField().spawnAffectedAreaAndRemoveOld(aa);
                chr.setSkillCooldown(AETHER_CONDUIT_L, slv);
                break;
            case BAPTISM_OF_LIGHT_AND_DARKNESS:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.SequenceAttack);
                chr.getField().spawnSummon(summon);
                break;
            case HEROS_WILL_LUMI:
                tsm.removeAllDebuffs();
                break;

        }
    }

    public int alterCooldownSkill(int skillID) {
        switch (skillID) {
            case ENDER:
            case DEATH_SCYTHE:
                if (equilibriumTimer != null && !equilibriumTimer.isDone()) {
                    return 0;
                }
        }
        return -1;
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(STANDARD_MAGIC_GUARD)) {
            if (chr.getMP() > 0) {
                Skill skill = chr.getSkill(STANDARD_MAGIC_GUARD);
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                int dmgPerc = si.getValue(x, skill.getCurrentLevel());
                int dmg = hitInfo.hpDamage;
                int mpDmg = (int) (dmg * (dmgPerc / 100D));
                if (chr.getMP() > 0) {
                    mpDmg = chr.getMP() - mpDmg < 0 ? chr.getMP() : mpDmg;
                    hitInfo.hpDamage = dmg - mpDmg;
                    hitInfo.mpDamage = mpDmg;
                }
            }
        }
        if (tsm.getOption(Larkness).rOption == EQUILIBRIUM_DARK) {
            return;
        } else {
            /*
            if (tsm.hasStat(BlessOfDarkness) && chr.hasSkill(BLACK_BLESSING) && hitInfo.hpDamage > 0) {
                Skill skill = chr.getSkill(BLACK_BLESSING);
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                int slv = skill.getCurrentLevel();
                changeBlackBlessingCount(chr, false); // deduct orbs as player gets hit
                int dmgAbsorbed = si.getValue(x, slv);
                hitInfo.hpDamage = (int) (hitInfo.hpDamage * ((double) dmgAbsorbed / 100));
            }*/
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleMobDebuffSkill(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(AntiMagicShell)) {
            tsm.removeAllDebuffs();
            deductShadowShell();
        }
        super.handleMobDebuffSkill(chr);
    }

    private void deductShadowShell() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(SHADOW_SHELL)) {
            return;
        }
        Skill skill = chr.getSkill(SHADOW_SHELL);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        Option o = new Option();
        if (tsm.hasStat(AntiMagicShell)) {
            int shadowShellCount = tsm.getOption(AntiMagicShell).nOption;

            if (shadowShellCount > 0) {
                shadowShellCount--;
            }

            if (shadowShellCount <= 0) {
                tsm.removeStatsBySkill(skill.getSkillId());
            } else {
                o.nOption = shadowShellCount;
                o.rOption = skill.getSkillId();
                o.tOption = si.getValue(time, slv);
                tsm.sendStat(AntiMagicShell, o);
            }
            chr.write(UserPacket.effect(Effect.skillSpecial(skill.getSkillId())));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillSpecial(skill.getSkillId())), chr);
        }
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.START_MAP);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (equilibriumTimer != null) {
            equilibriumTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.LUMINOUS1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1 || sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.LUMINOUS2.getJobId());
                sm.giveItem(1142480);
                sm.giveItem(2430874);
                sm.completeQuestNoRewards(25510);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.LUMINOUS2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.LUMINOUS3.getJobId());
                sm.giveItem(1142481);
                sm.completeQuestNoRewards(25511);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.LUMINOUS3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.LUMINOUS4.getJobId());
                sm.giveItem(1142482);
                sm.completeQuestNoRewards(25512);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
