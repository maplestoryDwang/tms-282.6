package net.swordie.ms.client.character.keys;

import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Keymapping {

    private int id;
    private int charid;
    private int index;
    private byte type;
    private int val;

    public void updateKeyMappingToSQL(int mappingID) {
        if (getId() == 0) {
            String query = "INSERT INTO `keymaps` (" +
                    "`fkmapid`, " +
                    "`charid`, " +
                    "`idx`, " +
                    "`type`, " +
                    "`val` " +
                    ") VALUES (" +
                    String.format("%d, ", mappingID) +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getIndex()) +
                    String.format("%d, ", getType()) +
                    String.format("%d ", getVal()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE keymaps SET " +
                    String.format("charid = %d, ", getCharId()) +
                    String.format("idx = %d, ", getIndex()) +
                    String.format("type = %d, ", getType()) +
                    String.format("val = %d ", getVal()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getVal() {
        return val;
    }

    public void setVal(int val) {
        this.val = val;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getCharId() {
        return charid;
    }

    public void setCharId(int charid) {
        this.charid = charid;
    }
}
