package net.swordie.ms.client.character.tms282;

import net.swordie.ms.connection.packet.Tms282VMatrixRequest;
import java.util.*;

/** Pure282 operations. Commit the returned state AND economic deltas in one transaction before publishing it. */
public final class Tms282VMatrixService {
    public record Player(int job, boolean eligible, boolean alive, long mesos, Map<Integer, Long> items,
                         Set<Integer> completedQuests, Set<Integer> cooldowns, Set<Integer> activeBuffs) {
        public Player { items = Map.copyOf(items); completedQuests = Set.copyOf(completedQuests);
            cooldowns = Set.copyOf(cooldowns); activeBuffs = Set.copyOf(activeBuffs); }
    }
    public record ItemDelta(int itemId, int quantity, long expiresEpochMillis) { }
    public record Plan(Tms282VMatrixState state, List<ItemDelta> items, long mesoDelta,
                       Map<Integer, Integer> effectiveSkills, Set<Integer> removeBuffs, int animation) {
        public Plan { items = List.copyOf(items); effectiveSkills = Map.copyOf(effectiveSkills); removeBuffs = Set.copyOf(removeBuffs); }
    }
    private final Tms282VMatrixData data;
    public Tms282VMatrixService(Tms282VMatrixData data) { this.data = Objects.requireNonNull(data); }

