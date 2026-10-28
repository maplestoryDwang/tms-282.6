package net.swordie.ms.loaders;

import net.swordie.ms.ServerConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Creation look whitelist from the TMS 282.3 Etc.wz/MakeCharInfo.img (data/wz282-special).
 * Nodes are keyed by the normalized creation job from the native race table
 * (research/tms2823/packet-audit/login-job-wire-map-2823.md), then male/female.
 */
public final class Tms282MakeCharInfo {
    public static final Path DEFAULT_FILE =
            Path.of(ServerConstants.DIR, "data", "wz282-special", "Etc.wz", "MakeCharInfo.img.xml");

    /** Native race -> MakeCharInfo job nodes. Explorer and Demon families span several variant nodes. */
    private static final Map<Integer, List<String>> RACE_NODES = Map.ofEntries(
            Map.entry(0, List.of("3000")), Map.entry(1, List.of("000", "000_1", "000_3", "508")),
            Map.entry(2, List.of("1000")), Map.entry(3, List.of("2000")), Map.entry(4, List.of("2001")),
            Map.entry(5, List.of("2002")), Map.entry(6, List.of("3001", "3101")), Map.entry(7, List.of("2003")),
            Map.entry(8, List.of("000", "000_1", "000_3", "508")), Map.entry(9, List.of("5000")),
            Map.entry(10, List.of("2004")), Map.entry(11, List.of("6000")), Map.entry(12, List.of("6001")),
            Map.entry(13, List.of("000", "000_1", "000_3", "508")), Map.entry(14, List.of("3002")),
            Map.entry(15, List.of("10112")), Map.entry(16, List.of("2005")), Map.entry(17, List.of("13100")),
            Map.entry(18, List.of("14000")), Map.entry(19, List.of("6002")), Map.entry(20, List.of("15000")),
            Map.entry(21, List.of("15001")), Map.entry(22, List.of("000", "000_1", "000_3", "508")),
            Map.entry(23, List.of("16000")), Map.entry(24, List.of("15002")), Map.entry(25, List.of("6003")),
            Map.entry(26, List.of("13500")), Map.entry(27, List.of("16001")), Map.entry(28, List.of("15003")),
            Map.entry(29, List.of("12100")), Map.entry(30, List.of("16002")), Map.entry(31, List.of("12200")),
            Map.entry(1000, List.of("17000")), Map.entry(1001, List.of("4001")), Map.entry(1002, List.of("4002")),
            Map.entry(1003, List.of("17001")));

    /** job node -> gender node ("male"/"female") -> every item ID listed under it. */
    private final Map<String, Map<String, Set<Integer>>> looks;
    private final Map<String, Byte> defaultSkins;

    private Tms282MakeCharInfo(Map<String, Map<String, Set<Integer>>> looks, Map<String, Byte> defaultSkins) {
        this.looks = looks;
        this.defaultSkins = defaultSkins;
    }

    public static Tms282MakeCharInfo load(Path file) throws Exception {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("Missing TMS 282 MakeCharInfo: " + file);
        }
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Element root = factory.newDocumentBuilder().parse(file.toFile()).getDocumentElement();
        Map<String, Map<String, Set<Integer>>> looks = new HashMap<>();
        Map<String, Byte> defaultSkins = new HashMap<>();
        for (Element job : children(root)) {
            Map<String, Set<Integer>> genders = new HashMap<>();
            for (Element gender : children(job)) {
                String name = gender.getAttribute("name");
                if (name.equals("male") || name.equals("female")) {
                    Set<Integer> ids = new HashSet<>();
                    collectIds(gender, ids);
                    genders.put(name, Set.copyOf(ids));
                }
            }
            if (!genders.isEmpty()) {
                looks.put(job.getAttribute("name"), Map.copyOf(genders));
            }
            for (Element info : children(job)) {
                if (!info.getAttribute("name").equals("info")) continue;
                for (Element field : children(info)) {
                    if (!field.getTagName().equals("int") || !field.getAttribute("name").equals("defaultSkin")) continue;
                    int headTemplate = Integer.parseInt(field.getAttribute("value"));
                    // Native MakeCharInfo uses head template 12000 + the CP008E skin byte (12051 -> 51).
                    if (headTemplate >= 12000 && headTemplate <= 12255) {
                        defaultSkins.put(job.getAttribute("name"), (byte) (headTemplate - 12000));
                    }
                }
            }
        }
        return new Tms282MakeCharInfo(Map.copyOf(looks), Map.copyOf(defaultSkins));
    }

    /** Exact native default for this creation race; it does not grant other races or intervening skin values. */
    public boolean isNativeDefaultSkin(int race, byte skin) {
        // Native creation initializer 3C93940 sets these before the optional MakeCharInfo override.
        // Khali's 30 also occurs in real default-look CP008E requests. Each body/head exists in the client.
        if (race == 28 && skin == 30 || race == 1000 && skin == 41 || race == 1003 && skin == 33) return true;
        List<String> nodes = RACE_NODES.get(race);
        return nodes != null && nodes.stream().anyMatch(node -> Objects.equals(defaultSkins.get(node), skin));
    }

    /** True only when the race is known and every item is listed for that race and gender. */
    public boolean isValidLook(int race, byte gender, int[] items) {
        List<String> nodes = RACE_NODES.get(race);
        if (nodes == null || items == null || items.length == 0 || gender < 0 || gender > 1) {
            return false;
        }
        String genderNode = gender == 0 ? "male" : "female";
        Set<Integer> allowed = new HashSet<>();
        for (String node : nodes) {
            Map<String, Set<Integer>> genders = looks.get(node);
            if (genders != null) {
                allowed.addAll(genders.getOrDefault(genderNode, Set.of()));
            }
        }
        return !allowed.isEmpty() && Arrays.stream(items).allMatch(allowed::contains);
    }

    private static void collectIds(Element element, Set<Integer> ids) {
        for (Element child : children(element)) {
            if (child.getTagName().equals("int")) {
                ids.add(Integer.parseInt(child.getAttribute("value")));
            } else if (child.getTagName().equals("imgdir")) {
                collectIds(child, ids);
            }
        }
    }

    private static List<Element> children(Element parent) {
        List<Element> result = new ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element) {
                result.add(element);
            }
        }
        return result;
    }
}
