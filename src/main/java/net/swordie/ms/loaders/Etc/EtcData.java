package net.swordie.ms.loaders.Etc;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.character.items.BossSoul;
import net.swordie.ms.client.character.items.ItemOption;
import net.swordie.ms.client.character.items.SetEffect;
import net.swordie.ms.enums.ScrollStat;
import net.swordie.ms.enums.SoulType;
import net.swordie.ms.loaders.containerclasses.AndroidInfo;
import net.swordie.ms.util.*;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.util.container.Tuple;
import org.w3c.dom.Node;

import java.io.*;
import java.util.*;

public class EtcData {

    private static final Map<Integer, Integer> familiarSkills = new HashMap<>();
    private static Map<Integer, BossSoul> soulCollection = new HashMap<>();
    private static final Map<Integer, SetEffect> setEffects = new HashMap<>();
    private static final Map<Integer, Integer> characterCards = new HashMap<>();
    private static final String SCROLL_STAT_ID = "1";
    private static final String ITEM_OPTION_ID = "2";
    private static Map<Integer, AndroidInfo> androidInfo = new HashMap<>();
    private static List<Triple<String, String, Boolean>> questions = new LinkedList<>();

    /**
     * Etc.wz/{@code name}: the 282 layer's file when the TMS 282 overlay has one (exported from the 282 client by
     * research/tms2823/android-export/ExportEtc282.java), else the base WZ file.
     */
    public static File etcWzFile(String name) {
        List<File> layers = net.swordie.ms.loaders.overlay.DataOverlays.current().wzLayers("Etc.wz/" + name);
        for (int i = layers.size() - 1; i >= 0; i--) {
            if (layers.get(i).isFile()) {
                return layers.get(i);
            }
        }
        return new File(String.format("%s/Etc.wz/%s", ServerConstants.WZ_DIR, name));
    }

    public static void loadAndroidsFromWz() {
        String wzDir = ServerConstants.WZ_DIR + "/Etc.wz/Android";
        File dir = new File(wzDir);
        for (File file : dir.listFiles()) {
            AndroidInfo ai = parseAndroidXml(file);
            androidInfo.put(ai.getId(), ai);
        }
    }

    /** One Etc/Android/NNNN.img.xml: its costume skins, hairs and faces (hair/face % 10000, skin % 1000, as 265 stores them). */
    static AndroidInfo parseAndroidXml(File file) {
        AndroidInfo ai = new AndroidInfo(Integer.parseInt(file.getName().replace(".img.xml", "")));
        Node node = XMLApi.getAllChildren(XMLApi.getRoot(file)).get(0);
        List<Node> nodes = XMLApi.getAllChildren(node);
        for (Node mainNode : nodes) {
            String mainName = XMLApi.getNamedAttribute(mainNode, "name");
            switch (mainName) {
                case "costume":
                    for (Node n : XMLApi.getAllChildren(mainNode)) {
                        String nName = XMLApi.getNamedAttribute(n, "name");
                        switch (nName) {
                            case "face":
                                for (Node inner : XMLApi.getAllChildren(n)) {
                                    ai.addFace(Integer.parseInt(XMLApi.getNamedAttribute(inner, "value")) % 10000);
                                }
                                break;
                            case "hair":
                                for (Node inner : XMLApi.getAllChildren(n)) {
                                    ai.addHair(Integer.parseInt(XMLApi.getNamedAttribute(inner, "value")) % 10000);
                                }
                                break;
                            case "skin":
                                for (Node inner : XMLApi.getAllChildren(n)) {
                                    ai.addSkin(Integer.parseInt(XMLApi.getNamedAttribute(inner, "value")) % 1000);
                                }
                                break;
                        }
                    }
                    break;
            }
        }
        return ai;
    }

