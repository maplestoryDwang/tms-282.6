package net.swordie.ms.world.tms282.newbie;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link NewbieStore} on the game database (DatabaseManager; the server's restricted account: SELECT / INSERT /
 * UPDATE and CREATE TABLE IF NOT EXISTS). Every write is a single conditional statement or one transaction.
 */
public final class JdbcNewbieStore implements NewbieStore {
    private static final String COLUMNS = "character_id, account_id, user_id, login_job, step_id, branch, gift_claimed_at, "
            + "job_done_at, completed_at, created_at, updated_at";

    private final Supplier<Instant> clock;
    private volatile boolean ready;

    public JdbcNewbieStore() {
        this(Instant::now);
    }

    public JdbcNewbieStore(Supplier<Instant> clock) {
        this.clock = clock;
    }

    private static Connection connection() throws SQLException {
        return DatabaseManager.getConnection();
    }

    private Timestamp now() {
        return Timestamp.from(clock.get());
    }

    @Override
    public void ensureSchema() throws SQLException {
        if (ready) return;
        synchronized (this) {
            if (ready) return;
            try (Connection c = connection(); Statement s = c.createStatement()) {
                for (String ddl : NewbieSchema.DDL) {
                    s.executeUpdate(ddl);
                }
            }
            ready = true;
        }
    }

