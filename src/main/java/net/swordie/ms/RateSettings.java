package net.swordie.ms;

import net.swordie.ms.connection.protocol.ProtocolConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Server-wide EXP / meso / drop multipliers. Values live in {@link ServerConfig} (read on every kill), load from
 * {@code config/server.properties} at class init, and can be changed at runtime by the GUI config panel or the
 * {@code !rate} command. Saving rewrites only the {@code rate.*} lines, keeping every other line and comment.
 */
public final class RateSettings {
    public static final int MIN = 1;
    public static final int MAX = 10_000;

    public enum Rate {
        EXP_1_100("rate.exp.1-100", "經驗倍率 Lv1–100", () -> ServerConfig.EXP_RATE_BELOW_100, v -> ServerConfig.EXP_RATE_BELOW_100 = v),
        EXP_101_210("rate.exp.101-210", "經驗倍率 Lv101–210", () -> ServerConfig.EXP_RATE_101_210, v -> ServerConfig.EXP_RATE_101_210 = v),
        EXP_211("rate.exp.211", "經驗倍率 Lv211 以上", () -> ServerConfig.EXP_RATE, v -> ServerConfig.EXP_RATE = v),
        MESO("rate.meso", "金幣倍率", () -> ServerConfig.MESO_RATE, v -> ServerConfig.MESO_RATE = v),
        DROP("rate.drop", "掉寶倍率", () -> ServerConfig.DROP_RATE, v -> ServerConfig.DROP_RATE = v),
        QUEST_EXP("rate.quest.exp", "任務經驗倍率", () -> ServerConfig.QUEST_EXP_RATE, v -> ServerConfig.QUEST_EXP_RATE = v);

        private final String key;
        private final String label;
        private final IntSupplier getter;
        private final IntConsumer setter;

        Rate(String key, String label, IntSupplier getter, IntConsumer setter) {
            this.key = key;
            this.label = label;
            this.getter = getter;
            this.setter = setter;
        }

        public String key() { return key; }
        public String label() { return label; }
        public int get() { return getter.getAsInt(); }
    }

    private static Path configFile = ProtocolConfig.CONFIG_FILE;

    private RateSettings() { }

    /** Tests only: point load/save at another file. */
    static void useConfigFile(Path file) {
        configFile = file;
    }

    public static boolean valid(int value) {
        return value >= MIN && value <= MAX;
    }

    /** Applies {@code rate.*} from the config file, then JVM {@code -D} overrides; invalid values are ignored. */
    public static synchronized void load() {
        Properties p = new Properties();
        if (Files.isRegularFile(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                p.load(in);
            } catch (IOException e) {
                System.err.println("[RateSettings] cannot read " + configFile + ": " + e.getMessage());
            }
        }
        for (Rate r : Rate.values()) {
            String raw = System.getProperty(r.key, p.getProperty(r.key));
            if (raw == null || raw.isBlank()) continue;
            try {
                int v = Integer.parseInt(raw.trim());
                if (valid(v)) {
                    r.setter.accept(v);
                } else {
                    System.err.println("[RateSettings] " + r.key + "=" + raw + " out of range " + MIN + ".." + MAX + ", kept " + r.get());
                }
            } catch (NumberFormatException e) {
                System.err.println("[RateSettings] " + r.key + "=" + raw + " is not a number, kept " + r.get());
            }
        }
    }

    /** Sets the live value first; with {@code persist} then writes the {@code rate.*} line. If the write fails the
     * live value stays applied and the IOException tells the caller the file was not updated. */
    public static synchronized void set(Rate rate, int value, boolean persist) throws IOException {
        if (!valid(value)) {
            throw new IllegalArgumentException(rate.label + " 必須介於 " + MIN + " 到 " + MAX);
        }
        rate.setter.accept(value);
        if (persist) {
            writeLine(rate.key, String.valueOf(value));
        }
    }

    static void writeLine(String key, String value) throws IOException {
        net.swordie.ms.util.PropertiesFileLines.put(configFile, java.util.Map.of(key, value));
    }

    /** Re-reads the rate.* keys and the {@link GameSettings} keys (which also reload by themselves when the file changes). */
    public static void reloadAll() {
        load();
        GameSettings.load();
    }

    public static String summary() {
        StringBuilder sb = new StringBuilder();
        for (Rate r : Rate.values()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(r.label).append(" x").append(r.get());
        }
        return sb.toString();
    }
}
