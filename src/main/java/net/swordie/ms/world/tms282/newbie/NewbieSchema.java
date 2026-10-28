package net.swordie.ms.world.tms282.newbie;

/**
 * The newbie flow tables (InnoDB, utf8mb4), created by the server with CREATE TABLE IF NOT EXISTS - the only DDL the
 * server's database account may run: indexes are part of the CREATE TABLE, no ALTER, no foreign keys. The same
 * statements open deploy/step7/sql/20261004-newbie-flow.sql (tools/Tms282NewbieFlowCheck compares them).
 * <ul>
 *     <li>newbie_flow: one row per character created after the deploy (or added by the deploy SQL for the
 *     characters recovered from an intro map at login): the next step, the chosen branch, when the gift / the first job / the flow
 *     were done.</li>
 *     <li>newbie_gift_log: one row per claimed starter gift, UNIQUE(character_id): what was given, by which config
 *     version.</li>
 * </ul>
 */
public final class NewbieSchema {
    static final String[] DDL = {
            "CREATE TABLE IF NOT EXISTS newbie_point_log ("
                    + "user_id INT NOT NULL,character_id INT NOT NULL,amount INT NOT NULL,granted_at DATETIME(3) NOT NULL,"
                    + "PRIMARY KEY(user_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS newbie_flow ("
                    + "character_id INT NOT NULL,"
                    + "account_id INT NOT NULL,"
                    + "user_id INT NOT NULL,"
                    + "login_job VARCHAR(32) NOT NULL,"
                    + "step_id VARCHAR(32) NOT NULL DEFAULT '',"
                    + "branch VARCHAR(32) NOT NULL DEFAULT '',"
                    + "gift_claimed_at DATETIME(3) NULL,"
                    + "job_done_at DATETIME(3) NULL,"
                    + "completed_at DATETIME(3) NULL,"
                    + "created_at DATETIME(3) NOT NULL,"
                    + "updated_at DATETIME(3) NOT NULL,"
                    + "PRIMARY KEY (character_id), KEY idx_newbie_flow_user (user_id), KEY idx_newbie_flow_updated (updated_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS newbie_gift_log ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT,"
                    + "character_id INT NOT NULL,"
                    + "account_id INT NOT NULL,"
                    + "user_id INT NOT NULL,"
                    + "config_version VARCHAR(40) NOT NULL,"
                    + "branch VARCHAR(32) NOT NULL DEFAULT '',"
                    + "items VARCHAR(2000) NOT NULL,"
                    + "claimed_at DATETIME(3) NOT NULL,"
                    + "PRIMARY KEY (id), UNIQUE KEY uq_newbie_gift_char (character_id), KEY idx_newbie_gift_user (user_id, claimed_at)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
    };

    private NewbieSchema() {
    }

    /** The statements of the sql file (tools compare them). */
    public static String[] ddl() {
        return DDL.clone();
    }
}
