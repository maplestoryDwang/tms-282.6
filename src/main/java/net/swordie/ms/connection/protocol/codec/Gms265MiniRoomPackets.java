package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.TradeRoom;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.MiniRoomType;
import net.swordie.ms.enums.RoomLeaveType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.Util;

/** The GMS 265 trade / battle RPS mini-room packets: the former bodies of {@code MiniroomPacket}, unchanged. */
public enum Gms265MiniRoomPackets implements MiniRoomPacketEncoder {
    INSTANCE;

    @Override
    public OutPacket enterTrade(Char chr, MiniRoomType miniRoomType) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.EnterTrade.getVal());
        outPacket.encodeByte(0);
        outPacket.encodeByte(miniRoomType.getVal());
        outPacket.encodeByte(2); // maxChars
        outPacket.encodeByte(0);

        outPacket.encodeByte(0);
        chr.getAvatarData().getAvatarLook().encode(outPacket);
        outPacket.encodeString(chr.getName());
        outPacket.encodeShort(chr.getJob());
        outPacket.encodeInt(0); // wing item
        outPacket.encodeByte(-1); // end indicator
        outPacket.encodeByte(0);

        return outPacket;
    }

    @Override
    public OutPacket accept(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.Accept.getVal());
        outPacket.encodeByte(1);
        chr.getAvatarData().getAvatarLook().encode(outPacket);
        outPacket.encodeString(chr.getName());
        outPacket.encodeShort(chr.getJob());
        outPacket.encodeInt(0); // wing item
        outPacket.encodeByte(0);

        return outPacket;
    }

    @Override
    public OutPacket enterTrade(TradeRoom tradeRoom, Char chr, MiniRoomType miniRoomType) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.EnterTrade.getVal());
        outPacket.encodeByte(0);
        outPacket.encodeByte(miniRoomType.getVal());
        outPacket.encodeByte(2); // maxChars
        outPacket.encodeByte(0);

        Char other = tradeRoom.getOtherChar(chr.getId());
        outPacket.encodeByte(0); // trade partner is always on the left
        other.getAvatarData().getAvatarLook().encode(outPacket);
        outPacket.encodeString(other.getName());
        outPacket.encodeShort(other.getJob());
        outPacket.encodeInt(0); // wing item

        outPacket.encodeByte(1);
        chr.getAvatarData().getAvatarLook().encode(outPacket);
        outPacket.encodeString(chr.getName());
        outPacket.encodeShort(chr.getJob());
        outPacket.encodeInt(0); // wing item

        outPacket.encodeByte(-1); // end indicator

        return outPacket;
    }

    @Override
    public OutPacket checkSSN2(Char chr) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.CheckSSN2.getVal());
        outPacket.encodeByte(16);
        outPacket.encodeByte(7);
        outPacket.encodeByte(1);
        outPacket.encodeInt(chr.getId());

        return outPacket;
    }

    @Override
    public OutPacket putItem(int user, int pos, Item item) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.PlaceItem.getVal());
        outPacket.encodeByte(user);
        outPacket.encodeByte(pos);
        item.encode(outPacket);

        return outPacket;
    }

    @Override
    public OutPacket putMoney(int user, long money) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.SetMesos.getVal());
        outPacket.encodeByte(user);
        outPacket.encodeLong(money);

        return outPacket;
    }

    @Override
    public OutPacket tradeRestraintItem() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.TradeRestraintItem.getVal());

        return outPacket;
    }

    @Override
    public OutPacket tradeInvite(Char chr, Char other, MiniRoomType miniRoomType) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.TradeInviteRequest.getVal());
        outPacket.encodeByte(miniRoomType.getVal());
        outPacket.encodeInt(chr.getId());
        outPacket.encodeString(chr.getName());
        outPacket.encodeInt(other.getId());
        outPacket.encodeInt(Util.getRandom(50, 60));

        return outPacket;
    }

    @Override
    public OutPacket cancelTrade() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.ExitTrade.getVal());
        outPacket.encodeByte(0); // other user cancelled
        outPacket.encodeByte(RoomLeaveType.TRLeave_TradeFail_Denied.getVal());

        return outPacket;
    }

    @Override
    public OutPacket tradeComplete() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.ExitTrade.getVal());
        outPacket.encodeByte(1);
        outPacket.encodeByte(RoomLeaveType.TRLeave_TradeDone.getVal());

        return outPacket;
    }

    @Override
    public OutPacket tradeConfirm() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.Trade.getVal());

        return outPacket;
    }

    @Override
    public OutPacket chat(Char chr, Char other, int pos, String msg) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.Chat0.getVal());
        outPacket.encodeByte(MiniRoomType.Chat.getVal());
        outPacket.encodeByte(pos);
        outPacket.encodeString(chr.getName());
        outPacket.encodeString(msg);
        outPacket.encodeInt(other.getId());
        outPacket.encodeInt(chr.getId());
        chr.encodeChatInfo(outPacket, msg);

        return outPacket;
    }

    @Override
    public OutPacket startRPS() {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.RPSStart.getVal());

        return outPacket;
    }

    @Override
    public OutPacket resultRPS(byte type) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.RPSResult.getVal());
        outPacket.encodeByte(type);
        // type = 0: kéo; 1: búa; 2: bao

        return outPacket;
    }

    @Override
    public OutPacket finishRPS(byte result, byte type) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.RPSFinish.getVal());
        outPacket.encodeByte(result);
        outPacket.encodeByte(type);
        // type = 0: kéo; 1: búa; 2: bao

        return outPacket;
    }
}
