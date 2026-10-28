package net.swordie.ms.enums;

import net.swordie.ms.util.Util;

public enum ApiTokenResultType {
    Success(0),
    InvalidUserPassCombination(1),
    TooManyRequest(2),
    WrongVersion(3),
    WrongWZFiles(4),
    Banned(5),
    AlreadyLogin(6),
    ;
    private final int val;

    ApiTokenResultType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static ApiTokenResultType getByVal(int val) {
        return Util.findWithPred(values(), a -> a.getVal() == val);
    }
}
