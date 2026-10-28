package net.swordie.ms.connection.packet;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.trunk.Trunk;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder;
import net.swordie.ms.enums.CashErrorType;
import net.swordie.ms.enums.CashItemType;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.shop.cashshop.CashItemInfo;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

/**
 * TMS 282.3 cash shop packets ({@link CashShopPacketEncoder}). Layouts from the client handlers, each traced CLEAN
 * against the emulated client (research/tms2823/cashshop-trace, tools/Tms282CashShopCheck.java):
 * <ul>
 *     <li>LP02F1 SET_CASH_SHOP: CStage::OnSetCashShop 44C0690 reads exactly one CharacterData::Decode
 *     (44C06EC -> 92AD20(cd, flags, pkt, 0, 0), the same call as the SetField first entry 44B9C09) and builds
 *     an intermediate stage. LP02F3 separately constructs CCashShop (2120640); omitting it leaves a black screen.</li>
 *     <li>LP0A75 CASH_SHOP_QUERY_CASH_RESULT: CCashShop::OnPacket 215CF70 jump table (0xA75..0xA94) -> 215D9F0,
 *     three i32 (215DA0D/215DA17/215DA21) = 265 queryCashResult.</li>
 *     <li>LP0A76 CASH_SHOP_CASH_ITEM_RESULT: -> 215DD10, u8 kind (215DD32) then a per-kind body
 *     ({@link Kind}). The kinds are renumbered against 265.</li>
 * </ul>
 * GW_CashItemInfo::Decode 8E2360 (282): Buf8 sn, i32 account, i32 character, i32 item, i32 commodity SN, i32 count,
 * Buf15 buyer name, Buf8 expire FILETIME, i32 payback rate, double discount, i32 order no, i32 product no, u8 refundable,
 * u8 source flag, u8 store bank, u8 has-item (non-zero: 932790 GW_ItemSlotBase). 265 sends i16 count and a 13-byte
 * name, so 282 is 4 bytes longer.
 */
public final class Tms282CashShopPackets implements CashShopPacketEncoder {
    public static final int SET_CASH_SHOP = 0x02F1;
    public static final int OPEN_CASH_SHOP = 0x02F3;
    public static final int PURCHASE_COUNT = 0x0165;

    /** CWvsContext case0165 ->22136B0: SN, scope, count, FILETIME (780520). */
    public static OutPacket purchaseCount(int sn, int scope, int count, FileTime updated) {
        if(sn<=0 || (scope!=3 && scope!=4) || count<0) throw new IllegalArgumentException("Invalid purchase count");
        OutPacket out=Tms282LoginPacketSupport.packet(PURCHASE_COUNT);
        out.encodeInt(sn);out.encodeInt(scope);out.encodeInt(count);out.encodeFT(updated);
        return out;
    }

    /** LP0A76 kind77 ->2161100: u8 item count, CashItemInfo[], u16, optional date, optional bonus. */
    public OutPacket buyPackageDone(List<CashItemInfo> items) {
        if(items.isEmpty() || items.size()>255)throw new IllegalArgumentException("Invalid package delivery size");
        OutPacket out=Tms282LoginPacketSupport.packet(CASH_ITEM_RESULT);
        out.encodeByte(77);out.encodeByte(items.size());
        for(CashItemInfo item:items)encodeCashItemInfo(out,item);
        out.encodeShort(1);out.encodeInt(0);out.encodeByte(0);
        return out;
    }
    public static final int QUERY_CASH_RESULT = 0x0A75;
    public static final int CASH_ITEM_RESULT = 0x0A76;
    /** Native2164CB0 inventory /2165120 storage: type(optional),u16 capacity,zero offer result/list. */
    public static OutPacket expansionDone(boolean storage,int type,int slots) {
        if(slots<1 || slots>(!storage&&type==6?256:128) || !storage&&(type<1||type>6))throw new IllegalArgumentException("Invalid expansion result");
        OutPacket out=Tms282LoginPacketSupport.packet(CASH_ITEM_RESULT);out.encodeByte(storage?28:26);
        if(!storage)out.encodeByte(type);
        out.encodeShort(slots);out.encodeInt(0);out.encodeInt(0);return out;
    }
    public static OutPacket expansionFailed(boolean storage,CashErrorType reason) {
        OutPacket out=Tms282LoginPacketSupport.packet(CASH_ITEM_RESULT);out.encodeByte(storage?29:27);
        out.encodeByte(clientReason(reason));return out;
    }
    /** GW_CashItemInfo buyer-name buffer (8E23CB DecodeBuffer 0xF). */
    static final int BUYER_NAME_WIDTH = 15;

