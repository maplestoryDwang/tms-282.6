package net.swordie.ms.client.character.skills;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class ChosenSkill {
    private int id;
    private int charID;
    private int skillId;
    private int position;

    public static Set<ChosenSkill> getChosenSkillsFromSQLByCharID(int charID) {
        Set<ChosenSkill> chosenSkills = new HashSet<>();
        String query = "SELECT * FROM chosenskills WHERE charid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ChosenSkill chosenSkill = new ChosenSkill();
                    chosenSkill.setId(rs.getInt("id"));
                    chosenSkill.setCharID(rs.getInt("charid"));
                    chosenSkill.setSkillId(rs.getInt("skillid"));
                    chosenSkill.setPosition(rs.getInt("position"));
                    chosenSkills.add(chosenSkill);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return chosenSkills;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `chosenskills` (" +
                    "`charid`, " +
                    "`skillid`, " +
                    "`position` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d, ", getSkillId()) +
                    String.format("%d ", getPosition()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE chosenskills SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("skillid = %d, ", getSkillId()) +
                    String.format("position = %d ", getPosition()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteChosenSkillFromSQL() {
        String query = "DELETE FROM `chosenskills` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public ChosenSkill() {

    }

    public ChosenSkill(int id, int skillId, int position) {
        this.id = id;
        this.skillId = skillId;
        this.position = position;
    }

    public static int getPositionByImpecSkillId(int impecSkillid) {
        return SkillConstants.getSMJobIdByImpecSkillId(impecSkillid);
    }

    public static void setChosenSkill(Char chr, int stolenSkillId, int impecSkillId) {
        int position = getPositionByImpecSkillId(impecSkillId);

        ChosenSkill cs = chr.getChosenSkillByPosition(position);
        if (cs != null) {
            chr.removeChosenSkill(chr.getChosenSkillByPosition(position));
            cs.deleteChosenSkillFromSQL();
        }
        if (stolenSkillId != 0) {
            ChosenSkill chosenSkill = new ChosenSkill(0, stolenSkillId, position);
            chosenSkill.setCharID(chr.getId());
            chr.addChosenSkill(chosenSkill);
            chosenSkill.saveToSQL();
        }
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

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
