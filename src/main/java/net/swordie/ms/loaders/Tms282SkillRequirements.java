package net.swordie.ms.loaders;

import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.loaders.overlay.DataOverlays;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The legacy DAT format never stored reqLev. Restore requirements from the selected 282 WZ layer,
 * without changing the DAT format or the separately cached 265 data. */
public final class Tms282SkillRequirements {
    private record Requirement(int level, Map<Integer,Integer> skills) { }
    private static final Map<Integer,Map<Integer,Requirement>> BOOKS = new ConcurrentHashMap<>();
    private static volatile int generation = -1;
    private Tms282SkillRequirements() { }

    static SkillInfo enrichOverlay(SkillInfo info) {
        if (info == null || !"tms282.3".equals(DataOverlays.current().id())) return info;
        Requirement req = requirement(info.getSkillId());
        if (req != null) {
            info.setReqLevel(req.level());
            info.getReqSkills().clear();
            info.getReqSkills().putAll(req.skills());
        }
        return info;
    }

    public static boolean hasOriginalSkill(int skillId) {
        return "tms282.3".equals(DataOverlays.current().id()) && requirement(skillId)!=null;
    }

    private static Requirement requirement(int skillId) {
        int current = DataOverlays.generation();
        if (generation != current) synchronized (BOOKS) {
            if (generation != current) { BOOKS.clear(); generation=current; }
        }
        int book=skillId/10000;
        if(book==8000 || book==8001) book=skillId/100;
        return BOOKS.computeIfAbsent(book,Tms282SkillRequirements::readBook).get(skillId);
    }

    private static Map<Integer,Requirement> readBook(int book) {
        Map<Integer,Requirement> result=new HashMap<>();
        String filename=String.format("Skill.wz/Skill/%03d.img.xml",book);
        for (var file:DataOverlays.current().wzLayers(filename)) {
            if (!file.isFile()) continue;
            try {
                DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
                factory.setExpandEntityReferences(false);
                Element skills=child(factory.newDocumentBuilder().parse(file).getDocumentElement(),"skill");
                if (skills==null) continue;
                for(Node node=skills.getFirstChild();node!=null;node=node.getNextSibling()) {
                    if (!(node instanceof Element skill)) continue;
                    int id=Integer.parseInt(skill.getAttribute("name"));
                    int level=value(child(skill,"reqLev"));
                    Map<Integer,Integer> parents=new HashMap<>();
                    Element requirements=child(skill,"req");
                    if(requirements!=null) for(Node n=requirements.getFirstChild();n!=null;n=n.getNextSibling()) {
                        if (!(n instanceof Element e)) continue;
                        String name=e.getAttribute("name");
                        if (name.equals("level")) level=Math.max(level,value(e));
                        else if (name.matches("[0-9]+")) parents.put(Integer.parseInt(name),value(e));
                    }
                    result.put(id,new Requirement(level,Map.copyOf(parents)));
                }
            } catch(Exception e) {
                throw new IllegalStateException("Cannot read 282 skill requirements from "+file,e);
            }
        }
        return Map.copyOf(result);
    }
    private static Element child(Element parent,String name) {
        for(Node node=parent.getFirstChild();node!=null;node=node.getNextSibling())
            if(node instanceof Element e && name.equals(e.getAttribute("name"))) return e;
        return null;
    }
    private static int value(Element e) { return e==null?0:Integer.parseInt(e.getAttribute("value")); }
}
