package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class MonsterCarnivalRanking {

    private int id;
    private int charId;
    private String charName;
    private int totalCP;
    // Giá trị tạm thời:
    private int cp;
    private int team; // 0 = red; 1 = blue

    public MonsterCarnivalRanking() {
    }

    public MonsterCarnivalRanking(int charId, String charName, int totalCP, int cp, int team) {
        this.charId = charId;
        this.charName = charName;
        this.totalCP = totalCP;
        this.cp = cp;
        this.team = team;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getCharId());
        outPacket.encodeString(getCharName());
        outPacket.encodeInt(getTotalCP());
        outPacket.encodeByte(getTeam() == 0 ? 1 : 0);
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

    public String getCharName() {
        return charName;
    }

    public void setCharName(String charName) {
        this.charName = charName;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void setTotalCP(int totalCP) {
        this.totalCP = totalCP;
    }

    public void incTotalCP(int totalCP) {
        this.totalCP += totalCP;
    }

    public void decTotalCP(int dec) {
        this.totalCP = Math.min(getTotalCP() - dec, 0);
    }

    public int getCp() {
        return cp;
    }

    public void setCp(int cp) {
        this.cp = cp;
    }

    public void incCP(int cp) {
        this.cp += cp;
    }

    public void decCP(int dec) {
        this.cp = Math.min(getCp() - dec, 0);
    }

    public int getTeam() {
        return team;
    }

    public void setTeam(int team) {
        this.team = team;
    }

    @Override
    public String toString() {
        return "MonsterCarnivalRanking{" +
                "id=" + id +
                ", charID=" + charId +
                ", totalCP=" + totalCP +
                ", charName='" + charName + '\'' +
                '}';
    }
}
