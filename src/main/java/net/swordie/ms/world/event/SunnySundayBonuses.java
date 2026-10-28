package net.swordie.ms.world.event;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Which 星期天楓之谷 bonus is on today: a registry of one rule per {@link Bonus}, evaluated against today's calendar
 * entry (sunnysunday.tw.json). A bonus is on only when (1) the revival switch "sunday" is on, (2) today has an entry,
 * (3) that entry enables the bonus. Callers ask {@link #isActive(Bonus)} instead of testing the calendar themselves.
 */
public final class SunnySundayBonuses {
    public enum Bonus {
        /** 咒文痕跡 fever (scroll upgrade UI): calendar field {@code spellTraceOff} > 0. */
        SPELL_TRACE_FEVER,
        /** 怪物公園 extra EXP: calendar field {@code mpIncEXP} > 0. */
        MONSTER_PARK_EXP
    }

    private static final Map<Bonus, Predicate<SunnySunday.SunnySundayEntry>> RULES;

    static {
        EnumMap<Bonus, Predicate<SunnySunday.SunnySundayEntry>> m = new EnumMap<>(Bonus.class);
        m.put(Bonus.SPELL_TRACE_FEVER, e -> e.spellTraceOff > 0);
        m.put(Bonus.MONSTER_PARK_EXP, e -> e.mpIncEXP > 0);
        for (Bonus b : Bonus.values()) if (!m.containsKey(b)) throw new IllegalStateException("no rule for " + b);
        RULES = Collections.unmodifiableMap(m);
    }

    private static final Supplier<SunnySunday.SunnySundayEntry> DEFAULT_TODAY = SunnySunday.SunnySundayLogic::get;
    private static final BooleanSupplier DEFAULT_SWITCH = () ->
            net.swordie.ms.world.tms282.revival.RevivalEvents.isOpen("sunday");
    private static volatile Supplier<SunnySunday.SunnySundayEntry> today = DEFAULT_TODAY;
    private static volatile BooleanSupplier enabled = DEFAULT_SWITCH;

    private SunnySundayBonuses() {
    }

    /** Checks replace the calendar / switch; {@code null} restores the live ones. */
    public static void install(Supplier<SunnySunday.SunnySundayEntry> calendar, BooleanSupplier sundaySwitch) {
        today = calendar == null ? DEFAULT_TODAY : calendar;
        enabled = sundaySwitch == null ? DEFAULT_SWITCH : sundaySwitch;
    }

    /** The rule for one entry (null entry = not a 星期天楓之谷 day). */
    public static boolean isActive(Bonus bonus, SunnySunday.SunnySundayEntry entry) {
        return entry != null && RULES.get(bonus).test(entry);
    }

    /** Today, through the switch and the calendar; a broken calendar or switch means "off". */
    public static boolean isActive(Bonus bonus) {
        try {
            return enabled.getAsBoolean() && isActive(bonus, today.get());
        } catch (RuntimeException e) {
            return false;
        }
    }
}
