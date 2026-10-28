package net.swordie.ms.enums;

public enum TrunkSlotIncResultType {
    AlreadyExpanded(-1),
    FailedToExpanded(0),
    Success(1);

    private final int val;

    TrunkSlotIncResultType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
