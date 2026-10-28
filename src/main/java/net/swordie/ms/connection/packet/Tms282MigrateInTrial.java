package net.swordie.ms.connection.packet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * TRIAL decode of the channel migrate-in request (presumed CP007D). Its 282.3 builder runs inside the
 * Themida VM (346CB1E), so neither opcode nor layout is known (channel-enter-2823.md 1.4). This reads the
 * 265 layout (MigrationHandler.handleMigrateIn: i32 world, i32 accountId, i32 characterId, 16-byte machine ID)
 * only to print candidate values next to the pending LP0014 migration; nothing here is verified and no server
 * decision may rest on it. The opcode is not checked, so the first unknown channel packet can be tried too.
 */
public record Tms282MigrateInTrial(int opcode, int world, int accountId, int characterId, byte[] machineId,
                                   int remaining) {
    public static final int PRESUMED_OPCODE = 0x007D;
    /** Opcode + the 265 fields. */
    public static final int MINIMUM_LENGTH = 2 + 4 + 4 + 4 + 16;

    public Tms282MigrateInTrial {
        machineId = machineId.clone();
    }

    @Override
    public byte[] machineId() {
        return machineId.clone();
    }

    /** Empty when the packet is shorter than the 265 layout; extra bytes are counted, not rejected. */
    public static Optional<Tms282MigrateInTrial> tryParse(byte[] packet) {
        Objects.requireNonNull(packet, "packet");
        if (packet.length < MINIMUM_LENGTH) {
            return Optional.empty();
        }
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = Short.toUnsignedInt(in.getShort());
        int world = in.getInt(), accountId = in.getInt(), characterId = in.getInt();
        byte[] machine = new byte[16];
        in.get(machine);
        return Optional.of(new Tms282MigrateInTrial(opcode, world, accountId, characterId, machine,
                in.remaining()));
    }

    /** Every payload offset (after the opcode, any alignment) where value appears as a little-endian i32. */
    public static List<Integer> offsetsOf(byte[] packet, int value) {
        Objects.requireNonNull(packet, "packet");
        List<Integer> offsets = new ArrayList<>();
        ByteBuffer in = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 2; i + 4 <= packet.length; i++) {
            if (in.getInt(i) == value) {
                offsets.add(i);
            }
        }
        return offsets;
    }

    @Override
    public String toString() {
        return String.format("Tms282MigrateInTrial[opcode=0x%04X, world=%d, accountId=%d, characterId=%d, machineId=%s, remaining=%d]",
                opcode, world, accountId, characterId, HexFormat.of().formatHex(machineId), remaining);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Tms282MigrateInTrial t && opcode == t.opcode && world == t.world
                && accountId == t.accountId && characterId == t.characterId && Arrays.equals(machineId, t.machineId)
                && remaining == t.remaining;
    }

    @Override
    public int hashCode() {
        return Objects.hash(opcode, world, accountId, characterId, Arrays.hashCode(machineId), remaining);
    }
}
