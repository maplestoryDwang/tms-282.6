package net.swordie.ms.loaders.Etc.Artifact;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.util.container.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArtifactInfo {

    private int extendPeriod;
    private int extendPoint;
    private int changePoint;
    private int resetPoint;
    private int maxSlotLevel;

    // key -> reqLv, value -> GradeInfo(0~4)
    private final Int2IntMap artifacts = new Int2IntOpenHashMap();

    // key -> slotId, value -> <skillLevelPlus, enforceSlotPoint>
    private final Map<Integer, Tuple<Integer, Integer>> slots = new HashMap<>();

    // key -> statId, value -> skillCode (as you commented)
    private final Int2IntMap stats = new Int2IntOpenHashMap();

    private final Map<Integer, CommonMission> commonMissions = new HashMap<>();
    private HuntMission huntMission = new HuntMission();
    private final Map<Integer, SpecialMission> specialMissions = new HashMap<>();

    public ArtifactInfo() {
    }

    public ArtifactInfo(int extendPeriod, int extendPoint, int changePoint, int resetPoint, int maxSlotLevel) {
        this.extendPeriod = extendPeriod;
        this.extendPoint = extendPoint;
        this.changePoint = changePoint;
        this.resetPoint = resetPoint;
        this.maxSlotLevel = maxSlotLevel;
    }

    public int getExtendPeriod() {
        return extendPeriod;
    }

    public void setExtendPeriod(int extendPeriod) {
        this.extendPeriod = extendPeriod;
    }

    public int getExtendPoint() {
        return extendPoint;
    }

    public void setExtendPoint(int extendPoint) {
        this.extendPoint = extendPoint;
    }

    public int getChangePoint() {
        return changePoint;
    }

    public void setChangePoint(int changePoint) {
        this.changePoint = changePoint;
    }

    public int getResetPoint() {
        return resetPoint;
    }

    public void setResetPoint(int resetPoint) {
        this.resetPoint = resetPoint;
    }

    public int getMaxSlotLevel() {
        return maxSlotLevel;
    }

    public void setMaxSlotLevel(int maxSlotLevel) {
        this.maxSlotLevel = maxSlotLevel;
    }

    public Int2IntMap getArtifacts() {
        return artifacts;
    }

    public int getArtifactReqLvByKey(int key) {
        return artifacts.get(key);
    }

    public void putArtifactReqLv(int key, int reqLv) {
        artifacts.put(key, reqLv);
    }

    public Map<Integer, Tuple<Integer, Integer>> getSlots() {
        return slots;
    }

    public Tuple<Integer, Integer> getSlotInfo(int slotId) {
        return slots.get(slotId);
    }

    public void putSlotInfo(int slotId, int skillLevelPlus, int enforceSlotPoint) {
        slots.put(slotId, new Tuple<>(skillLevelPlus, enforceSlotPoint));
    }

    public Int2IntMap getStats() {
        return stats;
    }

    public int getStatSkillCode(int statId) {
        return stats.get(statId);
    }

    public void putStatSkillCode(int statId, int skillCode) {
        stats.put(statId, skillCode);
    }

    public Map<Integer, CommonMission> getCommonMissions() {
        return commonMissions;
    }

    public CommonMission getCommonMission(int id) {
        return commonMissions.get(id);
    }

    public void putCommonMission(int id, CommonMission mission) {
        commonMissions.put(id, mission);
    }

    public HuntMission getHuntMission() {
        return huntMission;
    }

    public void setHuntMission(HuntMission huntMission) {
        this.huntMission = huntMission;
    }

    public Map<Integer, SpecialMission> getSpecialMissions() {
        return specialMissions;
    }

    public SpecialMission getSpecialMission(int id) {
        return specialMissions.get(id);
    }

    public SpecialMission getSpecialMissionByQuestID(int questID, String key) {
        for (SpecialMission specialMission : getSpecialMissions().values()) {
            for (var cond : specialMission.getConditions()) {
                if (cond.getQuestId() == questID) {
                    if (key != null && cond.getKey() != null) {
                        if (key.contains(cond.getKey())) {
                            return specialMission;
                        }
                    } else {
                        return specialMission;
                    }
                }
            }
        }
        return null;
    }

    public SpecialMission getSpecialMissionByItemID(int itemID) {
        for (SpecialMission specialMission : getSpecialMissions().values()) {
            for (var cond : specialMission.getConditions()) {
                if (cond.getQuestId() == itemID) {
                    return specialMission;
                }
            }
        }
        return null;
    }

    public void putSpecialMission(int id, SpecialMission mission) {
        specialMissions.put(id, mission);
    }

    @Override
    public String toString() {
        return "ArtifactInfo{" +
                "extendPeriod=" + extendPeriod +
                ", extendPoint=" + extendPoint +
                ", changePoint=" + changePoint +
                ", resetPoint=" + resetPoint +
                ", maxSlotLevel=" + maxSlotLevel +
                ", artifacts=" + artifacts.size() +
                ", slots=" + slots.size() +
                ", stats=" + stats.size() +
                ", commonMissions=" + commonMissions.size() +
                ", huntMission=" + huntMission +
                ", specialMissions=" + specialMissions.size() +
                '}';
    }

    // ===========================
    // Nested DTOs
    // ===========================

    public static class CommonMission {
        private String name;
        private String desc;
        private String type;
        private int artifactPoint;
        private int artifactExp;
        private int value;

        public CommonMission() {
        }

        public CommonMission(String name, String desc, String type, int artifactPoint, int artifactExp, int value) {
            this.name = name;
            this.desc = desc;
            this.type = type;
            this.artifactPoint = artifactPoint;
            this.artifactExp = artifactExp;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getArtifactPoint() {
            return artifactPoint;
        }

        public void setArtifactPoint(int artifactPoint) {
            this.artifactPoint = artifactPoint;
        }

        public int getArtifactExp() {
            return artifactExp;
        }

        public void setArtifactExp(int artifactExp) {
            this.artifactExp = artifactExp;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class HuntMission {
        private int maxCount;

        // key -> rank, value -> RankReward
        private final Int2ObjectMap<RankReward> rankRewards = new Int2ObjectOpenHashMap<>();

        // key -> missionId, value -> Mission
        private final Int2ObjectMap<Mission> missions = new Int2ObjectOpenHashMap<>();

        public int getMaxCount() {
            return maxCount;
        }

        public void setMaxCount(int maxCount) {
            this.maxCount = maxCount;
        }

        public Int2ObjectMap<RankReward> getRankRewards() {
            return rankRewards;
        }

        public RankReward getRankReward(int rank) {
            return rankRewards.get(rank);
        }

        public void putRankReward(int rank, RankReward reward) {
            rankRewards.put(rank, reward);
        }

        public Int2ObjectMap<Mission> getMissions() {
            return missions;
        }

        public Mission getMission(int missionId) {
            return missions.get(missionId);
        }

        public void putMission(int missionId, Mission mission) {
            missions.put(missionId, mission);
        }

        @Override
        public String toString() {
            return "HuntMission{" +
                    "maxCount=" + maxCount +
                    ", rankRewards=" + rankRewards.size() +
                    ", missions=" + missions.size() +
                    '}';
        }

        public static class RankReward {
            private int artifactPoint;
            private int artifactExp;

            public RankReward() {
            }

            public RankReward(int artifactPoint, int artifactExp) {
                this.artifactPoint = artifactPoint;
                this.artifactExp = artifactExp;
            }

            public int getArtifactPoint() {
                return artifactPoint;
            }

            public void setArtifactPoint(int artifactPoint) {
                this.artifactPoint = artifactPoint;
            }

            public int getArtifactExp() {
                return artifactExp;
            }

            public void setArtifactExp(int artifactExp) {
                this.artifactExp = artifactExp;
            }
        }

        public static class Mission {
            private String name;
            private String desc;
            private int mobID;
            private int rank;

            public Mission() {
            }

            public Mission(String name, String desc, int mobID, int rank) {
                this.name = name;
                this.desc = desc;
                this.mobID = mobID;
                this.rank = rank;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getDesc() {
                return desc;
            }

            public void setDesc(String desc) {
                this.desc = desc;
            }

            public int getMobID() {
                return mobID;
            }

            public void setMobID(int mobID) {
                this.mobID = mobID;
            }

            public int getRank() {
                return rank;
            }

            public void setRank(int rank) {
                this.rank = rank;
            }
        }
    }

    public static class SpecialMission {
        private int index;
        private String name;
        private String desc;
        private String type;
        private int artifactExp;
        private int artifactPoint;

        private List<MissionCond> conditions = new ArrayList<>();

        public SpecialMission() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getArtifactExp() {
            return artifactExp;
        }

        public void setArtifactExp(int artifactExp) {
            this.artifactExp = artifactExp;
        }

        public int getArtifactPoint() {
            return artifactPoint;
        }

        public void setArtifactPoint(int artifactPoint) {
            this.artifactPoint = artifactPoint;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public List<MissionCond> getConditions() {
            return conditions;
        }

        public void setConditions(List<MissionCond> conditions) {
            this.conditions = conditions != null ? conditions : new ArrayList<>();
        }

        public void addCondition(MissionCond cond) {
            if (cond != null) {
                conditions.add(cond);
            }
        }

        public void removeCondition(MissionCond cond) {
            conditions.remove(cond);
        }

        public void clearConditions() {
            conditions.clear();
        }
    }

    public static class MissionCond {
        private int questId;
        private String key;
        private int requiredValue;

        public MissionCond() {}

        public int getQuestId() {
            return questId;
        }

        public void setQuestId(int questId) {
            this.questId = questId;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public int getRequiredValue() {
            return requiredValue;
        }

        public void setRequiredValue(int requiredValue) {
            this.requiredValue = requiredValue;
        }
    }
}