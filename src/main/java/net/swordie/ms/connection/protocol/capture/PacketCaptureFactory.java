package net.swordie.ms.connection.protocol.capture;

import net.swordie.ms.connection.packet.tms282report.PacketTraceRecorder;
import net.swordie.ms.connection.protocol.ProtocolConfig;
import net.swordie.ms.connection.protocol.ProtocolProfile;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.connection.protocol.SessionKind;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Builds the {@link PacketCapture} of a new socket from the {@code capture.*} settings (factory; used by
 * ProtocolChannelInitializer): a {@link CaptureSession} folder when {@code capture.recv} or {@code capture.send}
 * is on, plus a {@link CaptureTraceObserver} when {@code capture.trace} is on. Returns null when nothing is
 * captured (the default, so the 265 pipeline stays as it was) or the folder cannot be created (logged; the socket
 * runs uncaptured).
 */
public final class PacketCaptureFactory {
    private static final class StandardTrace {
        static final PacketTraceRecorder RECORDER = PacketTraceRecorder.standard();
    }

    private final ProtocolConfig config;
    private final ProtocolProfile profile;
    private final Supplier<PacketTraceRecorder> trace;
    private final Executor executor;

    /**
     * @param trace    recorder for {@code capture.trace} (asked once per socket, only when tracing)
     * @param executor where files and trace lines are written
     */
    public PacketCaptureFactory(ProtocolConfig config, ProtocolProfile profile, Supplier<PacketTraceRecorder> trace,
                                Executor executor) {
        this.config = Objects.requireNonNull(config, "config");
        this.profile = Objects.requireNonNull(profile, "profile");
        this.trace = Objects.requireNonNull(trace, "trace");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /** {@link PacketTraceRecorder#standard()} (logs/tms282-packets.jsonl) and the shared {@link CaptureWriter}. */
    public static PacketCaptureFactory standard(ProtocolConfig config, ProtocolProfile profile) {
        return new PacketCaptureFactory(config, profile, () -> StandardTrace.RECORDER, CaptureWriter.shared());
    }

    public ProtocolConfig config() {
        return config;
    }

    /**
     * The capture of a new socket, or null when none is configured or it cannot be opened.
     *
     * @param sendScan secret search for sent packets (the socket's account)
     */
    public PacketCapture open(SessionKind kind, SendSecretScan sendScan) {
        if (!config.captureAny()) {
            return null;
        }
        CaptureRedaction redaction = CaptureRedaction.forProfile(profile);
        try {
            List<CaptureObserver> observers = new ArrayList<>(2);
            CaptureSession files = CaptureSession.open(config.captureRoot(), config.captureSessionPrefix(), kind, redaction);
            observers.add(files);
            if (config.captureTrace()) {
                observers.add(new CaptureTraceObserver(trace.get(), kind, files.name()));
            }
            SendSecretScan profileSendScan = profile.version() == ProtocolVersion.TMS_282_3
                    ? (main, aux) -> carriesReloginCredential(main) || sendScan.leaks(main, aux) : sendScan;
            return new PacketCapture(files.name(), redaction, profileSendScan, observers, executor);
        } catch (IOException | RuntimeException | LinkageError e) {
            System.out.println("[Capture] cannot open a session under " + config.captureRoot() + ": " + e);
            return null;
        }
    }

    /** Server-issued cookies and the accepted switch's typed PIC are not present in User's stored hashes. */
    private static boolean carriesReloginCredential(byte[] packet) {
        if (packet.length < 2) return false;
        int opcode = (packet[0] & 255) | (packet[1] & 255) << 8;
        return opcode == 0x015F || opcode == 0x028D && packet.length > 2 && packet[2] == 0;
    }
}
