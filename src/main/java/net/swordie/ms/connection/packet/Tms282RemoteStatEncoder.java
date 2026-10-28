package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TMS 282.3 remote CTS encoder: the block other users' clients read (remote decoder 14FCC50, LP048F and LP03D0 #24)
 * and the REMOTE_RESET_TEMPORARY_STAT body (5ADDD30). The local form is {@link Tms282TemporaryStatEncoder}; both
 * return the same {@link TemporaryStatEncoder.Selection}. Rows: {@link Tms282RemoteCtsTable}. Layout evidence:
 * cts-bits-282.md 3, ingame-layouts-282.md 1.3, and the emulated handlers (research/tms2823/skill-trace/remote_trace.py).
 * <pre>
 * remote CTS block (14FCC50):
 *   Buf152 mask                                                                        (14FCC95)
 *   explicit area, segment 1: each set supported bit in decoder order (seq), shape B / S / I / S I / none,
 *                               and the first reads of the two-read stats of {@link Tms282CtsRemoteExplicit}
 *   u8 0, u8 0, u8 0, i32 0     DefenseAtt, DefenseState, PvPDamage, EtherealForm.x    (15027BB..1502814)
 *   [segment 2]                 second reads of Tms282CtsRemoteExplicit (SerpentScrew, LefGloryWing, ...)
 *   i32 0, i32 0, i32 0, i32 0  StopForceAtom (3 x i32, n = 0)                          (124ACF1..124AD11)
 *   i32 n                       two-state list (1502F65): n x (i32 key 1502F94, entry virtual decode); only
 *                               RideVehicle (key 2: Buf4 n, Buf4 r, u8, i32, as local, traced with
 *                               social-trace/twostate_trace.py), never the mix vehicle 1992043 (1157730), whose
 *                               RideVehicle makes LP03D0 read #76
 *   i32 cnt, cnt x (i32 indieBit, i32 n, n x entry)   the local indie format            (128A3A0, isRemote = 1)
 *   [segment 3]                 second reads (KeyDownMoving, ComboCounter)
 *   u8 0                                                                               (15031B6)
 *   [segment 4]                 second reads (LefWarriorNobility, BMageAura*, ShadowShield)
 * REMOTE_RESET_TEMPORARY_STAT after the i32 characterId (5ADDD30):
 *   Buf152 mask                                                                        (5ADDE15)
 *   [mask meets {@link Tms282RemoteCtsTable#RESET_INDIE_BITS}] indie list, entries = options still active (128A3A0)
 *   (bits of {@link Tms282RemoteCtsTable#RESET_EXTRA_BITS} would add reads: never supported)
 * </pre>
 * An empty block is 184 bytes ({@link #EMPTY_BLOCK_BYTES}), the LP03D0 value before this encoder existed.
 */
public final class Tms282RemoteStatEncoder {
    /** Mask + u8 u8 u8 i32 + StopForceAtom 16 + two-state i32 + indie i32 + u8. */
    public static final int EMPTY_BLOCK_BYTES = 152 + 3 + 4 + 16 + 4 + 4 + 1;

    private final Tms282RemoteCtsTable table;

    public Tms282RemoteStatEncoder(Tms282RemoteCtsTable table) {
        this.table = Objects.requireNonNull(table, "table");
    }

    public Tms282RemoteCtsTable table() {
        return table;
    }

    /**
     * What an LP0490 must carry after options ended.
     *
     * @param reset          stats in the LP0490 mask (empty: no LP0490)
     * @param remainingIndie per indie stat of {@code reset}, the options still active (the list LP0490 carries when
     *                       the mask meets {@link Tms282RemoteCtsTable#RESET_INDIE_BITS})
     * @param refresh        indie stats that keep options but whose list LP0490 cannot carry: sent again as LP048F
     *                       with the remaining options (the client replaces the stat's list)
     */
    public record Reset(TemporaryStatEncoder.Selection reset, Map<CharacterTemporaryStat, List<Option>> remainingIndie,
                        TemporaryStatEncoder.Selection refresh) {
        public Reset {
            Objects.requireNonNull(reset, "reset");
            EnumMap<CharacterTemporaryStat, List<Option>> copy = new EnumMap<>(CharacterTemporaryStat.class);
            Objects.requireNonNull(remainingIndie, "remainingIndie").forEach((k, v) -> copy.put(k, List.copyOf(v)));
            remainingIndie = Collections.unmodifiableMap(copy);
            Objects.requireNonNull(refresh, "refresh");
        }
    }

    /** Same native virtual entry readers as local SET: RideVehicle, PartyBooster, RideVehicleExpire and RelicGauge. */
    static final Map<CharacterTemporaryStat, Integer> TWO_STATE_KEYS = Tms282TemporaryStatEncoder.TWO_STATE_KEYS;
    /** 1157730: the one vehicle whose RideVehicle makes LP03D0 read the mix-vehicle list #76. */
    public static final int MIX_VEHICLE = 1992043;

    /** The stats of {@code stats} other users can be shown; the rest is listed in {@code skipped} with the reason. */
    public TemporaryStatEncoder.Selection select(Map<CharacterTemporaryStat, List<Option>> stats) {
        Objects.requireNonNull(stats, "stats");
        EnumMap<CharacterTemporaryStat, List<Option>> sent = new EnumMap<>(CharacterTemporaryStat.class);
        List<TemporaryStatEncoder.Skipped> skipped = new ArrayList<>();
        for (Map.Entry<CharacterTemporaryStat, List<Option>> e : stats.entrySet()) {
            CharacterTemporaryStat cts = e.getKey();
            List<Option> options = e.getValue() == null ? List.of() : e.getValue();
            if (cts == CharacterTemporaryStat.StopForceAtomInfo
                    && (options.size() != 1 || options.getFirst() == null || options.getFirst().stopForceAtom == null)) {
                skipped.add(new TemporaryStatEncoder.Skipped(cts, "StopForceAtomInfo missing TSM state"));
                continue;
            }
            if (TWO_STATE_KEYS.containsKey(cts)) {
                if (options.size() != 1 || options.getFirst() == null) {
                    skipped.add(new TemporaryStatEncoder.Skipped(cts, "two-state stat with " + options.size() + " options"));
                } else if (cts == CharacterTemporaryStat.RideVehicle && options.getFirst().nOption == MIX_VEHICLE) {
                    skipped.add(new TemporaryStatEncoder.Skipped(cts, "mix vehicle " + MIX_VEHICLE + " (LP03D0 #76 not built)"));
                } else {
                    sent.put(cts, options);
                }
                continue;
            }
            var spec = explicitSpec(cts);
            if (spec.isPresent()) {
                if (options.size() != 1 || options.getFirst() == null) {
                    skipped.add(new TemporaryStatEncoder.Skipped(cts, "remote stat with " + options.size() + " options"));
                } else if (!spec.get().fits(options.getFirst().nOption)) {
                    skipped.add(new TemporaryStatEncoder.Skipped(cts, "nOption " + options.getFirst().nOption
                            + " does not fit remote " + spec.get().nWidth()));
                } else {
                    sent.put(cts, options);
                }
                continue;
            }
            var row = table.supportedRow(cts);
            if (row.isEmpty()) {
                skipped.add(new TemporaryStatEncoder.Skipped(cts, table.unsupportedReason(cts)));
                continue;
            }
            if (options.isEmpty() || options.stream().anyMatch(Objects::isNull)) {
                skipped.add(new TemporaryStatEncoder.Skipped(cts, "no option"));
                continue;
            }
            if (row.get().kind() == Tms282RemoteCtsTable.Kind.REMOTE) {
                if (options.size() != 1) {
                    skipped.add(new TemporaryStatEncoder.Skipped(cts, "remote stat with " + options.size() + " options"));
                    continue;
                }
                int n = options.getFirst().nOption;
                if (!row.get().shape().fits(n)) {
                    skipped.add(new TemporaryStatEncoder.Skipped(cts, "nOption " + n + " does not fit remote shape "
                            + row.get().shape().reads()));
                    continue;
                }
            }
            sent.put(cts, options);
        }
        return new TemporaryStatEncoder.Selection(sent, skipped, mask(sent.keySet()), false);
    }

    /** Writes the remote CTS block for {@code selection} (the empty block when it is empty). */
    public void encodeForRemote(OutPacket out, TemporaryStatEncoder.Selection selection, long nowMillis) {
        encodeForRemote(out, selection, nowMillis, null);
    }

    /** The StopForceAtom record is unconditional: a partial stat update must retain the active sword state. */
    public void encodeForRemote(OutPacket out, TemporaryStatEncoder.Selection selection, long nowMillis,
                                net.swordie.ms.client.character.skills.StopForceAtom activeSwords) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(selection, "selection");
        Tms282TemporaryStatEncoder.encodeMask(out, selection.mask());
        // Explicit reads in decoder order: supported one-read rows (segment 1) and the reads of the two-read stats of
        // Tms282CtsRemoteExplicit, whose second read may be in segment 2, 3 or 4.
        List<Tms282CtsRemoteExplicit.Write> writes = new ArrayList<>();
        for (CharacterTemporaryStat cts : selection.stats().keySet()) {
            if (TWO_STATE_KEYS.containsKey(cts)) {
                continue;
            }
            Option o = selection.stats().get(cts).getFirst();
            var spec = explicitSpec(cts);
            if (spec.isPresent()) {
                for (var e : spec.get().entries()) {
                    writes.add(new Tms282CtsRemoteExplicit.Write(e.seq(), e, o));
                }
            } else if (row(cts).kind() == Tms282RemoteCtsTable.Kind.REMOTE) {
                Tms282RemoteCtsTable.Shape shape = row(cts).shape();
                writes.add(new Tms282CtsRemoteExplicit.Write(row(cts).seq(), null, o, shape));
            }
        }
        writes.sort(Comparator.comparingInt(Tms282CtsRemoteExplicit.Write::seq));
        int next = writeUpTo(out, writes, 0, Tms282CtsRemoteExplicit.FIXED_SEQ);
        out.encodeByte(0);   // DefenseAtt       (15027BB)
        out.encodeByte(0);   // DefenseState     (15027D8)
        out.encodeByte(0);   // PvPDamage        (15027F5)
        out.encodeInt(0);    // EtherealForm.x   (1502814)
        next = writeUpTo(out, writes, next, Tms282CtsRemoteExplicit.LISTS_SEQ);
        var stopForce = selection.stats().get(CharacterTemporaryStat.StopForceAtomInfo);
        if (stopForce != null) stopForce.getFirst().stopForceAtom.encode(out);
        else if (activeSwords != null) activeSwords.encode(out);
        else out.encodeArr(new byte[16]); // 124ACC0 always reads the empty StopForceAtom as four i32s.
        List<CharacterTemporaryStat> twoState = selection.stats().keySet().stream().filter(TWO_STATE_KEYS::containsKey)
                .sorted(Comparator.comparingInt(TWO_STATE_KEYS::get)).toList();
        out.encodeInt(twoState.size()); // two-state count (1502F65)
        for (CharacterTemporaryStat cts : twoState) {
            Option o = selection.stats().get(cts).getFirst();
            Tms282TemporaryStatEncoder.encodeTwoState(out, cts, o, nowMillis);
        }
        List<CharacterTemporaryStat> indie = ordered(selection, Tms282RemoteCtsTable.Kind.INDIE);
        out.encodeInt(indie.size());
        for (CharacterTemporaryStat cts : indie) {
            encodeIndie(out, row(cts).bit(), selection.stats().get(cts), nowMillis);
        }
        next = writeUpTo(out, writes, next, Tms282CtsRemoteExplicit.TAIL_BYTE_SEQ);
        out.encodeByte(0);   // 15031B6
        writeUpTo(out, writes, next, Integer.MAX_VALUE);
    }

    /** Writes {@code writes[from..]} whose seq is below {@code limit}; returns the next index. */
    private static int writeUpTo(OutPacket out, List<Tms282CtsRemoteExplicit.Write> writes, int from, int limit) {
        int i = from;
        for (; i < writes.size() && writes.get(i).seq() < limit; i++) {
            writes.get(i).write(out);
        }
        return i;
    }

    /** The empty remote block (184 bytes): what LP03D0 carries for a character without a sendable buff. */
    public static void encodeEmpty(OutPacket out) {
        out.encodeArr(new byte[EMPTY_BLOCK_BYTES]);
    }

    /**
     * Splits what ended ({@code removed}: stat -> the options that ended; {@code remainingIndie}: per removed indie
     * stat, the options still active) into the LP0490 and the LP048F refresh (see {@link Reset}).
     */
    public Reset selectReset(Map<CharacterTemporaryStat, List<Option>> removed,
                             Map<CharacterTemporaryStat, List<Option>> remainingIndie) {
        Objects.requireNonNull(removed, "removed");
        Map<CharacterTemporaryStat, List<Option>> left = remainingIndie == null ? Map.of() : remainingIndie;
        EnumMap<CharacterTemporaryStat, List<Option>> reset = new EnumMap<>(CharacterTemporaryStat.class);
        EnumMap<CharacterTemporaryStat, List<Option>> resetLeft = new EnumMap<>(CharacterTemporaryStat.class);
        EnumMap<CharacterTemporaryStat, List<Option>> refresh = new EnumMap<>(CharacterTemporaryStat.class);
        List<TemporaryStatEncoder.Skipped> skipped = new ArrayList<>();
        for (Map.Entry<CharacterTemporaryStat, List<Option>> e : removed.entrySet()) {
            CharacterTemporaryStat cts = e.getKey();
            if (TWO_STATE_KEYS.containsKey(cts)) {
                reset.put(cts, e.getValue() == null ? List.of() : e.getValue());
                continue;
            }
            if (explicitSpec(cts).isPresent()) {
                reset.put(cts, e.getValue() == null ? List.of() : e.getValue());
                continue;
            }
            var row = table.supportedRow(cts);
            if (row.isEmpty()) {
                skipped.add(new TemporaryStatEncoder.Skipped(cts, table.unsupportedReason(cts)));
                continue;
            }
            List<Option> stays = left.getOrDefault(cts, List.of());
            if (row.get().kind() == Tms282RemoteCtsTable.Kind.INDIE && !stays.isEmpty()
                    && !Tms282RemoteCtsTable.RESET_INDIE_BITS.contains(row.get().bit())) {
                refresh.put(cts, stays);
                continue;
            }
            reset.put(cts, e.getValue() == null ? List.of() : e.getValue());
            if (row.get().kind() == Tms282RemoteCtsTable.Kind.INDIE) {
                resetLeft.put(cts, stays);
            }
        }
        TemporaryStatEncoder.Selection resetSel = new TemporaryStatEncoder.Selection(reset, skipped, mask(reset.keySet()), false);
        return new Reset(resetSel, resetLeft, select(refresh));
    }

    /** Writes the LP0490 body after the characterId: mask, and the indie list when the client reads one. */
    public void encodeReset(OutPacket out, Reset reset, long nowMillis) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(reset, "reset");
        TemporaryStatEncoder.Selection sel = reset.reset();
        Tms282TemporaryStatEncoder.encodeMask(out, sel.mask());
        if (readsResetIndie(sel)) {
            List<CharacterTemporaryStat> indie = ordered(sel, Tms282RemoteCtsTable.Kind.INDIE);
            out.encodeInt(indie.size());
            for (CharacterTemporaryStat cts : indie) {
                encodeIndie(out, row(cts).bit(), reset.remainingIndie().getOrDefault(cts, List.of()), nowMillis);
            }
        }
        // Same9BD7E00 flag as local reset: native37630 ORs bits202/206;5ADE146 reads one byte.
        if (sel.stats().containsKey(CharacterTemporaryStat.RideVehicle)
                || sel.stats().containsKey(CharacterTemporaryStat.RideVehicleExpire)) out.encodeByte(0);
    }

    /** 150AE70: the LP0490 mask meets {@link Tms282RemoteCtsTable#RESET_INDIE_BITS}. */
    public boolean readsResetIndie(TemporaryStatEncoder.Selection sel) {
        return sel.stats().keySet().stream()
                .anyMatch(c -> !TWO_STATE_KEYS.containsKey(c) && Tms282RemoteCtsTable.RESET_INDIE_BITS.contains(row(c).bit()));
    }

    private int[] mask(Iterable<CharacterTemporaryStat> stats) {
        int[] mask = new int[Tms282CtsTable.MASK_INTS];
        for (CharacterTemporaryStat cts : stats) {
            Integer key = TWO_STATE_KEYS.get(cts);
            if (key != null) {
                int bit = 200 + key;
                mask[bit >> 5] |= 1 << (bit & 31);
                continue;
            }
            Tms282RemoteCtsTable.Row row = row(cts);
            mask[row.maskIndex()] |= row.maskValue();
        }
        return mask;
    }

    private static void encodeIndie(OutPacket out, int bit, List<Option> options, long nowMillis) {
        out.encodeInt(bit);
        out.encodeInt(options.size());
        for (Option o : options) {
            out.encodeInt(o.nReason);
            out.encodeInt(o.nValue);
            out.encodeInt(o.nKey);
            long elapsed = o.startTime > 0 ? Math.max(0, nowMillis - o.startTime) : 0;
            out.encodeInt((int) Math.min(Integer.MAX_VALUE, elapsed));
            out.encodeInt(o.tTerm);
            out.encodeInt(0);
            out.encodeInt(0);
            out.encodeInt(0);
            out.encodeInt(0);
        }
    }

    private List<CharacterTemporaryStat> ordered(TemporaryStatEncoder.Selection selection, Tms282RemoteCtsTable.Kind kind) {
        return selection.stats().keySet().stream()
                .filter(c -> !TWO_STATE_KEYS.containsKey(c) && row(c).kind() == kind)
                .sorted(Comparator.comparingInt(c -> kind == Tms282RemoteCtsTable.Kind.REMOTE ? row(c).seq() : row(c).bit()))
                .toList();
    }

    private Tms282RemoteCtsTable.Row row(CharacterTemporaryStat cts) {
        var spec = explicitSpec(cts);
        if (spec.isPresent()) {
            return table.row(spec.get().bit()).orElseThrow();
        }
        return table.supportedRow(cts).orElseThrow(() -> new IllegalStateException(cts + " is not sent to other users"));
    }

    /**
     * The two-read spec of {@code cts} ({@link Tms282CtsRemoteExplicit}) when this table agrees: REMOTE row of the
     * spec's bit with the same 282 name, as many read entries as the spec, first read at the spec's first seq, and a
     * bit REMOTE_RESET_TEMPORARY_STAT reads nothing more for.
     */
    java.util.Optional<Tms282CtsRemoteExplicit.Spec> explicitSpec(CharacterTemporaryStat cts) {
        return Tms282CtsRemoteExplicit.spec(cts).filter(s -> table.row(s.bit()).filter(r ->
                r.kind() == Tms282RemoteCtsTable.Kind.REMOTE && r.name282().equals(cts.name())
                        && r.entries() == s.entries().size() && r.seq() == s.entries().getFirst().seq()
                        && !Tms282RemoteCtsTable.RESET_EXTRA_BITS.contains(r.bit())).isPresent());
    }
}
