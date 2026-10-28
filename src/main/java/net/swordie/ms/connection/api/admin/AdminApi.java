package net.swordie.ms.connection.api.admin;

import com.sun.net.httpserver.HttpServer;
import net.swordie.ms.connection.api.launcher.LauncherServices;
import net.swordie.ms.console.config.ServerRuntimeConfig;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Installs {@code /api/v2/admin} on the launcher API's HTTP server (called by HttpAuthApi.start): a new admin token
 * in {@code admin.token} ({@value AdminToken#FILE_KEY}), then the endpoint with the standard action table. When the
 * token cannot be written the admin API stays off (one log line) and the launcher API is unaffected.
 */
public final class AdminApi {
    private static volatile AdminEndpoint installed;

    private AdminApi() {
    }

    public static synchronized boolean install(HttpServer server, int port, LauncherServices services) {
        IssueCapture.install(); // groups the server's own problem lines for the console (ops.issues)
        Path file = tokenFile();
        AdminToken token;
        try {
            token = AdminToken.createAndWrite(file);
        } catch (IOException | RuntimeException e) {
            System.out.printf("[AdminApi] 管理 API 未啟用：無法寫入權杖檔 %s（%s）%n", file.toAbsolutePath(), e);
            return false;
        }
        AdminEnvironment env = new AdminEnvironment(services, ApiTraffic.standard(), AdminServerView.live(),
                Path.of("").toAbsolutePath(), port, System::currentTimeMillis);
        AdminEndpoint endpoint = new AdminEndpoint(AdminActionRegistry.standard(), () -> token, env);
        server.createContext(AdminEndpoint.PATH, endpoint);
        installed = endpoint;
        System.out.printf("[AdminApi] 管理 API 已啟用：127.0.0.1:%d%s（只收本機＋權杖，權杖檔 %s，每次啟動更新）%n",
                port, AdminEndpoint.PATH, file.toAbsolutePath().normalize());
        return true;
    }

    /** {@value AdminToken#FILE_KEY} (-D first, then server.properties), default {@value AdminToken#DEFAULT_FILE}. */
    public static Path tokenFile() {
        String raw = System.getProperty(AdminToken.FILE_KEY);
        if (raw == null || raw.isBlank()) {
            raw = ServerRuntimeConfig.stringValue(AdminToken.FILE_KEY);
        }
        return Path.of(raw == null || raw.isBlank() ? AdminToken.DEFAULT_FILE : raw.trim());
    }

    /** The installed endpoint (refusal counter), or null. */
    static AdminEndpoint endpoint() {
        return installed;
    }
}
