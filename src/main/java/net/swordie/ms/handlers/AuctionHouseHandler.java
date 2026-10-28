package net.swordie.ms.handlers;

import net.swordie.ms.Server;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Equip;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.connection.packet.AuctionHousePacket;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.ChatType;
import net.swordie.ms.enums.InvType;
import net.swordie.ms.enums.auctionhouse.*;
import net.swordie.ms.handlers.header.InHeader;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Util;
import net.swordie.ms.world.shop.auctionhouse.AuctionItem;
import net.swordie.ms.world.shop.auctionhouse.AuctionItemHistory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.swordie.ms.enums.InventoryOperation.Remove;

public class AuctionHouseHandler {

    private static final int AUCTION_WISH_LIST = 991245;

    @Handler(op = InHeader.AUCTION_HOUSE_REQUEST)
    public static void handleActionHouseRequest(Client c, InPacket inPacket) {
        Char chr = c.getChr();
        if (chr == null) return;
        AuctionHouseType type = AuctionHouseType.getRequestTypeByVal(inPacket.decodeByte());
        switch (type) {
            case Req_ShowHomeList: {
                final var now = FileTime.currentTime();
                Set<AuctionItem> auctionItems = new HashSet<>();
                Set<AuctionItem> sellingItems = new HashSet<>();
                Set<AuctionItem> sellingCharItems = new HashSet<>();
                Set<AuctionItem> soldCharItems = new HashSet<>();
                List<AuctionItem> activeWishListItems = new ArrayList<>();
                List<AuctionItem> wishlistItems = new ArrayList<>();
                if (!chr.hasQuest(AUCTION_WISH_LIST)) {
                    chr.createQuestWithQRValue(AUCTION_WISH_LIST, "0=0;1=0;2=0;3=0;4=0;5=0;6=0;7=0;8=0;9=0");
                }
                for (int index = 0; index < 10; index++) {
                    wishlistItems.add(null);
                }
                final var items = new HashSet<>(chr.getWorld().getAuctionItems());
                for (AuctionItem item : items) {
                    if (item.getItem() == null) {
                        continue;
                    }
                    if (item.getState() == AuctionState.Sold) {
                        if (auctionItems.size() < 100) auctionItems.add(item);
                        if (item.getOwnerId() == chr.getId() && soldCharItems.size() < 100) soldCharItems.add(item);
                    }
                    if (item.getState() == AuctionState.Claimed) {
                        if (item.getOwnerId() == chr.getId() && soldCharItems.size() < 100) soldCharItems.add(item);
                    }
                    if (item.getState() == AuctionState.Selling) {
                        if (sellingItems.size() < 100 && now.isBefore(item.getEndDate())) sellingItems.add(item);
                        if (item.getOwnerId() == chr.getId() && sellingCharItems.size() < 30) sellingCharItems.add(item);
                        if (wishlistItems.size() < 10) {
                            for (int index = 0; index < 10; index++) {
                                if (Long.parseLong(chr.getQRValueByKey(AUCTION_WISH_LIST, index + "")) == item.getId()) {
                                    wishlistItems.add(item);
                                }
                            }
                        }
                    }
                }
                for (int i = 0; i < 10; i++) {
                    try {
                        var wish = wishlistItems.get(i);
                        if (wish != null) {
                            activeWishListItems.add(wish);
                            chr.setQRValueByKey(AUCTION_WISH_LIST, i + "", wish.getId() + "");
                        } else {
                            chr.setQRValueByKey(AUCTION_WISH_LIST, i + "", "0");
                        }
                    } catch (Exception e) {
                        chr.setQRValueByKey(AUCTION_WISH_LIST, i + "", "0");
                    }
                }
                c.write(AuctionHousePacket.show(chr, sellingItems));
                c.write(AuctionHousePacket.marketPrice(chr, auctionItems));
                c.write(AuctionHousePacket.loadSellingItems(chr, sellingCharItems));
                c.write(AuctionHousePacket.loadSoldItems(chr, soldCharItems));
                //c.write(AuctionHousePacket.loadHistory(chr, soldCharItems));
                c.write(AuctionHousePacket.wishListShow(chr, activeWishListItems));
                c.write(AuctionHousePacket.on());
                break;
            }
            case Req_Show, Req_MarketPrice: {
                Set<AuctionItem> auctionItems = new HashSet<>();
                inPacket.decodeByte();
                int searchType = inPacket.decodeInt();
                inPacket.decodeInt();
                String nameWithoutSpace = inPacket.decodeString();
                String nameWithSpace = inPacket.decodeString();
                if (searchType == -1) {
                    for (AuctionItem auctionItem : chr.getWorld().getAuctionItems()) {
                        if (auctionItem.getItem() == null) {
                            continue;
                        }
                        Item item = auctionItem.getItem();
                        String name = StringData.getItemStringById(item.getItemId());
                        if (name != null && (name.equalsIgnoreCase(nameWithSpace) || name.replaceAll(" ", "").equalsIgnoreCase(nameWithoutSpace))) {
                            auctionItems.add(auctionItem);
                        }
                    }
                } else {
                    int itemType = inPacket.decodeInt();
                    int itemSemiType = inPacket.decodeInt();
                    int lvMin = inPacket.decodeInt();
                    int lvMax = inPacket.decodeInt();
                    long priceMin = inPacket.decodeLong();
                    long priceMax = inPacket.decodeLong();
                    int potentialType = inPacket.decodeInt();
                    boolean and = (inPacket.decodeByte() == 1);
                    int optionalSearchCount = inPacket.decodeInt();
                    for (int i4 = 0; i4 < optionalSearchCount; i4++) {
                        boolean isStarForce = (inPacket.decodeInt() == 1);
                        int optionType = inPacket.decodeInt();
                        int i5 = inPacket.decodeInt();
                    }
                    if (searchType <= 1) {
                        for (AuctionItem auctionItem : chr.getWorld().getAuctionItems()) {
                            if (auctionItem.getItem() == null) {
                                continue;
                            }
                            Item item = auctionItem.getItem();
                            if (item instanceof Equip equip) {
                                int level = equip.getrLevel();
                                boolean lvLimit = (level >= lvMin && level <= lvMax);
                                boolean priceLimit = (auctionItem.getPrice() >= priceMin && auctionItem.getPrice() <= priceMax);
                                boolean potentialLimit = (potentialType == -1 || (potentialType == 0 && equip.getGrade() == 0) || (potentialType > 0 && equip.getGrade() - 16 == potentialType));
                                boolean typeLimit = typeLimit(searchType, itemType, itemSemiType, equip.getItemId());
                                String name = StringData.getItemStringById(equip.getItemId());
                                    if (typeLimit && lvLimit && priceLimit && potentialLimit && (name.contains(nameWithSpace) || name.contains(nameWithoutSpace) || nameWithoutSpace.isEmpty())
                                            && ((type.getVal() == AuctionHouseType.Req_Show.getVal() && auctionItem.getState().getVal() == AuctionState.Init.getVal())
                                            || (type.getVal() == AuctionHouseType.Req_MarketPrice.getVal() && (auctionItem.getState().getVal() == AuctionState.Sold.getVal() || auctionItem.getState().getVal() == AuctionState.SoldDone.getVal())))) {
                                        auctionItems.add(auctionItem);
                                }
                            }
                        }
                    }
                }
                if (type.getVal() == AuctionHouseType.Req_Show.getVal()) {
                    chr.write(AuctionHousePacket.show(chr, auctionItems));
                } else {
                    chr.write(AuctionHousePacket.marketPrice(chr, auctionItems));
                }
                break;
            }
            case Req_WishListAdd: {
                c.write(AuctionHousePacket.msg(type.getVal(), 201));
                var id = inPacket.decodeLong();
                AuctionItem item = chr.getWorld().getAuctionItems().stream()
                        .filter(a -> a.getItem() != null && a.getId() == id).findAny().orElse(null);
                if (item != null) {
                    for (int index = 0; index < 10; index++) {
                        if (chr.getQRValueByKey(AUCTION_WISH_LIST, index + "").equals("" + id)) {
                            break;
                        }
                        if (chr.getQRValueByKey(AUCTION_WISH_LIST, index + "").equals("0")) {
                            chr.setQRValueByKey(AUCTION_WISH_LIST, index + "", id + "");
                            chr.write(AuctionHousePacket.addWishList(chr, item));
                            break;
                        }
                    }
                }
                c.write(AuctionHousePacket.msg(type.getVal(), 0));
                break;
            }
            case Req_WishListRemove: {
                c.write(AuctionHousePacket.msg(type.getVal(), 201));
                var id = inPacket.decodeLong();
                Set<AuctionItem> auctionItems = chr.getWorld().getAuctionItems();
                AuctionItem auctionItem = auctionItems.stream()
                        .filter(a -> a.getItem() != null && a.getId() == id).findAny().orElse(null);
                if (auctionItem != null) {
                    for (int index = 0; index < 10; index++) {
                        String added = chr.getQRValueByKey(AUCTION_WISH_LIST, String.valueOf(index));
                        if (added != null && added.equals("" + id)) {
                            chr.setQRValueByKey(AUCTION_WISH_LIST, index, 0);
                            chr.write(AuctionHousePacket.removeWishList(chr, auctionItem));
                            break;
                        }
                    }
                }
                c.write(AuctionHousePacket.msg(type.getVal(), 0));
                break;
            }
            case Req_WishListShow: {
                Set<Long> wishListSet = new HashSet<>();
                List<AuctionItem> wishListItems = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    String wish = chr.getQRValueByKey(AUCTION_WISH_LIST, i + "");
                    if (wish != null && !wish.equals("0")) {
                        wishListSet.add(Long.parseLong(wish));
                    }
                }
                Set<AuctionItem> items = chr.getWorld().getAuctionItems();
                for (Long i : wishListSet) {
                    items.stream().filter(a -> a.getItem() != null && a.getId() == i).findAny().ifPresent(wishListItems::add);
                }
                chr.write(AuctionHousePacket.wishListShow(chr, wishListItems));
                break;
            }
            case Req_Sell: {
                var invType = inPacket.decodeByte();
                var uPos = inPacket.decodeInt(); // 18
                var itemID = inPacket.decodeInt();
                var quantity = inPacket.decodeInt();
                var sellPrice = inPacket.decodeLong();
                var timeLeft = inPacket.decodeInt();
                Inventory inventory;
                var inventoryType = InvType.getInvTypeByVal(invType);
                if (inventoryType != null) {
                    inventory = chr.getInventoryByType(inventoryType);
                } else {
                    chr.chatPopup("未知錯誤\r\n找不到適合該道具的背包。");
                    return;
                }
                if (inventory == null) {
                    chr.chatPopup("未知錯誤\r\n找不到道具。");
                    return;
                }
                Item item = inventory.getItemBySlot(uPos);
                if (item == null || item.getItemId() != itemID) {
                    chr.chatPopup("未知錯誤\r\n找不到道具。");
                    return;
                }
                if (sellPrice < 1000) {
                    chr.chatPopup("不能以低於 1,000 楓幣的價格上架。");
                    return;
                }
                if (sellPrice > 999999999999L) {
                    chr.chatPopup("單個道具的上架價格不能超過 100,000,000,000 楓幣 (1,000B)。");
                    return;
                }
                if (item.getQuantity() < quantity) {
                    chr.chatPopup("出售數量不能超過你當前擁有的道具數量。");
                    return;
                }
                if (item.getInvType().getVal() != 1) {
                    chr.chatPopup("拍賣行只能出售裝備類道具。");
                    return;
                }
                if (chr.getMoney() < 2000) {
                    chr.chatPopup("你的楓幣不足，至少需要 2,000 楓幣才能上架道具。");
                    return;
                }
                Set<AuctionItem> auctionItems = chr.getWorld().getAuctionItems().stream().filter(a -> a.getOwnerId() == chr.getId()).collect(Collectors.toSet());
                if (auctionItems.size() >= 30) {
                    chr.chatPopup("你不能再上架更多道具了。");
                    return;
                }
                sell(c, chr, item, inventory, quantity, sellPrice, timeLeft);
                break;
            }
            case Req_CancelSell: {
                var id = inPacket.decodeLong();
                var auctionItem = chr.getWorld().getAuctionItems().stream()
                        .filter(a -> a.getOwnerId() == chr.getId() && a.getId() == id).findFirst().orElse(null);
                if (auctionItem == null) {
                    chr.chatPopup("未知錯誤\r\n找不到該道具的數據。");
                    return;
                }
                Inventory inventory = chr.getInventoryByType(auctionItem.getItem().getInvType());
                if (inventory == null) {
                    chr.chatPopup("未知錯誤\r\n找不到適合該道具的背包。");
                    return;
                }
                if (System.currentTimeMillis() - auctionItem.getRegDate().toMillis() < (5 * 60 * 1000)) {
                    chr.chatPopup("上架後前 5 分鐘內不能下架該道具。");
                    return;
                }
                if (chr.getId() != auctionItem.getOwnerId()) {
                    chr.chatPopup("你不是該道具的主人。");
                    return;
                }
                if (auctionItem.getState().getVal() == AuctionState.Sold.getVal()) {
                    chr.chatPopup("該道具已經下架。");
                    return;
                }
                if (!chr.canHold(auctionItem.getItem().getItemId())) {
                    chr.chatPopup("背包已滿，無法下架該道具。");
                    return;
                }
                cancelSell(c, chr, auctionItem);
                break;
            }
            case Req_Buy: {
                var id = inPacket.decodeLong();
                inPacket.decodeInt(); // world ID
                var itemID = inPacket.decodeInt();
                var quantity = inPacket.decodeInt();
                var sellPrice = inPacket.decodeLong();
                inPacket.decodeInt();
                Set<AuctionItem> auctionItems = chr.getWorld().getAuctionItems();
                AuctionItem auctionItem = auctionItems.stream()
                        .filter(a -> a.getItem() != null && a.getId() == id)
                        .findAny().orElse(null);
                if (auctionItem == null) {
                    chr.chatPopup("未知錯誤\r\n找不到該道具的數據。");
                    return;
                }
                if (chr.getMoney() < sellPrice) {
                    chr.chatPopup("你的楓幣不足，無法購買該道具。");
                    return;
                }
                if (auctionItem.getDirectPrice() > sellPrice) {
                    chr.chatPopup("購買價格不能低於當前上架價格。");
                    return;
                }
                if (chr.getId() == auctionItem.getOwnerId()) {
                    chr.chatPopup("不能購買自己上架的道具。");
                    return;
                }
                if (!FileTime.currentTime().isBefore(auctionItem.getEndDate())) {
                    chr.chatPopup("該道具的上架時間已過期。");
                    return;
                }
                if (!chr.canHold(auctionItem.getItem().getItemId())) {
                    chr.chatPopup("背包已滿，無法購買該道具。");
                    return;
                }
                buy(c, chr, auctionItem, sellPrice, quantity);
                break;
            }
            case Req_Reclaim_, Req_Reclaim: {
                final long dwInventoryId = inPacket.decodeLong();
                final long id = inPacket.decodeInt();
                final int charID = inPacket.decodeInt();
                final int accID = inPacket.decodeInt();
                final int itemID = inPacket.decodeInt();
                final int historyType = inPacket.decodeInt();
                final long sellPrice = inPacket.decodeLong();
                final long buyTime = inPacket.decodeLong(); // ?
                final long depositFee = inPacket.decodeLong();
                final int quantity = inPacket.decodeInt();
                final int worldID = inPacket.decodeInt();
                Set<AuctionItemHistory> histories = chr.getWorld().getAuctionItemHistories();
                AuctionItemHistory auctionItemHistory = null;
                for (AuctionItemHistory history : histories) {
                    Item item = history.getItem();
                    if (history.getAuctionId() == id && history.getHistoryType() == historyType && item != null && item.getItemId() == itemID) {
                        auctionItemHistory = history;
                        break;
                    }
                }
                if (auctionItemHistory == null) {
                    System.out.println("Unknown history ItemID: " + itemID);
                    return;
                }
                reclaim(c, chr, auctionItemHistory, historyType, sellPrice, depositFee, quantity);
                break;
            }
            case Req_ShowSellList: {
                Set<AuctionItem> sells = chr.getWorld().getAuctionItems().stream()
                        .filter(a -> a.getOwnerId() == chr.getId() && a.getState().getVal() == AuctionState.Selling.getVal()).collect(Collectors.toSet());
                c.write(AuctionHousePacket.loadSellingItems(chr, sells));
                break;
            }
            case Req_History: {
                Set<AuctionItem> histories = chr.getWorld().getAuctionItems().stream()
                        .filter(a -> (a.getOwnerId() == chr.getId() || a.getBidCharID() == chr.getId())
                                && (a.getState().getVal() == AuctionState.Sold.getVal()
                                || a.getState().getVal() == AuctionState.Claimed.getVal())
                        ).collect(Collectors.toSet());
                c.write(AuctionHousePacket.loadSoldItems(chr, histories));
                break;
            }
            case Req_Exit:
                c.auctionHouseOut(chr);
                break;
            default:
                break;
        }
    }

    public static void sell(Client c, Char chr, Item item, Inventory inventory, int quantity, long sellPrice, int timeLeft) {
        if(net.swordie.ms.client.character.tms282.Tms282CubeRuntime.refuseMove(chr,item))return;
        c.write(AuctionHousePacket.msg(5, 201));
        final var now = FileTime.currentTime();
        long expireTime = now.toMillis() + ((long) timeLeft * 60 * 60 * 1000);
        Set<AuctionItem> sellingItems = new HashSet<>();
        for (AuctionItem a : chr.getWorld().getAuctionItems()) {
            if (a.getItem() == null) {
                continue;
            }
            if (a.getState() == AuctionState.Selling && sellingItems.size() < 100 && now.isBefore(a.getEndDate())) {
                sellingItems.add(a);
            }
        }
        if (quantity == item.getQuantity()) {
            chr.write(WvsContext.inventoryOperation(true, false, Remove, (short) item.getBagIndex(), (byte) 0, 0, item));
            item.setInventoryID(0);
            item.setCharID(0);
            item.setAuctionHouseStatus(Item.AuctionHouseStatus.SELL);
            item.updatePositionInSQL();
            AuctionItem auctionItem = new AuctionItem(item, chr.getUser().getId(), chr.getId(), chr.getName(),
                    sellPrice, 0, 0,
                    FileTime.MAX_TIME(),//FileTime.fromLong(expireTime),
                    FileTime.currentTime());
            auctionItem.setState(AuctionState.Selling);
            auctionItem.saveToSQL();
            chr.getWorld().getAuctionItems().add(auctionItem);
            inventory.removeItem(item);
            c.write(AuctionHousePacket.sell(chr, auctionItem));
            c.write(AuctionHousePacket.msg(5, 0));
            sellingItems.add(auctionItem);
        }
        else if (quantity < item.getQuantity()) {
            Item newItem = item.deepCopy();
            newItem.setInventoryID(0);
            newItem.setCharID(0);
            newItem.setQuantity(quantity);
            newItem.setAuctionHouseStatus(Item.AuctionHouseStatus.SELL);
            newItem.saveToSQL();
            chr.consumeItem(item, quantity);
            item.saveToSQL();
            AuctionItem auctionItem = new AuctionItem(newItem, chr.getUser().getId(), chr.getId(), chr.getName(), sellPrice, 0, 0, FileTime.fromLong(expireTime), FileTime.currentTime());
            auctionItem.setState(AuctionState.Selling);
            auctionItem.saveToSQL();
            chr.getWorld().getAuctionItems().add(auctionItem);
            c.write(AuctionHousePacket.sell(chr, auctionItem));
            sellingItems.add(auctionItem);
        }
        c.write(AuctionHousePacket.show(chr, sellingItems));
        chr.deductMoney(2000);
        DataPrinter.send(DataPrinter.AUCTION, String.format("[出售] %s 上架物品 %s | ID: %d | 物品ID: %d | 數量: %d | 背包類型: %d | 背包位置: %d。",
                chr.getName(),
                StringData.getItemStringById(item.getItemId()),
                item.getId(),
                item.getItemId(),
                quantity,
                item.getInvType().getVal(),
                item.getBagIndex()));
        DataPrinter.send(DataPrinter.AUCTION, String.format("**[拍賣行]** %s 正在出售物品 **%s** x %d，價格 **%s**。",
                chr.getName(),
                StringData.getItemStringById(item.getItemId()),
                quantity,
                Util.getNumberFormat(sellPrice * quantity)));
        DataPrinter.send(DataPrinter.AUCTION, String.format("**[Auction House]** %s is selling item **%s** x %d with price **%s**.",
                chr.getName(),
                StringData.getItemStringById(item.getItemId()),
                quantity,
                Util.getNumberFormat(sellPrice * quantity)));
        c.write(AuctionHousePacket.msg(5, 0));
    }

    public static void cancelSell(Client c, Char chr, AuctionItem item) {
        c.write(AuctionHousePacket.msg(7, 201));
        if (item.getId() != 0) {
            Item copy = item.getItem().deepCopy();
            chr.addItemToInventory(copy);
            copy.setCharID(chr.getId());
            copy.setInventoryID(item.getItem().getInventoryID());
            copy.setAuctionHouseStatus(Item.AuctionHouseStatus.NORMAL);
            copy.saveToSQL();
            item.getItem().deleteFromSQL();
            item.deleteFromSQL();
            c.write(AuctionHousePacket.addSoldItems(chr, item));
            chr.getWorld().getAuctionItems().remove(item);
            Set<AuctionItem> sellingItems = new HashSet<>();
            final var now = FileTime.currentTime();
            for (AuctionItem a : chr.getWorld().getAuctionItems()) {
                if (a.getItem() == null) {
                    continue;
                }
                if (a.getState() == AuctionState.Selling && sellingItems.size() < 100 && now.isBefore(a.getEndDate())) {
                    sellingItems.add(a);
                }
            }
            Server.get().broadcastForAH(AuctionHousePacket.show(chr, chr.getWorld().getAuctionItems()));
        }
        c.write(AuctionHousePacket.msg(7, 0));
        chr.chatPopup("道具已自動放入你的背包。");
        DataPrinter.send(DataPrinter.AUCTION, String.format("[出售] %s 取消出售物品 %s | ID: %d | 物品ID: %d | 數量: %d | 背包類型: %d | 背包位置: %d。",
                chr.getName(),
                StringData.getItemStringById(item.getItem().getItemId()),
                item.getItem().getId(),
                item.getItem().getItemId(),
                item.getItem().getQuantity(),
                item.getItem().getInvType().getVal(),
                item.getItem().getBagIndex()));
    }

    public static void buy(Client c, Char chr, AuctionItem auctionItem, long sellPrice, int quantity) {
        c.write(AuctionHousePacket.msg(11, 201));
        chr.deductMoney(sellPrice);
        Char owner = c.getWorld().getCharById(auctionItem.getOwnerId());
        if (owner == null) {
            owner = Char.getCharDataByID(auctionItem.getOwnerId());
            owner.addMoney(sellPrice);
            owner.saveToSQL();
        } else {
            owner.addMoney(sellPrice);
            owner.chatMessage(ChatType.GameDesc, "你通過拍賣行出售道具獲得了 " + Util.getNumberFormat(sellPrice) + " 楓幣。可在拍賣場 NPC 或快速移動查看詳情。");
        }
        if (auctionItem.getItem().getQuantity() <= quantity) {
            Item copy = auctionItem.getItem().deepCopy();
            chr.addItemToInventory(copy);
            copy.setCharID(chr.getId());
            copy.setInventoryID(auctionItem.getItem().getInventoryID());
            copy.setAuctionHouseStatus(Item.AuctionHouseStatus.NORMAL);
            copy.saveToSQL();
            auctionItem.setBidUserID(chr.getUser().getId());
            auctionItem.setBidCharID(chr.getId());
            auctionItem.setState(AuctionState.Sold);
            auctionItem.setBidDate(FileTime.currentTime());
            auctionItem.saveToSQL();
        } else {
            Item copy = auctionItem.getItem().deepCopy();
            copy.setQuantity(quantity);
            chr.addItemToInventory(copy);
            copy.setCharID(chr.getId());
            copy.setInventoryID(auctionItem.getItem().getInventoryID());
            copy.setAuctionHouseStatus(Item.AuctionHouseStatus.NORMAL);
            copy.saveToSQL();
            auctionItem.getItem().setQuantity(auctionItem.getItem().getQuantity() - quantity);
            auctionItem.getItem().saveToSQL();
            auctionItem.saveToSQL();
        }
        c.write(AuctionHousePacket.addSoldItems(chr, auctionItem));
        c.write(AuctionHousePacket.buy(chr, auctionItem));
        chr.chatPopup("道具已自動放入你的背包。");
        Server.get().broadcastForAH(AuctionHousePacket.show(chr, chr.getWorld().getAuctionItems()));
        DataPrinter.send(DataPrinter.AUCTION, String.format("[購買] %s 從 %s 購買物品 %s | ID: %d | 物品ID: %d | 數量: %d | 背包類型: %d | 背包位置: %d。",
                chr.getName(),
                auctionItem.getOwnerId(),
                StringData.getItemStringById(auctionItem.getItem().getItemId()),
                auctionItem.getItem().getId(), auctionItem.getItem().getItemId(),
                auctionItem.getItem().getQuantity(),
                auctionItem.getItem().getInvType().getVal(),
                auctionItem.getItem().getBagIndex()));
        c.write(AuctionHousePacket.msg(11, 0));
    }

    public static void reclaim(Client c, Char chr, AuctionItemHistory auctionItemHistory, int historyType, long sellPrice, long depositFee, int quantity) {
        if (historyType != AuctionHistoryType.Sold_Collect.getVal()) {
            if (auctionItemHistory.getItem() != null) {
                InvType invType = auctionItemHistory.getItem().getInvType();
                if (chr.getInventoryByType(invType).isFull()) {
                    return;
                }
            }
        }
        auctionItemHistory.getItem().setAuctionHouseStatus(Item.AuctionHouseStatus.CLAIMED);
        Item item = auctionItemHistory.getItem().deepCopy();
        item.setQuantity(quantity);
        item.setAuctionHouseStatus(Item.AuctionHouseStatus.NORMAL);
        if (depositFee == 0 && historyType == AuctionHistoryType.SuccessfulBid_Collect.getVal()) {
            chr.addItemToInventory(item);
            auctionItemHistory.setHistoryType(AuctionHistoryType.SuccessfulBid_CompleteReceived.getVal());
            DataPrinter.send(DataPrinter.AUCTION, String.format("[購買] %s 成功領取物品 %s | ID: %d | 物品ID: %d | 數量: %d | 背包類型: %d | 新ID: %d。",
                    chr.getName(),
                    StringData.getItemStringById(auctionItemHistory.getItem().getItemId()),
                    auctionItemHistory.getItem().getId(),
                    auctionItemHistory.getItem().getItemId(),
                    auctionItemHistory.getItem().getQuantity(),
                    auctionItemHistory.getItem().getInvType().getVal(),
                    item.getId()));
        } else if (depositFee != 0) {
            if (historyType == AuctionHistoryType.Sold_Collect.getVal()) {
                long money = (long) (sellPrice * 0.95D + depositFee);
                chr.addMoney(money);
                auctionItemHistory.setHistoryType(AuctionHistoryType.Sold_CompleteReceived.getVal());
                DataPrinter.send(DataPrinter.AUCTION, String.format("[出售] %s 從出售物品 %s 獲得 %d meso | ID: %d | 物品ID: %d | 數量: %d | 背包類型: %d | 新ID: %d。",
                        chr.getName(),
                        StringData.getItemStringById(auctionItemHistory.getItem().getItemId()),
                        money,
                        auctionItemHistory.getItem().getId(),
                        auctionItemHistory.getItem().getItemId(),
                        auctionItemHistory.getItem().getQuantity(),
                        auctionItemHistory.getItem().getInvType().getVal(),
                        item.getId()));
            } else if (historyType == AuctionHistoryType.Unsold_Collect.getVal()) {
                chr.addItemToInventory(item);
                chr.addMoney(depositFee);
                auctionItemHistory.setHistoryType(AuctionHistoryType.Unsold_CompleteReceived.getVal());
                DataPrinter.send(DataPrinter.AUCTION, String.format("[出售] %s 領取未售出的物品 %s | ID: %d | 物品ID: %d | 數量: %d | 背包類型: %d | 新ID: %d。", chr.getName(), StringData.getItemStringById(auctionItemHistory.getItem().getItemId()), auctionItemHistory.getItem().getId(), auctionItemHistory.getItem().getItemId(), auctionItemHistory.getItem().getQuantity(), auctionItemHistory.getItem().getInvType().getVal(), item.getId()));
            }
        }
        auctionItemHistory.saveToSQL();
        Set<AuctionItemHistory> auctionItemHistories = chr.getWorld().getAuctionItemHistories().stream().filter(a -> a.getOwnerId() == chr.getId()).collect(Collectors.toSet());
        //c.write(AuctionHousePacket.history(chr, auctionItemHistories));
    }

    private static boolean typeLimit(int searchType, int itemType, int itemSemiType, int itemId) {
        switch (searchType) {
            case 0:
                switch (itemType) {
                    case 0:
                        return !ItemConstants.isWeapon(itemId);
                    case 1:
                        switch (itemSemiType) {
                            case 0:
                                return (ItemConstants.isWeapon(itemId) || ItemConstants.isAccessory(itemId));
                            case 1:
                                return (itemId / 1000 == 100);
                            case 2:
                                return (itemId / 1000 == 104);
                            case 3:
                                return (itemId / 1000 == 105);
                            case 4:
                                return (itemId / 1000 == 106);
                            case 5:
                                return (itemId / 1000 == 107);
                            case 6:
                                return (itemId / 1000 == 108);
                            case 7:
                                return (itemId / 1000 == 109);
                            case 8:
                                return (itemId / 1000 == 110);
                            }
                        break;
                    case 2:
                        switch (itemSemiType) {
                            case 0:
                                return !ItemConstants.isAccessory(itemId);
                            case 1:
                                return (itemId / 1000 == 1012);
                            case 2:
                                return (itemId / 1000 == 1022);
                            case 3:
                                return (itemId / 1000 == 1032);
                            case 4:
                                return ItemConstants.isRing(itemId);
                            case 5:
                                return (itemId / 1000 == 1122 || itemId / 1000 == 1123);
                            case 6:
                                return (itemId / 1000 == 1132);
                            case 7:
                                return ItemConstants.isMedal(itemId);
                            case 8:
                                return (itemId / 1000 == 1152);
                            case 9:
                                return (itemId / 1000 == 1162);
                            case 10:
                                return (itemId / 1000 == 1182);
                            }
                        break;
                    case 3:
                        switch (itemSemiType) {
                            case 0:
                                return (itemId / 1000 >= 1612 && itemId / 1000 <= 1652);
                            case 1:
                                return (itemId / 1000 == 1662);
                            case 2:
                                return (itemId / 1000 == 1672);
                            case 3:
                                return (itemId / 1000 >= 1942 && itemId / 1000 <= 1972);
                            }
                        break;
                    }
                    case 1:
                switch (itemType) {
                    case 0:
                        return ItemConstants.isWeapon(itemId);
                    case 1:
                        switch (itemSemiType) {
                            case 0:
                                return !ItemConstants.isTwoHanded(itemId);
                            case 1:
                                return (itemId / 1000 == 1212);
                            case 2:
                                return (itemId / 1000 == 1222);
                            case 3:
                                return (itemId / 1000 == 1232);
                            case 4:
                                return (itemId / 1000 == 1242);
                            case 5:
                                return (itemId / 1000 == 1302);
                            case 6:
                                return (itemId / 1000 == 1312);
                            case 7:
                                return (itemId / 1000 == 1322);
                            case 8:
                                return (itemId / 1000 == 1332);
                            case 9:
                                return (itemId / 1000 == 1342);
                            case 10:
                                return (itemId / 1000 == 1362);
                            case 11:
                                return (itemId / 1000 == 1372);
                            case 12:
                                return (itemId / 1000 == 1262);
                            case 13:
                                return (itemId / 1000 == 1272);
                            case 14:
                                return (itemId / 1000 == 1282);
                            case 15:
                                return (itemId / 1000 == 1292);
                            case 16:
                                return (itemId / 1000 == 1213);
                            }
                        break;
                    case 2:
                        switch (itemSemiType) {
                            case 0:
                                return ItemConstants.isTwoHanded(itemId);
                            case 1:
                                return (itemId / 1000 == 1402);
                            case 2:
                                return (itemId / 1000 == 1412);
                            case 3:
                                return (itemId / 1000 == 1422);
                            case 4:
                                return (itemId / 1000 == 1432);
                            case 5:
                                return (itemId / 1000 == 1442);
                            case 6:
                                return (itemId / 1000 == 1452);
                            case 7:
                                return (itemId / 1000 == 1462);
                            case 8:
                                return (itemId / 1000 == 1472);
                            case 9:
                                return (itemId / 1000 == 1482);
                            case 10:
                                return (itemId / 1000 == 1492);
                            case 11:
                                return (itemId / 1000 == 1522);
                            case 12:
                                return (itemId / 1000 == 1532);
                            case 13:
                                return (itemId / 1000 == 1582);
                            case 14:
                                return (itemId / 1000 == 1592);
                            }
                        break;
                    case 3:
                        switch (itemSemiType) {
                            case 0:
                                return (itemId / 1000 == 1352 || itemId / 1000 == 1353);
                            case 1:
                                return (itemId / 10 == 135220);
                            case 2:
                                return (itemId / 10 == 135221);
                            case 3:
                                return (itemId / 10 == 135222);
                            case 4:
                                return (itemId / 10 == 135223 || itemId / 10 == 135224 || itemId / 10 == 135225);
                            case 5:
                                return (itemId / 10 == 135226);
                            case 6:
                                return (itemId / 10 == 135227);
                            case 7:
                                return (itemId / 10 == 135228);
                            case 8:
                                return (itemId / 10 == 135229);
                            case 9:
                                return (itemId / 10 == 135290);
                            case 10:
                                return (itemId / 10 == 135291);
                            case 11:
                                return (itemId / 10 == 135292);
                            case 12:
                                return (itemId / 10 == 135297);
                            case 13:
                                return (itemId / 10 == 135293);
                            case 14:
                                return (itemId / 10 == 135294);
                            case 15:
                                return (itemId / 10 == 135240);
                            case 16:
                                return (itemId / 10 == 135201);
                            case 17:
                                return (itemId / 10 == 135210);
                            case 18:
                                return (itemId / 10 == 135310);
                            case 19:
                                return (itemId / 10 == 135295);
                            case 20:
                                return (itemId / 10 == 135296);
                            case 21:
                                return (itemId / 10 == 135300);
                            case 22:
                                return (itemId / 10 == 135270);
                            case 23:
                                return (itemId / 10 == 135250);
                            case 24:
                                return (itemId / 10 == 135260);
                            case 25:
                                return (itemId / 10 == 135320);
                            case 26:
                                return (itemId / 10 == 135340);
                            case 27:
                                return (itemId / 10 == 135330);
                            case 28:
                                return (itemId / 10 == 135350);
                            case 29:
                                return (itemId / 10 == 135360);
                            case 30:
                                return (itemId / 10 == 135370);
                            case 31:
                                return (itemId / 10 == 135380);
                            case 32:
                                return  (itemId / 10 == 135390);
                            }
                        break;
                    }
                break;
            case 2:
                return (itemId / 1000000 == 2);
            case 3:
                return ItemData.getEquipById(itemId) != null && ItemData.getEquipById(itemId).isCash();
            case 4:
                return (itemId / 1000000 == 4 || itemId / 1000000 == 3);
            }
        return false;
    }
}
