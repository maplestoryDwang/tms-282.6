package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback for buffs the 265 TemporaryStatManager sends (summon hits, job passives, quest/script buffs) that the
 * {@code Tms282SkillPackets.fromManager*} allowlist does not name: they used to be dropped by the outbound guard
 * (live 2026-09-27: TEMPORARY_STAT_SET x16, RESET x8). The 282 encoder already refuses anything it cannot write
 * exactly (only STD/INDIE rows whose 282 client name equals the 265 name, one option per standard stat, nOption in
 * i16; explicit/two-state stats skipped), and the CP04B4 buff-skill route sends its LP009E through the same encoder.
 * So a SET is built only when the encoder skips nothing (a RESET: for the mapped stats, see {@link #reset});
 * otherwise the 265 packet is still dropped and each
 * skipped stat is logged once, which lists what a future port must add (e.g. KinesisPsychicPoint has no 282 row).
 */
public final class Tms282ManagedBuffs {
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    /**
     * Stats whose client meaning depends on the option's source (icon / soul gauge by reason): only the
     * {@code fromManager*} allowlist sends them, for its verified reasons (Tms282SaitamaPsychicCheck: other summon
     * icons stay fail-closed).
     */
    static final Set<CharacterTemporaryStat> SOURCE_DEPENDENT = Set.of(CharacterTemporaryStat.IndieBuffIcon,
            CharacterTemporaryStat.SoulMP, CharacterTemporaryStat.FullSoulMP);

    private Tms282ManagedBuffs() {
    }

    public static Optional<OutPacket> set(TemporaryStatEncoder encoder, Map<CharacterTemporaryStat, List<Option>> stats,
                                          int curSize, boolean hide) {
        if (stats.isEmpty() || stats.keySet().stream().anyMatch(SOURCE_DEPENDENT::contains)) {
            return Optional.empty();
        }
        var selection = encoder.select(stats);
        if (!selection.skipped().isEmpty() || selection.stats().isEmpty()) {
            logSkipped("set", selection);
            return Optional.empty();
        }
        return Optional.of(Tms282SkillPackets.temporaryStatSet(encoder, selection, curSize, System.currentTimeMillis(), hide));
    }

    public static Optional<OutPacket> reset(TemporaryStatEncoder encoder, Map<CharacterTemporaryStat, List<Option>> removed,
                                            Map<CharacterTemporaryStat, List<Option>> remaining, int curSize) {
        if (removed.isEmpty()) {
            return Optional.empty();
        }
        // lucidfly-0930: a reset is sent for every stat the 282 table maps and only the unmapped ones are left out
        // (logged once); before, one unmapped or option-less stat dropped the whole 265 reset and the client kept the
        // buff (live 09-30: TEMPORARY_STAT_RESET len 146/147/178/210 dropped, chars 38/39). A reset writes no option,
        // only the mask bit, so a source-dependent stat is safe to reset too, except an indie one whose remaining
        // options (re-written by the indie list) are not all from verified sources: that one stays with its old path.
        Map<CharacterTemporaryStat, List<Option>> admitted = new java.util.EnumMap<>(CharacterTemporaryStat.class);
        admitted.putAll(removed);
        for (CharacterTemporaryStat cts : removed.keySet()) {
            if (SOURCE_DEPENDENT.contains(cts) && cts.isIndie() && remaining != null
                    && remaining.get(cts) != null && !remaining.get(cts).isEmpty()) {
                admitted.remove(cts);
                if (LOGGED.add("reset-source:" + cts)) {
                    System.out.printf("[TMS282] buff reset not shown (source-dependent with options left): %s%n", cts);
                }
            }
        }
        if (admitted.isEmpty()) {
            return Optional.empty();
        }
        var selection = encoder.selectReset(admitted);
        logSkipped("reset", selection);
        if (selection.stats().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Tms282SkillPackets.temporaryStatReset(encoder, selection, remaining, curSize, System.currentTimeMillis()));
    }

    private static void logSkipped(String kind, TemporaryStatEncoder.Selection selection) {
        for (var s : selection.skipped()) {
            if (LOGGED.add(kind + ":" + s.stat())) {
                System.out.printf("[TMS282] buff %s not shown (no exact 282 layout): %s - %s%n", kind, s.stat(), s.reason());
            }
        }
    }
}
