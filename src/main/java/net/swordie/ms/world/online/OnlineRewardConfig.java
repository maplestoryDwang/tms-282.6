package net.swordie.ms.world.online;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 累積上線獎勵（leftovers-1005 任務 7）的設定，來自 {@code data/tms282-online-reward/online-reward.json}
 * （{@link OnlineRewardSettings} 熱載入）。缺少的欄位用預設值；數值不合理時整份檔案不採用。
 *
 * @param enabled           false = 不累計、不發放（已累計的紀錄保留）
 * @param intervalMinutes   每累積幾分鐘發一次
 * @param pointsPerInterval 每次發幾點樂豆點（users.nxPrepaid）
 * @param dailyCapPoints    每個帳號每天最多幾點（0 = 不發放，只累計分鐘）
 * @param afkMinutes        角色連續幾分鐘沒有移動（同一張地圖、同一個座標）就不再累計；0 = 不判斷掛機
 * @param notice            發放時給玩家的訊息；{points} {minutes} {today} {cap} 會被取代
 */
public record OnlineRewardConfig(boolean enabled, int intervalMinutes, int pointsPerInterval, int dailyCapPoints,
                                 int afkMinutes, String notice) {

    public static final String DEFAULT_NOTICE = "[累積上線獎勵] 今日已上線 {minutes} 分鐘，獲得 {points} 樂豆點（今日 {today}/{cap}）。";

    /** 暫定值（使用者可在設定檔調整）：每 30 分鐘 5 點、每帳號每天上限 60 點、10 分鐘沒移動視為掛機。 */
    public static OnlineRewardConfig defaults() {
        return new OnlineRewardConfig(true, 30, 5, 60, 10, DEFAULT_NOTICE);
    }

    public OnlineRewardConfig {
        if (intervalMinutes < 1 || intervalMinutes > 24 * 60) {
            throw new IllegalArgumentException("intervalMinutes must be 1..1440: " + intervalMinutes);
        }
        if (pointsPerInterval < 0 || pointsPerInterval > 100_000) {
            throw new IllegalArgumentException("pointsPerInterval must be 0..100000: " + pointsPerInterval);
        }
        if (dailyCapPoints < 0 || dailyCapPoints > 1_000_000) {
            throw new IllegalArgumentException("dailyCapPoints must be 0..1000000: " + dailyCapPoints);
        }
        if (afkMinutes < 0 || afkMinutes > 24 * 60) {
            throw new IllegalArgumentException("afkMinutes must be 0..1440: " + afkMinutes);
        }
        if (notice == null || notice.isBlank()) {
            notice = DEFAULT_NOTICE;
        }
        if (notice.length() > 200) {
            throw new IllegalArgumentException("notice longer than 200 characters");
        }
    }

    /** JSON 物件 → 設定；缺少的欄位沿用 {@link #defaults()}，型別錯或數值超出範圍丟 IllegalArgumentException。 */
    public static OnlineRewardConfig parse(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("online reward config must be a JSON object");
        }
        OnlineRewardConfig d = defaults();
        return new OnlineRewardConfig(
                bool(root, "enabled", d.enabled()),
                integer(root, "intervalMinutes", d.intervalMinutes()),
                integer(root, "pointsPerInterval", d.pointsPerInterval()),
                integer(root, "dailyCapPoints", d.dailyCapPoints()),
                integer(root, "afkMinutes", d.afkMinutes()),
                root.hasNonNull("notice") ? text(root, "notice") : d.notice());
    }

    /** 到 {@code minutes} 分鐘為止應得的總點數（含上限）。 */
    public int pointsEarnedBy(int minutes) {
        if (minutes <= 0 || pointsPerInterval == 0) {
            return 0;
        }
        long earned = (long) (minutes / intervalMinutes) * pointsPerInterval;
        return (int) Math.min(earned, dailyCapPoints);
    }

    /** 今天已經拿滿上限（之後不再需要累計）。 */
    public boolean capReached(int pointsToday) {
        return pointsToday >= dailyCapPoints;
    }

    public String noticeFor(int points, int minutes, int today) {
        return notice.replace("{points}", String.valueOf(points)).replace("{minutes}", String.valueOf(minutes))
                .replace("{today}", String.valueOf(today)).replace("{cap}", String.valueOf(dailyCapPoints));
    }

    private static boolean bool(JsonNode root, String key, boolean def) {
        JsonNode n = root.get(key);
        if (n == null || n.isNull()) {
            return def;
        }
        if (!n.isBoolean()) {
            throw new IllegalArgumentException(key + " must be true/false");
        }
        return n.booleanValue();
    }

    private static int integer(JsonNode root, String key, int def) {
        JsonNode n = root.get(key);
        if (n == null || n.isNull()) {
            return def;
        }
        if (!n.isIntegralNumber() || !n.canConvertToInt()) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        return n.intValue();
    }

    private static String text(JsonNode root, String key) {
        JsonNode n = root.get(key);
        if (!n.isTextual()) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return n.textValue();
    }
}
