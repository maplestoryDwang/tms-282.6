package net.swordie.ms.connection.api.launcher.release;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The value rules of {@code data/launcher/release.json} and of the launcher's {@code download} object, exactly as
 * cat-launcher 1.2.0 checks them (electron/services/update/update-data.cjs; docs/launcher-api-update.md sections 2, 7
 * and 9). The server and the launcher must agree, so a file the launcher would reject is never served.
 */
public final class ReleaseRules {
    /** x.y.z: three non-negative integers without leading zeros, x and y at most 4 digits, z at most 6. */
    static final Pattern VERSION = Pattern.compile("(0|[1-9]\\d{0,3})\\.(0|[1-9]\\d{0,3})\\.(0|[1-9]\\d{0,5})");
    /** The update file name: alphanumeric first, then alphanumerics . _ -, lower-case .exe, at most 104 characters. */
    static final Pattern FILE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}\\.exe");
    /** Windows device names (also with an extension: CON.exe), case-insensitive. */
    static final Pattern RESERVED = Pattern.compile("(?i)(con|prn|aux|nul|com[0-9]|lpt[0-9]|conin\\$|conout\\$)(\\..*)?");
    /** The server answers sha256 exactly as release-sign.mjs wrote it: 64 lower-case hex digits. */
    static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    /** Ed25519 signature (64 bytes) in standard base64: 86 characters and "==". */
    static final Pattern SIGNATURE = Pattern.compile("[A-Za-z0-9+/]{86}==");
    public static final long SIZE_MAX = 300L * 1024 * 1024;
    /** release.json may be at most 16 KB (the launcher caps the whole /api/v2/launcher answer at 8 KB). */
    public static final int FILE_MAX_BYTES = 16 * 1024;
    public static final int NOTES_MAX = 10;
    public static final int NOTE_MAX = 100;
    static final int PUBLISHED_AT_MAX = 40;

    private ReleaseRules() {
    }

    /** x.y.z -> {x, y, z}; anything else (null, "v1.2.0", "1.2.0-beta", "01.2.0") -> null. */
    public static int[] parseVersion(String v) {
        if (v == null || v.length() > 20) {
            return null;
        }
        Matcher m = VERSION.matcher(v);
        if (!m.matches()) {
            return null;
        }
        return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
    }

    public static boolean isVersion(String v) {
        return parseVersion(v) != null;
    }

    /** a &lt; b: negative, equal: 0, a &gt; b: positive. Both must be x.y.z. */
    public static int compare(String a, String b) {
        int[] x = parseVersion(a);
        int[] y = parseVersion(b);
        if (x == null || y == null) {
            throw new IllegalArgumentException("not x.y.z");
        }
        for (int i = 0; i < 3; i++) {
            if (x[i] != y[i]) {
                return Integer.compare(x[i], y[i]);
            }
        }
        return 0;
    }

    /** The file name rule plus "not a Windows device name". */
    public static boolean isSafeFileName(String name) {
        return name != null && FILE.matcher(name).matches() && !RESERVED.matcher(name).matches();
    }

    public static boolean isSha256(String s) {
        return s != null && SHA256.matcher(s).matches();
    }

    public static boolean isSignature(String s) {
        return s != null && SIGNATURE.matcher(s).matches();
    }

    public static boolean isSize(long size) {
        return size >= 1 && size <= SIZE_MAX;
    }

    /**
     * One note line: control and invisible direction characters become spaces, runs of white space one space, trimmed,
     * at most {@value #NOTE_MAX} characters (code points); null when nothing is left.
     */
    public static String cleanNote(String s) {
        if (s == null) {
            return null;
        }
        String t = s.length() > 1000 ? s.substring(0, 1000) : s;
        StringBuilder b = new StringBuilder(t.length());
        t.codePoints().forEach(cp -> b.appendCodePoint(invisible(cp) ? ' ' : cp));
        String out = b.toString().replaceAll("\\s+", " ").strip();
        if (out.isEmpty()) {
            return null;
        }
        if (out.codePointCount(0, out.length()) > NOTE_MAX) {
            out = out.substring(0, out.offsetByCodePoints(0, NOTE_MAX));
        }
        return out;
    }

    private static boolean invisible(int cp) {
        return cp <= 0x1F || (cp >= 0x7F && cp <= 0x9F) || (cp >= 0x200B && cp <= 0x200F) || (cp >= 0x202A && cp <= 0x202E)
                || (cp >= 0x2066 && cp <= 0x2069);
    }
}
