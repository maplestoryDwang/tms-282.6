package net.swordie.ms.console;

public final class PacketLogBridge {
    private static volatile PacketLogAppender appender;

    private PacketLogBridge() {
    }

    public static void setAppender(PacketLogAppender packetAppender) {
        appender = packetAppender;
    }

    public static void log(String line) {
        PacketLogAppender current = appender;
        if (current != null && line != null && !line.isBlank()) {
            String normalized = ConsoleLogText.toChinesePacketLine(normalizeLine(line));
            if (PacketLogLineType.isPacketLine(normalized) && PacketLogLineType.hasGuiTimestamp(normalized)) {
                current.enqueue(normalized.endsWith("\n") ? normalized : normalized + "\n");
            }
        }
    }

    public static boolean acceptConsoleLine(String line) {
        if (!ConsoleDebugMode.isPacketDetailEnabled() || line == null) {
            return false;
        }
        String normalized = ConsoleLogText.toChinesePacketLine(normalizeLine(line));
        if (!PacketLogLineType.isPacketLine(normalized) || !PacketLogLineType.hasGuiTimestamp(normalized)) {
            return false;
        }
        PacketLogAppender current = appender;
        if (current != null) {
            current.enqueue(normalized.endsWith("\n") ? normalized : normalized + "\n");
        }
        return true;
    }

    private static String normalizeLine(String line) {
        String s = line.trim();
        if (s.startsWith("\r\n")) {
            s = s.substring(2);
        } else if (s.startsWith("\n")) {
            s = s.substring(1);
        }
        return s;
    }
}
