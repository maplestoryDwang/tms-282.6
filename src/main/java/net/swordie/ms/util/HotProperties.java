package net.swordie.ms.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.function.LongSupplier;

/**
 * A properties file read again whenever its modification time or size changed, checked at most once per
 * {@link #CHECK_MS} (the hot reload of {@code config/server.properties} keys that are read on hot paths). A JVM
 * {@code -Dkey=value} always wins over the file. A missing file reads as empty, so every key falls back to its caller's
 * default. A file that exists but cannot be read right now (locked by an editor, an anti-virus scan or a backup tool;
 * any I/O error) keeps the values last read successfully and is tried again at the next check, so a transient failure
 * never silently drops a setting such as {@code net.allow} back to its default. A file whose content is malformed
 * also keeps the previous values, until the file changes again.
 * <p>
 * Thread-safe; a read is a volatile load plus, at most every {@link #CHECK_MS}, one {@code stat} of the file.
 */
public final class HotProperties {
    public static final Path SERVER_PROPERTIES = Path.of("config", "server.properties");
    public static final long CHECK_MS = 3_000;
    private static final HotProperties SERVER = new HotProperties(SERVER_PROPERTIES, System::currentTimeMillis);

    private final Path file;
    private final LongSupplier clock;
    private final Object lock = new Object();
    private volatile Properties values = new Properties();
    private volatile long lastCheck = Long.MIN_VALUE;
    private volatile long generation;
    private String lastKey = "";
    private String failedKey = "";

    public HotProperties(Path file, LongSupplier clock) {
        this.file = Objects.requireNonNull(file, "file");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** {@code config/server.properties} relative to the working directory (the server's own config). */
    public static HotProperties server() {
        return SERVER;
    }

    public Path file() {
        return file;
    }

    /** Increments every time the file content was (re)loaded; lets callers cache parsed values. */
    public long generation() {
        refresh();
        return generation;
    }

    /** The raw value: {@code -Dkey} first, then the file; null when neither has it. */
    public String raw(String key) {
        String forced = System.getProperty(key);
        if (forced != null) {
            return forced;
        }
        refresh();
        return values.getProperty(key);
    }

    public String string(String key, String def) {
        String v = raw(key);
        return v == null || v.isBlank() ? def : v.trim();
    }

    public boolean bool(String key, boolean def) {
        String v = raw(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        String s = v.trim().toLowerCase(Locale.ROOT);
        if (s.equals("true") || s.equals("on") || s.equals("yes") || s.equals("1")) {
            return true;
        }
        if (s.equals("false") || s.equals("off") || s.equals("no") || s.equals("0")) {
            return false;
        }
        return def;
    }

    /** An int in {@code [min, max]}; anything else (absent, not a number, out of range) is {@code def}. */
    public int integer(String key, int def, int min, int max) {
        String v = raw(key);
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            int n = Integer.parseInt(v.trim());
            return n >= min && n <= max ? n : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Forces the next read to look at the file again (tests; the stop-file watcher after a write). */
    public void invalidate() {
        lastCheck = Long.MIN_VALUE;
    }

    private void refresh() {
        long now = clock.getAsLong();
        long last = lastCheck;
        if (last != Long.MIN_VALUE && now - last < CHECK_MS && now >= last) {
            return;
        }
        synchronized (lock) {
            if (lastCheck != last) {
                return;
            }
            lastCheck = now;
            String key;
            try {
                BasicFileAttributes a = Files.readAttributes(file, BasicFileAttributes.class);
                key = a.lastModifiedTime().toMillis() + ":" + a.size();
            } catch (NoSuchFileException e) {
                key = "missing";
            } catch (IOException e) {
                transientFailure("stat:" + e.getClass().getName(), e);
                return;
            }
            if (key.equals(lastKey)) {
                return;
            }
            Properties next = new Properties();
            if (!key.equals("missing")) {
                try (InputStream in = Files.newInputStream(file)) {
                    next.load(in);
                } catch (NoSuchFileException e) {
                    // deleted between the stat and the open: the next check sees "missing"
                    return;
                } catch (IOException e) {
                    // lastKey stays as it was: the next check (CHECK_MS later) reads the file again
                    transientFailure(key, e);
                    return;
                } catch (IllegalArgumentException e) {
                    // malformed content (a bad unicode escape): keep the previous values until the file changes again
                    System.out.println("[HotProperties] cannot parse " + file + ": " + e.getMessage()
                            + " (keeping the previous values until the file changes)");
                    lastKey = key;
                    failedKey = "";
                    return;
                }
            }
            if (!failedKey.isEmpty()) {
                System.out.println("[HotProperties] " + file + " readable again, reloaded");
            }
            failedKey = "";
            values = next;
            lastKey = key;
            generation++;
        }
    }

    /** One log line per failing file state (not one every CHECK_MS while an editor holds the file). */
    private void transientFailure(String state, IOException e) {
        if (!state.equals(failedKey)) {
            failedKey = state;
            System.out.println("[HotProperties] cannot read " + file + " right now: " + e
                    + " (keeping the previous values, retrying every " + CHECK_MS / 1000 + " s)");
        }
    }
}
