package net.swordie.ms.client.jobs.resistance;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.enums.MessageType;
import net.swordie.ms.enums.QuestStatus;
import net.swordie.ms.enums.TSIndex;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Life;
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

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

/**
 * Created on 12/14/2017.
 */
public class WildHunter extends Citizen {

    //Jaguar Summon
    public static final int SUMMON_JAGUAR_GREY = 33001007;           //No Special Jaguar Stats
    public static final int SUMMON_JAGUAR_YELLOW = 33001008;         //No Special Jaguar Stats
    public static final int SUMMON_JAGUAR_RED = 33001009;            //No Special Jaguar Stats
    public static final int SUMMON_JAGUAR_PURPLE = 33001010;         //No Special Jaguar Stats
    public static final int SUMMON_JAGUAR_BLUE = 33001011;           //No Special Jaguar Stats
    public static final int SUMMON_JAGUAR_JAIRA = 33001012;          //Critical Rate +5%
    public static final int SUMMON_JAGUAR_SNOW_WHITE = 33001013;     //Buff Duration +10%
    public static final int SUMMON_JAGUAR_ONYX = 33001014;           //Buff Duration +10%
    public static final int SUMMON_JAGUAR_CRIMSON = 33001015;        //Dmg Absorption +10%
    public static final int[] SUMMONS = new int[]{SUMMON_JAGUAR_GREY, SUMMON_JAGUAR_YELLOW, SUMMON_JAGUAR_RED,
            SUMMON_JAGUAR_PURPLE, SUMMON_JAGUAR_BLUE, SUMMON_JAGUAR_JAIRA, SUMMON_JAGUAR_SNOW_WHITE, SUMMON_JAGUAR_ONYX,
            SUMMON_JAGUAR_CRIMSON};

    //Jaguar Mount
    public static final int MOUNT_JAGUAR_GREY = 1932015;
    public static final int MOUNT_JAGUAR_YELLOW = 1932030;
    public static final int MOUNT_JAGUAR_RED = 1932031;
    public static final int MOUNT_JAGUAR_PURPLE = 1932032;
    public static final int MOUNT_JAGUAR_BLUE = 1932033;
    public static final int MOUNT_JAGUAR_JAIRA = 1932036;
    public static final int MOUNT_JAGUAR_SNOW_WHITE = 1932100;
    public static final int MOUNT_JAGUAR_ONYX = 1932149;
    public static final int MOUNT_JAGUAR_CRIMSON = 1932215;
    public static final int[] MOUNTS = new int[]{MOUNT_JAGUAR_GREY, MOUNT_JAGUAR_YELLOW, MOUNT_JAGUAR_RED,
            MOUNT_JAGUAR_PURPLE, MOUNT_JAGUAR_BLUE, MOUNT_JAGUAR_JAIRA, MOUNT_JAGUAR_SNOW_WHITE, MOUNT_JAGUAR_ONYX,
            MOUNT_JAGUAR_CRIMSON};

    public static final int SECRET_ASSEMBLY = 30001281;
    public static final int CAPTURE = 30001061;
    public static final int CALL_OF_THE_HUNTER = 30001062;

    public static final int RIDE_JAGUAR = 33001001; //Special Buff
    public static final int SWIPE = 33001016; //Special Attack (Bite Debuff)
    public static final int WILD_LURE = 33001025;
    public static final int ANOTHER_BITE = 33000036;

    public static final int SOUL_ARROW_CROSSBOW = 3200014; //Passive
    public static final int CROSSBOW_BOOSTER = 33101012; //Buff
    public static final int CALL_OF_THE_WILD = 33101005; //Buff
    public static final int DASH_N_SLASH_JAGUAR_SUMMONED = 33101115; //Special Attack (Stun Debuff) + (Bite Debuff)
    public static final int DASH_N_SLASH_JAGUAR_ON = 33101215; //Special Attack (Stun Debuff) + (Bite Debuff)

    public static final int FELINE_BERSERK = 33111007; //Buff
    public static final int BACKSTEP = 33111011; //Special Buff (ON/OFF)
    public static final int HUNTING_ASSISTANT_UNIT = 33111013; //Area of Effect
    public static final int SONIC_ROAR = 33111015; //Special Attack (Bite Debuff)
    public static final int FLURRY = 33110008; //Dodge
    public static final int JAGUAR_LINK = 33110014; // Passive

