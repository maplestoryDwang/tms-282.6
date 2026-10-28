package net.swordie.ms.client.character.tms282;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.connection.protocol.Protocol;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.util.Position;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TMS 282.3 SecondAtom templates read straight from the 282 Skill.wz XML
 * ({@code data/wz282/Skill.wz/Skill/<book>.img.xml}, {@code skill/<id>/SecondAtom}).
 * <p>
 * Why: {@code SkillData.loadSkill} reads the {@code .dat} SecondAtom block without the {@code expire} int (after
 * rotate) and the {@code localOnly} int (after followAngle) that {@code writeSkillInfo} writes, so every job's
 * {@code SkillInfo.getSecondAtomInfos()} is misaligned on 282 and 265 alike (151111003: extraPos key -655360,
 * dataIndex -9830400; 151001001: a second atom with key 2686876). Adele's 追蹤 then threw
 * {@code NullPointerException: ... "pos" is null} at {@code pos.add(sai.getExtraPos().get(0))}. The loader fix is a
 * SkillData change (see research/tms2823/job-fixes-0929/README.md); until then 282 job code takes the atom
 * templates from here. Same shape as the SkillData XML loader: root fields are the defaults, each {@code atom/N}
 * copies and overrides them.
 */
public final class Tms282JobFixSecondAtoms {
    private static final Map<Integer, Int2ObjectMap<SkillInfo.SecondAtomInfo>> CACHE = new ConcurrentHashMap<>();

    private Tms282JobFixSecondAtoms() {
    }

    private static boolean tms282() {
        return Protocol.active().version() == ProtocolVersion.TMS_282_3;
    }

    /**
     * On 282.3: the 282 XML atom templates of the skill (by atom index), falling back to {@code si}'s when the XML
     * has none. Outside 282.3: {@code si.getSecondAtomInfos()} unchanged.
     */
    public static Int2ObjectMap<SkillInfo.SecondAtomInfo> infos(SkillInfo si) {
        if (si == null) {
            return Int2ObjectMaps.emptyMap();
        }
        if (!tms282()) {
            return si.getSecondAtomInfos();
        }
        Int2ObjectMap<SkillInfo.SecondAtomInfo> xml = fromWz282(si.getSkillId());
        return xml.isEmpty() ? si.getSecondAtomInfos() : xml;
    }

    /** The 282 XML templates of a skill (empty when the book or the SecondAtom node is missing). */
    public static Int2ObjectMap<SkillInfo.SecondAtomInfo> fromWz282(int skillId) {
        return CACHE.computeIfAbsent(skillId, id -> {
            try {
                return Int2ObjectMaps.unmodifiable(parse(bookFile(id), id));
            } catch (Exception e) {
                System.out.println("[TMS282] SecondAtom XML " + id + ": " + e);
                return Int2ObjectMaps.emptyMap();
            }
        });
    }

    static Path bookFile(int skillId) {
        return Path.of(ServerConstants.DIR, "data", "wz282", "Skill.wz", "Skill", (skillId / 10000) + ".img.xml");
    }

    static Int2ObjectMap<SkillInfo.SecondAtomInfo> parse(Path file, int skillId) throws Exception {
        Int2ObjectMap<SkillInfo.SecondAtomInfo> out = new Int2ObjectOpenHashMap<>();
        if (!Files.isRegularFile(file)) {
            return out;
        }
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setExpandEntityReferences(false);
        Document doc = f.newDocumentBuilder().parse(file.toFile());
        Element skillDir = child(doc.getDocumentElement(), "skill");
        Element skill = skillDir == null ? null : child(skillDir, Integer.toString(skillId));
        Element sa = skill == null ? null : child(skill, "SecondAtom");
        if (sa == null) {
            return out;
        }
        SkillInfo.SecondAtomInfo def = new SkillInfo.SecondAtomInfo();
        Element atoms = null;
        for (Element c : children(sa)) {
            String n = c.getAttribute("name");
            if ("atom".equals(n)) {
                atoms = c;
            } else {
                apply(def, c);
            }
        }
        if (atoms == null) {
            out.put(0, def);
            return out;
        }
        for (Element a : children(atoms)) {
            int idx;
            try {
                idx = Integer.parseInt(a.getAttribute("name"));
            } catch (NumberFormatException e) {
                continue;
            }
            SkillInfo.SecondAtomInfo info = copy(def);
            for (Element c : children(a)) {
                apply(info, c);
            }
            out.put(idx, info);
        }
        return out;
    }

