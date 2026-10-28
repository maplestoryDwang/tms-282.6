package net.swordie.ms.enums.auctionhouse;

import net.swordie.ms.util.Util;

public enum AuctionState {
    Init(0),
    Selling(1),
    Claimed(2),
    Sold(3),
    Expire(4),
    PriceDifference(5),
    OutbidDone(6),
    BidSuccessDone(7),
    SoldDone(8),
    Done(9),
    ;
    private int val;

    AuctionState(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static AuctionState getByVal(int val) {
        return Util.findWithPred(values(), a -> a.getVal() == val);
    }
}
