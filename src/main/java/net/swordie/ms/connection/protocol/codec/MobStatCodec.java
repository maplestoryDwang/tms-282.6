package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.mob.MobStat;
import net.swordie.ms.life.mob.skill.BurnedInfo;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;

/**
 * Wire format of a MobTemporaryStat: flag mask ({@code 1 << (31 - bit % 32)} in int {@code bit / 32}, the same
 * direction in 265 and 282) followed by the values of the set bits. Used by MOB_ENTER_FIELD,
 * MOB_CHANGE_CONTROLLER, MOB_STAT_SET and (mask only) MOB_STAT_RESET.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}.
 */
public interface MobStatCodec {

    /** Mask length in ints (265: 5 = 20 bytes, 282: 6 = 24 bytes). */
    int maskInts();

    /** Mask of the stats this profile can send; the others are left out (see {@link #unsupported}). */
    int[] mask(Collection<MobStat> stats);

    /** Writes {@link #mask}. */
    default void encodeMask(OutPacket out, Collection<MobStat> stats) {
        for (int v : mask(stats)) {
            out.encodeInt(v);
        }
    }

    /** Mask and values (MobTemporaryStat::Decode layout of the profile). */
    void encode(OutPacket out, TreeMap<MobStat, Option> stats, Set<BurnedInfo> burnedInfos, String linkTeam);

    /**
     * Whether the client reads the extra byte after MOB_STAT_SET / MOB_STAT_RESET: the mask intersects the
     * profile's movement stats (282: 0x9E3F020 flag test at 0xED0420).
     */
    boolean hasMovementAffectingStat(Collection<MobStat> stats);

    /** Stats of {@code stats} that this profile never writes (no bit with the same name in its table). */
    Set<MobStat> unsupported(Collection<MobStat> stats);
}
