package net.swordie.ms.world.shop.cashshop;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.*;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Per-character cash shop wish list ({@code cs_wishlist}, the TMS279 layout: character_id, position, commodity_sn).
 * The 282 client keeps exactly {@link #SIZE} slots (CP0741 kind 4 Buf48 / LP0A76 kinds 0x0B, 0x0D Buf48).
 */
public final class CashShopWishlist {
    public static final int SIZE = 12;

    static final String DDL = "CREATE TABLE IF NOT EXISTS cs_wishlist ("
            + "character_id INT NOT NULL,"
            + "position INT NOT NULL,"
            + "commodity_sn BIGINT NULL,"
            + "PRIMARY KEY (character_id, position)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    private static volatile boolean ready;

    private CashShopWishlist() {
    }

    static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate(DDL);
        }
        ready = true;
    }

    /** The stored list, zero padded to {@link #SIZE}; all zeros when nothing is stored or the table is unavailable. */
    public static int[] load(int characterId) {
        int[] list = new int[SIZE];
        try {
            ensure();
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                    "SELECT position, commodity_sn FROM cs_wishlist WHERE character_id = ? ORDER BY position")) {
                p.setInt(1, characterId);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        int pos = r.getInt(1);
                        long sn = r.getLong(2);
                        if (pos >= 0 && pos < SIZE && sn > 0 && sn <= Integer.MAX_VALUE) list[pos] = (int) sn;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("[CashShop] wish list unavailable: " + e.getMessage());
        }
        return compact(list, sn -> true);
    }

    /** Replaces the stored list (one transaction). */
    public static void save(int characterId, int[] list) throws SQLException {
        ensure();
        int[] rows = compact(list, sn -> true);
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement("DELETE FROM cs_wishlist WHERE character_id = ?")) {
                    p.setInt(1, characterId);
                    p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO cs_wishlist (character_id, position, commodity_sn) VALUES (?,?,?)")) {
                    for (int i = 0; i < SIZE; i++) {
                        if (rows[i] <= 0) continue;
                        p.setInt(1, characterId);
                        p.setInt(2, i);
                        p.setLong(3, rows[i]);
                        p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /**
     * The list after a client change: the client's own list (valid SNs, no duplicates, in order), then {@code sn} added
     * (mode add) or removed (mode remove). Whether the client's Buf48 already holds the change is not established, so
     * both are applied; the result is the same either way.
     */
    public static int[] apply(int[] clientList, boolean add, int sn, IntPredicate valid) {
        Set<Integer> result = new LinkedHashSet<>();
        for (int value : clientList) if (value > 0 && valid.test(value)) result.add(value);
        if (add) {
            if (sn > 0 && valid.test(sn)) result.add(sn);
        } else {
            result.remove(sn);
        }
        int[] out = new int[SIZE];
        int i = 0;
        for (int value : result) {
            if (i >= SIZE) break;
            out[i++] = value;
        }
        return out;
    }

    private static int[] compact(int[] list, IntPredicate valid) {
        int[] out = new int[SIZE];
        int i = 0;
        Set<Integer> seen = new LinkedHashSet<>();
        for (int value : list) {
            if (i >= SIZE) break;
            if (value > 0 && valid.test(value) && seen.add(value)) out[i++] = value;
        }
        return out;
    }

    public static String toString(int[] list) {
        return Arrays.toString(list);
    }
}
