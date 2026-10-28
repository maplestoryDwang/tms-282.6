package net.swordie.ms.world.tms282.audit;

import java.util.List;
import java.util.Map;

/**
 * DDL and insert column lists for the audit / ban tables. Column names follow the TMS 279 reference schema
 * (E:\TMS\TMS279\db\schema.sql) exactly so a DB built from either source works. A copy of the DDL lives in
 * sql/tms282/20260928-audit.sql; tables are also created at runtime with CREATE TABLE IF NOT EXISTS.
 */
public final class AuditSchema {

    public static final String LOG_MONEY = "log_money";
    public static final String ZLOG_INVENTORY = "zlog_inventory";
    public static final String ZLOG_COMMAND = "zlog_command";
    public static final String ZLOG_ANTIMACRO = "zlog_antimacro";
    public static final String BLOCK_IP = "block_ip";
    public static final String ACCOUNT_BLOCK = "account_block";

    /** Insert column order for the batched (append-only) log tables. Row values must follow this order. */
    static final Map<String, List<String>> COLUMNS = Map.of(
            LOG_MONEY, List.of("created_at", "account_id", "character_id", "before", "after", "diff", "map_id", "type", "detail"),
            ZLOG_INVENTORY, List.of("created_at", "inventory_id", "parent_id", "owner_id", "item_id", "number", "log_type", "operation", "log"),
            ZLOG_COMMAND, List.of("created_at", "characters_id", "command"),
            ZLOG_ANTIMACRO, List.of("created_at", "account_id", "character_id", "source_id", "map_id", "attempt", "text", "answer", "result", "fail_reason")
    );

    static final List<String> DDL = List.of(
            "CREATE TABLE IF NOT EXISTS `log_money` (\n" +
                    "  `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "  `created_at` DATETIME(3) NULL,\n" +
                    "  `account_id` INT NULL,\n" +
                    "  `character_id` INT NULL,\n" +
                    "  `before` BIGINT NULL,\n" +
                    "  `after` BIGINT NULL,\n" +
                    "  `diff` BIGINT NULL,\n" +
                    "  `map_id` INT NULL,\n" +
                    "  `type` VARCHAR(1024) NULL,\n" +
                    "  `detail` VARCHAR(1024) NULL,\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  KEY `idx_log_money_char` (`character_id`, `created_at`),\n" +
                    "  KEY `idx_log_money_acc` (`account_id`, `created_at`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `zlog_inventory` (\n" +
                    "  `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n" +
                    "  `inventory_id` BIGINT NULL,\n" +
                    "  `parent_id` BIGINT NULL,\n" +
                    "  `owner_id` INT NULL,\n" +
                    "  `item_id` INT NULL,\n" +
                    "  `number` INT NULL,\n" +
                    "  `log_type` INT NULL,\n" +
                    "  `operation` VARCHAR(255) NULL,\n" +
                    "  `log` LONGTEXT NULL,\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  KEY `idx_zlog_inventory_owner` (`owner_id`, `created_at`),\n" +
                    "  KEY `idx_zlog_inventory_item` (`item_id`, `created_at`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `zlog_command` (\n" +
                    "  `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n" +
                    "  `characters_id` INT NULL,\n" +
                    "  `command` LONGTEXT NULL,\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  KEY `idx_zlog_command_char` (`characters_id`, `created_at`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `zlog_antimacro` (\n" +
                    "  `id` BIGINT NOT NULL AUTO_INCREMENT,\n" +
                    "  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n" +
                    "  `account_id` INT NULL,\n" +
                    "  `character_id` INT NULL,\n" +
                    "  `source_id` INT NULL,\n" +
                    "  `map_id` INT NULL,\n" +
                    "  `attempt` INT NULL,\n" +
                    "  `text` LONGTEXT NULL,\n" +
                    "  `answer` LONGTEXT NULL,\n" +
                    "  `result` TINYINT(1) NULL,\n" +
                    "  `fail_reason` LONGTEXT NULL,\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  KEY `idx_zlog_antimacro_char` (`character_id`, `created_at`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `block_ip` (\n" +
                    "  `id` INT NOT NULL AUTO_INCREMENT,\n" +
                    "  `created_at` DATETIME(3) NULL,\n" +
                    "  `ip` VARCHAR(1024) NULL,\n" +
                    "  `reason` VARCHAR(1024) NULL,\n" +
                    "  PRIMARY KEY (`id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
            "CREATE TABLE IF NOT EXISTS `account_block` (\n" +
                    "  `id` INT NOT NULL AUTO_INCREMENT,\n" +
                    "  `created_at` DATETIME(3) NULL,\n" +
                    "  `account_id` INT NULL,\n" +
                    "  `type` INT NULL,\n" +
                    "  `end_time` DATETIME(3) NULL,\n" +
                    "  `reason` VARCHAR(1024) NULL,\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  KEY `idx_account_block_acc` (`account_id`, `end_time`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
    );

    private AuditSchema() {
    }

    static String insertSql(String table) {
        List<String> cols = COLUMNS.get(table);
        StringBuilder sb = new StringBuilder("INSERT INTO `").append(table).append("` (");
        for (int i = 0; i < cols.size(); i++) {
            sb.append(i == 0 ? "`" : ", `").append(cols.get(i)).append('`');
        }
        sb.append(") VALUES (");
        for (int i = 0; i < cols.size(); i++) {
            sb.append(i == 0 ? "?" : ", ?");
        }
        return sb.append(')').toString();
    }
}
