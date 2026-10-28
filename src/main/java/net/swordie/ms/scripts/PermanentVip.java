package net.swordie.ms.scripts;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import java.math.BigDecimal;
import java.sql.*;

/** The script pack's permanent VIP ledger; independent of the timed User.vipGrade MVP tier. */
public final class PermanentVip {
    public static final int PRICE = 3688, LEVEL = 2;
    private PermanentVip() { }

    public static int level(int accountId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement p = c.prepareStatement("SELECT level FROM cash_vip WHERE account_id=?")) {
            p.setInt(1, accountId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? Math.max(0, r.getInt(1)) : 0; }
        }
    }

    public static BigDecimal balance(String name) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) { return balance(c, name); }
    }

    private static BigDecimal balance(Connection c, String name) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT COALESCE(SUM(rmb),0) FROM zz_rmb_vip WHERE account=?")) {
            p.setString(1, name);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getBigDecimal(1); }
        }
    }

    /** 0 activated, 1 already active, 2 insufficient coins. Debit and membership commit together. */
    public static int activate(int userId, int accountId, String name) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) { return activate(c, userId, accountId, name); }
    }

    static int activate(Connection c, int userId, int accountId, String name) throws SQLException {
        boolean auto = c.getAutoCommit();
        if (!auto) throw new SQLException("VIP activation requires an owned connection");
        c.setAutoCommit(false);
        try {
            lockOwner(c, userId, accountId, name);
            try (PreparedStatement p = c.prepareStatement("SELECT level FROM cash_vip WHERE account_id=? FOR UPDATE")) {
                p.setInt(1, accountId);
                try (ResultSet r = p.executeQuery()) {
                    if (r.next() && r.getInt(1) >= LEVEL) { c.rollback(); return 1; }
                }
            }
            if (balance(c, name).compareTo(BigDecimal.valueOf(PRICE)) < 0) { c.rollback(); return 2; }
            append(c, name, -PRICE, "開通永久VIP");
            try (PreparedStatement p = c.prepareStatement("INSERT INTO cash_vip(account_id,level,start_date) VALUES(?,?,NOW()) ON DUPLICATE KEY UPDATE level=VALUES(level),start_date=VALUES(start_date)")) {
                p.setInt(1, accountId); p.setInt(2, LEVEL); p.executeUpdate();
            }
            c.commit(); return 0;
        } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        finally { c.setAutoCommit(auto); }
    }

    // All accounts of a user share the named coin ledger, so serialize on users, not the world account.
    private static void lockOwner(Connection c, int userId, int accountId, String name) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT u.id FROM users u JOIN accounts a ON a.userid=u.id WHERE u.id=? AND u.name=? AND a.id=? FOR UPDATE")) {
            p.setInt(1, userId); p.setString(2, name); p.setInt(3, accountId);
            try (ResultSet r = p.executeQuery()) { if (!r.next()) throw new SQLException("VIP account ownership mismatch"); }
        }
    }

    public static void credit(int userId, int accountId, String name, int amount) throws SQLException {
        if (amount < 1 || amount > 999999999) throw new IllegalArgumentException("VIP coin amount out of range");
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try { lockOwner(c, userId, accountId, name); append(c, name, amount, "充值VIP幣"); c.commit(); }
            catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    private static void append(Connection c, String name, int amount, String remark) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO zz_rmb_vip(account,rmb,paytime,remark) VALUES(?,?,NOW(),?)")) {
            p.setString(1, name); p.setInt(2, amount); p.setString(3, remark);
            if (p.executeUpdate() != 1) throw new SQLException("VIP ledger insert failed");
        }
    }
}
