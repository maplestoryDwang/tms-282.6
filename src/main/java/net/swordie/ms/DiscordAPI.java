package net.swordie.ms;

/** Discord integration is disabled in the local-only login package. */
public final class DiscordAPI {
    public static final String mainGuildServer = "";
    public static final String staffGuildServer = "";

    private DiscordAPI() {
    }

    public static void start() {
        // Local-only: no external client or credentials are loaded.
    }

    public static void send(String name, String message, String server) {
        // Local-only: no outbound messages.
    }
}
