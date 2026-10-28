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

public class LinkedSkill {
    private int id;
    private int charid;
    private int index;
    private int skillID;
    private int slv;

    public static List<LinkedSkill> getLinkedSkillsByCharID(int charID) {
        List<LinkedSkill> linkedSkills = new ArrayList<>();
        String query = "SELECT * FROM linkedskills WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LinkedSkill linkedSkill = new LinkedSkill();
                    linkedSkill.setId(rs.getInt("id"));
                    linkedSkill.setCharId(rs.getInt("charid"));
                    linkedSkill.setIndex(rs.getInt("index"));
                    linkedSkill.setSkillID(rs.getInt("skillid"));
                    linkedSkill.setSkillLevel(rs.getInt("slv"));
                    linkedSkills.add(linkedSkill);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return linkedSkills;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `linkedskills` (" +
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
            String query = "UPDATE `linkedskills` SET " +
                    String.format("`charid` = %d, ", getCharId()) +
                    String.format("`index` = %d, ", getIndex()) +
                    String.format("`skillid` = %d, ", getSkillID()) +
                    String.format("`slv` = %d ", getSkillLevel()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public LinkedSkill() {
    }

    public LinkedSkill(int charid, int position, int skillID, int slv) {
        this.charid = charid;
        this.index = position;
        this.skillID = skillID;
        this.slv = slv;
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
