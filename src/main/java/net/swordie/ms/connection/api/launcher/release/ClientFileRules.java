package net.swordie.ms.connection.api.launcher.release;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The rules of the signed client file list (cat-launcher 1.2.3, docs/launcher-api-update.md sections 12 to 17), exactly as
 * the launcher checks them (electron/services/update/client-files-data.cjs) and release-sign.mjs client-sign writes them:
 * <ul>
 *     <li>a path is relative, '/'-separated, 1 to {@value #SEGMENTS_MAX} segments, each alphanumeric or {@code _}
 *     first then alphanumerics {@code . _ -}, at most 64 characters, not ending in '.', not a Windows device name; at
 *     most {@value #PATH_MAX} characters - so it can never leave the folder it is resolved against ({@code .} and
 *     {@code ..} can never be a segment: the first character is never a dot);</li>
 *     <li>path rules 2 (fixes10, 2026-10-05): a segment may start with {@code _} (e.g. {@code _Canvas}). Launchers up
 *     to 1.2.5 only know rules 1 (alphanumeric first) and refuse the WHOLE list when one entry breaks it
 *     (client-files-data.cjs sanitizeList returns null), and the list is signed as a whole, so it cannot be filtered
 *     per launcher: a list that needs rules 2 ({@link #needsPathRules2}) is offered only to launchers that send
 *     {@code "pathRules":2} ({@link #declaredPathRules}); older ones are told nothing is published.</li>
 *     <li>one file at most 2 GB, all files at most 10 GB, 1 to {@value #FILES_MAX} files, no duplicate path (case
 *     insensitive);</li>
 *     <li>the signature (Ed25519, made on the host by release-sign.mjs; the server has no key and never checks it) is
 *     over {@link #canonical}: {@code cat-launcher-client-files LF version LF count LF (path TAB size TAB sha256)...}
 *     sorted by path.</li>
 * </ul>
 */
public final class ClientFileRules {
    public static final String TAG = "cat-launcher-client-files";
    public static final String FORMAT = "cat-launcher-client-files";
    public static final long FILE_SIZE_MAX = 2L * 1024 * 1024 * 1024;
    public static final long TOTAL_SIZE_MAX = 10L * 1024 * 1024 * 1024;
    public static final int FILES_MAX = 1000;
    public static final int PATH_MAX = 200;
    public static final int SEGMENTS_MAX = 8;
    /** client-files.json may be at most 512 KB (the launcher caps the clientFiles answer at 256 KB; 396 files are about 45 KB). */
    public static final int LIST_MAX_BYTES = 512 * 1024;
    /** Path rules 2 = cat-launcher client-files-data.cjs SEGMENT_RE {@code ^[A-Za-z0-9_][A-Za-z0-9._-]{0,63}$}. */
    static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9._-]{0,63}");
    /** Path rules 1 (launchers 1.2.3 to 1.2.5): no leading {@code _}. */
    static final Pattern SEGMENT_V1 = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    /** The rules this server knows (request field {@code pathRules}; /api/ping feature {@code clientFiles.pathRules2}). */
    public static final int PATH_RULES = 2;
    static final Pattern RESERVED = Pattern.compile("(?i)(con|prn|aux|nul|com[0-9]|lpt[0-9]|conin\\$|conout\\$)(\\..*)?");
    static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    private ClientFileRules() {
    }

    public static boolean isSafePath(String p) {
        if (p == null || p.isEmpty() || p.length() > PATH_MAX) {
            return false;
        }
        String[] segs = p.split("/", -1);
        if (segs.length > SEGMENTS_MAX) {
            return false;
        }
        for (String s : segs) {
            if (!SEGMENT.matcher(s).matches() || RESERVED.matcher(s).matches() || s.endsWith(".")) {
                return false;
            }
        }
        return true;
    }

    /** The path is also valid under path rules 1 (what launchers up to 1.2.5 accept). */
    public static boolean isLegacySafePath(String p) {
        if (!isSafePath(p)) {
            return false;
        }
        for (String s : p.split("/", -1)) {
            if (!SEGMENT_V1.matcher(s).matches()) {
                return false;
            }
        }
        return true;
    }

    /** Some entry of {@code files} needs path rules 2 (a segment starting with {@code _}). */
    public static boolean needsPathRules2(List<ClientFileSet.Entry> files) {
        for (ClientFileSet.Entry e : files) {
            if (!isLegacySafePath(e.path())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The path rules the launcher declares in its request: {@code "pathRules":2} (a JSON integer, or a string of
     * digits); 1 when absent or malformed (launchers up to 1.2.5 send nothing).
     */
    public static int declaredPathRules(com.fasterxml.jackson.databind.JsonNode request) {
        com.fasterxml.jackson.databind.JsonNode n = request == null ? null : request.get("pathRules");
        if (n == null) {
            return 1;
        }
        if (n.isInt() && n.intValue() >= 1 && n.intValue() <= 99) {
            return n.intValue();
        }
        if (n.isTextual() && n.textValue().matches("[1-9][0-9]?")) {
            return Integer.parseInt(n.textValue());
        }
        return 1;
    }

    /** {@code set} may be offered to a launcher that declared {@code pathRules}. */
    public static boolean offerable(ClientFileSet set, int pathRules) {
        return set != null && (pathRules >= PATH_RULES || !set.pathRules2());
    }

    /** {@code rel} under {@code root}, or null when the rule fails or the result is not strictly inside root. */
    public static Path resolveInside(Path root, String rel) {
        if (root == null || !isSafePath(rel)) {
            return null;
        }
        Path base = root.toAbsolutePath().normalize();
        Path full = base;
        for (String s : rel.split("/")) {
            full = full.resolve(s);
        }
        full = full.normalize();
        return full.startsWith(base) && !full.equals(base) ? full : null;
    }

    public static boolean isSha256(String s) {
        return s != null && SHA256.matcher(s).matches();
    }

    /** The signed string (UTF-8, LF, no final newline); {@code files} sorted by path. */
    public static String canonical(String version, List<ClientFileSet.Entry> files) {
        StringBuilder b = new StringBuilder(TAG).append('\n').append(version).append('\n').append(files.size());
        for (ClientFileSet.Entry f : files) {
            b.append('\n').append(f.path()).append('\t').append(f.size()).append('\t').append(f.sha256());
        }
        return b.toString();
    }

    /** Lower-case SHA-256 of {@link #canonical} = launcherVersion's {@code clientFiles.listSha256}. */
    public static String listSha256(String version, List<ClientFileSet.Entry> files) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(version, files).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
