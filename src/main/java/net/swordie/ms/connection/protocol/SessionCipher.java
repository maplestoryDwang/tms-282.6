package net.swordie.ms.connection.protocol;

import net.swordie.ms.client.Client;
import net.swordie.ms.connection.crypto.MapleCrypto;

/** Body transform of one socket's outgoing frames; the frame header and IV update stay in PacketEncoder. */
@FunctionalInterface
public interface SessionCipher {
    /**
     * Encrypts {@code data} in place.
     *
     * @param iv        the current send IV (not advanced here)
     * @param auxiliary whether the frame carries an auxiliary cursor
     */
    void encrypt(MapleCrypto crypto, Client client, byte[] iv, byte[] data, boolean auxiliary);
}
