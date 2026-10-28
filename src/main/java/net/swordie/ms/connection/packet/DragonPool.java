package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Dragon;
import net.swordie.ms.life.movement.MovementInfo;

public class DragonPool {

    public static OutPacket createDragon(Dragon dragon) {
        OutPacket outPacket = new OutPacket(OutHeader.DRAGON_CREATED);

        outPacket.encodeInt(dragon.getOwnerId());
        outPacket.encodePositionInt(dragon.getPosition());
        outPacket.encodeByte(4); //Move Action
        outPacket.encodeShort(dragon.getFoothold()); // ignored, probably FH
        outPacket.encodeShort(dragon.getJob());

        return outPacket;
    }

    public static OutPacket moveDragon(Dragon dragon, MovementInfo movementInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.DRAGON_MOVE);

        outPacket.encodeInt(dragon.getOwnerId());
        outPacket.encode(movementInfo);

        return outPacket;
    }

    public static OutPacket removeDragon(Dragon dragon) {
        OutPacket outPacket = new OutPacket(OutHeader.DRAGON_REMOVE);

        outPacket.encodeInt(dragon.getOwnerId());

        return outPacket;
    }
}
