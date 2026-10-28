package net.swordie.ms.loaders.Etc.SetItemInfo;

import java.util.ArrayList;
import java.util.List;

public class SetItemInfo {

    private int setItemID;
    private int completeCount;
    private List<ActiveSkill> activeSkills = new ArrayList<>();
    private String effectLink;
    //First: Set Item Need, Second is a list item of set.
    private List<ItemID> itemIDs = new ArrayList<>();
    private int parts;
    private String setItemName;

    public int getSetItemID() {
        return setItemID;
    }

    public void setSetItemID(int setItemID) {
        this.setItemID = setItemID;
    }

    public int getCompleteCount() {
        return completeCount;
    }

    public void setCompleteCount(int completeCount) {
        this.completeCount = completeCount;
    }

    public String getEffectLink() {
        return effectLink;
    }

    public void setEffectLink(String effectLink) {
        this.effectLink = effectLink;
    }

    public List<ItemID> getItemIDs() {
        return itemIDs;
    }

    public void setItemIDs(List<ItemID> itemIDs) {
        this.itemIDs = itemIDs;
    }

    public void addItemID(ItemID itemID) {
        this.itemIDs.add(itemID);
    }

    public int getParts() {
        return parts;
    }

    public void setParts(int parts) {
        this.parts = parts;
    }

    public String getSetItemName() {
        return setItemName;
    }

    public void setSetItemName(String setItemName) {
        this.setItemName = setItemName;
    }

    public List<ActiveSkill> getActiveSkills() {
        return activeSkills;
    }

    public void setActiveSkills(List<ActiveSkill> activeSkills) {
        this.activeSkills = activeSkills;
    }

    public void addActiveSkill(ActiveSkill activeSkill) {
        this.activeSkills.add(activeSkill);
    }

    public static class ActiveSkill {

        private int effectIndex;

        private int level;

        private int skillID;

        public int getEffectIndex() {
            return effectIndex;
        }

        public void setEffectIndex(int effectIndex) {
            this.effectIndex = effectIndex;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getSkillID() {
            return skillID;
        }

        public void setSkillID(int skillID) {
            this.skillID = skillID;
        }

        @Override
        public String toString() {
            return "ActiveSkill {" +
                    " effectIndex = " + effectIndex +
                    ", level = " + level +
                    ", skillID =" + skillID +
                    '}';
        }
    }

    public static class ItemID {

        private int effectIndex;

        private int itemID;

        public int getEffectIndex() {
            return effectIndex;
        }

        public void setEffectIndex(int effectIndex) {
            this.effectIndex = effectIndex;
        }

        public int getItemID() {
            return itemID;
        }

        public void setItemID(int itemID) {
            this.itemID = itemID;
        }

        @Override
        public String toString() {
            return "ItemID {" +
                    "effectIndex = " + effectIndex +
                    ", itemID = " + itemID +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "SetItemInfo{" +
                "setItemID=" + setItemID +
                ", completeCount=" + completeCount +
                ", activeSkills=" + activeSkills +
                ", effectLink='" + effectLink + '\'' +
                ", itemIDs=" + itemIDs +
                ", parts=" + parts +
                ", setItemName='" + setItemName + '\'' +
                '}';
    }
}
