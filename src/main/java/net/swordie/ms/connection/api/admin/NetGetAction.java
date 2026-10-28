package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.SourceAllowlist;
import net.swordie.ms.connection.netty.GameSourceGate;
import net.swordie.ms.connection.netty.RefusedSources;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code net.get} (連線安全): net.allow / net.maxPerIp / net.login.idleSec / api.http.allow (file value, effective value,
 * default, -D override) with the file's SHA-256 for the save; the recently refused game-port sources; this machine's
 * network interfaces (Radmin 26.x, LAN, Tailscale 100.64/10) with whether each IPv4 address passes the two lists; the
 * server's listening ports. Read-only. The firewall is read by the console itself (Get-NetFirewall*, read-only).
 */
final class NetGetAction implements AdminAction {
    @Override
    public String name() {
        return "net.get";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        Path config = ctx.env().resolve("config", "server.properties");
        byte[] file = null;
        try {
            file = new ServerPropertiesEditor(config).read();
        } catch (java.io.IOException e) {
            // shown as no file values
        }
        List<Map<String, Object>> settings = new ArrayList<>();
        for (String k : NetSettings.KEYS) {
            settings.add(NetSettings.describe(k, file));
        }
        List<Map<String, Object>> refused = new ArrayList<>();
        for (RefusedSources.Entry e : RefusedSources.standard().snapshot()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ip", e.ip());
            m.put("count", e.count());
            m.put("firstAt", e.firstAt());
            m.put("lastAt", e.lastAt());
            m.put("reason", e.reason());
            m.put("kind", e.kind());
            m.put("port", e.port());
            m.put("allowedNow", GameSourceGate.standard().allowlist().allows(e.ip()));
            refused.add(m);
        }
        return LauncherResult.ok(AdminCodes.OK)
                .with("configFile", config.toAbsolutePath().normalize().toString())
                .with("sha256", file == null ? "" : ServerPropertiesEditor.sha256(file))
                .with("settings", settings)
                .with("refused", refused)
                .with("refusedTotal", RefusedSources.standard().total())
                .with("refusedCapacity", RefusedSources.CAPACITY)
                .with("interfaces", interfaces())
                .with("listeners", listeners(ctx.env()));
    }

    static List<Map<String, Object>> interfaces() throws SocketException {
        SourceAllowlist game = GameSourceGate.standard().allowlist();
        SourceAllowlist http = SourceAllowlist.current();
        List<Map<String, Object>> out = new ArrayList<>();
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            if (!ni.isUp()) {
                continue;
            }
            List<Map<String, Object>> addrs = new ArrayList<>();
            String category = ni.isLoopback() ? "loopback" : null;
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (a.isLinkLocalAddress()) {
                    continue;
                }
                String ip = a.getHostAddress();
                int pct = ip.indexOf('%');
                if (pct >= 0) {
                    ip = ip.substring(0, pct);
                }
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ip", ip);
                m.put("v4", a instanceof Inet4Address);
                m.put("netAllow", game.allows(a));
                m.put("httpAllow", http.allows(a));
                addrs.add(m);
                if (category == null && a instanceof Inet4Address) {
                    category = category(a.getAddress());
                }
            }
            if (addrs.isEmpty()) {
                continue;
            }
            String label = (ni.getName() + " " + ni.getDisplayName()).toLowerCase(Locale.ROOT);
            if (label.contains("tailscale")) {
                category = "tailscale";
            } else if (label.contains("radmin")) {
                category = "radmin";
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", ni.getName());
            m.put("displayName", ni.getDisplayName());
            m.put("category", category == null ? "other" : category);
            m.put("addresses", addrs);
            out.add(m);
        }
        return out;
    }

    /** radmin (26.0.0.0/8), tailscale (100.64.0.0/10), lan (RFC 1918), loopback, or null. */
    static String category(byte[] v4) {
        int a = v4[0] & 0xFF;
        int b = v4[1] & 0xFF;
        if (a == 127) {
            return "loopback";
        }
        if (a == 26) {
            return "radmin";
        }
        if (a == 100 && b >= 64 && b <= 127) {
            return "tailscale";
        }
        if (a == 10 || (a == 172 && b >= 16 && b <= 31) || (a == 192 && b == 168)) {
            return "lan";
        }
        return null;
    }

    static List<Map<String, Object>> listeners(AdminEnvironment env) {
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(listener("login", "127.0.0.1", env.server().loginPort()));
        int n = 1;
        for (int port : env.server().channelPorts()) {
            out.add(listener("channel" + n++, "127.0.0.1", port));
        }
        out.add(listener("api", "127.0.0.1", env.apiPort()));
        if (env.server().legacyApiEnabled()) {
            out.add(listener("legacyApi", "127.0.0.1", env.server().legacyApiPort()));
        }
        return out;
    }

    private static Map<String, Object> listener(String purpose, String address, int port) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("purpose", purpose);
        m.put("address", address);
        m.put("port", port);
        return m;
    }
}
