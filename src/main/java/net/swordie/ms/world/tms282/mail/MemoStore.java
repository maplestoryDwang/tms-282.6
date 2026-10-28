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

/** JDBC access to tms282_memo / tms282_memo_record (connections come from the caller). */
final class MemoStore {
    private MemoStore() { }

    static int countFor(Connection c, int receiverId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM tms282_memo WHERE receiver_id = ?")) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    static int insert(Connection c, Memo m) throws SQLException {
        int id;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO tms282_memo (flag, state, sender_character_id, "
                + "sender_account_id, sender_name, sent_time, receiver_id, receiver_name, account_memo, content) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.flag());
            ps.setInt(2, m.state());
            ps.setInt(3, m.senderCharacterId());
            ps.setInt(4, m.senderAccountId());
            ps.setString(5, m.senderName());
            ps.setTimestamp(6, Timestamp.valueOf(m.sentTime()));
            ps.setInt(7, m.receiverId());
            ps.setString(8, m.receiverName());
            ps.setBoolean(9, m.accountMemo());
            ps.setString(10, m.content());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("memo insert returned no id");
                id = keys.getInt(1);
            }
        }
        record(c, id, Memo.UNREAD, m);
        return id;
    }

    static void record(Connection c, int memoId, int state, Memo m) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO tms282_memo_record (memo_id, state, flag, sender_id, "
                + "sender_name, receiver_id, receiver_name, content, sent_time) VALUES (?,?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, memoId);
            ps.setInt(2, state);
            ps.setInt(3, m.flag());
            ps.setInt(4, m.senderCharacterId());
            ps.setString(5, m.senderName());
            ps.setInt(6, m.receiverId());
            ps.setString(7, m.receiverName());
            ps.setString(8, m.content());
            ps.setTimestamp(9, Timestamp.valueOf(m.sentTime()));
            ps.executeUpdate();
        }
    }

    static List<Memo> listFor(Connection c, int receiverId, boolean unreadOnly) throws SQLException {
        List<Memo> out = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM tms282_memo WHERE receiver_id = ?"
                + (unreadOnly ? " AND state = 0" : "") + " ORDER BY id")) {
            ps.setInt(1, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(read(rs));
            }
        }
        return out;
    }

    static Optional<Memo> get(Connection c, int id, int receiverId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM tms282_memo WHERE id = ? AND receiver_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, receiverId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    static boolean markRead(Connection c, int id, int receiverId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE tms282_memo SET state = 1 WHERE id = ? AND receiver_id = ? AND state = 0")) {
            ps.setInt(1, id);
            ps.setInt(2, receiverId);
            return ps.executeUpdate() == 1;
        }
    }

    static boolean delete(Connection c, int id, int receiverId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("DELETE FROM tms282_memo WHERE id = ? AND receiver_id = ?")) {
            ps.setInt(1, id);
            ps.setInt(2, receiverId);
            return ps.executeUpdate() == 1;
        }
    }

    private static Memo read(ResultSet rs) throws SQLException {
        Timestamp t = rs.getTimestamp("sent_time");
        return new Memo(rs.getInt("id"), rs.getInt("flag"), rs.getInt("state"), rs.getInt("sender_character_id"),
                rs.getInt("sender_account_id"), rs.getString("sender_name"),
                t != null ? t.toLocalDateTime() : LocalDateTime.now(), rs.getInt("receiver_id"),
                rs.getString("receiver_name"), rs.getBoolean("account_memo"), rs.getString("content"));
    }
}
