package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.ExtendSP;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.SkillStat;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.TemporaryStatEncoder;
import net.swordie.ms.connection.packet.Tms282SkillPackets;
import net.swordie.ms.connection.packet.Tms282SkillUpRequest;
import net.swordie.ms.connection.packet.Tms282SkillUseRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.connection.packet.Tms282EffectPackets;
import net.swordie.ms.connection.packet.Tms282RemoteStatEncoder;
import net.swordie.ms.connection.packet.Tms282UserRemotePackets;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.loaders.SkillData;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * TMS 282.3 skill requests on top of the 265 model: CP0196 skill-point spending and CP04B4 skill use. Decides and
 * mutates; wire formats are in {@link Tms282SkillPackets}, {@link Tms282StatPackets} and the
 * {@link TemporaryStatEncoder} it is given; persistence goes through {@link Tms282CharacterStore}.
 * <p>
 * Every request is answered with a packet that has exclRequest set (LP00A2 or LP009D), also when it is rejected:
 * the client locks further requests until then (265 relies on {@code chr.dispose()} for that, and does not answer
 * a CP0196 without enough SP at all, which leaves the 282 client locked).
 * <p>
 * With a {@link Tms282RemoteStatEncoder}, an outcome also carries the packets for the <b>other</b> users of the
 * character's field ({@link Outcome#fieldPackets}), in 265 order: a skill use sends LP048E SkillUse (265
 * SkillHandler broadcasts {@code UserRemote.effect(skillUse)} to the others only; the user's own client plays the
 * effect itself, so no LP04CA), then LP048F for the buff; a buff end sends LP0490 (and LP048F when an indie stat
 * keeps options that LP0490 cannot carry).
 */
public final class Tms282SkillService {
    /** 265 UserStatHandler: skills whose cap is maxLevel although they carry a master level. */
    static final List<Integer> MAX_LEVEL_CAPPED = List.of(1120012, 1320011, 21120011);

    private final TemporaryStatEncoder encoder;
    private final Tms282BuffResolver resolver;
    private final Tms282CharacterStore store;
    private final LongSupplier clock;
    private final Tms282RemoteStatEncoder remote;

    public Tms282SkillService(TemporaryStatEncoder encoder, Tms282BuffResolver resolver, Tms282CharacterStore store,
                              LongSupplier clock) {
        this(encoder, null, resolver, store, clock);
    }

