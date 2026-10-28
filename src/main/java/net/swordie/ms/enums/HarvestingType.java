package net.swordie.ms.enums;

public enum HarvestingType {
    HerbalismLevelTooLow(2),
    DoingThisRequiresHerbalismSkill(3),
    DoingThisRequiresMiningSkill(4),
    YouAreTooTiredToDoThis(5),
    YouAreTooFarAwayToHarvest(6),
    HarvestingCanceled(7),
    SomeoneIsAlreadyCollectingIt(8),
    YouCannotCollectItYet(9),
    AnUnknownErrorIsPrevetingYouFromHarvesting(10),
    DoHarvesting(11),
    YouHaveNoHerbalismTools(12),
    YouHaveNoMiningTools(13);

    private final int val;

    HarvestingType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
