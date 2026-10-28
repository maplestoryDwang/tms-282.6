package net.swordie.ms.world.shop;

import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.connection.OutPacket;
import net.swordie.ms.connection.hikariCP.DatabaseManager;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.enums.ItemGrade;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.DataPrinter;
import net.swordie.ms.util.FileTime;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class NpcShopItem {

    private long id;
    private int shopID;
    private Item item;
    private int itemID;
    private long price;
    private int tokenItemID;
    private int tokenPrice;
    private int pointQuestID;
    private int pointPrice;
    private int starCoin;
    private int questExID;
    private String questExKey;
    private int questExValue;
    private int itemPeriod;
    private int levelLimited;
    private int showLevMin;
    private int showLevMax;
    private int questID;
    private FileTime sellStart;
    private FileTime sellEnd;
    private int tabIndex;
    private boolean worldBlock;
    private int potentialGrade;
    private int buyLimit;
    private BuyLimitInfo buyLimitInfo;
    private short quantity;
    private long unitPrice;
    private short maxPerSlot;
    private int discountPerc;

    public static List<NpcShopItem> getNPCShopItemsFromSQLByShopID(int shopID) {
        List<NpcShopItem> npcShopItemList = new ArrayList<>();
        String query = "SELECT * FROM shopitems WHERE shopid = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, shopID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NpcShopItem npcShopItem = new NpcShopItem();
                    npcShopItem.setId(rs.getLong("id"));
                    npcShopItem.setShopID(shopID);
                    var itemID = rs.getInt("itemid");
                    npcShopItem.setItemID(itemID);
                    npcShopItem.setPrice(rs.getLong("price"));
                    npcShopItem.setTokenItemID(rs.getInt("tokenitemid"));
                    npcShopItem.setTokenPrice(rs.getInt("tokenprice"));
                    npcShopItem.setPointQuestID(rs.getInt("pointquestid"));
                    npcShopItem.setPointPrice(rs.getInt("pointprice"));
                    npcShopItem.setStarCoin(rs.getInt("starcoin"));
                    npcShopItem.setQuestExID(rs.getInt("questexid"));
                    npcShopItem.setQuestExKey(rs.getString("questexkey"));
                    npcShopItem.setQuestExValue(rs.getInt("questexvalue"));
                    npcShopItem.setItemPeriod(rs.getInt("itemperiod"));
                    npcShopItem.setLevelLimited(rs.getInt("levellimited"));
                    npcShopItem.setShowLevMin(rs.getInt("showlevmin"));
                    npcShopItem.setShowLevMax(rs.getInt("showlevmax"));
                    npcShopItem.setQuestID(rs.getInt("questid"));
                    npcShopItem.setSellStart(DatabaseManager.getFileTimeFromString(rs.getString("sellstart")));
                    npcShopItem.setSellEnd(DatabaseManager.getFileTimeFromString(rs.getString("sellend")));
                    npcShopItem.setTabIndex(rs.getInt("tabindex"));
                    npcShopItem.setWorldBlock(rs.getByte("worldblock") != 0);
                    npcShopItem.setPotentialGrade(rs.getInt("potentialgrade"));
                    npcShopItem.setBuyLimit(rs.getInt("buylimit"));
                    npcShopItem.setQuantity(rs.getShort("quantity"));
                    npcShopItem.setUnitPrice(rs.getLong("unitprice"));
                    npcShopItem.setMaxPerSlot(rs.getShort("maxperslot"));
                    npcShopItem.setDiscountPerc(rs.getInt("discountperc"));
                    if (ItemData.getItemDeepCopy(itemID) == null) {
                        continue;
                    }
                    npcShopItemList.add(npcShopItem);
                }
            }
        } catch (Exception exception) {
            DataPrinter.send(DataPrinter.EXCEPTION_CAUGHT, exception);
        }
        return npcShopItemList;
    }

    public NpcShopItem() {
        sellStart = FileTime.MIN_TIME();
        sellEnd = FileTime.MAX_TIME();
        maxPerSlot = 1000;
    }

    public NpcShopItem(int itemID, int price, short quantity, long unitPrice, short maxPerSlot, Item item) {
        this.tabIndex = 4;
        this.itemID = itemID;
        this.price = price;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.maxPerSlot = maxPerSlot;
        this.item = item;
        this.sellStart = FileTime.MIN_TIME();
        this.sellEnd = FileTime.MAX_TIME();
    }

    public void encode(OutPacket outPacket, int index) {
        outPacket.encodeInt(getBuyLimit() == 0 ? -1 : getBuyLimit()); // -1 = infinite, 0 = nothing left.
        // NpcShopItem::Decode
        outPacket.encodeInt(index);
        outPacket.encodeInt(getItemID());
        outPacket.encodeInt(getTabIndex());
        outPacket.encodeInt(getBuyLimit() == 0 ? -1 : getBuyLimit());
        outPacket.encodeInt(1440 * getItemPeriod());
        outPacket.encodeFT(FileTime.MIN_TIME()); // 1/1/1601 12:07 AM
        outPacket.encodeInt(0);
        if (getItem() == null) {
            long cost = getDiscountPerc() != 0 ? (long) (getPrice() - (getPrice() * ((getDiscountPerc() / (double) 100)))) : getPrice();
            outPacket.encodeLong(cost); // correct
        } else {
            outPacket.encodeLong(getPrice());
        }
        outPacket.encodeInt(getTokenItemID()); // correct
        outPacket.encodeInt(getTokenPrice()); // correct
        outPacket.encodeInt(getPointQuestID()); // correct
        outPacket.encodeInt(0); // clgt?
        outPacket.encodeInt(getPointPrice()); // correct
        outPacket.encodeByte(false);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        if (getBuyLimitInfo() != null) {
            getBuyLimitInfo().encode(outPacket);
        } else {
            new BuyLimitInfo().encode(outPacket);
        }
        outPacket.encodeInt(getShowLevMin()); // minLv
        outPacket.encodeShort(getShowLevMin()); // minLv
        outPacket.encodeShort(0); // maxLv
        outPacket.encodeByte(false); // bIsDisabled
        outPacket.encodeFT(getSellStart()); // 1/1/1601 12:07 AM
        outPacket.encodeFT(getSellEnd()); // 1/1/2079
        outPacket.encodeInt(0); // ? setting it to >0 will make the item not show up
        outPacket.encodeShort(1);
        outPacket.encodeByte(isWorldBlock());
        outPacket.encodeInt(getQuestExID()); // 503417 : crystal
        outPacket.encodeString(getQuestExKey()); // qState

        outPacket.encodeInt(1); // v263 : 1
        outPacket.encodeInt(0); // v263 : 0
        outPacket.encodeString(""); // v263 : ""

        outPacket.encodeInt(0); // v263 : 0
        outPacket.encodeInt(0); // v263 : 0
        outPacket.encodeString(""); // v263 : ""

        outPacket.encodeInt(getQuestExValue()); // 0
        outPacket.encodeInt(0); // 0
        outPacket.encodeByte(0); // 0
        outPacket.encodeString(""); // "Can be purchased by Lv. 200+ characters,\nafter completing the [Collector's Request]\nHero's Potion quest...,.i.Can be purchased by Lv. 200+ characters,\nafter completing the [Collector's Request]\nHero's Potion quest"

        outPacket.encodeString(""); // v263 : ""

        if (ItemConstants.isRechargable(getItemID())) {
            outPacket.encodeLong(getUnitPrice() <= 1 ? 1 : getUnitPrice());
        } else {
            outPacket.encodeShort(getQuantity()); // 1
        }
        outPacket.encodeShort(getItemID() == 2070000 ? 500 : getMaxPerSlot()); // 300
        outPacket.encodeString(""); // v263 : ""
        outPacket.encodeInt(0); // v263 : 0
        outPacket.encodeString(""); // v263 : ""
        outPacket.encodeInt(0); // v263 : 0
        outPacket.encodeByte(0); // v263 : 0
        // end NpcShopItem::Decode
        outPacket.encodeByte(0); // nếu > 0 => tất cả vật phẩm sẽ chuyển sang tab Recommend
        outPacket.encodeByte(0); // 0
        boolean isQuest = getQuestID() != 0;
        if (isQuest) {
            byte type = 0;
            outPacket.encodeByte(type);
            if (type == 1) {
                outPacket.encodeByte(0);
            }
        }
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeInt(0);
        outPacket.encodeArr(new byte[32]);
        outPacket.encodeByte(getItem() != null);
        if (getItem() != null) {
            getItem().encode(outPacket);
        }
    }

    public int getItemID() {
        return itemID;
    }

    /**
     * Sets the item id of this item.
     *
     * @param itemID The id of this item
     */
    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public long getPrice() {
        return price;
    }

    /**
     * Sets the price of this item, in mesos. If both this and token price are
     * set, the item will not be displayed.
     *
     * @param price The price of this item
     */
    public void setPrice(long price) {
        this.price = price;
    }

    public int getTokenItemID() {
        return tokenItemID;
    }

    /**
     * Sets the token id. Token ids start with 431. Items that aren't tokens won't
     * get their token displayed.
     *
     * @param tokenItemID The id of the token
     */
    public void setTokenItemID(int tokenItemID) {
        this.tokenItemID = tokenItemID;
    }

    public int getTokenPrice() {
        return tokenPrice;
    }

    /**
     * Sets the token price of this item. If both this and mesos price are set,
     * the item will not be displayed.
     *
     * @param tokenPrice The token price of this item.
     */
    public void setTokenPrice(int tokenPrice) {
        this.tokenPrice = tokenPrice;
    }

    public int getPointQuestID() {
        return pointQuestID;
    }

    public void setPointQuestID(int pointQuestID) {
        this.pointQuestID = pointQuestID;
    }

    public int getPointPrice() {
        return pointPrice;
    }

    public void setPointPrice(int pointPrice) {
        this.pointPrice = pointPrice;
    }

    public int getStarCoin() {
        return starCoin;
    }

    public void setStarCoin(int starCoin) {
        this.starCoin = starCoin;
    }

    public int getQuestExID() {
        return questExID;
    }

    public void setQuestExID(int questExID) {
        this.questExID = questExID;
    }

    public String getQuestExKey() {
        return questExKey != null ? questExKey : "";
    }

    public void setQuestExKey(String questExKey) {
        this.questExKey = questExKey;
    }

    public int getQuestExValue() {
        return questExValue;
    }

    public void setQuestExValue(int questExValue) {
        this.questExValue = questExValue;
    }

    public int getItemPeriod() {
        return itemPeriod;
    }

    public void setItemPeriod(int itemPeriod) {
        this.itemPeriod = itemPeriod;
    }

    public int getLevelLimited() {
        return levelLimited;
    }

    public void setLevelLimited(int levelLimited) {
        this.levelLimited = levelLimited;
    }

    public int getShowLevMin() {
        return showLevMin;
    }

    public void setShowLevMin(int showLevMin) {
        this.showLevMin = showLevMin;
    }

    public int getShowLevMax() {
        return showLevMax;
    }

    public void setShowLevMax(int showLevMax) {
        this.showLevMax = showLevMax;
    }

    public int getQuestID() {
        return questID;
    }

    public void setQuestID(int questID) {
        this.questID = questID;
    }

    public FileTime getSellStart() {
        return sellStart;
    }

    public void setSellStart(FileTime sellStart) {
        this.sellStart = sellStart;
    }

    public FileTime getSellEnd() {
        return sellEnd;
    }

    public void setSellEnd(FileTime sellEnd) {
        this.sellEnd = sellEnd;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    /**
     * Sets the tab index of this item.
     *
     * @param tabIndex the tab index of this item.
     */
    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public boolean isWorldBlock() {
        return worldBlock;
    }

    /**
     * Sets whether or not this item should be displayed on this world.
     *
     * @param worldBlock whether or not this item should be displayed on this
     *                   world.
     */
    public void setWorldBlock(boolean worldBlock) {
        this.worldBlock = worldBlock;
    }

    public int getPotentialGrade() {
        return potentialGrade;
    }

    /**
     * Sets the potential grade of this item (see {@link ItemGrade}). Will do
     * nothing if this item is not an equip.
     *
     * @param potentialGrade The potential grade of this item
     */
    public void setPotentialGrade(int potentialGrade) {
        this.potentialGrade = potentialGrade;
    }

    public int getBuyLimit() {
        return buyLimit;
    }

    /**
     * Sets the buy limit of this item.
     *
     * @param buyLimit The buy limit of this item.
     */
    public void setBuyLimit(int buyLimit) {
        this.buyLimit = buyLimit;
    }

    public BuyLimitInfo getBuyLimitInfo() {
        return buyLimitInfo;
    }

    public void setBuyLimitInfo(BuyLimitInfo buyLimitInfo) {
        this.buyLimitInfo = buyLimitInfo;
    }

    public short getQuantity() {
        return quantity;
    }

    /**
     * Sets the quantity this item should be given with.
     *
     * @param quantity The quantity of this item
     */
    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }

    public long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public short getMaxPerSlot() {
        return maxPerSlot;
    }

    /**
     * Sets the maximum amount of items the user can buy of these at once.
     *
     * @param maxPerSlot
     */
    public void setMaxPerSlot(short maxPerSlot) {
        this.maxPerSlot = maxPerSlot;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public int getDiscountPerc() {
        return discountPerc;
    }

    /**
     * Sets the discount percentage of this item, from 0 to 100.
     *
     * @param discountPerc The discount percentage of this item
     */
    public void setDiscountPerc(int discountPerc) {
        this.discountPerc = discountPerc;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getShopID() {
        return shopID;
    }

    public void setShopID(int shopID) {
        this.shopID = shopID;
    }
}
