package net.swordie.ms.client.character.keys;

import net.swordie.ms.client.character.Char;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Native TMS 282.3 tables: 6CB550 -> 81DCE20 (89 keys), 6CB580 -> 81DCC70 (32 quick slots).
 * Type 1 is Basic, 2 is Alternative (4B5CC00, StringPool 1B74/1B75); type 0 is the older default.
 * Extracted once from the audited client; the server never reads the executable.
 */
public final class Tms282CreationKeys {
    private Tms282CreationKeys() {}
    private static final int[][] BINDINGS = {
        {1, 4, 46, 2, 4, 10, 3, 4, 12, 4, 4, 13, 5, 4, 18, 6, 4, 23, 16, 4, 8, 17, 4, 5, 18, 4, 0, 19, 4, 4, 20, 4, 27, 22, 4, 39, 23, 4, 1, 24, 4, 41, 25, 4, 19, 27, 4, 15, 29, 5, 52, 31, 4, 2, 34, 4, 17, 35, 4, 11, 37, 4, 3, 38, 4, 20, 39, 4, 26, 40, 4, 16, 41, 4, 22, 43, 4, 9, 44, 5, 50, 45, 5, 51, 46, 4, 6, 47, 4, 31, 48, 4, 29, 50, 4, 7, 56, 5, 53, 57, 5, 54, 59, 6, 100, 60, 6, 101, 61, 6, 102, 63, 6, 103, 64, 6, 104, 65, 6, 105, 66, 6, 106, 70, 4, 47},
        {1, 4, 46, 2, 4, 10, 3, 4, 12, 4, 4, 13, 5, 4, 18, 6, 4, 23, 16, 4, 8, 17, 4, 5, 18, 4, 0, 19, 4, 4, 23, 4, 1, 24, 4, 216, 25, 4, 19, 27, 4, 15, 29, 5, 52, 31, 4, 2, 34, 4, 17, 35, 4, 11, 37, 4, 3, 38, 4, 20, 39, 4, 26, 40, 4, 16, 44, 5, 50, 45, 5, 51, 47, 4, 31, 48, 4, 29, 50, 4, 7, 51, 4, 45, 56, 5, 53, 57, 5, 54, 59, 4, 39, 60, 4, 214, 62, 4, 27, 63, 4, 22, 64, 4, 25, 65, 4, 49, 66, 4, 42, 67, 4, 35, 70, 4, 47, 87, 4, 9},
        {1, 4, 46, 22, 4, 0, 23, 4, 1, 24, 4, 216, 25, 4, 19, 27, 4, 15, 29, 5, 52, 34, 4, 17, 35, 4, 11, 36, 4, 8, 37, 4, 3, 38, 4, 20, 39, 4, 26, 40, 4, 16, 44, 5, 50, 45, 5, 51, 46, 4, 2, 47, 4, 31, 48, 4, 29, 49, 4, 5, 50, 4, 7, 51, 4, 45, 52, 4, 4, 56, 5, 53, 57, 5, 54, 59, 4, 39, 60, 4, 214, 62, 4, 27, 63, 4, 22, 64, 4, 25, 65, 4, 49, 66, 4, 42, 67, 4, 35, 70, 4, 47, 71, 4, 12, 73, 4, 13, 79, 4, 23, 82, 4, 10, 83, 4, 18, 87, 4, 9}
    };
    private static final int[] BASIC_QUICK_SLOTS = {42, 82, 71, 73, 29, 83, 79, 81, 2, 3, 4, 5, 16, 17, 18, 19, 6, 7, 8, 9, 20, 30, 31, 32, 10, 11, 33, 34, 37, 38, 49, 50};
    private static final int[] ALTERNATE_QUICK_SLOTS = {16, 17, 18, 19, 30, 31, 32, 33, 2, 3, 4, 5, 29, 56, 44, 45, 6, 7, 8, 9, 46, 22, 23, 36, 10, 11, 37, 38, 24, 25, 49, 50};

    public static boolean isSupported(int nativeType) { return nativeType >= 0 && nativeType <= 2; }

    public static FuncKeyMap mapping(int characterId, int preset, int nativeType) {
        if (!isSupported(nativeType)) throw new IllegalArgumentException("Unsupported creation key type " + nativeType);
        FuncKeyMap map = new FuncKeyMap();
        map.setCharID(characterId);
        map.setPreset(preset);
        int[] bindings = BINDINGS[nativeType];
        for (int i = 0; i < bindings.length; i += 3)
            map.putKeyBinding(characterId, bindings[i], (byte) bindings[i + 1], bindings[i + 2]);
        return map;
    }

    public static List<Integer> quickSlots(int nativeType) {
        if (!isSupported(nativeType)) throw new IllegalArgumentException("Unsupported creation key type " + nativeType);
        List<Integer> slots = new ArrayList<>(32);
        for (int key : nativeType == 2 ? ALTERNATE_QUICK_SLOTS : BASIC_QUICK_SLOTS) slots.add(key);
        return slots;
    }

    /** Only for a new, unsaved character; never overwrite an existing character's custom bindings. */
    public static void initialize(Char character, int nativeType) {
        if (character.getId() != 0) throw new IllegalArgumentException("Creation keys require an unsaved character");
        var maps = new LinkedHashMap<Integer, FuncKeyMap>();
        for (int preset = 0; preset < 3; preset++) maps.put(preset, mapping(0, preset, nativeType));
        character.setFuncKeyMaps(maps);
        character.setQuickslotKeys(quickSlots(nativeType));
    }
}
