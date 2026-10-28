package net.swordie.ms.connection.packet;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * CP007E, the channel migrate-in request: the second CP on a channel socket, after CP0070.
 * <p>
 * The 282.3 builder runs inside the Themida VM (346CB1E jmp B2F209A; the COutPacket is destroyed at 346D2D7,
 * channel-enter-2823.md 1.3/1.4), so no field below is proven by a readable Encode call. The layout comes from
 * the first real capture (sessions/channel-1790136212936-2-..., recv-001-007e.bin, 132 bytes) compared with
 * the values this server had sent before it. Evidence per field:
 * <pre>
 * off  width  field            evidence
 * 0    u16    opcode 0x007E    capture (the presumed 0x007D was wrong)
 * 2    i32    worldId          [presumed] = LP0014 world (55); 265 MigrationHandler reads i32 world first
 * 6    i32    userId           [presumed] = users.id (9), not the per-world accounts.id; 265 names it userID
 * 10   i32    characterId      [presumed] = LP0014 character (34)
 * 14   raw16  machineId        [guess] 265 position of the 16-byte machine ID; value not matched anywhere else
 * 30   u8     unknown1E        [unknown] 0 in the capture
 * 31   i32    unknown1F        [unknown] 40 in the capture, equal to the length of the trailing text buffer
 * 35   raw12  unknown23        [unknown] not the LP0010 client key, not the LP0014 token
 * 47   i32 n  identity length  [presumed] n + n bytes = the LP0050 identity blob (the CP00A4 format, n=31)
 * 51   raw n  identity blob
 * 51+n u16    unknown          [unknown] 0 in the capture
 * 53+n raw8   creationTime     [presumed] FILETIME; bytes equal the user creation date LP0013 sends
 * 61+n rest   trailingText     [unknown] 40 bytes: ASCII "0000-0000-0000-0000-0000-0000-0000-0000" + NUL
 * </pre>
 * The LP0014 token does not occur in the capture, so it cannot authenticate the request: the server has to rely
 * on its own pending migration (same world, user and character, not expired, used once).
 * Unknown fields are kept as raw bytes; nothing is inferred from them.
 */
public record Tms282MigrateInRequest(int worldId, int userId, int characterId, byte[] machineId,
                                     int unknown1E, int unknown1F, byte[] unknown23, byte[] identityBlob,
                                     int unknownAfterBlob, long creationTime, byte[] trailingText) {
    public static final int OPCODE = 0x007E;
    public static final int MACHINE_ID_LENGTH = 16;
    public static final int UNKNOWN23_LENGTH = 12;
    /** Opcode through the identity length, then u16 + FILETIME after the blob. */
    public static final int FIXED_LENGTH = 2 + 4 + 4 + 4 + MACHINE_ID_LENGTH + 1 + 4 + UNKNOWN23_LENGTH + 4 + 2 + 8;
    /** Same bound the client uses for LP0050 (34797D0: n &lt;= 1,000,000); a channel packet is far smaller. */
    public static final int MAX_IDENTITY_LENGTH = 4096;

    public Tms282MigrateInRequest {
        machineId = Objects.requireNonNull(machineId, "machineId").clone();
        unknown23 = Objects.requireNonNull(unknown23, "unknown23").clone();
        identityBlob = Objects.requireNonNull(identityBlob, "identityBlob").clone();
        trailingText = Objects.requireNonNull(trailingText, "trailingText").clone();
        if (machineId.length != MACHINE_ID_LENGTH || unknown23.length != UNKNOWN23_LENGTH) {
            throw new IllegalArgumentException("Fixed-width migrate-in field has the wrong length");
        }
    }

    @Override
    public byte[] machineId() {
        return machineId.clone();
    }

    @Override
    public byte[] unknown23() {
        return unknown23.clone();
    }

    @Override
    public byte[] identityBlob() {
        return identityBlob.clone();
    }

    @Override
    public byte[] trailingText() {
        return trailingText.clone();
    }

    /** Accepts the full plaintext packet, including the opcode. */
    public static Tms282MigrateInRequest parse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        try {
            if (Short.toUnsignedInt(in.getShort()) != OPCODE) {
                throw new IllegalArgumentException("Wrong migrate-in opcode");
            }
            int world = in.getInt(), user = in.getInt(), character = in.getInt();
            byte[] machine = bytes(in, MACHINE_ID_LENGTH);
            int unknown1E = Byte.toUnsignedInt(in.get());
            int unknown1F = in.getInt();
            byte[] unknown23 = bytes(in, UNKNOWN23_LENGTH);
            int identityLength = in.getInt();
            if (identityLength < 0 || identityLength > MAX_IDENTITY_LENGTH || identityLength > in.remaining() - 10) {
                throw new IllegalArgumentException("CP007E identity blob length out of range: " + identityLength);
            }
            byte[] identity = bytes(in, identityLength);
            int afterBlob = Short.toUnsignedInt(in.getShort());
            long creation = in.getLong();
            byte[] trailing = bytes(in, in.remaining());
            return new Tms282MigrateInRequest(world, user, character, machine, unknown1E, unknown1F, unknown23,
                    identity, afterBlob, creation, trailing);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Truncated CP007E", e);
        }
    }

    private static byte[] bytes(ByteBuffer in, int length) {
        byte[] out = new byte[length];
        in.get(out);
        return out;
    }

    /** The trailing buffer up to its first NUL, as ASCII (non-ASCII bytes are replaced); diagnostic only. */
    public String trailingTextAscii() {
        int end = 0;
        while (end < trailingText.length && trailingText[end] != 0) {
            end++;
        }
        return new String(trailingText, 0, end, StandardCharsets.US_ASCII);
    }

    /** Log form: identifiers and sizes only; the machine ID and identity blob are shown as lengths. */
    @Override
    public String toString() {
        return String.format("Tms282MigrateInRequest[world=%d, user=%d, character=%d, machineIdBytes=%d, unknown1E=%d,"
                        + " unknown1F=%d, unknown23=%s, identityBytes=%d, unknownAfterBlob=%d, creationTime=0x%016X,"
                        + " trailingBytes=%d]",
                worldId, userId, characterId, machineId.length, unknown1E, unknown1F, HexFormat.of().formatHex(unknown23),
                identityBlob.length, unknownAfterBlob, creationTime, trailingText.length);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Tms282MigrateInRequest r && worldId == r.worldId && userId == r.userId
                && characterId == r.characterId && Arrays.equals(machineId, r.machineId) && unknown1E == r.unknown1E
                && unknown1F == r.unknown1F && Arrays.equals(unknown23, r.unknown23)
                && Arrays.equals(identityBlob, r.identityBlob) && unknownAfterBlob == r.unknownAfterBlob
                && creationTime == r.creationTime && Arrays.equals(trailingText, r.trailingText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, userId, characterId, Arrays.hashCode(machineId), unknown1E, unknown1F,
                Arrays.hashCode(unknown23), Arrays.hashCode(identityBlob), unknownAfterBlob, creationTime,
                Arrays.hashCode(trailingText));
    }
}
