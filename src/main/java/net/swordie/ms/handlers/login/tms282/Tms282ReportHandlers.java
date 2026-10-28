package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.packet.tms282report.ClientReportParsers;
import net.swordie.ms.connection.packet.tms282report.ClientReportSink;
import net.swordie.ms.connection.packet.tms282report.JsonlClientReportSink;
import net.swordie.ms.connection.packet.tms282report.SensitiveClientOpcodes;
import net.swordie.ms.connection.packet.tms282report.Tms282ClientReport;
import net.swordie.ms.handlers.header.InHeader;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Client reports and acknowledgements that need no reply (the probe's ReportHandler and SensitiveNoReplyHandler).
 * <ul>
 *     <li>{@link #REPORT_OPS}: parsed by {@link ClientReportParsers} and appended to the JSONL file the server GUI's
 *     客戶端回報 tab reads; reporting failures are logged and swallowed, they never end the session;</li>
 *     <li>{@link #SENSITIVE_OPS}: carry credentials; only opcode and length are logged, the body is never parsed,
 *     stored or printed.</li>
 * </ul>
 * login-cp-layouts-2823.md, unknown-misc-cp-2823.md, unknown-lp-2823.md, unknown-orphan-cp-2823.md.
 */
final class Tms282ReportHandlers {
    /**
     * The probe's IGNORED_CLIENT_OPS: 007B 00A1 00A5 00BB 00C1 00D0 00DB 00DD 00E9 009F 00A0 0099 0307 0309 0374 07C6,
     * 009C (no busy), 00AE (report sent before disconnecting), 01B0 0617 (in-game, not login), 0070 0072 007A 0095
     * 0096 0098 00A3 00A4 00B5 02B0 (captured, flow continued), 0094 0097 00CC (static layouts).
     */
    static final Set<Integer> REPORT_OPS = Set.of(0x7b, 0xa1, 0xa5, 0xbb, 0xc1, 0xd0, 0xdb,
            0xdd, 0xe9, 0x9f, 0xa0, 0x99, 0x307, 0x309, 0x374, 0x7c6, 0x9c, 0xae, 0x1b0, 0x617,
            0x70, 0x72, 0x7a, 0x95, 0x96, 0x98, 0xa3, 0xa4, 0xb5, 0x2b0,
            0x94, 0x97, 0xcc);
    /** The probe's SENSITIVE_NO_REPLY_OPS: no reply, listed in SensitiveClientOpcodes. */
    static final Set<Integer> SENSITIVE_OPS = Set.of(0xa2, 0xdc);

    private final ClientReportParsers parsers;
    private final ClientReportSink sink;

    Tms282ReportHandlers(ClientReportParsers parsers, ClientReportSink sink) {
        this.parsers = Objects.requireNonNull(parsers, "parsers");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /** The standard parsers and the shared JSONL report file. */
    static Tms282ReportHandlers standard(Charset nameCharset) {
        return new Tms282ReportHandlers(ClientReportParsers.standard(nameCharset),
                new JsonlClientReportSink(JsonlClientReportSink.defaultFile()));
    }

    /** Registers every report and sensitive no-reply opcode on the login socket. */
    void register(Tms282Routes routes) {
        for (int op : REPORT_OPS) {
            if (SensitiveClientOpcodes.contains(op)) {
                throw new IllegalStateException(String.format("CP%04X carries credentials; never a report", op));
            }
            routes.login(op, headerFor(op)).handle("report (no reply)", this::report);
        }
        for (int op : SENSITIVE_OPS) {
            if (!SensitiveClientOpcodes.contains(op)) {
                throw new IllegalStateException(String.format("CP%04X is not listed in SensitiveClientOpcodes", op));
            }
            routes.login(op, headerFor(op)).handle("sensitive (no reply, not parsed)", Tms282ReportHandlers::sensitive);
        }
    }

    /** Report/ack that needs no reply. */
    void report(Tms282LoginState session, int op, byte[] packet) {
        System.out.printf("[TMS282] IGNORED %04X (report/ack; no reply needed)%n", op);
        record(session, packet);
    }

    /** Records the packet as a client report without replying; used alone or before a real reply. */
    void record(Tms282LoginState session, byte[] packet) {
        int op = packet.length < 2 ? -1 : (packet[0] & 0xFF) | (packet[1] & 0xFF) << 8;
        try {
            String account = session.user() == null ? "" : session.user().getName();
            var source = new Tms282ClientReport.Source(session.safeAddress(), account);
            parsers.parse(packet, source, Instant.now()).ifPresent(sink::accept);
        } catch (RuntimeException | LinkageError e) {
            System.err.printf("[TMS282] REPORT %04X not recorded: %s%n", op, e);
        }
    }

    static void sensitive(Tms282LoginState session, int op, byte[] packet) {
        System.out.printf("[TMS282] IGNORED %04X len=%d (sensitive; no reply, not parsed or recorded)%n", op, packet.length);
    }

    /**
     * Header of a report opcode when the opcode table has none: its CPxxxx constant, or the 282 name the status
     * table gives it (CP007B CLIENT_LOADING_TIME_LOG, CP00AE CLIENT_PACKET_DECODE_ERROR).
     */
    static InHeader headerFor(int op) {
        return switch (op) {
            case 0x7B -> InHeader.CLIENT_LOADING_TIME_LOG;
            case 0xAE -> InHeader.CLIENT_PACKET_DECODE_ERROR;
            default -> InHeader.valueOf(String.format("CP%04X", op));
        };
    }
}
