package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Objects;

/**
 * {@code POST /api/v2/launcher} {"action": ..., ...}: the P5a launcher entry.
 * <ol>
 *     <li>POST only (405 otherwise, no CORS headers - with the custom header and JSON media type below a browser
 *     page cannot send a simple cross-origin request).</li>
 *     <li>{@code X-Cat-Launcher: <version>} required (400 {@code launcherRequired}).</li>
 *     <li>Content-Type media type {@code application/json}, parameters ignored (415 {@code unsupportedMediaType}).</li>
 *     <li>Per-IP flood guard ({@link LauncherRateLimits#IP}), body at most 8 KB, a JSON object.</li>
 *     <li>{@link LauncherActionRegistry#dispatch} with {@code Authorization: Bearer <token>}.</li>
 * </ol>
 * Log line: action, source IP, code, launcher version - never a token, password, recovery code or gift code.
 */
public final class LauncherHttpEndpoint implements HttpHandler {
    private final LauncherActionRegistry registry;
    private final LauncherServices services;

    public LauncherHttpEndpoint(LauncherActionRegistry registry, LauncherServices services) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.services = Objects.requireNonNull(services, "services");
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String ip = LauncherHttp.sourceIp(ex);
        String version = LauncherHttp.launcherVersion(ex.getRequestHeaders().getFirst(LauncherHttp.LAUNCHER_HEADER));
        String action = "-";
        String code = LauncherCodes.UNAVAILABLE;
        try {
            if (!"POST".equals(ex.getRequestMethod())) {
                code = "405";
                LauncherHttp.status(ex, 405);
                return;
            }
            if (version == null) {
                code = LauncherCodes.LAUNCHER_REQUIRED;
                LauncherHttp.respond(ex, 400, LauncherResult.fail(code));
                return;
            }
            if (!LauncherHttp.isJsonMediaType(ex.getRequestHeaders().getFirst("Content-Type"))) {
                code = LauncherCodes.UNSUPPORTED_MEDIA_TYPE;
                LauncherHttp.respond(ex, 415, LauncherResult.fail(code));
                return;
            }
            if (!services.rateLimits().tryAcquire(LauncherRateLimits.IP, ip)) {
                code = LauncherCodes.RATE_LIMITED;
                LauncherHttp.respond(ex, 200, LauncherResult.fail(code).with("retryAfterSec",
                        LauncherPolicies.seconds(services.rateLimits().retryAfterMs(LauncherRateLimits.IP, ip))));
                return;
            }
            byte[] body = LauncherHttp.readBody(ex, LauncherHttp.MAX_BODY);
            JsonNode request = body == null ? null : parse(body);
            if (request == null || !request.isObject()) {
                code = LauncherCodes.INVALID_INPUT;
                LauncherHttp.respond(ex, body == null ? 413 : 400, LauncherResult.fail(code));
                return;
            }
            action = registry.find(LauncherJson.text(request, "action")).map(LauncherAction::name).orElse("unknown");
            String bearer = LauncherHttp.bearerToken(ex.getRequestHeaders().getFirst("Authorization"));
            LauncherContext ctx = LauncherContext.http(LauncherChannel.LAUNCHER_V2, ip, version, bearer, services);
            LauncherResult result = registry.dispatch(ctx, request);
            code = result.code();
            LauncherHttp.respond(ex, 200, result);
        } catch (Exception e) {
            code = LauncherCodes.UNAVAILABLE;
            System.out.printf("[HttpAuthApi] launcher %s failed: %s%n", action, e.getClass().getSimpleName());
            LauncherHttp.respond(ex, 200, LauncherResult.fail(code));
        } finally {
            System.out.printf("[HttpAuthApi] launcher %s %s -> %s v=%s%n", action, ip, code, LauncherHttp.logVersion(version));
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
