package net.swordie.ms.handlers.item;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.Core;
import net.swordie.ms.client.character.items.*;
import net.swordie.ms.client.character.tms282.Tms282CubeRuntime;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.AndroidPacket;
import net.swordie.ms.connection.packet.EvolvingPacket;
import net.swordie.ms.connection.packet.FoxManPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.EvolvingSystemType;
import net.swordie.ms.enums.FieldOption;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.ReactorData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.ReactorInfo;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.boss.Zakum;
import net.swordie.ms.world.field.Field;
import net.swordie.ms.world.field.Foothold;

import java.util.*;
import java.util.stream.Collectors;

import static net.swordie.ms.enums.InvType.*;
import static net.swordie.ms.enums.InventoryOperation.*;

public class InventoryHandler {

    @Handler(op = InHeader.USER_CHANGE_SLOT_POSITION_REQUEST)
    public static void handleUserChangeSlotPositionRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // update tick
        InvType invType = InvType.getInvTypeByVal(inPacket.decodeByte());
        if (invType == null) {
            chr.chatPopup("不能在該位置變更物品槽位。");
            chr.dispose();
            return;
        }
        short oldPos = inPacket.decodeShort();
        short newPos = inPacket.decodeShort();
        short quantity = inPacket.decodeShort();
        int fromItemID = inPacket.decodeInt();
        int toItemID = inPacket.decodeInt();
        // chặn pos âm cho các túi không phải EQUIP/DECORATION
        if ((oldPos < 0 || newPos < 0) &&
                !(invType == InvType.EQUIP || invType == InvType.DECORATION)) {
            chr.chatPopup("只有裝備/裝飾物可以放入已裝備位置。");
            chr.dispose();
            return;
        }
        InvType invTypeFrom = oldPos < 0 ? InvType.EQUIPPED : invType;
        InvType invTypeTo   = newPos < 0 ? InvType.EQUIPPED : invType;
        if (ServerConfig.DEBUG_MODE) {
            chr.chatMessage("oldPos: " + oldPos + " (" + invTypeFrom.name() +  ") ; newPos: " + newPos + " (" + invTypeTo.name() + ")");
        }
        if (invTypeFrom.equals(invTypeTo)) {
            if (newPos > chr.getInventoryByType(invTypeFrom).getSlots()) {
                chr.chatPopup("不能將兩個物品移動到同一位置。");
                chr.dispose();
                return;
            }
        }
        Item fromItem = chr.getInventoryByType(invTypeFrom).getItemBySlot(oldPos);
        if (fromItem == null) {
            chr.chatPopup("背包中找不到要移動的物品。");
            chr.dispose();
            return;
        }
        if(Tms282CubeRuntime.refuseMove(chr,fromItem)
                || newPos!=0 && Tms282CubeRuntime.refuseMove(chr,chr.getInventoryByType(invTypeTo).getItemBySlot(Math.abs((int)newPos))))return;
        if (newPos < 0 && !net.swordie.ms.loaders.Tms282PetEquipment.canWear(chr, fromItem, newPos)) {
            chr.chatPopup("此裝備與該欄位的寵物不相容，請先召喚對應寵物。");
            chr.dispose();
            return;
        }
        if (newPos < 0 && -newPos >= BodyPart.TotemBase.getVal() && -newPos < BodyPart.TotemEnd.getVal() && !ItemConstants.isTotem(fromItem.getItemId())) {
            chr.chatPopup("不能在該位置變更物品槽位。");
            chr.dispose();
            return;
        }
        if (newPos < 0 && -newPos >= BodyPart.CBPBase.getVal() && -newPos <= BodyPart.CBPEnd.getVal() && !fromItem.isCash()) {
            chr.chatPopup("不能在該位置變更物品槽位。");
            chr.dispose();
            return;
        }
        if (newPos < 0 && -newPos >= BodyPart.ZeroBase.getVal() && -newPos < BodyPart.ZeroEnd.getVal() && !fromItem.isCash()) {
            chr.chatPopup("不能在該位置變更物品槽位。");
            chr.dispose();
            return;
        }
        if (newPos < 0 && -newPos >= BodyPart.APTop.getVal() && -newPos < BodyPart.APEnd.getVal() && !fromItem.isCash()) {
            chr.chatPopup("不能在該位置變更物品槽位。");
            chr.dispose();
            return;
        }
        Item zeroShareItem = null;
        if (newPos == 0) { // Drop
            Field field = chr.getField();
            if ((field.getFieldLimit() & FieldOption.DropLimit.getVal()) > 0) {
                chr.dispose();
                return;
            }
            if (field.getDropsDisabled()) {
                chr.chatMessage("這個區域無法丟棄道具。");
                chr.dispose();
                return;
            }
            boolean fullDrop;
            Drop drop;
            if (!fromItem.getInvType().isStackable() || quantity >= fromItem.getQuantity() || ItemConstants.isThrowingStar(fromItem.getItemId()) || ItemConstants.isBullet(fromItem.getItemId())) {
                // Whole fromItem is dropped (equip/stackable items with all their quantity)
                fullDrop = true;
                chr.getInventoryByType(invTypeFrom).removeItem(fromItem);
                net.swordie.ms.world.tms282.audit.AuditLog.itemLose(chr, fromItem, fromItem.getQuantity(), net.swordie.ms.world.tms282.audit.AuditLog.DROP, field.getId()); // audit
                DataPrinter.send(DataPrinter.ITEM, String.format("Player %s has dropped item %s | ID: %d | Item ID: %d | Quantity: %d | Bag Type: %d | Bag Position: %d.",
                        chr.getName(), StringData.getItemStringById(fromItem.getItemId()), fromItem.getId(), fromItem.getItemId(), fromItem.getQuantity(), fromItem.getInvType().getVal(), fromItem.getBagIndex()), true);
                if (ItemConstants.isCoreItem(fromItem.getItemId())) {
                    Set<Core> removeSet = chr.getCores().stream().filter(core -> core.getCoreID() == fromItem.getItemId()).collect(Collectors.toSet());
                    Core core = chr.getCores().stream().filter(core1 -> core1.getCoreID() == fromItem.getItemId()).findFirst().orElse(null);
                    if (core != null) {
                        chr.getCores().removeIf(x -> x.getCoreID() == core.getCoreID() && x.getPos() == core.getPos() && x.getCharId() == core.getCharId() && x.getSlotType() == core.getSlotType());
                        core.deleteCoreFromSQL();
                        chr.write(EvolvingPacket.coreInventoryOperation(removeSet, EvolvingSystemType.Remove));
                    }
                }
                fromItem.drop();
                drop = new Drop(-1, fromItem);
            } else {
                // Part of the stack is dropped
                fullDrop = false;
                Item dropItem = ItemData.getItemDeepCopy(fromItem.getItemId());
                dropItem.setQuantity(quantity);
                if (ItemConstants.isCoreItem(fromItem.getItemId())) {
                    Core core = chr.getCores().stream().filter(core1 -> core1.getCoreID() == fromItem.getItemId()).findAny().orElse(null);
                    if (core != null) {
                        core.setLeftCount(core.getLeftCount() - quantity);
                        chr.write(EvolvingPacket.coreInventoryOperation(chr.getCores(), EvolvingSystemType.Update_Quantity));
                    }
                }
                net.swordie.ms.world.tms282.audit.AuditLog.itemLose(chr, fromItem, quantity, net.swordie.ms.world.tms282.audit.AuditLog.DROP, field.getId()); // audit
                fromItem.removeQuantity(quantity);
                drop = new Drop(-1, dropItem);
            }
            int x = chr.getPosition().getX();
            int y = chr.getPosition().getY();
            Foothold fh = field.findFootHoldBelow(new Position(x, y - GameConstants.DROP_HEIGHT));
            Position posTo = new Position(x, fh.getYFromX(x));
            if (fromItem.getItemId() == 4001796 && field.getId() == BossConstants.ZAKUM_EASY_ALTAR && field.getZakumStand() == 0) {
                Zakum.spawn(1, field);
            } else if (fromItem.getItemId() == 4001017 && field.getZakumStand() == 0) {
                if (field.getId() == BossConstants.ZAKUM_NORMAL_ALTAR) {
                    Zakum.spawn(2, field);
                } else if (field.getId() == BossConstants.ZAKUM_CHAOS_ALTAR) {
                    Zakum.spawn(3, field);
                }
            } else {
                field.drop(drop, chr.getPosition(), posTo);
                drop.setCanBePickedUpByPet(false);
            }
            if (newPos < 0 && -newPos >= BodyPart.APBase.getVal() && -newPos < BodyPart.APEnd.getVal() && chr.getAndroid() != null) {
                field.broadcast(AndroidPacket.modified(chr.getAndroid()));
            }
            if (fullDrop) {
                chr.write(WvsContext.inventoryOperation(true, false, Remove, oldPos, newPos, 0, fromItem));
            } else {
                chr.write(WvsContext.inventoryOperation(true, false, UpdateQuantity, oldPos, newPos, 0, fromItem));
            }

            Reactor reactor = chr.getField().getReactors().stream().filter(r -> r.getLimitedName().equals("boss")).findFirst().orElse(null);
            if (reactor != null) {
                ReactorInfo ri = ReactorData.getReactorInfoByID(reactor.getTemplateId());
                String action = ri.getAction();
                reactor.startScript(chr, action);
            }
        }
        else {
            Item toItem = chr.getInventoryByType(invTypeTo).getItemBySlot(newPos);
            if (oldPos < 0 && toItem != null && !net.swordie.ms.loaders.Tms282PetEquipment.canWear(chr, toItem, oldPos)) {
                chr.chatPopup("交換的裝備與該欄位的寵物不相容。");
                chr.dispose();
                return;
            }
            if (ItemConstants.isSymbol(fromItem.getItemId())) {
                if (ItemConstants.isArcaneSymbol(fromItem.getItemId()) && invTypeFrom == EQUIP) { // Prevent equipping 2 same Arcane Symbol
                    Equip existArcaneSymbol = chr.getArcaneSymbols().stream().filter(a -> a.getItemId() == fromItem.getItemId()).findFirst().orElse(null);
                    if (existArcaneSymbol != null) {
                        chr.chatPopup("不能重複裝備祕法符文。");
                        chr.dispose();
                        return;
                    }
                } else if (ItemConstants.isSacredSymbol(fromItem.getItemId()) && invTypeFrom == EQUIP) { // Prevent equipping 2 same Sacred Symbol
                    Equip existSacredSymbol = chr.getSacredSymbols().stream().filter(a -> a.getItemId() == fromItem.getItemId()).findFirst().orElse(null);
                    if (existSacredSymbol != null) {
                        chr.chatPopup("不能重複裝備真實符文。");
                        chr.dispose();
                        return;
                    }
                }
            }
            if (toItem != null) {
                if (oldPos < 0 && -oldPos >= BodyPart.CBPBase.getVal() && -oldPos <= BodyPart.PetAcc3.getVal() && !toItem.isCash()) {
                    chr.chatPopup("不能在該位置變更物品槽位。");
                    chr.dispose();
                    return;
                }
                if (oldPos < 0 && -oldPos >= BodyPart.ZeroBase.getVal() && -oldPos < BodyPart.ZeroEnd.getVal() && !toItem.isCash()) {
                    chr.chatPopup("不能在該位置變更物品槽位。");
                    chr.dispose();
                    return;
                }
                if (oldPos < 0 && -oldPos >= BodyPart.APTop.getVal() && -oldPos < BodyPart.APEnd.getVal() && !toItem.isCash()) {
                    chr.chatPopup("不能在該位置變更物品槽位。");
                    chr.dispose();
                    return;
                }
            }
            int oldChuc = 0;
            int newChuc = 0;
            if ((invTypeFrom == EQUIP || invTypeFrom == DECORATION) && invTypeTo == EQUIPPED) {
                if (toItem != null) {
                    if (JobConstants.isZero(chr.getJob()) && toItem.getZeroShareItemID() != 0) {
                        for (Item item : chr.getEquippedInventory().getItems()) {
                            if (item.getItemId() == toItem.getItemId() && item.getBagIndex() != toItem.getBagIndex()) {
                                chr.consumeItem(item);
                                break;
                            }
                        }
                    }
                    toItem.setInventoryID(toItem.isCash() ? chr.getDecorationInventory().getId() : chr.getEquipInventory().getId());
                    toItem.setInvType(toItem.isCash() ? DECORATION : EQUIP);
                    chr.unequip(toItem, oldPos);
                    if (toItem instanceof Equip equip) {
                        oldChuc = equip.getChuc();
                    }
                }
                /*int preset = chr.hasQuest(QuestConstants.EQUIPPED_PRESET) ? Integer.parseInt(chr.getQRValueByKey(QuestConstants.EQUIPPED_PRESET, "preset")) : 0;
                if (!fromItem.isCash() && fromItem instanceof Equip equip) {
                    if (-newPos >= BodyPart.BPBase.getVal() && -oldPos <= BodyPart.BPEnd.getVal()) {
                        if (preset == 0) {
                            equip.addHyperUpgrade((short) EquipState.PresetItem.getVal());
                            equip.addHyperUpgrade((short) EquipState.Preset1Unequip.getVal());
                        } else if (preset == 1) {
                            equip.addHyperUpgrade((short) EquipState.PresetItem.getVal());
                            equip.addHyperUpgrade((short) EquipState.Preset2Unequip.getVal());
                            fromItem.setBagIndex(newPos < 0 ? newPos - 3000 : newPos + 3000);
                            chr.write(WvsContext.changeEquippedItemPreset(equip, newPos, (short) fromItem.getBagIndex()));
                        } else if (preset == 2) {
                            equip.addHyperUpgrade((short) EquipState.PresetItem.getVal());
                            equip.addHyperUpgrade((short) EquipState.Preset3Unequip.getVal());
                            fromItem.setBagIndex(newPos < 0 ? newPos - 3000 : newPos + 3000);
                            chr.write(WvsContext.changeEquippedItemPreset(equip, newPos, (short) fromItem.getBagIndex()));
                        }
                    }
                }*/
                fromItem.setInventoryID(chr.getEquippedInventory().getId());
                fromItem.setInvType(EQUIPPED);
                chr.equip(fromItem, newPos);
                if (fromItem instanceof Equip equip) {
                    newChuc = equip.getChuc();
                }
                Equip.notifyUnionChuc(chr, newChuc - oldChuc);
            }
            else if (invTypeFrom == EQUIPPED && (invTypeTo == EQUIP || invTypeTo == DECORATION)) {
                if (JobConstants.isZero(chr.getJob()) && fromItem.getZeroShareItemID() != 0) {
                    for (Item item : chr.getEquippedInventory().getItems()) {
                        if (item.getItemId() == fromItem.getItemId() && item.getBagIndex() != fromItem.getBagIndex()) {
                            chr.consumeItem(item);
                            break;
                        }
                    }
                }
                fromItem.setInventoryID(fromItem.isCash() ? chr.getDecorationInventory().getId() : chr.getEquipInventory().getId());
                fromItem.setInvType(fromItem.isCash() ? DECORATION : EQUIP);
                chr.unequip(fromItem, newPos);
                if (fromItem instanceof Equip equip) {
                    oldChuc = equip.getChuc();
                }
                if (toItem != null) {
                    toItem.setInventoryID(chr.getEquippedInventory().getId());
                    toItem.setInvType(EQUIPPED);
                    chr.equip(toItem, oldPos);
                    if (toItem instanceof Equip equip) {
                        newChuc = equip.getChuc();
                    }
                }
            }
            else {
                chr.getInventoryByType(invTypeFrom).moveSlot(fromItem, newPos);
                if (toItem != null) {
                    chr.getInventoryByType(invTypeTo).moveSlot(toItem, oldPos);
                }
            }
            if (newPos < 0 && -newPos >= BodyPart.APBase.getVal() && -newPos < BodyPart.APEnd.getVal() && chr.getAndroid() != null) {
                chr.getField().broadcast(AndroidPacket.modified(chr.getAndroid()));
            }
            if (chr.getFoxMan() != null) {
                if (newPos < 0 && -newPos == BodyPart.HakuFan.getVal()) {
                    chr.getField().broadcast(FoxManPacket.modified(chr.getFoxMan()));
                    chr.getField().broadcast(FoxManPacket.update(chr.getFoxMan()));
                } else if (oldPos < 0 && -oldPos == BodyPart.HakuFan.getVal()) {
                    chr.getField().broadcast(FoxManPacket.update(chr.getFoxMan()));
                }
            }
            chr.write(WvsContext.inventoryOperation(true, false, Move, oldPos, newPos, 0, fromItem));
            fromItem.updateToChar(chr);
            fromItem.updatePositionInSQL();
            if (toItem != null) {
                toItem.updateToChar(chr);
                toItem.updatePositionInSQL();
            }
            Equip.notifyUnionChuc(chr, newChuc - oldChuc);
        }
        chr.setBulletIDForAttack(chr.calculateBulletIDForAttack(1));
    }

    @Handler(op = InHeader.USER_GATHER_ITEM_REQUEST)
    public static void handleUserGatherItemRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        InvType invType = InvType.getInvTypeByVal(inPacket.decodeByte());
        if (invType == null) {
            return;
        }
        Inventory inv = chr.getInventoryByType(invType);
        inv.rebuildIndex();
        List<Item> items = new ArrayList<>(inv.getItems());
        items.sort(Comparator.comparingInt(Item::getBagIndex).reversed());
        Map<Item, Tuple<Short, Short>> newItems = new HashMap<>();
        for (Item item : items) {
            if (Tms282CubeRuntime.isReserved(chr,item) || item instanceof Equip equip && equip.hasHyperUpgrade(EquipState.LockSort.getVal()) || item.hasAttribute((short) ItemState.LockSort.getVal())) {
                continue;
            }
            short oldPos = (short) item.getBagIndex();
            short firstSlot = (short) inv.getFirstOpenSlot();
            if (firstSlot > 0 && firstSlot < oldPos) {
                inv.moveSlot(item, firstSlot);
                newItems.put(item, new Tuple<>(oldPos, firstSlot));
            }
        }
        chr.write(WvsContext.moveItems(newItems));
        chr.write(WvsContext.gatherItemResult(invType.getVal()));
        chr.dispose();
    }

    @Handler(op = InHeader.USER_SORT_ITEM_REQUEST)
    public static void handleUserSortItemRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        InvType invType = InvType.getInvTypeByVal(inPacket.decodeByte());
        if (invType == null) {
            return;
        }
        Inventory inv = chr.getInventoryByType(invType);
        inv.rebuildIndex();
        List<Item> items = new ArrayList<>(inv.getItems());
        List<Item> unlockedItems = new ArrayList<>();
        boolean[] lockedSlot = new boolean[inv.getSlots() + 1];
        for (Item item : items) {
            if (Tms282CubeRuntime.isReserved(chr,item) || item instanceof Equip equip && equip.hasHyperUpgrade(EquipState.LockSort.getVal())
                    || item.hasAttribute((short) ItemState.LockSort.getVal())) {
                lockedSlot[item.getBagIndex()] = true;
                continue;
            }
            unlockedItems.add(item);
            chr.write(WvsContext.inventoryOperation(true, true, Remove, (short) item.getBagIndex(), (short) 0, -1, item));
        }
        unlockedItems.sort(Comparator.comparingInt(Item::getItemId));
        short newPos = 1;
        for (Item item : unlockedItems) {
            while (lockedSlot[newPos]) {
                newPos += 1;
            }
            inv.moveSlot(item, newPos);
            chr.write(WvsContext.inventoryOperation(true, true, Add, (short) newPos, (short) 0, -1, item));
            newPos += 1;
        }
        chr.write(WvsContext.sortItemResult(invType.getVal()));
        chr.dispose();
    }

    @Handler(op = InHeader.USER_LOCK_SORT_ITEM_REQUEST)
    public static void handleLockSortItemRequest(Char chr, InPacket inPacket) {
        inPacket.decodeInt(); // tick
        InvType invType = InvType.getInvTypeByVal(inPacket.decodeInt());
        if (invType == null) {
            return;
        }
        int pos = inPacket.decodeInt();
        boolean isLock = inPacket.decodeByte() != 0;
        Inventory inv = chr.getInventoryByType(invType);
        Item item = inv.getItemBySlot(pos);
        if (item == null) {
            chr.dispose();
            return;
        }
        if (item instanceof Equip equip) {
            if (isLock) {
                equip.addHyperUpgrade(EquipState.LockSort.getVal());
            } else {
                equip.removeHyperUpgrade(EquipState.LockSort.getVal());
            }
            chr.write(WvsContext.inventoryOperation(true, false, Lock, (short) item.getBagIndex(), (short) 0, -1, item));
        } else {
            if (isLock) {
                item.addAttribute((short) ItemState.LockSort.getVal());
            } else {
                item.removeAttribute((short) ItemState.LockSort.getVal());
            }
            chr.write(WvsContext.inventoryOperation(true, false, Lock, (short) item.getBagIndex(), (short) 0, -1, item));
        }
    }
}
