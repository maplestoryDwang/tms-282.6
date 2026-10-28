package net.swordie.ms;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BannedMachines {

    public static List<byte[]> getBannedMachinesFromSQL() {
        List<byte[]> list = new ArrayList<>();
        String query = "SELECT * FROM bannedmachines;";

        try (Connection connection = DatabaseManager.getConnection();
             Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {

            while (rs.next()) {
                byte[] machineid = Util.getByteArrayByString(rs.getString("machineid"));
                list.add(machineid);
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return list;
    }

    public static void addBannedMachine(byte[] machineID) {
        if (machineID == null) {
            return;
        }
        Server.get().getBannedMacs().add(machineID);
        String query = "INSERT INTO `bannedmachines` (" +
                "`machineid`, " +
                "`addeddate` " +
                ") VALUES (" +
                DatabaseManager.getSQLStringSyntax(false, "", Util.readableByteArray(machineID), false) +
                DatabaseManager.getSQLStringSyntax(false, "", FileTime.currentTime(), true) +
                ");";
        DatabaseManager.executeStatementReturnID(query);
    }
}
