package net.swordie.ms.enums.reward;

import java.util.Arrays;

public enum RewardItemType {
    Item(1),
    MaplePoint(3),
    Meso(4),
    Exp(5),
    Equip(6),
    ;

    private final byte val;

    RewardItemType(int val) {
        this.val = (byte) val;
    }

    public static RewardItemType getByVal(byte val) {
        return Arrays.stream(values()).filter(rt -> rt.getVal() == val).findAny().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
