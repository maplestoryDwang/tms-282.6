package net.swordie.ms.connection.packet;

import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.connection.OutPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/**
 * Explicit (non n/r/t) fields of the STD_EXPLICIT stats in the 282.3 local CTS block (TEMPORARY_STAT_SET decoder
 * 14F8DF0). One {@link Spec} per stat: its bit, its position in the client's explicit read order ({@code seq} =
 * research/tms2823/packet-audit/cts-order-282.tsv {@code local_x}), the RVA of its bit test, and the fields the client
 * reads, each taken from a 265 {@link Option} field the way 265 TemporaryStatManager.encodeForLocal writes it.
 * <p>
 * The decoder reads the explicit fields in three places ({@link Area}); inside an area the order is {@code seq}:
 * <pre>
 *   mask, standard walker (n r t)
 *   PRE_COUNT          seq 1..2   SoulMP I I, FullSoulMP I                         (14F8F96..14F8FE8)
 *   i16 0, u8 u8 u8, i32 EtherealForm.x
 *   BEFORE_TWO_STATE   seq 4..65  Dice 21xI .. HolyMagicShell I                     (14F91D8..14FA467)
 *   two-state list, indie list
 *   AFTER_INDIE        seq 67..   OutSide I .. NeoTokyoBossPowOfLife I              (14FA818..14FCAC9)
 * </pre>
 * Every spec was traced CLEAN with research/tms2823/skill-trace/cts_trace.py (fixtures and expected layouts in
 * research/tms2823/skill-trace/ctsx-out and ctsx-expected, compared by tools/Tms282CtsExplicitCheck.java), alone and
 * combined, SET and RESET.
 * Where 282 reads more than 265 writes, the extra field is 0: HolySymbol's second u8 (I I I I B <b>B</b> I),
 * EunwolUnleashFoxOrb and LamourCarte's second i32. StrikerElectricUsed/Stack are i32 in 282 (u8 in 265).
 * Dice: 21 i32 (fixed loop 14F91F8), the first 21 of the 265 dice-info array, recomputed from nOption exactly as
 * 265 TemporaryStatManager.throwDice (one die n, or double down n = 10a + b). RPEventStat: 19 i32 from Option.rpStats
 * (LucidAbility, bossmech1-0930; all 0 when null). NextSpecificSkillDamageUp: i32 n + n x i32 skill id (Option.skillIds).
 * <p>
 * StopForceAtomInfo snapshots its additional TSM state into Option before encoding (124ACC0).
 * Not here (still skipped by the encoder): MobZoneState (its fields are TemporaryStatManager state),
 * and the EXPLICIT-only / unnamed bits.
 */
public final class Tms282CtsExplicit {
    /** Where the decoder reads a spec's fields (see class comment). */
    public enum Area { PRE_COUNT, BEFORE_TWO_STATE, AFTER_INDIE }

    /** A 265 Option field (ZERO: a field 265 does not have, written as 0). */
    public enum Src {
        N(o -> o.nOption), R(o -> o.rOption), T(o -> o.tOption), X(o -> o.xOption), Y(o -> o.yOption),
        Z(o -> o.zOption), W(o -> o.wOption), U(o -> o.uOption), B(o -> o.bOption), C(o -> o.cOption),
        S(o -> o.sOption), SS(o -> o.ssOption), M(o -> o.mOption), P(o -> o.pOption), ZERO(o -> 0);

        private final ToIntFunction<Option> get;

        Src(ToIntFunction<Option> get) {
            this.get = get;
        }

        public int of(Option o) {
            return get.applyAsInt(o);
        }
    }

    /** One read: width (B = u8, S = i16, I = i32) and source. */
    public record Field(char width, Src src) {
        void write(OutPacket out, Option o) {
            int v = src.of(o);
            switch (width) {
                case 'B' -> out.encodeByte(v);
                case 'S' -> out.encodeShort(v);
                case 'I' -> out.encodeInt(v);
                default -> throw new IllegalStateException("width " + width);
            }
        }
    }

