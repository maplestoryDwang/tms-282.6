package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One step of the registry pipeline (chain of responsibility): a refusal, or null to continue. {@code request} is the
 * whole request object: the CLI's session step reads its username / password ({@link SessionSource#CREDENTIALS}).
 */
@FunctionalInterface
public interface LauncherPolicy {
    LauncherResult check(LauncherAction action, LauncherContext ctx, JsonNode request) throws Exception;
}
