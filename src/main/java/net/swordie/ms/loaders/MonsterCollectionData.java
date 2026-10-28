package net.swordie.ms.loaders;

import net.swordie.ms.constants.MonsterCollectionGroup;
import net.swordie.ms.constants.MonsterCollectionRegion;
import net.swordie.ms.constants.MonsterCollectionSession;
import net.swordie.ms.loaders.containerclasses.MonsterCollectionMobInfo;
import net.swordie.ms.loaders.overlay.DataOverlay;
import net.swordie.ms.loaders.overlay.DataOverlays;
import net.swordie.ms.util.XMLApi;
import net.swordie.ms.util.container.Triple;
import net.swordie.ms.util.container.Tuple;
import org.w3c.dom.Node;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonsterCollectionData {

    private static Map<Integer, MonsterCollectionRegion> monsterCollectionInfo = new HashMap<>();
    private static Map<Integer, Triple<Integer, Integer, Integer>> monsterInfo = new HashMap<>();
    // reward from group -> hours for exploration
    private static Map<Integer, Integer> rewardToMinutes = new HashMap<>();
    // reward from group -> (reward id, chance)
    private static Map<Integer, Tuple<Integer, Integer>> explorationRewards = new HashMap<>();
    /** TMS 282.3 overlay only: (region * 10000 + session) -> the session's mobCollection.img recordID. */
    private static Map<Integer, Integer> recordIds = new HashMap<>();
    /** TMS 282.3 overlay only: (region * 10000 + session * 100 + group) -> exploraionCycle minutes. */
    private static Map<Integer, Integer> explorationCycles = new HashMap<>();
    /** DataOverlays generation the tables were built for; -1 = never. */
    private static int loadedGeneration = -1;

    static {
        rewardToMinutes.put(2434929, 30);
        rewardToMinutes.put(2434930, 60 * 3);
        rewardToMinutes.put(2434931, 60 * 12);
        rewardToMinutes.put(2434932, 60 * 24);
        rewardToMinutes.put(2434958, 60 * 3);
        rewardToMinutes.put(2434959, 60 * 3);
    }

    public static void loadFromSQL() {
        //Not Handle now
//        long start = System.currentTimeMillis();
//        Session session = DatabaseManager.getSession();
//        Transaction t = session.beginTransaction();
//        Query q = session.createQuery("from MonsterCollectionSessionRewardInfo");
//        List<MonsterCollectionSessionRewardInfo> sessionRewardInfos = new ArrayList<>(q.list());
//        q = session.createQuery("from MonsterCollectionGroupRewardInfo");
//        List<MonsterCollectionGroupRewardInfo> groupRewardInfos = new ArrayList<>(q.list());
//        q = session.createQuery("from MonsterCollectionMobInfo");
//        List<MonsterCollectionMobInfo> mobInfos = new ArrayList<>(q.list());
//        t.commit();
//        session.close();
//        mobInfos.forEach(mi -> {
//            put(mi);
//            monsterInfo.put(mi.getMobID(), new Triple(mi.getRegion(), mi.getSession(), mi.getPosition()));
//        });
//        for (MonsterCollectionSessionRewardInfo mcsri : sessionRewardInfos) {
//            monsterCollectionInfo.get(mcsri.getRegion()).getMonsterCollectionSessions().get(mcsri.getSession())
//                    .setReward(mcsri.getRewardID());
//            monsterCollectionInfo.get(mcsri.getRegion()).getMonsterCollectionSessions().get(mcsri.getSession())
//                    .setRewardQuantity(mcsri.getQuantity());
//        }
//        for (MonsterCollectionGroupRewardInfo mcgri : groupRewardInfos) {
//            monsterCollectionInfo.get(mcgri.getRegion()).getMonsterCollectionSessions().get(mcgri.getSession())
//                    .getMonsterCollectionGroups().get(mcgri.getGroupID())
//                    .setReward(mcgri.getRewardID());
//            monsterCollectionInfo.get(mcgri.getRegion()).getMonsterCollectionSessions().get(mcgri.getSession())
//                    .getMonsterCollectionGroups().get(mcgri.getGroupID())
//                    .setRewardQuantity(mcgri.getQuantity());
//        }
    }

    /**
     * TMS 282.3: builds the tables from the overlay's {@code Etc.wz/mobCollection.img.xml} (the 282 client's image,
     * exported by data282-import MobCollectionCommand) the first time a lookup runs under an active overlay, and again
     * after the overlay changes. GMS 265 (base data only) keeps the old state: no collection definition is loaded
     * (the SQL loader above has never been wired), so every lookup answers "not a collection mob" as before.
     */
    private static synchronized void ensureLoaded() {
        int generation = DataOverlays.generation();
        if (generation == loadedGeneration) {
            return;
        }
        loadedGeneration = generation;
        DataOverlay overlay = DataOverlays.current();
        if (!overlay.isActive()) {
            return;
        }
        List<File> layers = overlay.wzLayers("Etc.wz/mobCollection.img.xml");
        File file = layers.isEmpty() ? null : layers.getLast();
        if (file == null || !file.isFile() || !file.getPath().replace('\\', '/').contains(DataOverlays.TMS282_WZ_DIR)) {
            System.out.println("[MonsterCollectionData] no overlay mobCollection.img.xml; collection disabled");
            return;
        }
        clear();
        loadImage(file);
        System.out.printf("[MonsterCollectionData] %s: %d collection mobs, %d sessions%n", file, monsterInfo.size(),
                recordIds.size());
    }

    private static void clear() {
        monsterCollectionInfo = new HashMap<>();
        monsterInfo = new HashMap<>();
        recordIds = new HashMap<>();
        explorationCycles = new HashMap<>();
    }

    /**
     * mobCollection.img: region/(info, session...)/(info recordID rewardID, group/g/(recordID rewardID
     * exploraionCycle mob/pos/(type id tooltip))). Non-numeric region nodes (sounds, icons) are skipped; mob id 0 is
     * an empty slot; a mob listed twice keeps its first position.
     */
    static void loadImage(File file) {
        Node root = XMLApi.getAllChildren(XMLApi.getRoot(file)).getFirst();
        for (Node regionNode : XMLApi.getAllChildren(root)) {
            Integer region = number(XMLApi.getNamedAttribute(regionNode, "name"));
            if (region == null) {
                continue;
            }
            for (Node sessionNode : XMLApi.getAllChildren(regionNode)) {
                Integer session = number(XMLApi.getNamedAttribute(sessionNode, "name"));
                if (session == null) {
                    continue;
                }
                Node info = child(sessionNode, "info");
                Node groups = child(sessionNode, "group");
                if (groups == null) {
                    continue;
                }
                int recordId = info == null ? 0 : intChild(info, "recordID", 0);
                if (recordId > 0) {
                    recordIds.put(region * 10000 + session, recordId);
                }
                for (Node groupNode : XMLApi.getAllChildren(groups)) {
                    Integer group = number(XMLApi.getNamedAttribute(groupNode, "name"));
                    Node mobs = child(groupNode, "mob");
                    if (group == null || mobs == null) {
                        continue;
                    }
                    for (Node mobNode : XMLApi.getAllChildren(mobs)) {
                        Integer position = number(XMLApi.getNamedAttribute(mobNode, "name"));
                        int mobId = intChild(mobNode, "id", 0);
                        if (position == null || mobId <= 0 || monsterInfo.containsKey(mobId)) {
                            continue;
                        }
                        monsterInfo.put(mobId, new Triple<>(region, session, position));
                        put(new MonsterCollectionMobInfo(mobId, region, session, position));
                    }
                    MonsterCollectionGroup mcg = groupOrNull(region, session, group);
                    if (mcg != null) {
                        mcg.setReward(intChild(groupNode, "rewardID", 0));
                        mcg.setRewardQuantity(1);
                    }
                    int cycle = intChild(groupNode, "exploraionCycle", 0);
                    if (cycle > 0) {
                        explorationCycles.put(region * 10000 + session * 100 + group, cycle);
                    }
                }
                MonsterCollectionRegion mcr = monsterCollectionInfo.get(region);
                MonsterCollectionSession mcs = mcr == null ? null : mcr.getMonsterCollectionSessions().get(session);
                if (mcs != null && info != null) {
                    mcs.setReward(intChild(info, "rewardID", 0));
                    mcs.setRewardQuantity(1);
                }
            }
        }
    }

    private static MonsterCollectionGroup groupOrNull(int region, int session, int group) {
        MonsterCollectionRegion mcr = monsterCollectionInfo.get(region);
        MonsterCollectionSession mcs = mcr == null ? null : mcr.getMonsterCollectionSessions().get(session);
        return mcs == null ? null : mcs.getMonsterCollectionGroups().get(group);
    }

    private static Node child(Node parent, String name) {
        for (Node c : XMLApi.getAllChildren(parent)) {
            if (name.equals(XMLApi.getNamedAttribute(c, "name"))) {
                return c;
            }
        }
        return null;
    }

    private static Integer number(String name) {
        if (name == null || name.isEmpty() || !name.chars().allMatch(Character::isDigit)) {
            return null;
        }
        return Integer.parseInt(name);
    }

    private static int intChild(Node parent, String name, int dflt) {
        Node c = child(parent, name);
        Integer value = c == null ? null : number(XMLApi.getNamedAttribute(c, "value"));
        return value == null ? dflt : value;
    }

    /**
     * The collection record key of a session: TMS 282.3 uses the session's mobCollection.img recordID (e.g. region 0
     * session 0 = 101); without that data it is the 265 key 100000 + region * 100 + session (unchanged).
     */
    public static int recordKey(int region, int session) {
        ensureLoaded();
        Integer recordId = recordIds.get(region * 10000 + session);
        return recordId != null ? recordId : 100000 + region * 100 + session;
    }

    /** Number of collection mobs known (0 when no definition is loaded, as on GMS 265). */
    public static int size() {
        ensureLoaded();
        return monsterInfo.size();
    }

    public static void put(MonsterCollectionMobInfo mcmi) {
        if (!monsterCollectionInfo.containsKey(mcmi.getRegion())) {
            monsterCollectionInfo.put(mcmi.getRegion(), new MonsterCollectionRegion());
        }
        monsterCollectionInfo.get(mcmi.getRegion()).addMob(mcmi);
    }


    public static MonsterCollectionMobInfo getMobInfoByID(int templateID) {
        ensureLoaded();
        Triple<Integer, Integer, Integer> info = monsterInfo.get(templateID);
        if (info == null) {
            return null;
        }
        return new MonsterCollectionMobInfo(templateID, info.getLeft(), info.getMiddle(), info.getRight());
    }

    public static int getRequiredMobs(int region, int session, int group) {
        ensureLoaded();
        return monsterCollectionInfo.get(region).getMonsterCollectionSessions().get(session)
                .getMonsterCollectionGroups().get(group).getMobs().size();
    }

    private static MonsterCollectionSession getSession(int region, int session) {
        return monsterCollectionInfo.get(region).getMonsterCollectionSessions().get(session);
    }

    private static MonsterCollectionGroup getGroup(int region, int session, int group) {
        return getSession(region, session).getMonsterCollectionGroups().get(group);
    }

    public static Tuple<Integer, Integer> getReward(int region, int session, int group) {
        ensureLoaded();
        if (group == -1) {
            MonsterCollectionSession mcs = getSession(region, session);
            return new Tuple<>(mcs.getReward(), mcs.getRewardQuantity());
        }
        MonsterCollectionGroup mcg = getGroup(region, session, group);
        return new Tuple<>(mcg.getReward(), mcg.getRewardQuantity());
    }

    public static int getExplorationMinutes(int region, int session, int group) {
        ensureLoaded();
        Integer cycle = explorationCycles.get(region * 10000 + session * 100 + group);
        if (cycle != null) {
            return cycle; // TMS 282.3: the group's exploraionCycle
        }
        int reward = getGroup(region, session, group).getReward();
        return rewardToMinutes.get(reward);
    }
}
