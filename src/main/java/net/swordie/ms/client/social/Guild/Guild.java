package net.swordie.ms.client.social.Guild;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.social.Alliance.Alliance;
import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.connection.packet.UserLocal;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GuildConstants;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.social.Guild.GuildGraderPermissionType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.container.Tuple;

import javax.sql.rowset.serial.SerialBlob;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class Guild implements Encodable {
    private int id;
    private String name;
    private int leaderID;
    private int worldID;
    private List<GuildRequestor> requestors = new ArrayList<>();
    private String grade1 = "";
    private int grade1Permission = 0;
    private String grade2 = "";
    private int grade2Permission = 0;
    private String grade3 = "";
    private int grade3Permission = 0;
    private String grade4 = "";
    private int grade4Permission = 0;
    private String grade5 = "";
    private int grade5Permission = 0;
    private List<GuildMember> members = new ArrayList<>();
    private int markBg;
    private int markBgColor;
    private int mark;
    private int markColor;
    private int maxMembers;
    private String notice;
    private int points;
    private int seasonPoints;
    private int allianceID;
    private int level;
    private int guildRank;
    private int ggp;
    private boolean appliable;
    private int joinSetting;
    private int reqLevel;
    private int battleSp;
    private byte[] customEmblem;
    private Map<Integer, GuildSkill> skills = new HashMap<>();
    private Set<BBSRecord> bbsRecords = new HashSet<>();
    private BBSRecord bbsNotice;
    private Alliance alliance;
    private record Invitation(int inviterId,long expires) { }
    private final Map<Integer, Invitation> invitations = new HashMap<>();
    private final Map<Integer, Tuple<String,Integer>> extraGrades = new HashMap<>();

    public synchronized boolean hasInvitation(int characterId,long now) {
        Invitation invitation=invitations.get(characterId);return invitation!=null && invitation.expires()>now;
    }
    public synchronized void revokeInvitation(int characterId) { invitations.remove(characterId); }
    public synchronized int inviterId(int characterId) { Invitation invitation=invitations.get(characterId);return invitation==null?0:invitation.inviterId(); }
    public String gradeName(int rank) {
        return switch(rank) { case 1->getGrade1();case 2->getGrade2();case 3->getGrade3();case 4->getGrade4();case 5->getGrade5();
            default->extraGrades.containsKey(rank)?extraGrades.get(rank).getLeft():"會員"+rank; };
    }
    public int gradePermission(int rank) {
        return switch(rank) { case 1->getGrade1Permission();case 2->getGrade2Permission();case 3->getGrade3Permission();
            case 4->getGrade4Permission();case 5->getGrade5Permission();default->extraGrades.containsKey(rank)?extraGrades.get(rank).getRight():0; };
    }
    public void setGrade(int rank,String name,int permission) {
        switch(rank) {
            case 1 -> { setGrade1(name);setGrade1Permission(permission); }
            case 2 -> { setGrade2(name);setGrade2Permission(permission); }
            case 3 -> { setGrade3(name);setGrade3Permission(permission); }
            case 4 -> { setGrade4(name);setGrade4Permission(permission); }
            case 5 -> { setGrade5(name);setGrade5Permission(permission); }
            default -> { if(rank<6||rank>10)throw new IllegalArgumentException("Invalid guild grade");extraGrades.put(rank,new Tuple<>(name,permission)); }
        }
    }

    public synchronized boolean invite(int inviterId, int targetId, long now) {
        invitations.entrySet().removeIf(entry -> entry.getValue().expires() <= now);
        if (!hasPermission(inviterId, GuildGraderPermissionType.InviteMembers.getVal())
                || targetId <= 0 || getMemberByCharID(targetId) != null
                || getMembers().size() >= getMaxMembers() || invitations.size() >= 200) return false;
        invitations.put(targetId, new Invitation(inviterId, Math.addExact(now, 120_000L)));
        return true;
    }

    public synchronized boolean consumeInvitation(int targetId, long now) {
        Invitation invitation = invitations.remove(targetId);
        return invitation != null && invitation.expires() > now && getMemberByCharID(targetId) == null
                && getMembers().size() < getMaxMembers();
    }

    public static List<Guild> loadGuildsFromSQL() {
        List<Guild> guildList = new ArrayList<>();
        String query = "SELECT * FROM guilds";
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int leaderID = rs.getInt("leaderid");
                int worldID = rs.getInt("worldid");
                int markBG = rs.getInt("markbg");
                int markBGColor = rs.getInt("markbgcolor");
                int mark = rs.getInt("mark");
                int markColor = rs.getInt("markcolor");
                Blob customemblem = rs.getBlob("customemblem");
                int maxMembers = rs.getInt("maxmembers");
                String notice = rs.getString("notice");
                int points = rs.getInt("points");
                int seasonPoints = rs.getInt("seasonpoints");
                int allianceID = rs.getInt("allianceid");
                int level = rs.getInt("level");
                int guildRank = rs.getInt("guildrank");
                int ggp = rs.getInt("ggp");
                boolean appliable = rs.getByte("appliable") != 0;
                int joinSetting = rs.getInt("joinsetting");
                int reqLevel = rs.getInt("reqlevel");
                int bbsNotice = rs.getInt("bbsNotice");
                int battleSP = rs.getInt("battleSp");

                Guild guild = new Guild();
                guild.setId(id);
                guild.setName(name);
                guild.setLeaderID(leaderID);
                guild.setWorldID(worldID);
                guild.setMarkBg(markBG);
                guild.setMarkBgColor(markBGColor);
                guild.setMark(mark);
                guild.setMarkColor(markColor);
                if (customemblem != null) {
                    int length = (int) customemblem.length();
                    guild.setCustomEmblem(customemblem.getBytes(1, length));
                } else {
                    guild.setCustomEmblem(null);
                }
                guild.setMaxMembers(maxMembers);
                guild.setNotice(notice);
                guild.setHonorEXP(points);
                guild.setSeasonPoints(seasonPoints);
                guild.setAllianceID(allianceID);
                guild.setLevel(level);
                guild.setRank(guildRank);
                guild.setGgp(ggp);
                guild.setAppliable(appliable);
                guild.setJoinSetting(joinSetting);
                guild.setReqLevel(reqLevel);
                guild.setBattleSp(battleSP);

                Map<Integer, Tuple<String, Integer>> grades = guild.loadGuildGradesFromSQL();
                for (Map.Entry<Integer, Tuple<String, Integer>> grade : grades.entrySet()) {
                    switch (grade.getKey()) {
                        case 1:
                            guild.setGrade1(grade.getValue().getLeft());
                            guild.setGrade1Permission(grade.getValue().getRight());
                            break;
                        case 2:
                            guild.setGrade2(grade.getValue().getLeft());
                            guild.setGrade2Permission(grade.getValue().getRight());
                            break;
                        case 3:
                            guild.setGrade3(grade.getValue().getLeft());
                            guild.setGrade3Permission(grade.getValue().getRight());
                            break;
                        case 4:
                            guild.setGrade4(grade.getValue().getLeft());
                            guild.setGrade4Permission(grade.getValue().getRight());
                            break;
                        case 5:
                            guild.setGrade5(grade.getValue().getLeft());
                            guild.setGrade5Permission(grade.getValue().getRight());
                            break;
                    }
                }
                guild.setMembers(GuildMember.getGuildMembersFromSQLByGuildID(id));
                guild.setRequestors(GuildRequestor.getGuildRequestorListFromSQLByGuildID(id));
                guild.setSkills(GuildSkill.getGuildSkillsFromSQLByGuildID(id));
                guild.setBbsRecords(BBSRecord.getBBSRecordsFromSQLByGuildID(id));

                if (bbsNotice != 0) {
                    guild.setBbsNotice(BBSRecord.getBBSNoticeFromSQLByNoticeID(bbsNotice));
                }
                guildList.add(guild);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return guildList;
    }

    public static List<String> loadGuildNamesFromSQL() {
        List<String> names = new ArrayList<>();
        String query = "SELECT name FROM guilds";
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return names;
    }

    public void updateGuildToSQL() {
        if(Tms282GuildRuntime.enabled()) {
            synchronized(net.swordie.ms.client.character.tms282.Tms282VMatrixRuntime.ECONOMY_MONITOR) {
                if(getId()<=0)throw new IllegalStateException("Native guild creation requires its journal");
                if(getMembers().stream().anyMatch(m->Tms282GuildJournal.isBlocked(m.getCharID())))return;
                try(Connection c=DatabaseManager.getConnection()) {
                    try(PreparedStatement ps=c.prepareStatement("SELECT character_id FROM char_guild282_pending WHERE guild_id=?")) {
                        ps.setInt(1,getId());try(ResultSet rs=ps.executeQuery()) { if(rs.next())return; }
                    }
                    // Preserve existing progress/cosmetic properties, never cached identity/leader/grade permissions.
                    String query="UPDATE guilds SET markbg=?,markbgcolor=?,mark=?,markcolor=?,maxmembers=?,points=?,seasonpoints=?,level=?,guildrank=?,ggp=?,appliable=?,joinsetting=?,reqlevel=?,battleSp=? WHERE id=? AND worldid=? AND leaderid=?";
                    try(PreparedStatement ps=c.prepareStatement(query)) {
                        int[] values={getMarkBg(),getMarkBgColor(),getMark(),getMarkColor(),getMaxMembers(),getHonorEXP(),getSeasonPoints(),
                                getLevel(),getRank(),getGgp(),isAppliable()?1:0,getJoinSetting(),getReqLevel(),getBattleSp(),getId(),getWorldID(),getLeaderID()};
                        for(int i=0;i<values.length;i++)ps.setInt(i+1,values[i]);
                        if(ps.executeUpdate()==0)return;
                    }
                } catch(java.sql.SQLException e) { throw new IllegalStateException("Could not persist guild metadata",e); }
                for(GuildMember member:java.util.List.copyOf(getMembers()))member.updateGuildMemberToSQL();
                return;
            }
        }
        if (getId() == 0) {
            String query = "INSERT INTO `guilds` (" +
                    "`name`, " +
                    "`leaderid`, " +
                    "`worldid`, " +
                    "`markbg`, " +
                    "`markbgcolor`, " +
                    "`mark`, " +
                    "`markcolor`, " +
                    "`maxmembers`, " +
                    "`notice`, " +
                    "`points`, " +
                    "`seasonpoints`, " +
                    "`allianceid`, " +
                    "`level`, " +
                    "`guildrank`, " +
                    "`ggp`, " +
                    "`appliable`, " +
                    "`joinsetting`, " +
                    "`reqlevel`, " +
                    "`bbsNotice`, " +
                    "`battleSp` " +
                    ") VALUES (" +
                    String.format("'%s', ", DatabaseManager.getStringFilter(getName())) +
                    String.format("%d, ", getLeaderID()) +
                    String.format("%d, ", getWorldID()) +
                    String.format("%d, ", getMarkBg()) +
                    String.format("%d, ", getMarkBgColor()) +
                    String.format("%d, ", getMark()) +
                    String.format("%d, ", getMarkColor()) +
                    String.format("%d, ", getMaxMembers()) +
                    "NULL," +
                    String.format("%d, ", getHonorEXP()) +
                    String.format("%d, ", getSeasonPoints()) +
                    String.format("%d, ", getAllianceID()) +
                    String.format("%d, ", getLevel()) +
                    String.format("%d, ", getRank()) +
                    String.format("%d, ", getGgp()) +
                    String.format("%d, ", isAppliable() ? 1 : 0) +
                    String.format("%d, ", getJoinSetting()) +
                    String.format("%d, ", getReqLevel()) +
                    String.format("%d, ", 0) +
                    String.format("%d ", getBattleSp()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);

            updateGradeNameToSQL();
            for (GuildMember guildMember : getMembers()) {
                guildMember.updateGuildMemberToSQL();
            }
            for (GuildRequestor guildRequestor : getRequestors()) {
                guildRequestor.saveToSQL();
            }
            for (BBSRecord bbsRecord : getBbsRecords()) {
                bbsRecord.updateBBSRecordToSQL();
            }
            for (GuildSkill guildSkill : getSkills().values()) {
                guildSkill.updateGuildSkillToSQL(getId());
            }
        } else {
            String query = "UPDATE guilds SET " +
                    String.format("name = '%s', ", DatabaseManager.getStringFilter(getName())) +
                    String.format("leaderid = %d, ", getLeaderID()) +
                    String.format("worldid = %d, ", getWorldID()) +
                    String.format("markbg = %d, ", getMarkBg()) +
                    String.format("markbgcolor = %d, ", getMarkBgColor()) +
                    String.format("mark = %d, ", getMark()) +
                    String.format("markcolor = %d, ", getMarkColor()) +
                    String.format("maxmembers = %d, ", getMaxMembers()) +
                    String.format("points = %d, ", getHonorEXP()) +
                    String.format("seasonpoints = %d, ", getSeasonPoints()) +
                    String.format("allianceid = %d, ", getAllianceID()) +
                    String.format("level = %d, ", getLevel()) +
                    String.format("guildrank = %d, ", getRank()) +
                    String.format("ggp = %d, ", getGgp()) +
                    String.format("appliable = %d, ", isAppliable() ? 1 : 0) +
                    String.format("joinsetting = %d, ", getJoinSetting()) +
                    String.format("reqlevel = %d, ", getReqLevel()) +
                    String.format("bbsNotice = %d, ", getBbsNotice() != null ? getBbsNotice().getId() : 0) +
                    String.format("battleSp = %d ", getBattleSp()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);

            updateGradeNameToSQL();
            for (GuildMember guildMember : getMembers()) {
                guildMember.updateGuildMemberToSQL();
            }
            for (GuildRequestor guildRequestor : getRequestors()) {
                guildRequestor.saveToSQL();
            }
            for (BBSRecord bbsRecord : getBbsRecords()) {
                bbsRecord.updateBBSRecordToSQL();
            }
            for (GuildSkill guildSkill : getSkills().values()) {
                guildSkill.updateGuildSkillToSQL(getId());
            }
        }
    }

    public void deleteGuildFromSQL() {
        Tms282GuildRuntime.requireLegacyMembershipMutation();
        deleteGradeNameFromSQL();
        for (GuildMember guildMember : getMembers()) {
            guildMember.deleteGuildMemberFromSQL();
        }
        for (GuildRequestor guildRequestor : getRequestors()) {
            guildRequestor.deleteGuildRequestorFromSQL();
        }
        for (BBSRecord bbsRecord : getBbsRecords()) {
            bbsRecord.deleteBBSRecordFromSQL();
        }
        for (GuildSkill guildSkill : getSkills().values()) {
            guildSkill.deleteGuildSkillFromSQL(getId());
        }
        String query = String.format("DELETE FROM `guilds` WHERE `id` = %d; ", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public byte[] getCustomEmblem() {
        return customEmblem;
    }

    public void setCustomEmblem(byte[] emblem) {
        this.customEmblem = emblem;
    }

    public void updateCustomEmblemToSQL(byte[] emblem) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            Blob blob = null;
            if (emblem != null) {
                blob = new SerialBlob(emblem);
            }
            con = DatabaseManager.getConnection();
            ps = con.prepareStatement("UPDATE guilds SET customemblem = ? WHERE id = ?;");
            ps.setBlob(1, blob);
            ps.setInt(2, getId());
            DataPrinter.send(DataPrinter.HIKARICP, ps.toString(), true);
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception exception) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
            }
        }
    }

    public void updateGuildNotice(String notice) {
        if (notice.contains("DROP TABLE") || notice.contains("TRUNCATE TABLE") || notice.contains("DROP DATABASE")) {
            DataPrinter.send(DataPrinter.AUTOBAN_WARNING, notice);
            return;
        }
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = DatabaseManager.getConnection();
            ps = con.prepareStatement("UPDATE guilds SET notice = ? WHERE id = ?;");
            ps.setString(1, notice);
            ps.setInt(2, getId());
            DataPrinter.send(DataPrinter.HIKARICP, ps.toString(), true);
            ps.execute();
            ps.close();
            con.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
        } finally {
            try {
                if (con != null) {
                    con.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (Exception exception) {
                DataPrinter.send(DataPrinter.HIKARICP_ERROR, exception);
            }
        }
    }

    public Map<Integer, Tuple<String, Integer>> loadGuildGradesFromSQL() {
        Map<Integer, Tuple<String, Integer>> grades = new HashMap<>();
        String query = String.format("SELECT * FROM guildgrades WHERE guildid = %d", getId());
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                Tuple<String, Integer> grade = new Tuple<>("", 0);
                grade.setLeft(rs.getString("gradename"));
                grade.setRight(rs.getInt("gradepermission"));
                grades.put(rs.getInt("graderole"), grade);
                if(rs.getInt("graderole")>5 && rs.getInt("graderole")<=10) extraGrades.put(rs.getInt("graderole"),grade);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return grades;
    }

    public synchronized void updateGradeNameToSQL() {
        if (getId() <= 0) throw new IllegalStateException("Persist guild before its grades");
        try(Connection connection=DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try {
                for(int rank=1;rank<=(extraGrades.isEmpty()?5:10);rank++) {
                    Integer rowId=null;
                    try(PreparedStatement read=connection.prepareStatement("SELECT id FROM guildgrades WHERE guildid=? AND graderole=? FOR UPDATE")) {
                        read.setInt(1,getId());read.setInt(2,rank);
                        try(ResultSet result=read.executeQuery()) {
                            if(result.next())rowId=result.getInt(1);
                            if(result.next())throw new SQLException("Duplicate guild grade");
                        }
                    }
                    String sql=rowId==null?"INSERT INTO guildgrades (gradename,gradepermission,guildid,graderole) VALUES (?,?,?,?)"
                            :"UPDATE guildgrades SET gradename=?,gradepermission=? WHERE guildid=? AND graderole=?";
                    try(PreparedStatement write=connection.prepareStatement(sql)) {
                        write.setString(1,gradeName(rank));write.setInt(2,gradePermission(rank));
                        write.setInt(3,getId());write.setInt(4,rank);
                        if(write.executeUpdate()!=1)throw new SQLException("Guild grade row changed during save");
                    }
                }
                connection.commit();
            } catch(SQLException failure) { connection.rollback();throw failure; }
        } catch(SQLException failure) { throw new IllegalStateException("Could not persist guild grades",failure); }
    }

    public void deleteGradeNameFromSQL() {
        String query = "DELETE FROM `guildgrades` WHERE " +
                String.format("`guildid` = %d", getId());
        DatabaseManager.executeStatement(query);
    }

    public static Guild getGuildByGuildID(int guildID) {
        Guild guild = null;
        String query = String.format("SELECT * FROM guilds WHERE id = %d", guildID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int leaderID = rs.getInt("leaderid");
                int worldID = rs.getInt("worldid");
                int markBG = rs.getInt("markbg");
                int markBGColor = rs.getInt("markbgcolor");
                int mark = rs.getInt("mark");
                int markColor = rs.getInt("markcolor");
                Blob customemblem = rs.getBlob("customemblem");
                int maxMembers = rs.getInt("maxmembers");
                String notice = rs.getString("notice");
                int points = rs.getInt("points");
                int seasonPoints = rs.getInt("seasonpoints");
                int allianceID = rs.getInt("allianceid");
                int level = rs.getInt("level");
                int guildRank = rs.getInt("guildrank");
                int ggp = rs.getInt("ggp");
                boolean appliable = rs.getByte("appliable") != 0;
                int joinSetting = rs.getInt("joinsetting");
                int reqLevel = rs.getInt("reqlevel");
                int bbsNotice = rs.getInt("bbsNotice");
                int battleSP = rs.getInt("battleSp");

                guild = new Guild();
                guild.setId(id);
                guild.setName(name);
                guild.setLeaderID(leaderID);
                guild.setWorldID(worldID);
                guild.setMarkBg(markBG);
                guild.setMarkBgColor(markBGColor);
                guild.setMark(mark);
                guild.setMarkColor(markColor);
                if (customemblem != null) {
                    int length = (int) customemblem.length();
                    guild.setCustomEmblem(customemblem.getBytes(1, length));
                } else {
                    guild.setCustomEmblem(null);
                }
                guild.setMaxMembers(maxMembers);
                guild.setNotice(notice);
                guild.setHonorEXP(points);
                guild.setSeasonPoints(seasonPoints);
                guild.setAllianceID(allianceID);
                guild.setLevel(level);
                guild.setRank(guildRank);
                guild.setGgp(ggp);
                guild.setAppliable(appliable);
                guild.setJoinSetting(joinSetting);
                guild.setReqLevel(reqLevel);
                guild.setBattleSp(battleSP);

                Map<Integer, Tuple<String, Integer>> grades = guild.loadGuildGradesFromSQL();
                for (Map.Entry<Integer, Tuple<String, Integer>> grade : grades.entrySet()) {
                    switch (grade.getKey()) {
                        case 1:
                            guild.setGrade1(grade.getValue().getLeft());
                            guild.setGrade1Permission(grade.getValue().getRight());
                            break;
                        case 2:
                            guild.setGrade2(grade.getValue().getLeft());
                            guild.setGrade2Permission(grade.getValue().getRight());
                            break;
                        case 3:
                            guild.setGrade3(grade.getValue().getLeft());
                            guild.setGrade3Permission(grade.getValue().getRight());
                            break;
                        case 4:
                            guild.setGrade4(grade.getValue().getLeft());
                            guild.setGrade4Permission(grade.getValue().getRight());
                            break;
                        case 5:
                            guild.setGrade5(grade.getValue().getLeft());
                            guild.setGrade5Permission(grade.getValue().getRight());
                            break;
                    }
                }
                guild.setMembers(GuildMember.getGuildMembersFromSQLByGuildID(id));
                guild.setRequestors(GuildRequestor.getGuildRequestorListFromSQLByGuildID(id));
                guild.setSkills(GuildSkill.getGuildSkillsFromSQLByGuildID(id));
                guild.setBbsRecords(BBSRecord.getBBSRecordsFromSQLByGuildID(id));

                if (bbsNotice != 0) {
                    guild.setBbsNotice(BBSRecord.getBBSNoticeFromSQLByNoticeID(bbsNotice));
                }
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return guild;
    }

    public static Guild getGuildByGuildName(String guildName) {
        Guild guild = null;
        String query = String.format("SELECT * FROM guilds WHERE name = %s", guildName);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int leaderID = rs.getInt("leaderid");
                int worldID = rs.getInt("worldid");
                int markBG = rs.getInt("markbg");
                int markBGColor = rs.getInt("markbgcolor");
                int mark = rs.getInt("mark");
                int markColor = rs.getInt("markcolor");
                Blob customemblem = rs.getBlob("customemblem");
                int maxMembers = rs.getInt("maxmembers");
                String notice = rs.getString("notice");
                int points = rs.getInt("points");
                int seasonPoints = rs.getInt("seasonpoints");
                int allianceID = rs.getInt("allianceid");
                int level = rs.getInt("level");
                int guildRank = rs.getInt("guildrank");
                int ggp = rs.getInt("ggp");
                boolean appliable = rs.getByte("appliable") != 0;
                int joinSetting = rs.getInt("joinsetting");
                int reqLevel = rs.getInt("reqlevel");
                int bbsNotice = rs.getInt("bbsNotice");
                int battleSP = rs.getInt("battleSp");

                guild = new Guild();
                guild.setId(id);
                guild.setName(name);
                guild.setLeaderID(leaderID);
                guild.setWorldID(worldID);
                guild.setMarkBg(markBG);
                guild.setMarkBgColor(markBGColor);
                guild.setMark(mark);
                guild.setMarkColor(markColor);
                if (customemblem != null) {
                    int length = (int) customemblem.length();
                    guild.setCustomEmblem(customemblem.getBytes(1, length));
                } else {
                    guild.setCustomEmblem(null);
                }
                guild.setMaxMembers(maxMembers);
                guild.setNotice(notice);
                guild.setHonorEXP(points);
                guild.setSeasonPoints(seasonPoints);
                guild.setAllianceID(allianceID);
                guild.setLevel(level);
                guild.setRank(guildRank);
                guild.setGgp(ggp);
                guild.setAppliable(appliable);
                guild.setJoinSetting(joinSetting);
                guild.setReqLevel(reqLevel);
                guild.setBattleSp(battleSP);

                Map<Integer, Tuple<String, Integer>> grades = guild.loadGuildGradesFromSQL();
                for (Map.Entry<Integer, Tuple<String, Integer>> grade : grades.entrySet()) {
                    switch (grade.getKey()) {
                        case 1:
                            guild.setGrade1(grade.getValue().getLeft());
                            guild.setGrade1Permission(grade.getValue().getRight());
                            break;
                        case 2:
                            guild.setGrade2(grade.getValue().getLeft());
                            guild.setGrade2Permission(grade.getValue().getRight());
                            break;
                        case 3:
                            guild.setGrade3(grade.getValue().getLeft());
                            guild.setGrade3Permission(grade.getValue().getRight());
                            break;
                        case 4:
                            guild.setGrade4(grade.getValue().getLeft());
                            guild.setGrade4Permission(grade.getValue().getRight());
                            break;
                        case 5:
                            guild.setGrade5(grade.getValue().getLeft());
                            guild.setGrade5Permission(grade.getValue().getRight());
                            break;
                    }
                }
                guild.setMembers(GuildMember.getGuildMembersFromSQLByGuildID(id));
                guild.setRequestors(GuildRequestor.getGuildRequestorListFromSQLByGuildID(id));
                guild.setSkills(GuildSkill.getGuildSkillsFromSQLByGuildID(id));
                guild.setBbsRecords(BBSRecord.getBBSRecordsFromSQLByGuildID(id));

                if (bbsNotice != 0) {
                    guild.setBbsNotice(BBSRecord.getBBSNoticeFromSQLByNoticeID(bbsNotice));
                }
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return guild;
    }

    public Guild() {
        setGrade1("Master");
        setGrade1Permission(GuildGraderPermissionType.Everything.getVal());
        setGrade2("Officer");
        int grade2Permission = GuildGraderPermissionType.InviteMembers.getVal()
                + GuildGraderPermissionType.EditGreeting.getVal()
                + GuildGraderPermissionType.SetMemberRank.getVal()
                + GuildGraderPermissionType.EditEmblem.getVal()
                + GuildGraderPermissionType.KickMember.getVal()
                + GuildGraderPermissionType.ManageBulletinBoard.getVal()
                + GuildGraderPermissionType.AcceptNewMembers.getVal();
        setGrade2Permission(grade2Permission);
        setGrade3("Veteran");
        int grade3Permission = GuildGraderPermissionType.InviteMembers.getVal();
        setGrade3Permission(grade3Permission);
        setGrade4("Member");
        int grade4Permission = GuildGraderPermissionType.InviteMembers.getVal();
        setGrade4Permission(grade4Permission);
        setGrade5("Initiate");
        setGrade5Permission(GuildGraderPermissionType.None.getVal());
        setAppliable(true);
        setMaxMembers(10);
        setLevel(1);
        setName("Default guild");
    }

    //region Guild ID Function
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    //endregion

    //region Guild Name Function
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static boolean checkName(String sGuildName) {
        List<String> names = Guild.loadGuildNamesFromSQL();
        if (names.size() > 0) {
            for (String name : names) {
                if (name.equals(sGuildName)) {
                    return true;
                }
            }
        }
        return false;
    }
    //endregion

    //region Guild Level Function
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void addLevel() {
        setLevel(getLevel() + 1);
    }
    //endregion

    //region Guild World ID Function
    public int getWorldID() {
        return worldID;
    }

    public void setWorldID(int worldID) {
        this.worldID = worldID;
    }
    //endregion

    //region Guild Settings Function
    public boolean isAppliable() {
        return appliable;
    }

    public void setAppliable(boolean setState) {
        this.appliable = setState;
    }

    public int getJoinSetting() {
        return joinSetting;
    }

    public void setJoinSetting(int joinSetting) {
        this.joinSetting = joinSetting;
    }

    public int getReqLevel() {
        return reqLevel;
    }

    public void setReqLevel(int reqLevel) {
        this.reqLevel = reqLevel;
    }
    //endregion

    //region Guild Skills Function
    public Map<Integer, GuildSkill> getSkills() {
        return skills;
    }

    public void setSkills(Map<Integer, GuildSkill> skills) {
        this.skills = skills;
    }

    public void addGuildSkill(GuildSkill gs) {
        getSkills().put(gs.getSkillID(), gs);
    }

    public int getSpentSp() {
        int spentSp = 0;
        for (GuildSkill guildSkill : getSkills().values()) {
            if (GuildConstants.isGuildContentSkill(guildSkill.getSkillID())) {
                spentSp += guildSkill.getLevel();
            }
        }
        return spentSp;
    }

    public int getSpentBattleSp() {
        int spentSp = 0;
        for (int i = 91001022; i < (Tms282GuildRuntime.enabled()?91001026:91001025); i++) {
            GuildSkill gs = getSkills().getOrDefault(i, null);
            spentSp += gs == null ? 0 : gs.getLevel();
        }
        return spentSp;
    }

    public int getBattleSp() {
        if(Tms282GuildRuntime.enabled())return battleSp;
        return getLevel() + 4;
    }

    public void setBattleSp(int battleSp) {
        this.battleSp = battleSp;
    }

    public GuildSkill getSkillById(int skillID) {
        return getSkills().getOrDefault(skillID, null);
    }
    //endregion

    //region Guild Honor EXP Function
    public int getHonorEXP() {
        return points;
    }

    public void setHonorEXP(int points) {
        this.points = points;
    }

    public void addHonorEXP(int contribution) {
        setHonorEXP(getHonorEXP() + contribution);
        final double expDeceaseR = 0.6;
        boolean levelUp = false;
        while (getLevel() < GuildConstants.MAX_GUILD_LV && getHonorEXP() >= (int) (GuildConstants.getExpRequiredForNextGuildLevel(getLevel()) * expDeceaseR)) {
            setHonorEXP(getHonorEXP() - (int) (GuildConstants.getExpRequiredForNextGuildLevel(getLevel()) * expDeceaseR));
            setLevel(getLevel() + 1);
            broadcast(UserLocal.chatMsg(ChatType.Notice2, String.format("%s 已達到 %d 級！", getName(), getLevel())));
            levelUp = true;
        }
        if (getLevel() == GuildConstants.MAX_GUILD_LV) {
            setHonorEXP(0);
        }
        broadcast(WvsContext.guildResult(GuildResult.response_PointIncrease_Success(this)));
        if (levelUp) {
            updateGuildToSQL();
        }
    }
    //endregion

    //region Guild Points Function
    public int getGgp() {
        return ggp;
    }

    public void setGgp(int ggp) {
        this.ggp = ggp;
    }

    public void addGgp(int ggp) {
        setGgp(getGgp() + ggp);
        broadcast(WvsContext.guildResult(GuildResult.response_GGPSet_Success(this)));
    }
    //endregion

    //region Guild Contribution Function
    public void addContributionToChar(Char chr, int contribution) {
        GuildMember gm = getMemberByCharID(chr.getId());
        if (gm != null) {
            if (gm.getContributionIncTime() != null) {
                LocalDateTime localDateTime = gm.getContributionIncTime().toLocalDateTime();
                LocalDateTime now = LocalDateTime.now();
                if (localDateTime.getYear() != now.getYear() || localDateTime.getDayOfYear() < now.getDayOfYear()) {
                    gm.setContributionIncTime(FileTime.currentTime());
                    gm.setDayContribution(0);
                }
            }
            if (gm.getRemainingDayContribution() > 0) {
                int contributionIncrease = 0;
                if (gm.getDayContribution() + contribution > GuildConstants.MAX_DAY_CONTRIBUTION) {
                    contributionIncrease = GuildConstants.MAX_DAY_CONTRIBUTION - gm.getDayContribution();
                } else {
                    contributionIncrease = contribution;
                }
                gm.addContribution(this, contributionIncrease);
                gm.addIGP((int) (contribution * GuildConstants.IGP_PER_CONTRIBUTION));
                addGgp((int) (contributionIncrease * GuildConstants.GGP_PER_CONTRIBUTION));
                addHonorEXP(contributionIncrease);
                chr.write(WvsContext.incGPMessage((int) (contributionIncrease * GuildConstants.GGP_PER_CONTRIBUTION)));
            }
        }
    }
    //endregion

    //region Guild Max Member Function
    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
    //endregion

    //region Guild Member Function
    public List<GuildMember> getMembers() {
        return members;
    }

    public void setMembers(List<GuildMember> members) {
        this.members = members;
    }

    public GuildMember getMemberByCharID(int id) {
        return getMembers().stream().filter(gm -> gm.getCharID() == id).findAny().orElse(null);
    }

    /** Authority comes from the stored membership, never a client-supplied rank. */
    public boolean hasPermission(int characterId, int permission) {
        GuildMember member = getMemberByCharID(characterId);
        if (member == null || permission == 0) return false;
        if (characterId == getLeaderID() && member.getRank() == 1) return true;
        int granted = member.getRank()>=2 && member.getRank()<=10?gradePermission(member.getRank()):0;
        return (granted & permission) == permission;
    }

    /** A single durable write: memory changes only after the DB confirms the guild still exists. */
    public synchronized void saveNotice(String value) throws SQLException {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE guilds SET notice = ? WHERE id = ?")) {
            statement.setString(1, value);
            statement.setInt(2, getId());
            if (statement.executeUpdate() != 1) throw new SQLException("Guild notice row no longer exists");
            setNotice(value);
        }
    }

    public void addMember(GuildMember guildMember) {
        Tms282GuildRuntime.requireLegacyMembershipMutation();
        getMembers().add(guildMember);
        guildMember.setGuildID(getId());
        Char chr = Server.get().getWorld().getCharById(guildMember.getCharID());
        if (chr != null && chr.getGuild() == null) {
            chr.setGuild(this);
        }
        if (getLeaderID() == guildMember.getCharID()) {
            setLeader(guildMember);
            guildMember.setRank(1);
        } else {
            guildMember.setRank(5);
        }
        guildMember.updateGuildMemberToSQL();
    }

    public void addMember(Char chr) {
        addMember(new GuildMember(chr));
    }

    public void removeMember(int charID) {
        Tms282GuildRuntime.requireLegacyMembershipMutation();
        GuildMember guildMember = getMemberByCharID(charID);
        if (guildMember != null) {
            guildMember.deleteGuildMemberFromSQL();
            getMembers().removeIf(x -> x.getCharID() == charID);
        }
    }

    public int getLeaderID() {
        return leaderID;
    }

    public void setLeaderID(int leaderID) {
        this.leaderID = leaderID;
    }

    public void setLeader(GuildMember leader) {
        Tms282GuildRuntime.requireLegacyMembershipMutation();
        int oldGrade = leader.getRank();
        if (getLeaderID() != 0) {
            getMemberByCharID(getLeaderID()).setRank(oldGrade);
        }
        this.leaderID = leader.getCharID();
        leader.setRank(1);
    }

    public boolean isGuildMaster(Char chr) {
        return getLeaderID() == chr.getId();
    }

    public GuildMember getGuildLeader() {
        return getMemberByCharID(getLeaderID());
    }

    public int getRank() {
        return guildRank;
    }

    public void setRank(int guildRank) {
        this.guildRank = guildRank;
    }

    public void demote(GuildMember guildMember) {
        guildMember.setRank(Math.min(guildMember.getRank() + 1, 5));
    }

    public void promote(GuildMember guildMember) {
        guildMember.setRank(Math.max(guildMember.getRank() - 1, 1));
    }

    public int getMarkBg() {
        return markBg;
    }

    public void setMarkBg(int markBg) {
        this.markBg = markBg;
    }

    public int getMarkBgColor() {
        return markBgColor;
    }

    public void setMarkBgColor(int markBgColor) {
        this.markBgColor = markBgColor;
    }

    public int getMark() {
        return mark;
    }

    public void setMark(int mark) {
        this.mark = mark;
    }

    public int getMarkColor() {
        return markColor;
    }

    public void setMarkColor(int markColor) {
        this.markColor = markColor;
    }

    public List<GuildRequestor> getRequestors() {
        return requestors;
    }

    public void setRequestors(List<GuildRequestor> requestors) {
        this.requestors = requestors;
    }

    public void addRequestors(GuildRequestor guildRequestor) {
        guildRequestor.saveToSQL();
        getRequestors().add(guildRequestor);
    }

    public void removeRequestors(GuildRequestor guildRequestor) {
        if (guildRequestor != null) {
            guildRequestor.deleteGuildRequestorFromSQL();
            getRequestors().removeIf(x -> x.getCharID() == guildRequestor.getCharID());
        }
    }

    public void updateRequestors(GuildRequestor guildRequestor) {
        guildRequestor.saveToSQL();
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String notice) {
        this.notice = notice;
    }

    public int getSeasonPoints() {
        return seasonPoints;
    }

    public void setSeasonPoints(int seasonPoints) {
        this.seasonPoints = seasonPoints;
    }

    public Alliance getAlliance() {
        return alliance;
    }

    public void setAlliance(Alliance alliance) {
        this.alliance = alliance;
        setAllianceID(alliance.getId());
    }

    public int getAllianceID() {
        return allianceID;
    }

    public void setAllianceID(int allianceID) {
        this.allianceID = allianceID;
    }

    public int getAverageMemberLevel() {
        int size = getMembers().size();
        int averageLevel = 0;
        for (GuildMember gm : getMembers()) {
            averageLevel += gm.getLevel();
        }
        return averageLevel / size;
    }

    public void broadcast(OutPacket outPacket) {
        broadcast(outPacket, null);
    }

    public void broadcast(OutPacket outPacket, Char exceptChar) {
        Server.get().broadcastForGuild(outPacket, this, exceptChar);
    }

    public Collection<Char> getChars() {
        return Server.get().getCharsByGuildId(getId());
    }

    public Set<BBSRecord> getBbsRecords() {
        return bbsRecords;
    }

    public void setBbsRecords(Set<BBSRecord> bbsRecords) {
        this.bbsRecords = bbsRecords;
    }

    public void addBbsRecord(BBSRecord record) {
        getBbsRecords().add(record);
        record.setIdForBbs(getBbsRecords().size()); // whatevs
    }

    public BBSRecord getRecordByID(int id) {
        BBSRecord record;
        if (id == 0) {
            record = getBbsNotice();
        } else {
            record = getBbsRecords().stream().filter(r -> r.getIdForBbs() == id).findAny().orElse(null);
        }
        return record;
    }

    public void removeRecord(BBSRecord record) {
        getBbsRecords().remove(record);
        if (record.getIdForBbs() == 0) {
            this.bbsNotice = null;
        } else if (record.getIdForBbs() != 0) {
            int i = 1;
            for (BBSRecord r : getBbsRecords()) {
                r.setIdForBbs(i++);
            }
        }
    }

    public BBSRecord getBbsNotice() {
        return bbsNotice;
    }

    public void setBbsNotice(BBSRecord bbsNotice) {
        this.bbsNotice = bbsNotice;
    }

    //endregion

    public void encodeForRemote(OutPacket outPacket) {
        outPacket.encodeInt(getId());
        outPacket.encodeString(getName());
        outPacket.encodeShort(getMarkBg());
        outPacket.encodeByte(getMarkBgColor());
        outPacket.encodeShort(getMark());
        outPacket.encodeByte(getMarkColor());
        outPacket.encodeInt(getCustomEmblem() != null ? getId() : 0);
        outPacket.encodeInt(getCustomEmblem() != null ? 1 : 0);
    }

    public void encodeGuildGrades(OutPacket outPacket) {
        outPacket.encodeString(getGrade1());
        outPacket.encodeInt(getGrade1Permission());
        outPacket.encodeString(getGrade2());
        outPacket.encodeInt(getGrade2Permission());
        outPacket.encodeString(getGrade3());
        outPacket.encodeInt(getGrade3Permission());
        outPacket.encodeString(getGrade4());
        outPacket.encodeInt(getGrade4Permission());
        outPacket.encodeString(getGrade5());
        outPacket.encodeInt(getGrade5Permission());
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getId());
        outPacket.encodeString(getName());

        encodeGuildGrades(outPacket);

        outPacket.encodeShort(getMembers().size());
        getMembers().forEach(gm -> outPacket.encodeInt(gm.getCharID()));
        getMembers().forEach(gm -> gm.encode(outPacket));

        List<GuildRequestor> onlineRequestors = getRequestors().stream().filter(c -> c.isOnline()).toList();
        int nOnlineRequestors = onlineRequestors.size();
        outPacket.encodeShort(nOnlineRequestors);
        onlineRequestors.forEach(guildRequestor -> outPacket.encodeInt(guildRequestor.getCharID()));
        onlineRequestors.forEach(guildRequestor -> guildRequestor.encode(outPacket));

        outPacket.encodeInt(getMaxMembers());
        outPacket.encodeShort(getMarkBg());
        outPacket.encodeByte(getMarkBgColor());
        outPacket.encodeShort(getMark());
        outPacket.encodeByte(getMarkColor());
        outPacket.encodeString(getNotice() != null ? getNotice() : "Chào möng các b¢n «ªn vÜi Bang hØi " + getName() + ".");
        outPacket.encodeInt(getHonorEXP());
        outPacket.encodeInt(getSeasonPoints());
        outPacket.encodeInt(getAllianceID());
        outPacket.encodeByte(getLevel());
        outPacket.encodeInt(getRank());
        outPacket.encodeInt(getGgp());
        outPacket.encodeInt(0); //idk
        outPacket.encodeByte(1);
        outPacket.encodeLong(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        outPacket.encodeShort(getSkills().size());
        getSkills().forEach((id, skill) -> {
            outPacket.encodeInt(id);
            outPacket.encode(skill);
        });
        outPacket.encodeByte(false);
        if (getCustomEmblem() != null) {
            outPacket.encodeInt(getCustomEmblem().length);
            for (byte b : getCustomEmblem()) {
                outPacket.encodeByte(b);
            }
        } else {
            outPacket.encodeInt(0);
        }
        outPacket.encodeInt(0);
    }

    public String getGrade1() {
        return grade1;
    }

    public void setGrade1(String grade1) {
        this.grade1 = grade1;
    }

    public String getGrade2() {
        return grade2;
    }

    public void setGrade2(String grade2) {
        this.grade2 = grade2;
    }

    public String getGrade3() {
        return grade3;
    }

    public void setGrade3(String grade3) {
        this.grade3 = grade3;
    }

    public String getGrade4() {
        return grade4;
    }

    public void setGrade4(String grade4) {
        this.grade4 = grade4;
    }

    public String getGrade5() {
        return grade5;
    }

    public void setGrade5(String grade5) {
        this.grade5 = grade5;
    }

    public int getGrade1Permission() {
        return grade1Permission;
    }

    public void setGrade1Permission(int grade1Permission) {
        this.grade1Permission = grade1Permission;
    }

    public int getGrade2Permission() {
        return grade2Permission;
    }

    public void setGrade2Permission(int grade2Permission) {
        this.grade2Permission = grade2Permission;
    }

    public int getGrade3Permission() {
        return grade3Permission;
    }

    public void setGrade3Permission(int grade3Permission) {
        this.grade3Permission = grade3Permission;
    }

    public int getGrade4Permission() {
        return grade4Permission;
    }

    public void setGrade4Permission(int grade4Permission) {
        this.grade4Permission = grade4Permission;
    }

    public int getGrade5Permission() {
        return grade5Permission;
    }

    public void setGrade5Permission(int grade5Permission) {
        this.grade5Permission = grade5Permission;
    }

    public void addToBaseStatCache(int skillID) {
        if (skillID == GuildConstants.GUILD_ON_FIRE_I
                || skillID == GuildConstants.GUILD_ON_FIRE_II
                || skillID == GuildConstants.GUILD_ON_FIRE_III
                || skillID == GuildConstants.WELL_ROUNDED) {
            for (Char chr : getChars()) {
                //chr.initBaseStats();
                // TODO?
            }
        }
    }
}
