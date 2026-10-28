package net.swordie.ms.client.character.damage;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.*;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

public class DamageSkinSaveData {

    private long id;
    private int charId;
    private int damageSkinID;
    private int itemID;
    private boolean notSave;
    private String description;
    private FileTime activateTime = FileTime.MIN_TIME();

    public static Set<DamageSkinSaveData> getDamageSkinsFromSQLByCharID(int charID) {
        Set<DamageSkinSaveData> damageSkins = new HashSet<>();
        String query = "SELECT * FROM damageskins WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DamageSkinSaveData damageSkinSaveData = new DamageSkinSaveData();
                    damageSkinSaveData.setId(rs.getLong("id"));
                    damageSkinSaveData.setCharId(rs.getInt("charid"));
                    damageSkinSaveData.setDamageSkinID(rs.getInt("damageskinid"));
                    damageSkinSaveData.setItemID(rs.getInt("itemid"));
                    damageSkinSaveData.setNotSave(rs.getByte("notsave") != 0);
                    damageSkinSaveData.setDescription(rs.getString("description"));
                    try {
                        damageSkinSaveData.setActivateTime(DatabaseManager.getFileTimeFromString(rs.getString("activatetime")));
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    damageSkins.add(damageSkinSaveData);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return damageSkins;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `damageskins` (" +
                    "`charid`, " +
                    "`damageskinid`, " +
                    "`itemID`, " +
                    "`notsave`, " +
                    "`activatetime`, " +
                    "`description` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getDamageSkinID()) +
                    String.format("%d, ", getItemID()) +
                    String.format("%d, ", isNotSave() ? 1 : 0) +
                    String.format("'%s', ", DatabaseManager.convertToDateTimeSQL(getActivateTime())) +
                    String.format("'%s' ", "") +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE damageskins SET " +
                    String.format("charid = %d, ", getCharId()) +
                    String.format("damageskinid = %d, ", getDamageSkinID()) +
                    String.format("itemID = %d, ", getItemID()) +
                    String.format("notsave = %d, ", isNotSave() ? 1 : 0) +
                    String.format("activatetime = '%s', ", DatabaseManager.convertToDateTimeSQL(getActivateTime())) +
                    String.format("description = '%s' ", "") +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteDamageSkinSaveDataFromSQL() {
        String query = "DELETE FROM `damageskins` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public DamageSkinSaveData() {
        this.damageSkinID = -1;
        this.notSave = true;
    }

    public DamageSkinSaveData(int damageSkinID, int itemID, boolean notSave, String description) {
        this.damageSkinID = damageSkinID;
        this.itemID = itemID;
        this.notSave = notSave;
        this.description = description;
        this.activateTime = FileTime.MIN_TIME();
    }

    public DamageSkinSaveData(int charId, int damageSkinID, int itemID, boolean notSave, String description, FileTime fileTime) {
        this.charId = charId;
        this.damageSkinID = damageSkinID;
        this.itemID = itemID;
        this.notSave = notSave;
        this.description = description;
        this.activateTime = fileTime;
    }

    public static DamageSkinSaveData getByItemID(int itemID) {
        return new DamageSkinSaveData(ItemConstants.getDamageSkinIDByItemID(itemID), itemID, false,
                StringData.getItemStringById(itemID));
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getDamageSkinID());
        outPacket.encodeInt(getItemID());
        outPacket.encodeByte(isNotSave());
        outPacket.encodeString(StringData.getItemStringById(getItemID()));
        outPacket.encodeInt(0);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }

    public int getDamageSkinID() {
        return damageSkinID;
    }

    public void setDamageSkinID(int damageSkinID) {
        this.damageSkinID = damageSkinID;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public boolean isNotSave() {
        return notSave;
    }

    public void setNotSave(boolean notSave) {
        this.notSave = notSave;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public FileTime getActivateTime() {
        return activateTime;
    }

    public void setActivateTime(FileTime activateTime) {
        this.activateTime = activateTime;
    }
}
