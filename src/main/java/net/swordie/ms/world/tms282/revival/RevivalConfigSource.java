package net.swordie.ms.world.tms282.revival;

import net.swordie.ms.ServerConstants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.function.LongSupplier;

/**
 * Hot-reloaded {@link RevivalConfig}: the file is looked at no more than every 2 s; a missing file means the built-in
 * defaults, an invalid edit keeps the last valid configuration (as the newbie flow source does).
 */
public final class RevivalConfigSource {
    public static final Path DEFAULT_FILE = Path.of(ServerConstants.DIR, "data", "tms282-revival", "revival-events.json");
    private static final long CHECK_MS = 2000;
    private static volatile RevivalConfigSource shared;

    public record Status(String state, String detail) {
    }

    private final Path file;
    private final LongSupplier clock;
    private RevivalConfig current = RevivalConfig.defaults();
    private FileTime loadedStamp;
    private boolean fromFile;
    private long checkedAt = Long.MIN_VALUE;
    private Status status = new Status("default", "built-in defaults");

    public RevivalConfigSource(Path file, LongSupplier clock) {
        this.file = file;
        this.clock = clock;
    }

    public static RevivalConfigSource shared() {
        RevivalConfigSource s = shared;
        if (s == null) {
            synchronized (RevivalConfigSource.class) {
                if (shared == null) shared = new RevivalConfigSource(DEFAULT_FILE, System::currentTimeMillis);
                s = shared;
            }
        }
        return s;
    }

    /** Checks replace the shared source; {@code null} restores the file source. */
    public static void install(RevivalConfigSource source) {
        shared = source;
        net.swordie.ms.world.tms282.revival.hub.HubCatalog.invalidate();
    }

    public synchronized RevivalConfig current() {
        long now = clock.getAsLong();
        if (checkedAt == Long.MIN_VALUE || now - checkedAt >= CHECK_MS) {
            checkedAt = now;
            refresh();
        }
        return current;
    }

    /** Forces a look at the file now (GM {@code !revival reload}). */
    public synchronized Status reload() {
        checkedAt = clock.getAsLong();
        loadedStamp = null;
        refresh();
        return status;
    }

    public synchronized Status status() {
        return status;
    }

    private void refresh() {
        try {
            if (!Files.isRegularFile(file)) {
                if (fromFile) {
                    current = RevivalConfig.defaults();
                    fromFile = false;
                    net.swordie.ms.world.tms282.revival.hub.HubCatalog.invalidate();
                }
                loadedStamp = null;
                status = new Status("default", "no " + file.getFileName() + ": built-in defaults");
                return;
            }
            FileTime stamp = Files.getLastModifiedTime(file);
            if (stamp.equals(loadedStamp)) return;
            loadedStamp = stamp;
            current = RevivalConfig.parse(Files.readAllBytes(file));
            fromFile = true;
            net.swordie.ms.world.tms282.revival.hub.HubCatalog.invalidate();
            status = new Status("file", "version " + current.version());
        } catch (RevivalConfig.InvalidConfig e) {
            status = new Status("invalid", e.getMessage() + " (kept version " + current.version() + ")");
            System.out.println("[Revival] " + file + " ignored: " + e.getMessage());
        } catch (Exception e) {
            status = new Status("invalid", e.getClass().getSimpleName() + " (kept version " + current.version() + ")");
        }
    }
}
