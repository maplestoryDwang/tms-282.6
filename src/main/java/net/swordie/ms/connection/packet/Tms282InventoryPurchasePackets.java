package net.swordie.ms.connection.packet;

import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.handlers.header.OutHeader;

import java.util.Optional;

/**
 * Inventory-window "+" slot purchase (UI/CashShop.img CSNotice dialog, ctor 61987E0, opened from CUIItem 4AEF310 and
 * kept at CUIItem+0x948 while modal). Opening it sends CP00AF (i32 account id) and sets pending [+0x2970]; confirm
 * (619AD50) is refused while pending, compares [+0x2978] (currency 1) or [+0x297C] (currency 2, mileage box) with the
 * price and sends CP00B0 (i32 account, i32 character, i32 item id [+0x2914], i32 SN [+0x291C], u8 currency).
 * <p>
 * Both replies are CWvsContext cases: LP01C9 (5DFA9B7) -> CUIItem 4B077E0 -> dialog 619D010 mode 0 sets the two
 * balances and clears pending; LP01CA (5DFAA1C) -> CUIItem 4B07800: result 0 = bought (balances, message 0xC53,
 * the client itself grows the current tab by the item's "delta", then closes), 2 = failure message 0xC48, other =
 * closes. The storage item 9110002 goes to the storage UI instead (462E570). Both also store the balances in
 * CWvsContext (+0x5650, +0x24E8).
 * <p>
 * Translator: 265 SET_MAPLE_POINT (i32 maplePoints) -> LP014A (5DF7763: u8 which, i32 value; which != 0 stores
 * CWvsContext+0x24E8 - the field LP01C9's second value (maplePoints) fills - else +0x24EC).
 */
public final class Tms282InventoryPurchasePackets implements Tms282LegacyPackets.Contributor {
    public static final Tms282InventoryPurchasePackets INSTANCE = new Tms282InventoryPurchasePackets();
    public static final int SET_MAPLE_POINT = 0x014A;
    public static final int BALANCE = 0x01C9;
    public static final int RESULT = 0x01CA;
    public static final int OK = 0, FAILED = 2;

    private Tms282InventoryPurchasePackets() {
    }

    @Override
    public void contribute(Tms282LegacyPackets.Builder builder) {
        builder.on(OutHeader.SET_MAPLE_POINT, Tms282InventoryPurchasePackets::setMaplePoint);
    }

    static Optional<OutPacket> setMaplePoint(OutPacket legacy) {
        Tms282LegacyPackets.Body body = Tms282LegacyPackets.Body.of(legacy);
        int maplePoints = body.i32();
        body.end();
        OutPacket out = Tms282LoginPacketSupport.packet(SET_MAPLE_POINT);
        out.encodeByte(1);
        out.encodeInt(maplePoints);
        return Optional.of(out);
    }

    /** LP01C9: i32 currency 1 (nxPrepaid, 楓點), i32 currency 2 (maplePoints, 里程). */
    public static OutPacket balance(int cash, int mileage) {
        OutPacket out = Tms282LoginPacketSupport.packet(BALANCE);
        out.encodeInt(cash);
        out.encodeInt(mileage);
        return out;
    }

    /** LP01CA: i32 item id, i32 cash, i32 mileage, u8 balance mode (0 = both), u8, u8 result. */
    public static OutPacket result(int itemId, int cash, int mileage, int result) {
        OutPacket out = Tms282LoginPacketSupport.packet(RESULT);
        out.encodeInt(itemId);
        out.encodeInt(cash);
        out.encodeInt(mileage);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeByte(result);
        return out;
    }
}
