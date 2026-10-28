package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.protocol.codec.MovePathCodec;
import net.swordie.ms.life.movement.MovementInfo;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * Strict little-endian reader of one TMS 282.3 in-field client packet (whole plaintext packet, opcode included), as
 * the Tms282*Request parsers expect: a wrong opcode, a short packet or trailing bytes are
 * {@link IllegalArgumentException}s. Paths are handed to a {@link MovePathCodec} over the same bytes.
 */
final class Tms282FieldRequestReader {
    private final String name;
    private final byte[] packet;
    private final ByteBuffer in;

    private Tms282FieldRequestReader(String name, byte[] packet) {
        this.name = name;
        this.packet = packet;
        this.in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
    }

    /** Checks the opcode and positions the reader on the body. */
    static Tms282FieldRequestReader open(byte[] packet, int opcode, String name) {
        Objects.requireNonNull(packet, "packet");
        Tms282FieldRequestReader r = new Tms282FieldRequestReader(name, packet);
        if (packet.length < 2 || r.u16() != opcode) {
            throw new IllegalArgumentException(String.format("Wrong %s opcode (expected %04X)", name, opcode));
        }
        return r;
    }

    int u8() {
        return guard(() -> Byte.toUnsignedInt(in.get()));
    }

    int u16() {
        return guard(() -> Short.toUnsignedInt(in.getShort()));
    }

    short i16() {
        return guard(in::getShort);
    }

    int i32() {
        return guard(in::getInt);
    }

    long i64() {
        return guard(in::getLong);
    }

    /** Client EncodeStr: u16 byte length, bytes in {@code charset}. */
    String str(Charset charset) {
        int n = u16();
        byte[] b = bytes(n);
        return new String(b, charset);
    }

    byte[] bytes(int n) {
        if (n < 0 || n > in.remaining()) {
            throw new IllegalArgumentException("Truncated " + name + ": need " + n + " bytes, " + in.remaining() + " left");
        }
        byte[] b = new byte[n];
        in.get(b);
        return b;
    }

    /** Everything not read yet (consumed). */
    byte[] rest() {
        return bytes(in.remaining());
    }

    /** Current offset in the whole packet (opcode included). */
    int position() {
        return in.position();
    }

    /** Moves to {@code offset} of the whole packet (opcode included). */
    void seek(int offset) {
        if (offset < 2 || offset > packet.length) {
            throw new IllegalArgumentException(name + ": offset " + offset + " outside the packet");
        }
        in.position(offset);
    }

    /** The whole packet (not a copy; callers must not modify it). */
    byte[] packet() {
        return packet;
    }

    int remaining() {
        return in.remaining();
    }

    /** A CMovePath at the current position, decoded by {@code codec}; the reader continues after it. */
    MovementInfo path(MovePathCodec codec) {
        byte[] tail = Arrays.copyOfRange(packet, in.position(), packet.length);
        InPacket ip = new InPacket(tail);
        MovementInfo path;
        try {
            path = codec.decode(ip);
        } catch (IndexOutOfBoundsException | IllegalStateException e) {
            throw new IllegalArgumentException("Truncated " + name + " path", e);
        }
        in.position(in.position() + tail.length - ip.getUnreadAmount());
        return path;
    }

    /** The packet must be fully read. */
    void end() {
        if (in.hasRemaining()) {
            throw new IllegalArgumentException(name + " has " + in.remaining() + " trailing bytes");
        }
    }

    private <T> T guard(java.util.function.Supplier<T> read) {
        try {
            return read.get();
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated " + name, e);
        }
    }
}
