package net.swordie.ms.loaders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

/** User-supplied 273 familiar tables. Missing packs are rejected rather than aliased to another pack. */
public final class TmsFamiliarRules {
    public record Card(int familiarId, int grade) { }
    public record Entry<T>(T value, long weight) { }
    public record Pool<T>(List<Entry<T>> entries, long total) {
        public Pool {
            entries = List.copyOf(entries);
            long checked = 0;
            for (Entry<T> entry : entries) {
                Objects.requireNonNull(entry.value());
                if (entry.weight() <= 0) throw new IllegalArgumentException("Nonpositive familiar weight");
                checked = Math.addExact(checked, entry.weight());
            }
            if (checked == 0 || checked != total) throw new IllegalArgumentException("Invalid familiar pool total");
        }
        public T select(long ticket) {
            if (ticket < 0 || ticket >= total) throw new IllegalArgumentException("Familiar ticket out of bounds");
            for (Entry<T> entry : entries) {
                if (ticket < entry.weight()) return entry.value();
                ticket -= entry.weight();
            }
            throw new AssertionError("Unreachable weighted selection");
        }
        public T draw(RandomGenerator random) { return select(random.nextLong(total)); }
    }
    private static final String[] GRADES = {"Ordinary", "Rare", "Epic", "Unique", "Legendary"};
    private final Path root;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Integer, Pool<Card>> packs = new ConcurrentHashMap<>();
    private final Map<String, Pool<Integer>> potentials = new ConcurrentHashMap<>();

    public TmsFamiliarRules(Path commonDirectory) { root = commonDirectory.toAbsolutePath().normalize(); }

    public Pool<Card> cards(int itemId) throws IOException {
        if (itemId <= 0) throw new IllegalArgumentException("Invalid pack id");
        Pool<Card> cached = packs.get(itemId);
        if (cached != null) return cached;
        JsonNode data = mapper.readTree(root.resolve("RandFamiliar/" + itemId + ".json").toFile());
        if (!data.isArray()) throw new IOException("Familiar pack must be an array");
        List<Entry<Card>> entries = new ArrayList<>(); long total = 0;
        for (JsonNode row : data) {
            int id = integer(row.get("familiar")), grade = integer(row.get("grade")), weight = integer(row.get("prop"));
            if (id <= 0 || grade < 0 || grade >= GRADES.length || weight < 0) throw new IOException("Invalid familiar pack entry");
            if (weight == 0) continue;
            entries.add(new Entry<>(new Card(id, grade), weight)); total = Math.addExact(total, weight);
        }
        Pool<Card> result = new Pool<>(entries, total); packs.putIfAbsent(itemId, result);
        return packs.get(itemId);
    }

    public Pool<Integer> potentials(int cubeId, int grade) throws IOException {
        if (cubeId <= 0 || grade < 0 || grade >= GRADES.length) throw new IllegalArgumentException("Invalid familiar cube/grade");
        String key = cubeId + "/" + GRADES[grade];
        Pool<Integer> cached = potentials.get(key);
        if (cached != null) return cached;
        JsonNode data = mapper.readTree(root.resolve("FamiliarCube/" + key + ".json").toFile());
        if (!data.isObject()) throw new IOException("Familiar potentials must be an object");
        List<Entry<Integer>> entries = new ArrayList<>(); long total = 0;
        var fields = data.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            int id = Integer.parseInt(field.getKey()), weight = integer(field.getValue());
            if (id <= 0 || weight < 0) throw new IOException("Invalid familiar potential entry");
            if (weight == 0) continue;
            entries.add(new Entry<>(id, weight)); total = Math.addExact(total, weight);
        }
        Pool<Integer> result = new Pool<>(entries, total); potentials.putIfAbsent(key, result);
        return potentials.get(key);
    }

    private static int integer(JsonNode node) throws IOException {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToInt()) throw new IOException("Expected integer familiar rule");
        return node.intValue();
    }
}
