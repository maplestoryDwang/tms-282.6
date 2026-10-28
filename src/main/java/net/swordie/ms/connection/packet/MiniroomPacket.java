package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.TradeRoom;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.MiniRoomPacketEncoder;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.MiniRoomType;
import net.swordie.ms.enums.RoomLeaveType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.life.Merchant.BoughtItem;
import net.swordie.ms.life.Merchant.Merchant;
import net.swordie.ms.life.Merchant.MerchantItem;
import net.swordie.ms.util.Util;

/**
 * Mini-room (MINI_ROOM_BASE_DLG) packets. The trade / battle RPS builders go through the profile codec
 * {@link MiniRoomPacketEncoder} (GMS 265: {@link net.swordie.ms.connection.protocol.codec.Gms265MiniRoomPackets}, the former
 * bodies, unchanged; TMS 282.3: {@code Tms282MiniRoomPackets}); the hired-merchant builders are still 265-only.
 */
public class MiniroomPacket {

    public static OutPacket enterTrade(Char chr, MiniRoomType miniRoomType) {
        return MiniRoomPacketEncoder.active().enterTrade(chr, miniRoomType);
    }

    public static OutPacket accept(Char chr) {
        return MiniRoomPacketEncoder.active().accept(chr);
    }

    public static OutPacket enterTrade(TradeRoom tradeRoom, Char chr, MiniRoomType miniRoomType) {
        return MiniRoomPacketEncoder.active().enterTrade(tradeRoom, chr, miniRoomType);
    }

    public static OutPacket checkSSN2(Char chr) {
        return MiniRoomPacketEncoder.active().checkSSN2(chr);
    }


    public static OutPacket putItem(int user, int pos, Item item) {
        return MiniRoomPacketEncoder.active().putItem(user, pos, item);
    }

    public static OutPacket putMoney(int user, long money) {
        return MiniRoomPacketEncoder.active().putMoney(user, money);
    }

    public static OutPacket tradeRestraintItem() {
        return MiniRoomPacketEncoder.active().tradeRestraintItem();
    }

    public static OutPacket tradeInvite(Char chr, Char other, MiniRoomType miniRoomType) {
        return MiniRoomPacketEncoder.active().tradeInvite(chr, other, miniRoomType);
    }

    public static OutPacket cancelTrade() {
        return MiniRoomPacketEncoder.active().cancelTrade();
    }

    public static OutPacket tradeComplete() {
        return MiniRoomPacketEncoder.active().tradeComplete();
    }

    public static OutPacket tradeConfirm() {
        return MiniRoomPacketEncoder.active().tradeConfirm();
    }

    public static OutPacket chat(Char chr, Char other, int pos, String msg) {
        return MiniRoomPacketEncoder.active().chat(chr, other, pos, msg);
    }

