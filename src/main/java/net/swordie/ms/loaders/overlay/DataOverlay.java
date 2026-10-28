package net.swordie.ms.loaders.overlay;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Where a loader finds its game data files. The 265 base data ({@code data/dat265}, {@code data/wz265}) is never
 * modified; a version overlay (for TMS 282.3: {@code data/dat282}, {@code data/wz282}) is consulted first for the
 * ids it contains and the base answers everything else.
 * <p>
 * Implementations: {@link BaseDataOverlay} (null object, exactly the historical paths) and
 * {@link DirectoryDataOverlay}. {@link DataOverlays} selects one from {@code protocol.version}.
 */
public interface DataOverlay {

    /** Short id for logs/checks, e.g. {@code gms265-base} or {@code tms282.3}. */
    String id();

    /** False for the base-only data source: loaders then behave exactly as before the overlay existed. */
    boolean isActive();

    /** The overlay's own DAT file for {@code relative} (e.g. {@code mobs/100100.dat}), if the overlay has one. */
    Optional<File> overlayDat(String relative);

    /** The base DAT file ({@code DAT_DIR/relative}), whether or not it exists. */
    File baseDat(String relative);

    /** DAT file a loader should read: the overlay copy when present, else the base file. */
    default File datFile(String relative) {
        return overlayDat(relative).orElseGet(() -> baseDat(relative));
    }

    /**
     * WZ XML layers for a merge-style loader (e.g. {@code String.wz/Mob.img.xml}): the base file first, then the
     * overlay file when present. Loaders read them in order, so overlay entries replace base entries with the same id.
     */
    List<File> wzLayers(String relative);

    /**
     * {@code data/resources} files for a merge-style loader (e.g. {@code mob_drops.json}): the base file first, then
     * the overlay's {@code data/resources/tms282/<relative>} when present.
     */
    List<File> resourceLayers(String relative);
}
