package net.swordie.ms.connection.api.launcher;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A planned restart the admin console announced (launcher 1.2.8): {@code status} carries it as
 * {@code restart: {at, message}}, the launcher shows "N 分鐘後維護重啟" and, when the game then closes, explains why.
 * In memory only - a restart is what ends it. It stays visible for {@link #LINGER_MS} after {@code at} (a deploy that
 * runs late), then drops.
 */
public final class LauncherNotices {
    public static final int MAX_MESSAGE = 200;
    public static final long LINGER_MS = 15 * 60_000L;

    /** @param at epoch milliseconds of the planned restart */
    public record Restart(long at, String message) {
    }

    private static volatile Restart restart;

    private LauncherNotices() {
    }

    /** Announces a restart at {@code at}; a longer message is clipped. */
    public static Restart announce(long at, String message) {
        String text = message == null ? "" : message.strip();
        if (text.length() > MAX_MESSAGE) {
            text = text.substring(0, MAX_MESSAGE);
        }
        Restart r = new Restart(at, text);
        restart = r;
        return r;
    }

    public static void clear() {
        restart = null;
    }

    /** The announced restart, or null when none / long past. */
    public static Restart restart(long now) {
        Restart r = restart;
        if (r != null && now > r.at() + LINGER_MS) {
            restart = null;
            return null;
        }
        return r;
    }

    /** {@code {at, message}} for the wire, or null. */
    public static Map<String, Object> restartMap(long now) {
        Restart r = restart(now);
        if (r == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("at", r.at());
        m.put("message", r.message());
        return m;
    }
}