    public static void loadSetEffectsFromWz() {
        //String wzDir = ServerConstants.WZ_DIR + "/Etc.wz/SetItemInfo";
        //Node root = XMLApi.getRoot(new File(wzDir));
        File file = new File(String.format("%s/Etc.wz/SetItemInfo.img.xml", ServerConstants.WZ_DIR));
        Node root = XMLApi.getRoot(file);
        Node mainNode = XMLApi.getAllChildren(root).get(0);
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int setId = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            Node effectNode = XMLApi.getFirstChildByNameBF(node, "Effect");
            List<Node> nodes1 = XMLApi.getAllChildren(effectNode);
            for (Node node1 : nodes1) {
                int level = Integer.parseInt(XMLApi.getNamedAttribute(node1, "name"));
                SetEffect setEffect = setEffects.getOrDefault(setId, new SetEffect());
                List<Node> nodes2 = XMLApi.getAllChildren(node1);
                for (Node node2 : nodes2) {
                    String ssName = XMLApi.getNamedAttribute(node2, "name");
                    ScrollStat stat = ScrollStat.getScrollStatByString(ssName);
                    if (!ssName.equals("Option") && stat != null) {
                        int statAmount = Integer.parseInt(XMLApi.getNamedAttribute(node2, "value"));
                        setEffect.addScrollStat(level, stat, statAmount);
                    } else if (ssName.equals("Option")) {
                        List<Node> nodes3 = XMLApi.getAllChildren(node2);
                        for (Node node3 : nodes3) {
                            Node optionLevel = XMLApi.getFirstChildByNameDF(node3, "level");
                            Node option = XMLApi.getFirstChildByNameDF(node3, "option");
                            ItemOption io = new ItemOption();
                            io.setId(Integer.parseInt(XMLApi.getNamedAttribute(option, "value")));
                            io.setReqLevel(Integer.parseInt(XMLApi.getNamedAttribute(optionLevel, "value")));
                            setEffect.addOption(level, io);
                        }
                    }
                }
                setEffects.put(setId, setEffect);
            }
        }
    }

    public static Integer loadSetItemCompleteCount(int setItemID) {
        //String wzDir = ServerConstants.WZ_DIR + "/Etc.wz/SetItemInfo";
        //Node root = XMLApi.getRoot(new File(wzDir));
        File file = new File(String.format("%s/Etc.wz/SetItemInfo.img.xml", ServerConstants.WZ_DIR));
        Node root = XMLApi.getRoot(file);
        Node mainNode = XMLApi.getAllChildren(root).get(0);
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int setId = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            if (setItemID == setId) {
                Node completeCountID = XMLApi.getFirstChildByNameBF(node, "completeCount");
                if (completeCountID != null) {
                    return Integer.parseInt(XMLApi.getNamedAttribute(completeCountID, "value"));
                }
            }
        }
        return 0;
    }

    public static List<Integer> getItemsInSetItem(int setItemID) {
        List<Integer> items = new ArrayList<>();
        File file = new File(String.format("%s/Etc.wz/SetItemInfo.img.xml", ServerConstants.WZ_DIR));
        Node root = XMLApi.getRoot(file);
        Node mainNode = XMLApi.getAllChildren(root).get(0);
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int setId = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            if (setItemID == setId) {
                List<Node> nodes_1 = XMLApi.getAllChildren(node);
                for (var node_1 : nodes_1) {
                    List<Node> nodes_2 = XMLApi.getAllChildren(node_1);
                    for (var node_2 : nodes_2) {
                        List<Node> nodes_3 = XMLApi.getAllChildren(node_2);
                        if (nodes_3.size() == 0) {
                            String itemString = XMLApi.getNamedAttribute(node_2, "value");
                            try {
                                items.add(Integer.parseInt(itemString));
                            } catch (Exception ignored) {
                            }
                        } else {
                            for (var node_3 : nodes_3) {
                                String itemString = XMLApi.getNamedAttribute(node_3, "value");
                                try {
                                    items.add(Integer.parseInt(itemString));
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }
        return items;
    }

    public static void loadCharacterCardsFromWz() {
        File file = etcWzFile("CharacterCard.img.xml");
        Node root = XMLApi.getRoot(file);
        Node firstNode = XMLApi.getAllChildren(root).get(0);
        Node mainNode = XMLApi.getFirstChildByNameBF(firstNode, "Card");
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int jobId = Integer.parseInt(XMLApi.getNamedAttribute(node, "name")) * 10;
            Node skillNode = XMLApi.getFirstChildByNameBF(firstNode, "skillID");
            int skillId = Integer.parseInt(XMLApi.getNamedAttribute(skillNode, "value"));
            characterCards.put(jobId, skillId);
        }
    }

    public static void saveCharacterCards(String dir) {
        Util.makeDirIfAbsent(dir);
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(dir + "/charactercards.dat")));
            dataOutputStream.writeInt(characterCards.size());
            for (Map.Entry<Integer, Integer> entry : characterCards.entrySet()) {
                dataOutputStream.writeInt(entry.getKey());
                dataOutputStream.writeInt(entry.getValue());
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static void saveSetEffects(String dir) {
        Util.makeDirIfAbsent(dir);
        for (Map.Entry<Integer, SetEffect> entry : setEffects.entrySet()) {
            try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(dir + "/" + entry.getKey() + ".dat")))) {
                dataOutputStream.writeShort(entry.getValue().getEffectsToLevel().size()); //
                for (Map.Entry<Integer, List<Object>> level : entry.getValue().getEffectsToLevel().entrySet()) {
                    dataOutputStream.writeInt(level.getKey()); //levels non consistent
                    dataOutputStream.writeShort(level.getValue().size());
                    for (Object stat : level.getValue()) {
                        if (stat instanceof Tuple) {
                            dataOutputStream.writeUTF(SCROLL_STAT_ID);
                            dataOutputStream.writeUTF(((Tuple) stat).getLeft().toString());
                            dataOutputStream.writeInt(Integer.parseInt(((Tuple) stat).getRight().toString()));
                        } else if (stat instanceof ItemOption) {
                            dataOutputStream.writeUTF(ITEM_OPTION_ID);
                            dataOutputStream.writeInt(((ItemOption) stat).getId());
                            dataOutputStream.writeInt(((ItemOption) stat).getReqLevel());
                        }
                    }
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static void loadCharacterCards(String dir) {
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(dir))) {
            short size = dataInputStream.readShort();
            for (int i = 0; i < size; i++) {
                int jobId = dataInputStream.readInt();
                int skillId = dataInputStream.readInt();
                characterCards.put(jobId, skillId);
            }
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    public static int getCharacterCardSkillByJob(int jobId) {
        if (characterCards.isEmpty()) {
            loadCharacterCards(String.format("%s/etc/%d.dat", ServerConstants.DAT_DIR, "charactercards"));
        }
        return characterCards.getOrDefault(jobId, 0);
    }

    public static SetEffect loadSetEffectByFile(String file) {
        SetEffect setEffect = new SetEffect();
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(file))) {
            short levelSize = dataInputStream.readShort();
            for (int i = 0; i < levelSize; i++) {
                int level = dataInputStream.readInt();
                short statSize = dataInputStream.readShort();
                for (int j = 0; j < statSize; j++) {
                    String type = dataInputStream.readUTF();
                    if (type.equals(SCROLL_STAT_ID)) {
                        Tuple<ScrollStat, Integer> ss = new Tuple<>(ScrollStat.getScrollStatByString(dataInputStream.readUTF()), dataInputStream.readInt());
                        setEffect.addScrollStat(level, ss.getLeft(), ss.getRight());
                    } else if (type.equals(ITEM_OPTION_ID)) {
                        ItemOption io = new ItemOption();
                        io.setId(dataInputStream.readInt());
                        io.setReqLevel(dataInputStream.readInt());
                        setEffect.addOption(level, io);
                    }
                }
            }
        } catch (IOException e) {
            //System.err.println(file + " non exists.");
        }
        return setEffect;
    }

    public static SetEffect getSetEffectInfoById(int setID) {
        if (setEffects.containsKey(setID)) {
            return setEffects.get(setID);
        }
        // 282 profile: data/dat282/etc/setEffects/<id>.dat first (DataOverlay.datFile); 265: DAT_DIR as before.
        return loadSetEffectByFile(net.swordie.ms.loaders.overlay.DataOverlays.current()
                .datFile("etc/setEffects/" + setID + ".dat").getPath());
    }

    public static void loadSoulCollectionFromWz() {
        String wzDir = ServerConstants.WZ_DIR + "/Etc.wz/SoulCollection.img.xml";
        File dir = new File(wzDir);
        Node node = XMLApi.getFirstChildByNameBF(XMLApi.getRoot(dir), "SoulCollection.img");
        List<Node> nodes = XMLApi.getAllChildren(node);
        for (Node mainNode : nodes) {
            int skillId = 0;
            int skillIdH = 0;
            Node soulSkill = XMLApi.getFirstChildByNameBF(mainNode, "soulSkill");
            Node soulSkillH = XMLApi.getFirstChildByNameBF(mainNode, "soulSkillH");
            if (soulSkill != null) {
                skillId = Integer.parseInt(XMLApi.getNamedAttribute(soulSkill, "value"));
            }
            if (soulSkillH != null) {
                skillIdH = Integer.parseInt(XMLApi.getNamedAttribute(soulSkillH, "value"));
            }

            Node soulList = XMLApi.getFirstChildByNameBF(mainNode, "soulList");
            List<Node> soulNodes = XMLApi.getAllChildren(soulList);
            for (int i = 0; i < soulNodes.size(); i++) {
                Node soulNode = soulNodes.get(i);
                int soul1 = -1;
                int soul2 = -1;
                Node soul1Node = XMLApi.getFirstChildByNameBF(soulNode, "0");
                Node soul2Node = XMLApi.getFirstChildByNameBF(soulNode, "1");
                if (soul1Node != null) {
                    soul1 = Integer.valueOf(XMLApi.getNamedAttribute(soul1Node, "value"));
                }
                if (soul2Node != null) {
                    soul2 = Integer.valueOf(XMLApi.getNamedAttribute(soul2Node, "value"));
                }
                int finalSkillId = -1;
                if (i <= SoulType.Radiant.getVal()) { //normal soul
                    finalSkillId = skillId;
                } else { //hard soul
                    finalSkillId = skillIdH;
                }
                BossSoul bs = new BossSoul(finalSkillId, SoulType.getSoulTypeByVal(i));
                if (finalSkillId != -1) {
                    if (soul1 != -1) {
                        soulCollection.put(soul1, bs);
                    }
                    if (soul2 != -1) {
                        soulCollection.put(soul2, bs);
                    }
                }
            }
        }
    }

    public static void saveSoulCollection(String dir) {
        Util.makeDirIfAbsent(dir);
        try (DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(new File(dir + "/" + "soulCollection" + ".dat")))) {
            dataOutputStream.writeInt(soulCollection.size());
            for (Map.Entry<Integer, BossSoul> soulEntry : soulCollection.entrySet()) {
                dataOutputStream.writeInt(soulEntry.getKey());
                dataOutputStream.writeInt(soulEntry.getValue().getSkillId());
                dataOutputStream.writeByte(soulEntry.getValue().getSoulType().getVal());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadSoulCollectionFromFile(String dir) {
        Util.makeDirIfAbsent(dir);
        try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(new File(dir + "/" + "soulCollection" + ".dat")))) {
            int size = dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                int itemId = dataInputStream.readInt();
                int skillId = dataInputStream.readInt();
                byte soulType = dataInputStream.readByte();
                soulCollection.put(itemId, new BossSoul(skillId, soulType));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveAndroidInfo(String dir) {
        Util.makeDirIfAbsent(dir);
        for (AndroidInfo ai : androidInfo.values()) {
            File file = new File(String.format("%s/%d.dat", dir, ai.getId()));
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(ai.getId());
                dos.writeInt(ai.getHairs().size());
                for (int hair : ai.getHairs()) {
                    dos.writeInt(hair);
                }
                dos.writeInt(ai.getFaces().size());
                for (int face : ai.getFaces()) {
                    dos.writeInt(face);
                }
                dos.writeInt(ai.getSkins().size());
                for (int skin : ai.getSkins()) {
                    dos.writeInt(skin);
                }
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    public static AndroidInfo getAndroidInfoById(int androidId) {
        if (androidInfo.containsKey(androidId)) {
            return androidInfo.get(androidId);
        }
        String dat = String.format("%s/etc/android/%d.dat", ServerConstants.DAT_DIR, androidId);
        if (new File(dat).isFile()) {
            return loadAndroidInfoFromFile(dat);
        }
        return loadOverlayAndroid(androidId);
    }

    /**
     * 282 profile: android types only the 282 client has (e.g. 318 of item 1662271), exported by
     * research/tms2823/android-export/ExportAndroids282.java to data/wz282/Etc.wz/Android. Null when no layer has it.
     */
    private static AndroidInfo loadOverlayAndroid(int androidId) {
        List<File> layers = net.swordie.ms.loaders.overlay.DataOverlays.current()
                .wzLayers(String.format("Etc.wz/Android/%04d.img.xml", androidId));
        for (int i = layers.size() - 1; i >= 0; i--) {
            File f = layers.get(i);
            if (f.isFile()) {
                try {
                    AndroidInfo ai = parseAndroidXml(f);
                    androidInfo.put(ai.getId(), ai);
                    return ai;
                } catch (RuntimeException e) {
                    System.out.printf("[EtcData] android %d unreadable in %s: %s%n", androidId, f, e);
                }
            }
        }
        return null;
    }

    private static AndroidInfo loadAndroidInfoFromFile(String file) {
        AndroidInfo ai = null;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            ai = new AndroidInfo(dis.readInt());
            int size = dis.readInt();
            for (int i = 0; i < size; i++) {
                ai.addHair(dis.readInt());
            }
            size = dis.readInt();
            for (int i = 0; i < size; i++) {
                ai.addFace(dis.readInt());
            }
            size = dis.readInt();
            for (int i = 0; i < size; i++) {
                ai.addSkin(dis.readInt());
            }
            androidInfo.put(ai.getId(), ai);
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return ai;
    }

    public static void generateDatFiles() {
        System.out.println("Started generating etc data.");
        Util.makeDirIfAbsent(ServerConstants.DAT_DIR + "/etc");
        long start = System.currentTimeMillis();

        loadAndroidsFromWz();
        saveAndroidInfo(ServerConstants.DAT_DIR + "/etc/android");

        loadSetEffectsFromWz();
        saveSetEffects(ServerConstants.DAT_DIR + "/etc/setEffects");

        loadCharacterCardsFromWz();
        saveCharacterCards(ServerConstants.DAT_DIR + "/etc");

        //loadFamiliarSkillsFromWz();
        //saveFamiliarSkills(ServerConstants.DAT_DIR + "/etc/familiarInfo");

        loadOXQuizFromWZ(); // Phú lười ok?

        loadSoulCollectionFromWz();
        saveSoulCollection(ServerConstants.DAT_DIR + "/etc/soulCollection");

        System.out.printf("Completed generating etc data in %dms%n", System.currentTimeMillis() - start);
    }

    public static void clear() {
        familiarSkills.clear();
        setEffects.clear();
        characterCards.clear();
        androidInfo.clear();
        questions.clear();
    }

    public static void load() {
        loadAndroidsFromWz();
        loadSetEffectsFromWz();
        loadCharacterCardsFromWz();
        loadFamiliarSkillsFromWz();
        loadOXQuizFromWZ();
    }

    public static void main(String[] args) {
        generateDatFiles();
    }

    private static void loadFamiliarSkillsFromWz() {
        File file = new File(String.format("%s/Etc.wz/FamiliarInfo.img.xml", ServerConstants.WZ_DIR));
        if (!file.isFile()) {
            return;
        }
        Node root = XMLApi.getRoot(file);
        if (root == null) {
            return;
        }
        List<Node> children = XMLApi.getAllChildren(root);
        if (children.isEmpty()) {
            return;
        }
        Node mainNode = children.get(0);
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) {
            int id = Integer.parseInt(XMLApi.getNamedAttribute(node, "name"));
            Node skillIDNode = XMLApi.getFirstChildByNameBF(node, "passive");
            if (skillIDNode != null) {
                int skillID = Integer.parseInt(XMLApi.getNamedAttribute(skillIDNode, "value"));
                familiarSkills.put(id, skillID);
            }
        }
    }

    @Saver(varName = "familiarSkills")
    private static void saveFamiliarSkills(String file) {
        Util.makeDirIfAbsent(file);
        for (Map.Entry<Integer, Integer> entry : familiarSkills.entrySet()) {
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(file + "/" + entry.getKey() + ".dat")))) {
                dos.writeInt(entry.getValue()); // skill ID
            } catch (IOException e) {
                DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
            }
        }
    }

    @Loader(varName = "familiarSkills")
    public static void loadFamiliarSkills(File file, boolean exists) {
        if (!exists) {
            familiarSkills.clear();
            loadFamiliarSkillsFromWz();
            saveFamiliarSkills(ServerConstants.DAT_DIR + "/etc/familiarInfo");
            File parent = file.getParentFile();
            if (parent != null) {
                Util.makeDirIfAbsent(parent.getPath());
            }
            try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                dos.writeInt(familiarSkills.size());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to write familiarSkills cache marker at " + file.getPath(), e);
            }
            return;
        }
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            dis.readInt();
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }

    private static int readFamiliarSkillFromDat(String path) {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(path))) {
            return dis.readInt();
        } catch (IOException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return 0;
    }

    public static int getSkillByFamiliarID(int familiarID) {
        if (familiarSkills.containsKey(familiarID)) {
            return familiarSkills.get(familiarID);
        }
        return readFamiliarSkillFromDat(String.format("%s/etc/familiarInfo/%d.dat", ServerConstants.DAT_DIR, familiarID));
    }

    public static void loadOXQuizFromWZ() {
        File file = etcWzFile("OXQuiz.img.xml");
        Node root = XMLApi.getRoot(file);
        Node mainNode = XMLApi.getAllChildren(root).get(0);
        List<Node> nodes = XMLApi.getAllChildren(mainNode);
        for (Node node : nodes) { // 1 - 9
            List<Node> nodes1 = XMLApi.getAllChildren(node);
            for (Node node1 : nodes1) { // 0 - 120 ....
                List<Node> nodes2 = XMLApi.getAllChildren(node1);
                String q = null;
                String d = null;
                boolean a = false;
                for (Node node2 : nodes2) { // d a q
                    String nodeName = XMLApi.getNamedAttribute(node2, "name");
                    switch (nodeName) {
                        case "d":
                            d = XMLApi.getNamedAttribute(node2, "value");
                            break;
                        case "q":
                            q = XMLApi.getNamedAttribute(node2, "value");
                            break;
                        case "a":
                            a = Integer.parseInt(XMLApi.getNamedAttribute(node2, "value")) != 0;
                            break;
                    }
                }
                questions.add(new Triple<>(q, d, a));
            }
        }
    }

    public static List<Triple<String, String, Boolean>> getOXQuizs() {
        if (questions.isEmpty()) {
            loadOXQuizFromWZ();
        }
        return questions;
    }
}
