package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The admin API over HTTP/1.1 to {@code http://127.0.0.1:<port>/api/v2/admin} with the token of {@code admin.token}
 * in the server folder. The token is read again whenever the server answers 401 (it is new after every restart).
 * Sends no Origin header (the server refuses browsers) and nothing but the token as a credential; never logs it.
 */
public final class HttpAdminGateway implements AdminGateway {
    static final ObjectMapper JSON = new ObjectMapper();

    private final int port;
    private final Path tokenFile;
    private final HttpClient http;
    private volatile String token;

    public HttpAdminGateway(int port, Path tokenFile) {
        this.port = port;
        this.tokenFile = tokenFile;
        this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                .proxy(java.net.ProxySelector.of(null))
                .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public String describe() {
        return "127.0.0.1:" + port;
    }

    @Override
    public JsonNode call(String action, Map<String, Object> params) throws AdminCallException {
        try {
            return send(action, params, false);
        } catch (AdminCallException e) {
            if (e.kind() == AdminCallException.Kind.TOKEN) {
                token = null;
                return send(action, params, true);
            }
            throw e;
        }
    }

    private JsonNode send(String action, Map<String, Object> params, boolean retry) throws AdminCallException {
        String t = token();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", action);
        body.putAll(params);
        byte[] json;
        try {
            json = JSON.writeValueAsBytes(body);
        } catch (IOException e) {
            throw new AdminCallException(AdminCallException.Kind.OTHER, "無法組成請求：" + e.getMessage(), e);
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v2/admin"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Authorization", "Bearer " + t)
                .POST(HttpRequest.BodyPublishers.ofByteArray(json))
                .build();
        HttpResponse<byte[]> res;
        try {
            res = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        } catch (ConnectException | HttpConnectTimeoutException e) {
            throw new AdminCallException(AdminCallException.Kind.SERVER_DOWN, "伺服器未啟動（" + describe() + " 沒有回應）", e);
        } catch (HttpTimeoutException e) {
            throw new AdminCallException(AdminCallException.Kind.OTHER, "伺服器回應逾時（" + action + "）", e);
        } catch (IOException e) {
            // a refused connection sometimes surfaces as a plain IOException with a ConnectException cause
            if (e.getCause() instanceof ConnectException) {
                throw new AdminCallException(AdminCallException.Kind.SERVER_DOWN, "伺服器未啟動（" + describe() + " 沒有回應）", e);
            }
            throw new AdminCallException(AdminCallException.Kind.OTHER, "連線中斷：" + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AdminCallException(AdminCallException.Kind.OTHER, "已取消", e);
        }
        if (res.statusCode() == 401) {
            throw new AdminCallException(AdminCallException.Kind.TOKEN, retry
                    ? "管理權杖不符：伺服器可能剛重新啟動，或 admin.token 不是這台伺服器的" : "管理權杖不符");
        }
        if (res.statusCode() == 403) {
            throw new AdminCallException(AdminCallException.Kind.FORBIDDEN, "伺服器拒絕（只接受本機 127.0.0.1 的主控台）");
        }
        if (res.statusCode() == 429) {
            throw new AdminCallException(AdminCallException.Kind.OTHER, "錯誤權杖太多次，伺服器暫時拒絕，請稍候再試");
        }
        if (res.statusCode() == 404) {
            throw new AdminCallException(AdminCallException.Kind.OTHER, "這台伺服器沒有管理 API（還是舊版 jar？）");
        }
        try {
            JsonNode n = JSON.readTree(res.body());
            if (n == null || !n.isObject()) {
                throw new AdminCallException(AdminCallException.Kind.OTHER, "回應格式不正確（HTTP " + res.statusCode() + "）");
            }
            return n;
        } catch (IOException e) {
            throw new AdminCallException(AdminCallException.Kind.OTHER, "回應格式不正確（HTTP " + res.statusCode() + "）", e);
        }
    }

    private String token() throws AdminCallException {
        String t = token;
        if (t != null) {
            return t;
        }
        try {
            t = Files.readString(tokenFile, StandardCharsets.US_ASCII).strip();
        } catch (NoSuchFileException e) {
            throw new AdminCallException(AdminCallException.Kind.SERVER_DOWN,
                    "找不到 " + tokenFile + "（伺服器還沒啟動，或不是含管理 API 的版本）", e);
        } catch (IOException e) {
            throw new AdminCallException(AdminCallException.Kind.TOKEN, "讀不到 " + tokenFile + "：" + e.getMessage(), e);
        }
        token = t;
        return t;
    }
}