    /**
     * @param reads  the 282 read shape (widths), for the check and the README
     * @param fields fixed fields, or empty when {@code custom} writes them
     */
    public record Spec(CharacterTemporaryStat stat, int bit, int seq, int rva, String reads, List<Field> fields,
                       BiConsumer<OutPacket, Option> custom) {
        public Area area() {
            return seq <= 2 ? Area.PRE_COUNT : seq <= 65 ? Area.BEFORE_TWO_STATE : Area.AFTER_INDIE;
        }

        public void write(OutPacket out, Option o) {
            if (custom != null) {
                custom.accept(out, o);
                return;
            }
            for (Field f : fields) {
                f.write(out, o);
            }
        }
    }

    /** Explicit i32 fields of RPEventStat (14FC274). */
    public static final int RP_EVENT_STAT_INTS = 19;
    /** Dice explicit i32 count (fixed loop 14F91F8). */
    public static final int DICE_INTS = 21;

    private static final Map<CharacterTemporaryStat, Spec> SPECS;

    static {
        List<Spec> list = new ArrayList<>(List.of(
            s(CharacterTemporaryStat.DarkSight, 308, 55, 0x14FA12D, "I:C I:X I:Y"),
            s(CharacterTemporaryStat.Stun, 315, 141, 0x14FC222, "I:X I:Y"),
            s(CharacterTemporaryStat.Poison, 316, 35, 0x14F9BE7, "I:X"),
            s(CharacterTemporaryStat.ComboCounter, 319, 89, 0x14FB194, "I:X I:Y"),
            s(CharacterTemporaryStat.HolySymbol, 323, 94, 0x14FB468, "I:X I:Y I:Z I:W B:B B:ZERO I:P"),
            s(CharacterTemporaryStat.PickPocket, 326, 59, 0x14FA2C6, "I:X"),
            s(CharacterTemporaryStat.Slow, 331, 23, 0x14F98E9, "B:B"),
            s(CharacterTemporaryStat.Stance, 335, 31, 0x14F9AF3, "I:X"),
            s(CharacterTemporaryStat.SharpEyes, 336, 32, 0x14F9B29, "I:M"),
            s(CharacterTemporaryStat.AdvancedBless, 341, 33, 0x14F9B5F, "I:X I:Y"),
            s(CharacterTemporaryStat.Beholder, 406, 28, 0x14F9A51, "I:S"),
            s(CharacterTemporaryStat.Bless, 411, 37, 0x14F9C53, "I:X"),
            s(CharacterTemporaryStat.HolyMagicShell, 436, 65, 0x14FA467, "I:X"),
            s(CharacterTemporaryStat.UsefulAdvancedBless, 463, 34, 0x14F9BB1, "I:X"),
            s(CharacterTemporaryStat.KeyDownMoving, 472, 8, 0x14F92F8, "I:X"),
            s(CharacterTemporaryStat.IgnoreTargetDEF, 473, 16, 0x14F9637, "I:M"),
            s(CharacterTemporaryStat.StrikerElectricUsed, 474, 17, 0x14F966D, "I:X"),
            s(CharacterTemporaryStat.StrikerElectricStack, 475, 18, 0x14F96A3, "I:X"),
            s(CharacterTemporaryStat.Judgement, 480, 10, 0x14F9367, "I:X"),
            s(CharacterTemporaryStat.Magnet, 483, 124, 0x14FBD86, "I:X"),
            s(CharacterTemporaryStat.LefBuffMastery, 487, 71, 0x14FAC94, "I:X I:C"),
            s(CharacterTemporaryStat.TempSecondaryStat, 488, 57, 0x14FA256, "I:X"),
            s(CharacterTemporaryStat.SixthJavelinStack, 491, 149, 0x14FC622, "I:X"),
            s(CharacterTemporaryStat.SixthGloryWingJavelinStack, 492, 150, 0x14FC658, "I:X"),
            s(CharacterTemporaryStat.YetiCook, 495, 129, 0x14FBEB0, "I:X B:B"),
            s(CharacterTemporaryStat.PinkbeanCheer, 496, 130, 0x14FBF05, "I:X B:B"),
            s(CharacterTemporaryStat.VampDeath, 498, 64, 0x14FA431, "I:X"),
            s(CharacterTemporaryStat.StackBuff, 503, 11, 0x14F939D, "B:M"),
            s(CharacterTemporaryStat.AntiMagicShell, 504, 15, 0x14F94DB, "B:B I:T"),
            s(CharacterTemporaryStat.LifeTidal, 505, 14, 0x14F94A5, "I:M"),
            s(CharacterTemporaryStat.SmashStack, 507, 20, 0x14F971E, "I:X I:Y I:T"),
            s(CharacterTemporaryStat.Trinity, 517, 12, 0x14F93D6, "B:M"),
            s(CharacterTemporaryStat.SoulExalt, 524, 36, 0x14F9C1D, "I:X"),
            s(CharacterTemporaryStat.DropRIncrease, 542, 26, 0x14F99A4, "I:X B:B"),
            s(CharacterTemporaryStat.IgnoreMobpdpR, 543, 24, 0x14F9932, "B:B"),
            s(CharacterTemporaryStat.BdR, 544, 25, 0x14F996B, "B:B"),
            s(CharacterTemporaryStat.NewFlying, 558, 131, 0x14FBF5A, "I:X"),
            s(CharacterTemporaryStat.PoseType, 567, 27, 0x14F99F9, "B:B B:S"),
            s(CharacterTemporaryStat.SoulMP, 572, 1, 0x14F8F96, "I:X I:R"),
            s(CharacterTemporaryStat.FullSoulMP, 573, 2, 0x14F8FE8, "I:X"),
            s(CharacterTemporaryStat.ElementalCharge, 575, 13, 0x14F940F, "B:X S:Y B:U B:W"),
            s(CharacterTemporaryStat.CrossOverChain, 577, 29, 0x14F9A87, "I:X"),
            s(CharacterTemporaryStat.ReincarnationMission, 579, 132, 0x14FBF90, "I:X"),
            s(CharacterTemporaryStat.ImmuneBarrier, 592, 30, 0x14F9ABD, "I:X"),
            s(CharacterTemporaryStat.CriticalGrowing, 593, 58, 0x14FA290, "I:X"),
            s(CharacterTemporaryStat.DotHealHPPerSecond, 606, 38, 0x14F9C89, "I:X"),
            s(CharacterTemporaryStat.DotHealMPPerSecond, 607, 39, 0x14F9CBF, "I:X"),
            s(CharacterTemporaryStat.SpiritGuard, 608, 41, 0x14F9D47, "I:N"),
            s(CharacterTemporaryStat.EunwolUnleashFoxOrb, 609, 40, 0x14F9CF5, "I:X I:ZERO"),
            s(CharacterTemporaryStat.SSFShootingAttack, 629, 45, 0x14F9E3B, "I:X"),
            s(CharacterTemporaryStat.ShieldAttack, 631, 44, 0x14F9E05, "I:X"),
            s(CharacterTemporaryStat.BMageDeath, 634, 144, 0x14FC4DC, "I:X"),
            s(CharacterTemporaryStat.NightWalkerBat, 641, 82, 0x14FAF72, "I:X"),
            s(CharacterTemporaryStat.PairingUser, 645, 60, 0x14FA2FC, "S:X"),
            s(CharacterTemporaryStat.Frenzy, 646, 61, 0x14FA335, "S:X"),
            s(CharacterTemporaryStat.ShadowSpear, 648, 62, 0x14FA36E, "S:X"),
            s(CharacterTemporaryStat.MastemaGuard, 649, 42, 0x14F9D7D, "I:X"),
            s(CharacterTemporaryStat.QuiverFullBurst, 651, 133, 0x14FBFC6, "I:X"),
            s(CharacterTemporaryStat.BattlePvP_Helena_Mark, 658, 46, 0x14F9E71, "I:C"),
            s(CharacterTemporaryStat.BattlePvP_Darklord_Explosion, 659, 47, 0x14F9EA7, "I:X"),
            s(CharacterTemporaryStat.PinkbeanAttackBuff, 664, 48, 0x14F9EDD, "I:B"),
            s(CharacterTemporaryStat.RoyalGuardState, 675, 49, 0x14F9F13, "I:B I:X"),
            s(CharacterTemporaryStat.MichaelSoulLink, 677, 50, 0x14F9F65, "I:X B:B I:C I:Y"),
            s(CharacterTemporaryStat.BladeStance, 681, 54, 0x14FA0F7, "I:X"),
            s(CharacterTemporaryStat.AdrenalinBoost, 692, 135, 0x14FC032, "I:X"),
            s(CharacterTemporaryStat.RWCylinder, 695, 51, 0x14F9FF2, "B:B S:C B:X"),
            s(CharacterTemporaryStat.RWMagnumBlow, 697, 53, 0x14FA09F, "S:B B:X"),
            s(CharacterTemporaryStat.Stigma, 707, 56, 0x14FA220, "I:B"),
            s(CharacterTemporaryStat.Michael_RhoAias, 723, 63, 0x14FA3A7, "I:X I:B I:C I:Y"),
            s(CharacterTemporaryStat.OutSide, 737, 69, 0x14FAC0C, "I:X"),
            s(CharacterTemporaryStat.WeaponVariety, 738, 74, 0x14FAD52, "I:X"),
            s(CharacterTemporaryStat.LefGloryWing, 739, 70, 0x14FAC42, "I:X I:C"),
            s(CharacterTemporaryStat.Shadower_Assassination, 740, 72, 0x14FACE6, "I:X"),
            s(CharacterTemporaryStat.BlackMageCreate, 746, 5, 0x14F9256, "I:X"),
            s(CharacterTemporaryStat.BlackMageDestroy, 747, 6, 0x14F928C, "I:X"),
            s(CharacterTemporaryStat.HitStackDamR, 749, 52, 0x14FA069, "I:X"),
            s(CharacterTemporaryStat.SpecterGauge, 762, 75, 0x14FAD88, "I:X"),
            s(CharacterTemporaryStat.SpecterMode, 763, 76, 0x14FADBE, "I:X"),
            s(CharacterTemporaryStat.SpellBullet_Plain, 764, 77, 0x14FADF4, "I:X I:C"),
            s(CharacterTemporaryStat.SpellBullet_Scarlet, 765, 78, 0x14FAE46, "I:X I:C"),
            s(CharacterTemporaryStat.SpellBullet_Gust, 766, 79, 0x14FAE98, "I:X I:C"),
            s(CharacterTemporaryStat.SpellBullet_Abyss, 767, 80, 0x14FAEEA, "I:X I:C"),
            s(CharacterTemporaryStat.BossWill_Infection, 779, 81, 0x14FAF3C, "I:X"),
            s(CharacterTemporaryStat.MemoryOfJourney, 796, 83, 0x14FAFA8, "I:X I:Y I:Z"),
            s(CharacterTemporaryStat.DecBaseDamageDebuff, 797, 87, 0x14FB128, "I:X"),
            s(CharacterTemporaryStat.LimitEquipStatDebuff, 799, 88, 0x14FB15E, "I:X"),
            s(CharacterTemporaryStat.PathFinderAncientGuidance, 811, 91, 0x14FB254, "I:X I:Y"),
            s(CharacterTemporaryStat.BattlePvP_KeyDown, 812, 92, 0x14FB3FC, "I:X"),
            s(CharacterTemporaryStat.BattlePvP_Wongki_AwesomeFairy, 814, 93, 0x14FB432, "I:X"),
            s(CharacterTemporaryStat.EventSoccerMomentBuff, 821, 152, 0x14FC6C4, "I:X"),
            s(CharacterTemporaryStat.FifthGoddessBless, 823, 90, 0x14FB1E6, "I:X I:Y I:Z"),
            s(CharacterTemporaryStat.CommonItemSkillContinuous, 824, 143, 0x14FC4A6, "I:X"),
            s(CharacterTemporaryStat.NewtroWarriors, 826, 85, 0x14FB084, "I:X I:Y"),
            s(CharacterTemporaryStat.MinigameStat, 830, 95, 0x14FB550, "I:X I:Y I:Z"),
            s(CharacterTemporaryStat.LuckyPapylus, 831, 86, 0x14FB0D6, "I:X I:Y"),
            s(CharacterTemporaryStat.AnimaThiefTaoistType, 832, 96, 0x14FB5BE, "I:X I:Y"),
            s(CharacterTemporaryStat.AnimaThiefTaoistGauge, 833, 97, 0x14FB610, "I:X"),
            s(CharacterTemporaryStat.AnimaThiefCloneAttack, 834, 128, 0x14FBE7A, "I:X"),
            s(CharacterTemporaryStat.AnimaThiefMetaphysics, 839, 98, 0x14FB646, "I:X"),
            s(CharacterTemporaryStat.XenonHoloGramGraffiti, 843, 99, 0x14FB67C, "I:X"),
            s(CharacterTemporaryStat.LefWarriorNobility, 851, 100, 0x14FB6B2, "I:X I:Y"),
            s(CharacterTemporaryStat.BMageAuraYellow, 855, 104, 0x14FB7DE, "I:X I:Y"),
            s(CharacterTemporaryStat.BMageAuraDrain, 856, 105, 0x14FB830, "I:X I:Y"),
            s(CharacterTemporaryStat.BMageAuraBlue, 857, 106, 0x14FB882, "I:X I:Y I:Z"),
            s(CharacterTemporaryStat.BMageAuraDark, 858, 107, 0x14FB8F0, "I:X I:Y"),
            s(CharacterTemporaryStat.BMageAuraDebuff, 859, 108, 0x14FB942, "I:X I:Y"),
            s(CharacterTemporaryStat.BMageAuraUnion, 860, 109, 0x14FB994, "I:X I:Y"),
            s(CharacterTemporaryStat.IceAura, 861, 110, 0x14FB9E6, "I:X I:Y"),
            s(CharacterTemporaryStat.KnightsAura, 862, 111, 0x14FBA38, "I:X I:Y"),
            s(CharacterTemporaryStat.ZeroAuraStr, 864, 112, 0x14FBA8A, "I:X I:Y"),
            s(CharacterTemporaryStat.NovaArcherIncanation, 865, 113, 0x14FBADC, "I:X I:Y"),
            s(CharacterTemporaryStat.RevenantGauge, 867, 101, 0x14FB704, "I:Z"),
            s(CharacterTemporaryStat.DeathDance, 868, 102, 0x14FB73A, "I:X I:Y I:Z"),
            s(CharacterTemporaryStat.ShadowShield, 869, 103, 0x14FB7A8, "I:X"),
            s(CharacterTemporaryStat.AranComboTempestAura, 870, 114, 0x14FBB2E, "I:X"),
            s(CharacterTemporaryStat.XenonBursterLaser, 871, 115, 0x14FBB68, "I:X"),
            s(CharacterTemporaryStat.BMageAbyssalLightning, 872, 116, 0x14FBB9E, "I:X"),
            s(CharacterTemporaryStat.LefMageCrystalGate, 878, 117, 0x14FBBD4, "I:X"),
            s(CharacterTemporaryStat.DarknessAura, 881, 120, 0x14FBC76, "I:X"),
            s(CharacterTemporaryStat.WeaponVarietyFinale, 882, 119, 0x14FBC40, "I:X"),
            s(CharacterTemporaryStat.NAThanatosDescent, 889, 122, 0x14FBCFE, "I:X"),
            s(CharacterTemporaryStat.NABrutalPang, 892, 123, 0x14FBD34, "I:X I:Y"),
            s(CharacterTemporaryStat.ATScrollPassive, 906, 125, 0x14FBDBC, "I:X I:Y"),
            s(CharacterTemporaryStat.YetiFuryGauge, 907, 126, 0x14FBE0E, "I:X"),
            s(CharacterTemporaryStat.YetiFuryMode, 908, 127, 0x14FBE44, "I:X"),
            s(CharacterTemporaryStat.SerpentScrew, 923, 121, 0x14FBCAC, "I:X I:Y"),
            s(CharacterTemporaryStat.HolyWater, 936, 118, 0x14FBC0A, "I:X"),
            s(CharacterTemporaryStat.LimitBreakFinalAttack, 946, 151, 0x14FC68E, "I:X"),
            s(CharacterTemporaryStat.SixthAssassination, 948, 73, 0x14FAD1C, "I:X"),
            s(CharacterTemporaryStat.Confinement, 972, 138, 0x14FC0F0, "I:X"),
            s(CharacterTemporaryStat.GrabAndThrow, 973, 140, 0x14FC178, "I:X I:Y I:Z I:C I:W"),
            s(CharacterTemporaryStat.DarkCloud, 974, 136, 0x14FC068, "I:X I:Y"),
            s(CharacterTemporaryStat.FixedSpeedAndJump, 975, 139, 0x14FC126, "I:X I:Y"),
            s(CharacterTemporaryStat.UserAroundAttackDebuff, 976, 137, 0x14FC0BA, "I:X"),
            s(CharacterTemporaryStat.LamourCarte, 987, 145, 0x14FC512, "I:X I:ZERO"),
            s(CharacterTemporaryStat.AdrenalinMaximum, 989, 146, 0x14FC564, "I:X I:Y"),
            s(CharacterTemporaryStat.Nightmare, 993, 148, 0x14FC5EC, "I:X"),
            s(CharacterTemporaryStat.RapidFire, 995, 147, 0x14FC5B6, "I:X"),
            s(CharacterTemporaryStat.ElementalFocus, 996, 134, 0x14FBFFC, "I:X"),
            s(CharacterTemporaryStat.DslayerMetamorphosis, 1003, 153, 0x14FC6FA, "I:X"),
            s(CharacterTemporaryStat.RenPlumSwordEx, 1010, 154, 0x14FC734, "I:X I:Y"),
            s(CharacterTemporaryStat.SpiritAwakeningStack, 1012, 155, 0x14FC786, "I:X"),
            s(CharacterTemporaryStat.SummonProp, 1044, 162, 0x14FCA25, "I:N I:Y I:Z"),
            s(CharacterTemporaryStat.WorldExpBuff, 1045, 159, 0x14FC8BF, "I:X I:Y I:Z"),
            s(CharacterTemporaryStat.WorldDropBuff, 1046, 160, 0x14FC92D, "I:X I:Y I:Z"),
            s(CharacterTemporaryStat.CurseRingBuff, 1047, 161, 0x14FC99B, "I:X I:Y I:Z I:U"),
            s(CharacterTemporaryStat.NeoTokyoBossPowOfLife, 1055, 164, 0x14FCAC9, "I:X")
        ));
        list.add(new Spec(CharacterTemporaryStat.StopForceAtomInfo, 512, 19, 0x14F96D9, "I I I I n + nxI", List.of(),
                (out, o) -> o.stopForceAtom.encode(out)));
        list.add(new Spec(CharacterTemporaryStat.Dice, 401, 4, 0x14F91D8, "21xI", List.of(),
                (out, o) -> {
                    for (int v : diceInfo(o.nOption)) {
                        out.encodeInt(v);
                    }
                }));
        list.add(new Spec(CharacterTemporaryStat.NextSpecificSkillDamageUp, 395, 22, 0x14F9837, "I n + nxI", List.of(),
                (out, o) -> {
                    List<Integer> ids = o.skillIds == null ? List.of() : o.skillIds;
                    out.encodeInt(ids.size());
                    for (Integer id : ids) {
                        out.encodeInt(id == null ? 0 : id);
                    }
                }));
        list.add(new Spec(CharacterTemporaryStat.RPEventStat, 978, 142, 0x14FC274, "19xI", List.of(),
                (out, o) -> {
                    for (int i = 0; i < RP_EVENT_STAT_INTS; i++) {
                        // bossmech1-0930: LEVEL, EXP, MAX_HP, MAX_MP, ATK, ATK_SPD, DEF, HP_REC, MP_REC, CRI, CRI_DAMR,
                        // MOVE_SPD, JUMP, STANCE, FS, MASTERY, REDUCE_COOL, ATK_MIN_RANGE, ATK_MAX_RANGE
                        // (lucid-re-0930/ability §1; values from LucidAbility, 自訂 level table); null = all 0
                        out.encodeInt(o.rpStats != null && i < o.rpStats.length ? o.rpStats[i] : 0);
                    }
                }));
        EnumMap<CharacterTemporaryStat, Spec> map = new EnumMap<>(CharacterTemporaryStat.class);
        for (Spec s : list) {
            if (map.put(s.stat(), s) != null) {
                throw new IllegalStateException("two specs for " + s.stat());
            }
        }
        SPECS = Collections.unmodifiableMap(map);
    }

