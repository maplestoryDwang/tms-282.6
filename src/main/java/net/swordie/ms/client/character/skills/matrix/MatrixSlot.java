package net.swordie.ms.client.character.skills.matrix;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class MatrixSlot {

    private int id;
    private int charid;
    private int level = 0;
    private int experience;
    private int position = -1;
    private boolean isUnLock = false;

    public static Set<MatrixSlot> getMatrixSlotsFromSQLByCharID(int charID) {
        Set<MatrixSlot> matrixSlots = new HashSet<>();
        String query = "SELECT * FROM matrixslot WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MatrixSlot matrixSlot = new MatrixSlot();
                    matrixSlot.setId(rs.getInt("id"));
                    matrixSlot.setCharId(rs.getInt("charid"));
                    matrixSlot.setLevel(rs.getInt("level"));
                    matrixSlot.setExperience(rs.getInt("experience"));
                    matrixSlot.setPosition(rs.getInt("position"));
                    matrixSlot.setUnLock(rs.getByte("unlocked") != 0);
                    matrixSlots.add(matrixSlot);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return matrixSlots;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `matrixslot` (" +
                    "`charid`, " +
                    "`level`, " +
                    "`experience`, " +
                    "`position`, " +
                    "`unlocked` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getLevel()) +
                    String.format("%d, ", getExperience()) +
                    String.format("%d, ", getPosition()) +
                    String.format("%d ", isUnLock() ? 1 : 0) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE matrixslot SET " +
                    String.format("charid = %d, ", getCharId()) +
                    String.format("level = %d, ", getLevel()) +
                    String.format("experience = %d, ", getExperience()) +
                    String.format("position = %d, ", getPosition()) +
                    String.format("unlocked = %d ", isUnLock() ? 1 : 0) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteMatrixSlotFromSQL() {
        String query = "DELETE FROM `matrixslot` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public MatrixSlot() {
    }

    public MatrixSlot(int charid, int position) {
        this.charid = charid;
        this.position = position;
        this.isUnLock = false;
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

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }

    public boolean isUnLock() {
        return isUnLock;
    }

    public void setUnLock(boolean unLock) {
        isUnLock = unLock;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
