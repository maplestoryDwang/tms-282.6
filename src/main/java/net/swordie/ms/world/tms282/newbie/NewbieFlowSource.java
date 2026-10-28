package net.swordie.ms.world.tms282.newbie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * {@code data/newbie/newbie-flow.json} (relative to the working directory), hot reloaded like news.json and
 * release.json: {@link #current} looks at the file's modification time and size at most every {@value #CHECK_MS} ms
 * and re-reads it when they changed. A file that does not parse, or whose data check fails (an item the 282 client
 * or the server does not have, a map the server cannot load), is refused as a whole: the previous valid version stays
 * in use and one log line names the first problem. Without a file the built-in {@link NewbieFlowDefaults} is used.
 */
public final class NewbieFlowSource {
    public static final Path DEFAULT_FILE = Path.of("data", "newbie", "newbie-flow.json");
    static final long CHECK_MS = 2_000;

    /** What the admin console shows. */
    public record Status(String state, String problem, NewbieFlowConfig config, String file, long loadedAt, List<String> warnings) {
        public Status {
            warnings = List.copyOf(warnings);
        }
    }

    public static final String OK = "ok", DEFAULT = "default", INVALID = "invalid";

    private final Path file;
    private final Function<NewbieFlowConfig, List<String>> dataCheck;
    private final LongSupplier clock;
    private long lastCheck = Long.MIN_VALUE;
    private String lastKey;
    private NewbieFlowConfig valid;
    private String state = DEFAULT;
    private String problem;
    private long loadedAt;
    private List<String> warnings = List.of();

    /**
     * @param dataCheck problems of a parsed config against the game data (empty = usable); applied to the built-in
     *                  defaults too, but a default with problems is still used (logged) since there is nothing else
     */
    public NewbieFlowSource(Path file, Function<NewbieFlowConfig, List<String>> dataCheck, LongSupplier clock) {
        this.file = Objects.requireNonNull(file, "file");
        this.dataCheck = Objects.requireNonNull(dataCheck, "dataCheck");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public Path file() {
        return file;
    }

    public synchronized NewbieFlowConfig current() {
        long now = clock.getAsLong();
        if (lastCheck == Long.MIN_VALUE || now - lastCheck >= CHECK_MS || now < lastCheck) {
            lastCheck = now;
            refresh(now);
        }
        return valid;
    }

    public synchronized Status status() {
        current();
        return new Status(state, problem, valid, file.toString(), loadedAt, warnings);
    }

    /** The next {@link #current} reads the file again (admin reload). */
    public synchronized Status reload() {
        lastCheck = Long.MIN_VALUE;
        lastKey = null;
        return status();
    }

    private static String key(Path f) {
        try {
            BasicFileAttributes a = Files.readAttributes(f, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!a.isRegularFile()) return "not-a-file";
            return a.lastModifiedTime().toMillis() + ":" + a.size();
        } catch (IOException e) {
            return "missing";
        }
    }

    private void refresh(long now) {
        String k = key(file);
        if (k.equals(lastKey)) return;
        lastKey = k;
        if (k.equals("missing")) {
            useDefaults(now, null);
            return;
        }
        if (k.equals("not-a-file")) {
            invalid("file: not a regular file");
            return;
        }
        NewbieFlowConfig parsed;
        try {
            byte[] bytes = Files.size(file) > NewbieFlowConfig.MAX_FILE_BYTES ? null : Files.readAllBytes(file);
            if (bytes == null) {
                invalid("file: larger than " + NewbieFlowConfig.MAX_FILE_BYTES + " bytes");
                return;
            }
            parsed = NewbieFlowConfig.parse(bytes);
        } catch (NoSuchFileException e) {
            useDefaults(now, null);
            return;
        } catch (NewbieFlowConfig.InvalidConfig e) {
            invalid(e.getMessage());
            return;
        } catch (IOException e) {
            invalid("file: cannot be read");
            return;
        }
        List<String> problems = checkData(parsed);
        if (!problems.isEmpty()) {
            invalid(problems.get(0) + (problems.size() > 1 ? " (+" + (problems.size() - 1) + " more)" : ""));
            return;
        }
        valid = parsed;
        state = OK;
        problem = null;
        loadedAt = now;
        warnings = List.of();
        System.out.println("[Newbie] " + file + " loaded: " + parsed.summary());
    }

    private void useDefaults(long now, String why) {
        NewbieFlowConfig d = NewbieFlowConfig.defaults();
        List<String> problems = checkData(d);
        if (!problems.isEmpty()) d = d.withEnabled(false);
        boolean wasDefault = DEFAULT.equals(state) && valid != null;
        valid = d;
        state = DEFAULT;
        problem = why;
        loadedAt = now;
        warnings = problems;
        if (!wasDefault) {
            System.out.println("[Newbie] " + file + " not found: using the built-in configuration (" + d.summary() + ")"
                    + (problems.isEmpty() ? "" : "; data problems: " + problems));
        }
    }

    private void invalid(String where) {
        if (valid == null) {
            useDefaults(clock.getAsLong(), where);
        }
        state = INVALID;
        problem = where;
        System.out.println("[Newbie] " + file + " is invalid (" + where + "): keeping the previous valid version ("
                + valid.summary() + ")");
    }

    private List<String> checkData(NewbieFlowConfig c) {
        try {
            return dataCheck.apply(c);
        } catch (RuntimeException e) {
            return List.of("game data validation failed: " + e.getClass().getSimpleName());
        }
    }
}
