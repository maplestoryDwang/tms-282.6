package net.swordie.ms.world.boss;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.SerenPacket;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * TMS 282 受選的賽蓮 server mechanics (bossmech3-0930, research/tms2823/newboss-0930/README.md).
 * <p>
 * 273 ran these in its native engine (event scripts only log the deaths), so this is a reconstruction from the 282
 * client and Mob.wz. What is from client/data and what is an approximation:
 * <ul>
 *     <li>【client】Both Seren fields open the Seren UI (UI 0x509, 2F92A00) with a sun gauge (default max 1000). Phase 2
 *     adds the clock: 4 segments noon / sunset / midnight / dawn, the hand moves 1 degree per cycleMs/360 and stops at
 *     the end of its segment until the server sends the next time of day (LP08A9), which also changes the background
 *     tint and the BossSeren.img/2PhaseBGM track.</li>
 *     <li>【data】Phase-2 core 8880632 (normal; 8880602 hard, 8880652 extreme) is an invisible HP holder (empty stand,
 *     hpTagColor, Mob.wz revive); the visible bodies 8880633 / 8880637 / 8880639 / 8880642 carry HpLinkMob = core.
 *     Damage on a body is forwarded to the core (BossHpConstants.getBalrogDamageSinkId), the core's death clears the
 *     fight (273 boss_seren_*.js: LOG_BOSS = 8880632).</li>
 *     <li>【推定】body order = time-of-day order (noon 8880633 has MobSkill 263, the P2 client's 0x8AE skill; sunset
 *     8880637 has P1's MobSkill 264). Segment sizes 108/72/108/72 degrees and a 120 s cycle are server choices
 *     (tms282.seren.cycleMs). The client has no duration table (Etc/BossSeren.img only lists the BGM).</li>
 *     <li>【非官方近似】sun gauge: +hit per damaging hit (x1.5 at noon), -decayPerSec every second (x3 at dawn); full
 *     gauge -> fullAction (default stun 3 s, then the gauge resets; "kill" and "none" selectable). The official rule
 *     is not in the client.</li>
 * </ul>
 * Not implemented: phase-1 MobSkill 264 areas (LP08AC/08AD), MobSkill 263 object (LP08AE), LP08AF, Nerotas helpers
 * (8880635/36), fire barriers, the midnight / dawn special rules. Settings: -D wins over config/server.properties,
 * {@code tms282.seren.mechanics.enabled=false} turns the whole machine off (spawn and clear stay as before).
 */
public final class SerenFight {
    public static final String PROPERTY = "SerenFight";
    public static final int NOON = 1, SUNSET = 2, MIDNIGHT = 3, DAWN = 4;
    /** Degrees per segment, noon first (sum 360). */
    static final int[] SEGMENT_DEGREES = {108, 72, 108, 72};
    /** Body template offset from the core, per time of day (normal: 8880632 + 1 / 5 / 7 / 10). */
    static final int[] BODY_OFFSET = {1, 5, 7, 10};
    static final int TICK_MS = 1000;
    static final int EMPTY_TICKS_BEFORE_STOP = 60;

    private final Field field;
    private final boolean phaseTwo;
    private final int coreTemplate;
    private final int x, y;
    private final long startMs;
    private final int cycleMs, gaugeMax, gaugeHit, gaugeDecay, stunSec;
    private final String fullAction;
    private final Map<Integer, Integer> gauges = new ConcurrentHashMap<>();
    private Set<Integer> present = new HashSet<>();
    private int timeOfDay;
    private Mob body;
    private int emptyTicks;
    private volatile boolean stopped;
    private ScheduledFuture<?> tick;

    private SerenFight(Field field, boolean phaseTwo, int coreTemplate, int x, int y) {
        this.field = field;
        this.phaseTwo = phaseTwo;
        this.coreTemplate = coreTemplate;
        this.x = x;
        this.y = y;
        this.startMs = System.currentTimeMillis();
        this.cycleMs = Math.max(36_000, intValue("tms282.seren.cycleMs", 120_000));
        this.gaugeMax = Math.max(1, intValue("tms282.seren.gauge.max", 1000));
        this.gaugeHit = Math.max(0, intValue("tms282.seren.gauge.hit", 40));
        this.gaugeDecay = Math.max(0, intValue("tms282.seren.gauge.decayPerSec", 15));
        this.stunSec = Math.max(1, intValue("tms282.seren.gauge.stunSec", 3));
        this.fullAction = stringValue("tms282.seren.gauge.fullAction", "stun").trim().toLowerCase();
    }

    // ---- settings ----------------------------------------------------------------------------------------------

    public static boolean enabled() {
        return !"false".equalsIgnoreCase(stringValue("tms282.seren.mechanics.enabled", "true").trim());
    }

    static String stringValue(String key, String fallback) {
        String jvm = System.getProperty(key);
        if (jvm != null) {
            return jvm;
        }
        try {
            String v = ServerRuntimeConfig.stringValue(key);
            return v == null || v.isBlank() ? fallback : v;
        } catch (RuntimeException | LinkageError e) {
            return fallback;
        }
    }

    static int intValue(String key, int fallback) {
        try {
            return Integer.parseInt(stringValue(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---- lifecycle ---------------------------------------------------------------------------------------------

    /** Phase 1 (stage 20): sun gauge only. {@code core} = the phase-1 boss (8880630 normal). */
    public static SerenFight startPhaseOne(Field field, Mob core) {
        return start(field, false, core);
    }

    /** Phase 2 (stage 60): {@code core} = the invisible HP core (8880632 normal); spawns the noon body. */
    public static SerenFight startPhaseTwo(Field field, Mob core) {
        return start(field, true, core);
    }

    private static SerenFight start(Field field, boolean phaseTwo, Mob core) {
        if (field == null || core == null || !enabled()) {
            return null;
        }
        SerenFight fight = new SerenFight(field, phaseTwo, core.getTemplateId(),
                core.getPosition().getX(), core.getPosition().getY());
        synchronized (field) {
            Object old = field.getProperty(PROPERTY);
            if (old instanceof SerenFight running && !running.stopped) {
                return running;
            }
            field.setProperty(PROPERTY, fight);
        }
        synchronized (fight) {
            if (phaseTwo) {
                fight.enterTimeOfDay(NOON);
            }
            fight.tickLocked();
        }
        fight.tick = field.getTimer().addFixedRateEvent(fight::tick, TICK_MS, TICK_MS, false);
        System.out.printf("[SEREN] %s started in field %d (core %d, cycle %d ms)%n",
                phaseTwo ? "phase 2" : "phase 1", field.getId(), fight.coreTemplate, fight.cycleMs);
        return fight;
    }

    public static SerenFight of(Field field) {
        Object o = field == null ? null : field.getProperty(PROPERTY);
        return o instanceof SerenFight f ? f : null;
    }

    /** The core died (Seren.onMobDeath) or the field emptied: stop the clock, clear the gauge, remove the body. */
    public synchronized void stop(boolean cleared) {
        if (stopped) {
            return;
        }
        stopped = true;
        if (tick != null) {
            tick.cancel(false);
        }
        if (phaseTwo) {
            broadcast(SerenPacket.timerPause(true));
        }
        for (Char chr : chars()) {
            chr.write(SerenPacket.stunGauge(gaugeMax, 0));
        }
        if (body != null && field.getMobs().contains(body)) {
            field.removeMob(body.getObjectId(), cleared ? DeathType.ANIMATION_DEATH : DeathType.INSTA_DEATH);
        }
        body = null;
        System.out.printf("[SEREN] stopped in field %d (%s)%n", field.getId(), cleared ? "cleared" : "abandoned");
    }

    public boolean isStopped() {
        return stopped;
    }

    // ---- per-second tick ----------------------------------------------------------------------------------------

    private void tick() {
        try {
            synchronized (this) {
                if (!stopped) {
                    tickLocked();
                }
            }
        } catch (Throwable t) {
            System.out.println("[SEREN] tick failed in field " + field.getId() + ": " + t);
        }
    }

    private void tickLocked() {
        Mob core = coreMob();
        if (core == null) {
            stop(false);
            return;
        }
        Set<Char> chars = chars();
        if (chars.isEmpty()) {
            if (++emptyTicks >= EMPTY_TICKS_BEFORE_STOP) {
                stop(false);
            }
            return;
        }
        emptyTicks = 0;
        Set<Integer> now = new HashSet<>();
        for (Char chr : chars) {
            now.add(chr.getId());
            if (!present.contains(chr.getId())) {
                sync(chr);
            }
        }
        present = now;
        gauges.keySet().retainAll(now);
        int decay = timeOfDay == DAWN ? gaugeDecay * 3 : gaugeDecay;
        if (decay > 0) {
            for (Char chr : chars) {
                int g = gauges.getOrDefault(chr.getId(), 0);
                if (g > 0) {
                    setGauge(chr, Math.max(0, g - decay));
                }
            }
        }
        if (phaseTwo) {
            int want = timeOfDayAt(System.currentTimeMillis() - startMs);
            if (want != timeOfDay) {
                enterTimeOfDay(want);
            }
        }
    }

    /** The time of day for an elapsed time: segment boundaries at 108 / 180 / 288 degrees of the cycle. */
    int timeOfDayAt(long elapsedMs) {
        return timeOfDayAt(elapsedMs, cycleMs);
    }

    /** Pure form of {@link #timeOfDayAt(long)} (checks). */
    public static int timeOfDayAt(long elapsedMs, int cycleMs) {
        long deg = (elapsedMs % cycleMs) * 360 / cycleMs;
        int acc = 0;
        for (int i = 0; i < SEGMENT_DEGREES.length; i++) {
            acc += SEGMENT_DEGREES[i];
            if (deg < acc) {
                return i + 1;
            }
        }
        return DAWN;
    }

    /** Initial packets for a character that (re)entered: gauge, and in phase 2 the clock + current time of day. */
    private void sync(Char chr) {
        chr.write(SerenPacket.stunGauge(gaugeMax, gauges.getOrDefault(chr.getId(), 0)));
        if (phaseTwo) {
            chr.write(SerenPacket.timerStart(cycleMs, SEGMENT_DEGREES[0], SEGMENT_DEGREES[1],
                    SEGMENT_DEGREES[2], SEGMENT_DEGREES[3]));
            if (timeOfDay != NOON) {
                chr.write(SerenPacket.timeOfDay(timeOfDay));
            }
        }
    }

    private void enterTimeOfDay(int next) {
        timeOfDay = next;
        broadcast(SerenPacket.timeOfDay(next));
        int template = coreTemplate + BODY_OFFSET[next - 1];
        Mob old = body;
        int bx = x, by = y;
        if (old != null && field.getMobs().contains(old)) {
            bx = old.getPosition().getX();
            field.removeMob(old.getObjectId(), DeathType.INSTA_DEATH);
        }
        body = field.spawnMob(template, bx, by, false);
        if (body == null) {
            System.out.printf("[SEREN] body %d missing (field %d); the core stays attackable%n", template, field.getId());
        }
    }

    // ---- gauge -------------------------------------------------------------------------------------------------

    /** Tms282CombatHandlers CP0115: a character in a Seren field lost HP to a hit. */
    public static void onUserHit(Char chr, int hpLost) {
        if (chr == null || hpLost <= 0) {
            return;
        }
        try {
            SerenFight fight = of(chr.getField());
            if (fight == null || fight.stopped) {
                return;
            }
            fight.hit(chr);
        } catch (RuntimeException e) {
            System.out.println("[SEREN] gauge hit failed for " + chr.getId() + ": " + e);
        }
    }

    private synchronized void hit(Char chr) {
        if (stopped || gaugeHit <= 0) {
            return;
        }
        int add = timeOfDay == NOON ? gaugeHit * 3 / 2 : gaugeHit;
        int g = gauges.getOrDefault(chr.getId(), 0) + add;
        if (g < gaugeMax) {
            setGauge(chr, g);
            return;
        }
        setGauge(chr, gaugeMax);
        switch (fullAction) {
            case "none" -> {
                return;
            }
            case "kill" -> {
                if (chr.getHP() > 0) {
                    chr.die();
                }
            }
            default -> stun(chr);
        }
        setGauge(chr, 0);
    }

    private void stun(Char chr) {
        Option o = new Option(MobSkillID.Stun.getVal());
        o.nOption = 1;
        o.slv = 1;
        o.tOption = stunSec;
        chr.getTemporaryStatManager().sendSetStatFromMobSkillPacket(CharacterTemporaryStat.Stun, o);
    }

    private void setGauge(Char chr, int value) {
        Integer old = gauges.put(chr.getId(), value);
        if (old == null || old != value) {
            chr.write(SerenPacket.stunGauge(gaugeMax, value));
        }
    }

    public int gauge(int charId) {
        return gauges.getOrDefault(charId, 0);
    }

    public int timeOfDay() {
        return timeOfDay;
    }

    public Mob body() {
        return body;
    }

    // ---- helpers -----------------------------------------------------------------------------------------------

    private Mob coreMob() {
        for (Mob mob : field.getMobs()) {
            if (mob.getTemplateId() == coreTemplate && mob.getHp() > 0) {
                return mob;
            }
        }
        return null;
    }

    private Set<Char> chars() {
        return new HashSet<>(new ArrayList<>(field.getChars()));
    }

    private void broadcast(OutPacket packet) {
        field.broadcast(packet);
    }
}
