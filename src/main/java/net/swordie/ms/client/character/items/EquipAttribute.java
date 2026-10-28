package net.swordie.ms.client.character.items;

public enum EquipAttribute {
    Locked(0x1), //1
    PreventSlipping(0x2), //2
    PreventColdness(0x4), //4
    UnTradable(0x8), //8
    UnTradableAfterTransaction(0x10), //16
    NoNonCombatStatGain(0x20), //32

    Crafted(0x80), //128
    ProtectionScroll(0x100), //256
    LuckyDay(0x200), //512

    TradedOnceWithinAccount(0x1000), //4096
    UpgradeCountProtection(0x2000), //8192
    ScrollProtection(0x4000), //16384
    ReturnScroll(0x8000); //32768

    private final int val;

    EquipAttribute(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }
}
