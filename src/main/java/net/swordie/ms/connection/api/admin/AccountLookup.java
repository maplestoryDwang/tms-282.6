package net.swordie.ms.connection.api.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Finds one users row for the admin console (SELECT only; never password, pic or email): by numeric id, by account
 * name, or by the name of one of its characters - in that order, the first that matches.
 */
final class AccountLookup {
    private static final Pattern ID = Pattern.compile("[0-9]{1,10}");
    static final int MAX_QUERY = 40;

    /** One account as the console shows it. {@code matchedBy}: id, name or character. */
    record UserRow(int id, String name, Long createdAt, int vipGrade, int characterSlots, int accountType,
                   Long banExpireAt, String banReason, String matchedBy) {
        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", id);
            m.put("name", name);
            m.put("createdAt", createdAt);
            m.put("vipGrade", vipGrade);
            m.put("characterSlots", characterSlots);
            m.put("accountType", accountType);
            m.put("banExpireAt", banExpireAt);
            m.put("banReason", banReason);
            m.put("matchedBy", matchedBy);
            return m;
        }
    }

    private AccountLookup() {
    }

    /** The trimmed query when it is usable (1..40 characters, no control characters), else null. */
    static String clean(String query) {
        if (query == null) {
            return null;
        }
        String q = query.strip();
        if (q.isEmpty() || q.length() > MAX_QUERY || q.chars().anyMatch(Character::isISOControl)) {
            return null;
        }
        return q;
    }

    static Optional<UserRow> resolve(Connection con, String query) throws SQLException {
        String q = clean(query);
        if (q == null) {
            return Optional.empty();
        }
        if (ID.matcher(q).matches()) {
            long id = Long.parseLong(q);
            if (id <= Integer.MAX_VALUE) {
                Optional<UserRow> r = byId(con, (int) id, "id");
                if (r.isPresent()) {
                    return r;
                }
            }
        }
        Optional<UserRow> r = one(con, "SELECT " + COLUMNS + " FROM users WHERE name = ? ORDER BY id LIMIT 1", q, "name");
        if (r.isPresent()) {
            return r;
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT a.userid FROM characterstats cs"
                + " JOIN avatardata ad ON ad.characterstat = cs.id"
                + " JOIN characters c ON c.avatardata = ad.id"
                + " JOIN accounts a ON a.id = c.accid"
                + " WHERE cs.name = ? ORDER BY c.id LIMIT 1")) {
            ps.setString(1, q);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return byId(con, rs.getInt(1), "character");
                }
            }
        }
        return Optional.empty();
    }

    static Optional<UserRow> byId(Connection con, int id, String matchedBy) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE id = ?")) {
            ps.setInt(1, id);
            return read(ps, matchedBy);
        }
    }

    private static final String COLUMNS = "id, name, creationdate, vipgrade, characterslots, accounttype, banExpireDate, banReason";

    private static Optional<UserRow> one(Connection con, String sql, String arg, String matchedBy) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, arg);
            return read(ps, matchedBy);
        }
    }

    private static Optional<UserRow> read(PreparedStatement ps, String matchedBy) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            String name = rs.getString("name");
            return Optional.of(new UserRow(rs.getInt("id"), name == null ? "" : name, millis(rs.getTimestamp("creationdate")),
                    rs.getInt("vipgrade"), rs.getInt("characterslots"), rs.getInt("accounttype"),
                    millis(rs.getTimestamp("banExpireDate")), rs.getString("banReason"), matchedBy));
        }
    }

    static Long millis(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        long ms = ts.getTime();
        return ms > 0 ? ms : null;
    }
}
