package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Made-up answers of every admin action, for the screenshot mode ({@code --screenshots}): long Chinese names, many
 * rows and every state, so clipped or overlapping text shows up. Never talks to a server.
 */
final class FakeAdminGateway implements AdminGateway {
    private final long now = System.currentTimeMillis();
    private final boolean down;
    /** onlinereward.save remembers the settings for the next onlinereward.get (fixes10). */
    private volatile Map<String, Object> onlineConfig;

    FakeAdminGateway(boolean down) {
        this.down = down;
    }

    @Override
    public String describe() {
        return "127.0.0.1:8600";
    }

    @Override
    public JsonNode call(String action, Map<String, Object> params) throws AdminCallException {
        if (down) {
            throw new AdminCallException(AdminCallException.Kind.SERVER_DOWN, "伺服器未啟動（127.0.0.1:8600 沒有回應）");
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("ok", true);
        r.put("code", "ok");
        switch (action) {
            case "server.ping" -> {
                r.put("pid", 65640);
                r.put("uptimeSec", 93_784);
            }
            case "api.status" -> apiStatus(r);
            case "account.find" -> account(r);
            case "news.get" -> news(r);
            case "daily.rewards" -> rewards(r);
            case "daily.save" -> { r.put("version", "edited-daily"); r.put("refreshed", 3); }
            case "daily.status" -> daily(r);
            case "net.get" -> net(r);
            case "save.status" -> save(r);
            case "release.get" -> release(r);
            case "release.validate" -> {
                r.put("raise", true);
                r.put("fromMin", "1.1.0");
            }
            case "release.save" -> {
                if (!Boolean.TRUE.equals(params.get("confirmRaise"))) {
                    r.put("ok", false);
                    r.put("code", "confirmRequired");
                    r.put("problem", "minSupported.raise");
                    r.put("fromMin", "1.1.0");
                    r.put("minSupported", params.get("minSupported"));
                } else {
                    r.put("sha256", "B1B2C3D4E5F60718293A4B5C6D7E8F90A1B2C3D4E5F60718293A4B5C6D7E8F90");
                    r.put("backup", "D:\\TMS\\server\\265\\data\\launcher\\release.json.bak");
                    r.put("info", m("minSupported", params.get("minSupported")));
                    r.put("reloadSec", 5);
                }
            }
            case "gift.list" -> giftList(r);
            case "gift.itemSearch" -> giftSearch(r);
            case "gift.redemptions" -> giftRedemptions(r);
            case "gift.queue" -> giftQueue(r);
            case "gift.create" -> {
                r.put("ids", List.of(41, 42, 43));
                r.put("codes", List.of("7KQ2-XHM9-P4TZ-W8RD", "N3VB-6YCF-QJ2L-ZT5H", "D8MW-R4EK-2GXP-HU7S"));
                r.put("batchId", "mfx2k1-AB12");
            }
            case "gift.disable" -> {
                r.put("id", params.get("id"));
                r.put("changed", true);
            }
            case "newbie.get", "newbie.reload" -> newbie(r);
            case "newbie.progress" -> newbieProgress(r);
            case "onlinereward.get" -> onlineReward(r, onlineConfig);
            case "onlinereward.save" -> {
                @SuppressWarnings("unchecked") Map<String, Object> c = (Map<String, Object>) params.get("config");
                onlineConfig = c == null ? null : new LinkedHashMap<>(c);
                onlineReward(r, onlineConfig);
            }
            case "newbie.jobs" -> r.put("jobs", List.of(m("job",100,"name","劍士","count",12),m("job",14200,"name","凱內西斯","count",3)));
            case "clientfiles.get" -> clientFiles(r);
            case "clientfiles.import" -> { r.put("started", true); r.put("version", "1.0.1"); r.put("count", 3); r.put("totalSize", 279_467_010L); }
            case "clientfiles.disable" -> r.put("reloadSec", 5);
            case "release.import" -> {
                r.put("file", "CatLauncher-1.2.1-portable.exe");
                r.put("copied", true);
                r.put("reloadSec", 5);
            }
            case "ops.health" -> {
                r.put("pid", 65640); r.put("uptimeSec", 93_784); r.put("heapUsedMb", 2310); r.put("heapCommittedMb", 4096);
                r.put("heapMaxMb", 8192); r.put("threads", 214); r.put("peakThreads", 260); r.put("gcCount", 412);
                r.put("gcMs", 18_300); r.put("processors", 16); r.put("loadAverage", -1.0); r.put("onlineUsers", 3);
                r.put("onlineCharacters", 2); r.put("connections", 5); r.put("now", now);
            }
            case "ops.online" -> {
                r.put("characters", List.of(
                        m("id", 68, "name", "雪花獵人", "job", 3312, "level", 300, "field", 100000000, "channel", 1, "user", 9, "ip", "127.0.0.1"),
                        m("id", 72, "name", "森林琳恩", "job", 17210, "level", 30, "field", 100010000, "channel", 1, "user", 12, "ip", "26.235.100.235")));
                r.put("users", 3); r.put("now", now);
            }
            case "ops.kick", "char.home" -> r.put("character", 68);
            case "char.cooldowns" -> { r.put("character", 68); r.put("cleared", 2); }
            case "ops.issuesClear" -> { }
            case "char.kit" -> {
                r.put("kit", m("source", "o清歌遠上o", "level", 296, "job", 6412, "hair", 47280, "face", 53690, "skin", 15, "equips", 28, "cash", 11,
                        "skipped", List.of("寶石 伊妮絲的寶玉"), "unmappedPotentials", 0));
                r.put("character", 68); r.put("name", "雪花獵人");
                r.put("done", List.of("職業 6412、等級 296、技能全滿", "一般裝備 28／28 件", "時裝 11／11 件")); r.put("failed", List.of());
            }
            case "ops.restartNotice" -> r.put("restart", m("at", now + 300_000L, "message", "伺服器即將維護更新，請先存檔下線。"));
            case "ops.reports" -> r.put("reports", List.of(
                    m("id", "20261007-171502-u9-3fa1", "time", now - 120_000L, "user", 9, "username", "snowhunter", "text", "琳恩的集中治癒按了沒有反應，其他技能正常。",
                            "mode", "local", "version", "1.2.8", "exit", "", "issues", 2),
                    m("id", "20261007-160233-u12-07bc", "time", now - 4_400_000L, "user", 12, "username", "forestlynn", "text", "打到一半遊戲直接關掉，沒有錯誤視窗。",
                            "mode", "remote", "version", "1.2.8", "exit", "crashed", "issues", 0)));
            case "ops.report" -> r.put("report", m("id", "20261007-171502-u9-3fa1", "time", now - 120_000L, "user", 9, "username", "snowhunter",
                    "ip", "127.0.0.1", "text", "琳恩的集中治癒按了沒有反應，其他技能正常。",
                    "client", m("version", "1.2.8", "mode", "local", "gameRunning", true),
                    "characters", List.of(m("id", 72, "name", "森林琳恩", "job", 17210, "level", 30, "field", 100010000)),
                    "issues", List.of(m("kind", "技能被拒絕", "subject", "CP04B4", "character", 72, "skill", 172111002, "reason", "character does not have the skill",
                            "count", 3, "last", now - 180_000L, "sample", "")),
                    "log", "2026-10-07T09:14:58Z [game] opened pid=24120 mode=local\n2026-10-07T09:15:01Z [game] window pid=24120"));
            case "ops.issues" -> {
                r.put("issues", List.of(
                        m("kind", "技能被拒絕", "subject", "CP04B4", "character", 69, "skill", 152101000, "reason", "character does not have the skill",
                                "count", 5, "first", now - 900_000L, "last", now - 60_000L,
                                "sample", "[TMS282] REJECTED CP04B4 skill=152101000 slv=0 (packet slv 1): character does not have the skill; SENT LP009D unlock character=69"),
                        m("kind", "封包被擋", "subject", "GUIDE_FORCE_ATOM op=0x0312", "character", 0, "skill", 0, "reason", "no tms282.3 opcode for GUIDE_FORCE_ATOM",
                                "count", 16, "first", now - 800_000L, "last", now - 120_000L,
                                "sample", "[OutboundGuard] #40 dropped GUIDE_FORCE_ATOM op=0x0312 len=26 to broadcast:Fields: no tms282.3 opcode for GUIDE_FORCE_ATOM"),
                        m("kind", "沒有效果", "subject", "", "character", 68, "skill", 400031074, "reason", "no-CTS",
                                "count", 1, "first", now - 300_000L, "last", now - 300_000L,
                                "sample", "[TMS282] CP04B4 skill=400031074 slv=30 mpCon=1000 hpCon=0 cooldown=120s kind=no-CTS character=68")));
                r.put("clearedAt", now - 3_600_000L); r.put("now", now);
            }
            case "char.inspect" -> {
                r.put("character", m("id", 68, "name", "雪花獵人", "job", 3312, "level", 300, "field", 100000000, "channel", 1, "user", 9, "ip", "127.0.0.1"));
                r.put("hp", 84950); r.put("maxHp", 84950); r.put("mp", 37537); r.put("maxMp", 39617);
                r.put("skills", List.of(m("id", 33001000, "level", 20, "master", 20), m("id", 33100002, "level", 10, "master", 10),
                        m("id", 400031074, "level", 30, "master", 30)));
                r.put("cooldowns", List.of(m("skill", 400031074, "leftSec", 87), m("skill", 33141007, "leftSec", 12)));
                r.put("linkSkills", List.of(m("owner", 57, "skill", 80002857, "level", 3, "linkedTo", 68), m("owner", 62, "skill", 80000514, "level", 3, "linkedTo", 0)));
            }
            default -> {
                r.put("ok", false);
                r.put("code", "invalidInput");
            }
        }
        return HttpAdminGateway.JSON.valueToTree(r);
    }

    private void newbie(Map<String, Object> r) {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(m("id", "spawn", "type", "Spawn", "detail", "預設：新手大廳 100000001；例外 {KINESIS=-2}"));
        steps.add(m("id", "story", "type", "StoryDialog", "detail", "3 頁；依種族 [KINESIS]"));
        steps.add(m("id", "firstJob", "type", "FirstJob", "detail", "18 種初始職業，等級補到 10"));
        steps.add(m("id", "gift", "type", "StarterGift", "detail", "楓幣 500000，共通 5 項，裝備組 [warrior, magician, bowman, crossbow, thief, pirate, *]"));
        steps.add(m("id", "warp", "type", "WarpMenu", "detail", "前往主城：弓箭手村 100000000、前往推薦練功地圖：弓箭手村北邊山丘 100010000"));
        r.put("flow", m("state", "invalid", "problem", "steps[3].items[2].itemId: must be 1000000..9999999", "file", "data\\newbie\\newbie-flow.json",
                "loadedAt", now - 3_600_000, "warnings", List.of(), "version", "2026-10-04.1", "enabled", true, "lobbyMap", 100000001,
                "lobbyPortal", 0, "speakerNpc", 9010000, "popupDelayMs", 2500, "resumeOnLogin", true,
                "summary", "version 2026-10-04.1, lobby 100000001", "steps", steps));
        r.put("fileText", "{\n  \"version\": \"2026-10-04.1\",\n  \"enabled\": true,\n  \"lobbyMap\": 100000001,\n  ...\n}\n");
    }

    private void newbieProgress(Map<String, Object> r) {
        List<Map<String, Object>> rows = new ArrayList<>();
        String[] names = {"新手冒險家一號", "凱內西斯的超能力者", "阿蘭", "Coju", "暗夜行者練習中"};
        String[] steps = {"story", "firstJob", "gift", "warp", ""};
        for (int i = 0; i < 24; i++) {
            boolean done = i % 5 == 4;
            rows.add(m("characterId", 100 + i, "name", names[i % names.length], "level", 10 + i % 3, "job", i % 2 == 0 ? 100 : 14200,
                    "posMap", done ? 100000000 : 100000001, "loginJob", i % 2 == 0 ? "EXPLORER" : "KINESIS", "stepId", steps[i % steps.length],
                    "branch", i % 3 == 0 ? "warrior" : "", "giftClaimedAt", i % 5 >= 3 ? now - i * 60_000L : 0,
                    "jobDoneAt", i % 5 >= 2 ? now - i * 61_000L : 0, "completedAt", done ? now - i * 30_000L : 0,
                    "createdAt", now - i * 90_000L, "updatedAt", now - i * 20_000L));
        }
        r.put("rows", rows);
    }

    /** 1.2.3 客戶端檔案：1.0.0 發佈中（3 個檔），dist-release\client 有 1.0.1 可以匯入、0.9.0 是測試金鑰 */
    private void clientFiles(Map<String, Object> r) {
        List<Map<String, Object>> files = List.of(
                m("path", "Data/String/String_000.wz", "size", 18_186_068L, "sha256", "091ea5dbe671903abecf1375fcfbcebf53857a8387b639723ca3bc7892228130"),
                m("path", "Data/UI/UI_000.wz", "size", 75_061_454L, "sha256", "827dae38f5d6975e74ecf4b2a4933555c20b78d0919ee3d54d4f7294c8453fa5"),
                m("path", "MapleStory.exe", "size", 186_219_488L, "sha256", "ef8bb4265463cad5bbf04544f57278f7d69c9a2657ec684175496ff389b00548"));
        r.put("dir", "D:\\TMS\\server\\265\\data\\launcher\\client-files");
        r.put("distDir", "D:\\TMS\\launcher\\cat-launcher\\dist-release\\client");
        r.put("live", m("state", "ok", "problem", null, "activeVersion", "1.0.0", "summary", m("version", "1.0.0", "count", 3, "totalSize", 279_467_010L,
                "listSha256", "0".repeat(64)), "files", files, "test", false, "keyId", "354fc76104c76e0c"));
        r.put("imported", List.of(m("version", "1.0.0", "count", 3, "totalSize", 279_467_010L, "test", false, "problem", null)));
        r.put("candidates", List.of(m("version", "1.0.1", "count", 3, "totalSize", 279_467_010L, "test", false, "keyId", "354fc76104c76e0c", "problem", null),
                m("version", "0.9.0", "count", 1, "totalSize", 1_024L, "test", true, "keyId", "5ebf0a2775743999", "problem", null)));
        r.put("job", m("state", "idle", "version", null, "done", 0, "total", 0, "bytes", 0L, "problem", null, "at", 0L));
        r.put("downloads", m("active", List.of(), "started", 4L, "refused", 0L));
        r.put("allowTestKey", false);
    }

    /** 「上線獎勵」 (fixes10): settings, one day's accounts, newest receipts. */
    private static void onlineReward(Map<String, Object> r, Map<String, Object> savedConfig) {
        boolean saved = savedConfig != null;
        Map<String, Object> cfg = saved ? savedConfig : m("enabled", true, "intervalMinutes", 30, "pointsPerInterval", 5, "dailyCapPoints", 60, "afkMinutes", 10,
                "notice", "[累積上線獎勵] 今日已上線 {minutes} 分鐘，獲得 {points} 樂豆點（今日 {today}/{cap}）。");
        r.put("config", cfg);
        r.put("defaults", m("enabled", true, "intervalMinutes", 30, "pointsPerInterval", 5, "dailyCapPoints", 60, "afkMinutes", 10,
                "notice", "[累積上線獎勵] 今日已上線 {minutes} 分鐘，獲得 {points} 樂豆點（今日 {today}/{cap}）。"));
        r.put("state", saved ? "file" : "default");
        r.put("problem", "");
        r.put("file", "D:\\TMS\\server\\265\\data\\tms282-online-reward\\online-reward.json");
        r.put("version", saved ? "saved-online-reward" : "missing");
        r.put("day", "2026-10-05");
        r.put("today", "2026-10-05");
        r.put("zone", "Asia/Taipei");
        r.put("tables", true);
        long now = 1_791_200_000_000L;
        r.put("totals", m("accounts", 3, "minutes", 412, "points", 125));
        r.put("accounts", List.of(
                m("userId", 12, "name", "catmaple", "minutes", 245, "points", 40, "updatedAt", now, "online", true),
                m("userId", 7, "name", "admin", "minutes", 125, "points", 20, "updatedAt", now - 600_000, "online", false),
                m("userId", 31, "name", "pudding", "minutes", 42, "points", 5, "updatedAt", now - 3_600_000, "online", true)));
        r.put("log", List.of(
                m("id", 503, "userId", 12, "name", "catmaple", "day", "2026-10-05", "amount", 5, "pointsAfter", 40, "minutes", 240, "balanceAfter", 1_340, "grantedAt", now - 300_000),
                m("id", 502, "userId", 7, "name", "admin", "day", "2026-10-05", "amount", 5, "pointsAfter", 20, "minutes", 120, "balanceAfter", 88, "grantedAt", now - 900_000)));
    }

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            out.put((String) kv[i], kv[i + 1]);
        }
        return out;
    }

    private void apiStatus(Map<String, Object> r) {
        r.put("apiPort", 8600);
        r.put("uptimeSec", 93_784);
        r.put("sessions", 7);
        r.put("requests", 12_847);
        r.put("byVersion", m("-", 2_113, "1.1.0", 8_204, "1.2.0-beta.3", 2_530));
        r.put("byPath", m("/api/ping", 3_100, "/api/v2/account", 2_113, "/api/v2/launcher", 7_634));
        r.put("byCode", m("ok", 9_010, "loggedIn", 1_204, "wrongCredentials", 318, "rateLimited", 71, "busy", 22, "needSession", 120,
                "sessionExpired", 33, "forbidden", 41, "unavailable", 2));
        List<Object> b = new ArrayList<>();
        b.add(m("bucket", "ip", "capacity", 20, "refillMs", 3000, "refused", 22, "tracked", 4));
        b.add(m("bucket", "auth", "capacity", 10, "refillMs", 6000, "refused", 49, "tracked", 3));
        b.add(m("bucket", "register", "capacity", 5, "refillMs", 60000, "refused", 0, "tracked", 1));
        b.add(m("bucket", "session", "capacity", 30, "refillMs", 2000, "refused", 0, "tracked", 2));
        b.add(m("bucket", "read", "capacity", 30, "refillMs", 2000, "refused", 22, "tracked", 5));
        r.put("rateLimits", b);
        r.put("rateLimitedTotal", 93);
        r.put("launcherBackoff", m("pairs", 3, "locked", 1));
        r.put("gameLoginLocked", 2);
        r.put("httpAllow", m("key", "api.http.allow", "source", "file", "fileValue", "127.0.0.1,26.0.0.0/8,192.168.1.0/24",
                "default", "127.0.0.1,26.0.0.0/8", "rules", List.of("127.0.0.1/32", "26.0.0.0/8", "192.168.1.0/24"), "rejected", List.of()));
        r.put("adminRefused", 3);
        r.put("legacyApi", m("enabled", false, "port", 8483));
        List<Object> rec = new ArrayList<>();
        String[][] rows = {{"26.41.170.9", "/api/v2/launcher", "200", "wrongCredentials", "1.2.0-beta.3"},
                {"26.41.170.9", "/api/v2/launcher", "200", "rateLimited", "1.2.0-beta.3"},
                {"192.168.1.40", "/api/v2/account", "200", "busy", "-"},
                {"26.88.3.120", "/api/v2/launcher", "200", "sessionExpired", "1.1.0"},
                {"203.0.113.77", "/api/ping", "403", "forbidden", "-"},
                {"26.88.3.120", "/api/v2/launcher", "400", "launcherRequired", "-"},
                {"127.0.0.1", "/api/v2/launcher", "200", "unavailable", "1.1.0"},
                {"26.12.34.56", "/api/v2/launcher", "200", "needSession", "1.1.0"}};
        long t = now - 40_000;
        for (String[] x : rows) {
            rec.add(m("at", t, "ip", x[0], "path", x[1], "status", Integer.parseInt(x[2]), "code", x[3], "version", x[4]));
            t -= 95_000;
        }
        r.put("recent", rec);
    }

    private void account(Map<String, Object> r) {
        r.put("account", m("id", 12, "name", "SnowyMapleHero", "createdAt", now - 40L * 86_400_000, "vipGrade", 3, "characterSlots", 12,
                "accountType", 0, "banExpireAt", null, "banReason", null, "matchedBy", "character"));
        r.put("online", true);
        r.put("sessions", 2);
        List<Object> c = new ArrayList<>();
        c.add(m("id", 105, "name", "雪花冒險家", "level", 236, "jobName", "冒險家 主教", "world", 0, "lastLogout", null, "online", true, "deleting", false));
        c.add(m("id", 131, "name", "竈門炭治郎", "level", 210, "jobName", "竈門炭治郎（合作職業）", "world", 55, "lastLogout", now - 86_400_000L, "online", false, "deleting", false));
        c.add(m("id", 140, "name", "一拳超人埼玉", "level", 201, "jobName", "埼玉（合作職業）", "world", 55, "lastLogout", now - 3 * 86_400_000L, "online", false, "deleting", false));
        c.add(m("id", 150, "name", "練功小號", "level", 33, "jobName", "米哈逸", "world", 0, "lastLogout", now - 9 * 86_400_000L, "online", false, "deleting", true));
        r.put("characters", c);
        r.put("lastPlayed", c.get(0));
        List<Object> l = new ArrayList<>();
        l.add(m("kind", "launcher", "ip", "26.41.170.9", "failures", 6, "lockedForSec", 52, "lastFailure", now - 8_000));
        l.add(m("kind", "game", "ip", "192.168.1.40", "failures", 7, "lockedForSec", 118, "lastFailure", now - 2_000));
        l.add(m("kind", "game", "ip", "127.0.0.1", "failures", 2, "lockedForSec", 0, "lastFailure", now - 600_000));
        r.put("locks", l);
        List<Object> b = new ArrayList<>();
        b.add(m("typeLabel", "自動程式/測謊未過", "createdAt", now - 20L * 86_400_000, "endTime", now - 19L * 86_400_000, "permanent", false,
                "active", false, "reason", "測謊未過三次 (by admin console)"));
        b.add(m("typeLabel", "禁言", "createdAt", now - 3_600_000, "endTime", now + 20 * 3_600_000L, "permanent", false, "active", true,
                "reason", "頻道洗版"));
        r.put("bans", b);
        r.put("legacyBanActive", false);
        List<Object> types = new ArrayList<>();
        String[][] tt = {{"GENERAL", "一般封鎖", "true"}, {"HACK", "外掛/修改", "true"}, {"MACRO", "自動程式/測謊未過", "true"},
                {"RMT", "現金交易/詐騙", "true"}, {"CHAT", "禁言", "false"}, {"TRADE", "禁止交易", "false"}};
        int v = 0;
        for (String[] t : tt) {
            types.add(m("val", v++, "name", t[0], "label", t[1], "blocksLogin", Boolean.parseBoolean(t[2])));
        }
        r.put("banTypes", types);
    }

    private void news(Map<String, Object> r) {
        List<Object> items = new ArrayList<>();
        items.add(m("id", "welcome", "kind", "up", "title", "歡迎使用新版登入器", "body", "登入後可以直接看到自己的角色、簽到進度與伺服器公告。\n簽到請進遊戲領取。",
                "startAt", "2026-10-01", "endAt", null, "pinned", true));
        items.add(m("id", "oct-update", "kind", "up", "title", "10 月更新說明：登入器角色清單與每日簽到集章", "body", "本次更新：登入器可顯示角色清單與每日簽到集章。",
                "startAt", "2026-10-01T00:00", "endAt", "2026-10-31", "pinned", false));
        items.add(m("id", "maint-notice-1015", "kind", "mt", "title", "10/15（四）凌晨例行維修預告", "body", "預計 03:00–04:00 維修，維修期間無法登入遊戲。",
                "startAt", "2026-10-10", "endAt", "2026-10-15T04:00", "pinned", false));
        items.add(m("id", "halloween", "kind", "ev", "title", "萬聖節活動：南瓜大作戰", "body", "",
                "startAt", "2026-10-25", "endAt", "2026-11-02", "pinned", false));
        r.put("file", "D:\\TMS\\server\\265\\data\\launcher\\news.json");
        r.put("exists", true);
        r.put("sha256", "0A06520DAF75E9C0A5AC84DA7CD42164B22AB0A45D191AFFCC429BDC0813BA98");
        r.put("items", items);
        r.put("problem", null);
        List<Object> preview = new ArrayList<>();
        preview.add(m("id", "welcome", "kind", "up", "title", "歡迎使用新版登入器", "body", "登入後可以直接看到自己的角色、簽到進度與伺服器公告。\n簽到請進遊戲領取。",
                "startAt", now - 2 * 86_400_000L, "endAt", null, "pinned", true, "state", "shown"));
        preview.add(m("id", "maint-notice-1015", "kind", "mt", "title", "10/15（四）凌晨例行維修預告", "body", "預計 03:00–04:00 維修，維修期間無法登入遊戲。",
                "startAt", now + 6 * 86_400_000L, "endAt", now + 11 * 86_400_000L, "pinned", false, "state", "scheduled"));
        preview.add(m("id", "halloween", "kind", "ev", "title", "萬聖節活動：南瓜大作戰", "body", "",
                "startAt", now + 21 * 86_400_000L, "endAt", now + 29 * 86_400_000L, "pinned", false, "state", "scheduled"));
        preview.add(m("id", "oct-update", "kind", "up", "title", "10 月更新說明：登入器角色清單與每日簽到集章", "body", "本次更新：登入器可顯示角色清單與每日簽到集章。",
                "startAt", now - 2 * 86_400_000L, "endAt", now + 27 * 86_400_000L, "pinned", false, "state", "shown"));
        r.put("preview", preview);
        // what the launchers get: the file's items plus ones the server generates (event, maintenance, slide)
        List<Object> live = new ArrayList<>(preview);
        live.add(m("id", "event-2026-10-burning", "kind", "ev", "title", "進行中的遊戲活動：燃燒加速器（10/01～10/31）", "body", "",
                "startAt", now - 3 * 86_400_000L, "endAt", now + 27 * 86_400_000L, "pinned", false));
        live.add(m("id", "maintenance", "kind", "mt", "title", "伺服器維修中，暫停登入（預計 30 分鐘）", "body", "",
                "startAt", now - 600_000L, "endAt", null, "pinned", true));
        r.put("live", live);
        r.put("liveStatus", "ok");
        r.put("reloadSec", 5);
    }

    private void rewards(Map<String, Object> r) {
        String[] names = {"楓葉祝福藥水", "經驗值加倍券（30 分鐘）", "高級神奇魔方", "楓幣袋（100 萬）", "傳說潛能卷軸 50%", "寵物食品", "回城卷軸",
                "第 7 天大獎：鳳凰椅子", "強化防具卷軸 70%", "紅色魔方", "星力 17 星強化券", "黃金鐵鎚 100%", "萬能藥水", "第 14 天大獎：火紅玫瑰稱號",
                "名譽之石", "附加潛能卷軸", "經驗值加倍券（1 小時）", "高級寵物食品", "傳說武器箱子", "楓幣袋（500 萬）", "第 21 天大獎：大天使戒指（7 天）",
                "高級神奇魔方", "黑色魔方", "星之力 22 星強化券", "閃耀方塊", "神秘禮盒", "記憶之石", "第 28 天大獎：漆黑的 BOSS 飾品箱"};
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            list.add(m("day", i + 1, "itemId", 2_000_000 + i * 1_311, "count", i % 7 == 6 ? 1 : (i % 3) + 1,
                    "periodDays", i % 9 == 8 ? 7 : 0, "name", names[i]));
        }
        r.put("rewards", list);
        r.put("version", "3f9a1c0be2d47a65");
        r.put("file", "D:\\TMS\\server\\265\\data\\protocol\\tms282.3\\daily-gift.tsv");
    }

    private void daily(Map<String, Object> r) {
        r.put("account", m("id", 12, "name", "SnowyMapleHero"));
        LocalDate today = LocalDate.now();
        YearMonth month = YearMonth.from(today);
        List<Object> worlds = new ArrayList<>();
        List<String> d0 = new ArrayList<>();
        for (int d = 1; d < today.getDayOfMonth(); d += 1) {
            if (d % 5 != 0) {
                d0.add(month.atDay(d).toString());
            }
        }
        worlds.add(m("world", 0, "hasAccount", true, "source", "memory", "today", today.toString(), "month", month.toString(),
                "claimedCount", d0.size() + 1, "claimedToday", false, "monthComplete", false,
                "nextReward", m("day", d0.size() + 2, "name", "黃金鐵鎚 100%", "count", 1), "claimDates", d0, "undatedCount", 1,
                "logAvailable", true));
        worlds.add(m("world", 55, "hasAccount", true, "source", "database", "today", today.toString(), "month", month.toString(),
                "claimedCount", 2, "claimedToday", true, "monthComplete", false,
                "nextReward", m("day", 3, "name", "高級神奇魔方", "count", 1),
                "claimDates", List.of(month.atDay(1).toString(), today.toString()), "undatedCount", 0, "logAvailable", true));
        r.put("worlds", worlds);
    }

    private void net(Map<String, Object> r) {
        r.put("configFile", "D:\\TMS\\server\\265\\config\\server.properties");
        r.put("sha256", "C0FFEE");
        r.put("settings", List.of(
                m("key", "net.allow", "fileValue", "127.0.0.1,26.0.0.0/8,192.168.1.0/24", "effective", "127.0.0.1/32,26.0.0.0/8,192.168.1.0/24",
                        "default", "127.0.0.1,26.0.0.0/8", "jvmOverride", false),
                m("key", "net.maxPerIp", "fileValue", null, "effective", "16", "default", "16", "jvmOverride", false),
                m("key", "net.login.idleSec", "fileValue", "60", "effective", "60", "default", "60", "jvmOverride", false),
                m("key", "api.http.allow", "fileValue", null, "effective", "127.0.0.1/32,26.0.0.0/8", "default", "127.0.0.1,26.0.0.0/8",
                        "jvmOverride", false)));
        r.put("refused", List.of(
                m("ip", "100.101.102.103", "count", 14, "reason", "NOT_ALLOWED", "port", 8484, "lastAt", now - 30_000, "allowedNow", false),
                m("ip", "203.0.113.77", "count", 1_208, "reason", "NOT_ALLOWED", "port", 8585, "lastAt", now - 300_000, "allowedNow", false),
                m("ip", "26.41.170.9", "count", 3, "reason", "TOO_MANY", "port", 8484, "lastAt", now - 3_600_000, "allowedNow", true)));
        r.put("refusedTotal", 1_225);
        r.put("refusedCapacity", 64);
        r.put("interfaces", List.of(
                m("name", "eth3", "displayName", "Radmin VPN", "category", "radmin", "addresses",
                        List.of(m("ip", "26.12.34.56", "v4", true, "netAllow", true, "httpAllow", true))),
                m("name", "wlan0", "displayName", "Intel(R) Wi-Fi 6 AX201 160MHz", "category", "lan", "addresses",
                        List.of(m("ip", "192.168.1.23", "v4", true, "netAllow", true, "httpAllow", false))),
                m("name", "eth7", "displayName", "Tailscale Tunnel", "category", "tailscale", "addresses",
                        List.of(m("ip", "100.88.12.4", "v4", true, "netAllow", false, "httpAllow", false))),
                m("name", "lo", "displayName", "Software Loopback Interface 1", "category", "loopback", "addresses",
                        List.of(m("ip", "127.0.0.1", "v4", true, "netAllow", true, "httpAllow", true)))));
        r.put("listeners", List.of(m("purpose", "login", "address", "0.0.0.0", "port", 8484), m("purpose", "channel1", "address", "0.0.0.0", "port", 8585),
                m("purpose", "channel2", "address", "0.0.0.0", "port", 8586), m("purpose", "channel3", "address", "0.0.0.0", "port", 8587),
                m("purpose", "channel4", "address", "0.0.0.0", "port", 8588), m("purpose", "api", "address", "0.0.0.0", "port", 8600)));
    }

    private void save(Map<String, Object> r) {
        r.put("periodic", m("running", true, "enabled", true, "minSec", 300, "maxSec", 600, "saved", 1_482, "failed", 1, "tracked", 5));
        List<Object> rec = new ArrayList<>();
        String[] names = {"雪花冒險家", "竈門炭治郎", "一拳超人埼玉", "練功小號", "Aran0920", "楓之谷第一劍士"};
        long t = now - 20_000;
        for (int i = 0; i < 14; i++) {
            rec.add(m("at", t, "characterId", 100 + i * 7, "name", names[i % names.length], "millis", 40 + (i * 37) % 300, "ok", i != 5));
            t -= 70_000;
        }
        r.put("recent", rec);
        r.put("pendingSaves", 0);
        r.put("onlineUsers", 5);
        r.put("onlineCharacters", 4);
        r.put("pid", 65640);
        r.put("uptimeSec", 93_784);
        r.put("stopFile", "D:\\TMS\\server\\265\\shutdown.request");
        r.put("stopRequested", false);
        r.put("stopWatcher", true);
        r.put("shuttingDown", false);
        r.put("startCommand", "powershell -NoProfile -ExecutionPolicy Bypass -File D:\\TMS\\launcher\\Start-CompatibleTest.ps1 -ServerOnly");
    }

    private void release(Map<String, Object> r) {
        String sha = "6f1c0e2b9d84a77f3e5b2c1d0a9f8e7d6c5b4a3928170615f4e3d2c1b0a99887";
        String sig = "Qm9ndXMgc2lnbmF0dXJlIGZvciB0aGUgc2NyZWVuc2hvdCBvbmx5LCBub3QgYSByZWFsIEVkMjU1MTkgc2lnbmF0dXJlIQ==";
        Map<String, Object> download = m("version", "1.2.0", "file", "CatLauncher-1.2.0-portable.exe", "size", 103_812_096L, "sha256", sha,
                "signature", sig);
        List<String> notes = List.of("登入器可以自己下載新版了（設定 → 更新 → 下載並更新）", "下載前先驗證主機的簽章，被改過的檔案不會執行",
                "修正：縮到系統匣後偶爾叫不回來", "更新說明很長的時候會自動截斷，完整內容請看主機公告；這一行刻意寫得比較長用來檢查版面");
        String text = "{\n  \"latest\" : \"1.2.0\",\n  \"minSupported\" : \"1.1.0\",\n  \"notes\" : [ \"登入器可以自己下載新版了（設定 → 更新 → 下載並更新）\", \"…\" ],\n"
                + "  \"publishedAt\" : \"2026-10-04T12:00:00+08:00\",\n  \"download\" : {\n    \"version\" : \"1.2.0\",\n"
                + "    \"file\" : \"CatLauncher-1.2.0-portable.exe\",\n    \"size\" : 103812096,\n    \"sha256\" : \"" + sha + "\",\n"
                + "    \"signature\" : \"" + sig + "\"\n  }\n}\n";
        r.put("file", "D:\\TMS\\server\\265\\data\\launcher\\release.json");
        r.put("releasesDir", "D:\\TMS\\server\\265\\data\\launcher\\releases");
        r.put("distDir", "D:\\TMS\\launcher\\cat-launcher\\dist-release");
        r.put("exists", true);
        r.put("sha256", "A1B2C3D4E5F60718293A4B5C6D7E8F90A1B2C3D4E5F60718293A4B5C6D7E8F90");
        r.put("text", text);
        r.put("problem", null);
        r.put("fields", m("latest", "1.2.0", "minSupported", "1.1.0", "notes", notes, "publishedAt", "2026-10-04T12:00:00+08:00", "download", download));
        r.put("live", m("status", "ok", "problem", null, "info", m("latest", "1.2.0", "minSupported", "1.1.0", "notes", notes,
                "publishedAt", "2026-10-04T12:00:00+08:00", "download", download), "download", download, "downloadState", "ok"));
        r.put("releases", List.of(
                m("name", "CatLauncher-1.1.0-portable.exe", "size", 101_220_352L, "modified", now - 86_400_000L, "registered", false, "validName", true),
                m("name", "CatLauncher-1.2.0-portable.exe", "size", 103_812_096L, "modified", now - 3_600_000L, "registered", true, "validName", true),
                m("name", "說明（請勿刪除）.txt", "size", 512L, "modified", now - 7 * 86_400_000L, "registered", false, "validName", false)));
        r.put("candidates", List.of(
                m("version", "1.2.1", "file", "CatLauncher-1.2.1-portable.exe", "size", 103_900_160L, "exePresent", true, "test", false,
                        "keyId", "9a3f0c2d7e1b4a56", "problem", null),
                m("version", "1.2.0", "file", "CatLauncher-1.2.0-portable.exe", "size", 103_812_096L, "exePresent", true, "test", false,
                        "keyId", "9a3f0c2d7e1b4a56", "problem", null),
                m("version", "1.1.9", "file", "CatLauncher-1.1.9-portable.exe", "size", 103_000_000L, "exePresent", true, "test", true,
                        "keyId", "0011223344556677", "problem", null),
                m("version", "1.1.5", "file", "", "size", 0L, "exePresent", false, "test", false, "keyId", null, "problem", "entry.missing")));
        r.put("manifest", m("ok", true, "referenceSha256", "ACD242B18190A7F90491C6CFA0D16191146E37E4864189D22B8A4A84615FDEE8",
                "exeSha256", "EF8BB4265463CAD5BBF04544F57278F7D69C9A2657EC684175496FF389B00548", "count", 396,
                "reference", "D:\\TMS\\research\\tms2824\\expected-file-report-2824.bin", "clientExe", "D:\\TMS\\MapleStory\\MapleStory.exe"));
        r.put("downloads", m("active", List.of(m("ip", "26.12.34.56", "file", "CatLauncher-1.2.0-portable.exe", "sent", 41_000_000L,
                "bytes", 103_812_096L, "startedAt", now - 30_000)), "started", 7, "refused", 2, "perIp", 1, "total", 4));
        r.put("launcherVersions", m("1.1.0", 8_204, "1.2.0", 2_530, "-", 2_113));
        r.put("allowTestKey", false);
        r.put("reloadSec", 5);
    }

    private Map<String, Object> giftItem(int id, String name, int q) {
        return m("itemId", id, "name", name, "quantity", q);
    }

    private void giftList(Map<String, Object> r) {
        r.put("keyId", "3f9c0a7d51e2b864");
        List<Object> codes = new ArrayList<>();
        String[] states = {"active", "active", "exhausted", "scheduled", "expired", "disabled", "otherKey", "active"};
        for (int i = 0; i < 14; i++) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", 40 - i);
            c.put("title", i == 0 ? "開服紀念禮包（十月）超長名稱測試" : "週末活動禮包 " + (i + 1));
            c.put("note", "第一批");
            c.put("maxUses", i % 3 == 0 ? 100 : 1);
            c.put("usedCount", i % 3 == 0 ? 37 : (i % 2));
            c.put("perUserLimit", 1);
            c.put("worldId", i % 4 == 1 ? 55 : -1);
            c.put("minLevel", i % 5 == 0 ? 30 : 0);
            c.put("startsAt", i == 3 ? now + 86_400_000L : null);
            c.put("expiresAt", now + (i == 4 ? -3_600_000L : 30L * 86_400_000L));
            c.put("enabled", !"disabled".equals(states[i % states.length]));
            c.put("createdAt", now - i * 3_600_000L);
            c.put("state", states[i % states.length]);
            c.put("items", List.of(giftItem(4001126, "楓葉", 100), giftItem(2000005, "超級藥水", 50), giftItem(1142000, "楓之谷新手勳章", 1)));
            codes.add(c);
        }
        r.put("codes", codes);
    }

    private void giftSearch(Map<String, Object> r) {
        List<Object> items = new ArrayList<>();
        String[] names = {"楓葉", "楓葉之心（超長的道具名稱測試）", "紅楓葉", "楓葉帽", "楓葉盾牌", "黃金楓葉"};
        for (int i = 0; i < names.length; i++) {
            items.add(m("itemId", 4001126 + i, "name", names[i], "type", i == 3 ? "裝備" : "其他", "grantable", i != 5, "unique", i == 3));
        }
        r.put("items", items);
    }

    private void giftRedemptions(Map<String, Object> r) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            list.add(m("id", 100 - i, "codeId", 40 - i % 3, "title", i % 2 == 0 ? "開服紀念禮包（十月）" : "週末活動禮包",
                    "userId", 12 + i, "userName", "SnowyMapleHero" + i, "characterId", 300 + i, "characterName", "雪花冒險家" + i,
                    "worldId", 0, "sourceIp", "26.12.34." + (100 + i), "redeemedAt", now - i * 600_000L, "delivered", i % 3 != 0));
        }
        r.put("redemptions", list);
    }

    private void giftQueue(Map<String, Object> r) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            list.add(m("id", 500 - i, "userId", 12 + i, "userName", "SnowyMapleHero" + i, "characterId", 300 + i,
                    "characterName", "雪花冒險家" + i, "itemId", 4001126, "itemName", "楓葉", "quantity", 100, "source", "gift",
                    "sourceId", 100 - i, "createdAt", now - i * 600_000L, "deliveredAt", null));
        }
        r.put("queue", list);
    }
}
