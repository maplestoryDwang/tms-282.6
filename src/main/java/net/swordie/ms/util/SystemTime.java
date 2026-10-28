package net.swordie.ms.util;

import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SystemTime {

    private int id;
    private int year;
    private int month;

    public static SystemTime getSystemTimeFromSQLByID(int systemTimesID) {
        SystemTime systemTime = null;
        String query = "SELECT * FROM systemtimes WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, systemTimesID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    int yr = rs.getInt("yr");
                    int mnth = rs.getInt("mnth");
                    systemTime = new SystemTime();
                    systemTime.setId(id);
                    systemTime.setYear(yr);
                    systemTime.setMonth(mnth);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return systemTime;
    }

    public void updateSystemTimeToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `systemtimes` (" +
                    "`yr`, " +
                    "`mnth` " +
                    ") VALUES (" +
                    String.format("%d, ", getYear()) +
                    String.format("%d ", getMonth()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE systemtimes SET " +
                    String.format("yr = %d, ", getYear()) +
                    String.format("mnth = %d ", getMonth()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteFromSQL(Connection conn) throws SQLException {
        String query = "DELETE FROM `systemtimes` WHERE `id` = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
    }

    public SystemTime() {
    }

    public SystemTime(int year, int month) {
        this.year = year;
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
