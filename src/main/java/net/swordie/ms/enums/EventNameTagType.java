package net.swordie.ms.enums;

import java.util.Arrays;

public enum EventNameTagType {
    RED(0),
    BLUE(1),
    YELLOW(2),
    GREEN(3),
    PURPLE(4);

    private final int val;

    EventNameTagType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static EventNameTagType getNameTagTypeByVal(int val) {
        return Arrays.stream(values()).filter(vut -> vut.getVal() == val).findAny().orElse(null);
    }
}
