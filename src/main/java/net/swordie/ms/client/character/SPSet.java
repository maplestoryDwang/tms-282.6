package net.swordie.ms.client.character;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SPSet {

    private int id;
    private byte jobLevel;
    private int sp;

    public static List<SPSet> getSPSetFromSQLByExtendSPID(int extendSPID) {
        List<SPSet> spSets = new ArrayList<>();
        String query = "SELECT * FROM spset WHERE extendsp_id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, extendSPID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SPSet spSet = new SPSet();
                    spSet.setId(rs.getInt("id"));
                    spSet.setJobLevel(rs.getByte("joblevel"));
                    spSet.setSp(rs.getInt("sp"));
                    spSets.add(spSet);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return spSets;
    }

    public void updateSPSetToSQL(int extendSPID) {
        if (getId() == 0) {
            String query = "INSERT INTO `spset` (" +
                    "`extendsp_id`, " +
                    "`joblevel`, " +
                    "`sp` " +
                    ") VALUES (" +
                    String.format("%d, ", extendSPID) +
                    String.format("%d, ", getJobLevel()) +
                    String.format("'%s' ", getSp()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE spset SET " +
                    String.format("joblevel = %d, ", getJobLevel()) +
                    String.format("sp = %d ", getSp()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteSPSetFromSQL() {
        String query = "DELETE FROM `spset` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public SPSet() {
    }

    public SPSet(byte jobLevel, int sp) {
        this.jobLevel = jobLevel;
        this.sp = sp;
    }

    public byte getJobLevel() {
        return jobLevel;
    }

    public void setJobLevel(byte jobLevel) {
        this.jobLevel = jobLevel;
    }

    public int getSp() {
        return sp;
    }

    public void setSp(int sp) {
        this.sp = sp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addSp(int sp) {
        setSp(getSp() + sp);
    }
}
