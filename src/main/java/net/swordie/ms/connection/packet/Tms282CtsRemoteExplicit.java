package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Remote CTS stats the 282.3 remote decoder (14FCC50) reads in <b>two</b> places, which {@link Tms282RemoteCtsTable}
 * leaves unsupported ("2 reads"): one {@link Entry} per read, at its decoder position {@code seq}
 * (research/tms2823/packet-audit/cts-order-282.tsv {@code remote_x}). The remote block is
 * <pre>
 *   mask
 *   segment 1         seq   1..302
 *   u8 u8 u8 i32      seq 303          DefenseAtt, DefenseState, PvPDamage, EtherealForm.x
 *   segment 2         seq 304..326
 *   StopForceAtom, two-state list, indie list      seq 327
 *   segment 3         seq 328..333
 *   u8                seq 334
 *   segment 4         seq 335..
 * </pre>
 * The fields follow 265: the first read is RemoteSecondaryStat's ({@code S n, I r} / {@code B n, I r} /
 * {@code B n} / {@code I n}), the second is what 265 TemporaryStatManager.encodeForRemote writes after the unmasked
 * fields (same position class: before StopForceAtom = segment 2, after the indie list = segment 3, after the u8 =
 * segment 4). SpecterMode's second i32 has no 265 counterpart: xOption, as the local block. Each was traced CLEAN
 * with research/tms2823/skill-trace/remote_trace.py (LP048F 5ADDAD0, LP0490 5ADDD30), alone and combined
 * (tools/Tms282CtsExplicitCheck.java, research/tms2823/skill-trace/ctsx-remote-expected).
 */
public final class Tms282CtsRemoteExplicit {
    /** Decoder positions of the unmasked reads. */
    public static final int FIXED_SEQ = 303;
    public static final int LISTS_SEQ = 327;
    public static final int TAIL_BYTE_SEQ = 334;

    public record Entry(int seq, List<Tms282CtsExplicit.Field> fields) {
        void write(OutPacket out, Option o) {
            for (var f : fields) {
                f.write(out, o);
            }
        }
    }

    public record Spec(CharacterTemporaryStat stat, int bit, List<Entry> entries) {
        /** Width of nOption in the first read ('B', 'S' or 'I'), or 0 when the first read does not carry it. */
        public char nWidth() {
            var first = entries.getFirst().fields().getFirst();
            return first.src() == Tms282CtsExplicit.Src.N ? first.width() : 0;
        }

        public boolean fits(int n) {
            return switch (nWidth()) {
                case 'B' -> n >= Byte.MIN_VALUE && n <= 0xFF;
                case 'S' -> n >= Short.MIN_VALUE && n <= 0xFFFF;
                default -> true;
            };
        }
    }

    /**
     * One write of the remote block at decoder position {@code seq}: an entry of a two-read stat, or ({@code entry}
     * null) the single read of a supported {@link Tms282RemoteCtsTable} row in its {@code shape}.
     */
    public record Write(int seq, Entry entry, Option option, Tms282RemoteCtsTable.Shape shape) {
        public Write(int seq, Entry entry, Option option) {
            this(seq, entry, option, null);
        }

        public void write(OutPacket out) {
            if (entry != null) {
                entry.write(out, option);
            } else {
                shape.write(out, option);
            }
        }
    }

    private static final Map<CharacterTemporaryStat, Spec> SPECS;

    static {
        List<Spec> list = List.of(
                s(CharacterTemporaryStat.ComboCounter, 319, 3, "B:N", 331, "I:X I:Y"),
                s(CharacterTemporaryStat.AntiMagicShell, 504, 109, "S:N I:R", 141, "B:N I:R"),
                s(CharacterTemporaryStat.KeyDownMoving, 472, 161, "S:N I:R", 329, "I:X"),
                s(CharacterTemporaryStat.SerpentScrew, 923, 170, "I:N", 314, "S:X"),
                s(CharacterTemporaryStat.LefBuffMastery, 487, 175, "S:N I:R", 320, "I:X I:C"),
                s(CharacterTemporaryStat.LefGloryWing, 739, 197, "S:N I:R", 319, "I:X I:C"),
                s(CharacterTemporaryStat.SpecterMode, 763, 203, "S:N I:R", 321, "I:X"),
                s(CharacterTemporaryStat.LefWarriorNobility, 851, 234, "S:N I:R", 335, "I:X I:Y"),
                s(CharacterTemporaryStat.BMageAuraYellow, 855, 238, "S:N I:R", 336, "I:X I:Y"),
                s(CharacterTemporaryStat.BMageAuraDrain, 856, 239, "S:N I:R", 337, "I:X I:Y"),
                s(CharacterTemporaryStat.BMageAuraBlue, 857, 240, "S:N I:R", 338, "I:X I:Y I:Z"),
                s(CharacterTemporaryStat.BMageAuraDark, 858, 241, "S:N I:R", 339, "I:X I:Y"),
                s(CharacterTemporaryStat.BMageAuraDebuff, 859, 242, "S:N I:R", 340, "I:X I:Y"),
                s(CharacterTemporaryStat.BMageAuraUnion, 860, 243, "S:N I:R", 341, "I:X I:Y"),
                s(CharacterTemporaryStat.IceAura, 861, 244, "S:N I:R", 342, "I:X I:Y"),
                s(CharacterTemporaryStat.ShadowShield, 869, 252, "S:N I:R", 347, "I:X"));
        EnumMap<CharacterTemporaryStat, Spec> map = new EnumMap<>(CharacterTemporaryStat.class);
        for (Spec spec : list) {
            if (map.put(spec.stat(), spec) != null) {
                throw new IllegalStateException("two remote specs for " + spec.stat());
            }
        }
        SPECS = Collections.unmodifiableMap(map);
    }

    private Tms282CtsRemoteExplicit() {
    }

    private static Spec s(CharacterTemporaryStat stat, int bit, int seq1, String f1, int seq2, String f2) {
        return new Spec(stat, bit, List.of(new Entry(seq1, fields(f1)), new Entry(seq2, fields(f2))));
    }

    private static List<Tms282CtsExplicit.Field> fields(String shape) {
        List<Tms282CtsExplicit.Field> out = new ArrayList<>();
        for (String part : shape.split(" ")) {
            String[] wf = part.split(":");
            out.add(new Tms282CtsExplicit.Field(wf[0].charAt(0), Tms282CtsExplicit.Src.valueOf(wf[1])));
        }
        return List.copyOf(out);
    }

    public static Map<CharacterTemporaryStat, Spec> specs() {
        return SPECS;
    }

    public static Optional<Spec> spec(CharacterTemporaryStat cts) {
        return Optional.ofNullable(SPECS.get(cts));
    }

    /** The widths of an entry, e.g. "S I" (compared with cts-order-282.tsv by the check). */
    public static String reads(Entry e) {
        StringBuilder b = new StringBuilder();
        for (var f : e.fields()) {
            b.append(b.isEmpty() ? "" : " ").append(f.width());
        }
        return b.toString();
    }
}
