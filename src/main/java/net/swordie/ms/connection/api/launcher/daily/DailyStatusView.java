package net.swordie.ms.connection.api.launcher.daily;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The caller's daily gift ("sign-in") state in one world, as the launcher's stamp card shows it - read-only, with
 * today's rollover applied to a copy.
 *
 * @param hasAccount     the user has an account (accounts row) in this world; false = nothing to claim there yet
 * @param source         {@code memory} (the game's copy while the user is online), {@code database} (the dailygift row
 *                       with the highest id), {@code none} (no record yet: day 0, not claimed today)
 * @param claimedCount   claims this month (dateComplete after the rollover)
 * @param claimedToday   claimed today (isClaim after the rollover)
 * @param nextReward     the reward of claim {@code claimedCount + 1} (today's when not claimed yet), null when the
 *                       month is complete or the reward table is unreadable
 * @param claimDates     this month's dated claims (dailygift_log); empty when the table does not exist
 * @param logAvailable   dailygift_log could be read; null without an account
 * @param rewardsVersion {@link DailyRewardTable#version()}, null when the reward table is unreadable
 * @param viewMonth      the month {@code claimDates} belong to (fixes10: dailyStatus {@code "month":"YYYY-MM"}, the
 *                       current month and up to {@link #PAST_MONTHS} before it); the current month by default
 */
public record DailyStatusView(int world, boolean hasAccount, String source, LocalDate today, int claimedCount,
                              boolean claimedToday, DailyRewardTable.Reward nextReward, List<LocalDate> claimDates,
                              Boolean logAvailable, String rewardsVersion, YearMonth viewMonth) {
    /** How many months before the current one dailyStatus may show (11: a year of stamp cards). */
    public static final int PAST_MONTHS = 11;

    public DailyStatusView {
        claimDates = List.copyOf(claimDates);
        if (viewMonth == null) {
            viewMonth = YearMonth.from(today);
        }
    }

    /** The current month's view (as before fixes10). */
    public DailyStatusView(int world, boolean hasAccount, String source, LocalDate today, int claimedCount,
                           boolean claimedToday, DailyRewardTable.Reward nextReward, List<LocalDate> claimDates,
                           Boolean logAvailable, String rewardsVersion) {
        this(world, hasAccount, source, today, claimedCount, claimedToday, nextReward, claimDates, logAvailable, rewardsVersion, null);
    }

    /** The month of {@code claimDates} (the current month unless another one was asked for). */
    public YearMonth month() {
        return viewMonth;
    }

    /** The month of {@code today}: claimedCount, claimedToday, monthComplete and nextReward always describe it. */
    public YearMonth currentMonth() {
        return YearMonth.from(today);
    }

    /** The oldest month dailyStatus answers for. */
    public YearMonth minMonth() {
        return currentMonth().minusMonths(PAST_MONTHS);
    }

    /** A month dailyStatus answers for: the current one or up to {@link #PAST_MONTHS} before it, never a future one. */
    public static boolean inRange(YearMonth month, YearMonth current) {
        return month != null && !month.isAfter(current) && !month.isBefore(current.minusMonths(PAST_MONTHS));
    }

    /** All 28 days of the month claimed. */
    public boolean monthComplete() {
        return claimedCount >= DailyRewardTable.DAYS;
    }

    /**
     * Claims this month without a dated log row (before dailygift_log existed, or a failed log write); 0 for a past
     * month (the game keeps no count of past months, only the log).
     */
    public int undatedCount() {
        return viewMonth.equals(currentMonth()) ? Math.max(0, claimedCount - claimDates.size()) : 0;
    }

    /**
     * Wire order: world, hasAccount, source, today, month, claimedCount, claimedToday, monthComplete, nextReward,
     * claimDates, undatedCount, logAvailable, rewardsVersion, then (fixes10) currentMonth, minMonth, maxMonth. Dates as
     * yyyy-MM-dd, months as yyyy-MM. {@code month} is the month of claimDates (the asked one); claimedCount,
     * claimedToday, monthComplete and nextReward are always today's state ({@code currentMonth}).
     */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("world", world);
        m.put("hasAccount", hasAccount);
        m.put("source", source);
        m.put("today", today.toString());
        m.put("month", month().toString());
        m.put("claimedCount", claimedCount);
        m.put("claimedToday", claimedToday);
        m.put("monthComplete", monthComplete());
        m.put("nextReward", nextReward == null ? null : nextReward.toMap());
        List<String> dates = new ArrayList<>();
        for (LocalDate d : claimDates) {
            dates.add(d.toString());
        }
        m.put("claimDates", dates);
        m.put("undatedCount", undatedCount());
        m.put("logAvailable", logAvailable);
        m.put("rewardsVersion", rewardsVersion);
        m.put("currentMonth", currentMonth().toString());
        m.put("minMonth", minMonth().toString());
        m.put("maxMonth", currentMonth().toString());
        return m;
    }
}