    public Plan plan(Tms282VMatrixState old, Tms282VMatrixRequest request, Player player, long nowMillis) {
        if (!player.eligible || !player.alive || !data.groups().containsKey(player.job)) deny("目前無法使用 V 矩陣。");
        validate(old);
        var cores = old.mutableCores(); long points = old.points(), mesos = 0;
        List<ItemDelta> items = new ArrayList<>(); Set<Integer> removeBuffs = new HashSet<>(); int animation = 0;
        long now = Tms282VMatrixState.fileTime(nowMillis);
        int id = request.argument(), value = request.value();
        switch (request.operation()) {
            case 0 -> { // vPoint -> the metadata's time-limited gemstone, count (not coreId)
                if (id <= 0) deny("轉換數量不正確。");
                long cost = Math.multiplyExact((long) id, data.transformCost);
                points = spend(points, cost);
                items.add(new ItemDelta(data.transformItem, id, Math.addExact(nowMillis, data.transformMinutes * 60_000L)));
            }
            case 1 -> { // consume the exact WZ-listed gemstone item and quantity
                var gem = data.gemstones().get(id);
                if (gem == null || value <= 0 || player.items.getOrDefault(id, 0L) < value) deny("沒有足夠的核心寶石。");
                if (points >= Tms282VMatrixState.MAX_POINTS) deny("V 點數已達 9999 點上限。");
                if (gem.requiredQuest() != 0 && !player.completedQuests.contains(gem.requiredQuest())) deny("尚未完成使用此核心寶石所需的任務。");
                int pointItems = value;
                if (gem.openCore() != 0) {
                    requireAllowed(player, gem.openCore());
                    var existing = cores.get(gem.openCore());
                    if (existing == null || existing.level() == 0) {
                        cores.put(gem.openCore(), new Tms282VMatrixState.Core(gem.openCore(), 1, false, Tms282VMatrixState.DEFAULT_EXPIRY));
                        pointItems--; //282 item text: first opens the core; only already-opened uses give V points.
                    }
                }
                points = Math.addExact(points, Math.multiplyExact((long) pointItems, gem.points()));
                if (points > Tms282VMatrixState.MAX_POINTS) deny("使用後將超過 V 點數 9999 點上限。");
                items.add(new ItemDelta(id, -value, 0)); animation = gem.animation();
            }
            case 2 -> {
                var definition = requireAllowed(player, id);
                var current = cores.get(id); int level = current == null ? 0 : current.level();
                if (definition.type() == 2) {
                    if (value != 1 || current != null && current.level() > 0) deny("特殊核心已經開啟。");
                    points = spend(points, data.registerSpecialCost);
                    cores.put(id, new Tms282VMatrixState.Core(id, 1, false, addDays(now, definition.expireDays())));
                } else {
                    if (definition.type() > 1 || value <= level || value > definition.maxLevel()) deny("核心目標等級不正確。");
                    if (!definition.selfOpen() && level == 0) deny("此核心必須先使用指定道具開啟。");
                    points = spend(points, data.costBetween(id, level, value));
                    cores.put(id, new Tms282VMatrixState.Core(id, value, false, Tms282VMatrixState.DEFAULT_EXPIRY,
                            current == null ? 0 : current.protectedLevel()));
                }
            }
            case 3 -> {
                var definition = requireAllowed(player, id); var current = cores.get(id);
                if (definition.type() != 2 || current == null || current.level() != 1) deny("尚未開啟此特殊核心。");
                int cost = extensionCost(current.expiresAt(), now);
                if (cost <= 0) deny("特殊核心的有效期限已達上限。");
                points = spend(points, cost);
                cores.put(id, new Tms282VMatrixState.Core(id, 1, current.selected() && current.expiresAt() > now, addDays(now, 30)));
            }
            case 4, 5 -> {
                var definition = requireAllowed(player, id); var current = cores.get(id);
                if (definition.type() != 2 || current == null || current.level() <= 0 || current.expiresAt() <= now)
                    deny("特殊核心尚未開啟或已到期。");
                if (request.operation() == 4 && current.selected()) deny("此特殊核心已經啟用。");
                for (var core : List.copyOf(cores.values())) if (core.selected()) {
                    checkRemoval(player, data.core(core.id()), removeBuffs);
                    cores.put(core.id(), new Tms282VMatrixState.Core(core.id(), core.level(), false, core.expiresAt()));
                }
                if (request.operation() == 5 && !current.selected()) deny("此特殊核心尚未啟用。");
                cores.put(id, new Tms282VMatrixState.Core(id, current.level(), request.operation() == 4, current.expiresAt()));
            }
            case 6 -> {
                if (player.mesos < data.resetMeso) deny("楓幣不足，無法重置 V 矩陣。");
                mesos = -(long) data.resetMeso;
                for (var current : List.copyOf(cores.values())) {
                    var definition = data.core(current.id());
                    if (definition.type() > 1) continue; // native10517E0 excludes special cores from refunds
                    checkRemoval(player, definition, removeBuffs);
                    int floor = Math.min(current.level(), Math.max(current.protectedLevel(), data.resetFloor(player.job, current.id())));
                    if (current.level() > floor) points = Math.addExact(points, data.costBetween(current.id(), floor, current.level()));
                    cores.put(current.id(), new Tms282VMatrixState.Core(current.id(), floor, false, current.expiresAt(), current.protectedLevel()));
                }
            }
            default -> deny("不支援的 V 矩陣操作。");
        }
        if (points > Tms282VMatrixState.MAX_POINTS) deny("操作後將超過 V 點數 9999 點上限。");
        var state = new Tms282VMatrixState(points, Math.addExact(old.revision(), 1), old.migrationPending(), cores, old.retainedSet());
        validate(state);
        return new Plan(state, items, mesos, effectiveSkills(state, nowMillis), removeBuffs, animation);
    }

    public void validate(Tms282VMatrixState state) {
        for (var core : state.cores().values()) {
            var definition = data.core(core.id());
            if (core.level() > definition.maxLevel() || definition.type() == 3 || core.selected() && definition.type() != 2)
                throw new IllegalArgumentException("Invalid stored282 VMatrix core");
        }
    }

