package net.swordie.ms.client.character.tms282;

import javax.sql.DataSource;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 282 萌獸 records ({@code sql/tms282_familiar.sql}, table {@code character_familiar282}): one row per registered
 * familiar, the row's auto-increment serial is the native RegisteredInfo serial. Names are stored as the CP950 wire
 * bytes. The table is never created automatically.
 */
public class Tms282FamiliarStore {
    private static final Charset CP950 = Charset.forName("MS950");
    private final DataSource source;

    /** The server database (DatabaseManager). */
    public Tms282FamiliarStore() {
        this.source = null;
    }

    public Tms282FamiliarStore(DataSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    Connection connection() throws SQLException {
        return source == null ? net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection() : source.getConnection();
    }

    /** The character's registered familiars, summoned serial and essence (character_familiar282_user). */
    public Tms282FamiliarState load(int characterId) throws SQLException {
        Map<Long, Tms282FamiliarState.RegisteredInfo> records = new HashMap<>();
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("SELECT serial, state, familiar_id, name, level, skill_id, exp,"
                     + " option_level, option1, option2, option3, attributes, grade, look_id"
                     + " FROM character_familiar282 WHERE character_id = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet r = ps.executeQuery()) {
                while (r.next()) {
                    var info = new Tms282FamiliarState.RegisteredInfo(r.getLong(1), r.getInt(2),
                            Integer.toUnsignedLong(r.getInt(3)), new String(r.getBytes(4), CP950), r.getInt(5),
                            r.getInt(6), r.getInt(7), r.getInt(8), r.getInt(9), r.getInt(10), r.getInt(11),
                            r.getInt(12), r.getInt(13), r.getInt(14));
                    records.put(info.serial(), info);
                }
            }
        }
        long summoned = 0;
        int essence = 0;
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("SELECT summoned_serial, essence FROM character_familiar282_user"
                     + " WHERE character_id = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet r = ps.executeQuery()) {
                if (r.next()) {
                    summoned = r.getLong(1);
                    essence = r.getInt(2);
                }
            }
        }
        if (summoned != 0 && !records.containsKey(summoned)) {
            summoned = 0; // the familiar is gone: nothing summoned
        }
        return new Tms282FamiliarState(characterId, 0, records, summoned, essence);
    }

    /** Link slots 1..3 + VIP (character_familiar282_user.link1..link_vip, 0 = empty). */
    public long[] loadLinks(int characterId) throws SQLException {
        long[] links = new long[4];
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("SELECT link1, link2, link3, link_vip FROM character_familiar282_user"
                     + " WHERE character_id = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet r = ps.executeQuery()) {
                if (r.next()) {
                    for (int i = 0; i < 4; i++) {
                        links[i] = r.getLong(i + 1);
                    }
                }
            }
        }
        return links;
    }

    public void saveLinks(int characterId, long[] links) throws SQLException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO character_familiar282_user (character_id, link1, link2,"
                     + " link3, link_vip) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE link1 = VALUES(link1),"
                     + " link2 = VALUES(link2), link3 = VALUES(link3), link_vip = VALUES(link_vip)")) {
            ps.setInt(1, characterId);
            for (int i = 0; i < 4; i++) {
                ps.setLong(i + 2, links[i]);
            }
            ps.executeUpdate();
        }
    }

    public void saveEssence(int characterId, int essence) throws SQLException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO character_familiar282_user (character_id, essence)"
                     + " VALUES (?, ?) ON DUPLICATE KEY UPDATE essence = VALUES(essence)")) {
            ps.setInt(1, characterId);
            ps.setInt(2, essence);
            ps.executeUpdate();
        }
    }

    /** The summoned familiar's serial (0 = none). */
    public void saveSummoned(int characterId, long serial) throws SQLException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO character_familiar282_user (character_id,"
                     + " summoned_serial) VALUES (?, ?) ON DUPLICATE KEY UPDATE summoned_serial = VALUES(summoned_serial)")) {
            ps.setInt(1, characterId);
            ps.setLong(2, serial);
            ps.executeUpdate();
        }
    }

    /** Inserts a new familiar (its serial is ignored) and returns the database serial. */
    public long insert(int characterId, int cardItemId, Tms282FamiliarState.RegisteredInfo info) throws SQLException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO character_familiar282 (character_id, state,"
                     + " familiar_id, name, level, skill_id, exp, option_level, option1, option2, option3, attributes,"
                     + " grade, look_id, card_item_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, characterId);
            bind(ps, 2, info);
            ps.setInt(15, cardItemId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next() || keys.getLong(1) <= 0) {
                    throw new SQLException("no familiar serial generated");
                }
                return keys.getLong(1);
            }
        }
    }

    /** Writes the editable fields of an owned familiar; false when the row is not the character's. */
    public boolean update(int characterId, Tms282FamiliarState.RegisteredInfo info) throws SQLException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("UPDATE character_familiar282 SET state = ?, familiar_id = ?,"
                     + " name = ?, level = ?, skill_id = ?, exp = ?, option_level = ?, option1 = ?, option2 = ?,"
                     + " option3 = ?, attributes = ?, grade = ?, look_id = ? WHERE serial = ? AND character_id = ?")) {
            bind(ps, 1, info);
            ps.setLong(14, info.serial());
            ps.setInt(15, characterId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Removes a row written by a registration that could not take the card. */
    public void delete(int characterId, long serial) throws SQLException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM character_familiar282 WHERE serial = ? AND character_id = ?")) {
            ps.setLong(1, serial);
            ps.setInt(2, characterId);
            ps.executeUpdate();
        }
    }

    /** Disassembly only: consume the validated records and credit essence as one InnoDB transaction. */
    public boolean disassemble(Tms282FamiliarState before, List<Long> serials, int essence) throws SQLException {
        try (Connection c = connection()) {
            // IF NOT EXISTS in the migration cannot change an older table's engine. Check before any mutation.
            try (PreparedStatement ps = c.prepareStatement("SELECT TABLE_NAME, ENGINE FROM information_schema.TABLES"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('character_familiar282', 'character_familiar282_user')");
                 ResultSet r = ps.executeQuery()) {
                int tables = 0;
                while (r.next()) {
                    if (!"InnoDB".equalsIgnoreCase(r.getString(2))) throw new SQLException("Familiar disassembly requires InnoDB tables");
                    tables++;
                }
                if (tables != 2) throw new SQLException("Familiar disassembly tables are missing");
            }
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO character_familiar282_user (character_id, essence) VALUES (?, 0)")) {
                    ps.setInt(1, before.characterId()); ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT essence, summoned_serial, link1, link2, link3, link_vip"
                        + " FROM character_familiar282_user WHERE character_id = ? FOR UPDATE")) {
                    ps.setInt(1, before.characterId());
                    try (ResultSet r = ps.executeQuery()) {
                        if (!r.next() || r.getInt(1) != before.essence()) { c.rollback(); return false; }
                        for (int i = 2; i <= 6; i++) {
                            if (serials.contains(r.getLong(i))) { c.rollback(); return false; }
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM character_familiar282"
                        + " WHERE serial = ? AND character_id = ? AND grade = ? AND attributes = ?")) {
                    for (long serial : serials) {
                        var record = before.registered().get(serial);
                        if (record == null) { c.rollback(); return false; }
                        ps.setLong(1, serial); ps.setInt(2, before.characterId());
                        ps.setInt(3, record.grade()); ps.setInt(4, record.attributes());
                        if (ps.executeUpdate() != 1) { c.rollback(); return false; }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE character_familiar282_user SET essence = ? WHERE character_id = ? AND essence = ?")) {
                    ps.setInt(1, essence); ps.setInt(2, before.characterId()); ps.setInt(3, before.essence());
                    if (ps.executeUpdate() != 1) { c.rollback(); return false; }
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                throw e;
            }
        }
    }

    private static void bind(PreparedStatement ps, int at, Tms282FamiliarState.RegisteredInfo info) throws SQLException {
        ps.setInt(at, info.state());
        ps.setInt(at + 1, (int) info.familiarId());
        ps.setBytes(at + 2, info.nameBytes());
        ps.setInt(at + 3, info.level());
        ps.setInt(at + 4, info.skillId());
        ps.setInt(at + 5, info.exp());
        ps.setInt(at + 6, info.optionLevel());
        ps.setInt(at + 7, info.option1());
        ps.setInt(at + 8, info.option2());
        ps.setInt(at + 9, info.option3());
        ps.setInt(at + 10, info.attributes());
        ps.setInt(at + 11, info.grade());
        ps.setInt(at + 12, info.lookId());
    }
}
