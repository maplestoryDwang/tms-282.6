package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.console.config.ServerRuntimeConfig;

/**
 * Account actions only touch the local game database: db.host 127.0.0.1 and db.name 265. The test database 265_test
 * is accepted only when the JVM runs with {@code -Dtms.launcher.testDb=true}. Checked before any connection is made
 * ({@code localOnly}).
 */
public final class LocalDatabaseGuard {
    public static final String TEST_DB_PROPERTY = "tms.launcher.testDb";
    public static final String LIVE_DB = "265";
    public static final String TEST_DB = "265_test";

    private LocalDatabaseGuard() {
    }

    /** The current config/server.properties of the working directory. */
    public static boolean accepted() {
        return accepted(ServerRuntimeConfig.stringValue("db.host"), ServerRuntimeConfig.stringValue("db.name"),
                Boolean.getBoolean(TEST_DB_PROPERTY));
    }

    public static boolean accepted(String host, String name, boolean testDbAllowed) {
        if (!"127.0.0.1".equals(host)) {
            return false;
        }
        return LIVE_DB.equals(name) || (testDbAllowed && TEST_DB.equals(name));
    }
}
