package net.swordie.ms.client.social.Guild;

import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class GuildSkill implements Encodable {

    private int id;
    private int skillID;
    private short level;
    private FileTime expireDate;
    private String buyCharacterName;
    private String extendCharacterName;

    public static Map<Integer, GuildSkill> getGuildSkillsFromSQLByGuildID(int guildID) {
        Map<Integer, GuildSkill> guildSkillMap = new HashMap<>();
        String query = String.format("SELECT * FROM guildskills JOIN guildskill ON guildskills.skillid = guildskill.id WHERE guild_id = %d", guildID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                int skillID = rs.getInt(7);
                int level = rs.getInt("level");
                FileTime expireDate = DatabaseManager.getFileTimeFromString(rs.getString("expiredate"));
                String buyCharacterMame = rs.getString("buycharactername");
                String extendcharactername = rs.getString("extendcharactername");

                GuildSkill guildSkill = new GuildSkill();
                guildSkill.setId(id);
                guildSkill.setSkillID(skillID);
                guildSkill.setLevel((short) level);
                guildSkill.setExpireDate(expireDate);
                guildSkill.setBuyCharacterName(buyCharacterMame);
                guildSkill.setExtendCharacterName(extendcharactername);

                guildSkillMap.put(skillID, guildSkill);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return guildSkillMap;
    }

    public void updateGuildSkillToSQL(int guildID) {
        String query;
        if (getId() == 0) {
            query = "INSERT INTO `guildskill` (" +
                    "`skillid`, " +
                    "`level`, " +
                    "`expiredate`, " +
                    "`buycharactername`, " +
                    "`extendcharactername` " +
                    ") VALUES (" +
                    String.format("%d, ", getSkillID()) +
                    String.format("%d, ", getLevel()) +
                    String.format("'%s', ", DatabaseManager.convertToDateTimeSQL(getExpireDate())) +
                    String.format("'%s', ", DatabaseManager.getStringFilter(getBuyCharacterName())) +
                    String.format("'%s' ", DatabaseManager.getStringFilter(getExtendCharacterName())) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);

            query = "INSERT INTO `guildskills` (" +
                    "`guild_id`, " +
                    "`skillid` " +
                    ") VALUES (" +
                    String.format("%d, ", guildID) +
                    String.format("%d ", getId()) +
                    ");";
        } else {
            query = "UPDATE guildskill SET " +
                    String.format("skillid = %d, ", getSkillID()) +
                    String.format("level = %d, ", getLevel()) +
                    String.format("expiredate = '%s', ", DatabaseManager.convertToDateTimeSQL(getExpireDate())) +
                    String.format("buycharactername = '%s', ", DatabaseManager.getStringFilter(getBuyCharacterName())) +
                    String.format("extendcharactername = '%s' ", DatabaseManager.getStringFilter(getExtendCharacterName())) +
                    String.format("WHERE id = %d;", getId());
        }
        DatabaseManager.executeStatement(query);
    }

    public void deleteGuildSkillFromSQL(int guildID) {
        String query = String.format("DELETE FROM `guildskills` WHERE `id` = %d AND `guild_id` = %d; ", getId(), guildID);
        query += String.format("DELETE FROM `guildskill` WHERE `id` = %d; ", getId());
        net.swordie.ms.connection.hikariCP.DatabaseManager.executeStatement(query);
        setId(0);
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public short getLevel() {
        return level;
    }

    public void setLevel(short level) {
        this.level = level;
    }

    public FileTime getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(FileTime expireDate) {
        this.expireDate = expireDate;
    }

    public String getBuyCharacterName() {
        return buyCharacterName;
    }

    public void setBuyCharacterName(String buyCharacterName) {
        this.buyCharacterName = buyCharacterName;
    }

    public String getExtendCharacterName() {
        return extendCharacterName;
    }

    public void setExtendCharacterName(String extendCharacterName) {
        this.extendCharacterName = extendCharacterName;
    }

    public void encode(OutPacket outPacket) {
        // GUILDDATA::SKILLENTRY::Decode
        outPacket.encodeShort(getLevel());
        outPacket.encodeLong(0); //Expire Date wrong ? this is cooldown skill
        outPacket.encodeString(getBuyCharacterName());
        outPacket.encodeString(getExtendCharacterName());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
