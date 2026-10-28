package net.swordie.ms.world.shop.cashshop;

import java.sql.*;
import java.util.*;

/** Native VIP point lots, separate from NX, Maple Points, mileage and permanent VIP coins.
 * The caller owns the transaction so point changes can commit with the purchased goods. */
public final class CashVipPoints {
    private CashVipPoints() { }
    public record Lot(long id, long remaining, long expiresAt) { }
    public record Debit(long lotId, long amount) { }
    public enum Result { APPLIED, ALREADY_APPLIED, INSUFFICIENT_POINTS }

    /** Native CCashShop 215CF40 selects these exact SN ranges for VIP-point spending. */
    public static boolean isVipCommodity(int sn) {
        return (sn >= 190000000 && sn < 190100000)
                || (sn >= 190200000 && sn < 190400000);
    }

    public static long balance(List<Lot> lots, long now) {
        long total = 0;
        for (Lot lot : lots) {
            if (lot.remaining() < 0) throw new IllegalArgumentException("Negative point lot");
            if (lot.expiresAt() > now) total = Math.addExact(total, lot.remaining());
        }
        return total;
    }

    /** Earliest expiry first; absent means insufficient funds and performs no mutation. */
    public static Optional<List<Debit>> plan(List<Lot> lots, long amount, long now) {
        if (amount <= 0) throw new IllegalArgumentException("Point amount must be positive");
        if (balance(lots, now) < amount) return Optional.empty();
        List<Debit> result = new ArrayList<>();
        for (Lot lot : lots.stream().filter(l -> l.remaining() > 0 && l.expiresAt() > now)
                .sorted(Comparator.comparingLong(Lot::expiresAt).thenComparingLong(Lot::id)).toList()) {
            long take = Math.min(amount, lot.remaining());
            result.add(new Debit(lot.id(), take));
            amount -= take;
            if (amount == 0) break;
        }
        return Optional.of(List.copyOf(result));
    }

    private static void lockOwner(Connection c, int userId) throws SQLException {
        if (userId <= 0 || c.getAutoCommit()) throw new IllegalArgumentException("Owned transaction required");
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM users WHERE id=? FOR UPDATE")) {
            p.setInt(1, userId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("VIP point owner missing");
            }
        }
    }

    public static List<Lot> lots(Connection c, int userId) throws SQLException {
        List<Lot> result = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(
                "SELECT id,remaining,expires_at FROM tms282_vip_point_lots WHERE userid=? AND remaining>0 ORDER BY expires_at,id")) {
            p.setInt(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) result.add(new Lot(r.getLong(1), r.getLong(2), r.getLong(3)));
            }
        }
        return List.copyOf(result);
    }

    private static boolean alreadyApplied(Connection c, int userId, String request, long delta, long expiresAt) throws SQLException {
        if (request == null || request.isBlank() || request.length() > 64
                || request.chars().anyMatch(ch -> ch < 33 || ch > 126))
            throw new IllegalArgumentException("Stable request id required");
        try (PreparedStatement p = c.prepareStatement(
                "SELECT delta,expires_at FROM tms282_vip_point_operations WHERE userid=? AND request_id=?")) {
            p.setInt(1, userId); p.setString(2, request);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return false;
                if (r.getLong(1) != delta || r.getLong(2) != expiresAt)
                    throw new SQLException("VIP point request reused with different parameters");
                return true;
            }
        }
    }

    private static void record(Connection c, int userId, String request, long delta, long expiresAt) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO tms282_vip_point_operations(userid,request_id,delta,expires_at) VALUES(?,?,?,?)")) {
            p.setInt(1, userId); p.setString(2, request); p.setLong(3, delta); p.setLong(4, expiresAt);
            if (p.executeUpdate() != 1) throw new SQLException("VIP point operation not saved");
        }
    }

    public static Result credit(Connection c, int userId, String request, long amount, long expiresAt, long now) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Point amount must be positive");
        lockOwner(c, userId);
        if (alreadyApplied(c, userId, request, amount, expiresAt)) return Result.ALREADY_APPLIED;
        if (expiresAt <= now) throw new IllegalArgumentException("New points already expired");
        Math.addExact(balance(lots(c, userId), now), amount);
        Savepoint savepoint = c.setSavepoint();
        try {
            try (PreparedStatement p = c.prepareStatement(
                    "INSERT INTO tms282_vip_point_lots(userid,remaining,expires_at) VALUES(?,?,?)")) {
                p.setInt(1, userId); p.setLong(2, amount); p.setLong(3, expiresAt);
                if (p.executeUpdate() != 1) throw new SQLException("VIP point credit not saved");
            }
            record(c, userId, request, amount, expiresAt);
            return Result.APPLIED;
        } catch (SQLException | RuntimeException e) { c.rollback(savepoint); throw e; }
    }

    public static Result spend(Connection c, int userId, String request, long amount, long now) throws SQLException {
        if (amount <= 0) throw new IllegalArgumentException("Point amount must be positive");
        lockOwner(c, userId);
        if (alreadyApplied(c, userId, request, -amount, 0)) return Result.ALREADY_APPLIED;
        var plan = plan(lots(c, userId), amount, now);
        if (plan.isEmpty()) return Result.INSUFFICIENT_POINTS;
        Savepoint savepoint = c.setSavepoint();
        try {
            for (Debit debit : plan.get()) {
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE tms282_vip_point_lots SET remaining=remaining-? WHERE id=? AND userid=? AND remaining>=? AND expires_at>?")) {
                    p.setLong(1, debit.amount()); p.setLong(2, debit.lotId()); p.setInt(3, userId);
                    p.setLong(4, debit.amount()); p.setLong(5, now);
                    if (p.executeUpdate() != 1) throw new SQLException("VIP point lot changed during purchase");
                }
            }
            record(c, userId, request, -amount, 0);
            return Result.APPLIED;
        } catch (SQLException | RuntimeException e) { c.rollback(savepoint); throw e; }
    }
}
