package net.swordie.ms.client.character.items;

public enum EquipPrefix {
    Hat(100),
    FaceAccessory(101),
    EyeAccessory(102),
    Earrings(103),
    Top(104),
    Overall(105),
    Bottom(106),
    Shoes(107),
    Gloves(108),
    Shield(109),
    Cape(110),
    Ring(111),
    Pendant(112),
    Belt(113),
    Medal(114),
    Shoulder(115),
    PocketItem(116),
    MonsterBook(117),
    Badge(118),
    Emblem(119),
    Totem(120),
    ShiningRod(121),
    SoulShooter(122),
    Desperado(123),
    WhipBlade(124),
    Scepter(125),
    PsyLimiter(126),
    Chain(127),
    Gauntlet(128),

    HoyoungFan(129),
    OneHandedSword(130),
    OneHandedAxe(131),
    OneHandedBluntWeapon(132),
    Dagger(133),
    Katara(134),
    SecondaryWeapon(135), // includes all TUC-ignore-secondaries
    Cane(136),
    Wand(137),
    Staff(138),
    // no 139? probably a weapon that GMS doesn't have
    TwoHandedSword(140),
    TwoHandedAxe(141),
    TwoHandedBluntWeapon(142),
    Spear(143),
    PoleArm(144),
    Bow(145),
    Crossbow(146),
    Claw(147),
    Knuckle(148),
    Gun(149),
    Shovel(150), // herbalism
    Pickaxe(151), // mining
    DualBowguns(152),
    Cannon(153),
    Katana(154),
    Fan(155),
    Lapis(156), // zero
    Lazuli(157), //zero
    ArmCannon(158),
    AncientBow(159),
    SkillEffect(160),
    MachineEngine(161),
    MachineArm(162),
    MachineLeg(163),
    MachineFrame(164),
    MachineTransistor(165),
    Android(166),
    MechanicalHeart(167),
    Bit(168),
    CWeapon(170),
    Symbol(171),
    AstraSecondary(172), // TMS 282 Astra secondary weapon (worn in 10 or 37; not in 265 data)
    PetWear(180),
    PetSkill(181),
    TamingMob(190),
    Saddle(191),
    EvanHat(194),
    EvanPendant(195),
    EvanWing(196),
    EvanShoes(197);

    private final int val;
    private static final int MIN_VAL = 100;
    private static final int MAX_VAL = 197;
    private static final EquipPrefix[] CACHE;

    static {
        CACHE = new EquipPrefix[MAX_VAL - MIN_VAL + 1];
        for (EquipPrefix t : values()) {
            CACHE[t.val - MIN_VAL] = t;
        }
    }

    EquipPrefix(int val) {
        this.val = val;
    }

    public static EquipPrefix getByVal(int val) {
        if (val < MIN_VAL || val > MAX_VAL) {
            return null;
        }
        return CACHE[val - MIN_VAL];
    }

    public int getVal() {
        return val;
    }
}
