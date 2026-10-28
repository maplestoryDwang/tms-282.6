package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.connection.protocol.codec.FieldAvailability;
import net.swordie.ms.loaders.overlay.DataOverlays;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether fields exist for the active profile: server field data (DataOverlays: dat282 overlay on 282, dat265 on
 * 265) and, on 282, the client's field list (FieldAvailability). Cached per field id.
 */
public final class BossRunMaps {
    /** Malitia lobby + normal / extreme first fields (TMS273 boss_malitia_normal / _extreme). */
    public static final int[] MALITIA_FIELDS = {875033004, 875030000, 875031000};

    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private BossRunMaps() {
    }

    public static boolean exists(int fieldId) {
        ProtocolVersion version = activeVersion();
        return CACHE.computeIfAbsent(version + ":" + fieldId, k -> lookup(version, fieldId));
    }

    public static boolean allExist(int... fieldIds) {
        for (int id : fieldIds) {
            if (!exists(id)) return false;
        }
        return true;
    }

    private static ProtocolVersion activeVersion() {
        try {
            return Protocol.active().version();
        } catch (RuntimeException | LinkageError e) {
            return ProtocolVersion.GMS_265;
        }
    }

    private static boolean lookup(ProtocolVersion version, int fieldId) {
        try {
            if (!FieldAvailability.active().isAvailable(fieldId)) return false;
            // Server field data of that profile: dat282 overlay over dat265 on 282, dat265 alone on 265.
            return DataOverlays.forVersion(version).datFile("fields/" + fieldId + ".dat").exists();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** Malitia may be offered only when its lobby and both first fields exist for the active profile. */
    public static boolean malitiaAvailable() {
        return allExist(MALITIA_FIELDS);
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
