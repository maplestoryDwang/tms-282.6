package net.swordie.ms.client.character;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.util.FileTime;

public class CommerceItemRecord {
    private int itemID;
    private int usedCount;
    private FileTime usedTime;

    public CommerceItemRecord(int itemID, int usedCount, FileTime usedTime) {
        this.itemID = itemID;
        this.usedCount = usedCount;
        this.usedTime = usedTime;
    }

    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(getItemID());
        outPacket.encodeInt(getUsedCount());
        outPacket.encodeFT(getUsedTime());
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    public FileTime getUsedTime() {
        return usedTime;
    }

    public void setUsedTime(FileTime usedTime) {
        this.usedTime = usedTime;
    }
}
