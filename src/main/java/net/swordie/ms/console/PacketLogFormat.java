package net.swordie.ms.console;

/**
 * 封包日誌行格式化：[時間][IP][方向:OPCODE][opcodeVal]長度=N [角色名] 十六進制包體
 */
public final class PacketLogFormat {
    public static final String DIR_RECV = "客戶端";
    public static final String DIR_SEND = "服務端";
    public static final int GUI_HEX_MAX_BYTES = 64;
    public static final int SIMPLE_LOG_HEX_MAX_BYTES = 18;

    private static final char[] HEX_LOWER = "0123456789abcdef".toCharArray();

    private PacketLogFormat() {
    }

    public static String formatRecv(String ip, String opcode, int opcodeVal, byte[] data, String charName) {
        return formatLine(DIR_RECV, ip, opcode, opcodeVal, data, charName, guiHexMaxBytes());
    }

    public static String formatSend(String ip, String opcode, int opcodeVal, byte[] data, String charName) {
        return formatLine(DIR_SEND, ip, opcode, opcodeVal, data, charName, guiHexMaxBytes());
    }

    public static String formatUnknownRecv(String ip, short opcode, byte[] data, String who) {
        return formatLine("未知客戶端", ip, "未處理", opcode, data, who, guiHexMaxBytes());
    }

    public static String formatUnknownSend(String ip, int opcode, byte[] data, String who) {
        return formatLine("未知服務端", ip, "未處理", opcode, data, who, guiHexMaxBytes());
    }

    public static int guiHexMaxBytes() {
        return PacketLogFilter.isSimpleLogEnabled() ? SIMPLE_LOG_HEX_MAX_BYTES : GUI_HEX_MAX_BYTES;
    }

    private static String formatLine(String direction, String ip, String opcode, int opcodeVal,
            byte[] data, String charName, int hexMaxBytes) {
        int len = data != null ? data.length : 0;
        int showBytes = len > 0 && hexMaxBytes > 0 ? Math.min(len, hexMaxBytes) : len;
        StringBuilder sb = new StringBuilder(showBytes * 3 + 96);
        sb.append('[').append(GuiLogTimestamp.formatNow()).append(']');
        if (ip != null && !ip.isEmpty()) {
            sb.append('[').append(ip).append(']');
        }
        sb.append('[').append(direction).append(':').append(opcode).append(']');
        sb.append('[').append(opcodeVal).append("]長度=").append(len);
        if (charName != null && !charName.isEmpty()) {
            sb.append(" [").append(charName).append(']');
        }
        if (data != null && len > 0) {
            sb.append(' ');
            appendHex(sb, data, 0, showBytes);
            if (showBytes < len) {
                sb.append(" ...(省略").append(len - showBytes).append("字節)");
            }
        }
        sb.append('\n');
        return sb.toString();
    }

    private static void appendHex(StringBuilder sb, byte[] data, int offset, int count) {
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            int v = data[offset + i] & 0xFF;
            sb.append(HEX_LOWER[v >>> 4]);
            sb.append(HEX_LOWER[v & 0x0F]);
        }
    }
}
