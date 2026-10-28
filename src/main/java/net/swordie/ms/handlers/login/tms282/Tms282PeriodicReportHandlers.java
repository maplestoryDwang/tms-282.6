package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.connection.packet.tms282report.ClientReportParsers;
import net.swordie.ms.connection.packet.tms282report.Tms282ClientReport;
import net.swordie.ms.handlers.header.InHeader;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Channel routes for the in-field TMS 282.3 packets the client sends on timers or once at field entry
 * (packet-audit/unknown-periodic-cp-2823.md): CP021F (10 s skill hash poll), CP02B0 (multi-subtype report; the login
 * socket already records it), CP0496 (settings table), CP04B5 (30 s counters), CP077D (request waiting for an ACK),
 * CP0782 (captured GameUser::PartyParadise_UpdateCache RPC) and CP0788 (once). These report senders take no exclusive-request lock, so none needs a
 * reply; CP077D does wait for an ACK, but its LP opcode is unknown and a wrong opcode would make the client throw, so
 * it is only logged and its 60 s resends are marked.
 * <p>
 * Each packet is decoded with the {@link ClientReportParsers} layouts, printed as one server log line and recorded
 * for the GUI 客戶端回報 tab ({@link Tms282ReportHandlers#record}). Nothing here ends the session: a malformed body
 * falls back to hex.
 */
final class Tms282PeriodicReportHandlers {
    static final int[] OPCODES = {0x021F, 0x02B0, 0x02B3, 0x0306, 0x0496, 0x04B5, 0x077D, 0x0782, 0x0788};
    static final int RELIABLE_REQUEST = 0x077D;

    private final Tms282ReportHandlers reports;
    private final ClientReportParsers parsers;
    private final Map<Tms282LoginState, Set<Integer>> reliableIds = Collections.synchronizedMap(new WeakHashMap<>());

    Tms282PeriodicReportHandlers(Tms282ReportHandlers reports, Charset charset) {
        this.reports = Objects.requireNonNull(reports, "reports");
        this.parsers = ClientReportParsers.standard(Objects.requireNonNull(charset, "charset"));
    }

    void register(Tms282Routes routes) {
        // Native29E68D0 and live character-selection captures also use the login socket.
        routes.login(0x0496, InHeader.CP0496).handle("settings report (no reply)", this::report);
        for (int op : OPCODES) {
            routes.channel(op, InHeader.valueOf(String.format("CP%04X", op)))
                    .handle(op == RELIABLE_REQUEST ? "reliable request (ACK opcode unknown; logged, resends marked)"
                            : "periodic report (no reply)", this::report);
        }
    }

    private void report(Tms282LoginState session, int op, byte[] packet) {
        if (op == 0x0782 && packet.length >= 8 && Tms282Guards.IN_FIELD.permits(session.client(), session.fieldCharacter())
                && java.nio.ByteBuffer.wrap(packet).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt(2) == 0x0A1A383A
                && net.swordie.ms.client.character.tms282.Tms282MyHomeRuntime.handleGameUser(session.fieldCharacter(),
                    java.util.Arrays.copyOfRange(packet, 2, packet.length))) return;
        String summary = parsers.parse(packet, Tms282ClientReport.Source.UNKNOWN, Instant.now())
                .map(r -> r.name() + " " + r.summary()).orElse("len=" + packet.length);
        String resend = op == RELIABLE_REQUEST ? resendMark(session, packet) : "";
        System.out.printf("[TMS282] REPORT CP%04X %s%s (no reply)%n", op, summary, resend);
        reports.record(session, packet);
    }

    /** " RESEND" when this session already sent the CP077D reqId (the client got no ACK within 60 s). */
    String resendMark(Tms282LoginState session, byte[] packet) {
        if (packet.length < 6) {
            return "";
        }
        int reqId = (packet[2] & 0xFF) | (packet[3] & 0xFF) << 8 | (packet[4] & 0xFF) << 16 | (packet[5] & 0xFF) << 24;
        Set<Integer> seen = reliableIds.computeIfAbsent(session, s -> Collections.synchronizedSet(new HashSet<>()));
        return seen.add(reqId) ? "" : " RESEND reqId=" + reqId + " (no ACK)";
    }
}
