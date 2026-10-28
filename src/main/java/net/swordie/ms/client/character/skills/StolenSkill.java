package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class StolenSkill {

    private int id;
    private int charID;
    private int skillid;
    private int position;
    private byte currentlv;

    public static Set<StolenSkill> getStolenSkillsFromSQLByCharID(int charID) {
        Set<StolenSkill> stolenSkills = new HashSet<>();
        String query = "SELECT * FROM stolenskills WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StolenSkill stolenSkill = new StolenSkill();
                    stolenSkill.setId(rs.getInt("id"));
                    stolenSkill.setCharID(rs.getInt("charid"));
                    stolenSkill.setSkillid(rs.getInt("skillid"));
                    stolenSkill.setPosition(rs.getInt("position"));
                    stolenSkill.setCurrentlv(rs.getByte("currentlv"));
                    stolenSkills.add(stolenSkill);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return stolenSkills;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `stolenskills` (" +
                    "`charid`, " +
                    "`skillid`, " +
                    "`position`, " +
                    "`currentlv` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d, ", getSkillid()) +
                    String.format("%d, ", getPosition()) +
                    String.format("%d ", getCurrentlv()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE stolenskills SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("skillid = %d, ", getSkillid()) +
                    String.format("position = %d, ", getPosition()) +
                    String.format("currentlv = %d ", getCurrentlv()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteStolenSkillFromSQL() {
        String query = "DELETE FROM `stolenskills` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public StolenSkill() {

    }

    public StolenSkill(int id, int skillid, int position, byte currentlv) {
        this.id = id;
        this.skillid = skillid;
        this.position = position;
        this.currentlv = currentlv;
    }

    public static void setSkill(Char chr, int skillId, int position, byte currentLv) {
        Skill skill = SkillData.getSkillDeepCopyById(skillId);
        skill.setCurrentLevel(currentLv);
        chr.addSkill(skill);

        StolenSkill stolenSkill = new StolenSkill(0, skillId, position, currentLv);
        stolenSkill.setCharID(chr.getId());
        chr.addStolenSkill(stolenSkill);
        stolenSkill.saveToSQL();
    }

    public static void removeSkill(Char chr, int skillId) {
        StolenSkill stolenSkill = chr.getStolenSkillBySkillId(skillId);
        if (stolenSkill == null) {
            return;
        }
        chr.removeStolenSkill(stolenSkill);
        if (chr.hasSkill(skillId)) {
            Skill skill = SkillData.getSkillDeepCopyById(skillId);
            skill.setCurrentLevel(0);
            chr.addSkill(skill);
        }
    }

    public static int getFirstEmptyPosition(Char chr, int skillId) {

        //Used to calculate the position to assign the stolen skill to
        int smJobID = SkillConstants.getStealSkillManagerTabFromSkill(skillId);
        int maxPos = SkillConstants.getMaxPosBysmJobID(smJobID);
        int startingPos = SkillConstants.getStartPosBysmJobID(smJobID);

        for (int i = startingPos; i <= (startingPos + maxPos); i++) {
            if (chr.getStolenSkillByPosition(i) == null) {
                return i;
            }
        }
        return -1;
    }

    public static int getPositionForTab(int position, int skillId) {

        //Used to calculate the position to assign the stolen skill to
        int smJobID = SkillConstants.getStealSkillManagerTabFromSkill(skillId);
        int maxPos = SkillConstants.getMaxPosBysmJobID(smJobID);
        int startingPos = SkillConstants.getStartPosBysmJobID(smJobID);

        return position - startingPos;
    }

    public static int getPositionPerTabFromStolenSkill(StolenSkill stolenSkill) {

        //Used to calculate the position to assign the stolen skill to
        int smJobID = SkillConstants.getStealSkillManagerTabFromSkill(stolenSkill.getSkillid());
        int maxPos = SkillConstants.getMaxPosBysmJobID(smJobID);
        int startingPos = SkillConstants.getStartPosBysmJobID(smJobID);

        return stolenSkill.getPosition() - startingPos;
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

    public int getSkillid() {
        return skillid;
    }

    public void setSkillid(int skillid) {
        this.skillid = skillid;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public byte getCurrentlv() {
        return currentlv;
    }

    public void setCurrentlv(byte currentlv) {
        this.currentlv = currentlv;
    }
}
