package net.swordie.ms.loaders;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.life.drop.DropInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DropData {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final int GLOBAL_MOB_ID = -1;
    private static final boolean ENABLE_GLOBAL_DROPS = true;

    // mobId -> immutable Set<DropInfo>
    private static volatile Int2ObjectMap<Set<DropInfo>> DROP_BY_MOB = new Int2ObjectOpenHashMap<>();
    private static volatile Set<DropInfo> GLOBAL_DROPS = Collections.emptySet();
    private static volatile boolean LOADED = false;

    private DropData() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class DropRow {
        public long id;
        public int mobId;
        public String mobName;
        public int itemId;
        public String itemName;
        public int chance;
        public int minQuant;
        public int maxQuant;
        public boolean reactorDrop;
    }

    public static void load() throws IOException {
        // 265 profile: only resources/mob_drops.json. 282 profile: + resources/tms282/mob_drops.json, whose mobs
        // replace the base rows of the same mob (see loaders.overlay.DataOverlay#resourceLayers).
        List<DropRow> rows = new ArrayList<>();
        for (java.io.File layer : net.swordie.ms.loaders.overlay.DataOverlays.current().resourceLayers("mob_drops.json")) {
            List<DropRow> layerRows = MAPPER.readValue(Files.readAllBytes(layer.toPath()), new TypeReference<List<DropRow>>() {});
            Set<Integer> replaced = new HashSet<>();
            for (DropRow r : layerRows) {
                replaced.add(r.mobId);
            }
            if (!rows.isEmpty()) {
                rows.removeIf(r -> replaced.contains(r.mobId));
            }
            rows.addAll(layerRows);
        }
        Int2ObjectOpenHashMap<Set<DropInfo>> map = new Int2ObjectOpenHashMap<>(Math.max(16, rows.size() / 3));
        ObjectOpenHashSet<DropInfo> global = new ObjectOpenHashSet<>();
        for (DropRow r : rows) {
            DropInfo di = new DropInfo();
            di.setId(r.id);
            di.setItemID(r.itemId);
            di.setChance(r.chance);
            if (r.itemId == 0) {
                di.setMinMoney(r.minQuant);
                di.setMaxMoney(r.maxQuant);
            } else {
                di.setMinQuant(r.minQuant);
                di.setMaxQuant(r.maxQuant);
            }
            di.generateNextDrop(); // The first kill must use the configured range too.
            di.setReactorDrop(r.reactorDrop);
            if (r.mobId == GLOBAL_MOB_ID) {
                global.add(di);
                continue;
            }
            Set<DropInfo> set = map.get(r.mobId);
            if (set == null) {
                set = new ObjectOpenHashSet<>();
                map.put(r.mobId, set);
            }
            set.add(di);
        }
        for (Int2ObjectMap.Entry<Set<DropInfo>> e : map.int2ObjectEntrySet()) {
            e.setValue(Collections.unmodifiableSet(e.getValue()));
        }
        DROP_BY_MOB = map;
        GLOBAL_DROPS = global.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(global);
        LOADED = true;
    }

    public static Set<DropInfo> getDropInfoByIdFromJson(int mobID) {
        if (!LOADED) return Collections.emptySet();
        Set<DropInfo> s = DROP_BY_MOB.get(mobID);
        if (!ENABLE_GLOBAL_DROPS || GLOBAL_DROPS.isEmpty()) {
            return (s != null) ? s : Collections.emptySet();
        }
        if (s == null || s.isEmpty()) return GLOBAL_DROPS;
        ObjectOpenHashSet<DropInfo> merged = new ObjectOpenHashSet<>(s.size() + GLOBAL_DROPS.size());
        merged.addAll(s);
        merged.addAll(GLOBAL_DROPS);
        return merged;
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static Set<DropInfo> getDropInfoByID(int mobTemplateID) {
        return DropData.getDropInfoByIdFromJson(mobTemplateID);
    }
}
