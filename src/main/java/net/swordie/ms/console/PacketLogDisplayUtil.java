package net.swordie.ms.console;

/** 封包日誌行顯示截斷（簡單日誌切換時快速重算，避免整表重建卡頓）。 */
public final class PacketLogDisplayUtil {
    private PacketLogDisplayUtil() {
    }

    /**
     * 對已有行應用簡單日誌截斷：保留頭部到 長度=N，十六進制體最多 {@link PacketLogFilter#SIMPLE_LOG_HEX_MAX_CHARS} 字符。
     */
    public static String applySimpleLogToLine(String line, boolean simple) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        boolean hasNewline = line.endsWith("\n");
        String body = hasNewline ? line.substring(0, line.length() - 1) : line;
        if (!simple) {
            return line;
        }
        int lenIdx = body.indexOf("長度=");
        if (lenIdx < 0) {
            lenIdx = body.indexOf("len=");
        }
        if (lenIdx < 0) {
            return truncateRaw(body, PacketLogFilter.SIMPLE_LOG_HEX_MAX_CHARS) + (hasNewline ? "\n" : "");
        }
        int hexStart = body.indexOf(' ', lenIdx);
        while (hexStart >= 0 && hexStart < body.length() && body.charAt(hexStart) == ' ') {
            int next = hexStart + 1;
            if (next < body.length() && body.charAt(next) == '[') {
                int close = body.indexOf(']', next);
                if (close > next) {
                    hexStart = body.indexOf(' ', close);
                    continue;
                }
            }
            break;
        }
        if (hexStart < 0 || hexStart >= body.length()) {
            return line;
        }
        String head = body.substring(0, hexStart + 1);
        String hexPart = body.substring(hexStart + 1).trim();
        if (hexPart.startsWith("...(省略")) {
            return line;
        }
        String truncated = truncateHex(hexPart, PacketLogFilter.SIMPLE_LOG_HEX_MAX_CHARS);
        String result = head + truncated;
        if (hasNewline) {
            result += "\n";
        }
        return result;
    }

    private static String truncateHex(String hexPart, int maxChars) {
        if (hexPart.length() <= maxChars) {
            return hexPart;
        }
        return hexPart.substring(0, maxChars) + " ...(省略)";
    }

    private static String truncateRaw(String body, int maxChars) {
        if (body.length() <= maxChars) {
            return body;
        }
        return body.substring(0, maxChars) + "...";
    }
}
