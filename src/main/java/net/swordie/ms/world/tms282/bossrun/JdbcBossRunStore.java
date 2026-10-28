package net.swordie.ms.world.tms282.bossrun;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL {@link BossRunStore}. Tables are created on first use with CREATE TABLE IF NOT EXISTS (same DDL as
 * {@code sql/tms282/20260929-bossrun.sql}); nothing is ever altered or dropped. SQL errors are logged and read as
 * "no rows", so a DB problem never blocks a boss kill.
 */
public final class JdbcBossRunStore implements BossRunStore {
    static final String[] DDL = {
            "CREATE TABLE IF NOT EXISTS tms282_bossrun_run ("
                    + " run_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + " boss_key VARCHAR(48) NOT NULL, order_id INT NOT NULL, difficulty INT NOT NULL,"
                    + " field_id INT NOT NULL, mob_id INT NOT NULL, leader_id INT NOT NULL, leader_name VARCHAR(32) NOT NULL,"
                    + " party_size INT NOT NULL, clear_ms BIGINT NOT NULL, practice TINYINT NOT NULL DEFAULT 0,"
                    + " cleared_at DATETIME NOT NULL, members VARCHAR(512) NOT NULL DEFAULT '',"
                    + " KEY idx_bossrun_run_boss (boss_key, cleared_at)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS tms282_bossrun_clear ("
                    + " id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, run_id BIGINT NOT NULL,"
                    + " account_id INT NOT NULL, char_id INT NOT NULL, char_name VARCHAR(32) NOT NULL,"
                    + " boss_key VARCHAR(48) NOT NULL, cleared_at DATETIME NOT NULL, clear_ms BIGINT NOT NULL,"
                    + " damage_permille INT NOT NULL DEFAULT 0, counted TINYINT NOT NULL DEFAULT 1,"
                    + " note VARCHAR(64) NOT NULL DEFAULT '',"
                    + " KEY idx_bossrun_clear_account (account_id, boss_key, cleared_at),"
                    + " KEY idx_bossrun_clear_char (char_id, cleared_at),"
                    + " KEY idx_bossrun_clear_run (run_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS tms282_bossrun_pending ("
                    + " id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, char_id INT NOT NULL, item_id INT NOT NULL,"
                    + " quantity INT NOT NULL, source VARCHAR(64) NOT NULL DEFAULT '',"
                    + " created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " KEY idx_bossrun_pending_char (char_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
            "CREATE TABLE IF NOT EXISTS tms282_bossrun_pqlog ("
                    + " char_id INT NOT NULL, log_key INT NOT NULL, log_day DATE NOT NULL, count INT NOT NULL DEFAULT 0,"
                    + " PRIMARY KEY (char_id, log_key, log_day)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4",
    };

    private volatile boolean ready;

    private Connection connect() throws SQLException {
        Connection c = DatabaseManager.getConnection();
        if (!ready) {
            synchronized (this) {
                if (!ready) {
                    try (Statement s = c.createStatement()) {
                        for (String ddl : DDL) s.executeUpdate(ddl);
                    }
                    ready = true;
                }
            }
        }
        return c;
    }

    private static void log(SQLException e) {
        DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
    }

    @Override
    public long insertRun(RunRow r) {
        String sql = "INSERT INTO tms282_bossrun_run(boss_key, order_id, difficulty, field_id, mob_id, leader_id, leader_name,"
                + " party_size, clear_ms, practice, cleared_at, members) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, r.bossKey());
            p.setInt(2, r.orderId());
            p.setInt(3, r.difficulty());
            p.setInt(4, r.fieldId());
            p.setInt(5, r.mobId());
            p.setInt(6, r.leaderId());
            p.setString(7, cut(r.leaderName(), 32));
            p.setInt(8, r.partySize());
            p.setLong(9, r.clearMillis());
            p.setInt(10, r.practice() ? 1 : 0);
            p.setTimestamp(11, Timestamp.valueOf(r.clearedAt()));
            p.setString(12, cut(r.members(), 512));
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) {
                return k.next() ? k.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            log(e);
            return 0L;
        }
    }

