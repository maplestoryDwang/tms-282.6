package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.trunk.TrunkDlg;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

/** The GMS 265 trunk dialog packet: the former body of {@code FieldPacket.trunkDlg}, unchanged. */
public enum Gms265TrunkPackets implements TrunkPacketEncoder {
    INSTANCE;

    @Override
    public OutPacket trunkDlg(TrunkDlg trunkDlg) {
        OutPacket outPacket = new OutPacket(OutHeader.TRUNK_DLG);

        outPacket.encodeByte(trunkDlg.getType().getVal());
        trunkDlg.encode(outPacket);

        return outPacket;
    }
}
