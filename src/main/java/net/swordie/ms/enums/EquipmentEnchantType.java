package net.swordie.ms.enums;

import java.util.Arrays;

public enum EquipmentEnchantType {
    ScrollUpgradeRequest(0), // v265.1
    HyperUpgradeResult(1), // v265.1
    TransmissionResult(2), // v265.1
    ScrollUpgradeDisplay(50), // v265.1
    ScrollTimerEffective(51), // v265.1
    HyperUpgradeDisplay(52), // v265.1
    MiniGameDisplay(53), // v265.1
    ShowScrollUpgradeResult(100), // v265.1
    ShowChaosScrollUpgradeResult(101), // v265.1
    ShowScrollVestigeCompensationResult(102), // v265.1
    ShowTransmissionResult(103), // v265.1
    ShowHyperUpgradeResult(104), // v265.1
    ShowUnknownFailResult(105), // v265.1
    Unk(109);

    private final byte val;

    EquipmentEnchantType(int val) {
        this.val = (byte) val;
    }

    public static EquipmentEnchantType getByVal(byte val) {
        return Arrays.stream(values()).filter(tt -> tt.getVal() == val).findAny().orElse(null);
    }

    public byte getVal() {
        return val;
    }
}
