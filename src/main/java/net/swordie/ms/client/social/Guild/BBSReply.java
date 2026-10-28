package net.swordie.ms.client.social.Guild;

import net.swordie.ms.connection.Encodable;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BBSReply implements Encodable {
    private int id;
    private int idForReply;
    private int creatorID;
    private FileTime creationDate;
    private String msg;
    private int recordID;

    public static List<BBSReply> getBBSReplyFromSQLByRecordID(int recordID) {
        List<BBSReply> bbsReplyList = new ArrayList<>();
        String query = String.format("SELECT * FROM bbs_replies WHERE recordid = %d", recordID);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = net.swordie.ms.connection.hikariCP.DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                int idForReply = rs.getInt("idforreply");
                int creatorID = rs.getInt("creatorid");
                FileTime creationDate = DatabaseManager.getFileTimeFromString(rs.getString("creationdate"));
                String msg = rs.getString("msg");

                BBSReply bbsReply = new BBSReply();
                bbsReply.setId(id);
                bbsReply.setIdForReply(idForReply);
                bbsReply.setCreatorID(creatorID);
                bbsReply.setCreationDate(creationDate);
                bbsReply.setMsg(msg);
                bbsReply.setRecordID(recordID);

                bbsReplyList.add(bbsReply);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            net.swordie.ms.connection.hikariCP.DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return bbsReplyList;
    }

    public void updateBBSReplyToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `bbs_replies` (" +
                    "`idforreply`, " +
                    "`creatorid`, " +
                    "`creationdate`, " +
                    "`msg`, " +
                    "`recordid` " +
                    ") VALUES (" +
                    String.format("%d, ", getIdForReply()) +
                    String.format("%d, ", getCreatorID()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getCreationDate(), false) +
                    String.format("'%s', ", DatabaseManager.getStringFilter(getMsg())) +
                    String.format("%d ", getRecordID()) +
                    ");";
            //int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE bbs_replies SET " +
                    String.format("idforreply = %d, ", getIdForReply()) +
                    String.format("creatorid = %d, ", getCreatorID()) +
                    DatabaseManager.getSQLStringSyntax(true, "creationdate", getCreationDate(), false) +
                    String.format("msg = '%s', ", DatabaseManager.getStringFilter(getMsg())) +
                    String.format("recordid = %d ", getRecordID()) +
                    String.format("WHERE id = %d;", getId());
            //DatabaseManager.executeStatement(query);
        }
    }

    public void deleteBBSReplyFromSQL() {
        String query = "DELETE FROM `bbs_replies` WHERE " +
                String.format("`id` = %d", getId());
        //DatabaseManager.executeStatement(query);
        setId(0);
    }

    public BBSReply() {
    }

    public BBSReply(int creatorID, FileTime creationDate, String msg) {
        this.creatorID = creatorID;
        this.creationDate = creationDate;
        this.msg = msg;
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getId());
        outPacket.encodeInt(getCreatorID());
        outPacket.encodeFT(getCreationDate());
        outPacket.encodeString(getMsg());
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

    public FileTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(FileTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getIdForReply() {
        return idForReply;
    }

    public void setIdForReply(int idForReply) {
        this.idForReply = idForReply;
    }

    public int getRecordID() {
        return recordID;
    }

    public void setRecordID(int recordID) {
        this.recordID = recordID;
    }
}
