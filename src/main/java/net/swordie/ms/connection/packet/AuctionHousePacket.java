package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.AuctionPacketEncoder;
import net.swordie.ms.connection.protocol.codec.AuctionPacketEncoder.Kind;
import net.swordie.ms.enums.auctionhouse.*;
import net.swordie.ms.world.shop.auctionhouse.AuctionItem;

import java.util.List;
import java.util.Set;

/**
 * Auction house packets. Header and the item slot of an entry come from the active {@link AuctionPacketEncoder}
 * (GMS 265: unchanged bytes; TMS 282.3: LP0A95..0A98 and the 282 item layout); the bodies are shared.
 */
public class AuctionHousePacket {

    private static AuctionPacketEncoder codec() {
        return AuctionPacketEncoder.active();
    }

    public static OutPacket msg(int type, int val) {
        OutPacket outPacket = codec().packet(Kind.MSG);

        outPacket.encodeByte(type);
        outPacket.encodeByte(val);

        return outPacket;
    }

    public static OutPacket on() {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_On.getVal());

        return outPacket;
    }

    public static OutPacket buy(Char chr, AuctionItem item) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_Buy.getVal());
        item.encode(outPacket, chr, System.currentTimeMillis(), codec()::encodeItem);
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket show(Char chr, Set<AuctionItem> items) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_Show.getVal());
        outPacket.encodeByte(200);
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(items.size());
        var now = System.currentTimeMillis();
        for (var item : items) {
            item.encode(outPacket, chr, now, codec()::encodeItem);
        }
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket marketPrice(Char chr, Set<AuctionItem> items) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_MarketPrice.getVal());
        outPacket.encodeByte(200);
        outPacket.encodeByte(1);
        outPacket.encodeByte(0);
        outPacket.encodeByte(0);
        outPacket.encodeInt(items.size());
        var now = System.currentTimeMillis();
        for (var item : items) {
            item.encode(outPacket, chr, now, codec()::encodeItem);
        }
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket loadSellingItems(Char chr, Set<AuctionItem> items) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_LoadSellingItems.getVal());
        outPacket.encodeInt(items.size());
        var now = System.currentTimeMillis();
        for (var item : items) {
            item.encode(outPacket, chr, now, codec()::encodeItem);
        }

        return outPacket;
    }

    public static OutPacket sell(Char chr, AuctionItem item) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_Sell.getVal());
        item.encode(outPacket, chr, System.currentTimeMillis(), codec()::encodeItem);
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket wishListShow(Char chr, List<AuctionItem> items) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_WishListShow.getVal());
        outPacket.encodeInt(items.size());
        var now = System.currentTimeMillis();
        for (var item : items) {
            item.encode(outPacket, chr, now, codec()::encodeItem);
        }

        return outPacket;
    }

    public static OutPacket loadSoldItems(Char chr, Set<AuctionItem> items) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_LoadSoldItems.getVal());
        outPacket.encodeInt(items.size());
        var now = System.currentTimeMillis();
        for (var item : items) {
            item.encode(outPacket, chr, now, codec()::encodeItem);
        }

        return outPacket;
    }

    public static OutPacket addSoldItems(Char chr, AuctionItem item) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_AddSoldItems.getVal());
        item.encode(outPacket, chr, System.currentTimeMillis(), codec()::encodeItem);
        outPacket.encodeByte(false);

        return outPacket;
    }

    public static OutPacket loadHistory(Char chr, Set<AuctionItem> items) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_LoadHistory.getVal());
        outPacket.encodeInt(items.size());
        for (var item : items) {
            item.encodeHistory(outPacket, chr, codec()::encodeItem);
        }

        return outPacket;
    }

    public static OutPacket addHistory(Char chr, AuctionItem item) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_AddHistory.getVal());
        item.encodeHistory(outPacket, chr, codec()::encodeItem);

        return outPacket;
    }

    public static OutPacket addWishList(Char chr, AuctionItem item) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_WishlistAdd.getVal());
        item.encode(outPacket, chr, System.currentTimeMillis(), codec()::encodeItem);

        return outPacket;
    }

    public static OutPacket removeWishList(Char chr, AuctionItem item) {
        OutPacket outPacket = codec().packet(Kind.ACTION);

        outPacket.encodeByte(AuctionHouseType.Res_WishListRemove.getVal());
        codec().encodeWishListRemoval(outPacket, chr, item);

        return outPacket;
    }

    public static OutPacket exit() {
        return codec().packet(Kind.EXIT);
    }
}
