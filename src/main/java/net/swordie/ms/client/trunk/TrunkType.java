package net.swordie.ms.client.trunk;

import java.util.Arrays;

public enum TrunkType {
    TrunkReq_Load(0),
    TrunkReq_Save(1),
    TrunkReq_Close(2),
    TrunkReq_CheckSSN2(3),
    TrunkReq_GetItem(4),
    TrunkReq_FindAll(5), // Move all stored items to your character’s inventory.
    TrunkReq_PutItem(6),
    TrunkReq_AutoStore(7), // Automatically move items from your inventory to Storage, if they match items already stored.
    TrunkReq_SortItem(8),
    TrunkReq_Money(9),
    TrunkReq_CloseDialog(10),

    TrunkRes_GetSuccess(11),
    TrunkRes_GetNoMoney(12),
    TrunkRes_GetUnknown(13),
    TruncRes_GetHavingOnlyItem(14),
    TrunkRes_FindAll(15),
    TrunkRes_PutSuccess(16),
    TrunkRes_PutIncorrectRequest(17),
    TrunkRes_SortItem(18),
    TrunkRes_PutNoMoney(19),
    TrunkRes_PutNoSpace(20),
    TrunkRes_PutUnknown(21),
    TrunkRes_AutoStore(22),
    TrunkRes_MoneySuccess(23),
    TrunkRes_MoneyUnknown(24),
    TrunkRes_MoneyExceededMesoLimit(25),
    TrunkRes_MoneyCantStoreAnyMoreMesos(26),
    TrunkRes_TrunkCheckSSN2(27),
    TrunkRes_OpenTrunkDlg(28),
    TrunkRes_TradeBlocked(32),
    TrunkRes_TradeBlocked_NotActive_Account(33),
    TrunkRes_TradeBlocked_Snapshot(34),
    TrunkRes_BlockedBehavior(35),
    TrunkRes_GetItemExpired(36),
    TrunkRes_BlockFunction(37),
    TrunkRes_BlockFunction2(38),
    ;

    private final int val;

    TrunkType(int val) {
        this.val = val;
    }

    public static TrunkType getByVal(byte val) {
        return Arrays.stream(values()).filter(tt -> tt.getVal() == val).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