    /** Explicit player/admin action only: no item conversion, no point award and no refundable free levels. */
    public Plan maxForJob(Tms282VMatrixState old, Player player, long nowMillis) {
        if (!player.eligible || !player.alive || !data.groups().containsKey(player.job)) deny("目前無法使用 V 矩陣。");
        validate(old);
        var cores = old.mutableCores();
        for (var definition : data.cores().values()) {
            if (definition.type() > 1 || !data.allowed(player.job, definition.id())) continue;
            cores.put(definition.id(), new Tms282VMatrixState.Core(definition.id(), definition.maxLevel(), false,
                    Tms282VMatrixState.DEFAULT_EXPIRY, definition.maxLevel()));
        }
        var state = new Tms282VMatrixState(old.points(), Math.addExact(old.revision(), 1), old.migrationPending(), cores, old.retainedSet());
        return new Plan(state, List.of(), 0, effectiveSkills(state, nowMillis), Set.of(), 0);
    }

    public Optional<Tms282VMatrixState.Core> selectedSpecial(Tms282VMatrixState state, long nowMillis) {
        long now = Tms282VMatrixState.fileTime(nowMillis);
        return state.cores().values().stream().filter(c -> c.selected() && c.level() > 0 && c.expiresAt() > now
                && data.core(c.id()).type() == 2).findFirst();
    }

    public Set<Integer> specialBuffSkills() {
        Set<Integer> result = new TreeSet<>();
        for (var definition : data.cores().values()) if (definition.type() == 2) {
            int skill = definition.specialOption().path("effect").path("skill_id").asInt();
            if (skill > 0) result.add(skill);
        }
        return Set.copyOf(result);
    }
    public Map<Integer, Integer> effectiveSkills(Tms282VMatrixState state, long nowMillis) {
        Map<Integer, Integer> result = new TreeMap<>(); long now = Tms282VMatrixState.fileTime(nowMillis);
        for (var core : state.cores().values()) {
            var definition = data.core(core.id());
            int level = core.expiresAt() <= now ? 0 : core.level();
            for (int skill : definition.skills()) result.merge(skill, level, Math::max);
            for (int skill : definition.subSkills()) result.merge(skill, level, Math::max);
            if (definition.type() == 2) {
                int skill = definition.specialOption().path("effect").path("skill_id").asInt();
                if (skill > 0) result.put(skill, core.selected() && level > 0 ? 1 : 0);
            }
            // These are skill records only. The metadata's conditional special-node buffs require trigger handling.
        }
        return result;
    }
    private Tms282VMatrixData.Core requireAllowed(Player player, int id) {
        if (!data.allowed(player.job, id)) deny("此職業無法使用所選核心。");
        return data.core(id);
    }
    private static void checkRemoval(Player player, Tms282VMatrixData.Core definition, Set<Integer> removeBuffs) {
        if (definition.cooldownSkills().stream().anyMatch(player.cooldowns::contains)
                || definition.equipRemovalSkills().stream().anyMatch(player.activeBuffs::contains))
            deny("核心技能仍在使用或冷卻中，暫時無法移除。");
        removeBuffs.addAll(definition.removalSkills());
        if (definition.type() == 2) {
            int skill = definition.specialOption().path("effect").path("skill_id").asInt();
            if (skill > 0) removeBuffs.add(skill);
        }
    }
    private static long spend(long points, long cost) {
        if (cost < 0 || points < cost) deny("V 點數不足。");
        return points - cost;
    }
    private static long addDays(long time, int days) {
        if (days <= 0) throw new IllegalArgumentException("Missing special-core duration");
        return Math.addExact(time, Math.multiplyExact((long) days, 864_000_000_000L));
    }
    /** Native1051750: cap expiry at now+30days; minutes difference, minimum60, ceil(*0.0011). */
    public static int extensionCost(long expiry, long now) {
        long minutes = (addDays(now, 30) - Math.max(now, expiry)) / 600_000_000L;
        return minutes < 60 ? 0 : (int) Math.ceil(minutes * 0.0011D);
    }
    private static void deny(String message) { throw new IllegalArgumentException(message); }
}
