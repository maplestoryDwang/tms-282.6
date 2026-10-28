package net.swordie.ms.connection.api.launcher;

/**
 * Base of the read-only launcher views (status, me, characters, dashboard, dailyRewards, dailyStatus, news): never
 * writes, may run anywhere
 * ({@link LauncherTransport#ANY}: over HTTP, or through the CLI when 8600 cannot be reached), may be called by remote
 * launchers, and is charged to the {@link LauncherRateLimits#READ} bucket. Public by default; see
 * {@link SessionReadAction} for the per-account ones.
 */
public abstract class ReadAction implements LauncherAction {

    @Override
    public boolean requiresSession() {
        return false;
    }

    /** A view only reads: database SELECTs and server memory, nothing is written back. */
    @Override
    public final boolean mutating() {
        return false;
    }

    @Override
    public final String rateBucket() {
        return LauncherRateLimits.READ;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    @Override
    public final LauncherTransport transport() {
        return LauncherTransport.ANY;
    }
}
