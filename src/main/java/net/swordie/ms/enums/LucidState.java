package net.swordie.ms.enums;

public enum LucidState {
    None(0),
    DoSkill(1);

    private final int val;

    LucidState(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
