package net.swordie.ms.enums.auctionhouse;

public enum AuctionBuyType {
    Success(0),
    PriceNotSet(1),
    BuyoutPriceDifferentFromRequestedPrice(2),
    CannotBuyYourListedItem(3),
    NotEnoughMesos(4),
    NotEnoughPurchaseSlots(5),
    AlreadySold_NotExist(6),
    RefundsNotCollected(6),
    UnknownError(7),
    ;

    private final int val;

    AuctionBuyType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
