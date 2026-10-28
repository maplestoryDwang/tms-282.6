package net.swordie.ms.connection.api.launcher;

import com.fasterxml.jackson.databind.JsonNode;

/** Request field access with the CLI's long-standing rule: a missing or non-string field reads as "". */
public final class LauncherJson {
    private LauncherJson() {
    }

    public static String text(JsonNode request, String key) {
        JsonNode value = request == null ? null : request.get(key);
        return value != null && value.isTextual() ? value.textValue() : "";
    }
}
