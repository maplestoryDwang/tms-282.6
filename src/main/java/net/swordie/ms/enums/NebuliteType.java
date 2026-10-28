package net.swordie.ms.enums;

import net.swordie.ms.util.Util;

import java.util.Arrays;

public enum NebuliteType {
    Rank_D(0),
    Rank_C(1),
    Rank_B(2),
    Rank_A(3),
    Rank_S(4);

    private final int val;

    NebuliteType(int val) {
        this.val = val;
    }

    public static NebuliteType getByVal(int val) {
        return Util.findWithPred(Arrays.asList(values()), csat -> csat.getVal() == val);
    }

    public int getVal() {
        return val;
    }
}
