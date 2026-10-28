package net.swordie.ms.world.online;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/**
 * 累積上線進度與樂豆點發放存在資料庫：online_reward_day（每帳號每天一列）與 online_reward_log（每次發放一張收據）。
 * 發放與 NewbiePoints 同一條安全路徑：一筆交易內 SELECT ... FOR UPDATE 鎖進度列與 users 列、檢查上限與溢位、
 * 更新 users.nxPrepaid、寫收據（唯一鍵 user_id+day+points_after 擋重複發放）、更新進度，最後一起提交；
 * 任何一步失敗整筆回滾（不會扣到一半或發兩次）。
 */
public final class JdbcOnlineRewardLedger implements OnlineRewardLedger {

    /** 也放在 sql/tms282/20261005-online-reward.sql；伺服器第一次用到時 CREATE TABLE IF NOT EXISTS。 */
    public static final List<String> DDL = List.of(
            "CREATE TABLE IF NOT EXISTS `online_reward_day` ("
                    + "`user_id` int(11) NOT NULL, `day` date NOT NULL, `minutes` int(11) NOT NULL DEFAULT 0, "
                    + "`points` int(11) NOT NULL DEFAULT 0, `last_tick_at` bigint(20) NOT NULL DEFAULT 0, "
                    + "`updated_at` datetime(3) NOT NULL, PRIMARY KEY (`user_id`, `day`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `online_reward_log` ("
                    + "`id` bigint(20) NOT NULL AUTO_INCREMENT, `user_id` int(11) NOT NULL, `day` date NOT NULL, "
                    + "`amount` int(11) NOT NULL, `points_after` int(11) NOT NULL, `minutes` int(11) NOT NULL, "
                    + "`balance_after` bigint(20) NOT NULL, `granted_at` datetime(3) NOT NULL, PRIMARY KEY (`id`), "
                    + "UNIQUE KEY `uq_online_reward_log` (`user_id`, `day`, `points_after`), KEY `idx_online_reward_log_time` (`granted_at`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci");

    private static final JdbcOnlineRewardLedger STANDARD = new JdbcOnlineRewardLedger();
    private volatile boolean ready;

    public static JdbcOnlineRewardLedger standard() {
        return STANDARD;
    }