    public static final int JAGUAR_SOUL = 33121017; //Special Attack (Stun Debuff) + (Bite Debuff) + (Magic Crash Debuff)
    public static final int DRILL_SALVO = 33121016; //Summon
    public static final int SHARP_EYES = 33121004; //Buff
    public static final int MAPLE_WARRIOR_WH = 33121007; //Buff
    public static final int HEROS_WILL_WH = 33121008;

    //Final Attack
    public static final int FINAL_ATTACK_WH = 33100009;
    public static final int ADVANCED_FINAL_ATTACK_WH = 33120011;

    public static final int FOR_LIBERTY_WH = 33121053;
    public static final int SILENT_RAMPAGE = 33121054;
    public static final int JAGUAR_RAMPAGE = 33121255;

    // V Skills
    public static final int JAGUAR_STORM = 400031005;
    public static final int PRIMAL_FURY = 400031012;
    public static final int PRIMAL_FURY_2 = 400031013; // screen
    public static final int PRIMAL_FURY_MOUNT_REQUEST = 400031014;
    public static final int PRIMAL_GRENADE = 400031032;
    public static final int WILD_ARROW_BLAST_TYPE_X = 400031046;

    // HEXA Skills
    public static final int HEXA_FINAL_ATTACK_WH = 21140012;

    private final int[] addedSkills = new int[]{
            SECRET_ASSEMBLY,};

    private final int[] jaguarSummons = new int[]{
            SUMMON_JAGUAR_GREY,
            SUMMON_JAGUAR_YELLOW,
            SUMMON_JAGUAR_RED,
            SUMMON_JAGUAR_PURPLE,
            SUMMON_JAGUAR_BLUE,
            SUMMON_JAGUAR_JAIRA,
            SUMMON_JAGUAR_SNOW_WHITE,
            SUMMON_JAGUAR_ONYX,
            SUMMON_JAGUAR_CRIMSON,};

    private int lastUsedSkill = 0;
    private long lastPrimalGrenadeCount = 0L;
    private long assistReadyAt = 0L;
    /** 282 另一個咬擊: mob -> {stack, expiry ms}. */
    private final Map<Integer, long[]> biteStacks = new LinkedHashMap<>();
    private long biteSentAt = 0L;
    private Field markField;
    private final Tms282PredatorsLeap predatorsLeap = new Tms282PredatorsLeap(chr,this);

    public Tms282PredatorsLeap predatorsLeap() { return predatorsLeap; }
    /** 282 滑流 HP recovery timer. */
    private java.util.concurrent.ScheduledFuture<?> glideTimer;

    /** TMS 282.3: the job was reworked (one ridden jaguar); see {@link Tms282WildHunter}. 265 keeps every path below. */
    private static boolean tms282() {
        return Tms282WildHunter.active();
    }

