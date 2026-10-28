package net.swordie.ms.connection.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.swordie.ms.Server;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.User;
import net.swordie.ms.connection.api.admin.AdminApi;
import net.swordie.ms.connection.api.admin.ApiTraffic;
import net.swordie.ms.connection.api.launcher.AccountCredentials;
import net.swordie.ms.connection.api.launcher.AccountSecrets;
import net.swordie.ms.connection.api.launcher.AccountV2Endpoint;
import net.swordie.ms.connection.api.launcher.LauncherActionRegistry;
import net.swordie.ms.connection.api.launcher.LauncherHttp;
import net.swordie.ms.connection.api.launcher.LauncherHttpEndpoint;
import net.swordie.ms.connection.api.launcher.LauncherRateLimits;
import net.swordie.ms.connection.api.launcher.LauncherServices;
import net.swordie.ms.connection.api.launcher.SourceAllowlist;
import net.swordie.ms.connection.api.launcher.TokenBucketLimiter;
import net.swordie.ms.connection.api.launcher.release.ClientFileEndpoint;
import net.swordie.ms.connection.api.launcher.release.DownloadSlots;
import net.swordie.ms.connection.api.launcher.release.LauncherDownloadEndpoint;
import net.swordie.ms.connection.api.launcher.release.LauncherClientFiles;
import net.swordie.ms.connection.api.launcher.release.LauncherRelease;
import net.swordie.ms.constants.DevAccounts;
import net.swordie.ms.util.Util;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本機限定登入器用的輕量 HTTP 登入/註冊 API。
 *
 * 與遊戲伺服器同進程執行，直接重用記憶體中的 token 表（{@link Server#addAuthToken(int, int)}），
 * 因此登入器只要用單純的 HTTP 請求就能換到遊戲客戶端 WebStart 用的 token，
 * 不必自行實作 MapleStory 的加密封包協定。
 *
 * 端點：
 *   /api/login    ?user=&pass=          -> {"result":"success","token":123} | wrongPassword | banned | alreadyLogin | busy
 *                 （限速開啟時：帳號只收 [A-Za-z0-9]{3,24}、每 IP 的 ip 與 auth 桶、帳號＋IP 失敗退避）
 *   /api/register ?user=&pass=&email=   -> {"result":"success"} | nameInUse | unknown（預設關閉）
 *   /api/ping                           -> {"result":"ok","api":3,"features":[...]}（1.0.0 只看 api>=2）
 *   /api/v2/account  POST JSON {action: login|register|reset, ...} -> 與 LocalAccountCli 相同的 {"ok":..,"code":..}
 *                    規則不變（cat-launcher 1.0.0）：POST、8 KB、每 IP 限速回 busy、不發 session。
 *   /api/v2/launcher POST JSON {action, ...}，X-Cat-Launcher 必填、Content-Type 只比對 application/json、
 *                    Authorization: Bearer <token>（login 發給；session.logout 等需要）。見 {@link LauncherHttpEndpoint}。
 *   /api/v2/launcher/download/<file>  GET，P8b 登入器 exe 下載（release.json 登記的檔名、Range、每 IP 1／全體 4 個同時下載）。
 *                    見 {@link LauncherDownloadEndpoint}。
 *   /api/v2/launcher/client/<version>/<path>  GET，1.2.3 客戶端遊戲檔（主控台匯入的已簽清單裡的路徑、Range、每 IP 1／全體 4 個）。
 *                    見 {@link ClientFileEndpoint}。
 *
 * 動作放在 {@code net.swordie.ms.connection.api.launcher} 的登錄表，LocalAccountCli 共用；
 * 固定綁 127.0.0.1，處理器也只接受 127.0.0.1 的來源，設定與 JVM 參數不能開放對外，
 * 並拒絕 Host 不是 IP／localhost（DNS rebinding）或帶 Origin（瀏覽器）的請求；
 * 請求逾時 5 秒、回應逾時 1800 秒（2026-10-04 起；原本 10 秒，exe 下載需要；停住的下載 60 秒由下載端點自己結束）、最多 512 條連線；工作執行緒 32 條＋佇列 32，滿了直接關連線。
 * JDK 的 HttpServer 在工作執行緒上才讀標頭，白名單要等標頭讀完才能判斷，所以慢速連線靠逾時與執行緒數量處理，
 * 所有登入 API 僅在本機 IPv4 回送介面監聽。
 */
public final class HttpAuthApi {

    /** 本機登入 API 埠（登入器設定需一致）。 */
    public static final int PORT = 8600;
    /**
     * 2026-10-03 管理主控台：埠可用 {@code api.http.port}（server.properties，-D 優先）改，只給測試實例用（正式一律 8600，
     * 登入器寫死 8600）；不設＝8600。
     */
    public static final String PORT_KEY = "api.http.port";
    /** -Dtms282.api.legacyLoginRateLimit=false 關閉舊 /api/login 的限速與失敗退避（預設開）。 */
    public static final String LEGACY_LOGIN_LIMIT = "tms282.api.legacyLoginRateLimit";
    /**
     * sun.net.httpserver.maxReqTime: from the first byte until the request is read. Counts time queued for a worker
     * too, and stays well under cat-launcher's 8 s timeout, so a request held up by stalled connections still gets a
     * worker before the launcher gives up.
     */
    static final int REQUEST_TIMEOUT_SEC = 5;
    /**
     * sun.net.httpserver.maxRspTime: from the end of the request until the whole response is sent. 2026-10-04 (P8b):
     * 1800 s instead of 10 s, because the JDK applies it to every response and a launcher exe download (about 100 MB
     * over Radmin) takes minutes; a transfer that stops moving is ended after 60 s by the download endpoint itself,
     * and the API answers (a few KB) are written at once as before. A cut transfer is resumed by the launcher (Range).
     */
    static final int RESPONSE_TIMEOUT_SEC = 1800;
    /** jdk.httpserver.maxConnections: bounds the sockets a flood can hold in the game server process. */
    static final int MAX_CONNECTIONS = 512;
    /** Workers mostly wait on the network (headers are read on the worker), so several stalled peers leave room. */
    static final int WORKERS = 32;
    static final int QUEUE = 32;

    // token 打包規則，與 ApiRequestHandler.getTokenPacked 保持一致。
    private static final int RND_BITS = 11;
    private static final int USER_BITS = 19;
    private static final int USER_MASK = (1 << USER_BITS) - 1;
    private static final int PREFIX_SHIFT = USER_BITS + RND_BITS;
    private static final int USER_SHIFT = RND_BITS;
    private static final int PREFIX_VALUE = 1;

    private static final LauncherServices SERVICES = LauncherServices.forServer();
    private static final LauncherActionRegistry REGISTRY = LauncherActionRegistry.standard();
    /** 1.0.0 的 /api/v2/account 只開這三個動作。 */
    private static final LauncherActionRegistry ACCOUNT_V2_ACTIONS = REGISTRY.only("login", "register", "reset");
    /** P8b 登入器更新檔下載（每 IP 1 個、全體 4 個同時下載）。 */
    private static final LauncherDownloadEndpoint DOWNLOADS =
            new LauncherDownloadEndpoint(LauncherRelease::standard, SERVICES, DownloadSlots.shared());
    /** 1.2.3 客戶端遊戲檔下載（已簽清單登記的路徑；每 IP 1／全體 4 個同時下載；自己的 clientFile 限速桶）。 */
    private static final ClientFileEndpoint CLIENT_FILES =
            new ClientFileEndpoint(LauncherClientFiles::standard, SERVICES, DownloadSlots.client());
    /** 被擋下的請求（白名單、Host、Origin）每 IP 每分鐘最多記一行 log。 */
    private static final TokenBucketLimiter REFUSED_LOG =
            new TokenBucketLimiter(new TokenBucketLimiter.Policy(1, 60_000), System::currentTimeMillis);

    private static volatile HttpServer server;
    private static volatile ThreadPoolExecutor executor;

    private HttpAuthApi() {}

    public static synchronized void start() {
        start(new InetSocketAddress("127.0.0.1", configuredPort()));
    }

    /** {@value #PORT_KEY}（-D 優先，再來 server.properties）；沒設或不合法＝{@value #PORT}。 */
    public static int configuredPort() {
        String raw = System.getProperty(PORT_KEY);
        if (raw == null || raw.isBlank()) {
            raw = net.swordie.ms.console.config.ServerRuntimeConfig.stringValue(PORT_KEY);
        }
        try {
            int p = Integer.parseInt(raw == null ? "" : raw.trim());
            return p >= 1 && p <= 65535 ? p : PORT;
        } catch (NumberFormatException e) {
            return PORT;
        }
    }

    /** 只允許 127.0.0.1（測試可用埠 0）；其他位址直接拒絕。 */
    public static synchronized int start(InetSocketAddress address) {
        if (address == null || address.getAddress() == null
                || !"127.0.0.1".equals(address.getAddress().getHostAddress())) {
            throw new IllegalArgumentException("This local-only server binds exclusively to 127.0.0.1");
        }
        if (server != null) {
            return server.getAddress().getPort();
        }
        ThreadPoolExecutor pool = null;
        try {
            configureServerLimits();
            HttpServer s = HttpServer.create(address, 0);
            // 管理主控台的流量統計只旁觀（ApiTraffic 過濾器：路徑、X-Cat-Launcher 版本、狀態與 code），不改任何回應。
            ApiTraffic traffic = ApiTraffic.standard();
            traffic.watch(s.createContext("/api/login", guarded(HttpAuthApi::handleLogin)));
            traffic.watch(s.createContext("/api/register", guarded(HttpAuthApi::handleRegister)));
            traffic.watch(s.createContext("/api/ping", guarded(HttpAuthApi::handlePing)));
            traffic.watch(s.createContext("/api/v2/account", guarded(new AccountV2Endpoint(ACCOUNT_V2_ACTIONS, SERVICES))));
            traffic.watch(s.createContext("/api/v2/launcher", guarded(new LauncherHttpEndpoint(REGISTRY, SERVICES))));
            // P8b：登入器 exe 的下載（GET /api/v2/launcher/download/<登記的檔名>；同一個白名單、IP 桶，另有同時下載數上限）
            traffic.watch(s.createContext(LauncherDownloadEndpoint.PATH, guarded(DOWNLOADS)));
            // 1.2.3：客戶端遊戲檔（GET /api/v2/launcher/client/<版本>/<已簽清單裡的相對路徑>；同一個白名單）
            traffic.watch(s.createContext(ClientFileEndpoint.PATH, guarded(CLIENT_FILES)));
            // 2026-10-03 管理 API（/api/v2/admin）：只收 127.0.0.1＋管理權杖，自己的檢查，不經 api.http.allow；失敗不影響上面。
            try {
                AdminApi.install(s, s.getAddress().getPort(), SERVICES);
            } catch (RuntimeException | Error e) {
                System.out.printf("[AdminApi] 未啟用：%s%n", e);
            }
            pool = newExecutor();
            s.setExecutor(pool);
            s.start();
            server = s;
            executor = pool;
            System.out.printf("[HttpAuthApi] 本機登入 API 已啟動於 %s:%d（api=3，來源白名單 %s）%n",
                    address.getHostString(), s.getAddress().getPort(), SourceAllowlist.current());
            return s.getAddress().getPort();
        } catch (IOException e) {
            if (pool != null) {
                pool.shutdownNow();
            }
            System.out.printf("[HttpAuthApi] 啟動失敗: %s%n", e.getMessage());
            return -1;
        }
    }

    /** 測試用：停止並釋放執行緒。 */
    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** session、限速、失敗退避（主控台監看與測試用）。 */
    public static LauncherServices services() {
        return SERVICES;
    }

    public static LauncherActionRegistry registry() {
        return REGISTRY;
    }

    /**
     * sun.net.httpserver.maxReqTime / maxRspTime（秒）與 jdk.httpserver.maxConnections 只在第一次建立 HttpServer 時讀取一次，
     * 啟動參數由登入器決定，所以在這裡補上（已用 -D 指定時不覆寫）。
     */
    static void configureServerLimits() {
        setDefault("sun.net.httpserver.maxReqTime", REQUEST_TIMEOUT_SEC);
        setDefault("sun.net.httpserver.maxRspTime", RESPONSE_TIMEOUT_SEC);
        setDefault("jdk.httpserver.maxConnections", MAX_CONNECTIONS);
    }

    private static void setDefault(String key, int value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, String.valueOf(value));
        }
    }

    /**
     * 有界佇列：滿了 AbortPolicy 丟 RejectedExecutionException，HttpServer 的 Dispatcher 會直接關掉那條連線。
     * 閒置 60 秒的執行緒會結束（平常只留著有在用的幾條）。
     */
    static ThreadPoolExecutor newExecutor() {
        AtomicInteger n = new AtomicInteger();
        ThreadPoolExecutor pool = new ThreadPoolExecutor(WORKERS, WORKERS, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE), r -> {
            Thread t = new Thread(r, "HttpAuthApi-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        }, new ThreadPoolExecutor.AbortPolicy());
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    /**
     * 每個處理器都包一層，依序檢查：來源白名單（api.http.allow）、Host 必須是 IP／localhost（擋 DNS rebinding）、
     * 不可帶 Origin（瀏覽器發出的請求，含跨站的 text/plain 簡單請求）；不符合回 403。結束時一律關閉 exchange。
     */
    private static HttpHandler guarded(HttpHandler handler) {
        return ex -> {
            try {
                String refusal = null;
                if (!SourceAllowlist.current().allows(ex.getRemoteAddress().getAddress())) {
                    refusal = "source (" + SourceAllowlist.KEY + ")";
                } else if (!LauncherHttp.hostIsLiteral(ex.getRequestHeaders().get("Host"))) {
                    refusal = "host name";
                } else if (LauncherHttp.fromBrowser(ex)) {
                    refusal = "browser origin";
                }
                if (refusal != null) {
                    String ip = LauncherHttp.sourceIp(ex);
                    if (REFUSED_LOG.tryAcquire(ip)) {
                        System.out.printf("[HttpAuthApi] refused %s: %s%n", ip, refusal);
                    }
                    LauncherHttp.respond(ex, 403, "{\"ok\":false,\"code\":\"forbidden\"}");
                    return;
                }
                handler.handle(ex);
            } finally {
                ex.close();
            }
        };
    }

    private static String pingJson() {
        StringBuilder sb = new StringBuilder("{\"result\":\"ok\",\"api\":3,\"features\":[");
        List<String> features = REGISTRY.pingFeatures();
        for (int i = 0; i < features.size(); i++) {
            sb.append(i == 0 ? "\"" : ",\"").append(features.get(i)).append('"');
        }
        return sb.append("]}").toString();
    }

    private static final String PING = pingJson();

    private static void handlePing(HttpExchange ex) throws IOException {
        respond(ex, PING);
    }

    /** 每個來源 IP 的 token bucket（20 個，每 3 秒補 1 個＝原本的每分鐘 20 次）；閒置 IP 會定期清掉。 */
    private static boolean allow(String ip) {
        return SERVICES.rateLimits().tryAcquire(LauncherRateLimits.IP, ip);
    }

    private static boolean legacyLoginLimited() {
        return !"false".equalsIgnoreCase(System.getProperty(LEGACY_LOGIN_LIMIT, "true"));
    }

    private static void handleLogin(HttpExchange ex) throws IOException {
        String ip = LauncherHttp.sourceIp(ex);
        String outcome = "error";
        try {
            boolean limited = legacyLoginLimited();
            if (limited && !allow(ip)) {
                outcome = "busy";
                respond(ex, "{\"result\":\"busy\"}");
                return;
            }
            Map<String, String> p = params(ex);
            if (p == null) {
                respond(ex, "{\"result\":\"error\"}");
                return;
            }
            String name = p.getOrDefault("user", "").trim();
            String password = p.getOrDefault("pass", "");
            if (name.isEmpty() || password.isEmpty()) {
                outcome = "wrongPassword";
                respond(ex, "{\"result\":\"wrongPassword\"}");
                return;
            }
            if (limited) {
                // users.name (utf8mb4_unicode_ci) also matches accented / full-width spellings of the same account:
                // only the ASCII spelling (the CP0074 rule) may be tried, so the back-off below cannot be sidestepped.
                if (!AccountCredentials.validUsername(name)) {
                    outcome = "wrongPassword";
                    respond(ex, "{\"result\":\"wrongPassword\"}");
                    return;
                }
                if (!SERVICES.rateLimits().tryAcquire(LauncherRateLimits.AUTH, ip)
                        || SERVICES.loginBackoff().retryAfterMs(name, ip) > 0) {
                    outcome = "busy";
                    respond(ex, "{\"result\":\"busy\"}");
                    return;
                }
            }
            User user = User.getUserFromSQLForAPI(name);
            boolean success;
            String dbPassword = user == null ? null : user.getPassword();
            if (dbPassword != null && !Util.isStringBCrypt(dbPassword)) {
                // Legacy plain-text row: the same comparison as before, in the time of a BCrypt check.
                AccountSecrets.spendVerifyTime(password);
                success = password.equals(dbPassword);
            } else {
                success = AccountSecrets.verifyPassword(password, dbPassword);
            }
            if (!success) {
                if (limited) {
                    SERVICES.loginBackoff().failure(name, ip);
                }
                outcome = "wrongPassword";
                respond(ex, "{\"result\":\"wrongPassword\"}");
                return;
            }
            if (limited) {
                SERVICES.loginBackoff().success(name, ip);
            }
            if (user.getBanExpireDate() != null && !user.getBanExpireDate().isExpired()) {
                outcome = "banned";
                respond(ex, "{\"result\":\"banned\"}");
                return;
            }
            if (Server.get().getUserById(user.getId()) != null) {
                outcome = "alreadyLogin";
                respond(ex, "{\"result\":\"alreadyLogin\"}");
                return;
            }
            int token = genToken(user.getId());
            Server.get().addAuthToken(token, user.getId());
            outcome = "success";
            respond(ex, "{\"result\":\"success\",\"token\":" + token + "}");
        } catch (Exception e) {
            respond(ex, "{\"result\":\"error\"}");
        } finally {
            // 不印帳號與 token。
            System.out.printf("[HttpAuthApi] legacy login %s -> %s%n", ip, outcome);
        }
    }

    private static void handleRegister(HttpExchange ex) throws IOException {
        if (!Boolean.getBoolean("tms282.api.legacyRegister")) {
            // 舊註冊不會設定第二組密碼（pic），282 在 CP007C 會丟 IOException 斷線；改用 /api/v2/account。
            respond(ex, "{\"result\":\"unknown\"}");
            return;
        }
        String ip = LauncherHttp.sourceIp(ex);
        try {
            if (!allow(ip)) {
                respond(ex, "{\"result\":\"unknown\"}");
                return;
            }
            Map<String, String> p = params(ex);
            if (p == null) {
                respond(ex, "{\"result\":\"unknown\"}");
                return;
            }
            String name = p.getOrDefault("user", "").trim();
            String pwd = p.getOrDefault("pass", "");
            String email = p.getOrDefault("email", "");
            if (name.isEmpty() || pwd.isEmpty()) {
                respond(ex, "{\"result\":\"unknown\"}");
                return;
            }
            if (DevAccounts.isForbiddenID(name)) {
                respond(ex, "{\"result\":\"unknown\"}");
                return;
            }
            if (User.getUserFromSQLForAPI(name) != null) {
                respond(ex, "{\"result\":\"nameInUse\"}");
                return;
            }
            User user = new User(name, pwd);
            user.setPassword(BCrypt.hashpw(pwd, BCrypt.gensalt(ServerConstants.BCRYPT_ITERATIONS)));
            user.setEmail(email);
            user.insertUserToSQL();
            System.out.printf("[HttpAuthApi] legacy register %s -> success%n", ip);
            respond(ex, "{\"result\":\"success\"}");
        } catch (Exception e) {
            respond(ex, "{\"result\":\"error\"}");
        }
    }

    private static int genToken(int userId) {
        if ((userId & ~USER_MASK) != 0) {
            throw new IllegalArgumentException("userId too large for packed token");
        }
        for (int i = 0; i < 64; i++) {
            int rnd = ThreadLocalRandom.current().nextInt(1 << RND_BITS);
            int token = (PREFIX_VALUE << PREFIX_SHIFT) | (userId << USER_SHIFT) | rnd;
            if (!Server.get().isTokenExist(token)) {
                return token;
            }
        }
        return ThreadLocalRandom.current().nextInt(1_000_000_000, Integer.MAX_VALUE);
    }

    /** query＋form body（最多 8 KB）；body 太大回 null。 */
    private static Map<String, String> params(HttpExchange ex) throws IOException {
        Map<String, String> m = new HashMap<>();
        parseInto(m, ex.getRequestURI().getRawQuery());
        byte[] body = LauncherHttp.readBody(ex, LauncherHttp.MAX_BODY);
        if (body == null) {
            return null;
        }
        if (body.length > 0) {
            parseInto(m, new String(body, StandardCharsets.UTF_8));
        }
        return m;
    }

    private static void parseInto(Map<String, String> m, String s) {
        if (s == null || s.isEmpty()) {
            return;
        }
        for (String kv : s.split("&")) {
            int i = kv.indexOf('=');
            if (i < 0) {
                continue;
            }
            m.put(urlDecode(kv.substring(0, i)), urlDecode(kv.substring(i + 1)));
        }
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static void respond(HttpExchange ex, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(200, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }
}
