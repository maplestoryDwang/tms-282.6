package net.swordie.ms.connection.packet.tms282report;

import java.nio.file.Path;
import java.util.List;

/**
 * Tails the JSONL file written by {@link JsonlPacketTraceSink} (reader side of the server GUI's 282 封包 tab).
 * Tail semantics are those of the shared {@link JsonlFileSource}; {@link #poll(int)} bounds a burst so the GUI
 * never decodes more rows than it can show.
 */
public final class JsonlPacketTraceSource {
    private final JsonlFileSource<Tms282PacketTrace> delegate;

    public JsonlPacketTraceSource(Path file) {
        this.delegate = new JsonlFileSource<>(file, PacketTraceJson.CODEC);
    }

    /** Absolute path of the tailed file. */
    public Path file() {
        return delegate.file();
    }

    /** Reads everything up to now, keeps only the last {@code max} traces, and starts tailing from the end. */
    public List<Tms282PacketTrace> readLast(int max) {
        return delegate.readLast(max);
    }

    /** Traces appended since the previous call, at most the newest {@code max}. */
    public List<Tms282PacketTrace> poll(int max) {
        return delegate.poll(max);
    }

    public boolean initialReadPending() {
        return delegate.initialReadPending();
    }

    /** Lines that could not be decoded (malformed, or refused by {@link Tms282PacketTrace}). */
    public long skippedLines() {
        return delegate.skippedLines();
    }

    /** New lines skipped because one {@link #poll(int)} brought more than its limit. */
    public long droppedLines() {
        return delegate.droppedLines();
    }
}
