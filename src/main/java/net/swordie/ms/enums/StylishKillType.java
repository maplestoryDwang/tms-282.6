package net.swordie.ms.enums;

public enum StylishKillType {
    MULTI_KILL(0),
    COMBO(1),
    ;

    private final byte val;

    StylishKillType(int val) {
        this.val = (byte) val;
    }

    public byte getVal() {
        return val;
    }
}
