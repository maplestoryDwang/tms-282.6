package net.swordie.ms.loaders;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.loaders.containerclasses.SkillStringInfo;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.XMLApi;

import net.swordie.ms.util.container.Tuple;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.*;
import java.util.*;

import static net.swordie.ms.ServerConstants.version;

public class StringData {

    public static final Int2ObjectMap<SkillStringInfo> skillString  = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<String>          itemStrings  = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<String>          itemDescStrings = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<String>          mapStrings   = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<Tuple<String, String>> mapStrings2 = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<String>          mobStrings   = new Int2ObjectOpenHashMap<>();
    public static final Int2ObjectMap<String>          npcStrings   = new Int2ObjectOpenHashMap<>();

    public static void loadItemStringsFromWz() {
        System.out.println("[WZ Data] Started loading item strings from wz.");
        long start = System.currentTimeMillis();
        String wzDir = ServerConstants.WZ_DIR + "/String.wz/";
        String[] files = new String[]{"Cash", "Consume", "Eqp", "Ins", "Pet", "Etc"};
        for (String fileDir : files) {
            // 265 profile: only wz265; 282 profile: wz265 then data/wz282 (later layer wins per id).
            for (File file : net.swordie.ms.loaders.overlay.DataOverlays.current().wzLayers("String.wz/" + fileDir + ".img.xml")) {
            Document doc = XMLApi.getRoot(file);
            Node node = doc;
            List<Node> nodes = XMLApi.getAllChildren(node);
            for (Node topNode : nodes) {
                if (!fileDir.equalsIgnoreCase("eqp") &&
                        !fileDir.equalsIgnoreCase("etc")) {
                    for (Node mainNode : XMLApi.getAllChildren(topNode)) {
                        int id = Integer.parseInt(XMLApi.getNamedAttribute(mainNode, "name"));
                        String desc = "";
                        if (XMLApi.getFirstChildByNameBF(mainNode, "name") != null) {
                            String name = XMLApi.getNamedAttribute(XMLApi.getFirstChildByNameBF(mainNode, "name"), "value");
                            if (XMLApi.getFirstChildByNameBF(mainNode, "desc") != null) {
                                desc = XMLApi.getNamedAttribute(XMLApi.getFirstChildByNameBF(mainNode, "desc"), "value");
                                if (Util.isEnglishContext(desc))
                                    itemDescStrings.put(id, desc);
                            }
                            itemStrings.put(id, name);
                        }
                    }
                } else if (fileDir.equalsIgnoreCase("etc")) {
                    for (Node category : XMLApi.getAllChildren(topNode)) {
                        for (Node mainNode : XMLApi.getAllChildren(category)) {
                            int id = Integer.parseInt(XMLApi.getNamedAttribute(mainNode, "name"));
                            String desc = "";
                            if (XMLApi.getFirstChildByNameBF(mainNode, "name") != null) {
                                String name = XMLApi.getNamedAttribute(XMLApi.getFirstChildByNameBF(mainNode, "name"), "value");
                                if (XMLApi.getFirstChildByNameBF(mainNode, "desc") != null) {
                                    desc = XMLApi.getNamedAttribute(XMLApi.getFirstChildByNameBF(mainNode, "desc"), "value");
                                    if (Util.isEnglishContext(desc))
                                        itemDescStrings.put(id, desc);
                                }
                                itemStrings.put(id, name);
                            }
                        }
                    }
                } else {
                    for (Node n : XMLApi.getAllChildren(topNode)) {
                        for (Node category : XMLApi.getAllChildren(n)) {
                            for (Node mainNode : XMLApi.getAllChildren(category)) {
                                int id = Integer.parseInt(XMLApi.getNamedAttribute(mainNode, "name"));
                                String desc = "";
                                if (XMLApi.getFirstChildByNameBF(mainNode, "name") != null) {
                                    String name = XMLApi.getNamedAttribute(XMLApi.getFirstChildByNameBF(mainNode, "name"), "value");
                                    if (XMLApi.getFirstChildByNameBF(mainNode, "desc") != null) {
                                        desc = XMLApi.getNamedAttribute(XMLApi.getFirstChildByNameBF(mainNode, "desc"), "value");
                                        if (Util.isEnglishContext(desc))
                                            itemDescStrings.put(id, desc);
                                    }
                                    itemStrings.put(id, name);
                                }
                            }
                        }
                    }
                }
            }
            }
        }
        System.out.printf("[WZ Data] Loaded Item String in %dms%n", System.currentTimeMillis() - start);
    }

