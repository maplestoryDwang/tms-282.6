package net.swordie.ms.connection.packet.tms282report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Appends each value as one JSON line ({@link JsonlCodec}) to a file. Shared base of
 * {@link JsonlClientReportSink} and {@link JsonlPacketTraceSink}; readers use {@link JsonlFileSource}.
 * <p>
 * Thread-safe within a process (one {@link ReentrantLock} per file path, shared by every sink of that file,
 * which does not pin virtual threads) and between processes (an exclusive {@link FileLock} around each append),
 * so several probe listeners can share the file and a reader never sees two lines interleaved. The lock is per
 * path, not per sink, because a JVM holds file locks per process: two sinks locking the same file at once would
 * otherwise fail with {@link java.nio.channels.OverlappingFileLockException}.
 */
public final class JsonlFileSink<T> {
    private final Path file;
    private final JsonlCodec<T> codec;
    private static final ConcurrentMap<Path, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private final ReentrantLock lock;

    public JsonlFileSink(Path file, JsonlCodec<T> codec) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
        this.codec = Objects.requireNonNull(codec, "codec");
        this.lock = LOCKS.computeIfAbsent(this.file, ignored -> new ReentrantLock());
    }

    public Path file() {
        return file;
    }

    /** Throws {@link UncheckedIOException} when the line cannot be appended. */
    public void append(T value) {
        byte[] line = (codec.toJson(value) + "\n").getBytes(StandardCharsets.UTF_8);
        lock.lock();
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                 FileLock ignored = channel.lock()) {
                ByteBuffer buffer = ByteBuffer.wrap(line);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot append JSON line to " + file, e);
        } finally {
            lock.unlock();
        }
    }
}
