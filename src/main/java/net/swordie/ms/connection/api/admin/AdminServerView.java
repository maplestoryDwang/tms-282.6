package net.swordie.ms.connection.api.admin;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.connection.api.ApiAcceptor;
import net.swordie.ms.lifecycle.ServerShutdown;
import net.swordie.ms.util.HotProperties;
import net.swordie.ms.world.Channel;
import net.swordie.ms.world.World;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * What the admin actions read from (or do to) the running game server, behind one interface so the endpoint can be
 * driven without a game world (checks). {@link #live()} only calls methods the formal jar already has.
 */
public interface AdminServerView {

    /** The headless start command the console shows (it never starts the server itself). */
    String START_COMMAND = "powershell -NoProfile -ExecutionPolicy Bypass -File D:\\TMS\\launcher\\Start-CompatibleTest.ps1 -ServerOnly";

    /** Users held by some client (login server, channels, cash shop). */
    int onlineUsers();

    /** Characters in the game world. */
    int onlineCharacters();

    long uptimeSec();

    /** Disconnects every client of {@code userId} (the in-game !tempban path: Client.close saves); returns how many. */
    int disconnectUser(int userId);

    int loginPort();

    /** Ports of the world's channels, in order (empty while the world is not up). */
    List<Integer> channelPorts();

    boolean legacyApiEnabled();

    int legacyApiPort();

    boolean shutdownInProgress();

    /** The stop file the watcher polls ({@code server.stopFile}, default shutdown.request in the working directory). */
    Path stopFile();

    /** The stop-file watcher thread runs in this process. */
    boolean stopWatcherRunning();

    /** Seconds left of a running maintenance countdown (MaintenanceCountdown), 0 when none runs. */
    default int countdownRemainingSec() {
        return net.swordie.ms.lifecycle.MaintenanceCountdown.standard().remainingSec();
    }

    /** Sends a world announcement (notice.send, notice-1005) once per kind; returns the characters online. */
    default int sendNotice(List<net.swordie.ms.connection.packet.Tms282Notice.Kind> kinds, String text) {
        return net.swordie.ms.connection.packet.Tms282Notice.broadcast(kinds, text);
    }

    static AdminServerView live() {
        return Live.INSTANCE;
    }

    enum Live implements AdminServerView {
        INSTANCE;

        @Override
        public int onlineUsers() {
            return Server.get().getUsers().size();
        }

        @Override
        public int onlineCharacters() {
            return Server.get().getChars().size();
        }

        @Override
        public long uptimeSec() {
            LocalDateTime start = Server.startTime;
            return start == null ? 0 : Math.max(0, Duration.between(start, LocalDateTime.now()).toSeconds());
        }

        @Override
        public int disconnectUser(int userId) {
            int n = 0;
            for (Client c : new ArrayList<>(Server.get().getClients())) {
                User u = c == null ? null : c.getUser();
                if (u != null && u.getId() == userId) {
                    c.close();
                    n++;
                }
            }
            return n;
        }

        @Override
        public int loginPort() {
            return ServerConstants.LOGIN_PORT;
        }

        @Override
        public List<Integer> channelPorts() {
            List<Integer> out = new ArrayList<>();
            World world = Server.get().getWorld();
            if (world == null || world.getChannels() == null) {
                return out;
            }
            for (Channel ch : new ArrayList<>(world.getChannels())) {
                if (ch != null) {
                    out.add(ch.getPort());
                }
            }
            return out;
        }

        @Override
        public boolean legacyApiEnabled() {
            return ApiAcceptor.enabled();
        }

        @Override
        public int legacyApiPort() {
            return ServerConstants.API_PORT;
        }

        @Override
        public boolean shutdownInProgress() {
            return ServerShutdown.inProgress();
        }

        @Override
        public Path stopFile() {
            return Path.of(HotProperties.server().string(net.swordie.ms.lifecycle.StopFileWatcher.KEY,
                    net.swordie.ms.lifecycle.StopFileWatcher.DEFAULT_FILE));
        }

        @Override
        public boolean stopWatcherRunning() {
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("StopFileWatcher".equals(t.getName()) && t.isAlive()) {
                    return true;
                }
            }
            return false;
        }
    }
}
