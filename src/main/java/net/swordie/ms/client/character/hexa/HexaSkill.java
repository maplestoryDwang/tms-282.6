package net.swordie.ms.client.character.hexa;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class HexaSkill {
    private int id;
    private int charId;
    private int skillId;
    private int skillLevel;
    private boolean isEnabled;
    private boolean isDiabled;

    public static Set<HexaSkill> getHexaSkillsFromSQLByCharID(int charID) {
        Set<HexaSkill> hexaSkills = new HashSet<>();
        String query = "SELECT * FROM hexaskills WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HexaSkill hexaSkill = new HexaSkill();
                    hexaSkill.setId(rs.getInt("id"));
                    hexaSkill.setCharId(rs.getInt("charid"));
                    hexaSkill.setSkillId(rs.getInt("skillid"));
                    hexaSkill.setSkillLevel(rs.getInt("slv"));
                    hexaSkill.setEnabled(rs.getByte("enabled") != 0);
                    hexaSkill.setDiabled(rs.getByte("disabled") != 0);
                    hexaSkills.add(hexaSkill);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return hexaSkills;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `hexaskills` (" +
                    "`charid`, " +
                    "`skillid`, " +
                    "`slv`, " +
                    "`enabled`, " +
                    "`disabled` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getSkillId()) +
                    String.format("%d, ", getSkillLevel()) +
                    String.format("%d, ", isEnabled() ? 1 : 0) +
                    String.format("%d ", isDiabled() ? 1 : 0) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE hexaskills SET " +
                    String.format("charid = %d, ", getCharId()) +
                    String.format("skillid = %d, ", getSkillId()) +
                    String.format("slv = %d, ", getSkillLevel()) +
                    String.format("enabled = %d, ", isEnabled() ? 1 : 0) +
                    String.format("disabled = %d ", isDiabled() ? 1 : 0) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public HexaSkill() {
    }

    public HexaSkill(int skillId, int slv) {
        this.skillId = skillId;
        this.skillLevel = slv;
        this.isEnabled = true;
        this.isDiabled = false;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getSkillId());
        outPacket.encodeInt(getSkillLevel());
        outPacket.encodeByte(isEnabled());
        outPacket.encodeByte(isDiabled());
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(int skillLevel) {
        this.skillLevel = skillLevel;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnable) {
        this.isEnabled = isEnable;
    }

    public boolean isDiabled() {
        return isDiabled;
    }

    public void setDiabled(boolean diabled) {
        this.isDiabled = diabled;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
