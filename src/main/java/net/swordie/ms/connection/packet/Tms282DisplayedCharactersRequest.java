package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * CP00EC, displayed-character dialog OK (21A06E0, send 21A1069): i32 accountId (app+23AC; -1 without app),
 * i32 n, n x i32 characterId (the whole CLogin+750 list). Evidence: burning-display-chars-2823.md 3.2.
 * No dedicated reply: the client re-selects the world itself and the next LP0013 carries the list.
 */
public record Tms282DisplayedCharactersRequest(int accountId, List<Integer> characterIds) {
    public static final int OPCODE = 0x00EC;

    public Tms282DisplayedCharactersRequest {
        characterIds = List.copyOf(characterIds);
    }

    /** Accepts the full plaintext packet, including opcode. n must match the remaining bytes exactly. */
    public static Tms282DisplayedCharactersRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Not a CP00EC displayed-character request");
            }
            int accountId = in.getInt();
            int count = in.getInt();
            if (count < 0 || (long) count * 4 != in.remaining()) {
                throw new IllegalArgumentException("CP00EC count does not match its length");
            }
            List<Integer> ids = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ids.add(in.getInt());
            }
            return new Tms282DisplayedCharactersRequest(accountId, ids);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP00EC", e);
        }
    }
}
