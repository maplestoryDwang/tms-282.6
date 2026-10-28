package net.swordie.ms.loaders.overlay;

import net.swordie.ms.ServerConstants;

import java.io.File;
import java.util.List;
import java.util.Optional;

/** Null object: the historical 265 paths only ({@code ServerConstants.DAT_DIR} / {@code WZ_DIR}). */
public final class BaseDataOverlay implements DataOverlay {
    public static final BaseDataOverlay INSTANCE = new BaseDataOverlay();

    private BaseDataOverlay() {
    }

    @Override
    public String id() {
        return "gms265-base";
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public Optional<File> overlayDat(String relative) {
        return Optional.empty();
    }

    @Override
    public File baseDat(String relative) {
        return new File(ServerConstants.DAT_DIR + "/" + relative);
    }

    @Override
    public List<File> wzLayers(String relative) {
        return List.of(new File(ServerConstants.WZ_DIR + "/" + relative));
    }

    @Override
    public List<File> resourceLayers(String relative) {
        return List.of(new File(ServerConstants.RESOURCES_DIR + "/" + relative));
    }
}
