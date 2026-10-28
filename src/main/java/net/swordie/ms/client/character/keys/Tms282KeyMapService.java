package net.swordie.ms.client.character.keys;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.packet.Tms282FuncKeyModifiedRequest.KeyChange;
import net.swordie.ms.connection.packet.Tms282FuncKeyModifiedRequest.KeyChanges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TMS 282.3 key-map state on top of the 265 model ({@link FuncKeyMap}/{@link Keymapping}, tables funckeymap and
 * keymaps). Wire formats live in Tms282KeyPackets / Tms282FuncKeyModifiedRequest; this class only decides what the
 * server holds and writes it back with the 265 persistence methods.
 * <p>
 * Consistency rule: CP020F type 0 carries only the keys that differ from the client's snapshot of the last LP0B16,
 * so the server must know every key the client was sent. Presets 0..2 are therefore always sent explicitly: an absent
 * or empty preset is filled with the 265 default ({@link FuncKeyMap#getDefaultMapping}, key setting type 0, as
 * 265 Char.initKeyBoards does). Preset 3 is new in 282 and has no 265 default; while it has no saved bindings it is
 * sent as {@code u8 1} (client table 6CB530, contents not recovered). Its first change seeds it with the same 265
 * default, so untouched keys of preset 3 can differ from what the client showed until the next LP0B16.
 */
public final class Tms282KeyMapService {
    public static final int DEFAULT_KEY_SETTING_TYPE = 0;
    /** Presets that always have explicit bindings (the three 265 presets). */
    public static final int EXPLICIT_PRESETS = 3;
    /** 265 GameConstants.QuickSlot_basic, copied so this class loads without the 265 constants. */
    static final Integer[] DEFAULT_QUICK_SLOTS = {42, 82, 71, 73, 29, 83, 79, 81, 2, 3, 4, 5, 16, 17, 18, 19, 6, 7, 8, 9,
            20, 30, 31, 32, 10, 11, 33, 34, 37, 38, 49, 50};

    private Tms282KeyMapService() {
    }

    /** Result of {@link #apply}: the preset changed in memory, and the bindings that must be written. */
    public record Applied(int characterId, FuncKeyMap preset, boolean seeded, List<Keymapping> changed) {
        public Applied {
            Objects.requireNonNull(preset, "preset");
            changed = List.copyOf(changed);
        }
    }

    /** What {@link #save} wrote: funckeymap row ID and number of keymaps rows inserted/updated. */
    public record Saved(int funcKeyMapId, boolean presetInserted, int rowsWritten) {
    }

    /**
     * Before LP0B16: fills presets 0..2 that are absent or empty with the 265 default, and the quick slots with
     * 265 QuickSlot_basic when none are saved. In memory only (written on the first change). Returns the presets
     * that were filled.
     */
    public static List<Integer> prepareForInit(Char chr) {
        Objects.requireNonNull(chr, "chr");
        Map<Integer, FuncKeyMap> maps = presets(chr);
        List<Integer> filled = new ArrayList<>();
        for (int preset = 0; preset < EXPLICIT_PRESETS; preset++) {
            if (seedIfEmpty(chr, maps, preset) != null) {
                filled.add(preset);
            }
        }
        if (chr.getQuickslotKeys() == null || chr.getQuickslotKeys().isEmpty()) {
            chr.setQuickslotKeys(new LinkedList<>(Arrays.asList(DEFAULT_QUICK_SLOTS)));
        }
        return List.copyOf(filled);
    }

    /**
     * CP020F type 0: applies the changes to the character's preset with 265 {@link FuncKeyMap#putKeyBinding}.
     * A preset that is absent or empty is first seeded with the 265 default. Nothing is written here.
     */
    public static Applied apply(Char chr, KeyChanges request) {
        Objects.requireNonNull(chr, "chr");
        Objects.requireNonNull(request, "request");
        Map<Integer, FuncKeyMap> maps = presets(chr);
        FuncKeyMap seededMap = seedIfEmpty(chr, maps, request.preset());
        FuncKeyMap map = maps.get(request.preset());
        List<Keymapping> changed = new ArrayList<>();
        for (KeyChange change : request.changes()) {
            map.putKeyBinding(chr.getId(), change.index(), (byte) change.type(), change.value());
            Keymapping binding = map.getMappingAt(change.index());
            if (!changed.contains(binding)) {
                changed.add(binding);
            }
        }
        if (seededMap != null) {
            // Every seeded binding is new (ID 0) and has to be inserted, not only the changed ones.
            for (Keymapping binding : map.getKeymap()) {
                if (!changed.contains(binding)) {
                    changed.add(binding);
                }
            }
        }
        return new Applied(chr.getId(), map, seededMap != null, changed);
    }

    /**
     * Writes {@link #apply}'s result with the 265 methods. {@link FuncKeyMap#saveToSQL()} only inserts the
     * funckeymap row when the preset is new (ID 0) and writes its bindings only on the update path, so a new preset
     * is inserted first; then each changed binding goes through {@link Keymapping#updateKeyMappingToSQL(int)}
     * (INSERT when its ID is 0, UPDATE otherwise). Requires an initialized database.
     */
    public static Saved save(Applied applied) {
        Objects.requireNonNull(applied, "applied");
        FuncKeyMap map = applied.preset();
        boolean inserted = false;
        if (map.getId() == 0) {
            map.saveToSQL();
            inserted = true;
            if (map.getId() <= 0) {
                throw new IllegalStateException("funckeymap insert returned no ID");
            }
        }
        for (Keymapping binding : applied.changed()) {
            binding.setCharId(applied.characterId());
            binding.updateKeyMappingToSQL(map.getId());
        }
        return new Saved(map.getId(), inserted, applied.changed().size());
    }

    private static Map<Integer, FuncKeyMap> presets(Char chr) {
        if (chr.getFuncKeyMaps() == null) {
            chr.setFuncKeyMaps(new LinkedHashMap<>());
        }
        return chr.getFuncKeyMaps();
    }

    /** Returns the default map whose bindings were copied in, or null when the preset already had bindings. */
    private static FuncKeyMap seedIfEmpty(Char chr, Map<Integer, FuncKeyMap> maps, int preset) {
        FuncKeyMap map = maps.get(preset);
        if (map != null && map.getKeymap() != null && !map.getKeymap().isEmpty()) {
            return null;
        }
        FuncKeyMap defaults = FuncKeyMap.getDefaultMapping(chr.getId(), preset, DEFAULT_KEY_SETTING_TYPE);
        if (map == null) {
            maps.put(preset, defaults);
        } else {
            // Keep the saved funckeymap row (its ID); only the bindings are new.
            map.setKeymap(defaults.getKeymap());
            map.setCharID(chr.getId());
        }
        return defaults;
    }
}
