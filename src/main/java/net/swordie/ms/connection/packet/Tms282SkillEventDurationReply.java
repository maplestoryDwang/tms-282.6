package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reply to CP019D ({@link Tms282SkillEventRequests.CheckDuration}): the client lists the stats whose local time ran
 * out and repeats every 200 ms until a LP009F TEMPORARY_STAT_RESET removes them. The server's own reset can be
 * missing (a TSM reset combining an encodable stat with one that has no 282 layout is dropped whole by
 * {@link Tms282ManagedBuffs}; live 09-29: IndieNotDamaged of 122001008 / 142 skills stayed on the client and was
 * asked for 12247 times), so the answer is built from what the server still holds:
 * <ul>
 *     <li>indie stat: LP009F with the server's still-running options of that stat as the remaining list (the
 *     client replaces its list; the expired entry goes);</li>
 *     <li>standard stat: LP009F only when the server holds no running option (otherwise the client asks again and
 *     the server's own expiry answers);</li>
 *     <li>bits without an exact 282 reset layout (not INDIE / STD / STD_EXPLICIT, or skipped by
 *     {@link Tms282TemporaryStatEncoder#selectReset}) are left out and logged once.</li>
 * </ul>
 * "Running" follows each owner's rule: 265 TemporaryStatManager.update (time = tOption &gt; 0 ? tOption : tTerm,
 * seconds unless isInMillis, 0 = no end) for the TSM, Tms282BuffBook.endMillis (ms, indie tTerm / standard
 * tOption, 0 = no end) for the CP04B4 book. The LP009F is Tms282SkillPackets.temporaryStatReset (traced CLEAN at
 * 5FE26F0, skill-events/expected/dur-*.tsv).
 */
public final class Tms282SkillEventDurationReply {
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();

    private Tms282SkillEventDurationReply() {
    }

    /** 265 TemporaryStatManager.update: is the option still running at {@code now}? */
    public static boolean tsmRunning(Option o, long now) {
        if (o == null) {
            return false;
        }
        long time = o.tOption > 0 ? o.tOption : o.tTerm;
        if (time == 0) {
            return true;
        }
        long end = o.startTime + (o.isInMillis ? time : time * 1000L);
        return end >= now;
    }

    /** Tms282BuffBook.endMillis: is the option still running at {@code now}? */
    public static boolean bookRunning(CharacterTemporaryStat cts, Option o, long now) {
        if (o == null) {
            return false;
        }
        long term = cts.isIndie() ? o.tTerm : o.tOption;
        return term == 0 || o.startTime + term > now;
    }

    /** The 265 stats of the request's bits that the encoder can reset exactly (282 bit order). */
    public static Map<Integer, CharacterTemporaryStat> resettable(Tms282TemporaryStatEncoder encoder,
                                                                   Tms282SkillEventRequests.CheckDuration request) {
        Map<Integer, CharacterTemporaryStat> byBit = new java.util.TreeMap<>();
        List<Integer> bits = request.bits();
        if (bits.isEmpty()) {
            return byBit;
        }
        for (CharacterTemporaryStat cts : CharacterTemporaryStat.values()) {
            var row = encoder.rowOf(cts);
            if (row.isEmpty() || !bits.contains(row.get().bit())) {
                continue;
            }
            var kind = row.get().kind();
            if (kind == Tms282CtsTable.Kind.INDIE || kind == Tms282CtsTable.Kind.STD
                    || kind == Tms282CtsTable.Kind.STD_EXPLICIT) {
                byBit.putIfAbsent(row.get().bit(), cts);
            }
        }
        for (int bit : bits) {
            if (!byBit.containsKey(bit) && LOGGED.add("bit:" + bit)) {
                System.out.printf("[TMS282] CP019D asks for CTS bit %d, which has no exact 282 reset: not answered%n", bit);
            }
        }
        return byBit;
    }

    /**
     * LP009F for the request, or empty when nothing is to be removed.
     *
     * @param tsmStats  the character's TemporaryStatManager stats (may be null)
     * @param tsmNow    System.currentTimeMillis() (the TSM clock)
     * @param bookStats the socket's Tms282BuffBook snapshot (may be null)
     * @param bookNow   the book's clock
     */
    public static Optional<OutPacket> reply(TemporaryStatEncoder encoder, Tms282SkillEventRequests.CheckDuration request,
                                            Map<CharacterTemporaryStat, List<Option>> tsmStats, long tsmNow,
                                            Map<CharacterTemporaryStat, List<Option>> bookStats, long bookNow) {
        if (!(encoder instanceof Tms282TemporaryStatEncoder enc) || request == null) {
            return Optional.empty();
        }
        Map<CharacterTemporaryStat, List<Option>> tsm = tsmStats == null ? Map.of() : tsmStats;
        Map<CharacterTemporaryStat, List<Option>> book = bookStats == null ? Map.of() : bookStats;
        var removed = new EnumMap<CharacterTemporaryStat, List<Option>>(CharacterTemporaryStat.class);
        var remaining = new EnumMap<CharacterTemporaryStat, List<Option>>(CharacterTemporaryStat.class);
        for (CharacterTemporaryStat cts : resettable(enc, request).values()) {
            List<Option> live = new ArrayList<>();
            for (Option o : tsm.getOrDefault(cts, List.of())) {
                if (tsmRunning(o, tsmNow)) live.add(o);
            }
            for (Option o : book.getOrDefault(cts, List.of())) {
                if (bookRunning(cts, o, bookNow)) live.add(o);
            }
            if (cts.isIndie()) {
                removed.put(cts, List.of(new Option()));
                remaining.put(cts, List.copyOf(live));
            } else if (live.isEmpty()) {
                removed.put(cts, List.of(new Option()));
            }
        }
        if (removed.isEmpty()) {
            return Optional.empty();
        }
        var selection = enc.selectReset(removed);
        for (var skip : selection.skipped()) {
            if (LOGGED.add("skip:" + skip.stat())) {
                System.out.printf("[TMS282] CP019D reset of %s not sent: %s%n", skip.stat(), skip.reason());
            }
        }
        if (!selection.skipped().isEmpty()) {
            selection.skipped().forEach(s -> removed.remove(s.stat()));
            if (removed.isEmpty()) {
                return Optional.empty();
            }
            selection = enc.selectReset(removed);
        }
        if (selection.stats().isEmpty() || !selection.skipped().isEmpty()) {
            return Optional.empty();
        }
        var held = new java.util.HashSet<CharacterTemporaryStat>();
        tsm.forEach((cts, list) -> { if (list != null && list.stream().anyMatch(o -> tsmRunning(o, tsmNow))) held.add(cts); });
        book.forEach((cts, list) -> { if (list != null && list.stream().anyMatch(o -> bookRunning(cts, o, bookNow))) held.add(cts); });
        int curSize = Math.min(0xFF, held.size());
        return Optional.of(Tms282SkillPackets.temporaryStatReset(enc, selection, remaining, curSize, tsmNow));
    }
}
