package net.swordie.ms.life;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.*;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

public class Familiar extends Life {

    private long id;
    private int charID;
    private int idk1;
    private int familiarID;
    private String name;
    private boolean idk2;
    private short idk3;
    private int fatigue;
    private long idk4;
    private long idk5;
    private FileTime expiration = FileTime.MAX_TIME();
    private short vitality;
    private int skillID;

    public static Set<Familiar> getFamiliarsFromSQLByCharID(int charID) {
        Set<Familiar> familiars = new HashSet<>();
        String query = "SELECT * FROM familiars WHERE charid = ?";

        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, charID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    int charIDFromDatabase = rs.getInt("charid");
                    int idk1 = rs.getInt("idk1");
                    int familiarID = rs.getInt("familiarid");
                    String name = rs.getString("name");
                    boolean idk2 = rs.getByte("idk2") != 0;
                    short idk3 = rs.getShort("idk3");
                    int fatigue = rs.getInt("fatigue");
                    long idk4 = rs.getLong("idk4");
                    long idk5 = rs.getLong("idk5");
                    FileTime expiration = DatabaseManager.getFileTimeFromString(rs.getString("expiration"));
                    short vitality = rs.getShort("vitality");

                    Familiar familiar = new Familiar();
                    familiar.setId(id);
                    familiar.setCharID(charIDFromDatabase);
                    familiar.setIdk1(idk1);
                    familiar.setFamiliarID(familiarID);
                    familiar.setName(name);
                    familiar.setIdk2(idk2);
                    familiar.setIdk3(idk3);
                    familiar.setFatigue(fatigue);
                    familiar.setIdk4(idk4);
                    familiar.setIdk5(idk5);
                    familiar.setExpiration(expiration);
                    familiar.setVitality(vitality);
                    familiars.add(familiar);
                }
            }
        } catch (SQLException | ParseException exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return familiars;
    }

    public void saveToSQL() {
        if (getId() == 0) {
            String query = "INSERT INTO `familiars` (" +
                    "`charid`, " +
                    "`idk1`, " +
                    "`familiarid`, " +
                    "`name`, " +
                    "`idk2`, " +
                    "`idk3`, " +
                    "`fatigue`, " +
                    "`idk4`, " +
                    "`idk5`, " +
                    "`expiration`, " +
                    "`vitality` " +
                    ") VALUES (" +
                    String.format("%d, ", getCharID()) +
                    String.format("%d, ", getIdk1()) +
                    String.format("%d, ", getFamiliarID()) +
                    String.format("'%s', ", getName()) +
                    String.format("%d, ", isIdk2() ? 1 : 0) +
                    String.format("%d, ", getIdk3()) +
                    String.format("%d, ", getFatigue()) +
                    String.format("%d, ", getIdk4()) +
                    String.format("%d, ", getIdk5()) +
                    String.format("'%s', ", DatabaseManager.convertToDateTimeSQL(getExpiration())) +
                    String.format("%d ", getVitality()) +
                    ");";
            long id = DatabaseManager.executeStatementReturnID(query);
            setId(id);
        } else {
            String query = "UPDATE familiars SET " +
                    String.format("charid = %d, ", getCharID()) +
                    String.format("idk1 = %d, ", getIdk1()) +
                    String.format("familiarid = %d, ", getFamiliarID()) +
                    String.format("name = '%s', ", getName()) +
                    String.format("idk2 = %d, ", isIdk2() ? 1 : 0) +
                    String.format("idk3 = %d, ", getIdk3()) +
                    String.format("fatigue = %d, ", getFatigue()) +
                    String.format("idk4 = %d, ", getIdk4()) +
                    String.format("idk5 = %d, ", getIdk5()) +
                    String.format("expiration = '%s', ", DatabaseManager.convertToDateTimeSQL(getExpiration())) +
                    String.format("vitality = %d ", getVitality()) +
                    String.format("WHERE id = %d;", getId());

            net.swordie.ms.connection.hikariCP.DatabaseManager.executeStatement(query);
        }
    }

    public void deleteFamiliarFromSQL() {
        String query = "DELETE FROM `familiars` WHERE " +
                String.format("`id` = %d", getId());
        DatabaseManager.executeStatement(query);
        setId(0);
    }

    public Familiar() {
        super(0);
    }

    public Familiar(int templateId) {
        super(templateId);
    }

    public Familiar(int charID, int familiarID, String name, FileTime expiration, short vitality) {
        super(0);
        this.charID = charID;
        this.familiarID = familiarID;
        this.name = name;
        this.expiration = expiration;
        this.vitality = vitality;
    }

    public void encode(OutPacket outPacket, Char chr) {
        int mask = 0xFFFF;
        outPacket.encodeInt(mask);

        if (mask == 0xFFFF) { // 50 bytes
            outPacket.encodeInt(chr.getId()); // charId
            outPacket.encodeInt(1); // familiarIdx?
            outPacket.encodeInt(getFamiliarID()); // familiarId

            outPacket.encodeString(getName(), 12);

            outPacket.encodeByte(3);
            outPacket.encodeByte(0); // bLocked
            outPacket.encodeByte(5);
            outPacket.encodeByte(6);

            outPacket.encodeByte(0); // bLocked again?
            outPacket.encodeByte(0); // nLevel (1~9)
            outPacket.encodeByte(3); // Affects gauge under Level
            outPacket.encodeByte(0);
            outPacket.encodeByte(1); // Affects gauge under Level
            outPacket.encodeByte(0); // nGrade

            outPacket.encodeShort(50); // exp

            outPacket.encodeByte(0); // nAtt - 1
            outPacket.encodeByte(0); // nDef - 1
            outPacket.encodeShort(10041); // nOption1
            outPacket.encodeShort(10041); // nOption2
              /*
            outPacket.encodeShort(getOptions()[0] | 10041); // nOption1
            outPacket.encodeShort(getOptions()[1] | 10041); // nOption2
              */
            outPacket.encodeByte(5);
            outPacket.encodeShort(0xCAFA);
            outPacket.encodeShort(0xB951);
            outPacket.encodeByte(6);
            outPacket.encodeShort(466); // was 466
        } else {
            if ((mask & 0x1) != 0) {
                outPacket.encodeInt(getFamiliarID());
            }
            if ((mask & 0x2) != 0) {
                outPacket.encodeString(getName(), 12);
            }
            if ((mask & 0x4) != 0) {
                outPacket.encodeByte(0);
            }
            if ((mask & 0x8) != 0) {
                outPacket.encodeByte(0);
            }
            if ((mask & 0x10) != 0) {
                outPacket.encodeByte(0);
            }
            if ((mask & 0x20) != 0) {
                outPacket.encodeShort(0);
            }
            if ((mask & 0x40) != 0) {
                outPacket.encodeByte(0);
            }
            if ((mask & 0x80) != 0) {
                outPacket.encodeShort(0);
            }
            if ((mask & 0x100) != 0) {
                outPacket.encodeByte(0);
            }
            if ((mask & 0x200) != 0) {
                outPacket.encodeByte(0);
            }
            if ((mask & 0x400) != 0) {
                outPacket.encodeShort(0);
            }
            if ((mask & 0x800) != 0) {
                outPacket.encodeShort(0);
            }
        }
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getIdk1());
        outPacket.encodeInt(getFamiliarID());
        outPacket.encodeString(getName(), 13);
        outPacket.encodeByte(isIdk2());
        outPacket.encodeShort(getIdk3());
        outPacket.encodeInt(getFatigue());
        outPacket.encodeLong(getIdk4());
        outPacket.encodeLong(getIdk5());
        outPacket.encodeFT(getExpiration());
        outPacket.encodeShort(getVitality());
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

    public int getIdk1() {
        return idk1;
    }

    public void setIdk1(int idk1) {
        this.idk1 = idk1;
    }

    public int getFamiliarID() {
        return familiarID;
    }

    public void setFamiliarID(int familiarID) {
        this.familiarID = familiarID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIdk2() {
        return idk2;
    }

    public void setIdk2(boolean idk2) {
        this.idk2 = idk2;
    }

    public short getIdk3() {
        return idk3;
    }

    public void setIdk3(short idk3) {
        this.idk3 = idk3;
    }

    public int getFatigue() {
        return fatigue;
    }

    public void setFatigue(int fatigue) {
        this.fatigue = fatigue;
    }

    public long getIdk4() {
        return idk4;
    }

    public void setIdk4(long idk4) {
        this.idk4 = idk4;
    }

    public long getIdk5() {
        return idk5;
    }

    public void setIdk5(long idk5) {
        this.idk5 = idk5;
    }

    public FileTime getExpiration() {
        return expiration;
    }

    public void setExpiration(FileTime expiration) {
        this.expiration = expiration;
    }

    public short getVitality() {
        return vitality;
    }

    public void setVitality(short vitality) {
        this.vitality = vitality;
    }

    public int getSkillID() {
        return skillID;
    }

    public void setSkillID(int skillID) {
        this.skillID = skillID;
    }

    public void encodeForRemote(OutPacket outPacket) {
        outPacket.encodeInt(getFamiliarID());
        outPacket.encodeInt((int) getId());
        outPacket.encodeString(getName());
        outPacket.encodePosition(getPosition());
        outPacket.encodeByte(getMoveAction());
        outPacket.encodeShort(getFh());
    }
}
