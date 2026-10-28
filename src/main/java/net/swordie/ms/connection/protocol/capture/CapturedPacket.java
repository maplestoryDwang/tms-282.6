package net.swordie.ms.connection.protocol.capture;

import java.util.Objects;

/**
 * One packet event on a captured socket, as handed to every {@link CaptureObserver}. Immutable by convention:
 * the arrays are private copies made by {@link PacketCapture} and are never written to afterwards.
 * <p>
 * {@code data} is already what may be stored: for a {@code redacted} packet the opcode is kept and every other
 * byte is zero (same length), and {@code auxiliary} is empty.
 *
 * @param kind      what happened to the packet
 * @param index     per-kind sequence number of the socket (the NNN of {@code recv-NNN-OOOO.bin})
 * @param data      plaintext packet, opcode first (the whole blob for {@link Kind#HELLO})
 * @param auxiliary auxiliary cursor bytes, empty when none
 * @param redacted  whether the body was zeroed because it carries (or may carry) credentials
 */
public record CapturedPacket(Kind kind, int index, byte[] data, byte[] auxiliary, boolean redacted) {
    private static final byte[] NONE = new byte[0];

    /** RECV = client to server, SEND = server to client (reached the encoder), DROPPED = refused by the OutboundGuard. */
    public enum Kind {
        RECV,
        SEND,
        DROPPED,
        /** The plaintext handshake blob written before the socket has a client (not an opcode packet). */
        HELLO
    }

    public CapturedPacket {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(data, "data");
        auxiliary = auxiliary == null ? NONE : auxiliary;
        if (index < 0) {
            throw new IllegalArgumentException("Negative index");
        }
    }

    /** Little-endian u16 opcode, 0 for anything shorter than two bytes. */
    public int opcode() {
        return data.length < 2 ? 0 : (data[0] & 0xFF) | (data[1] & 0xFF) << 8;
    }

    public boolean hasAuxiliary() {
        return auxiliary.length > 0;
    }

    /** Copy of {@code plain} with every byte after the opcode zeroed (length kept). */
    static byte[] redact(byte[] plain) {
        byte[] redacted = new byte[plain.length];
        System.arraycopy(plain, 0, redacted, 0, Math.min(2, plain.length));
        return redacted;
    }
}
