package net.swordie.ms.connection.packet.tms282report;

import net.swordie.ms.connection.protocol.Tms282OpcodeTables;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Display names of TMS 282.3 opcodes for {@link Tms282PacketTrace}, read from the generated opcode tables
 * ({@code data/protocol/tms282.3/inbound.tsv} and {@code outbound.tsv}, {@link Tms282OpcodeTables#read}).
 * An opcode that is missing, or listed without a name ("-"), is shown as {@code CPxxxx} / {@code LPxxxx}.
 * Immutable and thread-safe.
 */
public final class Tms282OpcodeNames {
    private static final Tms282OpcodeNames EMPTY = new Tms282OpcodeNames(Map.of());
    private final Map<Tms282PacketTrace.Direction, Map<Integer, String>> names;

    private Tms282OpcodeNames(Map<Tms282PacketTrace.Direction, Map<Integer, String>> names) {
        this.names = names;
    }

    /** Names nobody knows yet: every opcode falls back to its CP/LP label. */
    public static Tms282OpcodeNames empty() {
        return EMPTY;
    }

    /** {@code <server root>/data/protocol/tms282.3} ({@link Tms282LogFiles#serverRoot()}). */
    public static Path defaultDir() {
        return Tms282LogFiles.serverRoot().resolve("data").resolve("protocol").resolve("tms282.3");
    }

    /** Reads both tables in {@code dir}; throws when either is missing or malformed. */
    public static Tms282OpcodeNames load(Path dir) throws IOException {
        Objects.requireNonNull(dir, "dir");
        Map<Tms282PacketTrace.Direction, Map<Integer, String>> names = new EnumMap<>(Tms282PacketTrace.Direction.class);
        for (Tms282PacketTrace.Direction direction : Tms282PacketTrace.Direction.values()) {
            Map<Integer, String> byOpcode = new HashMap<>();
            for (Tms282OpcodeTables.Row row : Tms282OpcodeTables.read(dir.resolve(direction.table().file()), direction.table())) {
                if (!row.name().equals("-") && !row.name().isBlank()) {
                    byOpcode.put(row.op(), row.name());
                }
            }
            names.put(direction, Map.copyOf(byOpcode));
        }
        return new Tms282OpcodeNames(names);
    }

    /** {@link #load} of {@link #defaultDir()}, or {@link #empty()} (with one stderr line) when it cannot be read. */
    public static Tms282OpcodeNames loadDefaultOrEmpty() {
        Path dir = defaultDir();
        try {
            return load(dir);
        } catch (IOException | RuntimeException e) {
            System.err.println("TMS282 opcode names unavailable (" + dir + "): " + e);
            return EMPTY;
        }
    }

    /** Table name, or {@code CPxxxx} / {@code LPxxxx} when the opcode has none. */
    public String name(Tms282PacketTrace.Direction direction, int opcode) {
        String name = names.getOrDefault(Objects.requireNonNull(direction, "direction"), Map.of()).get(opcode);
        return name != null ? name : direction.label(opcode);
    }
}
