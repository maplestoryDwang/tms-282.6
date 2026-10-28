package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * HTTP plumbing shared by the 8600 endpoints. Never sends CORS headers: the API is not for browsers (requests with an
 * Origin header or a DNS-name Host are refused before any endpoint runs).
 */
public final class LauncherHttp {
    public static final String LAUNCHER_HEADER = "X-Cat-Launcher";
    public static final String JSON_MEDIA_TYPE = "application/json";
    public static final int MAX_BODY = 8192;
    public static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern VERSION = Pattern.compile("[0-9A-Za-z][0-9A-Za-z._+-]{0,31}");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer +([^ ]+) *");
    private static final Pattern PORT = Pattern.compile("[0-9]{1,5}");

    private LauncherHttp() {
    }

    public static String sourceIp(HttpExchange ex) {
        InetSocketAddress remote = ex.getRemoteAddress();
        return remote == null || remote.getAddress() == null ? "" : remote.getAddress().getHostAddress();
    }

    /** Only the media type counts: {@code application/json; charset=utf-8} (cat-launcher 1.0.0) is JSON. */
    public static boolean isJsonMediaType(String contentType) {
        if (contentType == null) {
            return false;
        }
        int semicolon = contentType.indexOf(';');
        String media = (semicolon < 0 ? contentType : contentType.substring(0, semicolon)).trim();
        return media.toLowerCase(Locale.ROOT).equals(JSON_MEDIA_TYPE);
    }

    /** The X-Cat-Launcher value if well formed (safe to log), else null. */
    public static String launcherVersion(String header) {
        if (header == null) {
            return null;
        }
        String v = header.trim();
        return VERSION.matcher(v).matches() ? v : null;
    }

    /** The token of {@code Authorization: Bearer <token>}, or null when absent / another scheme. */
    public static String bearerToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        var m = BEARER.matcher(authorization.trim());
        return m.matches() ? m.group(1) : null;
    }

    /**
     * Every Host header names this machine by address: absent, an IPv4 literal, an [IPv6] literal or localhost, each
     * with an optional port. Launchers connect by IP (cat-launcher settings accept IPv4 only), so a DNS name means a
     * browser reached 8600 through a name that resolves here (DNS rebinding: same-origin to the page, so the custom
     * header and the JSON media type no longer stop it, and the source is the victim's own allowed address).
     */
    public static boolean hostIsLiteral(List<String> hostHeaders) {
        if (hostHeaders == null) {
            return true;
        }
        for (String host : hostHeaders) {
            if (!hostIsLiteral(host)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hostIsLiteral(String host) {
        if (host == null || host.isBlank()) {
            return true;
        }
        String h = host.trim();
        String name;
        String port = null;
        if (h.startsWith("[")) {
            int end = h.indexOf(']');
            if (end < 0) {
                return false;
            }
            name = h.substring(1, end);
            String rest = h.substring(end + 1);
            if (!rest.isEmpty()) {
                if (rest.charAt(0) != ':') {
                    return false;
                }
                port = rest.substring(1);
            }
            if (name.indexOf(':') < 0 || SourceAllowlist.literal(name) == null) {
                return false;
            }
        } else {
            int colon = h.indexOf(':');
            if (colon >= 0) {
                if (h.indexOf(':', colon + 1) >= 0) {
                    return false;
                }
                name = h.substring(0, colon);
                port = h.substring(colon + 1);
            } else {
                name = h;
            }
            if (!name.equalsIgnoreCase("localhost") && SourceAllowlist.literal(name) == null) {
                return false;
            }
        }
        return port == null || PORT.matcher(port).matches();
    }

    /**
     * A browser made the request: browsers send Origin with every cross-origin request and with same-origin POSTs;
     * the launchers (Node http in the Electron main process, .NET, PowerShell) never do.
     */
    public static boolean fromBrowser(HttpExchange ex) {
        return ex.getRequestHeaders().containsKey("Origin");
    }

    /** At most {@code max} bytes of the body, or null when it is larger. */
    public static byte[] readBody(HttpExchange ex, int max) throws IOException {
        byte[] body = ex.getRequestBody().readNBytes(max + 1);
        return body.length > max ? null : body;
    }

    public static void respond(HttpExchange ex, int status, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    public static void respond(HttpExchange ex, int status, LauncherResult result) throws IOException {
        respond(ex, status, JSON.writeValueAsString(result.toMap()));
    }

    /** A status without a body (405). */
    public static void status(HttpExchange ex, int status) throws IOException {
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }

    /** For logs: the version or "-". */
    public static String logVersion(String version) {
        return version == null || version.isEmpty() ? "-" : version;
    }
}
