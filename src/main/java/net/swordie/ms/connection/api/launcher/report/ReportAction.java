package net.swordie.ms.connection.api.launcher.report;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.api.admin.IssueCapture;
import net.swordie.ms.connection.api.launcher.LauncherAction;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherRateLimits;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LauncherSession;
import net.swordie.ms.connection.api.launcher.LauncherTransport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code report} (1.2.8, session) {text, client, log}: the launcher's "回報問題" button. {@code text} is what the
 * player typed (1..{@link ReportStore#MAX_TEXT}), {@code client} a small object the launcher fills (version, mode,
 * game state, last exit; at most {@link ReportStore#MAX_CLIENT} characters serialized), {@code log} the tail of the
 * launcher's own log (at most {@link ReportStore#MAX_LOG}; it never holds a password). The server adds the source IP,
 * the account's characters that are online right now and their skill problems of the last 30 minutes
 * ({@link IssueCapture}), and stores it ({@link ReportStore}) for the admin console. Answers {@code ok} with the
 * report {@code id}. Bucket {@link LauncherRateLimits#GIFT}; runs anywhere (the CLI just has no live part).
 */
public final class ReportAction implements LauncherAction {
    private static final long ISSUE_WINDOW_MS = 30 * 60_000L;
    private final ReportStore store;

    public ReportAction() {
        this(ReportStore.standard());
    }

    public ReportAction(ReportStore store) {
        this.store = store;
    }

    @Override
    public String name() {
        return "report";
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
        return LauncherRateLimits.GIFT;
    }

    @Override
    public boolean remoteAllowed() {
        return true;
    }

    @Override
    public LauncherTransport transport() {
        return LauncherTransport.ANY;
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) throws Exception {
        LauncherSession session = ctx.session();
        if (session == null) {
            return LauncherResult.fail(LauncherCodes.NEED_SESSION);
        }
        JsonNode text = request == null ? null : request.get("text");
        if (text == null || !text.isTextual() || text.textValue().isBlank() || text.textValue().length() > ReportStore.MAX_TEXT) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "text");
        }
        JsonNode client = request.get("client");
        if (client != null && !client.isNull() && (!client.isObject() || client.toString().length() > ReportStore.MAX_CLIENT)) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "client");
        }
        JsonNode log = request.get("log");
        if (log != null && !log.isNull() && (!log.isTextual() || log.textValue().length() > ReportStore.MAX_LOG)) {
            return LauncherResult.fail(LauncherCodes.INVALID_INPUT).with("problem", "log");
        }
        long now = System.currentTimeMillis();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("ip", ctx.sourceIp());
        report.put("launcherVersion", ctx.launcherVersion());
        report.put("text", text.textValue().strip());
        report.put("client", client == null || client.isNull() ? Map.of() : client);
        List<Map<String, Object>> characters = new ArrayList<>();
        List<Map<String, Object>> issues = new ArrayList<>();
        if (ctx.services().memory().available()) {
            live(session.userId(), now, report, characters, issues);
        }
        report.put("characters", characters);
        report.put("issues", issues);
        report.put("log", log == null || log.isNull() ? "" : log.textValue());
        return LauncherResult.ok(LauncherCodes.OK).with("id", store.save(now, session.userId(), report));
    }

    /** The account's online characters and what {@link IssueCapture} grouped for them lately. */
    private static void live(int userId, long now, Map<String, Object> report, List<Map<String, Object>> characters,
                             List<Map<String, Object>> issues) {
        var user = Server.get().getUserById(userId);
        if (user != null) {
            report.put("username", user.getName());
        }
        IssueCapture capture = IssueCapture.current();
        for (Char c : new ArrayList<>(Server.get().getChars().values())) {
            if (c == null || c.getUser() == null || c.getUser().getId() != userId) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("job", c.getJob());
            m.put("level", c.getLevel());
            m.put("field", c.getFieldID());
            characters.add(m);
            if (capture == null) {
                continue;
            }
            for (IssueCapture.Issue i : capture.snapshot(now - ISSUE_WINDOW_MS, c.getId())) {
                if (issues.size() >= 60) {
                    break;
                }
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("kind", i.kind());
                row.put("subject", i.subject());
                row.put("character", i.character());
                row.put("skill", i.skill());
                row.put("reason", i.reason());
                row.put("count", i.count());
                row.put("last", i.lastMs());
                row.put("sample", i.sample());
                issues.add(row);
            }
        }
    }
}
