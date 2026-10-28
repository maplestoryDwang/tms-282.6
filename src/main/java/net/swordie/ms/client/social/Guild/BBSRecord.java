package net.swordie.ms.client.social.Guild;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BBSRecord {

    private int id;
    private int idForBbs; // just the number in the list
    private int creatorID;
    private String subject;
    private String msg;
    private FileTime creationDate;
    private int icon;
    private List<BBSReply> replies = new ArrayList<>();
    private int guildID;

    public static Set<BBSRecord> getBBSRecordsFromSQLByGuildID(int guildID) {
        Set<BBSRecord> bbsRecordSet = new HashSet<>();
        String query = String.format("SELECT * FROM bbs_records WHERE guildid = %d", guildID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                int idForBbs = rs.getInt("idforbbs");
                int creatorID = rs.getInt("creatorid");
                String subject = rs.getString("subject");
                String msg = rs.getString("msg");
                FileTime creationdate = DatabaseManager.getFileTimeFromString(rs.getString("creationdate"));
                int icon = rs.getInt("icon");
                BBSRecord bbsRecord = new BBSRecord();
                bbsRecord.setId(id);
                bbsRecord.setIdForBbs(idForBbs);
                bbsRecord.setCreatorID(creatorID);
                bbsRecord.setSubject(subject);
                bbsRecord.setMsg(msg);
                bbsRecord.setCreationDate(creationdate);
                bbsRecord.setIcon(icon);
                bbsRecord.setGuildID(guildID);
                bbsRecord.setReplies(BBSReply.getBBSReplyFromSQLByRecordID(id));
                bbsRecordSet.add(bbsRecord);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            net.swordie.ms.connection.hikariCP.DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return bbsRecordSet;
    }

    public static BBSRecord getBBSNoticeFromSQLByNoticeID(int noticeID) {
        BBSRecord bbsRecord = null;
        String query = String.format("SELECT * FROM bbs_records WHERE id = %d", noticeID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int idForBbs = rs.getInt("idforbbs");
                int creatorID = rs.getInt("creatorid");
                String subject = rs.getString("subject");
                String msg = rs.getString("msg");
                FileTime creationdate = DatabaseManager.getFileTimeFromString(rs.getString("creationdate"));
                int icon = rs.getInt("icon");
                int guildID = rs.getInt("guildid");

                bbsRecord = new BBSRecord();
                bbsRecord.setId(noticeID);
                bbsRecord.setIdForBbs(idForBbs);
                bbsRecord.setCreatorID(creatorID);
                bbsRecord.setSubject(subject);
                bbsRecord.setMsg(msg);
                bbsRecord.setCreationDate(creationdate);
                bbsRecord.setIcon(icon);
                bbsRecord.setGuildID(guildID);
                bbsRecord.setReplies(BBSReply.getBBSReplyFromSQLByRecordID(noticeID));
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            net.swordie.ms.connection.hikariCP.DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return bbsRecord;
    }

    public void updateBBSRecordToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `bbs_records` (" +
                    "`idforbbs`, " +
                    "`creatorid`, " +
                    "`subject`, " +
                    "`msg`, " +
                    "`creationdate`, " +
                    "`icon`, " +
                    "`guildid` " +
                    ") VALUES (" +
                    String.format("%d, ", getIdForBbs()) +
                    String.format("%d, ", getCreatorID()) +
                    String.format("'%s', ", DatabaseManager.getStringFilter(getSubject())) +
                    String.format("'%s ', ", getMsg()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getCreationDate(), false) +
                    String.format("%d, ", getIcon()) +
                    String.format("%d ", getGuildID()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE bbs_records SET " +
                    String.format("idforbbs = %d, ", getIdForBbs()) +
                    String.format("creatorid = %d, ", getCreatorID()) +
                    String.format("subject = '%s', ", DatabaseManager.getStringFilter(getSubject())) +
                    String.format("msg = '%s', ", getMsg()) +
                    DatabaseManager.getSQLStringSyntax(true, "creationdate", getCreationDate(), false) +
                    String.format("icon = %d, ", getIcon()) +
                    String.format("guildid = %d ", getGuildID()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteBBSRecordFromSQL() {
        for (BBSReply bbsReply : getReplies()) {
            bbsReply.deleteBBSReplyFromSQL();
        }
        String query = "DELETE FROM `bbs_records` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public BBSRecord() {
    }

    public BBSRecord(int creatorID, String subject, String msg, FileTime creationDate, int icon) {
        this.creatorID = creatorID;
        this.subject = subject;
        this.msg = msg;
        this.creationDate = creationDate;
        this.icon = icon;
    }

    public void encodeForPagesLoad(OutPacket outPacket) {
        outPacket.encodeInt(getIdForBbs());
        outPacket.encodeInt(getCreatorID());
        outPacket.encodeString(getSubject());
        outPacket.encodeFT(getCreationDate());
        outPacket.encodeInt(getIcon());
        outPacket.encodeInt(getReplies().size());
    }

    public void encodeForRecordLoad(OutPacket outPacket) {
        outPacket.encodeInt(getIdForBbs());
        outPacket.encodeInt(getCreatorID());
        outPacket.encodeFT(getCreationDate());
        outPacket.encodeString(getSubject());
        outPacket.encodeString(getMsg());
        outPacket.encodeInt(getIcon());
        outPacket.encodeInt(getReplies().size());
        for (BBSReply reply : getReplies()) {
            outPacket.encode(reply);
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(int creatorID) {
        this.creatorID = creatorID;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public FileTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(FileTime creationDate) {
        this.creationDate = creationDate;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public List<BBSReply> getReplies() {
        return replies;
    }

    public void setReplies(List<BBSReply> replies) {
        this.replies = replies;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getIdForBbs() {
        return idForBbs;
    }

    public void setIdForBbs(int idForBbs) {
        this.idForBbs = idForBbs;
    }

    public void addReply(BBSReply reply) {
        getReplies().add(reply);
        reply.setIdForReply(getReplies().size());
    }

    public void removeReply(BBSReply reply) {
        getReplies().remove(reply);
        int i = 1;
        for (BBSReply r : getReplies()) {
            r.setIdForReply(i++);
        }
    }

    public BBSReply getReplyById(int id) {
        return getReplies().stream().filter(reply -> reply.getId() == id).findAny().orElse(null);
    }

    public int getGuildID() {
        return guildID;
    }

    public void setGuildID(int guildID) {
        this.guildID = guildID;
    }
}
