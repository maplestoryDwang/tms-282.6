package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.packet.DropPool;
import net.swordie.ms.connection.protocol.ProtocolVersion;
import net.swordie.ms.enums.DropEnterType;
import net.swordie.ms.enums.DropLeaveType;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.util.Position;

/**
 * TMS 282.3 CDropPool packets (dispatcher 0x3662180). Layouts: ingame-layouts-282.md 2.1 / 2.2; both packets were
 * read back by the emulated client (research/tms2823/combat-trace, Tms282DropCheck).
 * <ul>
 *     <li>LP074D DROP_ENTER_FIELD (handler 0x3662EF0): the 265 body field for field (35 reads) [verified], written
 *     by {@link DropPool#encodeDropEnterBody}. A money drop of 0 takes another client path (366350C: no expiry, no
 *     4 x i32 / double), so the amount is raised to 1.</li>
 *     <li>LP074E DROP_LEAVE_FIELD (handler 0x36697D0): {@code B type, I dropId}, then type 2/3: I pickupId; type 5:
 *     I pickupId, I petIndex; type 7: I key; <b>type 4: i32 delay</b> (265 wrote a short) [verified].</li>
 * </ul>
 */
public final class Tms282DropPackets implements DropPacketEncoder {
    public static final int DROP_ENTER_FIELD = 0x074D;
    public static final int DROP_LEAVE_FIELD = 0x074E;
    public static final Tms282DropPackets INSTANCE = new Tms282DropPackets();

    private Tms282DropPackets() {
    }

    private static OutPacket packet(int op) {
        return OutPacket.forProfile(ProtocolVersion.TMS_282_3, op);
    }

    @Override
    public OutPacket enterField(Drop drop, DropEnterType dropEnterType, int rand, int dropSpeed, byte ownType,
                                Position dropPos, int sourceID, Position tempPos, int delay, boolean unkBool,
                                short fallingVY, boolean fadeInEffect, byte makeType, int collisionPickup,
                                boolean prepareCollisionPickUp, boolean canBePickedUpByPet) {
        if (drop.isMoney() && drop.getMoney() <= 0) {
            drop.setMoney(1);
        }
        OutPacket out = packet(DROP_ENTER_FIELD);
        DropPool.encodeDropEnterBody(out, drop, dropEnterType, ownType, dropPos, sourceID, tempPos, delay, unkBool,
                fallingVY, fadeInEffect, collisionPickup, prepareCollisionPickUp, canBePickedUpByPet);
        return out;
    }

    @Override
    public OutPacket leaveField(DropLeaveType dropLeaveType, int pickupID, int dropID, short delay, int petID, int key) {
        return leaveField(dropLeaveType, pickupID, dropID, (int) delay, petID, key);
    }

    /** LP074E with the 282 i32 delay of type 4 ({@link DropLeaveType#DelayedPickup}). */
    public OutPacket leaveField(DropLeaveType dropLeaveType, int pickupID, int dropID, int delay, int petID, int key) {
        OutPacket out = packet(DROP_LEAVE_FIELD);
        out.encodeByte(dropLeaveType.getVal());
        out.encodeInt(dropID);
        switch (dropLeaveType) {
            case CharPickup, CharPickup2 -> out.encodeInt(pickupID);
            case PetPickup -> {
                out.encodeInt(pickupID);
                out.encodeInt(petID);
            }
            case DelayedPickup -> out.encodeInt(delay);
            case Absorb -> out.encodeInt(key);
            default -> {
            }
        }
        return out;
    }
}
