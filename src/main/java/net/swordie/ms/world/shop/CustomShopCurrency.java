package net.swordie.ms.world.shop;

public enum CustomShopCurrency {
    NX_PREPAID(1, 0, "樂豆點"),
    MAPLE_POINT(2, 0, "楓點"),
    MESO(3, 0, "楓幣"),
    BOSS_COIN(4, 4009495, "首領怪幣"),
    WITCH_BALL(5, 4033159, "巫女球"),
    STAR_COIN(6, 0, "星語幣"),
    VIP_COIN(7, 4310209, "VIP幣");

    private final int type;
    private final int tokenItemId;
    private final String label;

    CustomShopCurrency(int type, int tokenItemId, String label) {
        this.type = type;
        this.tokenItemId = tokenItemId;
        this.label = label;
    }

    public int getType() {
        return type;
    }

    public int getTokenItemId() {
        return tokenItemId;
    }

    public String getLabel() {
        return label;
    }

    public static CustomShopCurrency getByType(int type) {
        for (CustomShopCurrency c : values()) {
            if (c.type == type) {
                return c;
            }
        }
        return null;
    }
}
