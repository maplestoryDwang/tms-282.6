package net.swordie.ms.world.tms282.jewel;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Jewel persistence: tms282_jewel (state) + tms282_jewel_log (audit of every operation). */
public final class JewelStore {
    private JewelStore() { }

    public static final String TABLE = "tms282_jewel";
    public static final String LOG_TABLE = "tms282_jewel_log";

    static final String CREATE_JEWEL = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
            + "id BIGINT NOT NULL AUTO_INCREMENT,"
            + "character_id INT NOT NULL,"
            + "tier INT NOT NULL,"
            + "engrave_level INT NOT NULL DEFAULT 0,"
            + "awaken_level INT NOT NULL DEFAULT 0,"
            + "polish VARCHAR(255) NOT NULL DEFAULT '',"
            + "source_item_id INT NOT NULL DEFAULT 0,"
            + "status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',"
            + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
            + "updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),"
            + "PRIMARY KEY (id),"
            + "KEY idx_char (character_id,status)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    static final String CREATE_LOG = "CREATE TABLE IF NOT EXISTS " + LOG_TABLE + " ("
            + "id BIGINT NOT NULL AUTO_INCREMENT,"
            + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
            + "jewel_id BIGINT NOT NULL,"
            + "character_id INT NOT NULL,"
            + "op VARCHAR(24) NOT NULL,"
            + "outcome VARCHAR(24) NOT NULL,"
            + "before_state VARCHAR(255) NOT NULL,"
            + "after_state VARCHAR(255) NOT NULL,"
            + "cost_item_id INT NOT NULL DEFAULT 0,"
            + "cost_item_count INT NOT NULL DEFAULT 0,"
            + "cost_meso BIGINT NOT NULL DEFAULT 0,"
            + "PRIMARY KEY (id),"
            + "KEY idx_jewel (jewel_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

    private static volatile boolean ready;

    static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(CREATE_JEWEL);
            s.executeUpdate(CREATE_LOG);
        }
        ready = true;
    }

    private static JewelState read(ResultSet r, int size) throws SQLException {
        return new JewelState(r.getLong("id"), r.getInt("character_id"), r.getInt("tier"), r.getInt("engrave_level"),
                r.getInt("awaken_level"), JewelState.parsePolish(r.getString("polish"), size), r.getInt("source_item_id"),
                r.getString("status"));
    }

    static JewelState insert(int characterId, int tier, int sourceItemId, int size) throws SQLException {
        ensure();
        JewelState fresh = new JewelState(0, characterId, tier, 0, 0, new int[size], sourceItemId, JewelState.ACTIVE);
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO " + TABLE + " (character_id,tier,polish,source_item_id) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, characterId); p.setInt(2, tier); p.setString(3, fresh.polishCsv()); p.setInt(4, sourceItemId);
            p.executeUpdate();
            try (ResultSet k = p.getGeneratedKeys()) {
                if (!k.next()) throw new SQLException("no jewel id");
                return new JewelState(k.getLong(1), characterId, tier, 0, 0, new int[size], sourceItemId, JewelState.ACTIVE);
            }
        }
    }

    static List<JewelState> list(int characterId, int size) throws SQLException {
        ensure();
        List<JewelState> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT * FROM " + TABLE + " WHERE character_id=? AND status='ACTIVE' ORDER BY id")) {
            p.setInt(1, characterId);
            try (ResultSet r = p.executeQuery()) { while (r.next()) out.add(read(r, size)); }
        }
        return out;
    }

    static JewelState get(int characterId, long id, int size) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT * FROM " + TABLE + " WHERE id=? AND character_id=?")) {
            p.setLong(1, id); p.setInt(2, characterId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? read(r, size) : null; }
        }
    }

    /** Optimistic update: only succeeds if the row still equals {@code before}. Writes the audit row in the same tx. */
    static boolean update(JewelState before, JewelState after, String op, String outcome, int costItemId, int costCount,
                          long costMeso) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("UPDATE " + TABLE
                        + " SET engrave_level=?, awaken_level=?, polish=?, status=? WHERE id=? AND character_id=?"
                        + " AND engrave_level=? AND awaken_level=? AND polish=? AND status=?")) {
                    p.setInt(1, after.engraveLevel()); p.setInt(2, after.awakenLevel()); p.setString(3, after.polishCsv());
                    p.setString(4, after.status()); p.setLong(5, before.id()); p.setInt(6, before.characterId());
                    p.setInt(7, before.engraveLevel()); p.setInt(8, before.awakenLevel()); p.setString(9, before.polishCsv());
                    p.setString(10, before.status());
                    if (p.executeUpdate() != 1) {
                        c.rollback();
                        return false;
                    }
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO " + LOG_TABLE
                        + " (jewel_id,character_id,op,outcome,before_state,after_state,cost_item_id,cost_item_count,cost_meso)"
                        + " VALUES (?,?,?,?,?,?,?,?,?)")) {
                    p.setLong(1, before.id()); p.setInt(2, before.characterId()); p.setString(3, op); p.setString(4, outcome);
                    p.setString(5, before.toString()); p.setString(6, after.toString());
                    p.setInt(7, costItemId); p.setInt(8, costCount); p.setLong(9, costMeso);
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
}
