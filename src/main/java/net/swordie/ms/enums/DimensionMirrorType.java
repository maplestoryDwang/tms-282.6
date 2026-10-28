package net.swordie.ms.enums;

import net.swordie.ms.util.Util;

import java.util.Arrays;

public enum DimensionMirrorType {
    MulungDojo(0, 925020000, "武陵道場", "挑戰武陵道場，看看你真正的實力吧。", 105, 0, 1207, new int[]{1082392, 1082393, 1082394}, false),
    EreveConferencePavilion(1, 913050010, "聯盟會議場", "大陸會議上結成了楓之谷聯盟。\n在「黑色天堂」與「楓之谷英雄」中對抗黑魔法師吧！", 75, 0, 7998, new int[]{1142769, 1142804, 3015030, 3700350, 3017016}, false),
    FightForAzwan(2, 262010000, "阿斯旺解放戰", "消滅阿斯旺地區殘存的希拉勢力。", 120, 0, 7892, new int[]{1162009, 2430690, 1032136}, false),
    EvolutionLab(3, 957000000, "進化系統研究所", "準備好體驗進化系統的奇妙了嗎？", 105, 1801, 1819, new int[]{1162013, 4001832, 2431935, 2431936}, false),
    DimensionalInvasion(4, 940020000, "次元入侵", "趁還來得及，打倒入侵者吧！", 140, 0, 31836, new int[]{1082488, 1082489, 1142527, 1142528, 1142529, 1142530}, false),
    CrimsonwoodCastle(5, 610030010, "緋紅之心城堡", "來自瑪斯特里亞的訊息……發生什麼事了？", 130, 31241, 31265, new int[]{1142619, 2431935, 2431936}, false),
    GrandAthanaeum(6, 302000000, "次元圖書館", "前往次元圖書館，一窺過去的故事。", 100, 0, 32670, new int[]{1122263, 2431892}, false),
    PartyQuestHub(7, 910002000, "組隊任務入口", "和朋友一起玩更有趣！組成隊伍\n參加特別的任務吧。", 50, 0, 7887, new int[]{1003762, 1022073, 1132013, 1022175, 1902048, 2432131}, true),
    TowerOfOz(8, 992000000, "海底之塔", "攻略名為海底之塔的水中之塔，幫助艾莉西亞的靈魂。", 140, 42009, 7839, new int[]{2028263, 2432465}, false),
    FriendStory(9, 100000004, "朋友物語", "體驗楓之谷的第一部外傳：朋友物語！", 100, 32707, 33041, new int[]{1182079, 3015119, 3010875, 2432776, 2432788}, false),
    MonsterPark(10, 951000000, "怪物公園", "大量經驗值與每天不同的獎勵等著你！", 105, 0, 7900, new int[]{2434746, 2434747, 2434748, 2434749, 2434750, 2434751, 2434745}, false),
    //Maplerunner(11, 993001000, "Maple Runner", "See how quickly you can run and jump through 40 fun stages in the all-new Maple Runner!", 30, 34555, 18837, new int[] { 1142957, 1142958, 1142959 , 4310195 }, false),
    RootAbyss(11, 910700200, "露塔必思", "與封印守護者戰鬥並削弱他們。\n楓之谷世界的未來就交給你了。", 125, 30000, 30028, new int[]{1003715, 1003716, 1003717, 1003718}, false),
    Ursus(12, 970072200, "烏勒斯", "你有自信對抗破壞者烏勒斯嗎？", 100, 0, 33553, new int[]{3015279, 2434509, 2434389, 3700334, 1142879}, true),
    GhostPark(13, 100000000, "鬼魂公園", "", 1, 0, 0, new int[]{}, false), // new
    //Alishan(15, 749080900, 1, "Alishan", "Do you want to move to Alishan?", 33, 55234, 55255, new int[] { 1202160, 2434004 }, false),
    //TwistedAquaRoad(19, 860000000, "Twisted Aqua Road", "Do you want to move to Twisted Aqua Road?", 200, 17100, 17132, new int[] {}, false),
    //Momijigaoka(20, 807000000, 4, "Momijigaoka", "Do you want to move to Momijigaoka?", 10, 0, 7844, new int[] {}, false),
    //BlackgateCity(21, 610050000, "Blackgate City", "Do you want to move to Blackgate City?", 100, 0, 61132, new int[] {1012535, 1122312, 1132289, 1152191, 1113185}, false),
    PrincessNoPQ(501, 811000000, "濃姬組隊任務", "要挑戰濃姬嗎？", 140, 58913, 58971, new int[]{2432755, 2432754, 2432753, 3010864}, false),
    EventHall(503, 820000000, 2, "活動會場", "要前往活動會場嗎？", 1, 0, 5399, new int[]{}, false),
    AlienVisitor(504, 861000000, "外星訪客", "要前往外星訪客嗎？", 200, 0, 17201, new int[]{1113038, 1122256, 1032191, 1132230, 1003893}, false),
    CommerciRepublic(505, 865000001, "主題地城：\r\n 聖克梅勒茲", "要前往聖克梅勒茲嗎？", 140, 17600, 17699, new int[]{1142981, 1302299, 1382232, 1452227, 1332249, 1492200}, false),
    HyperspaceCube(506, 100000000, "超空間方塊", "", 1, 0, 0, new int[]{}, false), // new
    Afterlands(508, "後世界", "要前往後世界嗎？", 75, 63020, 63255, new int[]{1202237, 1202238, 1202239, 1202240}, false),
    MushroomShrine(509, 800000000, "菇菇神社", "", 1, 0, 0, new int[]{}, false), // new
    AbrupBaseCamp(510, 100000000, "阿布魯普基地", "", 1, 0, 0, new int[]{}, false), // new
    NewLeafCity(511, 600000000, "新葉城", "", 1, 0, 0, new int[]{}, false), // new
    Gollux(512, 863010000, "培羅德", "", 1, 0, 0, new int[]{}, false), // new
    MapleTour(513, 100000000, "楓之谷旅遊", "", 1, 0, 0, new int[]{}, false), // new
    ;

