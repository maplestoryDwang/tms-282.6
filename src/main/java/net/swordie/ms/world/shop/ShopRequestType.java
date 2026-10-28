package net.swordie.ms.world.shop;

import java.util.Arrays;

public enum ShopRequestType {
    BUY(0),
    SELL(1),
    RECHARGE(2),
    CLOSE(3),
    SELL_ALL(4),
    ;

    private final int val;

    ShopRequestType(int val) {
        this.val = val;
    }

    public static ShopRequestType getByVal(byte type) {
        return Arrays.stream(values()).filter(v -> v.getVal() == type).findFirst().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
