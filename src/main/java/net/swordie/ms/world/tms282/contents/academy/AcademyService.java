package net.swordie.ms.world.tms282.contents.academy;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.tms282.contents.common.AccountLedger;
import net.swordie.ms.world.tms282.contents.common.HotJsonFile;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 楓之谷學院 (content-1005 feature 4) without the official academy window (not reverse-engineered): NPC 粉豆
 * 9063668 and the quest notifier (q68761s sign-in, q102385s hub) drive it.
 * <p>
 * Account-wide receipts ({@link AccountLedger}, questid {@link #LEDGER_QUEST}): {@code si:<date>} sign-in,
 * {@code bc:<date>} basic course, {@code ac:<week>:<n>} advanced course (each one 學分), {@code ms:<credits>}
 * milestone. Credits = the number of si / bc / ac receipts. Weeks start on Thursday 00:00 (Taipei).
 * <p>
 * Level-range kills (|mob level - character level| &lt;= levelRange, no bosses) are counted in memory from the kill
 * path (no database there) and written to the character's QR-ex of {@link #LEDGER_QUEST} ({@code kd/kc/kw/kwc})
 * every {@link #FLUSH_EVERY} kills and before a course claim; a crash loses at most that many kills.
 */
public final class AcademyService {
    public static final int LEDGER_QUEST = 102385;
    public static final int FLUSH_EVERY = 50;
    public static final Path FILE = Path.of("data/tms282-academy/academy.json");

    public enum Result {OK, ALREADY, CLOSED, LEVEL, WEEK_LIMIT, TOTAL_LIMIT, NOT_ENOUGH, NOT_REACHED, FULL, UNAVAILABLE,
        OFFLINE, FAILED}

    /** What the hub shows. */
    public record Status(boolean running, int credits, int signInTotal, int signInWeek, boolean signedToday,
                         boolean basicToday, int basicWeek, int advancedWeek, int dayKills, int weekKills,
                         List<Integer> claimableMilestones, List<Integer> claimedMilestones) {
    }

    /** Per-character kill counters (day and week). */
    static final class Kills {
        LocalDate day;
        int dayKills;
        LocalDate week;
        int weekKills;
        int unsaved;
    }

    private static volatile AcademyService instance;
    private final Supplier<AcademyConfig> config;
    private final Supplier<AccountLedger> ledger;
    private final Supplier<LocalDateTime> clock;
    private final Map<Integer, Kills> kills = new ConcurrentHashMap<>();

    public AcademyService(Supplier<AcademyConfig> config, Supplier<AccountLedger> ledger, Supplier<LocalDateTime> clock) {
        this.config = Objects.requireNonNull(config);
        this.ledger = Objects.requireNonNull(ledger);
        this.clock = Objects.requireNonNull(clock);
    }

    public static AcademyService get() {
        AcademyService s = instance;
        if (s == null) {
            synchronized (AcademyService.class) {
                s = instance;
                if (s == null) {
                    HotJsonFile<AcademyConfig> file = new HotJsonFile<>(FILE, AcademyConfig::parse, AcademyConfig::disabled, 2000);
                    s = new AcademyService(file::get, AccountLedger::get, () -> LocalDateTime.now(AcademyConfig.ZONE));
                    instance = s;
                }
            }
        }
        return s;
    }

    public static void install(AcademyService service) {
        instance = service;
    }

    public AcademyConfig config() {
        return config.get();
    }

    public boolean running() {
        return config.get().running(clock.get());
    }

    static LocalDate weekOf(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.THURSDAY));
    }

    private static int inWeek(Map<String, String> receipts, String prefix, LocalDate week) {
        int n = 0;
        for (String k : receipts.keySet()) {
            try {
                LocalDate d = LocalDate.parse(k.substring(prefix.length(), prefix.length() + 10));
                if (weekOf(d).equals(week)) {
                    n++;
                }
            } catch (RuntimeException ignored) {
                // a malformed key does not count
            }
        }
        return n;
    }

    // ---------------------------------------------------------------------------------------------- kills

    private Kills counters(Char chr, LocalDate today) {
        Kills k = kills.computeIfAbsent(chr.getId(), id -> load(chr));
        synchronized (k) {
            if (!today.equals(k.day)) {
                k.day = today;
                k.dayKills = 0;
            }
            LocalDate week = weekOf(today);
            if (!week.equals(k.week)) {
                k.week = week;
                k.weekKills = 0;
            }
        }
        return k;
    }

    private static Kills load(Char chr) {
        Kills k = new Kills();
        try {
            String kd = chr.getQRValueByKey(LEDGER_QUEST, "kd");
            String kw = chr.getQRValueByKey(LEDGER_QUEST, "kw");
            if (kd != null) {
                k.day = LocalDate.parse(kd);
                k.dayKills = Integer.parseInt(String.valueOf(chr.getQRValueByKey(LEDGER_QUEST, "kc")));
            }
            if (kw != null) {
                k.week = LocalDate.parse(kw);
                k.weekKills = Integer.parseInt(String.valueOf(chr.getQRValueByKey(LEDGER_QUEST, "kwc")));
            }
        } catch (RuntimeException e) {
            return new Kills();
        }
        return k;
    }

    /** Writes the counters to the character's QR-ex (kept with the character's save). */
    public void flush(Char chr) {
        if (chr == null) {
            return;
        }
        Kills k = kills.get(chr.getId());
        if (k == null) {
            return;
        }
        synchronized (k) {
            if (k.day != null) {
                chr.setQRValueByKey(LEDGER_QUEST, "kd", k.day.toString());
                chr.setQRValueByKey(LEDGER_QUEST, "kc", String.valueOf(k.dayKills));
            }
            if (k.week != null) {
                chr.setQRValueByKey(LEDGER_QUEST, "kw", k.week.toString());
                chr.setQRValueByKey(LEDGER_QUEST, "kwc", String.valueOf(k.weekKills));
            }
            k.unsaved = 0;
        }
    }

    /** Kill path (Job.handleMobKilled): memory only, a QR-ex write every {@link #FLUSH_EVERY} kills. */
    public void onMobKilled(Char chr, Mob mob) {
        if (chr == null || mob == null) {
            return;
        }
        try {
            AcademyConfig c = config.get();
            LocalDateTime now = clock.get();
            if (!c.running(now) || chr.getLevel() < c.minLevel() || mob.isBoss()
                    || Math.abs(mob.getLevel() - chr.getLevel()) > c.levelRange()) {
                return;
            }
            countKill(chr, now.toLocalDate());
        } catch (RuntimeException e) {
            // never break the kill path
        }
    }

    /** One level-range kill (also the test entry). */
    public void countKill(Char chr, LocalDate today) {
        Kills k = counters(chr, today);
        boolean flush;
        synchronized (k) {
            k.dayKills++;
            k.weekKills++;
            flush = ++k.unsaved >= FLUSH_EVERY;
        }
        if (flush) {
            flush(chr);
        }
    }

    // ---------------------------------------------------------------------------------------------- status

    public Status status(Char chr) {
        AcademyConfig c = config.get();
        LocalDateTime now = clock.get();
        LocalDate today = now.toLocalDate(), week = weekOf(today);
        AccountLedger l = ledger.get();
        Map<String, String> si = l.receipts(chr, LEDGER_QUEST, "si:");
        Map<String, String> bc = l.receipts(chr, LEDGER_QUEST, "bc:");
        Map<String, String> ac = l.receipts(chr, LEDGER_QUEST, "ac:" + week + ":");
        int credits = si.size() + bc.size() + l.receipts(chr, LEDGER_QUEST, "ac:").size();
        Map<String, String> ms = l.receipts(chr, LEDGER_QUEST, "ms:");
        List<Integer> claimable = new ArrayList<>(), claimed = new ArrayList<>();
        for (AcademyConfig.Milestone m : c.milestones()) {
            if (ms.containsKey("ms:" + m.credits())) {
                claimed.add(m.credits());
            } else if (credits >= m.credits()) {
                claimable.add(m.credits());
            }
        }
        Kills k = counters(chr, today);
        int dayKills, weekKills;
        synchronized (k) {
            dayKills = k.dayKills;
            weekKills = k.weekKills;
        }
        return new Status(c.running(now), credits, si.size(), inWeek(si, "si:", week), si.containsKey("si:" + today),
                bc.containsKey("bc:" + today), inWeek(bc, "bc:", week), ac.size(), dayKills, weekKills, claimable, claimed);
    }

    private Result gate(Char chr) {
        AcademyConfig c = config.get();
        if (chr == null) {
            return Result.OFFLINE;
        }
        if (!c.running(clock.get())) {
            return Result.CLOSED;
        }
        if (chr.getLevel() < c.minLevel()) {
            return Result.LEVEL;
        }
        return null;
    }

    private static Result map(AccountLedger.Outcome o) {
        return switch (o.result()) {
            case GRANTED -> Result.OK;
            case ALREADY -> Result.ALREADY;
            case FULL -> Result.FULL;
            case UNAVAILABLE -> Result.UNAVAILABLE;
            case OFFLINE -> Result.OFFLINE;
            case FAILED -> Result.FAILED;
            case DENIED -> switch (o.reason()) {
                case "WEEK" -> Result.WEEK_LIMIT;
                case "TOTAL" -> Result.TOTAL_LIMIT;
                default -> Result.FAILED;
            };
        };
    }

    /** Today's sign-in: 學分 + 約會券 + the n-th day's reward. */
    public Result signIn(Char chr) {
        Result g = gate(chr);
        if (g != null) {
            return g;
        }
        AcademyConfig c = config.get();
        LocalDate today = clock.get().toLocalDate(), week = weekOf(today);
        AccountLedger l = ledger.get();
        Map<String, String> before = l.receipts(chr, LEDGER_QUEST, "si:");
        if (before.containsKey("si:" + today)) {
            return Result.ALREADY;
        }
        int n = before.size() + 1;
        AccountLedger.Outcome o = l.claim(chr, LEDGER_QUEST, "si:" + today, "academy sign-in #" + n, c.signInRewardsFor(n),
                "si:", m -> m.size() != n - 1 ? "RETRY" : m.size() >= c.signInMax() ? "TOTAL"
                        : inWeek(m, "si:", week) >= c.signInPerWeek() ? "WEEK" : null);
        return map(o);
    }

    /** Basic course: today's kills >= basicKills, once a day, basicPerWeek a week; 學分 x1. */
    public Result basicCourse(Char chr) {
        Result g = gate(chr);
        if (g != null) {
            return g;
        }
        AcademyConfig c = config.get();
        LocalDate today = clock.get().toLocalDate(), week = weekOf(today);
        Kills k = counters(chr, today);
        int dayKills;
        synchronized (k) {
            dayKills = k.dayKills;
        }
        if (dayKills < c.basicKills()) {
            return Result.NOT_ENOUGH;
        }
        flush(chr);
        return map(ledger.get().claim(chr, LEDGER_QUEST, "bc:" + today, "academy basic course kills=" + dayKills,
                List.of(new AccountLedger.Reward(c.creditItem(), 1)), "bc:",
                m -> inWeek(m, "bc:", week) >= c.basicPerWeek() ? "WEEK" : null));
    }

    /** Advanced course: every advancedKills kills this week, at most advancedPerWeek a week; 學分 x1 each. */
    public Result advancedCourse(Char chr) {
        Result g = gate(chr);
        if (g != null) {
            return g;
        }
        AcademyConfig c = config.get();
        LocalDate week = weekOf(clock.get().toLocalDate());
        Kills k = counters(chr, clock.get().toLocalDate());
        int weekKills;
        synchronized (k) {
            weekKills = k.weekKills;
        }
        String prefix = "ac:" + week + ":";
        int n = ledger.get().receipts(chr, LEDGER_QUEST, prefix).size() + 1;
        if (n > c.advancedPerWeek()) {
            return Result.WEEK_LIMIT;
        }
        if (weekKills < (long) c.advancedKills() * n) {
            return Result.NOT_ENOUGH;
        }
        flush(chr);
        return map(ledger.get().claim(chr, LEDGER_QUEST, prefix + n, "academy advanced course kills=" + weekKills,
                List.of(new AccountLedger.Reward(c.creditItem(), 1)), prefix,
                m -> m.size() != n - 1 ? "RETRY" : m.size() >= c.advancedPerWeek() ? "WEEK" : null));
    }

    /** Credit milestone reward (once per account). */
    public Result milestone(Char chr, int credits) {
        Result g = gate(chr);
        if (g != null) {
            return g;
        }
        AcademyConfig.Milestone m = config.get().milestone(credits);
        if (m == null) {
            return Result.FAILED;
        }
        if (status(chr).credits() < credits) {
            return Result.NOT_REACHED;
        }
        return map(ledger.get().claim(chr, LEDGER_QUEST, "ms:" + credits, "academy milestone", m.rewards(), null, null));
    }
}
