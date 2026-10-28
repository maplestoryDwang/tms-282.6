package net.swordie.ms.enums.auctionhouse;

public enum AuctionHistoryType {
    HigherBid_CancelBid(1),
    SuccessfulBid_Collect(2),
    Sold_Collect(3),
    Unsold_Collect(4),
    SuccessfulTradePriceDifference_CancelBid(5),
    HigherBid_CompleteReceived(6),
    SuccessfulBid_CompleteReceived(7),
    Sold_CompleteReceived(8),
    Unsold_CompleteReceived(9),
    SuccessfulTradePriceDifference_CompleteReceived(10),
    Failure_CompleteReceived(11);

    private final int val;

    AuctionHistoryType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