    /**
     * The LP0A76 kinds this server sends: 282 wire value (215DD10 table 215EA3C / 215E948) and the body read by the
     * client. The locker moves and destroy are the 265 value - 5, buy + 1, load locker + 2.
     */
    public enum Kind {
        /** 215F770: u16 slot count, u16 character slots, i32, u16 n, n x CashItemInfo (265 Res_LoadLocker_Done 4). */
        LOAD_LOCKER_DONE(6),
        /** 215DFAC: u8 reason; the shared failure handler (also 37, 74, 116), used for kinds 282 has no own value for. */
        GENERIC_FAILED(14),
        /**
         * 21604E0: CashItemInfo, i32 has-date [FILETIME], u8 has-bonus [CashItemInfo], then (282 only) i32, i32 n,
         * n x i32 (265 Res_Buy_Done 14).
         */
        BUY_DONE(15),
        /** 2160BB0: u8 reason (265 Res_Buy_Failed 15). */
        BUY_FAILED(16),
        /** 2165920: u8 excl, u16 pos, GW_ItemSlotBase, ... (265 Res_MoveLtoS_Done 37). */
        MOVE_L_TO_S_DONE(32),
        /** 215DFB5 path: u8 reason (265 Res_MoveLtoS_Failed 38). */
        MOVE_L_TO_S_FAILED(33),
        /** 2165D20: u8, CashItemInfo (265 Res_MoveStoL_Done 39). */
        MOVE_S_TO_L_DONE(34),
        /** u8 reason (265 Res_MoveStoL_Failed 40). */
        MOVE_S_TO_L_FAILED(35),
        /** 215E310: Buf8 sn (265 Res_Destroy_Done 41). */
        DESTROY_DONE(36),
        /** u8 reason (265 Res_Destroy_Failed 42). */
        DESTROY_FAILED(37),
        /** 215DEA3 -> 2212B20: Buf48 = 12 x i32 wish-list SNs (265 Res_LoadWish_Done 8); pushed on entry. */
        LOAD_WISH_DONE(0x0B),
        /** 215DEB7: clears the pending flag (+0x88), 2212B20 Buf48 = 12 x i32 (265 Res_SetWish_Done 12). */
        SET_WISH_DONE(0x0D),
        /** 215E194 -> 21648E0: str receiver, i32 item id, i32 count; StringPool C57 (265 Res_Gift_Done 23). */
        GIFT_DONE(0x18),
        /** 215E3A6: clears +0x88, u8 reason (reasons 0x1F/0x20 add an i32 SN), 216BA40 message (265 Res_Gift_Failed 24). */
        GIFT_FAILED(0x19);

        private final int wire;

        Kind(int wire) {
            this.wire = wire;
        }

        public int wire() {
            return wire;
        }

        /** The 282 failure kind for a 265 *_Failed type; {@link #GENERIC_FAILED} when 282 has no own one here. */
        public static Kind failureOf(CashItemType type) {
            if (type == null) {
                return GENERIC_FAILED;
            }
            return switch (type) {
                case Res_Buy_Failed, Res_BuyPackage_Failed -> BUY_FAILED;
                case Res_MoveLtoS_Failed -> MOVE_L_TO_S_FAILED;
                case Res_MoveStoL_Failed -> MOVE_S_TO_L_FAILED;
                case Res_Destroy_Failed -> DESTROY_FAILED;
                // Res_Gift_Failed stays GENERIC_FAILED here (tools/Tms282CashShopCheck); the gift path sends
                // GIFT_FAILED through giftFailed().
                default -> GENERIC_FAILED;
            };
        }
    }

    private final Charset charset;

    public Tms282CashShopPackets(Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset");
    }

    // ---- LP02F1 ------------------------------------------------------------------------------------------------

