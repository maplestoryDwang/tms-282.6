package net.swordie.ms.client.character.skills.matrix;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.MatrixStateType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.io.Serializable;
import java.sql.*;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MatrixCore implements Serializable {

    private int id;
    private int charid;
    private MatrixStateType state = MatrixStateType.INACTIVE;
    private int coreID;
    private int skillID1;
    private int skillID2;
    private int skillID3;
    private int level = 1;
    private int maxLevel;
    private int experience;
    private int slot = -1;
    private boolean lock = false;

    public static Set<MatrixCore> getMatrixCoresFromSQLByCharID(int charID) {
        Set<MatrixCore> cores = new HashSet<>();
        String query = "SELECT * FROM matrixskill WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatrixCore core = new MatrixCore();
                    core.setId(rs.getInt("id"));
                    MatrixStateType state = MatrixStateType.getStateByVal(rs.getByte("state"));
                    if (state.getVal() == MatrixStateType.DISASSEMBLED.getVal()) {
                        core.deleteMatrixCoreFromSQL();
                        continue;
                    }
                    core.setCharId(rs.getInt("charid"));
                    core.setState(state);
                    core.setCoreID(rs.getInt("coreID"));
                    core.setSkillID1(rs.getInt("skillID1"));
                    core.setSkillID2(rs.getInt("skillID2"));
                    core.setSkillID3(rs.getInt("skillID3"));
                    core.setSkillLevel(rs.getInt("level"));
                    core.setMaxLevel(rs.getInt("maxLevel"));
                    core.setExperience(rs.getInt("experience"));
                    core.setSlot(rs.getInt("slot"));
                    core.setLock(rs.getByte("lock") != 0);
                    cores.add(core);
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP, e);
        }
        return cores;
    }

    public static void saveToSQL(List<MatrixCore> cores) {
        if (cores == null || cores.isEmpty()) return;

        final String sql =
                "INSERT INTO `matrixskill` " +
                        "(`charid`,`state`,`coreID`,`skillID1`,`skillID2`,`skillID3`,`level`,`maxLevel`,`experience`,`slot`,`lock`) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int batchCount = 0;
            for (MatrixCore c : cores) {
                if (c.getId() != 0) continue;

                ps.setInt(1, c.getCharId());
                ps.setInt(2, c.getState().getVal());
                ps.setInt(3, c.getCoreID());
                ps.setInt(4, c.getSkillID1());
                ps.setInt(5, c.getSkillID2());
                ps.setInt(6, c.getSkillID3());
                ps.setInt(7, c.getSkillLevel());
                ps.setInt(8, c.getMaxLevel());
                ps.setInt(9, c.getExperience());
                ps.setInt(10, c.getSlot());
                ps.setByte(11, (byte) (c.isLock() ? 1 : 0));
                ps.addBatch();
                batchCount++;
            }

            if (batchCount == 0) return;

            ps.executeBatch();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                for (MatrixCore c : cores) {
                    if (c.getId() != 0) continue;
                    if (!rs.next()) break;
                    c.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.HIKARICP, e);
        }
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `matrixskill` (" +
                    "`charid`, " +
                    "`state`, " +
                    "`coreID`, " +
                    "`skillID1`, " +
                    "`skillID2`, " +
                    "`skillID3`, " +
                    "`level`, " +
                    "`maxLevel`, " +
                    "`experience`, " +
                    "`slot`, " +
                    "`lock` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getState().getVal()) +
                    String.format("%d, ", getCoreID()) +
                    String.format("%d, ", getSkillID1()) +
                    String.format("%d, ", getSkillID2()) +
                    String.format("%d, ", getSkillID3()) +
                    String.format("%d, ", getSkillLevel()) +
                    String.format("%d, ", getMaxLevel()) +
                    String.format("%d, ", getExperience()) +
                    String.format("%d, ", getSlot()) +
                    String.format("%d ", isLock() ? 1 : 0) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE matrixskill SET " +
                    String.format("`state` = %d, ", getState().getVal()) +
                    String.format("`coreID` = %d, ", getCoreID()) +
                    String.format("`skillID1` = %d, ", getSkillID1()) +
                    String.format("`skillID2` = %d, ", getSkillID2()) +
                    String.format("`skillID3` = %d, ", getSkillID3()) +
                    String.format("`level` = %d, ", getSkillLevel()) +
                    String.format("`maxLevel` = %d, ", getMaxLevel()) +
                    String.format("`experience` = %d, ", getExperience()) +
                    String.format("`slot` = %d, ", getSlot()) +
                    String.format("`lock` = %d ", isLock() ? 1 : 0) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteMatrixCoreFromSQL() {
        String query = "DELETE FROM `matrixskill` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public MatrixCore() {
    }

    public MatrixCore(int charid, int coreID, int skillID1, int skillID2, int skillID3) {
        this.charid = charid;
        this.coreID = coreID;
        this.skillID1 = skillID1;
        this.skillID2 = skillID2;
        this.skillID3 = skillID3;
        this.maxLevel = 25;
        this.experience = 0;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeLong(getId());
        outPacket.encodeInt(getCoreID());
        outPacket.encodeInt(getSkillLevel());
        outPacket.encodeInt(getExperience());
        outPacket.encodeInt(getState().getVal());
        outPacket.encodeInt(getSkillID1());
        outPacket.encodeInt(getSkillID2());
        outPacket.encodeInt(getSkillID3());
        outPacket.encodeInt(getSlot());
        outPacket.encodeFT(FileTime.MAX_TIME());
        outPacket.encodeByte(isLock());
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

    public void setCharId(int charID) {
        this.charid = charID;
    }

    public MatrixStateType getState() {
        return state;
    }

    public void setState(MatrixStateType state) {
        this.state = state;
    }

    public boolean isActive() {
        return slot != -1;
    }

    public int getCoreID() {
        return coreID;
    }

    public void setCoreID(int coreID) {
        this.coreID = coreID;
    }

    public int getSkillID1() {
        return skillID1;
    }

    public void setSkillID1(int skillID1) {
        this.skillID1 = skillID1;
    }

    public int getSkillID2() {
        return skillID2;
    }

    public void setSkillID2(int skillID2) {
        this.skillID2 = skillID2;
    }

    public int getSkillID3() {
        return skillID3;
    }

    public void setSkillID3(int skillID3) {
        this.skillID3 = skillID3;
    }

    public int getSkillLevel() {
        return level;
    }

    public void setSkillLevel(int level) {
        this.level = level;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MatrixCore skill = (MatrixCore) o;
        return id == skill.id && coreID == skill.coreID && skillID1 == skill.skillID1 && skillID2 == skill.skillID2 && skillID3 == skill.skillID3;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, coreID);
    }

    public boolean isLock() {
        return lock;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }
}
