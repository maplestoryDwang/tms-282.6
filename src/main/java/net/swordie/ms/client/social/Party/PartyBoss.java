package net.swordie.ms.client.social.Party;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.*;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

public class PartyBoss {
    private int id;
    private int charId;
    private int orderId;
    private String bossName;
    private int difficulty;
    private int attempt;
    private FileTime lastAttemptTime;

    public static Set<PartyBoss> getPartyBossesFromSQLByCharID(int charID) {
        Set<PartyBoss> partyBosses = new HashSet<>();
        String query = "SELECT * FROM partyboss WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PartyBoss partyBoss = new PartyBoss();
                    partyBoss.setId(rs.getInt("id"));
                    partyBoss.setCharId(rs.getInt("charid"));
                    partyBoss.setOrderId(rs.getInt("orderid"));
                    partyBoss.setBossName(rs.getString("bossname"));
                    partyBoss.setDifficulty(rs.getInt("difficulty"));
                    partyBoss.setAttempt(rs.getInt("attempt"));
                    partyBoss.setLastAttemptTime(DatabaseManager.getFileTimeFromString(rs.getString("lastattempttime")));
                    partyBosses.add(partyBoss);
                }
            }
        } catch (SQLException | ParseException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return partyBosses;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `partyboss` (" +
                    "`charid`, " +
                    "`orderid`, " +
                    "`bossname`, " +
                    "`difficulty`, " +
                    "`attempt`, " +
                    "`lastattempttime` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getOrderId()) +
                    String.format("'%s', ", getBossName()) +
                    String.format("%d, ", getDifficulty()) +
                    String.format("%d, ", getAttempt()) +
                    String.format("'%s' ", DatabaseManager.convertToDateTimeSQL(getLastAttemptTime())) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE partyboss SET " +
                    String.format("charid = %d, ", getCharId()) +
                    String.format("orderid = %d, ", getOrderId()) +
                    String.format("bossname = '%s', ", getBossName()) +
                    String.format("difficulty = %d, ", getDifficulty()) +
                    String.format("attempt = %d, ", getAttempt()) +
                    String.format("lastattempttime = '%s' ", DatabaseManager.convertToDateTimeSQL(getLastAttemptTime())) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deletePartyBossFromSQL() {
        String query = "DELETE FROM `partyboss` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public PartyBoss() {
    }

    public PartyBoss(int charId, int orderId, String bossName, int difficulty, int attempt, FileTime lastAttemptTime) {
        this.charId = charId;
        this.orderId = orderId;
        this.bossName = bossName;
        this.difficulty = difficulty;
        this.attempt = attempt;
        this.lastAttemptTime = lastAttemptTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public String getBossName() {
        return bossName;
    }

    public void setBossName(String bossName) {
        this.bossName = bossName;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public void addAttempt(int attempt) {
        this.attempt += attempt;
    }

    public FileTime getLastAttemptTime() {
        return lastAttemptTime;
    }

    public void setLastAttemptTime(FileTime lastAttemptTime) {
        this.lastAttemptTime = lastAttemptTime;
    }
}
