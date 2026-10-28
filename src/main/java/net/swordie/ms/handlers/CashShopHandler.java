package net.swordie.ms.handlers;


import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.User;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.trunk.Trunk;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.CCashShop;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.CashErrorType;
import net.swordie.ms.enums.CashItemType;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.CashShopData;
import net.swordie.ms.loaders.Etc.Commodity.CommodityInfo;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.shop.cashshop.CashItemInfo;
import net.swordie.ms.world.shop.cashshop.CashShopItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 4/23/2018.
 */
public class CashShopHandler {

    @Handler(op = InHeader.CASH_SHOP_QUERY_CASH_REQUEST)
    public static void handleCashShopQueryCashRequest(Client c, InPacket inPacket) {
        disposeForCS(c.getChr());
    }

    @Handler(op = InHeader.CASH_SHOP_CASH_ITEM_REQUEST)
    public static void handleCashShopCashItemRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        User user = chr.getUser();
        Account account = chr.getAccount();
        Trunk trunk = account.getTrunk();
        byte type = inPacket.decodeByte();
        CashItemType cit = CashItemType.getRequestTypeByVal(type);
        if (cit == null) {
            System.out.println("Unhandled cash shop cash item request " + type);
            c.write(CCashShop.error());
            return;
        }
        switch (cit) {
            case Req_Buy: {
                inPacket.decodeByte();
                byte paymentMethod = inPacket.decodeByte();
                byte[] options=inPacket.decodeArr(5);
                int id = inPacket.decodeInt();
                int quantity = inPacket.decodeInt();
                if(net.swordie.ms.connection.protocol.Protocol.active().version()==net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3
                        && (options[0]!=0 || options[1]!=0 || options[2]!=0 || options[3]!=0 || options[4]!=0)) {
                    var commodity=CashShopData.getCommodityBySN(id);
                    if(commodity==null || !commodity.isOnlyMileage()) {
                        // Mixed mileage/coupon options require a separate quote; never silently charge full cash.
                        c.write(CCashShop.failed(CashItemType.Res_Buy_Failed,CashErrorType.PleaseTryAgain));disposeForCS(chr);return;
                    }
                }
                handleCashShopPurchase(c, paymentMethod, id, quantity, CashItemType.Res_Buy_Done, CashItemType.Res_Buy_Failed);
                break;
            }
            case Req_BuyPackage: {
                inPacket.decodeByte();
                int paymentMethod = inPacket.decodeInt();
                int id = inPacket.decodeInt();
                int quantity = inPacket.decodeInt();
                handleCashShopPurchase(c, paymentMethod, id, quantity, CashItemType.Res_BuyPackage_Done, CashItemType.Res_BuyPackage_Failed);
                break;
            }
            case Req_MoveLtoS: {
                //Di chuyển từ Trunk tới Inventory
                long itemSn = inPacket.decodeLong(); //primary Key
                int itemID = inPacket.decodeInt();
                int invType = inPacket.decodeByte();
                CashItemInfo cii = trunk.getLockerItemBySn(itemSn);
                if (cii == null) {
                    c.write(CCashShop.failed(CashItemType.Res_MoveLtoS_Failed, CashErrorType.ItemChanged_PleaseRefresh));
                    disposeForCS(chr);
                    return;
                }
                Item item = cii.getItem();
                boolean tms282 = net.swordie.ms.connection.protocol.Protocol.active().version()
                        == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
                if (tms282 && (item == null || (item.getDateExpire() != null && item.getDateExpire().isExpired()))) {
                    c.write(CCashShop.failed(CashItemType.Res_MoveLtoS_Failed, CashErrorType.ItemChanged_PleaseRefresh));
                    chr.dispose();
                    return;
                }
                if (item.getItemId() != itemID) {
                    c.write(CCashShop.failed(CashItemType.Res_MoveLtoS_Failed, CashErrorType.ItemChanged_PleaseRefresh));
                    disposeForCS(chr);
                    return;
                }
                Inventory inventory = chr.getInventoryByType(InvType.getInvTypeByVal(invType));
                if (tms282 && (inventory == null || inventory != chr.getInventoryByType(item.getInvType()))) {
                    c.write(CCashShop.failed(CashItemType.Res_MoveLtoS_Failed, CashErrorType.ItemChanged_PleaseRefresh));
                    chr.dispose();
                    return; // Validate the same destination that addItemToInventory will actually use.
                }
                if (!inventory.canPickUp(item)) {
                    c.write(CCashShop.failed(CashItemType.Res_MoveLtoS_Failed, CashErrorType.PleaseCheckYourInventoryFullOrNot));
                    disposeForCS(chr);
                    return;
                }

                if (ItemConstants.isEquip(item.getItemId())) {
                    Equip equip = (Equip) item;
                    if (!equip.hasAttribute(EquipAttribute.NoNonCombatStatGain)) {
                        equip.setCharmEXP(60);
                    }
                }
                trunk.getLocker().removeIf(x -> x.getId() == cii.getId());
                cii.setItem(null);
                cii.deleteCashItemInfoFromSQL();

                item.setCash(true);
                item.setTrunkID(0);
                chr.addItemToInventory(item);
                item.saveToSQL();

                c.write(CCashShop.resMoveLtoSDone(item));
                disposeForCS(chr);
                break;
            }
            case Req_MoveStoL: {
                long itemSN = inPacket.decodeLong();
                int itemID = inPacket.decodeInt();
                byte invType = inPacket.decodeByte();
                int pos = inPacket.decodeShort();
                Inventory inv = chr.getInventoryByType(InvType.getInvTypeByVal(invType));
                Item item = inv == null ? null : inv.getItemBySlot(pos);
                if (item == null || item.getItemId() != itemID || item.getId() != itemSN) {
                    c.write(CCashShop.failed(CashItemType.Res_MoveStoL_Failed, CashErrorType.ItemChanged_PleaseRefresh));
                    disposeForCS(chr);
                    return;
                }
                if (trunk.getLocker().size() + 1 > GameConstants.MAX_LOCKER_SIZE) {
                    c.write(CCashShop.failed(CashItemType.Res_MoveStoL_Failed, CashErrorType.TooManyCashItems));
                    disposeForCS(chr);
                    return;
                }
                if (item instanceof PetItem petItem) {
                    if (petItem.getActiveState() != 0) {
                        c.write(CCashShop.failed(CashItemType.Res_MoveStoL_Failed, CashErrorType.ThatItemCannotBeMoved));
                        disposeForCS(chr);
                        return;
                    }
                }
                item.setInventoryID(0);
                item.setCharID(chr.getId());
                item.setTrunkID(trunk.getId());
                item.saveToSQL();
                CashItemInfo cii = CashItemInfo.fromItem(chr, item);
                if (cii == null) {
                    c.write(CCashShop.failed(CashItemType.Res_MoveStoL_Failed, CashErrorType.ThatItemCannotBeMoved));
                    disposeForCS(chr);
                    return;
                }

                c.write(CCashShop.resMoveStoLDone(cii));
                inv.removeItem(item);
                cii.updateCashItemInfoToSQL();
                trunk.addCashItem(cii);
                disposeForCS(chr);
                break;
            }
            case Req_IncSlotCount: {
                inPacket.decodeByte();
                int paymentMethod = inPacket.decodeInt();
                int itemSN = inPacket.decodeInt();
                CommodityInfo ci = CashShopData.getCommodityBySN(itemSN);
                if (ci == null || ci.getOnSale() == 0 || ci.getTerm() != 0) {
                    c.write(CCashShop.error());
                    disposeForCS(chr);
                    return;
                }
                CashShopItem csi = new CashShopItem(ci);
                int cost = csi.getNewPrice();
                InvType slotType = InvType.getInvTypeByVal((ci.getItemId() - 9110000) / 1000);
                int slotCount = ci.getItemId() == 9111004 || ci.getItemId() == 9112004 || ci.getItemId() == 9113004 || ci.getItemId() == 9114004 ? 4 : 8;
                Inventory inv = chr.getInventoryByType(slotType);
                if (inv.getSlots() >= GameConstants.MAX_INVENTORY_SLOTS) {
                    c.write(CCashShop.failed(CashItemType.Res_IncSlotCount_Failed, CashErrorType.ReachedMaximumSlots));
                    disposeForCS(chr);
                    return;
                }
                boolean notEnoughMoney = false;
                switch (paymentMethod) {
                    case 1: // Prepaid
                        if (user.getNxPrepaid() >= cost) {
                            user.deductNXPrepaid(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                    case 2: // Maple points
                        if (user.getMaplePoints() >= cost) {
                            user.deductMaplePoints(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                    case 3: // Reward Points
                        if (account.getRewardPoint() >= cost) {
                            account.deductRewardPoint(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                }
                if (notEnoughMoney) {
                    c.write(CCashShop.failed(CashItemType.Res_IncSlotCount_Failed, CashErrorType.NotEnoughCash));
                    disposeForCS(chr);
                    return;
                }
                inv.addSlots((byte) slotCount);
                inv.updateSlotsToSQL();
                c.write(CCashShop.cashItemResIncSlotDone(slotType.getVal(), inv.getSlots()));
                c.write(CCashShop.cashItemResPurchaseRecordDone());
                c.write(WvsContext.expandInventory(slotType.getVal(), (byte) inv.getSlots()));
                disposeForCS(chr);
                break;
            }
            case Req_EnableEquipSlotExt: {
                c.write(CCashShop.error());
                disposeForCS(chr);
                break;
            }
            case Req_IncCharSlotCount: {
                inPacket.decodeByte();
                int paymentMethod = inPacket.decodeInt();
                int id = inPacket.decodeInt();
                CommodityInfo ci = CashShopData.getCommodityBySN(id);
                if (ci == null || ci.getOnSale() == 0 || ci.getTerm() != 0) {
                    c.write(CCashShop.error());
                    disposeForCS(chr);
                    return;
                }
                CashShopItem csi = new CashShopItem(ci);
                int cost = csi.getNewPrice();
                if (user.getCharacterSlots() == GameConstants.MAX_CHARACTER_SLOTS) {
                    c.write(CCashShop.failed(CashItemType.Res_IncBuyCharCount_Failed, CashErrorType.ReachedMaximumSlots));
                    disposeForCS(chr);
                    return;
                }
                boolean notEnoughMoney = false;
                switch (paymentMethod) {
                    case 1: // Prepaid
                        if (user.getNxPrepaid() >= cost) {
                            user.deductNXPrepaid(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                    case 2: // Maple points
                        if (user.getMaplePoints() >= cost) {
                            user.deductMaplePoints(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                    case 3: // Reward Points
                        if (account.getRewardPoint() >= cost) {
                            account.deductRewardPoint(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                }
                if (notEnoughMoney) {
                    c.write(CCashShop.failed(CashItemType.Res_IncBuyCharCount_Failed, CashErrorType.NotEnoughCash));
                    disposeForCS(chr);
                    return;
                }
                user.addCharacterSlots(1);
                user.updateUserCharacterSlotToSQL();
                c.write(CCashShop.cashItemResIncBuyCharCountDone((short) c.getUser().getCharacterSlots()));
                c.write(CCashShop.cashItemResPurchaseRecordDone());
                disposeForCS(chr);
                break;
            }
            case Req_IncTrunkCount: {
                inPacket.decodeByte();
                int paymentMethod = inPacket.decodeInt();
                int itemSN = inPacket.decodeInt();
                CommodityInfo ci = CashShopData.getCommodityBySN(itemSN);
                if (ci == null || ci.getOnSale() == 0 || ci.getTerm() != 0) {
                    c.write(CCashShop.error());
                    disposeForCS(chr);
                    return;
                }
                if (trunk.getSlotCount() == GameConstants.MAX_LOCKER_SIZE) {
                    c.write(CCashShop.failed(CashItemType.Res_IncTrunkCount_Failed, CashErrorType.ReachedMaximumSlots));
                    disposeForCS(chr);
                    return;
                }
                CashShopItem csi = new CashShopItem(ci);
                int cost = csi.getNewPrice();
                boolean notEnoughMoney = false;
                switch (paymentMethod) {
                    case 1: // Prepaid
                        if (user.getNxPrepaid() >= cost) {
                            user.deductNXPrepaid(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                    case 2: // Maple points
                        if (user.getMaplePoints() >= cost) {
                            user.deductMaplePoints(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                    case 3: // Reward Points
                        if (account.getRewardPoint() >= cost) {
                            account.deductRewardPoint(cost);
                        } else {
                            notEnoughMoney = true;
                        }
                        break;
                }
                if (notEnoughMoney) {
                    c.write(CCashShop.failed(CashItemType.Res_IncTrunkCount_Failed, CashErrorType.NotEnoughCash));
                    disposeForCS(chr);
                    return;
                }
                trunk.addSlots((byte) (ci.getItemId() == 9110000 ? 8 : 4));
                trunk.updateTrunkSlotToSQL();
                c.write(CCashShop.cashItemResIncTrunkCountDone((short) trunk.getSlotCount()));
                c.write(CCashShop.cashItemResPurchaseRecordDone());
                disposeForCS(chr);
                break;
            }
            case Req_ShowCashItems:
                disposeForCS(chr);
                break;
            case Req_Destroy:
                inPacket.decodeInt();
                inPacket.decodeByte();
                int picSize = inPacket.decodeShort();
                String pic = inPacket.decodeString(picSize); // Pic is bypassed
                long itemSN = inPacket.decodeLong();
                CashItemInfo removedItem = trunk.getLockerItemBySn(itemSN);
                if (removedItem != null) {
                    trunk.getLocker().removeIf(x -> x.getId() == removedItem.getId());
                    c.write(CCashShop.resDestroyDone(itemSN));
                    removedItem.deleteCashItemInfoFromSQL();
                }
                disposeForCS(chr);
                break;
            case Req_PurchaseRecord:
                int val = inPacket.decodeInt();
                c.write(CCashShop.cashItemResPurchaseRecordDone(val, val != 0 ? 0 : 1));
                break;
            default: {
                c.write(CCashShop.error());
                disposeForCS(chr);
                System.out.println("Unhandled cash shop cash item request " + cit);
                break;
            }
        }
    }

    @Handler(op = InHeader.SURPRISE_BOX)
    public static void handleSurpriseBox(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        Account account = chr.getAccount();

        long sn = inPacket.decodeLong();
        boolean isRare = Util.succeedProp(10);
        Trunk trunk = account.getTrunk();
        CashItemInfo box = trunk.getLockerItemBySn(sn);
        Equip equip;
        if (isRare) { // Lmao get fucked
            equip = ItemData.getEquipDeepCopyFromID(ItemConstants.getSSB_Rare_Reward(), false);
        } else {
            equip = ItemData.getEquipDeepCopyFromID(ItemConstants.getSSB_Common_Reward(), false);
        }
        if (equip == null) {
            c.write(CCashShop.surpriseBoxFailed());
            disposeForCS(chr);
            return;
        }
        CashItemInfo cii = CashItemInfo.fromItem(c.getChr(), equip);
        if (cii == null) {
            c.write(CCashShop.surpriseBoxFailed());
            disposeForCS(chr);
            return;
        }
        if (box != null) {
            box.getItem().setQuantity(box.getItem().getQuantity() - 1);
            c.write(CCashShop.openSurpriseBox(cii, box, isRare));
            if (box.getItem().getQuantity() == 0) {
                trunk.getLocker().removeIf(x -> x.getId() == box.getId());
                box.deleteCashItemInfoFromSQL();
                c.write(CCashShop.resDestroyDone(sn));
            } else {
                c.write(CCashShop.resDestroyDone(sn));
                c.write(CCashShop.resAddedCashItemDone(box));
            }
        } else {
            c.write(CCashShop.surpriseBoxFailed());
        }
        trunk.addCashItem(cii);
        cii.updateCashItemInfoToSQL();
        c.write(CCashShop.resAddedCashItemDone(cii));
        disposeForCS(chr);
    }

    private static void handleCashShopPurchase(Client c, int paymentMethod, int id, int quantity, CashItemType doneType, CashItemType failedType) {
        Char chr = c.getChr();
        User user = chr.getUser();
        Account account = chr.getAccount();
        Trunk trunk = account.getTrunk();
        boolean tracked = net.swordie.ms.connection.protocol.Protocol.active().version()
                == net.swordie.ms.connection.protocol.ProtocolVersion.TMS_282_3;
        CommodityInfo ci = CashShopData.getCommodityBySN(id);
        // VIP goods must never fall through to the three legacy wallets. Enable this branch only
        // with the native VIP reply codec and a transaction that persists both debit and delivery.
        if (tracked && net.swordie.ms.world.shop.cashshop.CashVipPoints.isVipCommodity(id)) {
            c.write(CCashShop.failed(failedType, CashErrorType.PleaseTryAgain));
            disposeForCS(chr);
            return;
        }
        if (ci == null || ci.getOnSale() == 0 || net.swordie.ms.world.shop.cashshop.CashShopCatalogRules.isBlocked(id)
                || (tracked ? !ci.isSaleActive(java.time.LocalDateTime.now()) : ci.getTerm() != 0)) {
            c.write(CCashShop.failed(failedType, CashErrorType.ItemChanged_PleaseRefresh));
            disposeForCS(chr);
            return;
        }
        if(tracked && ci.isOnlyMileage()) {
            if(paymentMethod!=1){c.write(CCashShop.failed(failedType,CashErrorType.PleaseTryAgain));disposeForCS(chr);return;}
            paymentMethod=3; // Client's onlyMileage commodity; native VIP points use a separate balance.
        }
        if (quantity <= 0 || quantity > GameConstants.MAX_LOCKER_SIZE) {
            c.write(CCashShop.failed(failedType, CashErrorType.PleaseTryAgain));
            disposeForCS(chr);
            return;
        }
        CashShopItem csi = new CashShopItem(ci);
        if (csi.getItemID() == 5190005) {
            c.write(CCashShop.failed(failedType, CashErrorType.ThatItemCannotBeMoved));
            disposeForCS(chr);
            return;
        }
        long price = (long) csi.getNewPrice() * quantity;
        long bundle = (long) csi.getBundleQuantity() * quantity;
        if (price < 0 || price > Integer.MAX_VALUE || bundle < 1 || bundle > Integer.MAX_VALUE) {
            c.write(CCashShop.failed(failedType, CashErrorType.PleaseTryAgain));
            disposeForCS(chr);
            return;
        }
        int cost = (int) price;
        List<Integer> packageMembers = tracked ? net.swordie.ms.loaders.Tms282CashPackages.components(ci.getItemId()) : List.of();
        boolean isPackage = !packageMembers.isEmpty();
        if(tracked && doneType==CashItemType.Res_BuyPackage_Done && !isPackage) {
            c.write(CCashShop.failed(failedType,CashErrorType.ItemChanged_PleaseRefresh));disposeForCS(chr);return;
        }
        List<CashItemInfo> delivery;
        try { delivery=prepareCashDelivery(ci,packageMembers,quantity,account,chr); }
        catch(IllegalArgumentException e) {
            System.out.printf("[CashShop] commodity %d unavailable before payment: %s%n",id,e.getMessage());
            c.write(CCashShop.failed(failedType,CashErrorType.ItemChanged_PleaseRefresh));disposeForCS(chr);return;
        }
        if (trunk.getLocker().size() + delivery.size() > GameConstants.MAX_LOCKER_SIZE || (isPackage && delivery.size()>255)) {
            c.write(CCashShop.failed(failedType, CashErrorType.TooManyCashItems));
            disposeForCS(chr);
            return;
        }
        int purchasePeriod = net.swordie.ms.world.shop.cashshop.CashShopPurchaseCounts.period(ci, java.time.LocalDate.now());
        if (paymentMethod < 1 || paymentMethod > 3) {
            c.write(CCashShop.failed(failedType, CashErrorType.PleaseTryAgain));disposeForCS(chr);return;
        }
        if (tracked) {
            try {
                if (!net.swordie.ms.world.shop.cashshop.CashShopPurchaseCounts.reserve(user.getId(), ci, quantity, purchasePeriod)) {
                    c.write(CCashShop.failed(failedType,CashErrorType.ExceededThePurchaseLimitForThisItem));disposeForCS(chr);return;
                }
            } catch (java.sql.SQLException e) {
                System.out.println("[CashShop] purchase count reservation failed: " + e.getClass().getSimpleName());
                c.write(CCashShop.failed(failedType,CashErrorType.PleaseTryAgain));disposeForCS(chr);return;
            }
        }
        boolean notEnoughMoney = false;
        try { switch (paymentMethod) {
            case 1: // Prepaid
                if (user.getNxPrepaid() >= cost) {
                    user.deductNXPrepaid(cost);
                } else {
                    notEnoughMoney = true;
                }
                break;
            case 2: // Maple points
                if (user.getMaplePoints() >= cost) {
                    user.deductMaplePoints(cost);
                } else {
                    notEnoughMoney = true;
                }
                break;
            case 3: // Reward Points
                if (account.getRewardPoint() >= cost) {
                    account.deductRewardPoint(cost);
                } else {
                    notEnoughMoney = true;
                }
                break;
            default:
                c.write(CCashShop.failed(failedType, CashErrorType.PleaseTryAgain));
                disposeForCS(chr);
                return;
        }
        } catch (RuntimeException e) {
            if (tracked) cancelCashPurchaseCount(user, ci, quantity, purchasePeriod);
            throw e;
        }
        if (notEnoughMoney) {
            if (tracked) cancelCashPurchaseCount(user, ci, quantity, purchasePeriod);
            CashErrorType reason=tracked && paymentMethod==3?CashErrorType.NotEnoughRewardPoints
                    :tracked && paymentMethod==2?CashErrorType.PleaseTryAgain:CashErrorType.NotEnoughCash;
            c.write(CCashShop.failed(failedType, reason));
            disposeForCS(chr);
            return;
        }
        for(CashItemInfo cashItemInfo:delivery) {
            cashItemInfo.updateCashItemInfoToSQL();
            account.getTrunk().addCashItem(cashItemInfo);
            var item = cashItemInfo.getItem();
            item.setTrunkID(account.getTrunk().getId());
            item.saveToSQL();
            if(!isPackage)c.write(CCashShop.cashItemResBuyDone(doneType,cashItemInfo,null,null));
        }
        if(isPackage) {
            var codec=(net.swordie.ms.connection.packet.Tms282CashShopPackets)net.swordie.ms.connection.protocol.codec.CashShopPacketEncoder.active().orElseThrow();
            c.write(codec.buyPackageDone(delivery));
        }
        if (!tracked) c.write(CCashShop.cashItemResPurchaseRecordDone());
        else {
            try {
                int count=net.swordie.ms.world.shop.cashshop.CashShopPurchaseCounts.count(user.getId(),ci);
                for(int scope:new int[]{3,4})c.write(net.swordie.ms.connection.packet.Tms282CashShopPackets.purchaseCount(
                        id,scope,count,net.swordie.ms.util.FileTime.currentTime()));
            } catch(java.sql.SQLException e) { System.out.println("[CashShop] purchase succeeded; count refresh unavailable"); }
        }

        int freeRP = (int) (cost * 5L / 100);
        account.addRewardPoint(freeRP);
        if (!tracked) c.write(CCashShop.chargeMileageNotice(freeRP));

        disposeForCS(chr);
    }

    private static void cancelCashPurchaseCount(User user, CommodityInfo item, int quantity, int period) {
        try { net.swordie.ms.world.shop.cashshop.CashShopPurchaseCounts.cancel(user.getId(),item,quantity,period); }
        catch (java.sql.SQLException e) { throw new IllegalStateException("Cash purchase count reservation could not be cancelled",e); }
    }

    /** Build every component before any charge; package children need not be sold separately. */
    public static List<CashItemInfo> prepareCashDelivery(CommodityInfo parent, List<Integer> members, int quantity, Account account, Char chr) {
        List<CashItemInfo> result=new ArrayList<>();
        List<CommodityInfo> items=members.isEmpty()?List.of(parent):members.stream().map(CashShopData::getCommodityBySN).toList();
        for(CommodityInfo info:items) {
            if(info==null || info.getCount()<1)throw new IllegalArgumentException("Missing component commodity");
            boolean single=ItemConstants.isEquip(info.getItemId()) || ItemConstants.isPet(info.getItemId());
            var itemInfo=single?null:ItemData.getItemInfoByID(info.getItemId());
            if(!single && itemInfo==null)throw new IllegalArgumentException("Missing item data "+info.getItemId());
            int remaining=Math.multiplyExact(info.getCount(),quantity);
            int stack=single?1:Math.max(1,Math.min(Short.MAX_VALUE,itemInfo.getSlotMax()));
            while(remaining>0) {
                if(result.size()>=GameConstants.MAX_LOCKER_SIZE)throw new IllegalArgumentException("Too many component slots");
                CashShopItem component=new CashShopItem(info);
                component.setBundleQuantity(Math.min(remaining,stack));
                var item=component.toCashItemInfo(account,chr);
                if(item==null || item.getItem()==null)throw new IllegalArgumentException("Unusable item data "+info.getItemId());
                result.add(item);remaining-=Math.min(remaining,stack);
            }
        }
        return result;
    }

    private static void disposeForCS(Char chr) {
        chr.write(CCashShop.queryCashResult(chr));
        Trunk trunk = chr.getAccount().getTrunk();
        final List<CashItemInfo> locker = trunk.getLocker();
        final List<CashItemInfo> removes = new ArrayList<>();
        for (CashItemInfo cii : locker) {
            if (cii.getItem() == null) {
                cii.deleteCashItemInfoFromSQL();
                continue;
            }
            if (cii.getItem() != null && cii.getItem().getDateExpire() != null && cii.getItem().getDateExpire().isExpired()) {
                chr.write(CCashShop.resExpireDone(cii.getItem().getId()));
                removes.add(cii);
                cii.deleteCashItemInfoFromSQL();
            }
        }
        trunk.getLocker().removeAll(removes);
        chr.dispose();
    }

    @Handler(op = InHeader.M_V_P__DAILY_PACK__REQUEST)
    public static void handleCashShopMVPDailyPackRequest(Client c, InPacket inPacket) {
        disposeForCS(c.getChr());
    }

    @Handler(op = InHeader.M_V_P__SPECIAL_PACK__REQUEST)
    public static void handleCashShopMVPSpecialPackRequest(Client c, InPacket inPacket) {
        disposeForCS(c.getChr());
    }
}
