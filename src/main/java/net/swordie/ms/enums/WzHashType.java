package net.swordie.ms.enums;

import java.util.Arrays;

public enum WzHashType {
    MapleCoin_dll(0),
    MapleCoin_exe(1),
    Character_wz(2),
    Etc_wz(3),
    Item_wz(4),
    Map_wz(5),
    Map2_wz(6),
    Mob_wz(7),
    Mob2_wz(8),
    Skill_wz(9),
    ;
    private final int val;

    WzHashType(int val) {
        this.val = (byte) val;
    }

    public static WzHashType getByVal(int val) {
        return Arrays.stream(values()).filter(rt -> rt.getVal() == val).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
