package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.movement.MovementInfo;
import net.swordie.ms.life.pet.Pet;

import java.util.List;

public class PetPacket {

    public static OutPacket activated(Pet pet) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_ACTIVATED);

        outPacket.encodeInt(pet.getOwnerID());
        outPacket.encodeInt(pet.getIdx());
        outPacket.encodeByte(true);
        outPacket.encodeByte(true); // init
        pet.encode(outPacket);

        return outPacket;
    }

    public static OutPacket deactivated(int ownerID, int petIdx) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_ACTIVATED);

        outPacket.encodeInt(ownerID);
        outPacket.encodeInt(petIdx);
        outPacket.encodeByte(false);
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket move(int id, int petID, MovementInfo movementInfo) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_MOVE);

        outPacket.encodeInt(id);
        outPacket.encodeInt(petID);
        outPacket.encode(movementInfo);

        return outPacket;
    }

    public static OutPacket action(int id, int petID, byte command1, byte command2, String sMsg) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_ACTION);

        outPacket.encodeInt(id);
        outPacket.encodeInt(petID);
        outPacket.encodeByte(command1);
        outPacket.encodeByte(command2);
        outPacket.encodeString(sMsg);

        return outPacket;
    }

    public static OutPacket actionSpeak(int id, int petID, String sMsg) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_ACTION_SPEAK);

        outPacket.encodeInt(id);
        outPacket.encodeInt(petID);
        outPacket.encodeString(sMsg);

        return outPacket;
    }

    public static OutPacket nameChanged(int id, int petID, String newName) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_NAME_CHANGED);

        outPacket.encodeInt(id);
        outPacket.encodeInt(petID);
        outPacket.encodeString(newName);

        return outPacket;
    }

    public static OutPacket loadExceptionList(int charID, Pet pet) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_LOAD_EXCEPTION_LIST);

        outPacket.encodeInt(charID);
        outPacket.encodeLong(pet.getIdx());
        if (pet.getItem().getExceptionList() != null) {
            outPacket.encodeByte(pet.getItem().getExceptionList().size());
            for (Integer i : pet.getItem().getExceptionList()) {
                outPacket.encodeInt(i);
            }
        } else {
            outPacket.encodeByte(0);
        }

        return outPacket;
    }

    public static OutPacket hueChanged(int id, int petID, int color) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_HUE_CHANGED);

        outPacket.encodeInt(id);
        outPacket.encodeInt(petID);
        outPacket.encodeInt(color);

        return outPacket;
    }

    public static OutPacket actionCommand(int id, int petID, byte command, boolean actSuccess) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_ACTION_COMMAND);

        outPacket.encodeInt(id);
        outPacket.encodeInt(petID);
        outPacket.encodeByte(1);
        outPacket.encodeByte(command);
        outPacket.encodeByte(actSuccess);
        outPacket.encodeByte(0);

        return outPacket;
    }

    public static OutPacket actionCommand_GiveFood(int id, int petID, int foodIDByPet) {
        OutPacket outPacket = new OutPacket(OutHeader.PET_ACTION_COMMAND);

        outPacket.encodeInt(id);
        outPacket.encodeInt(petID);
        outPacket.encodeByte(foodIDByPet);
        outPacket.encodeByte(0);
        outPacket.encodeInt(0);

        return outPacket;
    }
}
