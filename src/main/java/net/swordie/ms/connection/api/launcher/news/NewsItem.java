package net.swordie.ms.connection.api.launcher.news;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * One launcher notice. Plain text only: the server neither interprets nor strips markup (a {@code <b>} stays the four
 * characters it is - the launcher shows text with textContent); {@link #clean} only normalises line breaks to LF and
 * drops other control characters.
 *
 * @param kind    {@code ev} (event), {@code up} (update), {@code mt} (maintenance)
 * @param startAt epoch milliseconds from which it is shown; null = always
 * @param endAt   epoch milliseconds until which it is shown (inclusive); null = no end
 */
public record NewsItem(String id, String kind, String title, String body, Long startAt, Long endAt, boolean pinned) {
    public static final Set<String> KINDS = Set.of("ev", "up", "mt");
    public static final int MAX_TITLE = 80;
    public static final int MAX_BODY = 2000;

    public NewsItem {
        Objects.requireNonNull(id, "id");
        if (!KINDS.contains(kind)) {
            throw new IllegalArgumentException("kind " + kind);
        }
        title = clip(clean(title), MAX_TITLE);
        body = clip(clean(body), MAX_BODY);
    }

    /** Shown at {@code now}: inside [startAt, endAt]. */
    public boolean visibleAt(long now) {
        return (startAt == null || now >= startAt) && (endAt == null || now <= endAt);
    }

    /** Wire order: id, kind, title, body, startAt, endAt, pinned. */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("kind", kind);
        m.put("title", title);
        m.put("body", body);
        m.put("startAt", startAt);
        m.put("endAt", endAt);
        m.put("pinned", pinned);
        return m;
    }

    /** CRLF / CR -> LF; every other control character (C0 except LF, DEL, C1) removed; null -> "". */
    public static String clean(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder sb = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\n' || !(c < 0x20 || (c >= 0x7F && c <= 0x9F))) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** At most {@code max} code points. */
    static String clip(String s, int max) {
        return s.codePointCount(0, s.length()) <= max ? s : s.substring(0, s.offsetByCodePoints(0, max));
    }

    /** Code points (what the length limits count). */
    static int length(String s) {
        return s.codePointCount(0, s.length());
    }
}
