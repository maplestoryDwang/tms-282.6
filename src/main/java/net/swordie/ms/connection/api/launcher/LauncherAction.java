package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One launcher command ({@code {"action": name, ...}}), shared by LocalAccountCli and HttpAuthApi through
 * {@link LauncherActionRegistry}. The declarations below are enforced centrally by {@link LauncherPolicies}, so an
 * action only implements its own logic.
 */
public interface LauncherAction {

    /** The {@code action} value; unique within a registry. */
    String name();

    /**
     * Needs the caller's session ({@link SessionSource}: a bearer token on {@code /api/v2/launcher}, the request's own
     * username + password on the CLI); {@link LauncherContext#session()} is then set.
     */
    boolean requiresSession();

    /** Writes account data (informational for monitoring; write-specific policies build on it in later phases). */
    boolean mutating();

    /** The {@link LauncherRateLimits} bucket charged per source IP on {@code /api/v2/launcher}. */
    String rateBucket();

    /** May be called from a non-loopback source (Radmin friends); false = only 127.0.0.1 over HTTP. */
    boolean remoteAllowed();

    /** Whether the CLI may run it; see {@link LauncherTransport}. */
    LauncherTransport transport();

    /**
     * Runs the action after every policy passed. Never logs secrets; returns a result instead of throwing for every
     * expected outcome (an exception becomes {@code unavailable}).
     */
    LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception;
}
