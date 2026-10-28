package net.swordie.ms.connection.hotfix;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses {@code data/hotfix/Data.xml} (schema: TMS 279 server/hotfix/Data.xsd + WzNode.xsd) into flat
 * {@link HotfixEntry hotfix entries}:
 * <pre>
 * &lt;Hotfix&gt;
 *   &lt;Include src="more.xml"/&gt;                          other file, same schema, relative to this one
 *   &lt;ItemName id="2637436" name="..." desc="..."/&gt;     -&gt; String\...\&lt;id&gt;\name / desc
 *   &lt;SkillData id="80000545"&gt;&lt;stat name="maxLevel" value="200"/&gt;...&lt;/SkillData&gt;
 *                                                     -&gt; Skill\&lt;img&gt;.img\skill\&lt;id&gt;\common\&lt;stat&gt;
 *   &lt;Custom&gt; WzNode tree: property / null / bool / short / int / long / uint / float / double / string /
 *            vector / uol (names may be full paths, children are joined with '\') &lt;/Custom&gt;
 * &lt;/Hotfix&gt;
 * </pre>
 * Not supported (skipped with a warning): canvas (PNG pixel data), sound, convex. Later entries for the same path
 * replace earlier ones.
 */
public final class HotfixDocument {
    private static final int MAX_INCLUDE_DEPTH = 16;

    private final Map<String, HotfixEntry> entries = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private final Set<Path> visiting = new HashSet<>();

    private HotfixDocument() {
    }

    public static HotfixDocument parse(Path file) {
        HotfixDocument d = new HotfixDocument();
        d.parseFile(file.toAbsolutePath().normalize(), 0);
        return d;
    }

    public List<HotfixEntry> entries() {
        return List.copyOf(entries.values());
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    private void warn(String msg) {
        warnings.add(msg);
    }

    private void put(HotfixEntry e) {
        entries.remove(e.path());
        entries.put(e.path(), e);
    }

    private void parseFile(Path file, int depth) {
        if (depth > MAX_INCLUDE_DEPTH) {
            warn("include depth > " + MAX_INCLUDE_DEPTH + " at " + file);
            return;
        }
        if (!visiting.add(file)) {
            warn("include cycle at " + file);
            return;
        }
        try {
            if (!Files.isRegularFile(file)) {
                warn("missing file " + file);
                return;
            }
            Document doc = builder().parse(file.toFile());
            Element root = doc.getDocumentElement();
            if (!"Hotfix".equals(root.getTagName())) {
                warn(file + ": root is <" + root.getTagName() + ">, expected <Hotfix>");
                return;
            }
            for (Element el : children(root)) {
                switch (el.getTagName()) {
                    case "Include" -> {
                        String src = el.getAttribute("src");
                        if (src.isBlank()) {
                            warn(file + ": <Include> without src");
                        } else {
                            Path parent = file.getParent();
                            parseFile((parent == null ? Path.of(src) : parent.resolve(src)).normalize(), depth + 1);
                        }
                    }
                    case "ItemName" -> itemName(file, el);
                    case "SkillData" -> skillData(file, el);
                    case "Custom" -> {
                        for (Element n : children(el)) {
                            node(file, "", n);
                        }
                    }
                    default -> warn(file + ": unknown element <" + el.getTagName() + ">");
                }
            }
        } catch (Exception e) {
            warn(file + ": " + e);
        } finally {
            visiting.remove(file);
        }
    }

    private void itemName(Path file, Element el) {
        int id = parseInt(el.getAttribute("id"), -1);
        if (id <= 0) {
            warn(file + ": <ItemName> bad id '" + el.getAttribute("id") + "'");
            return;
        }
        String base = HotfixPaths.itemStringPath(id);
        if (base == null) {
            warn(file + ": <ItemName id=" + id + "> has no String image (unknown equip category?)");
            return;
        }
        boolean any = false;
        if (el.hasAttribute("name")) {
            put(HotfixEntry.string(base + "\\name", el.getAttribute("name")));
            any = true;
        }
        if (el.hasAttribute("desc")) {
            put(HotfixEntry.string(base + "\\desc", el.getAttribute("desc")));
            any = true;
        }
        if (!any) {
            warn(file + ": <ItemName id=" + id + "> has neither name nor desc");
        }
    }

    private void skillData(Path file, Element el) {
        int id = parseInt(el.getAttribute("id"), -1);
        if (id < 0) {
            warn(file + ": <SkillData> bad id '" + el.getAttribute("id") + "'");
            return;
        }
        HotfixPaths.SkillInfo info = HotfixPaths.skill(id);
        for (Element stat : children(el)) {
            if (!"stat".equals(stat.getTagName())) {
                warn(file + ": <SkillData id=" + id + "> unexpected <" + stat.getTagName() + ">");
                continue;
            }
            String name = stat.getAttribute("name");
            String value = stat.getAttribute("value");
            if (name.isBlank()) {
                warn(file + ": <SkillData id=" + id + "> stat without name");
                continue;
            }
            String path = HotfixPaths.skillStatPath(info, id, name);
            String wzType = info.commonTypes().get(name);
            Integer asInt = parseIntOrNull(value);
            if ("int".equals(wzType) && asInt != null) {
                put(new HotfixEntry(path, new HotfixEntry.Int(asInt)));
            } else {
                put(HotfixEntry.string(path, value));
            }
        }
    }

    private void node(Path file, String prefix, Element el) {
        String name = el.getAttribute("name").replace('/', '\\');
        if (name.isEmpty()) {
            warn(file + ": <" + el.getTagName() + "> without name under '" + prefix + "'");
            return;
        }
        String path = prefix.isEmpty() ? name : prefix + "\\" + name;
        String v = el.getAttribute("value");
        try {
            switch (el.getTagName()) {
                case "property" -> {
                    for (Element c : children(el)) {
                        node(file, path, c);
                    }
                }
                case "null" -> put(new HotfixEntry(path, new HotfixEntry.Null()));
                case "bool" -> put(new HotfixEntry(path, new HotfixEntry.Bool(
                        "true".equalsIgnoreCase(v.strip()) || "1".equals(v.strip()))));
                case "short" -> put(new HotfixEntry(path, new HotfixEntry.Short(Short.parseShort(v.strip()))));
                case "int" -> put(new HotfixEntry(path, new HotfixEntry.Int(Integer.parseInt(v.strip()))));
                case "uint" -> put(new HotfixEntry(path, new HotfixEntry.Int((int) Long.parseLong(v.strip()))));
                case "long" -> put(new HotfixEntry(path, new HotfixEntry.Long(Long.parseLong(v.strip()))));
                case "float" -> put(new HotfixEntry(path, new HotfixEntry.Float(Float.parseFloat(v.strip()))));
                case "double" -> put(new HotfixEntry(path, new HotfixEntry.Double(Double.parseDouble(v.strip()))));
                case "string" -> put(HotfixEntry.string(path, v));
                case "vector" -> put(new HotfixEntry(path, new HotfixEntry.Vector(
                        (int) Math.round(Double.parseDouble(el.getAttribute("x").strip())),
                        (int) Math.round(Double.parseDouble(el.getAttribute("y").strip())))));
                case "uol" -> {
                    String link = el.hasAttribute("link") ? el.getAttribute("link") : v;
                    put(new HotfixEntry(path, new HotfixEntry.Uol(link)));
                }
                case "canvas", "sound", "convex" ->
                        warn(file + ": <" + el.getTagName() + " name=" + path + "> not supported, skipped");
                default -> warn(file + ": unknown node <" + el.getTagName() + " name=" + path + ">");
            }
        } catch (NumberFormatException e) {
            warn(file + ": <" + el.getTagName() + " name=" + path + "> bad value: " + e.getMessage());
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    private static DocumentBuilder builder() throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setExpandEntityReferences(false);
        f.setNamespaceAware(false);
        f.setIgnoringComments(true);
        return f.newDocumentBuilder();
    }

    private static List<Element> children(Element parent) {
        List<Element> out = new ArrayList<>();
        NodeList nl = parent.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                out.add((Element) n);
            }
        }
        return out;
    }

    private static int parseInt(String s, int fallback) {
        Integer v = parseIntOrNull(s);
        return v == null ? fallback : v;
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
