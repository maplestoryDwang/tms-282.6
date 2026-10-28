package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.packet.Tms282FieldReport;
import net.swordie.ms.connection.packet.Tms282LockedUiRequest;
import net.swordie.ms.connection.packet.Tms282StatPackets;
import net.swordie.ms.handlers.header.InHeader;

import java.nio.charset.Charset;
import java.util.Objects;

/**
 * In-field client packets of TMS 282.3 whose senders were disassembled on 2026-09-23
 * (packet-audit/unknown-field-report-cp-2823.md):
 * <ul>
 *     <li>every {@link Tms282FieldReport}: no lock is taken, so no reply; the body is decoded strictly and logged
 *     (the GUI trace already records the raw packet) instead of "has no 282 handler";</li>
 *     <li>CP02C5 ({@link Tms282LockedUiRequest}): opens self character information through LP00CA
 *     (5FA81C0 unlocks, creates the window and forwards the requested tab to 5FA9970).
 *     Malformed requests receive LP009D unlock without opening a window.</li>
 * </ul>
 * A malformed report is logged and dropped; nothing here ends the session.
 */
final class Tms282FieldReportHandlers {
    private final Charset charset;

    Tms282FieldReportHandlers(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    void register(Tms282Routes routes) {
        for (Tms282FieldReport report : Tms282FieldReport.values()) {
            routes.channel(report.opcode(), header(report.opcode())).when(Tms282Guards.IN_FIELD)
                    .handle(report.meaning() + " (report, no reply)", (s, op, p) -> report(report, p));
        }
        routes.channel(Tms282LockedUiRequest.OPCODE, InHeader.CP02C5).when(Tms282Guards.IN_FIELD)
                .handle("self character information -> LP00CA", this::lockedUi);
    }

    private void report(Tms282FieldReport report, byte[] packet) {
        try {
            System.out.println("[TMS282] REPORT " + report.describe(packet, charset) + " (no reply)");
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] REPORT CP%04X malformed (%s): %s%n", report.opcode(), e.getMessage(),
                    Tms282FieldReport.hex(packet));
        }
    }

    private void lockedUi(Tms282LoginState session, int op, byte[] packet) {
        try {
            Tms282LockedUiRequest request = Tms282LockedUiRequest.parse(packet);
            System.out.printf("[TMS282] CP02C5 character information type=%d tick=%d -> LP00CA%n", request.type(),
                    request.tick());
            session.send(request.reply());
            return;
        } catch (IllegalArgumentException e) {
            System.out.printf("[TMS282] CP02C5 malformed (%s): %s -> LP009D unlock%n", e.getMessage(),
                    Tms282FieldReport.hex(packet));
        }
        session.send(Tms282StatPackets.unlock());
    }

    /** CP0109 has a 265 name (USER_UNK_REQUEST); the others use their CPxxxx constant. */
    static InHeader header(int op) {
        return op == 0x0109 ? InHeader.USER_UNK_REQUEST : InHeader.valueOf(String.format("CP%04X", op));
    }
}
