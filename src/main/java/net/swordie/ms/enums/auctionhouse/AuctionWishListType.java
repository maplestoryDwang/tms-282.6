package net.swordie.ms.enums.auctionhouse;

public enum AuctionWishListType {
    Success(0),
    UnknownError(1),
    ;

    private final int val;

    AuctionWishListType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
