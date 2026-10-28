package net.swordie.ms.world.tms282.revival.hub;

import net.swordie.ms.world.tms282.revival.RevivalEvents;
import net.swordie.ms.world.tms282.revival.rewards.RevivalRewards;
import net.swordie.ms.world.tms282.revival.rewards.RevivalRewardsSource;

import java.util.*;

/**
 * Hub definitions by revival event key. The code registers the structure ({@code RevivalEvents} /
 * {@code RevivalHubDefinitions}, "base"); lookups return it with the targets, rewards and offers of
 * revival-rewards.json applied ({@link RevivalRewards#apply}). "open" follows the event switch.
 */
public final class HubCatalog {
    private static final Map<String, HubDefinition> BASE = new LinkedHashMap<>();

    private record Applied(RevivalRewards source, Map<String, HubDefinition> hubs) {
    }

    private static volatile Applied applied = new Applied(null, Map.of());

    private HubCatalog() {
    }

    public static synchronized void register(HubDefinition hub) {
        if (BASE.putIfAbsent(hub.eventKey(), hub) != null) throw new IllegalStateException("duplicate hub " + hub.eventKey());
        applied = new Applied(null, Map.of());
    }

    /** The hubs are registered by RevivalEvents' static initializer: make sure it ran before any lookup. */
    private static void ensureRegistered() {
        RevivalEvents.all();
    }

    /** The code definitions, without the rewards file (defaults of the rewards file, structure checks). */
    public static List<HubDefinition> baseAll() {
        ensureRegistered();
        synchronized (HubCatalog.class) {
            return List.copyOf(BASE.values());
        }
    }

    private static Map<String, HubDefinition> appliedHubs() {
        ensureRegistered();
        RevivalRewards r = RevivalRewardsSource.current();
        Applied a = applied;
        if (a.source() == r) return a.hubs();
        Map<String, HubDefinition> out = new LinkedHashMap<>();
        for (HubDefinition h : baseAll()) {
            HubDefinition x;
            try {
                x = r.apply(h);
            } catch (IllegalArgumentException e) { // validated at load; never leave a hub undefined
                System.out.println("[Revival] rewards for " + h.eventKey() + " not applied: " + e.getMessage());
                x = h;
            }
            out.put(h.eventKey(), x);
        }
        Map<String, HubDefinition> hubs = Collections.unmodifiableMap(out);
        applied = new Applied(r, hubs);
        return hubs;
    }

    public static List<HubDefinition> all() {
        return List.copyOf(appliedHubs().values());
    }

    public static Optional<HubDefinition> find(String key) {
        return Optional.ofNullable(appliedHubs().get(key));
    }

    public static Optional<HubDefinition> open(String key) {
        return find(key).filter(h -> RevivalEvents.isOpen(key));
    }

    private record Snapshot(long at, List<HubDefinition> open) {
    }

    private static volatile Snapshot snapshot = new Snapshot(Long.MIN_VALUE, List.of());

    /** Open hubs; recomputed at most once a second (the mob-kill hook asks on every kill). */
    public static Collection<HubDefinition> openAll() {
        long now = System.currentTimeMillis();
        Snapshot s = snapshot;
        if (s.at() != Long.MIN_VALUE && now - s.at() < 1000 && now >= s.at()) return s.open();
        List<HubDefinition> out = new ArrayList<>();
        for (HubDefinition h : all()) if (RevivalEvents.isOpen(h.eventKey())) out.add(h);
        snapshot = new Snapshot(now, List.copyOf(out));
        return snapshot.open();
    }

    /** Configuration (switches or rewards) changed: drop the one-second snapshot. */
    public static void invalidate() {
        snapshot = new Snapshot(Long.MIN_VALUE, List.of());
    }
}