    /** {@code remote}: other users' view of the buffs (null: nothing is sent to them). */
    public Tms282SkillService(TemporaryStatEncoder encoder, Tms282RemoteStatEncoder remote, Tms282BuffResolver resolver,
                              Tms282CharacterStore store, LongSupplier clock) {
        this.remote = remote;
        this.encoder = Objects.requireNonNull(encoder, "encoder");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Result of a request: the packets to send in order, one log line, and (skill use) the earliest buff end so the
     * caller can arm the expiry timer. {@code fieldPackets} go to the other users of the character's field, in order.
     */
    public record Outcome(boolean accepted, List<OutPacket> packets, String log, OptionalLong nextExpiry,
                          List<OutPacket> fieldPackets) {
        public Outcome {
            packets = List.copyOf(packets);
            Objects.requireNonNull(log, "log");
            Objects.requireNonNull(nextExpiry, "nextExpiry");
            fieldPackets = List.copyOf(fieldPackets);
        }

        public Outcome(boolean accepted, List<OutPacket> packets, String log, OptionalLong nextExpiry) {
            this(accepted, packets, log, nextExpiry, List.of());
        }
    }

    /** The other users' view of buffs, when this service sends it. */
    public Optional<Tms282RemoteStatEncoder> remote() {
        return Optional.ofNullable(remote);
    }

    // ------------------------------------------------------------------ CP0196

    /** 265 {@code UserStatHandler.handleUserSkillUpRequest} rules; writes the skill row and the stats. */
    public Outcome skillUp(Char chr, Tms282SkillUpRequest request) {
        Objects.requireNonNull(chr, "chr");
        Objects.requireNonNull(request, "request");
        int skillId = request.skillId();
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.client.character.items.Tms282EquipmentEffects.serverTriggered(skillId))
            return rejected("CP0196 cannot allocate points to a server-triggered equipment effect=" + skillId);
        int amount = request.amount();
        if (amount < 1) {
            return rejected("CP0196 skill=" + skillId + " amount=" + amount + ": amount below 1");
        }
        Skill data = SkillData.getSkillDeepCopyById(skillId);
        if (data == null) {
            return rejected("CP0196 skill=" + skillId + ": not in skill data");
        }
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        short job = stat.getJob();
        int rootId = data.getRootId();
        SkillInfo info = SkillData.getSkillInfoById(skillId);
        String refusal = Tms282SkillLearning.pointRefusal(chr, info);
        if (refusal != null) return rejected("CP0196 skill=" + skillId + ": " + refusal);
        if ((!JobConstants.isBeginnerJob((short) rootId) && !SkillConstants.isMatching(rootId, job))
                || SkillConstants.isSkillFromItem(skillId)) {
            return rejected("CP0196 skill=" + skillId + " root=" + rootId + ": not a skill of job " + job);
        }
        Skill owned = chr.getSkill(skillId);
        int curLevel = owned == null ? 0 : owned.getCurrentLevel();
        int master = Math.max(owned == null ? 0 : owned.getMasterLevel(), data.getMasterLevel());
        int newLevel;
        String spNote;
        Runnable spend;
        boolean spChanged;
        if (JobConstants.isBeginnerJob((short) rootId)) {
            if (net.swordie.ms.connection.protocol.Protocol.active().version()
                    == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                    && !SkillConstants.isBeginnerSpAddableSkill(skillId)) {
                return rejected("CP0196 skill=" + skillId + ": beginner skill does not accept SP");
            }
            int spent = 0;
            for (Skill s : chr.getSkills()) {
                if (SkillConstants.isBeginnerSpAddableSkill(s.getSkillId())) {
                    spent += s.getCurrentLevel();
                }
            }
            int total = Math.min(stat.getLevel(), JobConstants.isResistance((short) rootId)
                    ? GameConstants.RESISTANCE_SP_MAX_LV : GameConstants.BEGINNER_SP_MAX_LV) - 1;
            if (total - spent < amount) {
                return rejected("CP0196 skill=" + skillId + ": beginner SP " + (total - spent) + " < " + amount);
            }
            int max = owned == null ? data.getMasterLevel() : owned.getMasterLevel();
            if (max == 0) {
                max = 3; // 265: some beginner skills have no max level
            }
            newLevel = Math.min(max, curLevel + amount);
            spNote = "beginner SP " + (total - spent) + "->" + (total - spent - (newLevel - curLevel));
            spend = () -> { };
            spChanged = false;
        } else {
            int max = MAX_LEVEL_CAPPED.contains(skillId) ? data.getMaxLevel()
                    : owned == null ? data.getMasterLevel() : owned.getMasterLevel();
            newLevel = Math.min(max, curLevel + amount);
            int cost = newLevel - curLevel;
            if (JobConstants.isExtendSpJob(job)) {
                ExtendSP esp = stat.getExtendSP();
                byte jobLevel = JobConstants.isZero((short) rootId) ? JobConstants.getJobLevelByZeroSkillID(skillId)
                        : (byte) (Tms282SkillLearning.enabled() ? Tms282SkillLearning.bookStage(rootId)
                        : JobConstants.getJobLevel((short) rootId));
                int have = esp == null ? -1 : esp.getSpByJobLevel(jobLevel);
                if (have < amount) {
                    return rejected("CP0196 skill=" + skillId + ": SP of job level " + jobLevel + " is " + have + " < " + amount);
                }
                spNote = "extendSP[" + jobLevel + "] " + have + "->" + (have - cost);
                spend = () -> esp.setSpToJobLevel(jobLevel, have - cost);
            } else {
                int have = stat.getSp();
                if (have < amount) {
                    return rejected("CP0196 skill=" + skillId + ": SP " + have + " < " + amount);
                }
                spNote = "SP " + have + "->" + (have - cost);
                spend = () -> stat.setSp(have - cost);
            }
            spChanged = true;
        }
        if (newLevel <= curLevel) {
            return rejected("CP0196 skill=" + skillId + ": level " + curLevel + " is already the cap");
        }
        // Mutate, then write; on a write failure the in-memory change stays for this session and is reported.
        Skill record = data;
        record.setCharId(chr.getId());
        record.setCurrentLevel(newLevel);
        record.setMasterLevel(master);
        Tms282SkillLearning.putWithStats(chr, record);
        spend.run();
        String saved;
        List<Skill> fixed = Tms282SkillLearning.reconcileFixed(chr,
                skill -> Tms282SkillLearning.putWithStats(chr, skill));
        try {
            store.saveSkill(record);
            for (Skill skill : fixed) store.saveSkill(skill);
            if (spChanged) {
                store.saveStats(stat);
            }
            saved = "saved skill row " + record.getId() + (spChanged ? " + characterstats" : "");
        } catch (RuntimeException e) {
            saved = "SAVE FAILED " + e.getClass().getSimpleName() + " (kept in memory)";
        }
        List<OutPacket> packets = new ArrayList<>();
        packets.add(Tms282SkillPackets.changeSkillRecordResult(record));
        if (!fixed.isEmpty()) packets.add(Tms282SkillPackets.changeSkillRecordResult(fixed, true, false, false));
        if (spChanged) {
            packets.add(Tms282SkillPackets.spChanged(true, job, stat));
        }
        return new Outcome(true, packets, "CP0196 skill=" + skillId + " level " + curLevel + "->" + newLevel
                + " master=" + master + " " + spNote + "; " + saved + "; SENT LP00A2" + (spChanged ? " + LP009D(sp)" : ""),
                OptionalLong.empty());
    }

