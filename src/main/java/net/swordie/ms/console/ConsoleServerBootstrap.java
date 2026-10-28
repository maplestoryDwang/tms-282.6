package net.swordie.ms.console;

import net.swordie.ms.Server;
import net.swordie.ms.util.FastStartup;

public final class ConsoleServerBootstrap {
    private ConsoleServerBootstrap() {
    }

    public static void run() {
        ConsoleDebugMode.load();
        PacketLogFilter.load();
        FastStartup.load();
        Server.main(new String[0]);
    }
}
