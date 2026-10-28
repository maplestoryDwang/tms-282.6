package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

public class DefensePacket {

    public static OutPacket wave(int wave) {
        OutPacket outPacket = new OutPacket(OutHeader.DEFENSE_WAVE);

        outPacket.encodeInt(wave);

        return outPacket;
    }

    public static OutPacket life(int life) {
        OutPacket outPacket = new OutPacket(OutHeader.DEFENSE_LIFE);

        outPacket.encodeInt(life);

        return outPacket;
    }

    public static OutPacket point(int point) {
        OutPacket outPacket = new OutPacket(OutHeader.DEFENSE_POINT);

        outPacket.encodeInt(point);

        return outPacket;
    }

    public static OutPacket result(boolean clear, int wave, int life, int point, int exp) {
        OutPacket outPacket = new OutPacket(OutHeader.DEFENSE_RESULT);

        outPacket.encodeByte(clear);
        outPacket.encodeInt(wave);
        outPacket.encodeInt(life);
        outPacket.encodeInt(point);
        outPacket.encodeInt(exp);

        return outPacket;
    }
}
