package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.FieldAttackObj;

import java.util.List;

/**
 * @author Sjonnie
 * Created on 8/19/2018.
 */
public class FieldAttackObjPool {

    public static OutPacket objCreate(FieldAttackObj fieldAttackObj) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_ATTACK_CREATE);

        outPacket.encode(fieldAttackObj);

        return outPacket;
    }

    public static OutPacket objRemoveByKey(int objectID) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_ATTACK_REMOVE_BY_KEY);

        outPacket.encodeInt(objectID);

        return outPacket;
    }

    public static OutPacket setAttack(int objectId, int attackIdx) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_ATTACK_SET_ATTACK);

        outPacket.encodeInt(objectId);
        outPacket.encodeInt(attackIdx);

        return outPacket;
    }

    public static OutPacket resultBoard(FieldAttackObj fieldAttackObj, boolean success) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_ATTACK_RESULT_BOARD);

        outPacket.encodeInt(fieldAttackObj.getObjectId());
        outPacket.encodeByte(success);
        outPacket.encodeInt(fieldAttackObj.getReserveID());
        outPacket.encodeInt(fieldAttackObj.getOwnerId());

        return outPacket;
    }

    public static OutPacket resultGetOff(FieldAttackObj fieldAttackObj, boolean bool) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_ATTACK_RESULT_GET_OFF);

        outPacket.encodeInt(fieldAttackObj.getObjectId());
        outPacket.encodeByte(bool);

        return outPacket;
    }

    public static OutPacket pushAct(FieldAttackObj fieldAttackObj, int act) {
        OutPacket outPacket = new OutPacket(OutHeader.FIELD_ATTACK_PUSH_ACT);

        outPacket.encodeInt(fieldAttackObj.getObjectId());
        outPacket.encodeInt(act);
        outPacket.encodeByte(fieldAttackObj.isFlip());

        return outPacket;
    }
}
