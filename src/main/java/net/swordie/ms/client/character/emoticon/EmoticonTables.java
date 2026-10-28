package net.swordie.ms.client.character.emoticon;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Runtime bootstrap of the chat emoticon tables. Additive and idempotent (CREATE TABLE IF NOT EXISTS only), run
 * once per process before the first query. A copy of the DDL lives in sql/tms282/20260928-emoticon.sql.
 * <p>
 * Columns follow the TMS 279 reference schema (E:\TMS\TMS279\db\schema.sql: character_emoticon
 * {character_id, emoticon_id, expire_at, favorite_pos, group_id} and character_emoticon_command
 * {character_id, index, emoticon_id, command}) with a {@code tms282_} prefix, NOT NULL columns and primary keys.
 * <ul>
 *     <li>{@code expire_at} NULL = permanent;</li>
 *     <li>{@code favorite_pos} -1 = not on the favorite bar, otherwise 0-based bar slot.</li>
 * </ul>
 */
public final class EmoticonTables {
    public static final String EMOTICON = "tms282_character_emoticon";
    public static final String COMMAND = "tms282_character_emoticon_command";

    static final List<String> DDL = List.of(
            "CREATE TABLE IF NOT EXISTS " + EMOTICON + " ("
                    + "character_id INT NOT NULL,"
                    + "emoticon_id INT NOT NULL,"
                    + "group_id INT NOT NULL DEFAULT 0,"
                    + "expire_at DATETIME(3) NULL,"
                    + "favorite_pos INT NOT NULL DEFAULT -1,"
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                    + "PRIMARY KEY (character_id, emoticon_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS " + COMMAND + " ("
                    + "character_id INT NOT NULL,"
                    + "`index` INT NOT NULL,"
                    + "emoticon_id INT NOT NULL,"
                    + "command VARCHAR(1024) NOT NULL DEFAULT '',"
                    + "PRIMARY KEY (character_id, `index`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    private static volatile boolean ready;

    private EmoticonTables() {
    }

    /** Creates the tables once per process. */
    public static void ensure() throws SQLException {
        if (ready) {
            return;
        }
        synchronized (EmoticonTables.class) {
            if (ready) {
                return;
            }
            try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
                for (String ddl : DDL) {
                    s.executeUpdate(ddl);
                }
            }
            ready = true;
        }
    }

    static Connection connection() throws SQLException {
        ensure();
        return DatabaseManager.getConnection();
    }

    static void log(String what, Exception e) {
        System.out.printf("[TMS282][emoticon] %s failed: %s%n", what, e);
    }
}
