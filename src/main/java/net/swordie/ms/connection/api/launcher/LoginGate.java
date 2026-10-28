package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.GameSettings;
import net.swordie.ms.client.User;
import net.swordie.ms.enums.AccountType;
import net.swordie.ms.world.tms282.audit.BanService;

/**
 * The refusals of the game login (CP0074, {@code Tms282AuthHandlers.authenticate}) that v2 login must mirror, in the
 * same order: IP ban before the password check; after a correct password users.banExpireDate (read with the row) or
 * an {@code account_block} ban; then {@code login.maintenance}.
 * <p>
 * BanService only loads its cache once the DB pool is up: call {@link #ipBanned} / {@link #accountBanned} after
 * {@code DatabaseManager.getConnection()}, otherwise a CLI process silently sees "not banned".
 */
public interface LoginGate {

    boolean ipBanned(String sourceIp);

    /** A login-blocking {@code account_block} row ({@code accountId = users.id}). */
    boolean accountBanned(int userId);

    /** {@code login.maintenance} is on and the account type is below {@code login.maintenance.minAccountType}. */
    boolean refusedByMaintenance(AccountType accountType);

    String maintenanceMessage();

    static LoginGate live() {
        return Live.INSTANCE;
    }

    /** BanService + GameSettings, exactly what CP0074 calls. */
    enum Live implements LoginGate {
        INSTANCE;

        @Override
        public boolean ipBanned(String sourceIp) {
            return BanService.isIpBanned(sourceIp);
        }

        @Override
        public boolean accountBanned(int userId) {
            return BanService.activeBan(userId).isPresent();
        }

        @Override
        public boolean refusedByMaintenance(AccountType accountType) {
            User probe = new User();
            probe.setAccountType(accountType);
            return GameSettings.refusedByMaintenance(probe);
        }

        @Override
        public String maintenanceMessage() {
            return GameSettings.maintenanceMessage();
        }
    }
}
