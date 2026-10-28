package net.swordie.ms.enums;

public enum ButterFlyType {
    Add(0),
    Move(1),
    Attack(2),
    Erase(3);

    private final int val;

    ButterFlyType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
