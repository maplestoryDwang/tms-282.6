package net.swordie.ms.connection.api.launcher.account;

import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.loaders.StringData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What the launcher's character panel shows besides level and job (1.2.8), so "why does this character have no
 * blessing / link skill" can be read off the launcher:
 * <ul>
 *     <li>{@code link}: the link skill this character hands out - {@code skill}, {@code name} (null when the string
 *     data is not loaded, e.g. the CLI), {@code level} 0..3 by character level (70 / 120 / 210; Zero from 110),
 *     {@code needLevel} while 0, {@code row} (a linkskills row exists) and {@code to} (the character it is linked to,
 *     or null). Absent for a job without a link skill.</li>
 *     <li>{@code fairy} / {@code empress}: the blessing source as the game picks it ({@code Char.initBlessingSkillNames}:
 *     the highest other character of the same world at level 10+ / the highest other Cygnus Knight or Mihile at 5+)
 *     - {@code from}, {@code fromLevel}, {@code level} (min 20, level / 10; min 30, level / 5). Null when there is no
 *     source.</li>
 * </ul>
 * Read-only: one SELECT on linkskills; nothing here creates a row.
 */
public final class CharacterExtras {
    private CharacterExtras() {
    }

    /** The same characters with {@link CharacterView#extras()} filled; the list unchanged when linkskills cannot be read. */
    public static List<CharacterView> apply(Connection con, int userId, List<CharacterView> characters) {
        Map<Integer, int[]> rows;
        try {
            rows = con == null ? null : linkRows(con, userId);
        } catch (SQLException e) {
            rows = null;
        }
        Map<Integer, String> names = new HashMap<>();
        for (CharacterView c : characters) {
            names.put(c.id(), c.name());
        }
        List<CharacterView> out = new ArrayList<>();
        for (CharacterView c : characters) {
            Map<String, Object> extras = new LinkedHashMap<>();
            try {
                Map<String, Object> link = link(c, rows, names);
                if (link != null) {
                    extras.put("link", link);
                }
                extras.put("fairy", blessing(c, characters, false));
                extras.put("empress", blessing(c, characters, true));
            } catch (RuntimeException e) {
                extras.clear(); // an unknown job id: this character is listed without the extras
            }
            out.add(c.withExtras(extras));
        }
        return out;
    }

    /** ownerid -> {linkskillid, level, linkedcharid} over the user's world accounts. */
    static Map<Integer, int[]> linkRows(Connection con, int userId) throws SQLException {
        Map<Integer, int[]> out = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT l.ownerid, l.linkskillid, l.level, l.linkedcharid FROM linkskills l"
                + " JOIN accounts a ON a.id = l.accid WHERE a.userid = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getInt(1), new int[]{rs.getInt(2), rs.getInt(3), rs.getInt(4)});
                }
            }
        }
        return out;
    }

    static Map<String, Object> link(CharacterView c, Map<Integer, int[]> rows, Map<Integer, String> names) {
        int skill = SkillConstants.getLinkSkillByJob((short) c.job());
        int[] row = rows == null ? null : rows.get(c.id());
        if (skill == 0 && row != null) {
            skill = row[0];
        }
        if (skill == 0) {
            return null;
        }
        boolean zero = JobConstants.isZero((short) c.job());
        int need = zero ? 110 : SkillConstants.LINK_SKILL_1_LEVEL;
        int level = c.level() < need ? 0 : Math.max(1, SkillConstants.getLinkSkillLevelByCharLevel((short) c.level(), (short) c.job()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("skill", skill);
        m.put("name", skillName(skill));
        m.put("level", level);
        m.put("needLevel", level == 0 ? need : null);
        m.put("row", rows == null ? null : row != null);
        m.put("to", row == null || row[2] == 0 ? null : names.getOrDefault(row[2], "#" + row[2]));
        return m;
    }

    static Map<String, Object> blessing(CharacterView c, List<CharacterView> all, boolean empress) {
        CharacterView best = null;
        for (CharacterView o : all) {
            if (o.id() == c.id() || o.world() != c.world() || o.level() < (empress ? 5 : 10)) {
                continue;
            }
            if (empress && !(JobConstants.isCygnusKnight((short) o.job()) || JobConstants.isMihile((short) o.job()))) {
                continue;
            }
            if (best == null || o.level() > best.level()) {
                best = o;
            }
        }
        if (best == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", best.name());
        m.put("fromLevel", best.level());
        m.put("level", empress ? Math.min(30, best.level() / 5) : Math.min(20, best.level() / 10));
        return m;
    }

    /** The skill's name from the loaded string data; null when it is not loaded (CLI) or has no entry. */
    static String skillName(int skill) {
        try {
            var info = StringData.getSkillStringById(skill);
            String name = info == null ? null : info.getName();
            return name == null || name.isBlank() ? null : name;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}
