package net.swordie.ms.connection.api;

import net.swordie.ms.util.Util;

public enum ApiOutHeader {
    REQUEST_TOKEN_RESULT(100),
    CREATE_ACCOUNT_RESULT(101),
    REQUEST_TOKEN_RESULT_X64(102),
    ;

    private final int val;

    ApiOutHeader(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static ApiOutHeader getByVal(int val) {
        return Util.findWithPred(values(), header -> header.getVal() == val);
    }
}
