package net.swordie.ms.world.tms282.activity;

import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ObjIntConsumer;

/**
 * Registry of the currencies paid raffles and gift packs use (handler registry: one {@link PointWallet} per script name).
 * <ul>
 *   <li>{@code maple} 樂豆／楓點 = users.maplePoints (stage.getCurrency(1))</li>
 *   <li>{@code donation} 點券／抵用券 = users.donationpoints (stage.getCurrency(2))</li>
 *   <li>{@code nx} 樂豆點 = users.nxPrepaid (stage.getCurrency(4))</li>
 *   <li>{@code hypay} 星語幣餘額 = hypay.pay of the user name (PayUtils type 1; a charge also adds payUsed, a
 *   PaidService refund takes it back out - fixes10)</li>
 *   <li>{@code hypay_reward} 累充 = hypay.payReward (PayUtils / vipCommon type 3)</li>
 *   <li>{@code rmb} 星語幣 = SUM(zz_rmb.rmb) of the compat account name (vipCommon type 1; a charge is a negative row)</li>
 *   <li>{@code money} 星語幣 (marin) = account.money (marin_7)</li>
 * </ul>
 */
public final class PointWallets {
    private static final Map<String, PointWallet> REGISTRY;

    static {
        Map<String, PointWallet> m = new LinkedHashMap<>();
        for (PointWallet w : List.of(
                new UserColumn("maple", "maplePoints", (u, v) -> u.setMaplePoints(v)),
                new UserColumn("donation", "donationpoints", (u, v) -> u.setDonationPoints(v)),
                new UserColumn("nx", "nxPrepaid", (u, v) -> u.setNxPrepaid(v)),
                new Hypay("hypay", "pay", true),
                new Hypay("hypay_reward", "payReward", false),
                new Rmb(),
                new AccountMoney())) m.put(w.id(), w);
        REGISTRY = Collections.unmodifiableMap(m);
    }

    private PointWallets() {}

    /** The wallet for a script name, or null. */
    public static PointWallet of(String id) { return id == null ? null : REGISTRY.get(id); }

    public static Map<String, PointWallet> all() { return REGISTRY; }

    static int checkedInt(long value) throws SQLException {
        if (value < 0 || value > Integer.MAX_VALUE) throw new SQLException("Wallet balance out of range");
        return (int) value;
    }

