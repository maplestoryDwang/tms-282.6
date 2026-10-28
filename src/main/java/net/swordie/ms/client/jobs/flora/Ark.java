package net.swordie.ms.client.jobs.flora;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.items.BodyPart;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.skills.ForceAtom;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.*;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.constants.FieldConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.enums.ForceAtomEnum;
import net.swordie.ms.life.AffectedArea;
import net.swordie.ms.life.Wreckage;
import net.swordie.ms.life.mob.Mob;
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

public class Ark extends Job {

    public static final int SPELL_BULLETS = 155001103;
    public static final int SPECTER_STATE = 155000007;
    public static final int CORRUPTION_COOLDOWN = 155001008;

    public static final int BASIC_CHARGE_DRIVE_ATTACK = 155001100;
    public static final int BASIC_CHARGE_DRIVE_ATOM = 155001000;
    public static final int BASIC_CHARGE_DRIVE_BUFF = 155001001;

    public static final int SCARLET_CHARGE_DRIVE_ATTACK_1 = 155101100;
    public static final int SCARLET_CHARGE_DRIVE_ATTACK_2 = 155101013;
    public static final int SCARLET_CHARGE_DRIVE_ATTACK_COMBO_1 = 155101101;
    public static final int SCARLET_CHARGE_DRIVE_ATTACK_COMBO_2 = 155101015;
    public static final int SCARLET_CHARGE_DRIVE_ATOM = 155101002;
    public static final int SCARLET_CHARGE_DRIVE_BUFF = 155101003;

    public static final int GUST_CHARGE_DRIVE_ATTACK = 155111102;
    public static final int GUST_CHARGE_DRIVE_ATTACK_COMBO = 155111111;
    public static final int GUST_CHARGE_DRIVE_ATOM = 155111003;
    public static final int GUST_CHARGE_DRIVE_BUFF = 155111005;

    public static final int ABYSSAL_CHARGE_DRIVE_ATTACK = 155121102;
    public static final int ABYSSAL_CHARGE_DRIVE_ATOM = 155121003;
    public static final int ABYSSAL_CHARGE_DRIVE_TILE = 155121004;
    public static final int ABYSSAL_CHARGE_DRIVE_BUFF = 155121005;

    public static final int OMINOUS_NIGHTMARE = 155001102;
    public static final int VIVID_NIGHTMARE = 155110000;
    public static final int ENDLESS_NIGHTMARE = 155120000;

    public static final int KNUCKLE_BOOSTER_ARK = 155101005;
    public static final int MASTER_CORRUPTION = 155101006;
    public static final int IMPENDING_DEATH_ATOM = 155100009;
    public static final int IMPENDING_DEATH = 155101008;

    public static final int BOUNDLESS_HORROR = 155111006;
    public static final int CREEPING_TERROR = 155111306;
    public static final int VENGEFUL_HATE = 155111207;

    public static final int HERO_OF_THE_FLORA = 155121008;
    public static final int FLORAN_HEROS_WILL = 155121009;
    public static final int BLISSFUL_RESTRAINT_TILE = 155121006;
    public static final int BLISSFUL_RESTRAINT_ATTACK = 155121306;
    public static final int ENDLESS_DREAM = 155120001;
    public static final int ENHANCED_SPECTRA = 155120034;

    // Hyper Skills
    public static final int DIVINE_WRATH = 155121042;
    public static final int CHARGE_SPELL_AMPLIFIER = 155121043;
    public static final int ENDLESS_AGONY = 155121341;

    // V skills
    public static final int ABYSSAL_RECALL = 400051334;
    public static final int INFINITY_SPELL = 400051036;
    public static final int NIGHTMARES_ESCAPE = 400051047;
    public static final int DREAMS_ESCAPE = 400051048;
    public static final int ENDLESSLY_STARVING_BEAST = 400051080;

    // HEXA Boosts
    public static final int HEXA_INFINITY_SPELL = 500061013;

    private long lastSpectraEnergy = 0L;
    private boolean tms282SpectraClockStarted;

