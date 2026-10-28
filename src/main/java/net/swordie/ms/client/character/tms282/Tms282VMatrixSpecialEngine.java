package net.swordie.ms.client.character.tms282;

import java.util.*;
import java.util.function.DoubleSupplier;

/** Pure server event policy driven by actual282 spCoreOption; no buffs, packets or DB writes.
 * Policy decisions (not claims about unknown official server internals): counters reset on selection,
 * expiry and trigger; WZ validtime is inactivity; cooldown accumulation is capped at the threshold;
 * same-mob attacks reset on target changes. Timed conditions wait a full interval after selection.
 * Per-core trigger cooldown survives deselection so switching cannot bypass it.
 */
public final class Tms282VMatrixSpecialEngine {
    public enum Type { ATTACK, HIT, KILL, COMBO_KILL, SKILL_USE, RUNE, DEATH }
    public enum EffectType { SELF_BUFF, HEAL, REDUCE_COOLDOWNS }
    /** ATTACK amount is validated positive damage-line count for one mob, never client claimed hits.
     * Probability is sampled once per ATTACK event; for a probability core submit one event per
     * attack packet, not one per target. Counting cores may receive one event per validated target.
     * Other counter amounts are verified events; signal events use amount=1, mobObjectId=0.
     */
    public record Event(Type type, int amount, int mobObjectId) {
        public Event {
            Objects.requireNonNull(type);
            if (amount <= 0 || mobObjectId < 0 || type == Type.ATTACK && mobObjectId == 0)
                throw new IllegalArgumentException("Invalid verified special-core event");
        }
    }
    public record Effect(int coreId, EffectType type, int skillId, int skillLevel, int percent) { }
    private record Rule(String condition, int count, long cooldown, long validTime, double probability, Effect effect) { }
    private final Map<Integer, Rule> rules = new HashMap<>();
    private final Map<Integer, Long> nextAllowed = new HashMap<>();
    private final DoubleSupplier random;
    private Tms282VMatrixState.Core selected;
    private long selectedAt, lastActivity = -1, count;
    private int target;

    public Tms282VMatrixSpecialEngine(Tms282VMatrixData data, DoubleSupplier random) {
        this.random = Objects.requireNonNull(random);
        for (var core : data.cores().values()) if (core.type() == 2) {
            var cond = core.specialOption().path("cond"); var effect = core.specialOption().path("effect");
            String condition = cond.path("type").asText();
            if (!Set.of("killCount", "attackCount", "attackCountMob", "hitCount", "prob", "cooltime", "die", "combokill", "rune").contains(condition))
                throw new IllegalArgumentException("Unsupported282 special condition");
            EffectType type = switch (effect.path("type").asText()) {
                case "selfbuff" -> EffectType.SELF_BUFF;
                case "heal" -> EffectType.HEAL;
                case "reduceCooltime" -> EffectType.REDUCE_COOLDOWNS;
                default -> throw new IllegalArgumentException("Unsupported282 special effect");
            };
            int percent = type == EffectType.HEAL ? effect.path("heal_percent").asInt() : effect.path("reducePercent").asInt();
            var result = new Effect(core.id(), type, effect.path("skill_id").asInt(), effect.path("skill_level").asInt(1), percent);
            var rule = new Rule(condition, cond.path("count").asInt(), cond.path("cooltime").asLong(),
                    cond.path("validtime").asLong(), cond.path("prob").asDouble(), result);
            if (result.skillId <= 0 || result.skillLevel <= 0 || rule.cooldown <= 0 || rule.validTime < 0
                    || isCounter(condition) && rule.count <= 0 || condition.equals("prob") && !(rule.probability > 0 && rule.probability <= 1)
                    || type != EffectType.SELF_BUFF && (percent <= 0 || percent > 100))
                throw new IllegalArgumentException("Invalid282 special metadata");
            rules.put(core.id(), rule);
        }
    }

    /** Call when authority changes and at login. Repeated synchronization of the same active core
     * preserves progress; expiry extension preserves progress too. Pass null to deselect.
     */
    public synchronized void reset(Tms282VMatrixState.Core core, long nowMillis) {
        requireTime(nowMillis);
        if (core != null && !rules.containsKey(core.id())) throw new IllegalArgumentException("Not a special core");
        if (core != null && (!core.selected() || core.level() == 0 || core.expiresAt() <= Tms282VMatrixState.fileTime(nowMillis))) core = null;
        if (selected != null && selected.expiresAt() <= Tms282VMatrixState.fileTime(nowMillis)) { selected = null; clearCounters(); }
        if (selected == null ? core != null : core == null || selected.id() != core.id()) {
            clearCounters(); selectedAt = nowMillis;
        }
        selected = core;
    }

    /** Restore committed absolute deadlines at login; selecting another core never erases them. */
    public synchronized void restoreCooldown(int coreId,long readyAt) {
        requireTime(readyAt);
        if(!rules.containsKey(coreId))throw new IllegalArgumentException("Not a special core");
        nextAllowed.merge(coreId,readyAt,Math::max);
    }

    public synchronized Optional<Effect> onEvent(Event event, long nowMillis) {
        Objects.requireNonNull(event); requireTime(nowMillis);
        if (selected == null) return Optional.empty();
        if (nowMillis < selectedAt) return Optional.empty();
        if (selected.expiresAt() <= Tms282VMatrixState.fileTime(nowMillis)) {
            selected = null; clearCounters(); return Optional.empty();
        }
        Rule rule = rules.get(selected.id());
        boolean matches = switch (rule.condition) {
            case "attackCount", "attackCountMob", "prob" -> event.type == Type.ATTACK;
            case "killCount" -> event.type == Type.KILL;
            case "hitCount" -> event.type == Type.HIT;
            case "combokill" -> event.type == Type.COMBO_KILL;
            case "cooltime" -> event.type == Type.SKILL_USE;
            case "rune" -> event.type == Type.RUNE;
            case "die" -> event.type == Type.DEATH;
            default -> false;
        };
        if (!matches) return Optional.empty();
        if (isCounter(rule.condition)) {
            if (lastActivity >= 0 && (nowMillis < lastActivity || nowMillis - lastActivity >= rule.validTime && rule.validTime > 0)) clearCounters();
            if (rule.condition.equals("attackCountMob") && target != event.mobObjectId) { count = 0; target = event.mobObjectId; }
            count = Math.min(rule.count, count + event.amount); lastActivity = nowMillis;
        }
        Long ready = nextAllowed.get(selected.id());
        if (ready != null && nowMillis < ready) return Optional.empty();
        if (rule.condition.equals("cooltime") && (nowMillis < selectedAt || nowMillis - selectedAt < rule.cooldown)) return Optional.empty();
        if (isCounter(rule.condition) && count < rule.count) return Optional.empty();
        if (rule.condition.equals("prob")) {
            double roll = random.getAsDouble();
            if (!Double.isFinite(roll) || roll < 0 || roll >= 1) throw new IllegalArgumentException("Random sample outside [0,1)");
            if (roll >= rule.probability) return Optional.empty();
        }
        nextAllowed.put(selected.id(), Math.addExact(nowMillis,rule.cooldown)); clearCounters();
        return Optional.of(rule.effect);
    }
    private static boolean isCounter(String condition) {
        return switch (condition) {
            case "killCount", "attackCount", "attackCountMob", "hitCount", "combokill" -> true;
            default -> false;
        };
    }
    private void clearCounters() { count = 0; target = 0; lastActivity = -1; }
    private static void requireTime(long now) { if (now < 0) throw new IllegalArgumentException("Invalid event time"); }
}
