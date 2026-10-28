package net.swordie.ms.client.jobs.Jianghu;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
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
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.HexaBoostSkillConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.EnumMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class MoXuan extends Job {

    public static final int XUANSHAN_SPIRIT = 170000001;
    public static final int RETURN_TO_XUANSHAN = 170001000;
    public static final int WILL_OF_THE_ALLIANCE = 170000190;

    public static final int AWAKEN = 175000006; // Restores #x% of Max HP/MP every #w sec, even in combat

    public static final int XUANSHAN_ARTS_TIAN = 175001000;
    public static final int XUANSHAN_STRIKE_TIAN = 175001002;
    public static final int XUANSHAN_CROSS_TIAN = 175121001;
    public static final int DIVINE_ART_ERUPTING_FLAME = 175001003;

    public static final int XUANSHAN_ARTS_DI = 175101003;
    public static final int XUANSHAN_STRIKE_DI = 175101001;
    public static final int XUANSHAN_CROSS_DI = 175121002;
    public static final int DIVINE_ART_TEARING_WIND = 175101004;
    public static final int DIVINE_ART_BURNING_IRON = 175101005;
    public static final int SECRET_ART_ARROW_FLIGHT = 175101007; // Secret Art: Arrow Flight

    public static final int THE_HOLY_MOUNTAIN = 175110010;
    public static final int DIVINE_ART_RIGHTEOUS_THUNDER = 175111001;
    public static final int DIVINE_ART_SWIRLING_TIDE = 175111002; // MukHyun_SEON_PUNG_GAG(10) // Buff
    public static final int DIVINE_ART_SWIRLING_TIDE_2 = 175111003;
    public static final int SECRET_ART_QI_PROJECTION = 175111004; // IndiePeriodicalSkillActivation(132160, 132159, 132158, 132157)
    public static final int SECRET_ART_QI_PROJECTION_ATOM = 175111011;

    public static final int HEIR_OF_THE_DIVINE = 175120016; // MukHyunDivine(2)
    public static final int DIVINE_FIST_PERSIST = 175120039;
    public static final int DIVINE_ART_HOWLING_STROM_ATTACK = 175121003; // IndieDamReduceR(50) + IndieAntiMagicShell(1) + IndieCheckTimeByClient(1)
    public static final int SECRET_ART_QI_DISRUPTION = 175121005; // IndieNotDamaged(1)
    public static final int SECRET_ART_QI_SHIELD = 175121009; // MukHyun_HO_SIN_GANG_GI(100)

    public static final int SOUL_ART_BLACK_WIND = 175121040;
    public static final int SECRET_ART_STRENGTH_WITHIN = 175121041;
    public static final int AURA_OF_DESTINY = 175121042;

    // V Skills
    public static final int SOUL_ART_BENEATH_HEAVEN = 400051084; // 真氣：神現無極
    public static final int SOUL_ART_THE_CONQUERED_SELF = 400051086;
    public static final int SOUL_ART_THE_CONQUERED_SELF_ATOM = 400051087;
    public static final int SOUL_ART_THE_OPENED_GATE = 400051088; // 真氣：爆氣
    public static final int DIVINE_ART_CRASHING_EARTH = 400051089;
    public static final int DIVINE_ART_CRASHING_EARTH_1 = 400051090;
    public static final int DIVINE_ART_CRASHING_EARTH_2 = 400051091;

    // HEXA Boosts
    public static final int HEXA_BOOST_OPENED_GATE_CORE = 500004198; // 真氣：爆氣強化 (hexa matrix core)
    public static final int HEXA_SOUL_ART_THE_OPENED_GATE = 500061070;

    /** GMS265 客戶端 IndiePeriodicalSkillActivation 編碼值（原版 server 固定值，非 WZ x/y）。 */
    public static final int PERIODICAL_SKILL_ACTIVATION_VALUE = 132160;

    // Sixth job (HEXA) — H1 origin / H2 ascent
    public static final int HEXA_BREAKING_FIST = 175141500; // H1 真氣：破荒拳
    public static final int SCORCHING_DIVINE_LEG = 175141503; // H2 真氣：灼熱破神腿

    // EFFECT 106:
    // 175120016 - Heir of the Divine => 01 01 00 => khi dùng 175001003 - Divine Art: Erupting Flame
    // 175120016 - Heir of the Divine => 01 01 00 => khi dùng 175111001 - Divine Art: Righteous Thunder
    // 175120016 - Heir of the Divine => 02 01 00 => khi dùng 175101004 - Divine Art: Tearing Wind
    // 175120016 - Heir of the Divine => 02 01 00 => khi dùng 175111002 - Divine Art: Swirling Tide
    // 175120016 - Heir of the Divine => 01 02 00 => khi dùng 175121003 - Divine Art: Howling Storm chưa đủ nộ
    // 175120016 - Heir of the Divine => 01 02 01 => khi dùng 175121003 - Divine Art: Howling Storm đủ nộ
    // 175121009 - Secret Art: Qi Shield => int 100 (64 00 00 00) => khi dùng 175121009 - Secret Art: Qi Shield
    // 175110010 - Khi Di 2 cái

    private final int[] addedSkills = new int[]{
            XUANSHAN_SPIRIT, RETURN_TO_XUANSHAN, WILL_OF_THE_ALLIANCE, DIVINE_ART_ERUPTING_FLAME, AWAKEN,
            THE_HOLY_MOUNTAIN, HEIR_OF_THE_DIVINE,
    };

    final Tms282HowlingStorm howlingStorm = new Tms282HowlingStorm();

    public AtomicInteger godPower = new AtomicInteger(0);
    public AtomicInteger powerType = new AtomicInteger(0);
    public ScheduledFuture<?> powerTimer;
    public long awaken = 0L;
    private int stackAckMask = 0;
    private volatile boolean exitPrepared = false;

    public MoXuan(Char chr) {
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

    public int getGodPower() {
        return godPower.get();
    }

    public void setGodPower(int godPower) {
        this.godPower.set(godPower);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isMoXuan(id);
    }

    /**
     * 爆氣 buff 期間應獲得額外傷害的招式/神功（不含六轉技能）。
     */
    public static boolean isFormOrDivineArtSkill(int skillId) {
        switch (skillId) {
            case XUANSHAN_ARTS_TIAN:
            case XUANSHAN_STRIKE_TIAN:
            case XUANSHAN_CROSS_TIAN:
            case DIVINE_ART_ERUPTING_FLAME:
            case XUANSHAN_ARTS_DI:
            case XUANSHAN_STRIKE_DI:
            case XUANSHAN_CROSS_DI:
            case DIVINE_ART_TEARING_WIND:
            case DIVINE_ART_BURNING_IRON:
            case DIVINE_ART_RIGHTEOUS_THUNDER:
            case DIVINE_ART_SWIRLING_TIDE:
            case DIVINE_ART_SWIRLING_TIDE_2:
            case DIVINE_ART_HOWLING_STROM_ATTACK:
            case SECRET_ART_QI_DISRUPTION:
            case SOUL_ART_BLACK_WIND:
            case SOUL_ART_BENEATH_HEAVEN:
            case DIVINE_ART_CRASHING_EARTH:
            case DIVINE_ART_CRASHING_EARTH_1:
            case DIVINE_ART_CRASHING_EARTH_2:
                return true;
            default:
                return false;
        }
    }


    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {

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
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        handleSpecialEffect(skillID);
        switch (skillID) {
            case DIVINE_ART_SWIRLING_TIDE:
                o1.nOption = 10;
                o1.rOption = skillID;
                o1.tOption = 5;
                tsm.sendStat(MukHyun_SEON_PUNG_GAG, o1);
                break;
            case DIVINE_ART_SWIRLING_TIDE_2:
                o1.nOption = 2010; // 10010
                o1.rOption = skillID;
                o1.tOption = 5;
                tsm.sendStat(MukHyun_SEON_PUNG_GAG, o1);
                break;
            case SECRET_ART_QI_DISRUPTION:
            case SOUL_ART_BLACK_WIND:
            case SOUL_ART_BENEATH_HEAVEN:
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = si.getValue(ndTime, slv);
                o1.isInMillis = true;
                tsm.sendStat(IndieNotDamaged, o1);
                break;
            case DIVINE_ART_HOWLING_STROM_ATTACK:
                if (Tms282HowlingStorm.handles(skillID)) break; // Prepare owns the current cast protection.
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = 4;
                newStats.put(IndieDamReduceR, o1);
                o2.nValue = 1;
                o2.nReason = skillID;
                o2.tTerm = 4;
                newStats.put(IndieAntiMagicShell, o2);
                tsm.sendStat(newStats);
                break;
        }
    }

    private void handleSpecialEffect(int skillID) {
        boolean stack = false;
        switch (skillID) {
            case XUANSHAN_ARTS_TIAN:
            case XUANSHAN_STRIKE_TIAN:
            case XUANSHAN_CROSS_TIAN:
                if (this.godPower.get() == 110) {
                    this.godPower.set(120);
                } else {
                    this.godPower.set(110);
                }
                chr.write(WvsContext.sendMoXuanStack(this.powerType.get(), this.godPower.get()));
                break;
            case XUANSHAN_ARTS_DI:
            case XUANSHAN_STRIKE_DI:
            case XUANSHAN_CROSS_DI:
                if (this.godPower.get() == 210) {
                    this.godPower.set(220);
                } else {
                    this.godPower.set(210);
                }
                chr.write(WvsContext.sendMoXuanStack(this.powerType.get(), this.godPower.get()));
                break;
            case DIVINE_ART_RIGHTEOUS_THUNDER:
            case DIVINE_ART_ERUPTING_FLAME:
                chr.write(UserPacket.effect(Effect.sendMoXuanEff(1, 1, 0, 0)));
                this.powerType.set(2);
                this.godPower.set(1);
                chr.write(WvsContext.sendMoXuanStack(this.powerType.get(), this.godPower.get()));
                stack = true;
                break;
            case DIVINE_ART_TEARING_WIND:
                chr.write(UserPacket.effect(Effect.sendMoXuanEff(2, 1, 0, 0)));
                this.powerType.set(2);
                this.godPower.set(2);
                chr.write(WvsContext.sendMoXuanStack(this.powerType.get(), this.godPower.get()));
                stack = true;
                break;
            case DIVINE_ART_SWIRLING_TIDE:
                chr.write(UserPacket.effect(Effect.sendMoXuanEff(2, 1, 1, 0)));
                this.powerType.set(2);
                this.godPower.set(0);
                chr.write(WvsContext.sendMoXuanStack(this.powerType.get(), this.godPower.get()));
                stack = true;
                break;
            case (DIVINE_ART_SWIRLING_TIDE + 1):
                this.powerType.set(1000);
                this.godPower.set(0);
                chr.write(WvsContext.sendMoXuanStack(this.powerType.get(), this.godPower.get()));
                stack = true;
                break;
            case DIVINE_ART_HOWLING_STROM_ATTACK:
                chr.write(UserPacket.effect(Effect.sendMoXuanEff(1, 2, Util.getRandom(0, 1), 0)));
                this.powerType.set(2);
                this.godPower.set(1);
                chr.write(WvsContext.sendMoXuanStack(this.powerType.get(), this.godPower.get()));
                stack = true;
                break;
            case SECRET_ART_QI_SHIELD:
                chr.write(UserPacket.effect(Effect.sendMoXuanEff(0, 0, 0, 100)));
                break;
        }
        if (stack && chr.hasSkill(HEIR_OF_THE_DIVINE)) {
            updateDivineStack(false);
        }
    }

    private void updateDivineStack(boolean fill) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        long amount = tsm.hasStat(MukHyunDivine) ? tsm.getTotalNOptionOfStat(MukHyunDivine) + 1 : 1;
        int duration = 60;
        if (Protocol.active().version() == ProtocolVersion.TMS_282_3) {
            SkillInfo info = SkillData.getSkillInfoById(HEIR_OF_THE_DIVINE);
            int level = chr.getSkillLevel(HEIR_OF_THE_DIVINE);
            int cap = info.getValue(y, level);
            amount = fill ? cap : Math.max(0, Math.min(amount, cap));
            duration = info.getValue(time, level);
        } else if (tsm.hasStat(MukHyunDivine)) {
            amount = Math.max(amount, 5); // Preserve the existing265 behavior.
        }
        Option option = new Option();
        option.nOption = (int) amount;
        option.rOption = HEIR_OF_THE_DIVINE;
        option.tOption = duration;
        tsm.sendStat(MukHyunDivine, option);
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        handleSpecialEffect(skillID);
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        switch (skillID) {
            case RETURN_TO_XUANSHAN:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case SECRET_ART_ARROW_FLIGHT:
                if (!tsm.hasStat(CannonShooter_BFCannonBall)) {
                    o1.nOption = si.getValue(y, slv);
                    o1.rOption = skillID;
                    tsm.sendStat(CannonShooter_BFCannonBall, o1);
                } else {
                    o1.nOption = (int) Math.max(tsm.getTotalNOptionOfStat(CannonShooter_BFCannonBall) - 1, 0);
                    o1.rOption = skillID;
                    tsm.sendStat(CannonShooter_BFCannonBall, o1);
                }
                break;
            case SECRET_ART_QI_SHIELD:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(MukHyun_HO_SIN_GANG_GI, o1);
                break;
            case SECRET_ART_QI_PROJECTION:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    tsm.removeStatsBySkill(SOUL_ART_THE_CONQUERED_SELF);
                    o1.nValue = PERIODICAL_SKILL_ACTIVATION_VALUE;
                    o1.nReason = skillID;
                    tsm.sendStat(IndiePeriodicalSkillActivation, o1);
                }
                break;
            case SOUL_ART_THE_CONQUERED_SELF:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    tsm.removeStatsBySkill(SECRET_ART_QI_PROJECTION);
                    o1.nValue = PERIODICAL_SKILL_ACTIVATION_VALUE;
                    o1.nReason = skillID;
                    tsm.sendStat(IndiePeriodicalSkillActivation, o1);
                }
                break;
            case SECRET_ART_STRENGTH_WITHIN:
                this.powerType.set(2);
                this.godPower.set(1);
                chr.write(WvsContext.sendMoXuanStack(this.powerType.get(), this.godPower.get()));
                if (Protocol.active().version() == ProtocolVersion.TMS_282_3 && chr.hasSkill(HEIR_OF_THE_DIVINE)) {
                    updateDivineStack(true);
                }
                break;
            case AURA_OF_DESTINY:
                o1.nValue = si.getValue(indieDamR, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case SOUL_ART_THE_OPENED_GATE:
            case HEXA_SOUL_ART_THE_OPENED_GATE:
                applyOpenedGateBuff(chr, tsm, skillID, si, slv, newStats, o1, o2, o3, o4);
                break;
        }
    }

    private void applyOpenedGateBuff(Char chr, TemporaryStatManager tsm, int skillID, SkillInfo si, int slv,
                                     EnumMap<CharacterTemporaryStat, Option> newStats,
                                     Option o1, Option o2, Option o3, Option o4) {
        SkillInfo baseSi = SkillData.getSkillInfoById(SOUL_ART_THE_OPENED_GATE);
        int baseSlv = chr.getSkillLevel(SOUL_ART_THE_OPENED_GATE);
        int hexaBoostLv = HexaBoostSkillConstants.getHexaBoostLevel(chr, SOUL_ART_THE_OPENED_GATE);
        SkillInfo boostSi = hexaBoostLv > 0 ? SkillData.getSkillInfoById(HEXA_SOUL_ART_THE_OPENED_GATE) : null;
        boolean hasHexaBoost = boostSi != null && hexaBoostLv > 0;

        int buffSkillId = hasHexaBoost ? HEXA_SOUL_ART_THE_OPENED_GATE : skillID;
        int duration;
        int critDmg;
        int formDamBoost;
        int extraAttacks = baseSi != null && baseSlv > 0 ? baseSi.getValue(y, baseSlv) : 0;

        if (hasHexaBoost) {
            duration = boostSi.getValue(time, hexaBoostLv);
            int baseCritDmg = baseSi != null && baseSlv > 0 ? baseSi.getValue(indieCD, baseSlv) : 0;
            critDmg = baseCritDmg + boostSi.getValue(w, hexaBoostLv);
            formDamBoost = boostSi.getValue(x, hexaBoostLv);
        } else {
            duration = si.getValue(time, slv);
            critDmg = si.getValue(indieCD, slv);
            formDamBoost = si.getValue(x, slv);
        }

        int critRate = si.getValue(indieCr, slv);
        if (critRate <= 0 && baseSi != null && baseSlv > 0) {
            critRate = baseSi.getValue(indieCr, baseSlv);
        }
        if (critRate > 0) {
            o1.nOption = critRate;
            o1.rOption = buffSkillId;
            o1.tOption = duration;
            newStats.put(CriticalBuff, o1);
        }

        o2.nReason = buffSkillId;
        o2.nValue = critDmg;
        o2.tTerm = duration;
        newStats.put(IndieCD, o2);

        o3.nOption = formDamBoost;
        o3.rOption = buffSkillId;
        o3.tOption = duration;
        newStats.put(MukHyunChosic, o3);

        o4.nOption = extraAttacks;
        o4.rOption = buffSkillId;
        o4.tOption = duration;
        if (extraAttacks > 0) {
            newStats.put(AddAttackCount, o4);
        }

        Option o5 = new Option();
        o5.nValue = 1;
        o5.nReason = buffSkillId;
        newStats.put(IndieCheckTimeByClient, o5);

        Option o6 = new Option();
        o6.nValue = 1;
        o6.nReason = buffSkillId;
        newStats.put(IndieBuffIcon, o6);

        tsm.sendStat(newStats);
    }

    public void increaseArrowFlight() {
        if (!chr.hasSkill(SECRET_ART_ARROW_FLIGHT)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        SkillInfo si = SkillData.getSkillInfoById(SECRET_ART_ARROW_FLIGHT);
        int max = si.getValue(y, chr.getSkillLevel(SECRET_ART_ARROW_FLIGHT));
        int count = 1;
        if (tsm.hasStat(CannonShooter_BFCannonBall)) {
            count = tsm.getOption(CannonShooter_BFCannonBall).nOption;
            if (count < max) {
                count++;
            }
        }
        updateVSkillStackBuff(chr, count);
    }

    private void awaken() {
        if (!chr.hasSkill(AWAKEN)) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(AWAKEN);
        if (si != null) {
            int slv = chr.getSkillLevel(AWAKEN);
            if (chr.getHP() < chr.getMaxHP()) {
                int healAmount = (int) (chr.getMaxHP() * si.getValue(x, slv) / 100);
                chr.heal(healAmount);
            }
        }
        this.awaken = System.currentTimeMillis();
    }

    @Override
    public void update(long now) {
        super.update(now);
        if (this.awaken == 0 || now - this.awaken >= 3000L) {
            awaken();
        }
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);
        cs.setJob(JobConstants.JobEnum.MOXUAN_1.getJobId());
        cs.setLevel(10);
        cs.setStr(4);
        cs.setDex(45);
        cs.setInt(4);
        cs.setLuk(4);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        cs.setMp(500);
        cs.setMaxMp(500);
        cs.getExtendSP().addSpToJobLevel(1, 5);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.MOXUAN_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MOXUAN_2.getJobId());
            }
        } else if (chr.getJob() == JobConstants.JobEnum.MOXUAN_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MOXUAN_3.getJobId());
            }
        } else if (chr.getJob() == JobConstants.JobEnum.MOXUAN_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.MOXUAN_4.getJobId());
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }

    @Override
    public void handleCancelTimer(Char chr) {
        cancelPowerTimer();
        super.handleCancelTimer(chr);
    }

    public void prepareForGameExit() {
        exitPrepared = true;
        stackAckMask = 0;
        cancelPowerTimer();
    }

    private void cancelPowerTimer() {
        if (powerTimer != null) {
            powerTimer.cancel(true);
            powerTimer = null;
        }
    }

    public void handleStackRequest(Char chr, InPacket inPacket) {
        int unread = inPacket.getUnreadAmount();
        if (unread < 4) {
            return;
        }
        int raw = inPacket.decodeInt();
        if (inPacket.getUnreadAmount() > 0) {
            inPacket.decodeArr(inPacket.getUnreadAmount());
        }
        int stackType = raw & 0xFF;
        if (stackType < 0 || stackType > 2) {
            return;
        }
        stackAckMask |= (1 << stackType);
        if (stackType == 2 || stackAckMask == 0b111) {
            chr.write(WvsContext.sendExtraSystemInit());
            stackAckMask = 0;
        }
    }

    private void sendStackSync(Char chr) {
        stackAckMask = 0;
        chr.write(WvsContext.sendExtraSystemStack(0, -1639974713, (byte) 246));
        chr.write(WvsContext.sendExtraSystemStack(1, -1639974713, (byte) 247));
        chr.write(WvsContext.sendExtraSystemStack(2, -1639974713, (byte) 248));
        chr.write(WvsContext.sendExtraSystemInit());
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        exitPrepared = false;
        stackAckMask = 0;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(MukHyunDivine) && chr.hasSkill(MoXuan.HEIR_OF_THE_DIVINE)) {
            Option o1 = new Option();
            o1.nOption = 0;
            o1.rOption = MoXuan.HEIR_OF_THE_DIVINE;
            tsm.sendStat(MukHyunDivine, o1);
        }
        if (this.powerTimer == null || this.powerTimer.isCancelled()) {
            ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(() -> {
                if (exitPrepared || chr.getClient() == null) {
                    return;
                }
                this.powerType.set(0);
                this.godPower.set(0);
                sendStackSync(chr);
            }, 3000, 5000, false);
            this.powerTimer = sf;
            GlobalTimerManager.addCharTimer(chr.getId(), sf);
        }
        super.handleInitAfterMigrate(chr);
    }
}
