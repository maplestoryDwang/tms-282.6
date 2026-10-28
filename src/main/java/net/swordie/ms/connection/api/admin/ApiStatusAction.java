package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherRateLimits;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LauncherServices;
import net.swordie.ms.connection.api.launcher.SourceAllowlist;
import net.swordie.ms.connection.api.launcher.TokenBucketLimiter;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.handlers.login.tms282.Tms282LoginThrottle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code api.status} (API 監看): the 8600 server's launcher sessions, request counts per launcher version / path /
 * code, the token-bucket refusals per bucket, the login back-off, api.http.allow and the recent non-success answers.
 * Read-only (expired sessions are swept first, as every launcher request does).
 */
final class ApiStatusAction implements AdminAction {
    private static final List<String> BUCKETS = List.of(LauncherRateLimits.IP, LauncherRateLimits.AUTH,
            LauncherRateLimits.REGISTER, LauncherRateLimits.SESSION, LauncherRateLimits.READ);

    @Override
    public String name() {
        return "api.status";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) {
        AdminEnvironment env = ctx.env();
        LauncherServices s = env.services();
        s.sessions().sweep();
        List<Map<String, Object>> buckets = new ArrayList<>();
        long refusedTotal = 0;
        for (String b : BUCKETS) {
            TokenBucketLimiter l = s.rateLimits().limiter(b);
            if (l == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bucket", b);
            m.put("capacity", l.policy().capacity());
            m.put("refillMs", l.policy().refillMs());
            m.put("refused", l.refused());
            m.put("tracked", l.size());
            buckets.add(m);
            refusedTotal += l.refused();
        }
        ApiTraffic t = env.traffic();
        List<Map<String, Object>> recent = new ArrayList<>();
        for (ApiTraffic.Recent r : t.recent()) {
            recent.add(r.toMap());
        }
        AdminEndpoint endpoint = AdminApi.endpoint();
        return LauncherResult.ok(AdminCodes.OK)
                .with("apiPort", env.apiPort())
                .with("uptimeSec", env.server().uptimeSec())
                .with("countingSince", t.startedAt())
                .with("sessions", s.sessions().size())
                .with("requests", t.total())
                .with("byVersion", t.byVersion())
                .with("byPath", t.byPath())
                .with("byCode", t.byCode())
                .with("rateLimits", buckets)
                .with("rateLimitedTotal", refusedTotal)
                .with("launcherBackoff", Map.of("pairs", s.loginBackoff().size(), "locked", s.loginBackoff().lockedCount()))
                .with("gameLoginLocked", Tms282LoginThrottle.standard().lockedCount())
                .with("httpAllow", allowlist())
                .with("adminRefused", endpoint == null ? 0 : endpoint.refusedCount())
                .with("legacyApi", Map.of("enabled", env.server().legacyApiEnabled(), "port", env.server().legacyApiPort()))
                .with("recent", recent);
    }

    /** api.http.allow as configured (where it comes from) and as applied. */
    static Map<String, Object> allowlist() {
        String forced = System.getProperty(SourceAllowlist.KEY);
        String file = ServerRuntimeConfig.stringValue(SourceAllowlist.KEY);
        SourceAllowlist current = SourceAllowlist.current();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", SourceAllowlist.KEY);
        m.put("source", forced != null && !forced.isBlank() ? "jvm" : file != null && !file.isBlank() ? "file" : "default");
        m.put("fileValue", file == null || file.isBlank() ? null : file);
        m.put("default", SourceAllowlist.DEFAULT);
        m.put("rules", current.rules());
        m.put("rejected", current.rejected());
        return m;
    }
}
