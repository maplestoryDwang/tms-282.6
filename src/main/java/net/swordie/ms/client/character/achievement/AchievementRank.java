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

public class AchievementRank {

    private long id;
    private int accID;
    private int rank; // 0-6 = Bronze - Silver - Gold - Platium - Diamond - Master
    private int status; // 0 = not unlocked, 1 = achieved, 2 = current selected for insignia
    private FileTime unlockTime;  // unlock time

    public AchievementRank() {
    }

    public AchievementRank(int accID, int rank, int status, FileTime unlockTime) {
        this.accID = accID;
        this.rank = rank;
        this.status = status;
        this.unlockTime = unlockTime;
    }

    public static Set<AchievementRank> getAchievementRanksFromSQLByAccountID(int accountID) {
        Set<AchievementRank> achievementRanks = new HashSet<>();
        String query = "SELECT * FROM achievement_ranks WHERE accid = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, accountID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AchievementRank achievementRank = new AchievementRank();
                    achievementRank.setId(rs.getInt("id"));
                    achievementRank.setAccID(accountID);
                    achievementRank.setRank(rs.getInt("rank"));
                    achievementRank.setStatus(rs.getInt("status"));
                    achievementRank.setUnlockTime(DatabaseManager.getFileTimeFromString(rs.getString("unlocktime")));

                    achievementRanks.add(achievementRank);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return achievementRanks;
    }

    public void updateAchievementRankToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `achievement_ranks` (" +
                    "`accid`, " +
                    "`rank`, " +
                    "`status`, " +
                    "`unlocktime` " +
                    ") VALUES (" +
                    String.format("%d, ", getAccID()) +
                    String.format("%d, ", getRank()) +
                    String.format("%d, ", getStatus()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getUnlockTime(), true) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE achievement_ranks SET " +
                    String.format("accid = %d, ", getAccID()) +
                    String.format("rank = %d, ", getRank()) +
                    String.format("status = %d, ", getStatus()) +
                    String.format("unlocktime = '%s' ", DatabaseManager.convertToDateTimeSQL(getUnlockTime())) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getRank());
        outPacket.encodeByte(getStatus());
        outPacket.encodeFT(getUnlockTime());
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public FileTime getUnlockTime() {
        return unlockTime;
    }

    public void setUnlockTime(FileTime unlockTime) {
        this.unlockTime = unlockTime;
    }

    @Override
    public String toString() {
        return "AchievementRank {" +
                "rank=" + rank +
                ", status=" + status +
                ", unlockTime=" + unlockTime +
                '}';
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAccID() {
        return accID;
    }

    public void setAccID(int accID) {
        this.accID = accID;
    }
}
