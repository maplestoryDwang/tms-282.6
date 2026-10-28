package net.swordie.ms.console.config;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.JobConstants;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class ServerRuntimeConfig {
    public static final Path CONFIG_FILE = Path.of("config", "server.properties");

    private static final Properties defaults = new Properties();
    private static Properties values;

    static {
        defaults.setProperty("server.name", "MapleStory");
        defaults.setProperty("server.host", "127.0.0.1");
        defaults.setProperty("login.port", "8484");
        defaults.setProperty("api.port", "8483");
        defaults.setProperty("channel.count", "10");
        defaults.setProperty("db.host", "127.0.0.1");
        defaults.setProperty("db.port", "3307");
        defaults.setProperty("db.name", "265");
        defaults.setProperty("db.user", "tms2825");
        defaults.setProperty("db.password", "12345678");
        for (JobConstants.LoginJob job : JobConstants.LoginJob.values()) {
            defaults.setProperty(jobCreationKey(job), String.valueOf(job.getFlag() != 0));
        }
        reload();
    }

    private ServerRuntimeConfig() {
    }

    public static synchronized void reload() {
        Properties next = new Properties();
        next.putAll(defaults);
        if (Files.isRegularFile(CONFIG_FILE)) {
            try (FileInputStream in = new FileInputStream(CONFIG_FILE.toFile())) {
                next.load(in);
            } catch (IOException ignored) {
            }
        }
        next.setProperty("server.host", "127.0.0.1");
        next.setProperty("db.host", "127.0.0.1");
        next.setProperty("net.allow", "127.0.0.1");
        next.setProperty("api.http.allow", "127.0.0.1");
        next.setProperty("tms282.channelAdvertisedHost", "127.0.0.1");
        values = next;
    }

    public static synchronized Properties snapshot() {
        Properties copy = new Properties();
        copy.putAll(defaults);
        copy.putAll(values);
        return copy;
    }

    /** Writes only the given keys in place: the rest of server.properties (comments, tms282.* flags, rate.* lines
     * saved elsewhere) is kept, instead of re-storing a stale snapshot of the whole file. */
    public static synchronized void save(Map<String, String> updates) throws IOException {
        Map<String, String> trimmed = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (key.equals("server.host") || key.equals("db.host") || key.equals("net.allow")
                    || key.equals("api.http.allow") || key.equals("tms282.channelAdvertisedHost")) {
                value = "127.0.0.1";
            }
            trimmed.put(key, value);
        }
        net.swordie.ms.util.PropertiesFileLines.put(CONFIG_FILE, trimmed);
        reload();
    }

    public static String stringValue(String key) {
        return snapshot().getProperty(key, defaults.getProperty(key, ""));
    }

    /**
     * The address login and migration packets give the game (2026-10-08). The 282.5 client ends itself about 70 s after
     * connecting when its server is a plain 127.0.0.1 / LAN address; at the official address it runs. The launcher puts
     * this address on the Windows loopback interface only while the game runs and relays it to the server on 127.0.0.1,
     * so nothing outside this computer can reach it. Every listener still binds 127.0.0.1 only.
     */
    public static final String PACKET_HOST = "202.80.104.26";

    public static byte[] serverHostBytes() {
        return new byte[]{(byte) 202, 80, 104, 26};
    }

    public static int intValue(String key) {
        return intValue(key, Integer.parseInt(defaults.getProperty(key, "0")));
    }

    public static int intValue(String key, int fallback) {
        String raw = stringValue(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static boolean isJobCreationEnabled(JobConstants.LoginJob job) {
        return Boolean.parseBoolean(stringValue(jobCreationKey(job)));
    }

    public static String jobCreationKey(JobConstants.LoginJob job) {
        return "job.create." + job.getJobType();
    }

    public static String jdbcUrl() {
        return "jdbc:mariadb://" + stringValue("db.host") + ":" + intValue("db.port", 3306) + "/"
                + stringValue("db.name") + "?allowMultiQueries=true";
    }

    public static synchronized void applyToRuntime() {
        ServerConstants.SERVER_NAME = stringValue("server.name");
        ServerConstants.LOGIN_PORT = intValue("login.port", 8484);
        ServerConstants.API_PORT = intValue("api.port", 8483);
        GameConstants.CHANNELS_PER_WORLD = intValue("channel.count", 10);
    }

    public static Map<String, String> defaultFormValues() {
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : defaults.stringPropertyNames()) {
            map.put(key, stringValue(key));
        }
        return map;
    }
}