    // ------------------------------------------------------------------ CP04B4

    /**
     * Skill use: level from the character (as 265 prefers it over the packet), MP/HP cost, cooldown, and for a buff
     * the CTS from the resolver. Attack skills only pay the cost (no monsters yet). The HP/MP change goes last, with
     * exclRequest, so the client unlocks after the other packets.
     */
    public Outcome useSkill(Char chr, Tms282BuffBook book, Tms282SkillUseRequest request) {
        Outcome outcome = useSkill0(chr, book, request);
        if (!outcome.accepted() && Tms282LucidFlight.isFlightSkill(request.skillId())) {
            // lucidfly-0930: the client took off by itself when it sent the request (5A7A3AC); a refused use must
            // still land it, or it flies with no CTS the server could ever reset (live 09-30, character 38).
            List<OutPacket> packets = new ArrayList<>();
            var land = Tms282LucidFlight.landingReset(encoder, request.skillId(), book.size(), clock.getAsLong());
            if (land.isEmpty()) {
                return outcome;
            }
            packets.add(land.get());
            packets.addAll(outcome.packets());
            return new Outcome(false, packets, outcome.log() + "; SENT LP009F NewFlying (land the client-side flight)",
                    outcome.nextExpiry(), outcome.fieldPackets());
        }
        return outcome;
    }

