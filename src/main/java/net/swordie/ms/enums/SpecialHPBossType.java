package net.swordie.ms.enums;

public enum SpecialHPBossType {
    Null(-1),

    Easy_Horntail(0),
    Normal_Horntail(1),
    Chaos_Horntail(2),

    Normal_PinkBean(3),
    Chaos_PinkBean(4),

    ;
    private final byte val;

    SpecialHPBossType(byte val) {
        this.val = val;
    }

    SpecialHPBossType(int val) {
        this((byte) val);
    }

    public byte getVal() {
        return val;
    }
}
