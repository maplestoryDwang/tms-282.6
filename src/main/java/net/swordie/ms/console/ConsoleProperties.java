package net.swordie.ms.console;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ConsoleProperties {
    public static final Path CONFIG_FILE = Path.of("config", "console.properties");

    private ConsoleProperties() {
    }

    public static Properties load() {
        Properties p = new Properties();
        if (!Files.isRegularFile(CONFIG_FILE)) {
            return p;
        }
        try (FileInputStream in = new FileInputStream(CONFIG_FILE.toFile())) {
            p.load(in);
        } catch (IOException ignored) {
        }
        return p;
    }

    public static void save(Properties p) throws IOException {
        Path parent = CONFIG_FILE.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE.toFile())) {
            p.store(out, "TMS282.3 console settings");
        }
    }

    public static boolean parseBool(String raw, boolean defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String v = raw.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v) || "on".equals(v);
    }
}
