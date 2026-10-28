package net.swordie.ms.world.boss;

import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.console.config.ServerRuntimeConfig;
import net.swordie.ms.enums.DeathType;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.world.field.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * TMS 282 fightable bodies for bosses whose server spawn is only the HP holder (bossflow-0930,
 * research/tms2823/boss-flow-0930/README.md).
 * <p>
 * 【data】The holders below are Mob.wz HP holders: mobType 14 / hideHP / empty 1x1 stand without lt/rb (no client hit
 * box), and the visible bodies carry {@code info/HpLinkMob} = holder (Kalos, Black Mage Extreme, Limbo), or they are the
 * Horntail parts that SpecialHPBossHandler already forwards to the dummy Horntail (Mob.wz 8810026 revive list =
 * the 8 parts + 8810018). Before this class the 282 server spawned only the holder, so nothing could be hit.
 * <ul>
 *     <li>HP link: body damage goes to the holder through {@code BossHpConstants.getBalrogDamageSinkId} (same
 *     damage-sink path as Seren, only while the holder is in the field) -> the holder's death is the existing phase
 *     transition / {@code completeCompatibleBoss}. Horntail parts keep their own HP and SpecialHPBossHandler.</li>
 *     <li>【推定】Kalos phase 2: the four linked bodies are used one at a time, switched at 75 / 50 / 25 % of the
 *     holder HP (the three later bodies have a {@code regen} action). Official switching rule unknown.</li>
 *     <li>Limbo: phase 1 A + B together, 2D ghost D, phase 3 black + white together (±250 px from the holder).</li>
 * </ul>
 * Kill switches (-D wins over config/server.properties, read at spawn time): {@code tms282.linkedBodies.enabled},
 * {@code tms282.kalos.bodies.enabled}, {@code tms282.blackMage.extremeBody.enabled}, {@code tms282.limbo.bodies.enabled},
 * {@code tms282.horntail.parts.enabled}. Off = the old behaviour (holder only). No new mechanics beyond spawn/HP/clean up.
 */
public final class Tms282LinkedBodies {
    public static final String PROPERTY = "Tms282LinkedBodies:";
    static final int TICK_MS = 1000;
    static final int EMPTY_TICKS_BEFORE_STOP = 60;

    public enum Mode { TOGETHER, SEQUENCE_BY_HP }

    /** One holder: its bodies (template, x offset from the holder), mode, owning boss and switch. */
    public record Spec(String boss, String switchKey, Mode mode, boolean sink, int[] bodies, int[] xOffsets) {
    }

    private static final Map<Integer, Spec> SPECS = new HashMap<>();
    private static final Map<Integer, Integer> SINKS = new HashMap<>();

    static {
        // Kalos phase 2 (normal / easy / chaos / extreme): holder phaseOneMob + 2, bodies + 3..+ 6 (HpLinkMob = holder).
        for (int holder : new int[]{8880802, 8881012, 8881032, 8881052}) {
            put(holder, new Spec("kalos", "tms282.kalos.bodies.enabled", Mode.SEQUENCE_BY_HP, true,
                    new int[]{holder + 1, holder + 2, holder + 3, holder + 4}, new int[]{0, 0, 0, 0}));
        }
        // Black Mage Extreme (firstenter_bossBlackMage diff 4): holder 8880549, body 8880534 (HpLinkMob 8880549).
        put(8880549, new Spec("blackMage", "tms282.blackMage.extremeBody.enabled", Mode.TOGETHER, true,
                new int[]{8880534}, new int[]{0}));
        // Limbo normal (+50 = hard): 1 AB holder -> ghost A/B, 2D CD holder -> ghost D, 3 holder -> black/white.
        for (int off : new int[]{0, 50}) {
            put(8881306 + off, new Spec("limbo", "tms282.limbo.bodies.enabled", Mode.TOGETHER, true,
                    new int[]{8881300 + off, 8881302 + off}, new int[]{-250, 250}));
            put(8881335 + off, new Spec("limbo", "tms282.limbo.bodies.enabled", Mode.TOGETHER, true,
                    new int[]{8881315 + off}, new int[]{0}));
            put(8881332 + off, new Spec("limbo", "tms282.limbo.bodies.enabled", Mode.TOGETHER, true,
                    new int[]{8881320 + off, 8881325 + off}, new int[]{-250, 250}));
        }
        // Horntail normal / chaos / easy: dummy + the 8 parts of Mob.wz 8810026 / 8810130 / 8810215 revive lists.
        // Parts are NOT damage sinks: SpecialHPBossHandler already copies part damage onto the dummy.
        put(8810018, horntail(8810002));
        put(8810122, horntail(8810102));
        put(8810214, horntail(8810202));
    }

