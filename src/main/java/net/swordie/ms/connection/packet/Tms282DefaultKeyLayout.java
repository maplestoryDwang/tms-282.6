package net.swordie.ms.connection.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Immutable source data for LP0018 (3CA8690), the server default key layout used by the KeyConfig
 * "default" button (3AF3710). It is not a security-module table (unknown-lp-2823.md).
 * <p>
 * The client parses each string with 4484D0 as {@code key=value;key=value;...}, at most 255 bytes per
 * string (448290). The four key strings fill one 89-entry table of {u8 type, i32 id} (6CBE80); the
 * client derives the type from the id and drops ids of no known type, so {@link Binding} rejects
 * any type that disagrees with {@link #functionType(int)}. The fifth string maps quick slot
 * "0".."31" to a key index 0..88 (6CC120).
 */
public final class Tms282DefaultKeyLayout {
    public static final int KEY_COUNT = 89;
    public static final int QUICK_SLOT_COUNT = 32;
    /** Table 867ABA0 = {0x214, 0x215, 0x216, 0x217}: the client reads exactly four key strings. */
    public static final int KEY_STRING_COUNT = 4;
    /** 448290 rejects longer strings. */
    public static final int MAX_STRING_BYTES = 255;

    /** One key: FuncKeyMapped type (4 menu, 5 basic action, 6 emotion) and its function id. */
    public record Binding(int type, int id) {
        public Binding {
            int expected = functionType(id);
            if (expected == 0) {
                throw new IllegalArgumentException("Function id " + id + " has no LP0018 type");
            }
            if (type != expected) {
                throw new IllegalArgumentException("Function id " + id + " is type " + expected + ", not " + type);
            }
        }

        /** Binding whose type is derived the way the client derives it. */
        public static Binding of(int id) {
            return new Binding(functionType(id), id);
        }
    }

    private final SortedMap<Integer, Binding> keys;
    private final int[] quickSlotKeys;

    /**
     * @param keys          key index 0..88 -> binding; unlisted keys keep the client's default
     * @param quickSlotKeys exactly 32 key indexes 0..88, quick slot 0 first
     */
    public Tms282DefaultKeyLayout(Map<Integer, Binding> keys, int[] quickSlotKeys) {
        Objects.requireNonNull(keys, "keys");
        Objects.requireNonNull(quickSlotKeys, "quickSlotKeys");
        TreeMap<Integer, Binding> sorted = new TreeMap<>();
        for (Map.Entry<Integer, Binding> entry : keys.entrySet()) {
            checkKeyIndex(Objects.requireNonNull(entry.getKey(), "key index"), "Key index");
            sorted.put(entry.getKey(), Objects.requireNonNull(entry.getValue(), "binding"));
        }
        if (quickSlotKeys.length != QUICK_SLOT_COUNT) {
            throw new IllegalArgumentException("Quick-slot layout must have exactly 32 entries");
        }
        for (int key : quickSlotKeys) {
            checkKeyIndex(key, "Quick-slot key index");
        }
        this.keys = Collections.unmodifiableSortedMap(sorted);
        this.quickSlotKeys = quickSlotKeys.clone();
    }

    /** The client's type for a function id (4, 5 or 6), or 0 when the client ignores the id. */
    public static int functionType(int id) {
        if (id >= 50 && id <= 54) {
            return 5;
        }
        if (id >= 100 && id <= 106) {
            return 6;
        }
        boolean menu = (id >= 0 && id <= 13) || (id >= 15 && id <= 23) || (id >= 25 && id <= 27)
                || id == 29 || id == 31 || id == 34 || id == 35 || id == 39 || (id >= 41 && id <= 43)
                || (id >= 45 && id <= 49) || (id >= 55 && id <= 61) || (id >= 200 && id <= 207) || id == 210
                || (id >= 212 && id <= 216) || id == 218 || id == 300 || id == 1000 || id == 1001;
        return menu ? 4 : 0;
    }

    public SortedMap<Integer, Binding> keys() {
        return keys;
    }

    public int[] quickSlotKeys() {
        return quickSlotKeys.clone();
    }

    /**
     * Exactly four "key=id;" strings in key order, each filled up to 255 bytes before the next starts.
     * 89 keys of at most 8 bytes ("88=1001;") always fit, so the overflow check is only a guard.
     */
    public List<String> keyStrings() {
        List<StringBuilder> parts = new ArrayList<>();
        parts.add(new StringBuilder());
        for (Map.Entry<Integer, Binding> entry : keys.entrySet()) {
            String pair = entry.getKey() + "=" + entry.getValue().id() + ";";
            if (parts.get(parts.size() - 1).length() + pair.length() > MAX_STRING_BYTES) {
                if (parts.size() == KEY_STRING_COUNT) {
                    throw new IllegalStateException("Key layout exceeds 4 x 255 bytes");
                }
                parts.add(new StringBuilder());
            }
            parts.get(parts.size() - 1).append(pair);
        }
        List<String> strings = new ArrayList<>(KEY_STRING_COUNT);
        for (int i = 0; i < KEY_STRING_COUNT; i++) {
            strings.add(i < parts.size() ? parts.get(i).toString() : "");
        }
        return List.copyOf(strings);
    }

    /** "slot=keyIndex;" for slots 0..31; at most 32 x 6 bytes, always within 255. */
    public String quickSlotString() {
        StringBuilder text = new StringBuilder();
        for (int slot = 0; slot < QUICK_SLOT_COUNT; slot++) {
            text.append(slot).append('=').append(quickSlotKeys[slot]).append(';');
        }
        return text.toString();
    }

    private static void checkKeyIndex(int key, String what) {
        if (key < 0 || key >= KEY_COUNT) {
            throw new IllegalArgumentException(what + " must be between 0 and 88");
        }
    }
}
