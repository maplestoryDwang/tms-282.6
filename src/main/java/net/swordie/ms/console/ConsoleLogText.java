package net.swordie.ms.console;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** GUI 日誌規範化：把刷屏狀態日誌收斂到調試模式，並統一面板文本語言。 */
public final class ConsoleLogText {
    private static final Pattern SKILL_COST = Pattern.compile("Thời gian xử lý kỹ năng (.+) \\((\\d+)\\): (\\d+) ms\\.");
    private static final Pattern ONLINE = Pattern.compile("Online\\(s\\):\\s*(\\d+)");
    private static final Pattern CHANNEL_STATE = Pattern.compile("\\[ChannelInactive] Client (.+): Current state: (.+), Next state: (.+)\\.");

    private ConsoleLogText() {
    }

    public static boolean shouldShowRunLine(String line, boolean error) {
        if (line == null || line.isBlank()) {
            return false;
        }
        if (error || ConsoleDebugMode.isActive()) {
            return true;
        }
        return !isStatusOrDebugLine(line.trim());
    }

    public static String toChineseRunLine(String line) {
        return TaiwanText.convert(english(toChineseRunLine0(line)));
    }

    private static final String[][] ENGLISH = {
            {"^Starting MapleStory - ver:(\\S+) at (.+?)\\.?$", "啟動伺服器（版本 $1），時間 $2"},
            {"^\\[Timer] Loaded Timers in (\\d+) ?ms$", "[計時器] 全部計時器載入完成，耗時 $1 ms"},
            {"^\\[Timer] Loaded Timer (\\S+) in (\\d+) ?ms$", "[計時器] 已載入 $1 計時器，耗時 $2 ms"},
            {"^\\[WZ Data] Started loading (.+?) from wz\\.?$", "[WZ 資料] 開始載入 $1"},
            {"^\\[WZ Data] Finished Loaded WZ data in (\\d+) ?ms$", "[WZ 資料] WZ 資料全部載入完成，耗時 $1 ms"},
            {"^\\[WZ Data] Loaded (.+?) in (\\d+) ?ms\\.?$", "[WZ 資料] 已載入 $1，耗時 $2 ms"},
            {"^\\[JSON Data] Finished Loaded Json data in (\\d+) ?ms$", "[JSON 資料] JSON 資料載入完成，耗時 $1 ms"},
            {"^\\[Scripting] Script engine name : (.+)$", "[腳本] 腳本引擎：$1"},
            {"^\\[Skill Cache] Loaded (\\d+) skill overrides from (\\S+) in (\\d+) ?ms$", "[技能快取] 從 $2 載入 $1 個技能覆寫，耗時 $3 ms"},
            {"^\\[Skill Cache] Applied (\\d+) skill overrides from (\\S+)$", "[技能快取] 已套用 $2 的 $1 個技能覆寫"},
            {"^\\[CustomShopData] Loaded (\\d+) shops, (\\d+) total items$", "[自訂商店] 已載入 $1 間商店，共 $2 項商品"},
            {"^Exception in thread \"(.+?)\" java\\.net\\.BindException: Address already in use: bind$",
                    "執行緒 $1 綁定連接埠失敗：連接埠已被占用（正式伺服器是否已在執行？）"},
            {"^Exception in thread \"(.+?)\" (.+)$", "執行緒 $1 發生例外：$2"},
            {"^Suppressed: (.+)$", "附帶例外：$1"},
            {"^Caused by: (.+)$", "原因：$1"},
    };
    private static final Pattern[] ENGLISH_PATTERNS = java.util.Arrays.stream(ENGLISH)
            .map(e -> Pattern.compile(e[0])).toArray(Pattern[]::new);

    /** 常見英文啟動/例外訊息轉成中文；其他英文原樣保留。 */
    static String english(String s) {
        if (s == null) {
            return null;
        }
        for (int i = 0; i < ENGLISH_PATTERNS.length; i++) {
            Matcher m = ENGLISH_PATTERNS[i].matcher(s);
            if (m.matches()) {
                return m.replaceFirst(ENGLISH[i][1]);
            }
        }
        return s;
    }

