package net.swordie.ms.handlers.login.tms282;

import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import net.swordie.ms.connection.packet.Tms282QuickSelection;

/** Per-user favorite survives restart without adding columns to the 265 database. */
public final class Tms282CharacterPreferences {
    private final Path directory;
    public Tms282CharacterPreferences(Path directory) { this.directory = directory.toAbsolutePath().normalize(); }
    private Path file(int userId) {
        if (userId <= 0) throw new IllegalArgumentException("Invalid user id");
        return directory.resolve(userId + ".txt");
    }
    public synchronized Optional<Tms282QuickSelection> read(int userId) throws IOException {
        Path path = file(userId);
        if (!Files.exists(path)) return Optional.empty();
        String text = Files.readString(path, StandardCharsets.US_ASCII).trim();
        if (text.isEmpty()) return Optional.empty();
        try {
            String[] parts = text.split("\\s+");
            if (parts.length != 3) throw new IllegalArgumentException();
            return Optional.of(new Tms282QuickSelection(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
        } catch (IllegalArgumentException e) { throw new IOException("Invalid character preference for user " + userId, e); }
    }
    public synchronized void write(int userId, Tms282QuickSelection favorite) throws IOException {
        Path path = file(userId);
        Files.createDirectories(directory);
        Path temp = Files.createTempFile(directory, userId + "-", ".tmp");
        try {
            Files.writeString(temp, favorite == null ? "" : favorite.worldId() + " " + favorite.channelId() + " " + favorite.characterId() + "\n", StandardCharsets.US_ASCII);
            try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
