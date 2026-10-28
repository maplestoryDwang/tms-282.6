package net.swordie.ms.enums;

public enum CharRenameType {
    Success(0),
    Error(1),
    InvalidRequest(2),
    RequiredCoupon(3),
    NotEnoughMaplePoints(4),
    PleaseTryAgainLater(5),
    InvalidName(6),
    UnavailableName(7),
    StillBeingProcessed(8),
    EnterNewName(9),
    WrongPic(10),
    ;

    private final int val;

    CharRenameType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
