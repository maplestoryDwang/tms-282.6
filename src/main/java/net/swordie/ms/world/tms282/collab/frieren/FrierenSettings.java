package net.swordie.ms.world.tms282.collab.frieren;

import net.swordie.ms.connection.protocol.ProtocolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Settings of the Frieren collab ({@code config/server.properties}; a JVM {@code -D} property with the same key
 * wins). Numbers come from the 282 strings unless noted.
 * <ul>
 *     <li>{@code tms282.frieren.enabled} (default true): kill gauge, clone monster, companion damage, Mini
 *     Frieren passive. Server logic plus packets the port already sends (mob enter/leave, chat line);</li>
 *     <li>{@code tms282.frieren.eventEnd} (default 202610062359, quest 68299..68305 {@code end}); after it the
 *     gauge stops;</li>
 *     <li>{@code tms282.frieren.levelRange} (default 20, policy: the usual 「等級範圍怪物」 |mob - character| &lt;=
 *     20; Skill 80012711 common/x = 15 is a candidate, unconfirmed);</li>
 *     <li>{@code tms282.frieren.gaugeMax} (500, desc), {@code tms282.frieren.dailyCloneLimit} (20, desc and quest
 *     600141), {@code tms282.frieren.clonesPerFragment} (2, desc), {@code tms282.frieren.castsPerClone} (2,
 *     {@code h}: 發動次數增加2次), {@code tms282.frieren.cloneLifetimeSec} (120, policy);</li>
 *     <li>{@code tms282.frieren.gaugePacket} (default <b>false</b>): LP0616 (6 x i32, skill 80012711 branch of
 *     CUserLocal 592BE10, semantics only partly known), see {@link FrierenPackets};</li>
 *     <li>{@code tms282.frieren.companionSummons} (default <b>false</b>): summons 80012692..94 for 一起冒險 through
 *     LP0664 (the summon ids are presumed);</li>
 *     <li>{@code tms282.frieren.stateDir} (default {@code data/tms282-frieren}): per-character state files.</li>
 * </ul>
 */
public record FrierenSettings(boolean enabled, LocalDateTime eventEnd, int levelRange, int gaugeMax, int dailyCloneLimit,
                              int clonesPerFragment, int castsPerClone, int cloneLifetimeSec, boolean gaugePacket,
                              boolean companionSummons, Path stateDir) {
    public static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    public static final String PREFIX = "tms282.frieren.";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public FrierenSettings {
        if (levelRange < 0 || gaugeMax <= 0 || dailyCloneLimit < 0 || clonesPerFragment <= 0 || castsPerClone < 0
                || cloneLifetimeSec < 0) {
            throw new IllegalArgumentException("invalid Frieren settings");
        }
    }

    public static FrierenSettings defaults() {
        return fromProperties(new Properties());
    }

    public static FrierenSettings load() {
        Properties p = new Properties();
        if (Files.isRegularFile(ProtocolConfig.CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(ProtocolConfig.CONFIG_FILE)) {
                p.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + ProtocolConfig.CONFIG_FILE, e);
            }
        }
        return fromProperties(p);
    }

    public static FrierenSettings fromProperties(Properties file) {
        return new FrierenSettings(
                bool(file, "enabled", true),
                LocalDateTime.parse(value(file, "eventEnd", "202610062359"), STAMP),
                integer(file, "levelRange", 20),
                integer(file, "gaugeMax", 500),
                integer(file, "dailyCloneLimit", 20),
                integer(file, "clonesPerFragment", 2),
                integer(file, "castsPerClone", 2),
                integer(file, "cloneLifetimeSec", 120),
                bool(file, "gaugePacket", false),
                bool(file, "companionSummons", false),
                Path.of(value(file, "stateDir", "data/tms282-frieren")));
    }

    /** True while the event runs ({@code now} in Taipei time). */
    public boolean active(LocalDateTime now) {
        return enabled && !now.isAfter(eventEnd);
    }

    private static String value(Properties file, String key, String fallback) {
        String v = System.getProperty(PREFIX + key);
        if (v == null) {
            v = file.getProperty(PREFIX + key);
        }
        return v == null || v.isBlank() ? fallback : v.strip();
    }

    private static int integer(Properties file, String key, int fallback) {
        return Integer.parseInt(value(file, key, Integer.toString(fallback)));
    }

    private static boolean bool(Properties file, String key, boolean fallback) {
        String v = value(file, key, Boolean.toString(fallback));
        if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(PREFIX + key + " must be true or false, got " + v);
        }
        return Boolean.parseBoolean(v);
    }
}
