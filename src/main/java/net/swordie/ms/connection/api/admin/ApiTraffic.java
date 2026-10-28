package net.swordie.ms.connection.api.admin;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import net.swordie.ms.connection.api.launcher.LauncherHttp;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.LongSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request statistics of the launcher endpoints on 8600 for the admin console, collected by an HttpServer
 * {@link Filter} that only looks: per path, per {@code X-Cat-Launcher} version (none = cat-launcher 1.0.0 or an
 * older launcher), per answered {@code code} / {@code result}, and the last {@value #RECENT} answers that were not a
 * success. The response body passes through unchanged; only its first {@value #PEEK} bytes are copied to read the
 * code. Counters live in memory and start at zero with the server.
 */
public final class ApiTraffic {
    public static final int RECENT = 50;
    static final int PEEK = 512;
    /** Version key of requests without (or with a malformed) X-Cat-Launcher header. */
    public static final String NO_VERSION = "-";
    /** Codes that mean the request did what it asked (everything else goes to the recent list); http200 / http206 = a P8b exe download (bytes, not JSON). */
    static final Set<String> SUCCESS = Set.of("ok", "success", "loggedIn", "registered", "reset", "loggedOut", "http200", "http206");
    private static final Pattern CODE = Pattern.compile("\"(?:code|result)\"\\s*:\\s*\"([A-Za-z0-9_]{1,40})\"");
    private static final ApiTraffic STANDARD = new ApiTraffic(System::currentTimeMillis);

    /** One answer that was not a success. */
    public record Recent(long at, String ip, String path, int status, String code, String version) {
        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("at", at);
            m.put("ip", ip);
            m.put("path", path);
            m.put("status", status);
            m.put("code", code);
            m.put("version", version);
            return m;
        }
    }

    private final LongSupplier clock;
    private final long startedAt;
    private final AtomicLong total = new AtomicLong();
    private final ConcurrentHashMap<String, LongAdder> byPath = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> byVersion = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> byCode = new ConcurrentHashMap<>();
    private final Deque<Recent> recent = new ArrayDeque<>();

    public ApiTraffic(LongSupplier clock) {
        this.clock = clock;
        this.startedAt = clock.getAsLong();
    }

    public static ApiTraffic standard() {
        return STANDARD;
    }

    /** Adds the counting filter to {@code context}; returns the context. */
    public HttpContext watch(HttpContext context) {
        context.getFilters().add(new Filter() {
            @Override
            public void doFilter(HttpExchange ex, Chain chain) throws IOException {
                Peek peek = null;
                try {
                    peek = new Peek(ex.getResponseBody());
                    ex.setStreams(null, peek);
                } catch (RuntimeException e) {
                    peek = null;
                }
                try {
                    chain.doFilter(ex);
                } finally {
                    try {
                        record(ex.getHttpContext().getPath(), LauncherHttp.sourceIp(ex),
                                ex.getRequestHeaders().getFirst(LauncherHttp.LAUNCHER_HEADER), ex.getResponseCode(),
                                peek == null ? null : peek.code());
                    } catch (RuntimeException ignored) {
                        // statistics never affect a request
                    }
                }
            }

            @Override
            public String description() {
                return "admin console request statistics";
            }
        });
        return context;
    }

    /** Counts one answered request (also used by checks). */
    void record(String path, String ip, String versionHeader, int status, String code) {
        String version = LauncherHttp.launcherVersion(versionHeader);
        String v = version == null ? NO_VERSION : version;
        String c = code == null ? (status > 0 ? "http" + status : "none") : code;
        total.incrementAndGet();
        byPath.computeIfAbsent(path == null ? "" : path, k -> new LongAdder()).increment();
        if (byVersion.size() < 200 || byVersion.containsKey(v)) {
            byVersion.computeIfAbsent(v, k -> new LongAdder()).increment();
        }
        if (byCode.size() < 200 || byCode.containsKey(c)) {
            byCode.computeIfAbsent(c, k -> new LongAdder()).increment();
        }
        if (status >= 400 || !SUCCESS.contains(c)) {
            synchronized (recent) {
                recent.addFirst(new Recent(clock.getAsLong(), ip, path, status, c, v));
                while (recent.size() > RECENT) {
                    recent.removeLast();
                }
            }
        }
    }

    public long total() {
        return total.get();
    }

    public long startedAt() {
        return startedAt;
    }

    public Map<String, Long> byPath() {
        return sorted(byPath);
    }

    public Map<String, Long> byVersion() {
        return sorted(byVersion);
    }

    public Map<String, Long> byCode() {
        return sorted(byCode);
    }

    /** Newest first. */
    public List<Recent> recent() {
        synchronized (recent) {
            return new ArrayList<>(recent);
        }
    }

    private static Map<String, Long> sorted(Map<String, LongAdder> m) {
        Map<String, Long> out = new TreeMap<>();
        m.forEach((k, v) -> out.put(k, v.sum()));
        return out;
    }

    /** The {@code code} (or legacy {@code result}) of a JSON body's first bytes, or null. */
    static String codeOf(byte[] head) {
        Matcher m = CODE.matcher(new String(head, StandardCharsets.UTF_8));
        return m.find() ? m.group(1) : null;
    }

    /** Passes every byte through and keeps a copy of the first {@value #PEEK}. */
    static final class Peek extends FilterOutputStream {
        private final ByteArrayOutputStream head = new ByteArrayOutputStream();

        Peek(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            keep(new byte[]{(byte) b}, 0, 1);
            out.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            keep(b, off, len);
            out.write(b, off, len);
        }

        private void keep(byte[] b, int off, int len) {
            int room = PEEK - head.size();
            if (room > 0) {
                head.write(b, off, Math.min(room, len));
            }
        }

        String code() {
            return codeOf(head.toByteArray());
        }
    }
}
