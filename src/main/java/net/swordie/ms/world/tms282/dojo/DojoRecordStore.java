package net.swordie.ms.world.tms282.dojo;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistence for weekly dojo records and settlement markers. Tables are created lazily
 * (copy in sql/tms282/20260928-dojo-ranking.sql).
 */
final class DojoRecordStore {
    private static volatile boolean ready;

    private DojoRecordStore() {
    }

    static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS dojo_weekly_record (char_id INT NOT NULL, week_key INT NOT NULL,"
                    + " stage INT NOT NULL, time_sec INT NOT NULL, job INT NOT NULL, level INT NOT NULL,"
                    + " name VARCHAR(32) NOT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " PRIMARY KEY(char_id, week_key), KEY idx_week(week_key)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS dojo_week_settlement (week_key INT NOT NULL PRIMARY KEY,"
                    + " settled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, reward_rows INT NOT NULL DEFAULT 0) ENGINE=InnoDB");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS zsf_rank_reward (id INT NOT NULL AUTO_INCREMENT, playerId INT DEFAULT NULL,"
                    + " `rank` INT DEFAULT NULL, `type` VARCHAR(255) DEFAULT '1', `check` INT DEFAULT NULL,"
                    + " `time` DATETIME DEFAULT CURRENT_TIMESTAMP, getTime DATETIME DEFAULT NULL, PRIMARY KEY(id))"
                    + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        ready = true;
    }

    /** Keeps the better of the stored and submitted record (higher stage, then lower time). */
    static boolean submit(DojoRecord r) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                DojoRecord old = null;
                try (PreparedStatement p = c.prepareStatement(
                        "SELECT stage, time_sec FROM dojo_weekly_record WHERE char_id=? AND week_key=? FOR UPDATE")) {
                    p.setInt(1, r.charId());
                    p.setInt(2, r.weekKey());
                    try (ResultSet rs = p.executeQuery()) {
                        if (rs.next()) {
                            old = new DojoRecord(r.charId(), r.weekKey(), rs.getInt(1), rs.getInt(2), 0, 0, "");
                        }
                    }
                }
                if (old != null && !r.isBetterThan(old)) {
                    c.rollback();
                    return false;
                }
                try (PreparedStatement p = c.prepareStatement(old == null
                        ? "INSERT INTO dojo_weekly_record(stage,time_sec,job,level,name,char_id,week_key) VALUES(?,?,?,?,?,?,?)"
                        : "UPDATE dojo_weekly_record SET stage=?,time_sec=?,job=?,level=?,name=?,updated_at=CURRENT_TIMESTAMP WHERE char_id=? AND week_key=?")) {
                    p.setInt(1, r.stage());
                    p.setInt(2, r.timeSec());
                    p.setInt(3, r.job());
                    p.setInt(4, r.level());
                    p.setString(5, r.name());
                    p.setInt(6, r.charId());
                    p.setInt(7, r.weekKey());
                    p.executeUpdate();
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** All records of a week in ranking order. */
    static List<DojoRecord> week(int weekKey) throws SQLException {
        ensure();
        List<DojoRecord> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT char_id, week_key, stage, time_sec, job, level, name FROM dojo_weekly_record WHERE week_key=?"
                        + " ORDER BY stage DESC, time_sec ASC, updated_at ASC, char_id ASC")) {
            p.setInt(1, weekKey);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    result.add(new DojoRecord(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                            rs.getInt(5), rs.getInt(6), rs.getString(7)));
                }
            }
        }
        return result;
    }

    /** Weeks before {@code currentWeek} that have records but no settlement marker yet. */
    static List<Integer> unsettledWeeks(int currentWeek) throws SQLException {
        ensure();
        List<Integer> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT DISTINCT r.week_key FROM dojo_weekly_record r LEFT JOIN dojo_week_settlement s ON s.week_key=r.week_key"
                        + " WHERE r.week_key<? AND s.week_key IS NULL ORDER BY r.week_key")) {
            p.setInt(1, currentWeek);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) result.add(rs.getInt(1));
            }
        }
        return result;
    }

    /**
     * Atomically claims the settlement marker for a week and inserts its reward rows.
     *
     * @return number of inserted reward rows, or -1 if the week was already settled
     */
    static int settle(int weekKey, List<int[]> rewardRows, Timestamp rewardTime) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT IGNORE INTO dojo_week_settlement(week_key, reward_rows) VALUES(?, ?)")) {
                    p.setInt(1, weekKey);
                    p.setInt(2, rewardRows.size());
                    if (p.executeUpdate() != 1) {
                        c.rollback();
                        return -1;
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO zsf_rank_reward(playerId, `rank`, `type`, `check`, `time`) VALUES(?, ?, 'dojang', 1, ?)")) {
                    for (int[] row : rewardRows) {
                        p.setInt(1, row[0]);
                        p.setInt(2, row[1]);
                        p.setTimestamp(3, rewardTime);
                        p.addBatch();
                    }
                    if (!rewardRows.isEmpty()) p.executeBatch();
                }
                c.commit();
                return rewardRows.size();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    static boolean isSettled(int weekKey) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT 1 FROM dojo_week_settlement WHERE week_key=?")) {
            p.setInt(1, weekKey);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Unclaimed dojo reward rows of a character: {id, rank, time(epoch ms)}. */
    static List<long[]> unclaimed(int charId) throws SQLException {
        ensure();
        List<long[]> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT id, `rank`, `time` FROM zsf_rank_reward WHERE playerId=? AND `type`='dojang' AND `check`=1 ORDER BY id")) {
            p.setInt(1, charId);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) {
                    Timestamp t = rs.getTimestamp(3);
                    result.add(new long[]{rs.getInt(1), rs.getInt(2), t == null ? 0 : t.getTime()});
                }
            }
        }
        return result;
    }

    /** Marks a reward row claimed; returns false if it was not claimable by this character. */
    static boolean markClaimed(int rowId, int charId) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE zsf_rank_reward SET `check`=2, getTime=NOW() WHERE id=? AND playerId=? AND `type`='dojang' AND `check`=1")) {
            p.setInt(1, rowId);
            p.setInt(2, charId);
            return p.executeUpdate() == 1;
        }
    }
}
