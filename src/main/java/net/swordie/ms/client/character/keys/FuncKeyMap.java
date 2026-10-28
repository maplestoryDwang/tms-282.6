package net.swordie.ms.client.character.keys;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FuncKeyMap {

    private static final int MAX_KEYBINDS = 89;
    private static final int MAX_COMBINATION = 10;
    private int id;
    private int charID;
    private int preset;
    private List<Keymapping> keymap = new ArrayList<>();

    public static Map<Integer, FuncKeyMap> getFuncKeyMapsFromSQLByCharID(int charID) {
        Map<Integer, FuncKeyMap> funcKeyMaps = new HashMap<>();
        String query = "SELECT fkm.id AS fkm_id, fkm.charid, fkm.ord, " +
                "km.id AS km_id, km.idx, km.type, km.val " +
                "FROM funckeymap fkm " +
                "LEFT JOIN keymaps km ON fkm.id = km.fkmapid " +
                "WHERE fkm.charid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, FuncKeyMap> map = new HashMap<>();
                while (rs.next()) {
                    int funcKeyMapId = rs.getInt("fkm_id");

                    // Nếu FuncKeyMap chưa được thêm vào map, tạo một đối tượng mới
                    if (!map.containsKey(funcKeyMapId)) {
                        FuncKeyMap funcKeyMap = new FuncKeyMap();
                        funcKeyMap.setId(funcKeyMapId);
                        funcKeyMap.setCharID(rs.getInt("charid"));
                        funcKeyMap.setPreset(rs.getInt("ord"));
                        funcKeyMap.setKeymap(new ArrayList<>()); // Tạo một ArrayList rỗng để lưu trữ các Keymapping
                        map.put(funcKeyMapId, funcKeyMap);
                        funcKeyMaps.put(funcKeyMap.getPreset(), funcKeyMap);
                    }

                    // Tải dữ liệu Keymapping và thêm vào FuncKeyMap tương ứng.
                    if (rs.getObject("km_id") != null) {
                        Keymapping keymapping = new Keymapping();
                        keymapping.setId(rs.getInt("km_id"));
                        keymapping.setCharId(rs.getInt("charid"));
                        keymapping.setIndex(rs.getInt("idx"));
                        keymapping.setType(rs.getByte("type"));
                        keymapping.setVal(rs.getInt("val"));

                        // Thêm Keymapping vào list của FuncKeyMap
                        map.get(funcKeyMapId).getKeymap().add(keymapping);
                    }
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return funcKeyMaps;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `funckeymap` (" +
                    "`charid`, " +
                    "`ord` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d ", getPreset()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE funckeymap SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("ord = %d ", getPreset()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);

            if (!getKeymap().isEmpty()) {
                for (Keymapping keyMapping : getKeymap()) {
                    keyMapping.updateKeyMappingToSQL(getId());
                }
            }
        }
    }

    public FuncKeyMap() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
        for (Keymapping keymapping : getKeymap()) {
            keymapping.setCharId(charID);
        }
    }

    public int getPreset() {
        return preset;
    }

    public void setPreset(int preset) {
        this.preset = preset;
    }

    public static FuncKeyMap getDefaultMapping(int charID, int preset, int keySettingType) {
        FuncKeyMap fkm = new FuncKeyMap();
        fkm.setCharID(charID);
        fkm.setPreset(preset);
        int[] array1;
        int[] array2;
        int[] array3;
        if (keySettingType == 0) {
            // Basic Key Setting
            array1 = new int[]{1, 2, 3, 4, 5, 6, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 31, 34, 35, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 50, 56, 57, 59, 60, 61, 63, 64, 65, 66, 70};
            array2 = new int[]{4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 6, 6, 6, 4};
            array3 = new int[]{46, 10, 12, 13, 18, 23, 8, 5, 0, 4, 27, 30, 39, 1, 41, 19, 14, 15, 52, 2, 17, 11, 3, 20, 26, 16, 22, 9, 50, 51, 6, 31, 29, 7, 53, 54, 100, 101, 102, 103, 104, 105, 106, 47};
        } else {
            // Secondary Key Setting
            array1 = new int[]{1, 20, 21, 22, 23, 25, 26, 27, 29, 34, 35, 36, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 49, 50, 52, 56, 57, 59, 60, 61, 63, 64, 65, 66, 70, 71, 73, 79, 82, 83};
            array2 = new int[]{4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4, 4, 4};
            array3 = new int[]{46, 27, 30, 0, 1, 19, 14, 15, 52, 17, 11, 8, 3, 20, 26, 16, 22, 9, 50, 51, 2, 31, 29, 5, 7, 4, 53, 54, 100, 101, 102, 103, 104, 105, 106, 47, 12, 13, 23, 10, 18};
        }
        for (int i = 0; i < array1.length; i++) {
            fkm.putKeyBinding(charID, array1[i], (byte) array2[i], array3[i]);
        }
        return fkm;
    }

    public List<Keymapping> getKeymap() {
        return keymap;
    }

    public void setKeymap(List<Keymapping> keymap) {
        this.keymap = keymap;
    }

    public Keymapping getMappingAt(int index) {
        for (Keymapping km : getKeymap()) {
            if (km.getIndex() == index) {
                return km;
            }
        }
        return null;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(keymap.isEmpty() ? 1 : 0);
        if (!keymap.isEmpty()) {
            Keymapping binding;
            for (int i = 0; i < MAX_KEYBINDS; i++) {
                binding = getMappingAt(i);
                if (binding != null) {
                    outPacket.encodeByte(binding.getType());
                    outPacket.encodeInt(binding.getVal());
                } else {
                    outPacket.encodeByte(0);
                    outPacket.encodeInt(0);
                }
            }
            for (int i = 0; i < MAX_COMBINATION; i++) {
                binding = getMappingAt(102 + i);
                if (binding != null) {
                    outPacket.encodeByte(binding.getType());
                    outPacket.encodeInt(binding.getVal());
                } else {
                    outPacket.encodeByte(0);
                    outPacket.encodeInt(0);
                }
            }
        }
    }

    public void putKeyBinding(int charID, int index, byte type, int value) {
        Keymapping km = getMappingAt(index);
        if (km == null) {
            km = new Keymapping();
            km.setCharId(charID);
            km.setIndex(index);
            km.setType(type);
            km.setVal(value);
            getKeymap().add(km);
        } else {
            km.setCharId(charID);
            km.setType(type);
            km.setVal(value);
        }
    }
}