    @Override
    public OutPacket setCashShop(Char chr) {
        Objects.requireNonNull(chr, "chr");
        CharacterStat stat = Objects.requireNonNull(chr.getAvatarData(), "avatar data").getCharacterStat();
        Tms282StagePackets.InventorySlots slots = Tms282StagePackets.InventorySlots.of(chr);
        return setCashShop(stat, slots, Tms282StagePackets.Inventories.of(chr));
    }

    /** LP02F1 from a stat and its items (offline fixtures use this one). */
    public OutPacket setCashShop(CharacterStat stat, Tms282StagePackets.InventorySlots slots,
                                 Tms282StagePackets.Inventories items) {
        OutPacket out = Tms282LoginPacketSupport.packet(SET_CASH_SHOP);
        // 44C06EC -> 92AD20 with r9d = 0 and [rsp+20] = 0, as the SetField first entry: same flags, same body.
        Tms282StagePackets.encodeCharacterData(out, stat, slots,
                new Tms282StagePackets.ItemSlotBlocks(items, slots), charset);
        return out;
    }

    // ---- LP0A75 ------------------------------------------------------------------------------------------------

    /** CStage 44B9472: FILETIME, then CCashShop 2120640 flags and settings.
     * Empty override collections retain the client's own commodity catalog; no opaque 265 shop blob.
     */
    public static OutPacket openCashShop(FileTime now) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPEN_CASH_SHOP);
        out.encodeFT(Objects.requireNonNull(now, "now"));
        out.encodeByte(0);
        out.encodeByte(1);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeInt(0);   // 5ED943E
        out.encodeShort(0); // 5ED94C2
        out.encodeShort(0); // 5ED9A1B
        out.encodeInt(0);   // 5ED9CA7
        out.encodeInt(0);   // 5ED9DC4
        out.encodeInt(0);   // 2136518
        out.encodeInt(0);   // 2137BF2
        out.encodeShort(0); // 2137CF0
        out.encodeShort(0); // 2137DC6
        out.encodeInt(0);   // 2138932
        out.encodeInt(0);   // 2138F4E
        return out;
    }

    /**
     * LP02F3 with the server catalog rules: 5ED943E i32 n + n x i32 blocked SNs (each found SN is erased from the
     * client's commodity array, 5ED9860..5ED997D), then 5ED94C2 u16 n + n x (i32 SN, CS_COMMODITY::DecodeModify 779470:
     * u64 mask, [i32 ItemId 0x1], [i32 Count 0x2], [i32 Price 0x4], [u16 Period 0x20], [u8 OnSale 0x400]); the rest as
     * {@link #openCashShop(FileTime)}. Mask bits checked against the Commodity.img loader 77B7A0 (ItemId 411970,
     * Count +40, Price +44, Period +54, OnSale +74). Only bits in that verified set are ever written.
     */
    public static OutPacket openCashShop(FileTime now, java.util.Collection<Integer> blocked,
                                         java.util.Collection<net.swordie.ms.world.shop.cashshop.CashShopCatalogRules.Modify> modifies) {
        OutPacket out = Tms282LoginPacketSupport.packet(OPEN_CASH_SHOP);
        out.encodeFT(Objects.requireNonNull(now, "now"));
        out.encodeByte(0);
        out.encodeByte(1);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeByte(0);
        out.encodeInt(blocked.size());   // 5ED943E
        for (int sn : blocked) out.encodeInt(sn);
        if (modifies.size() > 0xFFFF) throw new IllegalArgumentException("Too many commodity modifies");
        out.encodeShort(modifies.size()); // 5ED94C2
        for (var m : modifies) {
            long mask = m.clientMask();
            out.encodeInt(m.sn());
            out.encodeLong(mask);
            if (m.itemId() != null) out.encodeInt(m.itemId());
            if (m.count() != null) out.encodeInt(m.count());
            if (m.price() != null) out.encodeInt(m.price());
            if (m.period() != null) out.encodeShort(Math.max(0, Math.min(0xFFFF, m.period())));
            if (m.onSale() != null) out.encodeByte(m.onSale() ? 1 : 0);
        }
        out.encodeShort(0); // 5ED9A1B
        out.encodeInt(0);   // 5ED9CA7
        out.encodeInt(0);   // 5ED9DC4
        out.encodeInt(0);   // 2136518
        out.encodeInt(0);   // 2137BF2
        out.encodeShort(0); // 2137CF0
        out.encodeShort(0); // 2137DC6
        out.encodeInt(0);   // 2138932
        out.encodeInt(0);   // 2138F4E
        return out;
    }

    /** LP0A76 kind 0x0B: the 12 wish-list SNs (zero padded). */
    public static OutPacket loadWishDone(int[] sns) {
        return wishList(Kind.LOAD_WISH_DONE, sns);
    }

    /** LP0A76 kind 0x0D: the answer to CP0741 kind 4 (clears the shop's pending flag). */
    public static OutPacket setWishDone(int[] sns) {
        return wishList(Kind.SET_WISH_DONE, sns);
    }

    private static OutPacket wishList(Kind kind, int[] sns) {
        OutPacket out = result(kind);
        for (int i = 0; i < 12; i++) out.encodeInt(sns != null && i < sns.length ? sns[i] : 0);
        return out;
    }

    /** LP0A76 kind 0x18 (21648E0): receiver name, item id, count ("將'%d'個'%s'送禮給[%s]。"). */
    public OutPacket giftDone(String receiver, int itemId, int count) {
        OutPacket out = result(Kind.GIFT_DONE);
        Tms282LoginPacketSupport.str(out, receiver, charset);
        out.encodeInt(itemId);
        out.encodeInt(count);
        return out;
    }

    /** LP0A76 kind 0x19: u8 client reason ({@link #clientReason}; never 31/32, which need a trailing SN). */
    public static OutPacket giftFailed(CashErrorType reason) {
        return failed(Kind.GIFT_FAILED, clientReason(reason));
    }

    @Override
    public OutPacket queryCashResult(Char chr) {
        User user = chr.getUser();
        return queryCashResult(user.getNxPrepaid(), user.getMaplePoints(), chr.getAccount().getRewardPoint());
    }

    /** 215DA0D / 215DA17 / 215DA21, the 265 order (prepaid NX, maple points, reward points). */
    public static OutPacket queryCashResult(int nxPrepaid, int maplePoints, int rewardPoints) {
        OutPacket out = Tms282LoginPacketSupport.packet(QUERY_CASH_RESULT);
        out.encodeInt(nxPrepaid);
        out.encodeInt(maplePoints);
        out.encodeInt(rewardPoints);
        return out;
    }

    // ---- LP0A76 ------------------------------------------------------------------------------------------------

    @Override
    public OutPacket loadLockerDone(Account account) {
        Trunk trunk = account.getTrunk();
        return loadLockerDone(trunk.getSlotCount(), account.getUser().getCharacterSlots(), trunk.getLocker());
    }

    /** Kind 6, 215F770: the 265 loadLockerDone fields in the 265 order. */
    public OutPacket loadLockerDone(int slotCount, int characterSlots, List<CashItemInfo> locker) {
        OutPacket out = result(Kind.LOAD_LOCKER_DONE);
        out.encodeShort(slotCount);
        out.encodeShort(characterSlots);
        out.encodeInt(0);
        out.encodeShort(locker.size());
        for (CashItemInfo cii : locker) {
            encodeCashItemInfo(out, cii);
        }
        return out;
    }

    @Override
    public OutPacket buyDone(CashItemType resultType, CashItemInfo item, FileTime registerDate, CashItemInfo receiveBonus) {
        // Single-item result. A real package uses buyPackageDone (native kind77) with all components together.
        OutPacket out = result(Kind.BUY_DONE);
        encodeCashItemInfo(out, item);
        out.encodeInt(registerDate == null ? 0 : 1);
        if (registerDate != null) {
            out.encodeFT(registerDate);
        }
        out.encodeByte(receiveBonus != null);
        if (receiveBonus != null) {
            encodeCashItemInfo(out, receiveBonus);
        }
        // 282 only (not in 265): 216076E i32 -> 5F59C30(ctx, v) (looks v up in the ctx+5628 list; 0 matches nothing
        // [推定]), then 2160784 i32 n + 21607D6 DecodeBuffer(4n) (an i32 list, none).
        out.encodeInt(0);
        out.encodeInt(0);
        return out;
    }

    @Override
    public OutPacket moveLtoSDone(Item item) {
        OutPacket out = result(Kind.MOVE_L_TO_S_DONE);
        out.encodeByte(1);                                  // exclusive request sent
        out.encodeShort(item.getBagIndex());
        Tms282ItemEncoder.encode(out, item, charset);
        out.encodeInt(0);                                   // SN list (i64 each), none
        out.encodeByte(0);                                  // no bonus CashItemInfo
        return out;
    }

    @Override
    public OutPacket moveStoLDone(CashItemInfo item) {
        OutPacket out = result(Kind.MOVE_S_TO_L_DONE);
        out.encodeByte(0);
        encodeCashItemInfo(out, item);
        return out;
    }

    @Override
    public OutPacket failed(CashItemType type, CashErrorType reason) {
        return failed(Kind.failureOf(type), clientReason(reason));
    }

    /** Native 216FD80/StringPool, not the 265 error-number enum (265 retry75 is TMS purchase-limit75). */
    public static int clientReason(CashErrorType reason) {
        if(reason==null)return 57;
        return switch(reason) {
            case RequestTimedOut -> 1;
            case NotEnoughCash -> 3;
            case CannotGift_Under14 -> 4;
            case CannotExceedPriceGift -> 5;
            case CannotGiftToOwnAccount -> 6;
            case CannotFindCharacterGift -> 7;
            case GenderRestriction -> 8;
            case RecipientInventoryFull -> 9;
            case TooManyCashItems, YouHaveTooManyCashItems -> 10;
            case PleaseCheckYourInventoryFullOrNot -> 26;
            // Native 31/32 require a trailing commodity SN, unavailable to this generic factory.
            case SoldOut, SoldOut2 -> 56;
            case ThatItemIsNotBeingSold -> 56;
            case ExceededThePurchaseLimitForThisItem -> 75;
            case PreviousValuePackStillActive -> 76;
            case NotEnoughRewardPoints -> 77;
            case CannotGetBonusItem_CashInventoryFull -> 79;
            case ThatItemCannotBeMoved, ThatItemCannotBeMoved2 -> 87;
            default -> 57;
        };
    }

    /** A failure kind with its u8 reason. */
    public static OutPacket failed(Kind kind, int reason) {
        OutPacket out = result(kind);
        out.encodeByte(reason);
        return out;
    }

    @Override
    public OutPacket error() {
        // Native reason57 is generic without the forced exit used by reasons below3.
        return failed(Kind.BUY_FAILED, 57);
    }

    private static OutPacket result(Kind kind) {
        OutPacket out = Tms282LoginPacketSupport.packet(CASH_ITEM_RESULT);
        out.encodeByte(kind.wire());
        return out;
    }

    // ---- GW_CashItemInfo (8E2360) ------------------------------------------------------------------------------

    void encodeCashItemInfo(OutPacket out, CashItemInfo cii) {
        Item item = Objects.requireNonNull(cii.getItem(), "cash item without an item");
        out.encodeLong(item.getId());                       // 8E2382 Buf8 sn (265 writes the item id)
        out.encodeInt(cii.getAccountID());                  // 8E238A
        out.encodeInt(0);                                   // 8E2395 265 writes 0
        out.encodeInt(item.getItemId());                    // 8E23A0
        out.encodeInt(cii.getCommodityID());                // 8E23AB
        out.encodeInt(item.getQuantity());                  // 8E23B6 i32 (265: i16)
        out.encodeArr(Tms282ItemEncoder.fixed(cii.getBuyCharacterID(), BUYER_NAME_WIDTH, charset)); // 8E23CB Buf15
        FileTime expire = item.getDateExpire();
        out.encodeFT(expire == null ? FileTime.MAX_TIME() : FileTime.fromDate(expire.toLocalDateTime())); // 8E23DD
        out.encodeInt(cii.getPaybackRate());                // 8E23E5
        out.encodeLong(Double.doubleToLongBits(cii.getDiscount())); // 8E23F0 Decode double (265: (long) discount)
        out.encodeInt(cii.getOrderNo());                    // 8E23FD
        out.encodeInt(cii.getProductNo());                  // 8E2408
        out.encodeByte(cii.isRefundable());                 // 8E2413
        out.encodeByte(cii.getSourceFlag());                // 8E241E
        out.encodeByte(cii.isStoreBank());                  // 8E2429
        out.encodeByte(1);                                  // 8E2434 has item -> 8E2445 932790
        Tms282ItemEncoder.encode(out, item, charset);
    }
}
