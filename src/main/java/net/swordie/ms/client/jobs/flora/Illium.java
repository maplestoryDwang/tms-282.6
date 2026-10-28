package net.swordie.ms.client.jobs.flora;

import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.ShootObjectSkillInfo;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Summoned;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
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

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.enums.InvType.EQUIPPED;

/**
 * @author Sjonnie
 * Created on 6/25/2018.
 */
public class Illium extends Job {

    public static final int SHELTER_RETURN = 150001021; // requires Complete QuestId  34900
    public static final int MAGIC_CONVERSION = 150000079;

    public static final int EX = 152001003;
    public static final int RADIANT_ORB = 152001002;
    public static final int RADIANT_JAVELIN = 152001001;
    public static final int CRYSTALLINE_WINGS = 152001004;
    public static final int CRYSTALLINE_WINGS_FLY = 152001005;
    public static final int LUCENT_BRAND = 152000007;
    public static final int UMBRAL_BRAND_HIDDEN = 152000010;

    public static final int REACTION_DESTRUCTION = 152100001;
    public static final int REACTION_DOMINATION = 152100002;
    public static final int DEPLOY_CRYSTAL = 152101000;
    public static final int REPOSITION_CRYSTAL = 152101003;
    public static final int CRYSTAL_BATTERY = 152100010;
    public static final int VORTEX_OF_LIGHT = 152101006;
    public static final int UMBRAL_BRAND = 152100012;
    public static final int GAUNTLET_FRENZY = 152101007;
    public static final int MACHINA = 152101008;

    public static final int CRYSTAL_BATTERY_II = 152110008;
    public static final int LUCENT_BRAND_II = 152110009;
    public static final int UMBRAL_BRAND_II = 152110010;
    public static final int RESONANCE = 152111007;
    public static final int REACTION_DESTRUCTION_II = 152110001;
    public static final int REACTION_DOMINATION_II = 152110002;
    public static final int RADIANT_JAVELIN_ENHANCED = 152110004;

    public static final int AEGIS_OF_LIGHT = 152100011;
    public static final int RADIANT_ORB_II = 152120003;
    public static final int CRYSTAL_BATTERY_III = 152120014;
    public static final int LUCENT_BRAND_III = 152120012;
    public static final int UMBRAL_BRAND_III = 152120013;
    public static final int CRYSTAL_SKILL_DEUS = 152121005;
    public static final int HERO_OF_THE_FLORA = 152121009;
    public static final int FLORAN_HERO_WILL = 152121010;
    public static final int FLASH_CRYSTAL_BATTERY = 152121011;
    public static final int RADIANT_JAVELIN_II = 152120001;
    public static final int LONGINUS_SPEAR = 152121004;
    public static final int WINGS_OF_GLORY = 152111003;
    public static final int DEUS_SUB = 152121006;
    public static final int VORTEX_WINGS = 152121007;
    public static final int RADIANT_ATOM = 152120002;

    public static final int LONGINUS_ZONE = 152121041;
    public static final int DIVINE_WRATH = 152121042;
    public static final int CRYSTALLINE_BULWARK = 152121043;

    // V skills
    public static final int CRYSTAL_IGNITION = 400021061;
    public static final int REFLECTION_SPECTRAL_BLAST = 400021062;
    public static final int TEMPLAR_KNIGHT = 400021063;
    public static final int CRYSTALLINE_SPIRIT = 400021068;
    public static final int CRYSTAL_GATE = 400021099;
    public static final int CRYSTAL_GATE_PORTAL = 400021100;
    public static final int CRYSTAL_GATE_PORTAL_ATTACK = 400021111;

    public static final int CRYSTAL_SKILL_ID_VORTEX_OF_LIGHT = 1;
    public static final int CRYSTAL_SKILL_ID_RESONANCE = 2;
    public static final int CRYSTAL_SKILL_ID_DEUS = 3;
    public static final int CRYSTAL_SKILL_ID_WINGS_OF_GLORY = 4;
    public static final int CRYSTAL_SKILL_ID_VORTEX_WINGS = 5;

    private static final int[] lucentSkills = new int[]{
            LUCENT_BRAND,
            LUCENT_BRAND_II,
            LUCENT_BRAND_III
    };

    private static final int[] umbralSkills = new int[]{
            UMBRAL_BRAND,
            UMBRAL_BRAND_II,
            UMBRAL_BRAND_III
    };

    private record JavelinScope(java.lang.ref.WeakReference<Client> client,
                                java.lang.ref.WeakReference<Field> field,
                                java.lang.ref.WeakReference<ForceAtom> atom) { }
    private final Map<Integer, JavelinScope> tms282JavelinFields = new HashMap<>();

