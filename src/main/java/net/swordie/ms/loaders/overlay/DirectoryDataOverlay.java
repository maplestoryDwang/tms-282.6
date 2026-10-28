package net.swordie.ms.loaders.overlay;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A version overlay stored as a DAT directory (same serialized format as dat265, produced by the loaders' own
 * generators) plus a WZ XML directory (same XML dialect as wz265). Only files present in the overlay take effect.
 */
public final class DirectoryDataOverlay implements DataOverlay {
    private final String id;
    private final File datRoot;
    private final File wzRoot;
    private final DataOverlay base;
    private final File resourceRoot;

    public DirectoryDataOverlay(String id, File datRoot, File wzRoot, DataOverlay base) {
        this(id, datRoot, wzRoot, null, base);
    }

    /** @param resourceRoot overlay {@code data/resources} directory (may be null: no resource overlay) */
    public DirectoryDataOverlay(String id, File datRoot, File wzRoot, File resourceRoot, DataOverlay base) {
        this.id = Objects.requireNonNull(id);
        this.datRoot = Objects.requireNonNull(datRoot);
        this.wzRoot = Objects.requireNonNull(wzRoot);
        this.resourceRoot = resourceRoot;
        this.base = Objects.requireNonNull(base);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    public File datRoot() {
        return datRoot;
    }

    public File wzRoot() {
        return wzRoot;
    }

    @Override
    public Optional<File> overlayDat(String relative) {
        File f = new File(datRoot, relative);
        return f.isFile() ? Optional.of(f) : Optional.empty();
    }

    @Override
    public File baseDat(String relative) {
        return base.baseDat(relative);
    }

    @Override
    public List<File> wzLayers(String relative) {
        File overlay = new File(wzRoot, relative);
        List<File> baseLayers = base.wzLayers(relative);
        if (!overlay.isFile()) {
            return baseLayers;
        }
        return java.util.stream.Stream.concat(baseLayers.stream(), java.util.stream.Stream.of(overlay)).toList();
    }

    @Override
    public List<File> resourceLayers(String relative) {
        List<File> baseLayers = base.resourceLayers(relative);
        if (resourceRoot == null || !new File(resourceRoot, relative).isFile()) {
            return baseLayers;
        }
        return java.util.stream.Stream.concat(baseLayers.stream(), java.util.stream.Stream.of(new File(resourceRoot, relative))).toList();
    }

    public File resourceRoot() {
        return resourceRoot;
    }
}
