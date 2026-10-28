package net.swordie.ms.connection.packet.tms282report;

import java.nio.file.Path;
import java.util.List;

/**
 * Tails the JSONL file written by {@link JsonlClientReportSink} (reader side for the server GUI).
 * Tail semantics (partial lines, truncation and replacement detection, lock collisions, pending
 * {@link #readLast}) are those of the shared {@link JsonlFileSource}.
 * <p>
 * Needed only while the 282 login probe and the GUI are separate processes; an in-process
 * {@link ClientReportSink} replaces the file pair later without touching the parsers.
 */
public final class JsonlClientReportSource {
    private final JsonlFileSource<Tms282ClientReport> delegate;

    public JsonlClientReportSource(Path file) {
        this.delegate = new JsonlFileSource<>(file, ClientReportJson.CODEC);
    }

    /** Absolute path of the tailed file. */
    public Path file() {
        return delegate.file();
    }

    /** Reads everything up to now, keeps only the last {@code max} reports, and starts tailing from the end. */
    public List<Tms282ClientReport> readLast(int max) {
        return delegate.readLast(max);
    }

    /**
     * Reports appended since the previous call. While an initial {@link #readLast} is still pending
     * (its read failed), this repeats that read with the same limit.
     */
    public List<Tms282ClientReport> poll() {
        return delegate.poll();
    }

    /** True while a {@link #readLast} has not been able to read the file yet. */
    public boolean initialReadPending() {
        return delegate.initialReadPending();
    }

    public long skippedLines() {
        return delegate.skippedLines();
    }
}
