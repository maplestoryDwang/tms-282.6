package net.swordie.ms.connection.packet.tms282report;

import java.nio.file.Path;

/**
 * Appends each packet trace as one JSON line ({@link PacketTraceJson}) to a file, by default
 * {@link #defaultFile()}. The file work (locks, append) is the shared {@link JsonlFileSink}; the GUI reads the
 * file with {@link JsonlPacketTraceSource}.
 */
public final class JsonlPacketTraceSink implements PacketTraceSink {
    public static final String DEFAULT_FILE_NAME = "tms282-packets.jsonl";
    /** System property that overrides {@link #defaultFile()}. */
    public static final String FILE_PROPERTY = "tms.packetTraceFile";
    private final JsonlFileSink<Tms282PacketTrace> delegate;

    public JsonlPacketTraceSink(Path file) {
        this.delegate = new JsonlFileSink<>(file, PacketTraceJson.CODEC);
    }

    /**
     * The trace file shared by the login probe (writer) and the server GUI (reader), always absolute:
     * the system property {@value #FILE_PROPERTY} when set, otherwise
     * {@code <server root>/logs/}{@value #DEFAULT_FILE_NAME}. Same resolution as
     * {@link JsonlClientReportSink#defaultFile()} ({@link Tms282LogFiles#logFile}).
     */
    public static Path defaultFile() {
        return Tms282LogFiles.logFile(FILE_PROPERTY, DEFAULT_FILE_NAME);
    }

    public Path file() {
        return delegate.file();
    }

    @Override
    public void accept(Tms282PacketTrace trace) {
        delegate.append(trace);
    }
}
