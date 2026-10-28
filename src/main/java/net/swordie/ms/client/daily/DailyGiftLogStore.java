package net.swordie.ms.client.daily;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@code dailygift_log}: one row per world account (accounts.id, the same id as dailygift.accountID) and calendar day
 * on which a daily gift was claimed - the dates the launcher's stamp card shows (dailygift itself only keeps the count
 * and the last day). Table: {@code sql/tms282/20261001-dailygift-log.sql}, created at deploy time; a database without
 * it is detected by {@link #missingTable(SQLException)}. Dates are in the JVM's default time zone, like
 * {@link DailyGift#rollover(long)}.
 */
public final class DailyGiftLogStore {
    public static final String TABLE = "dailygift_log";
    /** The unique key (account_id, claim_date) swallows a second row for the same day; any other error is thrown. */
    static final String INSERT = "INSERT INTO `dailygift_log` (`account_id`, `claim_date`, `day_index`, `item_id`, `count`, `source`, `claimed_at`)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `id` = `id`";
    static final String MONTH = "SELECT `claim_date` FROM `dailygift_log` WHERE `account_id` = ? AND `claim_date` >= ? AND `claim_date` < ?"
            + " ORDER BY `claim_date`";
    /** MariaDB / MySQL "table doesn't exist". */
    static final int ER_NO_SUCH_TABLE = 1146;
    static final String SQLSTATE_NO_SUCH_TABLE = "42S02";

    /**
     * One claim.
     *
     * @param dayIndex  dateComplete after the claim (1..28)
     * @param source    {@code game} (the native daily gift window), {@code panel} (the control panel script)
     * @param claimedAt epoch milliseconds
     */
    public record Entry(int accountId, LocalDate claimDate, int dayIndex, int itemId, int count, String source,
                        long claimedAt) {
        public Entry {
            Objects.requireNonNull(claimDate, "claimDate");
            Objects.requireNonNull(source, "source");
        }
    }

    private DailyGiftLogStore() {
    }

    /** Writes {@code e}; a row for the same account and day is kept as it is (not an error). */
    public static void insert(Connection con, Entry e) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT)) {
            ps.setInt(1, e.accountId());
            ps.setDate(2, Date.valueOf(e.claimDate()));
            ps.setInt(3, e.dayIndex());
            ps.setInt(4, e.itemId());
            ps.setInt(5, e.count());
            ps.setString(6, e.source());
            ps.setTimestamp(7, new Timestamp(e.claimedAt()));
            ps.executeUpdate();
        }
    }

    /** The days of {@code month} on which {@code accountId} claimed, ascending (SELECT only). */
    public static List<LocalDate> claimDates(Connection con, int accountId, YearMonth month) throws SQLException {
        List<LocalDate> out = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(MONTH)) {
            ps.setInt(1, accountId);
            ps.setDate(2, Date.valueOf(month.atDay(1)));
            ps.setDate(3, Date.valueOf(month.plusMonths(1).atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date d = rs.getDate(1);
                    if (d != null) {
                        out.add(d.toLocalDate());
                    }
                }
            }
        }
        return out;
    }

    /** The table does not exist (not created yet on this database): SQLState 42S02 / error 1146, anywhere in the chain. */
    public static boolean missingTable(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException s && (s.getErrorCode() == ER_NO_SUCH_TABLE || SQLSTATE_NO_SUCH_TABLE.equals(s.getSQLState()))) {
                return true;
            }
        }
        SQLException next = e.getNextException();
        return next != null && next != e && missingTable(next);
    }
}
