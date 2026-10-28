package net.swordie.ms.enums;

import java.util.Arrays;

public enum MiniRoomType {
    PlaceItem(0), // v265.1
    PlaceItem_2(1), // v265.1
    PlaceItem_3(2), // v265.1
    PlaceItem_4(3), // v265.1

    SetMesos(4), // v265.1
    SetMesos_2(5), // v265.1
    SetMesos_3(6), // v265.1
    SetMesos_4(7), // v265.1
    Trade(8), // v265.1
    TradeConfirm(9),
    TradeConfirm2(10),
    TradeConfirm3(11), // 3...?

    TradeConfirmRemoteResponse(16), // what is this even used for
    TradeRestraintItem(17),
    Merchant(18),

    Accept(19), // v265.1

    EnterTrade(21), // v265.1
    TradeInviteRequest(22), // v265.1
    InviteResultStatic(23), // v265.1

    Chat0(25), // v265.1
    Chat(26), // v265.1

    ExitTrade(29), // v265.1

    CheckSSN2(32), // v265.1

    OwnerEnterMerchant(31),
    AddItem1(33),
    AddItem2(34),
    AddItem3(35),
    AddItem4(36),
    BuyItem(37),
    BuyItem1(38),
    BuyItem2(39),
    BuyItem3(40),
    RemoveItem(49),
    OwnerLeaveMerchant(50),
    TidyMerchant(51),
    CloseMerchant(52),
    Update(77),
    Open3(80),

    RPSStart(98),
    RPSResult(100),
    RPSInviteRequest(91), // v265.1
    RPSFinish(115),

    BingoGameMulti_Start(121),
    BingoGameMulti_UserTurn(176),
    BingoGameMulti_UserState(177),
    BingoGameMulti_GameResult(183),
    BingoGameMulti_LineComplete(184),

    OmokGame(1),
    MemoryGame(2),
    BattleRpsGame(3),
    TradingRoom(4),
    PersonalShop(5),
    EntrustedShop(6),
    CashTradingRoom(7),
    Wedding(8),
    EventTradingRoom(9),
    MultiYutGame(10),
    SignRoom(11),
    TenthAnniversaryBoardGame(12),
    BingoGame(13),
    OmokRenewal(14),
    MemoryGame2013(15),
    OneCardGameRoom(16),
    MultiYutGameUseSkill(17),
    UIRunnerMiniGame(18),
    ;

    private final byte val;

    MiniRoomType(int val) {
        this.val = (byte) val;
    }

    public static MiniRoomType getByVal(byte val) {
        return Arrays.stream(values()).filter(mrt -> mrt.getVal() == val).findAny().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
