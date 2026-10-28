package net.swordie.ms.loaders.Etc.Achievement;

import java.util.ArrayList;
import java.util.List;

public class AchievementInfo {

    private int id;
    private String mainCategory = "";
    private String subCategory = "";
    private String name = "";
    private String desc = "";
    private String difficulty = "";
    private int score = 0;
    private int reqAchievementID = 0;
    private List<MissionInfo> missions = new ArrayList<>();

    public AchievementInfo(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMainCategory() {
        return mainCategory;
    }

    public void setMainCategory(String mainCategory) {
        this.mainCategory = mainCategory;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
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

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getReqAchievementID() {
        return reqAchievementID;
    }

    public void setReqAchievementID(int reqAchievementID) {
        this.reqAchievementID = reqAchievementID;
    }

    public List<MissionInfo> getMissions() {
        return missions;
    }

    public void setMissions(List<MissionInfo> missions) {
        this.missions = missions;
    }

    public static class MissionInfo {
        private int id;
        private String name = "";
        private String key = "";
        private long value = 0L;
        private List<Integer> jobCodes = new ArrayList<>();
        private List<Integer> mobIDs = new ArrayList<>();
        private List<Integer> questIDs = new ArrayList<>();
        private List<Long> itemIDs = new ArrayList<>();
        private long fieldID = 0L;

        public MissionInfo(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public long getValue() {
            return value;
        }

        public void setValue(long value) {
            this.value = value;
        }

        public List<Integer> getJobCodes() {
            return jobCodes;
        }

        public void setJobCodes(List<Integer> jobCodes) {
            this.jobCodes = jobCodes;
        }

        public List<Integer> getMobIDs() {
            return mobIDs;
        }

        public void setMobIDs(List<Integer> mobIDs) {
            this.mobIDs = mobIDs;
        }

        public List<Integer> getQuestIDs() {
            return questIDs;
        }

        public void setQuestIDs(List<Integer> questIDs) {
            this.questIDs = questIDs;
        }

        public long getFieldID() {
            return fieldID;
        }

        public void setFieldID(long fieldID) {
            this.fieldID = fieldID;
        }

        public List<Long> getItemIDs() {
            return itemIDs;
        }

        public void setItemIDs(List<Long> itemIDs) {
            this.itemIDs = itemIDs;
        }
    }
}
