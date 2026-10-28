package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Tables of offline notes (memo) and Duey parcels. Created at runtime with CREATE TABLE IF NOT EXISTS (the same
 * statements are in sql/tms282/20260928-mail.sql). Columns follow the TMS 279 reference schema (memo, memo_record,
 * parcel) with a tms282_ prefix; parcel adds quantity / available_at / receiver_name / original_sender_id.
 */
public final class MailSchema {
    private MailSchema() { }

    static final String[] DDL = {
            "CREATE TABLE IF NOT EXISTS tms282_memo ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "flag INT NOT NULL DEFAULT 0,"
                    + "state INT NOT NULL DEFAULT 0,"
                    + "sender_character_id INT NOT NULL,"
                    + "sender_account_id INT NOT NULL,"
                    + "sender_name VARCHAR(64) NOT NULL,"
                    + "sent_time DATETIME(3) NOT NULL,"
                    + "receiver_id INT NOT NULL,"
                    + "receiver_name VARCHAR(64) NOT NULL,"
                    + "account_memo TINYINT(1) NOT NULL DEFAULT 0,"
                    + "content VARCHAR(1024) NOT NULL,"
                    + "PRIMARY KEY (id), KEY idx_tms282_memo_receiver (receiver_id, state)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS tms282_memo_record ("
                    + "memo_id INT NOT NULL,"
                    + "state INT NOT NULL,"
                    + "flag INT NOT NULL DEFAULT 0,"
                    + "sender_id INT NOT NULL,"
                    + "sender_name VARCHAR(64) NOT NULL,"
                    + "receiver_id INT NOT NULL,"
                    + "receiver_name VARCHAR(64) NOT NULL,"
                    + "content VARCHAR(1024) NOT NULL,"
                    + "sent_time DATETIME(3) NOT NULL,"
                    + "KEY idx_tms282_memo_record_sender (sender_id), KEY idx_tms282_memo_record_memo (memo_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS tms282_parcel ("
                    + "id INT NOT NULL AUTO_INCREMENT,"
                    + "created_at DATETIME(3) NOT NULL,"
                    + "available_at DATETIME(3) NOT NULL,"
                    + "receiver_id INT NOT NULL,"
                    + "receiver_name VARCHAR(64) NOT NULL,"
                    + "sender VARCHAR(64) NOT NULL,"
                    + "sender_id INT NOT NULL,"
                    + "original_sender_id INT NOT NULL,"
                    + "money BIGINT NOT NULL DEFAULT 0,"
                    + "expire DATETIME(3) NOT NULL,"
                    + "delivery_type INT NOT NULL DEFAULT 0,"
                    + "memo VARCHAR(1024) NULL,"
                    + "sn BIGINT NULL,"
                    + "item_id INT NOT NULL DEFAULT 0,"
                    + "quantity INT NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (id), KEY idx_tms282_parcel_receiver (receiver_id), KEY idx_tms282_parcel_expire (expire)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
    };

    private static volatile boolean ready;

    static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            for (String ddl : DDL) {
                s.executeUpdate(ddl);
            }
        }
        ready = true;
    }

    static Connection connection() throws SQLException {
        ensure();
        return DatabaseManager.getConnection();
    }
}
