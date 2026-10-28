package net.swordie.ms.world.shop;

public class CustomShopItem {
    private final int itemId;
    private final long price;
    private final int currencyType;
    private final long periodMs;
    private final int itemFlag;

    public CustomShopItem(int itemId, long price, int currencyType, long periodMs, int itemFlag) {
        this.itemId = itemId;
        this.price = price;
        this.currencyType = currencyType;
        this.periodMs = periodMs;
        this.itemFlag = itemFlag;
    }

    public int getItemId() {
        return itemId;
    }

    public long getPrice() {
        return price;
    }

    public int getCurrencyType() {
        return currencyType;
    }

    public long getPeriodMs() {
        return periodMs;
    }

    public int getItemFlag() {
        return itemFlag;
    }
}
