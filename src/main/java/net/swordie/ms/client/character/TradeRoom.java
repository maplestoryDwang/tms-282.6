package net.swordie.ms.client.character;

import net.swordie.ms.Server;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.EquipAttribute;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.container.Tuple;

import java.util.*;

public class TradeRoom {
    private int otherCharID;
    private int chrID;
    private boolean isTrade;
    private Map<Char, Byte> rpsTypes = new HashMap<>();
    private Map<Char, Long> money = new HashMap<>();
    private Set<Char> confirmedPlayers = new HashSet<>();
    private Map<Integer, List<Tuple<Integer, Item>>> offeredItems = new HashMap<>();
    private Map<Integer, List<Integer>> offeredCashItems = new HashMap<>();

    public TradeRoom(Char chr, Char other) {
        this.chrID = chr.getId();
        offeredItems.put(chr.getId(), new ArrayList<>());
        offeredCashItems.put(chr.getId(), new ArrayList<>());
        this.otherCharID = other.getId();
        offeredItems.put(other.getId(), new ArrayList<>());
        offeredCashItems.put(other.getId(), new ArrayList<>());
    }

    public int getOtherCharID() {
        return otherCharID;
    }

    public Char getOtherChar(int charID) {
        int result = (charID == getChrID() ? getOtherCharID() : getChrID());
        return Server.get().getWorld().getCharById(result);
    }

    public boolean canAddItem(Char chr) {
        return getOfferedItems().get(chr.getId()).size() < GameConstants.MAX_TRADE_ITEMS;
    }

    public int getTradeSlot(Char chr) {
        return getOfferedItems().get(chr.getId()).size() + 1;
    }

    public void addItem(Char chr, int pos, Item item) {
        net.swordie.ms.client.character.tms282.Tms282CubeRuntime.requireMovable(chr,item);
        List<Tuple<Integer, Item>> items = getOfferedItems().get(chr.getId());
        Tuple<Integer, Item> entry = new Tuple<>(pos, item);
        items.add(entry);
    }

    public void addCashItem(Char chr, Integer itemID) {
        List<Integer> items = getOfferedCashItems().get(chr.getId());
        items.add(itemID);
    }

    public int getChrID() {
        return chrID;
    }

    private Map<Char, Long> getMoney() {
        return money;
    }

    public long getMoney(Char chr) {
        return getMoney().getOrDefault(chr, 0L);
    }

    public void putMoney(Char chr, long money) {
        getMoney().put(chr, money);
    }

    public Map<Integer, List<Tuple<Integer, Item>>> getOfferedItems() {
        return offeredItems;
    }

    public Map<Integer, List<Integer>> getOfferedCashItems() {
        return offeredCashItems;
    }

    public Set<Char> getConfirmedPlayers() {
        return confirmedPlayers;
    }

    public void addConfirmedPlayer(Char chr) {
        getConfirmedPlayers().add(chr);
    }

    public boolean hasConfirmed(Char other) {
        return getConfirmedPlayers().contains(other);
    }

    // ---- multiplayer-0930: atomic trade ------------------------------------------------------------------------
    // Items and mesos are escrowed when offered (removed from the owner, stored with charId 0). Before this, a
    // completed trade kept its escrow, so a cancel racing with the completion (one side confirming while the other
    // disconnects: Client.disconnect -> cancelTrade outside any lock) refunded the owners AND delivered to the
    // partners, and a zero-quantity item cancelled + refunded in the middle of a delivery. Now complete and cancel
    // both run under ECONOMY_MONITOR -> lower id -> higher id (EconomyLocks), exactly one of them wins through
    // {@link #state}, validation happens before anything moves, and the escrow is emptied by whichever ran.

    public enum State {OPEN, COMPLETED, CANCELLED}

    private volatile State state = State.OPEN;

