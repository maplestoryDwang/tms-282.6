package net.swordie.ms.enums.auctionhouse;

public enum AuctionCancelType {
    Success(0),
    InProgress(1),
    First5MinutesOfListing(2),
    HasBid_MakingDeal(3),
    OnlySellerCanCancel(4),
    UnknownError(5);

    private final int val;

    AuctionCancelType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
