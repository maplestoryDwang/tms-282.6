package net.swordie.ms.enums.auctionhouse;

public enum AuctionShowType {
    Success(0),
    SearchRangeTooBroad(1),
    NoItemFound(2);

    private final int val;

    AuctionShowType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
