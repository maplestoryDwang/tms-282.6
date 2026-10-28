package net.swordie.ms.enums;

import java.util.Arrays;

public enum DimensionalPortalType {
    // Dimensional Gate - Type 0
    AriantColiseum(0, 0, 980010000, "Ariant Coliseum"),
    Dojo(1, 0, 925020000, "Mu Lung Dojo"),
    GonzoGallery(2, 0, 956000000, "Gonzo Gallery"),
    MonsterCarnival(3, 0, 980000000, "Monster Carnival"),
    GhostShipSea(4, 0, 0, "Ghost Ship Party Quest"),
    NettsPyramid(5, 0, 926010000, "Nett's Pyramid"),
    DustyPlatform(6, 0, 0, "Dusty Platform"),
    Happyville(7, 0, 209000000, "Happyville"),
    GoldenTemple(8, 0, 0, "Golden Temple"), // Unused
    MoonBunnyPQ(9, 0, 933000000, "Moon Bunny Party Quest"),
    KerningCityPQ(10, 0, 933010000, "First Time Together Party Quest"),
    LudiPQ(11, 0, 0, "Ludibrium Party Quest"),
    ElinPQ(12, 0, 0, "Ellin Forest Party Quest"),
    OrbisPQ(13, 0, 0, "Orbis Party Quest"),
    PiratePQ(14, 0, 0, "Pirate Party Quest"),
    RandJPQ(15, 0, 0, "Romeo and Juliet Party Quest"),
    ElNathPQ(16, 0, 0, "Hoblin King Party Quest"),
    DragonRiderPQ(17, 0, 0, "Dragon Rider Party Quest"),
    PurpleTown(18, 0, 0, "Purple Town"),
    HalloweenGPQ(19, 0, 0, "Halloween Guild Party Quest"),
    PurpleTown2(20, 0, 0, "Purple Town 2"),
    KentaPQ(21, 0, 0, "Kenta Party Quest"),
    LionKingPQ(22, 0, 0, "Lion King Party Quest"),
    IceKnightPQ(23, 0, 0, "Ice Knight Party Quest"),
    MapleAlliance(25, 0, 913050010, "Maple Alliance"),
    PurpleTown3(26, 0, 0, "Purple Town 2"),
    Azwan(27, 0, 0, "Azwan"),
    GoldenTemple2(28, 0, 252000000, "Golden Temple"),
    Spiegelmann(29, 0, 0, "Spiegelmann"),
    PvP(30, 0, 0, "Player Versus Player"),
    Zipangu(31, 0, 0, "Zipangu"),
    Evolution(32, 0, 957000000, "Evolution System"),
    DimensionalInvasion(33, 0, 940020000, "Dimensional Invasion"),
    BronzeGuestHouse(34, 0, 0, "Bronze Guest House"),
    SilverGuestHouse(35, 0, 0, "Silver Guest House"),
    GoldGuestHouse(36, 0, 0, "Gold Guest House"),
    TangyoonCooking(37, 0, 0, "Tangyoon Cooking"),
    CrimsonwoodKeep(38, 0, 610030010, "Crimsonwood Keep"),
    GrandAthanaeum(39, 0, 302000000, "The Grand Athanaeum"),
    SpecialMission(40, 0, 0, "Special Mission"),
    PartyQuestHub(41, 0, 910002000, "Party Quest Hub"), // Spiegelmann's quest house
    RootAbyss(42, 0, 910700200, "Root Abyss"),
    TowerOfOz(43, 0, 992000000, "Tower of Oz"),
    FriendStory(44, 0, 0, "FriendStory"),
    MonsterPark(45, 0, 951000000, "Monster Park"),
    WeirdMob(46, 0, 0, "Weird Mob"),
    Ursus(47, 0, 0, "Ursus"),
    Cygnus(49, 0, 0, "Cygnus Empress"),
    Maplerunner(50, 0, 993001000, "Maplerunner"),

    // Time Gate - Type 1 - Val 0 -> 6
    TimeGate_0(0, 1, 0, ""),
    TimeGate_1(1, 1, 0, ""),
    TimeGate_2(2, 1, 0, ""),
    TimeGate_3(3, 1, 0, ""),
    TimeGate_4(4, 1, 0, ""),
    TimeGate_5(5, 1, 0, ""),
    TimeGate_6(6, 1, 0, ""),

