package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Core;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.EvolvingSystemType;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.List;
import java.util.Set;

public class EvolvingPacket {

    public static OutPacket tryEnterEvolvingResult(List<Integer> coreList) {
        OutPacket outPacket = new OutPacket(OutHeader.EVOLVING_RESULT);

        outPacket.encodeByte(EvolvingSystemType.Res_TryEnter.getVal());
        for (Integer coreID : coreList) {
            outPacket.encodeInt(coreID);
        }

        return outPacket;
    }

    public static OutPacket evolvingSystemUIOperation(boolean isEnter, int count) {
        OutPacket outPacket = new OutPacket(OutHeader.EVOLVING_RESULT);

        outPacket.encodeByte(EvolvingSystemType.Res_CancelAccess.getVal());
        outPacket.encodeByte(isEnter);
        if (isEnter) {
            outPacket.encodeByte(count);
        }

        return outPacket;
    }

    public static OutPacket coreInventoryOperation(Set<Core> coreSet, EvolvingSystemType type) {
        OutPacket outPacket = new OutPacket(OutHeader.EVOLVING_RESULT);

        outPacket.encodeByte(EvolvingSystemType.Res_CoreInventoryOperation.getVal());
        outPacket.encodeInt(coreSet.size());
        for (Core core : coreSet) {
            outPacket.encodeByte(type.getVal());
            outPacket.encodeByte(core.getPos());
            if (type.getVal() == EvolvingSystemType.Update_Quantity.getVal()) {
                outPacket.encodeInt(core.getLeftCount());
            } else if (type.getVal() == EvolvingSystemType.Add.getVal()) {
                core.encode(outPacket);
            }

        }

        return outPacket;
    }

    public static OutPacket coreChangeSlotPositionResult(boolean success, byte fromSlotType, byte fromPos, byte toSlotType, byte toPos) {
        OutPacket outPacket = new OutPacket(OutHeader.EVOLVING_RESULT);

        outPacket.encodeByte(EvolvingSystemType.Res_CoreChangeSlotPositionResult.getVal());
        outPacket.encodeByte(success);
        if (!success) {
            outPacket.encodeByte(fromSlotType);
            outPacket.encodeByte(fromPos);
            outPacket.encodeByte(toSlotType);
            outPacket.encodeByte(toPos);
        }

        return outPacket;
    }

    public static OutPacket throwCore() {
        OutPacket outPacket = new OutPacket(OutHeader.EVOLVING_RESULT);

        outPacket.encodeByte(EvolvingSystemType.Res_ThrowCore.getVal());
        outPacket.encodeByte(0); // slotType
        outPacket.encodeByte(0); // pos
        outPacket.encodeInt(0); // leftcount

        return outPacket;
    }

    public static OutPacket coreInvenSort(Set<Core> coreSet) {
        OutPacket outPacket = new OutPacket(OutHeader.EVOLVING_RESULT);

        outPacket.encodeByte(EvolvingSystemType.Res_CoreInvenSort.getVal());
        outPacket.encodeInt(coreSet.size());
        for (Core core : coreSet) {
            outPacket.encodeInt(core.getPos());
            core.encode(outPacket);
        }

        return outPacket;
    }

}
