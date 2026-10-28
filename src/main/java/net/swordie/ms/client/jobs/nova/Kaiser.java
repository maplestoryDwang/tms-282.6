package net.swordie.ms.client.jobs.nova;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.InvType.EQUIPPED;

public class Kaiser extends Job {

    public static final int VERTICAL_GRAPPLE = 60001218;
    public static final int TRANSFIGURATION = 60000219; //Morph Gauge (SmashStack)
    public static final int DRAGON_LINK = 60001225;

    public static final int TEMPEST_BLADES_THREE = 61101002;
    public static final int TEMPEST_BLADES_THREE_FF = 61110211;
    public static final int BLAZE_ON = 61101004; //Buff
    public static final int TEMPEST_BLADES_CHARGES = 61101009; //Buff

    public static final int SELF_RECOVERY = 61110006;

    public static final int FINAL_FORM_THIRD = 61111008; //Buff 3rd Job
    public static final int STONE_DRAGON = 61111002; //Summon (Speed Debuff)
    public static final int STONE_DRAGON_FINAL_FORM = 61111220; //Summon (Speed Debuff)
    public static final int CURSEBITE = 61111003; //Buff

    public static final int FINAL_FORM_FOURTH = 61120008; //Buff 4rd Job
    public static final int TEMPEST_BLADES_FIVE = 61120007;
    public static final int TEMPEST_BLADES_FIVE_FF = 61121217;
    public static final int GRAND_ARMOR = 61121009; //Buff
    public static final int NOVA_WARRIOR_KAISER = 61121014; //Buff
    public static final int NOVA_TEMPERANCE_KAISER = 61121015;
    public static final int NOVA_TEMPERANCE_KAISER_INV = 61121220;

    public static final int FINAL_TRANCE = 61121053;
    public static final int KAISERS_MAJESTY = 61121054;

    //Attacking Skills
    public static final int DRAGON_SLASH_1 = 61001000; //First Swing
    public static final int DRAGON_SLASH_2 = 61001004; //2nd Swing
    public static final int DRAGON_SLASH_3 = 61001005; //Last Swing`
    public static final int DRAGON_SLASH_1_FINAL_FORM = 61120219; //Swing Final Form

    public static final int FLAME_SURGE = 61001101;
    public static final int FLAME_SURGE_FINAL_FORM = 61111215;

    public static final int IMPACT_WAVE = 61101100;
    public static final int IMPACT_WAVE_FINAL_FORM = 61111216;
    public static final int PIERCING_BLAZE = 61101101; //Special Attack (Stun Debuff)
    public static final int PIERCING_BLAZE_FINAL_FORM = 61111217;

    public static final int WING_BEAT = 61111100; //Special Attack (Speed Debuff)
    public static final int WING_BEAT_FINAL_FORM = 61111111;
    public static final int PRESSURE_CHAIN = 61111101; //Special Attack (Stun Debuff)
    public static final int PRESSURE_CHAIN_FINAL_FORM = 61111219;

    public static final int GIGA_WAVE = 61121100; //Special Attack (Speed Debuff)
    public static final int GIGA_WAVE_FINAL_FORM = 61121201;
    public static final int INFERNO_BREATH = 61121105;
    public static final int INFERNO_BREATH_FINAL_FORM = 61121222;
    public static final int INFERNO_BREATH_BURN = 61120047;
    public static final int DRAGON_BARRAGE = 61121102;
    public static final int DRAGON_BARRAGE_FINAL_FORM = 61121203;
    public static final int BLADE_BURST = 61121104;
    public static final int BLADE_BURST_FINAL_FORM = 61121221;
    public static final int DRACONIC_AEGIS = 61121027;

    //Realign Skills
    public static final int REALIGN_ATTACKER_MODE = 60001217; //Unlimited Duration
    public static final int REALIGN_DEFENDER_MODE = 60001216; //Unlimited Duration

    public static final int REALIGN_ATTACKER_MODE_I = 61100008;
    public static final int REALIGN_DEFENDER_MODE_I = 61100005;

    public static final int REALIGN_ATTACKER_MODE_II = 61110010;
    public static final int REALIGN_DEFENDER_MODE_II = 61110005;

    public static final int REALIGN_ATTACKER_MODE_III = 61120013;
    public static final int REALIGN_DEFENDER_MODE_III = 61120010;

    // V skills
    public static final int NOVA_GUARDIANS = 400011012;
    public static final int NOVA_GUARDIANS_2 = 400011013;
    public static final int NOVA_GUARDIANS_3 = 400011014;

    public static final int BLADEFALL_ATTACK = 400011058;
    public static final int BLADEFALL_ATTACK_FF = 400011059;
    public static final int BLADEFALL_TILE = 400011060;
    public static final int BLADEFALL_TILE_FF = 400011061;

    public static final int DRACO_SURGE_ATTACK = 400011079;
    public static final int DRACO_SURGE_ATTACK_FF = 400011080;
    public static final int DRACO_SURGE_SHOOTOBJ = 400011081;
    public static final int DRACO_SURGE_SHOOTOBJ_FF = 400011082;

