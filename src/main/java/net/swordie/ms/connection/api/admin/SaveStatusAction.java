package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.lifecycle.PendingSaves;
import net.swordie.ms.lifecycle.PeriodicSaveService;
import net.swordie.ms.lifecycle.SaveHistory;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code save.status} (存檔／停機): the periodic save (running, enabled, interval, saved / failed since start,
 * characters scheduled), the last periodic saves (character, duration, success), queued saves, online counts, the
 * stop file and whether its watcher runs, the server's process id (the console waits for it to disappear after a
 * stop) and the start command (the console never starts the server).
 */
final class SaveStatusAction implements AdminAction {
    @Override
    public String name() {
        return "save.status";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) {
        AdminServerView server = ctx.env().server();
        PeriodicSaveService p = PeriodicSaveService.running();
        Map<String, Object> periodic = new LinkedHashMap<>();
        periodic.put("running", p != null);
        if (p != null) {
            int[] range = p.intervalSec();
            periodic.put("enabled", p.enabled());
            periodic.put("minSec", range[0]);
            periodic.put("maxSec", range[1]);
            periodic.put("saved", p.savedCount());
            periodic.put("failed", p.failedCount());
            periodic.put("tracked", p.trackedCount());
        }
        List<Map<String, Object>> recent = new ArrayList<>();
        for (SaveHistory.Entry e : SaveHistory.standard().recent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("at", e.at());
            m.put("characterId", e.characterId());
            m.put("name", e.name());
            m.put("millis", e.millis());
            m.put("ok", e.ok());
            recent.add(m);
        }
        var stop = server.stopFile().toAbsolutePath().normalize();
        return LauncherResult.ok(AdminCodes.OK)
                .with("periodic", periodic)
                .with("recent", recent)
                .with("pendingSaves", PendingSaves.pending())
                .with("pendingSummary", PendingSaves.summary())
                .with("onlineUsers", server.onlineUsers())
                .with("onlineCharacters", server.onlineCharacters())
                .with("pid", ProcessHandle.current().pid())
                .with("uptimeSec", server.uptimeSec())
                .with("stopFile", stop.toString())
                .with("stopRequested", Files.exists(stop, LinkOption.NOFOLLOW_LINKS))
                .with("stopWatcher", server.stopWatcherRunning())
                .with("shuttingDown", server.shutdownInProgress())
                .with("countdownSec", server.countdownRemainingSec())
                .with("startCommand", AdminServerView.START_COMMAND);
    }
}
