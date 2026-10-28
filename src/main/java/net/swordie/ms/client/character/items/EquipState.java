package net.swordie.ms.client.character.items;

public enum EquipState {
    MiraculousEnhancement(0x1),
    RefundableGachaponItem(0x8),
    RefundableEventGachaponItem(0x10),
    RedLabelItem(0x20),
    BlackLabelItem(0x40),
    InnocentRUCItem(0x80),
    AmazingHyperUpgradeChecked(0x100),
    AmazingHyperUpgradeUsed_Log(0x200),
    AmazingHyperUpgradeUsed_Stat(0x400),
    AmazingHyperUpgradeUsed_Sync(0x800),
    Unk1(0x1000),
    ExpiredOnLogout(0x2000),
    Unstability(0x4000),
    ExpiredOnLogoutPremium(0x8000),
    Unk2(0x10000),
    Lock(0x20000),
    TransferableWithWorld(0x40000),
    LockSort(0x80000),
    PresetItem(0x100000),
    Preset1Unequip(0x200000),
    Preset2Unequip(0x400000),
    Preset3Unequip(0x800000),
    ;

    private final int val;

    EquipState(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
