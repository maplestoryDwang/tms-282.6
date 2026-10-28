package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class AkechiPacket {

    public static OutPacket encode(int type, int action, int param, Integer extraParam) {
        OutPacket outPacket = new OutPacket(OutHeader.BOSS_AKECHI_FIELD);
        outPacket.encodeInt(type); // v39, e.g., 261
        outPacket.encodeInt(action + 1); // v3, adjusted to match v3 - 1 logic
        outPacket.encodeInt(param); // v36
        if (action == 0 && extraParam != null) { // v3 - 1 == 0 case
            outPacket.encodeInt(extraParam); // Additional parameter
        } else if (action == 3) { // v3 - 1 == 3 case
            outPacket.encodeInt(1); // State flag, mirroring this[596] = 1
        }
        return outPacket;
    }
}
