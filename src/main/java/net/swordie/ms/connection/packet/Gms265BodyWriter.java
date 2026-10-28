package net.swordie.ms.connection.packet;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Little-endian writer of a 265 request body (no opcode) for the 282 request records that hand a rewritten request to
 * an unchanged 265 handler; strings are written like 265 {@code InPacket.decodeString} reads them (u16 length, UTF-8).
 */
public final class Gms265BodyWriter {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    public Gms265BodyWriter u8(int v) {
        out.write(v);
        return this;
    }

    public Gms265BodyWriter u16(int v) {
        out.write(v);
        out.write(v >>> 8);
        return this;
    }

    public Gms265BodyWriter i32(int v) {
        for (int i = 0; i < 4; i++) {
            out.write(v >>> (8 * i));
        }
        return this;
    }

    public Gms265BodyWriter i64(long v) {
        for (int i = 0; i < 8; i++) {
            out.write((int) (v >>> (8 * i)));
        }
        return this;
    }

    public Gms265BodyWriter str(String s) {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        u16(b.length);
        out.writeBytes(b);
        return this;
    }

    public Gms265BodyWriter raw(byte[] b) {
        out.writeBytes(b);
        return this;
    }

    public byte[] bytes() {
        return out.toByteArray();
    }
}
