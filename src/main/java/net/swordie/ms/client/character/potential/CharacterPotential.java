package net.swordie.ms.client.character.potential;

import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.*;

public class CharacterPotential {

    private long id;
    private int preset;
    private byte key;
    private int skillID;
    private byte slv;
    private byte grade;
    private int charID;

    public static Set<CharacterPotential> getCharacterPotentialsFromSQLByCharID(int charID) {
        Set<CharacterPotential> characterPotentials = new HashSet<>();
        String query = "SELECT * FROM characterpotentials WHERE charid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CharacterPotential characterPotential = new CharacterPotential();
                    characterPotential.setId(rs.getLong("id"));
                    characterPotential.setPreset(rs.getInt("preset"));
                    characterPotential.setKey(rs.getByte("potkey"));
                    characterPotential.setSkillID(rs.getInt("skillid"));
                    characterPotential.setSlv(rs.getByte("slv"));
                    characterPotential.setGrade(rs.getByte("grade"));
                    characterPotential.setCharID(rs.getInt("charid"));
                    characterPotentials.add(characterPotential);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return characterPotentials;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `characterpotentials` (" +
                    "`preset`, " +
                    "`potkey`, " +
                    "`skillID`, " +
                    "`slv`, " +
                    "`grade`, " +
                    "`charid` " +
                    ") VALUES (" +
                    String.format("%d, ", getPreset()) +
                    String.format("%d, ", getKey()) +
                    String.format("%d, ", getSkillID()) +
                    String.format("%d, ", getSlv()) +
                    String.format("%d, ", getGrade()) +
                    String.format("%d ", getCharID()) +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE characterpotentials SET " +
                    String.format("preset = %d, ", getPreset()) +
                    String.format("potkey = %d, ", getKey()) +
                    String.format("skillID = %d, ", getSkillID()) +
                    String.format("slv = %d, ", getSlv()) +
                    String.format("grade = %d, ", getGrade()) +
                    String.format("charid = %d ", getCharID()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteFromSQL() {
        String query = "DELETE FROM `characterpotentials` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public CharacterPotential() {
    }

    public CharacterPotential(int preset, byte key, int skillID, byte slv, byte grade) {
        this.preset = preset;
        this.key = key;
        this.skillID = skillID;
        this.slv = slv;
        this.grade = grade;
    }

    public CharacterPotential(int charID, int preset, byte key, int skillID, byte slv, byte grade) {
        this.charID = charID;
        this.preset = preset;
        this.key = key;
        this.skillID = skillID;
        this.slv = slv;
        this.grade = grade;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getPreset() {
        return preset;
    }

    public void setPreset(int preset) {
        this.preset = preset;
    }

    public byte getKey() {
        return key;
    }

    public void setKey(byte key) {
        this.key = key;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public byte getSlv() {
        return slv;
    }

    public void setSlv(byte slv) {
        this.slv = slv;
    }

    public byte getGrade() {
        return grade;
    }

    public void setGrade(byte grade) {
        this.grade = grade;
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(getKey());
        outPacket.encodeInt(getSkillID());
        outPacket.encodeByte(getSlv());
        outPacket.encodeByte(getGrade());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterPotential that = (CharacterPotential) o;
        return key == that.key && preset == that.preset && skillID == that.skillID && slv == that.slv;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    public Skill getSkill() {
        Skill skill = SkillData.getSkillDeepCopyById(getSkillID());
        if (skill == null) {
            return null;
        }
        skill.setCurrentLevel(getSlv());
        return skill;
    }
}