    // BigWig Teleport Rock - Type 2: Korean UI

    // Teleport Rock - Type 3:
    TeleportRock_0(0, 3, 100000000, "Henesys"),
    TeleportRock_1(1, 3, 101000000, "Ellinia"),
    TeleportRock_2(2, 3, 102000000, "Perion"),
    TeleportRock_3(3, 3, 103000000, "Kerning City"),
    TeleportRock_4(4, 3, 104000000, "Lith Harbor"),
    TeleportRock_5(5, 3, 105000000, "Sleepywood"),
    TeleportRock_6(6, 3, 120000000, "Nautilus Harbor"),
    TeleportRock_7(7, 3, 130000000, "Ereve"),
    TeleportRock_8(8, 3, 140000000, "Rien"),
    TeleportRock_9(9, 3, 200000000, "Orbis"),
    TeleportRock_10(10, 3, 211000000, "El Nath"),
    TeleportRock_11(11, 3, 220000000, "Ludibrium"),
    TeleportRock_12(12, 3, 221000000, "Omega Sector"),
    TeleportRock_13(13, 3, 222000000, "Korean Folk Town"),
    TeleportRock_14(14, 3, 230000000, "Aquarium"),
    TeleportRock_15(15, 3, 240000000, "Leafre"),
    TeleportRock_16(16, 3, 250000000, "Mu Lung"),
    TeleportRock_17(17, 3, 251000000, "Herb Town"),
    TeleportRock_18(18, 3, 260000100, "Ariant"),
    TeleportRock_19(19, 3, 261000000, "Magatia"),
    TeleportRock_20(20, 3, 310000000, "Edelstein"),
    TeleportRock_21(21, 3, 600000000, "New Leaf City"),
    TeleportRock_22(22, 3, 540000000, "Singapore"),
    TeleportRock_23(23, 3, 101050000, "Elluel"),
    TeleportRock_24(24, 3, 400000000, "Pantheon"),
    TeleportRock_25(25, 3, 310070000, "Haven"),

    // Mu Lung Dojo - Type 4
    DojoRecoveryBuff_0(0, 4, 0, ""),
    DojoRecoveryBuff_1(1, 4, 0, ""),
    DojoRecoveryBuff_2(2, 4, 0, ""),
    DojoRecoveryBuff_3(3, 4, 0, ""),
    DojoRecoveryBuff_4(4, 4, 0, ""),
    DojoRecoveryBuff_5(5, 4, 0, ""),
    DojoRecoveryBuff_6(6, 4, 0, ""),
    DojoRecoveryBuff_7(7, 4, 0, ""),
    DojoRecoveryBuff_8(8, 4, 0, ""),
    DojoRecoveryBuff_9(9, 4, 0, ""),

