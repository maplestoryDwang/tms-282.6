package net.swordie.ms.world.tms282.mail;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * A character that can receive mail, looked up by name (online or offline).
 *
 * @param characterId characters.id
 * @param name        characterstats.name (the stored spelling)
 * @param accountId   characters.accid (world account, accounts.id)
 * @param userId      accounts.userid (login user)
 * @param worldId     accounts.worldid
 */
public record MailRecipient(int characterId, String name, int accountId, int userId, int worldId) {

    static Optional<MailRecipient> byName(Connection c, String name) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT cs.characterid, cs.name, ch.accid, a.userid, a.worldid FROM characterstats cs "
                        + "JOIN characters ch ON ch.id = cs.characterid "
                        + "JOIN accounts a ON a.id = ch.accid WHERE cs.name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                MailRecipient r = new MailRecipient(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5));
                return rs.next() ? Optional.empty() : Optional.of(r); // ambiguous names are refused
            }
        }
    }

    static Optional<MailRecipient> byId(Connection c, int characterId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT cs.characterid, cs.name, ch.accid, a.userid, a.worldid FROM characterstats cs "
                        + "JOIN characters ch ON ch.id = cs.characterid "
                        + "JOIN accounts a ON a.id = ch.accid WHERE cs.characterid = ?")) {
            ps.setInt(1, characterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new MailRecipient(rs.getInt(1), rs.getString(2), rs.getInt(3),
                        rs.getInt(4), rs.getInt(5))) : Optional.empty();
            }
        }
    }

    /** The recipient's live Char when online on this server. */
    Optional<Char> online() {
        return Optional.ofNullable(Server.get().getChars().get(characterId));
    }

    static int userIdOf(Char chr) {
        return chr.getUser() != null ? chr.getUser().getId() : 0;
    }

    static int worldIdOf(Char chr) {
        return chr.getAccount() != null ? chr.getAccount().getWorldId() : -1;
    }
}
