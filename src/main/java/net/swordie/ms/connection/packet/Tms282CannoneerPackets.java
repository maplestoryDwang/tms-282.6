package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;

/** Native monkey companion switch. 282 CUserPool common case 5AA80E7 first reads the character ID,
 * then CUser 56746A0 reads one byte and sets the monkey-visible state at this+8300.
 * This is LP042C, not the unverified legacy USER_CALL_MONEY_EFFECT mapping. */
public final class Tms282CannoneerPackets {
    private Tms282CannoneerPackets() { }

    public static OutPacket monkey(int characterId, boolean visible) {
        OutPacket out = Tms282LoginPacketSupport.packet(0x042C);
        out.encodeInt(characterId);
        out.encodeByte(visible);
        return out;
    }
}