    // Pantheon Interdimensional Portal - Type 5
    PantheonInterdimensionalPortal_0(0, 5, 104020000, "Six Path Crossway"),
    PantheonInterdimensionalPortal_1(1, 5, 100000000, "Henesys"),
    PantheonInterdimensionalPortal_2(2, 5, 101000000, "Ellinia"),
    PantheonInterdimensionalPortal_3(3, 5, 102000000, "Perion"),
    PantheonInterdimensionalPortal_4(4, 5, 103000000, "Kerning City"),
    PantheonInterdimensionalPortal_5(5, 5, 104000000, "Lith Harbor"),
    PantheonInterdimensionalPortal_6(6, 5, 105000000, "Sleepywood"),
    PantheonInterdimensionalPortal_7(7, 5, 120000000, "Nautilus Harbor"),
    PantheonInterdimensionalPortal_8(8, 5, 130000000, "Ereve"),
    PantheonInterdimensionalPortal_9(9, 5, 140000000, "Rien"),
    PantheonInterdimensionalPortal_10(10, 5, 200000000, "Orbis"),
    PantheonInterdimensionalPortal_11(11, 5, 211000000, "El Nath"),
    PantheonInterdimensionalPortal_12(12, 5, 220000000, "Ludibrium"),
    PantheonInterdimensionalPortal_13(13, 5, 221000000, "Omega Sector"),
    PantheonInterdimensionalPortal_14(14, 5, 222000000, "Korean Folk Town"),
    PantheonInterdimensionalPortal_15(15, 5, 230000000, "Aquarium"),
    PantheonInterdimensionalPortal_16(16, 5, 240000000, "Leafre"),
    PantheonInterdimensionalPortal_17(17, 5, 250000000, "Mu Lung"),
    PantheonInterdimensionalPortal_18(18, 5, 251000000, "Herb Town"),
    PantheonInterdimensionalPortal_19(19, 5, 260000100, "Ariant"),
    PantheonInterdimensionalPortal_20(20, 5, 261000000, "Magatia"),
    PantheonInterdimensionalPortal_21(21, 5, 310000000, "Edelstein"),
    PantheonInterdimensionalPortal_22(22, 5, 101050000, "Elluel"),
    PantheonInterdimensionalPortal_24(23, 5, 820000000, "Event Hall"),
    PantheonInterdimensionalPortal_25(24, 5, 940001000, "Pantheon"),
    PantheonInterdimensionalPortal_26(25, 5, 940020000, "Stormfront"),
    PantheonInterdimensionalPortal_27(26, 5, 105300000, "Deserted Camp"),

    // Veritas Promessa Route - Type 6
    VeritasPromessaRoute_0(0, 6, 100000000, "Henesys"),
    VeritasPromessaRoute_1(1, 6, 101000000, "Ellinia"),
    VeritasPromessaRoute_2(2, 6, 102000000, "Perion"),
    VeritasPromessaRoute_3(3, 6, 103000000, "Kerning City"),
    VeritasPromessaRoute_4(4, 6, 104000000, "Lith Harbor"),
    VeritasPromessaRoute_5(5, 6, 105000000, "Sleepywood"),
    VeritasPromessaRoute_6(6, 6, 120000000, "Nautilus Harbor"),
    VeritasPromessaRoute_7(7, 6, 130000000, "Ereve"),
    VeritasPromessaRoute_8(8, 6, 140000000, "Rien"),
    VeritasPromessaRoute_9(9, 6, 200000000, "Orbis"),
    VeritasPromessaRoute_10(10, 6, 211000000, "El Nath"),
    VeritasPromessaRoute_11(11, 6, 220000000, "Ludibrium"),
    VeritasPromessaRoute_12(12, 6, 221000000, "Omega Sector"),
    VeritasPromessaRoute_13(13, 6, 222000000, "Korean Folk Town"),
    VeritasPromessaRoute_14(14, 6, 230000000, "Aquarium"),
    VeritasPromessaRoute_15(15, 6, 240000000, "Leafre"),
    VeritasPromessaRoute_16(16, 6, 250000000, "Mu Lung"),
    VeritasPromessaRoute_17(17, 6, 251000000, "Herb Town"),
    VeritasPromessaRoute_18(18, 6, 260000100, "Ariant"),
    VeritasPromessaRoute_19(19, 6, 261000000, "Magatia"),
    VeritasPromessaRoute_20(20, 6, 310000000, "Edelstein"),
    VeritasPromessaRoute_21(21, 6, 101050000, "Elluel"),
    VeritasPromessaRoute_22(22, 6, 400000000, "Pantheon"),
    VeritasPromessaRoute_23(23, 6, 820000000, "Event Hall"),
    VeritasPromessaRoute_24(24, 6, 310070000, "Haven"),

    ;

    private final int val;
    private final int type;
    private final int mapID;
    private final String desc;

    DimensionalPortalType(int val, int type, int mapID, String desc) {
        this.val = val;
        this.type = type;
        this.mapID = mapID;
        this.desc = desc;
    }

    public static DimensionalPortalType getByValWithType(int val, int type) {
        return Arrays.stream(values()).filter(dpt -> dpt.getVal() == val && dpt.getType() == type).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }

    public int getMapID() {
        return mapID;
    }

    public String getDesc() {
        return desc;
    }

    public int getType() {
        return type;
    }
}
