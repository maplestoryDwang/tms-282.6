package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Macro {

    private long id;
    private int charID;
    private String name;
    private boolean muted;
    private int[] skills = new int[3];

    public static List<Macro> getMarcosFromSQLByCharID(int charID) {
        List<Macro> macros = new ArrayList<>();
        String query = "SELECT m.id AS macro_id, m.muted, m.name, " +
                "ms.ordercol, ms.skillid " +
                "FROM macros m " +
                "LEFT JOIN macroskills ms ON m.id = ms.macroid " +
                "WHERE m.charid = ?";
        if (net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3)
            query += " ORDER BY m.id, ms.ordercol"; // New macro rows are inserted in client slot order.

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);

            try (ResultSet rs = ps.executeQuery()) {
                Map<Long, Macro> macroMap = new HashMap<>();

                while (rs.next()) {
                    long macroId = rs.getLong("macro_id");
                    if (!macroMap.containsKey(macroId)) {
                        Macro macro = new Macro();
                        macro.setId(macroId);
                        macro.setMuted(rs.getByte("muted") != 0);
                        macro.setName(rs.getString("name"));
                        macro.setSkills(new int[3]); // Khởi tạo mảng kỹ năng
                        macroMap.put(macroId, macro);
                        macros.add(macro);
                    }
                    int orderCol = rs.getInt("ordercol");
                    int skillID = rs.getInt("skillid");
                    if (orderCol >= 0 && orderCol < 3) {
                        macroMap.get(macroId).getSkills()[orderCol] = skillID;
                    }
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return macros;
    }

    public void saveToSQL(int charID) {
        if (getId() == 0) {
            String query = "INSERT INTO `macros` (" +
                    "`charid`, " +
                    "`muted`, " +
                    "`name` " +
                    ") VALUES (" +
                    String.format("%d, ", charID) +
                    String.format("%d, ", isMuted() ? 1 : 0) +
                    String.format("'%s' ", DatabaseManager.getStringFilter(getName())) +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);

            for (int i = 0; i < getSkills().length; i++) {
                query = "INSERT INTO `macroskills` (" +
                        "`ordercol`, " +
                        "`skillid`, " +
                        "`macroid` " +
                        ") VALUES (" +
                        String.format("%d, ", i) +
                        String.format("%d, ", getSkills()[i]) +
                        String.format("%d ", id) +
                        ");";
                DatabaseManager.executeStatement(query);
            }
        } else {

            String query = "UPDATE macros SET " +
                    String.format("muted = %d, ", isMuted() ? 1 : 0) +
                    String.format("name = '%s' ", DatabaseManager.getStringFilter(getName())) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);

            for (int i = 0; i < 3; i++) {
                query = "UPDATE macroskills SET " +
                        String.format("skillid = %d ", getSkills()[i]) +
                        String.format("WHERE macroid = %d AND ordercol = %d;", getId(), i);
                DatabaseManager.executeStatement(query);
            }
        }
    }

    public void deleteFromSQL() {
        Connection con = null;
        try {
            con = DatabaseManager.getConnection();
            con.setAutoCommit(false);
            deleteFromSQL(con);
            con.commit();
            setId(0);
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception rollBack) {
                    DataPrinter.send(DataPrinter.HIKARICP_ERROR, rollBack);
                }
            }
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        } finally {
            if (con != null) {
                try {
                    con.close();
                } catch (Exception e) {
                    DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
                }
            }
        }
    }

    public void deleteFromSQL(Connection con) throws SQLException {
        String macroSkillsQuery = "DELETE FROM `macroskills` WHERE `macroid` = ?";
        try (PreparedStatement ps = con.prepareStatement(macroSkillsQuery)) {
            ps.setLong(1, getId());
            ps.executeUpdate();
        }
        String macrosQuery = "DELETE FROM `macros` WHERE `id` = ?";
        try (PreparedStatement ps = con.prepareStatement(macrosQuery)) {
            ps.setLong(1, getId());
            ps.executeUpdate();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int[] getSkills() {
        return skills;
    }

    public void setSkills(int[] skills) {
        this.skills = skills;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void setSkillAtPos(int pos, int skillID) {
        if (pos >= 0 && pos < 3) {
            getSkills()[pos] = skillID;
        }
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeString(getName());
        outPacket.encodeByte(isMuted());
        for (int i : getSkills()) {
            outPacket.encodeInt(i);
        }
    }
}
