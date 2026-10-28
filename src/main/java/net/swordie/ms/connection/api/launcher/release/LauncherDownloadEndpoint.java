package net.swordie.ms.connection.api.launcher.release;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherHttp;
import net.swordie.ms.connection.api.launcher.LauncherRateLimits;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LauncherServices;
import net.swordie.ms.console.config.ServerRuntimeConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code GET /api/v2/launcher/download/<file>} (P8b): the launcher exe registered in release.json, for the launcher's
 * 「下載並更新」 (docs/launcher-api-update.md section 8). Runs behind HttpAuthApi's guard (api.http.allow, Host, Origin:
 * 403), then in this order:
 * <ol>
 *     <li>GET only (405);</li>
 *     <li>{@code X-Cat-Launcher} required (400 {@code launcherRequired});</li>
 *     <li>one token of the per-IP {@link LauncherRateLimits#IP} bucket shared with every launcher API (429 +
 *     Retry-After);</li>
 *     <li>{@code <file>} URL-decoded once must follow the file name rule, must not be a Windows device name and must be
 *     exactly the verified registered {@code download.file} ({@link LauncherRelease#served}); anything else 404 with no
 *     reason;</li>
 *     <li>{@code Range: bytes=<start>-} or {@code bytes=<start>-<end>} -> 206 + Content-Range; start beyond the file
 *     or a malformed range -> 416 + {@code Content-Range: bytes *}{@code /<size>}; several ranges -> the whole file;</li>
 *     <li>at most {@value DownloadSlots#PER_IP} running download per source address and {@value DownloadSlots#TOTAL}
 *     in all (429 + {@code Retry-After: 5}); the slot is given back when the transfer ends in any way.</li>
 * </ol>
 * The file is streamed in {@value #CHUNK}-byte pieces (never read into memory), not compressed, never redirected; no
 * session is needed (the launcher checks the Ed25519 signature, size and SHA-256). A transfer that makes no progress
 * for {@value #IDLE_MS} ms (client stopped reading) is ended so its worker and slot come back. Optional overall
 * bandwidth cap: {@value #MAX_KBPS_KEY} (KB/s, -D first, then server.properties; 0 or unset = none).
 * One log line per request: source, file name (only a valid one), status, range start, bytes sent, time, launcher
 * version - never a path or another header.
 */
public final class LauncherDownloadEndpoint implements HttpHandler {
    public static final String PATH = "/api/v2/launcher/download/";
    public static final String MAX_KBPS_KEY = "launcher.download.maxKBps";
    static final int CHUNK = 64 * 1024;
    static final long IDLE_MS = 60_000;
    static final int BUSY_RETRY_SEC = 5;
    private static final Pattern RANGE = Pattern.compile("(?i)bytes=(\\d{1,15})-(\\d{0,15})");

    private final Supplier<LauncherRelease> release;
    private final LauncherServices services;
    private final DownloadSlots slots;
    private final Bandwidth bandwidth = new Bandwidth();
    private final Set<Transfer> transfers = ConcurrentHashMap.newKeySet();
    private final long idleMs;
    private volatile ScheduledExecutorService watchdog;

    public LauncherDownloadEndpoint(Supplier<LauncherRelease> release, LauncherServices services, DownloadSlots slots) {
        this(release, services, slots, IDLE_MS);
    }

    /** {@code idleMs}: tests shorten the stalled-transfer limit. */
    public LauncherDownloadEndpoint(Supplier<LauncherRelease> release, LauncherServices services, DownloadSlots slots, long idleMs) {
        this.release = Objects.requireNonNull(release, "release");
        this.services = Objects.requireNonNull(services, "services");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.idleMs = idleMs;
    }

    public DownloadSlots slots() {
        return slots;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        long t0 = System.nanoTime();
        String ip = LauncherHttp.sourceIp(ex);
        String version = LauncherHttp.launcherVersion(ex.getRequestHeaders().getFirst(LauncherHttp.LAUNCHER_HEADER));
        String name = "-";
        String status = "-";
        long start = -1;
        long sent = 0;
        try {
            if (!"GET".equals(ex.getRequestMethod())) {
                status = "405";
                LauncherHttp.status(ex, 405);
                return;
            }
            if (version == null) {
                status = "400";
                LauncherHttp.respond(ex, 400, LauncherResult.fail(LauncherCodes.LAUNCHER_REQUIRED));
                return;
            }
            if (!services.rateLimits().tryAcquire(LauncherRateLimits.IP, ip)) {
                long sec = Math.max(1, Math.min(10, (services.rateLimits().retryAfterMs(LauncherRateLimits.IP, ip) + 999) / 1000));
                status = "429";
                busy(ex, sec);
                return;
            }
            String path = ex.getRequestURI().getRawPath();
            String requested = path == null || !path.startsWith(PATH) ? null : decodeOnce(path.substring(PATH.length()));
            name = ReleaseRules.isSafeFileName(requested) ? requested : "(invalid name)";
            LauncherRelease.Served served = requested == null ? null : release.get().served(requested).orElse(null);
            if (served == null) {
                status = "404";
                LauncherHttp.status(ex, 404);
                return;
            }
            long size = served.size();
            long from = 0;
            long to = size - 1;
            boolean partial = false;
            List<String> ranges = ex.getRequestHeaders().get("Range");
            if (ranges != null && ranges.size() == 1 && !ranges.get(0).contains(",")) {
                Matcher m = RANGE.matcher(ranges.get(0).trim());
                long[] r = m.matches() ? range(m, size) : null;
                if (r == null) {
                    status = "416";
                    ex.getResponseHeaders().set("Content-Range", "bytes */" + size);
                    LauncherHttp.status(ex, 416);
                    return;
                }
                from = r[0];
                to = r[1];
                partial = true;
            }
            start = from;
            DownloadSlots.Slot slot = slots.tryAcquire(ip, served.file(), to - from + 1);
            if (slot == null) {
                status = "429";
                busy(ex, BUSY_RETRY_SEC);
                return;
            }
            try (slot; FileChannel ch = FileChannel.open(served.path(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                if (ch.size() != size) {
                    // replaced since it was checked: the next snapshot re-verifies it
                    release.get().source().invalidate();
                    status = "404";
                    LauncherHttp.status(ex, 404);
                    return;
                }
                long length = to - from + 1;
                var h = ex.getResponseHeaders();
                h.set("Content-Type", "application/octet-stream");
                h.set("Accept-Ranges", "bytes");
                h.set("Cache-Control", "no-store");
                h.set("X-Content-Type-Options", "nosniff");
                if (partial) {
                    h.set("Content-Range", "bytes " + from + "-" + to + "/" + size);
                }
                int code = partial ? 206 : 200;
                status = String.valueOf(code);
                ex.sendResponseHeaders(code, length);
                Transfer t = new Transfer(Thread.currentThread());
                transfers.add(t);
                ensureWatchdog();
                try {
                    sent = send(ch, from, length, ex.getResponseBody(), slot, t);
                } catch (IOException e) {
                    sent = t.sent;
                    status = code + (t.stalled ? " stalled" : " aborted");
                } finally {
                    t.finish();
                    transfers.remove(t);
                    Thread.interrupted();
                }
            }
        } catch (IOException e) {
            if (status.equals("-")) {
                status = "error";
            }
        } finally {
            System.out.printf("[HttpAuthApi] download %s %s -> %s from=%d sent=%d %d ms v=%s%n", ip, name, status, start, sent,
                    (System.nanoTime() - t0) / 1_000_000, LauncherHttp.logVersion(version));
        }
    }

    /** Streams {@code length} bytes from {@code from}; returns the bytes written. */
    private long send(FileChannel ch, long from, long length, OutputStream out, DownloadSlots.Slot slot, Transfer t) throws IOException {
        byte[] arr = new byte[CHUNK];
        ByteBuffer buf = ByteBuffer.wrap(arr);
        long pos = from;
        long left = length;
        long kbps = maxKBps();
        while (left > 0) {
            buf.clear();
            buf.limit((int) Math.min(CHUNK, left));
            int n = ch.read(buf, pos);
            if (n <= 0) {
                throw new IOException("file shrank");
            }
            if (kbps > 0) {
                bandwidth.pace(n, kbps);
            }
            out.write(arr, 0, n);
            pos += n;
            left -= n;
            t.progress(n);
            slot.sent(n);
        }
        out.close();
        return t.sent;
    }

    /** {from, to} of a single range, or null (start at or past the end, end before start). */
    static long[] range(Matcher m, long size) {
        long from = Long.parseLong(m.group(1));
        if (from >= size) {
            return null;
        }
        long to = size - 1;
        if (!m.group(2).isEmpty()) {
            long end = Long.parseLong(m.group(2));
            if (end < from) {
                return null;
            }
            to = Math.min(end, size - 1);
        }
        return new long[]{from, to};
    }

    /** Percent-decodes once (no '+' rule: this is a path), strict UTF-8; null when malformed. */
    static String decodeOnce(String raw) {
        if (raw == null || raw.length() > 400) {
            return null;
        }
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%') {
                if (i + 2 >= raw.length()) {
                    return null;
                }
                int hi = Character.digit(raw.charAt(i + 1), 16);
                int lo = Character.digit(raw.charAt(i + 2), 16);
                if (hi < 0 || lo < 0) {
                    return null;
                }
                b.write(hi * 16 + lo);
                i += 2;
            } else if (c < 0x80) {
                b.write(c);
            } else {
                return null;
            }
        }
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(b.toByteArray())).toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static void busy(HttpExchange ex, long sec) throws IOException {
        ex.getResponseHeaders().set("Retry-After", String.valueOf(sec));
        LauncherHttp.respond(ex, 429, LauncherResult.fail(LauncherCodes.RATE_LIMITED).with("retryAfterSec", sec));
    }

    static long maxKBps() {
        String raw = System.getProperty(MAX_KBPS_KEY);
        if (raw == null || raw.isBlank()) {
            raw = ServerRuntimeConfig.stringValue(MAX_KBPS_KEY);
        }
        try {
            long v = Long.parseLong(raw == null ? "" : raw.trim());
            return v > 0 && v <= 10_000_000 ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void ensureWatchdog() {
        if (watchdog != null) {
            return;
        }
        synchronized (this) {
            if (watchdog == null) {
                ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread th = new Thread(r, "LauncherDownload-watchdog");
                    th.setDaemon(true);
                    return th;
                });
                long every = Math.max(200, Math.min(5_000, idleMs / 4));
                s.scheduleWithFixedDelay(this::sweep, every, every, TimeUnit.MILLISECONDS);
                watchdog = s;
            }
        }
    }

    /** Ends transfers that made no progress for {@code idleMs}: interrupting the worker closes its socket channel. */
    void sweep() {
        long now = System.nanoTime();
        for (Transfer t : transfers) {
            t.stopIfIdle(now, idleMs);
        }
    }

    /** One running transfer, as the watchdog sees it. */
    static final class Transfer {
        private final Thread worker;
        private volatile long last = System.nanoTime();
        private volatile long sent;
        private volatile boolean stalled;
        private boolean done;

        Transfer(Thread worker) {
            this.worker = worker;
        }

        void progress(int n) {
            sent += n;
            last = System.nanoTime();
        }

        synchronized void finish() {
            done = true;
        }

        /** Bytes written so far (the client file endpoint's log line). */
        long sentBytes() {
            return sent;
        }

        synchronized void stopIfIdle(long now, long idleMs) {
            if (!done && now - last > idleMs * 1_000_000L) {
                stalled = true;
                worker.interrupt();
            }
        }
    }

    /** Overall bandwidth cap shared by every transfer (simple pacing: each chunk waits for its share of time). */
    static final class Bandwidth {
        private long nextFree = System.nanoTime();

        void pace(int bytes, long kbps) throws IOException {
            long wait;
            synchronized (this) {
                long now = System.nanoTime();
                long cost = bytes * 1_000_000_000L / (kbps * 1024);
                nextFree = Math.max(nextFree, now - 200_000_000L) + cost;
                wait = nextFree - now;
            }
            if (wait > 0) {
                try {
                    TimeUnit.NANOSECONDS.sleep(wait);
                } catch (InterruptedException e) {
                    throw new IOException("interrupted");
                }
            }
        }
    }
}