    /** CP02A5 carries a snapshot; only a server-timed, owned Ark pulse can advance the existing resource logic. */
    public boolean pulseTms282SpectraEnergy(long now) {
        if(net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                || !JobConstants.isArk(chr.getJob()) || !chr.hasSkill(SPECTER_STATE) || chr.getHP()<=0)
            throw new IllegalArgumentException("Unowned Ark resource pulse");
        if(tms282SpectraClockStarted && now-lastSpectraEnergy<1000L)return false;
        if(tms282SpectraClockStarted)lastSpectraEnergy=now-(now-lastSpectraEnergy)%1000L;
        else { tms282SpectraClockStarted=true; lastSpectraEnergy=now; }
        modifySpectraEnergy();
        return true;
    }
    List<CharacterTemporaryStat> spellCasts = Arrays.asList(SpellBullet_Abyss, SpellBullet_Gust, SpellBullet_Scarlet, SpellBullet_Plain);

    private final int[] addedSkills = new int[]{
            MASTER_CORRUPTION,
            SPECTER_STATE
    };

    public enum SpellChargeType {
        Basic(15500),
        Scarlet(15510),
        Gust(15511),
        Abyssal(15512),
        ;
        private final int val;
        private static final int MIN_VAL = 15500;
        private static final int MAX_VAL = 15512;
        private static final SpellChargeType[] CACHE;

        static {
            CACHE = new SpellChargeType[MAX_VAL - MIN_VAL + 1];
            for (SpellChargeType t : values()) {
                CACHE[t.val - MIN_VAL] = t;
            }
        }

        SpellChargeType(int val) {
            this.val = val;
        }

        public static SpellChargeType getByVal(int val) {
            if (val < MIN_VAL || val > MAX_VAL) {
                return null;
            }
            return CACHE[val - MIN_VAL];
        }
    }

    public Ark(Char chr) {
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
        return JobConstants.isArk(id);
    }

