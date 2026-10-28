package net.swordie.ms.handlers.social;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.CharacterStat;
import net.swordie.ms.client.character.TradeRoom;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.EquipAttribute;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.MiniroomPacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.Handler;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.field.Field;

import java.time.LocalDateTime;

import static net.swordie.ms.enums.InventoryOperation.*;

public class RoomHandler {

    @Handler(op = InHeader.MINI_ROOM)
    public static void handleMiniRoom(Char chr, InPacket inPacket) {
        chr.dispose();
        byte type = inPacket.decodeByte(); // MiniRoom Type value
        MiniRoomType mrt = MiniRoomType.getByVal(type);
        if (mrt == null) {
            System.out.printf("Unknown miniroom type %d%n", type);
            return;
        }
        TradeRoom tradeRoom = chr.getTradeRoom();
        Char other;
        switch (mrt) {
            case PlaceItem:
            case PlaceItem_2:
            case PlaceItem_3:
            case PlaceItem_4:
                byte invType = inPacket.decodeByte();
                short bagIndex = inPacket.decodeShort();
                short quantity = inPacket.decodeShort();
                byte tradeSlot = inPacket.decodeByte(); // trade window slot number
                if (chr.getTradeRoom() == null) {
                    return;
                }
                if (!tradeRoom.canChangeOffer()) { // multiplayer-0930: no changes after a confirmation
                    chr.chatMessage("已確認交易，無法再放入物品。");
                    return;
                }
                var inventoryType = InvType.getInvTypeByVal(invType);
                if (inventoryType == null) {
                    return;
                }
                Item item = chr.getInventoryByType(inventoryType).getItemBySlot(bagIndex);
                if (item == null) {
                    return;
                }
                if (quantity <= 0 && !ItemConstants.isThrowingItem(item.getItemId())) {
                    return;
                }
                if (item.getQuantity() < quantity) {
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("Character {%d} tried to trade an item {%d} with a higher quantity {%s} than the item has {%d}.", chr.getId(), item.getItemId(), quantity, item.getQuantity()));
                    return;
                }
                if (item instanceof Equip) {
                    if ((((Equip) item).hasAttribute(EquipAttribute.UnTradable) || (((Equip) item).hasAttribute(EquipAttribute.TradedOnceWithinAccount)))) {
                        DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("角色 {%d} 嘗試交易不可交易的裝備 {%d}。", chr.getId(), item.getItemId()));
                        return;
                    }
                } else if (!item.isTradable()) {
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("角色 {%d} 嘗試交易不可交易的物品 {%d}。", chr.getId(), item.getItemId()));
                    return;
                }
                putItem(chr, chr.getInventoryByType(inventoryType), tradeRoom, tradeSlot, item, quantity, false);
                break;
            case SetMesos:
            case SetMesos_2:
            case SetMesos_3:
            case SetMesos_4:
                long money = inPacket.decodeLong();
                if (tradeRoom == null) {
                    return;
                }
                if (!tradeRoom.canChangeOffer()) { // multiplayer-0930: no changes after a confirmation
                    chr.chatMessage("已確認交易，無法再變更楓幣。");
                    return;
                }
                if (money < 0 || money > chr.getMoney() || chr.getMoney() <= 0) {
                    DataPrinter.send(DataPrinter.AUTOBAN_WARNING, String.format("角色 %d 嘗試輸入異常交易 meso。(%d，背包 meso: %d)", chr.getId(), money, chr.getMoney()));
                    return;
                }
                other = tradeRoom.getOtherChar(chr.getId());
                if (other == null) {
                    tradeRoom.cancelTrade();
                    chr.write(MiniroomPacket.cancelTrade());
                    chr.chatMessage("交易對象已斷開連接。");
                    break;
                }
                net.swordie.ms.world.tms282.audit.AuditContext.mark(chr, net.swordie.ms.world.tms282.audit.AuditLog.TRADE_OFFER, other.getId()); // audit label
                chr.deductMoney(money);
                chr.addMoney(tradeRoom.getMoney(chr));
                tradeRoom.putMoney(chr, money);
                chr.write(MiniroomPacket.putMoney(0, money));
                other.write(MiniroomPacket.putMoney(1, money));
                break;
            case Trade:
            case TradeConfirm:
            case TradeConfirm2:
            case TradeConfirm3:
                other = tradeRoom.getOtherChar(chr.getId());
                if (other == null) {
                    tradeRoom.cancelTrade();
                    chr.write(MiniroomPacket.cancelTrade());
                    chr.chatMessage("交易對象已斷開連接。");
                    break;
                }
                if (tradeRoom.isTrade()) {
                    if (!tradeRoom.isOpen() || tradeRoom.hasConfirmed(chr)) {
                        break; // multiplayer-0930: a second confirm of the same side does nothing
                    }
                    other.write(MiniroomPacket.tradeConfirm());
                    if (tradeRoom.hasConfirmed(other)) {
                        tradeRoom.addConfirmedPlayer(chr); // both confirmed: completeTrade re-validates everything
                        boolean success = tradeRoom.completeTrade();
                        if (success) {
                            chr.write(MiniroomPacket.tradeComplete());
                            other.write(MiniroomPacket.tradeComplete());
                        } else {
                            tradeRoom.cancelTrade();
                            chr.write(MiniroomPacket.cancelTrade());
                            other.write(MiniroomPacket.cancelTrade());
                        }
                        chr.setTradeRoom(null);
                        other.setTradeRoom(null);
                    } else {
                        tradeRoom.addConfirmedPlayer(chr);
                    }
                } else {
                    other.write(MiniroomPacket.tradeConfirm());
                    if (tradeRoom.hasConfirmed(other)) {
                        boolean success = tradeRoom.canHold();
                        if (success) {
                            chr.write(MiniroomPacket.cancelTrade());
                            other.write(MiniroomPacket.cancelTrade());
                            chr.chatScriptMessage("猜拳將在 1 秒後開始！");
                            other.chatScriptMessage("猜拳將在 1 秒後開始！");
                            chr.getTimer().addEvent(() -> {
                                TradeRoom tradeRoom1 = chr.getTradeRoom();
                                chr.write(MiniroomPacket.enterTrade(tradeRoom1, chr, MiniRoomType.BattleRpsGame));
                                other.write(MiniroomPacket.enterTrade(tradeRoom1, other, MiniRoomType.BattleRpsGame));
                                chr.write(MiniroomPacket.startRPS());
                                other.write(MiniroomPacket.startRPS());
                                chr.getTimer().addEvent(() -> {
                                    byte rpsChr = chr.getTradeRoom().getRPSType(chr);
                                    byte rpsOther = chr.getTradeRoom().getRPSType(other);
                                    if (rpsOther == 3) {
                                        rpsOther = (byte) Util.getRandom(0, 2);
                                    }
                                    if (rpsChr == 3) {
                                        rpsChr = (byte) Util.getRandom(0, 2);
                                    }
                                    byte resultChr = chr.getTradeRoom().getResult(rpsChr, rpsOther);
                                    byte resultOther = chr.getTradeRoom().getResult(rpsOther, rpsChr);
                                    if (resultOther == 2) {
                                        other.addStatAndSendPacket(Stat.pop, -1);
                                    } else if (resultOther == 0) {
                                        other.addStatAndSendPacket(Stat.pop, +1);
                                    }
                                    if (resultChr == 2) {
                                        chr.addStatAndSendPacket(Stat.pop, -1);
                                    } else if (resultOther == 0) {
                                        chr.addStatAndSendPacket(Stat.pop, +1);
                                    }
                                    chr.write(MiniroomPacket.finishRPS(resultChr, rpsOther));
                                    other.write(MiniroomPacket.finishRPS(resultOther, rpsChr));
                                    if (resultChr == 0) {
                                        chr.getTradeRoom().winRPS(chr, other);
                                    } else if (resultOther == 0) {
                                        chr.getTradeRoom().winRPS(other, chr);
                                    } else {
                                        chr.getTradeRoom().cancelTrade();
                                    }
                                    chr.getTimer().addEvent(() -> {
                                        chr.write(MiniroomPacket.cancelTrade());
                                        other.write(MiniroomPacket.cancelTrade());
                                    }, 3000);
                                }, 8000L);
                            }, 100);
                            return;
                        } else {
                            tradeRoom.cancelTrade();
                            chr.write(MiniroomPacket.cancelTrade());
                            other.write(MiniroomPacket.cancelTrade());
                        }
                        chr.setTradeRoom(null);
                        other.setTradeRoom(null);
                    } else {
                        tradeRoom.addConfirmedPlayer(chr);
                    }
                }
                break;
            case Chat0:
            case Chat:
                inPacket.decodeInt(); // tick
                String msg = inPacket.decodeString();
                String msgWithName = String.format("%s : %s", chr.getName(), msg);
                DataPrinter.send(DataPrinter.LOG_TRADE, msgWithName);
                if (tradeRoom != null) {
                    other = tradeRoom.getOtherChar(chr.getId());
                    if (other == null) {
                        tradeRoom.cancelTrade();
                        chr.write(MiniroomPacket.cancelTrade());
                        chr.chatMessage("交易對象已斷開連接。");
                        break;
                    }
                    chr.write(MiniroomPacket.chat(chr, other,1, msg));
                    other.write(MiniroomPacket.chat(chr, other,0, msg));
                    break;
                }
                chr.chatMessage("你當前沒有與任何人交易。");
                break;
            case Accept:
                if (tradeRoom == null) {
                    return;
                }
                other = tradeRoom.getOtherChar(chr.getId()); // initiator
                other.write(MiniroomPacket.accept(chr));
                chr.write(MiniroomPacket.enterTrade(tradeRoom, other, MiniRoomType.TradingRoom));
                break;
            case RPSResult:
                byte rpsType = inPacket.decodeByte();
                if (tradeRoom == null) {
                    return;
                }
                if (rpsType < 0 || rpsType > 2) {
                    return;
                }
                tradeRoom.putRPSType(chr, rpsType);
                break;
            case TradeInviteRequest:
            case RPSInviteRequest:
                if (mrt == MiniRoomType.TradeInviteRequest && !net.swordie.ms.GameSettings.tradeEnabled()) { // feature.trade
                    chr.chatMessage(net.swordie.ms.GameSettings.TRADE_DISABLED);
                    return;
                }
                int charID = inPacket.decodeInt();
                other = chr.getField().getCharByID(charID);
                if (other == null) {
                    chr.chatMessage("該玩家不存在。");
                    return;
                }
                if (other.equals(chr) || other.getName().equals(chr.getName())) {
                    return;
                }
                if (other.getTradeRoom() != null) {
                    chr.chatMessage("該玩家正在與其他人交易。");
                    return;
                }
                if (net.swordie.ms.client.character.social.tms282.SocialGuards.refuseTradeInvite(chr, other)) { // social: block list
                    return;
                }
                tradeRoom = new TradeRoom(chr, other);
                if (mrt.getVal() == MiniRoomType.TradeInviteRequest.getVal()) {
                    tradeRoom.setTrade(true);
                }
                chr.setTradeRoom(tradeRoom);
                other.setTradeRoom(tradeRoom);
                chr.write(MiniroomPacket.checkSSN2(chr));
                chr.write(MiniroomPacket.enterTrade(chr, MiniRoomType.TradingRoom));
                other.write(MiniroomPacket.tradeInvite(chr, other, MiniRoomType.TradingRoom));
                break;
            case InviteResultStatic: // always decline?
                if (tradeRoom != null) {
                    other = tradeRoom.getOtherChar(chr.getId());
                    if (other != null) {
                        other.chatMessage(String.format("%s 拒絕了你的交易邀請。", chr.getName()));
                        other.setTradeRoom(null);
                    } else {
                        chr.chatMessage("交易對象已斷開連接。");
                    }
                }
                chr.setTradeRoom(null);
                break;
            case ExitTrade:
                if (tradeRoom != null) {
                    tradeRoom.cancelTrade();
                    chr.write(MiniroomPacket.cancelTrade());
                    other = tradeRoom.getOtherChar(chr.getId());
                    if (other != null) {
                        other.write(MiniroomPacket.cancelTrade());
                    }
                }
                if (chr.getVisitingmerchant() != null) {
                    chr.getVisitingmerchant().removeVisitor(chr);
                    chr.setVisitingmerchant(null);
                }
                break;
            case TradeConfirmRemoteResponse:
                // just an ack by the client?
                break;
            default:
                System.out.printf("Unhandled miniroom type %s%n", mrt);
                break;
        }
    }

    public static void putItem(Char chr, Inventory inventory, TradeRoom tradeRoom, int tradeSlot, Item item, short requestQuantity, boolean isCash) {
        if(net.swordie.ms.client.character.tms282.Tms282CubeRuntime.refuseMove(chr,item))return;
        if (tradeRoom == null || !tradeRoom.canChangeOffer()) { // multiplayer-0930
            return;
        }
        Char other = tradeRoom.getOtherChar(chr.getId());
        if (other == null) {
            tradeRoom.cancelTrade();
            chr.write(MiniroomPacket.cancelTrade());
            chr.chatMessage("交易對象已斷開連接。");
            return;
        }
        if (tradeRoom.canAddItem(chr)) {
            //Gửi hết
            if (requestQuantity >= item.getQuantity() || ItemConstants.isThrowingItem(item.getItemId())) {
                chr.write(WvsContext.inventoryOperation(true, false, Remove, (short) item.getBagIndex(), (byte) 0, 0, item));
                item.setInventoryID(0);
                item.setCharID(0);
                if (isCash) {
                    tradeRoom.addCashItem(chr, item.getItemId());
                    item.setCash(false);
                }
                item.saveToSQL();
                net.swordie.ms.world.tms282.audit.AuditLog.itemLose(chr, item, item.getQuantity(), net.swordie.ms.world.tms282.audit.AuditLog.TRADE_OFFER, other.getId()); // audit
                inventory.removeItem(item);
                tradeRoom.addItem(chr, tradeSlot, item);
                chr.write(MiniroomPacket.putItem(0, tradeSlot, item));
                other.write(MiniroomPacket.putItem(1, tradeSlot, item));
            }
            //Gửi số lượng custom
            else {
                Item newItem = item.deepCopy();
                newItem.setInventoryID(0);
                newItem.setCharID(0);
                newItem.setQuantity(requestQuantity);
                if (isCash) {
                    tradeRoom.addCashItem(chr, newItem.getItemId());
                    newItem.setCash(false);
                }
                newItem.saveToSQL();
                chr.consumeItem(item, requestQuantity);
                item.saveToSQL();
                tradeRoom.addItem(chr, tradeSlot, newItem);
                chr.write(MiniroomPacket.putItem(0, tradeSlot, newItem));
                other.write(MiniroomPacket.putItem(1, tradeSlot, newItem));
            }
        }
    }

    @Handler(op = InHeader.USER_GIVE_POPULARITY_REQUEST)
    public static void handleUserGivePopularityRequest(Char chr, InPacket inPacket) {
        int targetChrId = inPacket.decodeInt();
        boolean increase = inPacket.decodeByte() != 0;
        // social records: tms282_pop_record (1 per 24h, 30 days per target); replaces the old cooldown-only body
        net.swordie.ms.client.character.social.tms282.FameService.get().give(chr, targetChrId, increase);
    }

    @Handler(op = InHeader.USER_ENTRUSTED_SHOP_REQUEST)
    public static void handleUserEntrustedShopRequest(Char chr, InPacket inPacket) {
        chr.write(WvsContext.merchantResult());
    }
}
