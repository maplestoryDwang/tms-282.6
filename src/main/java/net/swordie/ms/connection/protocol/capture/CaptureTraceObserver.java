package net.swordie.ms.connection.protocol.capture;

import net.swordie.ms.connection.packet.tms282report.PacketTraceRecorder;
import net.swordie.ms.connection.packet.tms282report.Tms282PacketTrace;
import net.swordie.ms.connection.protocol.SessionKind;

import java.util.Objects;

/**
 * Hands captured packets to a {@link PacketTraceRecorder} ({@code logs/tms282-packets.jsonl}, the server GUI's
 * 282 封包 tab), exactly like the probe's {@code Session.trace}: session = evidence folder name, sequence = the
 * NNN of the recv-/send- file. Guard-dropped packets become {@link Tms282PacketTrace.Direction#DROPPED} rows with
 * their own sequence; the hello is not traced (the probe does not trace it either).
 * <p>
 * The data is the already redacted copy, so the JSONL never holds more than the evidence folder; the recorder
 * additionally zeroes {@code SensitiveClientOpcodes} bodies and never throws.
 */
public final class CaptureTraceObserver implements CaptureObserver {
    private final PacketTraceRecorder recorder;
    private final Tms282PacketTrace.SocketKind socket;
    private final String session;

    public CaptureTraceObserver(PacketTraceRecorder recorder, SessionKind kind, String session) {
        this.recorder = Objects.requireNonNull(recorder, "recorder");
        this.socket = Objects.requireNonNull(kind, "kind") == SessionKind.CHANNEL
                ? Tms282PacketTrace.SocketKind.CHANNEL : Tms282PacketTrace.SocketKind.LOGIN;
        this.session = Objects.requireNonNull(session, "session");
    }

    @Override
    public void accept(CapturedPacket packet) {
        Tms282PacketTrace.Direction direction = switch (packet.kind()) {
            case RECV -> Tms282PacketTrace.Direction.RECV;
            case SEND -> Tms282PacketTrace.Direction.SEND;
            case DROPPED -> Tms282PacketTrace.Direction.DROPPED;
            case HELLO -> null;
        };
        if (direction != null && packet.data().length >= 2) {
            recorder.record(direction, socket, session, packet.index(), packet.data());
        }
    }

    @Override
    public String describe() {
        return "packet trace " + session;
    }
}
