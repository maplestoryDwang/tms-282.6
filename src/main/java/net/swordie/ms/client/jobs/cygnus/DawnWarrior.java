package net.swordie.ms.client.jobs.cygnus;

import net.swordie.ms.client.character.skills.*;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.QuestConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.info.HitInfo;
import net.swordie.ms.client.character.skills.info.AttackInfo;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.enums.AssistType;
import net.swordie.ms.enums.MoveAbility;
import net.swordie.ms.life.Summon;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.MobTemporaryStat;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.scripts.ScriptManagerImpl;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static net.swordie.ms.client.character.skills.SkillStat.*;
import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.*;
import static net.swordie.ms.client.jobs.cygnus.DawnWarrior.PoseType.*;

public class DawnWarrior extends Noblesse {

    public static final int ELEMENTAL_HARMONY_STR = 10000246;

    public static final int FALLING_MOON = 11001024;
    public static final int RISING_SUN = 11001025;

    public static final int SOUL_ELEMENT = 11001022; // ElementSoul
    public static final int SOUL_ELEMENT_BUFF = 11001027; // CosmicOrb
    public static final int SOUL_ELEMENT_ORB = 11001030;
    /** When the Solar/Luna skill opposite to the previous skill that hit the target also lands a hit, #prop% chance to
    * create 1 cosmic orb that lasts #y sec
    * Max Cosmic Orb Stacks: #x, duration of all cosmic orbs reset upon creation
    * When cosmic orb is created, Attack Power increases by #u2 for #u sec */

    public static final int LUNA_DIVIDE = 11001126;
    public static final int SOLAR_SLASH = 11001226;

    public static final int COSMIC_MATTER = 11101029;
    public static final int EQUINOX_CYCLE = 11101031; //Buff
    public static final int EQUINOX_CYCLE_MOON = 11101032;
    public static final int EQUINOX_CYCLE_SUN = 11101033;
    public static final int SOUL_BLESSING = 11100034;

    public static final int TRUE_SIGHT = 11111023; //Buff (Mob Def Debuff & Final DmgUp Debuff)
    public static final int TRUE_SIGHT_PERSIST = 11120043;
    public static final int WILL_OF_STEEL = 11110025;
    public static final int COSMIC_SHOWER = 11111029; // cosmicShower
    public static final int SOUL_BLESSING_II = 11110031;

    public static final int IMPALING_RAYS = 11121004; //Special Attack (Incapacitate Debuff)
    public static final int IMPALING_RAYS_EXPLOSION = 11121013;
    public static final int EQUINOX_SLASH = 11121014;
    public static final int CALL_OF_CYGNUS = 11121000;
    public static final int MASTER_OF_THE_SWORD = 11120009;
    public static final int MASTER_OF_THE_SWORD_BUFF = 11120010;
    public static final int CRESCENT_DIVIDE = 11121103;
    public static final int COSMIC_BURST = 11121018;
    public static final int SOUL_BLESSING_III = 11120019;

    public static final int GLORY_OF_THE_GUARDIANS = 11121053;
    public static final int COSMIC_FORGE = 11121054; // CosmicForge
    public static final int BLAZING_ASSAULT = 11121257;
    public static final int LUSTER_CHARGE = 11121157;

    // V Skills
    public static final int RIFT_OF_DAMNATION = 400011055;
    public static final int RIFT_OF_DAMNATION_ATTACK = 400011056;
    public static final int RIFT_OF_DAMNATION_SUMMON = 400011065;
    public static final int SOUL_ECLIPSE = 400011088;
    public static final int SOUL_ECLIPSE_BREAK = 400011089;
    public static final int FLARE_SLASH_SUN = 400011048; // Extra Skill
    public static final int FLARE_SLASH_MOON = 400011049; // Extra Skill
    public static final int COSMOS = 400011142;