    private static void apply(SkillInfo.SecondAtomInfo info, Element c) {
        String n = c.getAttribute("name");
        String v = c.getAttribute("value");
        switch (n) {
            case "extraPos" -> {
                for (Element e : children(c)) {
                    Position p = vector(e.getAttribute("value"));
                    if (p != null && isInt(e.getAttribute("name"))) {
                        info.getExtraPos().put(Integer.parseInt(e.getAttribute("name")), p);
                    }
                }
            }
            case "custom" -> {
                for (Element e : children(c)) {
                    if (isInt(e.getAttribute("name")) && isInt(e.getAttribute("value"))) {
                        info.getCustoms().put(Integer.parseInt(e.getAttribute("name")),
                                Integer.parseInt(e.getAttribute("value")));
                    }
                }
            }
            case "pos" -> {
                Position p = vector(v);
                if (p != null) {
                    info.setPos(p);
                }
            }
            default -> {
                if (!isInt(v)) {
                    return;
                }
                int i = Integer.parseInt(v);
                switch (n) {
                    case "dataIndex" -> info.setDataIndex(i);
                    case "expire" -> info.setExpire(i);
                    case "attackableCount" -> info.setAttackableCount(i);
                    case "createDelay" -> info.setCreateDelay(i);
                    case "enableDelay" -> info.setEnableDelay(i);
                    case "rotate" -> info.setRotate(i);
                    case "firstAngleStart" -> info.setFirstAngleStart(i);
                    case "firstAngleRange" -> info.setFirstAngleRange(i);
                    case "notRotateEndEffect" -> info.setNotRotateEndEffect(i);
                    case "posRandomOffset" -> info.setPosRandomOffset(i);
                    case "followAngle" -> info.setFollowAngle(i);
                    case "localOnly" -> info.setLocalOnly(i);
                    default -> { }
                }
            }
        }
    }

    private static SkillInfo.SecondAtomInfo copy(SkillInfo.SecondAtomInfo s) {
        SkillInfo.SecondAtomInfo d = new SkillInfo.SecondAtomInfo();
        d.setCreateDelay(s.getCreateDelay());
        d.setEnableDelay(s.getEnableDelay());
        d.setRotate(s.getRotate());
        d.setExpire(s.getExpire());
        d.setAttackableCount(s.getAttackableCount());
        d.setDataIndex(s.getDataIndex());
        d.setFirstAngleStart(s.getFirstAngleStart());
        d.setFirstAngleRange(s.getFirstAngleRange());
        d.setNotRotateEndEffect(s.getNotRotateEndEffect());
        d.setPosRandomOffset(s.getPosRandomOffset());
        d.setFollowAngle(s.getFollowAngle());
        d.setLocalOnly(s.getLocalOnly());
        if (s.getPos() != null) {
            d.setPos(new Position(s.getPos().getX(), s.getPos().getY()));
        }
        d.getCustoms().putAll(s.getCustoms());
        d.getExtraPos().putAll(s.getExtraPos());
        return d;
    }

    private static boolean isInt(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Position vector(String v) {
        if (v == null) {
            return null;
        }
        int comma = v.indexOf(',');
        if (comma < 0) {
            return null;
        }
        try {
            return new Position(Integer.parseInt(v.substring(0, comma).trim()),
                    Integer.parseInt(v.substring(comma + 1).trim()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Element child(Element e, String name) {
        for (Element c : children(e)) {
            if (name.equals(c.getAttribute("name"))) {
                return c;
            }
        }
        return null;
    }

    private static java.util.List<Element> children(Element e) {
        java.util.List<Element> out = new java.util.ArrayList<>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element el) {
                out.add(el);
            }
        }
        return out;
    }
}
