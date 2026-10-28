package net.swordie.ms.client.daily;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

/**
 * Dates the daily gift claims for the launcher's stamp card: {@link DailyGift#claim} calls {@link #claimed} after the
 * claim is saved and the reward granted, and one row goes to {@code dailygift_log} ({@link DailyGiftLogStore}).
 * <p>
 * The log is advisory and must never change a claim: {@link #claimed} never throws (the caller also wraps it), the
 * write happens after the grant (a failure can neither grant twice nor withhold the reward), and
 * <ul>
 *     <li>switch {@value #SWITCH_KEY} in config/server.properties (default true; {@code -D} wins), re-read at most
 *     every {@value #SWITCH_CHECK_MS} ms, so it can be turned off without a restart;</li>
 *     <li>a missing table (not created on this database yet) pauses the writes for {@value #PAUSE_MS} ms (one log
 *     line); any other failure logs at most one line per {@value #PAUSE_MS} ms - never an account or item.</li>
 * </ul>
 * The write is synchronous (the claim already saves dailygift synchronously under the same account lock): nothing is
 * queued, so nothing is lost on shutdown.
 */
public final class DailyGiftClaimLog {
    public static final String SWITCH_KEY = "launcher.dailyGiftLog";
    static final long SWITCH_CHECK_MS = 5_000;
    static final long PAUSE_MS = 10 * 60_000L;

    /** What one {@link #record} did. */
    public enum Outcome {
        WRITTEN, DISABLED, PAUSED, MISSING_TABLE, FAILED
    }

    /** Where an entry goes (the database; checks inject failing or recording sinks). */
    @FunctionalInterface
    public interface Sink {
        void write(DailyGiftLogStore.Entry entry) throws SQLException;
    }

    private static volatile DailyGiftClaimLog standard;

    private final Sink sink;
    private final BooleanSupplier enabled;
    private final LongSupplier clock;
    private volatile long pausedUntil = Long.MIN_VALUE;
    private volatile long nextErrorLine = Long.MIN_VALUE;
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public DailyGiftClaimLog(Sink sink, BooleanSupplier enabled, LongSupplier clock) {
        this.sink = Objects.requireNonNull(sink, "sink");
        this.enabled = Objects.requireNonNull(enabled, "enabled");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** The game's instance: the pool's database, the config switch, the real clock. */
    public static DailyGiftClaimLog standard() {
        DailyGiftClaimLog s = standard;
        if (s == null) {
            synchronized (DailyGiftClaimLog.class) {
                s = standard;
                if (s == null) {
                    s = new DailyGiftClaimLog(DailyGiftClaimLog::database, new ConfigSwitch(ConfigSwitch.CONFIG_FILE, System::currentTimeMillis),
                            System::currentTimeMillis);
                    standard = s;
                }
            }
        }
        return s;
    }

    /**
     * {@link DailyGift#claim}: {@code accountId} claimed reward day {@code dayIndex} ({@code count} x {@code itemId}) on
     * the day of {@code giftDate} (the record's date after the rollover). Never throws.
     */
    public static void claimed(int accountId, long giftDate, int dayIndex, int itemId, int count, String source) {
        try {
            standard().record(accountId, giftDate, dayIndex, itemId, count, source);
        } catch (Throwable ignored) {
            // advisory only
        }
    }

    /** Writes one entry unless switched off or paused; failures are counted and reported, never thrown. */
    public Outcome record(int accountId, long giftDate, int dayIndex, int itemId, int count, String source) {
        long now = clock.getAsLong();
        try {
            if (!enabled.getAsBoolean()) {
                return Outcome.DISABLED;
            }
        } catch (RuntimeException e) {
            return Outcome.DISABLED;
        }
        if (now < pausedUntil) {
            return Outcome.PAUSED;
        }
        try {
            LocalDate day = Instant.ofEpochMilli(giftDate).atZone(ZoneId.systemDefault()).toLocalDate();
            sink.write(new DailyGiftLogStore.Entry(accountId, day, dayIndex, itemId, count, source == null ? "" : source, now));
            written.incrementAndGet();
            return Outcome.WRITTEN;
        } catch (SQLException e) {
            failed.incrementAndGet();
            if (DailyGiftLogStore.missingTable(e)) {
                pausedUntil = now + PAUSE_MS;
                System.out.println("[DailyGiftLog] table dailygift_log does not exist: claims are not dated for the next 10 min"
                        + " (create it with sql/tms282/20261001-dailygift-log.sql)");
                return Outcome.MISSING_TABLE;
            }
            report(now, e.getClass().getSimpleName() + " SQLState " + e.getSQLState());
            return Outcome.FAILED;
        } catch (RuntimeException e) {
            failed.incrementAndGet();
            report(now, e.getClass().getSimpleName());
            return Outcome.FAILED;
        }
    }

    private void report(long now, String what) {
        if (now >= nextErrorLine) {
            nextErrorLine = now + PAUSE_MS;
            System.out.println("[DailyGiftLog] write failed (" + what + "); the claim itself is unaffected; next report in 10 min at the earliest");
        }
    }

    /** Entries written since start (monitoring / checks). */
    public long written() {
        return written.get();
    }

    /** Failed writes since start, a missing table included. */
    public long failed() {
        return failed.get();
    }

    /** Writes resume now (checks; console "retry"). */
    public void resume() {
        pausedUntil = Long.MIN_VALUE;
    }

    private static void database(DailyGiftLogStore.Entry entry) throws SQLException {
        try (Connection con = DatabaseManager.getConnection()) {
            DailyGiftLogStore.insert(con, entry);
        }
    }

    /**
     * {@value #SWITCH_KEY} from a properties file, re-read when its modification time or size changed (checked at most
     * every {@value #SWITCH_CHECK_MS} ms). {@code -Dlauncher.dailyGiftLog=...} wins; absent = on; {@code false},
     * {@code off}, {@code no}, {@code 0} = off.
     */
    public static final class ConfigSwitch implements BooleanSupplier {
        static final Path CONFIG_FILE = Path.of("config", "server.properties");

        private final Path file;
        private final LongSupplier clock;
        private long lastCheck = Long.MIN_VALUE;
        private String lastKey = "";
        private boolean value = true;

        public ConfigSwitch(Path file, LongSupplier clock) {
            this.file = Objects.requireNonNull(file, "file");
            this.clock = Objects.requireNonNull(clock, "clock");
        }

        @Override
        public synchronized boolean getAsBoolean() {
            String forced = System.getProperty(SWITCH_KEY);
            if (forced != null) {
                return on(forced);
            }
            long now = clock.getAsLong();
            if (lastCheck != Long.MIN_VALUE && now - lastCheck < SWITCH_CHECK_MS) {
                return value;
            }
            lastCheck = now;
            String key;
            try {
                BasicFileAttributes a = Files.readAttributes(file, BasicFileAttributes.class);
                key = a.lastModifiedTime().toMillis() + ":" + a.size();
            } catch (IOException e) {
                key = "missing";
            }
            if (!key.equals(lastKey)) {
                lastKey = key;
                value = read();
            }
            return value;
        }

        private boolean read() {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException | IllegalArgumentException e) {
                return true;
            }
            String v = p.getProperty(SWITCH_KEY);
            return v == null || on(v);
        }

        static boolean on(String v) {
            String s = v.trim().toLowerCase(java.util.Locale.ROOT);
            return !(s.equals("false") || s.equals("off") || s.equals("no") || s.equals("0"));
        }
    }
}
