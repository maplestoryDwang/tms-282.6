package net.swordie.ms.loaders.Etc.Achievement;

import net.swordie.ms.ServerConstants;
import net.swordie.ms.util.XMLApi;
import org.w3c.dom.Node;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AchievementInfoData {

    // Danh sách ban đầu, chỉ dùng để tải dữ liệu
    private static List<AchievementInfo> achievementInfos = new ArrayList<>();

    // Các Map để indexing dữ liệu, cho phép truy xuất cực nhanh
    private static final Map<Integer, AchievementInfo> achievementInfoByID = new HashMap<>();
    private static final Map<Integer, AchievementInfo.MissionInfo> missionInfoByID = new HashMap<>();
    private static final Map<Integer, List<Map.Entry<AchievementInfo, Integer>>> achievementInfoByQuestID = new HashMap<>();
    private static final Map<Long, List<Map.Entry<AchievementInfo, Integer>>> achievementInfoByFieldID = new HashMap<>();
    private static final Map<Integer, List<Map.Entry<AchievementInfo, Integer>>> achievementInfoByMobID = new HashMap<>();

    // Lấy danh sách ban đầu (ít dùng sau khi đã có Map)
    public static List<AchievementInfo> getAchievementInfos() {
        if (achievementInfos.isEmpty()) {
            load();
        }
        return achievementInfos;
    }

    // Truy xuất trực tiếp bằng ID (O(1))
    public static AchievementInfo getAchievementInfoByID(int infoID) {
        if (achievementInfoByID.isEmpty()) {
            load();
        }
        return achievementInfoByID.get(infoID);
    }

    // Truy xuất bằng QuestID (rất nhanh, O(1) để tra cứu, sau đó stream list nhỏ)
    public static Map<AchievementInfo, Integer> getAchievementInfoByQuestID(int questID) {
        if (achievementInfoByQuestID.isEmpty()) {
            load();
        }
        List<Map.Entry<AchievementInfo, Integer>> list = achievementInfoByQuestID.get(questID);
        if (list == null) {
            return new HashMap<>();
        }
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue));
    }

    // Truy xuất bằng FieldID (rất nhanh, O(1) để tra cứu)
    public static Map<AchievementInfo, Integer> getAchievementInfoByFieldID(int fieldID) {
        if (achievementInfoByFieldID.isEmpty()) {
            load();
        }
        List<Map.Entry<AchievementInfo, Integer>> list = achievementInfoByFieldID.get(fieldID);
        if (list == null) {
            return new HashMap<>();
        }
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue));
    }

    // Truy xuất bằng MobID (rất nhanh, O(1) để tra cứu)
    public static Map<AchievementInfo, Integer> getAchievementInfoByMobID(int mobID) {
        if (achievementInfoByMobID.isEmpty()) {
            load();
        }
        List<Map.Entry<AchievementInfo, Integer>> list = achievementInfoByMobID.get(mobID);
        if (list == null) {
            return new HashMap<>();
        }
        return list.stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue));
    }

    // Truy xuất MissionValue bằng MissionID (O(1))
    public static Long getMissionValueByInfoIDAndMissionID(int infoID, int missionID) {
        if (missionInfoByID.isEmpty()) {
            load();
        }
        AchievementInfo.MissionInfo mi = missionInfoByID.get(infoID);
        if (mi != null && mi.getId() == missionID) {
            return mi.getValue();
        }
        return 1L;
    }

    // Phương thức load đã được thay đổi để xây dựng các Map khi đọc dữ liệu
    public static void load() {
        // Xóa dữ liệu cũ để tránh trùng lặp nếu load lại
        achievementInfos.clear();
        achievementInfoByID.clear();
        missionInfoByID.clear();
        achievementInfoByQuestID.clear();
        achievementInfoByFieldID.clear();
        achievementInfoByMobID.clear();

        String wzDir = ServerConstants.WZ_DIR + "/Etc.wz/Achievement/AchievementData";
        File dir = new File(wzDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("Achievement directory not found: " + wzDir);
            return;
        }

        for (File file : dir.listFiles()) {
            AchievementInfo ai = new AchievementInfo(Integer.parseInt(file.getName().replace(".img.xml", "")));
            boolean isHide = false;
            List<AchievementInfo.MissionInfo> missionInfos = new ArrayList<>();
            Node node = XMLApi.getAllChildren(XMLApi.getRoot(file)).get(0);
            List<Node> nodes = XMLApi.getAllChildren(node);

            for (Node mainNode : nodes) {
                String mainName = XMLApi.getNamedAttribute(mainNode, "name");
                switch (mainName) {
                    case "info":
                        for (Node n : XMLApi.getAllChildren(mainNode)) {
                            String nName = XMLApi.getNamedAttribute(n, "name");
                            String nValue = XMLApi.getNamedAttribute(n, "value");
                            switch (nName) {
                                case "mainCategory": ai.setMainCategory(nValue); break;
                                case "subCategory": ai.setSubCategory(nValue); break;
                                case "name": ai.setName(nValue); break;
                                case "desc": ai.setDesc(nValue); break;
                                case "difficulty": ai.setDifficulty(nValue); break;
                                case "score": ai.setScore(Integer.parseInt(nValue)); break;
                                case "block": isHide = true; break;
                            }
                        }
                        break;
                    case "mission":
                        for (Node n : XMLApi.getAllChildren(mainNode)) {
                            String nName = XMLApi.getNamedAttribute(n, "name");
                            AchievementInfo.MissionInfo mi = new AchievementInfo.MissionInfo(Integer.parseInt(nName));
                            for (Node inner : XMLApi.getAllChildren(n)) {
                                String innerName = XMLApi.getNamedAttribute(inner, "name");
                                switch (innerName) {
                                    case "name":
                                        mi.setName(XMLApi.getNamedAttribute(inner, "value"));
                                        break;
                                    case "subMission":
                                        for (Node inner2 : XMLApi.getAllChildren(inner)) {
                                            String innerName2 = XMLApi.getNamedAttribute(inner2, "name");
                                            mi.setKey(innerName2);
                                            for (Node inner3 : XMLApi.getAllChildren(inner2)) {
                                                String innerName3 = XMLApi.getNamedAttribute(inner3, "name");
                                                switch (innerName3) {
                                                    case "score":
                                                        for (Node inner4 : XMLApi.getAllChildren(inner3)) {
                                                            if (XMLApi.getNamedAttribute(inner4, "name").equalsIgnoreCase("targetScore")) {
                                                                mi.setValue(Long.parseLong(XMLApi.getNamedAttribute(inner4, "value")));
                                                            }
                                                        }
                                                        break;
                                                    case "checkValue":
                                                        for (Node inner4 : XMLApi.getAllChildren(inner3)) {
                                                            String innerName4 = XMLApi.getNamedAttribute(inner4, "name");
                                                            switch (innerName4) {
                                                                case "mob":
                                                                    XMLApi.getAllChildren(inner4).forEach(inner5 -> {
                                                                        String name = XMLApi.getNamedAttribute(inner5, "name");
                                                                        if ("mob_id".equalsIgnoreCase(name) || "values".equalsIgnoreCase(name)) {
                                                                            if ("mob_id".equalsIgnoreCase(name)) {
                                                                                String value = XMLApi.getNamedAttribute(inner5, "value");
                                                                                if (value != null) mi.getMobIDs().add(Integer.parseInt(value));
                                                                            } else {
                                                                                XMLApi.getAllChildren(inner5).forEach(inner6 ->
                                                                                        XMLApi.getAllChildren(inner6).forEach(inner7 -> {
                                                                                            String value = XMLApi.getNamedAttribute(inner7, "value");
                                                                                            if (value != null) mi.getMobIDs().add(Integer.parseInt(value));
                                                                                        }));
                                                                            }
                                                                        }
                                                                    });
                                                                    break;
                                                                case "character":
                                                                    XMLApi.getAllChildren(inner4).forEach(inner5 -> {
                                                                        String name = XMLApi.getNamedAttribute(inner5, "name");
                                                                        if ("character_jobcode".equalsIgnoreCase(name)) {
                                                                            mi.getJobCodes().add(Integer.parseInt(XMLApi.getNamedAttribute(inner5, "value")));
                                                                        } else if ("values".equalsIgnoreCase(name)) {
                                                                            XMLApi.getAllChildren(inner5).forEach(inner6 ->
                                                                                    XMLApi.getAllChildren(inner6).forEach(inner7 -> {
                                                                                        if ("character_jobcode".equalsIgnoreCase(XMLApi.getNamedAttribute(inner7, "name"))) {
                                                                                            mi.getJobCodes().add(Integer.parseInt(XMLApi.getNamedAttribute(inner7, "value")));
                                                                                        }
                                                                                    }));
                                                                        }
                                                                    });
                                                                    break;
                                                                case "quest_change_info":
                                                                    XMLApi.getAllChildren(inner4).forEach(inner5 -> {
                                                                        String name = XMLApi.getNamedAttribute(inner5, "name");
                                                                        if ("quest_id".equalsIgnoreCase(name)) {
                                                                            String value = XMLApi.getNamedAttribute(inner5, "value");
                                                                            if (value != null) mi.getQuestIDs().add(Integer.parseInt(value));
                                                                        } else if ("values".equalsIgnoreCase(name)) {
                                                                            XMLApi.getAllChildren(inner5).forEach(inner6 ->
                                                                                    XMLApi.getAllChildren(inner6).forEach(inner7 -> {
                                                                                        String value = XMLApi.getNamedAttribute(inner7, "value");
                                                                                        if (value != null) mi.getQuestIDs().add(Integer.parseInt(value));
                                                                                    }));
                                                                        }
                                                                    });
                                                                    break;
                                                                case "field":
                                                                    XMLApi.getAllChildren(inner4).stream().filter(inner5 -> "field_id".equalsIgnoreCase(XMLApi.getNamedAttribute(inner5, "name"))).findFirst().ifPresent(inner5 ->
                                                                            mi.setFieldID(Long.parseLong(XMLApi.getNamedAttribute(inner5, "value"))));
                                                                    break;
                                                                case "item":
                                                                    XMLApi.getAllChildren(inner4).stream().filter(inner5 -> "values".equalsIgnoreCase(XMLApi.getNamedAttribute(inner5, "name"))).findFirst().ifPresent(inner5 ->
                                                                            XMLApi.getAllChildren(inner5).forEach(inner6 ->
                                                                                    XMLApi.getAllChildren(inner6).forEach(inner7 -> {
                                                                                        if ("item_id".equalsIgnoreCase(XMLApi.getNamedAttribute(inner7, "name"))) {
                                                                                            mi.getItemIDs().add(Long.parseLong(XMLApi.getNamedAttribute(inner7, "value")));
                                                                                        }
                                                                                    })));
                                                                    break;
                                                            }
                                                        }
                                                        break;
                                                }
                                            }
                                        }
                                        break;
                                }
                            }
                            missionInfos.add(mi);
                        }
                        break;
                }
            }

            if (!isHide) {
                ai.setMissions(missionInfos);
                achievementInfos.add(ai);

                // PUT vào các Map để indexing, đây là phần tối ưu hóa
                achievementInfoByID.put(ai.getId(), ai);
                for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                    missionInfoByID.put(mi.getId(), mi);

                    // Indexing cho QuestID
                    for (Integer questID : mi.getQuestIDs()) {
                        achievementInfoByQuestID.computeIfAbsent(questID, k -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(ai, mi.getId()));
                    }

                    // Indexing cho FieldID
                    if (mi.getFieldID() != 0) {
                        achievementInfoByFieldID.computeIfAbsent(mi.getFieldID(), k -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(ai, mi.getId()));
                    }

                    // Indexing cho MobID
                    for (Integer mobID : mi.getMobIDs()) {
                        achievementInfoByMobID.computeIfAbsent(mobID, k -> new ArrayList<>()).add(new AbstractMap.SimpleEntry<>(ai, mi.getId()));
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        load();
        List<AchievementInfo> ars = new ArrayList<>();
        for (AchievementInfo ai : getAchievementInfos()) {
            for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                if (mi.getKey() != null && (mi.getKey().equalsIgnoreCase("quest_state_change")
                        || mi.getKey().equalsIgnoreCase("user_lvup")
                        || mi.getKey().equalsIgnoreCase("multikill")
                        || mi.getKey().equalsIgnoreCase("mob_kill")
                        || mi.getKey().equalsIgnoreCase("field_enter")
                        || mi.getKey().equalsIgnoreCase("script")
                        || mi.getKey().equalsIgnoreCase("suddenmission_complete")
                        || mi.getKey().equalsIgnoreCase("suddenmission_reward")
                        || mi.getKey().equalsIgnoreCase("quest_qrex_change")
                        || mi.getKey().equalsIgnoreCase("achievement_state_change")
                        || mi.getKey().equalsIgnoreCase("ability_change")
                        || mi.getKey().equalsIgnoreCase("combokill_get_marble")
                        || mi.getKey().equalsIgnoreCase("combokill_increse")
                        || mi.getKey().equalsIgnoreCase("item_use"))
                        || (ai.getSubCategory() != null && ai.getSubCategory().equalsIgnoreCase("loot"))
                        || (ai.getId() == 862 || ai.getId() == 863 || ai.getId() == 864)
                ) {
                    //
                } else {
                    ars.add(ai);
                }
            }
        }
        for (AchievementInfo ai : ars) {
            System.out.printf("[ID: %d] " +
                            "mainCategory: %s | " +
                            "subCategory: %s | " +
                            "name: %s | " +
                            "desc: %s | " +
                            "difficulty: %s | " +
                            "score: %d | " +
                            "mission size: %d. %n",
                    ai.getId(),
                    ai.getMainCategory(),
                    ai.getSubCategory(),
                    ai.getName(),
                    ai.getDesc(),
                    ai.getDifficulty(),
                    ai.getScore(),
                    ai.getMissions().size());
            for (AchievementInfo.MissionInfo mi : ai.getMissions()) {
                System.out.printf("[Achievement ID: %d] " +
                                "Mission: %d | " +
                                "name: %s | " +
                                "key: %s | " +
                                "value: %d | ",
                        ai.getId(),
                        mi.getId(),
                        mi.getName(),
                        mi.getKey(),
                        mi.getValue());
                if (mi.getJobCodes().size() > 0) {
                    System.out.print("jobCode: " + mi.getJobCodes().size() + " | ");
                }
                if (mi.getMobIDs().size() > 0) {
                    System.out.print("mobIDs: " + mi.getMobIDs().size() + " | ");
                }
                if (mi.getQuestIDs().size() > 0) {
                    System.out.print("questIDs: " + mi.getQuestIDs().size() + " | ");
                }
                System.out.println();
            }
            System.out.println("---");
        }
    }
}