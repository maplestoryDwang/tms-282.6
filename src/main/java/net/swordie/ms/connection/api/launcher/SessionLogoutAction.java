package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

/** {@code session.logout}: drops the caller's own session (Authorization: Bearer); other sessions stay. */
public final class SessionLogoutAction implements LauncherAction {

    @Override
    public String name() {
        return "session.logout";
    }

    @Override
    public boolean requiresSession() {
        return true;
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String rateBucket() {
        return LauncherRateLimits.SESSION;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    /** Sessions exist only in the server process. */
    @Override
    public LauncherTransport transport() {
        return LauncherTransport.SERVER_ONLY;
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) {
        ctx.services().sessions().revoke(ctx.session());
        return LauncherResult.ok(LauncherCodes.LOGGED_OUT);
    }
}
