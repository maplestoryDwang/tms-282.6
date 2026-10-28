package net.swordie.ms.console.admin;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Traditional Chinese (Taiwan) texts for the admin API's codes and problems. */
final class AdminText {
    private static final Pattern NEWS_FIELD = Pattern.compile("items\\[(\\d+)]\\.?([A-Za-z]*)(\\.duplicate)?");
    private static final Map<String, String> NEWS_FIELDS = Map.of("id", "ID", "kind", "類型", "title", "標題", "body", "內容",
            "startAt", "開始時間", "endAt", "結束時間", "pinned", "置頂");
    private static final Map<String, String> CODES = Map.ofEntries(
            Map.entry("ok", "完成"),
            Map.entry("invalidInput", "輸入不正確"),
            Map.entry("notFound", "找不到這個帳號（可輸入帳號 ID、帳號名稱或角色名稱）"),
            Map.entry("conflict", "檔案在你讀取之後被改過了：請按「重新載入」再改一次"),
            Map.entry("rejected", "內容不符合規則"),
            Map.entry("unavailable", "伺服器處理失敗（詳見伺服器 log）"),
            Map.entry("onlineChanged", "在線人數已經變了，請重新確認"),
            Map.entry("alreadyStopping", "伺服器已經在停機（或已有停機要求）"),
            Map.entry("noStopWatcher", "這個伺服器程序沒有在監看 stop 檔，無法用這個按鈕停機"),
            Map.entry("noCountdown", "目前沒有維修倒數可以取消"),
            Map.entry("confirmRequired", "要提高 minSupported，需要再確認一次"));
    /** Launcher API codes as the monitor shows them. */
    private static final Map<String, String> LAUNCHER_CODES = Map.ofEntries(
            Map.entry("ok", "成功"),
            Map.entry("success", "成功（舊登入）"),
            Map.entry("loggedIn", "登入成功"),
            Map.entry("registered", "註冊成功"),
            Map.entry("reset", "重設密碼成功"),
            Map.entry("loggedOut", "登出"),
            Map.entry("wrongCredentials", "帳號或密碼錯誤"),
            Map.entry("wrongPassword", "密碼錯誤（舊登入）"),
            Map.entry("rateLimited", "被限速"),
            Map.entry("busy", "被限速（舊版）"),
            Map.entry("needSession", "需要登入"),
            Map.entry("sessionExpired", "登入已過期"),
            Map.entry("invalidInput", "輸入不正確"),
            Map.entry("forbidden", "來源被拒（api.http.allow／Host／Origin）"),
            Map.entry("launcherRequired", "沒帶 X-Cat-Launcher"),
            Map.entry("unsupportedMediaType", "Content-Type 不對"),
            Map.entry("unavailable", "伺服器錯誤"),
            Map.entry("error", "伺服器錯誤（舊登入）"),
            Map.entry("banned", "帳號被封鎖"),
            Map.entry("maintenance", "維修中"),
            Map.entry("alreadyOnline", "已在線上"),
            Map.entry("alreadyLogin", "已在線上（舊登入）"),
            Map.entry("nameUnavailable", "名稱不能用"),
            Map.entry("remoteForbidden", "只限本機"),
            Map.entry("passwordChanged", "改密碼成功"),
            Map.entry("pinChanged", "改第二組密碼成功"),
            Map.entry("wrongCurrentPassword", "目前密碼錯誤"),
            Map.entry("sameAsPin", "新密碼與第二組密碼相同"),
            Map.entry("redeemed", "兌換禮包成功"),
            Map.entry("invalidOrExpired", "序號無效或已過期"),
            Map.entry("alreadyRedeemed", "序號已兌換過"),
            Map.entry("noReceiver", "沒有可收禮的角色"),
            Map.entry("unknown", "未知"));
    private static final Pattern NOTE_LINE = Pattern.compile("notes\\[(\\d+)](\\.empty|\\.length)?");
    private static final Map<String, String> DOWNLOAD_FIELDS = Map.of("version", "版本", "file", "檔名", "size", "大小",
            "sha256", "SHA-256", "signature", "簽章");

