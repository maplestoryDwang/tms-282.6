package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;

public class CommerceRegionRecord {
    private byte regionType;
    private int voyagesCompleted;

    public CommerceRegionRecord(byte regionType, int voyagesCompleted) {
        this.regionType = regionType;
        this.voyagesCompleted = voyagesCompleted;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(getRegionType());
        outPacket.encodeInt(getVoyagesCompleted());
        outPacket.encodeInt(0); // idk
    }

    public byte getRegionType() {
        return regionType;
    }

    public void setRegionType(byte regionType) {
        this.regionType = regionType;
    }

    public int getVoyagesCompleted() {
        return voyagesCompleted;
    }

    public void setVoyagesCompleted(int voyagesCompleted) {
        this.voyagesCompleted = voyagesCompleted;
    }
}
