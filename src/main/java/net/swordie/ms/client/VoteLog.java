package net.swordie.ms.client;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.enums.AccountType;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.XMLApi;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class VoteLog {

    public VoteLog() {
    }

    public VoteLog(String username) {
        this.username = username;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.time_1 = FileTime.fromDate(now.toLocalDateTime());
        this.time_2 = now.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.hasGain = 1;
    }

    private int id;

    private FileTime time_1;

    private String time_2;

    private String username;

    private byte hasGain;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public FileTime getTime_1() {
        return time_1;
    }

    public void setTime_1(FileTime time) {
        this.time_1 = time;
    }

    public String getTime_2() {
        return time_2;
    }

    public void setTime_2(String time_2) {
        this.time_2 = time_2;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte hasGain() {
        return hasGain;
    }

    public void setGain(byte hasGain) {
        this.hasGain = hasGain;
    }

    public void update() {
        setTime_1(FileTime.currentTime());
        String query = "UPDATE vote_logs SET " +
                DatabaseManager.getSQLStringSyntax(true, "time_1", getTime_1(), true) +
                String.format("WHERE id = %d;", getId());
        DatabaseManager.executeStatement(query);
    }

    public static List<VoteLog> getVoteLogsFromSQL() {
        List<VoteLog> voteLogs = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String query = "SELECT * FROM vote_logs WHERE " +
                String.format("`hasGain` = %d AND `time_2` = '%s';", 0, now.format(dtf));

        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                FileTime time_1 = DatabaseManager.getFileTimeFromString(rs.getString("time_1"));
                String time_2 = rs.getString("time_2");
                String username = rs.getString("username");
                byte gain = rs.getByte("hasGain");

                VoteLog voteLog = new VoteLog();
                voteLog.setId(id);
                voteLog.setTime_1(time_1);
                voteLog.setTime_2(time_2);
                voteLog.setUsername(username);
                voteLog.setGain(gain);

                voteLogs.add(voteLog);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return voteLogs;
    }

    public static List<VoteLog> getVoteLogFromSQLByUsername(String username) {
        List<VoteLog> voteLogs = new ArrayList<>();
        String query = "SELECT * FROM vote_logs WHERE " + String.format("`username` = '%s';", username);
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = DatabaseManager.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("id");
                FileTime time_1 = DatabaseManager.getFileTimeFromString(rs.getString("time_1"));
                String time_2 = rs.getString("time_2");
                byte gain = rs.getByte("hasGain");

                VoteLog voteLog = new VoteLog();
                voteLog.setId(id);
                voteLog.setTime_1(time_1);
                voteLog.setTime_2(time_2);
                voteLog.setUsername(username);
                voteLog.setGain(gain);

                voteLogs.add(voteLog);
            }
            connection.close();
            st.close();
            rs.close();
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        } finally {
            DatabaseManager.closeQuery(rs, st, connection, query);
        }
        return voteLogs;
    }

    public void updateVoteLogToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `vote_logs` (" +
                    "`time_1`, " +
                    "`time_2`, " +
                    "`username`, " +
                    "`hasGain`" +
                    ") VALUES (" +
                    DatabaseManager.getSQLStringSyntax(false, "", getTime_1(), false) +
                    String.format("'%s', ", getTime_2()) +
                    String.format("'%s', ", getUsername()) +
                    String.format("%d", hasGain()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE vote_logs SET " +
                    DatabaseManager.getSQLStringSyntax(true, "time_1", getTime_1(), false) +
                    String.format("time_2 = %s, ", getTime_2()) +
                    DatabaseManager.getSQLStringSyntax(true, "username", getUsername(), false) +
                    String.format("hasGain = %d ", hasGain()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void updateTimeLogToSQL() {
        String query = "UPDATE vote_logs SET " +
                DatabaseManager.getSQLStringSyntax(true, "time_1", getTime_1(), false) +
                String.format("time_2 = %s, ", getTime_2()) +
                DatabaseManager.getSQLStringSyntax(true, "username", getUsername(), false) +
                String.format("WHERE username = %d;", getId());
        DatabaseManager.executeStatement(query);
    }

    public void deleteVoteLogFromSQL() {
        String query = "DELETE FROM `vote_logs` WHERE " +
                String.format("`id` = %d", getId());
        net.swordie.ms.connection.hikariCP.DatabaseManager.executeStatement(query);
        setId(0);
    }
}
