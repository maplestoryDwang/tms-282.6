package net.swordie.ms.enums;

import java.util.Arrays;

public enum EvolvingSystemType {
    Req_Add(0),
    Req_Sort(1),
    Req_Start(3),
    Req_CancelAccess(5),

    Res_TryEnter(0),
    Res_CancelAccess(1),
    Res_CoreInventoryOperation(2),
    Res_CoreChangeSlotPositionResult(3),
    Res_ThrowCore(4),
    Res_CoreInvenSort(5),

    Add(0),
    Update_Quantity(1),
    Remove(2);

    private final int val;

    EvolvingSystemType(int val) {
        this.val = val;
    }

    public static EvolvingSystemType getRequestTypeByVal(byte type) {
        return Arrays.stream(values()).filter(cit -> cit.toString().startsWith("Req") && cit.getVal() == type).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }
}
