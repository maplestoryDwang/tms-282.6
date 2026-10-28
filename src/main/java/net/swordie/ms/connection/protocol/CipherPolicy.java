package net.swordie.ms.connection.protocol;

/**
 * Chooses the server-to-client body cipher of a socket (strategy factory): the profile decides per
 * {@link SessionKind}, PacketEncoder applies the returned {@link SessionCipher} to every frame.
 */
public interface CipherPolicy {
    SessionCipher forSession(SessionKind kind);
}
