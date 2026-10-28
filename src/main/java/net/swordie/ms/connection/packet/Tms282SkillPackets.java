package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.util.FileTime;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * TMS 282.3 skill result packets (CWvsContext / CUserLocal). Layouts were checked by emulating the client handlers
 * on packets from these builders (research/tms2823/skill-trace, "CLEAN" = every byte read, nothing more).
 * <pre>
 * LP00A2 CHANGE_SKILL_RECORD_RESULT (5FE5D20) [trace-verified; opcode presumed in ingame-lp-map-2823.md]
 *   u8 exclRequest (!=0 unlocks, 5E04DA0), u8 showResult, i16 n,
 *   n x (i32 skillId, i32 level, i32 masterLevel, FT expire), u8 sn          -- same as 265 changeSkillRecordResult
 * LP0662 SKILL_COOLTIME_SET (inline 578831A) [verified]
 *   i32 n, n x (i32 skillId, i32 seconds); seconds != 0 sets (x1000 ms, 5FE8E30), 0 clears (5FE91C0)
 *   -- 265 sends SKILL_COOLTIME_SET_M in milliseconds; 282 must use this opcode and seconds
 * LP009E TEMPORARY_STAT_SET (5FDD030) [verified]
 *   CTS block ({@link TemporaryStatEncoder#encodeForLocal}), i16 delay, u8, u8 hideBuff, u8 transform,
 *   u8 1, u8 1, u8 moving, [mask meets 9E43800: u8], i32 0, u8 curSize     -- 265's final i32 -1 is not read
 * LP009F TEMPORARY_STAT_RESET (5FE26F0) [verified]
 *   u8 1, u8 1, u8 curSize (-> ctx+0x4D78), u8 moving, then {@link TemporaryStatEncoder#encodeReset}
 * </pre>
 */
public final class Tms282SkillPackets {
    public static final int CHANGE_SKILL_RECORD_RESULT = 0x00A2;
    public static final int SKILL_COOLTIME_SET = 0x0662;
    public static final int TEMPORARY_STAT_SET = 0x009E;
    public static final int TEMPORARY_STAT_RESET = 0x009F;

    private Tms282SkillPackets() {
    }

    /** LP00A2 with {@code exclRequest} set, as 265 {@code changeSkillRecordResult(Skill)} sends it. */
    public static OutPacket changeSkillRecordResult(Skill skill) {
        return changeSkillRecordResult(List.of(Objects.requireNonNull(skill, "skill")), true, false, false);
    }

    public static OutPacket changeSkillRecordResult(List<Skill> skills, boolean exclRequest, boolean showResult,
                                                    boolean sn) {
        Objects.requireNonNull(skills, "skills");
        if (skills.size() > Short.MAX_VALUE) {
            throw new IllegalArgumentException("Too many skills for an i16 count: " + skills.size());
        }
        OutPacket out = Tms282LoginPacketSupport.packet(CHANGE_SKILL_RECORD_RESULT);
        out.encodeByte(exclRequest);
        out.encodeByte(showResult);
        out.encodeShort(skills.size());
        for (Skill skill : skills) {
            out.encodeInt(skill.getSkillId());
            out.encodeInt(skill.getCurrentLevel());
            out.encodeInt(skill.getMasterLevel());
            out.encodeFT(FileTime.MIN_TIME());
        }
        out.encodeByte(sn);
        return out;
    }

    /**
     * LP009D STAT_CHANGED with only the SP bit (mask 0x8000). {@link Tms282StatPackets} (the shared STAT_CHANGED
     * factory) leaves SP out because its width depends on the job; this is the SP form, same frame:
     * u8 exclRequest, u8 0, u8 1, u64 mask, SP, u8 0 (no charmOld), u8 0 (no recovery).
     * SP (8DAA46 -> 8AD930(job), ingame-layouts-282.md 5.1): extended-SP job (JobConstants.isExtendSpJob, the 282
     * predicate) u8 n + n x (u8 jobLevel, i32 sp); otherwise u16. {@code job} must be the job the client holds.
     */
    public static OutPacket spChanged(boolean exclRequest, short job, CharacterStat stat) {
        Objects.requireNonNull(stat, "stat");
        OutPacket out = Tms282LoginPacketSupport.packet(Tms282StatPackets.STAT_CHANGED);
        out.encodeByte(exclRequest);
        out.encodeByte(0);
        out.encodeByte(1);
        out.encodeLong(SP_MASK);
        if (JobConstants.isExtendSpJob(job)) {
            ExtendSP esp = stat.getExtendSP();
            if (esp == null) {
                out.encodeByte(0);
            } else {
                if (esp.getSpSet().size() > 0xFF) {
                    throw new IllegalArgumentException("Too many SP sets: " + esp.getSpSet().size());
                }
                esp.encode(out);
            }
        } else {
            int sp = stat.getSp();
            if (sp < 0 || sp > 0xFFFF) {
                throw new IllegalArgumentException("SP " + sp + " does not fit u16");
            }
            out.encodeShort(sp);
        }
        out.encodeByte(0);
        out.encodeByte(0);
        return out;
    }

    /** STAT_CHANGED mask bit 15 (SP). */
    public static final long SP_MASK = 0x8000L;

    /** LP0662: cooldowns in <b>seconds</b> (0 clears the skill's cooldown). */
    public static OutPacket skillCooltimeSet(Map<Integer, Integer> secondsBySkill) {
        Objects.requireNonNull(secondsBySkill, "secondsBySkill");
        OutPacket out = Tms282LoginPacketSupport.packet(SKILL_COOLTIME_SET);
        out.encodeInt(secondsBySkill.size());
        secondsBySkill.forEach((skillId, seconds) -> {
            if (seconds == null || seconds < 0) {
                throw new IllegalArgumentException("Cooldown of skill " + skillId + " must be >= 0 seconds");
            }
            out.encodeInt(skillId);
            out.encodeInt(seconds);
        });
        return out;
    }

    public static OutPacket skillCooltimeSet(int skillId, int seconds) {
        return skillCooltimeSet(Map.of(skillId, seconds));
    }

    /**
     * LP009E. {@code selection} comes from {@code encoder.select(...)} and must not be empty. {@code curSize} is the
     * number of active stat kinds after the change (265 writes {@code currentStats.size()}).
     */
    public static OutPacket temporaryStatSet(TemporaryStatEncoder encoder, TemporaryStatEncoder.Selection selection,
                                             int curSize, long nowMillis) {
        return temporaryStatSet(encoder,selection,curSize,nowMillis,false);
    }

    public static OutPacket temporaryStatSet(TemporaryStatEncoder encoder, TemporaryStatEncoder.Selection selection,
                                             int curSize, long nowMillis, boolean hideBuff) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(selection, "selection");
        if (selection.isEmpty()) {
            throw new IllegalArgumentException("TEMPORARY_STAT_SET without stats");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(TEMPORARY_STAT_SET);
        encoder.encodeForLocal(out, selection, nowMillis);
        out.encodeShort(0);        // delay (5FDD296)
        out.encodeByte(0);
        out.encodeByte(hideBuff);  // isHideBuff
        out.encodeByte(selection.stats().containsKey(CharacterTemporaryStat.IceAura)); // aura visual refresh
        out.encodeByte(1);
        out.encodeByte(1);
        out.encodeByte(selection.movement());
        if (selection.movement()) {
            out.encodeByte(0);     // 5FDEE96; 265 sends a random 0..100 here
        }
        out.encodeInt(0);          // 5FDF564
        out.encodeByte(curSize);   // 5FDF576
        return out;
    }

    // Admit only the job-managed states whose local set/reset fixtures were verified. Other 265 states keep
    // their old fail-closed path; the session skill service continues to own its own, separately traced buffs.
    private static final Set<CharacterTemporaryStat> MANAGER_STATS = Set.of(
            CharacterTemporaryStat.BasicStatUp, CharacterTemporaryStat.IndieNBDR, CharacterTemporaryStat.AsrR);

    public static Optional<OutPacket> fromManagerSet(TemporaryStatEncoder encoder,
            Map<CharacterTemporaryStat,List<Option>> stats,int curSize,boolean hide) {
        if(stats.isEmpty() || !stats.entrySet().stream().allMatch(e -> managerStat(e.getKey(), e.getValue()))) return Optional.empty();
        var selection=encoder.select(stats);
        if(!selection.skipped().isEmpty()) return Optional.empty();
        return Optional.of(temporaryStatSet(encoder,selection,curSize,System.currentTimeMillis(),hide));
    }

    public static Optional<OutPacket> fromManagerReset(TemporaryStatEncoder encoder,
            Map<CharacterTemporaryStat,List<Option>> removed,Map<CharacterTemporaryStat,List<Option>> remaining,int curSize) {
        if(removed.isEmpty() || !removed.entrySet().stream().allMatch(e -> managerStat(e.getKey(), e.getValue())
                || VERIFIED_EQUIPMENT_RESET.contains(e.getKey())
                || TemporaryStatManager.DISPELLABLE_DEBUFFS.contains(e.getKey()))) return Optional.empty();
        var selection=encoder.selectReset(removed);
        if(!selection.skipped().isEmpty()) return Optional.empty();
        return Optional.of(temporaryStatReset(encoder,selection,remaining,curSize,System.currentTimeMillis()));
    }

    private static boolean managerStat(CharacterTemporaryStat stat, List<Option> options) {
        if (!options.isEmpty() && options.stream().allMatch(o ->
                net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.managedStat(stat, o)
                || ((stat == CharacterTemporaryStat.SoulMP || stat == CharacterTemporaryStat.FullSoulMP)
                    && o.rOption == net.swordie.ms.client.character.items.Tms282MugongSoul.SKILL)
                || ((stat == CharacterTemporaryStat.IndiePADR || stat == CharacterTemporaryStat.IndieMADR)
                    && o.nReason == net.swordie.ms.client.character.items.Tms282MugongSoul.SKILL))) return true;
        if (!options.isEmpty() && options.stream().allMatch(o ->
                net.swordie.ms.client.character.tms282.Tms282VMatrixEffects.supports(stat,o))) return true;
        if ((stat == CharacterTemporaryStat.IndieEXP || stat == CharacterTemporaryStat.IndiePMdR)
                && !options.isEmpty() && options.stream().allMatch(o -> o.nReason
                == net.swordie.ms.client.character.items.Tms282EquipmentEffects.ASCETIC)) return true;
        if (!options.isEmpty() && options.stream().allMatch(o ->
                net.swordie.ms.client.jobs.special.SaitamaActionProtection.fixedDuration(stat, o))) return true;
        return MANAGER_STATS.contains(stat) || stat == CharacterTemporaryStat.IndieBuffIcon
                && !options.isEmpty() && options.stream().allMatch(o -> o.nReason == 122001011 || o.nReason == 122001012
                || o.nReason == net.swordie.ms.client.jobs.Job.MONOLITH || Tms282SummonIcons.allowed(o));
    }

    // Reset carries remaining indie options (possibly empty or from another source), not the removed source.
    // These exact bit formats were native-traced; selection below still refuses any unsupported stat.
    private static final Set<CharacterTemporaryStat> VERIFIED_EQUIPMENT_RESET = Set.of(
            CharacterTemporaryStat.BlackMageWeaponCreation, CharacterTemporaryStat.DestinyWeaponIndomitable,
            CharacterTemporaryStat.Destiny2ndWeaponIndomitable, CharacterTemporaryStat.SoulMP,
            CharacterTemporaryStat.FullSoulMP, CharacterTemporaryStat.IndieNotDamaged,
            CharacterTemporaryStat.IndiePADR, CharacterTemporaryStat.IndieMADR);

    /**
     * LP009F for the stats in {@code removed}; {@code remainingIndie} holds, per removed indie stat, the options that
     * stay active (empty or absent: none).
     */
    public static OutPacket temporaryStatReset(TemporaryStatEncoder encoder, TemporaryStatEncoder.Selection removed,
                                               Map<CharacterTemporaryStat, List<Option>> remainingIndie, int curSize,
                                               long nowMillis) {
        Objects.requireNonNull(encoder, "encoder");
        Objects.requireNonNull(removed, "removed");
        if (removed.isEmpty()) {
            throw new IllegalArgumentException("TEMPORARY_STAT_RESET without stats");
        }
        OutPacket out = Tms282LoginPacketSupport.packet(TEMPORARY_STAT_RESET);
        out.encodeByte(1);
        out.encodeByte(1);
        out.encodeByte(curSize);
        out.encodeByte(removed.movement());
        encoder.encodeReset(out, removed, remainingIndie, nowMillis);
        return out;
    }
}
