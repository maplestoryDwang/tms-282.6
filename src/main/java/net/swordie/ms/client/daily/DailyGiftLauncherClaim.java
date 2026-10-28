package net.swordie.ms.client.daily;

import net.swordie.ms.connection.api.launcher.gift.GiftSchema;
import net.swordie.ms.connection.api.launcher.gift.RewardQueuePeriod;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.DailyGiftConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Launcher sign-in (2026-10-08): the game's own 28-day daily gift ({@link DailyGift}) claimed from the launcher for an
 * account that is not in the game; the reward goes to the chosen character of that world through launcher_reward_queue
 * (handed over at its next field entry, with the reward's period - {@link RewardQueuePeriod}). One record per account, so claiming in the game or
 * in the launcher is still once a day. The claim is one conditional UPDATE ({@code isClaim = 0 AND dateComplete = n}):
 * a concurrent claim cannot pass twice. Afterwards {@link DailyStreakBonus} adds the streak bonus, as the game claim does.
 */
public final class DailyGiftLauncherClaim {
    public enum Status { CLAIMED, ALREADY_CLAIMED, MONTH_COMPLETE, UNAVAILABLE }

    public record Result(Status status, int dayIndex, int itemId, int quantity, int periodDays, List<DailyStreakBonus.Granted> bonuses) {
    }

    private DailyGiftLauncherClaim() {
    }

    /** {@code accountId} (the world account, not in the game) claims today's gift for {@code characterId} of that account. */
    public static Result claim(int userId, int accountId, int characterId) throws SQLException {
        GiftSchema.ensure();
        long now = System.currentTimeMillis();
        DailyGift dg = DailyGift.getDailyGiftFromSQLByAccountID(accountId);
        if (dg == null) {
            dg = new DailyGift();
            dg.setDate(now);
            dg.setAccountID(accountId);
            dg.saveToSQL();
        }
        boolean[] reset = dg.rollover(now);
        if (reset[0] || reset[1]) {
            dg.saveToSQL();
        }
        if (dg.isClaim()) {
            return new Result(Status.ALREADY_CLAIMED, 0, 0, 0, 0, List.of());
        }
        DailyGiftItemInfo reward = dg.nextReward();
        if (reward == null) {
            return new Result(Status.MONTH_COMPLETE, 0, 0, 0, 0, List.of());
        }
        int itemId = reward.getItemId();
        int quantity = reward.getQuantity();
        if (!net.swordie.ms.world.tms282.items.Tms282ItemAvailability.isGrantable(itemId)) {
            return new Result(Status.UNAVAILABLE, 0, itemId, quantity, 0, List.of());
        }
        int period = DailyGiftConstants.periodDays(reward);
        if (period > 0 && !RewardQueuePeriod.available()) {
            // a timed reward is never queued as a permanent item: claim it in the game
            return new Result(Status.UNAVAILABLE, 0, itemId, quantity, period, List.of());
        }
        int before = dg.getDateComplete();
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE dailygift SET dateComplete = ?, isClaim = 1, date = ? WHERE id = ? AND isClaim = 0 AND dateComplete = ?")) {
                    ps.setInt(1, before + 1);
                    ps.setLong(2, dg.getDate());
                    ps.setLong(3, dg.getId());
                    ps.setInt(4, before);
                    if (ps.executeUpdate() != 1) {
                        c.rollback();
                        return new Result(Status.ALREADY_CLAIMED, 0, 0, 0, 0, List.of());
                    }
                }
                queue(c, userId, characterId, itemId, quantity, period, "daily", dg.getDate());
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
        DailyGiftClaimLog.claimed(accountId, dg.getDate(), before + 1, itemId, quantity, "launcher");
        System.out.printf("[DailyGift] launcher claim: user %d account %d day %d item %d x%d -> character %d%n",
                userId, accountId, before + 1, itemId, quantity, characterId);
        List<DailyStreakBonus.Granted> bonuses = DailyStreakBonus.afterClaim(userId, accountId, characterId);
        return new Result(Status.CLAIMED, before + 1, itemId, quantity, period, bonuses);
    }

    /** One launcher_reward_queue line (periodDays 0 = permanent; a timed line also gets its launcher_reward_period row). */
    static void queue(Connection c, int userId, int characterId, int itemId, int quantity, int periodDays, String source, long sourceId)
            throws SQLException {
        RewardQueuePeriod.queue(c, userId, characterId, itemId, quantity, Math.max(0, periodDays), source, sourceId);
    }
}
