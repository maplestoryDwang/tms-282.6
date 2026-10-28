package net.swordie.ms.world.tms282.bossrun;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Account clear counting over a {@link BossRunStore}: a clear counts once per run (two characters of one account
 * in the same party use one entry, like TMS273 counted per account), per boss key, in the current day and week.
 */
public final class BossRunLedger {

    public enum Verdict { OK, DAILY, WEEKLY }

    public record Usage(int today, int week, BossRunLimits.Limit limit) {
        public int dailyLeft() {
            return limit.daily() <= 0 ? Integer.MAX_VALUE : Math.max(0, limit.daily() - today);
        }

        public int weeklyLeft() {
            return limit.weekly() <= 0 ? Integer.MAX_VALUE : Math.max(0, limit.weekly() - week);
        }

        public Verdict verdict() {
            if (limit.daily() > 0 && today >= limit.daily()) return Verdict.DAILY;
            if (limit.weekly() > 0 && week >= limit.weekly()) return Verdict.WEEKLY;
            return Verdict.OK;
        }
    }

    private final BossRunStore store;
    private final BossRunLimits limits;
    private final DayOfWeek resetDay;

    public BossRunLedger(BossRunStore store, BossRunLimits limits, DayOfWeek resetDay) {
        this.store = store;
        this.limits = limits;
        this.resetDay = resetDay;
    }

    public BossRunLimits limits() {
        return limits;
    }

    /** Usage in the day / week containing {@code now}, not counting run {@code excludeRunId} (0 = none). */
    public Usage usage(int accountId, String bossKey, LocalDateTime now, long excludeRunId) {
        LocalDateTime week = BossRunLimits.weekStart(now, resetDay);
        LocalDateTime day = BossRunLimits.dayStart(now);
        LocalDateTime since = week.isBefore(day) ? week : day;
        List<BossRunStore.ClearRow> rows = store.countedClears(accountId, bossKey, since);
        Set<Long> dayRuns = new HashSet<>();
        Set<Long> weekRuns = new HashSet<>();
        for (BossRunStore.ClearRow r : rows) {
            if (excludeRunId != 0 && r.runId() == excludeRunId) continue;
            long key = r.runId() != 0 ? r.runId() : -(r.clearedAt().hashCode() * 31L + r.charId());
            if (!r.clearedAt().isBefore(week)) weekRuns.add(key);
            if (!r.clearedAt().isBefore(day)) dayRuns.add(key);
        }
        return new Usage(dayRuns.size(), weekRuns.size(), limits.limitOf(bossKey));
    }

    public Verdict verdict(int accountId, String bossKey, LocalDateTime now) {
        return usage(accountId, bossKey, now, 0).verdict();
    }

    public static String verdictText(Verdict v) {
        return switch (v) {
            case OK -> "可挑戰";
            case DAILY -> "今日挑戰次數已用完";
            case WEEKLY -> "本週挑戰次數已用完";
        };
    }
}