    public static final int DRAGON_BLAZE = 400011118;
    public static final int DRAGON_BLAZE_FIRE_ENERY = 400011119;
    public static final int DRAGON_BLAZE_FIRE_ORB = 400011120;
    public static final int DRAGON_BLAZE_FIRE_ENERGY_EXPLOSION = 400011130;

    // HEXA skills
    public static final int HEXA_GIGA_WAVE = 61141000; //Special Attack (Speed Debuff)
    public static final int HEXA_GIGA_WAVE_FINAL_FORM = 61141001;
    public static final int HEXA_BLADE_BURST = 61141002;
    public static final int HEXA_BLADE_BURST_FINAL_FORM = 61141004;
    public static final int HEXA_INFERNO_BREATH = 61141009;
    public static final int HEXA_INFERNO_BREATH_FINAL_FORM = 61141010;
    public static final int HEXA_ENHANCED_INFERNO_BREATH = 61141011;
    public static final int HEXA_ENHANCED_INFERNO_BREATH_FINAL_FORM = 61141012;
    public static final int HEXA_STONE_DRAGON = 61141013; //Summon (Speed Debuff)
    public static final int HEXA_STONE_DRAGON_FINAL_FORM = 61141014; //Summon (Speed Debuff)

    private final int[] addedSkills = new int[]{
            REALIGN_ATTACKER_MODE,
            REALIGN_DEFENDER_MODE,
            VERTICAL_GRAPPLE,
            TRANSFIGURATION,
            DRAGON_LINK,};

    private long lastSelfRecovery = 0L;
    private long lastBlazeFireOrb = 0L;
    private ScheduledFuture<?> dragonBlazeFireEnergyTimer;

    public Kaiser(Char chr) {
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

    public static int getTempBladeSkill(Char chr, TemporaryStatManager tsm) {
        int skill = 0;
        if (chr.hasSkill(TEMPEST_BLADES_THREE)) {
            skill = TEMPEST_BLADES_THREE;
        }
        if (chr.hasSkill(TEMPEST_BLADES_THREE) && tsm.hasStat(Morph)) {
            skill = TEMPEST_BLADES_THREE_FF;
        }
        if (chr.hasSkill(TEMPEST_BLADES_FIVE)) {
            skill = TEMPEST_BLADES_FIVE;
        }
        if (chr.hasSkill(TEMPEST_BLADES_FIVE) && tsm.hasStat(Morph)) {
            skill = TEMPEST_BLADES_FIVE_FF;
        }
        return skill;
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isKaiser(id);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        chr.getAvatarData().getCharacterStat().setPosMap(JobConstants.KAISER_CREATION_MAP);
        cs.setLevel(10);
        cs.setJob(6100);
        cs.setStr(49);
        cs.setMp(50);
        cs.setMaxMp(50);
        chr.setSpToCurrentJob(5);
    }

    public void giveRealignAttackBuffs() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        int[] realignattacks = new int[]{
                REALIGN_ATTACKER_MODE,
                REALIGN_ATTACKER_MODE_I,
                REALIGN_ATTACKER_MODE_II,
                REALIGN_ATTACKER_MODE_III,};
        int zPadX = 0;
        int zCr = 0;
        int zBdR = 0;
        for (int realignattack : realignattacks) {
            if (chr.hasSkill(realignattack)) {
                Skill skill = chr.getSkill(realignattack);
                int slv = skill.getCurrentLevel();
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                zPadX += si.getValue(padX, slv);
                zCr += si.getValue(cr, slv);
                zBdR += si.getValue(bdR, slv);
            }
        }
        o1.nOption = zPadX;
        o1.rOption = REALIGN_ATTACKER_MODE;
        newStats.put(PAD, o1);
        o2.nOption = zCr;
        o2.rOption = REALIGN_ATTACKER_MODE;
        newStats.put(CriticalBuff, o2);
        o3.nOption = zBdR;
        o3.rOption = REALIGN_ATTACKER_MODE;
        newStats.put(BossDamageRate, o3);
        tsm.sendStat(newStats);
    }

