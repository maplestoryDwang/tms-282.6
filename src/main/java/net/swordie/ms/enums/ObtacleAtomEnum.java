package net.swordie.ms.enums;

/**
 * Created by Asura on 31-7-2018.
 */

public enum ObtacleAtomEnum {
    // Magnus
    GreenMeteor(1, 20),
    GreenMeteor2(2, 20),
    GreenMeteor3(3, 20),
    GreenMeteor4(4, 20),
    GreenMeteor5(5, 20),
    BlueMeteor(6, 40),
    BlueMeteor2(7, 40),
    PurpleMeteor(8, 60),
    PurpleMeteor2(9, 60),

    // Some JQ (probably)
    SmallPumpkin(12, 10),
    LargePumpkin(13, 20),

    // Some JQ (probably)
    PigTotem(18, 15),
    PepeTotem(19, 15),

    // Von Bon
    VonBonPurpleClock(22, 60),
    VonBonBlueClock(23, 40),
    VonBonGreenClock(24, 20),
    VonBonPurpleClock2(25, 60),
    VonBonBlueClock2(26, 40),
    VonBonGreenClock2(27, 20),

    // Magnus
    PurpleMeteor3(28, 60),
    PurpleMeteor4(29, 60),
    PurpleMeteor5(30, 25),
    PurpleMeteor6(31, 60),
    PurpleMeteor7(32, 60),

    // Don't know
    RedStirge(34, 15),

    // Hekaton
    HekatonRedOrb(35, 35),
    HekatonLightOrd(36, 30),

    // Some Event (probably)
    Icicle(38, 10),
    LargeFallingSnow(39, 20),
    MediumFallingSnow(40, 15),
    LittleFallingSnow(41, 10),

    // Dorothy
    SmallDorothyMeteor(42, 27),
    MediumDorothyMeteor(43, 30),
    LargeDorothyMeteor(44, 30),

    // Mushroom Kingdom boss??
    LargeMushroom(45, 20),
    MediumMushroom(46, 18),
    SmallMushroom(47, 13),

    // Lotus
    LotusBlueDebris(48, 25),
    LotusYellowDebris(49, 35),
    LotusPurpleDebris(50, 55),
    LotusRobotDebris(51, 85),
    LotusCrusherDebris(52, 125),

    // Don't know
    IceSpike(53, 10),
    FlameOrb(54, 18),

    // Don't know
    IceRockFalling(55, 15),
    IceRockFalling_2(56, 12),   // They get smaller, but it's hard to see
    IceRockFalling_3(57, 10),

    // Demian/Damien
    DemianYellowOrb(58, 44),

    // Don't know
    RedOrb(59, 20),
    ;

    private int type;
    private int hitBox;

    ObtacleAtomEnum(int type, int hitBox) {
        this.type = type;
        this.hitBox = hitBox;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getHitBox() {
        return hitBox;
    }

    public void setHitBox(int hitBox) {
        this.hitBox = hitBox;
    }
}

