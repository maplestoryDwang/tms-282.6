package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;

/**
 * CUserPool packets whose body differs between profiles: USER_ENTER_FIELD and USER_LEAVE_FIELD. The 265 builders
 * {@link net.swordie.ms.connection.packet.UserPool#userEnterField} / {@code userLeaveField} ask the active profile
 * for this family first and keep their own 265 body as the fallback, so every 265 flow that shows or hides a user
 * (field entry, warp, disconnect, GM commands) sends the profile's packet without changing its call site.
 * <p>
 * Codec family of {@link net.swordie.ms.connection.protocol.ProfileCodecs}; the GMS 265 profile registers none.
 */
public interface UserPoolPacketEncoder {

    /** Shows {@code chr} to the other users of its field. */
    OutPacket userEnterField(Char chr);

    /** Removes {@code chr} from the other users' field. */
    OutPacket userLeaveField(Char chr);
}
