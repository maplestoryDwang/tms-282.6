package net.swordie.ms.loaders.overlay;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.connection.protocol.ProtocolVersion;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Selects the process-wide {@link DataOverlay} from {@code protocol.version}: only {@code tms282.3} gets the
 * {@code data/dat282} + {@code data/wz282} overlay; {@code gms265} (and any failure to read the config) keeps the
 * base-only behavior. Checks/tools may {@link #install} a source explicitly; every install bumps
 * {@link #generation()} so {@link OverlayRecordCache}s drop entries of the previous source.
 */
public final class DataOverlays {
    public static final String TMS282_DAT_DIR = "data/dat282";
    public static final String TMS282_WZ_DIR = "data/wz282";
    /** Generated (non-official) 282 resources, e.g. mob_drops.json; see its manifest.json. */
    public static final String TMS282_RESOURCE_DIR = "data/resources/tms282";

    private static final AtomicInteger GENERATION = new AtomicInteger();
    private static volatile DataOverlay current;

    private DataOverlays() {
    }

    public static DataOverlay current() {
        DataOverlay c = current;
        if (c == null) {
            synchronized (DataOverlays.class) {
                c = current;
                if (c == null) {
                    c = fromConfig();
                    current = c;
                    GENERATION.incrementAndGet();
                }
            }
        }
        return c;
    }

    public static DataOverlay base() {
        return BaseDataOverlay.INSTANCE;
    }

    /** The TMS 282.3 overlay rooted at the server directory ({@link ServerConstants#DIR}). */
    public static DirectoryDataOverlay tms282() {
        return new DirectoryDataOverlay("tms282.3", new File(ServerConstants.DIR, TMS282_DAT_DIR),
                new File(ServerConstants.DIR, TMS282_WZ_DIR), new File(ServerConstants.DIR, TMS282_RESOURCE_DIR), base());
    }

    public static DataOverlay forVersion(ProtocolVersion version) {
        return version == ProtocolVersion.TMS_282_3 ? tms282() : base();
    }

    /** Replaces the selected source (checks and offline tools). Callers clear loader caches themselves. */
    public static synchronized void install(DataOverlay overlay) {
        current = overlay;
        GENERATION.incrementAndGet();
    }

    public static int generation() {
        return GENERATION.get();
    }

    private static DataOverlay fromConfig() {
        try {
            return forVersion(ProtocolConfig.load().version());
        } catch (RuntimeException e) {
            System.err.println("[WZ Data] protocol.version unreadable, using 265 base data only: " + e.getMessage());
            return base();
        }
    }
}
