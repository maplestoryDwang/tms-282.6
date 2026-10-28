package net.swordie.ms.enums;

public enum EquipBaseStat {

    iStr(0x1, 0), // 0x1
    iDex(0x2, 0), // 0x2
    iInt(0x4, 0), // 0x4
    iLuk(0x8, 0), // 0x8
    iMaxHP(0x10, 0), // 0x10
    iMaxMP(0x20, 0), // 0x20
    iPAD(0x40, 0), // 0x40
    iMAD(0x80, 0), // 0x80
    iPDD(0x100, 0), // 0x100
    iMDD(0x200, 0), // 0x200

    iACC(-1, 0), // removed
    iEVA(-1, 0), // removed

    iCraft(0x400, 0), // 0x400
    iSpeed(0x800, 0), // 0x800
    iJump(0x1000, 0), // 0x1000

    tuc(0x1, 1),
    cuc(0x2, 1),
    attribute(0x4, 1),
    levelUpType(0x8, 1),
    level(0x10, 1),
    exp(0x20, 1),
    durability(0x40, 1),
    iuc(0x80, 1),

    iPvpDamage(-1, 1),

    iReduceReq(0x100, 1),
    specialAttribute(0x200, 1),
    durabilityMax(0x400, 1),
    iIncReq(0x800, 1),
    growthEnchant(0x1000, 1),
    psEnchant(0x2000, 1),
    bdr(0x4000, 1),
    imdr(0x8000, 1),
    damR(0x10000, 1),
    statR(0x20000, 1),
    cuttable(0x40000, 1),
    exGradeOption(0x80000, 1),
    hyperUpgrade(0x100000, 1); // itemState

    private final int val;
    private final int pos;

    EquipBaseStat(int val, int pos) {
        this.val = val;
        this.pos = pos;
    }

    public int getVal() {
        return val;
    }

    public int getPos() {
        return pos;
    }
}