    public static void loadSkillStringsFromWz() {
        System.out.println("[WZ Data] Started loading skill strings from wz.");
        long start = System.currentTimeMillis();
        String wzDir = ServerConstants.WZ_DIR + "/String.wz/Skill.img.xml";
        File file = new File(wzDir);
        Document doc = XMLApi.getRoot(file);
        Node node = doc;
        List<Node> nodes = XMLApi.getAllChildren(node);
        for (Node topNode : nodes) {
            for (Node mainNode : XMLApi.getAllChildren(topNode)) {
                Node bookNameNode = XMLApi.getFirstChildByNameBF(mainNode, "bookName");
                if (bookNameNode != null) {
                    continue;
                }
                SkillStringInfo ssi = new SkillStringInfo();
                Node nameNode = XMLApi.getFirstChildByNameBF(mainNode, "name");
                if (nameNode != null) {
                    ssi.setName(XMLApi.getNamedAttribute(nameNode, "value"));
                }
                Node descNode = XMLApi.getFirstChildByNameBF(mainNode, "desc");
                if (descNode != null) {
                    ssi.setDesc(XMLApi.getNamedAttribute(descNode, "value"));
                }
                Node hNode = XMLApi.getFirstChildByNameBF(mainNode, "h");
                if (hNode != null) {
                    ssi.setH(XMLApi.getNamedAttribute(hNode, "value"));
                } else {
                    Node h1Node = XMLApi.getFirstChildByNameBF(mainNode, "h1");
                    if (h1Node != null) {
                        ssi.setH(XMLApi.getNamedAttribute(h1Node, "value"));
                    }
                }
                skillString.put(Integer.parseInt(XMLApi.getNamedAttribute(mainNode, "name")), ssi);
            }
        }
        System.out.printf("[WZ Data] Loaded Skill String in %dms%n", System.currentTimeMillis() - start);
    }

    public static void loadMobStringsFromWz() {
        System.out.println("[WZ Data] Started loading mob strings from wz.");
        long start = System.currentTimeMillis();
        // 265 profile: only wz265; 282 profile: wz265 then data/wz282 (later layer wins per id).
        for (File file : net.swordie.ms.loaders.overlay.DataOverlays.current().wzLayers("String.wz/Mob.img.xml")) {
        Document doc = XMLApi.getRoot(file);
        List<Node> nodes = XMLApi.getAllChildren(doc);
        for (Node topNode : nodes) {
            for (Node mainNode : XMLApi.getAllChildren(topNode)) {
                int id = Integer.parseInt(XMLApi.getNamedAttribute(mainNode, "name").replaceAll("\\s", ""));
                for (Node infoNode : XMLApi.getAllChildren(mainNode)) {
                    String name = XMLApi.getNamedAttribute(infoNode, "name");
                    String value = XMLApi.getNamedAttribute(infoNode, "value");
                    switch (name) {
                        case "name":
                            mobStrings.put(id, value);
                            break;
                    }
                }
            }
        }
        }
        System.out.printf("[WZ Data] Loaded Mob String in %dms%n", System.currentTimeMillis() - start);
    }

    public static void loadNpcStringsFromWz() {
        System.out.println("[WZ Data] Started loading npc strings from wz.");
        long start = System.currentTimeMillis();
        // 265 profile: only wz265; 282 profile: wz265 then data/wz282 (later layer wins per id).
        for (File file : net.swordie.ms.loaders.overlay.DataOverlays.current().wzLayers("String.wz/Npc.img.xml")) {
        Document doc = XMLApi.getRoot(file);
        List<Node> nodes = XMLApi.getAllChildren(doc);
        for (Node topNode : nodes) {
            for (Node mainNode : XMLApi.getAllChildren(topNode)) {
                int id = Integer.parseInt(XMLApi.getNamedAttribute(mainNode, "name"));
                for (Node infoNode : XMLApi.getAllChildren(mainNode)) {
                    String name = XMLApi.getNamedAttribute(infoNode, "name");
                    String value = XMLApi.getNamedAttribute(infoNode, "value");
                    switch (name) {
                        case "name":
                            npcStrings.put(id, value);
                            break;
                    }
                }
            }
        }
        }
        System.out.printf("[WZ Data] Loaded NPC String in %dms%n", System.currentTimeMillis() - start);
    }

