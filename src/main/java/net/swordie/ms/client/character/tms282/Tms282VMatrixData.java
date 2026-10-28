package net.swordie.ms.client.character.tms282;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Exact installed282 VCore metadata, kept separate from the265 VCore loader. */
public final class Tms282VMatrixData {
    public record Core(int id, int type, int maxLevel, int expireDays, boolean selfOpen,
                       List<Integer> skills, List<Integer> subSkills, List<Integer> cooldownSkills,
                       List<Integer> removalSkills, List<Integer> equipRemovalSkills, JsonNode specialOption) { }
    public record Group(Set<Integer> skills, Set<Integer> boosts, Set<Integer> common) {
        boolean contains(int id) { return skills.contains(id) || boosts.contains(id) || common.contains(id); }
    }
    public record Gemstone(int itemId, int points, int openCore, int requiredQuest, int animation) { }
    private final Map<Integer, Core> cores = new TreeMap<>();
    private final Map<Integer, Group> groups = new TreeMap<>();
    private final Map<Integer, Gemstone> gemstones = new TreeMap<>();
    private final Set<Integer> legacyCompatible;
    private final NavigableMap<Integer, Integer> skillCosts = new TreeMap<>(), boostCosts = new TreeMap<>();
    public final int resetMeso, registerSpecialCost, extendSpecialCost, transformCost, transformItem, transformMinutes;

    public Tms282VMatrixData(Path file) throws IOException {
        JsonNode document = new ObjectMapper().readTree(file.toFile());
        legacyCompatible = Set.copyOf(values(document.path("legacyCompatibleCoreIds")));
        JsonNode root = document.path("data");
        if (!root.has("vSkill") || !root.has("vPoint") || !root.has("info"))
            throw new IOException("Incomplete282 VCore data");
        var entries = root.path("vSkill").path("CoreData").fields();
        while (entries.hasNext()) {
            var e = entries.next(); int id = Integer.parseInt(e.getKey()); var n = e.getValue();
            var core = new Core(id, n.path("type").asInt(-1), n.path("maxLevel").asInt(), n.path("expireAfter").asInt(),
                    n.path("notAbleSelfOpen").asInt() == 0, values(n.path("connectSkill")), values(n.path("subSkill")),
                    values(n.path("checkCooltimeSkill")), values(n.path("checkRemoveSkill")), values(n.path("NotDuringEquipRemove")),
                    n.path("spCoreOption").deepCopy());
            if (core.type < 0 || core.type > 3 || core.maxLevel < 1 || core.maxLevel > 60) throw new IOException("Invalid282 core metadata");
            cores.put(id, core);
        }
        var jobRows = root.path("vSkill").path("coreGroup").fields();
        while (jobRows.hasNext()) {
            var e = jobRows.next(); var n = e.getValue();
            var group = new Group(Set.copyOf(values(n.path("skill"))), Set.copyOf(values(n.path("enforce"))), Set.copyOf(values(n.path("common"))));
            for (var ids : List.of(group.skills, group.boosts, group.common))
                if (!cores.keySet().containsAll(ids)) throw new IOException("Unknown core in282 job group");
            groups.put(Integer.parseInt(e.getKey()), group);
        }
        root.path("coreGemstone").elements().forEachRemaining(n -> {
            for (int itemId : values(n.path("id"))) gemstones.put(itemId, new Gemstone(itemId, n.path("vPoint").asInt(),
                    n.path("openCore").asInt(), n.path("reqQuest").asInt(), n.path("animationID").asInt()));
        });
        readCosts(root.path("vPoint").path("skill"), skillCosts);
        readCosts(root.path("vPoint").path("enforce"), boostCosts);
        var info = root.path("info");
        resetMeso = info.path("resetMeso").asInt(); registerSpecialCost = info.path("registerSpecialCorevPoint").asInt();
        extendSpecialCost = info.path("extendSpecialCorevPoint").asInt(); transformCost = info.path("vPointTransformValue").asInt();
        transformItem = info.path("vPointTransformID").asInt(); transformMinutes = info.path("vPointTransformPeriod").asInt();
        if (cores.isEmpty() || groups.isEmpty() || resetMeso <= 0 || transformCost <= 0 || transformItem <= 0)
            throw new IOException("Incomplete282 matrix rules");
    }

    private static List<Integer> values(JsonNode node) {
        List<Integer> result = new ArrayList<>(); node.elements().forEachRemaining(v -> result.add(v.asInt()));
        return List.copyOf(result);
    }
    private static void readCosts(JsonNode node, NavigableMap<Integer, Integer> destination) {
        node.fields().forEachRemaining(e -> destination.put(Integer.parseInt(e.getKey()), e.getValue().asInt()));
    }
    public Map<Integer, Core> cores() { return Collections.unmodifiableMap(cores); }
    public Map<Integer, Group> groups() { return Collections.unmodifiableMap(groups); }
    public Map<Integer, Gemstone> gemstones() { return Collections.unmodifiableMap(gemstones); }
    public boolean legacyCompatible(int id) { return legacyCompatible.contains(id); }
    public Core core(int id) { var value = cores.get(id); if (value == null) throw new IllegalArgumentException("Unknown282 core"); return value; }
    public boolean allowed(int job, int id) {
        Core core = cores.get(id); Group group = groups.get(job);
        return core != null && group != null && (core.type == 2 || group.contains(id));
    }
    /** Native10510E0 takes the first threshold >= target level, not a floor lookup. */
    public int costForLevel(int id, int targetLevel) {
        Core core = core(id);
        if (targetLevel < 1 || targetLevel > core.maxLevel || core.type > 1) throw new IllegalArgumentException("Invalid core level");
        var entry = (core.type == 0 ? skillCosts : boostCosts).ceilingEntry(targetLevel);
        if (entry == null || entry.getValue() <= 0) throw new IllegalArgumentException("Missing core level cost");
        return entry.getValue();
    }
    public long costBetween(int id, int from, int to) {
        if (from < 0 || to <= from) throw new IllegalArgumentException("Invalid level transition");
        long value = 0; for (int level = from + 1; level <= to; level++) value = Math.addExact(value, costForLevel(id, level));
        return value;
    }
    /** Native10517E0 uses job skill-group or notAbleSelfOpen as the non-refundable first level. */
    public int resetFloor(int job, int id) {
        Core core = core(id); Group group = groups.get(job);
        return !core.selfOpen || group != null && group.skills.contains(id) ? 1 : 0;
    }
}