    private final String name;
    private final String desc;
    private final int id;
    private final int mapId;
    private final int portal;
    private final int reqLevel;
    private final int reqQuest;
    private final int questToSave;
    private final int[] rewards;
    private final boolean squad;

    DimensionMirrorType(int id, int mapId, int portal, String name, String desc, int reqLevel, int reqQuest, int questToSave, int[] rewards, boolean squad) {
        this.id = id;
        this.mapId = mapId;
        this.portal = portal;
        this.name = name;
        this.desc = desc;
        this.reqLevel = reqLevel;
        this.reqQuest = reqQuest;
        this.questToSave = questToSave;
        this.rewards = rewards;
        this.squad = squad;
    }

    DimensionMirrorType(int id, int mapId, String name, String desc, int reqLevel, int reqQuest, int questToSave, int[] rewards, boolean squad) {
        this.id = id;
        this.mapId = mapId;
        this.portal = 0;
        this.name = name;
        this.desc = desc;
        this.reqLevel = reqLevel;
        this.reqQuest = reqQuest;
        this.questToSave = questToSave;
        this.rewards = rewards;
        this.squad = squad;
    }

    DimensionMirrorType(int id, String name, String desc, int reqLevel, int reqQuest, int questToSave, int[] rewards, boolean squad) {
        this.id = id;
        this.mapId = 0;
        this.portal = 0;
        this.name = name;
        this.desc = desc;
        this.reqLevel = reqLevel;
        this.reqQuest = reqQuest;
        this.questToSave = questToSave;
        this.rewards = rewards;
        this.squad = squad;
    }

    public static DimensionMirrorType getByMapId(int mapId) {
        return Util.findWithPred(Arrays.asList(values()), csat -> csat.getMapId() == mapId);
    }

    public static DimensionMirrorType getByID(int id) {
        return Util.findWithPred(Arrays.asList(values()), csat -> csat.getId() == id);
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public int getId() {
        return id;
    }

    public int getReqLevel() {
        return reqLevel;
    }

    public int getReqQuest() {
        return reqQuest;
    }

    public int[] getRewards() {
        return rewards;
    }

    public boolean isSquad() {
        return squad;
    }

    public int getQuestToSave() {
        return questToSave;
    }

    public int getMapId() {
        return mapId;
    }

    public int getPortal() {
        return portal;
    }
}
