package net.swordie.ms.client.character.skills;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;

public class BuffFavorite {

    public static final int MAX_ROW = 10;
    public int id;
    private int charid;
    private int preset;
    private int index;
    private int skillID;

    public static Int2ObjectMap<Int2IntMap> getBuffFavoritesByCharID(int charID) {
        Int2ObjectMap<Int2IntMap> out = new Int2ObjectOpenHashMap<>();

        String sql = "SELECT preset, `index`, skillid FROM bufffavorites WHERE charid=? AND skillid<>0";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int preset  = rs.getInt("preset");
                    int index   = rs.getInt("index");
                    int skillId = rs.getInt("skillid");

                    if (preset < 0 || preset >= MAX_ROW || index < 0 || index >= MAX_ROW) continue;

                    Int2IntMap m = out.get(preset);
                    if (m == null) {
                        m = new Int2IntOpenHashMap();
                        out.put(preset, m);
                    }
                    m.put(index, skillId);
                }
            }
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
        return out;
    }

    public static void saveToSQL(int charId, Int2ObjectMap<Int2IntMap> favorites) {
        final String upsertSql = "INSERT INTO bufffavorites (charid, preset, `index`, skillid) VALUES (?,?,?,?)  ON DUPLICATE KEY UPDATE skillid=VALUES(skillid)";
        final String updateZeroSql = "UPDATE bufffavorites SET skillid=0 WHERE charid=? AND preset=? AND `index`=?";
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement psUpsert = con.prepareStatement(upsertSql);
                 PreparedStatement psZero   = con.prepareStatement(updateZeroSql)) {
                for (var e : favorites.int2ObjectEntrySet()) {
                    int preset = e.getIntKey();
                    Int2IntMap map = e.getValue();
                    if (map == null || map.isEmpty()) continue;
                    for (var it = map.int2IntEntrySet().iterator(); it.hasNext();) {
                        var en = it.next();
                        int index   = en.getIntKey();
                        int skillId = en.getIntValue();
                        psUpsert.setInt(1, charId);
                        psUpsert.setInt(2, preset);
                        psUpsert.setInt(3, index);
                        psUpsert.setInt(4, skillId);
                        psUpsert.addBatch();
                    }
                }
                for (int preset = 0; preset < MAX_ROW; preset++) {
                    Int2IntMap map = favorites.get(preset);
                    for (int index = 0; index < MAX_ROW; index++) {
                        boolean hasSkill = map != null && map.containsKey(index);
                        if (!hasSkill) {
                            psZero.setInt(1, charId);
                            psZero.setInt(2, preset);
                            psZero.setInt(3, index);
                            psZero.addBatch();
                        }
                    }
                }
                psUpsert.executeBatch();
                psZero.executeBatch();
            }
            con.commit();
        } catch (SQLException e) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, e);
        }
    }


    public BuffFavorite() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharId() {
        return charid;
    }

    public void setCharId(int charid) {
        this.charid = charid;
    }

    public int getPreset() {
        return preset;
    }

    public void setPreset(int preset) {
        this.preset = preset;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }
}
