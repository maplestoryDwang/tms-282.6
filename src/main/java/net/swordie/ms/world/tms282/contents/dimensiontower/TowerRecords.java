package net.swordie.ms.world.tms282.contents.dimensiontower;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Best clear times of 次元之塔 per season and character. The SQL table is created with CREATE TABLE IF NOT EXISTS (the
 * only DDL the server account may run; the same statement opens deploy/content-1005/sql/20261005-dimension-tower.sql,
 * compared by tools/Tms282DimensionTowerCheck). Ranking: best total time ascending, then the earlier record.
 */
public interface TowerRecords {
    record Entry(int characterId, int accountId, String name, long bestMillis, long stage1Millis, int clears,
                 long updatedAt) {
    }

    /** Stores a clear; keeps the better time (stage-1 split of the best run). Returns the entry after the update. */
    Entry record(String season, int characterId, int accountId, String name, long totalMillis, long stage1Millis, long at)
            throws SQLException;

    Entry find(String season, int characterId) throws SQLException;

    /** The first {@code limit} entries of the season's ranking. */
    List<Entry> top(String season, int limit) throws SQLException;

    /** 1-based rank of the character, 0 without a record. */
    int rank(String season, int characterId) throws SQLException;

    Comparator<Entry> ORDER = Comparator.comparingLong(Entry::bestMillis).thenComparingLong(Entry::updatedAt)
            .thenComparingInt(Entry::characterId);

    String DDL = "CREATE TABLE IF NOT EXISTS tms282_dimension_tower ("
            + "season VARCHAR(16) NOT NULL,"
            + "character_id INT NOT NULL,"
            + "account_id INT NOT NULL,"
            + "character_name VARCHAR(32) NOT NULL,"
            + "best_ms BIGINT NOT NULL,"
            + "stage1_ms BIGINT NOT NULL,"
            + "clears INT NOT NULL,"
            + "updated_at BIGINT NOT NULL,"
            + "PRIMARY KEY (season, character_id), KEY idx_tower_rank (season, best_ms, updated_at)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

    /** In-memory table (tests). */
    final class Memory implements TowerRecords {
        private final Map<String, Entry> rows = new HashMap<>();

        @Override
        public synchronized Entry record(String season, int characterId, int accountId, String name, long totalMillis,
                                         long stage1Millis, long at) {
            String k = season + "|" + characterId;
            Entry old = rows.get(k);
            Entry e;
            if (old == null) {
                e = new Entry(characterId, accountId, name, totalMillis, stage1Millis, 1, at);
            } else if (totalMillis < old.bestMillis()) {
                e = new Entry(characterId, accountId, name, totalMillis, stage1Millis, old.clears() + 1, at);
            } else {
                e = new Entry(characterId, accountId, name, old.bestMillis(), old.stage1Millis(), old.clears() + 1, old.updatedAt());
            }
            rows.put(k, e);
            return e;
        }

        @Override
        public synchronized Entry find(String season, int characterId) {
            return rows.get(season + "|" + characterId);
        }

        @Override
        public synchronized List<Entry> top(String season, int limit) {
            List<Entry> out = new ArrayList<>();
            rows.forEach((k, v) -> {
                if (k.startsWith(season + "|")) {
                    out.add(v);
                }
            });
            out.sort(ORDER);
            return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
        }

        @Override
        public synchronized int rank(String season, int characterId) {
            List<Entry> all = top(season, Integer.MAX_VALUE);
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).characterId() == characterId) {
                    return i + 1;
                }
            }
            return 0;
        }
    }

    /** The server table. */
    final class Sql implements TowerRecords {
        private volatile boolean ready;

        private Connection open() throws SQLException {
            Connection c = DatabaseManager.getConnection();
            if (!ready) {
                try (Statement s = c.createStatement()) {
                    s.executeUpdate(DDL);
                }
                ready = true;
            }
            return c;
        }

        @Override
        public Entry record(String season, int characterId, int accountId, String name, long totalMillis, long stage1Millis,
                            long at) throws SQLException {
            try (Connection c = open(); PreparedStatement p = c.prepareStatement(
                    "INSERT INTO tms282_dimension_tower(season,character_id,account_id,character_name,best_ms,stage1_ms,clears,updated_at)"
                            + " VALUES(?,?,?,?,?,?,1,?) ON DUPLICATE KEY UPDATE"
                            + " stage1_ms=IF(VALUES(best_ms)<best_ms,VALUES(stage1_ms),stage1_ms),"
                            + " updated_at=IF(VALUES(best_ms)<best_ms,VALUES(updated_at),updated_at),"
                            + " best_ms=LEAST(best_ms,VALUES(best_ms)), clears=clears+1, character_name=VALUES(character_name)")) {
                p.setString(1, season);
                p.setInt(2, characterId);
                p.setInt(3, accountId);
                p.setString(4, name);
                p.setLong(5, totalMillis);
                p.setLong(6, stage1Millis);
                p.setLong(7, at);
                p.executeUpdate();
            }
            return find(season, characterId);
        }

        private static Entry entry(ResultSet r) throws SQLException {
            return new Entry(r.getInt("character_id"), r.getInt("account_id"), r.getString("character_name"),
                    r.getLong("best_ms"), r.getLong("stage1_ms"), r.getInt("clears"), r.getLong("updated_at"));
        }

        @Override
        public Entry find(String season, int characterId) throws SQLException {
            try (Connection c = open(); PreparedStatement p = c.prepareStatement(
                    "SELECT * FROM tms282_dimension_tower WHERE season=? AND character_id=?")) {
                p.setString(1, season);
                p.setInt(2, characterId);
                try (ResultSet r = p.executeQuery()) {
                    return r.next() ? entry(r) : null;
                }
            }
        }

        @Override
        public List<Entry> top(String season, int limit) throws SQLException {
            List<Entry> out = new ArrayList<>();
            try (Connection c = open(); PreparedStatement p = c.prepareStatement(
                    "SELECT * FROM tms282_dimension_tower WHERE season=? ORDER BY best_ms, updated_at, character_id LIMIT ?")) {
                p.setString(1, season);
                p.setInt(2, limit);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        out.add(entry(r));
                    }
                }
            }
            return out;
        }

        @Override
        public int rank(String season, int characterId) throws SQLException {
            Entry me = find(season, characterId);
            if (me == null) {
                return 0;
            }
            try (Connection c = open(); PreparedStatement p = c.prepareStatement(
                    "SELECT COUNT(*) FROM tms282_dimension_tower WHERE season=? AND (best_ms<? OR (best_ms=? AND"
                            + " (updated_at<? OR (updated_at=? AND character_id<?))))")) {
                p.setString(1, season);
                p.setLong(2, me.bestMillis());
                p.setLong(3, me.bestMillis());
                p.setLong(4, me.updatedAt());
                p.setLong(5, me.updatedAt());
                p.setInt(6, characterId);
                try (ResultSet r = p.executeQuery()) {
                    r.next();
                    return r.getInt(1) + 1;
                }
            }
        }
    }
}
