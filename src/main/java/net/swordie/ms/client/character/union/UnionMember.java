package net.swordie.ms.client.character.union;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.UnionConstants;
import net.swordie.ms.util.DataPrinter;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UnionMember {

    public static final int[] boss = {9833101, 9833102, 9833103, 9833104, 9833105, 9833201, 9833202, 9833203, 9833204, 9833205};
    public static final int[] attackerCount = {9, 10, 11, 12, 13, 18, 19, 20, 21, 22, 27, 28, 29, 30, 31, 36, 37, 38, 39, 40};
    public static final int[] reqLev = {500, 100, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 5500, 6000, 6500, 7000, 7500, 8000, 8500, 9000, 9500, 10000};
    public static final int[] reqCoin = {0, 120, 140, 150, 160, 170, 430, 450, 470, 490, 510, 930, 960, 1000, 1030, 1060, 2200, 2300, 2350, 2400};
    public static final String[] ranks = {
            "無名的聯盟 I", "無名的聯盟 II", "無名的聯盟 III", "無名的聯盟 IV", "無名的聯盟 V",
            "知名的聯盟 I", "知名的聯盟 II", "知名的聯盟 III", "知名的聯盟 IV", "知名的聯盟 V",
            "英雄的聯盟 I", "英雄的聯盟 II", "英雄的聯盟 III", "英雄的聯盟 IV", "英雄的聯盟 V",
            "傳說的聯盟 I", "傳說的聯盟 II", "傳說的聯盟 III", "傳說的聯盟 IV", "傳說的聯盟 V"
    };

    private int id;
    private int unionBoardID;
    private int type; // 1 = normal, 2 = mobile, 3 = lab
    private String mobileName = "";
    private int gridPos;
    private int gridRotation;
    private int charID;
    private int charLevel;
    private int charJob;
    private int charSubJob;
    private int charTotalChuc;
    private long charCombatPower;
    private String charName;

    public UnionMember() {
        gridPos = -1;
    }

    public UnionMember(int type, Char chr, String mobileName) {
        this();
        this.type = type;
        this.mobileName = mobileName;
        this.charID = chr.getId();
        this.charLevel = chr.getLevel();
        this.charJob = chr.getJob();
        this.charSubJob = chr.getSubJob();
        this.charTotalChuc = chr.getTotalChuc(true);
        this.charCombatPower = chr.getCombatPower();
        this.charName = chr.getName();
    }

    public void updateUnionMemberToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `unionmember` (" +
                    "`unionboardid`, " +
                    "`type`, " +
                    "`charid`, " +
                    "`gridPos`, " +
                    "`gridRotation` " +
                    ") VALUES (" +
                    String.format("%d, ", getUnionBoardID()) +
                    String.format("%d, ", getType()) +
                    String.format("%d, ", getCharId()) +
                    String.format("%d, ", getGridPos()) +
                    String.format("%d ", getGridRotation()) +
                    ");";
            int id = (int) DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE unionmember SET " +
                    String.format("unionboardid = %d, ", getUnionBoardID()) +
                    String.format("type = %d, ", getType()) +
                    String.format("charid = %d, ", getCharId()) +
                    String.format("gridPos = %d, ", getGridPos()) +
                    String.format("gridRotation = %d ", getGridRotation()) +
                    String.format("WHERE id = %d;", getId());
            DatabaseManager.executeStatement(query);
        }
    }

    public void deleteFromSQL() {
        String query = "DELETE FROM unionmember WHERE unionboardid = ? AND charid = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, getUnionBoardID());
            ps.setInt(2, getCharId());
            ps.executeUpdate();
            setId(0);
        } catch (Exception e) {
            DataPrinter.send(DataPrinter.HIKARICP_ERROR, e);
        }
    }

    public static void setCharGridPos(Char chr, InPacket inPacket, int preset, Account account) {
        int type = inPacket.decodeInt(); // type
        int charID = inPacket.decodeInt(); // charID
        int level = inPacket.decodeInt(); // level
        int job = inPacket.decodeInt(); // job
        int subJob = inPacket.decodeInt(); // sub job
        int rotation = inPacket.decodeInt(); // Unknown
        int grid = inPacket.decodeInt(); // grid pos
        int chuc = inPacket.decodeInt(); // chuc
        long combatPower = inPacket.decodeLong(); // combat Power
        String name = inPacket.decodeString(); // char name
        if (type == 2) {
            String mobileName = inPacket.decodeString(); // mobile name
        }
        if (chr.getId() != charID) {
            chr = account.getEligibleUnionChars().stream().filter(c -> c.getId() == charID).findFirst().orElse(null);
            if (chr == null) {
                return;
            } else {
                System.out.println("CharName: " + name + " StarForce:" + chr.getTotalChuc(true));
            }
        }
        if (UnionConstants.isEligibleForUnion(chr)) {
            Union union = account.getUnion();
            union.setCharPosForPreset(preset, chr, rotation, grid);
        }
    }

    public void encode(OutPacket outPacket) {
        encode(outPacket, false);
    }

    public void encode(OutPacket outPacket, boolean forEligible) {
        outPacket.encodeInt(getType());
        outPacket.encodeInt(getCharId());
        outPacket.encodeInt(getLevel());
        outPacket.encodeInt(getJob());
        outPacket.encodeInt(getSubJob());
        outPacket.encodeInt(getGridRotation());
        outPacket.encodeInt(forEligible ? -1 : getGridPos());
        outPacket.encodeInt(getChuc());
        outPacket.encodeLong(getCombatPower());
        outPacket.encodeString(forEligible ? getCharacterName() : "");
        if (getType() == 2) {
            outPacket.encodeString(getMobileName());
        }
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCharId() {
        return charID;
    }

    public int getLevel() {
        return charLevel;
    }

    public int getJob() {
        return charJob;
    }

    public int getSubJob() {
        return charSubJob;
    }

    public int getChuc() {
        return charTotalChuc;
    }

    public long getCombatPower() {
        return charCombatPower;
    }

    public String getCharacterName() {
        return charName;
    }

    public String getMobileName() {
        return mobileName;
    }

    public int getGridPos() {
        return gridPos;
    }

    public void setGridPos(int gridPos) {
        this.gridPos = gridPos;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setGridRotation(int gridRotation) {
        this.gridRotation = gridRotation;
    }

    public int getGridRotation() {
        return gridRotation;
    }

    public int getUnionBoardID() {
        return unionBoardID;
    }

    public void setUnionBoardID(int unionBoardID) {
        this.unionBoardID = unionBoardID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public void setCharLevel(int charLevel) {
        this.charLevel = charLevel;
    }

    public void setCharJob(int charJob) {
        this.charJob = charJob;
    }

    public void setCharSubJob(int charSubJob) {
        this.charSubJob = charSubJob;
    }

    public void setCharTotalChuc(int charTotalChuc) {
        this.charTotalChuc = charTotalChuc;
    }

    public void setCharCombatPower(int charCombatPower) {
        this.charCombatPower = charCombatPower;
    }

    public void setCharName(String charName) {
        this.charName = charName;
    }
}
