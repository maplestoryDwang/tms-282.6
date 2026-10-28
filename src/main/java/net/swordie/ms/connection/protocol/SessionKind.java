package net.swordie.ms.connection.protocol;

/** Which acceptor a socket belongs to; handshakes differ between the two. */
public enum SessionKind {
    LOGIN,
    CHANNEL
}
