package net.swordie.ms.connection.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class TripleDESCipher {

    private byte[] rawKey = new byte[24];
    private Key key;
    private static Map<Short, Short> encryptedHeaderToNormalHeaders = new HashMap<>();

    public TripleDESCipher(byte[] key) {
        System.arraycopy(key, 0, this.rawKey, 0, key.length);
        this.key = new SecretKeySpec(key, "DESede");
    }

    public byte[] encrypt(byte[] data) {
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("DESede");
            cipher.init(Cipher.ENCRYPT_MODE, this.key);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher;
        cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, this.key);
        return cipher.doFinal(data);
    }

    public byte[] getRawKey() {
        return rawKey;
    }

    public void setRawKey(byte[] key) {
        System.arraycopy(key, 0, this.rawKey, 0, key.length);
        this.key = new SecretKeySpec(key, "DESede");
    }

    public void setKey(Key key) {
        this.key = key;
    }

    public Key getKey() {
        return key;
    }

    public static boolean isNumber(byte[] arr) {
        for (byte b : arr) {
            if (b < 48 || b > 57) {
                return false;
            }
        }
        return true;
    }

}
