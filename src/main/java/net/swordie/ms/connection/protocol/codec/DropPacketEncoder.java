package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.DropEnterType;
import net.swordie.ms.enums.DropLeaveType;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.util.Position;

/**
 * CDropPool packets whose wire form belongs to the profile: DROP_ENTER_FIELD and DROP_LEAVE_FIELD. The 265
 * builders {@link net.swordie.ms.connection.packet.DropPool#dropEnterField} / {@code dropLeaveField} (every
 * overload ends in the two full ones) ask the active profile for this family first and keep their 265 body as the
 * fallback, so every 265 drop flow (mob death, reactor, player drop, pickup, expiry) sends the profile's packet
 * without changing its call site. Signatures are those of the two full 265 builders.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none.
 */
public interface DropPacketEncoder {

    OutPacket enterField(Drop drop, DropEnterType dropEnterType, int rand, int dropSpeed, byte ownType, Position dropPos,
                         int sourceID, Position tempPos, int delay, boolean unkBool, short fallingVY,
                         boolean fadeInEffect, byte makeType, int collisionPickup, boolean prepareCollisionPickUp,
                         boolean canBePickedUpByPet);

    OutPacket leaveField(DropLeaveType dropLeaveType, int pickupID, int dropID, short delay, int petID, int key);
}
