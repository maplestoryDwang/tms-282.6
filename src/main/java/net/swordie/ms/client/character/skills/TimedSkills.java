package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Skills granted with an expiry date (mount/ride items of the MetaMS pack: {@code changeSkillLevel(id, 1, expireMs)}).
 * Skill rows carry no expiry, so the deadline lives in its own table and is enforced by {@link #removeExpired}.
 */
public final class TimedSkills {
    private static final Logger LOG = Logger.getLogger(TimedSkills.class.getName());
    private static volatile boolean ready;

    private TimedSkills() { }

    private static synchronized void ensure() throws SQLException {
        if (ready) return;
        try (Connection c = DatabaseManager.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS character_timed_skills (charid INT NOT NULL, skillid INT NOT NULL,"
                    + " expire BIGINT NOT NULL, PRIMARY KEY(charid, skillid)) ENGINE=InnoDB");
        }
        ready = true;
    }

    private static long expireOf(int charId, int skillId) throws SQLException {
        ensure();
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT expire FROM character_timed_skills WHERE charid=? AND skillid=?")) {
            p.setInt(1, charId); p.setInt(2, skillId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getLong(1) : 0; }
        }
    }

    /**
     * Records the deadline for a skill the caller is about to grant. A skill the character already owns permanently
     * stays permanent; an existing deadline is only ever extended.
     *
     * @return false when the skill is already owned permanently (nothing recorded)
     */
    public static boolean recordGrant(Char chr, int skillId, long expireMs) {
        try {
            long current = expireOf(chr.getId(), skillId);
            if (current == 0 && chr.hasSkill(skillId)) {
                return false;
            }
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                    "REPLACE INTO character_timed_skills (charid, skillid, expire) VALUES (?,?,?)")) {
                p.setInt(1, chr.getId()); p.setInt(2, skillId); p.setLong(3, Math.max(current, expireMs));
                p.executeUpdate();
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to record timed skill " + skillId + " for char " + chr.getId(), e);
        }
        return true;
    }

    /** Forgets a deadline, e.g. when the skill is granted permanently or removed. */
    public static void clear(Char chr, int skillId) {
        try {
            ensure();
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                    "DELETE FROM character_timed_skills WHERE charid=? AND skillid=?")) {
                p.setInt(1, chr.getId()); p.setInt(2, skillId);
                p.executeUpdate();
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to clear timed skill " + skillId + " for char " + chr.getId(), e);
        }
    }

    /** Removes every skill of this character whose deadline has passed. */
    public static void removeExpired(Char chr) {
        // Runs on every field entry; the package checks forbid real DB access (-Dtms282.tests.rejectRealDatabase).
        if (Boolean.getBoolean("tms282.tests.rejectRealDatabase")) {
            return;
        }
        List<Integer> expired = new ArrayList<>();
        try {
            ensure();
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement p = c.prepareStatement(
                    "SELECT skillid FROM character_timed_skills WHERE charid=? AND expire<=?")) {
                p.setInt(1, chr.getId()); p.setLong(2, System.currentTimeMillis());
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) expired.add(r.getInt(1));
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Failed to read timed skills for char " + chr.getId(), e);
            return;
        }
        for (int skillId : expired) {
            chr.removeSkillAndSendPacket(skillId);
            clear(chr, skillId);
        }
        if (!expired.isEmpty()) {
            chr.chatMessage("有 " + expired.size() + " 個限時技能已到期並移除。");
        }
    }
}