    private Tms282LinkedBodies() {
    }

    private static Spec horntail(int firstPart) {
        int[] parts = new int[8];
        for (int i = 0; i < 8; i++) {
            parts[i] = firstPart + i;
        }
        return new Spec("horntail", "tms282.horntail.parts.enabled", Mode.TOGETHER, false, parts, new int[8]);
    }

    private static void put(int holder, Spec spec) {
        SPECS.put(holder, spec);
        if (spec.sink()) {
            for (int body : spec.bodies()) {
                SINKS.put(body, holder);
            }
        }
    }

    // ---- lookups (pure) ---------------------------------------------------------------------------------------------

    public static Spec spec(int holderTemplate) {
        return SPECS.get(holderTemplate);
    }

    /** The holder that receives a body's damage (BossHpConstants.getBalrogDamageSinkId), 0 = none. */
    public static int sinkFor(int bodyTemplate) {
        return SINKS.getOrDefault(bodyTemplate, 0);
    }

    public static Map<Integer, Spec> specs() {
        return Collections.unmodifiableMap(SPECS);
    }

    /** Kalos body index for a holder HP ratio: > 75 % first body, > 50 % second, > 25 % third, else the fourth. */
    public static int sequenceIndex(long hp, long maxHp) {
        if (maxHp <= 0) {
            return 0;
        }
        double r = (double) hp / maxHp;
        return r > 0.75 ? 0 : r > 0.50 ? 1 : r > 0.25 ? 2 : 3;
    }

    // ---- settings ---------------------------------------------------------------------------------------------------

