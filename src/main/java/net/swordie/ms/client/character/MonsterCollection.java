package net.swordie.ms.client.character;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.constants.MonsterCollectionGroup;
import net.swordie.ms.constants.MonsterCollectionRegion;
import net.swordie.ms.constants.MonsterCollectionSession;
import net.swordie.ms.loaders.MonsterCollectionData;
import net.swordie.ms.loaders.containerclasses.MonsterCollectionMobInfo;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class MonsterCollection {

    private static final int EXPLORATION_POSITION_START = 20;

    private int id;
    private Map<Integer, MonsterCollectionRegion> collection = new HashMap<>();
    private Set<Integer> mobs = new HashSet<>();
    private Set<MonsterCollectionReward> monsterCollectionRewards = new HashSet<>();
    private Set<MonsterCollectionExploration> monsterCollectionExplorations = new HashSet<>();

    public static MonsterCollection getMonsterCollectionFromSQLByID(int monsterCollectionID) {
        MonsterCollection monsterCollection = null;
        String query = String.format("SELECT * FROM monster_collection WHERE id = %d", monsterCollectionID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int mobID = rs.getInt("mobid");
                int region = rs.getInt("region");
                int session = rs.getInt("session");
                int position = rs.getInt("position");
                //7 table for this system fck you.
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return monsterCollection;
    }

    public void init(Char chr) {
        for (int mob : getMobs()) {
            addMob(mob);
        }
        for (MonsterCollectionReward mcr : getMonsterCollectionRewards()) {
            int region = mcr.getRegion();
            int session = mcr.getSession();
            int group = mcr.getGroup();
            if (session == -1) {
                collection.get(region).setClaimedReward(true);
            } else if (group == -1) {
                collection.get(region).getMonsterCollectionSessions().get(session).setRewardClaimed(true);
            } else {
                collection.get(region).getMonsterCollectionSessions().get(session)
                        .getMonsterCollectionGroups().get(group).setRewardClaimed(true);
            }
        }
        sendCompleteInfo(chr);
    }

    public boolean hasMob(int templateID) {
        MonsterCollectionMobInfo mcmi = MonsterCollectionData.getMobInfoByID(templateID);
        return mcmi != null && collection.containsKey(mcmi.getRegion()) &&
                collection.get(mcmi.getRegion()).hasMob(templateID, mcmi);
    }

    public void addMob(int templateID) {
        MonsterCollectionMobInfo mcmi = MonsterCollectionData.getMobInfoByID(templateID);
        if (mcmi == null) {
            return;
        }
        if (!collection.containsKey(mcmi.getRegion())) {
            collection.put(mcmi.getRegion(), new MonsterCollectionRegion());
        }
        collection.get(mcmi.getRegion()).addMob(mcmi);
        getMobs().add(templateID);
    }

    public Set<Integer> getMobs() {
        return mobs;
    }

    public void setMobs(Set<Integer> mobs) {
        this.mobs = mobs;
    }

    public void sendCompleteInfo(Char chr) {
        for (Map.Entry<Integer, MonsterCollectionRegion> entry : collection.entrySet()) {
            entry.getValue().sendCompleteInfo(chr, entry.getKey());
        }
    }

    public void addMobAndUpdateClient(int templateID, Char chr) {
        if (!chr.getAccount().getMonsterCollection().hasMob(templateID)) {
            addMob(templateID);
            MonsterCollectionMobInfo mcmi = MonsterCollectionData.getMobInfoByID(templateID);
            if (mcmi == null) {
                return;
            }
            collection.get(mcmi.getRegion()).getMonsterCollectionSessions().get(mcmi.getSession()).sendCompletionInfo(chr,
                    MonsterCollectionData.recordKey(mcmi.getRegion(), mcmi.getSession()), mcmi.getSession());
            chr.write(UserPacket.effect(Effect.effectFromWZ("Effect/BasicEff.img/monsterCollectionGet")));
        }
    }

    /**
     * Restores a saved collection (MonsterCollectionStore): every saved mob id is kept, also the ones the loaded
     * definition does not know (so a missing or older data file loses nothing on the next save); the known ones are
     * placed into their region/session/group, then the claimed rewards are marked and the explorations added.
     */
    public void restore(Collection<Integer> savedMobs, Collection<MonsterCollectionReward> claimed,
                        Collection<MonsterCollectionExploration> explorations) {
        for (int mob : savedMobs) {
            addMob(mob);
            getMobs().add(mob);
        }
        for (MonsterCollectionReward mcr : claimed) {
            MonsterCollectionRegion region = getRegion(mcr.getRegion());
            if (region == null) {
                continue;
            }
            if (mcr.getSession() == -1) {
                region.setClaimedReward(true);
                continue;
            }
            MonsterCollectionSession session = getSession(mcr.getRegion(), mcr.getSession());
            if (session == null) {
                continue;
            }
            if (mcr.getGroup() == -1) {
                session.setRewardClaimed(true);
            } else if (getGroup(mcr.getRegion(), mcr.getSession(), mcr.getGroup()) != null) {
                getGroup(mcr.getRegion(), mcr.getSession(), mcr.getGroup()).setRewardClaimed(true);
            }
        }
        getMonsterCollectionExplorations().addAll(explorations);
    }

    /** The claimed rewards as (region, session, group) rows, -1 for "the whole session" / "the whole region". */
    public List<MonsterCollectionReward> claimedRewards() {
        List<MonsterCollectionReward> claimed = new ArrayList<>();
        for (Map.Entry<Integer, MonsterCollectionRegion> r : new TreeMap<>(collection).entrySet()) {
            if (r.getValue().isClaimedReward()) {
                claimed.add(MonsterCollectionReward.of(r.getKey(), -1, -1));
            }
            for (Map.Entry<Integer, MonsterCollectionSession> s : new TreeMap<>(r.getValue().getMonsterCollectionSessions()).entrySet()) {
                if (s.getValue().isRewardClaimed()) {
                    claimed.add(MonsterCollectionReward.of(r.getKey(), s.getKey(), -1));
                }
                for (Map.Entry<Integer, MonsterCollectionGroup> g : new TreeMap<>(s.getValue().getMonsterCollectionGroups()).entrySet()) {
                    if (g.getValue().isRewardClaimed()) {
                        claimed.add(MonsterCollectionReward.of(r.getKey(), s.getKey(), g.getKey()));
                    }
                }
            }
        }
        return claimed;
    }

    /**
     * Every collection record the client keeps (key -> property value), in key order: one per session with a
     * registered mob (the recordKey of MonsterCollectionData, value {@link MonsterCollectionSession#completionValue})
     * and one per running exploration (its position, value "mobKey=...;end=...;state=1"), i.e. what 265 sends one by
     * one after login (init / sendCompleteInfo) and what TMS 282.3 reads in CharacterData block [47].
     */
    public Map<Integer, String> records() {
        Map<Integer, String> records = new TreeMap<>();
        for (Map.Entry<Integer, MonsterCollectionRegion> r : collection.entrySet()) {
            for (Map.Entry<Integer, MonsterCollectionSession> s : r.getValue().getMonsterCollectionSessions().entrySet()) {
                records.put(MonsterCollectionData.recordKey(r.getKey(), s.getKey()), s.getValue().completionValue(s.getKey()));
            }
        }
        for (MonsterCollectionExploration mce : getMonsterCollectionExplorations()) {
            if (mce.getEndDate() != null && mce.getMonsterKey() != null) {
                records.put(mce.getPosition(), mce.getValue(true));
            }
        }
        return records;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Set<MonsterCollectionReward> getMonsterCollectionRewards() {
        return monsterCollectionRewards;
    }

    public void setMonsterCollectionRewards(Set<MonsterCollectionReward> monsterCollectionRewards) {
        this.monsterCollectionRewards = monsterCollectionRewards;
    }

    public MonsterCollectionRegion getRegion(int region) {
        return collection.getOrDefault(region, null);
    }

    public MonsterCollectionSession getSession(int region, int session) {
        if (getRegion(region) != null) {
            return getRegion(region).getMonsterCollectionSessions().getOrDefault(session, null);
        }
        return null;
    }

    public MonsterCollectionGroup getGroup(int region, int session, int group) {
        if (getSession(region, session) != null) {
            return getSession(region, session).getMonsterCollectionGroups().getOrDefault(group, null);
        }
        return null;
    }

    public boolean isComplete(int region, int session, int groupID) {
        MonsterCollectionGroup group = getGroup(region, session, groupID);
        if (groupID == -1) {
            boolean complete = true;
            for (Map.Entry<Integer, MonsterCollectionGroup> entry : getSession(region, session).getMonsterCollectionGroups().entrySet()) {
                complete &= isComplete(region, session, entry.getKey());
            }
            return complete;
        }
        return group != null && group.getMobs().size() >= MonsterCollectionData.getRequiredMobs(region, session, groupID);
    }

    public MonsterCollectionExploration getExploration(int region, int session, int group) {
        int collectionKey = region * 10000 + session * 100 + group;
        return getMonsterCollectionExplorations().stream().filter(mce -> mce.getCollectionKey() == collectionKey)
                .findAny().orElse(null);
    }

    public Set<MonsterCollectionExploration> getMonsterCollectionExplorations() {
        return monsterCollectionExplorations;
    }

    public void setMonsterCollectionExplorations(Set<MonsterCollectionExploration> monsterCollectionExplorations) {
        this.monsterCollectionExplorations = monsterCollectionExplorations;
    }

    public MonsterCollectionExploration createExploration(int region, int session, int group) {
        int collectionKey = region * 10000 + session * 100 + group;
        int minutes = MonsterCollectionData.getExplorationMinutes(region, session, group);
        FileTime ft = FileTime.fromDate(LocalDateTime.now().plusMinutes(minutes));
        MonsterCollectionExploration mce = new MonsterCollectionExploration(collectionKey, ft);
        mce.setMonsterKey(String.format("%d:%d:%d:0", region, session, group));
        mce.setPosition(getFirstOpenSlot());
        return mce;
    }

    private int getFirstOpenSlot() {
        List<MonsterCollectionExploration> mces = getMonsterCollectionExplorations().stream()
                .sorted(Comparator.comparingInt(MonsterCollectionExploration::getPosition))
                .collect(Collectors.toList());
        int i;
        for (i = 0; i < mces.size(); i++) {
            MonsterCollectionExploration mce = mces.get(i);
            int pos = i + EXPLORATION_POSITION_START;
            if (mce.getPosition() != pos) {
                return pos;
            }
        }
        return EXPLORATION_POSITION_START + i;
    }

    public void addExploration(MonsterCollectionExploration mce) {
        getMonsterCollectionExplorations().add(mce);
    }

    public void removeExploration(MonsterCollectionExploration mce) {
        getMonsterCollectionExplorations().remove(mce);
    }

    private int getTotalAmountOfSlots() {
        int num;
        int mobs = getMobs().size();
        if (mobs >= 600) {
            num = 5;
        } else if (mobs >= 300) {
            num = 4;
        } else if (mobs >= 150) {
            num = 3;
        } else {
            num = 2;
        }
        return num;
    }

    public int getOpenExplorationSlots() {
        return getTotalAmountOfSlots() - getMonsterCollectionExplorations().size();
    }
}
