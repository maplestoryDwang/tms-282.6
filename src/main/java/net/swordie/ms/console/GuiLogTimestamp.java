package net.swordie.ms.console;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class GuiLogTimestamp {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private GuiLogTimestamp() {
    }

    public static String formatNow() {
        return LocalDateTime.now().format(FMT);
    }
}
