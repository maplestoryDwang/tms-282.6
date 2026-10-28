package net.swordie.ms.loaders.overlay;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Overlay-first lookup for loaders whose base cache is filled in bulk at startup (ItemData equips/items): the base
 * map is left untouched, overlay records live here. Returns null whenever the overlay is inactive or has no file,
 * so the caller falls through to its unchanged base path.
 *
 * @param <T> record type (Equip, ItemInfo, ...)
 */
public final class OverlayRecordCache<T> {
    private final String datDir;
    private final Map<Integer, Optional<T>> cache = new ConcurrentHashMap<>();
    private volatile int generation = -1;

    /** @param datDir DAT sub directory, e.g. {@code equips} */
    public OverlayRecordCache(String datDir) {
        this.datDir = datDir;
    }

    public T get(int id, Function<File, T> reader) {
        DataOverlay overlay = DataOverlays.current();
        if (!overlay.isActive()) {
            return null;
        }
        int gen = DataOverlays.generation();
        if (gen != generation) {
            cache.clear();
            generation = gen;
        }
        return cache.computeIfAbsent(id, k -> overlay.overlayDat(datDir + "/" + k + ".dat").map(reader)).orElse(null);
    }

    public void clear() {
        cache.clear();
    }
}
