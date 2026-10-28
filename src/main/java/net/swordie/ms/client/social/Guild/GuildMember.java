package net.swordie.ms.client.social.Guild;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GuildConstants;
import net.swordie.ms.enums.MessageType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class GuildMember implements Encodable {

    private int id;
    private int charID;
    private int grade;
    private int allianceGrade;
    private int commitment;
    private int dayCommitment;
    private int igp;
    private FileTime commitmentIncTime;
    private String name;
    private int job;
    private int level;
    private int guildID;
    private boolean online;

    public static List<GuildMember> getGuildMembersFromSQLByGuildID(int guildID) {
        List<GuildMember> guildMemberList = new ArrayList<>();
        String query = String.format("SELECT * FROM guildmembers WHERE guildid = %d", guildID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                int charID = rs.getInt("charid");
                int grade = rs.getInt("grade");
                int allianceGrade = rs.getInt("alliancegrade");
                int commitment = rs.getInt("commitment");
                int dayCommitment = rs.getInt("daycommitment");
                int igp = rs.getInt("igp");
                FileTime commitmentIncTime = DatabaseManager.getFileTimeFromString(rs.getString("commitmentinctime"));
                String name = rs.getString("name");
                int job = rs.getInt("job");
                int level = rs.getInt("level");

                GuildMember guildMember = new GuildMember();
                guildMember.setId(id);
                guildMember.setCharID(charID);
                guildMember.setGuildID(guildID);
                guildMember.setRank(grade);
                guildMember.setAllianceRank(allianceGrade);
                guildMember.setContribution(commitment);
                guildMember.setDayContribution(dayCommitment);
                guildMember.setIGP(igp);
                guildMember.setContributionIncTime(commitmentIncTime);
                guildMember.setName(name);
                guildMember.setJob(job);
                guildMember.setLevel(level);

                guildMemberList.add(guildMember);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return guildMemberList;
    }

    public void updateGuildMemberToSQL() {
        if(Tms282GuildRuntime.enabled()) {
            synchronized(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
                if(Tms282GuildJournal.isBlocked(getCharID()))return;
                if(getId()<=0||getCharID()<=0||getGuildID()<=0)throw new IllegalStateException("Native guild membership must be created by its journal");
                // A stale object may outlive recovery. Never write membership/rank authority from this cache.
                String query="UPDATE guildmembers SET commitment=?,daycommitment=?,igp=?,commitmentinctime=?,name=?,job=?,level=? WHERE id=? AND charid=? AND guildid=?";
                try(Connection c=DatabaseManager.getConnection();PreparedStatement ps=c.prepareStatement(query)) {
                    ps.setInt(1,getContribution());ps.setInt(2,getDayContribution());ps.setInt(3,getIGP());
                    ps.setString(4,getContributionIncTime()==null?null:DatabaseManager.convertToDateTimeSQL(getContributionIncTime()).toString());
                    ps.setString(5,getName());ps.setInt(6,getJob());ps.setInt(7,getLevel());
                    ps.setInt(8,getId());ps.setInt(9,getCharID());ps.setInt(10,getGuildID());
                    if(ps.executeUpdate()>1)throw new java.sql.SQLException("Nonunique guild member identity");
                } catch(java.sql.SQLException e) { throw new IllegalStateException("Could not persist guild member metadata",e); }
                return;
            }
        }
        if (getId() == 0) {
            String query = "INSERT INTO `guildmembers` (" +
                    "`charid`, " +
                    "`guildid`, " +
                    "`grade`, " +
                    "`alliancegrade`, " +
                    "`commitment`, " +
                    "`daycommitment`, " +
                    "`igp`, " +
                    "`commitmentinctime`, " +
                    "`name`, " +
                    "`job`, " +
                    "`level` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d, ", getGuildID()) +
                    String.format("%d, ", getRank()) +
                    String.format("%d, ", getAllianceRank()) +
                    String.format("%d, ", getContribution()) +
                    String.format("%d, ", getDayContribution()) +
                    String.format("%d, ", getIGP()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getContributionIncTime(), false) +
                    String.format("'%s', ", DatabaseManager.getStringFilter(getName())) +
                    String.format("%d, ", getJob()) +
                    String.format("%d ", getLevel()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE guildmembers SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("guildid = %d, ", getGuildID()) +
                    String.format("grade = %d, ", getRank()) +
                    String.format("alliancegrade = %d, ", getAllianceRank()) +
                    String.format("commitment = %d, ", getContribution()) +
                    String.format("daycommitment = %d, ", getDayContribution()) +
                    String.format("igp = %d, ", getIGP()) +
                    DatabaseManager.getSQLStringSyntax(true, "commitmentinctime", getContributionIncTime(), false) +
                    String.format("name = '%s', ", DatabaseManager.getStringFilter(getName())) +
                    String.format("job = %d, ", getJob()) +
                    String.format("level = %d ", getLevel()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteGuildMemberFromSQL() {
        Tms282GuildRuntime.requireLegacyMembershipMutation();
        String query = "DELETE FROM `guildmembers` WHERE `id` = ?";
        try {
            PreparedStatement ps = DatabaseManager.getConnection().prepareStatement(query);
            ps.setInt(1, getId());
            ps.executeUpdate();
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
        setId(0);
    }

    public static void deleteGuildMemberFromSQL(int charid) {
        Tms282GuildRuntime.requireLegacyMembershipMutation();
        String query = "DELETE FROM `guildmembers` WHERE " + String.format("`charid` = %d", charid);
        DatabaseManager.executeStatement(query);
    }

    public GuildMember() {
    }

    public GuildMember(Char chr) {
        updateInfoFromChar(chr);
        this.grade = 5;
        this.allianceGrade = 5;
    }

    public void updateInfoFromChar(Char chr) {
        setName(chr.getName());
        setCharID(chr.getId());
        setJob(chr.getJob());
        setLevel(chr.getLevel());
        setOnline(chr.isOnline());
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
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

    //endregion

    //region Guild Rank Function
    public int getRank() {
        return grade;
    }

    public void setRank(int grade) {
        this.grade = grade;
    }
    //endregion

    //region Alliance Rank Function
    public int getAllianceRank() {
        return allianceGrade;
    }

    public void setAllianceRank(int allianceGrade) {
        this.allianceGrade = allianceGrade;
    }
    //endregion

    //region Contribution Function
    public int getContribution() {
        return commitment;
    }

    public void setContribution(int commitment) {
        this.commitment = commitment;
    }

    public int getDayContribution() {
        return dayCommitment;
    }

    public void setDayContribution(int dayCommitment) {
        this.dayCommitment = dayCommitment;
    }

    public FileTime getContributionIncTime() {
        return commitmentIncTime;
    }

    public void setContributionIncTime(FileTime commitmentIncTime) {
        this.commitmentIncTime = commitmentIncTime;
    }

    public int getRemainingDayContribution() {
        return GuildConstants.MAX_DAY_CONTRIBUTION - getDayContribution();
    }

    public void addContribution(Guild g, int commitment) {
        setContribution(getContribution() + commitment);
        setDayContribution(getDayContribution() + commitment);
        addIGP((int) (commitment * GuildConstants.IGP_PER_CONTRIBUTION));
        setContributionIncTime(FileTime.currentTime());
        g.broadcast(WvsContext.guildResult(GuildResult.response_CommitmentMemberSet_Success(g, this)));
        g.broadcast(WvsContext.message(MessageType.INC_COMMITMENT_MESSAGE, commitment, "", (byte) 0));
    }
    //endregion

    //region Independent Guild Point Function
    public int getIGP() {
        return igp;
    }

    public void setIGP(int igp) {
        this.igp = igp;
    }

    public void addIGP(int igp) {
        setIGP(getIGP() + igp);
    }
    //endregion

    //region Guild Member ID Function
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    //endregion

    public void encode(OutPacket outPacket) {
        outPacket.encodeString(getName(), 13);
        outPacket.encodeInt(getJob());
        outPacket.encodeInt(getLevel());
        outPacket.encodeInt(getRank());
        outPacket.encodeInt(isOnline() ? 1 : 0);
        outPacket.encodeInt(getAllianceRank());
        outPacket.encodeInt(getContribution() + 100);
        outPacket.encodeInt(getDayContribution());
        outPacket.encodeInt(getIGP());
        outPacket.encodeFT(getContributionIncTime());
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GuildMember && ((GuildMember) obj).getCharID() == getCharID();
    }

    public int getGuildID() {
        return guildID;
    }

    public void setGuildID(int guildID) {
        this.guildID = guildID;
    }
}
