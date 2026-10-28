package net.swordie.ms.world.tms282.collab.frieren;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link FrierenState} per character id, cached in memory and written to {@code <dir>/<characterId>.properties}
 * (temp file + move), so the gauge survives relogin and restart without a database table.
 */
public final class FrierenStateStore {
    private final Path directory;
    private final Map<Integer, FrierenState> cache = new ConcurrentHashMap<>();

    public FrierenStateStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    private Path file(int characterId) {
        if (characterId <= 0) {
            throw new IllegalArgumentException("Invalid character id " + characterId);
        }
        return directory.resolve(characterId + ".properties");
    }

    public FrierenState get(int characterId) {
        return cache.computeIfAbsent(characterId, this::read);
    }

    private FrierenState read(int characterId) {
        Path path = file(characterId);
        if (!Files.isRegularFile(path)) {
            return new FrierenState();
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
            return FrierenState.fromProperties(p);
        } catch (IOException | RuntimeException e) {
            System.err.println("[Frieren] unreadable state " + path + ": " + e + " (starting fresh)");
            return new FrierenState();
        }
    }

    public synchronized void save(int characterId) {
        FrierenState state = cache.get(characterId);
        if (state == null) {
            return;
        }
        Path path = file(characterId);
        try {
            Files.createDirectories(directory);
            Path temp = Files.createTempFile(directory, characterId + "-", ".tmp");
            try {
                try (OutputStream out = Files.newOutputStream(temp)) {
                    state.toProperties().store(out, "TMS282 Frieren collab state of character " + characterId);
                }
                try {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Drops the cached copy (the file stays); the next {@link #get} reads it again. */
    public void evict(int characterId) {
        cache.remove(characterId);
    }

    public Path directory() {
        return directory;
    }
}
