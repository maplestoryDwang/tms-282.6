package net.swordie.ms.enums.auctionhouse;

public enum AuctionRequestType {
    Success(0),
    HighestBidderNotMatch(1),
    ItemOwnerDifferent(2),
    InventoryFull(3),
    FailedToSellCashItem(4),
    ExceedMesoLimit(5),
    PleaseTryAgain(6),
    UniqueItem(7),
    UnknownError(8),
    ;

    private final int val;

    AuctionRequestType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
