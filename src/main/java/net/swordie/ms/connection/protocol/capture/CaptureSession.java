package net.swordie.ms.connection.protocol.capture;

import net.swordie.ms.connection.protocol.SessionKind;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Evidence folder of one socket, in the LoginProtocolProbe layout so the research tools read both:
 * {@code <root>/sessions/<prefix>[channel-]<millis>-<seq>-<random>/} holding
 * <ul>
 *     <li>{@code recv-NNN-OOOO.bin} (decrypted packet, opcode included) and {@code recv-NNN.aux} for an auxiliary
 *     cursor, or {@code recv-NNN-OOOO-redacted.bin} (opcode, then zeros of the same length, no .aux) for opcodes
 *     {@link CaptureRedaction} protects;</li>
 *     <li>{@code send-NNN-OOOO.bin} and {@code send-NNN.aux} for packets that reached the encoder (the main part,
 *     like the probe's {@code packet.getData()}), {@code send-NNN-OOOO-redacted.bin} when {@link SendSecretScan}
 *     found a secret in it;</li>
 *     <li>{@code hello.bin}, the plaintext handshake.</li>
 * </ul>
 * Packets refused by the OutboundGuard are never written here (the packet trace lists them as DROPPED).
 * The {@code prefix} (config {@code capture.sessionPrefix}, e.g. {@code formal-}) tells formal-server folders
 * apart from the probe's in the same {@code sessions} folder. {@link PacketCapture} calls {@link #accept} from
 * one writer thread at a time.
 */
public final class CaptureSession implements CaptureObserver {
    private static final AtomicLong SESSION_SEQUENCE = new AtomicLong();

    private final Path directory;
    private final CaptureRedaction redaction;

    private CaptureSession(Path directory, CaptureRedaction redaction) {
        this.directory = directory;
        this.redaction = redaction;
    }

    /** Creates a new, never reused session folder under {@code root/sessions}, without prefix. */
    public static CaptureSession open(Path root, SessionKind kind, CaptureRedaction redaction) throws IOException {
        return open(root, "", kind, redaction);
    }

    /** Creates a new, never reused session folder {@code root/sessions/<prefix>[channel-]<millis>-<seq>-<random>}. */
    public static CaptureSession open(Path root, String prefix, SessionKind kind, CaptureRedaction redaction)
            throws IOException {
        Objects.requireNonNull(redaction, "redaction");
        Path sessions = Files.createDirectories(root.resolve("sessions"));
        return new CaptureSession(Files.createTempDirectory(sessions, folderPrefix(prefix, kind)), redaction);
    }

    /** {@code <prefix>[channel-]<millis>-<seq>-}: the folder name before createTempDirectory's random suffix. */
    private static String folderPrefix(String prefix, SessionKind kind) {
        return (prefix == null ? "" : prefix.trim()) + (kind == SessionKind.CHANNEL ? "channel-" : "")
                + System.currentTimeMillis() + "-" + SESSION_SEQUENCE.incrementAndGet() + "-";
    }

    public Path directory() {
        return directory;
    }

    /** Folder name, the session id of the packet trace (as in the probe). */
    public String name() {
        return directory.getFileName().toString();
    }

    public CaptureRedaction redaction() {
        return redaction;
    }

    /** Writes the files of one packet ({@code packet.data()} is already redacted when it must be). */
    @Override
    public void accept(CapturedPacket packet) throws IOException {
        switch (packet.kind()) {
            case HELLO -> Files.write(directory.resolve("hello.bin"), packet.data());
            case RECV -> write("recv", packet);
            case SEND -> write("send", packet);
            case DROPPED -> {
            }
        }
    }

    private void write(String direction, CapturedPacket packet) throws IOException {
        int i = packet.index();
        if (packet.redacted()) {
            Files.write(directory.resolve(String.format("%s-%03d-%04x-redacted.bin", direction, i, packet.opcode())), packet.data());
            return;
        }
        if (packet.hasAuxiliary()) {
            Files.write(directory.resolve(String.format("%s-%03d.aux", direction, i)), packet.auxiliary());
        }
        Files.write(directory.resolve(String.format("%s-%03d-%04x.bin", direction, i, packet.opcode())), packet.data());
    }

    @Override
    public String describe() {
        return "files " + directory;
    }
}
