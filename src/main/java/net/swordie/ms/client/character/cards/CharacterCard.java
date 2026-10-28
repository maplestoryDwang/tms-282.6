package net.swordie.ms.client.character.cards;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CharacterCard {

    private int id;
    private int characterId;
    private int job;
    private byte level;

    public CharacterCard() {
    }

    public CharacterCard(int characterId, int job, byte level) {
        this.characterId = characterId;
        this.job = job;
        this.level = level;
    }

    public int getCharacterId() {
        return getId();
    }

    public void setCharacterId(int characterId) {
        this.characterId = characterId;
    }

    public int getJob() {
        return job;
    }

    public void setJob(int job) {
        this.job = job;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(byte level) {
        this.level = level;
    }

    public void encode(OutPacket outPacket) {
        //CHARACTERCARD::Decode
        for (int i = 0; i < 9; i++) {
            outPacket.encodeInt(getCharacterId());
            outPacket.encodeInt(getLevel());
            outPacket.encodeInt(getJob());
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
