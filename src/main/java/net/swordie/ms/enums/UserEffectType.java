package net.swordie.ms.enums;

import java.util.Arrays;

public enum UserEffectType {
    LevelUp(0),
    SkillUse(1),
    SkillUseBySummoned(2),
    Unk3(3), // new
    SkillAffected(4),
    SkillAffected_Ex(5),
    SkillAffected_Select(6),
    SkillSpecialAffected(7),
    Quest(8),
    Pet(9),
    SkillSpecial(10),
    Resist(11),
    ProtectOnDieItemUse(12),
    PlayPortalSE(13),
    JobChanged(14),
    QuestComplete(15),
    IncDecHPEffect(16),
    BuffItemEffect(17),
    SquibEffect(18),
    MonsterBookCardGet(19),
    LotteryUse(20),
    ItemLevelUp(21),
    ItemMaker(22),
    FieldMesoItemConsumed(23), // new
    ExpItemConsumed(24),
    FieldExpItemConsumed(25),
    ReservedEffect(26),
    unk27(27), // old unknown
    UpgradeTombItemUse(28),
    BattlefieldItemUse(29),
    unk30(30), // old unknown
    AvatarOriented(31),
    AvatarOrientedRepeat(32),
    AvatarOrientedMultipleRepeat(33),
    IncubatorUse(34),
    PlaySoundWithMuteBGM(35),
    PlayExclSoundWithDownBGM(36),
    SoulStoneUse(37),
    IncDecHPEffect_EX(38),
    IncDecHPRegenEffect(39),
    EffectUOL(40),
    PvPRage(41),
    PvPChampion(42),
    PvPGradeUp(43),
    PvPRevive(44),
    PvPJobEffect(45),
    FadeInOut(46),
    MobSkillHit(47),
    unk48(48), // new
    BlindEffect(49),
    BossShieldCount(50),
    ResetOnStateForOnOffSkill(51),
    JewelCraft(52),
    ConsumeEffect(53),
    PetBuff(54),
    LotteryUIResult(55),
    LeftMonsterNumber(56),
    ReservedEffectRepeat(57),
    RobbinsBomb(58),
    SkillMode(59),
    ActQuestComplete(60),
    Point(61), // v263
    SpeechBalloon(62), // v263
    SpeechBalloon_New(63),
    Unk64(64),
    Unk65(65),
    Unk66(66),
    TextEffect(67), // v263
    SkillPreLoopEnd(68), // 64 - 68
    Aiming(69),
    Unk70(70),
    DrawOriginEffect(71), // UI/UIWindow.img/FloatNotice/%d/DrawOrigin/icon 183 | HIỆU ỨNG ĐẸP CHO ĐỒ HIẾM
    BattlePvP_IncDecHp(72), // 68 - > 72
    CatchEffect(73),
    FailCatchEffect(74), // Effect/ItemEff.img/2270002/fail
    BiteAttack_ReceiveSuccess(75),
    BiteAttack_ReceiveFail(76),
    foxManActionSetUsed(77),
    Unk78(78),
    MobSkillSpecial(79),
    BlackMageEffect(80), // Black Mage  Effect (100017)
    ResistAbnormalStatus(81),
    Unk82(82),
    RedChat(83),
    Unk84(84),
    Unk85(85),
    GoblinBatHit(86),
    Unk87(87),
    IncDecHPEffect_Unk(88),
    SpeedMirageEff(89), // v265.1
    IncDecHPEffect_Unk2(90),
    Unk91(91),
    Unk92(92),
    Unk93(93),
    ShowSkillEfffect(94),
    Unk95(95),
    Unk96(96),
    HexaSkillEff(97),
    Unk98(98),
    Unk99(99),
    SkillMoveEffect(100), // Skill/%03d.img/skill/%07d/moveEffect
    UpgradePotionMsg(101),
    MonsterBookSetComplete(86), // not sure
    FamiliarEscape(87), // not sure
    WaterSmashResult(102),
    ElunaJewel(103), // Eluna Jewel?
    SomeUpgradeEffectOnUser(104), // Eluna
    Unk105(105),
    MoXuanEff(106),
    ;

    private final byte val;

    UserEffectType(int val) {
        this.val = (byte) val;
    }

    public byte getVal() {
        return val;
    }

    public static UserEffectType getByVal(int val) {
        return Arrays.stream(values()).filter(uet -> uet.getVal() == val).findAny().orElse(null);
    }
}
