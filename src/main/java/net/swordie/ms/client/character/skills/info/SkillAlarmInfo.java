package net.swordie.ms.client.character.skills.info;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.PetItem;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.SkillAlarmType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SkillAlarmInfo {

    public static final int MAX_INDEX = 6;
    private int id;
    private int charId;
    private int index;
    private int skillId;
    private int key;
    private boolean enable;

    public static List<SkillAlarmInfo> getSkillAlarmsByCharID(int charID) {
        List<SkillAlarmInfo> skillAlertInfos = new ArrayList<>();
        String query = "SELECT * FROM skillalarms WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SkillAlarmInfo skillAlertInfo = new SkillAlarmInfo();
                    skillAlertInfo.setId(rs.getInt("id"));
                    skillAlertInfo.setCharId(rs.getInt("charid"));
                    skillAlertInfo.setIndex(rs.getInt("index"));
                    skillAlertInfo.setSkillId(rs.getInt("skillid"));
                    skillAlertInfo.setEnable(rs.getByte("enable") != 0);
                    skillAlertInfo.setKey(rs.getInt("key_"));
                    skillAlertInfos.add(skillAlertInfo);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return skillAlertInfos;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `skillalarms` (" +
                    "`charid`, " +
                    "`index`, " +
                    "`skillid`, " +
                    "`enable`, " +
                    "`key_` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getIndex()) +
                    String.format("%d, ", getSkillId()) +
                    String.format("%d, ", isEnable() ? 1 : 0) +
                    String.format("%d ", getKey()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE `skillalarms` SET " +
                    String.format("`charid` = %d, ", getCharId()) +
                    String.format("`index` = %d, ", getIndex()) +
                    String.format("`skillid` = %d, ", getSkillId()) +
                    String.format("`enable` = %d, ", isEnable() ? 1 : 0) +
                    String.format("`key_` = %d ", getKey()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public SkillAlarmInfo() {
    }

    public SkillAlarmInfo(int charId, int skillId, int index, boolean isEnabled, int key) {
        this.charId = charId;
        this.skillId = skillId;
        this.index = index;
        this.enable = isEnabled;
        this.key = key;
    }

    public SkillAlarmInfo(int skillId, int index, boolean isEnabled, int key) {
        this.skillId = skillId;
        this.index = index;
        this.enable = isEnabled;
        this.key = key;
    }

    public SkillAlarmInfo(int index) {
        this(0, index, false, 0);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharId() {
        return charId;
    }

    public void setCharId(int charId) {
        this.charId = charId;
    }

    public int getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getSkillId());
        outPacket.encodeInt(getIndex());
        outPacket.encodeByte(isEnable());
        outPacket.encodeInt(getKey());
    }

    public void apply(Char chr) {
        for (SkillAlarmInfo info : chr.getSkillAlarms()) {
            if (info.index == index) {
                info.charId = chr.getId();
                info.skillId = skillId;
                info.enable = enable;
                info.key = key;
                info.saveToSQL();
                break;
            }
        }
    }

    public static OutPacket encode(SkillAlarmType type, SkillAlarmInfo info, SkillAlarmInfo swapInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_ALERT.getValue());

        outPacket.encodeInt(type.getType());
        switch(type) {
            case ADD:
            case UPDATE_STATE:
                info.encode(outPacket);
            case REMOVE:
            case NEW_TYPE:
                outPacket.encodeInt(info.getIndex());
                break;
            case CHANGE_POSITION:
                info.encode(outPacket);
                swapInfo.encode(outPacket);
                break;
        }
        return outPacket;
    }
}