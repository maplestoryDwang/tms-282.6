package net.swordie.ms.enums;

public enum BossPartyCheckDoneType {
    EntryLimitReached(0),
    CannotUseQueueHere(1),
    PlayerCannotMove(2),
    PartyMemberDisconnected(3),
    LevelOrQuestRequirementsNotMeet(4),
    RequestReloginCookie(5),
    FailedDueToUnknownError(6);

    private final int val;

    BossPartyCheckDoneType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