    private int getCurrentChargeCount() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int count = 0;
        if (tsm.hasStat(SpellBullet_Plain)) {
            count += (tsm.getOption(SpellBullet_Plain).xOption / 2);
        }
        if (tsm.hasStat(SpellBullet_Scarlet)) {
            count += tsm.getOption(SpellBullet_Scarlet).xOption;
        }
        if (tsm.hasStat(SpellBullet_Gust)) {
            count += tsm.getOption(SpellBullet_Gust).xOption;
        }
        if (tsm.hasStat(SpellBullet_Abyss)) {
            count += tsm.getOption(SpellBullet_Abyss).xOption;
        }
        return count;
    }

    private void resetCharges() {
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Option o4 = new Option();
        newStats.put(SpellBullet_Plain, o1);
        newStats.put(SpellBullet_Scarlet, o2);
        newStats.put(SpellBullet_Gust, o3);
        newStats.put(SpellBullet_Abyss, o4);
        tsm.sendStat(newStats);
    }

    public void modifySpectraEnergy() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int delta = chr.hasSkill(ENHANCED_SPECTRA) ? 8 : 7;
        if (tsm.hasStat(SpecterMode)) {
            delta = -9;
        } else if (tsm.hasStat(LPSpellAmplification)) {
            delta += delta * 2;
        }
        int currentEnergy = tsm.getOption(SpecterGauge).xOption;
        if (chr.hasSkillOnCooldown(CORRUPTION_COOLDOWN)) { // if Player has Spectra Fatigue
            return;
        }
        if (tsm.hasStat(LPHoldSpecterGauge)) {
            updateSpectraEnergy(currentEnergy - 1);
        } else {
            updateSpectraEnergy(currentEnergy + delta);
        }
    }

    private void updateSpectraEnergy(int spectraEnergy) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o = new Option();
        o.nOption = 1;
        o.xOption = (spectraEnergy > 1000 ? 1000 : (Math.max(spectraEnergy, 0)));
        tsm.sendStat(SpecterGauge, o);
        if (o.xOption <= 0) { // Spectra Fatigue
            SkillInfo si = SkillData.getSkillInfoById(CORRUPTION_COOLDOWN);
            tsm.removeStatsBySkill(SPECTER_STATE);
            Tms282ArkFlight.onSpecterExit(chr);
            chr.addSkillCooldown(CORRUPTION_COOLDOWN, si.getValue(cooltime, 1) * 1000);
        } else if (o.xOption >= 1000 && !chr.hasSkill(MASTER_CORRUPTION)) { // If Player doesn't have Control over their Specter State yet.
            changeSpecterState();
        }
    }

    private void changeSpecterState() {
        Skill skill = chr.getSkill(SPECTER_STATE);
        if (skill == null) {
            return;
        }

        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStat(SpecterMode)) {
            SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
            int slv = skill.getCurrentLevel();
            EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
            Option o1 = new Option();
            Option o2 = new Option();
            Option o3 = new Option();
            o1.nReason = SPECTER_STATE;
            o1.nValue = si.getValue(indiePad, slv);
            newStats.put(IndiePAD, o1);
            o2.nReason = SPECTER_STATE;
            o2.nValue = si.getValue(indieStance, slv);
            newStats.put(IndieStance, o2);
            o3.nOption = 1;
            o3.rOption = SPECTER_STATE;
            newStats.put(SpecterMode, o3);
            tsm.sendStat(newStats);
        } else {
            tsm.removeStatsBySkill(SPECTER_STATE);
            Tms282ArkFlight.onSpecterExit(chr);
        }
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        Char chr = c.getChr();
        switch (skillID) {
            case ENDLESS_DREAM:
                if (chr.hasSkill(NIGHTMARES_ESCAPE)) {
                    int vSkill = NIGHTMARES_ESCAPE;
                    if (chr.getTemporaryStatManager().hasStat(SpecterMode)) {
                        vSkill = DREAMS_ESCAPE;
                    }
                    if (chr.hasSkillOnCooldown(vSkill)) {
                        return;
                    }
                    chr.write(UserLocal.userBonusAttackRequest(vSkill));
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
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                != net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && now - lastSpectraEnergy >= 5000L) {
            modifySpectraEnergy();
            lastSpectraEnergy = now;
        }
        if (hasHitMobs
                && !SkillConstants.isForceAtomSkill(attackInfo.skillId)
                && attackInfo.skillId != ABYSSAL_RECALL
                && attackInfo.skillId != CONVERSION_OVERDRIVE_ATTACK) {
            createImpendingDeathForceAtom();
            bonusConversionOverdriveAttack();
        }
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        Field field = chr.getField();
        switch (attackInfo.skillId) {
            case IMPENDING_DEATH_ATOM:
                spawnWreckage(attackInfo);
                break;
            case BASIC_CHARGE_DRIVE_ATOM:
                if (hasHitMobs) {
                    Skill skill2 = chr.getSkill(BASIC_CHARGE_DRIVE_ATTACK);
                    SkillInfo si2 = SkillData.getSkillInfoById(BASIC_CHARGE_DRIVE_BUFF);
                    int slv2 = (byte) skill2.getCurrentLevel();
                    o1.nReason = BASIC_CHARGE_DRIVE_BUFF;
                    o1.nValue = si2.getValue(speed, slv2);
                    o1.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndieSpeed, o1);
                    o2.nReason = BASIC_CHARGE_DRIVE_BUFF;
                    o2.nValue = si2.getValue(indieStance, slv2);
                    o2.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndieStance, o2);
                    tsm.sendStat(newStats);
                }
                break;
            case SCARLET_CHARGE_DRIVE_ATOM:
                if (hasHitMobs) {
                    Skill skill2 = chr.getSkill(SCARLET_CHARGE_DRIVE_ATTACK_1);
                    SkillInfo si2 = SkillData.getSkillInfoById(SCARLET_CHARGE_DRIVE_BUFF);
                    int slv2 = (byte) skill2.getCurrentLevel();
                    o1.nReason = SCARLET_CHARGE_DRIVE_BUFF;
                    o1.nValue = si2.getValue(indiePad, slv2);
                    o1.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndiePAD, o1);
                    o2.nReason = SCARLET_CHARGE_DRIVE_BUFF;
                    o2.nValue = si2.getValue(indieCr, slv2);
                    o2.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndieCrR, o2);
                    tsm.sendStat(newStats);
                }
                break;
            case GUST_CHARGE_DRIVE_ATOM:
                if (hasHitMobs) {
                    Skill skill2 = chr.getSkill(GUST_CHARGE_DRIVE_ATTACK);
                    SkillInfo si2 = SkillData.getSkillInfoById(GUST_CHARGE_DRIVE_BUFF);
                    int slv2 = (byte) skill2.getCurrentLevel();
                    o1.nReason = GUST_CHARGE_DRIVE_BUFF;
                    o1.nValue = -1;
                    o1.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndieBooster, o1);
                    o2.nReason = GUST_CHARGE_DRIVE_BUFF;
                    o2.nValue = si2.getValue(indieEvaR, slv2);
                    o2.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndieEVAR, o2);
                    tsm.sendStat(newStats);
                }
                break;
            case ABYSSAL_CHARGE_DRIVE_ATOM:
                if (hasHitMobs) {
                    // Buff
                    if (tsm.hasStatBySkillId(ABYSSAL_CHARGE_DRIVE_BUFF)) {
                        tsm.removeStatsBySkill(ABYSSAL_CHARGE_DRIVE_BUFF);
                    }
                    Skill skill2 = chr.getSkill(ABYSSAL_CHARGE_DRIVE_ATTACK);
                    SkillInfo si2 = SkillData.getSkillInfoById(ABYSSAL_CHARGE_DRIVE_BUFF);
                    int slv2 = (byte) skill2.getCurrentLevel();
                    o1.nReason = ABYSSAL_CHARGE_DRIVE_BUFF;
                    o1.nValue = si2.getValue(indieDamR, slv2);
                    o1.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndieDamR, o1);
                    o2.nReason = ABYSSAL_CHARGE_DRIVE_BUFF;
                    o2.nValue = si2.getValue(indieBDR, slv2);
                    o2.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndieBDR, o2);
                    o3.nReason = ABYSSAL_CHARGE_DRIVE_BUFF;
                    o3.nValue = si2.getValue(indieIgnoreMobpdpR, slv2);
                    o3.tTerm = si2.getValue(time, slv2);
                    newStats.put(IndieIgnoreMobpdpR, o3);
                    tsm.sendStat(newStats);

                    // Tile
                    field = chr.getField();
                    Mob mob = Util.getRandomFromCollection(new ArrayList<>() {{
                        attackInfo.mobAttackInfo.forEach(mai -> add((Mob) chr.getField().getLifeByObjectID(mai.mobId)));
                    }});
                    SkillInfo rca = SkillData.getSkillInfoById(ABYSSAL_CHARGE_DRIVE_TILE);
                    AffectedArea aa = AffectedArea.getAffectedArea(chr, attackInfo);
                    aa.setSkillID(ABYSSAL_CHARGE_DRIVE_TILE);
                    aa.setPosition(mob != null ? mob.getPosition() : chr.getPosition());
                    Rect rect = aa.getPosition().getRectAround(rca.getFirstRect());
                    aa.setRect(rect);
                    field.spawnAffectedArea(aa);
                }
                break;

            // Gain Energy Charge on Attack
            case BASIC_CHARGE_DRIVE_ATTACK:
            case SCARLET_CHARGE_DRIVE_ATTACK_1:
            case SCARLET_CHARGE_DRIVE_ATTACK_2:
            case SCARLET_CHARGE_DRIVE_ATTACK_COMBO_1:
            case SCARLET_CHARGE_DRIVE_ATTACK_COMBO_2:
            case GUST_CHARGE_DRIVE_ATTACK:
            case GUST_CHARGE_DRIVE_ATTACK_COMBO:
            case ABYSSAL_CHARGE_DRIVE_ATTACK:
                if (hasHitMobs) {
                    addSpellCharge(attackInfo.skillId);
                    if (tsm.hasStat(LPInfinitySpell)) {
                        for (int i = 0; i < 4; i++) {
                            addSpellCharge(BASIC_CHARGE_DRIVE_ATTACK);
                        }
                    }
                }
                break;
            case 155101200: // Grievous Wound
                chr.setSkillCooldown(155101200, slv);
                break;
            case 155101204: // Tenacious Instinct
                chr.setSkillCooldown(155101104, slv);
                break;
            case 155111212: // Insatiable Hunger
                chr.setSkillCooldown(155111202, slv);
                break;
            case 155121202: // Unbridled Chaos
                chr.setSkillCooldown(155121202, slv);
                break;
            case 155101104: // Unstoppable Impulse
                chr.setSkillCooldown(155101104, slv);
                break;
            case OMINOUS_NIGHTMARE:
            case VIVID_NIGHTMARE:
            case ENDLESS_NIGHTMARE:
                chr.addSkillCooldown(OMINOUS_NIGHTMARE, 2000);
            case NIGHTMARES_ESCAPE:
            case DREAMS_ESCAPE:
                si = SkillData.getSkillInfoById(NIGHTMARES_ESCAPE);
                slv = chr.getSkillLevel(NIGHTMARES_ESCAPE);
                chr.addSkillCooldown(attackInfo.skillId, (int) (si.getValue(cooltime, slv) * 1000L));
                break;
            case CREEPING_TERROR:
                if (!tsm.hasStat(NotDamaged)) {
                    o1.nOption = 1;
                    o1.rOption = attackInfo.skillId;
                    o1.tOption = 2;
                    tsm.sendStat(NotDamaged, o1);
                }
                break;
            case BOUNDLESS_HORROR:
                tsm.removeStatsBySkill(CREEPING_TERROR);
                if (!tsm.hasStat(SpecterMode)) {
                    changeSpecterState();
                }
                chr.setSkillCooldown(CREEPING_TERROR, slv);
                break;
            case ABYSSAL_RECALL:
                if (!tsm.hasStat(LPHoldSpecterGauge)) {
                    o2.nValue = 1;
                    o2.nReason = skillID;
                    o2.tTerm = 10;
                    tsm.sendStat(IndieNotDamaged, o2); // Invincibility
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = 30;
                    tsm.sendStat(LPHoldSpecterGauge, o1);
                    if (!tsm.hasStat(SpecterMode)) {
                        changeSpecterState();
                    }
                }
                break;
            case ENDLESS_AGONY:
                if (!tsm.hasStat(SpecterMode)) {
                    changeSpecterState();
                }
                break;
            case BLISSFUL_RESTRAINT_ATTACK:
                if (!tsm.hasStat(SpecterMode)) {
                    changeSpecterState();
                }
                SkillInfo rca = SkillData.getSkillInfoById(BLISSFUL_RESTRAINT_TILE);
                AffectedArea aa = AffectedArea.getAffectedArea(chr, attackInfo);
                aa.setSkillID(BLISSFUL_RESTRAINT_TILE);
                aa.setPosition(chr.getPosition());
                Rect rect = aa.getPosition().getRectAround(rca.getRects().get(0));
                aa.setRect(rect);
                field.spawnAffectedArea(aa);
                chr.addSkillCooldown(BLISSFUL_RESTRAINT_ATTACK, 1000 * 180);
                break;
        }
    }

    private void addSpellCharge(int skillId) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        if (!tsm.hasStat(SpecterMode) && getCurrentChargeCount() < 5) {
            SpellChargeType spellChargeType = SpellChargeType.getByVal(skillId / 10000);
            switch (spellChargeType) {
                case Basic:
                    o1.xOption = tsm.hasStat(SpellBullet_Plain) ? tsm.getOption(SpellBullet_Plain).xOption > 10 ? 10 : tsm.getOption(SpellBullet_Plain).xOption + 2 : 2;
                    tsm.sendStat(SpellBullet_Plain, o1);
                    break;
                case Scarlet:
                    o1.xOption = 1;
                    tsm.sendStat(SpellBullet_Scarlet, o1);
                    break;
                case Gust:
                    o1.xOption = 1;
                    tsm.sendStat(SpellBullet_Gust, o1);
                    break;
                case Abyssal:
                    o1.xOption = 1;
                    tsm.sendStat(SpellBullet_Abyss, o1);
                    break;
            }
        }
    }

    private void spawnWreckage(AttackInfo attackInfo) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!chr.hasSkill(VENGEFUL_HATE) || !tsm.hasStat(SpecterMode) || !tsm.hasStat(ComingDeath)) {
            return;
        }
        Field field = chr.getField();
        Skill skill = chr.getSkill(VENGEFUL_HATE);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();
        if (Util.succeedProp(si.getValue(s, slv)) && field.getWreckageByChrId(chr.getId()).size() < si.getValue(z, slv)) {
            for (MobAttackInfo mai : attackInfo.mobAttackInfo) {
                Mob mob = (Mob) chr.getField().getLifeByObjectID(mai.mobId);
                if (mob == null || mob.getHp() <= 0) {
                    continue;
                }
                final Position pos = mob.getPosition();
                Wreckage wreckage = Wreckage.getWreckageBy(chr, skill.getSkillId(), pos, si.getValue(q, slv) * 1000, 0);
                field.spawnWreckage(chr, wreckage);
            }
        }
    }

    @Override
    public void handleSkill(Client c, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(c, inPacket, skillUseInfo);
        Char chr = c.getChr();
        SkillInfo si = skillUseInfo.skillInfo;
        int slv = skillUseInfo.slv;
        int skillID = skillUseInfo.skillID;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Option o1 = new Option();
        switch (skillID) {
            case KNUCKLE_BOOSTER_ARK:
                o1.nValue = si.getValue(x, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieBooster, o1);
                break;
            case IMPENDING_DEATH:
                if (tsm.hasStat(ComingDeath)) {
                    tsm.removeStatsBySkill(IMPENDING_DEATH);
                } else {
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(time, slv);
                    tsm.sendStat(ComingDeath, o1);
                }
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
            case CHARGE_SPELL_AMPLIFIER:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(LPSpellAmplification, o1);
                break;
            case INFINITY_SPELL:
            case HEXA_INFINITY_SPELL:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(LPInfinitySpell, o1);
                break;
            case SPELL_BULLETS:
                if (!chr.hasSkill(SPELL_BULLETS) || tsm.hasStat(SpecterMode)) {
                    return;
                }
                createSpellBulletForceAtom();
                break;
            case FLORAN_HEROS_WILL:
                tsm.removeAllDebuffs();
                break;
            case VENGEFUL_HATE:
                List<Wreckage> wreckageList = chr.getField().getWreckageByChrId(chr.getId());
                createVengefulHateForceAtom(wreckageList);
                break;
            case MASTER_CORRUPTION:
                if (chr.hasSkill(MASTER_CORRUPTION) && tsm.hasStat(SpecterGauge) && tsm.getOption(SpecterGauge).xOption > 0) {
                    changeSpecterState();
                } else {
                    chr.chatMessage("侵蝕疲勞中，無法進入幽靈狀態。");
                }
                break;
        }
    }

    private void createSpellBulletForceAtom() {
        if (!chr.hasSkill(SPELL_BULLETS)) {
            return;
        }
        Skill skill = chr.getSkill(SPELL_BULLETS);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();
        Rect rect = chr.getPosition().getRectAround(si.getFirstRect());
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        int i = new Random().nextBoolean() ? 900 : 700;
        ForceAtom abyssalFA = new ForceAtom(true, 0, chr.getId(), ForceAtomEnum.ABYSSAL_CHARGE,
                true, new ArrayList<>(), ABYSSAL_CHARGE_DRIVE_ATOM, new ArrayList<>(), new Rect(), 0, 0,
                new Position(), ABYSSAL_CHARGE_DRIVE_ATOM, new Position(), 0);
        ForceAtom gustFA = new ForceAtom(true, 0, chr.getId(), ForceAtomEnum.GUST_CHARGE,
                true, new ArrayList<>(), GUST_CHARGE_DRIVE_ATOM, new ArrayList<>(), new Rect(), 0, 0,
                new Position(), GUST_CHARGE_DRIVE_ATOM, new Position(), 0);
        ForceAtom scarletFA = new ForceAtom(true, 0, chr.getId(), ForceAtomEnum.SCARLET_CHARGE,
                true, new ArrayList<>(), SCARLET_CHARGE_DRIVE_ATOM, new ArrayList<>(), new Rect(), 0, 0,
                new Position(), SCARLET_CHARGE_DRIVE_ATOM, new Position(), 0);
        ForceAtom basicFA = new ForceAtom(true, 0, chr.getId(), ForceAtomEnum.BASIC_CHARGE,
                true, new ArrayList<>(), BASIC_CHARGE_DRIVE_ATOM, new ArrayList<>(), new Rect(), 0, 0,
                new Position(), BASIC_CHARGE_DRIVE_ATOM, new Position(), 0);

        List<ForceAtom> forceAtoms = new ArrayList<>();
        for (CharacterTemporaryStat cast : spellCasts) {
            if (cast == null || !tsm.hasStat(cast)) {
                continue;
            }
            for (int j = 0; j < tsm.getOption(cast).xOption; j++) {
                int firstImpact = new Random().nextInt(15) + 35;
                int secondImpact = new Random().nextInt(2) + 5;
                int delay = new Random().nextInt(400) + 500;
                int angle = new Random().nextInt(20) + 50;

                Mob mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
                ForceAtomInfo fai = new ForceAtomInfo(i, i, firstImpact, secondImpact,
                        angle, delay, Util.getCurrentTime(), 0, 0,
                        new Position());

                switch (cast) {
                    case SpellBullet_Scarlet: // Scarlet Charge
                        scarletFA.getTargetIdList().add(mob != null ? mob.getObjectId() : 0);
                        scarletFA.getFaiList().add(fai);
                        forceAtoms.add(scarletFA);
                        break;
                    case SpellBullet_Gust: // Gust Charge
                        gustFA.getTargetIdList().add(mob != null ? mob.getObjectId() : 0);
                        gustFA.getFaiList().add(fai);
                        forceAtoms.add(gustFA);
                        break;
                    case SpellBullet_Abyss: // Abyssal Charge
                        abyssalFA.getTargetIdList().add(mob != null ? mob.getObjectId() : 0);
                        abyssalFA.getFaiList().add(fai);
                        forceAtoms.add(abyssalFA);
                        break;
                    default:    // Basic Charge
                        basicFA.getTargetIdList().add(mob != null ? mob.getObjectId() : 0);
                        basicFA.getFaiList().add(fai);
                        forceAtoms.add(basicFA);
                        break;
                }
                i++;
            }
        }
        if (forceAtoms.size() > 0) {
            field.broadcast(FieldPacket.createArkForceAtom(chr.getId(), SPELL_BULLETS, forceAtoms));
            resetCharges();
        }
    }

    private void createImpendingDeathForceAtom() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (!tsm.hasStatBySkillId(SPECTER_STATE) || !chr.hasSkill(IMPENDING_DEATH) || !tsm.hasStat(ComingDeath)) {
            return;
        }
        Field field = chr.getField();
        Skill skill = chr.getSkill(IMPENDING_DEATH);
        SkillInfo si = SkillData.getSkillInfoById(skill.getSkillId());
        int slv = skill.getCurrentLevel();

        Rect rect = chr.getPosition().getRectAround(SkillData.getSkillInfoById(IMPENDING_DEATH_ATOM).getFirstRect());
        if (!chr.isLeft()) {
            rect = rect.horizontalFlipAround(chr.getPosition().getX());
        }
        List<ForceAtomInfo> faiList = new ArrayList<>();
        List<Integer> targetIdList = new ArrayList<>();
        int firstImpact = new Random().nextInt(15) + 35;
        int secondImpact = new Random().nextInt(2) + 5;
        ForceAtomEnum fae = ForceAtomEnum.IMPENDING_DEATH;
        int bulletCount = si.getValue(z, slv) + (tsm.hasStat(LPInfinitySpell) ? 3 : 0);
        for (int i = 0; i < bulletCount; i++) {
            Mob mob = Util.getRandomFromCollection(field.getMobsInRect(rect));
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(1, fae.getInc(), firstImpact, secondImpact,
                    270, 100, Util.getCurrentTime(), 0, 0,
                    new Position());

            targetIdList.add(mob != null ? mob.getObjectId() : 0);
            faiList.add(forceAtomInfo);
        }
        chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                true, targetIdList, IMPENDING_DEATH_ATOM, faiList, new Rect(), 0, 300,
                new Position(), IMPENDING_DEATH_ATOM, new Position(), 0));
    }

    private void createVengefulHateForceAtom(List<Wreckage> wreckageList) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Field field = chr.getField();
        if (!chr.hasSkill(VENGEFUL_HATE) || !tsm.hasStat(SpecterMode) || !tsm.hasStat(ComingDeath)) {
            return;
        }
        for (Wreckage wreckage : wreckageList) {
            int firstImpact = new Random().nextInt(30) + 330;
            int secondImpact = new Random().nextInt(5) + 60;
            ForceAtomEnum fae = ForceAtomEnum.VENGEFUL_HATE;
            Mob mob = Util.getRandomFromCollection(field.getMobs());
            ForceAtomInfo forceAtomInfo = new ForceAtomInfo(1, fae.getInc(), firstImpact, secondImpact,
                    0, 500, Util.getCurrentTime(), 8, 0,
                    wreckage.getPosition());
            chr.createForceAtom(new ForceAtom(false, 0, chr.getId(), fae,
                    false, new ArrayList<>(), VENGEFUL_HATE, Collections.singletonList(forceAtomInfo), new Rect(), 0, 300,
                    chr.getPosition(), VENGEFUL_HATE, mob == null || mob.getHp() <= 0 ? new Position() : mob.getPosition(), 0));
        }
        if (wreckageList.size() > 0) {
            field.removeWreckage(chr, wreckageList);
        }
    }

    @Override
    public void handleHit(Client c, InPacket inPacket, HitInfo hitInfo) {

        super.handleHit(c, inPacket, hitInfo);
    }

    @Override
    public void handleCancelTimer(Char chr) {
        super.handleCancelTimer(chr);
    }

    @Override
    public void setCharCreationStats(Char chr) {
        super.setCharCreationStats(chr);
        CharacterStat cs = chr.getAvatarData().getCharacterStat();
        cs.setPosMap(FieldConstants.HOME_MAP);//cs.setPosMap(FieldConstants.HENESYS_ID);
        cs.setLevel(10);
        cs.setJob(JobConstants.JobEnum.ARK_1.getJobId());
        cs.setStr(45);
        cs.setDex(4);
        cs.setInt(4);
        cs.setLuk(4);
        cs.setHp(400);
        cs.setMaxHp(400);
        cs.setMp(200);
        cs.setMaxMp(200);
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
        if (chr.getJob() == JobConstants.JobEnum.ARK_1.getJobId()) {
            if (chr.getLevel() < 30) {
                sm.sendSayOkay("#e這個職業需要達到#r30#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ARK_2.getJobId());
                sm.giveItem(1143099);
                sm.completeQuestNoRewards(34902);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.ARK_2.getJobId()) {
            if (chr.getLevel() < 60) {
                sm.sendSayOkay("#e這個職業需要達到#r60#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ARK_3.getJobId());
                sm.giveItem(1143100);
                sm.completeQuestNoRewards(34903);
            }
        } else if (chr.getJob() == JobConstants.JobEnum.ARK_3.getJobId()) {
            if (chr.getLevel() < 100) {
                sm.sendSayOkay("#e這個職業需要達到#r100#k等以上才能轉職");
                return;
            }
            if (sm.sendAskYesNo("#e要跳過轉職任務嗎？")) {
                if (sm.getEmptyInventorySlots(1) < 1) {
                    sm.sendSayOkay("#e請在裝備欄空出更多空間。");
                    return;
                }
                sm.jobAdvance(JobConstants.JobEnum.ARK_4.getJobId());
                sm.giveItem(1143101);
                sm.completeQuestNoRewards(34904);
            }
        } else {
            sm.sendSayOkay("#e目前的狀態無法轉職。");
        }
    }
}
