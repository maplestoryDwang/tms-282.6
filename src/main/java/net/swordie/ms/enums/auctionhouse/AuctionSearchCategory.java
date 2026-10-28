package net.swordie.ms.enums.auctionhouse;

import java.util.Arrays;

public enum AuctionSearchCategory {
    Armor(0),
    Weapons(1),
    Use(2),
    Cash(3),
    Etc(4);

    private final int val;

    AuctionSearchCategory(int val) {
        this.val = val;
    }

    public static AuctionSearchCategory getCategoryByVal(int type) {
        return Arrays.stream(values()).filter(cit -> cit.getVal() == type).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