    public static void loadMapStringsFromWz() {
        System.out.println("[WZ Data] Started loading map strings from wz.");
        long start = System.currentTimeMillis();
        // 265 profile: only wz265; 282 profile: wz265 then data/wz282 (later layer wins per id).
        for (File file : net.swordie.ms.loaders.overlay.DataOverlays.current().wzLayers("String.wz/Map.img.xml")) {
        Document doc = XMLApi.getRoot(file);
        List<Node> nodes = XMLApi.getAllChildren(doc);
        for (Node topNode : nodes) {
            for (Node areaNode : XMLApi.getAllChildren(topNode)) {
                for (Node mainNode : XMLApi.getAllChildren(areaNode)) {
                    int id = Integer.parseInt(XMLApi.getNamedAttribute(mainNode, "name"));
                    String mapName = "UNK";
                    String streetName = "UNK";
                    for (Node infoNode : XMLApi.getAllChildren(mainNode)) {
                        String name = XMLApi.getNamedAttribute(infoNode, "name");
                        String value = XMLApi.getNamedAttribute(infoNode, "value");
                        switch (name) {
                            case "mapName":
                                mapName = value;
                                break;
                            case "streetName":
                                streetName = value;
                                break;
                        }
                    }
                    mapStrings2.put(id, new Tuple<>(streetName, mapName));
                    mapStrings.put(id, String.format("%s : %s", streetName, mapName));
                }
            }
        }
        }
        System.out.printf("[WZ Data] Loaded map strings in %dms%n", System.currentTimeMillis() - start);
    }

    public static Map<Integer, SkillStringInfo> getSkillString() {
        return skillString;
    }

    public static void generateDatFiles() {
        System.out.println("Started generating string data.");
        long start = System.currentTimeMillis();
        loadSkillStringsFromWz();
        loadItemStringsFromWz();
        loadMobStringsFromWz();
        loadNpcStringsFromWz();
        loadMapStringsFromWz();
        saveSkillStrings(ServerConstants.DAT_DIR + "/strings");
        saveItemStrings(ServerConstants.DAT_DIR + "/strings");
        saveMobStrings(ServerConstants.DAT_DIR + "/strings");
        saveNpcStrings(ServerConstants.DAT_DIR + "/strings");
        saveMapStrings(ServerConstants.DAT_DIR + "/strings");
        System.out.printf("Completed generating string data in %dms%n", System.currentTimeMillis() - start);
    }

