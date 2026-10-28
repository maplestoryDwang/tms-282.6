package net.swordie.ms.client.trunk;

import net.swordie.ms.connection.OutPacket;

public class TrunkOpen implements TrunkDlg {

    private int npcTemplateID;
    private Trunk trunk;

    public TrunkOpen(int npcTemplateID, Trunk trunk) {
        this.npcTemplateID = npcTemplateID;
        this.trunk = trunk;
    }

    public int getNpcTemplateID() {
        return npcTemplateID;
    }

    public Trunk getTrunk() {
        return trunk;
    }

    @Override
    public TrunkType getType() {
        return TrunkType.TrunkRes_OpenTrunkDlg;
    }

    @Override
    public void encode(OutPacket outPacket) {
        outPacket.encodeInt(npcTemplateID);
        trunk.encodeItems(outPacket);
    }
}
