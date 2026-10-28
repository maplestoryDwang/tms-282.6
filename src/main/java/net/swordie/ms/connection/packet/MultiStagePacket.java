package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class MultiStagePacket {

    public static OutPacket setStage(int fieldID, int curStage) {
        OutPacket outPacket = new OutPacket(OutHeader.MULTI_STAGE_SET_STAGE);

        outPacket.encodeInt(fieldID);
        outPacket.encodeShort(curStage); // 2 will spawn 3 portals

        return outPacket;
    }

    public static OutPacket setMonsterGauge(int max, int cur) {
        OutPacket outPacket = new OutPacket(OutHeader.MULTI_STAGE_SET_MONSTER_GAUGE);

        outPacket.encodeInt(max);
        outPacket.encodeInt(cur);

        return outPacket;
    }
}
