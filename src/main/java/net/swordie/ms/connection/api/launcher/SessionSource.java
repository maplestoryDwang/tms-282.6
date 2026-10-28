package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Where a channel finds the caller of a {@code requiresSession} action (strategy of {@link LauncherPolicies#SESSION},
 * chosen by {@link LauncherChannel#sessionSource()}). Each either attaches the caller's {@link LauncherSession} to the
 * context and answers null, or answers the refusal. Actions read the user only from that session: a {@code userId}
 * (or any other identity field) in the request is never consulted.
 */
public enum SessionSource {

    /** {@code /api/v2/account} (cat-launcher 1.0.0): no sessions at all. */
    NONE {
        @Override
        LauncherResult resolve(LauncherContext ctx, JsonNode request) {
            return LauncherResult.fail(LauncherCodes.NEED_SESSION);
        }
    },

    /**
     * {@code /api/v2/launcher}: {@code Authorization: Bearer <token>} issued by login, bound to the source IP it was
     * issued to; the account and the IP must not have been banned since (a ban revokes every session of the account).
     */
    BEARER {
        @Override
        LauncherResult resolve(LauncherContext ctx, JsonNode request) {
            LauncherSessions sessions = ctx.services().sessions();
            LauncherSessions.Lookup lookup = sessions.lookup(ctx.bearerToken(), ctx.sourceIp());
            switch (lookup.status()) {
                case MISSING, WRONG_SOURCE -> {
                    return LauncherResult.fail(LauncherCodes.NEED_SESSION);
                }
                case EXPIRED -> {
                    return LauncherResult.fail(LauncherCodes.SESSION_EXPIRED);
                }
                default -> {
                    int userId = lookup.session().userId();
                    LoginGate gate = ctx.services().loginGate();
                    if (gate.ipBanned(ctx.sourceIp()) || gate.accountBanned(userId)) {
                        sessions.revokeAll(userId);
                        return LauncherResult.fail(LauncherCodes.BANNED);
                    }
                    ctx.attach(lookup.session());
                    return null;
                }
            }
        }
    },

    /**
     * LocalAccountCli (one request per JVM, nothing outlives it): the request's own {@code username} + {@code password},
     * checked exactly like login ({@link AccountAuthenticator}: IP ban, password, ban, maintenance - the CP0074 order),
     * become a session for this request only ({@link LauncherSession#transientFor}). No or malformed credentials:
     * {@code needSession}; the database guard runs before any connection ({@code localOnly}).
     */
    CREDENTIALS {
        @Override
        LauncherResult resolve(LauncherContext ctx, JsonNode request) throws Exception {
            AccountCredentials credentials = AccountCredentials.parse(request);
            if (credentials == null) {
                return LauncherResult.fail(LauncherCodes.NEED_SESSION);
            }
            if (!LocalDatabaseGuard.accepted()) {
                return LauncherResult.fail(LauncherCodes.LOCAL_ONLY);
            }
            AccountAuthenticator.Outcome outcome = AccountAuthenticator.authenticate(ctx, credentials);
            if (!outcome.accepted()) {
                return outcome.refusal();
            }
            ctx.attach(LauncherSession.transientFor(outcome.userId(), ctx.sourceIp(), System.currentTimeMillis()));
            return null;
        }
    };

    /** Attaches the caller's session to {@code ctx} and answers null, or answers the refusal. */
    abstract LauncherResult resolve(LauncherContext ctx, JsonNode request) throws Exception;
}
