package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.TradeRoom;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.MiniRoomType;

/**
 * Codec family of the trade / battle RPS mini-room packets (265 OutHeader.MINI_ROOM_BASE_DLG; TMS 282.3 LP097F). The 265
 * {@code MiniroomPacket} trade builders delegate here: GMS 265 registers no encoder and falls back to
 * {@link Gms265MiniRoomPackets} (the unchanged 265 bytes); TMS 282.3 registers {@code Tms282MiniRoomPackets}.
 */
public interface MiniRoomPacketEncoder {

    OutPacket enterTrade(Char chr, MiniRoomType miniRoomType);

    OutPacket accept(Char chr);

    OutPacket enterTrade(TradeRoom tradeRoom, Char chr, MiniRoomType miniRoomType);

    OutPacket checkSSN2(Char chr);

    OutPacket putItem(int user, int pos, Item item);

    OutPacket putMoney(int user, long money);

    OutPacket tradeRestraintItem();

    OutPacket tradeInvite(Char chr, Char other, MiniRoomType miniRoomType);

    OutPacket cancelTrade();

    OutPacket tradeComplete();

    OutPacket tradeConfirm();

    OutPacket chat(Char chr, Char other, int pos, String msg);

    OutPacket startRPS();

    OutPacket resultRPS(byte type);

    OutPacket finishRPS(byte result, byte type);

    /** The active profile's encoder (or its version's fallback), else the GMS 265 one. */
    static MiniRoomPacketEncoder active() {
        return ProfileEncoders.find(MiniRoomPacketEncoder.class).orElse(Gms265MiniRoomPackets.INSTANCE);
    }
}
