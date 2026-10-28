package net.swordie.ms.connection.api;

import net.swordie.ms.util.Util;

public enum ApiInHeader {
    REQUEST_TOKEN(100),
    CREATE_ACCOUNT_REQUEST(101),
    REQUEST_TOKEN_X64(102),
    ;

    private final int val;

    ApiInHeader(int val) {
        this.val = val;
    }

    public static ApiInHeader getInHeaderByOp(short op) {
        return Util.findWithPred(values(), header -> header.getVal() == op);
    }

    public int getVal() {
        return val;
    }
}
