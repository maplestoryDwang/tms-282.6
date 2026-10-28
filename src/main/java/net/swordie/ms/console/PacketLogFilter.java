package net.swordie.ms.console;

import java.util.Properties;

public final class PacketLogFilter {
    public static final int SIMPLE_LOG_HEX_MAX_CHARS = 100;

    private static final String KEY_GUI_SIMPLE_LOG = "packet.gui.simple.log";
    private static final String KEY_GUI_IGNORE_HIGH_FREQUENCY = "packet.gui.ignore.high.frequency.send";

    private static volatile boolean simpleLogEnabled;
    private static volatile boolean ignoreHighFrequencyEnabled = true;

    private PacketLogFilter() {
    }

    public static void load() {
        Properties p = ConsoleProperties.load();
        simpleLogEnabled = ConsoleProperties.parseBool(p.getProperty(KEY_GUI_SIMPLE_LOG), false);
        ignoreHighFrequencyEnabled = ConsoleProperties.parseBool(p.getProperty(KEY_GUI_IGNORE_HIGH_FREQUENCY), true);
    }

    public static boolean isSimpleLogEnabled() {
        return simpleLogEnabled;
    }

    public static void setSimpleLogEnabled(boolean enabled) {
        simpleLogEnabled = enabled;
        saveBoolean(KEY_GUI_SIMPLE_LOG, enabled);
    }

    public static boolean isIgnoreHighFrequencySendEnabled() {
        return ignoreHighFrequencyEnabled;
    }

    public static void setIgnoreHighFrequencySendEnabled(boolean enabled) {
        ignoreHighFrequencyEnabled = enabled;
        saveBoolean(KEY_GUI_IGNORE_HIGH_FREQUENCY, enabled);
    }

    public static boolean shouldDropHighFrequency(String opcode) {
        if (!ignoreHighFrequencyEnabled || opcode == null) {
            return false;
        }
        return PacketLogLineType.isHighFrequencyOpcode(opcode);
    }

    private static void saveBoolean(String key, boolean enabled) {
        try {
            Properties p = ConsoleProperties.load();
            p.setProperty(key, Boolean.toString(enabled));
            ConsoleProperties.save(p);
        } catch (Exception ignored) {
        }
    }
}
