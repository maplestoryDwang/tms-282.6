package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ExtendSP {

    private int id;
    private List<SPSet> spSet;

    public static ExtendSP getExtendSPFromSQLByID(int extendSPID) {
        ExtendSP extendSP = null;
        String query = "SELECT * FROM extendsp WHERE id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, extendSPID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    extendSP = new ExtendSP();
                    extendSP.setId(rs.getInt("id"));
                    extendSP.setSpSet(SPSet.getSPSetFromSQLByExtendSPID(extendSPID));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return extendSP;
    }

    public void updateExtendSPToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `extendsp` (`id`) VALUES (DEFAULT);";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        }
        for (SPSet spSet : getSpSet()) {
            spSet.updateSPSetToSQL(getId());
        }
    }

    public void deleteFromSQL(Connection con) throws SQLException {
        for (SPSet spSet : getSpSet()) {
            spSet.deleteSPSetFromSQL();
        }
        String extendspQuery = "DELETE FROM `extendsp` WHERE `id` = ?";
        try (PreparedStatement ps = con.prepareStatement(extendspQuery)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
    }

    public ExtendSP() {
        this(0);
    }

    public ExtendSP(int subJobs) {
        spSet = new ArrayList<>();
        for (int i = 1; i <= subJobs; i++) {
            spSet.add(new SPSet((byte) i, 0));
        }
    }

    public List<SPSet> getSpSet() {
        return spSet;
    }

    public void setSpSet(List<SPSet> spSet) {
        this.spSet = spSet;
    }

    public int getTotalSp() {
        return spSet.stream().mapToInt(SPSet::getSp).sum();
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(getSpSet().size());
        for (SPSet spSet : getSpSet()) {
            outPacket.encodeByte(spSet.getJobLevel());
            if (spSet.getSp() < 0) {
                spSet.setSp(0); // repair a stored negative value before it reaches the client
            }
            outPacket.encodeInt(spSet.getSp());
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSpToJobLevel(int jobLevel, int sp) {
        for (SPSet spSet : spSet) {
            if (spSet.getJobLevel() == jobLevel) {
                spSet.setSp(Math.max(0, sp)); // never negative (2026-09-29: a deduction left job level 3 at -25)
            }
        }
    }

    public void addSpToJobLevel(int jobLevel, int sp) {
        for (SPSet spSet : spSet) {
            if (spSet.getJobLevel() == jobLevel) {
                spSet.setSp(Math.max(0, sp + spSet.getSp()));
            }
        }
    }

    public int getSpByJobLevel(byte jobLevel) {
        SPSet spSet = Util.findWithPred(getSpSet(), sps -> sps.getJobLevel() == jobLevel);
        if (spSet != null) {
            if (spSet.getSp() < 0) {
                spSet.setSp(0); // repair a stored negative value (it is saved as 0 with the character)
            }
            return spSet.getSp();
        }
        return -1;
    }
}
