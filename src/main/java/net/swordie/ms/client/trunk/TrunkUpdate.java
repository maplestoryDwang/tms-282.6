package net.swordie.ms.client.trunk;

import net.swordie.ms.connection.OutPacket;

import java.util.List;

public class TrunkUpdate implements TrunkDlg {

    private TrunkType successType;
    private Trunk trunk;
    private List<Integer> items;
    private int size;
    private boolean bPartial;

    public TrunkUpdate(TrunkType successType, Trunk trunk) {
        this.successType = successType;
        this.trunk = trunk;
    }

    public TrunkUpdate(Trunk trunk, List<Integer> items, boolean bPartial) {
        this.successType = TrunkType.TrunkRes_FindAll;
        this.trunk = trunk;
        this.items = items;
        this.bPartial = bPartial;
    }

    public TrunkUpdate(Trunk trunk, int size, boolean bPartial) {
        this.successType = TrunkType.TrunkRes_AutoStore;
        this.trunk = trunk;
        this.size = size;
        this.bPartial = bPartial;
    }

    /** The trunk this update carries. */
    public Trunk getTrunk() {
        return trunk;
    }

    /** FindAll: the item ids moved to the inventory (null for other types). */
    public List<Integer> getItemIds() {
        return items;
    }

    /** AutoStore: the number of stacks stored. */
    public int getSize() {
        return size;
    }

    /** FindAll / AutoStore: only part of the items could be moved. */
    public boolean isPartial() {
        return bPartial;
    }

    @Override
    public TrunkType getType() {
        return successType;
    }

    @Override
    public void encode(OutPacket outPacket) {
        if (successType == TrunkType.TrunkRes_FindAll) {
            outPacket.encodeInt(items.size()); // size
            outPacket.encodeByte(bPartial); // bool
            // nếu bool = true
            // => size > 0 => SID_BOSSREWARD_GETALL_PARTIAL_COMPLETE_ALERT
            // => size <= 0 => SID_BOSSREWARD_GETALL_FAIL_ALERT
            // nếu bool = false
            // => size > 0 => SID_BOSSREWARD_GETALL_COMPLETE_ALERT
            // => size <= 0 => SID_BOSSREWARD_GETALL_FAIL_ALERT_1
            outPacket.encodeInt(items.size()); // size
            for (int i : items) {
                outPacket.encodeInt(i);
            }
        }
        if (successType == TrunkType.TrunkRes_AutoStore) {
            outPacket.encodeInt(size); // size
            outPacket.encodeByte(bPartial); // bool
            // nếu bool = true
            // => size > 0 => SID_TRUNK_AUTOMOVE_PARTIAL_COMPLETE_ALERT
            // => size <= 0 => SID_TRUNK_AUTOMOVE_FAIL_ALERT
            // nếu bool = false
            // => size > 0 => SID_TRUNK_AUTOMOVE_COMPLETE_ALERT
            // => size <= 0 => SID_TRUNK_AUTOMOVE_FAIL_ALERT_1
        }
        trunk.encodeItems(outPacket);
    }
}
