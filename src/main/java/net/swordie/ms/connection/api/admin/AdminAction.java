package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;

/**
 * One admin console command ({@code {"action": name, ...}} on {@code POST /api/v2/admin}), registered in
 * {@link AdminActionRegistry} (command pattern, like the launcher's {@code LauncherAction}). The endpoint has already
 * checked the source (127.0.0.1), Host, Origin and the admin token; an action only implements its own logic.
 */
public interface AdminAction {

    /** The {@code action} value, e.g. {@code account.find}; unique within a registry. */
    String name();

    /** Changes something (a file, the database, server memory): every call is written to the audit log line. */
    boolean mutating();

    /**
     * Parameters of a mutating call as they may appear in the audit log (never a password or token); "" when there
     * is nothing to add.
     */
    default String auditSummary(JsonNode request) {
        return "";
    }

    /** Runs the action; an expected refusal is a result, an exception becomes {@code unavailable}. */
    LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception;
}
