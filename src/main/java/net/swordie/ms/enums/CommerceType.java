package net.swordie.ms.enums;

public enum CommerceType {
    Res_MainUpdate(9),
    Res_Enter(10),
    // 11, 12? cấu trúc y như update? nhưng vô test thì lại crash?
    Res_MapReveal(13),
    Res_RegionUpdate(14),
    // 15?
    ;

    private final int val;

    CommerceType(int val) {
        this.val = (byte) val;
    }

    public int getVal() {
        return val;
    }
}
