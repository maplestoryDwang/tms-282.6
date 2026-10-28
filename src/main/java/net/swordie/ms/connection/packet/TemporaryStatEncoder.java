package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy for the CharacterTemporaryStat part of the local TEMPORARY_STAT_SET / TEMPORARY_STAT_RESET packets.
 * The 265 wire form lives in {@code TemporaryStatManager.encodeForLocal}; {@link Tms282TemporaryStatEncoder} is the
 * TMS 282.3 form. A protocol profile picks one, the packet factories only call this interface.
 * <p>
 * Options follow the 265 conventions after {@code Option.setTimeToMillis()}: standard stats use
 * {@code nOption/rOption/tOption} (tOption in milliseconds), indie stats use {@code nReason/nValue/nKey/tTerm}
 * (tTerm in milliseconds) and {@code startTime} (epoch milliseconds) for the elapsed time.
 */
public interface TemporaryStatEncoder {

    /**
     * Splits {@code stats} into what this encoder can send and what it must drop. Nothing is written; a stat the
     * wire form cannot express exactly is listed in {@link Selection#skipped()} with the reason.
     */
    Selection select(Map<CharacterTemporaryStat, List<Option>> stats);

    default Selection selectReset(Map<CharacterTemporaryStat, List<Option>> stats) { return select(stats); }

    /** Writes the local CTS block of TEMPORARY_STAT_SET (mask, values, fixed fields, lists). */
    void encodeForLocal(OutPacket out, Selection selection, long nowMillis);

    /**
     * Writes the CTS part of TEMPORARY_STAT_RESET that follows the leading bytes: the mask of {@code removed} and
     * whatever that mask makes the client read (indie lists carry {@code remainingIndie}, the options still active
     * for each removed indie stat).
     */
    void encodeReset(OutPacket out, Selection removed, Map<CharacterTemporaryStat, List<Option>> remainingIndie,
                     long nowMillis);

    /** A stat left out, and why. */
    record Skipped(CharacterTemporaryStat stat, String reason) {
        public Skipped {
            Objects.requireNonNull(stat, "stat");
            Objects.requireNonNull(reason, "reason");
        }
    }

    /**
     * Result of {@link #select}: the stats that will be written (insertion order kept by the enum), the skipped
     * ones, the wire mask and the two mask-derived switches the handlers test.
     *
     * @param movement at least one sent bit makes TEMPORARY_STAT_SET / RESET read the movement byte
     */
    record Selection(Map<CharacterTemporaryStat, List<Option>> stats, List<Skipped> skipped, int[] mask,
                     boolean movement) {
        public Selection {
            EnumMap<CharacterTemporaryStat, List<Option>> copy = new EnumMap<>(CharacterTemporaryStat.class);
            Objects.requireNonNull(stats, "stats").forEach((k, v) -> copy.put(k, List.copyOf(v)));
            stats = Collections.unmodifiableMap(copy);
            skipped = List.copyOf(skipped);
            mask = mask.clone();
        }

        @Override
        public int[] mask() {
            return mask.clone();
        }

        public boolean isEmpty() {
            return stats.isEmpty();
        }
    }
}
