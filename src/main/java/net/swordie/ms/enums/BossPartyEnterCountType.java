package net.swordie.ms.enums;

public enum BossPartyEnterCountType {
    OnceADay(0, "1 lần mỗi ngày"),
    TwiceAday(1, "2 lần mỗi ngày"),
    ThreeTimesAday(2, "3 lần mỗi ngày"),
    SevenTimesADay(3, "7 lần mỗi ngày"),
    TenTimesADay(4, "10 lần mỗi ngày"),
    OnceInSevendays(5, "1 lần mỗi 7 ngày"),
    TwiceInSevendays(6, "2 lần mỗi 7 ngày"),
    OnceInTwodays(7, "1 lần mỗi 2 ngày"),
    OnceInThreedays(8, "1 lần mỗi 3 ngày"),
    OnceInAMonth(9, "1 lần mỗi tháng"),
    ;

    private final int val;
    private final String name;

    BossPartyEnterCountType(int val, String name) {
        this.val = val;
        this.name = name;
    }

    public int getVal() {
        return val;
    }

    public String getName() {
        return name;
    }
}
