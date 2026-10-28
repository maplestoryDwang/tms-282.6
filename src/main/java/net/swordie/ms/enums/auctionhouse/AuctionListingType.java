package net.swordie.ms.enums.auctionhouse;

public enum AuctionListingType {
    Success(0),
    PriceProblem(1),
    ItemExpire(2),
    NotEnough_ListingDeposits(3),
    NotEnough_SalesSlots(4),
    ReversePriceCannotExceedBuyoutPrice(5),
    UnknownError(6),
    Failed(7),
    ItemAmountSetWrong(8),
    Failed_Below50Mesos(9),
    ;

    private final int val;

    AuctionListingType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
