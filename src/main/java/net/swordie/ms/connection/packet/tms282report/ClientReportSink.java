package net.swordie.ms.connection.packet.tms282report;

/**
 * Observer of parsed client reports (Strategy for where they go).
 * <p>
 * Today the login probe and the server GUI are separate processes, so the probe uses
 * {@link JsonlClientReportSink} and the GUI tails the file with {@link JsonlClientReportSource}.
 * When 282 login moves into the server itself, the login handler passes the same
 * {@link ClientReportParsers} output to an in-memory sink that notifies the GUI directly;
 * parsers and {@link Tms282ClientReport} stay unchanged.
 * <p>
 * Implementations must be thread-safe. They may throw {@link RuntimeException}; callers on a
 * connection thread catch it so reporting never ends a session.
 */
@FunctionalInterface
public interface ClientReportSink {
    void accept(Tms282ClientReport report);
}
