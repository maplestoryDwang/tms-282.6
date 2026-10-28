package net.swordie.ms.connection.packet.tms282report;

/**
 * Strategy for one report layout (one opcode, or several opcodes sharing a layout).
 * Implementations only read; they never reply to the client and hold no connection state,
 * so the same instances serve the login probe today and the server's login handlers later.
 * <p>
 * Throwing {@link RuntimeException} is allowed: {@link ClientReportParsers} catches it and
 * records the packet with {@link HexReportParser} instead. Bytes left unread become a
 * trailing hex field.
 */
public interface ClientReportParser {
    /** Display name shown in the GUI table. */
    String name();

    /** Reads the body after the two-byte opcode. */
    void parse(ReportReader in, ReportFields out);
}
