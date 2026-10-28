package net.swordie.ms.enums;

public enum InvType {
    EQUIPPED(-1),
    EQUIP(1),
    CONSUME(2),
    INSTALL(3),
    ETC(4),
    CASH(5),
    DECORATION(6);

    private final byte val;

    private static final int MIN_VAL = -1;
    private static final int MAX_VAL = 6;
    private static final InvType[] CACHE;

    InvType(int val) {
        this((byte) val);
    }

    InvType(byte val) {
        this.val = val;
    }

    static {
        CACHE = new InvType[MAX_VAL - MIN_VAL + 1];
        for (InvType t : values()) {
            CACHE[t.val - MIN_VAL] = t;
        }
    }

    public static InvType getInvTypeByVal(int val) {
        if (val < MIN_VAL || val > MAX_VAL) {
            return null;
        }
        return CACHE[val - MIN_VAL];
    }

    public static InvType getInvTypeByString(String subMap) {
        subMap = subMap.toLowerCase();
        InvType res = null;
        switch (subMap) {
            case "cash":
            case "pet":
                res = CASH;
                break;
            case "consume":
            case "special":
            case "use":
                res = CONSUME;
                break;
            case "etc":
                res = ETC;
                break;
            case "install":
            case "setup":
                res = INSTALL;
                break;
            case "eqp":
            case "equip":
                res = EQUIP;
                break;
            case "cash_equip":
                res = DECORATION;
                break;
        }
        return res;
    }

    public byte getVal() {
        return val;
    }

    public boolean isStackable() {
        return this != EQUIP && this != EQUIPPED && this != CASH && this != DECORATION;
    }
}
