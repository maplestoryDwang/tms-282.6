package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * The standard pipeline, in order: transport (CLI), remote source (HTTP), per-action rate bucket (HTTP), session.
 * Each reads only the action's declarations and the context (the session step also the CLI request's credentials), so
 * a new action gets every rule by declaring its flags.
 */
public enum LauncherPolicies implements LauncherPolicy {

    /** CLI only: SERVER_ONLY never runs; SERVER_WHEN_RUNNING refuses while 127.0.0.1:8484 is listening. */
    TRANSPORT {
        @Override
        public LauncherResult check(LauncherAction action, LauncherContext ctx, JsonNode request) {
            if (ctx.channel().http()) {
                return null;
            }
            return switch (action.transport()) {
                case ANY -> null;
                case SERVER_ONLY -> LauncherResult.fail(LauncherCodes.SERVER_ONLY);
                case SERVER_WHEN_RUNNING -> ctx.services().serverProbe().running()
                        ? LauncherResult.fail(LauncherCodes.SERVER_RUNNING) : null;
            };
        }
    },

    /** HTTP: remoteAllowed=false actions only from the host itself. */
    REMOTE_SOURCE {
        @Override
        public LauncherResult check(LauncherAction action, LauncherContext ctx, JsonNode request) {
            if (!ctx.channel().http() || action.remoteAllowed() || ctx.loopbackSource()) {
                return null;
            }
            return LauncherResult.fail(LauncherCodes.REMOTE_FORBIDDEN);
        }
    },

    /**
     * {@code /api/v2/launcher} and {@code /api/v2/account} ({@link LauncherChannel#limitsActions()}): one token of the
     * action's bucket per request and source IP, refused with the channel's rate-limit code.
     */
    RATE_LIMIT {
        @Override
        public LauncherResult check(LauncherAction action, LauncherContext ctx, JsonNode request) {
            if (!ctx.channel().limitsActions()) {
                return null;
            }
            LauncherRateLimits limits = ctx.services().rateLimits();
            if (limits.tryAcquire(action.rateBucket(), ctx.sourceIp())) {
                return null;
            }
            return LauncherResult.fail(ctx.channel().rateLimitedCode())
                    .with("retryAfterSec", seconds(limits.retryAfterMs(action.rateBucket(), ctx.sourceIp())));
        }
    },

    /**
     * requiresSession: the channel's {@link SessionSource} attaches the caller's {@link LauncherSession} or refuses -
     * a live bearer token bound to this source IP on {@code /api/v2/launcher} (account / IP not banned since login: a
     * ban revokes every session of the account), the request's own username + password on the CLI (one request, one
     * JVM), never on {@code /api/v2/account}. Actions read the user only from that session.
     */
    SESSION {
        @Override
        public LauncherResult check(LauncherAction action, LauncherContext ctx, JsonNode request) throws Exception {
            if (!action.requiresSession()) {
                return null;
            }
            return ctx.channel().sessionSource().resolve(ctx, request);
        }
    };

    public static List<LauncherPolicy> standard() {
        return List.of(values());
    }

    static long seconds(long millis) {
        return Math.max(1, (millis + 999) / 1000);
    }
}