    public void ensureSchema() throws SQLException {
        if (ready) {
            return;
        }
        synchronized (this) {
            if (ready) {
                return;
            }
            try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
                for (String ddl : DDL) {
                    s.executeUpdate(ddl);
                }
            }
            ready = true;
        }
    }

    @Override
    public Credit credit(int userId, LocalDate day, long nowMs, OnlineRewardConfig config) throws SQLException {
        if (userId <= 0) {
            throw new IllegalArgumentException("invalid user");
        }
        ensureSchema();
        try {
            return creditOnce(userId, day, nowMs, config);
        } catch (java.sql.SQLTransactionRollbackException e) { // deadlock / lock wait: nothing was committed, try once more
            return creditOnce(userId, day, nowMs, config);
        }
    }

    private Credit creditOnce(int userId, LocalDate day, long nowMs, OnlineRewardConfig config) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                Credit result = credit(c, userId, day, nowMs, config);
                c.commit();
                if (result.granted() > 0) {
                    System.out.printf("[OnlineReward] user=%d day=%s minutes=%d +%d nxPrepaid (today %d, balance %d)%n",
                            userId, day, result.progress().minutes(), result.granted(), result.progress().points(), result.balanceAfter());
                }
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** 交易本體（呼叫者負責 commit / rollback）。 */
    static Credit credit(Connection c, int userId, LocalDate day, long nowMs, OnlineRewardConfig config) throws SQLException {
        Timestamp now = new Timestamp(nowMs);
        // Lock the existing row first: INSERT IGNORE on an existing key takes a shared lock, and two such transactions
        // upgrading to FOR UPDATE deadlock. Only the first minute of a day inserts.
        long[] row = lockRow(c, userId, day);
        if (row == null) {
            try (PreparedStatement p = c.prepareStatement("INSERT IGNORE INTO online_reward_day (user_id, day, minutes, points, last_tick_at, updated_at) "
                    + "VALUES (?, ?, 0, 0, 0, ?)")) {
                p.setInt(1, userId);
                p.setDate(2, Date.valueOf(day));
                p.setTimestamp(3, now);
                p.executeUpdate();
            }
            row = lockRow(c, userId, day);
            if (row == null) {
                throw new SQLException("online reward row missing");
            }
        }
        int minutes = (int) row[0], points = (int) row[1];
        long lastTick = row[2];
        if (lastTick > 0 && nowMs - lastTick < MIN_TICK_GAP_MS) {
            return new Credit(new Progress(userId, day, minutes, points), false, 0, -1);
        }
        minutes++;
        int owed = Math.max(0, config.pointsEarnedBy(minutes) - points);
        long balanceAfter = -1;
        if (owed > 0) {
            long before;
            try (PreparedStatement p = c.prepareStatement("SELECT nxPrepaid FROM users WHERE id = ? FOR UPDATE")) {
                p.setInt(1, userId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) {
                        throw new SQLException("user missing");
                    }
                    before = r.getLong(1);
                }
            }
            balanceAfter = before + owed;
            if (before < 0 || balanceAfter > Integer.MAX_VALUE) {
                throw new SQLException("wallet limit");
            }
            try (PreparedStatement p = c.prepareStatement("UPDATE users SET nxPrepaid = ? WHERE id = ?")) {
                p.setLong(1, balanceAfter);
                p.setInt(2, userId);
                if (p.executeUpdate() != 1) {
                    throw new SQLException("wallet update failed");
                }
            }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO online_reward_log (user_id, day, amount, points_after, minutes, balance_after, granted_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                p.setInt(1, userId);
                p.setDate(2, Date.valueOf(day));
                p.setInt(3, owed);
                p.setInt(4, points + owed);
                p.setInt(5, minutes);
                p.setLong(6, balanceAfter);
                p.setTimestamp(7, now);
                p.executeUpdate();
            }
            points += owed;
        }
        try (PreparedStatement p = c.prepareStatement("UPDATE online_reward_day SET minutes = ?, points = ?, last_tick_at = ?, updated_at = ? "
                + "WHERE user_id = ? AND day = ?")) {
            p.setInt(1, minutes);
            p.setInt(2, points);
            p.setLong(3, nowMs);
            p.setTimestamp(4, now);
            p.setInt(5, userId);
            p.setDate(6, Date.valueOf(day));
            if (p.executeUpdate() != 1) {
                throw new SQLException("online reward progress not saved");
            }
        }
        return new Credit(new Progress(userId, day, minutes, points), true, owed, balanceAfter);
    }

    @Override
    public Progress read(int userId, LocalDate day) throws SQLException {
        ensureSchema();
        try (Connection c = DatabaseManager.getConnection()) {
            return read(c, userId, day);
        }
    }

    private static long[] lockRow(Connection c, int userId, LocalDate day) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT minutes, points, last_tick_at FROM online_reward_day WHERE user_id = ? AND day = ? FOR UPDATE")) {
            p.setInt(1, userId);
            p.setDate(2, Date.valueOf(day));
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new long[]{r.getInt(1), r.getInt(2), r.getLong(3)} : null;
            }
        }
    }

    /** 唯讀查詢（啟動器 dashboard 用呼叫者的連線）；表還不存在時當作 0 分 0 點。 */
    public static Progress read(Connection c, int userId, LocalDate day) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT minutes, points FROM online_reward_day WHERE user_id = ? AND day = ?")) {
            p.setInt(1, userId);
            p.setDate(2, Date.valueOf(day));
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new Progress(userId, day, r.getInt(1), r.getInt(2)) : Progress.empty(userId, day);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1146) { // table does not exist yet (server never ticked)
                return Progress.empty(userId, day);
            }
            throw e;
        }
    }
}
