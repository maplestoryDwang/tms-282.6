package net.swordie.ms.life.mob;

import java.util.Arrays;
import java.util.List;

public enum MobStat {
    IndiePDR(0),
    IndieMDR(1),
    IndieAddFinalDamSkill(2),
    IndieSlow(3),
    IndieTriangleFormation(4),
    IndieSixthTriangleFormation(5),
    IndieDummyBuffIcon(6),
    IndieAbyssalDarkness(7),
    IndieTotalDam(8),
    IndieTargetPriority(9),
    IndieEnd(10),
    PAD(11),
    PDR(12),
    MAD(13),
    MDR(14),
    ACC(15),
    EVA(16),
    Speed(17),
    ArcMage2Stack(18),
    Stun(19),
    Freeze(20),
    BeforeFreeze(21),
    Poison(22),
    Seal(23),
    Darkness(24),
    PowerUp(25),
    MagicUp(26),
    PGuardUp(27),
    MGuardUp(28),
    PImmune(29),
    MImmune(30),
    Web(31),
    HardSkin(32),
    Ambush(33),
    Venom(34),
    Blind(35),
    SealSkill(36),
    Dazzle(37),
    PCounter(38),
    MCounter(39),
    RiseByToss(40),
    Weakness(41),
    Showdown(42),
    DevilCry(43),
    MagicCrash(44),
    DamagedElemAttr(45),
    TotalDamParty(46),
    HitCriDamR(47),
    Fatality(48),
    Lifting(49),
    DeadlyCharge(50),
    Smite(51),
    AddDamSkill(52),
    AddExpSkill(53),
    Incizing(54),
    DodgeBodyAttack(55),
    DebuffHealing(56),
    AddDamSkill2(57),
    BodyAttack(58),
    TempMoveAbility(59),
    FixDamRBuff(60),
    SpiritGate(61),
    ElementDarkness(62),
    AreaInstallByHit(63),
    BMageDebuff(64),
    JaguarProvoke(65),
    JaguarBleeding(66),
    PinkBeanFlowerPot(67),
    BattlePvPHelenaMark(68),
    BattlePvP_Darklord_Explosion(69),
    PsychicLock(70),
    PsychicLockCoolTime(71),
    PsychicGroundMark(72),
    PowerImmune(73),
    MultiPMDR(74),
    BahamutLightElemAddDam(75),
    LefDebuff(76),
    BossPropPlus(77),
    MultiDamSkill(78),
    RWLiftPress(79),
    RWChoppingHammer(80),
    MobAggro(81),
    AreaPDR(82),
    BuffControl(83),
    IgnoreFreeze(84),
    BattlePvP_Ryude_Frozen(85),
    DamR(86),
    ContinuousHeal(87),
    HiddenPullingDebuff(88),
    CurseTransition(89),
    WindBreakerPinpointPierce(90),
    Morph(91),
    MobLock(92),
    LWGathering(93),
    KinesisLawOfGravity(94),
    TargetPlus(95),
    ReviveOnce(96),
    BuffFlag(97),
    HolyShell(98),
    ZeroCriticalBind(99),
    Panic(100),
    ReduceFinalDamage(101),
    Stalking(102),
    ChangeMobAction(103),
    HealByDamage(104),
    ActionState(105),
    ShamanCatDot(106),
    KannaAddAttack(107),
    NewPriatePanda(108),
    CatKnitting(109),
    SummonGhost(110),
    TimeBomb(111),
    AddEffect(112),
    Invincible(113),
    Explosion(114),
    Unk115(115),
    HangOver(116),
    LevelInc(117),
    AfterImage(118),
    BurnedInfo(119),
    Sleep(120),
    ExchangeAttack(121),
    ExtraBuffStat(122),
    LinkTeam(123),
    SoulExplosion(124),
    TrueSight(125),
    Laser(126),
    StatResetSkill(127),
    Unk128(128),
    Unk129(129), // Event
    Unk130(130), // Event
    Unk131(131), // Event
    Unk132(132),
    OriginDebuff(133), // 22?
    Unk134(134),
    Unk135(135),
    Unk136(136),
    Unk137(137),
    Unk138(138), // v265_1 => 90 | 100000006 | 42
    Unk139(139), // v265_1
    Unk140(140), // v265_1 | không có gì
    NewBurnedInfo(141), // v265_1
    No(160),
    ;

    public static final List<MobStat> orders = Arrays.asList(IndiePDR, IndieMDR, IndieAddFinalDamSkill, IndieSlow, IndieTriangleFormation, IndieSixthTriangleFormation, IndieDummyBuffIcon, IndieAbyssalDarkness, IndieTotalDam, IndieTargetPriority, PAD, PDR, MAD, MDR, ACC, EVA, Speed, ArcMage2Stack, Stun, Freeze, BeforeFreeze, Poison, Seal, Darkness, PowerUp, MagicUp, PGuardUp, MGuardUp, PImmune, MImmune, Web, HardSkin, Ambush, Venom, Blind, SealSkill, Dazzle, PCounter, MCounter, RiseByToss, Weakness, Showdown, DevilCry, MagicCrash, DamagedElemAttr, TotalDamParty, HitCriDamR, Fatality, Lifting, DeadlyCharge, Smite, AddDamSkill, AddExpSkill, Incizing, DodgeBodyAttack, DebuffHealing, AddDamSkill2, BodyAttack, TempMoveAbility, FixDamRBuff, SpiritGate, ElementDarkness, AreaInstallByHit, BMageDebuff, JaguarProvoke, JaguarBleeding, PinkBeanFlowerPot, BattlePvPHelenaMark, BattlePvP_Darklord_Explosion, PsychicLock, PsychicLockCoolTime, PsychicGroundMark, PowerImmune, MultiPMDR, BahamutLightElemAddDam, LefDebuff, BossPropPlus, MultiDamSkill, RWLiftPress, RWChoppingHammer, MobAggro, AreaPDR, BuffControl, IgnoreFreeze, BattlePvP_Ryude_Frozen, DamR, ContinuousHeal, HiddenPullingDebuff, CurseTransition, WindBreakerPinpointPierce, Morph, MobLock, LWGathering, KinesisLawOfGravity, TargetPlus, ReviveOnce, BuffFlag, HolyShell, ZeroCriticalBind, Panic, ReduceFinalDamage, Stalking, ChangeMobAction, HealByDamage, ActionState, ShamanCatDot, KannaAddAttack, NewPriatePanda, CatKnitting, SummonGhost, TimeBomb, AddEffect, Invincible, Explosion, Unk115, HangOver, LevelInc, AfterImage);

    public static final int LENGTH = 5;
    private int val;
    private int pos;
    private int bitPos;

    MobStat(int val) {
        this.bitPos = val;
        this.val = 1 << (31 - bitPos % 32);
        this.pos = bitPos / 32;
    }

    public int getOrder() {
        return orders.indexOf(this);
    }

    public int getPos() {
        return pos;
    }

    public int getVal() {
        return val;
    }

    public int getBitPos() {
        return bitPos;
    }

    public static MobStat getByBitPos(int bitPos) {
        return Arrays.stream(values()).filter(v -> v.getBitPos() == bitPos).findAny().orElse(null);
    }

    public boolean isIndie() {
        return ordinal() <= IndieEnd.ordinal();
    }

    public boolean isMovementAffectingStat() {
        switch (this) {
            case Speed:
            case Stun:
            case Freeze:
            case RiseByToss:
            case Lifting:
            case Smite:
            case TempMoveAbility:
            case RWLiftPress:
            case OriginDebuff:
            case IndieSlow:
                return true;
            default:
                return false;
        }
    }

}
