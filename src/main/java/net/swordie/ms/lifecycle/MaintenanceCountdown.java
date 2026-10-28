package net.swordie.ms.lifecycle;

import net.swordie.ms.connection.packet.Tms282Notice;
import net.swordie.ms.util.HotProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 維修倒數 (2026-10-05): a stop file whose first line is {@code countdown=<seconds>} (1..{@value #MAX_SEC}), or the
 * admin API {@code server.shutdown countdownSec}, starts a countdown instead of stopping at once:
 * <ul>
 *   <li>an announcement at the start - the moment new logins start being refused (「…即刻起暫停新登入…」) - and at
 *   the marks of {@value #MARKS_KEY} (default 300,60,30,10 seconds; marks not below the start are skipped), and one
 *   at zero right before the graceful stop ({@value #AT_ZERO_KEY}); {@value #START_KEY} turns the start one off;</li>
 *   <li>new logins are refused while it runs (GM-only like {@code login.maintenance}); players online keep playing;</li>
 *   <li>at zero the {@link StopFileWatcher} runs the usual graceful stop (save everyone, exit);</li>
 *   <li>deleting the stop file (or {@code server.cancelShutdown}) cancels it: 「維修取消」 is announced.</li>
 * </ul>
 * Announcements go through a registry of {@link Announcer}s chosen by {@value #ANNOUNCE_KEY} (hot reload):
 * {@code notice} (default, notice-1005: {@link Tms282Notice} with the kinds of {@value #KINDS_KEY}, falling back to
 * {@code tms282.notice.kinds}, default {@code chat} = LP04F0 type 11, the system-chat line players already see),
 * {@code chat} (that proven chat line only) or {@code none} (log only). The countdown, login refusal and graceful
 * stop work either way. Time is driven by {@link #tick()} (the watcher calls it every second), so tests use their own
 * clock; the schedule is read when a countdown starts.
 */
public final class MaintenanceCountdown {
    public static final String ANNOUNCE_KEY = "tms282.maintenance.announce";
    public static final String KINDS_KEY = "tms282.maintenance.notice.kinds";
    public static final String MARKS_KEY = "tms282.maintenance.announceMarks";
    public static final String START_KEY = "tms282.maintenance.announceStart";
    public static final String AT_ZERO_KEY = "tms282.maintenance.announceAtZero";
    public static final String DEFAULT_ANNOUNCER = "notice";
    public static final String DEFAULT_MARKS = "300,60,30,10";
    public static final int MAX_SEC = 3600;

    /** When to speak: the marks (seconds left) and whether to announce the start and zero. */
    public record Schedule(TreeSet<Integer> marks, boolean atStart, boolean atZero) {
        public Schedule {
            marks = new TreeSet<>(marks);
        }

        public static final Schedule DEFAULT = new Schedule(parseMarks(DEFAULT_MARKS), true, true);

        /** Comma list of seconds (1..MAX_SEC); invalid entries are skipped; an empty list means no marks. */
        public static TreeSet<Integer> parseMarks(String list) {
            TreeSet<Integer> out = new TreeSet<>();
            if (list != null) {
                for (String p : list.split(",")) {
                    try {
                        int v = Integer.parseInt(p.strip());
                        if (v >= 1 && v <= MAX_SEC) out.add(v);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return out;
        }

        static Schedule configured() {
            HotProperties p = HotProperties.server();
            return new Schedule(parseMarks(p.string(MARKS_KEY, DEFAULT_MARKS)), p.bool(START_KEY, true), p.bool(AT_ZERO_KEY, true));
        }
    }

    /** One way to tell everyone online. */
    public interface Announcer {
        String id();

        void announce(String message);
    }

    /** Result of a {@link #tick()}. */
    public enum Tick {IDLE, RUNNING, DUE}

    private static final Map<String, Announcer> REGISTRY;

    static {
        Map<String, Announcer> m = new LinkedHashMap<>();
        for (Announcer a : List.of(
                announcer("notice", msg -> Tms282Notice.broadcast(Tms282Notice.configuredKinds(KINDS_KEY), msg)),
                announcer("chat", msg -> Tms282Notice.broadcast(List.of(Tms282Notice.Kind.CHAT), msg)),
                announcer("none", msg -> {}))) {
            m.put(a.id(), a);
        }
        REGISTRY = Collections.unmodifiableMap(m);
    }

    private static Announcer announcer(String id, java.util.function.Consumer<String> send) {
        return new Announcer() {
            @Override public String id() { return id; }
            @Override public void announce(String message) { send.accept(message); }
        };
    }

    public static Map<String, Announcer> announcers() {
        return REGISTRY;
    }

    private static final MaintenanceCountdown STANDARD = new MaintenanceCountdown(System::currentTimeMillis,
            () -> REGISTRY.getOrDefault(HotProperties.server().string(ANNOUNCE_KEY, DEFAULT_ANNOUNCER).trim(), REGISTRY.get(DEFAULT_ANNOUNCER)),
            Schedule::configured);

    public static MaintenanceCountdown standard() {
        return STANDARD;
    }

    private final LongSupplier clock;
    private final Supplier<Announcer> announcer;
    private final Supplier<Schedule> schedule;
    private boolean announceAtZero;
    private long endAtMs;
    private int startSec;
    private String source = "";
    private final TreeSet<Integer> pendingMarks = new TreeSet<>();

    MaintenanceCountdown(LongSupplier clock, Supplier<Announcer> announcer) {
        this(clock, announcer, () -> Schedule.DEFAULT);
    }

    MaintenanceCountdown(LongSupplier clock, Supplier<Announcer> announcer, Supplier<Schedule> schedule) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.announcer = Objects.requireNonNull(announcer, "announcer");
        this.schedule = Objects.requireNonNull(schedule, "schedule");
    }

    /** Parses {@code countdown=<n>}; null when the line is not a countdown request, -1 when it is one but invalid. */
    public static Integer parse(String firstLine) {
        if (firstLine == null) {
            return null;
        }
        String s = firstLine.strip();
        if (!s.regionMatches(true, 0, "countdown", 0, 9)) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)countdown\\s*=\\s*(\\d{1,5})\\s*(?:\\s.*)?").matcher(s);
        if (!m.matches()) {
            return -1;
        }
        int n = Integer.parseInt(m.group(1));
        return n >= 1 && n <= MAX_SEC ? n : -1;
    }

    /** The announcement text for {@code remaining} seconds. */
    public static String message(int remaining) {
        String when = remaining >= 60 && remaining % 60 == 0 ? remaining / 60 + " 分鐘" : remaining >= 60 ? (remaining + 59) / 60 + " 分鐘" : remaining + " 秒";
        return "伺服器將於 " + when + "後維修，請在安全的地方下線，進度會自動保存";
    }

    /** The start announcement: new logins are refused from now on. */
    public static String startMessage(int seconds) {
        return message(seconds).replace("後維修，", "後維修，即刻起暫停新登入；");
    }

    public static final String CANCELLED = "維修取消，伺服器將繼續運作";
    public static final String AT_ZERO = "伺服器開始維修，正在保存所有角色資料並中斷連線";

    public synchronized boolean active() {
        return endAtMs > 0;
    }

    public synchronized int remainingSec() {
        if (endAtMs <= 0) {
            return 0;
        }
        return (int) Math.max(0, (endAtMs - clock.getAsLong() + 999) / 1000);
    }

    public synchronized String source() {
        return source;
    }

    /** Starts a countdown; false when one already runs or {@code seconds} is out of range. */
    public synchronized boolean start(int seconds, String source) {
        if (endAtMs > 0 || seconds < 1 || seconds > MAX_SEC) {
            return false;
        }
        endAtMs = clock.getAsLong() + seconds * 1000L;
        startSec = seconds;
        this.source = source == null ? "" : source;
        pendingMarks.clear();
        Schedule sch = schedule.get();
        pendingMarks.addAll(sch.marks().headSet(seconds, false));
        announceAtZero = sch.atZero();
        System.out.println("[Shutdown] maintenance countdown " + seconds + " s started (" + this.source + "), marks " + pendingMarks);
        if (sch.atStart()) {
            say(startMessage(seconds));
        }
        return true;
    }

    /** Called every second: announces the marks that were reached; DUE when the time is up (the countdown ends). */
    public synchronized Tick tick() {
        if (endAtMs <= 0) {
            return Tick.IDLE;
        }
        int left = remainingSec();
        if (left <= 0) {
            endAtMs = 0;
            pendingMarks.clear();
            System.out.println("[Shutdown] maintenance countdown reached zero (" + source + ")");
            if (announceAtZero) {
                say(AT_ZERO);
            }
            return Tick.DUE;
        }
        Integer reached = pendingMarks.ceiling(left);
        if (reached != null) {
            // only the latest mark: a late tick never sends two announcements at once
            pendingMarks.tailSet(left, true).clear();
            say(message(reached));
        }
        return Tick.RUNNING;
    }

    /** Cancels a running countdown and announces it; false when none runs. */
    public synchronized boolean cancel(String why) {
        if (endAtMs <= 0) {
            return false;
        }
        endAtMs = 0;
        pendingMarks.clear();
        System.out.println("[Shutdown] maintenance countdown cancelled (" + why + ")");
        say(CANCELLED);
        return true;
    }

    private void say(String message) {
        Announcer a = announcer.get();
        System.out.println("[Shutdown] announce via " + a.id() + ": " + message);
        try {
            a.announce(message);
        } catch (RuntimeException e) {
            System.out.println("[Shutdown] announcement failed: " + e);
        }
    }

    int startSec() {
        return startSec;
    }
}
