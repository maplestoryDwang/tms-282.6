package net.swordie.ms.connection.packet.tms282report;

import java.nio.file.Path;

/**
 * Appends each report as one JSON line ({@link ClientReportJson}) to a file, by default
 * {@link #defaultFile()}. The file work (locks, append) is the shared {@link JsonlFileSink}; readers use
 * {@link JsonlClientReportSource}.
 * <p>
 * This sink exists only because the 282 login probe and the server GUI run in separate processes.
 * Once 282 login runs inside the server, replace it with an in-memory {@link ClientReportSink};
 * the parsers do not change.
 */
public final class JsonlClientReportSink implements ClientReportSink {
    public static final String DEFAULT_FILE_NAME = "tms282-client-reports.jsonl";
    /** System property that overrides {@link #defaultFile()}. */
    public static final String FILE_PROPERTY = "tms.clientReportFile";
    private final JsonlFileSink<Tms282ClientReport> delegate;

    public JsonlClientReportSink(Path file) {
        this.delegate = new JsonlFileSink<>(file, ClientReportJson.CODEC);
    }

    /**
     * The report file shared by the login probe (writer) and the server GUI (reader), always absolute:
     * the system property {@value #FILE_PROPERTY} when set, otherwise
     * {@code <server root>/logs/}{@value #DEFAULT_FILE_NAME} ({@link Tms282LogFiles#logFile}).
     */
    public static Path defaultFile() {
        return Tms282LogFiles.logFile(FILE_PROPERTY, DEFAULT_FILE_NAME);
    }

    public Path file() {
        return delegate.file();
    }

    @Override
    public void accept(Tms282ClientReport report) {
        delegate.append(report);
    }
}
