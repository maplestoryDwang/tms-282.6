package net.swordie.ms.client.jobs.resistance;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.MobPool;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.handlers.GlobalTimerManager;
import net.swordie.ms.life.AffectedArea;
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
import java.util.concurrent.ScheduledFuture;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;

public class Xenon extends Job {

    public static final int SUPPLY_SURPLUS = 30020232;
    public static final int MULTILATERAL_I = 30020234;
    public static final int MODAL_SHIFT = 30021236;
    public static final int LIBERTY_BOOSTERS = 30021237;
    public static final int MIMIC_PROTOCOL = 30020240;
    public static final int PROMESSA_ESCAPE = 30021235;

    public static final int CIRCUIT_SURGE = 36001002; //Buff
    public static final int PINPOINT_SALVO = 36001005; //Special Attack

    public static final int XENON_BOOSTER = 36101004; //Buff
    public static final int EFFICIENCY_STREAMLINE = 36101003; //Buff
    public static final int ION_THRUST = 36101001; //Special Attack
    public static final int PINPOINT_SALVO_REDESIGN_A = 36100010; //Special Attack Upgrade  (Passive Upgrade)

    public static final int HYBRID_DEFENSES = 36111003; //Buff
    public static final int MANIFEST_PROJECTOR = 36111006; //Special Buff (Special Duration)
    public static final int EMERGENCY_RESUPPLY = 36111008; //Special Skill
    public static final int PINPOINT_SALVO_REDESIGN_B = 36110012; //Special Attack Upgrade  (Passive Upgrade)
    public static final int TRIANGULATION = 36110005;

    public static final int HYPOGRAM_FIELD_FORCE_FIELD = 36121002;                  //TODO Summon
    public static final int HYPOGRAM_FIELD_PENETRATE = 36121013;
    public static final int HYPOGRAM_FIELD_SUPPORT = 36121014;                      //TODO Summon
    public static final int HYPOGRAM_FIELD_PERSIST = 36120051; // hyper passive
    public static final int TEMPORAL_POD = 36121007;
    public static final int OOPARTS_CODE = 36121003; //Buff
    public static final int MAPLE_WARRIOR_XENON = 36121008; //Buff
    public static final int PINPOINT_SALVO_PERFECT_DESIGN = 36120015; //Sp. Attack Upgrade  (Passive Upgrade)
    public static final int HEROS_WILL_XENON = 36121009;

    public static final int ORBITAL_CATACLYSM = 36121052;
    public static final int ORBITAL_CATACLYSM_BUFF = 36121055;
    public static final int AMARANTH_GENERATOR = 36121054;
    public static final int ENTANGLISH_LASH = 36121053;

    // V Skills
    public static final int OMEGA_BLASTER = 400041007;
    public static final int CORE_OVERLOAD_BUFF = 400041029;
    public static final int CORE_OVERLOAD_ATTACK = 400041031;
    public static final int HYPOGRAM_FIELD_FUSION = 400041044;
    public static final int PHOTON_RAY = 400041057;
    public static final int PHOTON_RAY_ATTACK = 400041058;

    // HEXA Skills
    public static final int ARTIFICIAL_EVOLUTION = 36141500;
    public static final int ENHANCED_LIBERTY_BOOSTERS = 36141502;
    public static final int ARTIFICIAL_EVOLUTION_BUFF = 36141503;
    public static final int HEXA_ORBITAL_CATACLYSM = 36141012;
    public static final int HEXA_ORBITAL_CATACLYSM_BUFF = 36141013;
    public static final int HEXA_TRIANGULATION = 36141007;
    public static final int HEXA_TRIANGULATION_ATT = 36141008;
    public static final int HEXA_MECHA_PURGE_EXECUTE = 36141001;
    public static final int HEXA_MECHA_PURGE_FIRE = 36141003;
    public static final int HEXA_BEAM_DANCE = 36141014;
    public static final int HEXA_BEAM_DANCE_EXTRA = 36140015;
    public static final int HEXA_BEAM_DANCE_SUMMON = 36140016;
    public static final int HEXA_HYPOGRAM_FIELD_PENETRATE = 36141004;
    public static final int HEXA_HYPOGRAM_FIELD_FORCE_FIELD = 36141005;
    public static final int HEXA_HYPOGRAM_FIELD_SUPPORT = 36141006;

    // HEXA Boosts
    public static final int CORE_OVERLOAD_BOOST = 500004125;
    public static final int CORE_OVERLOAD_BOOST_SKILL = 500061059;

    private static ScheduledFuture<?> temporalPodTimer;
    private int incSupply;
    private final Tms282XenonSupplyTimer supplyTimer = new Tms282XenonSupplyTimer(this, chr);

