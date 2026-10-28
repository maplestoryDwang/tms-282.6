package net.swordie.ms.enums;

public enum ClaimResultType {
    Success(2),
    AlreadyReportThisUser(3),
    PleaseTryAgain(65),
    WrongCharacterName(66),
    NotEnoughMesosToReport(67),
    UnableToConnectToServer(68),
    ExceededNumbersOfReports(69),
    YouMayOnlyReportFromTime(71),
    UnableToReport(72);

    private final int val;

    ClaimResultType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
