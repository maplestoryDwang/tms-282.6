package net.swordie.ms.connection.api.launcher.gift;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The P7 tables (InnoDB, utf8mb4), created by the server with CREATE TABLE IF NOT EXISTS - the only DDL the server's
 * database account may run (CREATE; no ALTER / INDEX / DROP): indexes are part of the CREATE TABLE. The same
 * statements are in deploy/step6/sql/20261004-gift-codes.sql. No foreign keys (they would need REFERENCES).
 * <ul>
 *     <li>gift_codes: one row per code; only the HMAC-SHA256 of the code (code_hash) and the key id.</li>
 *     <li>gift_code_items: what a code gives (at most 10 lines).</li>
 *     <li>gift_code_redemptions: one row per use, UNIQUE(code_id, user_id, seq) - seq counts the account's uses of
 *     the code (1..per_user_limit).</li>
 *     <li>launcher_reward_queue: items waiting for a character's next field entry (delivered_at stays NULL until
 *     LauncherRewardDelivery hands them over).</li>
 * </ul>
 */
public final class GiftSchema {
    static final String[] DDL = {
            "CREATE TABLE IF NOT EXISTS gift_codes ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "code_hash BINARY(32) NOT NULL,"
                    + "key_id CHAR(16) NOT NULL,"
                    + "title VARCHAR(64) NOT NULL,"
                    + "note VARCHAR(200) NOT NULL DEFAULT '',"
                    + "batch_id VARCHAR(32) NOT NULL DEFAULT '',"
                    + "max_uses INT NOT NULL,"
                    + "used_count INT NOT NULL DEFAULT 0,"
                    + "per_user_limit INT NOT NULL DEFAULT 1,"
                    + "world_id INT NOT NULL DEFAULT -1,"
                    + "min_level INT NOT NULL DEFAULT 0,"
                    + "starts_at DATETIME(3) NULL,"
                    + "expires_at DATETIME(3) NULL,"
                    + "enabled TINYINT(1) NOT NULL DEFAULT 1,"
                    + "created_at DATETIME(3) NOT NULL,"
                    + "disabled_at DATETIME(3) NULL,"
                    + "PRIMARY KEY (id), UNIQUE KEY uq_gift_codes_hash (code_hash), KEY idx_gift_codes_created (created_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS gift_code_items ("
                    + "code_id INT NOT NULL,"
                    + "slot TINYINT NOT NULL,"
                    + "item_id INT NOT NULL,"
                    + "quantity INT NOT NULL,"
                    + "PRIMARY KEY (code_id, slot)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS gift_code_redemptions ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT,"
                    + "code_id INT NOT NULL,"
                    + "user_id INT NOT NULL,"
                    + "seq INT NOT NULL,"
                    + "character_id INT NOT NULL,"
                    + "world_id INT NOT NULL,"
                    + "source_ip VARCHAR(45) NOT NULL DEFAULT '',"
                    + "redeemed_at DATETIME(3) NOT NULL,"
                    + "PRIMARY KEY (id), UNIQUE KEY uq_gift_redemption (code_id, user_id, seq),"
                    + " KEY idx_gift_redemption_user (user_id, redeemed_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS launcher_reward_queue ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT,"
                    + "user_id INT NOT NULL,"
                    + "character_id INT NOT NULL,"
                    + "item_id INT NOT NULL,"
                    + "quantity INT NOT NULL,"
                    + "source VARCHAR(16) NOT NULL,"
                    + "source_id BIGINT NOT NULL,"
                    + "created_at DATETIME(3) NOT NULL,"
                    + "delivered_at DATETIME(3) NULL,"
                    + "PRIMARY KEY (id), KEY idx_reward_queue_char (character_id, delivered_at), KEY idx_reward_queue_source (source, source_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
    };

    private static volatile boolean ready;

    private GiftSchema() {
    }

    public static void ensure() throws SQLException {
        if (ready) return;
        synchronized (GiftSchema.class) {
            if (ready) return;
            try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
                for (String ddl : DDL) {
                    s.executeUpdate(ddl);
                }
            }
            ready = true;
        }
    }

    /** The statements of the sql file (tools compare them). */
    public static String[] ddl() {
        return DDL.clone();
    }
}
