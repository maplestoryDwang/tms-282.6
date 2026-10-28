package net.swordie.ms.connection.protocol.codec;

import net.swordie.ms.life.mob.MobStat;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * TMS 282.3 MobStat bit numbers for the 265 {@link MobStat} names (research/tms2823/packet-audit/mobstat-bits-282.tsv,
 * decoder 0xECA3D0). Only names that exist on both sides are listed; a 265 stat without a row here is never sent.
 * Tms282MobPacketsCheck compares this table with the TSV.
 */
public final class Tms282MobStatBits {
    /** 24-byte mask. */
    public static final int MASK_INTS = 6;
    /** Indie stats are bits 0..9 (indie loop 0xECA420); bit 10 (IndieEnd) is not decoded. */
    public static final int INDIE_LAST = 9;
    /** Standard {@code I n, I r, S t} blocks: bits 11..104 in bit order (0xECA48C..0xECC0F5). */
    public static final int STANDARD_FIRST = 11;
    public static final int STANDARD_LAST = 104;
    /**
     * Movement stats: the extra byte after MOB_STAT_SET is read when the mask intersects the flag 0x9E3F020
     * (built by 0x53F30): bits 3, 17, 19, 20, 40, 49, 51, 59, 74, 113 (113 has no 265 name).
     */
    public static final Set<Integer> MOVEMENT_BITS = Set.of(3, 17, 19, 20, 40, 49, 51, 59, 74, 113);

    private static final Map<MobStat, Integer> BIT;
    private static final Map<MobStat, String> UNSUPPORTED;

