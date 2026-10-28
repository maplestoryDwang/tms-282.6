package net.swordie.ms.client.character.hexa;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.loaders.Etc.HexaCore.HexaCore;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.container.Tuple;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class HexaStat {

    private int id;
    private int charId;
    private int preset;
    private int coreId;
    private Map<Integer, Tuple<HexaCore.HexaStatType, Integer>> stats = new HashMap<>();

    public static Map<Integer, HexaStat> getHexaStatsFromSQLByCharID(int charID) {
        Map<Integer, HexaStat> hexaStats = new HashMap<>();
        String query = "SELECT * FROM hexastats WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HexaStat hexaStat = new HexaStat();
                    hexaStat.setId(rs.getInt("id"));
                    hexaStat.setCharId(rs.getInt("charid"));
                    hexaStat.setPreset(rs.getInt("index"));
                    hexaStat.setCoreId(rs.getInt("coreid"));
                    int stat0 = rs.getInt("stat0");
                    int level0 = rs.getInt("level0");
                    int stat1 = rs.getInt("stat1");
                    int level1 = rs.getInt("level1");
                    int stat2 = rs.getInt("stat2");
                    int level2 = rs.getInt("level2");
                    hexaStat.getStats().put(0, new Tuple<>(HexaCore.HexaStatType.getValByType(stat0), level0));
                    hexaStat.getStats().put(1, new Tuple<>(HexaCore.HexaStatType.getValByType(stat1), level1));
                    hexaStat.getStats().put(2, new Tuple<>(HexaCore.HexaStatType.getValByType(stat2), level2));
                    hexaStats.put(hexaStat.getPreset(), hexaStat);
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return hexaStats;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `hexastats` (" +
                    "`charid`, " +
                    "`index`, " +
                    "`coreid`, " +
                    "`stat0`, " +
                    "`level0`, " +
                    "`stat1`, " +
                    "`level1`, " +
                    "`stat2`, " +
                    "`level2` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getPreset()) +
                    String.format("%d, ", getCoreId()) +
                    String.format("%d, ", getStatByIndex(0)) +
                    String.format("%d, ", getLevelByIndex(0)) +
                    String.format("%d, ", getStatByIndex(1)) +
                    String.format("%d, ", getLevelByIndex(1)) +
                    String.format("%d, ", getStatByIndex(2)) +
                    String.format("%d ", getLevelByIndex(2)) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE `hexastats` SET " +
                    String.format("`charid` = %d, ", getCharId()) +
                    String.format("`index` = %d, ", getPreset()) +
                    String.format("`coreid` = %d, ", getCoreId()) +
                    String.format("`stat0` = %d, ", getStatByIndex(0)) +
                    String.format("`level0` = %d, ", getLevelByIndex(0)) +
                    String.format("`stat1` = %d, ", getStatByIndex(1)) +
                    String.format("`level1` = %d, ", getLevelByIndex(1)) +
                    String.format("`stat2` = %d, ", getStatByIndex(2)) +
                    String.format("`level2` = %d ", getLevelByIndex(2)) +
                    String.format("WHERE `id` = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public HexaStat() {}

    public HexaStat(int coreId, int preset) {
        this.coreId = coreId;
        this.preset = preset;
    }

    public int getStatByIndex(int index) {
        if (getStats().get(index) == null || getStats().get(index).getLeft() == null) {
            return -1;
        }
        return getStats().get(index).getLeft().ordinal();
    }

    public int getLevelByIndex(int index) {
        if (getStats().get(index) == null) {
            return 0;
        }
        return getStats().get(index).getRight();
    }

    public int getPreset() {
        return preset;
    }

    public void setPreset(int preset) {
        this.preset = preset;
    }

    public int getCoreId() {
        return coreId;
    }

    public void setCoreId(int coreId) {
        this.coreId = coreId;
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

    public Map<Integer, Tuple<HexaCore.HexaStatType, Integer>> getStats() {
        return stats;
    }

    public void setStats(Map<Integer, Tuple<HexaCore.HexaStatType, Integer>> stats) {
        this.stats = stats;
    }
}
