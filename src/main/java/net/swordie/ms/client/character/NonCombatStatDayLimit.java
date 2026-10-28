package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.*;

public class NonCombatStatDayLimit {

    private int id;
    private short charisma;
    private short charm;
    private short insight;
    private short will;
    private short craft;
    private short sense;
    private FileTime lastUpdateCharmByCashPR;
    private byte charmByCashPR;

    public static NonCombatStatDayLimit getNonCombatStatDayLimitFromSQLByID(int nonCombatStatDayLimitID) {
        NonCombatStatDayLimit nonCombatStatDayLimit = null;
        String query = "SELECT * FROM noncombatstatdaylimit WHERE id = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {

            ps.setInt(1, nonCombatStatDayLimitID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    nonCombatStatDayLimit = new NonCombatStatDayLimit();
                    nonCombatStatDayLimit.setId(rs.getInt("id"));
                    nonCombatStatDayLimit.setCharisma(rs.getShort("charisma"));
                    nonCombatStatDayLimit.setCharm(rs.getShort("charm"));
                    nonCombatStatDayLimit.setInsight(rs.getShort("insight"));
                    nonCombatStatDayLimit.setWill(rs.getShort("will"));
                    nonCombatStatDayLimit.setCraft(rs.getShort("craft"));
                    nonCombatStatDayLimit.setSense(rs.getShort("sense"));
                    nonCombatStatDayLimit.setLastUpdateCharmByCashPR(DatabaseManager.getFileTimeFromString(rs.getString("lastupdatecharmbycashpr")));
                    nonCombatStatDayLimit.setCharmByCashPR(rs.getByte("charmbycashpr"));
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        }
        return nonCombatStatDayLimit;
    }

    public void updateNonCombatStatDayLimitToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `noncombatstatdaylimit` (" +
                    "`charisma`, " +
                    "`charm`, " +
                    "`insight`, " +
                    "`will`, " +
                    "`craft`, " +
                    "`sense`, " +
                    "`lastupdatecharmbycashpr`, " +
                    "`charmbycashpr` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharisma()) +
                    String.format("%d, ", getCharm()) +
                    String.format("%d, ", getInsight()) +
                    String.format("%d, ", getWill()) +
                    String.format("%d, ", getCraft()) +
                    String.format("%d, ", getSense()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getLastUpdateCharmByCashPR(), false) +
                    String.format("%d ", getCharmByCashPR()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE noncombatstatdaylimit SET " +
                    String.format("charisma = %d, ", getCharisma()) +
                    String.format("charm = %d, ", getCharm()) +
                    String.format("insight = %d, ", getInsight()) +
                    String.format("will = %d, ", getWill()) +
                    String.format("craft = %d, ", getCraft()) +
                    String.format("sense = %d, ", getSense()) +
                    DatabaseManager.getSQLStringSyntax(true, "lastupdatecharmbycashpr", getLastUpdateCharmByCashPR(), false) +
                    String.format("charmbycashpr = %d ", getCharmByCashPR()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteFromSQL(Connection con) throws SQLException {
        String query = "DELETE FROM `noncombatstatdaylimit` WHERE `id` = ?";
        try (PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, getId());
            ps.executeUpdate();
        }
    }

    public NonCombatStatDayLimit(short charisma, short charm, byte charmByCashPR, short insight, short will, short craft, short sense, FileTime lastUpdateCharmByCashPR) {
        this.charisma = charisma;
        this.charm = charm;
        this.charmByCashPR = charmByCashPR;
        this.insight = insight;
        this.will = will;
        this.craft = craft;
        this.sense = sense;
        this.lastUpdateCharmByCashPR = lastUpdateCharmByCashPR;
    }

    public NonCombatStatDayLimit() {
        this((short) 0, (short) 0, (byte) 0, (short) 0, (short) 0, (short) 0, (short) 0, FileTime.MIN_TIME());
    }

    public short getCharm() {
        return charm;
    }

    public void setCharm(short charm) {
        this.charm = charm;
    }

    public byte getCharmByCashPR() {
        return charmByCashPR;
    }

    public void setCharmByCashPR(byte charmByCashPR) {
        this.charmByCashPR = charmByCashPR;
    }

    public short getInsight() {
        return insight;
    }

    public void setInsight(short insight) {
        this.insight = insight;
    }

    public short getWill() {
        return will;
    }

    public void setWill(short will) {
        this.will = will;
    }

    public short getCraft() {
        return craft;
    }

    public void setCraft(short craft) {
        this.craft = craft;
    }

    public short getSense() {
        return sense;
    }

    public void setSense(short sense) {
        this.sense = sense;
    }

    public FileTime getLastUpdateCharmByCashPR() {
        return lastUpdateCharmByCashPR;
    }

    public void setLastUpdateCharmByCashPR(FileTime lastUpdateCharmByCashPR) {
        this.lastUpdateCharmByCashPR = lastUpdateCharmByCashPR;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getCharisma());
        outPacket.encodeInt(getInsight());
        outPacket.encodeInt(getWill());
        outPacket.encodeInt(getCraft());
        outPacket.encodeInt(getSense());
        outPacket.encodeInt(getCharm());
        outPacket.encodeByte(getCharmByCashPR());
        outPacket.encodeFT(getLastUpdateCharmByCashPR());
        outPacket.encodeInt(FileTime.currentTime().toYYMMDDintValue());
    }

    public short getCharisma() {
        return charisma;
    }

    public void setCharisma(short charisma) {
        this.charisma = charisma;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