    static String setting(String key, String fallback) {
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

    static boolean on(String key) {
        return !"false".equalsIgnoreCase(setting(key, "true").trim());
    }

    public static boolean enabled(Spec spec) {
        return spec != null && Protocol.active().version() == ProtocolVersion.TMS_282_3
                && on("tms282.linkedBodies.enabled") && on(spec.switchKey());
    }

    // ---- spawning ---------------------------------------------------------------------------------------------------

    /** Spawns one body (checks replace it; the server uses Field.spawnMob). */
    public interface Spawner {
        Mob spawn(Field field, int template, int x, int y);
    }

    private static volatile Spawner spawner = (field, template, x, y) -> field.spawnMob(template, x, y, false);

    public static void setSpawnerForChecks(Spawner s) {
        spawner = s == null ? (field, template, x, y) -> field.spawnMob(template, x, y, false) : s;
    }

    /**
     * Call right after the holder was spawned (Kalos.enter, Limbo.enter, field scripts). Returns the spawned bodies;
     * empty when disabled, not a holder, or already done for this holder. Never throws.
     */
    public static List<Mob> spawnFor(Mob holder) {
        try {
            Spec spec = holder == null ? null : spec(holder.getTemplateId());
            Field field = holder == null ? null : holder.getField();
            if (field == null || !enabled(spec)) {
                return List.of();
            }
            Controller c = new Controller(field, holder, spec);
            synchronized (field) {
                if (field.hasProperty(PROPERTY + holder.getObjectId())) {
                    return List.of();
                }
                field.setProperty(PROPERTY + holder.getObjectId(), c);
            }
            c.start();
            return c.bodies();
        } catch (RuntimeException e) {
            System.out.println("[LINKED] spawn failed for " + (holder == null ? "null" : holder.getTemplateId()) + ": " + e);
            return List.of();
        }
    }

    public static Controller of(Field field, Mob holder) {
        Object o = field == null || holder == null ? null : field.getProperty(PROPERTY + holder.getObjectId());
        return o instanceof Controller c ? c : null;
    }

    /** BossHelper.handleMobKilled: a holder died -> its bodies leave with the death animation. */
    public static void onMobDeath(Mob mob) {
        try {
            Controller c = of(mob == null ? null : mob.getField(), mob);
            if (c != null) {
                c.stop(true);
            }
        } catch (RuntimeException e) {
            System.out.println("[LINKED] death clean-up failed: " + e);
        }
    }

    /** One holder's bodies. */
    public static final class Controller {
        private final Field field;
        private final Mob holder;
        private final Spec spec;
        private final List<Mob> bodies = new ArrayList<>();
        private int index;
        private int emptyTicks;
        private volatile boolean stopped;
        private ScheduledFuture<?> tick;

        Controller(Field field, Mob holder, Spec spec) {
            this.field = field;
            this.holder = holder;
            this.spec = spec;
        }

        synchronized void start() {
            int x = holder.getPosition().getX(), y = holder.getPosition().getY();
            if (spec.mode() == Mode.SEQUENCE_BY_HP) {
                index = sequenceIndex(holder.getHp(), holder.getMaxHp());
                add(spec.bodies()[index], x + spec.xOffsets()[index], y);
            } else {
                for (int i = 0; i < spec.bodies().length; i++) {
                    add(spec.bodies()[i], x + spec.xOffsets()[i], y);
                }
            }
            if (field.getTimer() != null) {
                tick = field.getTimer().addFixedRateEvent(this::tick, TICK_MS, TICK_MS, false);
            }
            System.out.printf("[LINKED] %s holder %d in field %d: bodies %s%n", spec.boss(), holder.getTemplateId(),
                    field.getId(), templates());
        }

        private void add(int template, int x, int y) {
            Mob body = spawner.spawn(field, template, x, y);
            if (body != null) {
                bodies.add(body);
            } else {
                System.out.printf("[LINKED] body %d missing (field %d)%n", template, field.getId());
            }
        }

        private void tick() {
            try {
                tickForChecks();
            } catch (Throwable t) {
                System.out.println("[LINKED] tick failed in field " + field.getId() + ": " + t);
            }
        }

        /** One tick: holder gone -> clean up; empty field -> stop; Kalos -> switch body by holder HP. */
        public synchronized void tickForChecks() {
            if (stopped) {
                return;
            }
            if (holder.getHp() <= 0 || !field.getMobs().contains(holder)) {
                stop(true);
                return;
            }
            if (field.getChars().isEmpty()) {
                if (++emptyTicks >= EMPTY_TICKS_BEFORE_STOP) {
                    stop(false);
                }
                return;
            }
            emptyTicks = 0;
            if (spec.mode() == Mode.SEQUENCE_BY_HP) {
                int want = sequenceIndex(holder.getHp(), holder.getMaxHp());
                if (want > index) {
                    Mob old = bodies.isEmpty() ? null : bodies.getLast();
                    int x = holder.getPosition().getX(), y = holder.getPosition().getY();
                    if (old != null) {
                        x = old.getPosition().getX();
                        y = old.getPosition().getY();
                        if (field.getMobs().contains(old)) {
                            field.removeMob(old.getObjectId(), DeathType.INSTA_DEATH);
                        }
                        bodies.remove(old);
                    }
                    index = want;
                    add(spec.bodies()[index], x, y);
                    System.out.printf("[LINKED] %s field %d: body -> %d%n", spec.boss(), field.getId(),
                            spec.bodies()[index]);
                }
            }
        }

        /** Stop ticking and remove the bodies still on the field. */
        public synchronized void stop(boolean animation) {
            if (stopped) {
                return;
            }
            stopped = true;
            if (tick != null) {
                tick.cancel(false);
            }
            for (Mob body : bodies) {
                if (field.getMobs().contains(body)) {
                    field.removeMob(body.getObjectId(), animation ? DeathType.ANIMATION_DEATH : DeathType.INSTA_DEATH);
                }
            }
            bodies.clear();
        }

        public synchronized List<Mob> bodies() {
            return List.copyOf(bodies);
        }

        public synchronized List<Integer> templates() {
            List<Integer> t = new ArrayList<>();
            for (Mob b : bodies) {
                t.add(b.getTemplateId());
            }
            return t;
        }

        public boolean isStopped() {
            return stopped;
        }

        public int index() {
            return index;
        }
    }
}