    public WildHunter(Char chr) {
        super(chr);
        if (chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
            if (chr.getWildHunterInfo() == null) {
                chr.setWildHunterInfo(new WildHunterInfo());
            }
            for (int id : addedSkills) {
                Skill skill = chr.getSkill(id);
                if (!chr.hasSkill(id) || skill.getCurrentLevel() != skill.getMaxLevel()) {
                    skill = SkillData.getSkillDeepCopyById(id);
                    if (skill != null) {
                        skill.setCurrentLevel(skill.getMaxLevel());
                        chr.addSkill(skill);
                    }
                }
            }
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isWildHunter(id);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        if (tms282()) {
            // 265 jaguar ids (33001007..15 remap, 33101115/215, 33001016, 33121017, 400031012/13) are gone or other
            // skills on 282; 另一個咬擊 (WHOverbite) needs client RE.
            super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
            return;
        }
        Char chr = c.getChr();
        if (skillID >= SUMMON_JAGUAR_GREY && skillID <= SUMMON_JAGUAR_CRIMSON) {
            skillID = lastUsedSkill;
            lastUsedSkill = 0;
        }
        Option o1 = new Option();
        Option o2 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);

        int jaguarBleedingTime = SkillData.getSkillInfoById(SUMMON_JAGUAR_GREY).getValue(time, 1);
        switch (skillID) {
            case DASH_N_SLASH_JAGUAR_ON: //(33101115)  //Stun + Bite Debuff
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                        if (!mob.isBoss()) {
                            o2.nOption = 1;
                            o2.rOption = skillID;
                            o2.tOption = si.getValue(time, slv);
                            map.put(MobStat.Stun, o2);
                        }
                    }
                    int amount = 0;
                    if (mts.hasCurrentMobStat(MobStat.JaguarBleeding)) {
                        amount = mts.getCurrentOptionsByMobStat(MobStat.JaguarBleeding).nOption;
                    }
                    amount = Math.min(amount + 1, 3);
                    o1.nOption = amount;
                    o1.rOption = skillID;
                    o1.tOption = jaguarBleedingTime;
                    map.put(MobStat.JaguarBleeding, o1);
                    mts.addStatOptions(mob, map);
                } else {
                    if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                        if (!mob.isBoss()) {
                            o1.nOption = 1;
                            o1.rOption = skillID;
                            o1.tOption = si.getValue(time, slv);
                            mts.addStatOptions(mob, MobStat.Stun, o1);
                        }
                    }
                }
                break;
            case DASH_N_SLASH_JAGUAR_SUMMONED: //(33101215)   //Stun Debuff
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv)) && !mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case SWIPE: //Bite Debuff
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    int amount = 0;
                    if (mts.hasCurrentMobStat(MobStat.JaguarBleeding)) {
                        amount = mts.getCurrentOptionsByMobStat(MobStat.JaguarBleeding).nOption;
                    }
                    amount = Math.min(amount + 1, 3);
                    o1.nOption = amount;
                    o1.rOption = ANOTHER_BITE;
                    o1.tOption = jaguarBleedingTime;
                    mts.addStatOptions(mob, MobStat.JaguarBleeding, o1);
                }
                break;
            case JAGUAR_SOUL: //(Stun Debuff) + (Bite Debuff) + (Magic Crash Debuff)
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    int amount = 0;
                    if (mts.hasCurrentMobStat(MobStat.JaguarBleeding)) {
                        amount = mts.getCurrentOptionsByMobStat(MobStat.JaguarBleeding).nOption;
                    }
                    amount = Math.min(amount + 1, 3);
                    o1.nOption = amount;
                    o1.rOption = ANOTHER_BITE;
                    o1.tOption = jaguarBleedingTime;
                    o2.nOption = 1;
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(time, slv);
                    map.put(MobStat.JaguarBleeding, o1);
                    map.put(MobStat.Stun, o2);
                    mts.addStatOptions(mob, map);
                }
                break;
            case PRIMAL_FURY_2:
            case PRIMAL_FURY:
                o1.nOption = 3; // max stacks instantly
                o1.rOption = ANOTHER_BITE;
                o1.tOption = jaguarBleedingTime;
                mts.addStatOptions(mob, MobStat.JaguarBleeding, o1);
                break;
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        super.handleAttack(c, attackInfo, si, now);
        if (tms282()) {
            // no 265 jaguar id remap, 400031032 狂暴榴彈 is not in 282 Skill.wz
            if (!attackInfo.mobAttackInfo.isEmpty() && Tms282WildHunter.assistTrigger(attackInfo.skillId)) {
                tryAssist282(attackInfo, now);
                stackAnotherBite282(attackInfo, now);
            }
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (JobConstants.isWildHunter(chr.getJob())) {
            if (now - lastPrimalGrenadeCount >= 4500L && tsm.hasStat(WildGrenade)) {
                increasePrimalGrenadeCount();
                lastPrimalGrenadeCount = now;
            }
        }

        if (attackInfo.skillId >= SUMMON_JAGUAR_GREY && attackInfo.skillId <= SUMMON_JAGUAR_CRIMSON) {
            attackInfo.skillId = lastUsedSkill;
            lastUsedSkill = 0;
        }
        switch (attackInfo.skillId) {
            case PRIMAL_GRENADE:
                updatePrimalGrenadeCount(tsm.hasStat(WildGrenade) ? tsm.getOption(WildGrenade).nOption - 1 : 0);
                break;
        }
    }

    private void increasePrimalGrenadeCount() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 1;
        if (tsm.hasStat(WildGrenade)) {
            count = tsm.getOption(WildGrenade).nOption;
            if (count < 8) {
                count++;
            }
        }
        updatePrimalGrenadeCount(count);
    }

    private void updatePrimalGrenadeCount(int count) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = count;
        o.rOption = PRIMAL_GRENADE;
        tsm.sendStat(WildGrenade, o);
    }

    @Override
    public int getFinalAttackSkill(int faSkill) {
        if (tms282()) {
            int mapped = Tms282WildHunter.finalAttackSkill(faSkill, chr::hasSkill);
            return mapped != 0 ? mapped : super.getFinalAttackSkill(faSkill);
        }
        if (faSkill == FINAL_ATTACK_WH) {
            if (chr.hasSkill(HEXA_FINAL_ATTACK_WH)) return HEXA_FINAL_ATTACK_WH;
            if (chr.hasSkill(ADVANCED_FINAL_ATTACK_WH)) return ADVANCED_FINAL_ATTACK_WH;
        }
        return super.getFinalAttackSkill(faSkill);
    }

    @Override
    public int getFinalAttackProc(int faSkill) {
        if (tms282()) {
            // Job.handleFinalAttackSkill sends a one-entry finalAttack node whenever proc is 0, i.e. also for an
            // unlearned 終極攻擊 (prop at level 0); -1 makes succeedProp fail so no request is sent for it.
            return chr.getSkillLevel(faSkill) > 0 ? super.getFinalAttackProc(faSkill) : -1;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.getOptByCTSAndSkill(IndieDamR, SILENT_RAMPAGE) != null) {
            return 100;
        }
        return super.getFinalAttackProc(faSkill);
    }

    private void initJaguarLink() {
        if (chr.hasSkill(JAGUAR_LINK)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (chr.getTemporaryStatManager().hasStatBySkillId(JAGUAR_LINK)) {
                tsm.removeStatsBySkill(RIDE_JAGUAR);
            }
            int amount = 0;
            for (int i = 0; i < 9; i++) {
                if (chr.getQRValueByKey(QuestConstants.WILD_HUNTER_JAGUAR_STORAGE_ID, "" + i) != null) {
                    amount += 1;
                }
            }
            if (amount == 0) {
                return;
            }
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            amount = Math.min(amount, 6);
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            SkillInfo si = SkillData.getSkillInfoById(JAGUAR_LINK);
            int slv = chr.getSkillLevel(JAGUAR_LINK);
            o1.nReason = JAGUAR_LINK;
            o1.nValue = amount * si.getValue(x, slv);
            newStats.put(IndieCrR, o1);
            o2.nReason = JAGUAR_LINK;
            o2.nValue = amount * si.getValue(z, slv);
            newStats.put(IndieCD, o2);
            o3.nReason = JAGUAR_LINK;
            o3.nValue = amount * si.getValue(padX, slv);
            newStats.put(IndiePAD, o3);
            tsm.sendStat(newStats);
        }
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        if (tms282()) {
            if (skillUseInfo.skillID == Tms282WildHunter.PREDATORS_LEAP) {
                markPredatorsLeap282(inPacket, skillUseInfo);
            } else if (skillUseInfo.skillID == Tms282WildHunter.PREDATORS_LEAP_JUMP) {
                predatorsLeap.afterAccepted(skillUseInfo,Util.getCurrentTimeLong());
            }
            handleSkill282(skillUseInfo);
            return;
        }
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        AffectedArea aa;
        Rect rect;
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (skillID) {
            case WILD_LURE: {
                lastUsedSkill = skillID;
                c.write(UserLocal.jaguarSkill(skillID));
                List<Mob> provokedMobs = chr.getField().getMobsInRect(chr.getPosition().getRectAround(si.getRects().get(0)));
                o1.nOption = 1;
                o1.rOption = skillID;
                for (Mob mob : provokedMobs) {
                    MobTemporaryStat mts = mob.getTemporaryStat();
                    o1.tOption = mob.isBoss() ? 5 : 10;
                    mts.addStatOptions(mob, MobStat.JaguarProvoke, o1.deepCopy());
                }
                break;
            }
            case SWIPE:
            case DASH_N_SLASH_JAGUAR_SUMMONED:
            case SONIC_ROAR:
            case JAGUAR_SOUL:
            case JAGUAR_RAMPAGE:
                lastUsedSkill = skillID;
                c.write(UserLocal.jaguarSkill(skillID));
                break;
            case SECRET_ASSEMBLY:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case HUNTING_ASSISTANT_UNIT:
                aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                rect = aa.getPosition().getRectAround(si.getRects().get(0));
                if (!chr.isLeft()) {
                    rect = rect.horizontalFlipAround(chr.getPosition().getX());
                }
                aa.setRect(rect);
                aa.setFlip(!chr.isLeft());
                aa.setDelay((short) 4);
                chr.getField().spawnAffectedAreaAndRemoveOld(aa);
                break;
            case HEROS_WILL_WH:
                tsm.removeAllDebuffs();
                break;
            case CAPTURE:
                int mobID = inPacket.decodeInt();
                Life life = chr.getField().getLifeByObjectID(mobID);
                if (life instanceof Mob mob) {
                    if (mob.getMaxHp() * 0.90 <= mob.getHp()) {
                        chr.write(UserPacket.effect(Effect.showCaptureEffect(skillID, slv, 0, 1)));
                        return;
                    }
                    Quest quest = chr.getQuestById(QuestConstants.WILD_HUNTER_JAGUAR_STORAGE_ID);
                    if (quest == null) {
                        quest = new Quest(chr.getId(), QuestConstants.WILD_HUNTER_JAGUAR_STORAGE_ID, QuestStatus.Started);
                        chr.addQuest(quest);
                    }
                    String key = QuestConstants.getWhStorageQuestValByTemplateID(mob.getTemplateId());
                    if (key != null) {
                        quest.setProperty(key, "1");
                        chr.write(WvsContext.message(MessageType.QUEST_RECORD_EX_MESSAGE, quest.getQRKey(), quest.getQRValue(), (byte) 0));
                        chr.write(UserPacket.effect(Effect.showCaptureEffect(skillID, slv, 0, 0)));
                        WildHunterInfo whi = chr.getWildHunterInfo();
                        mob.remove(true);
                    } else {
                        chr.write(UserPacket.effect(Effect.showCaptureEffect(skillID, slv, 0, 2)));
                    }
                }
                break;
            case PRIMAL_FURY_MOUNT_REQUEST:
                skillUseInfo.skillID = RIDE_JAGUAR;
                skillUseInfo.slv = chr.getSkillLevel(RIDE_JAGUAR);
                handleSkill(c, inPacket, skillUseInfo);
                break;
            case SUMMON_JAGUAR_GREY:
            case SUMMON_JAGUAR_YELLOW:
            case SUMMON_JAGUAR_RED:
            case SUMMON_JAGUAR_PURPLE:
            case SUMMON_JAGUAR_BLUE:
            case SUMMON_JAGUAR_JAIRA:
            case SUMMON_JAGUAR_SNOW_WHITE:
            case SUMMON_JAGUAR_ONYX:
            case SUMMON_JAGUAR_CRIMSON:
                if (chr.getWildHunterInfo() == null
                        || chr.getWildHunterInfo().getIdx() < 0
                        || chr.getWildHunterInfo().getIdx() >= MOUNTS.length) {
                    chr.chatMessage("你尚未選擇豹。請打開技能欄重新選擇！");
                    return;
                }
                if (tsm.hasStatBySkillId(RIDE_JAGUAR)) {
                    tsm.removeStatsBySkill(RIDE_JAGUAR);
                }
                Summon.summonJaguar(chr, SUMMONS[chr.getWildHunterInfo().getIdx()]);
                chr.write(UserLocal.jaguarActive(true)); // Makes Jaguar use normal attacks
                break;
            case RIDE_JAGUAR:
                if (chr.getWildHunterInfo() == null
                        || chr.getWildHunterInfo().getIdx() < 0
                        || chr.getWildHunterInfo().getIdx() >= MOUNTS.length) {
                    chr.chatMessage("你尚未選擇豹。請打開技能欄重新選擇！");
                    return;
                }
                for (int jaguarSummonSkill : jaguarSummons) {
                    tsm.removeStatsBySkill(jaguarSummonSkill);
                }
                TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);
                if (tsm.hasStat(RideVehicle)) {
                    tsm.removeStat(RideVehicle);
                } else {
                    tsb.setNOption(MOUNTS[chr.getWildHunterInfo().getIdx()]);
                    tsb.setROption(skillID);
                    tsm.sendStat(RideVehicle, tsb.getOption());
                }
                break;
            case CALL_OF_THE_WILD:
                o1.nReason = skillID;
                o1.nValue = si.getValue(z, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndiePADR, o1);
                newStats.put(IndieMADR, o1.deepCopy());
                o2.nOption = si.getValue(x, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(DamageReduce, o2);
                newStats.put(Guard, o2.deepCopy());
                newStats.put(EVAR, o2.deepCopy());
                o3.nReason = skillID;
                o3.nValue = si.getValue(x, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieMMPR, o3);
                tsm.sendStat(newStats);
                break;
            case FELINE_BERSERK:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieBooster, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieBooster, o1);
                o2.nOption = si.getValue(z, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(DamR, o2);
                o3.nOption = si.getValue(x, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(Speed, o3);
                tsm.sendStat(newStats);
                break;
            case BACKSTEP:
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(DrawBack, o1);
                break;
            case SHARP_EYES: // x = crit rate%  |  y = max crit dmg%
                int cr = si.getValue(x, slv);
                int crDmg = si.getValue(y, slv);
                o1.nOption = (cr << 8) + crDmg;
                o1.nValue = si.getValue(y, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(SharpEyes, o1);
                break;
            case FOR_LIBERTY_WH:
            case SILENT_RAMPAGE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case DRILL_SALVO:
                aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                rect = aa.getPosition().getRectAround(si.getRects().get(0));
                if (!chr.isLeft()) {
                    rect = rect.horizontalFlipAround(chr.getPosition().getX());
                }
                aa.setRect(rect);
                aa.setFlip(!chr.isLeft());
                aa.setDelay((short) 8);
                chr.getField().spawnAffectedAreaAndRemoveOld(aa);
                break;
            case PRIMAL_FURY:
                aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setDuration(si.getValue(v, slv) * 1000);
                aa.setRect(aa.getPosition().getRectAround(si.getFirstRect()));
                chr.getField().spawnAffectedAreaAndRemoveOld(aa);

                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(v, slv);
                tsm.sendStat(NotDamaged, o1);
                break;
            case JAGUAR_STORM:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(SlowAttack, o1);

                Position pos = Util.getRandomFromCollection(chr.getField().getFootholdsInRect(chr.getRectAround(si.getFirstRect()))).getRandomPosition();
                Random rand = new Random();
                int x = chr.getPosition().getX();
                int count = 0;
                List<Integer> summon = new ArrayList<>();
                while (count < 6) {
                    int random = (int) (rand.nextDouble() * 9);
                    pos.setX(chr.isLeft() ? (x + random * 200) : (x - random * 200));
                    if (random != chr.getWildHunterInfo().getIdx() && !summon.contains(SUMMONS[random])) {
                        Summon.summonJaguarStorm(chr, WildHunter.SUMMONS[random], pos, si.getValue(time, slv));
                        summon.add(SUMMONS[random]);
                        count++;
                    }
                }
                break;
        }
    }

    /** 282 捕食者之躍: mark the mob the request names (ProcessType 19, first i32) so that the next press leaps to it. */
    private void markPredatorsLeap282(InPacket inPacket, SkillUseInfo skillUseInfo) {
        if (inPacket!=null && skillUseInfo.skillInfo!=null)
            predatorsLeap.afterAccepted(skillUseInfo,Util.getCurrentTimeLong());
    }

    /** 282 另一個咬擊: every skillList crossbow hit adds a stack (at most x) on the mobs hit for time seconds. */
    private void stackAnotherBite282(AttackInfo attackInfo, long now) {
        markField282();
        int id = chr.getSkillLevel(Tms282WildHunter.ANOTHER_BITE_VI) > 0 ? Tms282WildHunter.ANOTHER_BITE_VI
                : Tms282WildHunter.ANOTHER_BITE;
        int slv = chr.getSkillLevel(id);
        SkillInfo si = SkillData.getSkillInfoById(id);
        if (slv <= 0 || si == null) {
            return;
        }
        int max = Math.max(1, si.getValue(x, slv));
        long ms = Math.max(1, si.getValue(time, slv)) * 1000L;
        boolean changed = biteStacks.entrySet().removeIf(e -> e.getValue()[1] <= now
                || !(chr.getField().getLifeByObjectID(e.getKey()) instanceof Mob));
        for (net.swordie.ms.client.character.skills.info.MobAttackInfo mai : attackInfo.mobAttackInfo) {
            long[] cur = biteStacks.get(mai.mobId);
            long stack = cur == null ? 1 : Math.min(max, cur[0] + 1);
            changed |= cur == null || cur[0] != stack;
            biteStacks.put(mai.mobId, new long[]{stack, now + ms});
        }
        if (!changed && now < biteSentAt + 1000) {
            return; // refresh the timers at most once a second
        }
        biteSentAt = now;
        Map<Integer, Integer> stacks = new LinkedHashMap<>();
        biteStacks.forEach((mob, v) -> stacks.put(mob, (int) v[0]));
        chr.write(UserLocal.setMobAdvMark(Tms282WildHunter.ANOTHER_BITE, stacks, (int) ms));
    }

    private void markField282() {
        if(markField!=chr.getField()) {
            biteStacks.clear(); biteSentAt=0; markField=chr.getField();
        }
    }

    /** The client already erased these entries locally. Do not refresh other targets' durations with a new SET. */
    public void cleanupMarkedTargets282(int skillId,List<Integer> targets,long now) {
        if(!tms282())throw new IllegalArgumentException("Mark cleanup requires TMS282");
        if(skillId==Tms282WildHunter.PREDATORS_LEAP) { predatorsLeap.cleanup(targets,now);return; }
        markField282();
        if(skillId==Tms282WildHunter.ANOTHER_BITE) {
            if(chr.getSkillLevel(Tms282WildHunter.ANOTHER_BITE)<=0&&chr.getSkillLevel(Tms282WildHunter.ANOTHER_BITE_VI)<=0)
                throw new IllegalArgumentException("Unowned bite cleanup");
            for(int id:targets) {
                long[] mark=biteStacks.get(id); Life life=chr.getField().getLifeByObjectID(id);
                if(mark!=null&&(now>mark[1]||!(life instanceof Mob mob)||mob.getHp()<=0))biteStacks.remove(id);
            }
        } else throw new IllegalArgumentException("Unsupported mark cleanup");
    }

    /**
     * 282 支援: a crossbow hit asks the client (LP057E) for the spirit jaguar attack on the mobs just hit. At most once per
     * {@link Tms282WildHunter#ASSIST_GUARD_MS} and never while 33100002 is cooling down (its attack sets the cooltime).
     */
    private void tryAssist282(AttackInfo attackInfo, long now) {
        if (chr.getSkillLevel(Tms282WildHunter.ASSIST) <= 0 || now < assistReadyAt
                || chr.hasSkillOnCooldown(Tms282WildHunter.ASSIST)) {
            return;
        }
        List<Integer> mobs = new ArrayList<>();
        for (net.swordie.ms.client.character.skills.info.MobAttackInfo mai : attackInfo.mobAttackInfo) {
            if (mobs.size() < 8 && !mobs.contains(mai.mobId)) {
                mobs.add(mai.mobId);
            }
        }
        assistReadyAt = now + Tms282WildHunter.ASSIST_GUARD_MS;
        chr.write(UserLocal.userBonusAttackRequest(Tms282WildHunter.ASSIST, mobs));
    }

    /** 282 skill-use effects after the CP04B4 service (cost, cooldown, indie* buffs from Skill.wz) accepted the use. */
    private void handleSkill282(SkillUseInfo skillUseInfo) {
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (si == null) {
            return;
        }
        switch (skillID) {
            case Tms282WildHunter.RIDE_JAGUAR -> {
                TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);
                if (tsm.hasStat(RideVehicle)) {
                    tsm.removeStat(RideVehicle);
                } else {
                    if (chr.getWildHunterInfo() == null) {
                        chr.setWildHunterInfo(new WildHunterInfo());
                    }
                    // the 282 client knows one jaguar vehicle (AECB00: id == 1932982, 阿希莉); the 265 mount ids
                    // (1932015...) do not exist there and crashed the client on 美洲豹騎乘
                    tsb.setNOption(Tms282WildHunter.JAGUAR_VEHICLE);
                    tsb.setROption(skillID);
                    tsm.sendStat(RideVehicle, tsb.getOption());
                }
            }
            case Tms282WildHunter.HOWLING -> {
                // 咆哮: "#time秒內使所有隊員的攻擊力與魔力#x%增加" (the passive part is psd)
                int value = si.getValue(x, slv);
                int seconds = si.getValue(time, slv);
                if (value > 0 && seconds > 0) {
                    for (Char member : partyTargets()) {
                        EnumMap<CharacterTemporaryStat, Option> stats = new EnumMap<>(CharacterTemporaryStat.class);
                        Option pad = new Option();
                        pad.nReason = skillID;
                        pad.nValue = value;
                        pad.tTerm = seconds;
                        stats.put(IndiePADR, pad);
                        stats.put(IndieMADR, pad.deepCopy());
                        member.getTemporaryStatManager().sendStat(stats);
                    }
                }
            }
            case Tms282WildHunter.CRITICAL_EYE -> {
                // 會心之眼: "#time秒內增加爆擊機率#x%、爆擊傷害#y%" for the party. The official stat is probably
                // SharpEyes (STD_EXPLICIT, unsupported on 282): CriticalBuff (crit rate, as 265 FLURRY) + IndieCD.
                int cr = si.getValue(x, slv);
                int cd = si.getValue(y, slv);
                int seconds = si.getValue(time, slv);
                if (seconds > 0 && (cr > 0 || cd > 0)) {
                    for (Char member : partyTargets()) {
                        EnumMap<CharacterTemporaryStat, Option> stats = new EnumMap<>(CharacterTemporaryStat.class);
                        if (cr > 0) {
                            Option o = new Option();
                            o.nOption = cr;
                            o.rOption = skillID;
                            o.tOption = seconds;
                            stats.put(CriticalBuff, o);
                        }
                        if (cd > 0) {
                            Option o = new Option();
                            o.nReason = skillID;
                            o.nValue = cd;
                            o.tTerm = seconds;
                            stats.put(IndieCD, o);
                        }
                        member.getTemporaryStatManager().sendStat(stats);
                    }
                }
            }
            case Tms282WildHunter.BEAST_POSSESSION -> {
                // 狂獸附體: "在#u2秒內進入無敵"
                int seconds = si.getValue(u2, slv);
                if (seconds > 0) {
                    Option o = new Option();
                    o.nReason = skillID;
                    o.nValue = 1;
                    o.tTerm = seconds;
                    tsm.sendStat(IndieNotDamaged, o);
                }
            }
            case Tms282WildHunter.PRIMAL_BLOOM -> {
                int value = si.getValue(x, slv);
                int seconds = si.getValue(time, slv);
                if (value > 0 && seconds > 0) {
                    Option o = new Option();
                    o.nReason = skillID;
                    o.nValue = value;
                    o.tTerm = seconds;
                    tsm.sendStat(IndiePMdR, o);
                }
            }
            case Tms282WildHunter.HEROS_WILL -> tsm.removeAllDebuffs();
            case Tms282WildHunter.SECRET_ASSEMBLY -> {
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                if (toField != null) {
                    chr.warp(toField);
                }
            }
            default -> {
                // 自由意志 (indieDamR) is sent by the service's Skill.wz resolver; 捕食者之躍 / 原始綻放 / 誘餌信標 /
                // 陷阱散佈器 / 指令 skills need client RE (README). 265 ids with other 282 meanings do nothing here.
            }
        }
    }

    /** The caster and the party members in the same field (265 party-buff convention). */
    private List<Char> partyTargets() {
        if (chr.getParty() == null) {
            return List.of(chr);
        }
        List<Char> out = new ArrayList<>(chr.getParty().getPartyMembersInSameFieldWithChr(chr));
        if (!out.contains(chr)) {
            out.add(chr);
        }
        return out;
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        if (tms282()) {
            startGlideTimer();
        }
        super.handleInitAfterMigrate(chr);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if(tms282()) { biteStacks.clear(); biteSentAt=0; markField=null;predatorsLeap.clear(); }
        if (glideTimer != null) {
            glideTimer.cancel(true);
            glideTimer = null;
        }
        super.handleCancelTimer(chr);
    }

    /** 282 滑流: "每#dummyStr秒回復最大HP的#x%" (y = period in ms). */
    private void startGlideTimer() {
        if (glideTimer != null && !glideTimer.isCancelled()) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(Tms282WildHunter.GLIDE);
        if (si == null || chr.getSkillLevel(Tms282WildHunter.GLIDE) <= 0) {
            return;
        }
        int period = si.getValue(y, chr.getSkillLevel(Tms282WildHunter.GLIDE));
        if (period <= 0) {
            return;
        }
        glideTimer = chr.getTimer().addFixedRateEvent(() -> {
            int lv = chr.getSkillLevel(Tms282WildHunter.GLIDE);
            if (lv <= 0 || chr.getHP() <= 0 || chr.getHP() >= chr.getMaxHP()) {
                return;
            }
            int heal = Tms282WildHunter.glideHeal(chr.getMaxHP(), si.getValue(x, lv));
            if (heal > 0) {
                chr.heal(heal);
            }
        }, period, period, false);
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        if (tms282()) {
            super.handleHit(c, inPacket, hitInfo); // 33110008 疾風 (265 dodge crit) is not in 282 Skill.wz
            return;
        }
        if (hitInfo.hpDamage == 0 && hitInfo.mpDamage == 0) {
            // Dodged
            if (chr.hasSkill(FLURRY)) {
                Skill skill = chr.getSkill(FLURRY);
                int slv = skill.getCurrentLevel();
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                TemporaryStatManager tsm = chr.getTemporaryStatManager();
                Option o = new Option();
                o.nOption = 100;
                o.rOption = skill.getSkillId();
                o.tOption = si.getValue(time, slv);
                tsm.sendStat(CriticalBuff, o);
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.WILD_HUNTER_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.WILD_HUNTER_2.getJobId());
                sm.giveItem(1142243);
                sm.completeQuestNoRewards(23021);
                sm.completeQuestNoRewards(23024);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.WILD_HUNTER_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.WILD_HUNTER_3.getJobId());
                sm.giveItem(1142244);
                sm.completeQuestNoRewards(23031);
                sm.completeQuestNoRewards(23034);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.WILD_HUNTER_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.WILD_HUNTER_4.getJobId());
                sm.giveItem(1142245);
                sm.completeQuestNoRewards(23041);
                sm.completeQuestNoRewards(23044);
                sm.completeQuestNoRewards(23047);
                sm.completeQuestNoRewards(23050);
                sm.completeQuestNoRewards(23053);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
