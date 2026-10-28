package net.swordie.ms.enums;

public enum PartyQuestRankingType {
    Time(0),
    Monsters(1),
    Items(2);

    private final int val;

    PartyQuestRankingType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
