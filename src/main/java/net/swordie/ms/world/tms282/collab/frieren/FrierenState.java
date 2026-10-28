package net.swordie.ms.world.tms282.collab.frieren;

import java.time.LocalDate;
import java.util.Properties;

/**
 * One character's partner-event state (芙莉蓮夥伴們 / 一起冒險). Pure logic, no server objects, so the check can
 * drive it; {@link FrierenStateStore} persists it as a properties file (no DB).
 * <ul>
 *     <li>kill gauge: +1 per level-range kill while 芙莉蓮夥伴們 is ON; at {@code gaugeMax} (500) it empties and a
 *     clone is summoned, at most {@code dailyCloneLimit} (20) a day; once the limit is reached the event ends for
 *     the day (「達到上限後即結束」) and the gauge stops;</li>
 *     <li>clone defeats: every {@code clonesPerFragment} (2) defeats give one 靈魂艾爾達碎片; each defeat adds
 *     {@code castsPerClone} (2) companion casts.</li>
 * </ul>
 * Both skills start ON (the official default is not captured; the strings only say right-click switches them).
 */
public final class FrierenState {
    /** What a kill did to the gauge. */
    public enum KillResult {
        /** Not counted (partners OFF, event over, daily limit reached). */
        IGNORED,
        /** Gauge +1. */
        COUNTED,
        /** Gauge full: summon a clone (gauge already emptied, daily count already taken). */
        SUMMON_CLONE
    }

    /** What a clone defeat gives. */
    public record CloneReward(int fragments, int casts) {
    }

    private boolean adventureOn = true;
    private boolean partnersOn = true;
    private int gauge;
    private LocalDate day;
    private int clonesToday;
    private int cloneDefeats;

    public boolean adventureOn() {
        return adventureOn;
    }

    public boolean partnersOn() {
        return partnersOn;
    }

    public int gauge() {
        return gauge;
    }

    public int clonesToday() {
        return clonesToday;
    }

    public int cloneDefeats() {
        return cloneDefeats;
    }

    /** Flips the switch of {@code skillId}; returns the new state. */
    public boolean toggle(int skillId) {
        if (skillId == FrierenSkills.ADVENTURE_TOGETHER) {
            adventureOn = !adventureOn;
            return adventureOn;
        }
        if (skillId == FrierenSkills.PARTNERS) {
            partnersOn = !partnersOn;
            return partnersOn;
        }
        throw new IllegalArgumentException("not a Frieren toggle skill: " + skillId);
    }

    /** Starts a new day's clone count when {@code today} differs from the stored day. */
    public void rollDay(LocalDate today) {
        if (!today.equals(day)) {
            day = today;
            clonesToday = 0;
        }
    }

    public boolean dailyLimitReached(FrierenSettings s, LocalDate today) {
        rollDay(today);
        return clonesToday >= s.dailyCloneLimit();
    }

    /** One level-range kill. */
    public KillResult onLevelRangeKill(FrierenSettings s, LocalDate today) {
        if (!partnersOn || dailyLimitReached(s, today)) {
            return KillResult.IGNORED;
        }
        gauge++;
        if (gauge < s.gaugeMax()) {
            return KillResult.COUNTED;
        }
        gauge = 0;
        clonesToday++;
        return KillResult.SUMMON_CLONE;
    }

    /** The clone that could not be placed (no foothold): gives the daily count back and refills the gauge. */
    public void cloneNotPlaced(FrierenSettings s) {
        clonesToday = Math.max(0, clonesToday - 1);
        gauge = s.gaugeMax() - 1;
    }

    /** One clone defeated by its owner. */
    public CloneReward onCloneDefeated(FrierenSettings s) {
        cloneDefeats++;
        return new CloneReward(cloneDefeats % s.clonesPerFragment() == 0 ? 1 : 0, s.castsPerClone());
    }

    Properties toProperties() {
        Properties p = new Properties();
        p.setProperty("adventureOn", Boolean.toString(adventureOn));
        p.setProperty("partnersOn", Boolean.toString(partnersOn));
        p.setProperty("gauge", Integer.toString(gauge));
        p.setProperty("day", day == null ? "" : day.toString());
        p.setProperty("clonesToday", Integer.toString(clonesToday));
        p.setProperty("cloneDefeats", Integer.toString(cloneDefeats));
        return p;
    }

    static FrierenState fromProperties(Properties p) {
        FrierenState s = new FrierenState();
        s.adventureOn = Boolean.parseBoolean(p.getProperty("adventureOn", "true"));
        s.partnersOn = Boolean.parseBoolean(p.getProperty("partnersOn", "true"));
        s.gauge = Math.max(0, Integer.parseInt(p.getProperty("gauge", "0")));
        String d = p.getProperty("day", "");
        s.day = d.isEmpty() ? null : LocalDate.parse(d);
        s.clonesToday = Math.max(0, Integer.parseInt(p.getProperty("clonesToday", "0")));
        s.cloneDefeats = Math.max(0, Integer.parseInt(p.getProperty("cloneDefeats", "0")));
        return s;
    }

    @Override
    public String toString() {
        return "FrierenState[adventure=" + adventureOn + ", partners=" + partnersOn + ", gauge=" + gauge + ", day=" + day
                + ", clonesToday=" + clonesToday + ", cloneDefeats=" + cloneDefeats + "]";
    }
}
