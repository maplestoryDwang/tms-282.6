package net.swordie.ms.client.character.tms282;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.TemporaryStatEncoder;
import net.swordie.ms.connection.packet.Tms282RemoteCtsTable;
import net.swordie.ms.connection.packet.Tms282RemoteStatEncoder;
import net.swordie.ms.connection.packet.Tms282SkillPackets;
import net.swordie.ms.connection.packet.Tms282UserRemotePackets;
import net.swordie.ms.connection.protocol.codec.ProfileEncoders;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * lucidfly-0930: 變身燃燒 Lucid skill 異想之夢 (80004201, WZ: type 10, isCancelableBuff, no {@code time},
 * isFieldMovingLimit) is a flight toggle carried by CTS NewFlying (bit 558, CTS offset +0x15BC n / +0x15C8 r).
 * 282.4 client evidence (research/tms2823/change-burning-lucid/FLY-FIX.md):
 * <ul>
 *     <li>5A79D00 (after the CP04B4 is built) starts the flight locally at once: CVecCtrl 13D0740(on=1, skill) for
 *     80004201 (5A7A3AC); nothing ends it until the CTS says so.</li>
 *     <li>5EFEC60 (movement recompute on a CTS change whose mask meets the movement flag; NewFlying is one of the 45
 *     bits): NewFlying n != 0 -> SetFlying(1, r); NewFlying n == 0 while flying -> SetFlying(0) (5EFF917..5EFF9A6).</li>
 *     <li>4442C70 (skill vtable slot 111): 80004201 counts as "on" only while NewFlying n != 0 and r == 80004201;
 *     5A8B8C0 / 5A8B920 (field-move check, "isFieldMovingLimit") refuse while NewFlying r == 80004201.</li>
 *     <li>36D44F0 (buff icon cancel): flying by this skill -> CP04B7 + SetFlying(0).</li>
 * </ul>
 * The server never sent NewFlying, so the flight the client started on its own could not be ended (live 09-30,
 * character 38). Now: an accepted use sets NewFlying n=1 r=80004201 with no expiry; a second use, CP04B7, the
 * untransform and the next field entry reset it (LP009F with the movement byte -> 5EFEC60 lands the character).
 */
public final class Tms282LucidFlight {
    public static final int WHIMSICAL_DREAM = 80004201;
    /** The client tests n > 0 (5A8B8DE) and n != 0 (4442D0D, 5EFF8DF); 265 flight handlers (Evan, Illium) use 1. */
    public static final int FLYING_N = 1;

    private static volatile Tms282RemoteStatEncoder remote;

    private Tms282LucidFlight() {
    }

    /**
     * Xenon 自由飛行 (hidden child of 人工進化 36141500; 282 Skill.wz time 30000 ms): the same client-started NewFlying
     * toggle. Live 2026-10-07 (character 73): the use was refused as "does not have the skill" and the flight the
     * client had started by itself could not be ended.
     */
    public static final int XENON_FREE_FLIGHT = 36141502;

    /**
     * Mount flight (client 594A8E9): while riding, the client takes the vehicle id of RideVehicle, reads the
     * TamingMob's vehicleNewFlyingLevel (A0C9A0) and casts 80001332 or 80001242 (1157780 picks by vehicle). The skill
     * level is the vehicle's level (58B6EA0), not a learned skill, and neither id is in the server skill data.
     * vehicleNaviFlyingLevel mounts use 80001286 (NaviFlying, not handled here).
     */
    public static final int VEHICLE_FLIGHT_A = 80001242, VEHICLE_FLIGHT_B = 80001332;

    public static boolean isVehicleFlight(int skillId) {
        return skillId == VEHICLE_FLIGHT_A || skillId == VEHICLE_FLIGHT_B;
    }

    public static boolean isFlightSkill(int skillId) {
        return skillId == WHIMSICAL_DREAM || skillId == XENON_FREE_FLIGHT || isVehicleFlight(skillId)
                || skillId == net.swordie.ms.client.jobs.flora.Tms282ArkFlight.SKILL;
    }

    /** The CTS an accepted use sets: NewFlying n=1 r=80004201 t=0 (no end: the WZ entry has no time). */
    public static Tms282BuffResolver.ResolvedStat flight(int slv, long nowMillis) {
        return flight(WHIMSICAL_DREAM, slv, nowMillis);
    }

