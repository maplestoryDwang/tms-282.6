package net.swordie.ms.client.character.skills;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class Skill {

    private int id;
    private int charId;
    private int skillId;
    private int rootId;
    private int maxLevel;
    private int currentLevel;
    private int masterLevel;

    public static Int2ObjectMap<Skill> getSkillsFromSQLByCharID(int charID) {
        Int2ObjectMap<Skill> skills = new Int2ObjectOpenHashMap<>();
        String query = "SELECT * FROM skills WHERE charid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    int charIDFromDatabase = rs.getInt("charid");
                    int skillID = rs.getInt("skillid");
                    int rootID = rs.getInt("rootid");
                    int maxLevel = rs.getInt("maxlevel");
                    int currentLevel = rs.getInt("currentlevel");
                    int masterLevel = rs.getInt("masterlevel");

                    Skill skill = new Skill();
                    skill.setId(id);
                    skill.setCharId(charIDFromDatabase);
                    skill.setSkillId(skillID);
                    skill.setRootId(rootID);
                    skill.setMaxLevel(maxLevel);
                    skill.setCurrentLevel(currentLevel);
                    skill.setMasterLevel(masterLevel);

                    skills.put(skillID, skill);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return skills;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `skills` (" +
                    "`charid`, " +
                    "`skillid`, " +
                    "`rootid`, " +
                    "`maxlevel`, " +
                    "`currentlevel`, " +
                    "`masterlevel` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getSkillId()) +
                    String.format("%d, ", getRootId()) +
                    String.format("%d, ", getMaxLevel()) +
                    String.format("%d, ", getCurrentLevel()) +
                    String.format("%d ", getMasterLevel()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE skills SET " +
                    String.format("skillid = %d, ", getSkillId()) +
                    String.format("rootid = %d, ", getRootId()) +
                    String.format("maxlevel = %d, ", getMaxLevel()) +
                    String.format("currentlevel = %d, ", getCurrentLevel()) +
                    String.format("masterlevel = %d ", getMasterLevel()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteSkillFromSQL() {
        String query = "DELETE FROM `skills` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getRootId() {
        return rootId;
    }

    public void setRootId(int rootId) {
        this.rootId = rootId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
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

    public int getMasterLevel() {
        return masterLevel;
    }

    public void setMasterLevel(int masterLevel) {
        this.masterLevel = masterLevel;
    }

    @Override
    public String toString() {
        return "id = " + getSkillId() + ", cur = " + getCurrentLevel() + ", master = " + getMasterLevel();
    }
}
