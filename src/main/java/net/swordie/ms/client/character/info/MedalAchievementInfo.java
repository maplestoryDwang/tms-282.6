package net.swordie.ms.client.character.info;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.*;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Phu
 */
public class MedalAchievementInfo {
    private long id;
    private int charId;
    private int questID;
    private int itemID;
    private FileTime completedTime;

    public static Set<MedalAchievementInfo> getMedalAchievementInfosFromSQLByCharID(int charID) {
        Set<MedalAchievementInfo> medalAchievementInfos = new HashSet<>();
        String query = "SELECT * FROM medals WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MedalAchievementInfo medalAchievementInfo = new MedalAchievementInfo();
                    medalAchievementInfo.setId(rs.getLong("id"));
                    medalAchievementInfo.setCharId(rs.getInt("charid"));
                    medalAchievementInfo.setQuestID(rs.getInt("questid"));
                    medalAchievementInfo.setItemID(rs.getInt("itemid"));
                    medalAchievementInfo.setCompletedTime(DatabaseManager.getFileTimeFromString(rs.getString("completedtime")));
                    medalAchievementInfos.add(medalAchievementInfo);
                }
            }
        } catch (SQLException | ParseException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return medalAchievementInfos;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `medals` (" +
                    "`charid`, " +
                    "`questid`, " +
                    "`itemID`, " +
                    "`completedtime` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getQuestID()) +
                    String.format("%d, ", getItemID()) +
                    String.format("'%s' ", DatabaseManager.convertToDateTimeSQL(getCompletedTime())) +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE medals SET " +
            String.format("charid = %d, ", getCharId()) +
            String.format("questid = %d, ", getQuestID()) +
            String.format("itemID = %d, ", getItemID()) +
            String.format("completedtime = '%s' ", DatabaseManager.convertToDateTimeSQL(getCompletedTime())) +
            String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteMedalAchievementInfoFromSQL() {
        String query = "DELETE FROM `medals` WHERE " +
        String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public MedalAchievementInfo() {
    }

    public MedalAchievementInfo(int charId, int questID, int itemID, FileTime completedTime) {
        this.charId = charId;
        this.questID = questID;
        this.itemID = itemID;
        this.completedTime = completedTime;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getQuestID());
        getCompletedTime().encode(outPacket);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }

    public int getQuestID() {
        return questID;
    }

    public void setQuestID(int questID) {
        this.questID = questID;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public FileTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(FileTime completedTime) {
        this.completedTime = completedTime;
    }


}
