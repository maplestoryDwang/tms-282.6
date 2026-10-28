package net.swordie.ms.enums;

public enum InviteGroupChairResult {
    AlreadySitOnChair(3),
    TooClose(4),
    Success(4),
    NoRemainingSeats(7),
    UnableToFindGroupChair(8),
    InviteSuccessfully(9),
    PlayerNotFound(10),
    GroupChairInvitedNotFound(11),
    PlayerAlreadySitting(12),
    Declined(12),
    ;

    private final int val;

    InviteGroupChairResult(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
