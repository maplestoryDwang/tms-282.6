package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;

/**
 * Accounts the game server holds in memory. A CP0074 login keeps the whole users row (password and pic included) in
 * its Client from character select on - users.clientstate stays 0 until the character enters a channel - and the
 * disconnect saves it back ({@code User.saveToSQL -> updateUserToSQL} writes password and pic). A password written
 * behind such a copy is undone when that client leaves, so password writes check this first.
 * <p>
 * The CLI cannot see server memory ({@link #none()}); that is why {@link ResetAction} is SERVER_WHEN_RUNNING.
 */
public interface OnlineAccounts {

    /** Some client of this process (login server, channel, cash shop) holds the account's User. */
    boolean online(int userId);

    /**
     * After a committed password change: a copy that appeared meanwhile (a CP0074 login racing the change with the
     * old password) takes the new hash, so its save on disconnect does not restore the old one.
     */
    void passwordChanged(int userId, String hash);

    /**
     * Every in-memory copy of the account's User (P7 changePassword / changePin lock them, write the row and update
     * them together, so a save of a copy cannot write the old secret back). Empty without server memory.
     */
    default java.util.List<User> copies(int userId) {
        return java.util.List.of();
    }

    /** HttpAuthApi: the clients of the running server. */
    static OnlineAccounts live() {
        return Live.INSTANCE;
    }

    /** CLI and tests: no server memory. */
    static OnlineAccounts none() {
        return None.INSTANCE;
    }

    enum Live implements OnlineAccounts {
        INSTANCE;

        @Override
        public boolean online(int userId) {
            return Server.get().getUserById(userId) != null;
        }

        @Override
        public void passwordChanged(int userId, String hash) {
            for (Client c : Server.get().getClients()) {
                User u = c.getUser();
                if (u != null && u.getId() == userId) {
                    u.setPassword(hash);
                }
            }
        }

        @Override
        public java.util.List<User> copies(int userId) {
            java.util.Set<User> out = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Client c : Server.get().getClients()) {
                User u = c.getUser();
                if (u != null && u.getId() == userId) {
                    out.add(u);
                }
            }
            User cached = Server.get().getUserById(userId);
            if (cached != null) {
                out.add(cached);
            }
            return java.util.List.copyOf(out);
        }
    }

    enum None implements OnlineAccounts {
        INSTANCE;

        @Override
        public boolean online(int userId) {
            return false;
        }

        @Override
        public void passwordChanged(int userId, String hash) {
        }
    }
}
