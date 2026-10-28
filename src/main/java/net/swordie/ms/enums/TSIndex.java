package net.swordie.ms.enums;

import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;

import java.util.ArrayList;
import java.util.List;

public enum TSIndex {
    DashSpeed(0),
    DashJump(1),
    RideVehicle(2),
    PartyBooster(3),
    GuidedBullet(4),
    Undead(5),
    RideVehicleExpire(6),
    RelicGauge(7),
    SecondAtomLockOn(8);

    private final int index;

    TSIndex(int index) {
        this.index = index;
    }

    public static CharacterTemporaryStat getCTSFromTwoStatIndex(int index) {
        switch (index) {
            case 0:
                return CharacterTemporaryStat.DashSpeed;
            case 1:
                return CharacterTemporaryStat.DashJump;
            case 2:
                return CharacterTemporaryStat.RideVehicle;
            case 3:
                return CharacterTemporaryStat.PartyBooster;
            case 4:
                return CharacterTemporaryStat.GuidedBullet;
            case 5:
                return CharacterTemporaryStat.Undead;
            case 6:
                return CharacterTemporaryStat.RideVehicleExpire;
            case 7:
                return CharacterTemporaryStat.RelicGauge;
            case 8:
                return CharacterTemporaryStat.SecondAtomLockOn;
            default:
                return null;
        }
    }

    public static TSIndex getTSEFromCTS(CharacterTemporaryStat cts) {
        switch (cts) {
            case DashJump:
                return DashJump;
            case DashSpeed:
                return DashSpeed;
            case RideVehicle:
                return RideVehicle;
            case PartyBooster:
                return PartyBooster;
            case GuidedBullet:
                return GuidedBullet;
            case Undead:
                return Undead;
            case RideVehicleExpire:
                return RideVehicleExpire;
            case RelicGauge:
                return RelicGauge;
            case SecondAtomLockOn:
                return SecondAtomLockOn;
        }
        return null;
    }

    public static boolean isTwoStat(CharacterTemporaryStat cts) {
        return getTSEFromCTS(cts) != null;
    }

    public static List<CharacterTemporaryStat> cts = new ArrayList<>();

    static {
        for (int i = 0; i < TSIndex.values().length; i++) {
            cts.add(getCTSFromTwoStatIndex(i));
        }
    }

    public static List<CharacterTemporaryStat> getAllCTS() {
        return cts;
    }

    public int getIndex() {
        return index;
    }
}
