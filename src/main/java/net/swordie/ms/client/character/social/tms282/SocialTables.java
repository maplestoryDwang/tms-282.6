package net.swordie.ms.client.character.social.tms282;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Runtime bootstrap of the social persistence tables (rings, block list, fame history). Additive and idempotent:
 * CREATE TABLE IF NOT EXISTS only, run once per process before the first query (same pattern as
 * {@code CashShopPurchaseCounts}). A copy of the DDL lives in sql/tms282/20260928-social.sql.
 * <p>
 * Tables follow the TMS 279 reference schema (E:\TMS\TMS279\db\schema.sql: marriage_record, couple_record,
 * friend_record, blacklist_user, blacklist_guild, pop_record) with a {@code tms282_} prefix, keys and indexes added.
 */
public final class SocialTables {
    public static final String MARRIAGE = "tms282_marriage_record";
    public static final String COUPLE = "tms282_couple_record";
    public static final String FRIEND = "tms282_friend_record";
    public static final String BLACKLIST_USER = "tms282_blacklist_user";
    public static final String BLACKLIST_GUILD = "tms282_blacklist_guild";
    public static final String POP = "tms282_pop_record";

    static final List<String> DDL = List.of(
            "CREATE TABLE IF NOT EXISTS " + MARRIAGE + " ("
                    + "marriage_id INT NOT NULL AUTO_INCREMENT,"
                    + "groom_id INT NOT NULL,"
                    + "bride_id INT NOT NULL,"
                    + "groom_name VARCHAR(32) NOT NULL DEFAULT '',"
                    + "bride_name VARCHAR(32) NOT NULL DEFAULT '',"
                    + "stage INT NOT NULL DEFAULT 0,"
                    + "groom_ring_id INT NOT NULL DEFAULT 0,"
                    + "bride_ring_id INT NOT NULL DEFAULT 0,"
                    + "groom_ring_sn BIGINT NOT NULL DEFAULT 0,"
                    + "bride_ring_sn BIGINT NOT NULL DEFAULT 0,"
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                    + "PRIMARY KEY (marriage_id), KEY idx_groom (groom_id), KEY idx_bride (bride_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS " + COUPLE + " ("
                    + "character_id INT NOT NULL,"
                    + "pair_character_id INT NOT NULL,"
                    + "pair_name VARCHAR(32) NOT NULL DEFAULT '',"
                    + "ring_id INT NOT NULL,"
                    + "ring_sn BIGINT NOT NULL,"
                    + "pair_ring_sn BIGINT NOT NULL,"
                    + "text VARCHAR(1024) NULL,"
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                    + "PRIMARY KEY (character_id, ring_sn), KEY idx_pair (pair_character_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS " + FRIEND + " ("
                    + "character_id INT NOT NULL,"
                    + "pair_character_id INT NOT NULL,"
                    + "pair_name VARCHAR(32) NOT NULL DEFAULT '',"
                    + "ring_id INT NOT NULL,"
                    + "ring_sn BIGINT NOT NULL,"
                    + "pair_ring_sn BIGINT NOT NULL,"
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                    + "PRIMARY KEY (character_id, ring_sn), KEY idx_pair (pair_character_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS " + BLACKLIST_USER + " ("
                    + "account_id INT NOT NULL,"
                    + "block_character_id INT NOT NULL,"
                    + "name VARCHAR(32) NOT NULL DEFAULT '',"
                    + "nick_name VARCHAR(64) NULL,"
                    + "block_account_id INT NOT NULL DEFAULT 0,"
                    + "memo VARCHAR(1024) NULL,"
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                    + "PRIMARY KEY (account_id, block_character_id), KEY idx_block_acc (block_account_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS " + BLACKLIST_GUILD + " ("
                    + "character_id INT NOT NULL,"
                    + "guild_id INT NOT NULL,"
                    + "name VARCHAR(64) NOT NULL DEFAULT '',"
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                    + "PRIMARY KEY (character_id, guild_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS " + POP + " ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),"
                    + "character_id INT NOT NULL,"
                    + "target_id INT NOT NULL,"
                    + "increase TINYINT NOT NULL DEFAULT 1,"
                    + "PRIMARY KEY (id), KEY idx_char_time (character_id, created_at), KEY idx_char_target (character_id, target_id, created_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    private static volatile boolean ready;

    private SocialTables() {
    }

    /** Creates the tables once per process. */
    public static void ensure() throws SQLException {
        if (ready) {
            return;
        }
        synchronized (SocialTables.class) {
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
        System.out.printf("[TMS282][social] %s failed: %s%n", what, e);
    }
}
