package net.swordie.ms.world.tms282.urus;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.*;

/** Weekly Urus shop purchase counts per character (table urus_shop_buy, copy in sql/tms282/20260928-urus-shop.sql). */
final class UrusPurchaseLedger {
    private static volatile boolean ready;

    private UrusPurchaseLedger() {
    }

    private static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS urus_shop_buy (char_id INT NOT NULL, item_id INT NOT NULL,"
                    + " week_key INT NOT NULL, count INT NOT NULL DEFAULT 0,"
                    + " last_buy TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + " PRIMARY KEY(char_id, item_id, week_key)) ENGINE=InnoDB");
        }
        ready = true;
    }

    static int count(int charId, int itemId, int weekKey) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT count FROM urus_shop_buy WHERE char_id=? AND item_id=? AND week_key=?")) {
            p.setInt(1, charId);
            p.setInt(2, itemId);
            p.setInt(3, weekKey);
            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getInt(1) : 0;
            }
        }
    }

    /** Reserves {@code quantity} purchases within {@code limit}; returns false if the limit would be exceeded. */
    static boolean reserve(int charId, int itemId, int weekKey, int quantity, int limit) throws SQLException {
        if (quantity <= 0) return false;
        ensure();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT IGNORE INTO urus_shop_buy(char_id, item_id, week_key, count) VALUES(?, ?, ?, 0)")) {
                    p.setInt(1, charId);
                    p.setInt(2, itemId);
                    p.setInt(3, weekKey);
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE urus_shop_buy SET count=count+?, last_buy=CURRENT_TIMESTAMP"
                                + " WHERE char_id=? AND item_id=? AND week_key=? AND count<=?")) {
                    p.setInt(1, quantity);
                    p.setInt(2, charId);
                    p.setInt(3, itemId);
                    p.setInt(4, weekKey);
                    p.setInt(5, limit - quantity);
                    if (p.executeUpdate() != 1) {
                        c.rollback();
                        return false;
                    }
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    static void cancel(int charId, int itemId, int weekKey, int quantity) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE urus_shop_buy SET count=count-? WHERE char_id=? AND item_id=? AND week_key=? AND count>=?")) {
            p.setInt(1, quantity);
            p.setInt(2, charId);
            p.setInt(3, itemId);
            p.setInt(4, weekKey);
            p.setInt(5, quantity);
            p.executeUpdate();
        }
    }
}