    private static String toChineseRunLine0(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        String s = line.trim();

        Matcher skillCost = SKILL_COST.matcher(s);
        if (skillCost.matches()) {
            return "技能處理耗時 " + skillCost.group(1) + " (" + skillCost.group(2) + "): " + skillCost.group(3) + " ms。";
        }

        Matcher online = ONLINE.matcher(s);
        if (online.matches()) {
            return "當前連接數: " + online.group(1);
        }

        Matcher channelState = CHANNEL_STATE.matcher(s);
        if (channelState.matches()) {
            return "[連接斷開] 客戶端 " + channelState.group(1)
                    + "：當前狀態 " + channelState.group(2)
                    + "，下個狀態 " + channelState.group(3) + "。";
        }

        return s
                .replace("[ATTACK SKILL]", "[攻擊技能]")
                .replace("[Startup] fast startup disabled: serial data loading.", "[啓動] 快速啓動已關閉：按串行流程加載數據。")
                .replace("[Startup] fast startup enabled: serial data load with network overlap.", "[啓動] 快速啓動已開啓：串行加載數據並重疊綁定網絡端口。")
                .replace("[Startup] fast startup enabled, dat cache missing at", "[啓動] 快速啓動已開啓，dat 緩存不存在：")
                .replace("; will generate dat on first boot.", "；首次啓動時會生成 dat。")
                .replace("[Startup] dat cache missing at", "[啓動] dat 緩存不存在：")
                .replace("; will generate dat from WZ on first boot.", "；首次啓動時會從 WZ 生成 dat。")
                .replace("[HikariCP Database] Connect: Success", "[數據庫連接池] 連接成功")
                .replace("[HikariCP Database] Loaded Database in", "[數據庫連接池] 數據庫加載完成，耗時")
                .replace("[HikariCP Database] Exception during initialization:", "[數據庫連接池] 初始化異常：")
                .replace("[HikariCP Database] Exception closing ResultSet:", "[數據庫連接池] 關閉 ResultSet 異常：")
                .replace("[HikariCP Database] Exception closing Statement:", "[數據庫連接池] 關閉 Statement 異常：")
                .replace("[HikariCP Database] Exception closing Connection:", "[數據庫連接池] 關閉連接異常：")
                .replace("[HikariCP Database] Exception:", "[數據庫連接池] 異常：")
                .replace("forcibly closed the game.", "強制關閉了遊戲連接。")
                .replace("caught an exception:", "發生異常：")
                .replace("UNHANDLED", "未處理")
                .replace("UNKNOWN", "未知");
    }

    public static String toChinesePacketLine(String line) {
        return TaiwanText.convert(toChinesePacketLine0(line));
    }

    private static String toChinesePacketLine0(String line) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        return line
                .replace("[RECV ", "[客戶端:")
                .replace("[RECV:", "[客戶端:")
                .replace("[SEND ", "[服務端:")
                .replace("[SEND:", "[服務端:")
                .replace("[In UNKNOWN", "[未知客戶端")
                .replace("[Out UNKNOWN", "[未知服務端")
                .replace("UNHANDLED", "未處理")
                .replace("UNKNOWN", "未知")
                .replace("len=", "長度=")
                .replace("bytes", "字節")
                .replace("byte", "字節");
    }

    private static boolean isStatusOrDebugLine(String s) {
        return s.contains("[ATTACK SKILL]")
                || s.contains("[攻擊技能]")
                || s.startsWith("Online(s):")
                || s.startsWith("當前連接數:")
                || s.startsWith("Thời gian xử lý kỹ năng")
                || s.startsWith("技能處理耗時")
                || s.startsWith("[ChannelInactive] Client ")
                || s.startsWith("[In UNKNOWN")
                || s.startsWith("[Out UNKNOWN")
                || s.contains("UNHANDLED")
                || s.contains("未處理");
    }
}