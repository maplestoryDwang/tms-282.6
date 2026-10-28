package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class Core {

    private int id;
    private int pos;
    private int charId;
    private int slotType;
    private int coreID;
    private int leftCount;

    public static Set<Core> getCoresFromSQLByCharID(int charID) {
        Set<Core> cores = new HashSet<>();
        String query = "SELECT * FROM cores WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Core core = new Core();
                    core.setId(rs.getInt("id"));
                    core.setPos(rs.getInt("pos"));
                    core.setCharId(rs.getInt("charid"));
                    core.setSlotType(rs.getInt("slottype"));
                    core.setCoreID(rs.getInt("coreid"));
                    core.setLeftCount(rs.getInt("leftCount"));
                    cores.add(core);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return cores;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `cores` (" +
                    "`pos`, " +
                    "`charid`, " +
                    "`slottype`, " +
                    "`coreid`, " +
                    "`leftCount` " +
                    ") VALUES (" +
                    String.format("%d, ", getPos()) +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getSlotType()) +
                    String.format("%d, ", getCoreID()) +
                    String.format("%d ", getLeftCount()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE cores SET " +
                    String.format("pos = %d, ", getPos()) +
                    String.format("charid = %d, ", getCharId()) +
                    String.format("slottype = %d, ", getSlotType()) +
                    String.format("coreid = %d, ", getCoreID()) +
                    String.format("leftCount = %d ", getLeftCount()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteCoreFromSQL() {
        String query = "DELETE FROM `cores` WHERE " +
                String.format("`id` = %d", getId());
        net.swordie.ms.connection.hikariCP.DatabaseManager.executeStatement(query);
        setId(0);
    }

    public Core() {
    }

    public Core(int charId, int pos, int slotType, int coreID, int leftCount) {
        this.charId = charId;
        this.pos = pos;
        this.slotType = slotType;
        this.coreID = coreID;
        this.leftCount = leftCount;
    }

    public Core deepCopy() {
        Core copy = new Core();
        copy.setCharId(getCharId());
        copy.setPos(getPos());
        copy.setSlotType(getSlotType());
        copy.setCoreID(getCoreID());
        copy.setLeftCount(getLeftCount());
        return copy;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getCoreID());
        outPacket.encodeInt(getLeftCount());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSlotType() {
        return slotType;
    }

    public void setSlotType(int slotType) {
        this.slotType = slotType;
    }

    public int getCoreID() {
        return coreID;
    }

    public void setCoreID(int coreID) {
        this.coreID = coreID;
    }

    public int getLeftCount() {
        return leftCount;
    }

    public void setLeftCount(int leftCount) {
        this.leftCount = leftCount;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }
}
