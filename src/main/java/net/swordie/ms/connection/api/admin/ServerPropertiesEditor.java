package net.swordie.ms.connection.api.admin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Changes a few {@code key=value} lines of {@code config/server.properties} in place for the admin console. Every
 * other byte - comments (UTF-8 text read as ISO-8859-1, so it round-trips), other keys, blank lines, line endings -
 * stays as it is:
 * <ul>
 *     <li>refused unless the file still has the SHA-256 the console read ({@code conflict});</li>
 *     <li>every non-comment line of a key is replaced (the last one wins in Java properties, so replacing only the
 *     first would change nothing); a key that is not there is appended; a key line continued with a trailing
 *     backslash is refused (edit by hand);</li>
 *     <li>after building the new text, both versions are parsed as Java properties: the changed keys must read back
 *     exactly as given and every other key must be unchanged, otherwise nothing is written;</li>
 *     <li>the old file is copied to {@code server.properties.bak-<stamp>} first, the new one goes through a temporary
 *     file (a unique name in the same folder) and an atomic move.</li>
 *     <li>reading, the SHA-256 check and the write happen under one lock per file shared by every editor of that
 *     path ({@link #lockFor}), so two requests with the same {@code baseSha256} cannot both succeed: the second one
 *     sees the new hash and answers {@code conflict}.</li>
 * </ul>
 * Values are restricted by the callers to plain characters ({@code [0-9A-Za-z.:/, ]}), so no escaping is needed.
 */
final class ServerPropertiesEditor {
    /** One lock per file (absolute, normalized path), shared by every editor instance of this process. */
    private static final ConcurrentHashMap<Path, Object> LOCKS = new ConcurrentHashMap<>();

    private final Path file;

    ServerPropertiesEditor(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    Path file() {
        return file;
    }

    record Result(String code, String problem, String sha256, Path backup) {
    }

    static Object lockFor(Path file) {
        return LOCKS.computeIfAbsent(file.toAbsolutePath().normalize(), k -> new Object());
    }

    byte[] read() throws IOException {
        synchronized (lockFor(file)) {
            return Files.readAllBytes(file);
        }
    }

    /** The value a Java properties reader sees for {@code key} in {@code bytes}, or null. */
    static String value(byte[] bytes, String key) {
        return load(bytes).getProperty(key);
    }

    Result update(Map<String, String> updates, String baseSha256, String stamp) throws IOException {
        synchronized (lockFor(file)) {
            return updateLocked(updates, baseSha256, stamp);
        }
    }

    private Result updateLocked(Map<String, String> updates, String baseSha256, String stamp) throws IOException {
        byte[] before;
        try {
            before = Files.readAllBytes(file);
        } catch (NoSuchFileException e) {
            return new Result(AdminCodes.REJECTED, "missingFile", null, null);
        }
        String sha = sha256(before);
        if (!sha.equalsIgnoreCase(baseSha256 == null ? "" : baseSha256)) {
            return new Result(AdminCodes.CONFLICT, null, sha, null);
        }
        for (Map.Entry<String, String> u : updates.entrySet()) {
            if (!u.getValue().matches("[0-9A-Za-z.:/, ]*") || !u.getKey().matches("[a-z][A-Za-z0-9.]*")) {
                return new Result(AdminCodes.REJECTED, "characters:" + u.getKey(), sha, null);
            }
        }
        String text = new String(before, StandardCharsets.ISO_8859_1);
        String eol = text.contains("\r\n") ? "\r\n" : "\n";
        // every line keeps its own ending (a file edited by several tools may mix CRLF and LF)
        List<String> lines = new ArrayList<>();
        List<String> endings = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int nl = text.indexOf('\n', start);
            if (nl < 0) {
                lines.add(text.substring(start));
                endings.add("");
                break;
            }
            boolean crlf = nl > start && text.charAt(nl - 1) == '\r';
            lines.add(text.substring(start, crlf ? nl - 1 : nl));
            endings.add(crlf ? "\r\n" : "\n");
            start = nl + 1;
        }
        Map<String, Boolean> seen = new LinkedHashMap<>();
        boolean continued = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            boolean isContinuation = continued;
            String key = isContinuation ? null : keyOf(line);
            continued = (isContinuation || key != null) && oddTrailingBackslashes(line);
            if (key != null && updates.containsKey(key)) {
                if (continued) {
                    return new Result(AdminCodes.REJECTED, "continuation:" + key, sha, null);
                }
                lines.set(i, key + "=" + updates.get(key));
                seen.put(key, true);
            }
        }
        if (continued) {
            // the file ends inside a continued line: appending would join our key onto it
            return new Result(AdminCodes.REJECTED, "danglingContinuation", sha, null);
        }
        for (Map.Entry<String, String> u : updates.entrySet()) {
            if (!seen.containsKey(u.getKey())) {
                if (!endings.isEmpty() && endings.get(endings.size() - 1).isEmpty()) {
                    endings.set(endings.size() - 1, eol); // the last line had no line break: give it one before appending
                }
                lines.add(u.getKey() + "=" + u.getValue());
                endings.add(eol);
            }
        }
        StringBuilder out = new StringBuilder(text.length() + 128);
        for (int i = 0; i < lines.size(); i++) {
            out.append(lines.get(i)).append(endings.get(i));
        }
        byte[] after = out.toString().getBytes(StandardCharsets.ISO_8859_1);
        Properties oldProps = load(before);
        Properties newProps = load(after);
        for (Map.Entry<String, String> u : updates.entrySet()) {
            if (!u.getValue().equals(newProps.getProperty(u.getKey()))) {
                return new Result(AdminCodes.REJECTED, "verify:" + u.getKey(), sha, null);
            }
        }
        for (String k : oldProps.stringPropertyNames()) {
            if (!updates.containsKey(k) && !Objects.equals(oldProps.getProperty(k), newProps.getProperty(k))) {
                return new Result(AdminCodes.REJECTED, "verify:" + k, sha, null);
            }
        }
        for (String k : newProps.stringPropertyNames()) {
            if (!updates.containsKey(k) && !oldProps.containsKey(k)) {
                return new Result(AdminCodes.REJECTED, "verify:" + k, sha, null);
            }
        }
        Path abs = file.toAbsolutePath();
        Path backup = backupPath(abs, stamp);
        Files.write(backup, before, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        Path tmp = Files.createTempFile(abs.getParent(), abs.getFileName() + ".", ".admin.tmp");
        try {
            Files.write(tmp, after, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            replace(tmp, abs);
        } finally {
            Files.deleteIfExists(tmp);
        }
        return new Result(AdminCodes.OK, null, sha256(after), backup);
    }

    private static Path backupPath(Path abs, String stamp) throws IOException {
        for (int n = 0; n < 100; n++) {
            Path p = abs.resolveSibling(abs.getFileName() + ".bak-" + stamp + (n == 0 ? "" : "-" + n));
            if (!Files.exists(p)) {
                return p;
            }
        }
        throw new FileAlreadyExistsException(abs + ".bak-" + stamp);
    }

    /** The key of a {@code key=value} / {@code key: value} / {@code key value} line; null for blank and comment lines. */
    static String keyOf(String line) {
        int i = 0;
        int n = line.length();
        while (i < n && (line.charAt(i) == ' ' || line.charAt(i) == '\t' || line.charAt(i) == '\f')) {
            i++;
        }
        if (i >= n || line.charAt(i) == '#' || line.charAt(i) == '!') {
            return null;
        }
        StringBuilder key = new StringBuilder();
        for (; i < n; i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                // an escaped key never equals one of ours (plain [a-z.] names); keep it distinct
                key.append('\\');
                if (i + 1 < n) {
                    key.append(line.charAt(++i));
                }
                continue;
            }
            if (c == '=' || c == ':' || c == ' ' || c == '\t' || c == '\f') {
                break;
            }
            key.append(c);
        }
        return key.toString();
    }

    static boolean oddTrailingBackslashes(String line) {
        int n = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            n++;
        }
        return n % 2 == 1;
    }

    static Properties load(byte[] bytes) {
        Properties p = new Properties();
        try {
            p.load(new ByteArrayInputStream(bytes));
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("server.properties cannot be parsed: " + e.getMessage(), e);
        }
        return p;
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Moves {@code tmp} over {@code target} (atomically where possible). Windows refuses the replace while another
     * process or thread has {@code target} open without delete sharing (the server's own hot reload, a virus scanner,
     * an editor): retried for about a second before giving up.
     */
    static void replace(Path tmp, Path target) throws IOException {
        FileSystemException last = null;
        for (int i = 0; i < 25; i++) {
            try {
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (AtomicMoveNotSupportedException e) {
                throw e;
            } catch (FileSystemException e) {
                last = e;
                try {
                    Thread.sleep(40);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last != null ? last : new IOException("cannot replace " + target);
    }
}