    @Override
    public boolean create(int characterId, int accountId, int userId, String loginJob, String firstStepId) throws SQLException {
        ensureSchema();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO newbie_flow (character_id, account_id, user_id, login_job, step_id, branch, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, '', ?, ?)")) {
            Timestamp now = now();
            ps.setInt(1, characterId);
            ps.setInt(2, accountId);
            ps.setInt(3, userId);
            ps.setString(4, cut(loginJob, 32));
            ps.setString(5, cut(firstStepId, 32));
            ps.setTimestamp(6, now);
            ps.setTimestamp(7, now);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public Optional<NewbieProgress> load(int characterId) throws SQLException {
        ensureSchema();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM newbie_flow WHERE character_id = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(row(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public boolean advance(int characterId, String fromStep, String toStep) throws SQLException {
        ensureSchema();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE newbie_flow SET step_id = ?, updated_at = ? WHERE character_id = ? AND step_id = ? AND completed_at IS NULL")) {
            ps.setString(1, cut(toStep, 32));
            ps.setTimestamp(2, now());
            ps.setInt(3, characterId);
            ps.setString(4, cut(fromStep, 32));
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean complete(int characterId, String fromStep) throws SQLException {
        ensureSchema();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE newbie_flow SET step_id = '', completed_at = ?, updated_at = ? WHERE character_id = ? AND step_id = ? AND completed_at IS NULL")) {
            Timestamp now = now();
            ps.setTimestamp(1, now);
            ps.setTimestamp(2, now);
            ps.setInt(3, characterId);
            ps.setString(4, cut(fromStep, 32));
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public Claim claimGift(int characterId, String configVersion, String branch, String items, int maxClaimsPerUser) throws SQLException {
        ensureSchema();
        try (Connection c = connection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                Claim r = claim(c, characterId, configVersion, branch, items, maxClaimsPerUser);
                if (r == Claim.CLAIMED) {
                    c.commit();
                } else {
                    c.rollback();
                }
                return r;
            } catch (SQLIntegrityConstraintViolationException e) {
                c.rollback(); // uq_newbie_gift_char: a concurrent claim won
                return Claim.ALREADY;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(auto);
            }
        }
    }

    private Claim claim(Connection c, int characterId, String configVersion, String branch, String items, int max) throws SQLException {
        int userId;
        int accountId;
        try (PreparedStatement ps = c.prepareStatement("SELECT user_id, account_id FROM newbie_flow WHERE character_id = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Claim.NO_ROW;
                userId = rs.getInt(1);
                accountId = rs.getInt(2);
            }
        }
        if (max > 0) {
            // the user's rows in one fixed order (character_id), so two characters of one user never deadlock
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT character_id FROM newbie_flow WHERE user_id = ? ORDER BY character_id FOR UPDATE")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rs.getInt(1);
                    }
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT gift_claimed_at FROM newbie_flow WHERE character_id = ? FOR UPDATE")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Claim.NO_ROW;
                if (rs.getTimestamp(1) != null) return Claim.ALREADY;
            }
        }
        if (max > 0) {
            // Locking read sees the latest committed claims after waiting for the user's progress locks;
            // a plain COUNT under REPEATABLE READ would reuse the snapshot from the first SELECT above.
            try (PreparedStatement ps = c.prepareStatement("SELECT id FROM newbie_gift_log WHERE user_id = ? FOR UPDATE")) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    int count = 0;
                    while (rs.next()) if (++count >= max) return Claim.LIMIT;
                }
            }
        }
        Timestamp now = now();
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE newbie_flow SET gift_claimed_at = ?, updated_at = ? WHERE character_id = ? AND gift_claimed_at IS NULL")) {
            ps.setTimestamp(1, now);
            ps.setTimestamp(2, now);
            ps.setInt(3, characterId);
            if (ps.executeUpdate() != 1) return Claim.ALREADY;
        }
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO newbie_gift_log (character_id, account_id, user_id, config_version, branch, items, claimed_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, characterId);
            ps.setInt(2, accountId);
            ps.setInt(3, userId);
            ps.setString(4, cut(configVersion, 40));
            ps.setString(5, cut(branch, 32));
            ps.setString(6, cut(items, 2000));
            ps.setTimestamp(7, now);
            ps.executeUpdate();
        }
        return Claim.CLAIMED;
    }

    @Override
    public boolean markJobDone(int characterId, String branch) throws SQLException {
        ensureSchema();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE newbie_flow SET job_done_at = ?, branch = ?, updated_at = ? WHERE character_id = ? AND job_done_at IS NULL")) {
            Timestamp now = now();
            ps.setTimestamp(1, now);
            ps.setString(2, cut(branch, 32));
            ps.setTimestamp(3, now);
            ps.setInt(4, characterId);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public boolean markZeroJobDone(int characterId, String characterName) throws SQLException {
        ensureSchema();
        try (Connection c = connection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("SELECT job_done_at FROM newbie_flow WHERE character_id = ? FOR UPDATE")) {
                    ps.setInt(1, characterId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Missing Zero newbie row: " + characterId);
                        if (rs.getTimestamp(1) != null) { c.rollback(); return false; }
                    }
                }
                Timestamp now = now();
                // Original zeroProl000 -> Char.sendLevelRewardToChar(30/60/100). Level100 is an empty list.
                int[][] rewards = {{30, 2430447, 1}, {30, 1112444, 1}, {30, 2450140, 1}, {60, 2450140, 2}};
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO rewardinfo "
                        + "(charid, charname, rewardItemType, itemID, quantity, maplePoint, meso, exp, description, starttime, endtime) "
                        + "VALUES (?, ?, ?, ?, ?, 0, 0, 0, ?, ?, ?)")) {
                    for (int[] reward : rewards) {
                        ps.setInt(1, characterId);
                        ps.setString(2, characterName);
                        ps.setInt(3, net.swordie.ms.enums.reward.RewardItemType.Item.getVal());
                        ps.setInt(4, reward[1]);
                        ps.setInt(5, reward[2]);
                        ps.setString(6, "服務器贈送的等級 " + reward[0] + " 達成獎勵！");
                        ps.setTimestamp(7, now);
                        ps.setTimestamp(8, Timestamp.from(now.toInstant().plus(30, java.time.temporal.ChronoUnit.DAYS)));
                        if (ps.executeUpdate() != 1) throw new SQLException("Zero reward not inserted");
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE newbie_flow SET job_done_at = ?, branch = '*', updated_at = ? "
                        + "WHERE character_id = ? AND job_done_at IS NULL")) {
                    ps.setTimestamp(1, now);
                    ps.setTimestamp(2, now);
                    ps.setInt(3, characterId);
                    if (ps.executeUpdate() != 1) throw new SQLException("Zero receipt not recorded");
                }
                c.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally { c.setAutoCommit(auto); }
        }
    }

    @Override
    public boolean reopenZeroPrologue(int characterId, String firstJobStep) throws SQLException {
        ensureSchema();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(
                "UPDATE newbie_flow SET step_id = ?, completed_at = NULL, updated_at = ? WHERE character_id = ? AND completed_at IS NOT NULL")) {
            ps.setString(1, cut(firstJobStep, 32));
            ps.setTimestamp(2, now());
            ps.setInt(3, characterId);
            return ps.executeUpdate() == 1;
        }
    }

    @Override
    public List<Row> list(String query, int limit) throws SQLException {
        ensureSchema();
        String q = query == null ? "" : query.strip();
        int id = -1;
        try {
            id = q.isEmpty() ? -1 : Integer.parseInt(q);
        } catch (NumberFormatException ignored) {
            // a name
        }
        String sql = "SELECT f.character_id, f.account_id, f.user_id, f.login_job, f.step_id, f.branch, f.gift_claimed_at, f.job_done_at, "
                + "f.completed_at, f.created_at, f.updated_at, cs.name, cs.level, cs.job, cs.posmap FROM newbie_flow f "
                + "LEFT JOIN characterstats cs ON cs.characterid = f.character_id "
                + (q.isEmpty() ? "" : "WHERE f.character_id = ? OR cs.name = ? ")
                + "ORDER BY f.updated_at DESC LIMIT ?";
        List<Row> rows = new ArrayList<>();
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            if (!q.isEmpty()) {
                ps.setInt(i++, id);
                ps.setString(i++, q);
            }
            ps.setInt(i, Math.max(1, Math.min(limit, 500)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long posMap;
                    try {
                        String p = rs.getString(15);
                        posMap = p == null ? 0 : Long.parseLong(p.trim());
                    } catch (NumberFormatException e) {
                        posMap = 0;
                    }
                    rows.add(new Row(row(rs), rs.getString(12), rs.getInt(13), rs.getInt(14), posMap));
                }
            }
        }
        return rows;
    }

    private static NewbieProgress row(ResultSet rs) throws SQLException {
        return new NewbieProgress(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4), rs.getString(5), rs.getString(6),
                instant(rs.getTimestamp(7)), instant(rs.getTimestamp(8)), instant(rs.getTimestamp(9)),
                instant(rs.getTimestamp(10)), instant(rs.getTimestamp(11)));
    }

    private static Instant instant(Timestamp t) {
        return t == null ? null : t.toInstant();
    }

    private static String cut(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
