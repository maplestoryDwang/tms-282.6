package net.swordie.ms.enums;

public enum VIPGrade {
    None(0),
    Bronze(1),
    Silver(2),
    Gold(3),
    Diamond(4),
    //Red(5),
    ;

    private final int val;

    VIPGrade(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static VIPGrade getValByNum(int num) {
        for (VIPGrade at : VIPGrade.values()) {
            if (at.getVal() == num) {
                return at;
            }
        }
        return null;
    }
}
