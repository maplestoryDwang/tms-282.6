package net.swordie.ms.client.character.reward;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.reward.RewardItemType;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RewardInfo {

    private int id;
    private int charID;
    private String charName;
    private RewardItemType rewardItemType;
    private int itemID;
    private int quantity;
    private int maplePoint;
    private long meso;
    private long exp;
    private String description;
    private int enableShowItem = 1;
    private FileTime startTime;
    private FileTime endTime;
    private Item item;

    public static List<RewardInfo> getDataFromSQL(int charID) {
        List<RewardInfo> rewardInfoList = new ArrayList<>();
        String query = "SELECT * FROM rewardinfo WHERE charid = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RewardInfo rewardInfo = new RewardInfo();
                    rewardInfo.setId(rs.getInt("id"));
                    rewardInfo.setCharID(charID);
                    rewardInfo.setCharName(rs.getString("charname"));
                    rewardInfo.setRewardItemType(RewardItemType.getByVal(rs.getByte("rewardItemType")));
                    rewardInfo.setItemID(rs.getInt("itemID"));
                    rewardInfo.setQuantity(rs.getInt("quantity"));
                    rewardInfo.setMaplePoint(rs.getInt("maplePoint"));
                    rewardInfo.setMeso(rs.getLong("meso"));
                    rewardInfo.setExp(rs.getLong("exp"));
                    rewardInfo.setDescription(rs.getString("description"));
                    rewardInfo.setStartTime(DatabaseManager.getFileTimeFromString(rs.getString("starttime")));
                    rewardInfo.setEndTime(DatabaseManager.getFileTimeFromString(rs.getString("endtime")));

                    rewardInfoList.add(rewardInfo);
                }
            }
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return rewardInfoList;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String sql = "INSERT INTO rewardinfo (" +
                    "charid, " +
                    "charname, " +
                    "rewardItemType, " +
                    "itemID, " +
                    "quantity, " +
                    "maplePoint, " +
                    "meso, " +
                    "exp, " +
                    "description, " +
                    "starttime, " +
                    "endtime" +
                    ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection con = DatabaseManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                ps.setInt(i++, getCharID());
                ps.setString(i++, getCharName());
                ps.setInt(i++, getRewardItemType().getVal());
                ps.setInt(i++, getItemID());
                ps.setInt(i++, getQuantity());
                ps.setInt(i++, getMaplePoint());
                ps.setLong(i++, getMeso());
                ps.setLong(i++, getExp());
                ps.setString(i++, getDescription());
                ps.setTimestamp(i++, getStartTime() != null ? new Timestamp(getStartTime().toMillis()) : null);
                ps.setTimestamp(i++, getEndTime() != null ? new Timestamp(getEndTime().toMillis()) : null);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        setId(rs.getInt(1));
                    }
                }
            } catch (Exception e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            }
        } else {
            String sql = "UPDATE rewardinfo SET " +
                    "charname = ?, " +
                    "rewardItemType = ?, " +
                    "itemID = ?, " +
                    "quantity = ?, " +
                    "maplePoint = ?, " +
                    "meso = ?, " +
                    "exp = ?, " +
                    "description = ?, " +
                    "starttime = ?, " +
                    "endtime = ? " +
                    "WHERE id = ?";
            try (Connection con = DatabaseManager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                int i = 1;
                ps.setString(i++, getCharName());
                ps.setInt(i++, getRewardItemType().getVal());
                ps.setInt(i++, getItemID());
                ps.setInt(i++, getQuantity());
                ps.setInt(i++, getMaplePoint());
                ps.setLong(i++, getMeso());
                ps.setLong(i++, getExp());
                ps.setString(i++, getDescription());
                ps.setTimestamp(i++, getStartTime() != null ? new Timestamp(getStartTime().toMillis()) : null);
                ps.setTimestamp(i++, getEndTime() != null ? new Timestamp(getEndTime().toMillis()) : null);
                ps.setInt(i++, getId());
                ps.executeUpdate();
            } catch (Exception e) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
            }
        }
    }

    public void deleteFromSQL() {
        String query = "DELETE FROM `rewardinfo` WHERE " + String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
    }

    public RewardInfo() {

    }

    public RewardInfo(Char chr, RewardItemType rewardItemType, int itemID, int quantity, int maplePoint, long meso, long exp, String description, FileTime startTime, FileTime endTime) {
        this.charID = chr.getId();
        this.charName = chr.getName();
        this.rewardItemType = rewardItemType;
        this.itemID = itemID;
        this.quantity = quantity;
        this.maplePoint = maplePoint;
        this.meso = meso;
        this.exp = exp;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public RewardInfo(Char chr, RewardItemType rewardItemType, int itemID, int quantity, String description, FileTime startTime, FileTime endTime) {
        this.charID = chr.getId();
        this.charName = chr.getName();
        this.rewardItemType = rewardItemType;
        this.itemID = itemID;
        this.quantity = quantity;
        this.maplePoint = 0;
        this.meso = 0;
        this.exp = 0;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public RewardInfo(Char chr, RewardItemType rewardItemType, long meso, String description, FileTime startTime, FileTime endTime) {
        this.charID = chr.getId();
        this.charName = chr.getName();
        this.rewardItemType = rewardItemType;
        this.itemID = 0;
        this.quantity = 0;
        this.maplePoint = 0;
        this.meso = meso;
        this.exp = 0;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    public RewardInfo(Char chr, RewardItemType rewardItemType, long exp, String description, FileTime startTime, FileTime endTime, boolean isExp) {
        this.charID = chr.getId();
        this.charName = chr.getName();
        this.rewardItemType = rewardItemType;
        this.itemID = 0;
        this.quantity = 0;
        this.maplePoint = 0;
        this.meso = 0;
        this.exp = exp;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public String getCharName() {
        return charName;
    }

    public void setCharName(String charName) {
        this.charName = charName;
    }

    public int getEnableShowItem() {
        return enableShowItem;
    }

    public void setEnableShowItem(int enableShowItem) {
        this.enableShowItem = enableShowItem;
    }

    public RewardItemType getRewardItemType() {
        return rewardItemType;
    }

    public void setRewardItemType(RewardItemType rewardItemType) {
        this.rewardItemType = rewardItemType;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getMaplePoint() {
        return maplePoint;
    }

    public void setMaplePoint(int maplePoint) {
        this.maplePoint = maplePoint;
    }

    public long getMeso() {
        return meso;
    }

    public void setMeso(long meso) {
        this.meso = meso;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FileTime getStartTime() {
        return startTime;
    }

    public void setStartTime(FileTime startTime) {
        this.startTime = startTime;
    }

    public FileTime getEndTime() {
        return endTime;
    }

    public void setEndTime(FileTime endTime) {
        this.endTime = endTime;
    }

    public Item getItem() {
        if (item == null) {
            item = ItemData.getItemDeepCopy(getItemID());
        }
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public static boolean compare2Reward(RewardInfo rewardInfo1, RewardInfo rewardInfo2) {
        return rewardInfo1.getRewardItemType() == rewardInfo2.getRewardItemType()
                && rewardInfo1.getItemID() == rewardInfo2.getItemID()
                && rewardInfo1.getQuantity() == rewardInfo2.getQuantity()
                && rewardInfo1.getMeso() == rewardInfo2.getMeso()
                && rewardInfo1.getMaplePoint() == rewardInfo2.getMaplePoint()
                && rewardInfo1.getExp() == rewardInfo2.getExp()
                && rewardInfo1.getDescription().equals(rewardInfo2.getDescription());
    }

    @Override
    public String toString() {
        return "RewardInfo {" +
                " rewardItemType=" + rewardItemType +
                ", itemID=" + itemID +
                ", quantity=" + quantity +
                ", maplePoint=" + maplePoint +
                ", meso=" + meso +
                ", exp=" + exp +
                ", description='" + description + '\'' +
                ", startTime='" + startTime.toString() + '\'' +
                ", endTime='" + endTime.toString() + '\'' +
                " }";
    }
}