    /** A client update must reference a server-created javelin in its original field. */
    public synchronized void updateTms282Javelin(net.swordie.ms.connection.packet.Tms282ForceAtomUpdateRequest update) {
        var atom = chr.getForceAtomByKey(update.key());
        var scope = tms282JavelinFields.get(update.key());
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                || chr.getClient() == null || chr.getClient().getChr() != chr || chr.getField() == null
                || atom == null || scope == null || scope.field().get() != chr.getField()
                || scope.client().get() != chr.getClient() || scope.atom().get() != atom
                || atom.getCharId() != chr.getId()
                || atom.getForceAtomEnum() != ForceAtomEnum.RADIANT_JAVELIN)
            throw new IllegalArgumentException("Unowned or stale javelin");
        if (update.targetId() != 0 && !(chr.getField().getLifeByObjectID(update.targetId()) instanceof Mob))
            throw new IllegalArgumentException("Javelin target outside the field");
        atom.setTargetIdList(List.of(update.targetId()));
        chr.getField().broadcast(update.remote(chr.getId()), chr);
    }

    /** A field assignment destroys client javelins; revoke only the same tracked server instances. */
    public synchronized void clearTms282Javelins() {
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) return;
        tms282JavelinFields.forEach((key, scope) -> {
            if (scope.atom().get() == chr.getForceAtomByKey(key)) chr.removeForceAtomByKey(key);
        });
        tms282JavelinFields.clear();
    }

    private synchronized void registerTms282Javelin(int key, ForceAtom atom, Field field) {
        var entries = tms282JavelinFields.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next(); var scope = entry.getValue();
            if (scope.field().get() == field && scope.client().get() == chr.getClient()
                    && scope.atom().get() == chr.getForceAtomByKey(entry.getKey())) continue;
            if (scope.atom().get() == chr.getForceAtomByKey(entry.getKey())) chr.removeForceAtomByKey(entry.getKey());
            entries.remove();
        }
        tms282JavelinFields.put(key, new JavelinScope(new java.lang.ref.WeakReference<>(chr.getClient()),
                new java.lang.ref.WeakReference<>(field), new java.lang.ref.WeakReference<>(atom)));
    }

    private record BatteryScope(java.lang.ref.WeakReference<Client> client,
                                java.lang.ref.WeakReference<Field> field,
                                java.lang.ref.WeakReference<Summon> crystal, int userId, int source) { }
    private BatteryScope passiveCrystalScope;
    private long passiveCrystalCreditAt;
    private int passiveCrystalTick;
    private boolean passiveCrystalWasFull;

    private static boolean tms282Battery() {
        return net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
    }

    private SkillInfo tms282BatteryInfo() {
        int job = chr.getJob();
        if (job < 15210 || job > 15212) return null;
        for (int source : new int[]{CRYSTAL_BATTERY_III, CRYSTAL_BATTERY_II, CRYSTAL_BATTERY}) {
            int tier = source == CRYSTAL_BATTERY_III ? 15212 : source == CRYSTAL_BATTERY_II ? 15211 : 15210;
            if (job < tier || chr.getSkillLevel(source) <= 0) continue;
            return chr.getSkillLevel(source) == 1 ? SkillData.getSkillInfoById(source) : null;
        }
        return null;
    }

    private static int batteryCap(SkillInfo info) {
        if (info == null) return 0;
        return info.getValue(info.getSkillId() == CRYSTAL_BATTERY ? x
                : info.getSkillId() == CRYSTAL_BATTERY_II ? z : u, 1);
    }

    /** Char.setField calls this before changing the field, including same/cached field re-entry. */
    public synchronized void clearTms282BatteryTimer() {
        if (!tms282Battery()) return;
        passiveCrystalScope = null;
        passiveCrystalCreditAt = 0;
        passiveCrystalTick = 0;
        passiveCrystalWasFull = false;
    }

    /** Owned native409 receipt;408 can earn only server-clock credit, never client tick credit. */
    public synchronized int pulseTms282PassiveCrystalCharge(
            net.swordie.ms.connection.packet.Tms282IlliumTimerRequest request, long now) {
        Client client = chr.getClient();
        Field field = chr.getField();
        Summon crystal = field == null ? null : getCrystal();
        SkillInfo info = tms282BatteryInfo();
        Option producer = chr.getTemporaryStatManager().getOptByCTSAndSkill(IndieEmpty, DEPLOY_CRYSTAL);
        if (!tms282Battery() || request == null || chr.getJobHandler() != this || !isHandlerOfJob(chr.getJob())
                || chr.getHP() <= 0 || chr.getId() <= 0 || field == null || field.getCharByID(chr.getId()) != chr
                || client == null || client.getChr() != chr || client.getCurrentState() != Client.IN_FIELD
                || client.getCh() == null || !client.getCh().isActive() || client.getUser() == null
                || client.getUser().getId() <= 0 || chr.getUser() != client.getUser()
                || !net.swordie.ms.handlers.login.tms282.Tms282LoginState.of(client).isChannel()
                || info == null || crystal == null || producer == null || producer.summon != crystal
                || crystal.getChr() != chr || crystal.getOwnerId() != chr.getId()
                || field.getLifeByObjectID(crystal.getObjectId()) != crystal
                || crystal.getSlv() != chr.getSkillLevel(DEPLOY_CRYSTAL) || now <= 0)
            throw new IllegalArgumentException("Unowned crystal recharge timer");
        int cap = batteryCap(info), interval = info.getValue(s2, 1), rate = info.getValue(s3, 1);
        int stages = info.getSkillId() == CRYSTAL_BATTERY ? 1 : info.getSkillId() == CRYSTAL_BATTERY_II ? 3 : 4;
        if (interval != 1000 || info.getValue(w, 1) != stages || cap != (stages == 1 ? 30 : 150)
                || rate != (stages == 4 ? 3 : 1) || crystal.getCount() < 0 || crystal.getCount() > cap)
            throw new IllegalArgumentException("Unverified crystal recharge data/state");
        boolean bound = passiveCrystalScope != null && passiveCrystalScope.client().get() == client
                && passiveCrystalScope.field().get() == field && passiveCrystalScope.crystal().get() == crystal
                && passiveCrystalScope.userId() == client.getUser().getId() && passiveCrystalScope.source() == info.getSkillId();
        if (request.start()) {
            if (!bound) {
                passiveCrystalScope = new BatteryScope(new java.lang.ref.WeakReference<>(client),
                        new java.lang.ref.WeakReference<>(field), new java.lang.ref.WeakReference<>(crystal),
                        client.getUser().getId(), info.getSkillId());
                passiveCrystalCreditAt = now;
                passiveCrystalTick = request.clientTick();
                passiveCrystalWasFull = crystal.getCount() == cap;
            }
            return 0;
        }
        if (!bound) { clearTms282BatteryTimer(); throw new IllegalArgumentException("Crystal recharge timer was not started"); }
        if (now < passiveCrystalCreditAt) { clearTms282BatteryTimer(); throw new IllegalArgumentException("Crystal recharge clock rollback"); }
        long delta = Integer.toUnsignedLong(request.clientTick() - passiveCrystalTick);
        if (delta > 60000) throw new IllegalArgumentException("Stale crystal recharge tick");
        int before = crystal.getCount();
        if (before == cap) {
            passiveCrystalWasFull = true;
            passiveCrystalCreditAt = now;
        } else if (passiveCrystalWasFull) {
            passiveCrystalWasFull = false;
            passiveCrystalCreditAt = now; // time spent full never becomes recharge credit after consumption
        } else if (delta != 0) {
            long elapsed = now - passiveCrystalCreditAt;
            int ticks = (int)Math.min(3, elapsed / interval); // bounded catchup; discard older backlog
            if (ticks > 0) {
                passiveCrystalCreditAt = now - elapsed % interval;
                changeCrystalCharge(Math.min(cap, before + ticks * rate));
            }
        }
        passiveCrystalTick = request.clientTick();
        chr.write(net.swordie.ms.connection.packet.Tms282SkillStackTranslators.stackResult(
                net.swordie.ms.connection.packet.Tms282IlliumTimerRequest.KEY, crystal.getCount() < cap ? 1 : 0));
        return crystal.getCount() - before;
    }

    public Map<Integer, Boolean> crystalSkillMap = new HashMap<Integer, Boolean>() {{
        put(CRYSTAL_SKILL_ID_VORTEX_OF_LIGHT, true);
        put(CRYSTAL_SKILL_ID_RESONANCE, true);
        put(CRYSTAL_SKILL_ID_DEUS, true);
        put(CRYSTAL_SKILL_ID_WINGS_OF_GLORY, true);
        put(CRYSTAL_SKILL_ID_VORTEX_WINGS, true);
    }};

    private static final int[] addedSkills = new int[]{
            SHELTER_RETURN,
            MAGIC_CONVERSION,
    };

    public Illium(Char chr) {
        super(chr);
        if (chr != null && chr.getId() != 0 && isHandlerOfJob(chr.getJob())) {
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
        return JobConstants.isIllium(id);
    }


    public Summon getCrystal() {
        return chr.getField().getSummons().stream().filter(s -> s.getSkillID() == DEPLOY_CRYSTAL && s.getOwnerId() == chr.getId()).findAny().orElse(null);
    }

    private int getCrystalCharge() {
        if (getCrystal() == null) {
            return 0;
        }
        return getCrystal().getCount();
    }

    private void setCrystalCharge(int charge) {
        if (getCrystal() != null) {
            getCrystal().setCount(charge);
        }
    }

    private int getShards() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        return tsm.hasStat(CharacterTemporaryStat.CannonShooter_BFCannonBall) ? tsm.getOption(CharacterTemporaryStat.CannonShooter_BFCannonBall).nOption : 0;
    }

    public void incrementCrystallineShard() {
        if (!chr.hasSkill(CRYSTALLINE_SPIRIT) || chr.hasSkillOnCooldown(CRYSTALLINE_SPIRIT)) {
            return;
        }
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Skill skill = chr.getSkill(CRYSTALLINE_SPIRIT);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        int maxStack = si.getValue(y, slv);
        int count = 1;
        if (tsm.hasStat(CharacterTemporaryStat.CannonShooter_BFCannonBall)) {
            count = getShards();
            if (count < maxStack) {
                count++;
            }
        }
        updateVSkillStackBuff(chr, count);
        chr.addSkillCooldown(CRYSTALLINE_SPIRIT, 20 * 1000);
    }

    public void resetCrystalBattery() {
        Summon crystal = getCrystal();
        if (crystal == null) {
            return;
        }
        this.crystalSkillMap.put(CRYSTAL_SKILL_ID_VORTEX_OF_LIGHT, true);
        this.crystalSkillMap.put(CRYSTAL_SKILL_ID_RESONANCE, true);
        this.crystalSkillMap.put(CRYSTAL_SKILL_ID_DEUS, true);
        this.crystalSkillMap.put(CRYSTAL_SKILL_ID_WINGS_OF_GLORY, true);
        chr.getField().broadcast(Summoned.stateChanged(crystal, 2, crystalSkillMap));
        chr.getField().broadcast(Summoned.upgradeStage(crystal, 3)); // resets crystal attacks
    }

    public void doResonanceSkill() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        chr.getField().broadcast(Summoned.stateChanged(getCrystal(), 1, null));
        if (!chr.hasSkill(RESONANCE)) {
            return;
        }
        Skill skill = chr.getSkill(RESONANCE);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        o.nOption = 1;
        o.rOption = skill.getSkillId();
        o.tOption = si.getValue(x, slv);
        tsm.sendStat(HarmonyLink, o);
        this.crystalSkillMap.put(CRYSTAL_SKILL_ID_RESONANCE, false);
        chr.getField().broadcast(Summoned.stateChanged(getCrystal(), 2, crystalSkillMap));
        chr.getField().broadcast(Summoned.upgradeStage(getCrystal(), 3)); // resets crystal attacks
    }

    public void incrementCrystal(int skillId) {
        int increment = (getIncrementBySkillId(skillId) * (hasFlashCrystalBattery() ? 2 : 1));
        changeCrystalCharge(getCrystalCharge() + increment);
        chr.getField().broadcast(Summoned.upgradeStage(getCrystal(), 3)); // resets crystal attacks

        if (!chr.hasSkillOnCooldown(REACTION_DESTRUCTION_II) && (chr.getSkillLevel(REACTION_DESTRUCTION_II) >= 1) && (skillId == RADIANT_JAVELIN_II || skillId == RADIANT_JAVELIN || skillId == RADIANT_JAVELIN || skillId == LONGINUS_SPEAR)) {
            doDestructionII();
        } else if (!chr.hasSkillOnCooldown(REACTION_DESTRUCTION) && (chr.getSkillLevel(REACTION_DESTRUCTION) >= 1) && (skillId == RADIANT_JAVELIN || skillId == RADIANT_JAVELIN)) {
            doDestruction();
        }
    }

    private void doDestruction() {
        SkillInfo si = SkillData.getSkillInfoById(REACTION_DESTRUCTION);
        int slv = chr.getSkillLevel(REACTION_DESTRUCTION);
        //   chr.write(Summoned.summonUseSpecifiedSkill(getCrystal(), REACTION_DESTRUCTION));
        chr.addSkillCooldown(REACTION_DESTRUCTION, si.getValue(cooltime, slv) * 1000);
    }

    private void doDestructionII() {
        SkillInfo si = SkillData.getSkillInfoById(REACTION_DESTRUCTION_II);
        int slv = chr.getSkillLevel(REACTION_DESTRUCTION_II);
        chr.write(Summoned.useSpecifiedSkill(getCrystal(), REACTION_DESTRUCTION_II));
        chr.addSkillCooldown(REACTION_DESTRUCTION_II, si.getValue(cooltime, slv) * 1000);
    }

    public void doCrystallineDestruction(int summonObjId, int summonAttackId) {
        Summon illi = (Summon) chr.getField().getLifeByObjectID(summonObjId);
        if (illi != null) {
            chr.write(Summoned.useSpecifiedSkill(illi, summonAttackId));
        }
    }

    public void changeCrystalCharge(int charge) {
        Summon crystal = getCrystal();
        if (crystal == null) {
            return;
        }
        int curState = crystal.getState();
        if (tms282Battery() && passiveCrystalScope != null
                && (charge >= getMaxCrystalCharge() || crystal.getCount() >= getMaxCrystalCharge())) passiveCrystalWasFull = true;

        charge = charge < 0 ? 0 : Math.min(charge, getMaxCrystalCharge());
        setCrystalCharge(charge);
        crystal.setState(getCrystalStateByCharge(charge));
        chr.getField().broadcast(Summoned.upgradeStage(crystal, 2)); // change Crystal Summon State

        if (curState != crystal.getState()) {
            chr.getField().broadcast(Summoned.effect(crystal, 3)); // new form effect
        } else {
            chr.getField().broadcast(Summoned.effect(crystal, 2)); // increment effect
        }

        if (getCrystalCharge() >= getMaxCrystalCharge() && !chr.getTemporaryStatManager().hasStat(CharacterTemporaryStat.CrystalChargeBuffIcon)) {
            giveCrystalBatteryBuff();
        }
    }

    private int getMaxCrystalCharge() {
        if (tms282Battery()) return batteryCap(tms282BatteryInfo());
        int maxCrystalCharge = 0;
        if (chr.hasSkill(CRYSTAL_BATTERY)) {
            maxCrystalCharge = 30;
        }
        if (chr.hasSkill(CRYSTAL_BATTERY_II)) {
            maxCrystalCharge = 150;
        }
        return maxCrystalCharge;
    }


    private Skill getCrystalBatterySkill() {
        Skill skill = null;
        if (chr.hasSkill(CRYSTAL_BATTERY)) {
            skill = chr.getSkill(CRYSTAL_BATTERY);
        }
        if (chr.hasSkill(CRYSTAL_BATTERY_II)) {
            skill = chr.getSkill(CRYSTAL_BATTERY_II);
        }
        if (chr.hasSkill(CRYSTAL_BATTERY_III)) {
            skill = chr.getSkill(CRYSTAL_BATTERY_III);
        }
        return skill;
    }

    private boolean hasFlashCrystalBattery() {
        return chr.getTemporaryStatManager().hasStat(CharacterTemporaryStat.LefFastCharge);
    }

    private void giveCrystalBatteryBuff() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = 1;
        o.rOption = tms282Battery() ? tms282BatteryInfo().getSkillId() : getCrystalBatterySkill().getSkillId();
        o.tOption = 10;
        tsm.sendStat(CrystalChargeBuffIcon, o);
    }

    private int getIncrementBySkillId(int skillId) {
        switch (skillId) {
            case RADIANT_JAVELIN:
            case RADIANT_JAVELIN_II:
                return 1;
            case RADIANT_ORB:
            case RADIANT_ORB_II:
                return 2;
            case LONGINUS_SPEAR:
                return 3;
        }
        return 0;
    }

    private int getCrystalStateByCharge(int count) {
        if (tms282Battery()) {
            SkillInfo info = tms282BatteryInfo();
            if (info == null) return 0;
            int stage = 0;
            var thresholds = info.getSkillId() == CRYSTAL_BATTERY ? new net.swordie.ms.client.character.skills.SkillStat[]{x}
                    : info.getSkillId() == CRYSTAL_BATTERY_II ? new net.swordie.ms.client.character.skills.SkillStat[]{x, y, z}
                    : new net.swordie.ms.client.character.skills.SkillStat[]{x, y, z, u};
            for (var threshold : thresholds) if (count >= info.getValue(threshold, 1)) stage++;
            return stage;
        }
        int state = 0;
        if (count >= 0) {
            state = 0;
        }
        if (count >= 30) {
            state = 1;
        }
        if (count >= 60) {
            state = 2;
        }
        if (count >= 100) {
            state = 3;
        }
        if (count >= 150) {
            state = 4;
        }
        return state;
    }

    public void applyUmbralBrand(int oid) {
        int skillId = UMBRAL_BRAND;
        Skill skill = chr.getSkill(skillId);
        if (skill == null) {
            return;
        }
        Option o1 = new Option();
        int maxstack = chr.hasSkill(UMBRAL_BRAND_III) ? 5 : chr.hasSkill(UMBRAL_BRAND_II) ? 3 : 1;
        int skillID = chr.hasSkill(UMBRAL_BRAND_III) ? UMBRAL_BRAND_III : chr.hasSkill(UMBRAL_BRAND_II) ? UMBRAL_BRAND_II : UMBRAL_BRAND;
        Mob mob = (Mob) chr.getField().getLifeByObjectID(oid);
        if (mob != null) {
            MobTemporaryStat mts = mob.getTemporaryStat();
            if (mts.hasCurrentMobStat(MobStat.LefDebuff)) {
                int currentMark = mts.getCurrentOptionsByMobStat(MobStat.LefDebuff).nOption;
                o1.nOption = Math.min(currentMark + 1, maxstack);
            } else {
                o1.nOption = 1;
            }
            o1.rOption = UMBRAL_BRAND_HIDDEN;
            o1.tOption = chr.hasSkill(UMBRAL_BRAND_III) ? 50 : chr.hasSkill(UMBRAL_BRAND_II) ? 30 : 10; //TODO fix this
            o1.xOption = 4;
            o1.zOption = skillID;
            mts.addStatOptions(mob, MobStat.LefDebuff, o1);
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
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
        if (hasHitMobs && attackInfo.skillId != CONVERSION_OVERDRIVE_ATTACK) {
            bonusConversionOverdriveAttack();
        }
        if (hasHitMobs) {
            handleAegisOfLightMpRecovery(attackInfo);
            handleCrystalGateAttack(skillID);
            if (skillID == VORTEX_WINGS) {
                applyUmbralBrand(attackInfo.mobAttackInfo.get(0).mobId);
            }
            if (chr.getField().getSummonBySkillId(chr, CRYSTAL_SKILL_DEUS) != null) {
                chr.write(UserLocal.userBonusAttackRequest(CRYSTAL_SKILL_DEUS));
            }
        }
        switch (skillID) {
            case DEPLOY_CRYSTAL:
                int attackingSkillId = attackInfo.summonSpecialSkillId;
                switch (attackingSkillId) {
                    case REACTION_DOMINATION:
                    case REACTION_DOMINATION_II:
                        slv = chr.getSkillLevel(attackingSkillId);
                        chr.setSkillCooldown(attackingSkillId, slv);
                        si = SkillData.getSkillInfoById(attackingSkillId);
                        chr.addSkillCooldown(attackingSkillId, si.getValue(cooltime, slv) * 1000);
                        for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                            applyUmbralBrand(mai.mobId);
                        }
                        break;
                    case VORTEX_OF_LIGHT:
                        crystalSkillMap.put(CRYSTAL_SKILL_ID_VORTEX_OF_LIGHT, false);
                        chr.getField().broadcast(Summoned.stateChanged(getCrystal(), 2, crystalSkillMap));
                        break;
                    case REFLECTION_SPECTRAL_BLAST:
                        chr.setSkillCooldown(attackingSkillId, chr.getSkillLevel(CRYSTAL_IGNITION));
                        break;
                }
                chr.getField().broadcast(Summoned.upgradeStage(getCrystal(), 3)); // resets crystal attacks
                break;
            case VORTEX_WINGS:
                handleVortexWings();
                break;
        }
    }

    private void handleAegisOfLightMpRecovery(AttackInfo attackInfo) {
        if (chr.getSkillLevel(AEGIS_OF_LIGHT) <= 0 || Util.succeedProp(80)) {
            return;
        }
        for (Mob mob : chr.getField().getMobs(attackInfo.mobAttackInfo)) {
            if (mob == null || mob.getMp() <= 0 || mob.getMaxMp() <= 0) {
                continue;
            }
            chr.healMP((int) (mob.getMaxMp() * (mob.isBoss() ? 0.05 : 0.10)));
            return;
        }
    }

    private void handleCrystalGateAttack(int skillID) {
        if (skillID == CRYSTAL_GATE_PORTAL_ATTACK || !chr.getTemporaryStatManager().hasStat(LefMageCrystalGate)) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(CRYSTAL_GATE);
        int slv = chr.getSkillLevel(CRYSTAL_GATE);
        int interval = si == null ? 5 : Math.max(1, si.getValue(t, slv));
        if (chr.hasSkillOnCooldown(CRYSTAL_GATE_PORTAL_ATTACK)) {
            return;
        }
        chr.write(UserLocal.userBonusAttackRequest(CRYSTAL_GATE_PORTAL_ATTACK));
        chr.addSkillCooldown(CRYSTAL_GATE_PORTAL_ATTACK, interval * 1000);
    }

    private void handleVortexWings() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        if (tsm.hasStat(CharacterTemporaryStat.LefGloryWing)) {
            int nOpt = tsm.getOption(CharacterTemporaryStat.LefGloryWing).nOption;
            int nSkill = tsm.getOption(CharacterTemporaryStat.LefGloryWing).rOption;
            o.nOption = nOpt;
            o.rOption = nSkill;
            o.cOption = 1;
            tsm.sendStat(LefGloryWing, o);
        }
    }

    public void handleRadiantAtom() {
        Rect rect = new Rect(-550, -550, 550, 550);
        int targetID = 0;
        for (int i = 0; i < 2; i++) {
            ForceAtomEnum fae = ForceAtomEnum.RADIANT_JAVELIN_AFTER;
            Mob targetMob;
            if (!chr.getField().getMobsInRect(chr.getRectAround(rect)).isEmpty()) {
                targetMob = Util.getRandomFromCollection(chr.getField().getMobsInRect(chr.getRectAround(rect)));
            } else {
                targetMob = null;
            }
            targetID = (targetMob == null || targetMob.getHp() <= 0 ? 0 : targetMob.getObjectId());

            ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), 1, Util.getRandom(40, 70),
                    Util.getRandom(4, 8), chr.isLeft() ? 150 : 90, 0, Util.getCurrentTime(), 0,
                    0, chr.getPosition());
            ForceAtom fa = new ForceAtom(false, chr.getId(), chr.getId(), fae,
                    true, targetID, RADIANT_ATOM, fai, chr.getRectAround(rect), 0, 0,
                    chr.getPosition(), 0, chr.getPosition(), 0);
            chr.createForceAtom(fa);
        }
    }

    // Skill related methods -------------------------------------------------------------------------------------------
    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        Summon summon;
        Field field = chr.getField();
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        Option o5 = new Option();
        switch (skillID) {
            case FLORAN_HERO_WILL:
                tsm.removeAllDebuffs();
                break;
            case REPOSITION_CRYSTAL:
                Summon movingCrystal = getCrystal();
                if (movingCrystal != null && movingCrystal.getOwnerId() == chr.getId()
                        && movingCrystal.getField() == field
                        && field.getLifeByObjectID(movingCrystal.getObjectId()) == movingCrystal) {
                    inPacket.decodeInt(); // unknown
                    boolean isLeft = inPacket.decodeByte() != 0;
                    Position position = inPacket.decodePosition();
                    if (net.swordie.ms.connection.protocol.Protocol.active().version()
                            == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
                        movingCrystal.setPosition(position);
                    }
                    chr.getField().broadcast(Summoned.reposition(movingCrystal, skillID, position));
                }
                break;
            case RADIANT_JAVELIN:
            case RADIANT_JAVELIN_II:
            case RADIANT_JAVELIN_ENHANCED:
                ForceAtomEnum fae = skillID == RADIANT_JAVELIN_ENHANCED ? ForceAtomEnum.GLORY_WING_JAVELIN : ForceAtomEnum.RADIANT_JAVELIN;
                ForceAtomInfo fai = new ForceAtomInfo(chr.getNewForceAtomKey(), skillID == RADIANT_JAVELIN_II ? 2 : 1, 50, 50,
                        0, 300, Util.getCurrentTime(), 0, 0,
                        new Position(-48, 7));
                Position pos = new Position(0, 0);
                if (getCrystal() == null) {
                    pos = chr.getPosition();
                } else {
                    pos = getCrystal().getPosition();
                }
                ForceAtom fa = new ForceAtom(false, chr.getId(), chr.getId(), fae,
                        true, 0, skillID, fai, si.getFirstRect(), 0, 0,
                        pos, 0, pos, 0);

                if (skillID != RADIANT_JAVELIN_ENHANCED) {
                    // Current282 lt3/rb3: legacy DAT duplicated the primary rectangle into the last slot.
                    fa.setRect2(net.swordie.ms.connection.protocol.Protocol.active().version()
                            == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                            ? new Rect(-700, -450, 350, 450) : si.getLastRect());
                }
                chr.createForceAtom(fa);
                if (net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                        && fae == ForceAtomEnum.RADIANT_JAVELIN) {
                    registerTms282Javelin(fai.getKey(), fa, field);
                }
                if (skillID == RADIANT_JAVELIN_II) {
                    handleRadiantAtom();
                }
                break;
            case CRYSTALLINE_WINGS_FLY:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = 1000;
                o1.setInMillis(true);
                newStats.put(NewFlying, o1);
                //o2.nReason = 1;
                //o2.nValue = skillID;
                //o2.tTerm = si.getValue(time, slv);
                //newStats.put(IndieNuclearOption, o2);
                tsm.sendStat(newStats);
                break;
            case GAUNTLET_FRENZY:
                o1.nValue = si.getValue(x, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieBooster, o1);
                break;
            case HERO_OF_THE_FLORA:
                o1.nReason = skillID;
                o1.nValue = si.getValue(x, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieStatR, o1);
                break;
            case DIVINE_WRATH:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case FLASH_CRYSTAL_BATTERY:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(LefFastCharge, o1);
                break;
            case WINGS_OF_GLORY:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.xOption = 1;
                o1.cOption = 1;
                newStats.put(LefGloryWing, o1);
                o2.nReason = skillID;
                o2.nValue = 19; //Bugged
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndiePMdR, o2);
                o3.nReason = skillID;
                o3.nValue = si.getValue(indieBDR, slv);
                o3.tTerm = si.getValue(time, slv);
                newStats.put(IndieBDR, o3);
                o4.nReason = skillID;
                o4.nValue = si.getValue(indieStance, slv);
                o4.tTerm = si.getValue(time, slv);
                newStats.put(IndieStance, o4);
                o5.nOption = 1;
                o5.rOption = skillID;
                o5.tOption = si.getValue(time, slv);
                newStats.put(NewFlying, o5);
                tsm.sendStat(newStats);
                crystalSkillMap.put(CRYSTAL_SKILL_ID_WINGS_OF_GLORY, false);
                chr.getField().broadcast(Summoned.stateChanged(getCrystal(), 2, crystalSkillMap));
                chr.getField().broadcast(Summoned.upgradeStage(getCrystal(), 3)); // resets crystal attacks
                break;
            case EX:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Walk);
                summon.setAssistType(AssistType.AttackCounter);
                field.spawnSummon(summon);
                break;
            case MACHINA: // spawn at Crystal Position
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Fly);
                summon.setAssistType(AssistType.Attack);
                field.spawnSummon(summon);
                break;
            case CRYSTAL_SKILL_DEUS: // TODO
                field.getSummons().stream().filter(s -> s.getOwnerId() == chr.getId() && (s.getSkillID() == MACHINA || s.getSkillID() == EX)).forEach(field::removeLife);
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Walk);
                field.spawnSummon(summon);

                for (int i = 0; i < 5; i++) {
                    summon = Summon.getSummonByAndSetStat(chr, DEUS_SUB, slv);
                    summon.setMoveAbility(MoveAbility.Fly);
                    summon.setAssistType(AssistType.AttackCounter);
                    field.spawnAddSummon(summon);
                }

                crystalSkillMap.put(CRYSTAL_SKILL_ID_DEUS, false);
                chr.getField().broadcast(Summoned.stateChanged(getCrystal(), 2, crystalSkillMap));
                chr.getField().broadcast(Summoned.upgradeStage(getCrystal(), 3)); // resets crystal attacks
                break;
            case DEPLOY_CRYSTAL:
                // 282 Skill.wz: time = 1. The IndieBuffIcon of getSummonByAndSetStat then lasts one second and its expiry
                // removes the linked summon (live: crystal gone 1.24 s after it was made); the crystal has no term.
                Summon crystal = net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                        ? Summon.getSummonBy(chr, skillID, slv) : Summon.getSummonByAndSetStat(chr, skillID, slv);
                crystal.setMoveAbility(MoveAbility.Crystal);
                crystal.setAssistType(AssistType.AttackCounter);
                crystal.setSummonTerm(0);
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.summon = crystal;
                tsm.sendStat(IndieEmpty, o1);
                field.spawnSummon(crystal);
                resetCrystalBattery();
                chr.getField().broadcast(Summoned.upgradeStage(getCrystal(), 3)); // resets crystal attacks
                break;
            case CONVERSION_OVERDRIVE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                newStats.put(LPMagicCircuitFullDrive, o1);
                int mpRatio = (int) (chr.getCurrentMPPerc());
                int damR = si.getValue(y, slv);

                if (mpRatio <= 80) { // upto this based on mp proportions
                    damR = damR - 5;
                }
                if (mpRatio <= 60) {
                    damR = damR - 8;
                }
                if (mpRatio <= 40) {
                    damR = damR - 12;
                }
                if (mpRatio <= 20) {
                    damR = damR - 16;
                }
                if (mpRatio <= 5) {
                    damR = damR - 18;
                }
                o2.nValue = damR;
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv);
                newStats.put(IndieDamR, o2);
                tsm.sendStat(newStats);
                break;
            case TEMPLAR_KNIGHT:
                summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.CreateShootObj);
                field.spawnSummon(summon);
                break;
            case CRYSTALLINE_BULWARK:
                o1.nOption = 1;
                o1.rOption = skillID;
                int stack = 1;
                if (tsm.hasStat(CharacterTemporaryStat.LefBuffMastery)) {
                    stack = tsm.getOption(CharacterTemporaryStat.LefBuffMastery).nOption;
                    if (stack < getMaxLucentBrand()) {
                        stack++;
                    }
                }
                o1.tOption = stack;
                tsm.sendStat(NotDamaged, o1);
                break;
            case LONGINUS_ZONE:
                AffectedArea aa = AffectedArea.getPassiveAA(chr, LONGINUS_ZONE, slv);
                if (getCrystal() == null) {
                    pos = chr.getPosition();
                } else {
                    pos = getCrystal().getPosition();
                }
                aa.setPosition(pos);
                aa.setRect(aa.getPosition().getRectAround(si.getFirstRect()));
                aa.setSkillID(LONGINUS_ZONE);
                aa.setDelay((short) 18);
                chr.getField().spawnAffectedArea(aa);
                o1.nValue = 1;
                o1.nReason = skillID;
                o1.tTerm = 3;
                tsm.sendStat(IndieNotDamaged, o1);
                break;
            case CRYSTAL_GATE:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.xOption = Math.max(1, si.getValue(u, slv));
                tsm.sendStat(LefMageCrystalGate, o1);
                break;
            case CRYSTALLINE_SPIRIT:
                Summon crystalSummon = getCrystal();
                if (crystalSummon == null) {
                    chr.chatMessage("必須先放置水晶。");
                    return;
                }
                if (getShards() > 0) {
                    updateVSkillStackBuff(chr, getShards() - 1);
                } else {
                    chr.chatMessage("充能不足，無法使用。");
                    return;
                }
                Summon Shard;
                Shard = Summon.getSummonByAndSetStat(chr, skillID, slv);
                Shard.setMoveAbility(MoveAbility.Crystal);
                Shard.setAssistType(AssistType.AttackCounter);
                Shard.setSummonTerm(30);
                Shard.setPosition(crystalSummon.getPosition());
                field.spawnAddSummon(Shard);
                break;
        }
    }

    @Override
    public void handleShootObject(Char chr, ShootObjectSkillInfo sosi) {
        var skillId = sosi.getSkillId();
        var slv = sosi.getSlv();
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        switch (skillId) {
            case RADIANT_ORB:
            case RADIANT_ORB_II:
                SkillInfo si = SkillData.getSkillInfoById(skillId);
                slv = chr.getSkillLevel(skillId);
                Option o1 = new Option();
                Option o2 = new Option();
                Option o3 = new Option();
                Option o4 = new Option();
                o1.nOption = si.getValue(y, slv);
                o1.rOption = skillId;
                o1.tOption = 2000;
                o1.setInMillis(true);
                o1.skillIds = new ArrayList<>();
                o1.skillIds.add(RADIANT_JAVELIN_II);
                o1.skillIds.add(400021000); // Mana Overload
                newStats.put(NextSpecificSkillDamageUp, o1);
                //o2.nReason = 1;
                //o2.nValue = skillId;
                //o2.tTerm = 2000;
                //o2.setInMillis(true);
                //newStats.put(IndieNuclearOption, o2);

                Skill skill = getLucentBrandSkill();
                if (skill == null) {
                    return;
                }
                int stack = 1;
                if (tsm.hasStat(CharacterTemporaryStat.LefBuffMastery)) {
                    stack = tsm.getOption(CharacterTemporaryStat.LefBuffMastery).nOption;
                    if (stack < getMaxLucentBrand()) {
                        stack++;
                    }
                }
                o3.nOption = stack;
                o3.rOption = skill.getSkillId();
                o3.tOption = 2000;
                o3.xOption = skill.getSkillId();
                o3.cOption = skill.getSkillId() == LUCENT_BRAND ? 3 : skill.getSkillId() == LUCENT_BRAND_II ? 6 : 10;
                o3.setInMillis(true);
                newStats.put(LefBuffMastery, o3);
                tsm.sendStat(newStats);
                break;
        }
        super.handleShootObject(chr, sosi);
    }

    public void giveLucentBrand(TemporaryStatManager tsm, Option o3, Option o4) {
    }

    private int getMaxLucentBrand() {
        int maxStack = 0;
        for (int lucentSkillId : lucentSkills) {
            if (chr.hasSkill(lucentSkillId)) {
                SkillInfo si = SkillData.getSkillInfoById(lucentSkillId);
                maxStack = si.getValue(x, chr.getSkillLevel(lucentSkillId));
            }
        }
        return maxStack;
    }

    private Skill getLucentBrandSkill() {
        Skill skill = null;
        for (int lucentSkillId : lucentSkills) {
            if (chr.hasSkill(lucentSkillId)) {
                skill = chr.getSkill(lucentSkillId);
            }
        }
        return skill;
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(LefBuffMastery) && tsm.getOption(LefBuffMastery).nOption > 1) {
            Skill skill = chr.getSkill(AEGIS_OF_LIGHT);
            if (skill != null) {
                SkillInfo si = SkillData.getSkillInfoById(AEGIS_OF_LIGHT);
                int dmgPerc = si.getValue(x, skill.getCurrentLevel());
                int dmg = hitInfo.hpDamage;
                hitInfo.hpDamage = net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                        ? dmg - (int) ((long) dmg * dmgPerc / 100) : dmg - (dmg * (dmgPerc / 100));
                tsm.getOption(LefBuffMastery).nOption -= 1;
            }
        }
        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);
        cs.setLevel(10);
        cs.setJob(JobConstants.JobEnum.ILLIUM_1.getJobId());
        cs.setStr(4);
        cs.setDex(4);
        cs.setInt(45);
        cs.setLuk(4);
        cs.setHp(1000);
        cs.setMaxHp(1000);
        cs.setMp(500);
        cs.setMaxMp(500);
        cs.getExtendSP().addSpToJobLevel(1, 5);
    }

    @Override
    public void addItemToNewCharacter(Char chr) {
        super.addItemToNewCharacter(chr);
        chr.getAvatarData().getAvatarLook().setDrawElfEar(true);
        chr.getAvatarData().getAvatarLook().updateAvatarLookToSQL();
    }

    @Override
    public void handleJobAdvance() {
        ScriptManagerImpl sm = chr.getScriptManager();
        if (chr.getJob() == JobConstants.JobEnum.ILLIUM_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ILLIUM_2.getJobId());
                sm.giveItem(1143079);
                sm.completeQuestNoRewards(34817);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.ILLIUM_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ILLIUM_3.getJobId());
                sm.giveItem(1143080);
                sm.completeQuestNoRewards(34831);
                sm.completeQuestNoRewards(34832);
                sm.completeQuestNoRewards(34833);
                sm.completeQuestNoRewards(34834);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.ILLIUM_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ILLIUM_4.getJobId());
                sm.giveItem(1143081);
                sm.completeQuestNoRewards(34842);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }

    @Override
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        if (cts == CharacterTemporaryStat.CrystalChargeBuffIcon) {
            changeCrystalCharge(0);
            resetCrystalBattery();
        }
    }
}
