package net.swordie.ms.connection.api.launcher.account;

import net.swordie.ms.connection.api.launcher.LauncherServices;
import net.swordie.ms.connection.api.launcher.ServerMemory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * One users row as an {@link AccountView}: only name, creationdate, vipgrade and characterslots are selected (never
 * password, pic or email). While the account is online the server's User wins for vipGrade / characterSlots (they may
 * have changed in game and not been saved yet).
 */
public final class AccountReader {

    /** The account of {@code userId}; empty when the row is gone. */
    public Optional<AccountView> read(Connection con, int userId, LauncherServices services) throws SQLException {
        String name;
        Long createdAt;
        int vipGrade;
        int slots;
        try (PreparedStatement ps = con.prepareStatement("SELECT name, creationdate, vipgrade, characterslots FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                name = rs.getString("name");
                createdAt = epochMillis(rs.getTimestamp("creationdate"));
                vipGrade = rs.getInt("vipgrade");
                slots = rs.getInt("characterslots");
            }
        }
        Optional<ServerMemory.UserLive> live = services.memory().onlineUser(userId);
        if (live.isPresent()) {
            vipGrade = live.get().vipGrade();
            slots = live.get().characterSlots();
        }
        boolean online = live.isPresent() || services.onlineAccounts().online(userId);
        return Optional.of(new AccountView(name == null ? "" : name, createdAt, vipGrade, slots, online));
    }

    /** Epoch milliseconds, or null for NULL and FileTime-zero style placeholders (anything before 1970). */
    static Long epochMillis(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        long ms = ts.getTime();
        return ms > 0 ? ms : null;
    }
}