    private static void saveSkillStrings(String dir) {
        Util.makeDirIfAbsent(dir);
//        String fileDir = dir + "/skills";
//        Util.makeDirIfAbsent(fileDir);
        File file = new File(dir + "/skills.dat");
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            dataOutputStream.writeInt(getSkillString().size());
            for (Map.Entry<Integer, SkillStringInfo> entry : getSkillString().entrySet()) {
                int id = entry.getKey();
                SkillStringInfo ssi = entry.getValue();
                dataOutputStream.writeInt(id);
                dataOutputStream.writeUTF(ssi.getName() == null ? "" : ssi.getName());
                dataOutputStream.writeUTF(ssi.getDesc());
                dataOutputStream.writeUTF(ssi.getH());
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static void loadSkillStrings() {
        long start = System.currentTimeMillis();
        File file = new File(ServerConstants.DAT_DIR + "/strings/skills.dat");
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            int size = dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                int id = dataInputStream.readInt();
                SkillStringInfo ssi = new SkillStringInfo();
                ssi.setName(dataInputStream.readUTF());
                ssi.setDesc(dataInputStream.readUTF());
                ssi.setH(dataInputStream.readUTF());
                getSkillString().put(id, ssi);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        System.out.printf("[WZ Data] Loaded Skill String Data from \"skills.dat\" in %dms%n", System.currentTimeMillis() - start);
    }

    private static void saveItemStrings(String dir) {
        Util.makeDirIfAbsent(dir);
        File file = new File(dir + "/items.dat");
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            dataOutputStream.writeInt(itemStrings.size());
            for (var entry : itemStrings.int2ObjectEntrySet()) {
                int id = entry.getIntKey();
                String ssi = entry.getValue();
                dataOutputStream.writeInt(id);
                dataOutputStream.writeUTF(ssi);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static void loadItemStrings() {
        long start = System.currentTimeMillis();
        File file = new File(ServerConstants.DAT_DIR + "/strings/items.dat");
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            int size = dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                int id = dataInputStream.readInt();
                String name = dataInputStream.readUTF();
                itemStrings.put(id, name);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        System.out.printf("[WZ Data] Loaded Item String from \"items.dat\" in %dms%n", System.currentTimeMillis() - start);
    }

    private static void saveMobStrings(String dir) {
        Util.makeDirIfAbsent(dir);
        File file = new File(dir + "/mobs.dat");
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            dataOutputStream.writeInt(mobStrings.size());
            for (var entry : mobStrings.int2ObjectEntrySet()) {
                int id = entry.getIntKey();
                String name = entry.getValue();
                dataOutputStream.writeInt(id);
                dataOutputStream.writeUTF(name);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static void loadMobStrings() {
        long start = System.currentTimeMillis();
        File file = new File(ServerConstants.DAT_DIR + "/strings/mobs.dat");
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            int size = dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                int id = dataInputStream.readInt();
                String name = dataInputStream.readUTF();
                mobStrings.put(id, name);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        System.out.printf("[WZ Data] Loaded Mob String from \"mobs.dat\" in %dms%n", System.currentTimeMillis() - start);
    }

    private static void saveNpcStrings(String dir) {
        Util.makeDirIfAbsent(dir);
        File file = new File(dir + "/npcs.dat");
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            dataOutputStream.writeInt(npcStrings.size());
            for (var entry : npcStrings.int2ObjectEntrySet()) {
                int id = entry.getIntKey();
                String name = entry.getValue();
                dataOutputStream.writeInt(id);
                dataOutputStream.writeUTF(name);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static void loadNpcStrings() {
        long start = System.currentTimeMillis();
        File file = new File(ServerConstants.DAT_DIR + "/strings/npcs.dat");
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            int size = dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                int id = dataInputStream.readInt();
                String name = dataInputStream.readUTF();
                npcStrings.put(id, name);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        System.out.printf("[WZ Data] Loaded NPC String from \"npcs.dat\" in %dms%n", System.currentTimeMillis() - start);
    }

    private static void saveMapStrings(String dir) {
        Util.makeDirIfAbsent(dir);
        File file = new File(dir + "/maps.dat");
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file))) {
            dataOutputStream.writeInt(mapStrings.size());
            for (var entry : mapStrings.int2ObjectEntrySet()) {
                int id = entry.getIntKey();
                String name = entry.getValue();
                dataOutputStream.writeInt(id);
                dataOutputStream.writeUTF(name);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static void loadMapStrings() {
        long start = System.currentTimeMillis();
        File file = new File(ServerConstants.DAT_DIR + "/strings/maps.dat");
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            int size = dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                int id = dataInputStream.readInt();
                String name = dataInputStream.readUTF();
                mapStrings.put(id, name);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        System.out.printf("[WZ Data] Loaded Map String from maps.dat in %dms%n", System.currentTimeMillis() - start);
    }

    public static void main(String[] args) {
        //generateDatFiles();
        generateTextFiles();
    }


    public static SkillStringInfo getSkillStringById(int id) {
        return getSkillString().getOrDefault(id, null);
    }

    public static String getItemStringById(int itemID) {
        return itemStrings.get(itemID);
    }

    public static String getMobStringById(int mobTemplateID) {
        return mobStrings.get(mobTemplateID);
    }

    public static String getNpcStringById(int npcTemplateID) {
        return npcStrings.get(npcTemplateID);
    }

    public static String getMapStringById(int mapID) {
        return mapStrings.get(mapID);
    }

    public static Map<Integer, String> getItemStringByName(String query) {
        String q = query.toLowerCase();
        Map<Integer, String> res = new HashMap<>();
        for (var entry : itemStrings.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            String name = entry.getValue();
            if (name == null) {
                continue;
            }
            String ssName = name.toLowerCase();
            if (ssName.contains(q)) {
                res.put(id, name);
            }
        }
        return res;
    }

    public static Map<Integer, SkillStringInfo> getSkillStringByName(String query) {
        String q = query.toLowerCase();
        Map<Integer, SkillStringInfo> res = new HashMap<>();
        for (var entry : skillString.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            SkillStringInfo ssi = entry.getValue();
            if (ssi == null || ssi.getName() == null) {
                continue;
            }
            String ssName = ssi.getName().toLowerCase();
            if (ssName.contains(q)) {
                res.put(id, ssi);
            }
        }
        return res;
    }

    public static Map<Integer, String> getMobStringByName(String query) {
        String q = query.toLowerCase();
        Map<Integer, String> res = new HashMap<>();
        for (var entry : mobStrings.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            String name = entry.getValue();
            if (name == null) {
                continue;
            }
            String ssName = name.toLowerCase();
            if (ssName.contains(q)) {
                res.put(id, name);
            }
        }
        return res;
    }

    public static Map<Integer, String> getNpcStringByName(String query) {
        String q = query.toLowerCase();
        Map<Integer, String> res = new HashMap<>();
        for (var entry : npcStrings.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            String name = entry.getValue();
            if (name == null) {
                continue;
            }
            String ssName = name.toLowerCase();
            if (ssName.contains(q)) {
                res.put(id, name);
            }
        }
        return res;
    }

    public static Map<Integer, String> getMapStringByName(String query) {
        String q = query.toLowerCase();
        Map<Integer, String> res = new HashMap<>();
        for (var entry : mapStrings.int2ObjectEntrySet()) {
            int id = entry.getIntKey();
            String name = entry.getValue();
            if (name == null) {
                continue;
            }
            String ssName = name.toLowerCase();
            if (ssName.contains(q)) {
                res.put(id, name);
            }
        }
        return res;
    }

    public static void clear() {
        skillString.clear();
        itemStrings.clear();
        mobStrings.clear();
        npcStrings.clear();
        mapStrings.clear();
    }

    public static void load() {
        loadSkillStringsFromWz();
        loadItemStringsFromWz();
        loadMobStringsFromWz();
        loadNpcStringsFromWz();
        loadMapStringsFromWz();
    }

    public static void generateTextFiles() {
        load();
        StringBuilder sb = new StringBuilder();
        // Skills
        TreeMap<Integer, SkillStringInfo> sortedSkillTree = new TreeMap<>(Comparator.comparingInt(Integer::intValue));
        sortedSkillTree.putAll(skillString);
        for (Map.Entry<Integer, SkillStringInfo> entry : sortedSkillTree.entrySet()) {
            sb.append(entry.getKey())
                    .append(',')
                    .append('"')
                    .append(entry.getValue())
                    .append('"')
                    .append("\r\n");
        }
        String dir = ServerConstants.RESOURCES_DIR + "/string" + version;
        Util.makeDirIfAbsent(dir);
        File file = new File(dir + "/Skill.txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println(sb);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        // Mob + Npc
        TreeMap<Integer, String> sortedTree = new TreeMap<>(Comparator.comparingInt(Integer::intValue));
        int i = 0;
        List<Map<Integer, String>> mapList = new ArrayList<>();
        mapList.add(mobStrings);
        mapList.add(npcStrings);
        String[] names = new String[]{"Mob", "Npc"};
        for (Map<Integer, String> map : mapList) {
            sb = new StringBuilder();
            sortedTree.clear();
            sortedTree.putAll(map);
            String fileName = names[i++] + ".txt";
            for (Map.Entry<Integer, String> entry : sortedTree.entrySet()) {
                sb.append(entry.getKey())
                        .append(',')
                        .append('"')
                        .append(entry.getValue())
                        .append('"')
                        .append("\r\n");
            }
            file = new File(dir + "/" + fileName);
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println(sb);
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
        // Map
        TreeMap<Integer, Tuple<String, String>> sortedTree2 = new TreeMap<>(Comparator.comparingInt(Integer::intValue));
        int i2 = 0;
        List<Map<Integer, Tuple<String, String>>> mapList2 = new ArrayList<>();
        mapList2.add(mapStrings2);
        String[] names2 = new String[]{"Map"};
        for (Map<Integer, Tuple<String, String>> map : mapList2) {
            sb = new StringBuilder();
            sortedTree2.clear();
            sortedTree2.putAll(map);
            String fileName = names2[i2++] + ".txt";
            for (Map.Entry<Integer, Tuple<String, String>> entry : sortedTree2.entrySet()) {
                sb.append(entry.getKey()).append(',').append('"').append(entry.getValue().getLeft()).append('"');
                if (entry.getValue().getRight() != null) {
                    sb.append(',').append('"').append(entry.getValue().getRight()).append('"');
                }
                sb.append("\r\n");
            }
            file = new File(dir + "/" + fileName);
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println(sb);
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
        // Items
        names = new String[]{"Eqp", "Use", "Ins", "Etc", "Cash"};
        List<Map<Integer, String>> mapItemsList = new ArrayList<>();
        for (int j = 0; j < names.length; j++) {
            mapItemsList.add(new TreeMap<>(Comparator.comparingInt(Integer::intValue)));
        }
        for (var entry : itemDescStrings.int2ObjectEntrySet()) {
            // put them into buckets, one bucket per item category
            mapItemsList.get(Math.max(0, entry.getIntKey() / 1000000 - 1)).put(entry.getIntKey(), entry.getValue());
        }
        i = 0;
        for (Map<Integer, String> map : mapItemsList) {
            sb = new StringBuilder();
            String fileName = names[i++] + ".txt";
            for (Map.Entry<Integer, String> entry : map.entrySet()) {
                if (entry.getValue() != null) { // Dịch mỗi desc thôi
                    sb.append(entry.getKey()).append(',').append('"').append(entry.getValue()).append('"').append("\r\n");
                }
            }
            file = new File(dir + "/" + fileName);
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println(sb);
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }
}
