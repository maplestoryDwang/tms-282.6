package net.swordie.ms.world.tms282.activity;

import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.tms282.EconomyLocks;
import net.swordie.ms.client.character.tms282.Tms282CashExpansionService;
import net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.world.tms282.activity.PointRaffle.Result;
import net.swordie.ms.world.tms282.activity.PointRaffle.Status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 付費「服務」（leftovers-1005 任務 3）：時裝鐵砧、武器／防具轉換、洗練這類扣點後改動既有裝備、沒有新道具可交給
 * {@link PointOrder} 的功能。與 PointOrder 同一個錢包 registry（{@link PointWallets}）、同一張收據表
 * （account_questex {@link PointRaffle#RECEIPT_QUEST}）與同一組鎖（EconomyLocks、帳號、users 列 FOR UPDATE）。
 * <p>
 * 流程：腳本先檢查好所有條件 → {@link #charge}（一筆交易：鎖 users 列、鎖錢包、檢查餘額、扣款、寫收據
 * {@code ...;service=<說明>}，提交後同步記憶體）→ 改裝備 → 改裝備失敗（例外或回傳 false）就 {@link #refund}
 * （一筆交易：鎖收據列，還沒退過才把費用加回原錢包，收據加上 {@code ;refunded=<時間>}；重複呼叫不會退兩次）。
 * 扣款、退款都會在主控台留一行 {@code [PaidService]}。
 * <p>
 * 腳本：JS {@code Java.type('net.swordie.ms.world.tms282.activity.PaidService').charge(stage.getChr(), "donation", "anvil", 1000, "時裝鐵砧")}；
 * Python {@code from net.swordie.ms.world.tms282.activity import PaidService}。
 * <p>
 * fixes10：{@link #charge(Char, String, String, int, String, Grant)} 帶一個 {@link Grant}（例如 {@link PaidGrants#battlePass}）：
 * 「只能買一次」的權利在扣款的同一筆交易寫入（users 列已鎖，同一帳號的購買排隊），已經有了就整筆回滾、回傳 LIMITED、不扣款。
 * hypay 退款同時把 payUsed 扣回（{@link PointWallet#refund}），收據已標記 refunded 就不會再退。
 */
public final class PaidService {
    private PaidService() {
    }

    /**
     * 跟扣款同一筆交易寫入的權利（fixes10）：在餘額檢查之後、扣款之前執行；回傳 false = 已經擁有 → 整筆回滾，LIMITED，不扣款。
     * 例外 → 整筆回滾，FAILED。實作見 {@link PaidGrants}。
     */
    public interface Grant {
        /** 會讀寫的資料表，必須是 InnoDB（否則不扣款，FAILED）。 */
        List<String> tables();

        /** 已鎖 users 列的交易中：檢查並寫入；false = 已經擁有。 */
        boolean grant(Connection c, Char chr) throws SQLException;

        /** 寫在收據的說明（{@code ;grant=...}）。 */
        String describe();
    }

    /** 扣 {@code cost}（{@code wallet} 為 PointWallets 的名稱：maple 樂豆、donation 點券／抵用券、nx 樂豆點…）並寫收據。 */
    public static Result charge(Char chr, String wallet, String key, int cost, String detail) {
        return charge(chr, wallet, key, cost, detail, null);
    }

    /**
     * 同上，另外在同一筆交易寫入 {@code grant}（null = 沒有）：已經擁有 → LIMITED，什麼都不扣、不寫。成交後不需要再做任何事，
     * 也就不需要 refund（權利與扣款一起提交或一起回滾）。
     */
    public static Result charge(Char chr, String wallet, String key, int cost, String detail, Grant grant) {
        PointWallet debit = PointWallets.of(wallet);
        if (debit == null || key == null || !key.matches("[a-z][a-z0-9_]{0,23}") || cost <= 0 || cost > 10_000_000) {
            throw new IllegalArgumentException("Invalid paid service");
        }
        if (chr == null || Protocol.active().version() != ProtocolVersion.TMS_282_3) {
            return Result.of(Status.OFFLINE);
        }
        String what = clean(detail);
        Result[] result = {Result.of(Status.FAILED)};
        EconomyLocks.packet(chr, () -> {
            if (!ActivityRewards.online(chr) || chr.getUser() == null) {
                result[0] = Result.of(Status.OFFLINE);
                return null;
            }
            synchronized (chr.getAccount()) {
                try {
                    Tms282VMatrixRuntime.requireEconomicAccess(chr);
                    if (Tms282CashExpansionService.isBalanceUncertain(chr.getUser())) {
                        return null;
                    }
                    List<User> copies = ActivityRewards.walletCopies(chr);
                    result[0] = ActivityRewards.locked(copies, 0, () -> chargeLocked(chr, debit, key, cost, what, copies, grant));
                } catch (SQLException | RuntimeException e) {
                    System.out.println("[PaidService] charge failed service=" + key + " wallet=" + wallet + " character=" + chr.getId()
                            + " error=" + e.getClass().getSimpleName());
                }
            }
            return null;
        });
        return result[0];
    }

    private static Result chargeLocked(Char chr, PointWallet debit, String key, int cost, String detail, List<User> copies, Grant grant) throws SQLException {
        ActivityRewards.schema(true);
        try (Connection c = DatabaseManager.getConnection()) {
            if (grant != null) {
                innoDb(c, grant.tables());
            }
            c.setAutoCommit(false);
            try {
                lockUser(c, chr);
                long before = debit.balance(c, chr, true);
                if (before < cost) {
                    c.rollback();
                    debit.sync(copies, before);
                    return new Result(Status.INSUFFICIENT, 0, (int) Math.min(Integer.MAX_VALUE, before), null, false);
                }
                if (grant != null && !grant.grant(c, chr)) {
                    c.rollback();
                    debit.sync(copies, before);
                    System.out.println("[PaidService] already granted service=" + key + " character=" + chr.getId() + " grant=" + clean(grant.describe()));
                    return new Result(Status.LIMITED, 0, (int) Math.min(Integer.MAX_VALUE, before), null, false);
                }
                String receiptKey = PointRaffle.receiptKey(key, chr.getId());
                debit.apply(c, chr, before, -cost, receiptKey);
                String receipt = "character=" + chr.getId() + ";wallet=" + debit.id() + ";at=" + Instant.now() + ";cost=" + cost
                        + ";before=" + before + ";service=" + detail + (grant == null ? "" : ";grant=" + clean(grant.describe()));
                try (PreparedStatement p = c.prepareStatement("INSERT INTO account_questex(accountid,questid,`key`,`value`) VALUES(?,?,?,?)")) {
                    p.setInt(1, chr.getAccount().getId());
                    p.setInt(2, PointRaffle.RECEIPT_QUEST);
                    p.setString(3, receiptKey);
                    p.setString(4, receipt);
                    if (p.executeUpdate() != 1) {
                        throw new SQLException("Paid service receipt was not saved");
                    }
                }
                c.commit();
                debit.sync(copies, before - cost);
                System.out.println("[PaidService] charged " + receiptKey + " " + receipt);
                return new Result(Status.GRANTED, cost, (int) Math.min(Integer.MAX_VALUE, before - cost), receiptKey, true);
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** 把 {@link #charge} 的費用退回原錢包；收據不存在、不是這個帳號的或已經退過就回傳 false。 */
    public static boolean refund(Char chr, String receiptKey) {
        if (chr == null || receiptKey == null || !receiptKey.startsWith("raffle:")) {
            return false;
        }
        boolean[] done = {false};
        EconomyLocks.packet(chr, () -> {
            synchronized (chr.getAccount()) {
                try {
                    List<User> copies = ActivityRewards.walletCopies(chr);
                    done[0] = ActivityRewards.locked(copies, 0, () -> refundLocked(chr, receiptKey, copies));
                } catch (SQLException | RuntimeException e) {
                    System.out.println("[PaidService] REFUND FAILED receipt=" + receiptKey + " character=" + chr.getId()
                            + " error=" + e.getClass().getSimpleName() + " (charge kept, refund by hand)");
                }
            }
            return null;
        });
        return done[0];
    }

    private static boolean refundLocked(Char chr, String receiptKey, List<User> copies) throws SQLException {
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                lockUser(c, chr);
                String value;
                try (PreparedStatement p = c.prepareStatement("SELECT `value` FROM account_questex WHERE accountid=? AND questid=? AND `key`=? FOR UPDATE")) {
                    p.setInt(1, chr.getAccount().getId());
                    p.setInt(2, PointRaffle.RECEIPT_QUEST);
                    p.setString(3, receiptKey);
                    try (ResultSet r = p.executeQuery()) {
                        value = r.next() ? r.getString(1) : null;
                    }
                }
                if (value == null || !value.contains(";service=") || value.contains(";refunded=")
                        || !value.startsWith("character=" + chr.getId() + ";")) {
                    c.rollback();
                    return false;
                }
                PointWallet wallet = PointWallets.of(field(value, "wallet"));
                int cost = Integer.parseInt(field(value, "cost"));
                if (wallet == null || cost <= 0) {
                    throw new SQLException("Unreadable paid service receipt");
                }
                long before = wallet.balance(c, chr, true);
                wallet.refund(c, chr, before, cost, receiptKey);
                try (PreparedStatement p = c.prepareStatement("UPDATE account_questex SET `value`=? WHERE accountid=? AND questid=? AND `key`=?")) {
                    p.setString(1, value + ";refunded=" + Instant.now());
                    p.setInt(2, chr.getAccount().getId());
                    p.setInt(3, PointRaffle.RECEIPT_QUEST);
                    p.setString(4, receiptKey);
                    if (p.executeUpdate() != 1) {
                        throw new SQLException("Refund mark was not saved");
                    }
                }
                c.commit();
                wallet.sync(copies, before + cost);
                System.out.println("[PaidService] refunded " + receiptKey + " +" + cost + " " + wallet.id());
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Every table must exist and be InnoDB (a MyISAM table would not roll back with the charge). */
    static void innoDb(Connection c, List<String> tables) throws SQLException {
        Set<String> seen = new LinkedHashSet<>(tables);
        for (String table : seen) {
            try (PreparedStatement p = c.prepareStatement("SELECT ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=?")) {
                p.setString(1, table);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next() || !"InnoDB".equalsIgnoreCase(r.getString(1))) {
                        throw new SQLException(table + " must be InnoDB");
                    }
                }
            }
        }
    }

    private static void lockUser(Connection c, Char chr) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM users WHERE id=? FOR UPDATE")) {
            p.setInt(1, chr.getUser().getId());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new SQLException("Missing wallet user");
                }
            }
        }
    }

    static String field(String receipt, String name) {
        for (String part : receipt.split(";")) {
            if (part.startsWith(name + "=")) {
                return part.substring(name.length() + 1);
            }
        }
        return null;
    }

    static String clean(String detail) {
        String d = detail == null ? "" : detail.replace(';', ',').replace('=', ':').replace('\r', ' ').replace('\n', ' ').strip();
        return d.length() > 120 ? d.substring(0, 120) : d;
    }
}
