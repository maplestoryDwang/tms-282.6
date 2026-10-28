package net.swordie.ms.world.online;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * 每個登入帳號（users.id）每天的累積上線分鐘與已發樂豆點。存在資料庫（{@link JdbcOnlineRewardLedger}），所以重新登入、
 * 換頻道、伺服器重開都接得上；測試用 {@link MemoryOnlineRewardLedger}。
 */
public interface OnlineRewardLedger {

    /** 兩次計入之間至少隔這麼久（同一帳號在一分鐘內被算兩次的保護：例如兩個角色同時在線、計時器補跑）。 */
    long MIN_TICK_GAP_MS = 50_000L;

    /** {@code day}（伺服器時區的日期）到目前為止的進度。 */
    record Progress(int userId, LocalDate day, int minutes, int points) {
        public static Progress empty(int userId, LocalDate day) {
            return new Progress(userId, day, 0, 0);
        }
    }

    /**
     * 一次計入的結果。
     *
     * @param counted      這一分鐘有沒有算進去（距上次計入不到 {@link #MIN_TICK_GAP_MS} 就不算）
     * @param granted      這次發了幾點（0 = 沒發）
     * @param balanceAfter 發放後的 users.nxPrepaid（沒發時為 -1）
     */
    record Credit(Progress progress, boolean counted, int granted, long balanceAfter) {
    }

    /**
     * 在同一筆交易內：計入一分鐘、算出到目前為止應得的點數（{@link OnlineRewardConfig#pointsEarnedBy}，含每日上限），
     * 把還沒發的部分加到 users.nxPrepaid、寫一筆收據（online_reward_log），更新當日進度。任何一步失敗整筆回滾。
     */
    Credit credit(int userId, LocalDate day, long nowMs, OnlineRewardConfig config) throws SQLException;

    /** 唯讀：當日進度（沒有紀錄 = 0 分 0 點）。 */
    Progress read(int userId, LocalDate day) throws SQLException;
}
