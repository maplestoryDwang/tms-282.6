package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;

/**
 * A read-only view of the caller's own account (template method): needs a session, checks the local database guard
 * before any connection ({@code localOnly}), then reads with one connection. The user is always the session's:
 * {@link #read} gets {@code userId} from {@link LauncherContext#session()} and nothing else, so a {@code userId} (or a
 * username) in the request can never select another account.
 */
public abstract class SessionReadAction extends ReadAction {

    @Override
    public final boolean requiresSession() {
        return true;
    }

    @Override
    public final LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception {
        LauncherSession session = ctx.session();
        if (session == null) {
            // The SESSION policy attaches it; a registry without that policy must not leak anyone's data.
            return LauncherResult.fail(LauncherCodes.NEED_SESSION);
        }
        if (!LocalDatabaseGuard.accepted()) {
            return LauncherResult.fail(LauncherCodes.LOCAL_ONLY);
        }
        try (Connection con = DatabaseManager.getConnection()) {
            return read(ctx, request, con, session.userId());
        }
    }

    /** Reads {@code userId}'s data (the session's user) with {@code con}; SELECT only. */
    protected abstract LauncherResult read(LauncherContext ctx, JsonNode request, Connection con, int userId)
            throws Exception;
}
