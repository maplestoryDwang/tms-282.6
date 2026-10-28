package net.swordie.ms.connection.api.launcher.release;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SHA-256 of whole files, cached by path + size + modification time, so a 100 MB launcher exe or the 4 MB client
 * exe is hashed once per change instead of once per request. Streams the file (1 MB buffer); never holds it in memory.
 * Links are not followed. Thread-safe; at most one hash per file runs at a time.
 */
public final class FileHashCache {
    private static final FileHashCache STANDARD = new FileHashCache();
    private static final int MAX_ENTRIES = 64;

    private record Entry(String key, String sha256) {
    }

    private final Map<Path, Entry> cache = new ConcurrentHashMap<>();
    private final Map<Path, Object> locks = new ConcurrentHashMap<>();

    public static FileHashCache standard() {
        return STANDARD;
    }

    /** The key of a regular file (size:mtime), or null when it is missing, a link or not a regular file. */
    public static String key(Path file) {
        try {
            BasicFileAttributes a = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!a.isRegularFile() || a.isSymbolicLink() || a.isOther()) {
                return null;
            }
            return a.size() + ":" + a.lastModifiedTime().toMillis();
        } catch (IOException e) {
            return null;
        }
    }

    /** Upper-case hex SHA-256, or throws when the file cannot be read (or changed while it was read). */
    public String sha256(Path file) throws IOException {
        Path abs = file.toAbsolutePath().normalize();
        synchronized (locks.computeIfAbsent(abs, k -> new Object())) {
            String key = key(abs);
            if (key == null) {
                cache.remove(abs);
                throw new IOException("not a regular file");
            }
            Entry e = cache.get(abs);
            if (e != null && e.key().equals(key)) {
                return e.sha256();
            }
            String sha = hash(abs);
            if (!key.equals(key(abs))) {
                throw new IOException("changed while hashing");
            }
            if (cache.size() >= MAX_ENTRIES) {
                cache.clear();
            }
            cache.put(abs, new Entry(key, sha));
            return sha;
        }
    }

    static String hash(Path file) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        byte[] buf = new byte[1 << 20];
        try (InputStream in = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            int n;
            while ((n = in.read(buf)) > 0) {
                md.update(buf, 0, n);
            }
        }
        return HexFormat.of().withUpperCase().formatHex(md.digest());
    }
}
