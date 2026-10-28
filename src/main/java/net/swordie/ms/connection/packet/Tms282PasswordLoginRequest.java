package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * CP0074 account/password login (282.4). The sender is CLogin 3C9B800 (282.4 RVA), whose packet body is built
 * inside the Themida VM after the jump at 3C9B967; the field order below was recovered by emulating that VM path
 * from a 282.4 process dump (research/tms2823/packet-audit/cp0074-layout-1005):
 * <pre>
 * u16 opcode 0x74
 * raw[16]   client machine block; bytes 0..5 are the network adapter MAC (the old "layout byte 8" check at
 *           offset 2 was really the host's MAC first octet 08, so every other computer was refused)
 * u8        flag (offset 18)
 * u32       value (offset 19)
 * u8        launch type, CWvsApp+0x88 (offset 23; 3 = BeanFun command line)
 * u8        flag (offset 24)
 * str       account (offset 25)
 * str       password
 * </pre>
 * This parser only checks the framing; authentication (BCrypt, LoginBackoff, bans) stays in the handler.
 */
public record Tms282PasswordLoginRequest(int flag18, int value19, int launchType, int flag24, String name, String password) {
    public static final int OPCODE = 0x0074;
    public static final int MACHINE_BLOCK = 16;
    public static final int HEADER_LENGTH = 2 + MACHINE_BLOCK + 1 + 4 + 1 + 1;
    public static final int MAX_NAME = 32;
    public static final int MAX_PASSWORD = 72;

    public static Tms282PasswordLoginRequest parse(byte[] packet) {
        if (packet == null || packet.length < HEADER_LENGTH + 2 + 1 + 2 + 1) {
            throw new IllegalArgumentException("Truncated CP0074");
        }
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
            throw new IllegalArgumentException("Expected CP0074");
        }
        in.position(2 + MACHINE_BLOCK);
        int flag18 = Byte.toUnsignedInt(in.get());
        int value19 = in.getInt();
        int launchType = Byte.toUnsignedInt(in.get());
        int flag24 = Byte.toUnsignedInt(in.get());
        String name = string(in, MAX_NAME, "account");
        String password = string(in, MAX_PASSWORD, "password");
        if (in.hasRemaining()) {
            throw new IllegalArgumentException("Trailing bytes after CP0074 password");
        }
        if (!name.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Invalid CP0074 account characters");
        }
        return new Tms282PasswordLoginRequest(flag18, value19, launchType, flag24, name, password);
    }

    private static String string(ByteBuffer in, int max, String what) {
        if (in.remaining() < 2) {
            throw new IllegalArgumentException("Missing CP0074 " + what + " length");
        }
        int length = Short.toUnsignedInt(in.getShort());
        if (length < 1 || length > max || length > in.remaining()) {
            throw new IllegalArgumentException("Invalid CP0074 " + what + " length");
        }
        byte[] data = new byte[length];
        in.get(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Diagnostic line for a refused CP0074: total length, the MAC first octet (the byte the old check compared with
     * 8) and the non-secret header flags. Never the account, the password, their lengths or the rest of the MAC.
     */
    public static String rejectionSummary(byte[] packet) {
        if (packet == null) {
            return "CP0074 refused: no body";
        }
        StringBuilder out = new StringBuilder("CP0074 refused: length=").append(packet.length);
        if (packet.length > 2) {
            out.append(String.format(" mac0=0x%02X", packet[2] & 0xFF));
        }
        if (packet.length >= HEADER_LENGTH) {
            out.append(String.format(" flag18=0x%02X launchType=%d flag24=0x%02X",
                    packet[18] & 0xFF, packet[23] & 0xFF, packet[24] & 0xFF));
        }
        return out.append(" (credentials not logged)").toString();
    }

    @Override public String toString() {
        return "CP0074[launchType=" + launchType + ", account=<redacted>, password=<redacted>]";
    }
}
