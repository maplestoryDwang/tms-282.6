package net.swordie.ms.world.shop;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ItemWarehouseManager {

    public static class Entry {
        private final long id;
        private final int itemId;
        private final int quantity;

        public Entry(long id, int itemId, int quantity) {
            this.id = id;
            this.itemId = itemId;
            this.quantity = quantity;
        }

        public long getId() {
            return id;
        }

        public int getItemId() {
            return itemId;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    public static List<Entry> getPendingItems(int characterId) {
        List<Entry> entries = new ArrayList<>();
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, item_id, quantity FROM item_warehouse "
                             + "WHERE character_id = ? AND is_deleted = 0 ORDER BY id")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new Entry(
                            rs.getLong("id"),
                            rs.getInt("item_id"),
                            rs.getInt("quantity")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public static boolean markClaimed(long entryId) {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE item_warehouse SET is_deleted = 1, update_time = ? "
                             + "WHERE id = ? AND is_deleted = 0")) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setLong(2, entryId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
