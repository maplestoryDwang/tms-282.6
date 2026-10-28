package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.FileTime;

import java.util.HashSet;
import java.util.Set;

public class CommerceRecord {

    private int slotExtend = 0;
    private int vesselLevel = 1;
    private int currentEnergy = 100;
    private FileTime fileTime = FileTime.MAX_TIME();
    private Set<CommerceRegionRecord> commerceRegionRecords = new HashSet<>();
    private Set<CommerceItemRecord> commerceItemRecords = new HashSet<>();

    public void encode(OutPacket outPacket) {
        outPacket.encodeByte(true);
        outPacket.encodeByte(getSlotExtend());
        outPacket.encodeInt(getVesselLevel());
        outPacket.encodeInt(0); // idk
        outPacket.encodeInt(getCurrentEnergy());
        outPacket.encodeFT(FileTime.MAX_TIME());
    }

    public int getCurrentEnergy() {
        return currentEnergy;
    }

    public void setCurrentEnergy(int currentEnergy) {
        this.currentEnergy = currentEnergy;
    }

    public FileTime getFileTime() {
        return fileTime;
    }

    public void setFileTime(FileTime fileTime) {
        this.fileTime = fileTime;
    }

    public int getVesselLevel() {
        return vesselLevel;
    }

    public void setVesselLevel(int vesselLevel) {
        this.vesselLevel = vesselLevel;
    }

    public int getSlotExtend() {
        return slotExtend;
    }

    public void setSlotExtend(int slotExtend) {
        this.slotExtend = slotExtend;
    }

    public Set<CommerceRegionRecord> getCommerceRegionRecords() {
        return commerceRegionRecords;
    }

    public void setCommerceRegionRecords(Set<CommerceRegionRecord> commerceRegionRecords) {
        this.commerceRegionRecords = commerceRegionRecords;
    }

    public Set<CommerceItemRecord> getCommerceItemRecords() {
        return commerceItemRecords;
    }

    public void setCommerceItemRecords(Set<CommerceItemRecord> commerceItemRecords) {
        this.commerceItemRecords = commerceItemRecords;
    }
}
