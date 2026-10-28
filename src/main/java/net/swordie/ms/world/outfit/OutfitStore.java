package net.swordie.ms.world.outfit;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared outfits in the database: {@code outfit_share} (one row per outfit, the item ids as a comma list) and
 * {@code outfit_like} (one like per account and outfit). The tables are created on first use.
 */
public final class OutfitStore {
    public static final int MAX_PER_USER = 10;
    public static final int PAGE = 8;
    static final String[] DDL = {
            "CREATE TABLE IF NOT EXISTS outfit_share ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "user_id INT NOT NULL,"
                    + "char_id INT NOT NULL,"
                    + "char_name VARCHAR(32) NOT NULL,"
                    + "name VARCHAR(32) NOT NULL,"
                    + "gender TINYINT NOT NULL,"
                    + "skin INT NOT NULL,"
                    + "face INT NOT NULL,"
                    + "hair INT NOT NULL,"
                    + "mix_color INT NOT NULL DEFAULT 0,"
                    + "mix_percent INT NOT NULL DEFAULT 0,"
                    + "weapon_sticker INT NOT NULL DEFAULT 0,"
                    + "weapon INT NOT NULL DEFAULT 0,"
                    + "elf_ear TINYINT(1) NOT NULL DEFAULT 0,"
                    + "items VARCHAR(600) NOT NULL,"
                    + "likes INT NOT NULL DEFAULT 0,"
                    + "created_at DATETIME(3) NOT NULL,"
                    + "PRIMARY KEY (id), KEY idx_outfit_user (user_id), KEY idx_outfit_created (created_at), KEY idx_outfit_likes (likes)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS outfit_like ("
                    + "outfit_id INT NOT NULL,"
                    + "user_id INT NOT NULL,"
                    + "PRIMARY KEY (outfit_id, user_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
    };
    private static final String COLUMNS = "id, user_id, char_id, char_name, name, gender, skin, face, hair, mix_color, mix_percent,"
            + " weapon_sticker, weapon, elf_ear, items, likes, created_at";
    private static final OutfitStore STANDARD = new OutfitStore();
    private volatile boolean ready;

    public static OutfitStore standard() {
        return STANDARD;
    }

    private Connection open() throws SQLException {
        Connection con = DatabaseManager.getConnection();
        if (!ready) {
            synchronized (this) {
                if (!ready) {
                    try (Statement st = con.createStatement()) {
                        for (String ddl : DDL) {
                            st.execute(ddl);
                        }
                    } catch (SQLException e) {
                        con.close();
                        throw e;
                    }
                    ready = true;
                }
            }
        }
        return con;
    }

    static String join(List<Integer> items) {
        return items.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    static List<Integer> split(String items) {
        List<Integer> out = new ArrayList<>();
        for (String s : items == null ? new String[0] : items.split(",")) {
            if (s.matches("\\d{1,9}")) {
                out.add(Integer.parseInt(s));
            }
        }
        return out;
    }

    private static Outfit row(ResultSet rs) throws SQLException {
        return new Outfit(rs.getInt("id"), rs.getInt("user_id"), rs.getInt("char_id"), rs.getString("char_name"),
                rs.getString("name"), rs.getInt("gender"), rs.getInt("skin"), rs.getInt("face"), rs.getInt("hair"),
                rs.getInt("mix_color"), rs.getInt("mix_percent"), rs.getInt("weapon_sticker"), rs.getInt("weapon"),
                rs.getBoolean("elf_ear"), split(rs.getString("items")), rs.getInt("likes"),
                rs.getTimestamp("created_at").getTime());
    }

    public Outfit save(Outfit o) throws SQLException {
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement("INSERT INTO outfit_share (user_id, char_id, char_name,"
                + " name, gender, skin, face, hair, mix_color, mix_percent, weapon_sticker, weapon, elf_ear, items, likes, created_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,0,?)", Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            ps.setInt(i++, o.userId());
            ps.setInt(i++, o.charId());
            ps.setString(i++, o.charName());
            ps.setString(i++, o.name());
            ps.setInt(i++, o.gender());
            ps.setInt(i++, o.skin());
            ps.setInt(i++, o.face());
            ps.setInt(i++, o.hair());
            ps.setInt(i++, o.mixColor());
            ps.setInt(i++, o.mixPercent());
            ps.setInt(i++, o.weaponSticker());
            ps.setInt(i++, o.weapon());
            ps.setBoolean(i++, o.elfEar());
            ps.setString(i++, join(o.items()));
            ps.setTimestamp(i, new Timestamp(o.createdAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? o.withId(keys.getInt(1)) : o;
            }
        }
    }

    public int countOf(int userId) throws SQLException {
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM outfit_share WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** One page ({@link #PAGE} rows): newest first, or most liked first; {@code userId > 0} = only that account's. */
    public List<Outfit> list(boolean byLikes, int userId, int page) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM outfit_share" + (userId > 0 ? " WHERE user_id = ?" : "")
                + " ORDER BY " + (byLikes ? "likes DESC, id DESC" : "id DESC") + " LIMIT ? OFFSET ?";
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            if (userId > 0) {
                ps.setInt(i++, userId);
            }
            ps.setInt(i++, PAGE);
            ps.setInt(i, Math.max(0, page) * PAGE);
            List<Outfit> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(row(rs));
                }
            }
            return out;
        }
    }

    public Outfit get(int id) throws SQLException {
        try (Connection con = open(); PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNS + " FROM outfit_share WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? row(rs) : null;
            }
        }
    }

    /** Deletes the account's own outfit (and its likes); false when it is not theirs. */
    public boolean delete(int id, int userId) throws SQLException {
        try (Connection con = open()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM outfit_share WHERE id = ? AND user_id = ?")) {
                ps.setInt(1, id);
                ps.setInt(2, userId);
                if (ps.executeUpdate() == 0) {
                    return false;
                }
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM outfit_like WHERE outfit_id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return true;
        }
    }

    /** One like per account: true when this call added it, false when it was already there. */
    public boolean like(int id, int userId) throws SQLException {
        try (Connection con = open()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO outfit_like (outfit_id, user_id) VALUES (?, ?)")) {
                ps.setInt(1, id);
                ps.setInt(2, userId);
                if (ps.executeUpdate() == 0) {
                    return false;
                }
            }
            try (PreparedStatement ps = con.prepareStatement("UPDATE outfit_share SET likes = likes + 1 WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            return true;
        }
    }
}
