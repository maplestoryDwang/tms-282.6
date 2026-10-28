package net.swordie.ms.client.character;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Objects;

public class EventNameTag {

    private int id;
    private int charid;
    private int activeRed;
    private int activeBlue;
    private int activeYellow;
    private int activeGreen;
    private int activePurple;
    private String sRed;
    private String sBlue;
    private String sYellow;
    private String sGreen;
    private String sPurple;

    public static EventNameTag getEventNameTagFromSQLByCharID(int charID) {
        EventNameTag eventNameTag = null;
        String query = "SELECT * FROM eventnametag WHERE charid = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    eventNameTag = new EventNameTag();
                    eventNameTag.setId(rs.getInt("id"));
                    eventNameTag.setCharId(charID);
                    eventNameTag.setActiveRed(rs.getByte("activeRed"));
                    eventNameTag.setActiveBlue(rs.getByte("activeBlue"));
                    eventNameTag.setActiveYellow(rs.getByte("activeYellow"));
                    eventNameTag.setActiveGreen(rs.getByte("activeGreen"));
                    eventNameTag.setActivePurple(rs.getByte("activePurple"));
                    eventNameTag.setsRed(rs.getString("sRed"));
                    eventNameTag.setsBlue(rs.getString("sBlue"));
                    eventNameTag.setsYellow(rs.getString("sYellow"));
                    eventNameTag.setsGreen(rs.getString("sGreen"));
                    eventNameTag.setsPurple(rs.getString("sPurple"));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return eventNameTag;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `eventnametag` (" +
                    "`charid`, " +
                    "`activeRed`, " +
                    "`activeBlue`, " +
                    "`activeYellow`, " +
                    "`activeGreen`, " +
                    "`activePurple`, " +
                    "`sRed`, " +
                    "`sBlue`, " +
                    "`sYellow`, " +
                    "`sGreen`, " +
                    "`sPurple` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getActiveRed()) +
                    String.format("%d, ", getActiveBlue()) +
                    String.format("%d, ", getActiveYellow()) +
                    String.format("%d, ", getActiveGreen()) +
                    String.format("%d, ", getActivePurple()) +
                    String.format("'%s', ", getsRed()) +
                    String.format("'%s', ", getsBlue()) +
                    String.format("'%s', ", getsYellow()) +
                    String.format("'%s', ", getsGreen()) +
                    String.format("'%s' ", getsPurple()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE eventnametag SET " +
                    String.format("charid = %d, ", getCharId()) +
                    String.format("activeRed = %d, ", getActiveRed()) +
                    String.format("activeBlue = %d, ", getActiveBlue()) +
                    String.format("activeYellow = %d, ", getActiveYellow()) +
                    String.format("activeGreen = %d, ", getActiveGreen()) +
                    String.format("activePurple = %d, ", getActivePurple()) +
                    String.format("sRed = '%s', ", getsRed()) +
                    String.format("sBlue = '%s', ", getsBlue()) +
                    String.format("sYellow = '%s', ", getsYellow()) +
                    String.format("sGreen = '%s', ", getsGreen()) +
                    String.format("sPurple = '%s' ", getsPurple()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteEventNameTagFromSQL() {
        String query = "DELETE FROM `eventnametag` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
    }

    public EventNameTag() {

    }

    public EventNameTag(int charID, int activeRed, int activeBlue, int activeYellow, int activeGreen, int activePurple, String sRed, String sBlue, String sYellow, String sGreen, String sPurple) {
        this.charid = charID;
        this.activeRed = activeRed;
        this.activeBlue = activeBlue;
        this.activeYellow = activeYellow;
        this.activeGreen = activeGreen;
        this.activePurple = activePurple;
        this.sRed = sRed;
        this.sBlue = sBlue;
        this.sYellow = sYellow;
        this.sGreen = sGreen;
        this.sPurple = sPurple;
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

    public void setCharId(int charId) {
        this.charid = charId;
    }

    public int getActiveRed() {
        return activeRed;
    }

    public void setActiveRed(int activeRed) {
        this.activeRed = activeRed;
    }

    public int getActiveBlue() {
        return activeBlue;
    }

    public void setActiveBlue(int activeBlue) {
        this.activeBlue = activeBlue;
    }

    public int getActiveYellow() {
        return activeYellow;
    }

    public void setActiveYellow(int activeYellow) {
        this.activeYellow = activeYellow;
    }

    public int getActiveGreen() {
        return activeGreen;
    }

    public void setActiveGreen(int activeGreen) {
        this.activeGreen = activeGreen;
    }

    public int getActivePurple() {
        return activePurple;
    }

    public void setActivePurple(int activePurple) {
        this.activePurple = activePurple;
    }

    public String getsRed() {
        return sRed;
    }

    public void setsRed(String sRed) {
        this.sRed = sRed;
    }

    public String getsBlue() {
        return sBlue;
    }

    public void setsBlue(String sBlue) {
        this.sBlue = sBlue;
    }

    public String getsYellow() {
        return sYellow;
    }

    public void setsYellow(String sYellow) {
        this.sYellow = sYellow;
    }

    public String getsGreen() {
        return sGreen;
    }

    public void setsGreen(String sGreen) {
        this.sGreen = sGreen;
    }

    public String getsPurple() {
        return sPurple;
    }

    public void setsPurple(String sPurple) {
        this.sPurple = sPurple;
    }

    public int[] getActiveNameTags() {
        return new int[]{getActiveRed(), getActiveBlue(), getActiveYellow(), getActiveGreen(), getActivePurple()};
    }

    public String[] getsNameTags() {
        return new String[]{getsRed(), getsBlue(), getsYellow(), getsGreen(), getsPurple()};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventNameTag eventNameTag = (EventNameTag) o;
        return charid == eventNameTag.charid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(charid);
    }

}
