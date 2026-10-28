package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.social.Friend.FriendResult;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

/** The GMS 265 buddy list result: the former body of {@code WvsContext.friendResult}, unchanged. */
public enum Gms265FriendPackets implements FriendPacketEncoder {
    INSTANCE;

    @Override
    public OutPacket friendResult(FriendResult friendResult) {
        OutPacket outPacket = new OutPacket(OutHeader.FRIEND_RESULT);

        friendResult.encode(outPacket);

        return outPacket;
    }
}
