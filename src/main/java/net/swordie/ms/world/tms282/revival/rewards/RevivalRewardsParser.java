package net.swordie.ms.world.tms282.revival.rewards;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.swordie.ms.world.tms282.revival.hub.HubDefinition;

import java.util.*;
import java.util.function.IntPredicate;

/**
 * revival-rewards.json reader / writer. Each top-level section ("hubs", "punch", "shooting") is one registered
 * {@link Section} handler; a section the file leaves out keeps the built-in values. Anything wrong (syntax, unknown
 * key, unknown event or mission, item that does not exist or cannot be granted, quantity / price / target out of range)
 * rejects the whole file with {@link HotFile.Invalid}.
 * <pre>
 * {"version": "...",
 *  "hubs": {"lacheln": {"missions": {"d1": {"target": 1000, "rewards": [{"item": 4310343, "qty": 30}], "title": "..."}},
 *                       "offers": [{"item": 2450180, "qty": 1, "coin": 4310343, "cost": 60}]}},
 *  "punch": {"tiers": [[{"item": 2023999, "qty": 1}], ...7 lists], "tomatoShards": [10, 15, 20, 30, 40, 50, 70]},
 *  "shooting": {"tiers": [{"min": 35, "fantasy": 60, "dream": 5}, ..., {"min": 0, ...}]}}
 * </pre>
 */
public final class RevivalRewardsParser {
    /** What a section may consult: the code hubs (structure) and the item check. */
    public record Context(Map<String, HubDefinition> baseHubs, IntPredicate itemOk) {
    }

    /** Mutable copy of the defaults that the sections overwrite. */
    static final class Draft {
        String version;
        final Map<String, RevivalRewards.HubRewards> hubs;
        List<List<HubDefinition.Reward>> punchTiers;
        List<Integer> tomatoShards;
        List<RevivalRewards.ShootingTier> shootingTiers;

        Draft(RevivalRewards d) {
            hubs = new LinkedHashMap<>(d.hubs());
            punchTiers = d.punchTiers();
            tomatoShards = d.tomatoShards();
            shootingTiers = d.shootingTiers();
        }

        RevivalRewards build() {
            return new RevivalRewards(version, hubs, punchTiers, tomatoShards, shootingTiers);
        }
    }

    /** One top-level section of the file. */
    interface Section {
        String name();

        void read(JsonNode node, Draft draft, Context ctx);

        JsonNode write(RevivalRewards r, ObjectMapper m);
    }

    private static final Map<String, Section> SECTIONS = new LinkedHashMap<>();

    static {
        register(new HubsSection());
        register(new PunchSection());
        register(new ShootingSection());
    }

    private static void register(Section s) {
        if (SECTIONS.putIfAbsent(s.name(), s) != null) throw new IllegalStateException("duplicate section " + s.name());
    }

    private RevivalRewardsParser() {
    }

    static HotFile.Invalid bad(String m) {
        return new HotFile.Invalid(m);
    }

