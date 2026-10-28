package net.swordie.ms.enums;

public enum ChatBlockReasonType {
    FoulLanguage(0),
    Advertising(1),
    Hack(2),
    AccountTrading(3),
    Trading(4),
    PenaltyAlert(5);


    private final int val;

    ChatBlockReasonType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