    public static Tms282BuffResolver.ResolvedStat flight(int skillId, int slv, long nowMillis) {
        Option o = new Option();
        o.nOption = FLYING_N;
        o.rOption = skillId;
        o.tOption = skillId == net.swordie.ms.client.jobs.flora.Tms282ArkFlight.SKILL
                ? net.swordie.ms.client.jobs.flora.Tms282ArkFlight.durationMillis(slv)
                : skillId == XENON_FREE_FLIGHT ? 30_000 : 0;
        o.slv = slv;
        o.startTime = nowMillis;
        o.tStart = (int) nowMillis;
        o.isInMillis = true;
        if (skillId == net.swordie.ms.client.jobs.flora.Tms282ArkFlight.SKILL)
            return new Tms282BuffResolver.ResolvedStat(CharacterTemporaryStat.NewFlying, o, "Ark Mystical Leap flight ON");
        return new Tms282BuffResolver.ResolvedStat(CharacterTemporaryStat.NewFlying, o,
                skillId == XENON_FREE_FLIGHT ? "Xenon 自由飛行 flight ON"
                        : isVehicleFlight(skillId) ? "mount flight ON" : "Lucid 異想之夢 flight ON");
    }

    /** True while {@code book} holds the NewFlying of 80004201. */
    public static boolean flying(Tms282BuffBook book) {
        List<Option> options = book == null ? null : book.snapshot().get(CharacterTemporaryStat.NewFlying);
        return options != null && options.stream().anyMatch(o -> isFlightSkill(o.rOption));
    }

    /**
     * LP009F for NewFlying alone (the client lands in 5EFEC60 when n becomes 0). Also used when the server refuses a
     * use: the client has already taken off by itself (5A7A3AC), and a reset of a stat it does not hold is harmless.
     */
    public static java.util.Optional<OutPacket> landingReset(TemporaryStatEncoder encoder, int curSize, long nowMillis) {
        return landingReset(encoder, WHIMSICAL_DREAM, curSize, nowMillis);
    }

    public static java.util.Optional<OutPacket> landingReset(TemporaryStatEncoder encoder, int skillId, int curSize,
                                                             long nowMillis) {
        Option o = new Option();
        o.nOption = FLYING_N;
        o.rOption = skillId;
        var selection = encoder.select(Map.of(CharacterTemporaryStat.NewFlying, List.of(o)));
        return selection.isEmpty() ? java.util.Optional.empty()
                : java.util.Optional.of(Tms282SkillPackets.temporaryStatReset(encoder, selection, Map.of(), curSize, nowMillis));
    }

    /**
     * Ends the flight outside a skill packet (untransform, field entry): removes it from the character's published
     * book and sends LP009F to the owner and LP0490 to the field. Returns the log, or null when not flying.
     */
    public static String land(Char chr, String why) {
        if (chr == null || chr.getClient() == null) {
            return null;
        }
        Tms282BuffBook book = Tms282BuffDirectory.INSTANCE.book(chr.getId());
        if (!flying(book)) {
            return null;
        }
        Tms282BuffBook.Expired ended = book.cancelSkill(WHIMSICAL_DREAM);
        if (ended.isEmpty()) {
            ended = book.cancelSkill(XENON_FREE_FLIGHT);
        }
        if (ended.isEmpty()) {
            ended = book.cancelSkill(VEHICLE_FLIGHT_A);
        }
        if (ended.isEmpty()) {
            ended = book.cancelSkill(VEHICLE_FLIGHT_B);
        }
        if (ended.isEmpty()) {
            ended = book.cancelSkill(net.swordie.ms.client.jobs.flora.Tms282ArkFlight.SKILL);
        }
        if (ended.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();
        StringBuilder log = new StringBuilder("[TMS282] Lucid 異想之夢 flight ended (" + why + ") character=" + chr.getId());
        var encoder = ProfileEncoders.find(TemporaryStatEncoder.class);
        if (encoder.isEmpty()) {
            log.append(": no 282 CTS encoder, nothing sent");
        } else {
            var selection = encoder.get().select(ended.removed());
            if (selection.isEmpty()) {
                log.append(": ").append(selection.skipped());
            } else {
                chr.getClient().write(Tms282SkillPackets.temporaryStatReset(encoder.get(), selection,
                        ended.remainingIndie(), ended.remainingKinds(), now));
                log.append(" SENT LP009F ").append(selection.stats().keySet());
            }
        }
        try {
            Tms282RemoteStatEncoder r = remote();
            Tms282RemoteStatEncoder.Reset reset = r.selectReset(ended.removed(), ended.remainingIndie());
            if (!reset.reset().isEmpty() && chr.getField() != null) {
                chr.getField().broadcast(Tms282UserRemotePackets.resetTemporaryStat(chr.getId(), r, reset, now), chr);
                log.append(" FIELD LP0490 ").append(reset.reset().stats().keySet());
            }
        } catch (RuntimeException | java.io.IOException e) {
            log.append(" (no LP0490: ").append(e).append(')');
        }
        return log.toString();
    }

    private static Tms282RemoteStatEncoder remote() throws java.io.IOException {
        Tms282RemoteStatEncoder r = remote;
        if (r == null) {
            synchronized (Tms282LucidFlight.class) {
                if (remote == null) {
                    remote = new Tms282RemoteStatEncoder(Tms282RemoteCtsTable.load(Path.of("data", "protocol", "tms282.3")));
                }
                r = remote;
            }
        }
        return r;
    }
}
