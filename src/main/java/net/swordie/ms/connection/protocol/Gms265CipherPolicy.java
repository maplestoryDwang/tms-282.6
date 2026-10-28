package net.swordie.ms.connection.protocol;

import net.swordie.ms.client.Client;
import net.swordie.ms.connection.crypto.MapleCrypto;
import net.swordie.ms.util.Util;

/**
 * Former PacketEncoder rule, unchanged and the same on both sockets: a client with a character gets encInit,
 * everyone else the frame cipher.
 */
public final class Gms265CipherPolicy implements CipherPolicy, SessionCipher {
    public static final Gms265CipherPolicy INSTANCE = new Gms265CipherPolicy();

    private Gms265CipherPolicy() {
    }

    @Override
    public SessionCipher forSession(SessionKind kind) {
        return this;
    }

    @Override
    public void encrypt(MapleCrypto crypto, Client client, byte[] iv, byte[] data, boolean auxiliary) {
        if (client.getChr() != null) {
            crypto.encInit(Util.toInt(iv), data, false);
        } else {
            crypto.crypt(data, iv, auxiliary);
        }
    }
}
