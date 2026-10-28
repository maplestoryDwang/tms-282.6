package net.swordie.ms.handlers.user;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.SecondAtomCollision;
import net.swordie.ms.client.jobs.anima.Lara;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.SecondAtomPacket;
import net.swordie.ms.constants.JobConstants;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;

import java.util.*;

public class SecondAtomHandler {

    @Handler(op = InHeader.USER_SECOND_ATOM_COMMAND_REQUEST)
    public static void handleUserSecondAtomCommandRequest(Char chr, InPacket inPacket) {
        if (JobConstants.isLara(chr.getJob())) {
            inPacket.decodeByte();
            inPacket.decodeByte();
            inPacket.decodeInt();
            var secondAtomObjId = inPacket.decodeInt();
            var sa = chr.getSecondAtomById(secondAtomObjId);
            if (sa != null) {
                ((Lara) chr.getJobHandler()).secondAtomCommandRequest(sa);
            }

        } else {
            inPacket.decodeByte();
            byte[] arr = inPacket.decodeArr(inPacket.getUnreadAmount()); // Can't be arsed rn
            chr.getField().broadcast(SecondAtomPacket.secondAtomCommandResult(chr, arr), chr);
        }
    }

    @Handler(op = InHeader.USER_SECOND_ATOM_REMOVE_REQUEST)
    public static void handleUserSecondAtomRemoveRequest(Char chr, InPacket inPacket) {
        inPacket.skipInt();
        int objId = inPacket.decodeInt();
        if (chr.getJobHandler().handleSecondAtomRemoveRequest(objId)) { // Only used for additional things when a second atom is removed.
            chr.removeSecondAtom(objId);
        }
    }

    @Handler(op = InHeader.USER_SECOND_ATOM_COLLISION_REQUEST)
    public static void handleUserSecondAtomCollisionRequest(Char chr, InPacket inPacket) {
        int charID = inPacket.decodeInt();
        if (charID != chr.getId()) {
            return;
        }
        int size = inPacket.decodeInt();
        List<SecondAtomCollision> collisions = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            var collision = new SecondAtomCollision();
            collision.secondAtomObjId = inPacket.decodeInt();
            collision.mobObjId = inPacket.decodeInt();
            var map = new Int2IntOpenHashMap();
            var loop = inPacket.decodeInt();
            for (int ii = 0; ii < loop; ii++) {
                map.put(inPacket.decodeInt(), inPacket.decodeInt());
            }
            collision.map.putAll(map);
            collisions.add(collision);
        }
        chr.getJobHandler().handleSecondAtomCollisionRequest(collisions);
        chr.getField().broadcast(SecondAtomPacket.secondAtomCollisionResult(charID, collisions));
    }
}
