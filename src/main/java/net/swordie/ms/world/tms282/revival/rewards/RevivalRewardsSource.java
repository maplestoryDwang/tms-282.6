package net.swordie.ms.world.tms282.revival.rewards;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.world.tms282.items.Tms282ItemAvailability;
import net.swordie.ms.world.tms282.revival.circus.AnnualShooting;
import net.swordie.ms.world.tms282.revival.hub.HubCatalog;
import net.swordie.ms.world.tms282.revival.hub.HubDefinition;
import net.swordie.ms.world.tms282.revival.punch.PunchKing;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.LongSupplier;

/** The shared, hot-reloaded {@link RevivalRewards} (data/tms282-revival/revival-rewards.json). */
public final class RevivalRewardsSource {
    public static final Path DEFAULT_FILE = Path.of(ServerConstants.DIR, "data", "tms282-revival", "revival-rewards.json");
    /** Live item check: in the item data and grantable on 282. */
    public static final IntPredicate LIVE_ITEMS = id -> Tms282ItemAvailability.isGrantable(id) && ItemData.getItemDeepCopy(id) != null;
    private static volatile HotFile<RevivalRewards> shared;

    private RevivalRewardsSource() {
    }

    /** The values in the code. */
    public static RevivalRewards defaults() {
        return RevivalRewards.defaults(HubCatalog.baseAll(), PunchKing.DEFAULT_TIERS, PunchKing.DEFAULT_TOMATO_SHARDS,
                AnnualShooting.DEFAULT_TIERS);
    }

    public static RevivalRewardsParser.Context context(IntPredicate itemOk) {
        Map<String, HubDefinition> base = new LinkedHashMap<>();
        for (HubDefinition h : HubCatalog.baseAll()) base.put(h.eventKey(), h);
        return new RevivalRewardsParser.Context(base, itemOk);
    }

    public static HotFile<RevivalRewards> create(Path file, LongSupplier clock, IntPredicate itemOk) {
        return new HotFile<>("revival-rewards.json", file, clock,
                bytes -> RevivalRewardsParser.parse(bytes, defaults(), context(itemOk)),
                RevivalRewardsSource::defaults, RevivalRewards::version, HubCatalog::invalidate);
    }

    public static HotFile<RevivalRewards> shared() {
        HotFile<RevivalRewards> s = shared;
        if (s == null) {
            synchronized (RevivalRewardsSource.class) {
                if (shared == null) shared = create(DEFAULT_FILE, System::currentTimeMillis, LIVE_ITEMS);
                s = shared;
            }
        }
        return s;
    }

    /** Checks replace the shared file; {@code null} restores the live one. */
    public static void install(HotFile<RevivalRewards> source) {
        shared = source;
        HubCatalog.invalidate();
    }

    public static RevivalRewards current() {
        return shared().current();
    }
}
