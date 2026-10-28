package net.swordie.ms.world.shop;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class HypayManager {

    public static int getPayBalance(String accountName) {
        if (accountName == null || accountName.isEmpty()) {
            return 0;
        }
        ensureAccount(accountName, accountName);
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT pay FROM hypay WHERE accname = ? LIMIT 1")) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal pay = rs.getBigDecimal("pay");
                    return pay == null ? 0 : pay.intValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean deductPay(String accountName, int amount, String playerName, int playerId, int accountId,
                                    String reason, String reasonDetail) {
        if (amount <= 0) {
            return true;
        }
        ensureAccount(accountName, playerName);
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE hypay SET pay = pay - ?, payUsed = payUsed + ? WHERE accname = ? AND pay >= ?")) {
                ps.setBigDecimal(1, BigDecimal.valueOf(amount));
                ps.setBigDecimal(2, BigDecimal.valueOf(amount));
                ps.setString(3, accountName);
                ps.setBigDecimal(4, BigDecimal.valueOf(amount));
                if (ps.executeUpdate() <= 0) {
                    con.rollback();
                    return false;
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO zsf_pay_detail (player_name, player_id, account_id, pay_type, amount, reason, reason_detail) "
                            + "VALUES (?, ?, ?, 1, ?, ?, ?)")) {
                ps.setString(1, playerName);
                ps.setInt(2, playerId);
                ps.setInt(3, accountId);
                ps.setLong(4, -amount);
                ps.setString(5, reason);
                ps.setString(6, reasonDetail);
                ps.executeUpdate();
            }
            con.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addPay(String accountName, int amount, String playerName, int playerId, int accountId,
                                 String reason, String reasonDetail) {
        if (amount <= 0) {
            return false;
        }
        ensureAccount(accountName, playerName);
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE hypay SET pay = pay + ? WHERE accname = ?")) {
                ps.setBigDecimal(1, BigDecimal.valueOf(amount));
                ps.setString(2, accountName);
                if (ps.executeUpdate() <= 0) {
                    con.rollback();
                    return false;
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO zsf_pay_detail (player_name, player_id, account_id, pay_type, amount, reason, reason_detail) "
                            + "VALUES (?, ?, ?, 1, ?, ?, ?)")) {
                ps.setString(1, playerName);
                ps.setInt(2, playerId);
                ps.setInt(3, accountId);
                ps.setLong(4, amount);
                ps.setString(5, reason);
                ps.setString(6, reasonDetail);
                ps.executeUpdate();
            }
            con.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getLeijiBalance(String accountName) {
        if (accountName == null || accountName.isEmpty()) {
            return 0;
        }
        ensureAccount(accountName, accountName);
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT leiji FROM hypay WHERE accname = ? LIMIT 1")) {
            ps.setString(1, accountName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal leiji = rs.getBigDecimal("leiji");
                    return leiji == null ? 0 : leiji.intValue();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static void ensureAccount(String accountName, String displayName) {
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     // hypay has no unique key on accname: INSERT IGNORE added one more row on every balance read.
                     "INSERT INTO hypay (accname, name, pay, payUsed, payReward, leiji) SELECT ?, ?, 0, 0, 0, 0 FROM DUAL "
                             + "WHERE NOT EXISTS (SELECT 1 FROM hypay WHERE accname = ?)")) {
            ps.setString(1, accountName);
            ps.setString(2, displayName);
            ps.setString(3, accountName);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
