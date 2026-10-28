package net.swordie.ms.enums;

public enum InGameDirectionAsk {
    NOT(0),
    DELAY(1),
    PATTERN_INPUT_REQUEST(2),
    CAMERA_MOVE_TIME(3);

    private final int val;

    InGameDirectionAsk(int val) {
        this.val = val;
    }

    public static InGameDirectionAsk getByVal(int val) {
        if (val >= 0 && val < values().length) {
            return values()[val];
        }
        return null;
    }

    public int getVal() {
        return val;
    }
}