    public State getState() {
        return state;
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    /** True while items / mesos may still be offered: open and nobody has confirmed yet. */
    public boolean canChangeOffer() {
        return state == State.OPEN && getConfirmedPlayers().isEmpty();
    }

    public boolean completeTrade() {
        Char chr = Server.get().getWorld().getCharById(getChrID());
        Char other = Server.get().getWorld().getCharById(getOtherCharID());
        if (chr == null || other == null) {
            System.out.println("兩個角色之間交易失敗：" + getChrID() + " 和 " + getOtherCharID());
            return false;
        }
        return net.swordie.ms.client.character.tms282.EconomyLocks.call(chr, other, () -> completeLocked(chr, other));
    }

    private boolean completeLocked(Char chr, Char other) {
        if (state != State.OPEN) {
            return false;
        }
        if (chr.getClient() == null || other.getClient() == null) {
            return false; // one side is disconnecting
        }
        if (chr.getField() == null || chr.getField() != other.getField()) {
            chr.chatMessage("交易雙方必須在同一張地圖。");
            other.chatMessage("交易雙方必須在同一張地圖。");
            return false;
        }
        if (!hasConfirmed(chr) || !hasConfirmed(other)) {
            return false;
        }
        net.swordie.ms.world.tms282.audit.AuditContext.mark(chr, net.swordie.ms.world.tms282.audit.AuditLog.TRADE, other.getId()); // audit label
        net.swordie.ms.world.tms282.audit.AuditContext.mark(other, net.swordie.ms.world.tms282.audit.AuditLog.TRADE, chr.getId()); // audit label
        for(var entry:getOfferedItems().get(getChrID()))
            if(net.swordie.ms.client.character.tms282.Tms282CubeRuntime.refuseMove(chr,entry.getRight()))return false;
        for(var entry:getOfferedItems().get(getOtherCharID()))
            if(net.swordie.ms.client.character.tms282.Tms282CubeRuntime.refuseMove(other,entry.getRight()))return false;
        List<Item> toChr = new ArrayList<>();
        List<Item> toOther = new ArrayList<>();
        for (Tuple<Integer, Item> entry : getOfferedItems().get(getOtherCharID())) {
            Item item = entry.getRight();
            if (item == null || (item.getQuantity() <= 0 && !ItemConstants.isThrowingItem(item.getItemId()))) {
                return false;
            }
            toChr.add(item);
        }
        for (Tuple<Integer, Item> entry : getOfferedItems().get(getChrID())) {
            Item item = entry.getRight();
            if (item == null || (item.getQuantity() <= 0 && !ItemConstants.isThrowingItem(item.getItemId()))) {
                return false;
            }
            toOther.add(item);
        }
        // each side must hold what it RECEIVES (was: both sides' items against the partner's bag)
        if (!chr.canHold(toChr)) {
            chr.chatMessage("你的背包空間不足。");
            other.chatMessage(chr.getName() + " 的背包空間不足。");
            return false;
        }
        if (!other.canHold(toOther)) {
            other.chatMessage("你的背包空間不足。");
            chr.chatMessage(other.getName() + " 的背包空間不足。");
            return false;
        }
        long mesoToOther = GameConstants.applyTax(getMoney(chr));
        long mesoToChr = GameConstants.applyTax(getMoney(other));
        if (mesoToChr > 0 && chr.getMoney() + mesoToChr > GameConstants.MAX_MONEY) {
            chr.chatMessage("楓幣超過上限，無法完成交易。");
            other.chatMessage(chr.getName() + " 的楓幣超過上限，無法完成交易。");
            return false;
        }
        if (mesoToOther > 0 && other.getMoney() + mesoToOther > GameConstants.MAX_MONEY) {
            other.chatMessage("楓幣超過上限，無法完成交易。");
            chr.chatMessage(other.getName() + " 的楓幣超過上限，無法完成交易。");
            return false;
        }
        state = State.COMPLETED; // from here on a cancel can no longer refund the escrow
        deliver(chr, other, getChrID());
        if (mesoToOther > 0) {
            other.addMoney(mesoToOther);
            DataPrinter.send(DataPrinter.LOG_TRADE, String.format("角色 %s 收到 %d Meso | 來源: %s", other.getName(), mesoToOther, chr.getName()));
        }
        deliver(other, chr, getOtherCharID());
        if (mesoToChr > 0) {
            chr.addMoney(mesoToChr);
            DataPrinter.send(DataPrinter.LOG_TRADE, String.format("玩家 %s 收到 %d meso | 來源: %s", chr.getName(), mesoToChr, other.getName()));
        }
        clearEscrow();
        return true;
    }

    /** The escrow of {@code fromId} to {@code to}. */
    private void deliver(Char from, Char to, int fromId) {
        for (Tuple<Integer, Item> entry : getOfferedItems().get(fromId)) {
            Item item = entry.getRight();
            if (item.getInvType().equals(InvType.EQUIP)) {
                Equip equip = (Equip) item;
                if (equip.hasAttribute(EquipAttribute.UnTradableAfterTransaction)) {
                    equip.setTradeBlock(true);
                    equip.removeAttribute(EquipAttribute.UnTradableAfterTransaction);
                    equip.addAttribute(EquipAttribute.UnTradable);
                }
            }
            for (Integer itemId : getOfferedCashItems().get(fromId)) {
                if (itemId == item.getItemId()) {
                    item.setCash(true);
                    break;
                }
            }
            to.addItemToInventory(item);
            DataPrinter.send(DataPrinter.LOG_TRADE, String.format("角色 %s 收到物品 %s | 物品ID: %d | 數量: %d | 來源: %s", to.getName(), StringData.getItemStringById(item.getItemId()), item.getItemId(), item.getQuantity(), from.getName()));
        }
    }

    private void clearEscrow() {
        for (List<Tuple<Integer, Item>> items : getOfferedItems().values()) {
            items.clear();
        }
        for (List<Integer> ids : getOfferedCashItems().values()) {
            ids.clear();
        }
        getMoney().clear();
    }

    public boolean canHold() {
        Char chr = Server.get().getWorld().getCharById(getChrID());
        Char other = Server.get().getWorld().getCharById(getOtherCharID());
        if (chr == null || other == null) {
            System.out.println("兩個角色之間交易無法接收物品：" + getChrID() + " 和 " + getOtherCharID());
            return false;
        }
        List<Item> items = new ArrayList<>();
        for (Tuple<Integer, Item> entry : getOfferedItems().get(getOtherCharID())) {
            items.add(entry.getRight());
        }
        for (Tuple<Integer, Item> entry : getOfferedItems().get(getChrID())) {
            items.add(entry.getRight());
        }
        if (!chr.canHold(items) || !other.canHold(items)) {
            chr.chatMessage("雙方至少有一人的背包空間不足，無法接收穫勝物品。");
            other.chatMessage("雙方至少有一人的背包空間不足，無法接收穫勝物品。");
            return false;
        }
        return true;
    }

    public void winRPS(Char winner, Char loser) {
        net.swordie.ms.client.character.tms282.EconomyLocks.run(winner, loser, () -> winRPSLocked(winner, loser));
    }

    private void winRPSLocked(Char winner, Char loser) {
        if (state != State.OPEN) { // multiplayer-0930: a cancel already refunded the escrow
            return;
        }
        state = State.COMPLETED;
        for (Map.Entry<Integer, List<Tuple<Integer, Item>>> items : getOfferedItems().entrySet()) {
            for (Tuple<Integer, Item> entry : items.getValue()) {
                if (entry.getRight().getInvType().equals(InvType.EQUIP)) {
                    Equip equip = (Equip) entry.getRight();
                    if (equip.hasAttribute(EquipAttribute.UnTradableAfterTransaction)) {
                        equip.setTradeBlock(true);
                        equip.removeAttribute(EquipAttribute.UnTradableAfterTransaction);
                        equip.addAttribute(EquipAttribute.UnTradable);
                    }
                }
                Item item = entry.getRight();
                for (Integer itemId : getOfferedCashItems().get(winner.getId())) {
                    if (itemId == item.getItemId()) {
                        item.setCash(true);
                        break;
                    }
                }
                winner.addItemToInventory(item);
                //item.saveToSQL();
                DataPrinter.send(DataPrinter.LOG_RPS, String.format("玩家 %s 贏得猜拳並獲得物品 %s | 物品ID: %d | 數量: %d | 來源: %s", winner.getName(), StringData.getItemStringById(item.getItemId()), item.getItemId(), item.getQuantity(), loser.getName()));
            }
        }
        long meso = 0;
        for (Map.Entry<Char, Long> entry : getMoney().entrySet()) {
            meso += entry.getValue();
        }
        if (GameConstants.applyTax(meso) > 0) {
            winner.addMoney(GameConstants.applyTax(meso));
            DataPrinter.send(DataPrinter.LOG_RPS, String.format("玩家 %s 贏得猜拳並獲得 %d meso | 來源: %s", winner.getName(), GameConstants.applyTax(meso), loser.getName()));
        }
        clearEscrow();
        winner.setTradeRoom(null);
        loser.setTradeRoom(null);
    }

    public void cancelTrade() {
        Char chr = Server.get().getWorld().getCharById(getChrID());
        Char other = Server.get().getWorld().getCharById(getOtherCharID());
        // multiplayer-0930: under ECONOMY_MONITOR -> lower id -> higher id; refunds only an OPEN trade, once
        net.swordie.ms.client.character.tms282.EconomyLocks.run(chr, other, () -> cancelLocked(chr, other));
    }

    private void cancelLocked(Char chr, Char other) {
        if (chr != null && chr.getTradeRoom() == this) {
            chr.setTradeRoom(null);
        }
        if (other != null && other.getTradeRoom() == this) {
            other.setTradeRoom(null);
        }
        if (state != State.OPEN) {
            return;
        }
        state = State.CANCELLED;
        int[] charIds = new int[]{getChrID(), getOtherCharID()};
        for (int charID : charIds) {
            Char receiver = charID == getChrID() ? chr : other;
            if (receiver == null) {
                // cannot happen on the disconnect path (the leaver is still in the world); never drop it silently
                for (Tuple<Integer, Item> entry : getOfferedItems().get(charID)) {
                    Item item = entry.getRight();
                    System.out.printf("[Trade] REFUND LOST character=%d item=%d x%d (escrow charId 0)%n", charID,
                            item.getItemId(), item.getQuantity());
                }
                continue;
            }
            net.swordie.ms.world.tms282.audit.AuditContext.mark(receiver, net.swordie.ms.world.tms282.audit.AuditLog.TRADE_CANCEL, 0); // audit label
            for (Tuple<Integer, Item> entry : getOfferedItems().get(charID)) {
                Item item = entry.getRight();
                for (Integer itemId : getOfferedCashItems().get(charID)) {
                    if (itemId == item.getItemId()) {
                        item.setCash(true);
                        break;
                    }
                }
                receiver.addItemToInventory(item);
            }
            long refund = getMoney(receiver);
            if (refund > 0) {
                receiver.addMoney(refund);
            }
        }
        clearEscrow();
    }

    public boolean isTrade() {
        return isTrade;
    }

    public void setTrade(boolean trade) {
        isTrade = trade;
    }

    public Map<Char, Byte> getRpsTypes() {
        return rpsTypes;
    }

    public void putRPSType(Char chr, byte rpsType) {
        getRpsTypes().put(chr, rpsType);
    }

    public byte getRPSType(Char chr) {
        return getRpsTypes().getOrDefault(chr, (byte) 3);
    }

    public final byte getResult(byte rps1, byte rps2) {
        switch (rps1) {
            case 0:
                if (rps2 == 1)
                    return 2;
                if (rps2 == 2)
                    return 0;
                break;
            case 1:
                if (rps2 == 2)
                    return 2;
                if (rps2 == 0)
                    return 0;
                break;
            case 2:
                if (rps2 == 0)
                    return 2;
                if (rps2 == 1)
                    return 0;
                break;
        }
        return 1;
    }
}
