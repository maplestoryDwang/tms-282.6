package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.FoxMan;
import net.swordie.ms.life.movement.MovementInfo;

public class FoxManPacket {

    public static OutPacket created(FoxMan foxMan) {
        OutPacket outPacket = new OutPacket(OutHeader.FOX_MAN_ENTER_FIELD);

        outPacket.encodeInt(foxMan.getOwnerId());
        outPacket.encode(foxMan);

        return outPacket;
    }

    public static OutPacket removed(FoxMan foxMan) {
        OutPacket outPacket = new OutPacket(OutHeader.FOX_MAN_LEAVE_FIELD);

        outPacket.encodeInt(foxMan.getOwnerId());

        return outPacket;
    }

    public static OutPacket move(FoxMan foxMan, MovementInfo mi) {
        OutPacket outPacket = new OutPacket(OutHeader.FOX_MAN_MOVE);

        outPacket.encodeInt(foxMan.getOwnerId());
        mi.encode(outPacket);

        return outPacket;
    }

    public static OutPacket modified(FoxMan foxMan) {
        OutPacket outPacket = new OutPacket(OutHeader.FOX_MAN_MODIFIED);

        outPacket.encodeInt(foxMan.getOwnerId());

        if (foxMan.getEquip() != null) {
            Equip equip = foxMan.getEquip();
            outPacket.encodeByte(equip.getBagIndex());
            outPacket.encodeInt(equip.getItemId());
        } else {
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket update(FoxMan foxMan) {
        OutPacket outPacket = new OutPacket(OutHeader.FOX_MAN_EXCL_RESULT);

        outPacket.encodeInt(foxMan.getOwnerId());

        return outPacket;
    }

    public static OutPacket showChangeEffect(FoxMan foxMan) {
        OutPacket outPacket = new OutPacket(OutHeader.FOX_MAN_SHOW_CHANGE_EFFECT);

        outPacket.encodeInt(foxMan.getOwnerId());

        return outPacket;
    }
}
