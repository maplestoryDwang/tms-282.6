package net.swordie.ms.connection.packet;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.trunk.Trunk;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.enums.CashErrorType;
import net.swordie.ms.enums.CashItemType;
import net.swordie.ms.handlers.header.OutHeader;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.world.shop.cashshop.CashItemInfo;

import java.util.List;

public class CCashShop {

    public static OutPacket queryCashResult(Char chr) {
        var profile = net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active();
        if (profile.isPresent()) {
            return profile.get().queryCashResult(chr);
        }
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_QUERY_CASH_RESULT);

        outPacket.encodeInt(chr.getUser().getNxPrepaid());
        outPacket.encodeInt(chr.getUser().getMaplePoints());
        outPacket.encodeInt(chr.getAccount().getRewardPoint()); // Reward Points

        return outPacket;
    }

    public static OutPacket cashItemResBuyDone(CashItemInfo cashItemInfo, FileTime registerDate, CashItemInfo receiveBonus) {
        return cashItemResBuyDone(CashItemType.Res_Buy_Done, cashItemInfo, registerDate, receiveBonus);
    }

    public static OutPacket cashItemResBuyDone(CashItemType resultType, CashItemInfo cashItemInfo, FileTime registerDate, CashItemInfo receiveBonus) {
        var profile = net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active();
        if (profile.isPresent()) {
            return profile.get().buyDone(resultType, cashItemInfo, registerDate, receiveBonus);
        }
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(resultType.getVal());
        cashItemInfo.encode(outPacket);
        boolean hasRegisterDate = registerDate != null;
        outPacket.encodeInt(hasRegisterDate ? 1 : 0);
        if (hasRegisterDate) {
            outPacket.encodeFT(registerDate);
        }
        boolean hasReceiveBonus = receiveBonus != null;
        outPacket.encodeByte(hasReceiveBonus);
        if (receiveBonus != null) {
            receiveBonus.encode(outPacket);
        }

        return outPacket;
    }

    public static OutPacket cashItemResIncSlotDone(byte type, int count) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_IncSlotCount_Done.getVal());
        outPacket.encodeByte(type);
        outPacket.encodeShort(count);

        return outPacket;
    }

    public static OutPacket cashItemResIncTrunkCountDone(short count) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_IncTrunkCount_Done.getVal());
        outPacket.encodeShort(count);

        return outPacket;
    }

    public static OutPacket cashItemResIncBuyCharCountDone(short count) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_IncBuyCharCount_Done.getVal());
        outPacket.encodeShort(count);

        return outPacket;
    }

    public static OutPacket cashItemResEnableEquipSlotExtDone(short duration) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_EnableEquipSlotExt_Done.getVal());
        outPacket.encodeShort(0);
        outPacket.encodeShort(duration);

        return outPacket;
    }

    public static OutPacket cashItemResPurchaseRecordDone() {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_PurchaseRecord_Done.getVal());
        outPacket.encodeInt(0);
        outPacket.encodeInt(1);

        return outPacket;
    }

    public static OutPacket cashItemResPurchaseRecordDone(int val, int type) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_PurchaseRecord_Done.getVal());
        outPacket.encodeInt(val);
        outPacket.encodeInt(type);

        return outPacket;
    }

    public static OutPacket error() {
        var profile = net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active();
        if (profile.isPresent()) {
            return profile.get().error();
        }
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_Buy_Failed.getVal());
        outPacket.encodeByte(137);
        outPacket.encodeShort(0);

        return outPacket;
    }

    public static OutPacket failed(CashItemType cit, CashErrorType cet) {
        var profile = net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active();
        if (profile.isPresent()) {
            return profile.get().failed(cit, cet);
        }
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(cit.getVal());
        outPacket.encodeByte(cet.getVal());

        return outPacket;
    }

    public static OutPacket resMoveLtoSDone(Item item) {
        var profile = net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active();
        if (profile.isPresent()) {
            return profile.get().moveLtoSDone(item);
        }
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_MoveLtoS_Done.getVal());
        outPacket.encodeByte(true); // bExclRequestSent
        outPacket.encodeShort(item.getBagIndex());
        item.encode(outPacket);
        outPacket.encodeInt(0); // List of SNs (longs)
        outPacket.encodeByte(false); // Bonus cash item (CashItemInfo::Decode)

        return outPacket;
    }

    public static OutPacket unk() {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(2);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);

        return outPacket;
    }

    public static OutPacket resAddedCashItemDone(CashItemInfo cii) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_AddedCashItem_Done.getVal());
        outPacket.encodeShort(1); // size
        cii.encode(outPacket);

        return outPacket;
    }

    public static OutPacket loadLockerDone(Account account) {
        var profile = net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active();
        if (profile.isPresent()) {
            return profile.get().loadLockerDone(account);
        }
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_LoadLocker_Done.getVal());
        Trunk trunk = account.getTrunk();
        List<CashItemInfo> locker = trunk.getLocker();
        int lockerSize = locker.size();
        outPacket.encodeShort(trunk.getSlotCount());
        outPacket.encodeShort(account.getUser().getCharacterSlots());
        outPacket.encodeInt(0);
        outPacket.encodeShort(lockerSize);
        locker.forEach(item -> item.encode(outPacket));

        return outPacket;
    }

    public static OutPacket resMoveStoLDone(CashItemInfo cii) {
        var profile = net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active();
        if (profile.isPresent()) {
            return profile.get().moveStoLDone(cii);
        }
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_MoveStoL_Done.getVal());
        outPacket.encodeByte(false);
        cii.encode(outPacket);

        return outPacket;
    }

    public static OutPacket resDestroyDone(long itemSN) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_Destroy_Done.getVal());
        outPacket.encodeLong(itemSN);

        return outPacket;
    }

    public static OutPacket resExpireDone(long itemSN) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CASH_ITEM_RESULT);

        outPacket.encodeByte(CashItemType.Res_Expire_Done.getVal());
        outPacket.encodeLong(itemSN);

        return outPacket;
    }

    public static OutPacket openSurpriseBox(CashItemInfo reward, CashItemInfo box, boolean Rare) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_OPEN_SSB_RESULT);

        outPacket.encodeByte(CashItemType.Res_Open_Premium_Style_Box_Done.getVal());
        outPacket.encodeLong(box.getItem().getId());
        outPacket.encodeInt(box.getItem().getQuantity());
        outPacket.encodeInt(box.getItem().getItemId());
        box.encode(outPacket);
        outPacket.encodeInt(reward.getItem().getItemId());
        outPacket.encodeByte(1); // showItemName
        outPacket.encodeByte(Rare);

        return outPacket;
    }

    public static OutPacket openSurpriseBoxes(long SN, int itemID, int quantity, List<CashItemInfo> rewards, List<CashItemInfo> boxes, boolean Rare) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_OPEN_SSB_RESULT);

        outPacket.encodeByte(CashItemType.Res_Open_Premium_Style_Boxes_Done.getVal());
        outPacket.encodeLong(SN); // probs SN doesn't seem like a date
        outPacket.encodeInt(quantity);//Box Quantity
        outPacket.encodeInt(itemID);
        outPacket.encodeInt(boxes.size());
        for (CashItemInfo box : boxes) {
            box.encode(outPacket);
        }
        outPacket.encodeInt(rewards.size());
        for (CashItemInfo reward : rewards) {
            outPacket.encodeInt(reward.getItem().getItemId());
            outPacket.encodeByte(1);
        }
        outPacket.encodeByte(Rare);

        return outPacket;
    }

    public static OutPacket surpriseBoxFailed() {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_OPEN_SSB_RESULT);

        outPacket.encodeByte(CashItemType.Res_Open_Premium_Style_Box_Failed.getVal());

        return outPacket;
    }

    public static OutPacket chargeMileageNotice(int val) {
        OutPacket outPacket = new OutPacket(OutHeader.CASH_SHOP_CHARGE_MILEAGE_NOTICE);

        outPacket.encodeByte(0);
        outPacket.encodeInt(val);

        return outPacket;
    }
}
