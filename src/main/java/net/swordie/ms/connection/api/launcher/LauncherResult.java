package net.swordie.ms.connection.api.launcher;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One launcher response: {@code ok}, {@code code}, then extra fields in insertion order - exactly the JSON object the
 * CLI and HTTP endpoints write. Extra fields may carry a bearer token or a recovery code, so {@link #toString()} never
 * prints them.
 */
public final class LauncherResult {
    private final LinkedHashMap<String, Object> fields = new LinkedHashMap<>();

    private LauncherResult(boolean ok, String code) {
        fields.put("ok", ok);
        fields.put("code", code);
    }

    public static LauncherResult ok(String code) {
        return new LauncherResult(true, code);
    }

    public static LauncherResult fail(String code) {
        return new LauncherResult(false, code);
    }

    /** Appends (or replaces) an extra field; {@code ok} and {@code code} are fixed at construction. */
    public LauncherResult with(String key, Object value) {
        if ("ok".equals(key) || "code".equals(key)) {
            throw new IllegalArgumentException("fixed field " + key);
        }
        fields.put(key, value);
        return this;
    }

    public boolean ok() {
        return Boolean.TRUE.equals(fields.get("ok"));
    }

    public String code() {
        return (String) fields.get("code");
    }

    public Object get(String key) {
        return fields.get(key);
    }

    /** A copy in wire order (ok, code, extras). */
    public Map<String, Object> toMap() {
        return new LinkedHashMap<>(fields);
    }

    @Override
    public String toString() {
        return "LauncherResult[ok=" + ok() + ", code=" + code() + ", fields=" + fields.keySet() + "]";
    }
}
