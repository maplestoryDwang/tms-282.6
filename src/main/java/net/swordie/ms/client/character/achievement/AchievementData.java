package net.swordie.ms.client.character.achievement;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class AchievementData {

    private int id;
    private int accID;
    private int infoID; // achievement id
    private byte missionID; // sub-mission = -1 là xong all
    private byte status; // (0 = not started, 1 = started, 2 = completed
    private FileTime unlockTime = FileTime.MAX_TIME();
    private String msg; // questEx

    public AchievementData() {
    }

    public AchievementData(int id, int infoID, byte missionID, byte status, FileTime unlockTime, String msg) {
        this.id = id;
        this.infoID = infoID;
        this.missionID = missionID;
        this.status = status;
        this.unlockTime = unlockTime;
        this.msg = msg;
    }

    public static Set<AchievementData> getAchievementDatasFromSQLByAccountID(int accountID) {
        Set<AchievementData> achievementDatas = new HashSet<>();
        String query = "SELECT * FROM achievement_datas WHERE accid = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AchievementData achievementData = new AchievementData();
                    achievementData.setId(rs.getInt("id"));
                    achievementData.setAccID(accountID);
                    achievementData.setInfoID(rs.getInt("infoid"));
                    achievementData.setMissionID(rs.getByte("missionid"));
                    achievementData.setStatus(rs.getByte("status"));
                    achievementData.setUnlockTime(DatabaseManager.getFileTimeFromString(rs.getString("unlocktime")));
                    achievementData.setMsg(rs.getString("msg"));

                    achievementDatas.add(achievementData);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return achievementDatas;
    }

    public void updateAchievementDataToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `achievement_datas` (" +
                    "`accid`, " +
                    "`infoid`, " +
                    "`missionid`, " +
                    "`status`, " +
                    "`msg`, " +
                    "`unlocktime` " +
                    ") VALUES (" +
                    String.format("%d, ", getAccID()) +
                    String.format("%d, ", getInfoID()) +
                    String.format("%d, ", getMissionID()) +
                    String.format("%d, ", getStatus()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getMsg(), false) +
                    DatabaseManager.getSQLStringSyntax(false, "", getUnlockTime(), true) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE achievement_datas SET " +
                    String.format("accid = %d, ", getAccID()) +
                    String.format("infoid = %d, ", getInfoID()) +
                    String.format("missionid = %d, ", getMissionID()) +
                    String.format("status = %d, ", getStatus()) +
                    DatabaseManager.getSQLStringSyntax(true, "msg", getMsg(), false) +
                    String.format("unlocktime = '%s' ", DatabaseManager.convertToDateTimeSQL(getUnlockTime())) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getInfoID());
        outPacket.encodeByte(getMissionID());
        outPacket.encodeByte(getStatus());
        outPacket.encodeFT(getUnlockTime());
        int type = 42;
        outPacket.encodeInt(type);
        if (type == 42) {
            outPacket.encodeString(getMsg());
        } else {
            outPacket.encodeFT(getUnlockTime());
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInfoID() {
        return infoID;
    }

    public void setInfoID(int infoID) {
        this.infoID = infoID;
    }

    public byte getMissionID() {
        return missionID;
    }

    public void setMissionID(byte missionID) {
        this.missionID = missionID;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public FileTime getUnlockTime() {
        return unlockTime;
    }

    public void setUnlockTime(FileTime unlockTime) {
        this.unlockTime = unlockTime;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getValue() {
        String val = getMsg();
        String[] props = val.split(";");
        for (String prop : props) {
            String[] keyVal = prop.split("=");
            if (keyVal.length == 2) {
                return keyVal[1];
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return "AchievementData {" +
                "id=" + id +
                ", infoID=" + infoID +
                ", missionID=" + missionID +
                ", status=" + status +
                ", unlockTime=" + unlockTime +
                ", msg=" + msg +
                '}';
    }

    public int getAccID() {
        return accID;
    }

    public void setAccID(int accID) {
        this.accID = accID;
    }
}
