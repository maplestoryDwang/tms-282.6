package net.swordie.ms.world.tms282.revival.rewards;

import net.swordie.ms.world.tms282.revival.hub.HubDefinition;

import java.util.*;

/**
 * Rewards, thresholds and prices of the revived events (data/tms282-revival/revival-rewards.json). Immutable; built
 * by {@link RevivalRewardsParser} from the file or by {@link #defaults} from the values in the code. Every section is
 * optional in the file: a missing section keeps the built-in values.
 */
public final class RevivalRewards {
    public static final int MAX_QTY = 30_000, MAX_TARGET = 1_000_000, PUNCH_TIERS = 7;

    /** Per mission: new target, new rewards, optional new title (null = derived from the code title). */
    public record MissionOverride(int target, List<HubDefinition.Reward> rewards, String title) {
        public MissionOverride {
            rewards = List.copyOf(rewards);
        }
    }

    /** Per hub: overridden missions by id and the full offer list. */
    public record HubRewards(Map<String, MissionOverride> missions, List<HubDefinition.Offer> offers) {
        public HubRewards {
            missions = Collections.unmodifiableMap(new LinkedHashMap<>(missions));
            offers = List.copyOf(offers);
        }
    }

    /** {min score, fantasy coins, dream coins}. */
    public record ShootingTier(int minScore, int fantasy, int dream) {
    }

    private final String version;
    private final Map<String, HubRewards> hubs;
    private final List<List<HubDefinition.Reward>> punchTiers;
    private final List<Integer> tomatoShards;
    private final List<ShootingTier> shootingTiers;

    RevivalRewards(String version, Map<String, HubRewards> hubs, List<List<HubDefinition.Reward>> punchTiers,
                   List<Integer> tomatoShards, List<ShootingTier> shootingTiers) {
        this.version = version;
        this.hubs = Collections.unmodifiableMap(new LinkedHashMap<>(hubs));
        List<List<HubDefinition.Reward>> p = new ArrayList<>();
        for (List<HubDefinition.Reward> t : punchTiers) p.add(List.copyOf(t));
        this.punchTiers = List.copyOf(p);
        this.tomatoShards = List.copyOf(tomatoShards);
        this.shootingTiers = List.copyOf(shootingTiers);
    }

    public String version() {
        return version;
    }

    public Map<String, HubRewards> hubs() {
        return hubs;
    }

    public List<List<HubDefinition.Reward>> punchTiers() {
        return punchTiers;
    }

    public List<Integer> tomatoShards() {
        return tomatoShards;
    }

    public List<ShootingTier> shootingTiers() {
        return shootingTiers;
    }

    /** The hub as configured: the code structure with this file's targets, rewards, titles and offers. */
    public HubDefinition apply(HubDefinition base) {
        HubRewards r = hubs.get(base.eventKey());
        if (r == null) return base;
        List<HubDefinition.Mission> missions = new ArrayList<>();
        for (HubDefinition.Mission m : base.missions()) {
            MissionOverride o = r.missions().get(m.id());
            if (o == null) {
                missions.add(m);
                continue;
            }
            String title = o.title() != null ? o.title() : retitle(m.title(), m.target(), o.target());
            missions.add(new HubDefinition.Mission(m.id(), title, m.kind(), o.target(), m.range(), m.mobs(), m.minLevel(),
                    m.period(), o.rewards(), m.solo()));
        }
        return new HubDefinition(base.eventKey(), base.title(), base.npcId(), missions, r.offers());
    }

    /** The code titles spell the default count ("擊敗等級範圍怪物 1000 隻"): the first such number follows the target. */
    static String retitle(String title, int oldTarget, int newTarget) {
        if (oldTarget == newTarget) return title;
        String n = String.valueOf(oldTarget);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![0-9])" + n + "(?![0-9])").matcher(title);
        return m.find() ? title.substring(0, m.start()) + newTarget + title.substring(m.end()) : title;
    }

    /** The values of the code (RevivalHubDefinitions, PunchKing, AnnualShooting) as a rewards object. */
    public static RevivalRewards defaults(Collection<HubDefinition> baseHubs, List<List<HubDefinition.Reward>> punch,
                                          List<Integer> tomato, List<ShootingTier> shooting) {
        Map<String, HubRewards> hubs = new LinkedHashMap<>();
        for (HubDefinition h : baseHubs) {
            Map<String, MissionOverride> ms = new LinkedHashMap<>();
            for (HubDefinition.Mission m : h.missions()) ms.put(m.id(), new MissionOverride(m.target(), m.rewards(), null));
            hubs.put(h.eventKey(), new HubRewards(ms, h.offers()));
        }
        return new RevivalRewards("built-in", hubs, punch, tomato, shooting);
    }
}
