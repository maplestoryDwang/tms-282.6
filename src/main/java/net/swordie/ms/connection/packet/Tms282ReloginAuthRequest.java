package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** CP0073, native sender 3C9BFD0 (3C9C12F..3C9C2ED), for a server-issued relogin cookie.
 * Layout: i32 mode, u8 environment, u8 retry, i32 retryCount, str cookie,
 * raw SYSTEMTIME[16], i32 tick, u8 launcherMode, i32 local IPv4. Includes the two-byte opcode.
 * Modes 3/4/9/10 are exactly 3CBF140's cookie branch, read from context+3AF0 by 5E4BF40.
 * This parser does not authenticate: the owner, peer, expiry and single-use checks belong to the ticket store.
 */
public record Tms282ReloginAuthRequest(int mode, int environment, int retry, int retryCount,
                                     String cookie, byte[] systemTime, int tick, int launcherMode, int localAddress) {
    public static final int OPCODE = 0x0073;
    /** 24 random bytes encoded base64url without padding; separate from the CP0079/PIC handoff credential. */
    public static final int COOKIE_LENGTH = 32;
    public static final int PACKET_LENGTH = 39 + COOKIE_LENGTH;

    public Tms282ReloginAuthRequest {
        systemTime = systemTime.clone();
    }

    @Override public byte[] systemTime() { return systemTime.clone(); }

    public static Tms282ReloginAuthRequest parse(byte[] packet) {
        if (packet == null || packet.length != PACKET_LENGTH)
            throw new IllegalArgumentException("Invalid CP0073 length");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (Short.toUnsignedInt(in.getShort()) != OPCODE)
            throw new IllegalArgumentException("Expected CP0073");
        int mode = in.getInt();
        if (mode != 3 && mode != 4 && mode != 9 && mode != 10)
            throw new IllegalArgumentException("Unsupported CP0073 authentication mode");
        int environment = Byte.toUnsignedInt(in.get());
        int retry = Byte.toUnsignedInt(in.get());
        int retryCount = in.getInt();
        if (retry > 1 || retryCount < 0 || retryCount > 10)
            throw new IllegalArgumentException("Invalid CP0073 retry state");
        if (Short.toUnsignedInt(in.getShort()) != COOKIE_LENGTH)
            throw new IllegalArgumentException("Invalid CP0073 cookie length");
        byte[] bytes = new byte[COOKIE_LENGTH];
        in.get(bytes);
        String cookie = new String(bytes, StandardCharsets.US_ASCII);
        if (!cookie.matches("[A-Za-z0-9_-]{32}"))
            throw new IllegalArgumentException("Invalid CP0073 cookie format");
        byte[] systemTime = new byte[16];
        in.get(systemTime);
        int tick = in.getInt();
        int launcherMode = Byte.toUnsignedInt(in.get());
        int localAddress = in.getInt();
        return new Tms282ReloginAuthRequest(mode, environment, retry, retryCount,
                cookie, systemTime, tick, launcherMode, localAddress);
    }

    @Override public String toString() {
        return "CP0073[mode=" + mode + ", retry=" + retry + ", retryCount=" + retryCount + ", cookie=<redacted>]";
    }
}
