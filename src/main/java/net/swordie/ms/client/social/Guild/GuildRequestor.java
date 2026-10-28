package net.swordie.ms.client.social.Guild;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GuildRequestor {

    private int id;
    private int charID;
    private int guildID;
    private String name;
    private int job;
    private int level;
    private Guild guild;

    public static List<GuildRequestor> getGuildRequestorListFromSQLByGuildID(int guildID) {
        List<GuildRequestor> guildRequestorList = new ArrayList<>();
        String query = String.format("SELECT * FROM guildrequestors WHERE guildid = %d", guildID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                //requestors_id TODO: DELETE THIS COLUMN
                int charID = rs.getInt("charid");
                String name = rs.getString("name");
                int job = rs.getInt("job");
                int level = rs.getInt("level");

                GuildRequestor guildRequestor = new GuildRequestor();
                guildRequestor.setId(id);
                guildRequestor.setCharID(charID);
                guildRequestor.setGuildID(guildID);
                guildRequestor.setName(name);
                guildRequestor.setJob(job);
                guildRequestor.setLevel(level);

                guildRequestorList.add(guildRequestor);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            net.swordie.ms.connection.hikariCP.DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return guildRequestorList;
    }

    public static GuildRequestor getGuildRequestorFromSQLByCharID(int charID) {
        GuildRequestor guildRequestor = null;
        String query = String.format("SELECT * FROM guildrequestors WHERE charid = %d", charID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                int guildID = rs.getInt("guildid");
                String name = rs.getString("name");
                int job = rs.getInt("job");
                int level = rs.getInt("level");

                guildRequestor = new GuildRequestor();
                guildRequestor.setId(id);
                guildRequestor.setCharID(charID);
                guildRequestor.setGuildID(guildID);
                guildRequestor.setName(name);
                guildRequestor.setJob(job);
                guildRequestor.setLevel(level);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            net.swordie.ms.connection.hikariCP.DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return guildRequestor;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `guildrequestors` (" +
                    "`charid`, " +
                    "`guildid`, " +
                    "`name`, " +
                    "`job`, " +
                    "`level` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d, ", getGuildID()) +
                    String.format("'%s', ", net.swordie.ms.connection.hikariCP.DatabaseManager.getStringFilter(getName())) +
                    String.format("%d, ", getJob()) +
                    String.format("%d ", getLevel()) +
                    ");";
            int id = (int) net.swordie.ms.connection.hikariCP.DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE guildrequestors SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("guildid = %d, ", getGuildID()) +
                    String.format("name = '%s', ", net.swordie.ms.connection.hikariCP.DatabaseManager.getStringFilter(getName())) +
                    String.format("job = %d, ", getJob()) +
                    String.format("level = %d ", getLevel()) +
                    String.format("WHERE id = %d;", getId());
            net.swordie.ms.connection.hikariCP.DatabaseManager.executeStatement(query);
        }
    }

    public void deleteGuildRequestorFromSQL() {
        String query = "DELETE FROM `guildrequestors` WHERE " +
                String.format("`id` = %d", getId());
        net.swordie.ms.connection.hikariCP.DatabaseManager.executeStatement(query);
        setId(0);
    }

    public Guild getGuild() {
        return guild;
    }

    public GuildRequestor() {
    }

    public GuildRequestor(Char chr, Guild guild) {
        this.guild = guild;
        updateInfoFromChar(chr);
        updateInfoFromGuild(guild);
    }

    public void updateInfoFromChar(Char chr) {
        setName(chr.getName());
        setCharID(chr.getId());
        setJob(chr.getJob());
        setLevel(chr.getLevel());
    }

    public void updateInfoFromGuild(Guild guild) {
        setGuildID(guild.getId());
    }

    public Char getChr() {
        return Server.get().getWorld().getCharById(charID);
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public int getGuildID() {
        return guildID;
    }

    public void setGuildID(int guildID) {
        this.guildID = guildID;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getJob() {
        return job;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isOnline() {
        if (getChr() != null) {
            return getChr().isOnline();
        }
        return false;
    }

    public static void setOnlineStateByChar(Char chr, GuildRequestor guildRequestor, boolean online) {
        if (chr == null || guildRequestor == null) {
            return;
        }
        guildRequestor.saveToSQL();
        Guild guild = chr.getClient().getWorld().getGuildByID(guildRequestor.getGuildID());
        guild.broadcast(online ? WvsContext.guildResult(GuildResult.response_GuildRequest_Success(chr, guild, guildRequestor))
                : WvsContext.guildResult(GuildResult.response_GuildRequest_Cancelled(chr, guild)), chr);
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeString(getName(), 13);
        outPacket.encodeInt(getJob());
        outPacket.encodeInt(getLevel());
        outPacket.encodeInt(5); // rank?
        outPacket.encodeInt(isOnline() ? 1 : 0);
        // Following is guild specific info, requestors don't have these
        outPacket.encodeFT(FileTime.MIN_TIME());
        outPacket.encodeInt(5); // alliance rank
        outPacket.encodeInt(0); // contribution
        outPacket.encodeInt(0);
        outPacket.encodeFT(FileTime.MIN_TIME());
        outPacket.encodeInt(0);
        outPacket.encodeFT(FileTime.MIN_TIME());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GuildMember && ((GuildMember) obj).getCharID() == getCharID();
    }

}
