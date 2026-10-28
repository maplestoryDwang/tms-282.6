package net.swordie.ms.connection.crypto;

import java.util.Objects;

/**
 * Body cipher of server-to-client frames in TMS 282.3, chosen per socket. The frame header (MapleCrypto
 * getFrameHeader) and the IV update after every frame (MapleCrypto.getNewIv, client 15A5B60) are the same for
 * both; only the body transform differs.
 * <p>
 * Client evidence (MapleStory.exe 282.3, RVAs from image base 0x140000000): both receive paths,
 * CClientSocket 346D960 (346D974..346D98C) and the network thread 3481410 (3481821..34818BD), pass
 * {@code mode = [socket+0x48] != 0 ? 1 : 2} to the CInPacket decrypt F8E0F0. Mode 1 calls 15A2D70 (the AES
 * stream used by MapleCrypto.crypt); mode 2 calls F94BB0, which subtracts the low byte of the receive IV
 * ([socket+0xDC] = IV2 of the hello, the server send IV) from every body byte (psubb loop F94C40, byte loop
 * F94CB0). [socket+0x48] also selects the hello layout (346B6DE..346BC68): 0 on the channel socket, which is the
 * layout the channel hello uses. So every frame the server sends on a channel socket, from the first one, is
 * "byte + sendIV[0]", the same transform as 265 MapleCrypto.encInit(Util.toInt(iv)) (the low byte of toInt is
 * iv[0]). Client-to-server frames are not affected: the captured channel CP0070/CP007E decrypt with AES.
 */
public enum Tms282ServerCipher {
    /** Login socket: AES stream (client mode 1, 15A2D70). */
    LOGIN {
        @Override
        public byte[] encrypt(MapleCrypto aes, byte[] body, byte[] iv, boolean auxiliary) {
            Objects.requireNonNull(aes, "aes");
            return aes.crypt(checked(body, iv).clone(), iv, auxiliary);
        }
    },
    /** Channel socket: every byte plus the low byte of the send IV (client mode 2, F94BB0 subtracts it). */
    CHANNEL {
        @Override
        public byte[] encrypt(MapleCrypto aes, byte[] body, byte[] iv, boolean auxiliary) {
            byte[] out = checked(body, iv).clone();
            byte delta = iv[0];
            for (int i = 0; i < out.length; i++) {
                out[i] += delta;
            }
            return out;
        }
    };

    /** Encrypts a frame body (MAIN plus AUX trailer when {@code auxiliary}); the arguments are not modified. */
    public abstract byte[] encrypt(MapleCrypto aes, byte[] body, byte[] iv, boolean auxiliary);

    /** Client side of {@link #CHANNEL} (F94BB0 with its flag argument 1): every byte minus iv[0]. For checks. */
    public static byte[] decryptChannel(byte[] body, byte[] iv) {
        byte[] out = checked(body, iv).clone();
        byte delta = iv[0];
        for (int i = 0; i < out.length; i++) {
            out[i] -= delta;
        }
        return out;
    }

    public static Tms282ServerCipher forSocket(boolean channel) {
        return channel ? CHANNEL : LOGIN;
    }

    private static byte[] checked(byte[] body, byte[] iv) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(iv, "iv");
        if (iv.length != 4) {
            throw new IllegalArgumentException("IV must be 4 bytes");
        }
        return body;
    }
}
