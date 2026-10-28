package net.swordie.ms.enums.auctionhouse;

import java.util.Arrays;

public enum AuctionHouseType {
    Req_ShowHomeList(0), // v265.1
    Req_Sell(5), // v265.1

    Req_ItemListAgain(6),

    Req_CancelSell(7), // v265.1
    Req_Buy(10), // v265.1
    Req_Buy_Amount(11), // v265.1
    Req_Reclaim_(-1),
    Req_Reclaim(-1),
    Req_Search(-1),
    Req_Show(-1),
    Req_MarketPrice(-1),

    Req_WishListAdd(25), // v265.1
    Req_WishListRemove(26), // v265.1
    Req_ShowSellList(50), // v265.1
    Req_History(52), // v265.1
    Req_WishListShow(54), // v265.1
    Req_Exit(59), // v265.1

    Res_On(0), // v265.1
    Res_Show(15), // v265.1
    Res_MarketPrice(16), // v265.1

    Res_WishlistAdd(25), // v265.1
    Res_WishListRemove(26), // v265.1

    Res_LoadSellingItems(50), // v265.1
    Res_Sell(51), // v265.1
    Res_LoadSoldItems(52), // v265.1
    Res_AddSoldItems(53), // v265.1
    Res_WishListShow(54), // v265.1
    Res_LoadHistory(57), // v265.1
    Res_AddHistory(58), // v265.1
    Res_Buy(56), // v265.1
    ;

    private final int val;

    AuctionHouseType(int val) {
        this.val = val;
    }

    public static AuctionHouseType getRequestTypeByVal(int type) {
        return Arrays.stream(values()).filter(cit -> cit.toString().startsWith("Req") && cit.getVal() == type).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
