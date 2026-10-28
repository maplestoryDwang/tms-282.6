package net.swordie.ms.enums;

public enum ReportUserResultType {
    UnableToLocateTheUser(0),
    SuccessfullyReportTheUser(1),
    OnlyReport10TimesADay(2),
    CannotReportGM(3);

    private final int val;

    ReportUserResultType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
