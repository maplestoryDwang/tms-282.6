package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.console.config.ServerRuntimeConfig;

import java.time.DayOfWeek;
import java.util.Locale;

/**
 * Boss-run switches. A {@code -D} JVM property wins; otherwise {@code config/server.properties}; otherwise the default.
 * <pre>
 * tms282.bossrun.enabled=true            master switch (limits, clear log, rewards, broadcast; 282 profile only)
 * tms282.bossrun.limitMode=account       account (TMS273 party_boss: per login account) | character (legacy BossPartyType/partyboss)
 * tms282.bossrun.rewards.enabled=true    BOSS_REWARD_APPLY rewards on clear
 * tms282.bossrun.broadcast.enabled=true  world message with clear time and damage shares
 * tms282.bossrun.weekResetDay=MONDAY     weekly counter reset (00:00 server time); TMS273 counted Monday weeks
 * </pre>
 */
public record BossRunConfig(boolean enabled, LimitMode limitMode, boolean rewardsEnabled, boolean broadcastEnabled,
                            DayOfWeek weekResetDay) {

    public enum LimitMode { ACCOUNT, CHARACTER }

    public static final String PREFIX = "tms282.bossrun.";

    public static BossRunConfig load() {
        return new BossRunConfig(
                bool("enabled", true),
                "character".equalsIgnoreCase(str("limitMode", "account")) ? LimitMode.CHARACTER : LimitMode.ACCOUNT,
                bool("rewards.enabled", true),
                bool("broadcast.enabled", true),
                day(str("weekResetDay", "MONDAY")));
    }

    public static BossRunConfig defaults() {
        return new BossRunConfig(true, LimitMode.ACCOUNT, true, true, DayOfWeek.MONDAY);
    }

    public BossRunConfig withLimitMode(LimitMode mode) {
        return new BossRunConfig(enabled, mode, rewardsEnabled, broadcastEnabled, weekResetDay);
    }

    public BossRunConfig withRewards(boolean on) {
        return new BossRunConfig(enabled, limitMode, on, broadcastEnabled, weekResetDay);
    }

    static String str(String key, String fallback) {
        String sys = System.getProperty(PREFIX + key);
        if (sys != null && !sys.isBlank()) return sys.trim();
        String file;
        try {
            file = ServerRuntimeConfig.stringValue(PREFIX + key);
        } catch (RuntimeException | LinkageError e) {
            file = null;
        }
        return file == null || file.isBlank() ? fallback : file.trim();
    }

    static boolean bool(String key, boolean fallback) {
        String v = str(key, String.valueOf(fallback));
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "on".equalsIgnoreCase(v);
    }

    static DayOfWeek day(String v) {
        try {
            return DayOfWeek.valueOf(v.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DayOfWeek.MONDAY;
        }
    }
}
