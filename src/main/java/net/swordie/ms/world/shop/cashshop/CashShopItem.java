package net.swordie.ms.world.shop.cashshop;

import net.swordie.ms.client.Account;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.PetItem;
import net.swordie.ms.constants.ItemConstants;
import net.swordie.ms.loaders.Etc.Commodity.CommodityInfo;
import net.swordie.ms.loaders.ItemData;
import net.swordie.ms.util.FileTime;

import java.time.LocalDateTime;

public class CashShopItem {

    private int id;
    private int itemID;
    private int stock;
    private CashShopItemFlag shopItemFlag = CashShopItemFlag.None;
    private int idk1;
    private int idk2;
    private int oldPrice;
    private int newPrice;
    private FileTime idkTime1;
    private FileTime saleFromFT;
    private FileTime idkTime3;
    private FileTime saleToFT;
    private int idk3;
    private int bundleQuantity;
    private int availableDays;
    private short buyableWithMaplePoints;
    private short buyableWithCredit;
    private short buyableWithPrepaid;
    private short likable;
    private short meso;
    private short favoritable;
    private int gender;
    private int likes;
    private int requiredLevel;
    private String idk10;
    private int idk11;
    private int idk13;
    private int idk14;
    private String category;
    private int subCategory;
    private int parent;

    public CashShopItem(CommodityInfo ci) {
        id = ci.getSN();
        itemID = ci.getItemId();
        oldPrice = ci.getOriginalPrice();
        newPrice = ci.getPrice();
        availableDays = ci.getPeriod();
        bundleQuantity = ci.getCount();
        // Old:
        idkTime1 = FileTime.currentTime();
        saleFromFT = FileTime.currentTime();
        idkTime3 = FileTime.currentTime();
        saleToFT = FileTime.fromEpochMillis(2524669200000L); // 2 Tháng 1 Năm 2050
        stock = 100;
        buyableWithMaplePoints = 1;
        buyableWithCredit = 1;
        buyableWithPrepaid = 1;
        likable = 1;
        favoritable = 1;
        gender = 2;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getItemID() {
        return itemID;
    }

    public CashShopItem setItemID(int itemID) {
        this.itemID = itemID;
        return this;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public CashShopItemFlag getShopItemFlag() {
        return shopItemFlag;
    }

    public void setShopItemFlag(CashShopItemFlag shopItemFlag) {
        this.shopItemFlag = shopItemFlag;
    }

    public int getIdk1() {
        return idk1;
    }

    public void setIdk1(int idk1) {
        this.idk1 = idk1;
    }

    public int getIdk2() {
        return idk2;
    }

    public void setIdk2(int idk2) {
        this.idk2 = idk2;
    }

    public int getOldPrice() {
        return oldPrice;
    }

    public void setOldPrice(int oldPrice) {
        this.oldPrice = oldPrice;
    }

    public int getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(int newPrice) {
        this.newPrice = newPrice;
    }

    public FileTime getIdkTime1() {
        return idkTime1;
    }

    public void setIdkTime1(FileTime idkTime1) {
        this.idkTime1 = idkTime1;
    }

    public FileTime getSaleFrom() {
        return saleFromFT;
    }

    public void setSaleFrom(FileTime saleFromFT) {
        this.saleFromFT = saleFromFT;
    }

    public FileTime getIdkTime3() {
        return idkTime3;
    }

    public void setIdkTime3(FileTime idkTime3) {
        this.idkTime3 = idkTime3;
    }

    public FileTime getSaleTo() {
        return saleToFT;
    }

    public void setSaleTo(FileTime saleToFT) {
        this.saleToFT = saleToFT;
    }

    public int getIdk3() {
        return idk3;
    }

    public void setIdk3(int idk3) {
        this.idk3 = idk3;
    }

    public int getBundleQuantity() {
        return bundleQuantity;
    }

    public void setBundleQuantity(int bundleQuantity) {
        this.bundleQuantity = bundleQuantity;
    }

    public int getAvailableDays() {
        if (availableDays != 0) {
            return availableDays;
        }
        return ItemConstants.CASH_ITEM_AVAILABLE_DAYS;
    }

    public void setAvailableDays(int availableDays) {
        this.availableDays = availableDays;
    }

    public short getBuyableWithMaplePoints() {
        return buyableWithMaplePoints;
    }

    public void setBuyableWithMaplePoints(short buyableWithMaplePoints) {
        this.buyableWithMaplePoints = buyableWithMaplePoints;
    }

    public short getBuyableWithCredit() {
        return buyableWithCredit;
    }

    public void setBuyableWithCredit(short buyableWithCredit) {
        this.buyableWithCredit = buyableWithCredit;
    }

    public short getBuyableWithPrepaid() {
        return buyableWithPrepaid;
    }

    public void setBuyableWithPrepaid(short buyableWithPrepaid) {
        this.buyableWithPrepaid = buyableWithPrepaid;
    }

    public short getLikable() {
        return likable;
    }

    public void setLikable(short likable) {
        this.likable = likable;
    }

    public short getMeso() {
        return meso;
    }

    public void setMeso(short meso) {
        this.meso = meso;
    }

    public short getFavoritable() {
        return favoritable;
    }

    public void setFavoritable(short favoritable) {
        this.favoritable = favoritable;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public String getIdk10() {
        return idk10;
    }

    public void setIdk10(String idk10) {
        this.idk10 = idk10;
    }

    public int getIdk11() {
        return idk11;
    }

    public void setIdk11(int idk11) {
        this.idk11 = idk11;
    }

    public int getIdk13() {
        return idk13;
    }

    public void setIdk13(int idk13) {
        this.idk13 = idk13;
    }

    public int getIdk14() {
        return idk14;
    }

    public void setIdk14(int idk14) {
        this.idk14 = idk14;
    }

    public int getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(int subCategory) {
        this.subCategory = subCategory;
    }

    public int getParent() {
        return parent;
    }

    public void setParent(int parent) {
        this.parent = parent;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "CashShopItem{" +
                "itemID=" + itemID +
                ", newPrice=" + newPrice +
                ", category='" + category + '\'' +
                '}';
    }

    public CashItemInfo toCashItemInfo(Account account, Char chr) {
        CashItemInfo cashItemInfo = new CashItemInfo();
        cashItemInfo.setAccountID(account.getId());
        cashItemInfo.setTrunkID(account.getTrunk().getId());
        Item item = ItemData.getItemDeepCopy(getItemID());
        if (item == null) return null;
        item.setQuantity((short) (getBundleQuantity() == 0 ? 1 : getBundleQuantity()));
        cashItemInfo.setItem(item);
        cashItemInfo.setCommodityID(getId());
        if (getAvailableDays() > 0) {
            item.setDateExpire(FileTime.fromDate(LocalDateTime.now().plusDays(getAvailableDays())));
            if (item instanceof PetItem petItem) {
                petItem.setDateDead(item.getDateExpire());
            }
        }
        return cashItemInfo;
    }

    private enum CashShopItemFlag {
        None,
        Event,
        New,
        Sale,
        Hot,
        Limited,
        BlackFriday,
        AccountLimited,
        CharLimited
    }

}
