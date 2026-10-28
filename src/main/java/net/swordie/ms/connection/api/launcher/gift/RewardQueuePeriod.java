package net.swordie.ms.connection.api.launcher.gift;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * The period (days) of a launcher_reward_queue line whose item expires - the daily gift's timed rewards claimed from the
 * launcher (2026-10-08). It lives in a side table, launcher_reward_period (queue_id -> period_days), because the server's
 * database account may only CREATE tables: an ALTER on launcher_reward_queue is denied (see {@link GiftSchema}). No row =
 * a permanent item, so gift-code lines are untouched.
 * <p>
 * {@link #available()} never throws: when the table cannot be made, callers keep working without periods
 * ({@link LauncherRewardDelivery} reads the queue alone; the launcher claim refuses a timed reward).
 */
public final class RewardQueuePeriod {
    static final String DDL = "CREATE TABLE IF NOT EXISTS launcher_reward_period ("
            + "queue_id BIGINT NOT NULL,"
            + "period_days INT NOT NULL,"
            + "PRIMARY KEY (queue_id)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
    /** The queue lines of one character with their period (0 = permanent). */
    static final String SELECT_WITH_PERIOD = "SELECT q.id, q.item_id, q.quantity, COALESCE(p.period_days, 0) FROM launcher_reward_queue q "
            + "LEFT JOIN launcher_reward_period p ON p.queue_id = q.id WHERE q.character_id = ? AND q.delivered_at IS NULL ORDER BY q.id";
    static final String SELECT_PLAIN = "SELECT id, item_id, quantity, 0 FROM launcher_reward_queue WHERE character_id = ? AND delivered_at IS NULL ORDER BY id";

    private static volatile Boolean available;

    private RewardQueuePeriod() {
    }

    /** True when launcher_reward_period exists (made on first use). One log line when it cannot be made; asked again after a restart. */
    public static boolean available() {
        Boolean a = available;
        if (a != null) return a;
        synchronized (RewardQueuePeriod.class) {
            if (available != null) return available;
            try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
                s.executeUpdate(DDL);
                available = Boolean.TRUE;
            } catch (SQLException | RuntimeException e) {
                System.out.println("[RewardQueue] launcher_reward_period cannot be made (" + e.getClass().getSimpleName()
                        + "): timed rewards are not sent through the launcher queue");
                available = Boolean.FALSE;
            }
            return available;
        }
    }

    /** The query of {@link LauncherRewardDelivery}: with the period when the side table is there. */
    public static String selectSql() {
        return available() ? SELECT_WITH_PERIOD : SELECT_PLAIN;
    }

    /**
     * One launcher_reward_queue line on {@code c} (the caller's transaction); {@code periodDays > 0} also writes its period.
     * A timed line needs {@link #available()}: without the side table it is refused (IllegalStateException) rather than
     * queued as a permanent item.
     *
     * @return the queue id
     */
    public static long queue(Connection c, int userId, int characterId, int itemId, int quantity, int periodDays, String source, long sourceId)
            throws SQLException {
        if (periodDays > 0 && !available()) {
            throw new IllegalStateException("launcher_reward_period is not available");
        }
        long id;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO launcher_reward_queue (user_id, character_id, item_id, quantity, source, "
                + "source_id, created_at) VALUES (?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, characterId);
            ps.setInt(3, itemId);
            ps.setInt(4, quantity);
            ps.setString(5, source);
            ps.setLong(6, sourceId);
            ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
            if (ps.executeUpdate() != 1) throw new SQLException("reward queue line was not written");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw new SQLException("reward queue line has no id");
                id = rs.getLong(1);
            }
        }
        if (periodDays > 0) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO launcher_reward_period (queue_id, period_days) VALUES (?,?)")) {
                ps.setLong(1, id);
                ps.setInt(2, periodDays);
                if (ps.executeUpdate() != 1) throw new SQLException("reward period was not written");
            }
        }
        return id;
    }
}
