package net.swordie.ms.connection.api.admin;

import net.swordie.ms.connection.api.launcher.LauncherServices;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * The collaborators of the admin actions: the launcher API's live services (sessions, limits, back-off, news,
 * rewards - the same instances the launchers use, so the console sees and changes the server's own state), the
 * request statistics, the game server, the working directory (config\, data\) and the API's own port.
 */
public record AdminEnvironment(LauncherServices services, ApiTraffic traffic, AdminServerView server, Path root,
                               int apiPort, LongSupplier clock) {
    public AdminEnvironment {
        Objects.requireNonNull(services, "services");
        Objects.requireNonNull(traffic, "traffic");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(clock, "clock");
    }

    /** A path below the working directory ({@code config/server.properties}, {@code data/launcher/news.json}). */
    public Path resolve(String first, String... more) {
        return root.resolve(Path.of(first, more));
    }

    public long now() {
        return clock.getAsLong();
    }
}
