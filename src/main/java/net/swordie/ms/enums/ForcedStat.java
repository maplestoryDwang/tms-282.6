package net.swordie.ms.enums;

import java.util.Arrays;

public enum ForcedStat {
    str(0x1),
    dex(0x2),
    inte(0x4),
    luk(0x8),
    pad(0x10),
    pdd(0x20),
    mad(0x40),
    mdd(0x80),
    acc(0x100),
    eva(0x200),
    speed(0x400),
    jump(0x800),
    speedMax(0x1000), // speed
    optOff(0x2000),
    Unk3(0x4000),
    addMHP(0x8000), // maxHP
    speedDec(0x10000), // 20 => speed = 0
    Unk4(0x20000),
    jumpMax(0x40000), // jump
    ;

    private final int val;

    ForcedStat(int val) {
        this.val = val;
    }

    public static ForcedStat getByVal(int stat) {
        return Arrays.stream(values()).filter(s -> s.getVal() == stat).findFirst().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
