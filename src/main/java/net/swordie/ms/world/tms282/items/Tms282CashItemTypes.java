package net.swordie.ms.world.tms282.items;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * The 282.3 client's GetCashItemType (AEFA50) and teleport subtype (AF16F0) per cash item, emulated offline by
 * {@code research/tms2823/item-usability-20260928/cash-use/gen_cash_types.py} into
 * {@code data/resources/tms282/cashuse/cash-item-type.tsv} ({@code itemId, type (hex), teleportKind, choice}).
 * The server cannot run the client's switch, so this table decides which CP0150 layout an item uses.
 */
public final class Tms282CashItemTypes {
    /**
     * @param map          Item.wz info/map (fixed-destination teleport stones 5042xxx), 0 when absent
     * @param recoveryRate Item.wz info/recoveryRate (safety charms: HP/MP percent on revive), 0 when absent
     */
    public record Entry(int itemId, int type, int teleportKind, boolean choice, int map, int recoveryRate) {
        public Entry(int itemId, int type, int teleportKind, boolean choice) {
            this(itemId, type, teleportKind, choice, 0, 0);
        }

        /** Client cash type 0x07 with a recovery rate: death protection (5130000, 5130002, 5130003, 5131000). */
        public boolean safetyCharm() {
            return type == 0x07 && recoveryRate > 0;
        }
    }

    private static volatile Tms282CashItemTypes shared;
    private final Map<Integer, Entry> entries;

    Tms282CashItemTypes(Map<Integer, Entry> entries) {
        this.entries = Map.copyOf(entries);
    }

    public static Path defaultFile() {
        return Path.of(System.getProperty("tms282.boxesDir", "data/resources/tms282"), "cashuse", "cash-item-type.tsv");
    }

    /** Loaded once from {@link #defaultFile()}; a missing file gives an empty table (every item is left unclaimed). */
    public static Tms282CashItemTypes get() {
        Tms282CashItemTypes t = shared;
        if (t == null) {
            synchronized (Tms282CashItemTypes.class) {
                if (shared == null) {
                    Path file = defaultFile();
                    if (Files.isRegularFile(file)) {
                        shared = load(file);
                    } else {
                        System.out.println("[TMS282] cash item type table missing: " + file.toAbsolutePath());
                        shared = new Tms282CashItemTypes(Map.of());
                    }
                }
                t = shared;
            }
        }
        return t;
    }

    public static Tms282CashItemTypes load(Path file) {
        Map<Integer, Entry> map = new HashMap<>();
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#") || !Character.isDigit(line.charAt(0))) {
                    continue;
                }
                String[] c = line.split("\t");
                int id = Integer.parseInt(c[0].trim());
                String t = c[1].trim();
                int type = t.startsWith("0x") || t.startsWith("0X") ? Integer.parseInt(t.substring(2), 16) : Integer.parseInt(t);
                int kind = c.length > 2 ? Integer.parseInt(c[2].trim()) : -1;
                boolean choice = c.length > 3 && "1".equals(c[3].trim());
                int destination = c.length > 4 && !c[4].isBlank() ? Integer.parseInt(c[4].trim()) : 0;
                int recovery = c.length > 5 && !c[5].isBlank() ? Integer.parseInt(c[5].trim()) : 0;
                map.put(id, new Entry(id, type, kind, choice, destination, recovery));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new Tms282CashItemTypes(map);
    }

    public OptionalInt type(int itemId) {
        Entry e = entries.get(itemId);
        return e == null ? OptionalInt.empty() : OptionalInt.of(e.type());
    }

    public Entry entry(int itemId) {
        return entries.get(itemId);
    }

    public int size() {
        return entries.size();
    }
}