    private Tms282CtsExplicit() {
    }

    private static Spec s(CharacterTemporaryStat stat, int bit, int seq, int rva, String shape) {
        List<Field> fields = new ArrayList<>();
        StringBuilder reads = new StringBuilder();
        for (String part : shape.split(" ")) {
            String[] wf = part.split(":");
            fields.add(new Field(wf[0].charAt(0), Src.valueOf(wf[1])));
            reads.append(reads.isEmpty() ? "" : " ").append(wf[0]);
        }
        return new Spec(stat, bit, seq, rva, reads.toString(), List.copyOf(fields), null);
    }

    public static Map<CharacterTemporaryStat, Spec> specs() {
        return SPECS;
    }

    public static Optional<Spec> spec(CharacterTemporaryStat cts) {
        return Optional.ofNullable(SPECS.get(cts));
    }

    /** Writes the explicit fields of every spec'd stat of {@code stats} that belongs to {@code area}, in seq order. */
    public static void write(OutPacket out, Map<CharacterTemporaryStat, List<Option>> stats, Area area) {
        stats.keySet().stream().map(SPECS::get).filter(Objects::nonNull).filter(s -> s.area() == area)
                .sorted(Comparator.comparingInt(Spec::seq))
                .forEach(s -> s.write(out, stats.get(s.stat()).getFirst()));
    }

