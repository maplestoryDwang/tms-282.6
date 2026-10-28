package net.swordie.ms.connection.hotfix;

import net.swordie.ms.ServerConstants;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps the shorthand elements of Data.xml to full WZ paths, using the server's WZ XML dumps (data/wz282 first,
 * then {@code WZ_DIR}, then data/wz265):
 * <ul>
 *     <li>ItemName id: {@code String\Eqp.img\Eqp\<Category>\<id>} (category looked up in String.wz/Eqp.img.xml),
 *     {@code String\Consume.img\<id>} (2xx), {@code String\Ins.img\<id>} (3xx), {@code String\Etc.img\Etc\<id>}
 *     (4xx), {@code String\Pet.img\<id>} (500xxxx), {@code String\Cash.img\<id>} (5xx);</li>
 *     <li>SkillData id: {@code Skill\<img>.img\skill\<id>\common\<stat>} where the image is found among
 *     Skill.wz/Skill ({@code id/10000} as %03d, or {@code id/100} for the 800xxx images), and the stat keeps the WZ
 *     type it has there (int / string), string when unknown.</li>
 * </ul>
 * Shapes match the 265 Data2.wz sample ({@code String\Etc.img\Etc\4310085\name},
 * {@code String\Eqp.img\Eqp\Weapon\1402259\name}, {@code String\Consume.img\2430070\desc}).
 */
final class HotfixPaths {
    private static volatile Map<Integer, String> eqpCategories;
    private static final Map<Integer, SkillInfo> skills = new ConcurrentHashMap<>();

    /** Image name and the WZ element type of each {@code common} child ("int", "string", ...). */
    record SkillInfo(String image, Map<String, String> commonTypes) {
    }

    private HotfixPaths() {
    }

    /** WZ string path prefix of an item (no trailing backslash), or null when the id has no String image. */
    static String itemStringPath(int itemId) {
        int prefix = itemId / 1000000;
        return switch (prefix) {
            case 1 -> {
                String cat = eqpCategories().get(itemId);
                yield cat == null ? null : "String\\Eqp.img\\Eqp\\" + cat + "\\" + itemId;
            }
            case 2 -> "String\\Consume.img\\" + itemId;
            case 3 -> "String\\Ins.img\\" + itemId;
            case 4 -> "String\\Etc.img\\Etc\\" + itemId;
            case 5 -> itemId / 10000 == 500 ? "String\\Pet.img\\" + itemId : "String\\Cash.img\\" + itemId;
            default -> null;
        };
    }

    static SkillInfo skill(int skillId) {
        return skills.computeIfAbsent(skillId, HotfixPaths::findSkill);
    }

    static String skillStatPath(SkillInfo info, int skillId, String stat) {
        return "Skill\\" + info.image() + "\\skill\\" + skillId + "\\common\\" + stat;
    }

    static void clearCaches() {
        eqpCategories = null;
        skills.clear();
    }

    // ---- lookups ---------------------------------------------------------------------------------------------

    private static Map<Integer, String> eqpCategories() {
        Map<Integer, String> m = eqpCategories;
        if (m == null) {
            synchronized (HotfixPaths.class) {
                if (eqpCategories == null) {
                    eqpCategories = loadEqpCategories();
                }
                m = eqpCategories;
            }
        }
        return m;
    }

    /** String.wz/Eqp.img.xml: Eqp.img(1) / Eqp(2) / Category(3) / id(4). */
    private static Map<Integer, String> loadEqpCategories() {
        Map<Integer, String> out = new HashMap<>();
        for (Path base : dataRoots()) {
            readEqp(base.resolve("String.wz").resolve("Eqp.img.xml"), out);
        }
        return out;
    }