    // HEXA Skills
    public static final int ASTRAL_BLITZ = 11141500;
    public static final int ASTRAL_BLITZ_BUFF = 11141501; // Increases final damage by #y% for #time seconds when used
    public static final int HEXA_LUNA_DIVIDE = 11141100;
    public static final int HEXA_SOLAR_SLASH = 11141200;
    public static final int EQUINOX_POWER = 11141003;
    public static final int EQUINOX_POWER_EXTRA = 11141000;
    public static final int EQUINOX_POWER_SUN = 11141001;
    public static final int HEXA_COSMIC_SHOWER = 11141002;
    public static final int HEXA_COSMIC_BURST = 11141004;
    public static final int HEXA_EQUINOX_SLASH = 11141005;
    public static final int EQUINOX_POWER_II = 11141006; // Second Atoms

    private final int[] addedSkills = new int[]{
            ELEMENTAL_HARMONY_STR
    };

    private long wosTime = 0L;

    public DawnWarrior(Char chr) {
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
        return JobConstants.isDawnWarrior(id);
    }

    @Override
    public void update(long now) {
        super.update(now);
        handleWillOfSteel(now);
    }

    // Attack related methods ------------------------------------------------------------------------------------------
    @Override
    public void handleDebuffOnMob(Client c, Mob mob, SkillInfo si, int skillID, int slv, long damage) {
        switch (skillID) {
            case IMPALING_RAYS:
                if (Util.succeedProp(si.getValue(prop, slv))) {
                    Option o1 = new Option();
                    Option o2 = new Option();
                    MobTemporaryStat mts = mob.getTemporaryStat();
                    EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
                    o1.nOption = 1;
                    o1.rOption = skillID;
                    o1.tOption = si.getValue(SkillStat.time, slv);
                    map.put(MobStat.Freeze, o1);
                    o2.nOption = si.getValue(x, slv);
                    o2.rOption = skillID;
                    o2.tOption = si.getValue(SkillStat.time, slv);
                    o2.wOption = chr.getId();
                    map.put(MobStat.SoulExplosion, o2);
                    mts.addStatOptions(mob, map);
                }
                break;
        }
        super.handleDebuffOnMob(c, mob, si, skillID, slv, damage);
    }

    @Override
    public void handleAttack(Client c, AttackInfo attackInfo, SkillInfo si, long now) {
        Char chr = c.getChr();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int skillID = attackInfo.skillId;
        int slv = attackInfo.slv;
        boolean hasHitMobs = !attackInfo.mobAttackInfo.isEmpty();
        if (hasHitMobs) {
            if (SkillConstants.isLunaSkill(skillID) || SkillConstants.isSolarSkill(skillID) || skillID == RIFT_OF_DAMNATION_ATTACK) {
                updateSoulElement(1);
                handleEquinoxPower();
                handleCosmicBurstAuto();
            }
        }
        handleEquinoxAttack(skillID);
        handleFlareSlashRun(skillID);
        Option o1 = new Option();
        switch (attackInfo.skillId) {
            case RIFT_OF_DAMNATION_ATTACK:
            case RIFT_OF_DAMNATION_SUMMON:
                summonAndIncRift(attackInfo);
                break;
            case SOUL_ECLIPSE_BREAK:
                Summon summon = chr.getField().getSummonBySkillId(chr, SOUL_ECLIPSE);
                if (summon != null) {
                    o1.nValue = 1;
                    o1.nReason = SOUL_ECLIPSE_BREAK;
                    o1.tTerm = 3;
                    tsm.sendStat(IndieNotDamaged, o1); // Invincibility
                    chr.getField().removeLife(chr.getField().getSummonBySkillId(chr, SOUL_ECLIPSE));
                }
                break;
            case BLAZING_ASSAULT:
            case LUSTER_CHARGE:
                if (!tsm.hasStatBySkillId(skillID)) {
                    o1.nValue = 1;
                    o1.nReason = skillID;
                    o1.tTerm = si.getValue(x, slv);
                    o1.setInMillis(true);
                    tsm.sendStat(IndieNotDamaged, o1);
                }
                break;
            case ASTRAL_BLITZ:
                if (!tsm.hasStatBySkillId(ASTRAL_BLITZ_BUFF)) {
                    si = SkillData.getSkillInfoById(ASTRAL_BLITZ_BUFF);
                    o1.nValue = si.getValue(y, slv);
                    o1.nReason = ASTRAL_BLITZ_BUFF;
                    o1.tTerm = si.getValue(time, slv);
                    tsm.sendStat(IndiePMdR, o1);
                }
                break;
        }
        super.handleAttack(c, attackInfo, si, now);
    }

