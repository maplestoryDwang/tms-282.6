import com.sun.net.httpserver.HttpServer;
import net.swordie.ms.DiscordAPI;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.connection.api.HttpAuthApi;
import net.swordie.ms.connection.api.launcher.SourceAllowlist;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.netty.GameSourceGate;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.handlers.login.tms282.ChannelAddressPolicy;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/** Compile with JDK 22 against the packaged JAR, then run with --enable-preview and the JAR on the classpath. */
public class LoopbackOnlyCheck {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("child")) {
            verify(Path.of(args[1]));
            return;
        }
        Path sourceRoot = (args.length == 0 ? Path.of("") : Path.of(args[0])).toAbsolutePath().normalize();
        Path work = Files.createTempDirectory("tms282-loopback-check-");
        try {
            Files.createDirectory(work.resolve("config"));
            Files.writeString(work.resolve("config/server.properties"), """
                    server.host=10.20.30.40
                    db.host=example.invalid
                    net.allow=0.0.0.0/0,::/0
                    api.http.allow=0.0.0.0/0,::/0
                    tms282.channelAdvertisedHost=10.20.30.40
                    """);
            String cp = Arrays.stream(System.getProperty("java.class.path").split(Pattern.quote(File.pathSeparator)))
                    .map(p -> Path.of(p).toAbsolutePath().normalize().toString())
                    .reduce((a, b) -> a + File.pathSeparator + b).orElseThrow();
            Process child = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                    "--enable-preview", "-cp", cp, LoopbackOnlyCheck.class.getName(), "child", sourceRoot.toString())
                    .directory(work.toFile()).inheritIO().start();
            check(child.waitFor() == 0, "isolated checks failed");
        } finally {
            try (var files = Files.walk(work)) {
                for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
                    check(p.toAbsolutePath().normalize().startsWith(work), "cleanup outside temporary directory");
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    private static void verify(Path sourceRoot) throws Exception {
        System.setProperty("tms282.tests.rejectRealDatabase", "true");
        for (String key : new String[]{"server.host", "db.host", "net.allow", "api.http.allow",
                "tms282.channelAdvertisedHost", "api.http.bind"}) {
            System.setProperty(key, "0.0.0.0/0,::/0");
        }
        ServerRuntimeConfig.reload();
        for (String key : new String[]{"server.host", "db.host", "net.allow", "api.http.allow",
                "tms282.channelAdvertisedHost"}) {
            check(ServerRuntimeConfig.stringValue(key).equals("127.0.0.1"), "file/JVM override of " + key);
        }
        check(ServerRuntimeConfig.jdbcUrl().startsWith("jdbc:mariadb://127.0.0.1:"), "local database host");
        local(ServerConstants.CHANNEL_IP, "legacy channel address");
        // 2026-10-08: packets give the game the loopback alias (relayed to 127.0.0.1 by the launcher); bindings stay 127.0.0.1
        alias(ServerRuntimeConfig.serverHostBytes(), "server packet address");
        byte[] copy = ServerRuntimeConfig.serverHostBytes();
        copy[0] = 10;
        alias(ServerRuntimeConfig.serverHostBytes(), "fresh address array");
        alias(ChannelAddressPolicy.server().advertisedHost(), "advertised channel address");
        alias(ChannelAddressPolicy.server().channelAddress(new InetSocketAddress("10.20.30.40", 1)), "channel socket address");
        alias(ChannelAddressPolicy.server().legacyAddress(null), "legacy migration address");
        check(ChannelAddressPolicy.server().describe().equals("202.80.104.26"), "described packet address");
        for (byte[] bytes : new byte[][]{{127, 0, 0, 1}, {127, 0, 0, 2}, {10, 20, 30, 40},
                {26, 1, 2, 3}, {0, 0, 0, 0}, new byte[16]}) {
            InetAddress address = InetAddress.getByAddress(bytes);
            boolean expected = Arrays.equals(bytes, new byte[]{127, 0, 0, 1});
            check(SourceAllowlist.current().allows(address) == expected, "API source " + address);
            check(GameSourceGate.standard().allows(address) == expected, "game source " + address);
        }
        ServerRuntimeConfig.save(Map.of("server.host", "10.20.30.40", "db.host", "example.invalid",
                "net.allow", "0.0.0.0/0", "api.http.allow", "::/0", "tms282.channelAdvertisedHost", "auto"));
        Properties saved = new Properties();
        try (var in = Files.newInputStream(Path.of("config/server.properties"))) {
            saved.load(in);
        }
        for (String key : saved.stringPropertyNames()) {
            check(saved.getProperty(key).equals("127.0.0.1"), "console save of " + key);
        }
        for (String bad : new String[]{"0.0.0.0", "127.0.0.2", "10.20.30.40", "::1"}) {
            try {
                HttpAuthApi.start(new InetSocketAddress(bad, 0));
                throw new AssertionError("HTTP accepted " + bad);
            } catch (IllegalArgumentException expected) {
                // Refused before opening any listener.
            }
        }
        try {
            int port = HttpAuthApi.start(new InetSocketAddress("127.0.0.1", 0));
            check(port > 0, "ephemeral HTTP API started");
            HttpServer bound = null;
            for (var field : HttpAuthApi.class.getDeclaredFields()) {
                if (field.getType() == HttpServer.class) {
                    field.setAccessible(true);
                    bound = (HttpServer) field.get(null);
                }
            }
            check(bound != null && bound.getAddress().getAddress().getHostAddress().equals("127.0.0.1"), "actual HTTP bind");
            HttpURLConnection ping = (HttpURLConnection) URI.create("http://127.0.0.1:" + port + "/api/ping")
                    .toURL().openConnection(Proxy.NO_PROXY);
            ping.setConnectTimeout(3000);
            ping.setReadTimeout(3000);
            try {
                check(ping.getResponseCode() == 200, "local ping response");
                try (var in = ping.getInputStream()) {
                    check(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("\"result\":\"ok\""), "local ping body");
                }
            } finally {
                ping.disconnect();
            }
        } finally {
            HttpAuthApi.stop();
        }
        DiscordAPI.start();
        DiscordAPI.send("ignored", "ignored", "ignored");
        String base = "src/main/java/net/swordie/ms/";
        contains(sourceRoot.resolve(base + "connection/netty/LoginAcceptor.java"), "b.bind(\"127.0.0.1\", ServerConstants.LOGIN_PORT)");
        contains(sourceRoot.resolve(base + "connection/netty/ChannelAcceptor.java"), "b.bind(\"127.0.0.1\", channel.getPort())");
        contains(sourceRoot.resolve(base + "connection/api/ApiAcceptor.java"), "BIND_ADDRESS = \"127.0.0.1\"");
        contains(sourceRoot.resolve(base + "connection/api/ApiAcceptor.java"), "b.bind(BIND_ADDRESS, ServerConstants.API_PORT)");
        contains(sourceRoot.resolve(base + "connection/api/HttpAuthApi.java"), "start(new InetSocketAddress(\"127.0.0.1\", configuredPort()))");
        contains(sourceRoot.resolve(base + "console/ConsoleMain.java"), "InetAddress.getByAddress(new byte[]{127, 0, 0, 1})");
        contains(sourceRoot.resolve(base + "console/ConsoleFrame.java"), "probe.bind(new java.net.InetSocketAddress(\"127.0.0.1\"");
        contains(sourceRoot.resolve(base + "console/admin/KitDownload.java"), "throw new IOException(");
        check(!DatabaseManager.isInitialized(), "no database connection");
        System.out.println("PASS: strict 127.0.0.1 bindings, sources and config/JVM overrides; packets give the loopback alias 202.80.104.26 (offline)");
    }

    private static void contains(Path file, String text) throws Exception {
        check(Files.readString(file).contains(text), "binding policy: " + file.getFileName());
    }

    private static void local(byte[] address, String message) {
        check(Arrays.equals(address, new byte[]{127, 0, 0, 1}), message);
    }

    private static void alias(byte[] address, String message) {
        check(Arrays.equals(address, new byte[]{(byte) 202, 80, 104, 26}), message);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
