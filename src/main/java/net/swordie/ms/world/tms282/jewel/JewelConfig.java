package net.swordie.ms.world.tms282.jewel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.ServerConstants;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Jewel (寶玉) rules from data/resources/tms282/jewel/jewel.json, converted 1:1 from TMS279 jewel.toml.
 * engrave.N and awaken.N are keyed by the TARGET level.
 */
public record JewelConfig(boolean enabled, int maxEngrave, int maxAwaken, Map<Integer, Tier> tiers,
                          Map<Integer, EngraveStep> engrave, Map<Integer, AwakenStep> awaken,
                          Map<Integer, Integer> jewelItems, Ticket maxPolishTicket, Ticket polishResetTicket) {

    public record Ingredient(int id, int count) { }

    public record Tier(int tier, int maxStat, int engraveBonus, long polishResetCost) { }

    /** Percent weights, summing to 100. */
    public record EngraveStep(int level, Ingredient ingredient, int upgrade, int downgrade, int destroy) { }

    /** grade (25/50/75/100) -> weight, iteration order ascending. */
    public record AwakenStep(int level, Ingredient ingredient, Ingredient polishIngredient, SortedMap<Integer, Integer> polishWeights) {
        public int totalWeight() {
            return polishWeights.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    public record Ticket(int itemId, int maxTier) { }

    private static volatile JewelConfig current;

    public static Path defaultPath() {
        return Path.of(System.getProperty("tms282.jewel.config", ServerConstants.RESOURCES_DIR + "/tms282/jewel/jewel.json"));
    }

    public static JewelConfig get() {
        JewelConfig c = current;
        if (c == null) {
            synchronized (JewelConfig.class) {
                if (current == null) current = load(defaultPath());
                c = current;
            }
        }
        return c;
    }

    public static void reload() {
        current = load(defaultPath());
    }

    public static JewelConfig load(Path path) {
        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("jewel config " + path + " not readable", e);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode tree(String json) {
        try {
            JsonNode n = MAPPER.readTree(json);
            if (n == null || !n.isObject()) throw new IllegalArgumentException("jewel config is not a JSON object");
            return n;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("jewel config JSON: " + e.getOriginalMessage(), e);
        }
    }

    private static Iterable<Map.Entry<String, JsonNode>> entries(JsonNode node) {
        if (node == null) throw new IllegalArgumentException("jewel config section missing");
        return node::fields;
    }

    private static Ingredient ingredient(JsonNode o) {
        return new Ingredient(o.get("id").asInt(), o.get("count").asInt());
    }

    public static JewelConfig parse(String json) {
        JsonNode root = tree(json);
        if (root.has("jewel") && !root.has("tier")) root = root.get("jewel"); // raw staged form
        boolean enabled = !root.has("enabled") || root.get("enabled").asBoolean();
        int maxEngrave = root.has("maxEngrave") ? root.get("maxEngrave").asInt() : 20;
        int maxAwaken = root.has("maxAwaken") ? root.get("maxAwaken").asInt() : 20;

        Map<Integer, Tier> tiers = new TreeMap<>();
        for (Map.Entry<String, JsonNode> e : entries(root.get("tier"))) {
            JsonNode t = e.getValue();
            int n = Integer.parseInt(e.getKey());
            tiers.put(n, new Tier(n, t.get("maxStat").asInt(), t.get("engraveBonus").asInt(), t.get("polishResetCost").asLong()));
        }
        Map<Integer, EngraveStep> engrave = new TreeMap<>();
        for (Map.Entry<String, JsonNode> e : entries(root.get("engrave"))) {
            JsonNode s = e.getValue();
            JsonNode p = s.get("prob");
            int n = Integer.parseInt(e.getKey());
            EngraveStep step = new EngraveStep(n, ingredient(s.get("ingredient")),
                    p.get("upgrade").asInt(), p.get("downgrade").asInt(), p.get("destroy").asInt());
            if (step.upgrade + step.downgrade + step.destroy != 100)
                throw new IllegalArgumentException("engrave." + n + " probabilities do not sum to 100");
            engrave.put(n, step);
        }
        Map<Integer, AwakenStep> awaken = new TreeMap<>();
        for (Map.Entry<String, JsonNode> e : entries(root.get("awaken"))) {
            JsonNode s = e.getValue();
            JsonNode polish = s.get("polish");
            SortedMap<Integer, Integer> weights = new TreeMap<>();
            for (Map.Entry<String, JsonNode> w : entries(polish.get("prob")))
                weights.put(Integer.parseInt(w.getKey()), w.getValue().asInt());
            int n = Integer.parseInt(e.getKey());
            AwakenStep step = new AwakenStep(n, ingredient(s.get("ingredient")),
                    ingredient(polish.get("ingredient")), Collections.unmodifiableSortedMap(weights));
            if (step.totalWeight() <= 0) throw new IllegalArgumentException("awaken." + n + " polish weights empty");
            awaken.put(n, step);
        }
        for (int i = 1; i <= maxEngrave; i++) if (!engrave.containsKey(i)) throw new IllegalArgumentException("engrave." + i + " missing");
        for (int i = 1; i <= maxAwaken; i++) if (!awaken.containsKey(i)) throw new IllegalArgumentException("awaken." + i + " missing");

        Map<Integer, Integer> items = new HashMap<>();
        if (root.has("jewelItems"))
            for (Map.Entry<String, JsonNode> e : entries(root.get("jewelItems")))
                items.put(Integer.parseInt(e.getKey()), e.getValue().asInt());
        Ticket maxPolish = null, reset = null;
        if (root.has("tickets")) {
            JsonNode t = root.get("tickets");
            if (t.has("maxPolish")) maxPolish = new Ticket(t.get("maxPolish").get("id").asInt(), t.get("maxPolish").get("maxTier").asInt());
            if (t.has("polishReset")) reset = new Ticket(t.get("polishReset").get("id").asInt(), t.get("polishReset").get("maxTier").asInt());
        }
        return new JewelConfig(enabled, maxEngrave, maxAwaken, Collections.unmodifiableMap(tiers),
                Collections.unmodifiableMap(engrave), Collections.unmodifiableMap(awaken), Collections.unmodifiableMap(items),
                maxPolish, reset);
    }

    public Tier tier(int tier) {
        Tier t = tiers.get(tier);
        if (t == null) throw new IllegalArgumentException("unknown jewel tier " + tier);
        return t;
    }
}
