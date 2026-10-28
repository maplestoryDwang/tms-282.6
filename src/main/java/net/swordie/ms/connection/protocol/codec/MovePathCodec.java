package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.life.movement.MovementInfo;

/**
 * Wire format of a CMovePath (USER_MOVE, MOB_MOVE, NPC_MOVE, pets, summons... and their remote broadcasts):
 * {@code I I S S S S S count × element, B keyPad n, ceil(n/2) B}. The header and key pad are the same in every
 * profile; the element types and the element tail differ (265: 4-byte tail; 282: 5-byte tail, renumbered types).
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; one stateless instance per profile.
 */
public interface MovePathCodec {

    /** Reads a whole path at the current position; the packet is left right after the key pad bytes. */
    MovementInfo decode(InPacket inPacket);

    /**
     * Writes a path in this profile's format.
     *
     * @throws IllegalArgumentException when the path holds an element this profile cannot write (for example a
     *                                  path decoded by another profile's codec)
     */
    void encode(OutPacket outPacket, MovementInfo path);

    /**
     * Writes a path inside a server broadcast (REMOTE_MOVE, MOB_MOVE, ...). 282 reads it with 3FDD1D0(keyPad = 0):
     * header and elements, no key pad. 265 builders write the whole path ({@link #encode}).
     */
    void encodeBroadcast(OutPacket outPacket, MovementInfo path);
}
