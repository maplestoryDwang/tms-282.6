package net.swordie.ms.enums;

public enum DemainStigmaType {
    MostThreatingOpponent(0),
    MostBrandedOpponent(1),
    LeastBrandedOpponent(2),
    RandomOpponentOpponent(3);

    private final int val;

    DemainStigmaType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
