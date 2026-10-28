package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * {@code server.shutdown} {confirm: "shutdown", expectOnline, countdownSec?} (優雅停機): writes the stop file the server's own
 * StopFileWatcher polls every second - the same graceful stop Deploy.ps1 uses: refuse new connections, save and
 * disconnect everyone, wait for the queued saves, exit 0. Refused when the online count is no longer the one the
 * console showed in its confirmation ({@code onlineChanged}), when a stop is already pending or running, or when no
 * watcher runs in this process. There is no restart: starting the server stays with Start-CompatibleTest.ps1.
 * <p>
 * {@code countdownSec} 1..3600 (2026-10-05 維修倒數): the stop file starts with {@code countdown=<n>} - announcements,
 * new logins refused, graceful stop at zero (MaintenanceCountdown); {@code server.cancelShutdown} cancels it.
 */
final class ServerShutdownAction implements AdminAction {
    @Override
    public String name() {
        return "server.shutdown";
    }

    @Override
    public boolean mutating() {
        return true;
    }

    @Override
    public String auditSummary(JsonNode request) {
        return "expectOnline=" + AdminParams.number(request, "expectOnline", 0, 1_000_000)
                + (AdminParams.has(request, "countdownSec") ? " countdownSec=" + AdminParams.number(request, "countdownSec", 0, 3600) : "");
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) throws Exception {
        if (!"shutdown".equals(AdminParams.text(request, "confirm"))) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "confirm");
        }
        Long expect = AdminParams.number(request, "expectOnline", 0, 1_000_000);
        if (expect == null) {
            return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "expectOnline");
        }
        long countdown = 0;
        if (AdminParams.has(request, "countdownSec")) {
            Long c = AdminParams.number(request, "countdownSec", 0, net.swordie.ms.lifecycle.MaintenanceCountdown.MAX_SEC);
            if (c == null) {
                return LauncherResult.fail(AdminCodes.INVALID_INPUT).with("problem", "countdownSec");
            }
            countdown = c;
        }
        AdminServerView server = ctx.env().server();
        int online = server.onlineUsers();
        if (online != expect) {
            return LauncherResult.fail(AdminCodes.ONLINE_CHANGED).with("onlineUsers", online);
        }
        if (server.shutdownInProgress()) {
            return LauncherResult.fail(AdminCodes.ALREADY_STOPPING);
        }
        if (!server.stopWatcherRunning()) {
            return LauncherResult.fail(AdminCodes.NO_STOP_WATCHER);
        }
        Path stop = server.stopFile().toAbsolutePath().normalize();
        String note = (countdown > 0 ? "countdown=" + countdown + " " : "")
                + "AdminConsole " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " online=" + online;
        try {
            Files.writeString(stop, note, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException e) {
            return LauncherResult.fail(AdminCodes.ALREADY_STOPPING);
        }
        return LauncherResult.ok(AdminCodes.OK)
                .with("stopFile", stop.toString())
                .with("pid", ProcessHandle.current().pid())
                .with("onlineUsers", online)
                .with("countdownSec", countdown);
    }
}
