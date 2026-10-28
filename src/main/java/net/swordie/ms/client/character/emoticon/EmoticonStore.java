package net.swordie.ms.client.character.emoticon;

import net.swordie.ms.client.character.emoticon.EmoticonService.Command;
import net.swordie.ms.client.character.emoticon.EmoticonService.Owned;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** JDBC access to {@link EmoticonTables}. Every method opens its own connection (write-through, no batching). */
final class EmoticonStore {

    private EmoticonStore() {
    }

    static List<Owned> loadOwned(int characterId) throws SQLException {
        List<Owned> out = new ArrayList<>();
        try (Connection c = EmoticonTables.connection();
             PreparedStatement ps = c.prepareStatement("SELECT emoticon_id, group_id, expire_at, favorite_pos FROM "
                     + EmoticonTables.EMOTICON + " WHERE character_id = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp(3);
                    out.add(new Owned(rs.getInt(1), rs.getInt(2), ts == null ? null : ts.toInstant(), rs.getInt(4)));
                }
            }
        }
        return out;
    }

    static List<Command> loadCommands(int characterId) throws SQLException {
        List<Command> out = new ArrayList<>();
        try (Connection c = EmoticonTables.connection();
             PreparedStatement ps = c.prepareStatement("SELECT `index`, emoticon_id, command FROM "
                     + EmoticonTables.COMMAND + " WHERE character_id = ? ORDER BY `index`")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Command(rs.getInt(1), rs.getInt(2), rs.getString(3)));
                }
            }
        }
        return out;
    }

    static void upsertOwned(int characterId, Owned o) throws SQLException {
        try (Connection c = EmoticonTables.connection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO " + EmoticonTables.EMOTICON
                     + " (character_id, emoticon_id, group_id, expire_at, favorite_pos) VALUES (?, ?, ?, ?, ?)"
                     + " ON DUPLICATE KEY UPDATE group_id = VALUES(group_id), expire_at = VALUES(expire_at),"
                     + " favorite_pos = VALUES(favorite_pos)")) {
            ps.setInt(1, characterId);
            ps.setInt(2, o.emoticonId());
            ps.setInt(3, o.groupId());
            if (o.expireAt() == null) {
                ps.setNull(4, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(4, Timestamp.from(o.expireAt()));
            }
            ps.setInt(5, o.favoritePos());
            ps.executeUpdate();
        }
    }

    static void deleteOwned(int characterId, List<Integer> emoticonIds) throws SQLException {
        if (emoticonIds.isEmpty()) {
            return;
        }
        try (Connection c = EmoticonTables.connection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + EmoticonTables.EMOTICON
                     + " WHERE character_id = ? AND emoticon_id = ?")) {
            for (int id : emoticonIds) {
                ps.setInt(1, characterId);
                ps.setInt(2, id);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    static void upsertCommand(int characterId, Command cmd) throws SQLException {
        try (Connection c = EmoticonTables.connection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO " + EmoticonTables.COMMAND
                     + " (character_id, `index`, emoticon_id, command) VALUES (?, ?, ?, ?)"
                     + " ON DUPLICATE KEY UPDATE emoticon_id = VALUES(emoticon_id), command = VALUES(command)")) {
            ps.setInt(1, characterId);
            ps.setInt(2, cmd.index());
            ps.setInt(3, cmd.emoticonId());
            ps.setString(4, cmd.command());
            ps.executeUpdate();
        }
    }

    static void deleteCommands(int characterId, List<Integer> indexes) throws SQLException {
        if (indexes.isEmpty()) {
            return;
        }
        try (Connection c = EmoticonTables.connection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + EmoticonTables.COMMAND
                     + " WHERE character_id = ? AND `index` = ?")) {
            for (int idx : indexes) {
                ps.setInt(1, characterId);
                ps.setInt(2, idx);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
