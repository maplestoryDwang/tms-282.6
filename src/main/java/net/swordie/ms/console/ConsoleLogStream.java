package net.swordie.ms.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class ConsoleLogStream extends OutputStream {
    private final ConsoleLogAppender appender;
    private final boolean error;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);

    public ConsoleLogStream(ConsoleLogAppender appender, boolean error) {
        this.appender = appender;
        this.error = error;
    }

    @Override
    public synchronized void write(int b) throws IOException {
        if (b == '\r') {
            return;
        }
        if (b == '\n') {
            flushLine();
            return;
        }
        buffer.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = off; i < off + len; i++) {
            write(b[i]);
        }
    }

    @Override
    public synchronized void flush() {
        // PrintStream 的 autoFlush 會頻繁觸發 flush；這裏不強制輸出半行，避免啓動日誌被切碎。
    }

    @Override
    public synchronized void close() {
        flushLine();
    }

    private void flushLine() {
        if (buffer.size() == 0) {
            return;
        }
        String line = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        if (PacketLogBridge.acceptConsoleLine(line)) {
            return;
        }
        appender.append(line, error);
    }
}