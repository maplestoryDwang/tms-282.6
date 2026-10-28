package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.client.User;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * P7: writes a new password or second password (pin) of an account that the game server may hold in memory.
 * <ol>
 *     <li>Reads the row and checks the current password with BCrypt without any lock (slow).</li>
 *     <li>Takes the monitor of every in-memory {@link User} copy ({@link OnlineAccounts#copies}, a stable order),
 *     then in one transaction locks the row ({@code FOR UPDATE}), checks that password / pic are still the values
 *     checked in step 1, writes the new hash, sets it on every copy, commits. {@code User.updateUserToSQL} is
 *     synchronized on the copy, so a save either ran before (and is overwritten) or runs after and writes the new
 *     hash: an online account can never get its old secret back.</li>
 * </ol>
 * The monitors are taken before any row lock, and nothing else is locked inside, so a save thread (which holds a
 * copy's monitor and then needs the row) cannot deadlock with this.
 */
final class AccountSecretWriter {
    enum Field {PASSWORD, PIN}

    /** The row as read in step 1. */
    record Row(int id, String name, String password, String pin) {
        @Override
        public String toString() {
            return "Row[id=" + id + ", name=" + name + "]";
        }
    }

    enum Outcome {WRITTEN, CHANGED_MEANWHILE, GONE}

    private AccountSecretWriter() {
    }

    static Row read(Connection con, int userId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id, name, password, pic FROM users WHERE id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Row(rs.getInt("id"), rs.getString("name"), rs.getString("password"), rs.getString("pic")) : null;
            }
        }
    }

    /** Step 2: {@code hash} becomes the new value of {@code field} in the row and in every copy. */
    static Outcome write(OnlineAccounts online, Row checked, Field field, String hash) throws SQLException {
        List<User> copies = new ArrayList<>(online.copies(checked.id()));
        copies.sort(Comparator.comparingInt(System::identityHashCode));
        BiConsumer<User, String> set = field == Field.PASSWORD ? User::setPassword : User::setPic;
        return locked(copies, 0, () -> {
            try (Connection con = DatabaseManager.getConnection()) {
                con.setAutoCommit(false);
                try {
                    Row now;
                    try (PreparedStatement ps = con.prepareStatement("SELECT id, name, password, pic FROM users WHERE id = ? FOR UPDATE")) {
                        ps.setInt(1, checked.id());
                        try (ResultSet rs = ps.executeQuery()) {
                            now = rs.next() ? new Row(rs.getInt("id"), rs.getString("name"), rs.getString("password"), rs.getString("pic")) : null;
                        }
                    }
                    if (now == null) {
                        con.rollback();
                        return Outcome.GONE;
                    }
                    if (!same(now.password(), checked.password()) || !same(now.pin(), checked.pin())) {
                        con.rollback();
                        return Outcome.CHANGED_MEANWHILE;
                    }
                    try (PreparedStatement ps = con.prepareStatement(field == Field.PASSWORD
                            ? "UPDATE users SET password = ? WHERE id = ?" : "UPDATE users SET pic = ? WHERE id = ?")) {
                        ps.setString(1, hash);
                        ps.setInt(2, checked.id());
                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("Account changed");
                        }
                    }
                    for (User u : copies) {
                        set.accept(u, hash);
                    }
                    con.commit();
                    return Outcome.WRITTEN;
                } catch (SQLException | RuntimeException e) {
                    con.rollback();
                    // the copies may hold the new hash although the row does not: put the checked value back
                    for (User u : copies) {
                        set.accept(u, field == Field.PASSWORD ? checked.password() : checked.pin());
                    }
                    throw e;
                } finally {
                    con.setAutoCommit(true);
                }
            }
        });
    }

    interface Body {
        Outcome run() throws SQLException;
    }

    private static Outcome locked(List<User> copies, int i, Body body) throws SQLException {
        if (i == copies.size()) {
            return body.run();
        }
        synchronized (copies.get(i)) {
            return locked(copies, i + 1, body);
        }
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
