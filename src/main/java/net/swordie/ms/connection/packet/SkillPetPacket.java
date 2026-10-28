package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.SkillPet;
import net.swordie.ms.life.movement.MovementInfo;

public class SkillPetPacket {

    public static OutPacket created(SkillPet skillpet) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_PET_TRANSFER_FIELD);

        outPacket.encodeInt(skillpet.getOwnerId());
        outPacket.encode(skillpet);

        return outPacket;
    }

    public static OutPacket move(int ownerID, MovementInfo mi) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_PET_MOVE);

        outPacket.encodeInt(ownerID);
        mi.encode(outPacket);

        return outPacket;
    }

    public static OutPacket action(SkillPet skillpet, String sMsg) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_PET_ACTION);

        outPacket.encodeInt(skillpet.getOwnerId());
        outPacket.encodeByte(skillpet.getActionType());
        outPacket.encodeByte(skillpet.getAction());
        outPacket.encodeString(sMsg);

        return outPacket;
    }

    public static OutPacket state(SkillPet skillpet) {
        OutPacket outPacket = new OutPacket(OutHeader.SKILL_PET_STATE);

        outPacket.encodeInt(skillpet.getOwnerId());
        outPacket.encodeInt(skillpet.getObjectId());
        outPacket.encodeByte(skillpet.getState());

        return outPacket;
    }
}