    @Override
    public void insertClear(ClearRow r) {
        String sql = "INSERT INTO tms282_bossrun_clear(run_id, account_id, char_id, char_name, boss_key, cleared_at, clear_ms,"
                + " damage_permille, counted, note) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, r.runId());
            p.setInt(2, r.accountId());
            p.setInt(3, r.charId());
            p.setString(4, cut(r.charName(), 32));
            p.setString(5, r.bossKey());
            p.setTimestamp(6, Timestamp.valueOf(r.clearedAt()));
            p.setLong(7, r.clearMillis());
            p.setInt(8, r.damagePermille());
            p.setInt(9, r.counted() ? 1 : 0);
            p.setString(10, cut(r.note(), 64));
            p.executeUpdate();
        } catch (SQLException e) {
            log(e);
        }
    }

    private static List<ClearRow> clears(PreparedStatement p) throws SQLException {
        List<ClearRow> out = new ArrayList<>();
        try (ResultSet r = p.executeQuery()) {
            while (r.next()) {
                out.add(new ClearRow(r.getLong("run_id"), r.getInt("account_id"), r.getInt("char_id"), r.getString("char_name"),
                        r.getString("boss_key"), r.getTimestamp("cleared_at").toLocalDateTime(), r.getLong("clear_ms"),
                        r.getInt("damage_permille"), r.getInt("counted") != 0, r.getString("note")));
            }
        }
        return out;
    }

    @Override
    public List<ClearRow> countedClears(int accountId, String bossKey, LocalDateTime since) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "SELECT * FROM tms282_bossrun_clear WHERE account_id=? AND boss_key=? AND counted=1 AND cleared_at>=?")) {
            p.setInt(1, accountId);
            p.setString(2, bossKey);
            p.setTimestamp(3, Timestamp.valueOf(since));
            return clears(p);
        } catch (SQLException e) {
            log(e);
            return List.of();
        }
    }

    @Override
    public List<ClearRow> countedClears(int accountId, LocalDateTime since) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "SELECT * FROM tms282_bossrun_clear WHERE account_id=? AND counted=1 AND cleared_at>=?")) {
            p.setInt(1, accountId);
            p.setTimestamp(2, Timestamp.valueOf(since));
            return clears(p);
        } catch (SQLException e) {
            log(e);
            return List.of();
        }
    }

    @Override
    public List<ClearRow> recentClears(int accountId, int limit) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "SELECT * FROM tms282_bossrun_clear WHERE account_id=? ORDER BY cleared_at DESC, id DESC LIMIT ?")) {
            p.setInt(1, accountId);
            p.setInt(2, Math.max(1, limit));
            return clears(p);
        } catch (SQLException e) {
            log(e);
            return List.of();
        }
    }

    @Override
    public int resetCounted(int accountId, String bossKey) {
        String sql = "UPDATE tms282_bossrun_clear SET counted=0, note='GM reset' WHERE account_id=? AND counted=1"
                + (bossKey == null ? "" : " AND boss_key=?");
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, accountId);
            if (bossKey != null) p.setString(2, bossKey);
            return p.executeUpdate();
        } catch (SQLException e) {
            log(e);
            return 0;
        }
    }

    @Override
    public void addPending(int charId, int itemId, int quantity, String source) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO tms282_bossrun_pending(char_id, item_id, quantity, source) VALUES(?,?,?,?)")) {
            p.setInt(1, charId);
            p.setInt(2, itemId);
            p.setInt(3, quantity);
            p.setString(4, cut(source, 64));
            p.executeUpdate();
        } catch (SQLException e) {
            log(e);
        }
    }

    @Override
    public List<PendingRow> pending(int charId) {
        List<PendingRow> out = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "SELECT id, char_id, item_id, quantity, source FROM tms282_bossrun_pending WHERE char_id=? ORDER BY id")) {
            p.setInt(1, charId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new PendingRow(r.getLong(1), r.getInt(2), r.getInt(3), r.getInt(4), r.getString(5)));
            }
        } catch (SQLException e) {
            log(e);
        }
        return out;
    }

    @Override
    public boolean deletePending(long id) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "DELETE FROM tms282_bossrun_pending WHERE id=?")) {
            p.setLong(1, id);
            return p.executeUpdate() == 1;
        } catch (SQLException e) {
            log(e);
            return false;
        }
    }

    @Override
    public int pqCount(int charId, int logKey, LocalDate day) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "SELECT count FROM tms282_bossrun_pqlog WHERE char_id=? AND log_key=? AND log_day=?")) {
            p.setInt(1, charId);
            p.setInt(2, logKey);
            p.setDate(3, java.sql.Date.valueOf(day));
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        } catch (SQLException e) {
            log(e);
            return 0;
        }
    }

    @Override
    public void pqAdd(int charId, int logKey, LocalDate day, int amount) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO tms282_bossrun_pqlog(char_id, log_key, log_day, count) VALUES(?,?,?,?)"
                        + " ON DUPLICATE KEY UPDATE count=count+VALUES(count)")) {
            p.setInt(1, charId);
            p.setInt(2, logKey);
            p.setDate(3, java.sql.Date.valueOf(day));
            p.setInt(4, amount);
            p.executeUpdate();
        } catch (SQLException e) {
            log(e);
        }
    }

    @Override
    public int pqReset(int charId, int logKey) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(
                "DELETE FROM tms282_bossrun_pqlog WHERE char_id=? AND log_key=?")) {
            p.setInt(1, charId);
            p.setInt(2, logKey);
            return p.executeUpdate();
        } catch (SQLException e) {
            log(e);
            return 0;
        }
    }

    @Override
    public CharRef findChar(String name) {
        String sql = "SELECT c.id, a.userid, cs.name FROM characterstats cs JOIN characters c ON c.id = cs.characterid"
                + " JOIN accounts a ON a.id = c.accid WHERE cs.name = ? LIMIT 1";
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, name);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? new CharRef(r.getInt(1), r.getInt(2), r.getString(3)) : null;
            }
        } catch (SQLException e) {
            log(e);
            return null;
        }
    }

    @Override
    public int resetLegacyPartyBoss(int charId, int orderId, int difficulty) {
        String sql = "DELETE FROM partyboss WHERE charid=?" + (orderId < 0 ? "" : " AND orderid=?" + (difficulty < 0 ? "" : " AND difficulty=?"));
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, charId);
            if (orderId >= 0) {
                p.setInt(2, orderId);
                if (difficulty >= 0) p.setInt(3, difficulty);
            }
            return p.executeUpdate();
        } catch (SQLException e) {
            log(e);
            return 0;
        }
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