    static {
        Map<MobStat, Integer> m = new EnumMap<>(MobStat.class);
        // indie 0..9 and standard 11..60: same bit as 265
        MobStat[] same = {MobStat.IndiePDR, MobStat.IndieMDR, MobStat.IndieAddFinalDamSkill, MobStat.IndieSlow,
                MobStat.IndieTriangleFormation, MobStat.IndieSixthTriangleFormation, MobStat.IndieDummyBuffIcon,
                MobStat.IndieAbyssalDarkness, MobStat.IndieTotalDam, MobStat.IndieTargetPriority,
                MobStat.PAD, MobStat.PDR, MobStat.MAD, MobStat.MDR, MobStat.ACC, MobStat.EVA, MobStat.Speed,
                MobStat.ArcMage2Stack, MobStat.Stun, MobStat.Freeze, MobStat.BeforeFreeze, MobStat.Poison, MobStat.Seal,
                MobStat.Darkness, MobStat.PowerUp, MobStat.MagicUp, MobStat.PGuardUp, MobStat.MGuardUp, MobStat.PImmune,
                MobStat.MImmune, MobStat.Web, MobStat.HardSkin, MobStat.Ambush, MobStat.Venom, MobStat.Blind,
                MobStat.SealSkill, MobStat.Dazzle, MobStat.PCounter, MobStat.MCounter, MobStat.RiseByToss,
                MobStat.Weakness, MobStat.Showdown, MobStat.DevilCry, MobStat.MagicCrash, MobStat.DamagedElemAttr,
                MobStat.TotalDamParty, MobStat.HitCriDamR, MobStat.Fatality, MobStat.Lifting, MobStat.DeadlyCharge,
                MobStat.Smite, MobStat.AddDamSkill, MobStat.AddExpSkill, MobStat.Incizing, MobStat.DodgeBodyAttack,
                MobStat.DebuffHealing, MobStat.AddDamSkill2, MobStat.BodyAttack, MobStat.TempMoveAbility,
                MobStat.FixDamRBuff};
        for (MobStat s : same) {
            m.put(s, s.getBitPos());
        }
        put(m, MobStat.ElementDarkness, 62);
        put(m, MobStat.AreaInstallByHit, 63);
        put(m, MobStat.BMageDebuff, 64);
        put(m, MobStat.PinkBeanFlowerPot, 65);
        put(m, MobStat.BattlePvPHelenaMark, 66);
        put(m, MobStat.BattlePvP_Darklord_Explosion, 67);
        put(m, MobStat.PowerImmune, 68);
        put(m, MobStat.MultiPMDR, 69);
        put(m, MobStat.BahamutLightElemAddDam, 70);
        put(m, MobStat.LefDebuff, 71);
        put(m, MobStat.BossPropPlus, 72);
        put(m, MobStat.MultiDamSkill, 73);
        put(m, MobStat.RWLiftPress, 74);
        put(m, MobStat.MobAggro, 75);
        put(m, MobStat.ActionState, 76);
        put(m, MobStat.AreaPDR, 77);
        put(m, MobStat.BuffControl, 78);
        put(m, MobStat.IgnoreFreeze, 79);
        put(m, MobStat.BattlePvP_Ryude_Frozen, 80);
        put(m, MobStat.DamR, 81);
        put(m, MobStat.ContinuousHeal, 82);
        put(m, MobStat.Morph, 83);
        put(m, MobStat.MobLock, 84);
        put(m, MobStat.TargetPlus, 86);
        put(m, MobStat.ReviveOnce, 87);
        put(m, MobStat.BuffFlag, 88);
        put(m, MobStat.HolyShell, 89);
        put(m, MobStat.ZeroCriticalBind, 90);
        put(m, MobStat.Panic, 91);
        put(m, MobStat.ReduceFinalDamage, 92);
        put(m, MobStat.Stalking, 93);
        put(m, MobStat.ChangeMobAction, 94);
        put(m, MobStat.TimeBomb, 97);
        put(m, MobStat.AddEffect, 98);
        put(m, MobStat.Invincible, 99);
        put(m, MobStat.Explosion, 100);
        put(m, MobStat.HangOver, 101);
        put(m, MobStat.LevelInc, 102);
        put(m, MobStat.AfterImage, 103);
        put(m, MobStat.Sleep, 104);
        // explicit-only blocks (no standard I I S), 265 bit - 14
        put(m, MobStat.BurnedInfo, 105);
        put(m, MobStat.ExchangeAttack, 107);
        put(m, MobStat.ExtraBuffStat, 108);
        put(m, MobStat.LinkTeam, 109);
        put(m, MobStat.SoulExplosion, 110);
        put(m, MobStat.TrueSight, 111);
        put(m, MobStat.Laser, 112);
        put(m, MobStat.Unk128, 114);
        put(m, MobStat.Unk129, 115);
        put(m, MobStat.Unk130, 116);
        put(m, MobStat.Unk131, 117);
        put(m, MobStat.Unk132, 118);
        put(m, MobStat.OriginDebuff, 119);
        put(m, MobStat.Unk134, 120);
        put(m, MobStat.Unk135, 121);
        put(m, MobStat.Unk136, 122);
        put(m, MobStat.Unk137, 123);
        put(m, MobStat.Unk138, 124);
        put(m, MobStat.Unk139, 125);
        put(m, MobStat.NewBurnedInfo, 127);
        BIT = Collections.unmodifiableMap(m);

        Map<MobStat, String> u = new EnumMap<>(MobStat.class);
        u.put(MobStat.IndieEnd, "bit 10 is not decoded by 282 (indie loop is bits 0..9)");
        u.put(MobStat.SpiritGate, "282 bit 61 is named GhostDisposition");
        u.put(MobStat.StatResetSkill, "282 bit 113 has no name and no value");
        u.put(MobStat.Unk140, "282 bit 126 has no name and no value");
        for (MobStat s : MobStat.values()) {
            if (!m.containsKey(s) && !u.containsKey(s)) {
                u.put(s, "no 282 bit with this name");
            }
        }
        UNSUPPORTED = Collections.unmodifiableMap(u);
    }

    private Tms282MobStatBits() {
    }

    private static void put(Map<MobStat, Integer> m, MobStat s, int bit) {
        if (m.put(s, bit) != null || m.values().stream().filter(b -> b == bit).count() > 1) {
            throw new ExceptionInInitializerError("duplicate 282 MobStat bit " + bit + " / " + s);
        }
    }

    /** 282 bit of a 265 stat, empty when 282 has no stat of that name. */
    public static Optional<Integer> bit(MobStat stat) {
        return Optional.ofNullable(BIT.get(stat));
    }

    public static boolean isSupported(MobStat stat) {
        return BIT.containsKey(stat);
    }

    /** Why a stat is not sent (null when it is). */
    public static String whyUnsupported(MobStat stat) {
        return UNSUPPORTED.get(stat);
    }

    /** Every supported stat and its 282 bit. */
    public static Map<MobStat, Integer> table() {
        return BIT;
    }

    /** Every 265 stat without a 282 bit. */
    public static Set<MobStat> unsupportedStats() {
        return UNSUPPORTED.isEmpty() ? EnumSet.noneOf(MobStat.class) : EnumSet.copyOf(UNSUPPORTED.keySet());
    }

    public static boolean isIndie(int bit) {
        return bit <= INDIE_LAST;
    }

    public static boolean isStandard(int bit) {
        return bit >= STANDARD_FIRST && bit <= STANDARD_LAST;
    }
}