    public static OutPacket enterMerchant(Char character, Merchant merchant, boolean firsttime) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.EnterTrade.getVal());
        outPacket.encodeByte(6); // action
        outPacket.encodeByte(GameConstants.MAX_MERCHANT_VISITORS + 1); // number of slots
        if (character.getId() == merchant.getOwnerID()) {
            outPacket.encodeShort(0); //my position
        } else {
            outPacket.encodeShort(merchant.getVisitors().indexOf(character) + 1); //my position
        }
        outPacket.encodeInt(merchant.getItemID());
        outPacket.encodeString("Hired Merchant");
        for (byte i = 0; i < merchant.getVisitors().size(); i++) {
            outPacket.encodeByte(i + 1);
            merchant.getVisitors().get(i).getAvatarData().getAvatarLook().encode(outPacket);
            outPacket.encodeString(merchant.getVisitors().get(i).getName());
            outPacket.encodeShort(merchant.getVisitors().get(i).getJob());
        }
        outPacket.encodeByte(-1);
        outPacket.encodeShort(0);
        outPacket.encodeString(merchant.getOwnerName());
        if (merchant.getOwnerID() == character.getId()) {
            int timeleft = merchant.getTimeLeft();
            outPacket.encodeInt(timeleft);
            outPacket.encodeByte(firsttime ? 1 : 0);
            outPacket.encodeByte(merchant.getBoughtitems().size());
            for (BoughtItem item : merchant.getBoughtitems()) {
                outPacket.encodeInt(item.id);
                outPacket.encodeShort(item.quantity);
                outPacket.encodeLong(item.totalPrice);
                outPacket.encodeString(item.buyer);
            }
            outPacket.encodeLong(merchant.getMesos());
        }
        outPacket.encodeInt(263);
        outPacket.encodeString(merchant.getMessage());
        outPacket.encodeByte(GameConstants.MAX_MERCHANT_SLOTS); //size
        outPacket.encodeLong(merchant.getMesos());
        outPacket.encodeByte(merchant.getItems().size());
        for (MerchantItem item : merchant.getItems()) {
            outPacket.encodeShort(item.bundles);
            outPacket.encodeShort(item.item.getQuantity());
            outPacket.encodeLong(item.price);
            item.item.encode(outPacket);
        }
        outPacket.encodeShort(0);
        return outPacket;
    }

    public static OutPacket shopVisitorAdd(Char chr, int slot) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.Accept.getVal());

        outPacket.encodeByte(slot);
        chr.getAvatarData().getAvatarLook().encode(outPacket);
        outPacket.encodeString(chr.getName());
        outPacket.encodeShort(chr.getJob());

        return outPacket;
    }

    public static OutPacket shopVisitorRemove(Char chr, int slot) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.Accept.getVal());
        outPacket.encodeByte(slot);

        return outPacket;
    }

    public static OutPacket openShop(Merchant merchant) {

        OutPacket outPacket = new OutPacket(OutHeader.EMPLOYEE_ENTER_FIELD);


        outPacket.encodeInt(merchant.getOwnerID());
        outPacket.encodeInt(merchant.getItemID());
        outPacket.encodePosition(merchant.getPosition());
        outPacket.encodeShort(merchant.getFh());
        outPacket.encodeString(merchant.getOwnerName());
        int itemID = merchant.getItemID();
        byte type = 6;
        /*
        if(itemID>=5030000&&itemID<=5030001) //elf
            {type=6;}
        if(itemID>=5030002&&itemID<=5030003) //bear
        {type=20;}
        if(itemID>=5030004&&itemID<=5030005) //robo
        {type=8;}
        if(itemID>=5030008&&itemID<=5030009) //cute girl
        {type=9;}
        if(itemID>=5030010&&itemID<=5030011) //grandma
        {type=10;}
        if(itemID==5030012) //mustach guy
        {type=11;}
        */
        outPacket.encodeByte(type);
        outPacket.encodeInt(merchant.getObjectId());
        outPacket.encodeString(merchant.getMessage());
        outPacket.encodeByte(merchant.getShopHasPassword());
        outPacket.encodeByte(merchant.getItems().size());
        outPacket.encodeByte(GameConstants.MAX_MERCHANT_SLOTS);
        outPacket.encodeByte(merchant.getOpen());


        return outPacket;
    }

    public static OutPacket destroyShop(Merchant merchant) {
        OutPacket outPacket = new OutPacket(OutHeader.EMPLOYEE_LEAVE_FIELD);

        outPacket.encodeInt(merchant.getOwnerID());

        return outPacket;
    }

    public static OutPacket shopItemUpdate(Merchant merchant) {
        OutPacket outPacket = new OutPacket(OutHeader.MINI_ROOM_BASE_DLG);

        outPacket.encodeInt(MiniRoomType.Update.getVal());
        outPacket.encodeLong(0L);
        outPacket.encodeByte(merchant.getItems().size());
        for (MerchantItem item : merchant.getItems()) {
            outPacket.encodeShort(item.bundles);
            outPacket.encodeShort(item.item.getQuantity());
            outPacket.encodeLong(item.price);
            item.item.encode(outPacket);
        }
        outPacket.encodeShort(0);

        return outPacket;
    }

    public static OutPacket startRPS() {
        return MiniRoomPacketEncoder.active().startRPS();
    }


    public static OutPacket resultRPS(byte type) {
        return MiniRoomPacketEncoder.active().resultRPS(type);
    }


    public static OutPacket finishRPS(byte result, byte type) {
        return MiniRoomPacketEncoder.active().finishRPS(result, type);
    }
}
