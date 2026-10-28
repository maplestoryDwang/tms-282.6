package net.swordie.ms.connection.packet.tms282report;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Entry point for a connection thread: turns one plaintext packet into a {@link Tms282PacketTrace}
 * ({@link Tms282PacketTrace#capture}: names, masking, truncation) and hands it to a {@link PacketTraceSink}.
 * <p>
 * {@link #record} never throws: any failure (unwritable file, missing class, bad packet) is passed to the
 * error logger and swallowed, so tracing can never end a session. Thread-safe when the sink is.
 */
public final class PacketTraceRecorder {
    private final Tms282OpcodeNames names;
    private final PacketTraceSink sink;
    private final Clock clock;
    private final Consumer<String> errorLog;

    public PacketTraceRecorder(Tms282OpcodeNames names, PacketTraceSink sink, Clock clock, Consumer<String> errorLog) {
        this.names = Objects.requireNonNull(names, "names");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.errorLog = Objects.requireNonNull(errorLog, "errorLog");
    }

    /** Names from the default opcode tables, the default JSONL file, the system clock and stderr. */
    public static PacketTraceRecorder standard() {
        return new PacketTraceRecorder(Tms282OpcodeNames.loadDefaultOrEmpty(),
                new JsonlPacketTraceSink(JsonlPacketTraceSink.defaultFile()), Clock.systemUTC(), System.err::println);
    }

    /** Traces {@code packet} (opcode included); failures are logged, never thrown. */
    public void record(Tms282PacketTrace.Direction direction, Tms282PacketTrace.SocketKind socket, String session,
                       int sequence, byte[] packet) {
        try {
            sink.accept(Tms282PacketTrace.capture(clock.instant(), direction, socket, session, sequence, packet, names));
        } catch (RuntimeException | LinkageError e) {
            try {
                errorLog.accept("PACKET TRACE " + direction + " #" + sequence + " not recorded: " + e);
            } catch (RuntimeException ignored) {
                // Logging must not end the session either.
            }
        }
    }
}
