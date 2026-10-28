package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class MesoMarketPacket {

    public static OutPacket open(int type) {
        OutPacket outPacket = new OutPacket(OutHeader.MESO_EXCHANGE_RESULT);

        outPacket.encodeByte(type);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        if (type == 2) {
            outPacket.encodeByte(1);
        } else if (type == 8) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeString("");
            outPacket.encodeInt(0);
            outPacket.encodeString("");
        } else if (type == 9) {
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
            outPacket.encodeInt(0);
        }
        // type == 3,4: Deposit deducted after successful trade request
        // type == 5: Cancelled successful. Click [Reclaim My Mesos & Maple Points] to reclaim them.
        // type == 7: %d million mesos / %d Maple Points sucessfully reclaimed.

        return outPacket;
    }
}
