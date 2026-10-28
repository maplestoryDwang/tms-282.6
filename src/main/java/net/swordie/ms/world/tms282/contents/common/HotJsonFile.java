package net.swordie.ms.world.tms282.contents.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A JSON data file that is re-read when it changes (checked at most every {@code checkMillis}); a file that does not
 * parse keeps the last good value (logged once per change), a missing file gives the fallback. Used by the
 * content-1005 event definitions so GMs can edit rewards / periods without a restart.
 */
public final class HotJsonFile<T> {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Path path;
    private final Function<JsonNode, T> parser;
    private final Supplier<T> fallback;
    private final long checkMillis;
    private T value;
    private long stamp = Long.MIN_VALUE;
    private long checked;

    public HotJsonFile(Path path, Function<JsonNode, T> parser, Supplier<T> fallback, long checkMillis) {
        this.path = Objects.requireNonNull(path, "path");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.checkMillis = checkMillis;
    }

    public Path path() {
        return path;
    }

    public synchronized T get() {
        long now = System.currentTimeMillis();
        if (value != null && now - checked < checkMillis) {
            return value;
        }
        checked = now;
        long modified;
        try {
            modified = Files.isRegularFile(path) ? Files.getLastModifiedTime(path).toMillis() : -1;
        } catch (Exception e) {
            modified = -1;
        }
        if (value != null && modified == stamp) {
            return value;
        }
        stamp = modified;
        if (modified < 0) {
            value = fallback.get();
            return value;
        }
        try {
            value = parser.apply(JSON.readTree(Files.readString(path)));
            System.out.println("[HotJsonFile] loaded " + path);
        } catch (Exception e) {
            System.out.println("[HotJsonFile] " + path + " not loaded (" + e.getMessage() + "); keeping "
                    + (value == null ? "the default" : "the previous version"));
            if (value == null) {
                value = fallback.get();
            }
        }
        return value;
    }

    /** Parses {@code text} with this file's parser (tests, GM validation). */
    public T parse(String text) throws Exception {
        return parser.apply(JSON.readTree(text));
    }
}
