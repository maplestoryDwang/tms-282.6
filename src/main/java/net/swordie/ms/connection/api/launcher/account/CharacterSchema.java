package net.swordie.ms.connection.api.launcher.account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The optional parts of the characters table the launcher reads. {@code characters.deletionStartTime} exists in 265
 * (and 265_test), but a database built from sql\AIO.sql alone lacks it: then the query does not select it and every
 * character reads {@code deleting=false}. {@link #detect} asks information_schema once per process.
 */
public final class CharacterSchema {

    /** Where a reader gets the schema (tests inject a fixed one). */
    @FunctionalInterface
    public interface Source {
        CharacterSchema schema(Connection con) throws SQLException;
    }

    private static volatile CharacterSchema detected;

    private final boolean deletionStartTime;

    private CharacterSchema(boolean deletionStartTime) {
        this.deletionStartTime = deletionStartTime;
    }

    public static CharacterSchema of(boolean hasDeletionStartTime) {
        return new CharacterSchema(hasDeletionStartTime);
    }

    /** The connected database's schema, looked up on the first call of this process. */
    public static CharacterSchema detect(Connection con) throws SQLException {
        CharacterSchema s = detected;
        if (s == null) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'characters' AND COLUMN_NAME = 'deletionStartTime'");
                 ResultSet rs = ps.executeQuery()) {
                s = new CharacterSchema(rs.next() && rs.getInt(1) > 0);
            }
            detected = s;
        }
        return s;
    }

    public boolean hasDeletionStartTime() {
        return deletionStartTime;
    }

    /**
     * The caller's characters: users -> accounts (one per world) -> characters -> avatardata -> characterstats, only
     * where {@code accounts.userid} is the session's user (the one parameter).
     */
    String query() {
        return "SELECT c.id, c.orderid, a.worldid, cs.id AS statid, cs.name, cs.level, cs.job, cs.subjob, cs.lastlogout"
                + (deletionStartTime ? ", c.deletionStartTime" : "")
                + " FROM accounts a"
                + " JOIN characters c ON c.accid = a.id"
                + " LEFT JOIN avatardata ad ON ad.id = c.avatardata"
                + " LEFT JOIN characterstats cs ON cs.id = ad.characterstat"
                + " WHERE a.userid = ?"
                + " ORDER BY a.worldid, c.orderid, c.id";
    }

    @Override
    public String toString() {
        return "CharacterSchema[deletionStartTime=" + deletionStartTime + "]";
    }
}
