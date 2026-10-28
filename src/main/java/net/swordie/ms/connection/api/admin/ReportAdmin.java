package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherNotices;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.report.ReportStore;

/**
 * Launcher 1.2.8 from the console's side: {@code ops.restartNotice} (the planned restart the launcher shows) and the
 * players' problem reports ({@code ops.reports}, {@code ops.report}).
 */
public final class ReportAdmin {
    private ReportAdmin() {
    }

    /**
     * {@code ops.restartNotice} {minutes 0..600, text}: announces a restart {@code minutes} from now with {@code text}
     * (may be empty); {@code minutes} absent or {@code clear:true} withdraws it. Answers the current notice
     * ({@code restart: {at, message}} or null). Reading: call it with {@code read:true} (not audited as a change).
     */
    public static final class RestartNotice implements AdminAction {
        @Override
        public String name() {
            return "ops.restartNotice";
        }

        @Override
        public boolean mutating() {
            return true;
        }

        @Override
        public String auditSummary(JsonNode request) {
            if (request != null && request.path("read").asBoolean()) {
                return "read";
            }
            return request != null && request.path("clear").asBoolean() ? "clear" : "minutes=" + AdminParams.text(request, "minutes");
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) {
            long now = ctx.env().now();
            if (request != null && request.path("read").asBoolean()) {
                return LauncherResult.ok(AdminCodes.OK).with("restart", LauncherNotices.restartMap(now)).with("now", now);
            }
            if (request != null && request.path("clear").asBoolean()) {
                LauncherNotices.clear();
                return LauncherResult.ok(AdminCodes.OK).with("restart", null).with("now", now);
            }
            Long minutes = AdminParams.number(request, "minutes", 0, 600);
            if (minutes == null) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "minutes");
            }
            String text = AdminParams.text(request, "text");
            if (text != null && text.length() > LauncherNotices.MAX_MESSAGE) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "text");
            }
            LauncherNotices.announce(now + minutes * 60_000L, text);
            return LauncherResult.ok(AdminCodes.OK).with("restart", LauncherNotices.restartMap(now)).with("now", now);
        }
    }

    /** {@code ops.reports} {limit 1..200, default 50}: the newest problem reports as summaries. */
    public static final class Reports implements AdminAction {
        @Override
        public String name() {
            return "ops.reports";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
            Long limit = AdminParams.number(request, "limit", 1, 200);
            return LauncherResult.ok(AdminCodes.OK).with("reports", ReportStore.standard().list(limit == null ? 50 : limit.intValue()))
                    .with("now", ctx.env().now());
        }
    }

    /** {@code ops.report} {id}: one report as stored (text, client, characters, issues, log). */
    public static final class Report implements AdminAction {
        @Override
        public String name() {
            return "ops.report";
        }

        @Override
        public boolean mutating() {
            return false;
        }

        @Override
        public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
            JsonNode report = ReportStore.standard().read(AdminParams.text(request, "id"));
            if (report == null) {
                return LauncherResult.fail(AdminCodes.NOT_FOUND);
            }
            return LauncherResult.ok(AdminCodes.OK).with("report", report);
        }
    }
}
