package net.swordie.ms.connection.packet.tms282report;

/**
 * Observer of traced 282 packets (Strategy for where they go), the packet counterpart of
 * {@link ClientReportSink}.
 * <p>
 * Today the login probe and the server GUI are separate processes, so the probe uses
 * {@link JsonlPacketTraceSink} and the GUI tails the file with {@link JsonlPacketTraceSource}.
 * When 282 login moves into the server itself, an in-memory sink can hand the same
 * {@link Tms282PacketTrace} values to the GUI directly.
 * <p>
 * Implementations must be thread-safe. They may throw {@link RuntimeException}; {@link PacketTraceRecorder}
 * catches it so tracing never ends a session.
 */
@FunctionalInterface
public interface PacketTraceSink {
    void accept(Tms282PacketTrace trace);
}
