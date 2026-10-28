package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * CP01C0 USER_REQUEST_INSTANCE_TABLE (265 InHeader 0x1B6), sender 5FC9D50: {@code Str table, i32 type, i32 subType}
 * [verified: static sender, encode sequence S 4 4, and 66 captured samples]. The only sender callers (5FCA4DA,
 * 5FCAB22) ask for a table value the client has not cached yet; the answer is LP0121 ({@link Tms282InstanceTablePackets}).
 * In the capture every request is {@code "hyper"}, type 28..60, subType 0 then 1 (sent once while entering the field,
 * never repeated). Table names are ASCII (265 InstanceTableType); the name is decoded as US-ASCII and a non-ASCII byte
 * is rejected.
 */
public record Tms282InstanceTableRequest(String table, int type, int subType) {
    public static final int OPCODE = 0x01C0;

    public Tms282InstanceTableRequest {
        Objects.requireNonNull(table, "table");
    }

    /** Accepts the full plaintext packet, including the opcode. */
    public static Tms282InstanceTableRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong CP01C0 opcode");
            }
            byte[] raw = new byte[Short.toUnsignedInt(in.getShort())];
            in.get(raw);
            for (byte b : raw) {
                if (b < 0x20) {
                    // negative (>= 0x80) or control byte: not a table name
                    throw new IllegalArgumentException("CP01C0 table name is not printable ASCII");
                }
            }
            var request = new Tms282InstanceTableRequest(new String(raw, StandardCharsets.US_ASCII), in.getInt(), in.getInt());
            if (in.hasRemaining()) {
                throw new IllegalArgumentException("CP01C0 has " + in.remaining() + " trailing bytes");
            }
            return request;
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP01C0", e);
        }
    }
}
