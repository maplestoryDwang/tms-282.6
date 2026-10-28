package net.swordie.ms.world.tms282.bossrun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Per boss / difficulty clear rewards ported from TMS273 {@code BOSS_REWARD_APPLY.js}
 * ({@code data/resources/tms282/bossrun/rewards.json}; generator filtered to items named in the 282 String.wz,
 * absent from items-removed.tsv and present in the server item data). Each entry rolls independently:
 * {@code rate}/10000, quantity uniform in [min, max] (TMS273 applyReward).
 */
public final class BossRewardTable {
    public record Entry(int itemId, String name, int rate, int min, int max) {
    }

    public record Table(String bossKey, String source, List<Entry> basic, List<Entry> advanced) {
    }

    public record Grant(int itemId, String name, int quantity, boolean advanced) {
    }

    private final Map<String, Table> tables;

    public BossRewardTable(Map<String, Table> tables) {
        this.tables = Map.copyOf(tables);
    }

    public static Path defaultPath() {
        return Path.of(ServerConstants.RESOURCES_DIR, "tms282", "bossrun", "rewards.json");
    }

    public static BossRewardTable load(Path path) {
        try {
            JsonNode root = new ObjectMapper().readTree(path.toFile());
            Map<String, Table> map = new HashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = root.path("bosses").fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                map.put(e.getKey(), new Table(e.getKey(), e.getValue().path("source").asText(""),
                        entries(e.getValue().path("listA")), entries(e.getValue().path("listB"))));
            }
            return new BossRewardTable(map);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
    }

    private static List<Entry> entries(JsonNode list) {
        List<Entry> out = new ArrayList<>();
        for (JsonNode n : list) {
            int min = Math.max(1, n.path("min").asInt(1));
            out.add(new Entry(n.path("itemId").asInt(), n.path("name").asText(""), n.path("rate").asInt(0), min,
                    Math.max(min, n.path("max").asInt(min))));
        }
        return List.copyOf(out);
    }

    public Table table(String bossKey) {
        return tables.get(bossKey);
    }

    public Map<String, Table> tables() {
        return tables;
    }

    public List<Grant> roll(String bossKey, RandomGenerator random) {
        Table t = tables.get(bossKey);
        if (t == null) return List.of();
        List<Grant> out = new ArrayList<>();
        roll(t.basic(), false, random, out);
        roll(t.advanced(), true, random, out);
        return out;
    }

    private static void roll(List<Entry> list, boolean advanced, RandomGenerator random, List<Grant> out) {
        for (Entry e : list) {
            if (random.nextInt(10000) >= e.rate()) continue;
            int qty = e.min() + (e.max() > e.min() ? random.nextInt(e.max() - e.min() + 1) : 0);
            out.add(new Grant(e.itemId(), e.name(), qty, advanced));
        }
    }
}
