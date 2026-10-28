package net.swordie.ms.connection.api.launcher.account;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.GameSettings;
import net.swordie.ms.connection.api.launcher.LauncherCodes;
import net.swordie.ms.connection.api.launcher.LauncherContext;
import net.swordie.ms.connection.api.launcher.LauncherResult;
import net.swordie.ms.connection.api.launcher.LoginAction;
import net.swordie.ms.connection.api.launcher.ReadAction;
import net.swordie.ms.connection.api.launcher.ServerMemory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code status} (public): whether the server runs, how many are online (a number only - never a name), channel
 * loads, login.maintenance, the protocol and the uptime.
 * <p>
 * A running maintenance countdown ({@link net.swordie.ms.lifecycle.MaintenanceCountdown}: a planned restart - Deploy.ps1's
 * {@code countdown=<seconds>} stop file or the admin console) refuses new logins the way maintenance does, so the
 * launcher is told the same (2026-10-08): {@code maintenance.on} with 「伺服器即將維修（約 N 分鐘後），暫停登入」, and
 * {@code restart.at} = the moment it reaches zero unless the admin announced a restart of his own. The launcher shows
 * 維護中 from then on and keeps it while the server is down after {@code at}.
 * <ul>
 *     <li>In the server process ({@link ServerMemory#available()}): {@code online} = users held by some client
 *     ({@code Server.getUsers().size()}), {@code channels} = each channel's characters, {@code source:"server"}.</li>
 *     <li>CLI (8600 unreachable): no server memory, {@code source:"cli"}; the login port decides {@code running} -
 *     not running: {@code online:0, uptimeSec:0}; running but 8600 unreachable: {@code online:null, uptimeSec:null}
 *     (unknown).</li>
 * </ul>
 * The maintenance message is clipped like login's ({@link LoginAction#MAX_MESSAGE}) and sent only while maintenance is
 * on.
 */
public final class StatusAction extends ReadAction {
    /** The client protocol this server speaks, as the launcher shows it. */
    public static final String PROTOCOL = net.swordie.ms.connection.packet.Tms282ClientBuild.protocol();

    @Override
    public String name() {
        return "status";
    }

    @Override
    public LauncherResult execute(LauncherContext ctx, JsonNode request) {
        ServerMemory memory = ctx.services().memory();
        LauncherResult result = LauncherResult.ok(LauncherCodes.OK);
        Long uptime;
        String source;
        if (memory.available()) {
            result.with("running", true).with("online", memory.onlineUsers()).with("channels", channels(memory.channels()));
            uptime = memory.uptimeSec();
            source = "server";
        } else {
            boolean running = ctx.services().serverProbe().running();
            result.with("running", running).with("online", running ? null : 0).with("channels", List.of());
            uptime = running ? null : 0L;
            source = "cli";
        }
        net.swordie.ms.lifecycle.MaintenanceCountdown countdown = net.swordie.ms.lifecycle.MaintenanceCountdown.standard();
        boolean counting = countdown.active();
        boolean maintenance = GameSettings.maintenance() || counting;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("on", maintenance);
        m.put("message", maintenance ? LoginAction.clip(GameSettings.maintenanceMessage()) : "");
        // 1.2.8: a planned restart announced from the admin console ({at, message}), null when none;
        // 2026-10-08: else the end of a running maintenance countdown
        long now = System.currentTimeMillis();
        Map<String, Object> restart = net.swordie.ms.connection.api.launcher.LauncherNotices.restartMap(now);
        if (restart == null && counting) {
            restart = new LinkedHashMap<>();
            restart.put("at", now + countdown.remainingSec() * 1000L);
            restart.put("message", "");
        }
        return result.with("maintenance", m).with("protocol", PROTOCOL).with("uptimeSec", uptime).with("source", source)
                .with("restart", restart);
    }

    private static List<Map<String, Object>> channels(List<ServerMemory.ChannelLoad> loads) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ServerMemory.ChannelLoad load : loads) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", load.id());
            c.put("name", load.name());
            c.put("online", load.online());
            out.add(c);
        }
        return out;
    }
}
