package net.swordie.ms.client.character.info;

import net.swordie.ms.connection.OutPacket;

public class FarmUserInGameInfo {
    private int worldID;
    private String worldName;
    private int charID;
    private String charName;

    public FarmUserInGameInfo() {
    }

    public FarmUserInGameInfo(int worldID, String worldName, int charID, String charName) {
        this.worldID = worldID;
        this.worldName = worldName;
        this.charID = charID;
        this.charName = charName;
    }

    public int getWorldID() {
        return worldID;
    }

    public void setWorldID(int worldID) {
        this.worldID = worldID;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public int getCharID() {
        return charID;
    }

    public void setCharID(int charID) {
        this.charID = charID;
    }

    public String getCharName() {
        return charName;
    }

    public void setCharName(String charName) {
        this.charName = charName;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(true);
        outPacket.encodeInt(getWorldID());
        outPacket.encodeString(getWorldName());
        outPacket.encodeInt(getCharID());
        outPacket.encodeString(getCharName());
    }
}
