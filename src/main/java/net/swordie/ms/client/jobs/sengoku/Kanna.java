package net.swordie.ms.client.jobs.sengoku;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.SkillStat.x;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Kanna extends Job {

    // Beginner Job
    public static final int BLESSING_OF_THE_FIVE_ELEMENTS = 40020000;
    public static final int MANA_FLOW = 40020001;
    public static final int HAKU = 40020109;
    public static final int RETURN_OF_THE_FIVE_PLANETS = 40021227;

    // 1st Job
    public static final int SHIKIGAMI_HAUNTING_1_1 = 42001000;
    public static final int SHIKIGAMI_HAUNTING_1_2 = 42001005;
    public static final int SHIKIGAMI_HAUNTING_1_3 = 42001006; // spawns mana vein
    public static final int SHIKIGAME_HAUNTING_1_SLOW = 42100024; // mob debuff
    public static final int MANA_WARP = 42001002;
    public static final int GEOMANCY = 42000000;
    public static final int MANA_VEIN = 42001101;
    public static final int GHOST_YAKSHA_TRAINEE = 42001100;

    // 2nd Job
    public static final int SHIKIGAMI_HAUNTING_2_1 = 42101100;
    public static final int SHIKIGAMI_HAUNTING_2_2 = 42101101;
    public static final int SHIKIGAMI_HAUNTING_2_3 = 42101102; // spawns mana vein
    public static final int SHIKIGAMI_HAUNTING_2_SLOW = 42101103; // mob debuff
    public static final int RADIANT_PEACOCK = 42101003;
    public static final int GHOST_YAKSHA_BROTHER = 42100000;
    public static final int SHIKIGAMI_CHARM = 42101104; // spawns mana vein
    public static final int EXORCIST_CHARM = 42101005;
    public static final int HAKU_REBORN = 42101002;

    // 3rd Job
    public static final int SHIKIGAMI_ACTIVATION_SKILL = 42110013;
    public static final int SHIKIGAMI_HAUNTING_3_1 = 42111110;
    public static final int SHIKIGAMI_HAUNTING_3_2 = 42111111;
    public static final int SHIKIGAMI_HAUNTING_3_3 = 42111112; // spawns mana vein
    public static final int GHOST_YAKSHA_LIEUTENANT = 42110000;
    public static final int BLOSSOM_BARRIER = 42111004;
    public static final int YOSUZUME = 42110002;
    public static final int TENGU_STRIKE = 42111100;
    public static final int TENGU_STRIKE_SUMMON_L = 42111101;
    public static final int TENGU_STRIKE_SUMMON_R = 42111102;
    public static final int MANA_BALANCE = 42111012;
    public static final int WARDING_BARRIER = 42110001;

    // 4th Job
    public static final int SHIKIGAMI_HAUNTING_4_1 = 42120026;
    public static final int SHIKIGAMI_HAUNTING_4_2 = 42120027;
    public static final int SHIKIGAMI_HAUNTING_4_3 = 42120028; // spawns mana vein
    public static final int GHOST_YAKSHA_BOSS = 42120001;
    public static final int FALLING_SAKURA = 42121002; // spawns mana vein
    public static final int HAKU_PERFECTED = 42120011;
    public static final int OROCHI_UNBOUND = 42121100;
    public static final int OROCHI_UNBOUND2 = 42121101;
    public static final int SHIKIGAMI_DOPPLEGANGER = 42121104;
    public static final int SHIKIGAMI_DOPPLEGANGER_ACTIVATED = 42121105;

    // Old Skills
    public static final int KISHIN_SHOUKAN = 42111003; //summon
    public static final int LIFEBLOOD_RITUAL = 42110008;
    public static final int BELLFLOWER_BARRIER = 42121005; //AoE
    public static final int BELLFLOWER_BARRIER_PERSIST_H = 42120049;
    public static final int BELLFLOWER_BARRIER_BOSS_RUSH_H = 42120051;
    public static final int AKATUSKI_HERO_KANNA = 42121006;
    public static final int NINE_TAILED_FURY = 42121102; //Attacking Skill + Buff
    public static final int NINE_TAILED_FURY_PASSIVE = 42120000;
    public static final int BINDING_TEMPEST = 42121004;
    public static final int BLOSSOMING_DAWN = 42121007;
    public static final int FALLING_SAKURA_VITALITY_H = 42120048;
    public static final int VERITABLE_PANDEMONIUM = 42121052; //Immobility Debuff
    public static final int PRINCESS_VOW_KANNA = 42121053;
    public static final int BLACKHEARTED_CURSE = 42121054;

    //Haku Buffs
    public static final int HAKUS_GIFT = 80001842;
    public static final int FOXFIRE = 42101021;
    public static final int HAKUS_BLESSING = 42101022;
    public static final int BREATH_UNSEEN = 42101023;
    public static final int FOXFIRE_2 = 42121021;
    public static final int HAKUS_BLESSING_2 = 42121022;
    public static final int BREATH_UNSEEN_2 = 42121023;
    public static final int HAKUS_GIFT_2 = 80011056;

    // Hyper Skills
    public static final int GEOMANCY_SPREAD = 42120044;
    public static final int GEOMANCY_PERSIST = 42120050;

    // V Skills
    public static final int YUKI_MUSUME_SHOUKAN = 400021017;
    public static final int YUKI_MUSUME_SHOUKAN_SUMMON = 400021018;
    public static final int SPIRITS_DOMAIN = 400021054;
    public static final int LIBERATED_SPIRIT_CIRCLE_SMALL = 400021078;
    public static final int LIBERATED_SPIRIT_CIRCLE_BIG = 400021080;
    public static final int LIBERATED_SPIRIT_CIRCLE_SUMMON = 400021079;
    public static final int LIBERATED_SPIRIT_CIRCLE_SUMMON_2 = 400021081;
    public static final int PRINCESS_SAKUNO_BLESSING = 400001057;
    public static final int GHOST_YAKSHA_GREAT_ONI_LORD_LEGION = 400021114;

    // HEXA skills
    public static final int HEXA_KISHIN_SHOUKAN = 42141011; //summon
    public static final int HEXA_YOSUZUME = 42141007;
    public static final int HEXA_TENGU_STRIKE_SUMMON_R = 42141014;
    public static final int HEXA_TENGU_STRIKE_SUMMON_L = 42141013;
    public static final int HEXA_TENGU_STRIKE = 42141012;
    public static final int HEXA_SHIKIGAMI_DOPPLEGANGER = 42141005;
    public static final int HEXA_SHIKIGAMI_DOPPLEGANGER_ACTIVATED = 42141006;
    public static final int HEXA_NINE_TAILED_FURY = 42141009; //Attacking Skill + Buff
    public static final int HEXA_NINE_TAILED_FURY_PASSIVE = 42141010;
    public static final int HEXA_GHOST_YAKSHA_BOSS = 42141015;
    // 42141008 HEXA Nightghost Guide

    // HEXA Boosts
    public static final int HEXA_SPIRITS_DOMAIN = 500061069; // TODO

    private final int[] addedSkills = new int[]{
            RETURN_OF_THE_FIVE_PLANETS, BLESSING_OF_THE_FIVE_ELEMENTS, MANA_FLOW, HAKU
    };

    public int incMP = 0;
    public ScheduledFuture<?> manaFlowTimer;
    /** 282 紫扇傳授 counter (research/tms2823/kanna-wh-282). */
    private final Tms282Kanna.FanTransmission fanTransmission = new Tms282Kanna.FanTransmission();
    private final Tms282KannaOniController oniController = new Tms282KannaOniController(chr, this);

    public Tms282KannaOniController getOniController() {
        return oniController;
    }

    /** TMS 282.3: the job was reworked (shikigami); see {@link Tms282Kanna}. 265 keeps every path below. */
    private static boolean tms282() {
        return Tms282Kanna.active();
    }

    public Kanna(Char chr) {
        super(chr);
        if (chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
            for (int id : addedSkills) {
                if (tms282() && id == HAKU) {
                    continue; // 40020109 is not in 282 Skill.wz (282 花狐的同行 is 40021109)
                }
                if (!chr.hasSkill(id)) {
                    Skill skill = SkillData.getSkillDeepCopyById(id);
                    if (skill == null) {
                        continue;
                    }
                    skill.setCurrentLevel(skill.getMaxLevel());
                    chr.addSkill(skill);
                }
            }
        }
    }

    public static void hakuGift(Char chr) {
        int skillID = !hasHakuPerfected(chr) ? HAKUS_GIFT : HAKUS_GIFT_2;
        Skill skill = chr.getSkill(skillID);
        int slv = getHakuSkillLevel(chr);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        if (!hasHakuPerfected(chr)) {
            if ((chr.getMaxHP() - chr.getHP()) >= (int) (chr.getMaxHP() * 0.1)) {
                chr.heal((int) (chr.getMaxHP() / ((double) 100 / si.getValue(hp, slv))));
            }
        } else {
            for (Char member : chr.getParty().getOnlineChars()) {
                if ((member.getMaxHP() - member.getHP()) < (int) (member.getMaxHP() * 0.3)) {
                    return;
                } else {
                    member.heal((int) (member.getMaxHP() / ((double) 100 / si.getValue(hp, slv))));
                }
            }
        }
    }

    public static void hakuFoxFire(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(FireBarrier)) {
            int skillID = !hasHakuPerfected(chr) ? FOXFIRE : FOXFIRE_2;
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = getHakuSkillLevel(chr);
            Option o1 = new Option();
            o1.nOption = 3;
            o1.rOption = si.getSkillId();
            o1.tOption = si.getValue(time, slv);
            tsm.sendStat(FireBarrier, o1);
        }
    }

    public static void hakuHakuBlessing(Char chr) {
        int skillID = !hasHakuPerfected(chr) ? HAKUS_BLESSING : HAKUS_BLESSING_2;
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = getHakuSkillLevel(chr);
        int value = si.getValue(x, slv);
        if (chr.getParty() == null) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (!tsm.hasStat(FoxBless)) {
                Option o1 = new Option();
                o1.nOption = value;
                o1.rOption = skillID;
                tsm.sendStat(FoxBless, o1);
            }
        } else {
            for (Char player : chr.getParty().getPartyMembersInSameFieldWithChr(chr)) {
                TemporaryStatManager tsm = player.getTemporaryStatManager();
                if (!tsm.hasStat(FoxBless)) {
                    Option o1 = new Option();
                    o1.nOption = value;
                    o1.rOption = skillID;
                    tsm.sendStat(FoxBless, o1);
                }
            }
        }
    }

    public static void hakuBreathUnseen(Char chr) {
        int skillID = BREATH_UNSEEN;
        if (chr.getParty() == null) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (!tsm.hasStat(BlessEnsenble)) {
                Option o1 = new Option();
                o1.nOption = 1;
                o1.rOption = skillID;
                tsm.sendStat(BlessEnsenble, o1);
            }
        } else {
            int size = chr.getParty().getPartyMembersInSameFieldWithChr(chr).size();
            for (Char player : chr.getParty().getPartyMembersInSameFieldWithChr(chr)) {
                TemporaryStatManager tsm = player.getTemporaryStatManager();
                if (!tsm.hasStat(FoxBless)) {
                    Option o1 = new Option();
                    o1.nOption = size;
                    o1.rOption = skillID;
                    tsm.sendStat(BlessEnsenble, o1);
                }
            }
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isKanna(id);
    }

    public void spawnHaku() {
        if (chr.getFoxMan().isTranformed()) {
            chr.getField().removeLife(chr.getSkillPet());
            chr.getFoxMan().setOwnerChar(chr);
            chr.getFoxMan().setMoveAction((byte) 4);
            chr.getFoxMan().setUpgrade(1);
            chr.getField().spawnLife(chr.getFoxMan(), null);
            // Hide small Haku
        } else {
            chr.getField().removeLife(chr.getFoxMan());
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            if (tsm.hasStat(FoxBless)) {
                tsm.removeStat(FoxBless);
            }
            if (tsm.hasStat(BlessEnsenble)) {
                tsm.removeStat(BlessEnsenble);
            }
            if (tsm.hasStat(FireBarrier)) {
                tsm.removeStat(FireBarrier);
            }
            chr.getField().spawnLife(chr.getSkillPet(), null);
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        if (tms282()) {
            // Every 265 case is another skill on 282 (Tms282Kanna.CHANGED_265_IDS): no slow / burn / stun from them.
            super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
            return;
        }
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();

        switch (skillID) {
            case SHIKIGAMI_HAUNTING_1_1:
            case SHIKIGAMI_HAUNTING_1_2:
            case SHIKIGAMI_HAUNTING_1_3:
            case SHIKIGAMI_HAUNTING_2_1:
            case SHIKIGAMI_HAUNTING_2_2:
            case SHIKIGAMI_HAUNTING_2_3:
            case SHIKIGAMI_HAUNTING_3_1:
            case SHIKIGAMI_HAUNTING_3_2:
            case SHIKIGAMI_HAUNTING_3_3:
            case SHIKIGAMI_HAUNTING_4_1:
            case SHIKIGAMI_HAUNTING_4_2:
            case SHIKIGAMI_HAUNTING_4_3:
                si = SkillData.getSkillInfoById(SHIKIGAMI_HAUNTING_2_SLOW);
                if (si != null) {
                    if (!mts.hasCurrentMobStatBySkillId(SHIKIGAMI_HAUNTING_2_SLOW)) {
                        if (Util.succeedProp(si.getValue(prop, slv))) {
                            slv = 1;
                            o1.nOption = si.getValue(x, slv);
                            o1.rOption = SHIKIGAMI_HAUNTING_2_SLOW;
                            o1.tOption = si.getValue(time, slv);
                            mts.addStatOptions(mob, MobStat.Speed, o1);
                            if (chr.hasSkill(SHIKIGAMI_ACTIVATION_SKILL)) {
                                BurnedInfo bi = BurnedInfo.createBurnInfo(chr, SHIKIGAMI_HAUNTING_2_SLOW, chr.getSkillLevel(SHIKIGAMI_ACTIVATION_SKILL), damage);
                                mts.createAndAddBurnedInfo(mob, bi, SHIKIGAMI_HAUNTING_2_SLOW);
                            }
                        }
                    }
                }
                break;
            case YOSUZUME:
            case HEXA_YOSUZUME:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = si.getValue(v, slv);
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.AddDamSkill2, o1);
                }
                break;
            case BINDING_TEMPEST:
            case VERITABLE_PANDEMONIUM:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    mts.addStatOptions(mob, MobStat.Stun, o1);
                }
                break;
            case MANA_WARP:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss() && Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
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
        if (tms282()) {
            handleAttack282(attackInfo, si);
            return;
        }

        if (hasHitMobs) {
            if (skillID != YOSUZUME && skillID != HEXA_YOSUZUME && skillID != 0) {
                yosuzume();
            }
            if (chr.getField().getSummonBySkillId(chr, YUKI_MUSUME_SHOUKAN_SUMMON) != null
                    && chr.hasSkill(YUKI_MUSUME_SHOUKAN)
                    && tsm.hasStat(KannaSiksinAutoAttack)) {
                doYukiMusumeShoukanAttack();
            }
            lifeBloodRitual(attackInfo);
        }
        if (SkillConstants.isSpiritWalkerSkill(skillID)) {
            spawnManaVein();
        }
        Option o1 = new Option();
        switch (skillID) {
            case GHOST_YAKSHA_TRAINEE:
            case GHOST_YAKSHA_BROTHER:
            case GHOST_YAKSHA_LIEUTENANT:
            case GHOST_YAKSHA_BOSS:
            case HEXA_GHOST_YAKSHA_BOSS:
                if (attackInfo.attackActionType == 34 && tsm.hasStatBySkillId(getGhostSkill())) {
                    // 25 is ActionType for the Leaving Attack
                    tsm.removeStatsBySkill(getGhostSkill());
                }
                break;
            case EXORCIST_CHARM:
                AffectedArea aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setPosition(chr.getPosition());
                aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
                aa.setDelay((short) 15);
                chr.getField().spawnAffectedArea(aa);
                break;
            case FALLING_SAKURA:
                List<Char> chrList = new ArrayList<>();
                if (chr.getParty() != null) {
                    chrList.addAll(chr.getParty().getPartyMembersInSameField(chr));
                }
                chrList.add(chr);
                int percentHealed = si.getValue(x, slv) + (chr.hasSkill(FALLING_SAKURA_VITALITY_H) ? 20 : 0);
                for (Char pChr : chrList) {
                    pChr.heal((int) ((pChr.getMaxHP() * percentHealed) / 100D));
                }
                break;
            case NINE_TAILED_FURY:
                o1.nReason = NINE_TAILED_FURY_PASSIVE;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                chr.setSkillCooldown(skillID, slv);
                break;
            case HEXA_NINE_TAILED_FURY:
                o1.nReason = HEXA_NINE_TAILED_FURY_PASSIVE;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                chr.setSkillCooldown(skillID, slv);
                break;
            case YUKI_MUSUME_SHOUKAN:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(KannaSiksinAutoAttack, o1);
                Summon summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setSkillID(YUKI_MUSUME_SHOUKAN_SUMMON);
                summon.setAssistType(AssistType.AttackCounter);
                summon.setMoveAbility(MoveAbility.Walk);
                chr.getField().spawnSummon(summon);
                break;
            case OROCHI_UNBOUND2:
                chr.addSkillCooldown(OROCHI_UNBOUND, 90000);
                break;
        }
    }

    /**
     * 282 attack path: pay the attack's {@code mpCon} (265 applyMpCon charged Kanna {@code epCon} = 0 on 282) and run
     * 紫扇傳授. None of the 265 attack logic (夜雀 atoms, 靈脈 veins, 鬼夜叉, 碎心符 party heal, 九尾 IndieDamR,
     * 雪女 KannaSiksinAutoAttack) applies to the 282 ids.
     */
    private void handleAttack282(AttackInfo attackInfo, SkillInfo si) {
        int skillID = attackInfo.skillId;
        int slv = attackInfo.slv;
        if (si != null && skillID != 0
                && !SkillConstants.isShootObj(si)
                && !SkillConstants.isNoMPConsumeSkill(skillID)
                && !net.swordie.ms.constants.SixthJobSkillConstants.isRecognizedOriginSkill(skillID, si)
                && !net.swordie.ms.constants.SixthJobSkillConstants.isRecognizedAscentSkill(skillID, si)) {
            int cost = Tms282Kanna.attackMpCost(si, slv, chr.getTotalStat(BaseStat.mpconReduce));
            if (cost > 0) {
                chr.addStatAndSendPacket(Stat.mp, -Math.min(cost, chr.getMP()));
            }
        }
        int fanLv = chr.getSkillLevel(Tms282Kanna.PURPLE_FAN_TRANSMISSION);
        if (fanLv > 0 && Tms282Kanna.countsAsAttackSkill(skillID, si)) {
            SkillInfo fan = SkillData.getSkillInfoById(Tms282Kanna.PURPLE_FAN_TRANSMISSION);
            if (fan != null && fanTransmission.onAttackSkill(fan.getValue(x, fanLv))) {
                Option o = new Option();
                o.nReason = Tms282Kanna.PURPLE_FAN_TRANSMISSION;
                o.nValue = fan.getValue(indieDamR, fanLv);
                o.tTerm = fan.getValue(time, fanLv);
                if (o.nValue > 0 && o.tTerm > 0) {
                    chr.getTemporaryStatManager().sendStat(IndieDamR, o);
                }
            }
        }
    }

    /** 282 skill-use effects after the CP04B4 service (cost, cooldown, indie* buffs from Skill.wz) accepted the use. */
    private void handleSkill282(SkillUseInfo skillUseInfo) {
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (skillUseInfo.skillID) {
            case Tms282KannaOniController.GHOST, Tms282KannaOniController.GHOST_VI ->
                    oniController.afterAccepted(skillUseInfo);
            case Tms282Kanna.SPIRIT_WARD_BARRIER -> {
                // 結界：咒靈護身: "#time秒無敵" (282 type 10, time 3); the service resolver has no stat for it.
                int seconds = si == null ? 0 : si.getValue(time, slv);
                if (seconds > 0) {
                    Option o = new Option();
                    o.nReason = skillUseInfo.skillID;
                    o.nValue = 1;
                    o.tTerm = seconds;
                    tsm.sendStat(IndieNotDamaged, o);
                }
            }
            case Tms282Kanna.DAWN_WILL -> tsm.removeAllDebuffs();
            default -> {
                // 曉之庇護 / 展開孔雀 / 紋櫻公主的祝福: the service's Skill.wz resolver already sent the indie stats.
                // Everything else is a shikigami / 282-only mechanic (README "needs client RE").
            }
        }
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        if (tms282()) {
            handleSkill282(skillUseInfo);
            return;
        }
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();
        Summon summon;
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (skillID) {
            case HAKU_REBORN:
                if (chr.getFoxMan().isTranformed()) {
                    spawnHaku();
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, getHakuSkillLevel(chr));
                    tsm.sendStat(ChangeFoxMan, o1);
                    chr.getFoxMan().setTranformed(true);
                    spawnHaku();
                }
                break;
            case GHOST_YAKSHA_TRAINEE:
            case GHOST_YAKSHA_BROTHER:
            case GHOST_YAKSHA_LIEUTENANT:
            case GHOST_YAKSHA_BOSS:
            case HEXA_GHOST_YAKSHA_BOSS:
                summon = Summon.getSummonByAndSetStat(chr, getGhostSkill(), slv);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.Attack);
                summon.setEnterType(EnterType.Animation);
                summon.setLeaveType(LeaveType.ATTACK_AFTER_DEAD);
                summon.setMoveAction((byte) Util.getRandom(4, 5));
                field.spawnSummon(summon);
                break;
            case RADIANT_PEACOCK:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(Booster, o1);
                break;
            case TENGU_STRIKE:
                if (hasTenguStrikeActive()) {
                    Summon left = field.getSummonBySkillId(chr, TENGU_STRIKE_SUMMON_L);
                    Summon right = field.getSummonBySkillId(chr, TENGU_STRIKE_SUMMON_R);
                    if (left != null && right != null) {
                        field.broadcast(Summoned.reposition(left, TENGU_STRIKE_SUMMON_L, chr.getPosition()));
                        field.broadcast(Summoned.assistSpecialAttackRequest(left, 11));
                        field.broadcast(Summoned.reposition(right, TENGU_STRIKE_SUMMON_R, chr.getPosition()));
                        field.broadcast(Summoned.assistSpecialAttackRequest(right, 11));
                        chr.addSkillCooldown(TENGU_STRIKE, 3000);
                    }
                } else {
                    for (int i = TENGU_STRIKE_SUMMON_L; i <= TENGU_STRIKE_SUMMON_R; i++) {
                        summon = Summon.getSummonBy(chr, i, slv);
                        summon.setMoveAbility(MoveAbility.Fly);
                        summon.setMoveAction((byte) 4);
                        summon.setAssistType(AssistType.Attack);
                        summon.setEnterType(EnterType.Animation);
                        field.spawnSummon(summon);
                        int xTranslation = i == TENGU_STRIKE_SUMMON_L ? -600 : 600;
                        field.broadcast(Summoned.reposition(summon, i, new Position(summon.getX() + xTranslation, summon.getY() - 50)));
                    }
                }
                break;
            case HEXA_TENGU_STRIKE:
                if (hasTenguStrikeActive()) {
                    Summon left = field.getSummonBySkillId(chr, HEXA_TENGU_STRIKE_SUMMON_L);
                    Summon right = field.getSummonBySkillId(chr, HEXA_TENGU_STRIKE_SUMMON_R);
                    if (left != null && right != null) {
                        field.broadcast(Summoned.reposition(left, HEXA_TENGU_STRIKE_SUMMON_L, chr.getPosition()));
                        field.broadcast(Summoned.assistSpecialAttackRequest(left, 11));
                        field.broadcast(Summoned.reposition(right, HEXA_TENGU_STRIKE_SUMMON_R, chr.getPosition()));
                        field.broadcast(Summoned.assistSpecialAttackRequest(right, 11));
                        chr.addSkillCooldown(HEXA_TENGU_STRIKE, 3000);
                    }
                } else {
                    for (int i = HEXA_TENGU_STRIKE_SUMMON_L; i <= HEXA_TENGU_STRIKE_SUMMON_R; i++) {
                        summon = Summon.getSummonBy(chr, i, slv);
                        summon.setMoveAbility(MoveAbility.Fly);
                        summon.setMoveAction((byte) 4);
                        summon.setAssistType(AssistType.Attack);
                        summon.setEnterType(EnterType.Animation);
                        field.spawnSummon(summon);
                        int xTranslation = i == HEXA_TENGU_STRIKE_SUMMON_L ? -600 : 600;
                        field.broadcast(Summoned.reposition(summon, i, new Position(summon.getX() + xTranslation, summon.getY() - 50)));
                    }
                }
                break;
            case MANA_BALANCE: // HP subtraction  done by client, this is only to server matches.
                chr.setStatAndSendPacket(Stat.hp, chr.getHPPerc(si.getValue(hp, slv)));
                break;
            case BLOSSOM_BARRIER:
                spawnBlossomBarrier();
                break;
            case BELLFLOWER_BARRIER:
                spawnBellflowerBarrier();
                break;
            case BLOSSOMING_DAWN:
                tsm.removeAllDebuffs();
                break;
            case LIBERATED_SPIRIT_CIRCLE_SUMMON:
            case LIBERATED_SPIRIT_CIRCLE_SUMMON_2:
                Position endPosition = inPacket.decodePosition();

                SkillInfo ssi = SkillData.getSkillInfoById(skillID);
                si = SkillData.getSkillInfoById(LIBERATED_SPIRIT_CIRCLE_SMALL);
                slv = chr.getSkillLevel(LIBERATED_SPIRIT_CIRCLE_SMALL);
                int soulAmount = si.getValue((skillID == LIBERATED_SPIRIT_CIRCLE_SUMMON_2 ? u2 : u), slv);
                Rect rect = endPosition.getRectAround(ssi.getFirstRect());

                for (int i = 0; i < soulAmount; i++) {
                    Foothold fh = Util.getRandomFromCollection(field.getFootholdsInRect(rect));
                    summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setAssistType(AssistType.Attack);
                    summon.setPosition(fh.getRandomPosition());
                    summon.setCurFoothold((short) (int) java.util.Optional.ofNullable(field.findFootHoldBelow(summon.getPosition())).map(below -> below.getId()).orElse(0) /* in the air: no foothold (was an NPE) */);
                    field.spawnAddSummon(summon);
                }
                break;
            case KISHIN_SHOUKAN:
                Summon.createKishin(chr, slv);
                break;
            case HEXA_KISHIN_SHOUKAN:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setMoveAction((byte) 1);
                summon.setAssistType(AssistType.Attack);
                chr.getField().spawnSummon(summon);
                break;
            case PRINCESS_VOW_KANNA:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case BLACKHEARTED_CURSE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(AntiEvilShield, o1);
                break;
            case SPIRITS_DOMAIN:
            case HEXA_SPIRITS_DOMAIN:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                chr.getField().spawnSummon(summon);
                createSpiritDomainAA(summon);
                break;
            case SHIKIGAMI_DOPPLEGANGER:
            case HEXA_SHIKIGAMI_DOPPLEGANGER:
                o2.nOption = 0;
                o2.rOption = skillID;
                tsm.sendStat(KannaSiksinAutoAttack, o2);
                break;
            case PRINCESS_SAKUNO_BLESSING:
                o1.nReason = skillID;
                o1.nValue = si.getValue(q, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePMdR, o1);
                break;
            case YUKI_MUSUME_SHOUKAN:
                summon = Summon.getSummonByAndSetStat(c.getChr(), YUKI_MUSUME_SHOUKAN_SUMMON, slv);
                summon.setFlyMob(true);
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setAssistType(AssistType.Attack);
                summon.setAttackActive(true);
                chr.getField().spawnSummon(summon);
                chr.addSkillCooldown(YUKI_MUSUME_SHOUKAN, si.getValue(cooltime, slv) * 1000);
                break;
            case GHOST_YAKSHA_GREAT_ONI_LORD_LEGION:
                if (!tsm.hasStatBySkillId(skillID)) {
                    EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                    summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                    summon.setMoveAbility(MoveAbility.Walk);
                    summon.setAssistType(AssistType.Attack);
                    summon.setLeaveType(LeaveType.ATTACK_AFTER_DEAD);
                    summon.setAttackActive(true);
                    chr.getField().spawnSummon(summon);
                    o1.nValue = 1;
                    o1.nReason = skillID;
                    o1.tTerm = 2;
                    newStats.put(IndieNotDamaged, o1);
                    o2.nValue = 1;
                    o2.nReason = skillID;
                    o2.tTerm = 2;
                    newStats.put(IndieIgnorePCounter, o2);
                    o3.tOption = 0;
                    o3.zOption = 0;
                    newStats.put(KannaFifthAttract, o3);
                    tsm.sendStat(newStats);
                } else {
                    tsm.removeStatsBySkill(skillID);
                    tsm.removeStat(KannaFifthAttract);
                }
                break;
        }
    }

    private int getGhostSkill() {
        if (chr.getSkillLevel(HEXA_GHOST_YAKSHA_BOSS) > 0) {
            return HEXA_GHOST_YAKSHA_BOSS;
        }
        if (chr.getSkillLevel(GHOST_YAKSHA_BOSS) > 0) {
            return GHOST_YAKSHA_BOSS;
        }
        if (chr.getSkillLevel(GHOST_YAKSHA_LIEUTENANT) > 0) {
            return GHOST_YAKSHA_LIEUTENANT;
        }
        if (chr.getSkillLevel(GHOST_YAKSHA_BROTHER) > 0) {
            return GHOST_YAKSHA_BROTHER;
        }
        return GHOST_YAKSHA_TRAINEE;
    }

    public void chargeMPShikigami(int mpCon) {
        if (tms282()) {
            return; // 42141005 is 飛身下擊VI on 282; KannaSiksinAutoAttack has no 282 CTS row
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = chr.hasSkill(HEXA_SHIKIGAMI_DOPPLEGANGER) ? HEXA_SHIKIGAMI_DOPPLEGANGER : SHIKIGAMI_DOPPLEGANGER;
        if (!chr.hasSkill(skillID)) {
            return;
        }
        int activateSkillID = chr.hasSkill(HEXA_SHIKIGAMI_DOPPLEGANGER) ? HEXA_SHIKIGAMI_DOPPLEGANGER_ACTIVATED : SHIKIGAMI_DOPPLEGANGER_ACTIVATED;
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkillLevel(skillID);
        Option o = new Option();
        this.incMP += mpCon;
        if (this.incMP >= si.getValue(x, slv)) {
            o.nOption = si.getValue(x, slv); // Once charged with #x Mana
            o.rOption = skillID;
            tsm.sendStat(KannaSiksinAutoAttack, o);
            chr.write(UserLocal.skillUseResult((byte) 1, activateSkillID));
            this.incMP = 0;
        } else {
            o.nOption = this.incMP; // Once charged with #x Mana
            o.rOption = skillID;
            tsm.sendStat(KannaSiksinAutoAttack, o);
        }
    }

    public void spawnBlossomBarrier() {
        if (!chr.hasSkill(BLOSSOM_BARRIER)) {
            return;
        }
        Skill skill = chr.getSkill(BLOSSOM_BARRIER);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int maxBarriers = chr.getSkillStatValue(x, WARDING_BARRIER);
        if (getBlossomBarriers() < maxBarriers) {
            Field field = chr.getField();
            Rect rect = chr.getRectAround(new Rect(-100, -100, 100, 100));
            List<AffectedArea> affectAreasInRect = field.getAffectAreasInRect(rect).stream().filter(aa -> aa.getCharID() == chr.getId() && aa.getSkillID() == MANA_VEIN).collect(Collectors.toList());
            boolean hasAAsInRect = affectAreasInRect.size() > 0;
            if (hasAAsInRect) {
                affectAreasInRect.forEach(field::removeLife);
            }
            AffectedArea aa = AffectedArea.getPassiveAA(chr, skill.getSkillId(), slv);
            aa.setPosition(chr.getPosition());
            aa.setRect(aa.getPosition().getRectAround(si.getFirstRect()));
            aa.setDelay((short) 3);
            field.spawnAffectedArea(aa);
            if (hasAAsInRect) {
                aa.activateTimer(1000, 1000);
            }
        }
    }

    public int getBlossomBarriers() {
        return (int) chr.getField().getAffectedAreas().stream().filter(aa -> aa.getCharID() == chr.getId() && aa.getSkillID() == BLOSSOM_BARRIER).count();
    }

    public void spawnBellflowerBarrier() {
        if (!chr.hasSkill(BELLFLOWER_BARRIER)) {
            return;
        }
        Field field = chr.getField();
        Skill skill = chr.getSkill(BELLFLOWER_BARRIER);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        Rect rect = chr.getRectAround(new Rect(-100, -100, 100, 100));
        List<AffectedArea> affectAreasInRect = field.getAffectAreasInRect(rect).stream().filter(aa -> aa.getCharID() == chr.getId() && aa.getSkillID() == MANA_VEIN).collect(Collectors.toList());
        boolean hasAAsInRect = affectAreasInRect.size() > 0;
        if (hasAAsInRect) {
            affectAreasInRect.forEach(field::removeLife);
        }

        AffectedArea aa = AffectedArea.getPassiveAA(chr, skill.getSkillId(), slv);
        aa.setPosition(chr.getPosition());
        aa.setDuration((si.getValue(time, slv) + (chr.hasSkill(BELLFLOWER_BARRIER_PERSIST_H) ? 20 : 0)) * 1000);
        aa.setRect(aa.getPosition().getRectAround(si.getRects().get(0)));
        aa.setDelay((short) 3);
        field.spawnAffectedAreaAndRemoveOld(aa);
        if (hasAAsInRect) {
            aa.activateTimer(1000, 1000);
        }
    }

    private int getAmountOfManaVeins() {
        return (int) chr.getField().getAffectedAreas().stream().filter(aa -> aa.getCharID() == chr.getId() && aa.getSkillID() == MANA_VEIN).count();
    }

    private void spawnManaVein() {
        Skill skill = chr.getSkill(GEOMANCY);
        if (!chr.hasSkill(skill.getSkillId())) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        SkillInfo mvsi = SkillData.getSkillInfoById(MANA_VEIN);

        int maxManaVeins = si.getValue(x, slv) + chr.getSkillStatValue(x, GEOMANCY_SPREAD);
        if (getAmountOfManaVeins() < maxManaVeins && !chr.hasSkillOnCooldown(GEOMANCY)) {
            Field field = chr.getField();
            Rect rect = new Rect(-300, -300, 300, 300);
            try {
                Position position = Util.getRandomFromCollection(field.getFootholdsInRect(chr.getPosition().getRectAround(rect)).stream().filter(fh -> !fh.isWall()).collect(Collectors.toList())).getRandomPosition();
                AffectedArea aa = AffectedArea.getPassiveAA(chr, MANA_VEIN, slv);
                aa.setPosition(position);
                aa.setDuration(5000 + (mvsi.getValue(time, 1) + (chr.getSkillStatValue(time, GEOMANCY_PERSIST))) * 1000);
                aa.setRect(aa.getPosition().getRectAround(SkillData.getSkillInfoById(MANA_VEIN).getFirstRect()));
                aa.setDelay((short) 4);
                field.spawnAffectedArea(aa);
                aa.activateTimer(1000, 1000);
                chr.addSkillCooldown(skill.getSkillId(), (int) (si.getValue(cooltime, slv) * 1000L));
            } catch (NullPointerException ex) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, ex);
            }
        }
    }

    private void yosuzume() {
        int skillID = chr.hasSkill(HEXA_YOSUZUME) ? HEXA_YOSUZUME : YOSUZUME;
        Skill skill = chr.getSkill(skillID);
        if (!chr.hasSkill(skillID)) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        int chance = si.getValue(prop, slv);
        if (Util.succeedProp(chance)) {
            createYosuzumeForceAtoms();
        }
    }

    private void createYosuzumeForceAtoms() {
        Random random = new Random();
        ForceAtomEnum fae = ForceAtomEnum.YOSUZUME_1;
        Mob mob = Util.getRandomFromCollection(chr.getField().getMobsInRect(chr.getPosition().getRectAround(new Rect(-300, -300, 300, 300))));
        int angle = random.nextInt(360);
        if (mob != null) {
            angle = (int) Util.getAngleOfTwoPositions(chr.getPosition(), mob.getPosition());
        }
        ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), random.nextInt(13) + 50, 6,
                angle, 500, Util.getCurrentTime(), 0, 0,
                new Position());
        ForceAtom fa = new ForceAtom(chr.getId(), fae, mob == null || mob.getHp() <= 0 ? 0 : mob.getObjectId(), chr.hasSkill(HEXA_YOSUZUME) ? HEXA_YOSUZUME : YOSUZUME, fai);
        chr.createForceAtom(fa);
    }

    private void lifeBloodRitual(AttackInfo attackInfo) {
        if (!chr.hasSkill(LIFEBLOOD_RITUAL)) {
            return;
        }
        Skill skill = chr.getSkill(LIFEBLOOD_RITUAL);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int healed = (int) ((chr.getMaxHP() * si.getValue(x, slv)) / 100D);
        boolean hasHealed = false;
        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
            Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
            if (mob == null || mob.getHp() <= 0) {
                continue;
            }
            long totalDmg = Arrays.stream(mai.damages).sum();
            if (totalDmg >= mob.getHp()) {
                chr.heal(healed);
                hasHealed = true;
            }
        }
        if (hasHealed) {
            chr.write(UserPacket.effect(Effect.skillAffected(skill.getSkillId(), slv, healed)));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillAffected(skill.getSkillId(), slv, healed)), chr);
        }
    }

    private void doYukiMusumeShoukanAttack() {
        if (!chr.hasSkill(YUKI_MUSUME_SHOUKAN)) {
            return;
        }
        Skill skill = chr.getSkill(YUKI_MUSUME_SHOUKAN);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int randomInt = new Random().nextInt(2) + 10;
        chr.getField().broadcast(Summoned.assistAttackRequest(chr.getField().getSummonBySkillId(chr, YUKI_MUSUME_SHOUKAN_SUMMON), randomInt));
        chr.heal((int) ((chr.getMaxHP() * si.getValue(y, slv)) / 100D));
    }

    private int getManaConsumptionBySkill(int skillId) {
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (si != null) {
            return si.getValue(epCon, chr.getSkillLevel(skillId));
        }
        return 0;
    }

    private void createSpiritDomainForceAtom(int skillID) {
        Summon summon = getSpiritDomainSummon();
        if (summon == null) {
            return;
        }
        ForceAtomEnum fae = ForceAtomEnum.SPIRIT_DOMAIN;
        List<ForceAtomInfo> faiList = new ArrayList<>();
        int atomsCreated = getManaConsumptionBySkill(skillID) / 5;
        int angle = chr.isLeft() ? 220 : 140;
        for (int i = 0; i < atomsCreated; i++) {
            int fImpact = new Random().nextInt(7) + 23;
            int sImpact = new Random().nextInt(3) + 3;
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), fImpact, sImpact,
                    new Random().nextInt(30) + angle - 15, 0, Util.getCurrentTime(), 1, 0, new Position());
            faiList.add(fai);
        }
        chr.createForceAtom(new ForceAtom(false, chr.getId(), chr.getId(), fae,
                false, Collections.singletonList(0), SPIRITS_DOMAIN, faiList, new Rect(), 0, 0,
                summon.getPosition(), SPIRITS_DOMAIN, summon.getPosition(), 0));
    }

    private void incrementSpiritDomainCount() {
        Summon summon = getSpiritDomainSummon();
        if (summon == null) {
            return;
        }
        int countPerAtom = 3;
        int skillID = chr.hasSkill(HEXA_SPIRITS_DOMAIN) ? HEXA_SPIRITS_DOMAIN : SPIRITS_DOMAIN;
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkillLevel(skillID);
        int stateIncReq = si.getValue(z, slv);
        summon.incCount(countPerAtom);
        if (summon.getCount() >= stateIncReq) {
            incrementSpiritDomainState();
        }
    }

    private void incrementSpiritDomainState() {
        Summon summon = getSpiritDomainSummon();
        if (summon == null) {
            return;
        }
        if (summon.getState() < 2) {
            summon.incState();
            summon.setCount(0);
            createSpiritDomainAA(summon);
            broadcastSpiritDomainState(getSpiritDomainSummon().getState());
        }
    }

    private void createSpiritDomainAA(Summon summon) {
        if (summon == null) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(summon.getSkillID());
        AffectedArea aa = AffectedArea.getPassiveAA(chr, summon.getSkillID(), summon.getSlv());
        aa.setPosition(summon.getPosition());
        aa.setRect(aa.getRectAround(si.getRects().get(summon.getState())));
        aa.setOption(summon.getState());
        aa.activateTimer(3000, 3000);
        chr.getField().spawnAffectedAreaAndRemoveOld(aa);
    }

    private void broadcastSpiritDomainState(int state) {
        Field field = chr.getField();
        for (Char otherChr : field.getChars()) {
            chr.write(Summoned.spiritDomainState(otherChr.getId(), chr.getId(), state));
        }
    }

    private Summon getSpiritDomainSummon() {
        return chr.getField().getSummonBySkillId(chr, SPIRITS_DOMAIN);
    }

    public void handleForceAtomCollision(int faKey, int skillId, int mobObjId, Position position, InPacket inPacket) {
        ForceAtom fa = chr.getForceAtomByKey(faKey);
        if (fa == null) {
            return;
        }
        if (fa.getSkillId() == SPIRITS_DOMAIN) {
            incrementSpiritDomainCount();
        }
        super.handleForceAtomCollision(faKey, skillId, mobObjId, position, inPacket);
    }

    private boolean hasTenguStrikeActive() {
        if (chr.hasSkill(HEXA_TENGU_STRIKE)) {
            return chr.getField().getSummonBySkillId(chr, HEXA_TENGU_STRIKE_SUMMON_R) != null
                    && chr.getField().getSummonBySkillId(chr, HEXA_TENGU_STRIKE_SUMMON_L) != null;
        }
        return chr.getField().getSummonBySkillId(chr, TENGU_STRIKE_SUMMON_R) != null
                && chr.getField().getSummonBySkillId(chr, TENGU_STRIKE_SUMMON_L) != null;
    }

    public static boolean hasHakuPerfected(Char chr) {
        return chr.hasSkill(HAKU_PERFECTED) && chr.getSkillLevel(HAKU_PERFECTED) != 0;
    }

    public static int getHakuSkillLevel(Char chr) {
        return chr.getSkillLevel(!hasHakuPerfected(chr) ? HAKU_REBORN : HAKU_PERFECTED);
    }

    public void giveKannaFifthAttract(long startTime) {
        SkillInfo si = SkillData.getSkillInfoById(GHOST_YAKSHA_GREAT_ONI_LORD_LEGION);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (si != null) {
            long now = System.currentTimeMillis();
            long time = now - startTime;
            if (!tsm.hasStat(KannaFifthAttract)) {
                Option o1 = new Option();
                o1.tOption = (int) now;
                o1.zOption = (int) time;
                tsm.sendStat(KannaFifthAttract, o1);
            } else {
                Option o1 = tsm.getOption(KannaFifthAttract);
                if (o1.zOption < 30 * 1000) {
                    o1.tOption = (int) now;
                    o1.zOption += (int) time;
                    tsm.sendStat(KannaFifthAttract, o1);
                }
            }
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        int foxfires = 6;
        if (tsm.hasStat(FireBarrier)) {
            if (foxfires > 1) {
                foxfires = foxfires - 1;
            }
            if (foxfires == 4 || foxfires == 3) {
                o.nOption = 2;
                tsm.sendStat(FireBarrier, o);
            } else if (foxfires == 2) {
                o.nOption = 1;
                tsm.sendStat(FireBarrier, o);
            } else if (foxfires == 1) {
                resetFireBarrier();
                tsm.sendStat(FireBarrier, o);
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    public void resetFireBarrier() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        tsm.removeStat(FireBarrier);
    }

    // Character creation related methods ---------------------------------------------------------------------------------------------
    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(JobConstants.KANNA_CREATION_MAP);
        cs.setMp(100);
        cs.setMaxMp(100);
    }

    @Override
    public void handleLevelUp(short level) {
        super.handleLevelUp(level);
        ScriptManagerImpl sm = chr.getScriptManager();
        if (level >= 30 && chr.getJob() == 4200) {
            sm.jobAdvance((short) 4210);
        }
        if (level >= 60 && chr.getJob() == 4210) {
            sm.jobAdvance((short) 4211);
            chr.completeQuest(57459);
        }
        if (level >= 100 && chr.getJob() == 4211) {
            sm.jobAdvance((short) 4212);
            chr.completeQuest(57460);
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        switch (cts) {
            case ChangeFoxMan:
                chr.getFoxMan().setTranformed(false);
                spawnHaku();
                break;
        }
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        if (tms282()) {
            oniController.cancel();
        }
        if (manaFlowTimer != null) {
            manaFlowTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        if (tms282()) {
            // 40020001 is 奇妙之手 (mastery) on 282; 400021114 is stale 265 data (weapon 55) and KannaFifthAttract
            // has no 282 CTS row.
            super.handleInitAfterMigrate(chr);
            return;
        }
        if (chr.hasSkill(MANA_FLOW)) {
            if (this.manaFlowTimer == null || this.manaFlowTimer.isCancelled()) {
                this.manaFlowTimer = chr.getTimer().addFixedRateEvent(() -> {
                    if (chr.getMP() < chr.getMaxMP()) {
                        chr.healMP(50);
                    }
                }, 1000, 6000, false);
            }
        }
        if  (chr.hasSkill(GHOST_YAKSHA_GREAT_ONI_LORD_LEGION)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            SkillInfo si = SkillData.getSkillInfoById(GHOST_YAKSHA_GREAT_ONI_LORD_LEGION);
            if (si != null) {
                if (!tsm.hasStat(KannaFifthAttract)) {
                    Option o1 = new Option();
                    o1.zOption = 0;
                    tsm.sendStat(KannaFifthAttract, o1);
                }
            }
        }
        super.handleInitAfterMigrate(chr);
    }
}
