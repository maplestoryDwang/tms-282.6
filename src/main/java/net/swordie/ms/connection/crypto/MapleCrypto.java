package net.swordie.ms.connection.crypto;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static net.swordie.ms.ServerConstants.version;

public final class MapleCrypto {

    private Cipher cipher;
    private final int largePacketFirstBlock;
    private static short gVersion, sVersion, rVersion;
    public static final int[] SHUFFLE_BYTES = new int[]{0xEC, 0x3F, 0x77, 0xA4, 0x45, 0xD0, 0x71, 0xBF, 0xB7, 0x98, 0x20, 0xFC,
            0x4B, 0xE9, 0xB3, 0xE1, 0x5C, 0x22, 0xF7, 0x0C, 0x44, 0x1B, 0x81, 0xBD, 0x63, 0x8D, 0xD4, 0xC3,
            0xF2, 0x10, 0x19, 0xE0, 0xFB, 0xA1, 0x6E, 0x66, 0xEA, 0xAE, 0xD6, 0xCE, 0x06, 0x18, 0x4E, 0xEB,
            0x78, 0x95, 0xDB, 0xBA, 0xB6, 0x42, 0x7A, 0x2A, 0x83, 0x0B, 0x54, 0x67, 0x6D, 0xE8, 0x65, 0xE7,
            0x2F, 0x07, 0xF3, 0xAA, 0x27, 0x7B, 0x85, 0xB0, 0x26, 0xFD, 0x8B, 0xA9, 0xFA, 0xBE, 0xA8, 0xD7,
            0xCB, 0xCC, 0x92, 0xDA, 0xF9, 0x93, 0x60, 0x2D, 0xDD, 0xD2, 0xA2, 0x9B, 0x39, 0x5F, 0x82, 0x21,
            0x4C, 0x69, 0xF8, 0x31, 0x87, 0xEE, 0x8E, 0xAD, 0x8C, 0x6A, 0xBC, 0xB5, 0x6B, 0x59, 0x13, 0xF1,
            0x04, 0x00, 0xF6, 0x5A, 0x35, 0x79, 0x48, 0x8F, 0x15, 0xCD, 0x97, 0x57, 0x12, 0x3E, 0x37, 0xFF,
            0x9D, 0x4F, 0x51, 0xF5, 0xA3, 0x70, 0xBB, 0x14, 0x75, 0xC2, 0xB8, 0x72, 0xC0, 0xED, 0x7D, 0x68,
            0xC9, 0x2E, 0x0D, 0x62, 0x46, 0x17, 0x11, 0x4D, 0x6C, 0xC4, 0x7E, 0x53, 0xC1, 0x25, 0xC7, 0x9A,
            0x1C, 0x88, 0x58, 0x2C, 0x89, 0xDC, 0x02, 0x64, 0x40, 0x01, 0x5D, 0x38, 0xA5, 0xE2, 0xAF, 0x55,
            0xD5, 0xEF, 0x1A, 0x7C, 0xA7, 0x5B, 0xA6, 0x6F, 0x86, 0x9F, 0x73, 0xE6, 0x0A, 0xDE, 0x2B, 0x99,
            0x4A, 0x47, 0x9C, 0xDF, 0x09, 0x76, 0x9E, 0x30, 0x0E, 0xE4, 0xB2, 0x94, 0xA0, 0x3B, 0x34, 0x1D,
            0x28, 0x0F, 0x36, 0xE3, 0x23, 0xB4, 0x03, 0xD8, 0x90, 0xC8, 0x3C, 0xFE, 0x5E, 0x32, 0x24, 0x50,
            0x1F, 0x3A, 0x43, 0x8A, 0x96, 0x41, 0x74, 0xAC, 0x52, 0x33, 0xF0, 0xD9, 0x29, 0x80, 0xB1, 0x16,
            0xD3, 0xAB, 0x91, 0xB9, 0x84, 0x7F, 0x61, 0x1E, 0xCF, 0xC5, 0xD1, 0x56, 0x3D, 0xCA, 0xF4, 0x05,
            0xC6, 0xE5, 0x08, 0x49};

    public MapleCrypto() {
        this(version);
    }

