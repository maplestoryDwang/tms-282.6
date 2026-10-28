package net.swordie.ms.client.character;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BeautySalon {

    private int id;
    private int charID;
    private int hairSize = 3;
    private int faceSize = 3;
    private int skinSize = 3;
    private String hairString = "-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1";
    private String faceString = "-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1";
    private String skinString = "-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1";

    public static BeautySalon getBeautySalonFromSQLByCharID(int charID) {
        BeautySalon beautySalon = new BeautySalon();
        beautySalon.setCharID(charID);
        String query = "SELECT * FROM beautydata WHERE charid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    beautySalon.setId(rs.getInt("id"));
                    beautySalon.setHairSize(rs.getInt("hairsize"));
                    beautySalon.setFaceSize(rs.getInt("facesize"));
                    beautySalon.setSkinSize(rs.getInt("skinsize"));
                    beautySalon.setHairString(rs.getString("hairstring"));
                    beautySalon.setFaceString(rs.getString("facestring"));
                    beautySalon.setSkinString(rs.getString("skinstring"));
                }
            }
        } catch (SQLException sqlException) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, sqlException);
        }
        return beautySalon;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `beautydata` (" +
                    "`charid`, " +
                    "`hairsize`, " +
                    "`facesize`, " +
                    "`skinsize`, " +
                    "`hairstring`, " +
                    "`facestring`, " +
                    "`skinstring` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d, ", getHairSize()) +
                    String.format("%d, ", getFaceSize()) +
                    String.format("%d, ", getSkinSize()) +
                    DatabaseManager.getSQLStringSyntax(false, "", getHairString(), false) +
                    DatabaseManager.getSQLStringSyntax(false, "", getFaceString(), false) +
                    DatabaseManager.getSQLStringSyntax(false, "", getSkinString(), true) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE beautydata SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("hairsize = %d, ", getHairSize()) +
                    String.format("facesize = %d, ", getFaceSize()) +
                    String.format("skinsize = %d, ", getSkinSize()) +
                    DatabaseManager.getSQLStringSyntax(true, "hairstring", getHairString(), false) +
                    DatabaseManager.getSQLStringSyntax(true, "facestring", getFaceString(), false) +
                    DatabaseManager.getSQLStringSyntax(true, "skinstring", getFaceString(), true) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteBeautySalonToSQL() {
        String query = "DELETE FROM `beautydata` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
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
    }

    public int getHairSize() {
        return hairSize;
    }

    public void setHairSize(int hairSize) {
        this.hairSize = hairSize;
    }

    public int getFaceSize() {
        return faceSize;
    }

    public void setFaceSize(int faceSize) {
        this.faceSize = faceSize;
    }

    public String getHairString() {
        return hairString;
    }

    public void setHairString(String hairString) {
        this.hairString = hairString;
    }

    public String getFaceString() {
        return faceString;
    }

    public void setFaceString(String faceString) {
        this.faceString = faceString;
    }

    public int getSkinSize() {
        return skinSize;
    }

    public void setSkinSize(int skinSize) {
        this.skinSize = skinSize;
    }

    public String getSkinString() {
        return skinString;
    }

    public void setSkinString(String skinString) {
        this.skinString = skinString;
    }

    public List<Integer> generateValueFromString(String value) {
        List<Integer> result = new ArrayList<>();

        String[] splitStrings = value.split(",");
        for (String s : splitStrings) {
            result.add(Integer.parseInt(s));
        }

        return result;
    }

    public String generateValueFromList(List<Integer> value) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < value.size(); i++) {
            if (i == value.size() - 1) {
                result.append(value.get(i));
            } else {
                result.append(value.get(i)).append(",");
            }
        }

        return result.toString();
    }

    public int getHairByIndex(int index) {
        return generateValueFromString(getHairString()).get(index);
    }

    public int getFaceByIndex(int index) {
        return generateValueFromString(getFaceString()).get(index);
    }

    public int getSkinByIndex(int index) {
        return generateValueFromString(getSkinString()).get(index);
    }

    public void setHairByIndex(int index, int hairID) {
        List<Integer> hairs = generateValueFromString(getHairString());
        hairs.set(index, hairID);
        setHairString(generateValueFromList(hairs));
    }

    public void setFaceByIndex(int index, int faceID) {
        List<Integer> faces = generateValueFromString(getFaceString());
        faces.set(index, faceID);
        setFaceString(generateValueFromList(faces));
    }

    public void setSkinByIndex(int index, int skinID) {
        List<Integer> skins = generateValueFromString(getSkinString());
        skins.set(index, skinID);
        setSkinString(generateValueFromList(skins));
    }

    @Override
    public String toString() {
        return "BeautySalon{" +
                "id=" + id +
                ", charID=" + charID +
                ", hairSize=" + hairSize +
                ", faceSize=" + faceSize +
                ", skinSize=" + skinSize +
                ", hairString='" + hairString + '\'' +
                ", faceString='" + faceString + '\'' +
                ", skinString='" + skinString + '\'' +
                '}';
    }
}
