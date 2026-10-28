package net.swordie.ms.client.character.emoticon;

import net.swordie.ms.ServerConstants;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Which chat emoticons exist and to which group (tab) each belongs.
 * <p>
 * Emoticon ids are {@code group * 10000 + n} (e.g. 10030001 = group 1003, #1; item scripts
 * data/scripts/item/consume_26310xx.js grant them). Sources, merged in this order:
 * <ol>
 *     <li>{@code <WZ_DIR>/UI.wz/ChatEmoticon.img.xml} (the 282 dump has no UI.wz today),
 *     then {@code data/wz265/UI.wz/ChatEmoticon.img.xml} ({@code ChatEmoticon.img/Emoticon/<group>/<id>});</li>
 *     <li>{@code data/resources/tms282/chat-emoticon-groups.tsv}: {@code groupId<TAB>id,id,...} lines (# comments),
 *     for 282 groups missing from the 265 dump (1000, 1002, 1003, ...).</li>
 * </ol>
 * Loaded lazily once; {@link #reload()} re-reads.
 */
public final class EmoticonCatalog {
    public static final int MIN_GROUP = 1000;
    public static final int MAX_GROUP = 1999;

    private static volatile Map<Integer, List<Integer>> groups;

    private EmoticonCatalog() {
    }

    public static int groupOf(int emoticonId) {
        return emoticonId / 10000;
    }

    /** Id shape check only ({@code 1000xxxx..1999xxxx}, n &gt; 0). */
    public static boolean wellFormed(int emoticonId) {
        int g = groupOf(emoticonId);
        return g >= MIN_GROUP && g <= MAX_GROUP && emoticonId % 10000 > 0;
    }

    public static boolean knowsGroup(int groupId) {
        return groups().containsKey(groupId);
    }

    /** True when the catalog lists the emoticon; false when unknown (including unknown group). */
    public static boolean exists(int emoticonId) {
        List<Integer> ids = groups().get(groupOf(emoticonId));
        return ids != null && ids.contains(emoticonId);
    }

    /** Emoticon ids of the group, ascending; empty when the group is unknown. */
    public static List<Integer> group(int groupId) {
        return groups().getOrDefault(groupId, List.of());
    }

    public static Map<Integer, List<Integer>> all() {
        return groups();
    }

    public static synchronized void reload() {
        groups = load();
    }

    private static Map<Integer, List<Integer>> groups() {
        Map<Integer, List<Integer>> g = groups;
        if (g == null) {
            synchronized (EmoticonCatalog.class) {
                if (groups == null) {
                    groups = load();
                }
                g = groups;
            }
        }
        return g;
    }

    private static Map<Integer, List<Integer>> load() {
        Map<Integer, TreeSet<Integer>> acc = new TreeMap<>();
        Path primary = Path.of(ServerConstants.WZ_DIR, "UI.wz", "ChatEmoticon.img.xml");
        Path fallback = Path.of(ServerConstants.DIR, "data", "wz265", "UI.wz", "ChatEmoticon.img.xml");
        if (Files.isRegularFile(primary)) {
            readWz(primary, acc);
        } else if (Files.isRegularFile(fallback)) {
            readWz(fallback, acc);
        }
        readOverrides(Path.of(ServerConstants.RESOURCES_DIR, "tms282", "chat-emoticon-groups.tsv"), acc);
        Map<Integer, List<Integer>> out = new TreeMap<>();
        acc.forEach((k, v) -> out.put(k, List.copyOf(v)));
        System.out.printf("[TMS282][emoticon] catalog: %d groups, %d emoticons%n", out.size(),
                out.values().stream().mapToInt(List::size).sum());
        return Collections.unmodifiableMap(out);
    }

    /** ChatEmoticon.img(depth1) / Emoticon(2) / group(3) / emoticon(4). */
    private static void readWz(Path file, Map<Integer, TreeSet<Integer>> acc) {
        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        try (InputStream in = Files.newInputStream(file)) {
            XMLStreamReader r = f.createXMLStreamReader(in);
            int depth = 0;
            boolean inEmoticon = false;
            int group = -1;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String name = r.getAttributeValue(null, "name");
                    boolean dir = "imgdir".equals(r.getLocalName());
                    if (depth == 2) {
                        inEmoticon = dir && "Emoticon".equals(name);
                    } else if (depth == 3 && inEmoticon) {
                        group = dir ? parse(name) : -1;
                    } else if (depth == 4 && inEmoticon && dir && group >= MIN_GROUP && group <= MAX_GROUP) {
                        int id = parse(name);
                        if (id > 0 && groupOf(id) == group) {
                            acc.computeIfAbsent(group, k -> new TreeSet<>()).add(id);
                        }
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    depth--;
                }
            }
            r.close();
        } catch (Exception e) {
            EmoticonTables.log("read " + file, e);
        }
    }

    private static void readOverrides(Path file, Map<Integer, TreeSet<Integer>> acc) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] cols = line.split("\t", 2);
                int group = parse(cols[0].strip());
                if (group < MIN_GROUP || group > MAX_GROUP || cols.length < 2) {
                    continue;
                }
                List<Integer> ids = new ArrayList<>();
                for (String s : cols[1].split("[,\\s]+")) {
                    int id = parse(s.strip());
                    if (id > 0 && groupOf(id) == group) {
                        ids.add(id);
                    }
                }
                acc.computeIfAbsent(group, k -> new TreeSet<>()).addAll(ids);
            }
        } catch (Exception e) {
            EmoticonTables.log("read " + file, e);
        }
    }

    private static int parse(String s) {
        if (s == null || s.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