    /** A column of the users row (cached on every live {@link User} copy). */
    static final class UserColumn implements PointWallet {
        private final String id, column;
        private final ObjIntConsumer<User> setter;
        UserColumn(String id, String column, ObjIntConsumer<User> setter) { this.id = id; this.column = column; this.setter = setter; }
        @Override public String id() { return id; }
        @Override public List<String> tables() { return List.of("users"); }
        @Override public long balance(Connection c, Char chr, boolean forUpdate) throws SQLException {
            try (PreparedStatement p = c.prepareStatement("SELECT " + column + " FROM users WHERE id=?" + (forUpdate ? " FOR UPDATE" : ""))) {
                p.setInt(1, chr.getUser().getId());
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) throw new SQLException("Missing wallet user");
                    long v = r.getLong(1);
                    if (r.wasNull()) return 0;
                    return checkedInt(v);
                }
            }
        }
        @Override public void apply(Connection c, Char chr, long before, int delta, String receipt) throws SQLException {
            int after = checkedInt(before + delta);
            try (PreparedStatement p = c.prepareStatement("UPDATE users SET " + column + "=? WHERE id=?")) {
                p.setInt(1, after); p.setInt(2, chr.getUser().getId());
                if (p.executeUpdate() != 1) throw new SQLException("Wallet update was not saved");
            }
        }
        @Override public void sync(List<User> copies, long after) {
            for (User copy : copies) setter.accept(copy, (int) after);
        }
    }

    /** hypay row(s) of the user name. Legacy data has duplicate rows per name; every writer updates all of them. */
    static final class Hypay implements PointWallet {
        private final String id, column;
        private final boolean trackUsed;
        Hypay(String id, String column, boolean trackUsed) { this.id = id; this.column = column; this.trackUsed = trackUsed; }
        @Override public String id() { return id; }
        @Override public List<String> tables() { return List.of("hypay", "zsf_pay_detail"); }
        static String name(Char chr) throws SQLException {
            String n = chr.getUser().getName();
            if (n == null || n.isEmpty()) throw new SQLException("Missing hypay account name");
            return n;
        }
        @Override public long balance(Connection c, Char chr, boolean forUpdate) throws SQLException {
            try (PreparedStatement p = c.prepareStatement("SELECT " + column + " FROM hypay WHERE accname=? ORDER BY id" + (forUpdate ? " FOR UPDATE" : " LIMIT 1"))) {
                p.setString(1, name(chr));
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return 0;
                    BigDecimal v = r.getBigDecimal(1);
                    return v == null ? 0 : checkedInt(v.setScale(0, RoundingMode.FLOOR).longValueExact());
                }
            }
        }
        @Override public void apply(Connection c, Char chr, long before, int delta, String receipt) throws SQLException {
            checkedInt(before + delta);
            String name = name(chr);
            int rows;
            try (PreparedStatement p = c.prepareStatement("UPDATE hypay SET " + column + "=COALESCE(" + column + ",0)+?"
                    + (trackUsed && delta < 0 ? ", payUsed=payUsed+?" : "") + " WHERE accname=?")) {
                int i = 1;
                p.setBigDecimal(i++, BigDecimal.valueOf(delta));
                if (trackUsed && delta < 0) p.setBigDecimal(i++, BigDecimal.valueOf(-delta));
                p.setString(i, name);
                rows = p.executeUpdate();
            }
            if (rows == 0) {
                if (delta < 0) throw new SQLException("Missing hypay row for a charge");
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO hypay (accname, name, pay, payUsed, payReward, leiji) VALUES (?, ?, 0, 0, 0, 0)")) {
                    p.setString(1, name); p.setString(2, chr.getName()); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE hypay SET " + column + "=COALESCE(" + column + ",0)+? WHERE accname=?")) {
                    p.setBigDecimal(1, BigDecimal.valueOf(delta)); p.setString(2, name);
                    if (p.executeUpdate() < 1) throw new SQLException("hypay credit was not saved");
                }
            }
            detail(c, chr, delta, "point_order", receipt);
        }

        private void detail(Connection c, Char chr, long amount, String reason, String receipt) throws SQLException {
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO zsf_pay_detail (player_name, player_id, account_id, pay_type, amount, reason, reason_detail) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                p.setString(1, chr.getName()); p.setInt(2, chr.getId()); p.setInt(3, chr.getAccount().getId());
                p.setInt(4, trackUsed ? 1 : 3); p.setLong(5, amount); p.setString(6, reason); p.setString(7, receipt);
                p.executeUpdate();
            }
        }

        /**
         * fixes10: a refunded charge was never spent - the balance comes back and (星語幣 pay) payUsed goes down by the
         * same amount (never below 0), on every row of the name, like the charge added it.
         */
        @Override public void refund(Connection c, Char chr, long before, int amount, String receipt) throws SQLException {
            if (amount <= 0) throw new SQLException("Invalid refund amount");
            checkedInt(before + amount);
            String name = name(chr);
            try (PreparedStatement p = c.prepareStatement("UPDATE hypay SET " + column + "=COALESCE(" + column + ",0)+?"
                    + (trackUsed ? ", payUsed=GREATEST(COALESCE(payUsed,0)-?,0)" : "") + " WHERE accname=?")) {
                int i = 1;
                p.setBigDecimal(i++, BigDecimal.valueOf(amount));
                if (trackUsed) p.setBigDecimal(i++, BigDecimal.valueOf(amount));
                p.setString(i, name);
                if (p.executeUpdate() < 1) throw new SQLException("Missing hypay row for a refund");
            }
            detail(c, chr, amount, "paid_refund", receipt);
        }
    }

    /** zz_rmb ledger of the compat account name; the account row is the lock. */
    static final class Rmb implements PointWallet {
        @Override public String id() { return "rmb"; }
        @Override public List<String> tables() { return List.of("account", "zz_rmb"); }
        static String name(Connection c, Char chr, boolean forUpdate) throws SQLException {
            try (PreparedStatement p = c.prepareStatement("SELECT name FROM account WHERE id=?" + (forUpdate ? " FOR UPDATE" : ""))) {
                p.setInt(1, chr.getAccount().getId());
                try (ResultSet r = p.executeQuery()) { return r.next() ? r.getString(1) : null; }
            }
        }
        @Override public long balance(Connection c, Char chr, boolean forUpdate) throws SQLException {
            String name = name(c, chr, forUpdate);
            if (name == null) return 0;
            try (PreparedStatement p = c.prepareStatement("SELECT COALESCE(SUM(rmb),0) FROM zz_rmb WHERE account=?")) {
                p.setString(1, name);
                try (ResultSet r = p.executeQuery()) {
                    r.next();
                    return checkedInt(Math.max(0L, (long) Math.floor(r.getDouble(1) + 1e-9)));
                }
            }
        }
        @Override public void apply(Connection c, Char chr, long before, int delta, String receipt) throws SQLException {
            checkedInt(before + delta);
            String name = name(c, chr, true);
            if (name == null) throw new SQLException("Missing rmb account");
            try (PreparedStatement p = c.prepareStatement("INSERT INTO zz_rmb(account, rmb, paytime, remark) VALUES (?, ?, NOW(), ?)")) {
                p.setString(1, name); p.setInt(2, delta); p.setString(3, receipt);
                if (p.executeUpdate() != 1) throw new SQLException("rmb ledger row was not saved");
            }
        }
    }

    /** account.money (marin 星語幣). */
    static final class AccountMoney implements PointWallet {
        @Override public String id() { return "money"; }
        @Override public List<String> tables() { return List.of("account"); }
        @Override public long balance(Connection c, Char chr, boolean forUpdate) throws SQLException {
            try (PreparedStatement p = c.prepareStatement("SELECT money FROM account WHERE id=?" + (forUpdate ? " FOR UPDATE" : ""))) {
                p.setInt(1, chr.getAccount().getId());
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return 0;
                    long v = r.getLong(1);
                    return r.wasNull() ? 0 : Math.max(0, v);
                }
            }
        }
        @Override public void apply(Connection c, Char chr, long before, int delta, String receipt) throws SQLException {
            long after = before + delta;
            if (after < 0) throw new SQLException("Wallet balance out of range");
            try (PreparedStatement p = c.prepareStatement("UPDATE account SET money=? WHERE id=?")) {
                p.setLong(1, after); p.setInt(2, chr.getAccount().getId());
                if (p.executeUpdate() != 1) throw new SQLException("account money was not saved");
            }
        }
    }
}
