package net.swordie.ms.client.character.reward;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.reward.RewardItemType;
import net.swordie.ms.enums.reward.RewardSystemType;
import net.swordie.ms.util.FileTime;

import java.util.ArrayList;
import java.util.List;

public class RewardSystem {

    private RewardSystemType type;
    private int totalItem;
    private List<RewardInfo> rewards = new ArrayList<>();

    public RewardSystem(RewardSystemType type) {
        this.type = type;
    }

    public RewardSystem(RewardSystemType type, List<RewardInfo> rewards) {
        this.type = type; //Open UI
        this.totalItem = rewards.size();
        this.rewards = rewards;
    }

    public RewardSystemType getType() {
        return type;
    }

    public void setType(RewardSystemType type) {
        this.type = type;
    }

    public int getTotalItem() {
        return totalItem;
    }

    public void setTotalItem(int totalItem) {
        this.totalItem = totalItem;
    }

    public List<RewardInfo> getRewards() {
        return rewards;
    }

    public void setRewards(List<RewardInfo> rewards) {
        this.rewards = rewards;
    }

    public void addReward(RewardInfo rewardInfo) {
        getRewards().add(rewardInfo);
        rewardInfo.saveToSQL();
    }

    public boolean checkRewardByValue(RewardItemType type, int value) {
        for (RewardInfo rewardInfo : getRewards()) {
            switch (type) {
                case Item:
                    if (rewardInfo.getItemID() == value) {
                        return true;
                    }
                    break;
                case MaplePoint:
                    if (rewardInfo.getMaplePoint() == value) {
                        return true;
                    }
                    break;
                case Meso:
                    if (rewardInfo.getMeso() == value) {
                        return true;
                    }
                    break;
                case Exp:
                    if (rewardInfo.getExp() == value) {
                        return true;
                    }
                    break;
                default:
                    return false;
            }
        }
        return false;
    }

    public RewardInfo getRewardByValue(RewardItemType type, long value) {
        for (RewardInfo rewardInfo : getRewards()) {
            switch (type) {
                case Item:
                    if (rewardInfo.getItemID() == value) {
                        return rewardInfo;
                    }
                    break;
                case MaplePoint:
                    if (rewardInfo.getMaplePoint() == value) {
                        return rewardInfo;
                    }
                    break;
                case Meso:
                    if (rewardInfo.getMeso() == value) {
                        return rewardInfo;
                    }
                    break;
                case Exp:
                    if (rewardInfo.getExp() == value) {
                        return rewardInfo;
                    }
                    break;
                default:
                    return null;
            }
        }
        return null;
    }

    public void removeReward(RewardInfo rewardInfo) {
        getRewards().removeIf(x -> x.getId() == rewardInfo.getId());
        rewardInfo.deleteFromSQL();
    }

    @Override
    public String toString() {
        return "RewardSystem {" +
                "type=" + type +
                ", totalItem=" + totalItem +
                ", rewards=" + rewards +
                '}';
    }

    public void encode(OutPacket outPacket) {
        int type = 9;
        outPacket.encodeFT(FileTime.currentTime());
        outPacket.encodeInt(rewards.size());
        for (int i = 0; i < rewards.size(); i++) {
            RewardInfo rewardInfo = getRewards().get(i);
            outPacket.encodeInt(rewardInfo.getEnableShowItem());

            if ((type & 1) != 0) {
                outPacket.encodeFT(rewardInfo.getStartTime());
                outPacket.encodeFT(rewardInfo.getEndTime());
                outPacket.encodeFT(rewardInfo.getStartTime());
                outPacket.encodeFT(rewardInfo.getEndTime());
            }
            if ((type & 2) != 0) {
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeInt(0);
                outPacket.encodeString("");
                outPacket.encodeString("");
                outPacket.encodeString("");
            }
            outPacket.encodeInt(rewardInfo.getRewardItemType().getVal());
            outPacket.encodeInt(rewardInfo.getItemID());
            outPacket.encodeInt(rewardInfo.getQuantity());
            outPacket.encodeInt(0);             // maplepoint?
            outPacket.encodeLong(0);            // meso?
            outPacket.encodeInt(0);             // exp?
            outPacket.encodeInt(rewardInfo.getMaplePoint());
            outPacket.encodeLong(rewardInfo.getMeso());
            outPacket.encodeLong(rewardInfo.getExp());
            outPacket.encodeInt(0);             // ??
            outPacket.encodeInt(0);             // ??
            outPacket.encodeString("");         // ??
            outPacket.encodeString("");         // ??
            outPacket.encodeString("");         // ??

            if ((type & 4) != 0) {
                outPacket.encodeString("");         // ??
            }

            if ((type & 8) != 0) {
                outPacket.encodeString(rewardInfo.getDescription());
            }

            outPacket.encodeInt(0);             // ??

            outPacket.encodeInt(0);             // ??

            if (rewardInfo.getRewardItemType().getVal() == RewardItemType.Equip.getVal()) {
                rewardInfo.getItem().encode(outPacket);
            }
        }
    }
}
