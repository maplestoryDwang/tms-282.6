package net.swordie.ms.client.character;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Persistence of an account's (world account's) Monster Collection, which 265 never loaded or saved. TMS keeps the
 * collection per account and world, so the rows are keyed by {@code accounts.id}. Three tables, created by
 * {@code sql/tms282/20260924-monster-collection.sql} (additive; the older 265 {@code monster_collection*} tables are
 * definition tables, and their reward table has no account column, so they are not used):
 * <ul>
 *     <li>{@code account_monster_collection_mobs(accid, mobid)}: registered mobs;</li>
 *     <li>{@code account_monster_collection_rewards(accid, region, session, groupid)}: claimed rewards, -1 = whole
 *     session / region;</li>
 *     <li>{@code account_monster_collection_explorations(accid, collectionkey, monsterkey, enddate, position)}.</li>
 * </ul>
 * Save replaces the account's rows in one transaction (delete, then insert). While the tables do not exist yet the
 * load returns an empty collection and the save does nothing; both log that once instead of failing the login/save.
 * The row form ({@link Rows}) is separate from the SQL so it can be checked without a database.
 */
public final class MonsterCollectionStore {
    public static final String MOBS = "account_monster_collection_mobs";
    public static final String REWARDS = "account_monster_collection_rewards";
    public static final String EXPLORATIONS = "account_monster_collection_explorations";
    /** MariaDB/MySQL ER_NO_SUCH_TABLE. */
    private static final int NO_SUCH_TABLE = 1146;
    private static final AtomicBoolean MISSING_LOGGED = new AtomicBoolean();

    private MonsterCollectionStore() {
    }

    /** One exploration row. */
    public record ExplorationRow(int collectionKey, String monsterKey, long endMillis, int position) {
    }

    /** The saved form of one account's collection. */
    public record Rows(List<Integer> mobs, List<MonsterCollectionReward> rewards, List<ExplorationRow> explorations) {
        public Rows {
            mobs = List.copyOf(mobs);
            rewards = List.copyOf(rewards);
            explorations = List.copyOf(explorations);
        }

        public static Rows of(MonsterCollection mc) {
            Objects.requireNonNull(mc, "mc");
            List<ExplorationRow> explorations = new ArrayList<>();
            for (MonsterCollectionExploration mce : mc.getMonsterCollectionExplorations()) {
                if (mce.getEndDate() != null) {
                    explorations.add(new ExplorationRow(mce.getCollectionKey(), mce.getMonsterKey(),
                            mce.getEndDate().toMillis(), mce.getPosition()));
                }
            }
            explorations.sort((a, b) -> Integer.compare(a.collectionKey(), b.collectionKey()));
            return new Rows(new ArrayList<>(new TreeSet<>(mc.getMobs())), mc.claimedRewards(), explorations);
        }

        public MonsterCollection toCollection() {
            MonsterCollection mc = new MonsterCollection();
            List<MonsterCollectionExploration> list = new ArrayList<>();
            for (ExplorationRow row : explorations) {
                MonsterCollectionExploration mce = new MonsterCollectionExploration(row.collectionKey(),
                        FileTime.fromEpochMillis(row.endMillis()));
                mce.setMonsterKey(row.monsterKey());
                mce.setPosition(row.position());
                list.add(mce);
            }
            mc.restore(mobs, rewards, list);
            return mc;
        }
    }

    /** The account's collection; empty when the account has none or the tables do not exist yet. */
    public static MonsterCollection load(int accountId) {
        if (accountId <= 0) {
            return new MonsterCollection();
        }
        List<Integer> mobs = new ArrayList<>();
        List<MonsterCollectionReward> rewards = new ArrayList<>();
        List<ExplorationRow> explorations = new ArrayList<>();
        try (Connection con = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT mobid FROM " + MOBS + " WHERE accid = ?")) {
                ps.setInt(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        mobs.add(rs.getInt(1));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT region, session, groupid FROM " + REWARDS + " WHERE accid = ?")) {
                ps.setInt(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rewards.add(MonsterCollectionReward.of(rs.getInt(1), rs.getInt(2), rs.getInt(3)));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement("SELECT collectionkey, monsterkey, enddate, position FROM "
                    + EXPLORATIONS + " WHERE accid = ?")) {
                ps.setInt(1, accountId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Timestamp end = rs.getTimestamp(3);
                        if (end != null) {
                            explorations.add(new ExplorationRow(rs.getInt(1), rs.getString(2), end.getTime(), rs.getInt(4)));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            if (!missingTable(e)) {
                System.out.printf("[MonsterCollectionStore] load account %d failed: %s%n", accountId, e);
            }
            return new MonsterCollection();
        }
        return new Rows(mobs, rewards, explorations).toCollection();
    }

    /** Replaces the account's rows with {@code mc}; does nothing while the tables are missing. */
    public static void save(int accountId, MonsterCollection mc) {
        if (accountId <= 0 || mc == null) {
            return;
        }
        Rows rows = Rows.of(mc);
        try (Connection con = DatabaseManager.getConnection()) {
            boolean auto = con.getAutoCommit();
            con.setAutoCommit(false);
            try {
                for (String table : List.of(MOBS, REWARDS, EXPLORATIONS)) {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM " + table + " WHERE accid = ?")) {
                        ps.setInt(1, accountId);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + MOBS + " (accid, mobid) VALUES (?, ?)")) {
                    for (int mob : rows.mobs()) {
                        ps.setInt(1, accountId);
                        ps.setInt(2, mob);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO " + REWARDS + " (accid, region, session, groupid) VALUES (?, ?, ?, ?)")) {
                    for (MonsterCollectionReward r : rows.rewards()) {
                        ps.setInt(1, accountId);
                        ps.setInt(2, r.getRegion());
                        ps.setInt(3, r.getSession());
                        ps.setInt(4, r.getGroup());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO " + EXPLORATIONS
                        + " (accid, collectionkey, monsterkey, enddate, position) VALUES (?, ?, ?, ?, ?)")) {
                    for (ExplorationRow e : rows.explorations()) {
                        ps.setInt(1, accountId);
                        ps.setInt(2, e.collectionKey());
                        ps.setString(3, e.monsterKey());
                        ps.setTimestamp(4, new Timestamp(e.endMillis()));
                        ps.setInt(5, e.position());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            if (!missingTable(e)) {
                System.out.printf("[MonsterCollectionStore] save account %d failed: %s%n", accountId, e);
            }
        }
    }

    private static boolean missingTable(SQLException e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException s && s.getErrorCode() == NO_SUCH_TABLE) {
                if (MISSING_LOGGED.compareAndSet(false, true)) {
                    System.out.println("[MonsterCollectionStore] tables missing (apply sql/tms282/20260924-monster-"
                            + "collection.sql); Monster Collection is not loaded or saved until then");
                }
                return true;
            }
        }
        return false;
    }
}
