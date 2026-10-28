package net.swordie.ms.connection.protocol;

import net.swordie.ms.connection.OutPacket;

import java.util.List;

/**
 * What a new socket receives before the client says anything.
 * <p>
 * The caller writes {@link #hello} in the clear (before the channel's client/crypto attributes exist, so the
 * encoder passes it through untouched), then installs the session attributes, then writes every
 * {@link #afterHello} packet encrypted, then pings when {@link #pingOnConnect} says so.
 */
public interface HandshakeStrategy {
    /**
     * Plaintext hello, length-prefixed.
     *
     * @param recvIv the IV the server decrypts with (the client's send IV)
     * @param sendIv the IV the server encrypts with
     */
    OutPacket hello(SessionKind kind, byte[] recvIv, byte[] sendIv);

    /** Encrypted packets sent unprompted right after the hello, in order. */
    List<OutPacket> afterHello(SessionKind kind);

    /** Whether the server sends its first alive request immediately. */
    boolean pingOnConnect(SessionKind kind);

    /** Called last, once the socket is set up (hello, attributes, unprompted packets). Default: nothing. */
    default void sessionStarted(net.swordie.ms.client.Client client, SessionKind kind) {
    }

    /** Receiver of {@link #sessionStarted} (profiles that keep per-socket state). */
    @FunctionalInterface
    interface SessionStartListener {
        void sessionStarted(net.swordie.ms.client.Client client, SessionKind kind);
    }
}
