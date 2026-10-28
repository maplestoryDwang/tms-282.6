package net.swordie.ms.console.datamanage;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 控制檯數據管理所需的數據庫連接與表結構自檢。
 */
public final class ConsoleDatabaseService {
    private static final String[] REQUIRED_TABLES = {
            "characters", "characterstats", "accounts", "users",
            "mob_drops", "equip_drops", "shopitems", "cs_items", "cs_categories"
    };

    private static final String[] SCHEMA_PATCHES = {
            """
            CREATE TABLE IF NOT EXISTS `mob_drops` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT,
              `mobid` int(11) DEFAULT NULL,
              `itemid` int(11) DEFAULT NULL,
              `chance` int(11) DEFAULT NULL,
              `minquant` int(11) DEFAULT 1,
              `maxquant` int(11) DEFAULT 1,
              `reactordrop` bit(1) DEFAULT b'0',
              PRIMARY KEY (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS `equip_drops` (
              `id` int(11) NOT NULL,
              `job` varchar(255) NOT NULL,
              `level` int(11) NOT NULL,
              PRIMARY KEY (`id`, `job`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS `shopitems` (
              `id` bigint(20) NOT NULL AUTO_INCREMENT,
              `shopid` int(11) DEFAULT NULL,
              `itemid` int(11) DEFAULT NULL,
              `price` bigint(20) DEFAULT 0,
              `tokenitemid` int(11) DEFAULT 0,
              `tokenprice` int(11) DEFAULT 0,
              `pointquestid` int(11) DEFAULT 0,
              `pointprice` int(11) DEFAULT 0,
              `starcoin` int(11) DEFAULT 0,
              `questexid` int(11) DEFAULT 0,
              `questexkey` varchar(255) DEFAULT NULL,
              `questexvalue` int(11) DEFAULT 0,
              `itemperiod` int(11) DEFAULT 0,
              `levellimited` int(11) DEFAULT 0,
              `showlevmin` int(11) DEFAULT 0,
              `showlevmax` int(11) DEFAULT 0,
              `questid` int(11) DEFAULT 0,
              `sellstart` datetime(3) DEFAULT '1970-01-01 00:00:01.000',
              `sellend` datetime(3) DEFAULT '9999-01-01 00:00:01.000',
              `tabindex` int(11) DEFAULT NULL,
              `worldblock` tinyint(1) DEFAULT NULL,
              `potentialgrade` int(11) DEFAULT NULL,
              `buylimit` int(11) DEFAULT NULL,
              `quantity` smallint(6) DEFAULT NULL,
              `unitprice` bigint(20) DEFAULT 0,
              `maxperslot` smallint(6) DEFAULT 9999,
              `discountperc` int(11) DEFAULT 0,
              PRIMARY KEY (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS `cs_categories` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `idx` int(11) DEFAULT NULL,
              `name` varchar(255) DEFAULT NULL,
              `flag` int(11) DEFAULT NULL,
              `parentIdx` int(11) DEFAULT 0,
              `stock` int(11) DEFAULT 100,
              PRIMARY KEY (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS `cs_items` (
              `id` int(11) NOT NULL AUTO_INCREMENT,
              `itemID` int(11) NOT NULL,
              `stock` int(11) DEFAULT 1,
              `shopItemFlag` int(11) DEFAULT 0,
              `idk1` int(11) DEFAULT 0,
              `idk2` int(11) DEFAULT 0,
              `oldPrice` int(11) DEFAULT 0,
              `newPrice` int(11) DEFAULT 0,
              `idkTime1` datetime(3) DEFAULT NULL,
              `saleFromFT` datetime(3) DEFAULT NULL,
              `idkTime3` datetime(3) DEFAULT NULL,
              `saleToFT` datetime(3) DEFAULT NULL,
              `idk3` int(11) DEFAULT 0,
              `bundleQuantity` int(11) DEFAULT 0,
              `availableDays` int(11) DEFAULT 0,
              `buyableWithMaplePoints` smallint(6) DEFAULT 1,
              `buyableWithCredit` smallint(6) DEFAULT 1,
              `buyableWithPrepaid` smallint(6) DEFAULT 1,
              `likable` smallint(6) DEFAULT 1,
              `meso` smallint(6) DEFAULT 0,
              `favoritable` smallint(6) DEFAULT 1,
              `gender` int(11) DEFAULT 2,
              `likes` int(11) DEFAULT 0,
              `requiredLevel` int(11) DEFAULT 0,
              `idk10` varchar(255) DEFAULT NULL,
              `idk11` int(11) DEFAULT 0,
              `idk13` int(11) DEFAULT 0,
              `idk14` int(11) DEFAULT 0,
              `category` varchar(255) DEFAULT NULL,
              PRIMARY KEY (`id`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
    };

    private ConsoleDatabaseService() {
    }

    public static void prepare() throws SQLException {
        DatabaseManager.ensureInitialized();
        ensureSchema();
    }

    public static List<String> missingRequiredTables() throws SQLException {
        List<String> missing = new ArrayList<>();
        try (Connection con = DatabaseManager.getConnection();
             ResultSet rs = con.getMetaData().getTables(con.getCatalog(), null, "%", new String[]{"TABLE"})) {
            java.util.Set<String> existing = new java.util.HashSet<>();
            while (rs.next()) {
                existing.add(rs.getString("TABLE_NAME").toLowerCase());
            }
            for (String table : REQUIRED_TABLES) {
                if (!existing.contains(table.toLowerCase())) {
                    missing.add(table);
                }
            }
        }
        return missing;
    }

    private static void ensureSchema() throws SQLException {
        try (Connection con = DatabaseManager.getConnection(); Statement st = con.createStatement()) {
            for (String ddl : SCHEMA_PATCHES) {
                st.executeUpdate(ddl.trim());
            }
            ensureCharactersFieldId(con);
        }
    }

    private static void ensureCharactersFieldId(Connection con) throws SQLException {
        if (columnExists(con, "characters", "fieldid")) {
            return;
        }
        try (Statement st = con.createStatement()) {
            st.executeUpdate("ALTER TABLE `characters` ADD COLUMN `fieldid` int(11) DEFAULT NULL");
        }
    }

    private static boolean columnExists(Connection con, String table, String column) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public static String formatSqlError(Exception e) {
        if (e == null) {
            return "未知錯誤";
        }
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        if (msg.contains("because \"con\" is null") || msg.contains("連接池未初始化")) {
            return "數據庫未連接，請確認 MySQL 已啓動且賬號密碼正確（默認 root/root，庫名 265）";
        }
        if (msg.contains("Unknown database")) {
            return "數據庫 265 不存在，請先導入 265.sql";
        }
        if (msg.contains("doesn't exist")) {
            return msg + "（已嘗試自動創建，若仍失敗請運行 python tools/db_supplement.py）";
        }
        return msg;
    }
}