    private void handleEquinoxPower() {
        int skillID = EQUINOX_POWER;
        int extraSkillID = EQUINOX_POWER_EXTRA;
        if (chr.hasSkill(skillID) && !chr.hasSkillOnCooldown(skillID)) {
            int slv = chr.getSkillLevel(skillID);
            chr.setSkillCooldown(skillID, slv);
            ExtraSkill extraSkill = new ExtraSkill(extraSkillID, chr.getPosition());
            extraSkill.FaceLeft = chr.isLeft() ? 1 : 0;
            extraSkill.Value = 1;
            chr.write(UserLocal.registerExtraSkill(extraSkillID, List.of(extraSkill)));
            chr.write(UserLocal.userBonusAttackRequest(EQUINOX_POWER_SUN));
            var effect = Effect.skillUse(extraSkillID, chr.getLevel(), slv);
            chr.write(UserPacket.effect(effect));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
            if (chr.hasSkill(EQUINOX_POWER_II)) {
                handleEquinoxPowerII();
            }
        }
    }

    private void handleEquinoxPowerII() {
        int skillID = EQUINOX_POWER_II;
        SkillInfo si = SkillData.getSkillInfoById(skillID);
        int slv = chr.getSkillLevel(skillID);
        List<SecondAtom> secondAtoms = new ArrayList<>();
        final long now = System.currentTimeMillis();
        final var position = chr.getPosition();
        for (int key = 0; key < si.getValue(bulletCount, slv); key++) {
            var sai = si.getSecondAtomInfos().get(0);
            final var pos = position.add(sai.getPos());
            SecondAtom sa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), 0, key, si.getSkillId(), pos, now);
            secondAtoms.add(sa);
        }
        chr.createSecondAtom(secondAtoms);
    }

    private void handleEquinoxAttack(int skillID) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (tsm.hasStat(GlimmeringTime)) {
            if (SkillConstants.isLunaSkill(skillID)) {
                SkillInfo si = SkillData.getSkillInfoById(EQUINOX_CYCLE_SUN);
                int slv = chr.getSkillLevel(EQUINOX_CYCLE);
                handlePoseTypeMode(RisingSun, si, slv);
            } else if (SkillConstants.isSolarSkill(skillID)) {
                SkillInfo si = SkillData.getSkillInfoById(EQUINOX_CYCLE_MOON);
                int slv = chr.getSkillLevel(EQUINOX_CYCLE);
                handlePoseTypeMode(FallingMoon, si, slv);
            }
        }
    }

    private void handleFlareSlashRun(int attackSkillID) {
        int skillID = FLARE_SLASH_SUN;
        if (chr.hasSkill(skillID) && SkillConstants.isSolarSkill(attackSkillID) && !chr.hasSkillOnCooldown(skillID)) {
            int slv = chr.getSkillLevel(skillID);
            chr.setSkillCooldown(skillID, slv);
            ExtraSkill extraSkill = new ExtraSkill(skillID, chr.getPosition());
            extraSkill.FaceLeft = chr.isLeft() ? 1 : 0;
            extraSkill.Value = 1;
            chr.write(UserLocal.registerExtraSkill(skillID, List.of(extraSkill)));
            var effect = Effect.skillUse(skillID, chr.getLevel(), slv);
            chr.write(UserPacket.effect(effect));
            chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
        }
    }

    private int updateSoulElement(int update) {
        int skillID = SOUL_ELEMENT;
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        int consume = tsm.getOption(CosmicOrb).nOption;
        if (chr.hasSkill(skillID) && tsm.hasStat(ElementSoul)) {
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = chr.getSkillLevel(skillID);
            var prop = si.getValue(SkillStat.prop, slv);
            var max = si.getValue(x, slv) + (tsm.hasStat(CosmicForge) ? 5 : 0);
            var padInc = si.getValue(u2, slv); // 20
            var pmdr = 0;
            if (chr.hasSkill(SOUL_BLESSING)) {
                var sbSI = SkillData.getSkillInfoById(SOUL_BLESSING);
                var sbSLV = chr.getSkillLevel(SOUL_BLESSING);
                // Cosmic Orb Creation Chance: prop%, Max Stacks: +x
                // When cosmic orb is created, Bonus Attack Power: +y
                prop = sbSI.getValue(SkillStat.prop, sbSLV);
                max += sbSI.getValue(SkillStat.x, sbSLV);
                padInc += sbSI.getValue(SkillStat.y, sbSLV); // -6
            }
            if (chr.hasSkill(SOUL_BLESSING_II)) {
                var sbSI = SkillData.getSkillInfoById(SOUL_BLESSING_II);
                var sbSLV = chr.getSkillLevel(SOUL_BLESSING_II);
                // Cosmic Orb Creation Chance: prop%, Max Stacks: +x
                // When cosmic orb is created, Bonus Attack Power: +y
                prop = sbSI.getValue(SkillStat.prop, sbSLV);
                max += sbSI.getValue(SkillStat.x, sbSLV);
                padInc += sbSI.getValue(SkillStat.y, sbSLV);
            }
            if (chr.hasSkill(SOUL_BLESSING_III)) {
                var sbSI = SkillData.getSkillInfoById(SOUL_BLESSING_III);
                var sbSLV = chr.getSkillLevel(SOUL_BLESSING_III);
                // Cosmic Orb Creation Chance: prop%, Max Stacks: +x
                // When cosmic orb is created, Bonus Final Damage: +z%
                prop = sbSI.getValue(SkillStat.prop, sbSLV);
                max += sbSI.getValue(SkillStat.x, sbSLV);
                padInc += sbSI.getValue(SkillStat.y, sbSLV);
                pmdr += sbSI.getValue(SkillStat.z, sbSLV);
            }
            int orb = consume;
            if (Util.succeedProp(prop) || update != 1) {
                Option o2 = new Option();
                orb = Math.min(max, orb + update);
                if (orb <= 0) {
                    orb = 0;
                }
                o2.nOption = orb;
                o2.rOption = SOUL_ELEMENT_ORB;
                o2.tOption = si.getValue(y, slv);
                tsm.sendStat(CosmicOrb, o2, true);
                if (update > 0) {
                    EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
                    Option o3 = new Option();
                    Option o4 = new Option();
                    o3.nValue = padInc;
                    o3.nReason = SOUL_ELEMENT_BUFF;
                    o3.tTerm = si.getValue(y, slv);
                    newStats.put(IndiePAD, o3);
                    if (pmdr != 0) {
                        o4.nValue = pmdr;
                        o4.nReason = SOUL_ELEMENT_BUFF;
                        o4.tTerm = si.getValue(y, slv);
                        newStats.put(IndiePMdR, o4);
                    }
                    tsm.sendStat(newStats, true);
                }
            }
            Option o1 = tsm.getOption(ElementSoul);
            o1.nOption = slv;
            o1.rOption = skillID;
            o1.yOption = orb;
            tsm.sendStat(ElementSoul, o1);
        }
        return consume;
    }

    private void handleWillOfSteel(long now) {
        int skillID = WILL_OF_STEEL;
        if (chr.hasSkill(skillID)) {
            SkillInfo si = SkillData.getSkillInfoById(skillID);
            int slv = chr.getSkillLevel(skillID);
            int interval = si.getValue(w, slv);
            if (now - this.wosTime >= interval * 1000L) {
                int amount = (int) (chr.getMaxHP() / ((double) 100 / si.getValue(y, slv)));
                this.wosTime = now;
                if (chr.getHP() < chr.getMaxHP()) {
                    chr.heal(amount);
                }
            }
        }
    }

    private void summonAndIncRift(AttackInfo attackInfo) {
        Field field = chr.getField();
        Rect rect = chr.getRectAround(new Rect(-300, -300, 300, 300));
        int curFieldRiftCount = (int) field.getSummons().stream().filter(s -> s.getSkillID() == RIFT_OF_DAMNATION_SUMMON && s.getOwnerId() == chr.getId()).count();
        SkillInfo si = SkillData.getSkillInfoById(RIFT_OF_DAMNATION);
        int slv = chr.getSkillLevel(RIFT_OF_DAMNATION);
        Summon riftInRect = field.getSummonByChrAndSkillIdInRect(chr, RIFT_OF_DAMNATION_SUMMON, rect);
        switch (attackInfo.skillId) {
            case RIFT_OF_DAMNATION_ATTACK:
                if (riftInRect == null) {
                    if (curFieldRiftCount >= 2 || chr.hasSkillOnCooldown(RIFT_OF_DAMNATION_SUMMON)) {
                        return;
                    }
                    Summon summon = Summon.getSummonByAndSetStat(chr, RIFT_OF_DAMNATION_SUMMON, slv);
                    summon.setState(1);
                    summon.setFlip(!attackInfo.left);
                    summon.setMoveAbility(MoveAbility.Stop);
                    field.spawnAddSummon(summon);
                } else {
                    field.broadcast(Summoned.stateChanged(riftInRect, 1, null));
                }
                break;
            case RIFT_OF_DAMNATION_SUMMON:
                if (riftInRect != null) {
                    field.removeLife(riftInRect);
                    chr.addSkillCooldown(attackInfo.skillId, (int) (si.getValue(z, slv) * 1000L));
                }
                break;
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
        Field field = chr.getField();
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        Summon summon;
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (skillID) {
            case SOUL_ELEMENT:
                if (tsm.hasStat(ElementSoul)) {
                    tsm.removeStatsBySkill(skillID);
                    tsm.removeStatsBySkill(SOUL_ELEMENT_ORB);
                    tsm.removeStatsBySkill(SOUL_ELEMENT_BUFF);
                } else {
                    o1.nOption = slv;
                    o1.rOption = skillID;
                    tsm.sendStat(ElementSoul, o1);
                }
                break;
            case FALLING_MOON:
                handlePoseTypeMode(FallingMoon, si , slv);
                break;
            case RISING_SUN:
                handlePoseTypeMode(RisingSun, si , slv);
                if (chr.hasQuest(QuestConstants.SKILL_COMMAND_LOCK_ARK)
                    && "1".equals(chr.getQRValueByKey(
                    QuestConstants.SKILL_COMMAND_LOCK_ARK,
                    String.valueOf(EQUINOX_CYCLE)))) {
                    handlePoseTypeMode(EquinoxCycle, SkillData.getSkillInfoById(EQUINOX_CYCLE) , chr.getSkillLevel(EQUINOX_CYCLE));
                }
                break;
            case EQUINOX_CYCLE:
                handlePoseTypeMode(EquinoxCycle, si ,slv);
                break;
            case COSMIC_MATTER:
                updateSoulElement(-10);
                break;
            case COSMIC_SHOWER:
            case HEXA_COSMIC_SHOWER:
                int count = updateSoulElement(-10);
                summon = Summon.getSummonByAndSetStatWithTime(chr, skillID, slv, System.currentTimeMillis(),
                        si.getValue(time, slv) + count * si.getValue(u, slv));
                summon.setMoveAbility(MoveAbility.Stop);
                summon.setAssistType(AssistType.Attack);
                field.spawnSummon(summon);
                break;
            case COSMIC_BURST:
            case HEXA_COSMIC_BURST:
                handleCosmicBurst();
                break;
            case COSMIC_FORGE:
                o1.nOption = 5;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                tsm.sendStat(CosmicForge, o1);
                updateSoulElement(5);
                break;
            case GLORY_OF_THE_GUARDIANS:
                o1.nReason = skillID;
                o1.nValue = si.getValue(indieDamR, slv);
                o1.tTerm = si.getValue(time, slv);
                tsm.sendStat(IndieDamR, o1);
                break;
            case RIFT_OF_DAMNATION:
                o1.nOption = 1;
                o1.rOption = skillID;
                o1.tOption = si.getValue(SkillStat.time, slv);
                tsm.sendStat(Elysion, o1);
                break;
            case SOUL_ECLIPSE:
                if (field.getSummonBySkillId(chr, skillID) == null) {
                    summon = Summon.getSummonByAndSetStat(chr, skillID, slv);
                    summon.setMoveAbility(MoveAbility.Walk);
                    field.spawnSummon(summon);

                    o1.nValue = 1;
                    o1.nReason = SOUL_ECLIPSE_BREAK;
                    o1.tTerm = 3;
                    tsm.sendStat(IndieNotDamaged, o1); // Invincibility
                }
                break;
            case TRUE_SIGHT: {
                Rect rect = chr.getPosition().getRectAround(si.getFirstRect());
                if (!chr.isLeft()) {
                    rect = rect.moveRight();
                }
                final int max = si.getValue(mobCount, slv);
                final int extraTime = chr.hasSkill(TRUE_SIGHT_PERSIST) ? 20 : 0;
                // True Sight - Persist: Increases True Sight's duration. Duration: +20 sec

                o1.nValue = si.getValue(x, slv);
                o1.nReason = skillID;
                o1.tTerm = si.getValue(time, slv) + extraTime;

                o2.nValue = si.getValue(y, slv);
                o2.nReason = skillID;
                o2.tTerm = si.getValue(time, slv) + extraTime;

                o3.nOption = 1;
                o3.rOption = skillID;
                o3.tOption = si.getValue(time, slv) + extraTime;

                count = 0;
                for (Mob mob : chr.getField().getMobsInRect(rect)) {
                    if (count >= max) {
                        break;
                    }
                    MobTemporaryStat mts = mob.getTemporaryStat();
                    EnumMap<MobStat, Option> map = new EnumMap<>(MobStat.class);
                    map.put(MobStat.IndiePDR, o1.deepCopy());
                    map.put(MobStat.IndieMDR, o1.deepCopy());
                    map.put(MobStat.TrueSight, o3.deepCopy());
                    mts.addStatOptions(mob, map);
                    mts.addStatOptions(mob, MobStat.IndieAddFinalDamSkill, o2.deepCopy());
                    count++;
                }
                break;
            }
            case COSMOS:
                // 282 Cosmos is a 15s on/off galaxy. Native5A13FE4 gates on CTS926 != 0, not old orb stock.
                count = net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 ? 1 : updateSoulElement(-10);
                if (count <= 0) {
                    chr.chatMessage("宇宙球不足，無法使用該技能。");
                    break;
                }
                o1.nOption = count;
                o1.rOption = skillID;
                o1.tOption = si.getValue(time, slv);
                o1.setInMillis(true);
                tsm.sendStat(Cosmos, o1);
                break;
        }
    }

    /** Periodic282 Cosmos attacks belong to the already paid, unexpired cast. */
    public boolean canAttackWithCosmos(AttackInfo attack, Option cast, long now) {
        Option active = chr.getTemporaryStatManager().getOptByCTSAndSkill(Cosmos, COSMOS);
        int level = chr.getSkillLevel(COSMOS);
        SkillInfo info = SkillData.getSkillInfoById(COSMOS);
        if (attack.skillId != COSMOS || level <= 0 || info == null || chr.getHP() <= 0 || !isHandlerOfJob(chr.getJob())
                || active == null || active != cast || active.rOption != COSMOS || active.nOption == 0
                || now < active.startTime
                || now >= active.startTime + active.tOption * (active.isInMillis ? 1L : 1000L)) return false;
        int hits = info.getValue(attackCount, level), mobs = info.getValue(mobCount, level);
        return Byte.toUnsignedInt(attack.hits) <= hits && attack.mobCount <= mobs
                && attack.mobAttackInfo.size() <= mobs
                && attack.mobAttackInfo.stream().allMatch(m -> m.damages != null && m.damages.length <= hits);
    }

    private void handleCosmicBurstAuto() {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (chr.hasSkill(COSMIC_BURST) && tsm.hasStat(ElementSoul) && tsm.getOption(CosmicOrb).nOption >= 1
                && !chr.hasSkillOnCooldown(COSMIC_BURST)) {
            if (chr.hasQuest(QuestConstants.SKILL_COMMAND_LOCK_ARK)
                    && "1".equals(chr.getQRValueByKey(
                    QuestConstants.SKILL_COMMAND_LOCK_ARK,
                    String.valueOf(COSMIC_BURST)))) {
                chr.setSkillCooldown(COSMIC_BURST, chr.getSkillLevel(COSMIC_BURST));
                handleCosmicBurst();
                var effect = Effect.skillUse(chr.hasSkill(HEXA_COSMIC_BURST) ? HEXA_COSMIC_BURST : COSMIC_BURST,
                        chr.getLevel(), chr.getSkillLevel(COSMIC_BURST));
                chr.write(UserPacket.effect(effect));
                chr.getField().broadcast(UserRemote.effect(chr.getId(), effect), chr);
            }
        }
    }

    private void handleCosmicBurst() {
        int count = updateSoulElement(-10);
        if (count <= 0) {
            return;
        }
        SkillInfo si = SkillData.getSkillInfoById(chr.hasSkill(HEXA_COSMIC_BURST) ? HEXA_COSMIC_BURST : COSMIC_BURST);
        final var position = chr.getPosition();
        List<SecondAtom> secondAtoms = new ArrayList<>();
        final long now = System.currentTimeMillis();
        Rect rect = position.getRectAround(si.getFirstRect());
        if (!chr.isLeft()) {
            rect = rect.moveRight();
        }
        int key = 0;
        final var mob = Util.getRandomFromCollection(chr.getField().getMobsInRect(rect));
        var mobID = 0;
        if (mob != null) {
            mobID = mob.getObjectId();
        }
        for (var sai : si.getSecondAtomInfos().values()) {
            if (key >= count) {
                break;
            }
            final var pos = position.add(sai.getPos());
            SecondAtom sa = new SecondAtom(sai, chr.getNewSecondAtomKey(), chr.getId(), mobID, key, si.getSkillId(), pos, now);
            secondAtoms.add(sa);
            key++;
        }
        chr.createSecondAtom(secondAtoms);
        chr.addSkillCooldown(si.getSkillId(), -count * si.getValue(u2, chr.getSkillLevel(si.getSkillId())) * 1000);
    }

    public enum PoseType {
        RisingSun(0),
        FallingMoon(1),
        EquinoxCycle(2);

        public final int val;

        PoseType(int val) {
            this.val = val;
        }
    }

    private void handlePoseTypeMode(PoseType poseType, SkillInfo si, int slv) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        EnumMap<CharacterTemporaryStat, Option> newStats = new EnumMap<>(CharacterTemporaryStat.class);
        Option o1 = new Option();
        Option o2 = new Option();
        Option o3 = new Option();
        switch (poseType) {
            case FallingMoon: {
                if (tsm.hasStatBySkillId(RISING_SUN)) {
                    tsm.removeStatsBySkill(RISING_SUN);
                }
                if (tsm.hasStatBySkillId(EQUINOX_CYCLE_SUN)) {
                    tsm.removeStatsBySkill(EQUINOX_CYCLE_SUN);
                }
                SkillInfo mosSI = SkillData.getSkillInfoById(MASTER_OF_THE_SWORD);
                int mosSLV = chr.getSkillLevel(MASTER_OF_THE_SWORD);
                SkillInfo noxSI = SkillData.getSkillInfoById(EQUINOX_CYCLE);
                int noxSLV = chr.getSkillLevel(EQUINOX_CYCLE);
                boolean hasGlimmeringTime = tsm.hasStat(GlimmeringTime);
                o1.nOption = 10;
                o1.rOption = si.getSkillId();
                newStats.put(BuckShot, o1);
                o2.nOption = 1;
                o2.rOption = si.getSkillId();
                newStats.put(PoseType, o2);
                o3.nValue = (chr.hasSkill(MASTER_OF_THE_SWORD) ? mosSI.getValue(indieCr, mosSLV) : si.getValue(indieCr, slv))
                        + (hasGlimmeringTime ? noxSI.getValue(z, noxSLV) : 0);
                o3.nReason = si.getSkillId();
                newStats.put(IndieCrR, o3);
                tsm.sendStat(newStats, hasGlimmeringTime);
                break;
            }
            case RisingSun: {
                if (tsm.hasStatBySkillId(FALLING_MOON)) {
                    tsm.removeStatsBySkill(FALLING_MOON);
                }
                if (tsm.hasStatBySkillId(EQUINOX_CYCLE_MOON)) {
                    tsm.removeStatsBySkill(EQUINOX_CYCLE_MOON);
                }
                SkillInfo mosSI = SkillData.getSkillInfoById(MASTER_OF_THE_SWORD_BUFF);
                int mosSLV = chr.getSkillLevel(MASTER_OF_THE_SWORD);
                SkillInfo noxSI = SkillData.getSkillInfoById(EQUINOX_CYCLE);
                int noxSLV = chr.getSkillLevel(EQUINOX_CYCLE);
                boolean hasGlimmeringTime = tsm.hasStat(GlimmeringTime);
                o1.nOption = 2;
                o1.rOption = si.getSkillId();
                o1.bOption = 1;
                newStats.put(PoseType, o1);
                o2.nValue = (chr.hasSkill(MASTER_OF_THE_SWORD) ? mosSI.getValue(indieBooster, mosSLV) : si.getValue(indieBooster, slv))
                        + (hasGlimmeringTime ? -1 : 0);
                o2.nReason = si.getSkillId();
                newStats.put(IndieBooster, o2);
                o3.nValue = (chr.hasSkill(MASTER_OF_THE_SWORD) ? mosSI.getValue(indiePMdR, mosSLV) : si.getValue(indiePMdR, slv))
                        + (hasGlimmeringTime ? noxSI.getValue(x, noxSLV) : 0);
                o3.nReason = si.getSkillId();
                newStats.put(IndiePMdR, o3);
                tsm.sendStat(newStats, hasGlimmeringTime);
                break;
            }
            case EquinoxCycle: {
                o1.nOption = 1;
                o1.rOption = si.getSkillId();
                tsm.sendStat(GlimmeringTime, o1);
                break;
            }
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
    public void handleRemoveCTS(CharacterTemporaryStat cts,  List<Option> options) {
        TemporaryStatManager tsm = chr.getTemporaryStatManager();
        if (cts == GlimmeringTime) {
            tsm.removeStatsBySkill(EQUINOX_CYCLE_MOON);
            tsm.removeStatsBySkill(EQUINOX_CYCLE_SUN);
        } else if (cts == CosmicForge) {
            updateSoulElement(-10);
        }
        super.handleRemoveCTS(cts, options);
    }

    @Override
    public int handleSetCoolDownSkill(int skillId) {
        switch (skillId) {
            case CRESCENT_DIVIDE -> {
                chr.addSkillCooldown(skillId, 500);
                return 1;
            }
            case HEXA_COSMIC_BURST -> {
                int skillID = COSMIC_BURST;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
            case HEXA_EQUINOX_SLASH -> {
                int skillID = EQUINOX_SLASH;
                int slv = chr.getSkillLevel(skillID);
                chr.setSkillCooldown(skillID, slv);
                return 1;
            }
        }
        return super.handleSetCoolDownSkill(skillId);
    }
}
