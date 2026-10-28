package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * TMS 282.3 local CTS encoder (the Strategy {@link TemporaryStatEncoder}). Layout evidence:
 * research/tms2823/packet-audit/cts-bits-282.md 1-2 (decoder 14F8DF0, reset handler 5FE26F0), checked by emulating
 * the client handlers (research/tms2823/skill-trace).
 * <pre>
 * TEMPORARY_STAT_SET CTS block (14F8DF0):
 *   Buf152 mask            38 x i32, bit k = int[k>>5] |= 1 << (k & 31)                     (14F8E40)
 *   standard area          for each set STD bit in client walk order (1421930): n, i32 r, i32 t
 *                          n is i32 for every entry when the mask meets flag 9EC2DD0, else i16  (156FE20)
 *   i16 0                  count of (i32 key, u8 enable)                                       (14F905A)
 *   u8 0, u8 0, u8 0       DefenseAtt, DefenseState, PvPDamage                                 (14F915D..14F9197)
 *   i32 0                  EtherealForm.x                                                       (14F91B6)
 *   i32 n                  two-state list (14FA498): n x (i32 key = bit - 200, then the entry object's virtual
 *                          decode, vtable +0x30 of the object the populate function 128B390 put in cts+0x7B60):
 *                          key 2 RideVehicle: Buf4 nOption, Buf4 rOption, u8, i32 (13759E5..1375A12) [traced with
 *                          social-trace/twostate_trace.py]; keys 0/6 add i16 (1375CDC), 3 u8 i32 i16, 4 four i32
 *   i32 cnt                indie list, cnt x (i32 indieBit, i32 n, n x entry)                  (128A3A0)
 *     entry: i32 reason, i32 value, i32 key, i32 elapsed, i32 term, i32 0, i32 0, i32 0, i32 0  (9DB110..9DB254)
 * TEMPORARY_STAT_RESET after the four leading bytes (5FE26F0):
 *   Buf152 mask                                                                               (5FE2817)
 *   [mask meets 9E44590]   indie list as above, entries = options still active                (5FE2A89)
 *   [mask meets 9E43800]   u8 movement byte                                                     (5FE2E48)
 *   [bit 567]              u8 (PoseType, 265 writes 1)                                          (5FE2E68)
 *   [bit 816]              u8 (BattleSurvivalDefence)                                           (5FE2EB8)
 *   [bit 524]              i32 (SoulExalt, 0)                                                   (5FE2EE5)
 *   [bit 202 or 206]       u8 mount reset (flag9BD7E00, native initializer37630)                (5FE2F38)
 * </pre>
 * STD/INDIE rows whose 282 client name equals the 265 name are sent, plus every STD_EXPLICIT stat of
 * {@link Tms282CtsExplicit} (n r t in the walker, explicit fields in the client's explicit order: before the count,
 * before the two-state list, or after the indie list; each traced CLEAN, research/tms2823/ctsx/README.md) and the
 * RideVehicle, PartyBooster, GuidedBullet, RideVehicleExpire, RelicGauge and SecondAtomLockOn two-state entries. Other explicit stats remain skipped (see {@link Tms282CtsTable#unsupportedReason}). The reset-only fields of bits 567 and 524 are written
 * by {@link #encodeReset}. Bits 89..199, 209..299 and 1069..1199 are read by nothing and never set.
 */
public final class Tms282TemporaryStatEncoder implements TemporaryStatEncoder {
    /** Reset handler: BattleSurvivalDefence (bit 816) makes it read one more byte (5FE2EA0..5FE2EB8). */
    /**
     * Traced two-state keys (bit - 200). RideVehicle/RelicGauge use the base i32 n,i32 r,u8,i32;
     * PartyBooster adds a relative timestamp (u8,i32) and a u16 duration in seconds.
     * RideVehicleExpire adds a u16 duration to the base (12C1B00).
     * GuidedBullet and SecondAtomLockOn share current/previous mob and user IDs (1376260).
     * Other keys stay skipped. See docs/two-state-buffs-1006.md for complete local/remote reader evidence.
     */
    public static final Map<CharacterTemporaryStat, Integer> TWO_STATE_KEYS = Map.of(
            CharacterTemporaryStat.RideVehicle, 2, CharacterTemporaryStat.PartyBooster, 3,
            CharacterTemporaryStat.GuidedBullet, 4,
            CharacterTemporaryStat.RideVehicleExpire, 6, CharacterTemporaryStat.RelicGauge, 7,
            CharacterTemporaryStat.SecondAtomLockOn, 8);
    public static final int RESET_EXTRA_BYTE_BIT = 816;
    /** Reset handler: PoseType (bit 567) adds a u8 before the bit-816 byte (5FE2E50..5FE2E68, traced ctsx-reset-PoseType). */
    public static final int RESET_POSE_TYPE_BIT = 567;
    /** Reset handler: SoulExalt (bit 524) adds an i32 after the bit-816 byte (5FE2ECD..5FE2EE5, traced ctsx-reset-SoulExalt). */
    public static final int RESET_SOUL_EXALT_BIT = 524;

    private static boolean hasBit(int[] mask, int bit) {
        return (mask[bit >> 5] & (1 << (bit & 31))) != 0;
    }

    private final Tms282CtsTable table;

    public Tms282TemporaryStatEncoder(Tms282CtsTable table) {
        this.table = Objects.requireNonNull(table, "table");
    }

    public Tms282CtsTable table() {
        return table;
    }

    @Override
    public Selection select(Map<CharacterTemporaryStat, List<Option>> stats) {
        Objects.requireNonNull(stats, "stats");
        EnumMap<CharacterTemporaryStat, List<Option>> sent = new EnumMap<>(CharacterTemporaryStat.class);
        List<Skipped> skipped = new ArrayList<>();
        for (Map.Entry<CharacterTemporaryStat, List<Option>> e : stats.entrySet()) {
            CharacterTemporaryStat cts = e.getKey();
            List<Option> options = e.getValue() == null ? List.of() : e.getValue();
            var row = rowOf(cts);
            if (row.isEmpty()) {
                skipped.add(new Skipped(cts, table.unsupportedReason(cts)));
                continue;
            }
            if (options.isEmpty() || options.stream().anyMatch(Objects::isNull)) {
                skipped.add(new Skipped(cts, "no option"));
                continue;
            }
            if (row.get().kind() != Tms282CtsTable.Kind.INDIE && options.size() != 1) {
                skipped.add(new Skipped(cts, "standard stat with " + options.size() + " options"));
                continue;
            }
            if (cts == CharacterTemporaryStat.StopForceAtomInfo && options.getFirst().stopForceAtom == null) {
                skipped.add(new Skipped(cts, "StopForceAtomInfo missing TSM state"));
                continue;
            }
            sent.put(cts, options);
        }
        int[] mask = new int[Tms282CtsTable.MASK_INTS];
        boolean movement = false;
        boolean intWidth = false;
        for (CharacterTemporaryStat cts : sent.keySet()) {
            Tms282CtsTable.Row row = rowOf(cts).orElseThrow();
            mask[row.maskIndex()] |= row.maskValue();
            movement |= movementStat(cts, row);
            intWidth |= row.intWidth();
        }
        if (!intWidth) {
            // Every standard nOption is read as i16 in this packet: a value that does not fit would be cut.
            for (var it = sent.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                Tms282CtsTable.Row row = rowOf(e.getKey()).orElseThrow();
                int n = e.getValue().getFirst().nOption;
                if ((row.kind() == Tms282CtsTable.Kind.STD || row.kind() == Tms282CtsTable.Kind.STD_EXPLICIT)
                        && (n < Short.MIN_VALUE || n > Short.MAX_VALUE)) {
                    skipped.add(new Skipped(e.getKey(), "nOption " + n + " does not fit the i16 the client reads"));
                    mask[row.maskIndex()] &= ~row.maskValue();
                    it.remove();
                }
            }
            movement = sent.keySet().stream().anyMatch(c -> movementStat(c, rowOf(c).orElseThrow()));
        }
        return new Selection(sent, skipped, mask, movement);
    }

    @Override
    public void encodeForLocal(OutPacket out, Selection selection, long nowMillis) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(selection, "selection");
        encodeMask(out, selection.mask());
        boolean intWidth = selection.stats().keySet().stream().anyMatch(c -> row(c).intWidth());
        for (CharacterTemporaryStat cts : ordered(selection, Tms282CtsTable.Kind.STD)) {
            Option o = selection.stats().get(cts).getFirst();
            if (intWidth) {
                out.encodeInt(o.nOption);
            } else {
                out.encodeShort(o.nOption);
            }
            out.encodeInt(o.rOption);
            out.encodeInt(o.tOption);
        }
        // Original282 14F8FAA..14F905A, after the standard walker and before the unconditional count (SoulMP, FullSoulMP).
        Tms282CtsExplicit.write(out, selection.stats(), Tms282CtsExplicit.Area.PRE_COUNT);
        out.encodeShort(0);  // (key, enable) count
        out.encodeByte(0);   // DefenseAtt
        out.encodeByte(0);   // DefenseState
        out.encodeByte(0);   // PvPDamage
        out.encodeInt(0);    // EtherealForm.x
        // Explicit area 1 (14F91CB..14FA48A): Dice .. HolyMagicShell, in client read order.
        Tms282CtsExplicit.write(out, selection.stats(), Tms282CtsExplicit.Area.BEFORE_TWO_STATE);
        List<CharacterTemporaryStat> twoState = ordered(selection, Tms282CtsTable.Kind.TWO_STATE);
        out.encodeInt(twoState.size());
        for (CharacterTemporaryStat cts : twoState) {
            Option o = selection.stats().get(cts).getFirst();
            encodeTwoState(out, cts, o, nowMillis);
        }
        List<CharacterTemporaryStat> indie = ordered(selection, Tms282CtsTable.Kind.INDIE);
        out.encodeInt(indie.size());
        for (CharacterTemporaryStat cts : indie) {
            encodeIndie(out, row(cts).bit(), selection.stats().get(cts), nowMillis);
        }
        // Explicit area 2 (14FA80B..14FCAF2), in client read order: e.g. LefWarriorNobility x, y (14FB6C6 / 14FB6E2,
        // traced rp-out/nob-*), RPEventStat 19 x i32 (14FC28C..14FC484, traced rp-out/rp-b).
        Tms282CtsExplicit.write(out, selection.stats(), Tms282CtsExplicit.Area.AFTER_INDIE);
    }

    /** Explicit i32 fields of RPEventStat in TEMPORARY_STAT_SET (14FC274). */
    public static final int RP_EVENT_STAT_INTS = Tms282CtsExplicit.RP_EVENT_STAT_INTS;

    /**
     * Whether a stat makes TEMPORARY_STAT_SET / RESET carry the movement byte (5FDEE96). The static flag 9E43800
     * (row.movement) does not list the mounts, but the live 282 client under-read our RideVehicle SET by exactly that
     * byte (CP00AE decode error 0x26 on LP009E SET 10-06 22:29 / 10-07 00:01 and, once SET carried it, on the LP009F RESET 10-07 00:23), and 265
     * CharacterTemporaryStat.isMovementAffectingStat counts RideVehicle and RideVehicleExpire. ?典?; switch
     * -Dtms282.cts.rideMovementByte=false restores the flag-only rule.
     */
    static boolean movementStat(CharacterTemporaryStat cts, Tms282CtsTable.Row row) {
        return row.movement() || RIDE_MOVEMENT_BYTE
                && (cts == CharacterTemporaryStat.RideVehicle || cts == CharacterTemporaryStat.RideVehicleExpire);
    }

    private static final boolean RIDE_MOVEMENT_BYTE = Boolean.parseBoolean(System.getProperty("tms282.cts.rideMovementByte", "true"));

    /** Shared local/remote virtual readers: base1375970, PartyBooster12C1A50, RelicGauge12C19B0. */
    static void encodeTwoState(OutPacket out, CharacterTemporaryStat cts, Option o, long nowMillis) {
        out.encodeInt(TWO_STATE_KEYS.get(cts));
        out.encodeInt(o.nOption);
        out.encodeInt(o.rOption);
        long elapsed = o.startTime > 0 ? Math.max(0, nowMillis - o.startTime) : 0;
        boolean timedMount = cts == CharacterTemporaryStat.RideVehicleExpire;
        out.encodeByte(timedMount ? 1 : 0);
        out.encodeInt(timedMount ? (int) Math.min(Integer.MAX_VALUE, elapsed) : 0);
        if (cts == CharacterTemporaryStat.PartyBooster) {
            // 1375DD0: nonzero flag means client now - elapsed, never a server absolute tick.
            out.encodeByte(1);
            out.encodeInt((int) Math.min(Integer.MAX_VALUE, elapsed));
            out.encodeShort(Math.min(65535, Math.max(0, o.isInMillis() ? o.tOption / 1000 : o.tOption)));
        } else if (timedMount) {
            out.encodeShort(Math.min(65535, Math.max(0, o.isInMillis() ? o.tOption / 1000 : o.tOption)));
        } else if (cts == CharacterTemporaryStat.GuidedBullet || cts == CharacterTemporaryStat.SecondAtomLockOn) {
            // Key4/key8 both use1376260: current mob/user, previous mob/user. These skills mark mobs.
            out.encodeInt(o.xOption);
            out.encodeInt(0);
            out.encodeInt(o.yOption);
            out.encodeInt(0);
        }
    }

    @Override
    public Selection selectReset(Map<CharacterTemporaryStat, List<Option>> stats) {
        Selection base = select(stats);
        var sent = new EnumMap<CharacterTemporaryStat, List<Option>>(CharacterTemporaryStat.class);
        sent.putAll(base.stats());
        var skipped = new ArrayList<Skipped>();
        int[] mask = base.mask();
        boolean movement = base.movement();
        for (var skip : base.skipped()) {
            // lucidfly-0930: a RESET body carries only the mask bit of a removed stat (plus the remaining indie options
            // and the fixed reset-only fields encodeReset writes), never the removed option itself. A stat the SET side
            // supports but skipped for its options ("no option": the 265 TSM passes List.of() for an indie stat whose
            // last option ended; several options; an nOption beyond i16) is therefore still reset. Live 09-30:
            // IndieDamR / IndieBooster / IndieNotDamaged resets were refused here and the whole 265 reset was dropped.
            var mapped = rowOf(skip.stat());
            if (mapped.isEmpty()) {
                // These debuffs have extra SET payloads, but RESET only needs their verified mask bits.
                mapped = table.rows().stream().filter(r -> r.name265().equals(skip.stat().name())
                        && r.kind() == Tms282CtsTable.Kind.STD_EXPLICIT
                        && net.swordie.ms.client.character.skills.temp.TemporaryStatManager.DISPELLABLE_DEBUFFS.contains(skip.stat())).findFirst();
            }
            if (mapped.isEmpty()) { skipped.add(skip); continue; }
            sent.put(skip.stat(), resetOptions(stats.get(skip.stat())));
            mask[mapped.get().maskIndex()] |= mapped.get().maskValue();
            movement |= mapped.get().movement();
        }
        return new Selection(sent, skipped, mask, movement);
    }

    /** The removed options of a reset (never written): nulls dropped, so the Selection copy accepts them. */
    private static List<Option> resetOptions(List<Option> options) {
        return options == null ? List.of() : options.stream().filter(Objects::nonNull).toList();
    }

    @Override
    public void encodeReset(OutPacket out, Selection removed, Map<CharacterTemporaryStat, List<Option>> remainingIndie,
                            long nowMillis) {
        Objects.requireNonNull(out, "out");
        Objects.requireNonNull(removed, "removed");
        encodeMask(out, removed.mask());
        List<CharacterTemporaryStat> indie = removed.stats().keySet().stream().filter(CharacterTemporaryStat::isIndie)
                .sorted(Comparator.comparingInt(c -> row(c).bit())).toList();
        if (!indie.isEmpty()) {
            out.encodeInt(indie.size());
            for (CharacterTemporaryStat cts : indie) {
                List<Option> left = remainingIndie == null ? null : remainingIndie.get(cts);
                encodeIndie(out, row(cts).bit(), left == null ? List.of() : left, nowMillis);
            }
        }
        if (removed.movement()) {
            out.encodeByte(1); // 265 temporaryStatReset writes 1 here
        }
        if (hasBit(removed.mask(), RESET_POSE_TYPE_BIT)) {
            out.encodeByte(1); // PoseType u8 (5FE2E68 -> ctx+0x3610); 265 temporaryStatReset writes 1 too
        }
        if (hasBit(removed.mask(), RESET_EXTRA_BYTE_BIT)) {
            out.encodeByte(0);
        }
        if (hasBit(removed.mask(), RESET_SOUL_EXALT_BIT)) {
            out.encodeInt(0);  // SoulExalt i32 (5FE2EE5 -> secure int ctx+0x2600); 265 has no field for it
        }
        if (hasBit(removed.mask(), 202) || hasBit(removed.mask(), 206)) {
            // Static initializer37630 ORs bits202 and206 into9BD7E00; 5FE2F38 reads this for either mount.
            out.encodeByte(0); // Clear vehicle without invoking the forced-ride branch.
        }
    }

    /** The mask as 38 little-endian ints (the client copies 152 raw bytes). */
    public static void encodeMask(OutPacket out, int[] mask) {
        if (mask.length != Tms282CtsTable.MASK_INTS) {
            throw new IllegalArgumentException("282 CTS mask has 38 ints, got " + mask.length);
        }
        for (int value : mask) {
            out.encodeInt(value);
        }
    }

    private static void encodeIndie(OutPacket out, int bit, List<Option> options, long nowMillis) {
        out.encodeInt(bit); // Buf4 indieBit (128A3FE)
        out.encodeInt(options.size());
        for (Option o : options) {
            out.encodeInt(o.nReason);
            out.encodeInt(o.nValue);
            out.encodeInt(o.nKey);
            long elapsed = o.startTime > 0 ? Math.max(0, nowMillis - o.startTime) : 0;
            out.encodeInt((int) Math.min(Integer.MAX_VALUE, elapsed));
            out.encodeInt(o.tTerm);
            out.encodeInt(0);
            out.encodeInt(0); // m x (i32, i32)
            out.encodeInt(0); // k x (i32, i32)
            out.encodeInt(0); // j x (i32 a, i32 p, p x (i64, i32))
        }
    }

    private List<CharacterTemporaryStat> ordered(Selection selection, Tms282CtsTable.Kind kind) {
        return selection.stats().keySet().stream()
                .filter(c -> row(c).kind() == kind || kind == Tms282CtsTable.Kind.STD
                        && row(c).kind() == Tms282CtsTable.Kind.STD_EXPLICIT)
                .sorted(Comparator.comparingInt(c -> kind == Tms282CtsTable.Kind.STD ? row(c).stdOrder() : row(c).bit()))
                .toList();
    }

    private Tms282CtsTable.Row row(CharacterTemporaryStat cts) {
        return rowOf(cts).orElseThrow(() -> new IllegalStateException(cts + " is not supported"));
    }

    /** The supported STD / INDIE row, or the TWO_STATE row of a stat in {@link #TWO_STATE_KEYS}. */
    Optional<Tms282CtsTable.Row> rowOf(CharacterTemporaryStat cts) {
        // STD_EXPLICIT stats with a traced explicit layout (Tms282CtsExplicit): the row must be the spec's bit, walked,
        // with the 282 client name equal to the 265 name (SoulMP, FullSoulMP, LefWarriorNobility, RPEventStat, ...).
        var spec = Tms282CtsExplicit.spec(cts);
        if (spec.isPresent()) {
            return table.row(spec.get().bit()).filter(r -> r.kind() == Tms282CtsTable.Kind.STD_EXPLICIT
                    && r.stdOrder() > 0 && r.name282().equals(cts.name()));
        }
        // 265 calls this critical-rate indie IndieCrR;282/WZ calls the same value IndieCr (bit31).
        // Manager admission remains exact-source guarded in Tms282VMatrixEffects.supports.
        if (cts == CharacterTemporaryStat.IndieCrR) {
            return table.row(31).filter(r -> r.name282().equals("IndieCr") && r.kind() == Tms282CtsTable.Kind.INDIE);
        }
        Optional<Tms282CtsTable.Row> row = table.supportedRow(cts);
        Integer key = TWO_STATE_KEYS.get(cts);
        if (row.isPresent() || key == null) {
            return row;
        }
        return table.row(200 + key).filter(r -> r.kind() == Tms282CtsTable.Kind.TWO_STATE && r.name265().equals(cts.name()));
    }
}
