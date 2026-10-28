package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.swordie.ms.connection.api.launcher.LauncherHttp;
import net.swordie.ms.connection.api.launcher.LauncherJson;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.TokenBucketLimiter;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * {@code POST /api/v2/admin} {"action": ..., ...}: the admin console's API on the launcher API's HTTP server (8600).
 * Checked in this order, every refusal before the body is read:
 * <ol>
 *     <li>source address exactly 127.0.0.1 (the TCP peer; X-Forwarded-For and similar headers are never read) - 403;</li>
 *     <li>exactly one Host header, {@code 127.0.0.1:<port>} or {@code localhost:<port>} (DNS rebinding) - 403;</li>
 *     <li>no Origin / Sec-Fetch-Site header (a browser page, CSRF) - 403;</li>
 *     <li>POST (405), media type application/json (415);</li>
 *     <li>{@code Authorization: Bearer <admin token>} (the token of {@code admin.token}, compared in constant time) -
 *     401; after {@value #BAD_TOKEN_BURST} wrong tokens in a short time further wrong ones answer 429 (the right
 *     token is checked first and is never throttled, so a local process sending wrong tokens cannot lock the console
 *     out);</li>
 *     <li>body at most {@value #MAX_BODY} bytes (413), a JSON object (400).</li>
 * </ol>
 * Never sends CORS headers. Writes (mutating actions) run one at a time ({@link #WRITES}) and leave one
 * {@link AdminAudit} line; reads run in parallel and leave none.
 */
public final class AdminEndpoint implements HttpHandler {
    public static final String PATH = "/api/v2/admin";
    /** news.json may be 256 KB; its JSON-escaped form in a request somewhat more. */
    public static final int MAX_BODY = 768 * 1024;
    static final int BAD_TOKEN_BURST = 10;
    /** Mutating actions of every endpoint instance run one at a time (the editors also lock per file). */
    private static final ReentrantLock WRITES = new ReentrantLock(true);

    private final AdminActionRegistry registry;
    private final Supplier<AdminToken> token;
    private final AdminEnvironment env;
    /** Wrong tokens: 10 at once, then one per 6 s (shared by every source - there is only 127.0.0.1). */
    private final TokenBucketLimiter badTokens;
    private final TokenBucketLimiter refusedLog;
    private final AtomicLong refused = new AtomicLong();

    public AdminEndpoint(AdminActionRegistry registry, Supplier<AdminToken> token, AdminEnvironment env) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.token = Objects.requireNonNull(token, "token");
        this.env = Objects.requireNonNull(env, "env");
        this.badTokens = new TokenBucketLimiter(new TokenBucketLimiter.Policy(BAD_TOKEN_BURST, 6_000), env.clock());
        this.refusedLog = new TokenBucketLimiter(new TokenBucketLimiter.Policy(1, 60_000), env.clock());
    }

    /** Refused admin requests since start (any reason). */
    public long refusedCount() {
        return refused.get();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String ip = LauncherHttp.sourceIp(ex);
        String action = "-";
        try {
            String refusal = refusal(ex);
            if (refusal != null) {
                refuse(ip, refusal);
                LauncherHttp.respond(ex, 403, LauncherResult.fail(AdminCodes.FORBIDDEN));
                return;
            }
            if (!"POST".equals(ex.getRequestMethod())) {
                refused.incrementAndGet();
                LauncherHttp.status(ex, 405);
                return;
            }
            if (!LauncherHttp.isJsonMediaType(ex.getRequestHeaders().getFirst("Content-Type"))) {
                refused.incrementAndGet();
                LauncherHttp.respond(ex, 415, LauncherResult.fail(AdminCodes.UNSUPPORTED_MEDIA_TYPE));
                return;
            }
            AdminToken t = token.get();
            String presented = LauncherHttp.bearerToken(ex.getRequestHeaders().getFirst("Authorization"));
            if (t == null || !t.matches(presented)) {
                // the right token is never throttled: only wrong ones use up the bucket and get 429 once it is empty
                if (!badTokens.tryAcquire("bad")) {
                    refuse(ip, "too many wrong admin tokens");
                    LauncherHttp.respond(ex, 429, LauncherResult.fail(AdminCodes.RATE_LIMITED));
                    return;
                }
                refuse(ip, presented == null ? "no admin token" : "wrong admin token");
                LauncherHttp.respond(ex, 401, LauncherResult.fail(AdminCodes.UNAUTHORIZED));
                return;
            }
            byte[] body = LauncherHttp.readBody(ex, MAX_BODY);
            if (body == null) {
                LauncherHttp.respond(ex, 413, LauncherResult.fail(AdminCodes.TOO_LARGE));
                return;
            }
            JsonNode request = parse(body);
            if (request == null || !request.isObject()) {
                LauncherHttp.respond(ex, 400, LauncherResult.fail(AdminCodes.INVALID_INPUT));
                return;
            }
            String name = LauncherJson.text(request, "action");
            AdminAction a = registry.find(name).orElse(null);
            action = a == null ? "unknown" : a.name();
            LauncherResult result;
            boolean serialize = a != null && a.mutating();
            if (serialize) {
                WRITES.lock();
            }
            try {
                result = registry.dispatch(new AdminContext(ip, env), request);
            } catch (Exception e) {
                System.out.printf("[AdminApi] %s failed: %s%n", action, e);
                result = LauncherResult.fail(AdminCodes.UNAVAILABLE).with("problem", e.getClass().getSimpleName());
            } finally {
                if (serialize) {
                    WRITES.unlock();
                }
            }
            if (a != null && a.mutating()) {
                String summary;
                try {
                    summary = a.auditSummary(request);
                } catch (RuntimeException e) {
                    summary = "(summary failed)";
                }
                AdminAudit.write(a.name(), ip, summary, result.code());
            }
            LauncherHttp.respond(ex, 200, result);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            System.out.printf("[AdminApi] %s failed: %s%n", action, e.getClass().getSimpleName());
            LauncherHttp.respond(ex, 500, LauncherResult.fail(AdminCodes.UNAVAILABLE));
        } finally {
            ex.close();
        }
    }

    /** Why the request may not reach the token check (source, Host, browser), or null. */
    String refusal(HttpExchange ex) {
        InetSocketAddress remote = ex.getRemoteAddress();
        InetAddress a = remote == null ? null : remote.getAddress();
        if (!(a instanceof Inet4Address) || !"127.0.0.1".equals(a.getHostAddress())) {
            return "source is not 127.0.0.1";
        }
        if (!hostAllowed(ex.getRequestHeaders().get("Host"), env.apiPort())) {
            return "Host is not 127.0.0.1:" + env.apiPort() + " / localhost:" + env.apiPort();
        }
        if (ex.getRequestHeaders().containsKey("Origin") || ex.getRequestHeaders().containsKey("Sec-Fetch-Site")) {
            return "browser request (Origin)";
        }
        return null;
    }

    /** Exactly one Host header naming this API by loopback address or localhost, with the port. */
    static boolean hostAllowed(List<String> hosts, int port) {
        if (hosts == null || hosts.size() != 1 || hosts.get(0) == null) {
            return false;
        }
        String h = hosts.get(0).trim().toLowerCase(Locale.ROOT);
        return h.equals("127.0.0.1:" + port) || h.equals("localhost:" + port);
    }

    private void refuse(String ip, String why) {
        refused.incrementAndGet();
        if (refusedLog.tryAcquire(ip + "|" + why)) {
            System.out.printf("[AdminApi] refused %s: %s%n", ip, why);
        }
    }

    private static JsonNode parse(byte[] body) {
        try {
            return LauncherHttp.JSON.readTree(body);
        } catch (IOException e) {
            return null;
        }
    }
}