    private AdminText() {
    }

    static String code(String code) {
        String s = CODES.get(code);
        return s == null ? code : s;
    }

    static String launcherCode(String code) {
        String s = LAUNCHER_CODES.get(code);
        if (s != null) {
            return s;
        }
        if (code != null && code.startsWith("http")) {
            return "HTTP " + code.substring(4);
        }
        return "";
    }

    static String version(String v) {
        return "-".equals(v) ? "（沒帶版本：1.0.0 或舊版）" : v;
    }

    /** A failed result as one sentence: the code, then the problem in words. */
    static String failure(JsonNode r) {
        String code = AdminUi.text(r, "code");
        String problem = AdminUi.text(r, "problem");
        String key = AdminUi.text(r, "key");
        String base = code(code);
        if (problem.isEmpty()) {
            return base;
        }
        return base + "：" + problem(key, problem);
    }

    static String problem(String key, String problem) {
        Matcher m = NEWS_FIELD.matcher(problem);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1)) + 1;
            String f = m.group(2);
            if (m.group(3) != null) {
                return "第 " + n + " 則的 ID 跟前面重複";
            }
            String name = NEWS_FIELDS.get(f);
            return "第 " + n + " 則的「" + (name == null ? (f.isEmpty() ? "格式" : f + "（不認得的欄位）") : name) + "」不正確";
        }
        String prefix = key.isEmpty() ? "" : key + "：";
        if (problem.equals("missingLoopback")) {
            return prefix + "名單必須包含 127.0.0.1（否則本機也會被擋在外面）";
        }
        if (problem.startsWith("invalidEntry:")) {
            return prefix + "看不懂的項目「" + problem.substring(13) + "」（只接受 IP 或 IP/前綴長度，例如 192.168.1.0/24）";
        }
        if (problem.startsWith("range:")) {
            return prefix + "必須是 " + problem.substring(6).replace("-", " 到 ") + " 的整數";
        }
        if (problem.equals("characters")) {
            return prefix + "只能用數字、a-f、. : / , 和空白";
        }
        if (problem.equals("jvmOverride")) {
            return prefix + "這個設定被伺服器啟動參數 -D 指定了，改檔案不會生效";
        }
        if (problem.equals("empty")) {
            return prefix + "不可空白";
        }
        if (problem.startsWith("continuation:")) {
            return "server.properties 裡 " + problem.substring(13) + " 那行用了反斜線續行，請手動修改";
        }
        if (problem.startsWith("gift.")) {
            return gift(problem.substring(5));
        }
        if (problem.startsWith("verify:")) {
            return "寫入前自我檢查失敗（" + problem.substring(7) + "），沒有寫入";
        }
        return switch (problem) {
            case "json" -> "不是正確的 JSON";
            case "items" -> "缺少 items 清單";
            case "items.size" -> "最多 50 則";
            case "size" -> "檔案超過 256 KB";
            case "root" -> "最外層必須是物件";
            case "query" -> "請輸入帳號 ID、帳號名稱或角色名稱";
            case "reason" -> "請填原因（1–100 字）";
            case "hours" -> "時數必須是 0（永久）到 87600";
            case "type" -> "封鎖類型不正確";
            case "confirm", "expectOnline" -> "確認資料不完整";
            default -> prefix + problem;
        };
    }

    /** gift.create problems (GiftCodeService.validate / GiftAdmin). */
    static String gift(String p) {
        return switch (p) {
            case "title" -> "名稱必填，最多 64 字";
            case "note" -> "備註最多 200 字";
            case "items" -> "要有 1 到 10 行道具";
            case "itemId" -> "有道具 ID 不存在";
            case "itemNotGrantable" -> "有道具在 282 客戶端不能發放";
            case "quantity" -> "數量不正確（裝備、寵物只能 1；其他 1 到 1000）";
            case "maxUses" -> "使用次數上限要 1 到 100000";
            case "perUserLimit" -> "每帳號次數要 1 到 100，且不超過使用次數上限";
            case "worldId" -> "世界不正確";
            case "minLevel" -> "最低等級要 0 到 300";
            case "expiresAt" -> "到期時間必須在未來";
            case "startsAt" -> "開始時間必須早於到期時間";
            case "count" -> "一次產生 1 到 200 組";
            default -> p;
        };
    }

    /** gift.list state. */
    static String giftState(String state) {
        return switch (state) {
            case "active" -> "可兌換";
            case "disabled" -> "已停用";
            case "scheduled" -> "未開始";
            case "expired" -> "已過期";
            case "exhausted" -> "已用完";
            case "otherKey" -> "金鑰不符";
            default -> state;
        };
    }

    static String kind(String kind) {
        return switch (kind) {
            case "ev" -> "活動";
            case "up" -> "更新";
            case "mt" -> "維修";
            default -> kind;
        };
    }

    static String newsState(String state) {
        return switch (state) {
            case "shown" -> "顯示中";
            case "scheduled" -> "尚未開始";
            case "ended" -> "已結束";
            default -> state;
        };
    }

    static String category(String c) {
        return switch (c) {
            case "radmin" -> "Radmin VPN";
            case "tailscale" -> "Tailscale";
            case "lan" -> "區網";
            case "loopback" -> "本機";
            default -> "其他";
        };
    }

    static String refusal(String reason) {
        return switch (reason) {
            case "NOT_ALLOWED" -> "不在 net.allow";
            case "TOO_MANY" -> "超過 net.maxPerIp";
            case "SHUTTING_DOWN" -> "伺服器停機中";
            case "NOT_INET" -> "沒有 IP";
            default -> reason;
        };
    }

    static String purpose(String p) {
        if (p.startsWith("channel")) {
            return "頻道 " + p.substring(7);
        }
        return switch (p) {
            case "login" -> "登入伺服器";
            case "api" -> "8600 API";
            case "legacyApi" -> "舊 API（8483）";
            default -> p;
        };
    }

    // ------------------------------------------------------------------ 發佈

    /** 1 234 567 bytes -> "1.2 MB". */
    static String bytes(long n) {
        if (n < 0) {
            return "-";
        }
        if (n < 1024) {
            return n + " B";
        }
        if (n < 1024 * 1024) {
            return String.format("%.1f KB", n / 1024.0);
        }
        return String.format("%.1f MB", n / (1024.0 * 1024));
    }

    /** First and last 6 hex digits. */
    static String shortHash(String h) {
        return h == null || h.length() < 16 ? (h == null ? "-" : h) : h.substring(0, 6) + "…" + h.substring(h.length() - 6);
    }

    static String downloadState(String state) {
        return switch (state == null ? "" : state) {
            case "none" -> "release.json 沒有 download：登入器不會出現「下載並更新」";
            case "fileMissing" -> "releases\\ 找不到登記的檔案：不提供下載";
            case "sizeMismatch" -> "檔案大小跟登記的不同：不提供下載";
            case "hashMismatch" -> "檔案的 SHA-256 跟登記的不同：不提供下載";
            case "unreadable" -> "讀不到登記的檔案：不提供下載";
            case "ok" -> "提供下載";
            default -> state;
        };
    }

    static String manifestProblem(String p) {
        return switch (p == null ? "" : p) {
            case "reference" -> "tms282.fileReference 的參考檔";
            case "clientExe" -> "客戶端 MapleStory.exe（launcher.clientExe）";
            default -> "設定";
        };
    }

    /** The launcher versions seen since the server started (X-Cat-Launcher), most used first. */
    static String versions(JsonNode byVersion) {
        if (byVersion == null || !byVersion.isObject() || byVersion.isEmpty()) {
            return "（伺服器啟動後還沒有登入器連線）";
        }
        java.util.List<Map.Entry<String, Long>> list = new java.util.ArrayList<>();
        byVersion.fields().forEachRemaining(e -> list.add(Map.entry(e.getKey(), e.getValue().asLong())));
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size() && i < 6; i++) {
            if (i > 0) {
                sb.append("、");
            }
            sb.append("-".equals(list.get(i).getKey()) ? "沒帶版本（1.0.0）" : list.get(i).getKey()).append(" ×").append(list.get(i).getValue());
        }
        if (list.size() > 6) {
            sb.append("…");
        }
        return sb.toString();
    }

    static String releaseProblem(String p) {
        if (p == null || p.isEmpty()) {
            return "";
        }
        if (p.startsWith("current.")) {
            return "伺服器上的 release.json 本身有錯（" + releaseProblem(p.substring(8)) + "）：請先手動修正或移走它";
        }
        Matcher m = NOTE_LINE.matcher(p);
        if (m.matches()) {
            int n = Integer.parseInt(m.group(1)) + 1;
            if (m.group(2) == null) {
                return "更新說明第 " + n + " 行不是文字";
            }
            return m.group(2).equals(".empty") ? "更新說明第 " + n + " 行是空的" : "更新說明第 " + n + " 行超過 100 字";
        }
        if (p.startsWith("download.") || p.startsWith("entry.")) {
            boolean entry = p.startsWith("entry.");
            String rest = p.substring(entry ? 6 : 9);
            String where = entry ? "release-entry.json" : "release.json 的 download";
            switch (rest) {
                case "missing": return "找不到 release-entry.json";
                case "size": return entry ? "release-entry.json 太大（超過 4 KB）或「大小」不正確" : where + " 的「大小」不正確";
                case "read": return "讀不到 release-entry.json";
                case "json": return "release-entry.json 不是正確的 JSON";
                case "testKey": return "這是測試金鑰簽的：正式伺服器不接受（正式版登入器也驗不過）";
                case "version.folder": return "release-entry.json 的版本跟資料夾名稱不同";
                case "version.newerThanLatest": return "download 的版本比 latest 新";
                default: break;
            }
            String name = DOWNLOAD_FIELDS.get(rest);
            return where + " 的「" + (name == null ? rest : name) + "」不正確";
        }
        return switch (p) {
            case "latest" -> "「最新版」必須是 x.y.z（例如 1.2.0）";
            case "minSupported" -> "「最低支援」必須是 x.y.z（例如 1.1.0）";
            case "minSupported.newerThanLatest" -> "「最低支援」不能比「最新版」新";
            case "minSupported.raise" -> "會提高最低支援";
            case "notes" -> "更新說明必須是文字清單";
            case "notes.size" -> "更新說明最多 10 行";
            case "publishedAt" -> "發佈時間格式不對（例如 2026-10-04T12:00:00+08:00，可按「現在」）";
            case "size" -> "release.json 超過 16 KB";
            case "json" -> "不是正確的 JSON";
            case "root" -> "最外層必須是物件";
            case "read" -> "讀不到檔案";
            case "edit" -> "輸入不完整";
            case "version" -> "版本格式不對";
            case "dist.missing" -> "dist-release 裡沒有這個版本的資料夾";
            case "version.older" -> "比目前的 latest 舊，不能匯入";
            case "exe.missing" -> "找不到 exe（請重新執行 release-sign.mjs sign）";
            case "exe.size" -> "exe 的大小跟 release-entry.json 不同";
            case "exe.sha256" -> "exe 的 SHA-256 跟 release-entry.json 不同（檔案被換過？請重新 sign）";
            case "exe.read" -> "讀不到 exe";
            case "releases.exists" -> "releases\\ 已經有同名但內容不同的檔案：請先把它移走";
            default -> p;
        };
    }
}