    public Tms282XenonSupplyTimer supplyTimer() { return supplyTimer; }

    /** Called on every field assignment, including a cached-field round trip. Energy itself is retained. */
    public void onTms282FieldChange() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) supplyTimer.clear();
    }
    private int supplyProp = 0;
    private int hybridDefenseCount;
    private long pinPointDelay = 0;
    private long nextSixthBeamDance = 0;
    final Tms282OmegaBlaster omegaBlaster = new Tms282OmegaBlaster();

    /** Current282 anotherModeInfo groups; each selected skill retains its own ordinary/HEXA parent key. */
    private static final int[][] TMS282_SKILL_MODES = {
            {36101000,36101008,36101009}, {36111000,36111009,36111010},
            {36121001,36121011,36121012}, {36121013,36121014},
            {36141000,36141002}, {36141005,36141006}
    };

    public net.swordie.ms.client.character.quest.Quest selectTms282SkillMode(int selectedSkill) {
        int parent = 0;
        for (int[] group : TMS282_SKILL_MODES) {
            for (int id : group) if (id == selectedSkill) parent = group[0];
        }
        if (!JobConstants.isXenon(chr.getJob()) || parent == 0
                || net.swordie.ms.client.character.tms282.Tms282HiddenSkillParents.authorisedLevel(chr,parent) <= 0)
            throw new IllegalArgumentException("Unowned Xenon multi-skill mode");
        var quest = net.swordie.ms.client.character.quest.Tms282QuestService.markStarted(chr,
                net.swordie.ms.connection.packet.Tms282MultiSkillModeRequest.QUEST_ID).quest();
        quest.setProperty(Integer.toString(parent),selectedSkill);
        return quest;
    }

    private final int[] addedSkills = new int[]{
            SUPPLY_SURPLUS,
            MULTILATERAL_I,
            MODAL_SHIFT,
            LIBERTY_BOOSTERS,
            MIMIC_PROTOCOL,
            PROMESSA_ESCAPE,};

    public Xenon(Char chr) {
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

    public static void temporalPodTimer(Char chr) {
        ScheduledFuture<?> sf = chr.getTimer().addFixedRateEvent(() -> temporalPodEffect(chr), 1000, 1000, false);
        temporalPodTimer = sf;
        GlobalTimerManager.addCharTimer(chr.getId(), sf);
    }

    public static void temporalPodEffect(Char chr) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStatBySkillId(TEMPORAL_POD)) {
            for (int skillId : chr.getSkillCoolTimes().keySet()) {
                chr.reduceSkillCoolTime(skillId, 1000);
            }
        } else {
            temporalPodTimer.cancel(false);
        }
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isXenon(id);
    }

    public void applySupplyCost(int skillID, int slv, SkillInfo si) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (skillID == PINPOINT_SALVO
                || skillID == PINPOINT_SALVO_REDESIGN_A
                || skillID == PINPOINT_SALVO_REDESIGN_B
                || skillID == PINPOINT_SALVO_PERFECT_DESIGN) {
            return;
        }
        if (!tsm.hasStat(AmaranthGenerator)) {
            if (si == null) {
                return;
            }
            if (si.getValue(powerCon, slv) > 0) {
                incSupply -= si.getValue(powerCon, slv);
                incSupply = Math.max(0, incSupply);
            }
            updateSupply();
        }
    }

    public int getMaxSupply() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int max = 5 * JobConstants.getJobLevel(chr.getJob());
        if (tsm.hasStat(OverloadMode)) {
            int boostID = CORE_OVERLOAD_BOOST;
            if (chr.hasSkill(boostID)) {
                SkillInfo si = SkillData.getSkillInfoById(boostID);
                int slv = chr.getSkillLevel(boostID);
                if (si != null) {
                    max += si.getValue(w, slv);
                }
            } else {
                max += 20;
            }
        }
        return max;
    }

    public void incrementSupply(int amount) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (incSupply < getMaxSupply()) {
            incSupply = tsm.getOption(SurplusSupply).nOption;
            incSupply += amount;
            incSupply = Math.min(getMaxSupply(), incSupply);
            updateSupply();
        }
    }

    private void updateSupply() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(OverloadMode)) {
            Option o1 = new Option();
            o1.nReason = CORE_OVERLOAD_BUFF + 100;
            o1.nValue = incSupply;
            newStats.put(IndieDamR, o1);
            chr.write(WvsContext.updateSkillStackRequestResult(CORE_OVERLOAD_BUFF, (byte) incSupply));
        }
        Option o2 = new Option();
        o2.nOption = incSupply;
        newStats.put(SurplusSupply, o2);
        tsm.sendStat(newStats);
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            chr.write(WvsContext.updateSkillStackRequestResult(SUPPLY_SURPLUS, (byte) incSupply));
    }

    private void coreOverloadSkillManaConsumption() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = CORE_OVERLOAD_BUFF;
        Skill skill = chr.getSkill(skillID);
        if (!tsm.hasStat(OverloadMode) || skill == null || !chr.hasSkill(skillID)) {
            return;
        }
        if (chr.getMP() < 2 * (chr.getMaxMP() / 100D)) {
            handleSkillRemove(chr, skillID);
        }
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = skill.getCurrentLevel();
        int mpConsumptionPerSkill = (int) (chr.getMaxMP() / 100D) * si.getValue(q, slv);
        chr.healMP(-mpConsumptionPerSkill);
    }

    public void coreOverloadManaConsumption() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(CORE_OVERLOAD_BUFF);
        if (!tsm.hasStat(OverloadMode) || skill == null || !chr.hasSkill(CORE_OVERLOAD_BUFF)) {
            return;
        }
        if (chr.getMP() < 2 * (chr.getMaxMP() / 100D)) {
            handleSkillRemove(chr, CORE_OVERLOAD_BUFF);
        }
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        chr.healMP(-si.getValue(y, slv));
    }

    @Override
    public void handleCancelTimer(Char chr) {
        supplyTimer.clear();
        if (temporalPodTimer != null) {
            temporalPodTimer.cancel(true);
        }
        super.handleCancelTimer(chr);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        Option o1 = new Option();
        MobTemporaryStat mts = mob.getTemporaryStat();
        EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
        // Triangulation
        applyTriangulationOnMob(mob);
        switch (skillID) {
            case ENTANGLISH_LASH:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.cOption = chr.getId();
                map.put(MobStat.Freeze, o1);
                map.put(MobStat.MagicCrash, o1.deepCopy());
                mts.addStatOptions(mob, map);
                break;
            case ORBITAL_CATACLYSM:
            case HEXA_ORBITAL_CATACLYSM:
                if (!mts.hasCurrentMobStatBySkillId(skillID)) {
                    if (Util.succeedProp(si.getValue(prop, slv))) {
                        o1.nValue = -si.getValue(x, slv);
                        o1.nReason = skillID;
                        o1.tTerm = 10;
                        map.put(MobStat.IndiePDR, o1);
                        map.put(MobStat.IndieMDR, o1.deepCopy());
                        mts.addStatOptions(mob, map);
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
        if (hasHitMobs) {
            // Increment Supply on attack
            if (supplyProp == 0) {
                supplyProp = SkillData.getSkillInfoById(SUPPLY_SURPLUS).getValue(prop, 1);
            }
            if (Util.succeedProp(supplyProp)
                    && skillID != 0
                    && skillID != PINPOINT_SALVO
                    && skillID != PINPOINT_SALVO_REDESIGN_A
                    && skillID != PINPOINT_SALVO_REDESIGN_B
                    && skillID != PINPOINT_SALVO_PERFECT_DESIGN
                    && skillID != TRIANGULATION
                    && skillID != HEXA_TRIANGULATION
                    && skillID != HEXA_TRIANGULATION_ATT) {
                incrementSupply(1);
            }
            if (skillID != PINPOINT_SALVO
                    && skillID != PINPOINT_SALVO_REDESIGN_B
                    && skillID != PINPOINT_SALVO_REDESIGN_A
                    && skillID != PINPOINT_SALVO_PERFECT_DESIGN
                    && skillID != TRIANGULATION
                    && skillID != HEXA_TRIANGULATION
                    && skillID != HEXA_TRIANGULATION_ATT
                    && tsm.hasStatBySkillId(PINPOINT_SALVO)) {
                if (now - pinPointDelay >= 2000) { //2 sec delay
                    createPinPointSalvoForceAtom();
                    pinPointDelay = now;
                }
            }
            if (skillID != PINPOINT_SALVO
                    && skillID != PINPOINT_SALVO_REDESIGN_A
                    && skillID != PINPOINT_SALVO_REDESIGN_B
                    && skillID != PINPOINT_SALVO_PERFECT_DESIGN
                    && skillID != TRIANGULATION
                    && skillID != HEXA_TRIANGULATION
                    && skillID != HEXA_TRIANGULATION_ATT
                    && skillID != CORE_OVERLOAD_ATTACK
                    && skillID != HYPOGRAM_FIELD_FORCE_FIELD
                    && skillID != HYPOGRAM_FIELD_PENETRATE
                    && skillID != HYPOGRAM_FIELD_FUSION) {
                coreOverloadSkillManaConsumption();
                if (!chr.hasSkillOnCooldown(PINPOINT_SALVO)) {
                    createPinPointSalvoForceAtom();
                }
            }
        }
        applySupplyCost(skillID, slv, si);
        Option o1 = new Option();
        Option o2 = new Option();
        switch (skillID) {
            case ORBITAL_CATACLYSM:
            case HEXA_ORBITAL_CATACLYSM:
                int buffID = skillID == HEXA_ORBITAL_CATACLYSM ? HEXA_ORBITAL_CATACLYSM_BUFF : ORBITAL_CATACLYSM_BUFF;
                EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                SkillInfo buff = SkillData.getSkillInfoById(buffID);
                o1.nReason = buffID;
                o1.nValue = buff.getValue(indieDamR, slv);
                o1.tTerm = buff.getValue(time, slv);
                newStats.put(IndieDamR, o1);
                o2.nReason = skillID;
                o2.nValue = 1;
                o2.tTerm = 3;
                newStats.put(IndieNotDamaged, o2);
                tsm.sendStat(newStats);
                break;
            case HEXA_BEAM_DANCE: // biggest meme ever
                Summon summon;
                int summonID = HEXA_BEAM_DANCE_SUMMON;
                int extraAttackID = HEXA_BEAM_DANCE_EXTRA;
                if (!chr.hasSkillOnCooldown(summonID)) {
                    summon = Summon.getSummonByAndSetStat(chr, summonID, chr.getSkillLevel(skillID));
                    summon.setMoveAbility(MoveAbility.Fly);
                    summon.setAssistType(AssistType.Attack);
                    summon.setMoveAction((byte) 4);
                    chr.getField().spawnSummon(summon);
                    chr.addSkillCooldown(summonID, 10000);
                    nextSixthBeamDance = now + 1000;
                    break;
                }
                if (chr.hasSkillOnCooldown(summonID) && now >= nextSixthBeamDance) {
                    ExtraSkill extraSkill = new ExtraSkill(extraAttackID, chr.getPosition());
                    extraSkill.Value = 1;
                    chr.write(UserLocal.registerExtraSkill(extraAttackID, List.of(extraSkill)));
                    nextSixthBeamDance = now + 3000;
                }
                break;
        }
    }

    public void applyTriangulationOnMob(Mob mob) {
        if (!chr.hasSkill(TRIANGULATION)) {
            return;
        }
        int skillID = chr.hasSkill(HEXA_TRIANGULATION) ? HEXA_TRIANGULATION : TRIANGULATION;
        Skill skill = chr.getSkill(skillID);
        int slv = skill.getCurrentLevel();
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int proc = si.getValue(prop, slv);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        int amount = 1;
        MobTemporaryStat mts = mob.getTemporaryStat();
        MobStat mobStat = MobStat.IndieTriangleFormation;
        if (skillID == HEXA_TRIANGULATION) {
            mobStat = MobStat.IndieSixthTriangleFormation;
        }
        if (mts.hasCurrentMobStat(mobStat)) {
            amount = mts.getCurrentOptionsByMobStat(mobStat).nValue;
        }
        if (amount == 3 && chr.hasSkill(HEXA_TRIANGULATION)) {
            chr.write(UserLocal.userBonusAttackRequest(HEXA_TRIANGULATION_ATT, mob.getObjectId()));
            chr.getField().broadcast(MobPool.specialEffectBySkill(mob, HEXA_TRIANGULATION_ATT, chr.getId(), 0));
            mts.removeMobStat(mob, mobStat);
            mts.removeMobStat(mob, MobStat.EVA);
            mts.removeMobStat(mob, MobStat.Blind);
            return;
        }
        if (Util.succeedProp(proc)) {
            if (amount < 3) {
                amount++;
            }
            amount = Math.min(3, amount);
            EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
            int time = si.getValue(SkillStat.time, slv);
            o1.nOption = -si.getValue(SkillStat.x, slv);
            o1.rOption = skillID;
            o1.tOption = time;
            map.put(MobStat.EVA, o1);
            o2.nOption = si.getValue(SkillStat.y, slv);
            o2.rOption = skillID;
            o2.tOption = time;
            map.put(MobStat.Blind, o2);
            o3.nValue = amount;
            o3.nReason = chr.getId();
            o3.tTerm = time;
            map.put(mobStat, o3);
            mts.addStatOptions(mob, map);
        }
    }

    private void createPinPointSalvoForceAtom() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();
        SkillInfo si = SkillData.getSkillInfoById(PINPOINT_SALVO);
        int slv = chr.getSkillLevel(PINPOINT_SALVO);
        Rect rect = chr.getPosition().getRectAround(si.getFirstRect());
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        List<Integer> targetList = new ArrayList<>();
        List<ForceAtomInfo> faiList = new ArrayList<>();
        ForceAtomEnum fae = ForceAtomEnum.XENON_ROCKET_3;
        for (int i = 0; i < si.getValue(bulletCount, slv); i++) {
            Mob mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
            if (mob != null && mob.getHp() >= 0) {
                int angle = new Random().nextInt(90) + 45;
                int fImpact = new Random().nextInt(3) + 18;
                int sImpact = new Random().nextInt(3) + 28;
                int delay = new Random().nextInt(200) + 50;
                ForceAtomInfo forceAtomInfo = new ForceAtomInfo(1, fae.getInc(), fImpact, sImpact,
                        angle, delay, Util.getCurrentTime(), 1, 0,
                        new Position());
                faiList.add(forceAtomInfo);
                targetList.add(mob.getObjectId());
            }
        }
        chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                true, targetList, PINPOINT_SALVO, faiList, rect, 0, 300,
                new Position(), PINPOINT_SALVO, new Position(), 0));
        chr.addSkillCooldown(PINPOINT_SALVO, si.getValue(x, slv) * 1000);
    }

    public int getPinPointSkill() {
        int skill = PINPOINT_SALVO;
        if (chr.hasSkill(PINPOINT_SALVO_REDESIGN_A)) {
            skill = PINPOINT_SALVO_REDESIGN_A;
        }
        if (chr.hasSkill(PINPOINT_SALVO_REDESIGN_B)) {
            skill = PINPOINT_SALVO_REDESIGN_B;
        }
        if (chr.hasSkill(PINPOINT_SALVO_PERFECT_DESIGN)) {
            skill = PINPOINT_SALVO_PERFECT_DESIGN;
        }
        return skill;
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        if (Tms282OmegaBlaster.handles(skillUseInfo.skillID)) return;
        if (isTms282CoreOverload(skillUseInfo.skillID)) {
            if (c == null || c != chr.getClient() || c.getChr() != chr || chr.getJobHandler() != this
                    || !JobConstants.isXenon(chr.getJob()) || Tms282HiddenSkillParents.useLevel(chr, skillUseInfo.skillID) <= 0) return;
            skillUseInfo.skillInfo = SkillData.getSkillInfoById(skillUseInfo.skillID);
            skillUseInfo.slv = Tms282HiddenSkillParents.useLevel(chr, skillUseInfo.skillID);
        }
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        applySupplyCost(skillID, slv, si);
        Option o1 = new Option();
        Option o2 = new Option();
        Summon summon;
        AffectedArea aa;
        Field field;
        if (isTms282CoreOverload(skillID)) {
            // 282 active entries own w/time; passive boost z is not the cast lifetime.
            o1.nOption = si.getValue(w, slv);
            o1.rOption = skillID;
            o1.xOption = 91; // Preserve the existing opaque field; its meaning remains unverified.
            o1.tOption = si.getValue(time, slv);
            tsm.sendStat(OverloadMode, o1);
            updateSupply();
            return;
        }
        switch (skillID) {
            case EMERGENCY_RESUPPLY:
                incrementSupply(si.getValue(x, slv));
                break;
            case PROMESSA_ESCAPE:
                Field toField = chr.getOrCreateFieldByCurrentInstanceType(si.getValue(x, slv));
                chr.warp(toField);
                break;
            case PINPOINT_SALVO:
                if (tsm.hasStat(SurplusSupply)) {
                    int currentSupply = (int) tsm.getTotalNOptionOfStat(SurplusSupply);
                    if (currentSupply > 1) {
                        this.incSupply -= 1;
                        updateSupply();
                        createPinPointSalvoForceAtom();
                    }
                }
                break;
            case HEROS_WILL_XENON:
                tsm.removeAllDebuffs();
                break;
            case LIBERTY_BOOSTERS:
            case ENHANCED_LIBERTY_BOOSTERS:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv) / 1000;
                tsm.sendStat(NewFlying, o1);
                break;
            case CIRCUIT_SURGE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indiePad, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndiePAD, o1);
                break;
            case EFFICIENCY_STREAMLINE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieMhpR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieMHPR, o1);
                o2.nReason = skillID;
                o2.nValue = si.getValue(indieMmpR, slv);
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieMMPR, o2);
                tsm.sendStat(newStats);
                break;
            case HYBRID_DEFENSES:
                o1.nOption = si.getValue(prop, slv);
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(EVAR, o1);
                hybridDefenseCount = si.getValue(x, slv);
                o2.nOption = 1;
                o2.rOption = skillID;
                o2.mOption = hybridDefenseCount;
                newStats.put(StackBuff, o2);
                tsm.sendStat(newStats);
                break;
            case MANIFEST_PROJECTOR:
                o1.nOption = si.getValue(y, slv);
                o1.rOption = skillID;
                tsm.sendStat(ShadowPartner, o1);
                break;
            case OOPARTS_CODE:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o1);
                o2.nOption = si.getValue(x, slv);
                o2.rOption = skillID;
                o2.tOption = si.getValue(time, slv);
                newStats.put(BdR, o2);
                tsm.sendStat(newStats);
                break;
            case AMARANTH_GENERATOR:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(AmaranthGenerator, o1);
                incrementSupply(20);
                break;
            case HYPOGRAM_FIELD_FUSION:
            case HYPOGRAM_FIELD_FORCE_FIELD:
            case HYPOGRAM_FIELD_PENETRATE:
            case HYPOGRAM_FIELD_SUPPORT:
            case HEXA_HYPOGRAM_FIELD_PENETRATE:
            case HEXA_HYPOGRAM_FIELD_FORCE_FIELD:
            case HEXA_HYPOGRAM_FIELD_SUPPORT:
                final int[] ids = {HYPOGRAM_FIELD_FUSION,
                        HYPOGRAM_FIELD_FORCE_FIELD,
                        HYPOGRAM_FIELD_PENETRATE,
                        HYPOGRAM_FIELD_SUPPORT,
                        HEXA_HYPOGRAM_FIELD_PENETRATE,
                        HEXA_HYPOGRAM_FIELD_FORCE_FIELD,
                        HEXA_HYPOGRAM_FIELD_SUPPORT};
                for (int id : ids) {
                    if (id != skillID) {
                        chr.getField().removeAffectedArea(id, chr.getId());
                        tsm.removeStatsBySkill(id);
                    }
                }
                Position position = inPacket.decodePosition();
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setPosition(position);
                summon.setCurFoothold((short) (int) java.util.Optional.ofNullable(chr.getField().findFootHoldBelow(summon.getPosition())).map(below -> below.getId()).orElse(0) /* in the air: no foothold (was an NPE) */);
                summon.setFlyMob(false);
                summon.setMoveAction((byte) 0);
                summon.setMoveAbility(MoveAbility.Stop);
                si = SkillData.getSkillInfoById(skillID);
                summon.setSummonTerm(si.getValue(time, slv) + (chr.hasSkill(HYPOGRAM_FIELD_PERSIST) && skillID != HYPOGRAM_FIELD_FUSION ? 10 : 0));
                chr.getField().spawnSummon(summon);
                if (skillID == HYPOGRAM_FIELD_FUSION || skillID == HYPOGRAM_FIELD_SUPPORT || skillID == HEXA_HYPOGRAM_FIELD_SUPPORT) {
                    aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                    aa.setDelay((short) 2);
                    aa.setPosition(chr.getPosition());
                    aa.setRect(position.getRectAround(si.getFirstRect()));
                    aa.setDuration((si.getValue(time, slv) + (chr.hasSkill(HYPOGRAM_FIELD_PERSIST) && skillID != HYPOGRAM_FIELD_FUSION ? 10 : 0)) * 1000);
                    chr.getField().spawnAffectedArea(aa);
                }
                break;
            case TEMPORAL_POD:
                field = chr.getField();
                aa = AffectedArea.getPassiveAA(chr, skillID, slv);
                aa.setMobOrigin((byte) 0);
                aa.setDelay((short) 0);
                aa.setElemAttr(0);
                aa.setPosition(chr.getPosition());
                aa.setForce(0);
                aa.setOption(0);
                aa.setLinkingSkillID(0);
                aa.setField(chr.getField());
                aa.setRect(chr.getPosition().getRectAround(si.getFirstRect()));
                field.spawnAffectedArea(aa);
                temporalPodTimer(chr);
                chr.write(UserLocal.SitTimeCapsule());
                break;
            case OMEGA_BLASTER:
                o1.nOption = -1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv) + 2;
                tsm.sendStat(MegaSmasher, o1);
                break;
            case CORE_OVERLOAD_BUFF:
                o1.nOption = si.getValue(w, slv);
                o1.rOption = skillID;
                o1.xOption = 91; // from sniff, stayed the same but unsure what it means
                o1.tOption = si.getValue(u2, slv);
                tsm.sendStat(OverloadMode, o1);
                updateSupply();
            case CORE_OVERLOAD_BOOST_SKILL:
                int boostID = CORE_OVERLOAD_BOOST;
                si = SkillData.getSkillInfoById(boostID);
                slv = chr.getSkillLevel(boostID);
                o1.nOption = si.getValue(w, slv);
                o1.rOption = skillID;
                o1.xOption = 91; // from sniff, stayed the same but unsure what it means
                o1.tOption = si.getValue(z, slv);
                tsm.sendStat(OverloadMode, o1);
                updateSupply();
                break;
            case PHOTON_RAY:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.xOption = 0; // ?
                tsm.sendStat(XenonBursterLaser, o1);
                break;
            case PHOTON_RAY_ATTACK:
                if (tsm.hasStat(XenonBursterLaser)) {
                    int bulletCount = si.getValue(SkillStat.bulletCount, slv);
                    List<SecondAtom> secondAtoms = new LinkedList<>();
                    Rect rect = chr.getRectAround(new Rect(-500, -500, 500, 500));
                    if (!chr.isLeft()) {
                        rect = rect.horizontalFlipAround(chr.getPosition().getX());
                    }
                    int bossID = 0;
                    final var mobs = chr.getField().getMobsInRect(rect);
                    var sai = si.getSecondAtomInfos().get(0);
                    if (sai != null) {
                        final var pos = chr.getPosition(); // nếu Position mutable -> copy()
                        int key = 0;
                        final long start = System.currentTimeMillis();
                        for (int i = 0; i < bulletCount; i++) {
                            var mob = Util.getRandomFromCollection(mobs);
                            SecondAtom fa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(),
                                    mob.getObjectId(), key, si.getSkillId(), pos, start);
                            fa.setCustoms(sai.getCustoms());
                            secondAtoms.add(fa);
                            key++;
                        }
                    }
                    chr.createSecondAtom(secondAtoms);
                    if (!secondAtoms.isEmpty()) {
                        tsm.removeStatsBySkill(PHOTON_RAY);
                    }
                }
                break;
            case ARTIFICIAL_EVOLUTION_BUFF:
                slv = chr.getSkillLevel(ARTIFICIAL_EVOLUTION);
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(ArtificialEvolution, o1);
                break;
        }
    }

    // Hit related methods ---------------------------------------------------------------------------------------------
    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();

        if (chr.hasSkill(HYBRID_DEFENSES)) {
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            Skill skill = chr.getSkill(HYBRID_DEFENSES);
            int slv = skill.getCurrentLevel();
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());

            if (tsm.getOptByCTSAndSkill(StackBuff, HYBRID_DEFENSES) != null) {
                if (hitInfo.hpDamage > 0) {
                    o1.nOption = 1;
                    o1.rOption = skill.getSkillId();
                    o1.mOption = hybridDefenseCount;
                    newStats.put(StackBuff, o1);
                    o2.nOption -= si.getValue(y, slv);
                    o2.rOption = skill.getSkillId();
                    newStats.put(EVAR, o2);

                    tsm.sendStat(newStats);
                } else {
                    hybridDefenseCount--;
                    if (hybridDefenseCount <= 0) {
                        tsm.removeStatsBySkill(HYBRID_DEFENSES);
                        return;
                    }
                    o1.nOption = 1;
                    o1.rOption = skill.getSkillId();
                    o1.mOption = hybridDefenseCount;
                    newStats.put(StackBuff, o1);
                    o2.nOption -= 0;
                    o2.rOption = skill.getSkillId();
                    newStats.put(EVAR, o2);
                    tsm.sendStat(newStats);
                }

            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.XENON1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.XENON2.getJobId());
                sm.giveItem(1142576);
                sm.completeQuestNoRewards(23610);
                sm.completeQuestNoRewards(23611);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.XENON2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.XENON3.getJobId());
                sm.giveItem(1142577);
                sm.completeQuestNoRewards(23612);
                sm.completeQuestNoRewards(23613);
                sm.completeQuestNoRewards(23614);
                sm.completeQuestNoRewards(23615);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.XENON3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.XENON4.getJobId());
                sm.giveItem(1142578);
                sm.completeQuestNoRewards(23616);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }

    public void handleOmegaBlaster(boolean start) {
        if (Tms282OmegaBlaster.handles(OMEGA_BLASTER)) return;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!start || tsm.hasStat(ArtificialEvolution)) {
            if (tsm.getOption(MegaSmasher).nOption == 1) {
                return;
            }
            Skill skill = chr.getSkill(OMEGA_BLASTER);
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            Option o1 = new Option();
            Option o2 = new Option();

            long secPerXSec = si.getValue(y, slv) * 1000L;
            long cap = si.getValue(z, slv) * 1000L;

            long chargingDuration = Util.getCurrentTimeLong() - tsm.getOptByCTSAndSkill(MegaSmasher, OMEGA_BLASTER).startTime;
            long addedDuration = (chargingDuration / secPerXSec) * 1000;
            long initDuration = si.getValue(time, slv) * 1000L;
            long fullDuration = tsm.hasStat(ArtificialEvolution) ? 10000: initDuration + (Math.min(addedDuration, cap));

            o1.nOption = 1;
            o1.rOption = skill.getSkillId();
            o1.tOption = (int) fullDuration;
            o1.setInMillis(true);
            tsm.sendStat(MegaSmasher, o1);
            o2.nOption = 1;
            o2.rOption = skill.getSkillId();
            o2.tOption = (int) fullDuration;
            o2.setInMillis(true);
            tsm.sendStat(IndieNotDamaged, o2); // invincibility
        }
    }

    @Override
    public void handleSkillRemove(Char chr, int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (isTms282CoreOverload(skillID)) {
            tsm.removeStatsBySkill(skillID); // The same CTS removal callback also owns natural expiry.
            return;
        }
        switch (skillID) {
            case CORE_OVERLOAD_BUFF:
                tsm.removeStatsBySkill(CORE_OVERLOAD_BUFF + 100);
                tsm.removeStatsBySkill(skillID);
                incSupply = incSupply > 20 ? 20 : incSupply;
                updateSupply();
                break;
        }

        super.handleSkillRemove(chr, skillID);
    }

    private static boolean isTms282CoreOverload(int skillID) {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3
                && (skillID == CORE_OVERLOAD_BUFF || skillID == CORE_OVERLOAD_BOOST_SKILL);
    }

    @Override
    public void applyHpCost(int skillID) {
        if (isTms282CoreOverload(skillID) && skillID == CORE_OVERLOAD_BOOST_SKILL) {
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = Tms282HiddenSkillParents.useLevel(chr, skillID);
            // Legacy normalization asks for hidden attack400041031 instead of the learned active VI record.
            if (chr.getJobHandler() == this && JobConstants.isXenon(chr.getJob()) && slv > 0
                    && si != null && si.getValue(hpCon, slv) == 0 && si.getValue(hpRCon, slv) == 0) return;
        }
        super.applyHpCost(skillID);
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts, List<Option> options) {
        super.handleRemoveCTS(cts, options);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (cts == OverloadMode && chr.getJobHandler() == this && JobConstants.isXenon(chr.getJob())
                && !tsm.hasStat(OverloadMode) && options.stream().anyMatch(o -> o != null && isTms282CoreOverload(o.rOption))) {
            tsm.removeStatsBySkill(CORE_OVERLOAD_BUFF + 100);
            incSupply = Math.min(incSupply, getMaxSupply());
            updateSupply();
        }
    }

    @Override
    public void setCharCreationStats(Char chr) {
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);
        cs.setLevel(10);
        cs.setStr(15);
        cs.setDex(15);
        cs.setInt(4);
        cs.setLuk(15);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        if (!JobConstants.isNoManaJob(chr.getJob())) {
            cs.setMp(500);
            cs.setMaxMp(500);
        }
    }

    @Override
    public void handleInitAfterMigrate(Char chr) {
        if (chr.getLevel() < 30) {
            ScriptManagerImpl sm = chr.getScriptManager();
            boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
            sm.setJob((short) (tms282 ? 3610 : 3600));
            sm.levelUntil(30);
            sm.completeQuestNoRewards(23600);
            for (int qid = 23606; qid <= 23656; qid++) {
                sm.completeQuestNoRewards(qid);
            }
            sm.addSPJobAdv((short) 3600, 5);
            sm.addSPJobAdv((short) 3610, 3);
            sm.giveAndEquip(1242002);
            sm.giveAndEquip(1353002);
            sm.warp(FieldConstants.HOME_MAP);
        }
        super.handleInitAfterMigrate(chr);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case HYPOGRAM_FIELD_PENETRATE -> { // Hypogram Field: Force Field
                chr.setSkillCooldown(skillId, chr.getSkillLevel(HYPOGRAM_FIELD_FORCE_FIELD));
                return 1;
            }
            case HYPOGRAM_FIELD_SUPPORT -> { // Hypogram Field: Support
                chr.setSkillCooldown(skillId, chr.getSkillLevel(HYPOGRAM_FIELD_FORCE_FIELD));
                return 1;
            }
            case HEXA_ORBITAL_CATACLYSM -> {
                int skillID = ORBITAL_CATACLYSM;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_MECHA_PURGE_EXECUTE -> {
                chr.addSkillCooldown(skillId, 8000);
                return 1;
            }
            case HEXA_MECHA_PURGE_FIRE -> {
                chr.addSkillCooldown(skillId, 6000);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
