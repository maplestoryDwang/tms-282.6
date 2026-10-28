package net.swordie.ms.world.tms282.revival.rewards;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * A configuration file re-read at most every 2 s: missing file = built-in default, a file that does not parse or
 * validate keeps the last valid value and is logged. {@code onChange} runs after the current value was replaced.
 */
public final class HotFile<T> {
    public static final long CHECK_MS = 2000;

    public record Status(String state, String detail) {
    }

    /** Thrown by the parser for a file that must not replace the current value. */
    public static final class Invalid extends RuntimeException {
        public Invalid(String message) {
            super(message);
        }
    }

    private final String name;
    private final Path file;
    private final LongSupplier clock;
    private final Function<byte[], T> parser;
    private final Supplier<T> defaults;
    private final Runnable onChange;
    private T current;
    private String version;
    private FileTime loadedStamp;
    private boolean fromFile;
    private long checkedAt = Long.MIN_VALUE;
    private Status status = new Status("default", "built-in defaults");

    public HotFile(String name, Path file, LongSupplier clock, Function<byte[], T> parser, Supplier<T> defaults,
                   Function<T, String> versionOf, Runnable onChange) {
        this.name = name;
        this.file = file;
        this.clock = clock;
        this.parser = parser;
        this.defaults = defaults;
        this.onChange = onChange == null ? () -> { } : onChange;
        this.versionOf = versionOf;
        this.current = defaults.get();
        this.version = versionOf.apply(current);
    }

    private final Function<T, String> versionOf;

    public synchronized T current() {
        long now = clock.getAsLong();
        if (checkedAt == Long.MIN_VALUE || now - checkedAt >= CHECK_MS) {
            checkedAt = now;
            refresh();
        }
        return current;
    }

    public synchronized Status reload() {
        checkedAt = clock.getAsLong();
        loadedStamp = null;
        refresh();
        return status;
    }

    public synchronized Status status() {
        return status;
    }

    private void replace(T value) {
        current = value;
        version = versionOf.apply(value);
        onChange.run();
    }

    private void refresh() {
        try {
            if (!Files.isRegularFile(file)) {
                if (fromFile) {
                    replace(defaults.get());
                    fromFile = false;
                }
                loadedStamp = null;
                status = new Status("default", "no " + file.getFileName() + ": built-in defaults");
                return;
            }
            FileTime stamp = Files.getLastModifiedTime(file);
            if (stamp.equals(loadedStamp)) return;
            loadedStamp = stamp;
            T parsed = parser.apply(Files.readAllBytes(file));
            replace(parsed);
            fromFile = true;
            status = new Status("file", "version " + version);
        } catch (Invalid e) {
            status = new Status("invalid", e.getMessage() + " (kept version " + version + ")");
            System.out.println("[Revival] " + name + " " + file + " ignored: " + e.getMessage() + " (kept version " + version + ")");
        } catch (Exception e) {
            status = new Status("invalid", e.getClass().getSimpleName() + " (kept version " + version + ")");
            System.out.println("[Revival] " + name + " " + file + " ignored: " + e.getClass().getSimpleName() + " (kept version " + version + ")");
        }
    }
}
