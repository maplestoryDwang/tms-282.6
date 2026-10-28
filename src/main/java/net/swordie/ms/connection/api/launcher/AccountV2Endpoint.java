package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Objects;

/**
 * {@code POST /api/v2/account} {action: login|register|reset, ...}: kept for cat-launcher 1.0.0 - POST only, no
 * Content-Type or X-Cat-Launcher requirements, 8 KB body, malformed JSON answered with {@code unavailable}, never a
 * session token, only the 09-22 codes ({@link LauncherChannel#ACCOUNT_V2}). The per-IP guard and the per-action
 * buckets (auth, register) answer {@code busy}, which 1.0.0 shows as "too many attempts". Requests go to the shared
 * registry (restricted view).
 */
public final class AccountV2Endpoint implements HttpHandler {
    private final LauncherActionRegistry registry;
    private final LauncherServices services;

    public AccountV2Endpoint(LauncherActionRegistry registry, LauncherServices services) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.services = Objects.requireNonNull(services, "services");
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String ip = LauncherHttp.sourceIp(ex);
        String version = LauncherHttp.launcherVersion(ex.getRequestHeaders().getFirst(LauncherHttp.LAUNCHER_HEADER));
        try {
            if (!"POST".equals(ex.getRequestMethod())) {
                LauncherHttp.status(ex, 405);
                return;
            }
            if (!services.rateLimits().tryAcquire(LauncherRateLimits.IP, ip)) {
                LauncherHttp.respond(ex, 200, LauncherResult.fail(LauncherCodes.BUSY));
                return;
            }
            byte[] body = LauncherHttp.readBody(ex, LauncherHttp.MAX_BODY);
            if (body == null) {
                LauncherHttp.respond(ex, 200, LauncherResult.fail(LauncherCodes.INVALID_INPUT));
                return;
            }
            JsonNode request = LauncherHttp.JSON.readTree(body);
            if (request == null || !request.isObject()) {
                LauncherHttp.respond(ex, 200, LauncherResult.fail(LauncherCodes.INVALID_INPUT));
                return;
            }
            String action = registry.find(LauncherJson.text(request, "action")).map(LauncherAction::name).orElse("unknown");
            LauncherContext ctx = LauncherContext.http(LauncherChannel.ACCOUNT_V2, ip, version, null, services);
            LauncherResult result = registry.dispatch(ctx, request);
            System.out.printf("[HttpAuthApi] v2 %s %s -> %s v=%s%n", action, ip, result.code(), LauncherHttp.logVersion(version));
            LauncherHttp.respond(ex, 200, result);
        } catch (Exception e) {
            LauncherHttp.respond(ex, 200, LauncherResult.fail(LauncherCodes.UNAVAILABLE));
        }
    }
}