    private Outcome useSkill0(Char chr, Tms282BuffBook book, Tms282SkillUseRequest request) {
        Objects.requireNonNull(chr, "chr");
        Objects.requireNonNull(book, "book");
        Objects.requireNonNull(request, "request");
        long now = clock.getAsLong();
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3) {
            book.bind(chr);
        }
        int skillId = request.skillId();
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && skillId == net.swordie.ms.client.jobs.flora.Tms282ArkFlight.SKILL) {
            String refusal = net.swordie.ms.client.jobs.flora.Tms282ArkFlight.refusal(chr);
            if (refusal != null) return rejected("CP04B4 " + refusal);
            try {
                net.swordie.ms.client.jobs.flora.Tms282ArkFlight.validateUse(request);
            } catch (RuntimeException malformed) {
                return rejected("CP04B4 malformed Ark flight: " + malformed.getMessage());
            }
        }
        if (Tms282SkillLearning.enabled() && skillId == net.swordie.ms.client.jobs.flora.Adele.AETHER_GUARD)
            return rejected("CP04B4 Aether Guard requires its native CP04B9 prepare");
        if(net.swordie.ms.client.jobs.Jianghu.Tms282HowlingStorm.handles(skillId))
            return rejected("Howling Storm requires its native prepare and owned finisher");
        if (net.swordie.ms.client.jobs.adventurer.thief.Tms282BladeTempest.handles(skillId))
            return rejected("CP04B4 Blade Tempest requires its native opening/prepare lifecycle");
        if(net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.handles(skillId)) {
            if(skillId!=net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.INSTALLED
                    || !net.swordie.ms.client.jobs.resistance.Tms282OmegaBlaster.install(chr,book,request.nativeOptionWithoutBody(),now))
                return rejected("CP04B4 Omega Blaster requires an owned release or active Artificial Evolution");
            return new Outcome(true,List.of(Tms282StatPackets.unlock()),"CP04B4 Omega Blaster installed",book.nextExpiry(),List.of());
        }
        if (Tms282SkillLearning.enabled() && skillId == net.swordie.ms.client.jobs.cygnus.BlazeWizard.INFERNO_SPHERE)
            return rejected("CP04B4 Inferno Sphere requires its native CP04B9 prepare");
        if (Tms282LucidFlight.isVehicleFlight(skillId) && !Tms282LucidFlight.flying(book)) {
            // mount flight: not in any skill data, level = the vehicle's (client 58B6EA0); only while riding
            if (!chr.getTemporaryStatManager().hasStat(CharacterTemporaryStat.RideVehicle))
                return rejected("CP04B4 skill=" + skillId + ": mount flight without a mount");
            List<OutPacket> replies = new ArrayList<>();
            List<OutPacket> others = new ArrayList<>();
            Map<CharacterTemporaryStat, List<Option>> changed = book.apply(List.of(Tms282LucidFlight.flight(skillId, 1, now)));
            var authoritative = book.serverSnapshot();
            changed.replaceAll((cts, options) -> authoritative.getOrDefault(cts, List.of()));
            TemporaryStatEncoder.Selection selection = encoder.select(changed);
            if (!selection.isEmpty())
                replies.add(Tms282SkillPackets.temporaryStatSet(encoder, selection, authoritative.size(), now));
            if (remote != null) {
                TemporaryStatEncoder.Selection shown = remote.select(changed);
                if (!shown.isEmpty()) others.add(Tms282UserRemotePackets.setTemporaryStat(chr.getId(), remote, shown, now));
            }
            replies.add(Tms282StatPackets.unlock());
            return new Outcome(true, replies, "CP04B4 skill=" + skillId + " mount flight ON"
                    + (selection.isEmpty() ? " (NewFlying not encodable: " + selection.skipped() + ")" : " SENT LP009E [NewFlying]"),
                    book.nextExpiry(), others);
        }
        if (Tms282LucidFlight.isFlightSkill(skillId) && Tms282LucidFlight.flying(book)) {
            // lucidfly-0930: 異想之夢 is a toggle (isCancelableBuff); a second use while flying lands, no cost/cooldown.
            Outcome off = cancel(book, chr.getId(), skillId);
            List<OutPacket> replies = new ArrayList<>(off.packets());
            replies.add(Tms282StatPackets.unlock());
            return new Outcome(true, replies, "CP04B4 skill=" + skillId + " Lucid 異想之夢 flight OFF (second use): "
                    + off.log(), off.nextExpiry(), off.fieldPackets());
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.client.character.items.Tms282EquipmentEffects.serverTriggered(skillId))
            return rejected("CP04B4 equipment effect requires a server-side trigger=" + skillId);
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && Tms282VMatrixEffects.source(skillId))
            return rejected("CP04B4 server-trigger-only VMatrix effect=" + skillId);
        int slv = Tms282HiddenSkillParents.useLevel(chr, skillId);
        if (skillId == 80002889 || skillId == 80002890)
            slv = Tms282RuneRuntime.temporarySkillLevel(chr, skillId, now);
        if (skillId == 121001007) slv = net.swordie.ms.client.jobs.special.SpecialJobProgression.requestLevel(chr, skillId);
        String head = "CP04B4 skill=" + skillId + " slv=" + slv + (request.slv() != slv ? " (packet slv " + request.slv() + ")" : "");
        if (slv <= 0) {
            return rejected(head + ": character does not have the skill");
        }
        SkillInfo si = SkillData.getSkillInfoById(skillId);
        if (si == null) {
            return rejected(head + ": not in skill data");
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.isActiveSkill(skillId)) {
            if (!net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.sourceEligible(chr, skillId))
                return rejected(head + ": special weapon skill requires its unlocked record and equipped source");
            if (!net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.effectSupported(skillId))
                return rejected(head + ": special weapon native activation/cancellation effect is not yet supported; no cost");
            if (net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.cancelActive(chr, skillId, now)) {
                return new Outcome(true, List.of(Tms282StatPackets.unlock()),
                        head + ": invulnerability cancelled without restarting cooldown", book.nextExpiry(), List.of());
            }
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.client.character.items.Tms282EquipmentSkillData.requiresEquippedSource(skillId)) {
            int beginner = net.swordie.ms.constants.JobConstants.JobEnum.getJobById(chr.getJob()).getBeginnerJobId();
            int granted = net.swordie.ms.client.character.items.EquipmentSkills.levels(
                    chr.getEquippedInventory().getItems(), beginner).getOrDefault(skillId, 0);
            if (granted < slv) {
                return rejected(head + ": equipped item does not grant this skill level");
            }
        }
        if (skillId == net.swordie.ms.client.character.items.Tms282EquipmentEffects.FIRESTARTER
                && !net.swordie.ms.client.character.items.Tms282EquipmentEffects.canFirestart(chr)) {
            return rejected(head + ": Firestarter requires its equipped ring and a normal burning hunting field");
        }
        if (skillId == net.swordie.ms.client.character.items.Tms282EquipmentEffects.ASCETIC
                && !net.swordie.ms.client.character.items.Tms282EquipmentEffects.wearing(chr,
                net.swordie.ms.client.character.items.Tms282EquipmentEffects.ASCETIC_RING))
            return rejected(head + ": Ascetic requires its equipped ring");
        if (skillId == 121002002) {
            if (net.swordie.ms.client.jobs.special.SpecialJobProgression.requestLevel(chr, skillId) <= 0)
                return rejected(head + ": Beast Breathing requires the correct job and level");
            if (book.snapshot().containsKey(CharacterTemporaryStat.TanjiroBeastBreathingType5)) {
                Outcome off = cancel(book, chr.getId(), skillId);
                List<OutPacket> replies = new ArrayList<>(off.packets());
                replies.add(Tms282StatPackets.unlock());
                return new Outcome(true, replies, "CP04B4 Beast Breathing OFF (no MP cost)", off.nextExpiry(), off.fieldPackets());
            }
        }
        if (skillId == Tms282DefensiveSkills.DEFENSIVE_OVERDRIVE
                && Tms282DefensiveSkills.active(book.snapshot(),skillId,now)) {
            Outcome off = cancel(book,chr.getId(),skillId);
            List<OutPacket> replies = new ArrayList<>(off.packets());
            replies.add(Tms282StatPackets.unlock());
            return new Outcome(true,replies,"CP04B4 Defensive Overdrive cancelled (cooldown retained)",off.nextExpiry(),off.fieldPackets());
        }
        boolean tanjiroDash = skillId == 121001008;
        if (Tms282SkillLearning.enabled() && net.swordie.ms.client.jobs.cygnus.BlazeWizard.isPhoenixDrive(skillId)) {
            if (!JobConstants.isBlazeWizard(chr.getJob())) return rejected(head + ": Phoenix Drive requires Blaze Wizard");
            int other = skillId == net.swordie.ms.client.jobs.cygnus.BlazeWizard.PHOENIX_DRIVE
                    ? net.swordie.ms.client.jobs.cygnus.BlazeWizard.HEXA_PHOENIX_DRIVE
                    : net.swordie.ms.client.jobs.cygnus.BlazeWizard.PHOENIX_DRIVE;
            // WZ changeSkill replaces the same ability; a retained base/VI key must not bypass its cooldown.
            if (book.coolingDown(other, now) || chr.getSkillCoolTimes().get(other) > now)
                return rejected(head + ": Phoenix Drive upgrade shares its active cooldown");
        }
        if (tanjiroDash && net.swordie.ms.client.jobs.special.SpecialJobProgression.requestLevel(chr, skillId) <= 0)
            return rejected(head + ": dash requires the correct job and level");
        if (!tanjiroDash && book.coolingDown(skillId, now)) {
            return rejected(head + ": cooling down, " + book.cooldownRemainingMillis(skillId, now) + " ms left");
        }
        CharacterStat stat = chr.getAvatarData().getCharacterStat();
        short job = stat.getJob();
        boolean kanna282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && JobConstants.isKanna(job);
        int mpCon = JobConstants.isNoManaJob(job) && !kanna282 ? 0 : Math.max(0, si.getValue(SkillStat.mpCon, slv));
        if (skillId == net.swordie.ms.client.jobs.resistance.demon.Tms282DemonicBlast.SKILL
                && net.swordie.ms.client.jobs.resistance.demon.Tms282DemonicBlast.active())
            return rejected(head + ": Demonic Blast requires an attack request");
        int mpPercent = si.getValue(SkillStat.mpRCon, slv);
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && (!JobConstants.isNoManaJob(job) || kanna282) && mpPercent > 0) {
            // Same percentage override as Char.applyMpCon; compute before accepting cost/cooldown/effects.
            mpCon = (int) Math.min(Integer.MAX_VALUE, (long) Math.max(0, chr.getMaxMP()) * mpPercent / 100);
        }
        if (skillId == net.swordie.ms.client.jobs.adventurer.magician.Tms282IceAura.SKILL
                && net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.client.jobs.adventurer.magician.Tms282IceAura.active(chr)) mpCon = 0;
        int hpCon = Math.max(0, si.getValue(SkillStat.hpCon, slv));
        if (stat.getMp() < mpCon) {
            return rejected(head + ": MP " + stat.getMp() + " < mpCon " + mpCon);
        }
        if (hpCon > 0 && stat.getHp() <= hpCon) {
            return rejected(head + ": HP " + stat.getHp() + " <= hpCon " + hpCon);
        }
        int dashUse = 0;
        if (tanjiroDash) {
            dashUse = book.claimTanjiroDash(now, si.getValue(SkillStat.y, slv), si.getValue(SkillStat.q, slv),
                    cooldownSeconds(si, slv) * 1000L);
            if (dashUse == 0) return rejected(head + ": consecutive dash exhausted or window closed");
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && skillId == net.swordie.ms.client.character.items.Tms282MugongSoul.SKILL) {
            net.swordie.ms.client.character.items.Tms282MugongSoul.Charge charge;
            try {
                charge = net.swordie.ms.client.character.items.Tms282MugongSoul.preflight(chr, slv);
            } catch (IllegalArgumentException refusal) {
                return rejected(head + ": " + refusal.getMessage());
            }
            if (!net.swordie.ms.client.character.items.Tms282MugongSoul.commit(chr, charge))
                return rejected(head + ": soul weapon or charge changed before use");
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && net.swordie.ms.client.character.items.Tms282SpecialWeaponSkills.isActiveSkill(skillId)
                && !net.swordie.ms.client.character.items.Tms282SpecialWeaponEffects.activate(chr, skillId, slv))
            return rejected(head + ": special weapon source changed before activation; no cost");
        stat.setMp(stat.getMp() - mpCon);
        stat.setHp(stat.getHp() - hpCon);
        List<OutPacket> packets = new ArrayList<>();
        List<OutPacket> field = new ArrayList<>();
        StringBuilder log = new StringBuilder(head).append(" mpCon=").append(mpCon).append(" hpCon=").append(hpCon);
        if (tanjiroDash) log.append(" consecutiveDash=").append(dashUse).append('/').append(si.getValue(SkillStat.y, slv));
        if (remote != null) {
            var effect = Tms282EffectPackets.skillUse(skillId, stat.getLevel(), slv);
            if (effect.isPresent()) {
                field.add(Tms282UserRemotePackets.effect(chr.getId(), effect.get()));
                log.append(" FIELD LP048E SkillUse");
            } else {
                log.append(" (no LP048E: this skill's SkillUse reads are not traced)");
            }
        }

        int cooldownSeconds = Tms282KinesisUnleash.cooldownSeconds(chr, skillId, cooldownSeconds(si, slv), now);
        cooldownSeconds = net.swordie.ms.client.jobs.flora.Khali.oblivionCooldownSeconds(chr, skillId, cooldownSeconds);
        if (cooldownSeconds > 0 && (!tanjiroDash || dashUse == 1)) {
            book.startCooldown(skillId, now + cooldownSeconds * 1000L);
            packets.add(Tms282SkillPackets.skillCooltimeSet(skillId, cooldownSeconds));
            log.append(" cooldown=").append(cooldownSeconds).append("s (LP0662)");
        }

        List<Tms282BuffResolver.ResolvedStat> resolved = resolver.resolve(si, skillId, slv, now);
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                && skillId == net.swordie.ms.client.jobs.adventurer.Kinesis.CLEAR_MIND_282) {
            resolved = List.of(); // asrR is its psd passive; the active use only clears debuffs in Kinesis.handleSkill.
        }
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3 && !si.isNotIncBuffDuration()) {
            long buffTimeR = chr.getTotalStat(net.swordie.ms.enums.BaseStat.buffTimeR);
            if (buffTimeR == 0) buffTimeR = 100;
            for (var resolvedStat : resolved) {
                if (net.swordie.ms.client.character.skills.temp.TemporaryStatManager.fixedBuffDuration(resolvedStat.stat(), resolvedStat.option())) continue;
                int duration = (int) Math.min(Integer.MAX_VALUE, resolvedStat.durationMillis() * buffTimeR / 100);
                if (resolvedStat.stat().isIndie()) resolvedStat.option().tTerm = duration; else resolvedStat.option().tOption = duration;
            }
        }
        if (resolved.isEmpty()) {
            log.append(" kind=").append(si.getValue(SkillStat.time, slv) > 0 ? "no-CTS" : "attack/other");
        } else {
            Map<CharacterTemporaryStat, List<Option>> wanted = new EnumMap<>(CharacterTemporaryStat.class);
            resolved.forEach(r -> wanted.put(r.stat(), List.of(r.option())));
            TemporaryStatEncoder.Selection check = encoder.select(wanted);
            List<Tms282BuffResolver.ResolvedStat> sendable = resolved.stream()
                    .filter(r -> check.stats().containsKey(r.stat())).toList();
            log.append(" buff ").append(resolved.stream().map(r -> r.stat() + "(" + r.source() + ")")
                    .collect(Collectors.joining(",")));
            if (!check.skipped().isEmpty()) {
                log.append(" SKIPPED ").append(check.skipped().stream().map(s -> s.stat() + ": " + s.reason())
                        .collect(Collectors.joining("; ")));
            }
            if (!sendable.isEmpty()) {
                Map<CharacterTemporaryStat, List<Option>> changed = book.apply(sendable);
                var authoritative = book.serverSnapshot();
                changed.replaceAll((cts, options) -> authoritative.getOrDefault(cts, List.of()));
                TemporaryStatEncoder.Selection selection = encoder.select(changed);
                if (!selection.isEmpty()) {
                    packets.add(Tms282SkillPackets.temporaryStatSet(encoder, selection, authoritative.size(), now));
                    log.append(" SENT LP009E ").append(selection.stats().keySet()).append(" for ")
                            .append(sendable.getFirst().durationMillis()).append(" ms");
                }
                if (remote != null) {
                    TemporaryStatEncoder.Selection shown = remote.select(changed);
                    if (!shown.isEmpty()) {
                        field.add(Tms282UserRemotePackets.setTemporaryStat(chr.getId(), remote, shown, now));
                        log.append(" FIELD LP048F ").append(shown.stats().keySet());
                    }
                    if (!shown.skipped().isEmpty()) {
                        log.append(" (not shown to others: ").append(shown.skipped().stream()
                                .map(s -> s.stat() + ": " + s.reason()).collect(Collectors.joining("; "))).append(")");
                    }
                }
            }
        }
        packets.add(Tms282StatPackets.hpMpChanged(true, stat.getHp(), stat.getMp()));
        log.append(" SENT LP009D hp=").append(stat.getHp()).append(" mp=").append(stat.getMp()).append(" (unlock)");
        return new Outcome(true, packets, log.toString(), book.nextExpiry(), field);
    }

    /**
     * Removes the buffs whose time is up and returns the TEMPORARY_STAT_RESET for them (none when nothing expired).
     * The log says what was removed.
     */
    public Outcome expire(Tms282BuffBook book) {
        return expire(book, 0);
    }

    /**
     * {@link #expire(Tms282BuffBook)} for character {@code characterId}; with a remote encoder and a characterId
     * above 0, the other users get LP0490 (and LP048F for indie stats that keep options LP0490 cannot carry).
     */
    public Outcome expire(Tms282BuffBook book, int characterId) {
        long now = clock.getAsLong();
        Tms282BuffBook.Expired expired = book.expire(now);
        if (expired.isEmpty()) {
            return new Outcome(false, List.of(), "no buff expired", book.nextExpiry());
        }
        return removedOutcome(book, expired, characterId, now, "buff expired ");
    }

    /**
     * CP04B7 USER_SKILL_CANCEL_REQUEST: ends the buffs of {@code skillId} at once, with the same LP009F / LP0490 /
     * LP048F as an expiry; nothing is sent when the skill has no active buff.
     */
    public Outcome cancel(Tms282BuffBook book, int characterId, int skillId) {
        long now = clock.getAsLong();
        Tms282BuffBook.Expired expired = book.cancelSkill(skillId);
        if (expired.isEmpty()) {
            return new Outcome(false, List.of(), "skill " + skillId + " has no active buff", book.nextExpiry());
        }
        return removedOutcome(book, expired, characterId, now, "buff cancelled (skill " + skillId + ") ");
    }

    private Outcome removedOutcome(Tms282BuffBook book, Tms282BuffBook.Expired expired, int characterId, long now,
                                   String label) {
        TemporaryStatEncoder.Selection selection = encoder.select(expired.removed());
        var authoritative = book.serverSnapshot();
        Map<CharacterTemporaryStat, List<Option>> remaining = new EnumMap<>(CharacterTemporaryStat.class);
        for (CharacterTemporaryStat cts : expired.removed().keySet()) {
            if (cts.isIndie()) remaining.put(cts, authoritative.getOrDefault(cts, List.of()));
        }
        List<OutPacket> packets = selection.isEmpty() ? List.of()
                : List.of(Tms282SkillPackets.temporaryStatReset(encoder, selection, remaining,
                authoritative.size(), now));
        Map<CharacterTemporaryStat, Integer> left = new LinkedHashMap<>();
        remaining.forEach((k, v) -> left.put(k, v.size()));
        List<OutPacket> field = new ArrayList<>();
        String fieldLog = "";
        if (remote != null && characterId > 0) {
            Tms282RemoteStatEncoder.Reset reset = remote.selectReset(expired.removed(), remaining);
            if (!reset.reset().isEmpty()) {
                field.add(Tms282UserRemotePackets.resetTemporaryStat(characterId, remote, reset, now));
                fieldLog += " FIELD LP0490 " + reset.reset().stats().keySet();
            }
            if (!reset.refresh().isEmpty()) {
                field.add(Tms282UserRemotePackets.setTemporaryStat(characterId, remote, reset.refresh(), now));
                fieldLog += " FIELD LP048F refresh " + reset.refresh().stats().keySet();
            }
        }
        return new Outcome(true, packets, label + expired.removed().keySet()
                + (left.isEmpty() ? "" : " indie left " + left) + " active kinds=" + authoritative.size()
                + (packets.isEmpty() ? " (nothing sendable)" : " SENT LP009F") + fieldLog, book.nextExpiry(), field);
    }

    static int cooldownSeconds(SkillInfo si, int slv) {
        int seconds = si.getSkillStatInfo().containsKey(SkillStat.cooltime) ? si.getValue(SkillStat.cooltime, slv) : 0;
        if (seconds <= 0 && si.getSkillStatInfo().containsKey(SkillStat.cooltimeMS)) {
            int ms = si.getValue(SkillStat.cooltimeMS, slv);
            seconds = ms > 0 ? (ms + 999) / 1000 : 0;
        }
        return Math.max(0, seconds);
    }

    private static Outcome rejected(String log) {
        return new Outcome(false, List.of(Tms282StatPackets.unlock()), "REJECTED " + log + "; SENT LP009D unlock",
                OptionalLong.empty());
    }
}