    /**
     * The 265 dice-info array (TemporaryStatManager.throwDice) for a Dice nOption, first {@link #DICE_INTS}: one die
     * (n < 10) is throwDice(n), double down (n = 10a + b) throwDice(a, b).
     */
    public static int[] diceInfo(int nOption) {
        int d1 = nOption >= 10 ? nOption / 10 : nOption;
        int d2 = nOption >= 10 ? nOption % 10 : 0;
        int d3 = 0;
        int[] info = new int[DICE_INTS];
        if (d1 < 0 || d1 > 7 || d2 < 0 || d2 > 7) {
            return info;
        }
        int[] normal = {0, 0, 30, 20, 15, 20, 30, 20};
        int[] twice = {0, 0, 40, 30, 25, 30, 40, 30};
        int[] thrice = {0, 0, 50, 40, 40, 40, 50, 40};
        int[] opt = new int[8];
        if (d1 == d2 && d1 == d3) {
            opt[d1] = thrice[d1];
        } else if (d1 == d2) {
            opt[d1] = twice[d1];
            opt[d3] = normal[d3];
        } else if (d1 == d3) {
            opt[d1] = twice[d1];
            opt[d2] = normal[d2];
        } else if (d2 == d3) {
            opt[d2] = twice[d2];
            opt[d1] = normal[d1];
        } else {
            opt[d1] = normal[d1];
            opt[d2] = normal[d2];
            opt[d3] = normal[d3];
        }
        info[0] = opt[3];  // MHPR
        info[1] = opt[3];  // MMPR
        info[2] = opt[4];  // Cr
        info[8] = opt[2];  // PDDR
        info[9] = opt[2];  // MDDR
        info[12] = opt[5]; // PIDR
        info[17] = opt[6]; // EXP
        info[18] = opt[7]; // IED
        return info;
    }
}
