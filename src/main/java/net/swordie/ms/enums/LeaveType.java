package net.swordie.ms.enums;

public enum LeaveType {
    NO_ANIMATION(0),
    ANIMATION(1),
    SUMMON(4),
    ATTACK_AFTER_DEAD(5),
    BEHIND(10),
    ASCENT(17),
    ;


    private final byte val;

    LeaveType(int val) {
        this.val = (byte) val;
    }

    public byte getVal() {
        return val;
    }
}
