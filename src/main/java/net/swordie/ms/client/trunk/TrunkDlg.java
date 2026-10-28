package net.swordie.ms.client.trunk;

import net.swordie.ms.connection.OutPacket;

public interface TrunkDlg {

    TrunkType getType();

    void encode(OutPacket outPacket);
}
