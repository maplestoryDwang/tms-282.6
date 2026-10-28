package net.swordie.ms.handlers.login.tms282;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.loaders.MobData;
import net.swordie.ms.util.Position;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Portal;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * TMS 282.3 summon sacks (CP014B), from the 282 client's item data {@code data/wz282/Item.wz/Consume/0210.img.xml}
 * (44 items, 2100156..2101318). Per item: {@code mob/n/{id, prob}}; {@code info/random = 1} picks one entry weighted
 * by prob, otherwise every entry is rolled on its own ({@code prob}% each, 100 = always);
 * {@code info/limit/{town, nearPortal, teleport}} restricts where the sack may be used (teleport: not enforced, its
 * meaning is not known). The field's own {@link FieldOption#SummonLimit} forbids every sack. The GMS 265 server
 * loads none of this (its handler only consumed the sack).
 * <p>
 * {@link #use} validates everything before it changes anything: an unknown sack, a mob id the server cannot load, a
 * forbidden field -> {@link Result#refused} and nothing consumed. Otherwise the sack is consumed (also when every
 * roll failed, as the item is spent in the official game) and the rolled mobs spawn at the user's position.
 */
public final class Tms282SummonSacks {
    public record Entry(int mobId, int prob) {
    }

    public record Sack(int itemId, boolean random, boolean notInTown, int nearPortal, boolean teleportLimit, List<Entry> mobs) {
        public Sack {
            mobs = List.copyOf(mobs);
        }
    }

    /** What {@link #use} did: the spawned template ids, or why it refused (nothing changed then). */
    public record Result(List<Integer> spawned, String refused) {
        static Result refuse(String why) {
            return new Result(List.of(), why);
        }

        public boolean ok() {
            return refused == null;
        }
    }

    static final Path SOURCE = Path.of(ServerConstants.DIR, "data", "wz282", "Item.wz", "Consume", "0210.img.xml");
    private static volatile Tms282SummonSacks instance;

    private final Map<Integer, Sack> sacks;

    Tms282SummonSacks(Map<Integer, Sack> sacks) {
        this.sacks = Collections.unmodifiableMap(new LinkedHashMap<>(sacks));
    }

    /** The table from {@link #SOURCE}, loaded once; empty (every sack refused) when the file cannot be read. */
    public static Tms282SummonSacks get() {
        Tms282SummonSacks s = instance;
        if (s == null) {
            synchronized (Tms282SummonSacks.class) {
                if (instance == null) {
                    Map<Integer, Sack> loaded;
                    try {
                        loaded = parse(SOURCE);
                        System.out.println("[TMS282] summon sacks: " + loaded.size() + " from " + SOURCE);
                    } catch (Exception e) {
                        loaded = Map.of();
                        System.out.println("[TMS282] summon sacks: cannot read " + SOURCE + ": " + e);
                    }
                    instance = new Tms282SummonSacks(loaded);
                }
                s = instance;
            }
        }
        return s;
    }

    public Map<Integer, Sack> sacks() {
        return sacks;
    }

    public Optional<Sack> sack(int itemId) {
        return Optional.ofNullable(sacks.get(itemId));
    }

    /** Parses a 0210.img.xml dump. */
    public static Map<Integer, Sack> parse(Path xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc;
        try (var in = Files.newInputStream(xml)) {
            doc = f.newDocumentBuilder().parse(in);
        }
        Map<Integer, Sack> out = new LinkedHashMap<>();
        for (Element item : children(doc.getDocumentElement(), "imgdir")) {
            int itemId;
            try {
                itemId = Integer.parseInt(item.getAttribute("name"));
            } catch (NumberFormatException e) {
                continue;
            }
            Element info = child(item, "info");
            Element limit = info == null ? null : child(info, "limit");
            Element mob = child(item, "mob");
            if (mob == null) {
                continue;
            }
            List<Entry> entries = new ArrayList<>();
            for (Element e : children(mob, "imgdir")) {
                int id = intValue(e, "id", 0);
                int prob = intValue(e, "prob", 100); // the first prob node wins (2101314/1 has two)
                if (id > 0) {
                    entries.add(new Entry(id, prob));
                }
            }
            if (entries.isEmpty()) {
                continue;
            }
            out.put(itemId, new Sack(itemId, intValue(info, "random", 0) == 1, intValue(limit, "town", 0) == 1,
                    intValue(limit, "nearPortal", 0), intValue(limit, "teleport", 0) == 1, entries));
        }
        return out;
    }

    /**
     * Uses {@code sack} (verified by the caller to be in the CONSUME slot the client named) in {@code chr}'s field.
     */
    public Result use(Char chr, Item sackItem, RandomGenerator rnd) {
        Objects.requireNonNull(chr, "chr");
        Sack sack = sacks.get(sackItem.getItemId());
        if (sack == null) {
            return Result.refuse("no 282 summon data for " + sackItem.getItemId());
        }
        Field field = chr.getField();
        if (field == null) {
            return Result.refuse("no field");
        }
        if ((field.getFieldLimit() & FieldOption.SummonLimit.getVal()) != 0) {
            return Result.refuse("field " + field.getId() + " has SummonLimit");
        }
        if (sack.notInTown() && field.isTown()) {
            return Result.refuse("sack limit town: field " + field.getId() + " is a town");
        }
        Position pos = chr.getPosition();
        if (pos == null) {
            return Result.refuse("no position");
        }
        if (sack.nearPortal() > 0) {
            for (Portal p : field.getPortals()) {
                long dx = p.getX() - pos.getX(), dy = p.getY() - pos.getY();
                if (dx * dx + dy * dy < (long) sack.nearPortal() * sack.nearPortal()) {
                    return Result.refuse("sack limit nearPortal " + sack.nearPortal() + ": portal " + p.getName() + " too close");
                }
            }
        }
        for (Entry e : sack.mobs()) {
            if (MobData.getMobById(e.mobId()) == null) {
                return Result.refuse("mob " + e.mobId() + " is not in the server's mob data");
            }
        }
        List<Integer> rolled = roll(sack, rnd);
        chr.consumeItem(sackItem, 1);
        List<Integer> spawned = new ArrayList<>(rolled.size());
        for (int id : rolled) {
            Mob m = field.spawnMob(id, pos.getX(), pos.getY(), false);
            if (m != null) {
                spawned.add(id);
            }
        }
        return new Result(List.copyOf(spawned), null);
    }

    /** The template ids to spawn: one weighted pick when {@code random}, else each entry with prob%. */
    public static List<Integer> roll(Sack sack, RandomGenerator rnd) {
        List<Integer> out = new ArrayList<>();
        if (sack.random()) {
            int total = 0;
            for (Entry e : sack.mobs()) {
                total += Math.max(0, e.prob());
            }
            if (total <= 0) {
                return out;
            }
            int r = rnd.nextInt(total);
            for (Entry e : sack.mobs()) {
                r -= Math.max(0, e.prob());
                if (r < 0) {
                    out.add(e.mobId());
                    break;
                }
            }
            return out;
        }
        for (Entry e : sack.mobs()) {
            if (e.prob() >= 100 || rnd.nextInt(100) < e.prob()) {
                out.add(e.mobId());
            }
        }
        return out;
    }

    // ---- XML helpers --------------------------------------------------------------------------------------------

    private static List<Element> children(Element parent, String tag) {
        List<Element> out = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof Element el && el.getTagName().equals(tag)) {
                out.add(el);
            }
        }
        return out;
    }

    private static Element child(Element parent, String name) {
        for (Element e : children(parent, "imgdir")) {
            if (name.equals(e.getAttribute("name"))) {
                return e;
            }
        }
        return null;
    }

    private static int intValue(Element parent, String name, int def) {
        if (parent == null) {
            return def;
        }
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element el && name.equals(el.getAttribute("name"))) {
                try {
                    return Integer.parseInt(el.getAttribute("value"));
                } catch (NumberFormatException e) {
                    return def;
                }
            }
        }
        return def;
    }
}
