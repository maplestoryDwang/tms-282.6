package net.swordie.ms.connection.packet.tms282report;

import net.swordie.ms.connection.protocol.Tms282OpcodeTables;

import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * One packet the 282 login probe received or sent, as shown in the server GUI's 282 封包 tab.
 * <p>
 * Immutable value. {@code data} is the plaintext packet including its two-byte opcode, which must equal
 * {@code opcode}; at most {@link #MAX_DATA_BYTES} bytes are kept and {@code length} is the original packet length.
 * A received packet with a {@link SensitiveClientOpcodes sensitive opcode} (credentials, SPW) keeps only its
 * opcode and length: every body byte is zero. The constructor refuses anything else, so neither the JSONL file
 * nor the GUI can ever hold such a body; build traces with {@link #capture}, which masks and truncates.
 * Sent packets use the LP opcode space, where the sensitive list does not apply.
 */
public record Tms282PacketTrace(Instant time, Direction direction, SocketKind socket, String session, int sequence,
                                int opcode, String name, int length, byte[] data) {
    /** Most packet bytes a trace keeps (and the JSONL file stores); longer packets are truncated. */
    public static final int MAX_DATA_BYTES = 64 * 1024;

    /**
     * RECV = client to server (CP opcodes), SEND = server to client (LP opcodes), DROPPED = a server packet the
     * formal server's OutboundGuard refused (LP opcodes; it never reached the socket).
     */
    public enum Direction {
        RECV(Tms282OpcodeTables.Direction.CP),
        SEND(Tms282OpcodeTables.Direction.LP),
        DROPPED(Tms282OpcodeTables.Direction.LP);

        private final Tms282OpcodeTables.Direction table;

        Direction(Tms282OpcodeTables.Direction table) {
            this.table = table;
        }

        /** Opcode table (and label prefix CP/LP) of this direction. */
        public Tms282OpcodeTables.Direction table() {
            return table;
        }

        /** "CP0074" / "LP02EF". */
        public String label(int opcode) {
            return String.format("%s%04X", table, opcode);
        }
    }

    /** Which listener (probe or formal server) the packet went through. */
    public enum SocketKind {
        LOGIN,
        CHANNEL
    }

    public Tms282PacketTrace {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(socket, "socket");
        session = session == null ? "" : session;
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(data, "data");
        if (sequence < 0) {
            throw new IllegalArgumentException("Negative sequence");
        }
        if (opcode < 0 || opcode > 0xFFFF) {
            throw new IllegalArgumentException("Opcode outside u16 range");
        }
        if (length < 2) {
            throw new IllegalArgumentException("Packet shorter than its opcode");
        }
        if (data.length != Math.min(length, MAX_DATA_BYTES)) {
            throw new IllegalArgumentException("Kept " + data.length + " bytes of a " + length + "-byte packet");
        }
        if ((Byte.toUnsignedInt(data[0]) | Byte.toUnsignedInt(data[1]) << 8) != opcode) {
            throw new IllegalArgumentException(String.format("Data does not start with opcode %04X", opcode));
        }
        data = data.clone();
        if (isSensitive(direction, opcode)) {
            for (int i = 2; i < data.length; i++) {
                if (data[i] != 0) {
                    throw new IllegalArgumentException(direction.label(opcode) + " carries credentials; its body must be masked");
                }
            }
        }
    }

    /**
     * Trace of a whole plaintext packet: the opcode is read from it, a sensitive body is zeroed and bytes past
     * {@link #MAX_DATA_BYTES} are dropped. {@code packet} is not modified.
     */
    public static Tms282PacketTrace capture(Instant time, Direction direction, SocketKind socket, String session,
                                            int sequence, byte[] packet, Tms282OpcodeNames names) {
        Objects.requireNonNull(packet, "packet");
        Objects.requireNonNull(names, "names");
        if (packet.length < 2) {
            throw new IllegalArgumentException("Packet shorter than its opcode");
        }
        int opcode = Byte.toUnsignedInt(packet[0]) | Byte.toUnsignedInt(packet[1]) << 8;
        int kept = Math.min(packet.length, MAX_DATA_BYTES);
        byte[] data;
        if (isSensitive(direction, opcode)) {
            data = new byte[kept];
            data[0] = packet[0];
            data[1] = packet[1];
        } else {
            data = Arrays.copyOf(packet, kept);
        }
        return new Tms282PacketTrace(time, direction, socket, session, sequence, opcode,
                names.name(direction, opcode), packet.length, data);
    }

    /** True for received packets whose opcode is in {@link SensitiveClientOpcodes}. */
    public static boolean isSensitive(Direction direction, int opcode) {
        return direction == Direction.RECV && SensitiveClientOpcodes.contains(opcode);
    }

    /** True when the body was zeroed because the opcode carries credentials. */
    public boolean redacted() {
        return isSensitive(direction, opcode);
    }

    /** True when the packet was longer than {@link #MAX_DATA_BYTES} and only its head is kept. */
    public boolean truncated() {
        return length > data.length;
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    /** "CP0074" / "LP02EF". */
    public String opcodeLabel() {
        return direction.label(opcode);
    }

    public String hex() {
        return HexFormat.of().formatHex(data);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tms282PacketTrace other && time.equals(other.time) && direction == other.direction
                && socket == other.socket && session.equals(other.session) && sequence == other.sequence
                && opcode == other.opcode && name.equals(other.name) && length == other.length
                && Arrays.equals(data, other.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, direction, socket, session, sequence, opcode, name, length) * 31 + Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return "Tms282PacketTrace[" + time + " " + direction + " " + socket + " " + session + "#" + sequence + " "
                + opcodeLabel() + " " + name + " len=" + length + (redacted() ? " redacted" : "") + "]";
    }
}
