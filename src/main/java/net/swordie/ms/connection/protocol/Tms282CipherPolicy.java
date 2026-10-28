package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.crypto.Tms282ServerCipher;

/**
 * TMS 282.3 (channel-enter-2823.md 6): the client picks the server-to-client body cipher by socket. Login
 * socket: AES stream ({@link Tms282ServerCipher#LOGIN}); channel socket: every byte plus the send IV's byte 0
 * from the first frame after the 18-byte channel hello ({@link Tms282ServerCipher#CHANNEL}, the transform of
 * 265 encInit). Client-to-server frames stay AES on both sockets (PacketDecoder, unchanged).
 */
public final class Tms282CipherPolicy implements CipherPolicy {
    public static final Tms282CipherPolicy INSTANCE = new Tms282CipherPolicy();

    private Tms282CipherPolicy() {
    }

    @Override
    public SessionCipher forSession(SessionKind kind) {
        Tms282ServerCipher cipher = Tms282ServerCipher.forSocket(kind == SessionKind.CHANNEL);
        return (crypto, client, iv, data, auxiliary) -> {
            byte[] encrypted = cipher.encrypt(crypto, data, iv, auxiliary);
            System.arraycopy(encrypted, 0, data, 0, data.length);
        };
    }
}
