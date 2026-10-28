package net.swordie.ms.connection.api.launcher.release;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherHttp;
import net.swordie.ms.connection.api.launcher.LauncherRateLimits;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LauncherServices;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
 * {@code GET /api/v2/launcher/client/<version>/<path>} (cat-launcher 1.2.3): one file of the published client file set
 * ({@link LauncherClientFiles}) for the launcher's 「下載並覆蓋」 (docs/launcher-api-update.md section 14). Runs behind
 * HttpAuthApi's guard (api.http.allow, Host, Origin: 403), then the {@link #GUARDS} in order - each a small
 * {@link Guard} that either answers (and the request ends) or lets the next one run:
 * <ol>
 *     <li>{@link #METHOD}: GET only (405);</li>
 *     <li>{@link #LAUNCHER}: {@code X-Cat-Launcher} required (400 {@code launcherRequired});</li>
 *     <li>{@link #RATE}: one token of the per-IP {@link LauncherRateLimits#CLIENT_FILE} bucket (429 + Retry-After) - its
 *     own bucket, not the login-shared IP one, because a set can be hundreds of files;</li>
 *     <li>{@link #RESOLVE}: the rest of the path URL-decoded once must be {@code <x.y.z>/<path>} where the version is
 *     the published one and the path is exactly (case-sensitive) one listed in the signed list
 *     ({@link ClientFileRules#isSafePath}: no {@code ..}, no backslash, no drive, no device name); anything else 404
 *     with no reason;</li>
 *     <li>{@link #RANGE}: {@code Range: bytes=<start>-[<end>]} -> 206 + Content-Range; past the end or malformed -> 416
 *     + {@code Content-Range: bytes *}{@code /<size>}; several ranges -> the whole file;</li>
 *     <li>{@link #SLOT}: at most 1 running transfer per address and 4 in all ({@link DownloadSlots#client()}; 429 +
 *     {@code Retry-After: 5}); given back however the transfer ends.</li>
 * </ol>
 * The file is streamed in {@value LauncherDownloadEndpoint#CHUNK}-byte pieces (never held in memory), not compressed, not
 * redirected, with the same optional bandwidth cap as the exe download ({@value LauncherDownloadEndpoint#MAX_KBPS_KEY}). A
 * transfer that stops moving for {@code idleMs} is ended so its worker and slot come back. One log line per request:
 * source, version and relative path (only a valid one), status, range start, bytes sent, time, launcher version - never
 * a host path or another header.
 */
public final class ClientFileEndpoint implements HttpHandler {
    public static final String PATH = "/api/v2/launcher/client/";
    static final long IDLE_MS = 60_000;
    private static final Pattern RANGE_RE = Pattern.compile("(?i)bytes=(\\d{1,15})-(\\d{0,15})");

    private final Supplier<LauncherClientFiles> files;
    private final LauncherServices services;
    private final DownloadSlots slots;
    private final long idleMs;
    private final LauncherDownloadEndpoint.Bandwidth bandwidth = new LauncherDownloadEndpoint.Bandwidth();
    private final Set<LauncherDownloadEndpoint.Transfer> transfers = ConcurrentHashMap.newKeySet();
    private volatile ScheduledExecutorService watchdog;

    /** The state one request carries through the guards. */
    static final class Request {
        final HttpExchange ex;
        final String ip;
        final String version;
        String logName = "-";
        String status = "-";
        LauncherClientFiles.Served served;
        long from;
        long to;
        boolean partial;
        DownloadSlots.Slot slot;

        Request(HttpExchange ex, String ip, String version) {
            this.ex = ex;
            this.ip = ip;
            this.version = version;
        }
    }

    /** One step of the pipeline: true = answered (stop), false = go on. */
    @FunctionalInterface
    interface Guard {
        boolean refuse(ClientFileEndpoint ep, Request r) throws IOException;
    }

    static final Guard METHOD = (ep, r) -> {
        if ("GET".equals(r.ex.getRequestMethod())) {
            return false;
        }
        r.status = "405";
        LauncherHttp.status(r.ex, 405);
        return true;
    };

    static final Guard LAUNCHER = (ep, r) -> {
        if (r.version != null) {
            return false;
        }
        r.status = "400";
        LauncherHttp.respond(r.ex, 400, LauncherResult.fail(LauncherCodes.LAUNCHER_REQUIRED));
        return true;
    };

    static final Guard RATE = (ep, r) -> {
        if (ep.services.rateLimits().tryAcquire(LauncherRateLimits.CLIENT_FILE, r.ip)) {
            return false;
        }
        long sec = Math.max(1, Math.min(10, (ep.services.rateLimits().retryAfterMs(LauncherRateLimits.CLIENT_FILE, r.ip) + 999) / 1000));
        r.status = "429";
        busy(r.ex, sec);
        return true;
    };

    static final Guard RESOLVE = (ep, r) -> {
        String raw = r.ex.getRequestURI().getRawPath();
        String rest = raw == null || !raw.startsWith(PATH) ? null : LauncherDownloadEndpoint.decodeOnce(raw.substring(PATH.length()));
        int slash = rest == null ? -1 : rest.indexOf('/');
        String version = slash > 0 ? rest.substring(0, slash) : null;
        String rel = slash > 0 ? rest.substring(slash + 1) : null;
        boolean valid = ReleaseRules.isVersion(version) && ClientFileRules.isSafePath(rel);
        r.logName = valid ? version + "/" + rel : "(invalid path)";
        r.served = valid ? ep.files.get().served(version, rel).orElse(null) : null;
        if (r.served != null) {
            return false;
        }
        r.status = "404";
        LauncherHttp.status(r.ex, 404);
        return true;
    };

    static final Guard RANGE = (ep, r) -> {
        long size = r.served.size();
        r.from = 0;
        r.to = size - 1;
        List<String> ranges = r.ex.getRequestHeaders().get("Range");
        if (ranges == null || ranges.size() != 1 || ranges.get(0).contains(",")) {
            return false;
        }
        Matcher m = RANGE_RE.matcher(ranges.get(0).trim());
        long[] x = m.matches() ? LauncherDownloadEndpoint.range(m, size) : null;
        if (x == null) {
            r.status = "416";
            r.ex.getResponseHeaders().set("Content-Range", "bytes */" + size);
            LauncherHttp.status(r.ex, 416);
            return true;
        }
        r.from = x[0];
        r.to = x[1];
        r.partial = true;
        return false;
    };

    static final Guard SLOT = (ep, r) -> {
        r.slot = ep.slots.tryAcquire(r.ip, r.logName, Math.max(0, r.to - r.from + 1));
        if (r.slot != null) {
            return false;
        }
        r.status = "429";
        busy(r.ex, LauncherDownloadEndpoint.BUSY_RETRY_SEC);
        return true;
    };

    /** The pipeline (registry order = check order). */
    static final List<Guard> GUARDS = List.of(METHOD, LAUNCHER, RATE, RESOLVE, RANGE, SLOT);

    public ClientFileEndpoint(Supplier<LauncherClientFiles> files, LauncherServices services, DownloadSlots slots) {
        this(files, services, slots, IDLE_MS);
    }

    public ClientFileEndpoint(Supplier<LauncherClientFiles> files, LauncherServices services, DownloadSlots slots, long idleMs) {
        this.files = Objects.requireNonNull(files, "files");
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
        Request r = new Request(ex, LauncherHttp.sourceIp(ex), LauncherHttp.launcherVersion(ex.getRequestHeaders().getFirst(LauncherHttp.LAUNCHER_HEADER)));
        long sent = 0;
        try {
            for (Guard g : GUARDS) {
                if (g.refuse(this, r)) {
                    return;
                }
            }
            sent = stream(r);
        } catch (IOException e) {
            if (r.status.equals("-")) {
                r.status = "error";
            }
        } finally {
            if (r.slot != null) {
                r.slot.close();
            }
            System.out.printf("[HttpAuthApi] client %s %s -> %s from=%d sent=%d %d ms v=%s%n", r.ip, r.logName, r.status, r.served == null ? -1 : r.from, sent,
                    (System.nanoTime() - t0) / 1_000_000, LauncherHttp.logVersion(r.version));
        }
    }

    private long stream(Request r) throws IOException {
        long size = r.served.size();
        try (FileChannel ch = FileChannel.open(r.served.path(), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            if (ch.size() != size) {
                files.get().invalidate();
                r.status = "404";
                LauncherHttp.status(r.ex, 404);
                return 0;
            }
            long length = r.to - r.from + 1;
            var h = r.ex.getResponseHeaders();
            h.set("Content-Type", "application/octet-stream");
            h.set("Accept-Ranges", "bytes");
            h.set("Cache-Control", "no-store");
            h.set("X-Content-Type-Options", "nosniff");
            if (r.partial) {
                h.set("Content-Range", "bytes " + r.from + "-" + r.to + "/" + size);
            }
            int code = r.partial ? 206 : 200;
            r.status = String.valueOf(code);
            // an empty file: Content-Length 0 (-1 would mean "no body" to the JDK server)
            r.ex.sendResponseHeaders(code, size == 0 ? -1 : length);
            if (size == 0) {
                return 0;
            }
            LauncherDownloadEndpoint.Transfer t = new LauncherDownloadEndpoint.Transfer(Thread.currentThread());
            transfers.add(t);
            ensureWatchdog();
            try {
                return send(ch, r.from, length, r.ex.getResponseBody(), r.slot, t);
            } catch (IOException e) {
                r.status = code + " aborted";
                return t.sentBytes();
            } finally {
                t.finish();
                transfers.remove(t);
                Thread.interrupted();
            }
        }
    }

    private long send(FileChannel ch, long from, long length, OutputStream out, DownloadSlots.Slot slot, LauncherDownloadEndpoint.Transfer t) throws IOException {
        byte[] arr = new byte[LauncherDownloadEndpoint.CHUNK];
        ByteBuffer buf = ByteBuffer.wrap(arr);
        long pos = from;
        long left = length;
        long kbps = LauncherDownloadEndpoint.maxKBps();
        while (left > 0) {
            buf.clear();
            buf.limit((int) Math.min(arr.length, left));
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
        return t.sentBytes();
    }

    private static void busy(HttpExchange ex, long sec) throws IOException {
        ex.getResponseHeaders().set("Retry-After", String.valueOf(sec));
        LauncherHttp.respond(ex, 429, LauncherResult.fail(LauncherCodes.RATE_LIMITED).with("retryAfterSec", sec));
    }

    private void ensureWatchdog() {
        if (watchdog != null) {
            return;
        }
        synchronized (this) {
            if (watchdog == null) {
                ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread th = new Thread(r, "ClientFile-watchdog");
                    th.setDaemon(true);
                    return th;
                });
                long every = Math.max(200, Math.min(5_000, idleMs / 4));
                s.scheduleWithFixedDelay(() -> {
                    long now = System.nanoTime();
                    for (LauncherDownloadEndpoint.Transfer t : transfers) {
                        t.stopIfIdle(now, idleMs);
                    }
                }, every, every, TimeUnit.MILLISECONDS);
                watchdog = s;
            }
        }
    }
}