    public void giveRealignDefendBuffs() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        int[] realigndefends = new int[]{
                REALIGN_DEFENDER_MODE,
                REALIGN_DEFENDER_MODE_I,
                REALIGN_DEFENDER_MODE_II,
                REALIGN_DEFENDER_MODE_III,};
        int zDef = 0;
        int zAcc = 0;
        int zMHPR = 0;
        for (int realigndefend : realigndefends) {
            if (chr.hasSkill(realigndefend)) {
                Skill skill = chr.getSkill(realigndefend);
                int slv = skill.getCurrentLevel();
                SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
                zDef += si.getValue(pddX, slv);
                zAcc += si.getValue(accX, slv);
                zMHPR += si.getValue(mhpR, slv);
            }
        }
        o1.nOption = zDef;
        o1.rOption = REALIGN_DEFENDER_MODE;
        newStats.put(PDD, o1);
        o2.nOption = zAcc;
        o2.rOption = REALIGN_DEFENDER_MODE;
        newStats.put(ACC, o2);
        o3.nOption = zMHPR;
        o3.rOption = REALIGN_DEFENDER_MODE;
        newStats.put(SelfHyperBodyMaxHP, o3);
        tsm.sendStat(newStats);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        switch (skillID) {
            case PIERCING_BLAZE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv)) && !mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case PIERCING_BLAZE_FINAL_FORM:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case WING_BEAT:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = 5;
                        mts.addStatOptions(mob, MobStat.Speed, o1);
                    }
                }
                break;
            case WING_BEAT_FINAL_FORM:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = 5;
                    mts.addStatOptions(mob, MobStat.Speed, o1);
                }
                break;
            case PRESSURE_CHAIN:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv)) && !mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = 3;
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case PRESSURE_CHAIN_FINAL_FORM:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (!mob.isBoss()) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = 3;
                        mts.addStatOptions(mob, MobStat.Stun, o1);
                    }
                }
                break;
            case DRACO_SURGE_ATTACK_FF:
            case GIGA_WAVE:
            case HEXA_GIGA_WAVE:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = -30;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Speed, o1);
                    }
                }
                break;
            case GIGA_WAVE_FINAL_FORM:
                if (!mts.hasCurrentMobStatBySkillId(GIGA_WAVE)) {
                    o1.nOption = -30;
                    o1.rOption = GIGA_WAVE;
                    o1.tOption = 4;
                    mts.addStatOptions(mob, MobStat.Speed, o1);
                }
                break;
            case HEXA_GIGA_WAVE_FINAL_FORM:
                if (!mts.hasCurrentMobStatBySkillId(HEXA_GIGA_WAVE)) {
                    o1.nOption = -30;
                    o1.rOption = HEXA_GIGA_WAVE;
                    o1.tOption = 4;
                    mts.addStatOptions(mob, MobStat.Speed, o1);
                }
                break;
            case STONE_DRAGON_FINAL_FORM:
            case STONE_DRAGON:
            case HEXA_STONE_DRAGON:
            case HEXA_STONE_DRAGON_FINAL_FORM:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nOption = 1;
                        o1.rOption = skillID;
                        o1.tOption = si.getValue(time, slv);
                        mts.addStatOptions(mob, MobStat.Speed, o1);
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
        if (chr.hasSkill(SELF_RECOVERY)) {
            SkillInfo siSF = SkillData.getSkillInfoById(SELF_RECOVERY);
            int slvSF = chr.getSkillLevel(SELF_RECOVERY);
            if (now - lastSelfRecovery >= siSF.getValue(y, slvSF)) {
                chr.heal((int) (siSF.getValue(x, slvSF) * chr.getMaxHP() / 100.0D));
                chr.healMP((int) (siSF.getValue(x, slvSF) * chr.getMaxMP() / 100.0D));
                lastSelfRecovery = now;
            }
        }
        if (hasHitMobs) {
            int kaiserGaugeIncrementBySkill = SkillConstants.getKaiserGaugeIncrementBySkill(attackInfo.skillId);
            int inc = attackInfo.mobCount == 1 ? attackInfo.hits : attackInfo.mobCount;
            incrementMorphGauge(kaiserGaugeIncrementBySkill * inc);

            if (canProcDragonBlazeFireEnergyExplosion(attackInfo)) {
                doDragonBlazeFireEnergyExplosion();
            }
            if (canProcDragonBlazeFireOrb()) {
                if (skillID != DRAGON_BLAZE_FIRE_ORB) {
                    doDragonBlazeFireOrb(now);
                }
            }
        }
        switch (attackInfo.skillId) {
            case DRACO_SURGE_ATTACK_FF:
                slv = chr.getSkillLevel(DRACO_SURGE_ATTACK);
                chr.setSkillCooldown(DRACO_SURGE_ATTACK, slv);
            case INFERNO_BREATH:
            case INFERNO_BREATH_FINAL_FORM: {
                SkillInfo rca = SkillData.getSkillInfoById(INFERNO_BREATH);
                if (attackInfo.positions == null) {
                    break;
                }
                for (Position position : attackInfo.positions) {
                    AffectedArea aa = AffectedArea.getAffectedArea(chr, attackInfo);
                    aa.setDuration((rca.getValue(cooltime, slv) + (chr.hasSkill(INFERNO_BREATH_BURN) ? 10 : 0)) * 1000);
                    aa.setMobOrigin((byte) 0);
                    aa.setSkillID(INFERNO_BREATH);
                    aa.setPosition(position);
                    Rect rect = aa.getPosition().getRectAround(rca.getFirstRect());
                    if (!attackInfo.left) {
                        rect = rect.horizontalFlipAround(chr.getPosition().getX());
                    }
                    aa.setRect(rect);
                    aa.setDelay((short) 7); //spawn delay
                    chr.getField().spawnAffectedArea(aa);
                }
                break;
            }
            case HEXA_INFERNO_BREATH:
            case HEXA_INFERNO_BREATH_FINAL_FORM: {
                Option o1 = tsm.getOption(EnhanceInfernalBreath);
                int val = o1.nOption + 1;
                if (val >= 2) {
                    val = 0;
                    chr.write(UserLocal.userBonusAttackRequest(skillID + 2));
                }
                o1.nOption = val;
                o1.rOption = skillID;
                tsm.sendStat(EnhanceInfernalBreath, o1);
                break;
            }
        }
    }

    private void incrementMorphGauge(int increment) {
        SkillInfo gaugeInfo = SkillData.getSkillInfoById(TRANSFIGURATION);
        if (chr.hasSkill(TRANSFIGURATION)) {
            TemporaryStatManager tsm = chr.getTemporaryStatManager();
            int maxGauge = getKaiserGauge(chr);
            int amount = 1;
            int stage = 0;
            amount = tsm.getOption(SmashStack).nOption;
            if (amount <= maxGauge) {
                if (amount + increment > maxGauge) {
                    amount = getKaiserGauge(chr);
                } else {
                    amount = tsm.getOption(SmashStack).nOption + increment;
                }
            }
            if (amount >= gaugeInfo.getValue(s, 1)) {
                stage = 1;
            }
            if (amount >= (gaugeInfo.getValue(v, 1)) - 1) {
                stage = 2;
            }

            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);

            Option o = new Option();
            o.nOption = amount;
            newStats.put(SmashStack, o);

            Option o1 = new Option();
            o1.nOption = (stage * gaugeInfo.getValue(prop, 1));
            newStats.put(Stance, o1);

            Option o2 = new Option();
            o2.nOption = (stage * gaugeInfo.getValue(psdJump, 1));
            newStats.put(Jump, o2);

            Option o3 = new Option();
            o3.nOption = (stage * gaugeInfo.getValue(psdSpeed, 1));
            newStats.put(Speed, o3);

            Option o4 = new Option();
            o4.nValue = (stage * gaugeInfo.getValue(actionSpeed, 1));
            newStats.put(IndieBooster, o4); //Indie

            if (tsm.hasStatBySkillId(Job.GRANDIS_GODDESS_BLESSING_KAISER)) {
                Option o5 = new Option();
                SkillInfo si = SkillData.getSkillInfoById(Job.GRANDIS_GODDESS_BLESSING);
                int slv = chr.getSkillLevel(Job.GRANDIS_GODDESS_BLESSING);
                o5.nValue = (stage * si.getValue(q, slv));
                newStats.put(IndieDamR, o5);
                chr.write(UserPacket.effect(Effect.skillUse(Job.GRANDIS_GODDESS_BLESSING_KAISER, chr.getLevel(), slv)));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), Effect.skillUse(Job.GRANDIS_GODDESS_BLESSING_KAISER, chr.getLevel(), slv)), chr);
            }

            tsm.sendStat(newStats);
        }
    }

    private void resetGauge(TemporaryStatManager tsm) {
        tsm.removeStat(SmashStack);
    }

    private int getKaiserGauge(Char chr) {
        int maxGauge;
        switch (chr.getJob()) {
            case 6100:
                maxGauge = SkillData.getSkillInfoById(60000219).getValue(s, 1);
                break;
            case 6110:
                maxGauge = SkillData.getSkillInfoById(60000219).getValue(u, 1);
                break;
            case 6111:
            case 6112:
                maxGauge = SkillData.getSkillInfoById(60000219).getValue(v, 1);
                break;
            default:
                maxGauge = 0;
        }
        return maxGauge;
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
        Option o4 = new Option();
        Option o5 = new Option();
        Option o6 = new Option();
        Summon summon;
        Field field;
        switch (skillID) {
            case REALIGN_ATTACKER_MODE:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    tsm.removeStatsBySkill(REALIGN_DEFENDER_MODE);
                    giveRealignAttackBuffs();
                }
                break;
            case REALIGN_DEFENDER_MODE:
                if (tsm.hasStatBySkillId(skillID)) {
                    tsm.removeStatsBySkill(skillID);
                } else {
                    tsm.removeStatsBySkill(REALIGN_ATTACKER_MODE);
                    giveRealignDefendBuffs();
                }
                break;
            case BLAZE_ON:
                o1.nOption = si.getValue(x, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(Booster, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePad, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o2);
                tsm.sendStat(newStats);
                break;
            case CURSEBITE:
                o1.nOption = si.getValue(asrR, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(AsrR, o1);
                o2.nOption = si.getValue(terR, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(TerR, o2);
                tsm.sendStat(newStats);
                break;
            case GRAND_ARMOR:
                // w = party dmg taken  v = self dmg taken
                o1.nOption = si.getValue(v, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(DamageReduce, o1);
                break;
            case TEMPEST_BLADES_THREE:
            {
                Item item = chr.getEquippedItemByBodyPart(BodyPart.Weapon);
                StopForceAtom stopForceAtom = new StopForceAtom();
                int weaponID = item.getItemId();
                if (tsm.getOption(StopForceAtomInfo).nOption != 1 && tsm.hasStat(StopForceAtomInfo)) {
                    tsm.removeStat(StopForceAtomInfo);
                }
                o1.nOption = 1;
                o1.rOption = skillID;
                List<Integer> angles = Arrays.asList(0, 0, 0);
                stopForceAtom.setCount(3);
                stopForceAtom.setIdx(1);
                stopForceAtom.setWeaponId(weaponID);
                stopForceAtom.setAngleInfo(angles);
                tsm.setStopForceAtom(stopForceAtom);
                tsm.sendStat(StopForceAtomInfo, o1);
                break;
            }
            case TEMPEST_BLADES_THREE_FF: //Final Form
            {
                Item item = chr.getEquippedItemByBodyPart(BodyPart.Weapon);
                StopForceAtom stopForceAtom = new StopForceAtom();
                int weaponID = item.getItemId();
                if (tsm.getOption(StopForceAtomInfo).nOption != 3 && tsm.hasStat(StopForceAtomInfo)) {
                    tsm.removeStat(StopForceAtomInfo);
                }
                o1.nOption = 3;
                o1.rOption = skillID;
                List<Integer> angles = Arrays.asList(0, 0, 0);
                stopForceAtom.setCount(3);
                stopForceAtom.setIdx(3);
                stopForceAtom.setWeaponId(weaponID);
                stopForceAtom.setAngleInfo(angles);
                tsm.setStopForceAtom(stopForceAtom);
                tsm.sendStat(StopForceAtomInfo, o1);
                break;
            }
            case TEMPEST_BLADES_FIVE: {
                Item item = chr.getEquippedItemByBodyPart(BodyPart.Weapon);
                StopForceAtom stopForceAtom = new StopForceAtom();
                int weaponID = item.getItemId();
                if (tsm.hasStat(StopForceAtomInfo) && tsm.getOption(StopForceAtomInfo).nOption != 2) {
                    tsm.removeStat(StopForceAtomInfo);
                }
                o1.nOption = 2;
                o1.rOption = skillID;
                List<Integer> angles = Arrays.asList(0, 0, 0, 0, 0);
                stopForceAtom.setCount(5);
                stopForceAtom.setIdx(2);
                stopForceAtom.setWeaponId(weaponID);
                stopForceAtom.setAngleInfo(angles);
                tsm.setStopForceAtom(stopForceAtom);
                tsm.sendStat(StopForceAtomInfo, o1);
                break;
            }
            case TEMPEST_BLADES_FIVE_FF: //Final Form
            {
                Item item = chr.getEquippedItemByBodyPart(BodyPart.Weapon);
                StopForceAtom stopForceAtom = new StopForceAtom();
                int weaponID = item.getItemId();
                if (tsm.getOption(StopForceAtomInfo).nOption != 4 && tsm.hasStat(StopForceAtomInfo)) {
                    tsm.removeStat(StopForceAtomInfo);
                }
                o1.nOption = 4;
                o1.rOption = skillID;
                List<Integer> angles = Arrays.asList(0, 0, 0, 0, 0);
                stopForceAtom.setCount(5);
                stopForceAtom.setIdx(4);
                stopForceAtom.setWeaponId(weaponID);
                stopForceAtom.setAngleInfo(angles);
                tsm.setStopForceAtom(stopForceAtom);
                tsm.sendStat(StopForceAtomInfo, o1);
                break;
            }
            case FINAL_FORM_THIRD:
                if (tsm.hasStat(StopForceAtomInfo)) {
                    tsm.removeStat(StopForceAtomInfo);
                }
                if (tsm.hasStat(Morph)) {
                    tsm.removeStat(Morph);
                }
                o6.nOption = 1200;
                o6.rOption = skillID;
                o6.tOption = si.getValue(time, slv);
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                    o6.tOption *= 1000; // Retain the legacy 265 packet duration.
                }
                newStats.put(Morph, o6);
                o1.nOption = si.getValue(cr, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(CriticalBuff, o1);
                o2.nOption = si.getValue(jump, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Jump, o2);
                o3.nOption = si.getValue(prop, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(Stance, o3);
                o4.nOption = si.getValue(speed, slv);
                o4.rOption = skillID;
                o4.tOption = si.getValue(time, slv);
                newStats.put(Speed, o4);
                o5.nReason = skillID;
                o5.nValue = si.getValue(indiePMdR, slv);
                o5.tTerm = si.getValue(time, slv);
                newStats.put(IndiePMdR, o5);
                tsm.sendStat(newStats);
                resetGauge(tsm);
                break;
            case FINAL_TRANCE:
            case FINAL_FORM_FOURTH:
                if (tsm.hasStat(StopForceAtomInfo)) {
                    tsm.removeStat(StopForceAtomInfo);
                }
                if (tsm.hasStat(Morph)) {
                    tsm.removeStat(Morph);
                }
                o6.nOption = 1201;
                o6.rOption = skillID;
                o6.tOption = si.getValue(time, slv);
                newStats.put(Morph, o6);
                o1.nOption = si.getValue(cr, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(CriticalBuff, o1);
                o2.nOption = si.getValue(jump, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(Jump, o2);
                o3.nOption = si.getValue(prop, slv);
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv);
                newStats.put(Stance, o3);
                o4.nOption = si.getValue(speed, slv);
                o4.rOption = skillID;
                o4.tOption = si.getValue(time, slv);
                newStats.put(Speed, o4);
                o5.nReason = skillID;
                o5.nValue = si.getValue(indiePMdR, slv);
                o5.tTerm = si.getValue(time, slv);
                newStats.put(IndiePMdR, o5);
                tsm.sendStat(newStats);
                resetGauge(tsm);
                break;
            case KAISERS_MAJESTY:
                o1.nReason = skillID;
                o1.nValue = -1;
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieBooster, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indiePad, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePAD, o2);
                tsm.sendStat(newStats);
                for (int skillId : chr.getSkillCoolTimes().keySet()) {
                    si = SkillData.getSkillInfoById(skillId);
                    if (si != null && si.getHyper() == 0) {
                        chr.resetSkillCoolTime(skillId);
                    }
                }
                break;
            case STONE_DRAGON:
            case STONE_DRAGON_FINAL_FORM:
            case HEXA_STONE_DRAGON:
            case HEXA_STONE_DRAGON_FINAL_FORM:
                Position position = new Position(chr.isLeft() ? chr.getPosition().getX() - 250 : chr.getPosition().getX() + 250, chr.getPosition().getY());
                if (chr.getField().findFootHoldBelow(position) != null) {
                    summon = Summon.getSummonByAndSetStat(c.getChr(), skillID, slv);
                    field = c.getChr().getField();
                    summon.setFlyMob(false);
                    summon.setMoveAction((byte) 0);
                    summon.setMoveAbility(MoveAbility.Stop);
                    summon.setCurFoothold((short) (int) java.util.Optional.ofNullable(chr.getField().findFootHoldBelow(position)).map(below -> below.getId()).orElse(0) /* in the air: no foothold (was an NPE) */);
                    summon.setPosition(position);
                    field.spawnSummon(summon);
                } else {
                    chr.chatMessage("請換個位置再使用這個技能。");
                }
                break;
            case DRACONIC_AEGIS:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = 3;
                tsm.sendStat(NotDamaged, o1);
                break;
            case NOVA_GUARDIANS:
                field = c.getChr().getField();
                for (int sid = 400011012; sid <= 400011014; sid++) {
                    summon = Summon.getSummonByAndSetStat(c.getChr(), sid, slv);
                    summon.setFlyMob(true);
                    summon.setMoveAbility(MoveAbility.WalkRandom);
                    summon.setAssistType(AssistType.TeleportToMobs);
                    summon.setAttackActive(true);
                    field.spawnSummon(summon);
                }
                break;
            case NOVA_TEMPERANCE_KAISER:
                tsm.removeAllDebuffs();
                break;
            case DRAGON_BLAZE:
                var tOpt = si.getValue(time, slv);
                o1.nOption = slv;
                o1.rOption = skillID;
                o1.tOption = tOpt;
                tsm.sendStat(DevilishPower, o1);
                if (dragonBlazeFireEnergyTimer != null) {
                    dragonBlazeFireEnergyTimer.cancel(false);
                }
                var interval = 250;
                var executes = (tOpt * 1000) / interval;
                dragonBlazeFireEnergyTimer = chr.getTimer().addFixedRateEvent(this::doDragonBlazeFireEnergy, interval, interval, executes);
                break;
        }
    }

    private boolean canProcDragonBlazeFireOrb() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return chr.hasSkill(DRAGON_BLAZE) // Must have skill
                && chr.hasSkillOnCooldown(DRAGON_BLAZE) // Dragon Blaze must be on cooldown
                && tsm.getOptByCTSAndSkill(DevilishPower, DRAGON_BLAZE) == null; // Does not currently have the Dragon Blaze Buff
    }

    private boolean canProcDragonBlazeFireEnergyExplosion(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return attackInfo.skillId != DRAGON_BLAZE && attackInfo.skillId != DRAGON_BLAZE_FIRE_ENERGY_EXPLOSION
                && tsm.getOptByCTSAndSkill(DevilishPower, DRAGON_BLAZE) != null
                && !chr.hasSkillOnCooldown(DRAGON_BLAZE_FIRE_ENERGY_EXPLOSION);
    }

    private void doDragonBlazeFireEnergyExplosion() {
        chr.write(UserLocal.userBonusAttackRequest(DRAGON_BLAZE_FIRE_ENERGY_EXPLOSION));
        chr.addSkillCooldown(DRAGON_BLAZE_FIRE_ENERGY_EXPLOSION, chr.getSkillStatValue(t, DRAGON_BLAZE) * 1000);
    }

    private void doDragonBlazeFireEnergy() {
        var skillId = DRAGON_BLAZE;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(skillId) || tsm.getOptByCTSAndSkill(DevilishPower, DRAGON_BLAZE) == null) {
            if (dragonBlazeFireEnergyTimer != null) {
                dragonBlazeFireEnergyTimer.cancel(false);
            }
            return;
        }
        // TODO
    }

    private void doDragonBlazeFireOrb(long now) {
        SkillInfo si = SkillData.getSkillInfoById(DRAGON_BLAZE_FIRE_ORB);
        var slv = chr.getSkillLevel(DRAGON_BLAZE);
        if (si == null || slv <= 0) {
            return;
        }
        if (now - lastBlazeFireOrb >= 10000) {
            int mobCount = si.getValue(SkillStat.mobCount, slv);
            List<SecondAtom> secondAtoms = new LinkedList<>();
            var rect = chr.getRectAround(new Rect(-800, -800, 800, 800));
            if (!chr.isLeft()) {
                rect = rect.horizontalFlipAround(chr.getPosition().getX());
            }
            int bossID = 0;
            List<Integer> mobs = new ArrayList<>(mobCount);
            for (Mob mob : chr.getField().getMobsInRect(rect)) {
                if (mob.isBoss()) {
                    bossID = mob.getObjectId();
                }
                if (mobs.size() < mobCount) {
                    mobs.add(mob.getObjectId());
                }
            }
            var sai = si.getSecondAtomInfos().get(0);
            final var pos = chr.getPosition();
            int key = 0;
            int shots = (bossID != 0) ? mobCount : Math.min(mobCount, mobs.size());
            for (int i = 0; i < shots; i++) {
                int mobOID = (bossID != 0) ? bossID : mobs.get(i);
                SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mobOID, key,
                        si.getSkillId(), pos, now);
                secondAtoms.add(fa);
                key++;
            }
            chr.createSecondAtom(secondAtoms);
            lastBlazeFireOrb = now;
        }
    }

    public void recallNovaGuardians() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        List<Integer> skillList = Arrays.asList(NOVA_GUARDIANS, NOVA_GUARDIANS_2, NOVA_GUARDIANS_3);
        Field field = chr.getField();

        for (int skillId : skillList) {
            Summon summon = Summon.getSummonByAndSetStat(c.getChr(), skillId, chr.getSkillLevel(NOVA_GUARDIANS));
            summon.setFlyMob(false);
            summon.setMoveAbility(MoveAbility.FixVMove);
            summon.setAssistType(AssistType.TeleportToMobs);
            int random = new Random().nextInt(500) - 250;
            Position position2 = new Position(chr.getPosition().getX() + random, chr.getPosition().getY());
            summon.setCurFoothold((short) (int) java.util.Optional.ofNullable(chr.getField().findFootHoldBelow(position2)).map(below -> below.getId()).orElse(0) /* in the air: no foothold (was an NPE) */);
            summon.setPosition(position2);
            summon.setSummonTerm((int) ((tsm.getRemainingTime(IndieEmpty, NOVA_GUARDIANS)) / 1000));
            field.spawnSummon(summon);
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
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
            sm.completeQuestNoRewards(level == 60 ? 25711 : 25712);
            sm.giveAndEquip(level == 60 ? 1352502 : 1352503);
            sm.giveAndEquip(level == 60 ? 1142486 : 1142487);
            sm.addSPJobAdv(jobID, 5);
            sm.addSPJobAdv(next, 3);
        }
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        if (chr.getLevel() < 30) {
            ScriptManagerImpl sm = chr.getScriptManager();
            sm.setJob(JobConstants.JobEnum.KAISER2.getJobId());
            sm.levelUntil(30);
            for (int qid = 25720; qid <= 25761; qid++) {
                sm.completeQuestNoRewards(qid);
            }
            sm.addSPJobAdv(JobConstants.JobEnum.KAISER1.getJobId(), 5);
            sm.addSPJobAdv(JobConstants.JobEnum.KAISER2.getJobId(), 3);
            boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
            sm.giveAndEquip(tms282 ? 1402002 : 1213001);
            sm.giveAndEquip(tms282 ? 1352501 : 1354001);
            sm.warp(FieldConstants.HOME_MAP);
        }
        super.handleInitAfterMigrate(chr);
    }

    public void createFlyingSwordForceAtom(InPacket inPacket) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int stopForceAtomNOption = tsm.getOption(StopForceAtomInfo).nOption;
        int skillId = inPacket.decodeInt();
        int targetCount = inPacket.decodeInt();

        int maxCount = 3;
        if (stopForceAtomNOption == 2 || stopForceAtomNOption == 4) {
            maxCount = 5;
        }
        List<ForceAtomInfo> faiList = new ArrayList<>();
        List<Integer> targetList = new ArrayList<>();

        ForceAtomEnum fae = skillId < BLADEFALL_ATTACK ? ForceAtomEnum.KAISER_WEAPON_THROW_1 : ForceAtomEnum.KAISER_V_WEAPON_THROW_1;
        int atomSkillId = TEMPEST_BLADES_THREE;

        switch (stopForceAtomNOption) {
            case 3:
                fae = skillId < BLADEFALL_ATTACK ? ForceAtomEnum.KAISER_WEAPON_THROW_MORPH_1 : ForceAtomEnum.KAISER_V_WEAPON_THROW_MORPH_1;
                atomSkillId = TEMPEST_BLADES_THREE_FF;
                break;
            case 2:
                fae = skillId < BLADEFALL_ATTACK ? ForceAtomEnum.KAISER_WEAPON_THROW_2 : ForceAtomEnum.KAISER_V_WEAPON_THROW_2;
                atomSkillId = TEMPEST_BLADES_FIVE;
                break;
            case 4:
                fae = skillId < BLADEFALL_ATTACK ? ForceAtomEnum.KAISER_WEAPON_THROW_MORPH_2 : ForceAtomEnum.KAISER_V_WEAPON_THROW_MORPH_2;
                atomSkillId = TEMPEST_BLADES_FIVE_FF;
                break;
        }

        for (int i = 0; i < targetCount; i++) {
            targetList.add(inPacket.decodeInt());
            int firstImpact = new Random().nextInt(5) + 20;
            int secondImpact = new Random().nextInt(5) + 25;
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), firstImpact, secondImpact, 0, 600, Util.getCurrentTime(), 0, 0, new Position());
            if (skillId >= BLADEFALL_ATTACK) {
                fai.setDisappearDelay(2000);
            }
            faiList.add(fai);
        }
        for (int i = targetCount; i < maxCount; i++) {
            targetList.add(Util.getRandomFromCollection(targetList));
            int firstImpact = new Random().nextInt(5) + 20;
            int secondImpact = new Random().nextInt(5) + 25;
            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), fae.getInc(), firstImpact, secondImpact, 0, 600, Util.getCurrentTime(), 0, 0, new Position());
            if (skillId >= BLADEFALL_ATTACK) {
                fai.setDisappearDelay(2000);
            }
            faiList.add(fai);
        }

        chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                true, targetList, skillId < BLADEFALL_ATTACK ? atomSkillId : skillId, faiList, new Rect(), 0, 300,
                new Position(), skillId < BLADEFALL_ATTACK ? atomSkillId : skillId, new Position(), 0));
        tsm.removeStat(StopForceAtomInfo);

        if (skillId == BLADEFALL_ATTACK || skillId == BLADEFALL_ATTACK_FF) {
            chr.setSkillCooldown(BLADEFALL_ATTACK, chr.getSkillLevel(BLADEFALL_ATTACK));
        }
        chr.setSkillCooldown(TEMPEST_BLADES_CHARGES, chr.getSkillLevel(TEMPEST_BLADES_CHARGES));
    }

    @Override
    public void handleForceAtomCollision(int faKey, int skillId, int mobObjId, Position position, InPacket inPacket) {
        if (skillId == 0 && inPacket.getUnreadAmount() > 0) {
            createBladeFallTiles(inPacket);
        }

        super.handleForceAtomCollision(faKey, skillId, mobObjId, position, inPacket);
    }

    private void createBladeFallTiles(InPacket inPacket) {
        int skillId = inPacket.decodeInt();
        if (skillId == BLADEFALL_ATTACK || skillId == BLADEFALL_ATTACK_FF) {
            Position position = inPacket.decodePositionInt();
            int option = inPacket.decodeInt();

            SkillInfo si = SkillData.getSkillInfoById(BLADEFALL_ATTACK);
            int slv = chr.getSkillLevel(BLADEFALL_ATTACK);

            AffectedArea aa = AffectedArea.getPassiveAA(chr, skillId + 2, slv);
            aa.setPosition(position);
            aa.setRect(aa.getPosition().getRectAround(si.getFirstRect()));
            aa.setDuration(1000);
            aa.setOption(option);
            chr.getField().spawnAffectedArea(aa);
        }
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case TEMPEST_BLADES_THREE_FF -> { // Tempest Blades
                chr.setSkillCooldown(TEMPEST_BLADES_THREE, chr.getSkillLevel(TEMPEST_BLADES_THREE));
                return 1;
            }
            case TEMPEST_BLADES_FIVE_FF -> { // Advanced Tempest Blades
                chr.setSkillCooldown(TEMPEST_BLADES_THREE, chr.getSkillLevel(TEMPEST_BLADES_FIVE));
                return 1;
            }
            case DRAGON_BARRAGE_FINAL_FORM -> { // Dragon Barrage
                int skillID = DRAGON_BARRAGE;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case NOVA_TEMPERANCE_KAISER_INV -> { // Nova Temperance
                int skillID = NOVA_TEMPERANCE_KAISER;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case INFERNO_BREATH,
                 INFERNO_BREATH_FINAL_FORM,
                 HEXA_INFERNO_BREATH,
                 HEXA_INFERNO_BREATH_FINAL_FORM,
                 HEXA_ENHANCED_INFERNO_BREATH,
                 HEXA_ENHANCED_INFERNO_BREATH_FINAL_FORM -> {
                int skillID = INFERNO_BREATH;
                if (!chr.hasSkillOnCooldown(skillID)) {
                    int slv = chr.getSkillLevel(skillID);
                    chr.setSkillCooldown(skillID, slv);
                }
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
