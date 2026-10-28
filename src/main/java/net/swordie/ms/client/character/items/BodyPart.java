package net.swordie.ms.client.character.items;

import java.util.Arrays;

public enum BodyPart {
    BPBase(0),
    Hair(0),
    Hat(1),
    FaceAccessory(2),
    EyeAccessory(3),
    Earrings(4),
    Top(5),
    Overall(5), // Top and overall share the same body part
    Bottom(6),
    Shoes(7),
    Gloves(8),
    Cape(9),
    Shield(10), // Includes things such as katara, 2ndary
    Weapon(11),
    Ring1(12),
    Ring2(13),
    PetWear1(14),
    Ring3(15),
    Ring4(16),
    Pendant(17),
    TamingMob(18),
    Saddle(19),
    MobEquip(20),
    Medal_OLD(21),
    Belt(22),
    Shoulder_OLD(23),
    PetWear2(24),
    PetWear3(25),
    PocketItem_OLD(26),
    Android_OLD(27),
    MechanicalHeart_OLD(28),
    Badge_OLD(29),
    Emblem_OLD(30),
    Extended0(31),
    ExtendedPendant_OLD(31),
    Extended1(32),
    Extended2(33),
    Extended3(34),
    Extended4(35),
    Extended5(36),
    Extended6(37),
    Medal(49),
    Shoulder(51),
    PocketItem(52),
    Android(53),
    MechanicalHeart(54),
    MonsterBook(55),
    Badge(56),
    Emblem(61),
    ExtendedPendant(65),
    Sticker(100),
    BPEnd(66),
    CBPBase(101), // CASH
    CHat(101),
    CFaceAccessory(102),
    CEyeAccessory(103),
    CEarrings(104),
    CTop(105),
    COverall(105),
    CBottom(106),
    CShoes(107),
    CGloves(108),
    CCape(109),
    CWeapon(111),
    CRing1(112),
    CRing2(113),
    CRing3(114),
    CRing4(115),
    CPendant(117),
    PetAcc1(114),
    PetAcc2(130),
    PetAcc3(138),
    PetConsumeHPItem(200),
    PetConsumeMPItem(201),
    Title(768),
    CBPEnd(1000),
    EvanBase(1000),
    EvanHat(1000),
    EvanPendant(1001),
    EvanWing(1002),
    EvanShoes(1003),
    EvanEnd(1004),
    MechBase(1100),
    MachineEngine(1100),
    MachineArm(1101),
    MachineLeg(1102),
    MachineFrame(1103),
    MachineTransistor(1104),
    MechEnd(1105),
    APBase(1200),
    APHat(1200),
    APCape(1201),
    APFaceAccessory(1202),
    APTop(1203),
    APOverall(1203),
    APBottom(1204),
    APShoes(1205),
    APGloves(1206),
    APWeapon(1207),
    APRing(1210),
    APRing2(1211),
    APRing3(1212),
    APRing4(1213),
    APEnd(1214),
    DUBase(1300),
    DUHat(1300),
    DUCape(1301),
    DUFaceAccessory(1302),
    DUTop(1303),
    DUOverall(1303),
    DUGloves(1304),
    DUEnd(1305),
    BitsBase(1400), // 1400~1424
    BitsEnd(1425),
    ZeroBase(1500),
    ZeroEyeAccessory(1500),
    ZeroHat(1501),
    ZeroFaceAccessory(1502),
    ZeroEarrings(1503),
    ZeroCape(1504),
    ZeroTop(1505),
    ZeroOverall(1505),
    ZeroGloves(1506),
    ZeroWeapon(1507),
    ZeroBottom(1508),
    ZeroShoes(1509),
    ZeroRing1(1510),
    ZeroRing2(1511),
    ZeroPendant1(1512), // ?
    ZeroPendant2(1513), // ?
    ZeroEnd(1512),
    ArcBase(1600),
    ArcEnd(1606),
    AUSBase(1700),
    AUSEnd(1706),
    // 3000 ~ 3066 : preset 1
    // 4000 ~ 4066 : preset 2
    TotemBase(5000),
    Totem1(5000),
    Totem2(5001),
    Totem3(5002),
    Totem4(5003),
    TotemEnd(5004),
    MBPBase(5100),
    MBPHat(5101),
    MBPCape(5102),
    MBPTop(5103),
    MBPOverall(5103),
    MBPGloves(5104),
    MBPShoes(5105),
    MBPWeapon(5106),
    MBPEnd(5107),
    HakuStart(5200),
    HakuFan(5200),
    HakuEnd(5201),
    VEIBase(20000),
    VEIEnd(20024),
    SlotIndexNotDefined(15440);

    private final int val;

    BodyPart(int val) {
        this.val = val;
    }

    public static BodyPart getByVal(int bodyPartVal) {
        return Arrays.stream(values()).filter(bp -> bp.getVal() == bodyPartVal).findAny().orElse(null);
    }

    public int getVal() {
        return val;
    }

    public static int getZeroBetaCashEquipByBodyPart(int bodyPart) {
        switch (BodyPart.getByVal(bodyPart)) {
            case Hat:
                return 1501;
            case FaceAccessory:
                return 1502;
            case EyeAccessory:
                return 1500;
            case Earrings:
                return 1503;
            case Top:
            case Overall:
                return 1505;
            case Bottom:
                return 1508;
            case Shoes:
                return 1509;
            case Gloves:
                return 1506;
            case Cape:
                return 1504;
            case Weapon:
                return 1507;
            case Pendant:
                return 1512;
            case Ring1:
                return 1510;
            case Ring2:
                return 1511;
            default:
                return -1;
        }
    }

    public static int getCashEquipByBodyPart(int bodyPart) {
        switch (BodyPart.getByVal(bodyPart)) {
            case Hat:
                return 101;
            case FaceAccessory:
                return 102;
            case EyeAccessory:
                return 103;
            case Earrings:
                return 104;
            case Top:
            case Overall:
                return 105;
            case Bottom:
                return 106;
            case Shoes:
                return 107;
            case Gloves:
                return 108;
            case Cape:
                return 109;
            case Weapon:
                return 111;
            case Pendant:
                return 117;
            case Ring1:
                return 112;
            case Ring2:
                return 113;
            default:
                return -1;
        }
    }
}
