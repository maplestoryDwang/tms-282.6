package net.swordie.ms.enums;

import java.util.Arrays;

public enum RuneType {
    Destruction(0),
    Thunder(1),
    Giants(2),
    Darkness(3),
    Skill(4),
    Purification(5),
    Contact(6),
    Ignition(7),
    Blessing(8),
    ;

    private final int val;

    RuneType(int val) {
        this.val = val;
    }

    public static RuneType getByVal(int val) {
        return Arrays.stream(values()).filter(rt -> rt.getVal() == val).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
