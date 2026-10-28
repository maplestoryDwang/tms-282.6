package net.swordie.ms.client.character.items;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PetVac {

    private int id;
    private int charID;
    private int itemID;
    private int quantity;

    public static List<PetVac> getPetVacFromSQLByCharID(int charID) {
        List<PetVac> petVacList = new ArrayList<>();
        String query = "SELECT * FROM petvac WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PetVac petVac = new PetVac();
                    petVac.setCharID(charID);
                    petVac.setId(rs.getInt("id"));
                    petVac.setItemID(rs.getInt("itemid"));
                    petVac.setQuantity(rs.getInt("quantity"));
                    petVacList.add(petVac);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return petVacList;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `petvac` (" +
                    "`charid`, " +
                    "`itemid`, " +
                    "`quantity` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d, ", getItemID()) +
                    String.format("%d ", getQuantity()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE petvac SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("itemid = %d, ", getItemID()) +
                    String.format("quantity = %d ", getQuantity()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deletePetVacToSQL() {
        String query = "DELETE FROM `petvac` WHERE " +
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

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PetVac petvac
                && petvac.getCharID() == getCharID()
                && petvac.getItemID() == getItemID()
                && petvac.getQuantity() == getQuantity();
    }
}
