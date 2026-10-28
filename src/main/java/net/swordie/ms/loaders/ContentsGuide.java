package net.swordie.ms.loaders;

import net.swordie.ms.ServerConstants;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class ContentsGuide {
    // ====== data ======
    public static final Map<Integer, Integer> fieldReq       = new HashMap<>(); // fieldID -> beginLv
    public static final Map<String,  Integer> nameToField    = new HashMap<>(); // groupName -> fieldID
    public static final Map<Integer, Integer> fieldType      = new HashMap<>(); // fieldID -> type (0|1)

    // ====== API ======
    public static int getReqLevelByFieldID(int fieldID) {
        return fieldReq.getOrDefault(fieldID, 0);
    }

    public static FieldInfo getFieldInfoByGroupName(String name) {
        int fieldID = nameToField.getOrDefault(name, 0);
        int type    = fieldType.getOrDefault(fieldID, 0);
        return new FieldInfo(fieldID, type);
    }

    public static final class FieldInfo {
        public final int fieldID;
        public final int type;
        public FieldInfo(int fieldID, int type) { this.fieldID = fieldID; this.type = type; }
        @Override public String toString() { return "FieldInfo{fieldID=" + fieldID + ", type=" + type + "}"; }
    }

    // ====== loader ======
    public static void load() {
        fieldReq.clear(); nameToField.clear(); fieldType.clear();

        String path = ServerConstants.WZ_DIR + "/Etc.wz/ContentsGuide.img.xml";
        Document doc;
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setIgnoringComments(true);
            doc = f.newDocumentBuilder().parse(new File(path));
        } catch (Exception e) {
            throw new RuntimeException("Parse failed: " + path, e);
        }
        doc.getDocumentElement().normalize();

        // tìm imgdir name="field"
        Element root = doc.getDocumentElement(); // <imgdir name="ContentsGuide.img">
        Element fieldRoot = getFirstImgdirByName(root, "field");
        if (fieldRoot == null) return;

        // duyệt tất cả <imgdir name="..."> dưới field -> mỗi cái là một group "name"
        List<Element> groups = getChildImgdirs(fieldRoot);
        for (Element g : groups) {
            String groupName = g.getAttribute("name"); // ví dụ "8","9",...
            Integer beginLv  = getIntChild(g, "beginLv");
            Integer fieldId  = getIntChild(g, "field");
            if (fieldId == null) continue;

            // fieldReq
            if (beginLv != null) fieldReq.put(fieldId, beginLv);

            // nameToField
            if (groupName != null && !groupName.isEmpty()) {
                nameToField.put(groupName, fieldId);
            }

            // fieldType: 1 nếu có moveCondition/mobID, ngược lại 0
            int type = hasMoveConditionMobId(g) ? 1 : 0;
            fieldType.put(fieldId, type);
        }
    }

    // ====== helpers ======
    private static boolean hasMoveConditionMobId(Element group) {
        Element moveCond = getFirstImgdirByName(group, "moveCondition");
        if (moveCond == null) return false;
        Integer mobId = getIntChild(moveCond, "mobID");
        return mobId != null;
    }

    private static Element getFirstImgdirByName(Element parent, String name) {
        NodeList nl = parent.getElementsByTagName("imgdir");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            if (e.getParentNode() == parent && name.equals(getAttr(e, "name"))) {
                return e;
            }
        }
        return null;
    }

    private static List<Element> getChildImgdirs(Element parent) {
        List<Element> out = new ArrayList<>();
        NodeList nl = parent.getElementsByTagName("imgdir");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            if (e.getParentNode() == parent) out.add(e);
        }
        return out;
    }

    private static Integer getIntChild(Element parent, String name) {
        NodeList nl = parent.getElementsByTagName("int");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            if (e.getParentNode() == parent && name.equals(getAttr(e, "name"))) {
                String v = getAttr(e, "value");
                try { return Integer.parseInt(v); } catch (Exception ignore) { return null; }
            }
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private static String getAttr(Element e, String key) {
        return e.hasAttribute(key) ? e.getAttribute(key) : null;
    }
}