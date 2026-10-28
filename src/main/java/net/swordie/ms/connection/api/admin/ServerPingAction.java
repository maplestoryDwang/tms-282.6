package net.swordie.ms.connection.api.admin;

import com.fasterxml.jackson.databind.JsonNode;
import net.swordie.ms.connection.api.launcher.LauncherResult;

/** {@code server.ping}: the console's connectivity check (server process id, uptime, port, working directory). */
final class ServerPingAction implements AdminAction {
    @Override
    public String name() {
        return "server.ping";
    }

    @Override
    public boolean mutating() {
        return false;
    }

    @Override
    public LauncherResult execute(AdminContext ctx, JsonNode request) {
        AdminEnvironment env = ctx.env();
        return LauncherResult.ok(AdminCodes.OK)
                .with("admin", 1)
                .with("pid", ProcessHandle.current().pid())
                .with("uptimeSec", env.server().uptimeSec())
                .with("apiPort", env.apiPort())
                .with("root", env.root().toString())
                .with("now", env.now());
    }
}
