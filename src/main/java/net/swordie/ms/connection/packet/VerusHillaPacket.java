package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.Randomizer;
import net.swordie.ms.world.field.Field;

public class VerusHillaPacket {

    public static OutPacket encode(int type, Char chr, Field field) {
        OutPacket outPacket = new OutPacket(OutHeader.VERUS_HILLA);

        outPacket.encodeInt(type);
        switch (type) {
            case 0:
                outPacket.encodeInt(field.getCandles());
                outPacket.encodeByte(false);
                break;
            case 1:
                outPacket.encodeInt(field.getLightCandles());
                break;
            case 3:
                outPacket.encodeInt(chr.getVHDeathCount().length);
                for (int i = 0; i < chr.getVHDeathCount().length; i++) {
                    outPacket.encodeInt(0);
                    outPacket.encodeByte(chr.getVHDeathCount()[i]);
                }
                break;
            case 4:
                outPacket.encodeInt((int) (field.getSandGlassTime() * 1000L));
                outPacket.encodeInt(247);
                outPacket.encodeInt(1);
                break;
            case 6:
                outPacket.encodeInt(Randomizer.rand(-700, 700));
                outPacket.encodeInt(266);
                outPacket.encodeInt(30);
                break;
            case 7:
                outPacket.encodeInt(30 - field.getReqTouched());
                break;
            case 8:
                outPacket.encodeByte((field.getReqTouched() == 0));
                break;
            case 10:
                outPacket.encodeInt(chr.getId());
                outPacket.encodeInt(chr.getDeathCount());
                break;
        }

        return outPacket;
    }
}
