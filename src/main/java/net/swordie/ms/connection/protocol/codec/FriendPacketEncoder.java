package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.social.Friend.FriendResult;
import net.swordie.ms.connection.OutPacket;

/**
 * Codec family of the buddy list result (265 OutHeader.FRIEND_RESULT; TMS 282.3 LP00DB). The 265
 * {@code WvsContext.friendResult} builds through {@link #active()}, so a profile only registers its own encoder:
 * GMS 265 registers none and falls back to {@link Gms265FriendPackets} (the unchanged 265 bytes); TMS 282.3 registers
 * {@code Tms282FriendPackets}.
 */
public interface FriendPacketEncoder {

    /** One buddy list result; a result type the profile has no layout for goes out as the 265 packet (guard-dropped). */
    OutPacket friendResult(FriendResult friendResult);

    /** The active profile's encoder, else the GMS 265 one. */
    static FriendPacketEncoder active() {
        return ProfileEncoders.find(FriendPacketEncoder.class).orElse(Gms265FriendPackets.INSTANCE);
    }
}
