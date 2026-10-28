package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.lifecycle.MaintenanceCountdown;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * {@code server.cancelShutdown} {confirm: "cancel"} (取消倒數): deletes the stop file of a running maintenance
 * countdown; the StopFileWatcher sees it gone within a second, cancels the countdown and announces 「維修取消」.
 * Refused ({@code noCountdown}) when no countdown runs - an immediate stop already in progress cannot be cancelled.
 */
final class ServerShutdownCancelAction implements AdminAction {
    @Override
    public String name() {
        return "server.cancelShutdown";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        return "cancel countdown";
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        if (!"cancel".equals(AdminParams.text(request, "confirm"))) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "confirm");
        }
        AdminServerView server = ctx.env().server();
        int left = server.countdownRemainingSec();
        Path stop = server.stopFile().toAbsolutePath().normalize();
        if (left <= 0 || server.shutdownInProgress()) {
            return LauncherResult.fail(AdminCodes.NO_COUNTDOWN);
        }
        String first = Files.exists(stop, LinkOption.NOFOLLOW_LINKS) ? Files.readString(stop).strip() : "";
        Integer sec = MaintenanceCountdown.parse(first.isEmpty() ? null : first.lines().findFirst().orElse(""));
        if (sec == null || sec < 0) {
            return LauncherResult.fail(AdminCodes.NO_COUNTDOWN);
        }
        Files.deleteIfExists(stop);
        return LauncherResult.ok(AdminCodes.OK).with("cancelledWithSec", left).with("stopFile", stop.toString());
    }
}
