package net.swordie.ms.connection.api.launcher;

import net.swordie.ms.console.config.ServerRuntimeConfig;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Whether the game server is running on this machine (CLI defence for SERVER_WHEN_RUNNING actions). */
@FunctionalInterface
public interface GameServerProbe {
    int CONNECT_TIMEOUT_MS = 500;
    /** login.port when config/server.properties does not set it. */
    int DEFAULT_LOGIN_PORT = 8484;

    boolean running();

    /** Something accepts TCP connections on 127.0.0.1:{@code port}. */
    static GameServerProbe loopback(int port) {
        return () -> {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}), port),
                        CONNECT_TIMEOUT_MS);
                return true;
            } catch (IOException e) {
                return false;
            }
        };
    }

    /** 127.0.0.1:{@code login.port} (8484), read from the working directory's config at each probe. */
    static GameServerProbe loginPort() {
        return () -> loopback(configuredLoginPort()).running();
    }

    static int configuredLoginPort() {
        return ServerRuntimeConfig.intValue("login.port", DEFAULT_LOGIN_PORT);
    }
}
