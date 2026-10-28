package net.swordie.ms.handlers.ui;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.MesoMarketPacket;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;

public class MesoMarketHandler {

    @Handler(op = InHeader.USER_MESO_EXCHANGE_REQUEST)
    public static void handleUserMesoExchangeRequest(Char chr, InPacket inPacket) {
        byte type = inPacket.decodeByte();
        switch (type) {
            case 3:
                int pointToMesos = inPacket.decodeInt();
                int idk = inPacket.decodeInt();
                long mesosToBuy = idk * 100000000;
                chr.write(MesoMarketPacket.open(8));
                break;
            case 21:
                break;
        }
    }
}
