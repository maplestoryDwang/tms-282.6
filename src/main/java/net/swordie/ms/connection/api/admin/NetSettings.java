package net.swordie.ms.connection.api.admin;

import net.swordie.ms.connection.api.launcher.SourceAllowlist;
import net.swordie.ms.connection.netty.GameSourceGate;
import net.swordie.ms.connection.netty.LoginIdleTimeout;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.util.HotProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The four connection-security keys the console may change, their validation and their effective values. Each list
 * must parse completely (no ignored entry) and must admit 127.0.0.1, so a change can never lock the host out of its
 * own server; numbers must be in the range the server accepts (outside it the server would silently use its default).
 */
final class NetSettings {
    static final String NET_ALLOW = GameSourceGate.KEY;
    static final String MAX_PER_IP = GameSourceGate.MAX_PER_IP_KEY;
    static final String IDLE_SEC = LoginIdleTimeout.KEY;
    static final String HTTP_ALLOW = SourceAllowlist.KEY;
    static final List<String> KEYS = List.of(NET_ALLOW, MAX_PER_IP, IDLE_SEC, HTTP_ALLOW);

    private NetSettings() {
    }

    /** Null when {@code value} is acceptable for {@code key}, else the reason (shown by the console). */
    static String problem(String key, String value) {
        if (value == null) {
            return "empty";
        }
        String v = value.strip();
        switch (key) {
            case NET_ALLOW, HTTP_ALLOW -> {
                return v.equals("127.0.0.1") || v.equals("127.0.0.1/32") ? null : "localOnly:127.0.0.1";
            }
            case MAX_PER_IP -> {
                return integer(v, 0, 100_000) ? null : "range:0-100000";
            }
            case IDLE_SEC -> {
                return integer(v, 0, 3600) ? null : "range:0-3600";
            }
            default -> {
                return "unknownKey";
            }
        }
    }

    /** {@code a , b} -> {@code a,b} for the lists; numbers trimmed. */
    static String normalize(String key, String value) {
        String v = value.strip();
        if (key.equals(NET_ALLOW) || key.equals(HTTP_ALLOW)) {
            return "127.0.0.1";
        }
        return v;
    }

    private static boolean integer(String v, int min, int max) {
        if (!v.matches("[0-9]{1,6}")) {
            return false;
        }
        int n = Integer.parseInt(v);
        return n >= min && n <= max;
    }

    /** What the server uses right now, as text. */
    static String effective(String key) {
        return switch (key) {
            case NET_ALLOW -> GameSourceGate.standard().allowlist().toString();
            case MAX_PER_IP -> String.valueOf(GameSourceGate.standard().maxPerIp());
            case IDLE_SEC -> String.valueOf(LoginIdleTimeout.configuredSeconds(HotProperties.server()));
            case HTTP_ALLOW -> SourceAllowlist.current().toString();
            default -> null;
        };
    }

    static String defaultValue(String key) {
        return switch (key) {
            case NET_ALLOW -> GameSourceGate.DEFAULT;
            case MAX_PER_IP -> String.valueOf(GameSourceGate.DEFAULT_MAX_PER_IP);
            case IDLE_SEC -> String.valueOf(LoginIdleTimeout.DEFAULT_SECONDS);
            case HTTP_ALLOW -> SourceAllowlist.DEFAULT;
            default -> null;
        };
    }

    /** How the server picks the new file value up: the game-port keys through HotProperties (3 s), api.http.allow on reload. */
    static void reload(boolean httpAllowChanged) {
        HotProperties.server().invalidate();
        if (httpAllowChanged) {
            ServerRuntimeConfig.reload();
        }
    }

    /** {@code -Dkey=...} on the server's command line: the file value would not apply. */
    static boolean jvmOverride(String key) {
        if (key.equals(NET_ALLOW) || key.equals(HTTP_ALLOW)) {
            return false;
        }
        return System.getProperty(key) != null;
    }

    static Map<String, Object> describe(String key, byte[] file) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("fileValue", file == null ? null : ServerPropertiesEditor.value(file, key));
        m.put("effective", effective(key));
        m.put("default", defaultValue(key));
        m.put("jvmOverride", jvmOverride(key));
        return m;
    }
}
