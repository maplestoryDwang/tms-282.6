package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.ShopPacketEncoder;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.shop.BuyLimitInfo;
import net.swordie.ms.world.shop.NpcShopDlg;
import net.swordie.ms.world.shop.NpcShopItem;
import net.swordie.ms.world.shop.result.BuyShopResult;
import net.swordie.ms.world.shop.result.RechargeShopResult;
import net.swordie.ms.world.shop.result.SellShopResult;
import net.swordie.ms.world.shop.result.ShopResult;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * TMS 282.3 NPC shop packets, the {@link ShopPacketEncoder} of the 282 profile. The client's CShopDlg::OnPacket is
 * the static dispatcher 4343AA0 (f(nType, CInPacket*)): nType 0x962 opens a shop, 0x963 is a shop result. The earlier
 * in-game map guessed LP0961 for SHOP_OPEN; 0x961 is another dialog (2BEDF00, u8 sub-type 0x4C..0x4E). 0x962/0x963
 * keep the Delta +33 of the verified LP0960 SCRIPT_MESSAGE (265 2367), and 265 SHOP_OPEN / SHOP_RESULT are 2369 /
 * 2370. Traced with research/tms2823/daily-trace/daily_trace.py (see its README).
 * <p>
 * LP0962 SHOP_OPEN, extended frame {@code MAIN || AUX || u32 len(AUX)}:
 * <pre>
 * MAIN i32 npcTemplateId (4344CEA), u8 hasPet (4344CF9), [i32 petTemplateId (4344D05)]
 * AUX  u8 (4344D84, stored at dlg+0x31D0; 0)
 * MAIN i32, i32, i32 (433A9F5 / 433AA04 / 433AA13; 265 has two), str + i32 (8E5730), u16 count (433AAB6; 265 has a
 *      u8 quest flag before it), then count x {i32 buyLimit (433AB10), NpcShopItem (F245B0), ...}
 * </pre>
 * NpcShopItem F245B0 reads one i32 more than 265 after pointPrice, then 4 bytes from the AUX stream (F246A0), and its
 * tail after the quest strings differs from 265 (quantity is an i32, see {@link #encodeItem}).
 */
public final class Tms282ShopPackets implements ShopPacketEncoder {
    public static final int SHOP_OPEN = 0x0962;
    public static final int SHOP_RESULT = 0x0963;
    /** LP0963 result types (u8, jump table 4344DDC, 0..0x31). */
    public static final int RESULT_BUY = 0;
    public static final int RESULT_LIST = 12;
    public static final int RESULT_DONE = 18;
    /**
     * NpcShopItem+0x60 (the 4 AUX bytes, F246A0) is a VIP-grade requirement, -1 = none (43621D5 compares it with -1).
     * 0..3 make the row (43486C0) and the buy dialog (43616B5) ask the VIP manager (6B9E330 / 6B83710: grade >= 2..5):
     * without it the row is disabled and 4349B78 replaces the price text with the currency name alone ("楓幣", no
     * amount), and the buy dialog refuses. 0 was sent before; see research/tms2823/shop-overlap-0929.
     */
    static final int NO_VIP_GRADE = -1;
    /**
     * NpcShopItem+0x1A0 / +0x1A8 (strings F24CD3 / F24D21) are an atoi window: 435F9F0 is true only when
     * atoi(+0x1A0) <= dlg+0x3180 (the header i32 433AA04, sent 0) < atoi(+0x1A8). Outside it the row (4349145 ->
     * 434914B..434935F) draws the name a second time in font dlg+0x3178 at (x+0x2B, y+4) over the normal name
     * (434A2BF), and the buy action (43525F0) stops with StringPool 0x4CAA. Two empty strings (0 <= 0 < 0) were
     * always outside; an end of INT_MAX keeps every item inside. The strings are never shown: the tooltip copy of a
     * row (4360DA0) does not take them, only 435F9F0 reads them.
     */
    static final String SALE_WINDOW_END = "2147483647";
    /**
     * Sale period FILETIMEs F24943 / F24955 (item+0xB0 / +0xE0). The row copy for the tooltip (4360DA0) puts them at
     * +0x24 / +0x2C, and the tooltip section 5211E60 adds 「%04d年 %d月 %d日 %02d時 %02d分開始」 (StringPool 0x32A7) when
     * start != [879FE90] and start is in the future, and 「... %02d分為止」 (0x32A8) + 「可購買」 (0x32A9) whenever
     * end != [879FE98]. Those two client constants are 1900-01-01 and 2079-01-01, i.e. FileTime MIN_TIME / MAX_TIME
     * as written by encodeFT. The DB default sellend '9999-01-01 00:00:01' (parsed in the server time zone, UTC+8)
     * became 9998-12-31 16:00 and showed 「9998年 12月 31日 16時 00分為止 可購買」 on every item; see
     * research/tms2823/shop-overlap-0929/TAB-DATE.md. Open ends are therefore sent as the client's own sentinels.
     */
    static final long SALE_START_OPEN = 0x014F373BFDE04000L;  // [879FE90] 1900-01-01 00:00
    static final long SALE_END_OPEN = 0x0217E646BB058000L;    // [879FE98] 2079-01-01 00:00
    /** Starts before 2000-01-01 (the DB default 1970-01-01) are sent as SALE_START_OPEN. */
    static final long SALE_START_OPEN_BEFORE = 0x01BF53EB256D4000L;
    /**
     * NpcShopItem+0x20 (F245E6) is the tab key. 433A900 groups the rows by it (hash map, 433B331); with one key the
     * dialog shows one tab labelled UINpcShop.img/Shop/tabString/BasicShopTab/0 「全體」 (433C3B8), with two or more
     * one tab per key labelled CustumShopTab/&lt;key&gt; (1 裝備, 2 消耗, 3 裝飾, 4 其他, ...; 433BD69), and key 0 has
     * no label there (433BEE6 -> 433C1E7 inserts an empty string). Repurchase rows (with an item) never enter the map:
     * they form the 「再次購買」 tab (BasicShopTab/4, 433C4AA). The 265 DB tabindex (0 / 1 in the general stores) meant
     * neither, so the key is the inventory type of the item: one type -> 「全體」, mixed -> labelled type tabs.
     * {@code -Dtms282.shop.categoryTabs=false} sends the DB tabindex unchanged.
     */
    static final int TAB_EQUIP = 1;
    static final int TAB_CONSUME = 2;
    static final int TAB_INSTALL = 3;
    static final int TAB_ETC = 4;

    private final Charset charset;

    public Tms282ShopPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    @Override
    public OutPacket openShop(int petTemplateId, NpcShopDlg nsd) {
        List<NpcShopItem> repurchase = nsd.getChar() == null ? List.of() : nsd.getChar().getRepurchaseItems();
        return openShop(nsd.getNpcTemplateID(), petTemplateId, nsd.getItems(), repurchase, charset);
    }

    @Override
    public OutPacket shopResult(ShopResult shopResult) {
        Objects.requireNonNull(shopResult, "shopResult");
        if (shopResult instanceof BuyShopResult) {
            OutPacket gms265 = new OutPacket((short) 0);
            shopResult.encode(gms265);
            ByteBuffer b = ByteBuffer.wrap(gms265.getData(), 2, gms265.getData().length - 2).order(ByteOrder.LITTLE_ENDIAN);
            if (b.get() != 0) {
                return buyRepurchased(b.getInt());
            }
            return bought(b.getInt(), b.getInt());
        }
        NpcShopDlg shop = shopResult instanceof SellShopResult sell ? sell.getShop()
                : shopResult instanceof RechargeShopResult recharge ? recharge.getShop() : null;
        if (shop != null) {
            List<NpcShopItem> repurchase = shop.getChar() == null ? List.of() : shop.getChar().getRepurchaseItems();
            return listRefresh(shop.getItems(), repurchase, charset);
        }
        System.out.println("[TMS282] LP0963 shop result " + shopResult.getType()
                + " has no traced 282 message; sent the no-op result " + RESULT_DONE + " (clears the pending request)");
        return done();
    }

    /** Result 0 (4343BB8): an item was bought: u8 0, i32 itemId, i32 amountLeft (265 adds a star-coin i32). */
    public static OutPacket bought(int itemId, int amountLeft) {
        OutPacket out = Tms282LoginPacketSupport.packet(SHOP_RESULT);
        out.encodeByte(RESULT_BUY);
        out.encodeByte(0);                                          // 4343BBB
        out.encodeInt(itemId);                                      // 4343BE2
        out.encodeInt(amountLeft);                                  // 4343BED
        return out;
    }

    /** Result 0 (4343BB8): a repurchase entry was bought back: u8 1, i32 index (4343BCE). */
    public static OutPacket buyRepurchased(int index) {
        OutPacket out = Tms282LoginPacketSupport.packet(SHOP_RESULT);
        out.encodeByte(RESULT_BUY);
        out.encodeByte(1);
        out.encodeInt(index);
        return out;
    }

    /** Result 12 (4343EA0): the whole list again (433A900, AUX 4 bytes per entry), after a sale or a recharge. */
    public static OutPacket listRefresh(List<NpcShopItem> items, List<NpcShopItem> repurchase, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(SHOP_RESULT);
        ByteArrayOutputStream aux = new ByteArrayOutputStream();
        out.encodeByte(RESULT_LIST);
        encodeList(out, aux, items, repurchase, charset);
        out.setAuxiliaryData(aux.toByteArray());
        return out;
    }

    /**
     * Result 18: no body (the jump table sends 18 straight to the epilogue 4344DB8). Every result first clears the
     * dialog's request-pending flag (dlg+0x9E8, 4343B66), so this releases a request whose outcome has no 282 message.
     */
    public static OutPacket done() {
        OutPacket out = Tms282LoginPacketSupport.packet(SHOP_RESULT);
        out.encodeByte(RESULT_DONE);
        return out;
    }

    /** LP0962 for {@code items} then {@code repurchase} (listed after the shop's own items, as 265). */
    public static OutPacket openShop(int npcTemplateId, int petTemplateId, List<NpcShopItem> items,
                                     List<NpcShopItem> repurchase, Charset charset) {
        OutPacket out = Tms282LoginPacketSupport.packet(SHOP_OPEN);
        ByteArrayOutputStream aux = new ByteArrayOutputStream();
        out.encodeInt(npcTemplateId);
        out.encodeByte(petTemplateId != 0);
        if (petTemplateId != 0) {
            out.encodeInt(petTemplateId);
        }
        aux.write(0);
        encodeList(out, aux, items, repurchase, charset);
        out.setAuxiliaryData(aux.toByteArray());
        return out;
    }

    static void encodeList(OutPacket out, ByteArrayOutputStream aux, List<NpcShopItem> items, List<NpcShopItem> repurchase,
                           Charset charset) {
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeInt(0);
        Tms282LoginPacketSupport.str(out, "", charset);
        out.encodeInt(0);
        out.encodeShort(items.size() + repurchase.size());
        int index = 0;
        for (NpcShopItem nsi : items) {
            encodeEntry(out, aux, nsi, index++, charset);
        }
        index = 0;
        for (NpcShopItem nsi : repurchase) {
            encodeEntry(out, aux, nsi, index++, charset);
        }
    }

    static void encodeEntry(OutPacket out, ByteArrayOutputStream aux, NpcShopItem nsi, int index, Charset charset) {
        int buyLimit = nsi.getBuyLimit() == 0 ? -1 : nsi.getBuyLimit();
        out.encodeInt(buyLimit);                                    // 433AB10
        encodeItem(out, aux, nsi, index, charset);
        out.encodeByte(0);                                          // 433AB2D
        Item item = nsi.getItem();
        out.encodeByte(item != null);                               // 433AB6F
        if (item != null) {
            Tms282ItemEncoder.encode(out, item, charset);           // 433AD19 -> 932790 GW_ItemSlotBase
        }
    }

    /** NpcShopItem::Decode F245B0. */
    static void encodeItem(OutPacket out, ByteArrayOutputStream aux, NpcShopItem nsi, int index, Charset charset) {
        int buyLimit = nsi.getBuyLimit() == 0 ? -1 : nsi.getBuyLimit();
        out.encodeInt(index);                                       // F245D0
        out.encodeInt(nsi.getItemID());                             // F245DB
        out.encodeInt(tabKey(nsi));                                 // F245E6 item+0x20 tab key (see TAB_EQUIP)
        out.encodeInt(buyLimit);                                    // F245F1
        out.encodeInt(1440 * nsi.getItemPeriod());                  // F245FC
        out.encodeFT(FileTime.MIN_TIME());                          // F24611 Buf8
        out.encodeInt(0);                                           // F24623 Buf4
        out.encodeLong(price(nsi));                                 // F24631
        out.encodeInt(nsi.getTokenItemID());                        // F2463D
        out.encodeInt(nsi.getTokenPrice());                         // F24648
        out.encodeInt(nsi.getPointQuestID());                       // F24653
        out.encodeInt(0);                                           // F2465E
        out.encodeInt(nsi.getPointPrice());                         // F24669
        out.encodeInt(0);                                           // F24674 (282 only)
        writeInt(aux, NO_VIP_GRADE);                                // F246A0 AUX 4 bytes -> item+0x60 (see NO_VIP_GRADE)
        out.encodeByte(0);                                          // F246B0 (non-zero: F234C0 block)
        out.encodeInt(0);                                           // F247FE
        out.encodeInt(0);                                           // F2480C
        out.encodeInt(0);                                           // F2481A
        encodeBuyLimit(out, nsi.getBuyLimitInfo());                 // F21F70
        out.encodeInt(nsi.getShowLevMin());                         // F248F8
        out.encodeShort(nsi.getShowLevMin());                       // F24906
        out.encodeShort(0);                                         // F24917
        out.encodeByte(0);                                          // F24928
        out.encodeLong(saleStart(nsi.getSellStart()));              // F24943 item+0xB0 (see SALE_START_OPEN)
        out.encodeLong(saleEnd(nsi.getSellEnd()));                  // F24955 item+0xE0
        out.encodeInt(0);                                           // F249E7
        out.encodeShort(1);                                         // F249F5
        out.encodeByte(nsi.isWorldBlock());                         // F24A06
        out.encodeInt(nsi.getQuestExID());                          // F24A19
        Tms282LoginPacketSupport.str(out, nsi.getQuestExKey() == null ? "" : nsi.getQuestExKey(), charset); // F24A2B
        out.encodeInt(1);                                           // F24A75
        out.encodeInt(0);                                           // F24A83
        Tms282LoginPacketSupport.str(out, "", charset);             // F24A95
        out.encodeInt(0);                                           // F24ADF
        out.encodeInt(0);                                           // F24AED
        Tms282LoginPacketSupport.str(out, "", charset);             // F24AFF
        out.encodeInt(nsi.getQuestExValue());                       // F24B49
        out.encodeInt(0);                                           // F24B57
        out.encodeByte(0);                                          // F24B65
        Tms282LoginPacketSupport.str(out, "", charset);             // F24B7C
        Tms282LoginPacketSupport.str(out, "", charset);             // F24BCA
        if (isRechargeable(nsi.getItemID())) {
            out.encodeLong(nsi.getUnitPrice() <= 1 ? 1 : nsi.getUnitPrice());   // F24C4F Buf8
        } else {
            out.encodeInt(Math.max(1, nsi.getQuantity()));          // F24C3C
        }
        out.encodeInt(nsi.getItemID() == 2070000 ? 500 : nsi.getMaxPerSlot());  // F24C57
        Tms282LoginPacketSupport.str(out, "", charset);             // F24C69
        out.encodeInt(0);                                           // F24CB3
        out.encodeInt(0);                                           // F24CC1
        Tms282LoginPacketSupport.str(out, "", charset);             // F24CD3 item+0x1A0: window start (atoi 0)
        Tms282LoginPacketSupport.str(out, SALE_WINDOW_END, charset); // F24D21 item+0x1A8: window end (see SALE_WINDOW_END)
        out.encodeInt(0);                                           // 8B9910: 4 x i32 + Buf40
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeInt(0);
        out.encodeArr(new byte[0x28]);
        out.encodeInt(0);                                           // F24D87 Buf4
        Tms282LoginPacketSupport.str(out, "", charset);             // F24D93
        out.encodeInt(0);                                           // F24DEA Buf4
    }

    static void encodeBuyLimit(OutPacket out, BuyLimitInfo info) {
        if (info == null) {
            info = new BuyLimitInfo();
        }
        info.encode(out);
    }

    static boolean categoryTabs() {
        return !"false".equalsIgnoreCase(System.getProperty("tms282.shop.categoryTabs", "true"));
    }

    /** The tab key sent at F245E6: the inventory type of the item (see TAB_EQUIP). */
    static int tabKey(NpcShopItem nsi) {
        if (!categoryTabs()) {
            return nsi.getTabIndex();
        }
        return switch (nsi.getItemID() / 1_000_000) {
            case 1 -> TAB_EQUIP;
            case 2 -> TAB_CONSUME;
            case 3 -> TAB_INSTALL;
            default -> TAB_ETC;
        };
    }

    /** The FILETIME encodeFT writes for {@code ft} (the 265 FileTime holds either millis or a client value). */
    static long wire(FileTime ft) {
        OutPacket tmp = new OutPacket((short) 0);
        tmp.encodeFT(ft);
        return ByteBuffer.wrap(tmp.getData(), 2, 8).order(ByteOrder.LITTLE_ENDIAN).getLong();
    }

    /** Null or before 2000 -> the client's open start [879FE90]; otherwise the stored time. */
    static long saleStart(FileTime start) {
        long v = start == null ? SALE_START_OPEN : wire(start);
        return Long.compareUnsigned(v, SALE_START_OPEN_BEFORE) < 0 ? SALE_START_OPEN : v;
    }

    /** Null or at/after 2079-01-01 -> the client's open end [879FE98] (no 「為止 可購買」 line); otherwise the stored time. */
    static long saleEnd(FileTime end) {
        long v = end == null ? SALE_END_OPEN : wire(end);
        return Long.compareUnsigned(v, SALE_END_OPEN) >= 0 ? SALE_END_OPEN : v;
    }

    /** F24C1F..F24C37: 2070000..2079999 and 2330000..2339999 carry a unit price (Buf8), everything else an i32. */
    public static boolean isRechargeable(int itemId) {
        return (itemId >= 2070000 && itemId < 2080000) || (itemId >= 2330000 && itemId < 2340000);
    }

    static long price(NpcShopItem nsi) {
        if (nsi.getItem() == null && nsi.getDiscountPerc() != 0) {
            return (long) (nsi.getPrice() - nsi.getPrice() * (nsi.getDiscountPerc() / (double) 100));
        }
        return nsi.getPrice();
    }

    private static void writeInt(ByteArrayOutputStream aux, int v) {
        aux.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array());
    }
}
