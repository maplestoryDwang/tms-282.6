package net.swordie.ms.handlers;

import net.swordie.ms.client.Client;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.SecurityPacket;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.handlers.header.OutHeader;

public class SecurityHandler {

    //@Handler(op = InHeader.SECURITY_REQUEST)
    public static void handleVerifyTimePulse(Client c, InPacket inPacket) {
        OutPacket say = new OutPacket(OutHeader.SECURITY_REQUEST.getValue());
        byte[] bytes = new byte[3];
        for (int i = 0; i < 3; i++) {
            bytes[i] = (byte) (inPacket.decodeByte() + i + 1);
            if (bytes[i] > 0x7F) {
                bytes[i] = (byte) (bytes[i] - 0xFF - 1);
            }
            say.encodeByte(bytes[i]);
        }
        say.encodeArr(new byte[5]);
        for (int i = 0; i < 3; i++) {
            bytes[i] = (byte) (bytes[i] + 1);
            if (bytes[i] > 0x7F) {
                bytes[i] = (byte) (bytes[i] - 0xFF - 1);
            }
            say.encodeByte(bytes[i]);
        }
        say.encodeArr(new byte[5]);
        c.write(say);
    }

    @Handler(op = InHeader.REQUEST_STATUS_CHECK)
    public static void handleRequestStatusCheck(Client c, InPacket inPacket) {
        int NextType = inPacket.decodeInt();
        inPacket.skipInt();
        boolean action = inPacket.decodeByte() != 0;
        c.write(SecurityPacket.VerifyTimePulseNext(NextType, action));
    }
}
