package net.swordie.ms.enums;

public enum UnionStatType {

    Union_StatPosition0(0),
    Union_StatPosition1(1),
    Union_StatPosition2(2),
    Union_StatPosition3(3),
    Union_StatPosition4(4),
    Union_StatPosition5(5),
    Union_StatPosition6(6),
    Union_StatPosition7(7),
    Union_AbnormalStatusResistance(8),
    Union_BonusExp(9),
    Union_CriticalRate(10),
    Union_BossDamage(11),
    Union_KnockbackResistance(12),
    Union_BuffDuration(13),
    Union_IgnoreDef(14),
    Union_CriticalDamage(15),
    ;
    private final int val;

    UnionStatType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