    private static void readEqp(Path file, Map<Integer, String> out) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            XMLStreamReader r = factory().createXMLStreamReader(in);
            int depth = 0;
            String cat = null;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    if (depth == 3) {
                        cat = r.getAttributeValue(null, "name");
                    } else if (depth == 4 && cat != null) {
                        int id = parse(r.getAttributeValue(null, "name"));
                        if (id > 0) {
                            out.putIfAbsent(id, cat);
                        }
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    depth--;
                }
            }
            r.close();
        } catch (Exception e) {
            HotfixService.log("read " + file + ": " + e);
        }
    }

    private static SkillInfo findSkill(int skillId) {
        int job = skillId / 10000;
        String[] candidates = {
                (job < 1000 ? String.format("%03d", job) : String.valueOf(job)) + ".img",
                (skillId / 100) + ".img",
                String.valueOf(job) + ".img"
        };
        for (Path base : dataRoots()) {
            for (String img : candidates) {
                for (Path file : skillFiles(base, img)) {
                    Map<String, String> types = readCommonTypes(file, skillId);
                    if (types != null) {
                        return new SkillInfo(img, types);
                    }
                }
            }
        }
        // Not in any dump: fall back to the standard image name; every stat is sent as a string.
        return new SkillInfo(candidates[0], Map.of());
    }

    /** 282 dump first (the client's data), then WZ_DIR, then the 265 dump. */
    private static List<Path> dataRoots() {
        Set<Path> roots = new LinkedHashSet<>();
        roots.add(Path.of(ServerConstants.DIR, "data", "wz282").toAbsolutePath().normalize());
        roots.add(Path.of(ServerConstants.WZ_DIR).toAbsolutePath().normalize());
        roots.add(Path.of(ServerConstants.DIR, "data", "wz265").toAbsolutePath().normalize());
        return new ArrayList<>(roots);
    }

    /** 282 layout Skill.wz/Skill/X.img.xml, flat Skill.wz/X.img.xml, 265 layout Skill.wz/Skill_NNNNN/X.xml. */
    private static List<Path> skillFiles(Path base, String img) {
        List<Path> out = new ArrayList<>();
        Path wz = base.resolve("Skill.wz");
        for (Path p : List.of(wz.resolve("Skill").resolve(img + ".xml"), wz.resolve(img + ".xml"))) {
            if (Files.isRegularFile(p)) {
                out.add(p);
            }
        }
        String bare = img.endsWith(".img") ? img.substring(0, img.length() - 4) : img;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(wz, "Skill_*")) {
            for (Path d : ds) {
                Path p = d.resolve(bare + ".xml");
                if (Files.isRegularFile(p)) {
                    out.add(p);
                }
            }
        } catch (IOException ignored) {
            // no such directory
        }
        return out;
    }

    /** Types of {@code <img>/skill/<id>/common/*}, or null when the skill is not in the file. */
    private static Map<String, String> readCommonTypes(Path file, int skillId) {
        String want = String.valueOf(skillId);
        try (InputStream in = Files.newInputStream(file)) {
            XMLStreamReader r = factory().createXMLStreamReader(in);
            int depth = 0;
            boolean inSkillDir = false;
            boolean inSkill = false;
            boolean inCommon = false;
            boolean found = false;
            Map<String, String> types = new HashMap<>();
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    depth++;
                    String name = r.getAttributeValue(null, "name");
                    if (depth == 2) {
                        inSkillDir = "skill".equals(name);
                    } else if (depth == 3 && inSkillDir) {
                        inSkill = want.equals(name);
                        found |= inSkill;
                    } else if (depth == 4 && inSkill) {
                        inCommon = "common".equals(name);
                    } else if (depth == 5 && inCommon && name != null) {
                        types.put(name, r.getLocalName());
                    }
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    if (depth == 3 && inSkill) {
                        r.close();
                        return types;
                    }
                    if (depth == 4) {
                        inCommon = false;
                    }
                    depth--;
                }
            }
            r.close();
            return found ? types : null;
        } catch (Exception e) {
            HotfixService.log("read " + file + ": " + e);
            return null;
        }
    }

    private static XMLInputFactory factory() {
        XMLInputFactory f = XMLInputFactory.newInstance();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return f;
    }

    private static int parse(String s) {
        try {
            return s == null ? -1 : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
