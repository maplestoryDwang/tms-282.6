package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TMS 282.3 CharacterTemporaryStat bit table for the local encoder ({@link Tms282TemporaryStatEncoder}).
 * <p>
 * Generated from research/tms2823/packet-audit/cts-bits-282.tsv and cts-order-282.tsv (static analysis of
 * 14F8DF0 / 1421930, see cts-bits-282.md) into {@code data/protocol/tms282.3/cts-local.tsv} by
 * tools/GenerateTms282CtsTable.java; Tms282SkillPacketsCheck fails while the file is out of date.
 * <p>
 * Kinds (how the local decoder 14F8DF0 reads a set bit):
 * <ul>
 *     <li>{@link Kind#STD}: only the standard walker ({@code n r t}, order column);</li>
 *     <li>{@link Kind#STD_EXPLICIT}: the standard walker <b>and</b> extra fields in the explicit area;</li>
 *     <li>{@link Kind#EXPLICIT}: only explicit fields;</li>
 *     <li>{@link Kind#INDIE}: only the indie list ({@code indie key = bit});</li>
 *     <li>{@link Kind#TWO_STATE}: only the two-state list ({@code key = bit - 200});</li>
 *     <li>{@link Kind#NONE}: never read.</li>
 * </ul>
 * A row is <b>supported</b> when the encoder may send it: an STD or INDIE row whose 282 client name equals the 265
 * CharacterTemporaryStat name (and that enum constant exists). Everything else is skipped by the encoder, which
 * never sends a guessed layout. Indie bits 47 and 60 are excluded as well: the TEMPORARY_STAT_RESET handler reads
 * its indie list only when the mask meets flag 9E44590, which (emulated static initializer 7A210) holds every
 * indie bit 0..87 except those two, so a reset of them could not carry their remaining entries.
 */
public final class Tms282CtsTable {
    public static final String FILE = "cts-local.tsv";
    public static final String COLUMNS = "bit\tname_282\tkind\tstd_order\tint_width\tmovement\tname_265\tsupported";
    public static final int MASK_INTS = 38;
    public static final int MAX_BIT = MASK_INTS * 32 - 1;
    /** Indie bits the reset handler cannot read (see class comment). */
    public static final List<Integer> RESET_UNREADABLE_INDIE = List.of(47, 60);

    private static final Pattern NAME_282 = Pattern.compile("282名稱=([A-Za-z0-9_]+)");

    public enum Kind { STD, STD_EXPLICIT, EXPLICIT, INDIE, TWO_STATE, NONE }

    /**
     * @param stdOrder   position in the client's standard walk (1..753), -1 when not walked
     * @param intWidth   the bit is in flag 9EC2DD0: a packet containing it reads every standard nOption as 4 bytes
     * @param movement   the bit is in flag 9E43800: TEMPORARY_STAT_SET / RESET read one extra byte
     * @param name265    265 CharacterTemporaryStat name paired by the research table, "-" when none
     */
    public record Row(int bit, String name282, Kind kind, int stdOrder, boolean intWidth, boolean movement,
                      String name265, boolean supported) {
        public Row {
            if (bit < 0 || bit > MAX_BIT) {
                throw new IllegalArgumentException("CTS bit out of range: " + bit);
            }
            Objects.requireNonNull(name282, "name282");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(name265, "name265");
        }

        public int maskIndex() {
            return bit >>> 5;
        }

        /** 282 direction: {@code 1 << (bit & 31)} (cts-bits-282.md 1.1; 265 uses the opposite direction). */
        public int maskValue() {
            return 1 << (bit & 31);
        }

        String toTsv() {
            return bit + "\t" + name282 + "\t" + kind + "\t" + stdOrder + "\t" + (intWidth ? 1 : 0) + "\t"
                    + (movement ? 1 : 0) + "\t" + name265 + "\t" + (supported ? 1 : 0);
        }
    }

    private final List<Row> rows;
    private final Map<Integer, Row> byBit;
    private final Map<CharacterTemporaryStat, Row> supported;

    private Tms282CtsTable(List<Row> rows) {
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
                    continue; // paired name missing from this 265 build: stays unsupported
                }
                if (map.putIfAbsent(cts, row) != null) {
                    throw new IllegalArgumentException("CTS " + cts + " mapped to two 282 bits");
                }
            }
        }
        this.byBit = Collections.unmodifiableMap(bits);
        // Original282 local standard walker entry515: new stat has no legacy265 name in the source table.
        Row destiny = bits.get(805);
        if (destiny != null && destiny.kind() == Kind.STD && destiny.stdOrder() == 515
                && destiny.name282().equals("Destiny2ndWeaponIndomitable"))
            map.put(CharacterTemporaryStat.Destiny2ndWeaponIndomitable, destiny);
        // 282 Arcana: 0x142560F, n/r/t; no legacy 265 enum or payload changes.
        Row arcana = bits.get(340);
        if (arcana != null && arcana.kind() == Kind.STD && arcana.stdOrder() == 83
                && arcana.name282().equals("Arcana"))
            map.put(CharacterTemporaryStat.Arcana, arcana);
        Row fury = bits.get(1009);
        if (fury != null && fury.kind() == Kind.STD && fury.stdOrder() == 740
                && fury.name282().equals("GlacialFury"))
            map.put(CharacterTemporaryStat.GlacialFury, fury);
        this.supported = Collections.unmodifiableMap(map);
    }

    public static Tms282CtsTable of(List<Row> rows) {
        return new Tms282CtsTable(rows);
    }

    /** Loads {@code dir/cts-local.tsv} (normally data/protocol/tms282.3). */
    public static Tms282CtsTable load(Path dir) throws IOException {
        return new Tms282CtsTable(read(dir.resolve(FILE)));
    }

    public List<Row> rows() {
        return rows;
    }

    public Optional<Row> row(int bit) {
        return Optional.ofNullable(byBit.get(bit));
    }

    /** The 282 row a 265 stat is sent as, empty when the stat is not supported. */
    public Optional<Row> supportedRow(CharacterTemporaryStat cts) {
        return Optional.ofNullable(supported.get(cts));
    }

    public Map<CharacterTemporaryStat, Row> supportedStats() {
        return supported;
    }

    /** Why a 265 stat is not supported (for logs); null when it is. */
    public String unsupportedReason(CharacterTemporaryStat cts) {
        if (supported.containsKey(cts)) {
            return null;
        }
        for (Row row : rows) {
            if (row.name265().equals(cts.name())) {
                if (row.supported()) {
                    return "282 bit " + row.bit() + " unsupported";
                }
                if (row.kind() != Kind.STD && row.kind() != Kind.INDIE) {
                    return "282 bit " + row.bit() + " is " + row.kind() + " (only standard n/r/t and indie entries are encoded)";
                }
                if (!row.name282().equals(cts.name())) {
                    return "282 bit " + row.bit() + " has client name " + row.name282() + ", not " + cts.name();
                }
                return "indie bit " + row.bit() + " cannot be reset (not in flag 9E44590)";
            }
        }
        return "no 282 bit pairs with 265 " + cts.name();
    }

    // ------------------------------------------------------------------ generation

    /** Builds the rows from the lines of cts-bits-282.tsv and cts-order-282.tsv (headers included). */
    public static List<Row> generate(List<String> bitsLines, List<String> orderLines) {
        Map<Integer, Integer> stdOrder = new HashMap<>();
        Map<Integer, String> stdName = new HashMap<>();
        Map<Integer, String> explicitName = new HashMap<>();
        boolean header = true;
        for (String line : orderLines) {
            if (header) {
                header = false;
                if (!line.startsWith("section\tseq\tbit_282\tname_282\treads")) {
                    throw new IllegalArgumentException("Unexpected cts-order header: " + line);
                }
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t", -1);
            if (f[2].equals("-")) {
                continue;
            }
            int bit = Integer.parseInt(f[2]);
            switch (f[0]) {
                case "local_std" -> {
                    if (stdOrder.put(bit, Integer.parseInt(f[1])) != null) {
                        throw new IllegalArgumentException("Bit " + bit + " walked twice");
                    }
                    stdName.put(bit, f[3]);
                }
                case "local_x" -> explicitName.putIfAbsent(bit, f[3]);
                default -> {
                }
            }
        }
        List<Row> rows = new ArrayList<>();
        header = true;
        for (String line : bitsLines) {
            if (header) {
                header = false;
                if (!line.startsWith("bit_282\tint_index\tmask\tname_265\tbit_265\tread_sequence")) {
                    throw new IllegalArgumentException("Unexpected cts-bits header: " + line);
                }
                continue;
            }
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split("\t", -1);
            int bit = Integer.parseInt(f[0]);
            String name265 = f[3].isEmpty() ? "-" : f[3];
            String seq = f[5];
            String evidence = f[7];
            Kind kind;
            if (seq.startsWith("IND:")) {
                kind = Kind.INDIE;
            } else if (seq.startsWith("TS:")) {
                kind = Kind.TWO_STATE;
            } else if (stdOrder.containsKey(bit)) {
                kind = explicitName.containsKey(bit) ? Kind.STD_EXPLICIT : Kind.STD;
            } else if (explicitName.containsKey(bit)) {
                kind = Kind.EXPLICIT;
            } else {
                kind = Kind.NONE;
            }
            String name282 = stdName.getOrDefault(bit, explicitName.getOrDefault(bit, "-"));
            if (kind == Kind.INDIE) {
                Matcher m = NAME_282.matcher(evidence);
                name282 = m.find() ? m.group(1) : "-";
            }
            if (name282.endsWith("?")) {
                name282 = "-"; // research marks unconfirmed names with '?'
            }
            boolean supported = switch (kind) {
                case STD -> name282.equals(name265);
                case INDIE -> name282.equals(name265) && !RESET_UNREADABLE_INDIE.contains(bit);
                default -> false;
            };
            rows.add(new Row(bit, name282, kind, stdOrder.getOrDefault(bit, -1),
                    evidence.contains("int 寬度集合 0x9EC2DD0"), evidence.contains("移動類集合 0x9E43800"),
                    name265, supported));
        }
        rows.sort((a, b) -> Integer.compare(a.bit(), b.bit()));
        return rows;
    }

    /** File text of {@link #FILE}. */
    public static String render(List<Row> rows, String bitsSource, String orderSource) {
        StringBuilder out = new StringBuilder();
        out.append("# Generated by tools/GenerateTms282CtsTable.java from ").append(bitsSource).append(" and ")
                .append(orderSource).append(". Do not edit.\n");
        out.append("# Rules: net.swordie.ms.connection.packet.Tms282CtsTable. supported=1: STD/INDIE row whose 282 client name equals the 265 name.\n");
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
            if (f.length != 8) {
                throw new IOException(file + ": bad row " + line);
            }
            rows.add(new Row(Integer.parseInt(f[0]), f[1], Kind.valueOf(f[2]), Integer.parseInt(f[3]),
                    f[4].equals("1"), f[5].equals("1"), f[6], f[7].equals("1")));
        }
        return rows;
    }

    /** Per-kind counts, for logs and checks. */
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
