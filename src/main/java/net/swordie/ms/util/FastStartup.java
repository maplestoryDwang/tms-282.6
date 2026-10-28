package net.swordie.ms.util;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.console.ConsoleProperties;

import java.io.File;
import java.util.Properties;

public final class FastStartup {
    private static final String KEY_ENABLED = "server.faststart";

    private static volatile boolean enabled;

    static {
        load();
    }

    private FastStartup() {
    }

    public static void load() {
        Properties p = ConsoleProperties.load();
        enabled = ConsoleProperties.parseBool(p.getProperty(KEY_ENABLED), true);
        System.setProperty(KEY_ENABLED, Boolean.toString(enabled));
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean on) {
        boolean changed = enabled != on;
        enabled = on;
        System.setProperty(KEY_ENABLED, Boolean.toString(on));
        try {
            Properties p = ConsoleProperties.load();
            p.setProperty(KEY_ENABLED, Boolean.toString(on));
            ConsoleProperties.save(p);
        } catch (Exception ignored) {
        }
        if (changed) {
            System.out.println("[Startup] 快速啓動: " + (on ? "已開啓（網絡重疊）" : "已關閉（串行加載）"));
        }
    }

    public static File datDir() {
        return new File(ServerConstants.DAT_DIR);
    }

    /** dat265 就緒：equips 目錄存在即視爲已有 per-file 緩存。 */
    public static boolean isDatCacheReady() {
        return new File(ServerConstants.DAT_DIR, "equips").isDirectory();
    }

    public static void logCacheStatus() {
        File dat = datDir();
        if (!isEnabled()) {
            System.out.println("[啓動] 快速啓動已關閉：按串行流程加載數據。");
            if (!isDatCacheReady()) {
                System.out.printf("[啓動] dat 緩存不存在：%s；首次啓動時會從 WZ 生成 dat。%n", dat.getAbsolutePath());
            }
            return;
        }
        if (isDatCacheReady()) {
            System.out.println("[啓動] 快速啓動已開啓：串行加載數據並重疊綁定網絡端口。");
            return;
        }
        System.out.printf("[啓動] 快速啓動已開啓，dat 緩存不存在：%s；首次啓動時會生成 dat。%n", dat.getAbsolutePath());
    }
}
