package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.RemoteSecondaryStat;
import net.swordie.ms.connection.OutPacket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * TMS 282.3 CharacterTemporaryStat bit table for the <b>remote</b> CTS block ({@link Tms282RemoteStatEncoder}): what
 * the other users' client reads in REMOTE_SET_TEMPORARY_STAT (LP048F) and USER_ENTER_FIELD #24 (LP03D0), both through
 * the remote decoder 14FCC50, and in REMOTE_RESET_TEMPORARY_STAT (LP0490, 5ADDD30).
 * <p>
 * Generated from research/tms2823/packet-audit/cts-order-282.tsv ({@code remote_x}, 351 explicit reads in decoder
 * order) and cts-bits-282.tsv into {@code data/protocol/tms282.3/cts-remote.tsv} by tools/GenerateTms282CtsTable.java;
 * Tms282RemoteBuffCheck fails while the file is out of date.
 * <p>
 * Kinds: {@link Kind#REMOTE} (read by the explicit area, {@code seq} = first read position), {@link Kind#INDIE}
 * (the indie list 128A3A0, read unconditionally with isRemote = 1, same entry format as the local list),
 * {@link Kind#NONE} (the remote decoder never reads the bit). There is no standard (n r t) area in the remote block.
 * <p>
 * A row is <b>supported</b> (the encoder may send it) only when every rule holds; otherwise {@code note} says why:
 * <ul>
 *     <li>REMOTE: the 282 client name equals the 265 name; the bit has exactly one read entry, in the first
 *     explicit segment (before the unmasked {@code B B B I} at 15027BB), of a plain shape {@link Shape};
 *     the 265 {@link RemoteSecondaryStat} writes the same bytes for it (nOption, and rOption for {@code S I}), so
 *     the values keep their 265 meaning; and REMOTE_RESET_TEMPORARY_STAT reads nothing more for it
 *     ({@link #RESET_EXTRA_BITS});</li>
 *     <li>INDIE: the local table's indie rule (282 name equals 265 name; bits 47 and 60 excluded).</li>
 * </ul>
 */
public final class Tms282RemoteCtsTable {
    public static final String FILE = "cts-remote.tsv";
    public static final String COLUMNS = "bit\tname_282\tkind\tseq\tentries\tshape\tname_265\tsupported\tnote";
    /** Remote decoder position of the unmasked {@code B DefenseAtt, B DefenseState, B PvPDamage, I EtherealForm.x}. */
    public static final int FIXED_SEQ = 303;
    /**
     * Bits REMOTE_RESET_TEMPORARY_STAT (5ADDD30) reads more for (traced / disassembled): 567 PoseType u8 (5ADDE62),
     * 816 BattleSurvivalDefence u8 (5ADDEAF), 206 u8 (5ADE146, flag 9BD7E00 = the map entry of key 206), and
     * 646 / 878 / 444 / 807, whose tests call object code (5620 4A0, 56E6330, 567E3D0, 13A7410) not traced here.
     */
    public static final Set<Integer> RESET_EXTRA_BITS = Set.of(206, 444, 567, 646, 807, 816, 878);
    /**
     * Indie bits whose presence in the LP0490 mask makes 5ADDD30 read the indie list: 150AE70 tests bit 81, then
     * ORs the flag constants 9BB83A0 (57), 9BB7F40 (50), 9BB8120 (53), 9BB7E00 (48), 9BB8B20 (69), 9BB7CC0 (46),
     * 9BB8440 (58) (static-initializer state, cts-flags-static-init.pkl) and tests the mask against them.
     */
    public static final Set<Integer> RESET_INDIE_BITS = Set.of(46, 48, 50, 53, 57, 58, 69, 81);

    public enum Kind { REMOTE, INDIE, NONE }

    /** The plain read shapes a supported REMOTE row may have (cts-order-282.tsv {@code reads}). */
    public enum Shape {
        /** {@code u8 nOption}. */
        B("B"),
        /** {@code i16 nOption}. */
        S("S"),
        /** {@code i32 nOption}. */
        I("I"),
        /** {@code i16 nOption, i32 rOption}. */
        S_I("S I"),
        /** The bit is only tested (no field). */
        EMPTY("（無欄位）");

        private final String reads;

        Shape(String reads) {
            this.reads = reads;
        }

        public String reads() {
            return reads;
        }

        public static Optional<Shape> of(String reads) {
            return Arrays.stream(values()).filter(s -> s.reads.equals(reads)).findFirst();
        }

        /** Writes {@code o} in this shape. */
        public void write(OutPacket out, Option o) {
            switch (this) {
                case B -> out.encodeByte(o.nOption);
                case S -> out.encodeShort(o.nOption);
                case I -> out.encodeInt(o.nOption);
                case S_I -> {
                    out.encodeShort(o.nOption);
                    out.encodeInt(o.rOption);
                }
                case EMPTY -> {
                }
            }
        }

        /** Whether nOption survives the width this shape writes it in (signed or unsigned reading). */
        public boolean fits(int nOption) {
            return switch (this) {
                case B -> nOption >= Byte.MIN_VALUE && nOption <= 0xFF;
                case S, S_I -> nOption >= Short.MIN_VALUE && nOption <= 0xFFFF;
                case I, EMPTY -> true;
            };
        }
    }

    /**
     * @param seq     remote decoder position of the bit's (first) read, -1 when not read by the explicit area
     * @param entries number of explicit reads the bit has (0 for INDIE / NONE)
     * @param shape   the read shape when {@code entries == 1} and plain, else null
     */
    public record Row(int bit, String name282, Kind kind, int seq, int entries, Shape shape, String name265,
                      boolean supported, String note) {
        public Row {
            if (bit < 0 || bit > Tms282CtsTable.MAX_BIT) {
                throw new IllegalArgumentException("CTS bit out of range: " + bit);
            }
            Objects.requireNonNull(name282, "name282");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(name265, "name265");
            Objects.requireNonNull(note, "note");
            if (supported && kind == Kind.REMOTE && shape == null) {
                throw new IllegalArgumentException("supported remote bit " + bit + " without a shape");
            }
        }

        public int maskIndex() {
            return bit >>> 5;
        }

        public int maskValue() {
            return 1 << (bit & 31);
        }

        String toTsv() {
            return bit + "\t" + name282 + "\t" + kind + "\t" + seq + "\t" + entries + "\t"
                    + (shape == null ? "-" : shape.name()) + "\t" + name265 + "\t" + (supported ? 1 : 0) + "\t" + note;
        }
    }

    private final List<Row> rows;
    private final Map<Integer, Row> byBit;
    private final Map<CharacterTemporaryStat, Row> supported;

    private Tms282RemoteCtsTable(List<Row> rows) {
        this.rows = List.copyOf(rows);
        Map<Integer, Row> bits = new HashMap<>();
        Map<CharacterTemporaryStat, Row> map = new EnumMap<>(CharacterTemporaryStat.class);
        for (Row row : this.rows) {
            if (bits.putIfAbsent(row.bit(), row) != null) {
                throw new IllegalArgumentException("Duplicate CTS bit " + row.bit());
            }
            if (row.supported()) {
                CharacterTemporaryStat cts = cts(row.name265());
                if (cts == null) {
                    continue;
                }
                if (map.putIfAbsent(cts, row) != null) {
                    throw new IllegalArgumentException("CTS " + cts + " mapped to two 282 bits");
                }
            }
        }
        this.byBit = Collections.unmodifiableMap(bits);
        // Original 282 remote reader 0x1501B26: stage i16, source i32.
        Row arcana = bits.get(340);
        if (arcana != null && arcana.kind() == Kind.REMOTE && arcana.seq() == 262
                && arcana.entries() == 1 && arcana.shape() == Shape.S_I && arcana.name282().equals("Arcana"))
            map.put(CharacterTemporaryStat.Arcana, arcana);
        Row fury = bits.get(1009);
        if (fury != null && fury.kind() == Kind.REMOTE && fury.seq() == 287
                && fury.shape() == Shape.S_I && fury.name282().equals("GlacialFury"))
            map.put(CharacterTemporaryStat.GlacialFury, fury);
        this.supported = Collections.unmodifiableMap(map);
    }

    public static Tms282RemoteCtsTable of(List<Row> rows) {
        return new Tms282RemoteCtsTable(rows);
    }

    /** Loads {@code dir/cts-remote.tsv} (normally data/protocol/tms282.3). */
    public static Tms282RemoteCtsTable load(Path dir) throws IOException {
        return new Tms282RemoteCtsTable(read(dir.resolve(FILE)));
    }

    public List<Row> rows() {
        return rows;
    }

    public Optional<Row> row(int bit) {
        return Optional.ofNullable(byBit.get(bit));
    }

    public Optional<Row> supportedRow(CharacterTemporaryStat cts) {
        return Optional.ofNullable(supported.get(cts));
    }

    public Map<CharacterTemporaryStat, Row> supportedStats() {
        return supported;
    }

    /** Why a 265 stat is not sent to other users (for logs); null when it is. */
    public String unsupportedReason(CharacterTemporaryStat cts) {
        if (supported.containsKey(cts)) {
            return null;
        }
        for (Row row : rows) {
            if (row.name265().equals(cts.name())) {
                return "remote bit " + row.bit() + " " + row.kind() + ": " + row.note();
            }
        }
        return "no 282 bit pairs with 265 " + cts.name();
    }

    // ------------------------------------------------------------------ generation

    /**
     * Builds the rows from the lines of cts-bits-282.tsv and cts-order-282.tsv (headers included). The 265
     * comparison runs the 265 {@link RemoteSecondaryStat#encode} on a probe option.
     */
    public static List<Row> generate(List<String> bitsLines, List<String> orderLines) {
        Map<Integer, Tms282CtsTable.Row> local = new HashMap<>();
        for (Tms282CtsTable.Row r : Tms282CtsTable.generate(bitsLines, orderLines)) {
            local.put(r.bit(), r);
        }
        Map<Integer, List<String[]>> remote = new TreeMap<>();
        boolean header = true;
        for (String line : orderLines) {
            if (header) {
                header = false;
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t", -1);
            if (f[0].equals("remote_x") && !f[2].equals("-")) {
                remote.computeIfAbsent(Integer.parseInt(f[2]), k -> new ArrayList<>()).add(f);
            }
        }
        List<Row> rows = new ArrayList<>();
        header = true;
        for (String line : bitsLines) {
            if (header) {
                header = false;
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t", -1);
            int bit = Integer.parseInt(f[0]);
            String name265 = f[3].isEmpty() ? "-" : f[3];
            Tms282CtsTable.Row loc = local.get(bit);
            List<String[]> reads = remote.getOrDefault(bit, List.of());
            if (loc != null && loc.kind() == Tms282CtsTable.Kind.INDIE) {
                rows.add(new Row(bit, loc.name282(), Kind.INDIE, -1, 0, null, name265, loc.supported(),
                        loc.supported() ? "indie list 128A3A0 (isRemote=1)"
                                : "local indie rule: " + (loc.name282().equals(name265) ? "reset cannot read it" : "282 name " + loc.name282())));
                continue;
            }
            if (reads.isEmpty()) {
                rows.add(new Row(bit, loc == null ? "-" : loc.name282(), Kind.NONE, -1, 0, null, name265, false,
                        "not read by the remote decoder"));
                continue;
            }
            String[] first = reads.getFirst();
            int seq = Integer.parseInt(first[1]);
            String name282 = first[3].endsWith("?") ? "-" : first[3];
            Shape shape = reads.size() == 1 ? Shape.of(first[4]).orElse(null) : null;
            String note;
            if (!name282.equals(name265)) {
                note = "282 name " + first[3] + " != 265 name " + name265;
            } else if (reads.size() != 1) {
                note = reads.size() + " reads (" + String.join(" + ", reads.stream().map(r -> r[4]).toList()) + ")";
            } else if (seq > FIXED_SEQ) {
                note = "read after the unmasked fields (seq " + seq + ")";
            } else if (shape == null) {
                note = "shape " + first[4] + " is not plain";
            } else if (RESET_EXTRA_BITS.contains(bit)) {
                note = "REMOTE_RESET reads more for this bit";
            } else if (cts(name265) == null) {
                note = "265 has no CharacterTemporaryStat " + name265;
            } else if (!agrees265(cts(name265), shape)) {
                note = "265 RemoteSecondaryStat writes " + hex265(cts(name265)) + ", 282 reads " + shape.reads();
            } else {
                note = "same as 265 RemoteSecondaryStat";
            }
            rows.add(new Row(bit, name282, Kind.REMOTE, seq, reads.size(), shape, name265,
                    note.equals("same as 265 RemoteSecondaryStat"), note));
        }
        rows.sort((a, b) -> Integer.compare(a.bit(), b.bit()));
        return rows;
    }

    /** A probe option whose every field differs, so a 265 writer that uses another field is noticed. */
    static Option probeOption() {
        Option o = new Option();
        o.nOption = 0x0102;
        o.rOption = 0x03040506;
        o.tOption = 0x0708090A;
        o.xOption = 0x0B0C;
        o.yOption = 0x0D0E;
        o.zOption = 0x0F10;
        o.bOption = 0x1112;
        o.cOption = 0x1314;
        o.uOption = 0x1516;
        return o;
    }

    static boolean agrees265(CharacterTemporaryStat cts, Shape shape) {
        OutPacket ours = new OutPacket();
        shape.write(ours, probeOption());
        return Arrays.equals(body(ours), body(encode265(cts)));
    }

    private static OutPacket encode265(CharacterTemporaryStat cts) {
        EnumMap<CharacterTemporaryStat, List<Option>> one = new EnumMap<>(CharacterTemporaryStat.class);
        one.put(cts, List.of(probeOption()));
        OutPacket out = new OutPacket();
        RemoteSecondaryStat.encode(out, one);
        return out;
    }

    private static String hex265(CharacterTemporaryStat cts) {
        byte[] b = body(encode265(cts));
        if (b.length == 0) {
            return "nothing";
        }
        StringBuilder s = new StringBuilder();
        for (byte x : b) {
            s.append(String.format("%02x", x));
        }
        return s.toString();
    }

    /** OutPacket() data without a header (the no-arg OutPacket writes none). */
    private static byte[] body(OutPacket p) {
        return p.getData();
    }

    public static String render(List<Row> rows, String bitsSource, String orderSource) {
        StringBuilder out = new StringBuilder();
        out.append("# Generated by tools/GenerateTms282CtsTable.java from ").append(bitsSource).append(" and ")
                .append(orderSource).append(". Do not edit.\n");
        out.append("# Rules: net.swordie.ms.connection.packet.Tms282RemoteCtsTable. supported=1: sent to other users (LP048F / LP03D0 / LP0490).\n");
        out.append(COLUMNS).append('\n');
        for (Row row : rows) {
            out.append(row.toTsv()).append('\n');
        }
        return out.toString();
    }

    public static List<Row> read(Path file) throws IOException {
        List<Row> rows = new ArrayList<>();
        boolean columns = false;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (!columns) {
                if (!line.equals(COLUMNS)) {
                    throw new IOException(file + ": unexpected columns " + line);
                }
                columns = true;
                continue;
            }
            String[] f = line.split("\t", -1);
            if (f.length != 9) {
                throw new IOException(file + ": bad row " + line);
            }
            rows.add(new Row(Integer.parseInt(f[0]), f[1], Kind.valueOf(f[2]), Integer.parseInt(f[3]),
                    Integer.parseInt(f[4]), f[5].equals("-") ? null : Shape.valueOf(f[5]), f[6], f[7].equals("1"), f[8]));
        }
        return rows;
    }

    public Map<String, Integer> summary() {
        Map<String, Integer> counts = new TreeMap<>();
        for (Row row : rows) {
            counts.merge(row.kind() + (row.supported() ? "+supported" : ""), 1, Integer::sum);
        }
        counts.put("supported-265-stats", supported.size());
        return counts;
    }

    private static CharacterTemporaryStat cts(String name) {
        try {
            return CharacterTemporaryStat.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
