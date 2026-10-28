package net.swordie.ms.world.tms282.dojo;

import net.swordie.ms.ServerConfig;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Weekly period keys shared by weekly-reset content (dojo ranking, Urus shop limits).
 * A week starts Monday 00:00 in {@link ServerConfig#ZONE}; its key is that Monday as yyyyMMdd.
 */
public final class WeekKeys {
    private WeekKeys() {
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static int key(LocalDate monday) {
        return monday.getYear() * 10000 + monday.getMonthValue() * 100 + monday.getDayOfMonth();
    }

    public static LocalDate date(int key) {
        return LocalDate.of(key / 10000, (key / 100) % 100, key % 100);
    }

    public static int current() {
        return key(weekStart(ZonedDateTime.now(ServerConfig.ZONE).toLocalDate()));
    }

    public static int previous(int key) {
        return key(date(key).minusWeeks(1));
    }
}
