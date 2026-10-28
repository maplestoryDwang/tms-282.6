package net.swordie.ms.handlers.user;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.trunk.Trunk;
import net.swordie.ms.client.trunk.TrunkMsg;
import net.swordie.ms.client.trunk.TrunkType;
import net.swordie.ms.client.trunk.TrunkUpdate;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.FieldPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import static net.swordie.ms.enums.InventoryOperation.*;

public class UserTrunkHandler {
    @Handler(op = InHeader.USER_TRUNK_REQUEST)
    public static void handleUserTrunkRequest(Char chr, InPacket inPacket) {
        Trunk trunk = chr.getAccount().getTrunk();
        if (trunk != null) net.swordie.ms.world.tms282.audit.AuditContext.mark(chr, net.swordie.ms.world.tms282.audit.AuditLog.STORAGE, trunk.getId()); // audit label
        byte req = inPacket.decodeByte();
        TrunkType trunkType = TrunkType.getByVal(req);
        if (trunkType == null) {
            System.out.printf("Unknown trunk request type %d.%n", req);
            return;
        }
        switch (trunkType) {
            case TrunkReq_Money:
                long reqMoney = inPacket.decodeLong();
                if (reqMoney == 0) {
                    chr.dispose();
                    return;
                }
                long curMoney = chr.getMoney();
                if (reqMoney < 0) {
                    long addMoney = -reqMoney;
                    if (addMoney > curMoney || chr.getMoney() <= 0) {
                        chr.chatPopup("你的楓幣不足，無法存入倉庫。");
                        chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_MoneySuccess, trunk)));
                        return;
                    }
                    if (trunk.canAddMoney(addMoney)) {
                        chr.deductMoney(addMoney);
                        trunk.addMoney(addMoney);
                        trunk.saveToSQL();
                        chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_MoneySuccess, trunk)));
                        DataPrinter.send(DataPrinter.STORAGE, String.format("%s 存入 %s meso。倉庫餘額: %s。背包餘額: %s。",
                                chr.getName(),
                                Util.getNumberFormat(reqMoney),
                                Util.getNumberFormat(trunk.getMoney()),
                                Util.getNumberFormat(chr.getMoney())));
                    } else {
                        chr.chatPopup("無法繼續向倉庫存入楓幣。");
                        chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_MoneySuccess, trunk)));
                        return;
                    }
                } else {
                    if (trunk.getMoney() <= 0) {
                        chr.chatPopup("倉庫中沒有可取出的楓幣。");
                        chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_MoneySuccess, trunk)));
                        return;
                    }
                    if (reqMoney <= trunk.getMoney() && chr.canAddMoney(reqMoney)) {
                        chr.addMoney(reqMoney);
                        trunk.deductMoney(reqMoney);
                        trunk.saveToSQL();
                        chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_MoneySuccess, trunk)));
                        DataPrinter.send(DataPrinter.STORAGE, String.format("%s 取出 %s meso。倉庫餘額: %s。背包餘額: %s。",
                                chr.getName(),
                                Util.getNumberFormat(reqMoney),
                                Util.getNumberFormat(trunk.getMoney()),
                                Util.getNumberFormat(chr.getMoney())));
                    } else {
                        chr.chatPopup("無法從倉庫取出楓幣。");
                        chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_MoneySuccess, trunk)));
                        return;
                    }
                }
                break;
            case TrunkReq_GetItem:
                byte invTypeVal = inPacket.decodeByte();
                byte itemIndex = inPacket.decodeByte();
                short pos = inPacket.decodeShort();
                Inventory inventory = chr.getInventoryByType(InvType.getInvTypeByVal(invTypeVal));
                if (inventory == null) {
                    return;
                }
                Item item = trunk.getItemByIndexAndType(InvType.getInvTypeByVal(invTypeVal), itemIndex);//trunk.getItems().get(itemIndex);
                if (item == null) {
                    chr.chatPopup("物品不存在。");
                    chr.dispose();
                    return;
                }
                if (inventory.canPickUp(item)) {
                    takeItem(chr, inventory, trunk, item, true);
                } else {
                    chr.write(FieldPacket.trunkDlg(new TrunkMsg(TrunkType.TrunkRes_GetUnknown)));
                }
                chr.dispose();
                break;
            case TrunkReq_FindAll: {
                findAll(chr, trunk);
                break;
            }
            case TrunkReq_AutoStore:
                inPacket.decodeInt();
                int moved = 0;
                final HashMap<Integer, ArrayList<Item>> trunkStacks = new HashMap<>(trunk.getItems().size() * 2);
                for (Item t : trunk.getItems()) {
                    trunkStacks.computeIfAbsent(t.getItemId(), k -> new ArrayList<>(1)).add(t);
                }
                final ArrayList<Item> toMove = new ArrayList<>(128);
                for (Inventory inv : chr.getInventories()) {
                    for (Item it : inv.getItems()) {
                        if (it == null) continue;
                        if (it.getQuantity() <= 0) continue;
                        if (!it.isTradable()) continue;
                        if (it instanceof Equip) continue;
                        if (it.isCash()) continue;
                        if (!trunkStacks.containsKey(it.getItemId())) continue;
                        toMove.add(it);
                    }
                }
                for (Item it : toMove) {
                    ArrayList<Item> stacks = trunkStacks.get(it.getItemId());
                    if (stacks == null || stacks.isEmpty()) continue;
                    final int itemID = it.getItemId();
                    ItemInfo ii = ItemData.getItemInfoByID(itemID);
                    if (ii == null) continue;
                    final int slotMax = ii.getSlotMax();
                    if (slotMax <= 0) continue;
                    int remain = it.getQuantity();
                    boolean changedAny = false;
                    for (int i = 0; i < stacks.size() && remain > 0; i++) {
                        Item exist = stacks.get(i);
                        int cur = exist.getQuantity();
                        if (cur >= slotMax) continue;
                        int can = slotMax - cur;
                        int moveQty = Math.min(remain, can);
                        exist.setQuantity(cur + moveQty);
                        exist.updateQuantityInSQL();
                        remain -= moveQty;
                        changedAny = true;
                    }
                    if (!changedAny) continue;
                    int movedQty = it.getQuantity() - remain;
                    if (movedQty <= 0) continue;
                    if (remain == 0) {
                        chr.write(WvsContext.inventoryOperation(true, false, Remove,
                                (short) it.getBagIndex(), (byte) 0, 0, it));
                        net.swordie.ms.world.tms282.audit.AuditLog.itemLose(chr, it, movedQty, net.swordie.ms.world.tms282.audit.AuditLog.STORAGE_PUT, trunk.getId()); // audit
                        chr.getInventoryByType(it.getInvType()).removeItem(it);
                        it.setInventoryID(0);
                        it.deleteFromSQL();
                    } else {
                        it.setQuantity(remain);
                        net.swordie.ms.world.tms282.audit.AuditLog.itemLose(chr, it, movedQty, net.swordie.ms.world.tms282.audit.AuditLog.STORAGE_PUT, trunk.getId()); // audit
                        it.updateQuantityInSQL();
                        chr.write(WvsContext.inventoryOperation(true, false, UpdateQuantity,
                                (short) it.getBagIndex(), (byte) 0, 0, it));
                    }
                    DataPrinter.send(DataPrinter.STORAGE, String.format("%s 存入 %s (%d)，數量: %s。", chr.getName(), StringData.getItemStringById(itemID), itemID, remain));
                    moved++;
                }
                chr.write(FieldPacket.trunkDlg(new TrunkUpdate(trunk, moved, false)));
                break;
            case TrunkReq_PutItem:
                short slot = inPacket.decodeShort();
                int itemID = inPacket.decodeInt();
                short quantity = inPacket.decodeShort();
                InvType invType = ItemConstants.getInvTypeByItemID(itemID);
                if (invType == null) {
                    return;
                }
                inventory = chr.getInventoryByType(invType);
                if (inventory == null) {
                    return;
                }
                item = inventory.getItemBySlot(slot);
                if (item != null && quantity > 0 && item.getQuantity() >= quantity && item.getItemId() == itemID) {
                    if (trunk.getItems().size() >= trunk.getSlotCount()) {
                        chr.write(FieldPacket.trunkDlg(new TrunkMsg(TrunkType.TrunkRes_PutNoSpace)));
                        return;
                    }
                    putItem(chr, inventory, trunk, item, quantity);
                    DataPrinter.send(DataPrinter.STORAGE, String.format("%s 存入 %s (%d)，數量: %s。", chr.getName(), StringData.getItemStringById(itemID), itemID, quantity));
                } else {
                    chr.write(FieldPacket.trunkDlg(new TrunkMsg(TrunkType.TrunkRes_GetUnknown)));
                }
                chr.dispose();
                break;
            case TrunkReq_SortItem:
                trunk.sortItemsByIndex();
                chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_SortItem, trunk)));
                break;
            case TrunkReq_CloseDialog:
                chr.dispose();
                break;
            default:
                chr.dispose();
        }
    }

    public static void takeItem(Char chr, Inventory inventory, Trunk trunk, Item item, boolean sendPacket) {
        ItemInfo itemInfo = ItemData.getItemInfoByID(item.getItemId());
        Item curItem = null;
        for (Item temp : inventory.getItems()) {
            if (temp.getItemId() == item.getItemId()
                    && !ItemConstants.isEquip(item.getItemId())
                    && !ItemConstants.isThrowingItem(item.getItemId())
                    && !ItemConstants.isBullet(item.getItemId())) {
                if (itemInfo != null && temp.getQuantity() < itemInfo.getSlotMax()) {
                    curItem = temp;
                    break;
                }
            }
        }
        if (curItem == null
                || ItemConstants.isEquip(item.getItemId())
                || ItemConstants.isThrowingItem(item.getItemId())
                || ItemConstants.isBullet(item.getItemId())) {
            Item newItem = item.deepCopy();
            newItem.setTrunkID(0);
            newItem.setInventoryID(inventory.getId());
            if (ItemConstants.isEquip(item.getItemId())) {
                Equip equip = (Equip) newItem;
                if (equip.isAccountSharable() && equip.isSharableOnce()) {
                    equip.addAttribute(EquipAttribute.UnTradable);
                }
            } else if (itemInfo != null && itemInfo.isSharableOnce() && itemInfo.isAccountSharable() && !itemInfo.isTradeBlock()) {
                newItem.addAttribute((short) ItemState.KarmaUsed.getVal());
            }
            chr.addItemToInventory(newItem);
            if (newItem instanceof Equip equip) {
                if (!equip.isCash()) {
                    equip.recalcEnchantmentStats();
                }
            }
            item.deleteFromSQL();
            trunk.removeItem(item);
        } else {
            int quantity = item.getQuantity() + curItem.getQuantity();
            if (itemInfo != null && quantity > itemInfo.getSlotMax() && curItem.getQuantity() != itemInfo.getSlotMax()) {
                int quantityLeft = quantity - itemInfo.getSlotMax();
                Item newItem = item.deepCopy();
                newItem.setTrunkID(0);
                newItem.setInventoryID(inventory.getId());
                newItem.setQuantity(item.getQuantity() - quantityLeft);
                chr.addItemToInventory(newItem);
                item.setQuantity(quantityLeft);
                item.updateQuantityInSQL();
            } else {
                Item newItem = item.deepCopy();
                newItem.setTrunkID(0);
                newItem.setInventoryID(inventory.getId());
                chr.addItemToInventory(newItem);
                item.deleteFromSQL();
                trunk.removeItem(item);
            }
        }
        if (sendPacket) {
            chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_GetSuccess, trunk)));
        }
        DataPrinter.send(DataPrinter.STORAGE, String.format("%s 取出 %s (%d)，數量: %s。", chr.getName(), StringData.getItemStringById(item.getItemId()), item.getItemId(), item.getQuantity()));
    }

    public static void findAll(Char chr, Trunk trunk) {
        final ArrayList<Item> items = new ArrayList<>(trunk.getItems());
        final ArrayList<Integer> result = new ArrayList<>();
        for (Item item : items) {
            if (item == null) continue;
            InvType invType = InvType.getInvTypeByVal(item.getInvType().getVal());
            if (invType == null) continue;
            Inventory inv = chr.getInventoryByType(invType);
            if (inv == null) continue;
            //if (item instanceof Equip) continue;
            if (item.isCash()) continue;
            final int beforeQuantity = item.getQuantity();
            takeItem(chr, inv, trunk, item, false);
            result.add(beforeQuantity - item.getQuantity());
        }
        chr.write(FieldPacket.trunkDlg(new TrunkUpdate(trunk, result, false)));
    }

    public static void putItem(Char chr, Inventory inventory, Trunk trunk, Item item, short requestQuantity) {
        if(net.swordie.ms.client.character.tms282.Tms282CubeRuntime.refuseMove(chr,item))return;
        //Gửi hết
        if (requestQuantity >= item.getQuantity() || ItemConstants.isThrowingItem(item.getItemId())) {
            chr.write(WvsContext.inventoryOperation(true, false, Remove, (short) item.getBagIndex(), (byte) 0, 0, item));
            item.setInventoryID(0);
            item.setTrunkID(trunk.getId());
            item.updatePositionInSQL();

            net.swordie.ms.world.tms282.audit.AuditLog.itemLose(chr, item, item.getQuantity(), net.swordie.ms.world.tms282.audit.AuditLog.STORAGE_PUT, trunk.getId()); // audit
            inventory.removeItem(item);
            trunk.addItem(item, (short) item.getQuantity());
        }
        //Gửi số lượng custom
        else {
            Item newItem = item.deepCopy();
            newItem.setInventoryID(0);
            newItem.setTrunkID(trunk.getId());
            newItem.setQuantity(requestQuantity);
            newItem.saveToSQL();
            trunk.addItem(newItem, (short) newItem.getQuantity());

            chr.consumeItem(item, requestQuantity);
            item.saveToSQL();
        }
        chr.deductMoney(500);
        chr.write(FieldPacket.trunkDlg(new TrunkUpdate(TrunkType.TrunkRes_PutSuccess, trunk)));
    }
}
