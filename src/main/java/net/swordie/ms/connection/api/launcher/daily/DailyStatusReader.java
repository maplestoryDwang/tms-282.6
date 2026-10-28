package net.swordie.ms.connection.api.launcher.daily;

import net.swordie.ms.client.daily.DailyGift;
import net.swordie.ms.client.daily.DailyGiftLogStore;
import net.swordie.ms.connection.api.launcher.LauncherServices;
import net.swordie.ms.connection.api.launcher.ServerMemory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Reads one world's {@link DailyStatusView} of the session's user - SELECT only, nothing is written back:
 * <ol>
 *     <li>the user's account in that world ({@code accounts.userid = ? AND worldid = ?}, unique);</li>
 *     <li>its record: the game's copy while the server holds the user ({@link ServerMemory#dailyGift}), else the
 *     dailygift row with the highest id ({@code accountID} has no unique key; the game reads one of them), else none;</li>
 *     <li>today's rollover ({@link DailyGift#rollover}: a new day clears the claim, a new month the count) applied to
 *     a fresh {@link DailyGift} value object - never to the game's object, never saved;</li>
 *     <li>this month's dates from {@code dailygift_log} (fixes10: or an asked past month's, up to
 *     {@link DailyStatusView#PAST_MONTHS} back); a missing table answers {@code logAvailable:false}.</li>
 * </ol>
 * Days follow the JVM's default time zone, as the game's rollover does.
 */
public final class DailyStatusReader {
    private final LongSupplier clock;

    public DailyStatusReader() {
        this(System::currentTimeMillis);
    }

    /** With another clock (checks: a fixed "today"). */
    public DailyStatusReader(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The current month of this reader's clock (JVM default time zone). */
    public YearMonth currentMonth() {
        return YearMonth.from(Instant.ofEpochMilli(clock.getAsLong()).atZone(ZoneId.systemDefault()).toLocalDate());
    }

    public DailyStatusView read(Connection con, int userId, int worldId, LauncherServices services) throws SQLException {
        return read(con, userId, worldId, services, null);
    }

    /**
     * fixes10: {@code month} = the month whose dated claims to list (null = the current month); must be
     * {@link DailyStatusView#inRange} of today (IllegalArgumentException otherwise - the action checks first).
     */
    public DailyStatusView read(Connection con, int userId, int worldId, LauncherServices services, YearMonth month) throws SQLException {
        long now = clock.getAsLong();
        LocalDate today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate();
        YearMonth view = month == null ? YearMonth.from(today) : month;
        if (!DailyStatusView.inRange(view, YearMonth.from(today))) {
            throw new IllegalArgumentException("month out of range");
        }
        DailyRewardTable table = services.rewards();
        DailyRewardTable.Snapshot catalog = null;
        String rewardsVersion = null;
        try {
            catalog = table.snapshot();
            rewardsVersion = catalog.version();
        } catch (RuntimeException e) {
            // An unavailable first catalog leaves both version and reward absent.
        }
        Integer accountId = accountId(con, userId, worldId);
        if (accountId == null) {
            return new DailyStatusView(worldId, false, "none", today, 0, false, next(catalog, 0), List.of(), null, rewardsVersion, view);
        }
        String source = "none";
        DailyGift copy = new DailyGift();
        copy.setAccountID(accountId);
        copy.setDate(now);
        Optional<ServerMemory.DailyGiftLive> live = Optional.empty();
        try {
            live = services.memory().dailyGift(userId, worldId);
        } catch (RuntimeException e) {
            // the users / accounts maps changed under us: the database copy is the fallback
        }
        if (live.isPresent()) {
            ServerMemory.DailyGiftLive g = live.get();
            fill(copy, g.id(), g.date(), g.dateComplete(), g.claimed());
            source = "memory";
        } else if (fromDatabase(con, accountId, copy)) {
            source = "database";
        }
        copy.rollover(now);
        int claimed = Math.max(0, copy.getDateComplete());
        List<LocalDate> dates;
        boolean logAvailable;
        try {
            dates = DailyGiftLogStore.claimDates(con, accountId, view);
            logAvailable = true;
        } catch (SQLException e) {
            if (!DailyGiftLogStore.missingTable(e)) {
                throw e;
            }
            dates = List.of();
            logAvailable = false;
        }
        return new DailyStatusView(worldId, true, source, today, claimed, copy.isClaim(), next(catalog, claimed), dates,
                logAvailable, rewardsVersion, view);
    }

    private static DailyRewardTable.Reward next(DailyRewardTable.Snapshot catalog, int claimed) {
        return catalog == null ? null : catalog.next(claimed);
    }

    private static void fill(DailyGift copy, long id, long date, int dateComplete, boolean claimed) {
        copy.setId(id);
        copy.setDate(date);
        copy.setDateComplete(dateComplete);
        copy.setClaim(claimed);
    }

    static Integer accountId(Connection con, int userId, int worldId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM accounts WHERE userid = ? AND worldid = ? ORDER BY id LIMIT 1")) {
            ps.setInt(1, userId);
            ps.setInt(2, worldId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    /** The row with the highest id of {@code accountId}; false when there is none. */
    static boolean fromDatabase(Connection con, int accountId, DailyGift copy) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id, date, dateComplete, isClaim FROM dailygift WHERE accountID = ? ORDER BY id DESC LIMIT 1")) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                fill(copy, rs.getLong("id"), rs.getLong("date"), rs.getInt("dateComplete"), rs.getByte("isClaim") == 1);
                return true;
            }
        }
    }
}
