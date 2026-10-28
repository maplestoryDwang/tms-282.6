package net.swordie.ms.client.character;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class BuffData {

    private int id;
    private int charID;
    private String stat;
    private int itemID;
    private int startTime;
    private int value;
    private int duration;

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

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public int getStartTime() {
        return startTime;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public static List<BuffData> getBuffDataListFromSQLByCharID(int charID) {
        List<BuffData> buffDataList = new ArrayList<>();
        String query = "SELECT * FROM buffdata WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String stat = rs.getString("stat");
                    // Kiểm tra điều kiện ngay từ đầu để tránh tạo đối tượng không cần thiết
                    if (stat == null || stat.isEmpty()) {
                        continue;
                    }

                    BuffData buffData = new BuffData();
                    buffData.setId(rs.getInt("id"));
                    buffData.setCharID(charID);
                    buffData.setStat(stat);
                    buffData.setItemID(rs.getInt("itemid"));
                    buffData.setStartTime(rs.getInt("starttime"));
                    buffData.setValue(rs.getInt("value"));
                    buffData.setDuration(rs.getInt("duration"));
                    buffDataList.add(buffData);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return buffDataList;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `buffdata` (" +
                    "`charid`, " +
                    "`stat`, " +
                    "`itemid`, " +
                    "`starttime`, " +
                    "`value`, " +
                    "`duration` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("'%s', ", DatabaseManager.getStringFilter(getStat())) +
                    String.format("%d, ", getItemID()) +
                    String.format("%d, ", getStartTime()) +
                    String.format("%d, ", getValue()) +
                    String.format("%d ", getDuration()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        }
    }

    public void deleteBuffStatFromSQL() {
        String query = "DELETE FROM `buffdata` WHERE " +
                String.format("`charid` = %d", getCharID());
        DatabaseManager.executeStatement(query);
        setId(0);
    }
}
