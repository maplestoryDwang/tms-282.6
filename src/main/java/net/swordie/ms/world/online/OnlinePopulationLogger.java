package net.swordie.ms.world.online;

import net.swordie.ms.Server;
import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.handlers.Timer;
import net.swordie.ms.world.World;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 每 10 分鐘（對齊整 10 分）將線上人數附加到 logs/online/online-YYYY-MM-DD.csv，
 * 格式同 MetaMS 273 data/Online：標頭 {@code hour,minute,online}，每列 {@code 4,10,0}。
 * 時區使用 {@link ServerConfig#ZONE}。
 */
public final class OnlinePopulationLogger {

    public static final Path LOG_DIR = Path.of(ServerConstants.DIR, "logs", "online");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final long PERIOD_MS = 10 * 60_000L;
    private static volatile boolean started;

    private OnlinePopulationLogger() {
    }

    public static synchronized void start(Timer timer) {
        if (started || timer == null) {
            return;
        }
        started = true;
        long now = System.currentTimeMillis();
        long delay = PERIOD_MS - (now % PERIOD_MS) + 2_000L; // 對齊到下一個整 10 分鐘 (UTC 對齊，時區皆為整點偏移)
        timer.addFixedRateEvent(OnlinePopulationLogger::safeLog, delay, PERIOD_MS, TimeUnit.MILLISECONDS, true);
    }

    /** 各頻道線上角色總數（排除機器人與沒有連線的殘留角色）。 */
    public static int countOnline() {
        World world = Server.get().getWorld();
        if (world == null) {
            return 0;
        }
        int count = 0;
        for (Char chr : Server.get().getChars().values()) {
            if (chr == null || chr.isBot()) {
                continue;
            }
            Client c = chr.getClient();
            if (c == null) {
                continue;
            }
            count++;
        }
        return count;
    }

    private static void safeLog() {
        try {
            log();
        } catch (Throwable t) {
            System.err.println("[OnlineLog] 寫入線上人數失敗: " + t);
        }
    }

    public static synchronized void log() throws IOException {
        ZonedDateTime now = ZonedDateTime.now(ServerConfig.ZONE);
        int online = countOnline();
        Files.createDirectories(LOG_DIR);
        Path file = LOG_DIR.resolve("online-" + now.format(DATE) + ".csv");
        StringBuilder sb = new StringBuilder();
        if (!Files.exists(file) || Files.size(file) == 0) {
            sb.append("hour,minute,online\n");
        }
        // 273 格式分鐘為 0,10,20...；四捨去到整 10 分避免 timer 抖動造成 1、11...
        int minute = now.getMinute() / 10 * 10;
        sb.append(now.getHour()).append(',').append(minute).append(',').append(online).append('\n');
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
    }
}