    public static RevivalRewards parse(byte[] json, RevivalRewards defaults, Context ctx) {
        JsonNode root;
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
            mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            root = mapper.readTree(json);
        } catch (Exception e) {
            throw bad("not valid JSON: " + e.getClass().getSimpleName());
        }
        if (root == null || !root.isObject()) throw bad("root must be an object");
        String version = root.path("version").asText("");
        if (version.isEmpty() || version.length() > 40) throw bad("version missing");
        Draft d = new Draft(defaults);
        d.version = version;
        for (Iterator<String> it = root.fieldNames(); it.hasNext(); ) {
            String k = it.next();
            if (k.equals("version") || k.equals("note")) continue;
            Section s = SECTIONS.get(k);
            if (s == null) throw bad("unknown section " + k);
            s.read(root.get(k), d, ctx);
        }
        return d.build();
    }

    /** The full file for these values (deploy sample, docs). */
    public static String write(RevivalRewards r) {
        ObjectMapper m = new ObjectMapper();
        ObjectNode root = m.createObjectNode();
        root.put("version", r.version());
        root.put("note", "revival-1005 獎勵、門檻、商店價格（暫定，可改）。存檔後 2 秒內生效；格式錯誤或道具不存在時沿用上一份正確設定。說明見 docs/revival/README.md");
        for (Section s : SECTIONS.values()) root.set(s.name(), s.write(r, m));
        try {
            return m.writerWithDefaultPrettyPrinter().writeValueAsString(root).replace("\r\n", "\n") + "\n";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---- field helpers ----------------------------------------------------------------------------------------------

    static void only(JsonNode n, String where, String... allowed) {
        if (n == null || !n.isObject()) throw bad(where + " must be an object");
        Set<String> ok = Set.of(allowed);
        for (Iterator<String> it = n.fieldNames(); it.hasNext(); ) {
            String f = it.next();
            if (!ok.contains(f)) throw bad(where + ": unknown field " + f);
        }
    }

    static int intIn(JsonNode n, String field, int min, int max, String where) {
        JsonNode v = n.get(field);
        if (v == null || !v.isIntegralNumber() || !v.canConvertToInt()) throw bad(where + "." + field + " must be an integer");
        int x = v.intValue();
        if (x < min || x > max) throw bad(where + "." + field + " = " + x + " out of range " + min + ".." + max);
        return x;
    }

    static int item(JsonNode n, String field, String where, Context ctx) {
        int id = intIn(n, field, 1, Integer.MAX_VALUE, where);
        if (!ctx.itemOk().test(id)) throw bad(where + "." + field + " = " + id + ": item does not exist or cannot be granted");
        return id;
    }

    static List<HubDefinition.Reward> rewards(JsonNode arr, String where, Context ctx) {
        if (arr == null || !arr.isArray() || arr.isEmpty() || arr.size() > 8) throw bad(where + " must be a list of 1..8 rewards");
        List<HubDefinition.Reward> out = new ArrayList<>();
        int i = 0;
        for (JsonNode r : arr) {
            String w = where + "[" + i++ + "]";
            only(r, w, "item", "qty");
            out.add(new HubDefinition.Reward(item(r, "item", w, ctx), intIn(r, "qty", 1, RevivalRewards.MAX_QTY, w)));
        }
        return out;
    }

    static ArrayNode writeRewards(List<HubDefinition.Reward> rs, ObjectMapper m) {
        ArrayNode a = m.createArrayNode();
        for (HubDefinition.Reward r : rs) a.addObject().put("item", r.itemId()).put("qty", r.quantity());
        return a;
    }

    // ---- sections ---------------------------------------------------------------------------------------------------

    /** "hubs": per event, mission overrides by id and the complete offer list. */
    static final class HubsSection implements Section {
        public String name() {
            return "hubs";
        }

        public void read(JsonNode node, Draft d, Context ctx) {
            if (node == null || !node.isObject()) throw bad("hubs must be an object");
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                String key = e.getKey();
                HubDefinition base = ctx.baseHubs().get(key);
                if (base == null) throw bad("hubs: unknown event " + key);
                JsonNode h = e.getValue();
                only(h, "hubs." + key, "missions", "offers");
                RevivalRewards.HubRewards current = d.hubs.get(key);
                Map<String, RevivalRewards.MissionOverride> ms = new LinkedHashMap<>(current.missions());
                if (h.has("missions")) {
                    JsonNode mn = h.get("missions");
                    if (!mn.isObject()) throw bad("hubs." + key + ".missions must be an object");
                    for (Iterator<Map.Entry<String, JsonNode>> mi = mn.fields(); mi.hasNext(); ) {
                        Map.Entry<String, JsonNode> me = mi.next();
                        String w = "hubs." + key + ".missions." + me.getKey();
                        if (base.mission(me.getKey()).isEmpty()
                                && net.swordie.ms.world.tms282.revival.RevivalHubDefinitions.retired(key, me.getKey())) {
                            continue; // retired on purpose (launcherapi9: mushroom level checkpoints -> MushroomPassService)
                        }
                        HubDefinition.Mission bm = base.mission(me.getKey()).orElseThrow(() -> bad(w + ": unknown mission"));
                        JsonNode o = me.getValue();
                        only(o, w, "target", "rewards", "title");
                        RevivalRewards.MissionOverride old = ms.get(bm.id());
                        int target = o.has("target") ? intIn(o, "target", 1, RevivalRewards.MAX_TARGET, w) : old.target();
                        List<HubDefinition.Reward> rw = o.has("rewards") ? rewards(o.get("rewards"), w + ".rewards", ctx) : old.rewards();
                        String title = null;
                        if (o.has("title")) {
                            if (!o.get("title").isTextual() || o.get("title").asText().isBlank() || o.get("title").asText().length() > 40
                                    || o.get("title").asText().contains("#"))
                                throw bad(w + ".title must be 1..40 characters without #");
                            title = o.get("title").asText();
                        }
                        ms.put(bm.id(), new RevivalRewards.MissionOverride(target, rw, title));
                    }
                }
                List<HubDefinition.Offer> offers = current.offers();
                if (h.has("offers")) {
                    JsonNode on = h.get("offers");
                    if (!on.isArray() || on.size() > 60) throw bad("hubs." + key + ".offers must be a list of at most 60 offers");
                    offers = new ArrayList<>();
                    int i = 0;
                    for (JsonNode o : on) {
                        String w = "hubs." + key + ".offers[" + i++ + "]";
                        only(o, w, "item", "qty", "coin", "cost");
                        int item = item(o, "item", w, ctx), coin = item(o, "coin", w, ctx);
                        if (item == coin) throw bad(w + ": item and coin are the same");
                        offers.add(new HubDefinition.Offer(item, intIn(o, "qty", 1, RevivalRewards.MAX_QTY, w), coin,
                                intIn(o, "cost", 1, RevivalRewards.MAX_QTY, w)));
                    }
                }
                RevivalRewards.HubRewards next = new RevivalRewards.HubRewards(ms, offers);
                try {
                    new RevivalRewards("check", Map.of(key, next), List.of(), List.of(), List.of()).apply(base);
                } catch (IllegalArgumentException ex) {
                    throw bad("hubs." + key + ": " + ex.getMessage());
                }
                d.hubs.put(key, next);
            }
        }

        public JsonNode write(RevivalRewards r, ObjectMapper m) {
            ObjectNode out = m.createObjectNode();
            r.hubs().forEach((key, h) -> {
                ObjectNode hn = out.putObject(key);
                ObjectNode ms = hn.putObject("missions");
                h.missions().forEach((id, o) -> {
                    ObjectNode on = ms.putObject(id);
                    on.put("target", o.target());
                    on.set("rewards", writeRewards(o.rewards(), m));
                    if (o.title() != null) on.put("title", o.title());
                });
                ArrayNode offers = hn.putArray("offers");
                for (HubDefinition.Offer o : h.offers())
                    offers.addObject().put("item", o.itemId()).put("qty", o.quantity()).put("coin", o.costItem()).put("cost", o.costQuantity());
            });
            return out;
        }
    }

    /** "punch": 世界最強拳擊王 tier rewards (7 lists, by machines broken 0..6) and 番茄節 夜之碎片 (7 numbers). */
    static final class PunchSection implements Section {
        public String name() {
            return "punch";
        }

        public void read(JsonNode node, Draft d, Context ctx) {
            only(node, "punch", "tiers", "tomatoShards");
            if (node.has("tiers")) {
                JsonNode t = node.get("tiers");
                if (!t.isArray() || t.size() != RevivalRewards.PUNCH_TIERS) throw bad("punch.tiers must be 7 reward lists (0..6 machines)");
                List<List<HubDefinition.Reward>> tiers = new ArrayList<>();
                for (int i = 0; i < t.size(); i++) tiers.add(rewards(t.get(i), "punch.tiers[" + i + "]", ctx));
                d.punchTiers = tiers;
            }
            if (node.has("tomatoShards")) {
                JsonNode t = node.get("tomatoShards");
                if (!t.isArray() || t.size() != RevivalRewards.PUNCH_TIERS) throw bad("punch.tomatoShards must be 7 numbers");
                List<Integer> s = new ArrayList<>();
                for (int i = 0; i < t.size(); i++) {
                    JsonNode v = t.get(i);
                    if (!v.isIntegralNumber() || !v.canConvertToInt() || v.intValue() < 1 || v.intValue() > RevivalRewards.MAX_QTY)
                        throw bad("punch.tomatoShards[" + i + "] must be 1.." + RevivalRewards.MAX_QTY);
                    s.add(v.intValue());
                }
                d.tomatoShards = s;
            }
        }

        public JsonNode write(RevivalRewards r, ObjectMapper m) {
            ObjectNode out = m.createObjectNode();
            ArrayNode tiers = out.putArray("tiers");
            for (List<HubDefinition.Reward> t : r.punchTiers()) tiers.add(writeRewards(t, m));
            ArrayNode s = out.putArray("tomatoShards");
            for (int x : r.tomatoShards()) s.add(x);
            return out;
        }
    }

    /** "shooting": 動態視力修煉場 tiers, highest first, the last one with min 0. */
    static final class ShootingSection implements Section {
        public String name() {
            return "shooting";
        }

        public void read(JsonNode node, Draft d, Context ctx) {
            only(node, "shooting", "tiers");
            if (!node.has("tiers")) return;
            JsonNode t = node.get("tiers");
            if (!t.isArray() || t.isEmpty() || t.size() > 10) throw bad("shooting.tiers must be 1..10 tiers");
            List<RevivalRewards.ShootingTier> tiers = new ArrayList<>();
            int prev = Integer.MAX_VALUE;
            for (int i = 0; i < t.size(); i++) {
                String w = "shooting.tiers[" + i + "]";
                JsonNode n = t.get(i);
                only(n, w, "min", "fantasy", "dream");
                int min = intIn(n, "min", 0, 150, w);
                if (min >= prev) throw bad(w + ".min must be lower than the tier before it");
                prev = min;
                tiers.add(new RevivalRewards.ShootingTier(min, intIn(n, "fantasy", 1, RevivalRewards.MAX_QTY, w),
                        intIn(n, "dream", 0, RevivalRewards.MAX_QTY, w)));
            }
            if (prev != 0) throw bad("shooting.tiers: the last tier must have min 0");
            d.shootingTiers = tiers;
        }

        public JsonNode write(RevivalRewards r, ObjectMapper m) {
            ObjectNode out = m.createObjectNode();
            ArrayNode tiers = out.putArray("tiers");
            for (RevivalRewards.ShootingTier s : r.shootingTiers())
                tiers.addObject().put("min", s.minScore()).put("fantasy", s.fantasy()).put("dream", s.dream());
            return out;
        }
    }
}
