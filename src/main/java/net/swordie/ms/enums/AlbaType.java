package net.swordie.ms.enums;

public enum AlbaType {
    ShowResult(0),
    JustFinishPartTimeJob(1),
    RequestCannotHandled(2),
    CharactersAwaitingDeletion(3),
    FailedToMakeARequest(4);

    private final int val;

    AlbaType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
