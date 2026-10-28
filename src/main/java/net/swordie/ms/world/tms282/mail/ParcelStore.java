package net.swordie.ms.world.tms282.mail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC access to tms282_parcel (connections and transactions come from the caller). */
final class ParcelStore {
    private ParcelStore() { }

    static int insert(Connection c, Parcel p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO tms282_parcel (created_at, available_at, receiver_id, "
                + "receiver_name, sender, sender_id, original_sender_id, money, expire, delivery_type, memo, sn, item_id, "
                + "quantity) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, Timestamp.valueOf(p.createdAt()));
            ps.setTimestamp(2, Timestamp.valueOf(p.availableAt()));
            ps.setInt(3, p.receiverId());
            ps.setString(4, p.receiverName());
            ps.setString(5, p.sender());
            ps.setInt(6, p.senderId());
            ps.setInt(7, p.originalSenderId());
            ps.setLong(8, p.money());
            ps.setTimestamp(9, Timestamp.valueOf(p.expire()));
            ps.setInt(10, p.deliveryType());
            ps.setString(11, p.memo());
            if (p.itemRow() > 0) ps.setLong(12, p.itemRow()); else ps.setNull(12, java.sql.Types.BIGINT);
            ps.setInt(13, p.itemId());
            ps.setInt(14, p.quantity());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("parcel insert returned no id");
                return keys.getInt(1);
            }
        }
    }

    static void setItemRow(Connection c, int parcelId, long itemRow) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE tms282_parcel SET sn = ? WHERE id = ?")) {
            ps.setLong(1, itemRow);
            ps.setInt(2, parcelId);
            if (ps.executeUpdate() != 1) throw new SQLException("parcel " + parcelId + " vanished");
        }
    }

    static List<Parcel> listFor(Connection c, int receiverId) throws SQLException {
        List<Parcel> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM tms282_parcel WHERE receiver_id = ? ORDER BY id")) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(read(rs));
            }
        }
        return out;
    }

    static int countSentPending(Connection c, int senderId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT COUNT(*) FROM tms282_parcel WHERE sender_id = ? AND delivery_type <> 2")) {
            ps.setInt(1, senderId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    static int countFor(Connection c, int receiverId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM tms282_parcel WHERE receiver_id = ?")) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    static Optional<Parcel> lockForReceiver(Connection c, int id, int receiverId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM tms282_parcel WHERE id = ? AND receiver_id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            ps.setInt(2, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    /** Parcels whose expire time has passed (limit per sweep). */
    static List<Parcel> expired(Connection c, LocalDateTime now, int limit) throws SQLException {
        List<Parcel> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM tms282_parcel WHERE expire <= ? ORDER BY expire LIMIT ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(read(rs));
            }
        }
        return out;
    }

    static boolean delete(Connection c, int id, int receiverId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM tms282_parcel WHERE id = ? AND receiver_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, receiverId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Turns an expired parcel around to its sender; guarded by the old receiver and type. */
    static boolean returnToSender(Connection c, Parcel p, LocalDateTime now, LocalDateTime newExpire) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE tms282_parcel SET receiver_id = ?, receiver_name = ?, "
                + "sender = ?, sender_id = ?, delivery_type = 2, available_at = ?, expire = ? "
                + "WHERE id = ? AND receiver_id = ? AND delivery_type <> 2")) {
            ps.setInt(1, p.senderId());
            ps.setString(2, "");
            ps.setString(3, p.receiverName());
            ps.setInt(4, p.receiverId());
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setTimestamp(6, Timestamp.valueOf(newExpire));
            ps.setInt(7, p.id());
            ps.setInt(8, p.receiverId());
            return ps.executeUpdate() == 1;
        }
    }

    private static Parcel read(ResultSet rs) throws SQLException {
        return new Parcel(rs.getInt("id"), ts(rs, "created_at"), ts(rs, "available_at"), rs.getInt("receiver_id"),
                rs.getString("receiver_name"), rs.getString("sender"), rs.getInt("sender_id"),
                rs.getInt("original_sender_id"), rs.getLong("money"), ts(rs, "expire"), rs.getInt("delivery_type"),
                rs.getString("memo"), rs.getLong("sn"), rs.getInt("item_id"), rs.getInt("quantity"));
    }

    private static LocalDateTime ts(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t != null ? t.toLocalDateTime() : LocalDateTime.now();
    }
}
