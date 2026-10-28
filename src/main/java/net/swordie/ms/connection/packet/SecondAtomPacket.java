package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SecondAtom;
import net.swordie.ms.client.character.skills.SecondAtomCollision;
import net.swordie.ms.client.jobs.flora.Adele;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SecondAtomPacket {

    public static OutPacket secondAtomCommandResult(Char chr, byte[] arr) {
        OutPacket outPacket = new OutPacket(OutHeader.SECOND_ATOM_COMMAND_RESULT);

        outPacket.encodeInt(chr.getId());
        outPacket.encodeArr(arr);

        return outPacket;
    }

    public static OutPacket createSecondAtoms(int charID, List<SecondAtom> secondAtoms) {
        OutPacket outPacket = new OutPacket(OutHeader.CREATE_INFINITY_BLADE_ATOM);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(secondAtoms.size()); //Size of SecondAtoms
        AtomicBoolean infinity = new AtomicBoolean(false);
        secondAtoms.forEach(it -> {
            //it.encode(outPacket);
            outPacket.encodeInt(it.getObjectID());
            outPacket.encodeInt(0);
            outPacket.encodeInt(it.getDataIndex()); //DataIndex
            outPacket.encodeInt(it.getKey()); //Key
            outPacket.encodeInt(it.getCharId());
            outPacket.encodeInt(it.getTargetID()); //Target

            outPacket.encodeInt(it.getCreateDelay()); // createDelay
            outPacket.encodeInt(it.getEnableDelay()); // enableDelay
            outPacket.encodeInt(it.getRotate()); // Rotate
            outPacket.encodeInt(it.getSkillId()); //skillId
            outPacket.encodeInt(it.getFirstAngleRange());
            outPacket.encodeInt(it.getFirstAngleStart());
            outPacket.encodeInt(it.getExpire()); // expire
            outPacket.encodeInt(it.getCustomRotate()); // angle
            outPacket.encodeInt(it.getAttackableCount()); // attackableCount | 1

            outPacket.encodeInt(it.getCollisionCheck()); // collisionCheck
            outPacket.encodeInt(0); //  getSkillId() == 400031066 ? 0x3 : 0

            outPacket.encodePositionInt(it.getPosition());

            outPacket.encodeByte(it.isLocalOnly()); // new 218 | tile.getSkillId() == 400011119 ???
            outPacket.encodeByte(false);

            outPacket.encodeByte(it.isSpecialAtom()); // ATTACK
            outPacket.encodeByte(false);
            outPacket.encodeByte(false);

            final var customs = it.getCustoms();
            outPacket.encodeInt(customs.size());
            for (int i = 0; i < customs.size(); i++) {
                outPacket.encodeInt(customs.get(i));
            }

            outPacket.encodeInt(it.getUnk());
            outPacket.encodeInt(0);
            outPacket.encodeLong(0);
            outPacket.encodeByte(false);
            outPacket.encodeByte(false);

            if (it.getSkillId() == Adele.INFINITY_BLADE) {
                infinity.set(true);
            }
        });
        outPacket.encodeInt(infinity.get() ? 1 : 0);

        return outPacket;
    }

    public static OutPacket secondAtomAttack(Char chr, int fakey, int level) {
        OutPacket outPacket = new OutPacket(OutHeader.SECOND_ATOM_ATTACK);

        outPacket.encodeInt(chr.getId());

        outPacket.encodeInt(fakey);
        outPacket.encodeInt(level); //Animate?

        return outPacket;
    }

    public static OutPacket secondAtomCollisionResult(int charID, List<SecondAtomCollision> collisions) {
        OutPacket outPacket = new OutPacket(OutHeader.SECOND_ATOM_COLLISION_RESULT);

        outPacket.encodeInt(charID);
        outPacket.encodeInt(collisions.size());
        for (var collision : collisions) {
            collision.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket removeSecondAtom(Char chr, int oid) {
        OutPacket outPacket = new OutPacket(OutHeader.REMOVE_SECOND_ATOM);

        outPacket.encodeInt(chr.getId());

        int size = 1; //AtomSize
        outPacket.encodeInt(size);
        for (int z = 0; z < size; z++) {
            outPacket.encodeInt(oid);
        }
        outPacket.encodeInt(0);
        outPacket.encodeInt(6);

        return outPacket;
    }
}
