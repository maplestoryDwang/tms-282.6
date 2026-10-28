package net.swordie.ms.client.character.skills;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HyperStat {
    private int id;
    private int charid;
    private int index;
    private int skillID;
    private int slv;

    public static List<HyperStat> getHyperStatsByCharID(int charID) {
        List<HyperStat> hyperStats = new ArrayList<>();
        String query = "SELECT * FROM hyperstats WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HyperStat hyperStat = new HyperStat();
                    hyperStat.setId(rs.getInt("id"));
                    hyperStat.setCharId(rs.getInt("charid"));
                    hyperStat.setIndex(rs.getInt("index"));
                    hyperStat.setSkillID(rs.getInt("skillid"));
                    hyperStat.setSkillLevel(rs.getInt("slv"));
                    hyperStats.add(hyperStat);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return hyperStats;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `hyperstats` (" +
                    "`charid`, " +
                    "`index`, " +
                    "`skillid`, " +
                    "`slv` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getIndex()) +
                    String.format("%d, ", getSkillID()) +
                    String.format("%d ", getSkillLevel()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE `hyperstats` SET " +
                    String.format("`charid` = %d, ", getCharId()) +
                    String.format("`index` = %d, ", getIndex()) +
                    String.format("`skillid` = %d, ", getSkillID()) +
                    String.format("`slv` = %d ", getSkillLevel()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public HyperStat() {
    }

    public HyperStat(int charid, int position, int skillID, int slv) {
        this.charid = charid;
        this.index = position;
        this.skillID = skillID;
        this.slv = slv;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getIndex());
        outPacket.encodeInt(getSkillID());
        outPacket.encodeInt(getSkillLevel());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharId() {
        return charid;
    }

    public void setCharId(int charid) {
        this.charid = charid;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skill) {
        this.skillID = skill;
    }

    public int getSkillLevel() {
        return slv;
    }

    public void setSkillLevel(int level) {
        this.slv = level;
    }
}
