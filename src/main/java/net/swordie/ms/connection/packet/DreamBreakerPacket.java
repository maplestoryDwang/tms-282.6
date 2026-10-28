package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class DreamBreakerPacket {

    public static OutPacket setStage(int stage) {
        OutPacket outPacket = new OutPacket(OutHeader.DREAM_BREAKER_RESULT);

        outPacket.encodeInt(3);
        outPacket.encodeInt(500);
        outPacket.encodeInt(180000);
        outPacket.encodeInt(stage);

        return outPacket;
    }

    public static OutPacket setGauge(int Gauge) {
        OutPacket outPacket = new OutPacket(OutHeader.DREAM_BREAKER_RESULT);

        outPacket.encodeInt(4);
        outPacket.encodeInt(Gauge);

        return outPacket;
    }

    public static OutPacket setCooldown(int stage) {
        OutPacket outPacket = new OutPacket(OutHeader.DREAM_BREAKER_RESULT);

        outPacket.encodeInt(5);
        outPacket.encodeInt(stage);

        return outPacket;
    }

    public static OutPacket disableTimer(boolean Disable, int Time) {
        OutPacket outPacket = new OutPacket(OutHeader.DREAM_BREAKER_RESULT);

        outPacket.encodeInt(6);
        outPacket.encodeByte(Disable);
        outPacket.encodeInt(Time);

        return outPacket;
    }

    public static OutPacket getResult(int ClearTime) {
        OutPacket outPacket = new OutPacket(OutHeader.DREAM_BREAKER_RESULT);

        outPacket.encodeInt(7);
        outPacket.encodeInt(ClearTime / 1000);

        return outPacket;
    }

    public static OutPacket lockSkill(int skillCode) {
        OutPacket outPacket = new OutPacket(OutHeader.DREAM_BREAKER_RESULT);

        outPacket.encodeInt(8);
        outPacket.encodeInt(skillCode);

        return outPacket;
    }

    public static OutPacket result() {
        OutPacket outPacket = new OutPacket(OutHeader.DREAM_BREAKER_RESULT);

        outPacket.encodeInt(9);

        return outPacket;
    }
}