    public MapleCrypto(int version) {
        largePacketFirstBlock = version == 282 ? 0x5AC : 0x5B0;
        try {
            cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(AES.getKey(version), "AES"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if ((len & 1) != 0) throw new IllegalArgumentException("Odd hex length");
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Bad hex");
            out[i / 2] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    public static void initialize(short v) {
        gVersion = v;
        sVersion = (short) ((((0xFFFF - v) >>> 8) & 0xFF) | (((0xFFFF - v) << 8) & 0xFF00));
        rVersion = (short) (((v >>> 8) & 0xFF) | ((v << 8) & 0xFF00));
    }

    public byte[] encInit(int seqSnd, byte[] delta, boolean decrypt) {
        for (int i = 0; i < delta.length; i++) {
            if (decrypt) {
                delta[i] -= (byte) seqSnd;
            } else {
                delta[i] += (byte) seqSnd;
            }
        }
        return delta;
    }

    public static byte[] multiplyBytes(byte[] iv, int count, int mul) {
        byte[] ret = new byte[count * mul];
        for (int x = 0; x < ret.length; x++) {
            ret[x] = iv[x % count];
        }
        return ret;
    }

    public byte[] crypt(byte[] data, byte[] curIv) {
        return crypt(data, curIv, false);
    }

    public byte[] crypt(byte[] data, byte[] curIv, boolean paddedFrame) {
        int remaining = data.length;
        int llength = data.length > 0xFF00 ? largePacketFirstBlock : 0x5B0;
        if (paddedFrame) llength = Math.min(llength, 0x5AC);
        int start = 0;
        while (remaining > 0) {
            byte[] myIv = multiplyBytes(curIv, 4, 4);
            if (remaining < llength) {
                llength = remaining;
            }
            for (int e = start; e < (start + llength); e++) {
                if ((e - start) % myIv.length == 0) {
                    try {
                        byte[] newIv = cipher.doFinal(myIv);
                        System.arraycopy(newIv, 0, myIv, 0, myIv.length);
                    } catch (Exception ex) {
                        ex.printStackTrace(); // may eventually want to remove this
                    }
                }
                data[e] ^= myIv[(e - start) % myIv.length];
            }
            start += llength;
            remaining -= llength;
            llength = 0x5B4;
        }
        return data;
    }

    public static byte[] getHeader(int delta, byte[] gamma) {
        int a = (gamma[3]) & 0xFF;
        a |= (gamma[2] << 8) & 0xFF00;
        a ^= sVersion;
        int b = ((delta << 8) & 0xFF00) | (delta >>> 8);
        int c = a ^ b;
        byte[] ret = new byte[4];
        ret[0] = (byte) ((a >>> 8) & 0xFF);
        ret[1] = (byte) (a & 0xFF);
        ret[2] = (byte) ((c >>> 8) & 0xFF);
        ret[3] = (byte) (c & 0xFF);
        return ret;
    }

    public static byte[] getFrameHeader(int length, byte[] iv) {
        return getFrameHeader(length, iv, false);
    }

    public static byte[] getFrameHeader(int length, byte[] iv, boolean auxiliary) {
        if (length < 2 || length > 0xFFFFFF) throw new IllegalArgumentException("Invalid packet length: " + length);
        if (auxiliary && length < 6) throw new IllegalArgumentException("Auxiliary frame requires opcode and length trailer");
        byte[] header = getHeader(auxiliary ? 0xFF00 : Math.min(length, 0xFF00), iv);
        if (length < 0xFF00 && !auxiliary) return header;
        header = java.util.Arrays.copyOf(header, 8);
        int sequence = (header[0] & 0xFF) | ((header[1] & 0xFF) << 8);
        java.nio.ByteBuffer.wrap(header, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .putInt(sequence ^ length ^ (auxiliary ? Integer.MIN_VALUE : 0));
        return header;
    }

    public static byte[] getHeader(int delta, byte[] gamma, short v) {
        int a = (gamma[3]) & 0xFF;
        a |= (gamma[2] << 8) & 0xFF00;
        a ^= sVersion;
        int b = ((delta << 8) & 0xFF00) | (delta >>> 8);
        int c = a ^ b;
        byte[] ret = new byte[4];
        ret[0] = (byte) ((a >>> 8) & 0xFF);
        ret[1] = (byte) (a & 0xFF);
        ret[2] = (byte) ((c >>> 8) & 0xFF);
        ret[3] = (byte) (c & 0xFF);
        return ret;
    }

    public static int getLength(int delta) {
        int a = ((delta >>> 16) ^ (delta & 0xFFFF));
        a = ((a << 8) & 0xFF00) | ((a >>> 8) & 0xFF);
        return a;
    }

    public static boolean checkPacket(byte[] delta, byte[] gamma) {
        return ((((delta[0] ^ gamma[2]) & 0xFF) == ((rVersion >>> 8) & 0xFF))
                && (((delta[1] ^ gamma[3]) & 0xFF) == (rVersion & 0xFF)));
    }

    public static boolean checkPacket(int delta, byte[] gamma) {
        byte[] a = new byte[2];
        a[0] = (byte) ((delta >>> 24) & 0xFF);
        a[1] = (byte) ((delta >>> 16) & 0xFF);
        return checkPacket(a, gamma);
    }

    public static byte[] getNewIv(byte[] delta) {
        byte[] ret = delta;
        int[] nIv = {0xF2, 0x53, 0x50, 0xC6};
        for (int i = 0; i < 4; i++) {
            int a = (ret[i] & 0xFF);
            int b = SHUFFLE_BYTES[a];
            nIv[0] += SHUFFLE_BYTES[nIv[1]] - a;
            nIv[1] -= nIv[2] ^ b;
            nIv[2] ^= SHUFFLE_BYTES[nIv[3]] + a;
            nIv[3] -= nIv[0] - b;
            int c = nIv[0] & 0xFF;
            c |= (nIv[1] << 8) & 0xFF00;
            c |= (nIv[2] << 16) & 0xFF0000;
            c |= (nIv[3] << 24) & 0xFF000000;
            int d = (c << 3) | (c >>> 0x1D);
            nIv[0] = (d & 0xFF);
            nIv[1] = ((d >>> 8) & 0xFF);
            nIv[2] = ((d >>> 16) & 0xFF);
            nIv[3] = ((d >>> 24) & 0xFF);
        }
        for (int i = 0; i < 4; i++) {
            ret[i] = (byte) nIv[i];
        }
        return ret;
    }
}
