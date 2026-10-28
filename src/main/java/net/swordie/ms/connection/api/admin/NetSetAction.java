package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code net.set} {baseSha256, values: {key: value}} (連線安全): changes net.allow, net.maxPerIp, net.login.idleSec
 * and/or api.http.allow in config/server.properties - every value validated first ({@link NetSettings}: complete
 * parse, 127.0.0.1 must stay allowed, numbers in range; a key forced by a -D option is refused because the file
 * would not apply), only those lines changed, a timestamped backup kept ({@link ServerPropertiesEditor}). Then the
 * server's existing hot reload picks them up (HotProperties is told to look again now; api.http.allow through
 * ServerRuntimeConfig.reload), and the answer carries the effective values afterwards.
 */
final class NetSetAction implements AdminAction {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public String name() {
        return "net.set";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        JsonNode values = request.get("values");
        Map<String, String> m = new LinkedHashMap<>();
        if (values != null && values.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = values.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                m.put(e.getKey(), e.getValue().asText());
            }
        }
        return m.toString();
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        JsonNode values = request.get("values");
        if (values == null || !values.isObject() || values.isEmpty()) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "values");
        }
        Map<String, String> updates = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = values.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            String key = e.getKey();
            if (!NetSettings.KEYS.contains(key)) {
                return LauncherResult.fail(AdminCodes.REJECTED).with("key", key).with("problem", "unknownKey");
            }
            if (!e.getValue().isTextual() && !e.getValue().isIntegralNumber()) {
                return LauncherResult.fail(AdminCodes.REJECTED).with("key", key).with("problem", "type");
            }
            String v = e.getValue().asText();
            String problem = NetSettings.problem(key, v);
            if (problem != null) {
                return LauncherResult.fail(AdminCodes.REJECTED).with("key", key).with("problem", problem);
            }
            if (NetSettings.jvmOverride(key)) {
                return LauncherResult.fail(AdminCodes.REJECTED).with("key", key).with("problem", "jvmOverride");
            }
            updates.put(key, NetSettings.normalize(key, v));
        }
        ServerPropertiesEditor editor = new ServerPropertiesEditor(ctx.env().resolve("config", "server.properties"));
        ServerPropertiesEditor.Result r = editor.update(updates, AdminParams.text(request, "baseSha256"),
                LocalDateTime.now().format(STAMP));
        if (!AdminCodes.OK.equals(r.code())) {
            return LauncherResult.fail(r.code()).with("problem", r.problem()).with("sha256", r.sha256());
        }
        NetSettings.reload(updates.containsKey(NetSettings.HTTP_ALLOW));
        Map<String, Object> effective = new LinkedHashMap<>();
        for (String k : NetSettings.KEYS) {
            effective.put(k, NetSettings.effective(k));
        }
        return LauncherResult.ok(AdminCodes.OK)
                .with("written", updates)
                .with("effective", effective)
                .with("sha256", r.sha256())
                .with("backup", r.backup().toString());
    }
}
