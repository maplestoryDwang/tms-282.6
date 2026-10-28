package net.swordie.ms.enums;

public enum CashErrorType {
    RequestTimedOut(1),
    NotEnoughCash(3),
    CannotGift_Under14(4),
    CannotExceedPriceGift(5),
    CannotGiftToOwnAccount(6),
    CannotFindCharacterGift(7),
    GenderRestriction(8),
    RecipientInventoryFull(9),
    TooManyCashItems(10),
    CheckAgainGift(11),
    CouponNumberWrong(14),
    CouponExpired_CannotBeUsed(16),
    CouponAlreadyUsed(17),
    CouponOnlyBeUsedNexonInternetCafe(18),
    NexonInternetCafeCouponUsed(19),
    NexonInternetCafeCouponExpired(20),
    NXCouponNumber(21),
    PleaseCheckYourInventoryFullOrNot(25),
    SendingGiftToInvalidRecipient(28),
    PleaseCheckNameOfReceiver(29),
    SoldOut(31),
    DueToTechnicalDifficulties_PleaseTryAgain(57),
    PleaseTryAgain(75),
    ThisItemCannotBeReceivedByAnyoneUnder7(78),
    YouCannotMakeAnyMorePurchasesIn(79),
    NXUseRestricted(80),
    ThatItemIsNotBeingSold(84),
    YouHaveTooManyCashItems(91),
    CanOnlyBePurchasedOnceAMonth(95),
    AtLeastOneItemThatCanOnlyBePurchasedWithNX(96),
    PreviousValuePackStillActive(104),
    NotEnoughRewardPoints(105),
    CannotGetBonusItem_CashInventoryFull(107),
    RefundUnvailable(113),
    CannotRefund_7DaysPassed(114),
    ItemNonRefundable(115),
    RefundCannotBeProcessed(116),
    ExceededThePurchaseLimitForThisItem(117),
    // U-OTP errors : 118-122
    ThatItemCannotBeMoved(123),
    ThatItemCannotBeMoved2(124),
    MustBeLvl10orHigherToPurchaseCharacterNameChange(127),
    ItemChanged_PleaseRefresh(132),
    CannotPurchaseByMeso(133),
    PurchaseOnlyForLevel249OrBelow(134),
    MovableOnlyForLevel249OrBelow(135),
    SoldOut2(138),
    ReachedMaximumSlots(142),
    ;

    private final int val;

    CashErrorType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
