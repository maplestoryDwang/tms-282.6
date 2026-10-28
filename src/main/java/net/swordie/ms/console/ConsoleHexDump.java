package net.swordie.ms.console;

/**
 * 16 bytes per line hex dump (offset、hex、ASCII)，客戶端回報與 282 封包 Tab 共用。
 * 最多 64 KB 的封包也要在背景執行緒內快速組好，所以不用 String.format。
 */
final class ConsoleHexDump {
    private static final char[] DIGITS = "0123456789ABCDEF".toCharArray();

    private ConsoleHexDump() {
    }

    static void append(StringBuilder text, byte[] data) {
        text.ensureCapacity(text.length() + (data.length / 16 + 1) * 76);
        for (int offset = 0; offset < data.length; offset += 16) {
            appendHex(text, offset, 4);
            text.append("  ");
            for (int i = 0; i < 16; i++) {
                if (offset + i < data.length) {
                    appendHex(text, data[offset + i] & 0xFF, 2);
                    text.append(' ');
                } else {
                    text.append("   ");
                }
            }
            text.append(' ');
            for (int i = 0; i < 16 && offset + i < data.length; i++) {
                int b = data[offset + i] & 0xFF;
                text.append(b >= 0x20 && b < 0x7F ? (char) b : '.');
            }
            text.append('\n');
        }
    }

    private static void appendHex(StringBuilder text, int value, int digits) {
        for (int shift = (digits - 1) * 4; shift >= 0; shift -= 4) {
            text.append(DIGITS[(value >>> shift) & 0xF]);
        }
    }
}
