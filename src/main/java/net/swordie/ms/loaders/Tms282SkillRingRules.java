package net.swordie.ms.loaders;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.swordie.ms.connection.packet.Tms282SkillRingRequest;
import net.swordie.ms.connection.packet.Tms282SkillRingRequest.Rule;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Rules explicitly imported from the supplied 279 pack. Loading never touches character state or SQL. */
public final class Tms282SkillRingRules {
    private record Key(int stone, int ring, int from, int count) { }
    private final Map<Key, Rule> rules;
    private final Map<Integer, LevelExchange> levelExchange;

    /**
     * 279 {@code [[skill-ring.level_exchange]]}: a ring of {@code ringItemId} may be re-leveled to any level in
     * [{@code minLevel}, {@code maxLevel}] for {@code meso}. Data only: no native 282 request/catalog for it has been
     * identified yet (LP0AC8 reason17 kind0 is the grindstone catalog; see research/tms2823/port279/README.md).
     */
    public record LevelExchange(int ringItemId, int minLevel, int maxLevel, long meso) {
        public LevelExchange {
            if (ringItemId / 10000 != 111 || minLevel <= 0 || maxLevel < minLevel || maxLevel > 255 || meso < 0)
                throw new IllegalArgumentException("無效的戒指等級交換規則。");
        }
    }

    public Tms282SkillRingRules(Collection<Rule> input) {
        this(input, List.of());
    }

    public Tms282SkillRingRules(Collection<Rule> input, Collection<LevelExchange> exchanges) {
        Map<Key, Rule> values = new LinkedHashMap<>();
        for (Rule rule : input) {
            Objects.requireNonNull(rule);
            if (values.putIfAbsent(key(rule), rule) != null)
                throw new IllegalArgumentException("Duplicate skill ring rule");
        }
        if (values.isEmpty()) throw new IllegalArgumentException("Empty skill ring rules");
        rules = Collections.unmodifiableMap(values);
        Map<Integer, LevelExchange> byRing = new LinkedHashMap<>();
        for (LevelExchange exchange : exchanges) {
            Objects.requireNonNull(exchange);
            if (byRing.putIfAbsent(exchange.ringItemId(), exchange) != null)
                throw new IllegalArgumentException("Duplicate skill ring level exchange rule");
        }
        levelExchange = Collections.unmodifiableMap(byRing);
    }

    public static Tms282SkillRingRules load(Path path) throws IOException {
        ObjectMapper json = new ObjectMapper();
        var root = json.readTree(path.toFile());
        if (!root.path("grindstone").isArray()) throw new IOException("Missing grindstone rules");
        List<Rule> rows = new ArrayList<>();
        Set<String> columns = Set.of("stoneItemId", "useCount", "prop", "meso", "ringItemId", "ringFromLevel", "ringToLevel");
        for (var node : root.get("grindstone")) {
            Set<String> actual = new HashSet<>(); node.fieldNames().forEachRemaining(actual::add);
            if (!actual.equals(columns)) throw new IOException("Invalid skill ring columns");
            for (String column : columns) {
                var value = node.get(column);
                if (!value.isIntegralNumber() || (column.equals("meso") ? !value.canConvertToLong() : !value.canConvertToInt()))
                    throw new IOException("Invalid skill ring value: " + column);
            }
            rows.add(json.treeToValue(node, Rule.class));
        }
        // Optional; "grindstoneUnsupported282"/"levelExchangeUnsupported282" are reference rows and never loaded.
        List<LevelExchange> exchanges = new ArrayList<>();
        var exchangeNode = root.path("levelExchange");
        if (!exchangeNode.isMissingNode()) {
            if (!exchangeNode.isArray()) throw new IOException("Invalid levelExchange rules");
            Set<String> exchangeColumns = Set.of("ringItemId", "minLevel", "maxLevel", "meso");
            for (var node : exchangeNode) {
                Set<String> actual = new HashSet<>(); node.fieldNames().forEachRemaining(actual::add);
                if (!actual.equals(exchangeColumns)) throw new IOException("Invalid skill ring level exchange columns");
                for (String column : exchangeColumns) {
                    var value = node.get(column);
                    if (!value.isIntegralNumber() || (column.equals("meso") ? !value.canConvertToLong() : !value.canConvertToInt()))
                        throw new IOException("Invalid skill ring level exchange value: " + column);
                }
                try {
                    exchanges.add(new LevelExchange(node.get("ringItemId").intValue(), node.get("minLevel").intValue(),
                            node.get("maxLevel").intValue(), node.get("meso").longValue()));
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid skill ring level exchange rule", e);
                }
            }
        }
        return new Tms282SkillRingRules(rows, exchanges);
    }

    private static Key key(Rule rule) {
        return new Key(rule.stoneItemId(), rule.ringItemId(), rule.ringFromLevel(), rule.useCount());
    }

    public List<Rule> rows() { return List.copyOf(rules.values()); }

    public List<LevelExchange> levelExchangeRows() { return List.copyOf(levelExchange.values()); }

    public Optional<LevelExchange> levelExchange(int ringItemId) { return Optional.ofNullable(levelExchange.get(ringItemId)); }

    /**
     * Validate a level exchange before any money or item change. {@code skillMaxLevel} is the lowest max level of the
     * ring's skills in the active (282) data; expiry/ownership are caller checks, like {@link #validate}.
     */
    public LevelExchange validateLevelExchange(int ownedRingId, int ownedRingLevel, int targetLevel,
                                               long money, int skillMaxLevel) {
        LevelExchange rule = levelExchange.get(ownedRingId);
        if (rule == null)
            throw new IllegalArgumentException("此戒指無法交換等級。");
        if (ownedRingLevel < rule.minLevel() || ownedRingLevel > rule.maxLevel())
            throw new IllegalArgumentException("戒指目前等級無法交換。");
        if (targetLevel == ownedRingLevel || targetLevel < rule.minLevel() || targetLevel > rule.maxLevel())
            throw new IllegalArgumentException("請選擇可交換的戒指等級。");
        if (targetLevel > skillMaxLevel)
            throw new IllegalArgumentException("目前技能資料不支援此戒指等級。");
        if (money < rule.meso())
            throw new IllegalArgumentException("楓幣不足。");
        return rule;
    }

    /** Validate before RNG, item removal, money changes, or database writes. Expiry/ownership are caller checks. */
    public Rule validate(Tms282SkillRingRequest request, int ownedRingId, int ownedRingLevel,
                         long money, long usableStoneCount, int skillMaxLevel) {
        Rule rule = rules.get(key(request.rule()));
        if (rule == null || !rule.equals(request.rule()))
            throw new IllegalArgumentException("研磨條件已更新，請重新開啟視窗。");
        if (ownedRingId != rule.ringItemId() || ownedRingLevel != rule.ringFromLevel())
            throw new IllegalArgumentException("戒指或等級已變更，請重新選擇。");
        if (rule.ringToLevel() > skillMaxLevel)
            throw new IllegalArgumentException("目前技能資料不支援此戒指等級。");
        if (money < rule.meso() || usableStoneCount < rule.useCount())
            throw new IllegalArgumentException("楓幣或研磨石不足。");
        return rule;
    }
}
