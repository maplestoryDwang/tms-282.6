package net.swordie.ms.connection.packet.tms282report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tails a JSONL file written by a {@link JsonlFileSink} (reader side for the server GUI). Shared base of
 * {@link JsonlClientReportSource} and {@link JsonlPacketTraceSource}.
 * <p>
 * Only complete lines (ending in '\n') are consumed; a line still being written is read on a
 * later call. A missing file reads as empty. A file that shrank, or that was replaced by another
 * file, is re-read from the start: the reader remembers the file key (null on Windows), the creation
 * time and the first {@value #HEAD_BYTES} bytes, and any of them changing means a different file.
 * The head comparison also catches a replacement that Windows gives the old creation time
 * (file-system tunneling). A read that collides with the writer's lock returns nothing and consumes
 * nothing, so the next call retries; if that happens to {@link #readLast}, the following {@link #poll}
 * repeats the limited initial read instead of returning the whole file. Malformed lines are skipped
 * and counted ({@link #skippedLines()}); only the kept lines are decoded.
 */
public final class JsonlFileSource<T> {
    private static final int CHUNK = 64 * 1024;
    static final int HEAD_BYTES = 64;
    private final Path file;
    private final JsonlCodec<T> codec;
    private final ReentrantLock lock = new ReentrantLock();
    private long position;
    private FileIdentity identity;
    /** Limit of a {@link #readLast} that could not read the file yet; 0 when none is pending. */
    private int pendingInitial;
    private long skippedLines;
    private long droppedLines;

    public JsonlFileSource(Path file, JsonlCodec<T> codec) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /** Absolute path of the tailed file. */
    public Path file() {
        return file;
    }

    /** Reads everything up to now, keeps only the last {@code max} values, and starts tailing from the end. */
    public List<T> readLast(int max) {
        requirePositive(max);
        lock.lock();
        try {
            return initialRead(max);
        } finally {
            lock.unlock();
        }
    }

    /** Every value appended since the previous call; see {@link #poll(int)}. */
    public List<T> poll() {
        return poll(Integer.MAX_VALUE);
    }

    /**
     * Values appended since the previous call, at most the newest {@code max} (older new lines are consumed and
     * counted in {@link #droppedLines()}, so a burst never costs more than {@code max} decodes). While an initial
     * {@link #readLast} is still pending (its read failed), this repeats that read with the same limit.
     */
    public List<T> poll(int max) {
        requirePositive(max);
        lock.lock();
        try {
            if (pendingInitial > 0) {
                return initialRead(pendingInitial);
            }
            List<T> values = read(max);
            return values == null ? List.of() : values;
        } finally {
            lock.unlock();
        }
    }

    /** True while a {@link #readLast} has not been able to read the file yet. */
    public boolean initialReadPending() {
        lock.lock();
        try {
            return pendingInitial > 0;
        } finally {
            lock.unlock();
        }
    }

    /** Lines that could not be decoded. */
    public long skippedLines() {
        lock.lock();
        try {
            return skippedLines;
        } finally {
            lock.unlock();
        }
    }

    /** Complete lines consumed by {@link #poll(int)} but not decoded because they exceeded its limit. */
    public long droppedLines() {
        lock.lock();
        try {
            return droppedLines;
        } finally {
            lock.unlock();
        }
    }

    private static void requirePositive(int max) {
        if (max <= 0) {
            throw new IllegalArgumentException("max must be positive");
        }
    }

    private List<T> initialRead(int max) {
        pendingInitial = max;
        position = 0;
        identity = null;
        long dropped = droppedLines;
        List<T> values = read(max);
        droppedLines = dropped; // Older history skipped by readLast is intentional, not a dropped burst.
        if (values == null) {
            return List.of();
        }
        pendingInitial = 0;
        return values;
    }

    /** Null when the file could not be read; nothing is consumed then. */
    private List<T> read(int keepLast) {
        if (!Files.isRegularFile(file)) {
            position = 0;
            identity = null;
            return List.of();
        }
        ArrayDeque<String> lines = new ArrayDeque<>();
        long dropped = 0;
        try (SeekableByteChannel channel = Files.newByteChannel(file)) {
            long size = channel.size();
            FileIdentity current = FileIdentity.of(file, channel, size);
            if (size < position || identity != null && !identity.sameFileAs(current)) {
                position = 0;
            }
            if (size == position) {
                identity = current;
                return List.of();
            }
            channel.position(position);
            ByteArrayOutputStream line = new ByteArrayOutputStream();
            ByteBuffer buffer = ByteBuffer.allocate(CHUNK);
            long offset = position, consumed = position;
            while (offset < size && channel.read(buffer.clear()) > 0) {
                buffer.flip();
                while (buffer.hasRemaining() && offset < size) {
                    byte b = buffer.get();
                    offset++;
                    if (b == '\n') {
                        lines.addLast(line.toString(StandardCharsets.UTF_8).strip());
                        line.reset();
                        consumed = offset;
                        if (lines.size() > keepLast) {
                            lines.removeFirst();
                            dropped++;
                        }
                    } else {
                        line.write(b);
                    }
                }
            }
            position = consumed;
            identity = current;
        } catch (IOException e) {
            // Usually the writer's lock; nothing is consumed, so the next call retries.
            return null;
        }
        droppedLines += dropped;
        List<T> values = new ArrayList<>(lines.size());
        for (String text : lines) {
            if (text.isEmpty()) {
                continue;
            }
            try {
                values.add(codec.fromJson(text));
            } catch (IllegalArgumentException e) {
                skippedLines++;
            }
        }
        return values;
    }

    /** What identifies one file across polls: file key (if the platform has one), creation time and head bytes. */
    private record FileIdentity(Object key, FileTime created, byte[] head) {
        static FileIdentity of(Path file, SeekableByteChannel channel, long size) throws IOException {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            ByteBuffer head = ByteBuffer.allocate((int) Math.min(HEAD_BYTES, size));
            channel.position(0);
            while (head.hasRemaining() && channel.read(head) > 0) {
                // Fill the head buffer.
            }
            return new FileIdentity(attributes.fileKey(), attributes.creationTime(),
                    Arrays.copyOf(head.array(), head.position()));
        }

        /** An appended-to file keeps its key and creation time, and its old head is a prefix of the new one. */
        boolean sameFileAs(FileIdentity current) {
            return Objects.equals(key, current.key) && Objects.equals(created, current.created)
                    && current.head.length >= head.length
                    && Arrays.equals(head, 0, head.length, current.head, 0, head.length);
        }
    }
}
