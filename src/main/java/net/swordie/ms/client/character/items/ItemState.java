package net.swordie.ms.client.character.items;

public enum ItemState {
    None(0),
    Protected(0x1),
    PreventSlip(0x2),
    KarmaUsed(0x2),
    Binded(0x4),
    PossibleTrading(0x8),
    KarmaEQ(0x10),
    CraftedUsed(0x10),
    CharmEquipped(0x20),
    AndroidActivated(0x40),
    Crafted(0x80),
    ProtectionScrolled(0x100),
    LuckyDayScrolled(0x200),
    KarmaAccountUsed(0x400),
    Lock(0x800),
    LockSort(0x1000),
    SafetyScrolled(0x2000),
    RecoveryScrolled(0x4000),
    ReturnScrolled(0x8000);

    private final int val;

    ItemState(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
